package arman.papoyan.zentreesecond.utils;

import android.content.Context;
import android.content.SharedPreferences;

import arman.papoyan.zentreesecond.model.TreeModel;

public class TreeManager {
    private static final String PREFS_NAME = "ZenTreePrefs";
    private static final String KEY_LEVEL = "tree_level";
    private static final String KEY_TOTAL_MINUTES = "total_minutes";
    private static final String KEY_STAGE = "tree_stage";

    private SharedPreferences prefs;
    private TreeModel currentTree;

    public TreeManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        currentTree = loadTree();
    }

    public void saveTree(TreeModel tree) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_LEVEL, tree.getLevel());
        editor.putInt(KEY_TOTAL_MINUTES, tree.getTotalMinutes());
        editor.putInt(KEY_STAGE, tree.getCurrentStage());
        editor.apply();
        currentTree = tree;
    }

    public TreeModel loadTree() {
        if (currentTree == null) {
            currentTree = new TreeModel();
            int savedMinutes = prefs.getInt(KEY_TOTAL_MINUTES, 0);
            if (savedMinutes > 0) {
                currentTree.addMinutes(savedMinutes);
            }
        }
        return currentTree;
    }

    public TreeModel getCurrentTree() {
        if (currentTree == null) {
            currentTree = loadTree();
        }
        return currentTree;
    }

    public static int getTreeStage(int level) {
        if (level <= 1) return 1;
        if (level <= 2) return 2;
        if (level <= 3) return 3;
        if (level <= 4) return 4;
        if (level <= 5) return 5;
        return 6;
    }
}