package arman.papoyan.zentreesecond.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import arman.papoyan.zentreesecond.model.TreeModel;

public class TreeManager {
    private static final String PREFS_NAME = "ZenTreePrefs";
    private static final String KEY_TOTAL_MINUTES = "total_minutes";

    private SharedPreferences prefs;
    private SharedPreferences loginPrefs;
    private TreeModel currentTree;
    private TreeFirestoreManager firestoreManager;
    private boolean isLoaded = false;
    private boolean isGuest;

    public TreeManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loginPrefs = context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        firestoreManager = new TreeFirestoreManager();
        currentTree = new TreeModel();
        isGuest = loginPrefs.getBoolean("is_guest", false);
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
                }

                @Override
                public void onError(String error) {
                    Log.e("TreeManager", "Ошибка сохранения в Firestore: " + error);
                }
            });
        }
    }

    public TreeModel loadTree() {
        if (isLoaded) {
            return currentTree;
        }
        int savedMinutes = prefs.getInt(KEY_TOTAL_MINUTES, 0);
        if (savedMinutes > 0) {
            currentTree.addMinutes(savedMinutes);
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
}