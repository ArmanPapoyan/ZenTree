package arman.papoyan.zentreesecond.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SyncQueueManager {
    private static final String PREF_NAME = "sync_queue";
    private static final String KEY_PENDING_TASKS = "pending_tasks";
    private static final String KEY_PENDING_TREE = "pending_tree";
    private static SyncQueueManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    private SyncQueueManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized SyncQueueManager getInstance(Context context) {
        if (instance == null) {
            instance = new SyncQueueManager(context.getApplicationContext());
        }
        return instance;
    }

    public void addPendingTask(String taskJson) {
        List<String> pendingTasks = getPendingTasks();
        pendingTasks.add(taskJson);
        savePendingTasks(pendingTasks);
    }

    public List<String> getPendingTasks() {
        String json = prefs.getString(KEY_PENDING_TASKS, "[]");
        Type type = new TypeToken<ArrayList<String>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public void removePendingTask(int index) {
        List<String> pendingTasks = getPendingTasks();
        if (index < pendingTasks.size()) {
            pendingTasks.remove(index);
            savePendingTasks(pendingTasks);
        }
    }

    public void clearPendingTasks() {
        savePendingTasks(new ArrayList<>());
    }

    private void savePendingTasks(List<String> tasks) {
        String json = gson.toJson(tasks);
        prefs.edit().putString(KEY_PENDING_TASKS, json).apply();
    }

    public void setPendingTree(String treeJson) {
        prefs.edit().putString(KEY_PENDING_TREE, treeJson).apply();
    }

    public String getPendingTree() {
        return prefs.getString(KEY_PENDING_TREE, null);
    }

    public void clearPendingTree() {
        prefs.edit().remove(KEY_PENDING_TREE).apply();
    }
}