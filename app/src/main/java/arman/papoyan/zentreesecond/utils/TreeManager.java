package arman.papoyan.zentreesecond.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import arman.papoyan.zentreesecond.models.TreeModel;

public class TreeManager {
    private static final String PREFS_NAME = "ZenTreePrefs";
    private static final String KEY_TOTAL_MINUTES = "total_minutes";

    private SharedPreferences prefs;
    private SharedPreferences loginPrefs;
    private TreeModel currentTree;
    private TreeFirestoreManager firestoreManager;
    private boolean isLoaded = false;
    private boolean isGuest;
    private Context context;
    public TreeManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loginPrefs = context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        firestoreManager = new TreeFirestoreManager();
        currentTree = new TreeModel();
        isGuest = loginPrefs.getBoolean("is_guest", false);
        this.context = context;
    }

    public void saveTree(TreeModel tree) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_TOTAL_MINUTES, tree.getTotalMinutes());
        editor.apply();
        currentTree = tree;

        if (!isGuest) {
            firestoreManager.saveTree(tree, new TreeFirestoreManager.TreeSaveCallback() {
                @Override
                public void onSuccess() {
                    Log.d("TreeManager", "Сохранено в Firestore");
                    SyncQueueManager.getInstance(context).clearPendingTree();
                }

                @Override
                public void onError(String error) {
                    Log.e("TreeManager", "Ошибка сохранения в Firestore: " + error);
                    saveTreeToQueue(tree);
                }
            });
        }
    }
    private void saveTreeToQueue(TreeModel tree) {
        if (context == null) return;

        try {
            Gson gson = new Gson();
            Map<String, Object> treeMap = new HashMap<>();
            treeMap.put("currentStage", tree.getCurrentStage());
            treeMap.put("totalMinutes", tree.getTotalMinutes());
            treeMap.put("progressInCurrentStage", tree.getProgressPercentage());
            treeMap.put("level", tree.getLevel());
            treeMap.put("lastUpdateDate", tree.getLastUpdateDate());
            treeMap.put("lastUpdateTime", System.currentTimeMillis());

            String treeJson = gson.toJson(treeMap);
            SyncQueueManager.getInstance(context).setPendingTree(treeJson);
            Log.d("TreeManager", "Дерево сохранено в очередь для синхронизации");
        } catch (Exception e) {
            Log.e("TreeManager", "Ошибка сохранения дерева в очередь: " + e.getMessage());
        }
    }


    public TreeModel loadTree() {
        if (isLoaded) {
            return currentTree;
        }
        int savedMinutes = prefs.getInt(KEY_TOTAL_MINUTES, 0);
        if (savedMinutes > 0) {
            int defaultX = 60;
            float defaultMotivation = 1.0f;
            currentTree.addMinutes(savedMinutes, defaultX, defaultMotivation);
        }
        if (!isGuest) {
            firestoreManager.loadTree(new TreeFirestoreManager.TreeLoadCallback() {
                @Override
                public void onSuccess(TreeModel tree) {
                    currentTree = tree;
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt(KEY_TOTAL_MINUTES, tree.getTotalMinutes());
                    editor.apply();
                    isLoaded = true;
                }

                @Override
                public void onError(String error) {
                    Log.e("TreeManager", "Ошибка загрузки из Firestore: " + error);
                    isLoaded = true;
                }
            });
        } else {
            isLoaded = true;
        }

        return currentTree;
    }

    public TreeModel getCurrentTree() {
        if (!isLoaded) {
            loadTree();
        }
        return currentTree;
    }
    public void resetTree(TreeModel tree, String todayDate) {
        tree.resetToDefault(todayDate);
        this.currentTree = tree;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_TOTAL_MINUTES, 0);
        editor.apply();
        if (!isGuest) {
            firestoreManager.saveTree(tree, new TreeFirestoreManager.TreeSaveCallback() {
                @Override
                public void onSuccess() {
                    Log.d("TreeManager", "Прогресс обнулен в Firestore");
                }
                @Override
                public void onError(String error) {
                    Log.e("TreeManager", "Ошибка при обнулении в Firestore: " + error);
                }
            });
        }
    }
    public void syncPendingTree() {
        if (isGuest || context == null) return;

        String treeJson = SyncQueueManager.getInstance(context).getPendingTree();
        if (treeJson == null) return;

        try {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> treeMap = gson.fromJson(treeJson, type);

            TreeModel pendingTree = new TreeModel();

            if (treeMap.containsKey("currentStage")) {
                pendingTree.setCurrentStage((int) (double) treeMap.get("currentStage"));
            }
            if (treeMap.containsKey("totalMinutes")) {
                pendingTree.setTotalMinutes((int) (double) treeMap.get("totalMinutes"));
            }
            if (treeMap.containsKey("progressInCurrentStage")) {
                pendingTree.setProgressInCurrentStage((int) (double) treeMap.get("progressInCurrentStage"));
            }
            if (treeMap.containsKey("level")) {
                pendingTree.setLevel((int) (double) treeMap.get("level"));
            }
            if (treeMap.containsKey("lastUpdateDate")) {
                pendingTree.setLastUpdateDate((String) treeMap.get("lastUpdateDate"));
            }

            firestoreManager.saveTree(pendingTree, new TreeFirestoreManager.TreeSaveCallback() {
                @Override
                public void onSuccess() {
                    SyncQueueManager.getInstance(context).clearPendingTree();
                    Log.d("TreeManager", "Очередь дерева синхронизирована");
                }
                @Override
                public void onError(String error) {
                    Log.e("TreeManager", "Ошибка синхронизации очереди: " + error);
                }
            });
        } catch (Exception e) {
            Log.e("TreeManager", "Ошибка восстановления дерева из очереди: " + e.getMessage());
        }
    }
    public long getTotalFocusMinutes() {
        SharedPreferences prefs = context.getSharedPreferences("tree_prefs", Context.MODE_PRIVATE);
        return prefs.getLong("total_focus_minutes", 0);
    }

    public int getTreeLevel() {
        SharedPreferences prefs = context.getSharedPreferences("tree_prefs", Context.MODE_PRIVATE);
        return prefs.getInt("tree_level", 1);
    }
}