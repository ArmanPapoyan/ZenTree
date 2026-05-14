package arman.papoyan.zentreesecond.utils;

import android.content.Context;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import java.util.List;
import java.util.Map;

public class SyncHelper {
    private static final String TAG = "SyncHelper";
    private final Context context;
    private final FirebaseFirestore firestore;
    private final SyncQueueManager queueManager;
    private final Gson gson;

    public SyncHelper(Context context) {
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
        this.queueManager = SyncQueueManager.getInstance(context);
        this.gson = new Gson();
    }

    public void syncAll() {
        String userId = getUserId();
        if (userId == null) return;

        syncPendingTasks(userId);
        syncPendingTree(userId);
    }

    private void syncPendingTasks(String userId) {
        List<String> pendingTasks = queueManager.getPendingTasks();
        if (pendingTasks.isEmpty()) return;

        for (int i = 0; i < pendingTasks.size(); i++) {
            String taskJson = pendingTasks.get(i);
            Map<String, Object> taskMap = gson.fromJson(taskJson, Map.class);
            String taskId = (String) taskMap.get("id");

            int finalI = i;
            firestore.collection("users")
                    .document(userId)
                    .collection("tasks")
                    .document(taskId)
                    .set(taskMap)
                    .addOnSuccessListener(aVoid -> {
                        queueManager.removePendingTask(finalI);
                        Log.d(TAG, "Task synced: " + taskId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to sync task: " + taskId, e);
                    });
        }
    }

    private void syncPendingTree(String userId) {
        String treeJson = queueManager.getPendingTree();
        if (treeJson == null) return;

        Map<String, Object> treeMap = gson.fromJson(treeJson, Map.class);

        firestore.collection("users")
                .document(userId)
                .collection("tree")
                .document("current")
                .set(treeMap)
                .addOnSuccessListener(aVoid -> {
                    queueManager.clearPendingTree();
                    Log.d(TAG, "Tree synced");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to sync tree", e);
                });
    }

    private String getUserId() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }
}