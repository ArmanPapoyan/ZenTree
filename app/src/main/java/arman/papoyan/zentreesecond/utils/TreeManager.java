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
    private static final String KEY_TREE_LEVEL = "tree_level";
    private static final String KEY_CURRENT_STAGE = "current_stage";

    private SharedPreferences prefs;
    private SharedPreferences loginPrefs;
    private TreeModel currentTree;
    private TreeFirestoreManager firestoreManager;
    private boolean isLoaded = false;
    private Context context;

    public TreeManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loginPrefs = context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        firestoreManager = new TreeFirestoreManager();
        currentTree = new TreeModel();
    }

    public void saveTree(TreeModel tree) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_TOTAL_MINUTES, tree.getTotalMinutes());
        editor.putInt(KEY_TREE_LEVEL, tree.getLevel());
        editor.putInt(KEY_CURRENT_STAGE, tree.getCurrentStage());
        editor.apply();
        currentTree = tree;
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
        Log.d("TreeManager", "Loading tree, isLoaded=" + isLoaded);

        if (isLoaded && currentTree != null) {
            return currentTree;
        }

        int savedMinutes = prefs.getInt(KEY_TOTAL_MINUTES, 0);
        int savedLevel = prefs.getInt(KEY_TREE_LEVEL, 1);
        int savedStage = prefs.getInt(KEY_CURRENT_STAGE, 1);

        if (savedMinutes > 0) {
            currentTree.setTotalMinutes(savedMinutes);
            currentTree.setLevel(savedLevel);
            currentTree.setCurrentStage(savedStage);

            SharedPreferences growthPrefs = context.getSharedPreferences("growth_prefs", Context.MODE_PRIVATE);
            int x = growthPrefs.getInt("x", 60);
            float motivation = growthPrefs.getFloat("motivation", 1.0f);

            currentTree.recalculateProgress(savedMinutes, x, motivation);
        }

        isLoaded = true;

        Log.d("TreeManager", "Tree loaded: minutes=" + currentTree.getTotalMinutes() +
                ", level=" + currentTree.getLevel() +
                ", stage=" + currentTree.getCurrentStage());

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
        editor.putInt(KEY_TREE_LEVEL, 1);
        editor.putInt(KEY_CURRENT_STAGE, 1);
        editor.apply();
    }

    public void syncPendingTree() {
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

    public int getTotalFocusMinutes() {
        return prefs.getInt(KEY_TOTAL_MINUTES, 0);
    }

    public int getTreeLevel() {
        return prefs.getInt(KEY_TREE_LEVEL, 1);
    }

    public int getCurrentStage() {
        return prefs.getInt(KEY_CURRENT_STAGE, 1);
    }

    public void addFocusMinutes(int minutes) {
        int currentMinutes = prefs.getInt(KEY_TOTAL_MINUTES, 0);
        int newMinutes = currentMinutes + minutes;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_TOTAL_MINUTES, newMinutes);
        editor.apply();

        if (currentTree != null) {
            SharedPreferences growthPrefs = context.getSharedPreferences("growth_prefs", Context.MODE_PRIVATE);
            int x = growthPrefs.getInt("x", 60);
            float motivation = growthPrefs.getFloat("motivation", 1.0f);

            currentTree.addMinutes(minutes, x, motivation);
            saveTree(currentTree);
        }

        Log.d("TreeManager", "Добавлено " + minutes + " минут. Всего: " + newMinutes);
    }
}