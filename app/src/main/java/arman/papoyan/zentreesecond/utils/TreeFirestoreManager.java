package arman.papoyan.zentreesecond.utils;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import arman.papoyan.zentreesecond.models.TreeModel;

public class TreeFirestoreManager {
    private static final String TAG = "TreeFirestore";
    private static final String COLLECTION_USERS = "users";
    private static final String FIELD_TREE = "tree";

    private FirebaseFirestore db;
    private String userId;

    public TreeFirestoreManager() {
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        }
    }

    public void saveTree(TreeModel tree, TreeSaveCallback callback) {
        if (userId == null) {
            Log.e(TAG, "Пользователь не авторизован");
            callback.onError("User not logged in");
            return;
        }

        DocumentReference userDoc = db.collection(COLLECTION_USERS).document(userId);

        TreeData treeData = new TreeData(
                tree.getTotalMinutes(),
                tree.getLevel(),
                tree.getCurrentStage(),
                System.currentTimeMillis()
        );

        userDoc.collection(FIELD_TREE).document("progress")
                .set(treeData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Дерево сохранено");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка сохранения: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    public void loadTree(TreeLoadCallback callback) {
        if (userId == null) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                userId = user.getUid();
            } else {
                callback.onError("User not logged in");
                return;
            }
        }

        db.collection(COLLECTION_USERS).document(userId)
                .collection(FIELD_TREE).document("progress")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        TreeData data = documentSnapshot.toObject(TreeData.class);
                        if (data != null) {
                            TreeModel tree = new TreeModel();
                            int defaultX = 60;
                            float defaultMotivation = 1.0f;
                            tree.addMinutes(data.totalMinutes, defaultX, defaultMotivation);
                            callback.onSuccess(tree);
                        } else {
                            callback.onSuccess(new TreeModel());
                        }
                    } else {
                        callback.onSuccess(new TreeModel());
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onError(e.getMessage());
                });
    }

    private static class TreeData {
        public int totalMinutes;
        public int level;
        public int stage;
        public long updatedAt;

        public TreeData() {}

        public TreeData(int totalMinutes, int level, int stage, long updatedAt) {
            this.totalMinutes = totalMinutes;
            this.level = level;
            this.stage = stage;
            this.updatedAt = updatedAt;
        }
    }

    public interface TreeSaveCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface TreeLoadCallback {
        void onSuccess(TreeModel tree);
        void onError(String error);
    }
}