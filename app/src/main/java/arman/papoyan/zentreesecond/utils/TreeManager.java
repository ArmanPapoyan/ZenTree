package arman.papoyan.zentreesecond.utils;

import android.content.Context;
import android.content.SharedPreferences;
import arman.papoyan.zentreesecond.model.TreeModel;

public class TreeManager {
    private static final String PREFS_NAME = "ZenTreePrefs";
    private static final String KEY_LEVEL = "tree_level";
    private static final String KEY_EXP = "tree_experience";
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
        editor.putInt(KEY_EXP, tree.getExperience());
        editor.putInt(KEY_STAGE, tree.getCurrentStage());
        editor.apply();
        currentTree = tree;
    }

    public TreeModel loadTree() {
        if (currentTree == null) {
            currentTree = new TreeModel();
            int savedLevel = prefs.getInt(KEY_LEVEL, 1);
            int savedExp = prefs.getInt(KEY_EXP, 0);
            int savedStage = prefs.getInt(KEY_STAGE, 1);
            restoreTreeState(savedLevel, savedExp, savedStage);
        }
        return currentTree;
    }

    private void restoreTreeState(int level, int exp, int stage) {
        currentTree = new TreeModel();
        int targetExp = exp;
        while (targetExp > 0) {
            int expToAdd = Math.min(targetExp, 10);
            currentTree.addExperience(expToAdd);
            targetExp -= expToAdd;
        }
    }

    public TreeModel getCurrentTree() {
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