package arman.papoyan.zentreesecond.workers;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Map;

import arman.papoyan.zentreesecond.utils.NotificationHelper;

public class UsageTrackerWorker extends Worker {
    public UsageTrackerWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    @NonNull
    @Override
    public Result doWork() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("notification_prefs", Context.MODE_PRIVATE);
        long lastCheck = prefs.getLong("last_check_time", 0);
        long now = System.currentTimeMillis();
        long screenTimeMs = getScreenTimeSince(lastCheck);
        long continuousTime = prefs.getLong("continuous_time", 0);
        continuousTime += screenTimeMs;
        if (now - lastCheck > 5 * 60 * 1000) {
            continuousTime = screenTimeMs;
        }
        if (continuousTime >= 60 * 60 * 1000) {
            sendNotification();
            continuousTime = 0;
            prefs.edit().putLong("last_notification_time", now).apply();
            prefs.edit().putBoolean("notification_sent", true).apply();
        }

        prefs.edit().putLong("continuous_time", continuousTime).apply();
        prefs.edit().putLong("last_check_time", now).apply();

        return Result.success();
    }

    private long getScreenTimeSince(long time) {
        UsageStatsManager usm = (UsageStatsManager) getApplicationContext().getSystemService(Context.USAGE_STATS_SERVICE);
        long end = System.currentTimeMillis();
        Map<String, UsageStats> stats = usm.queryAndAggregateUsageStats(time, end);
        long total = 0;
        for (UsageStats stat : stats.values()) {
            total += stat.getTotalTimeInForeground();
        }
        return total;
    }
    private void sendNotification() {
        NotificationHelper notificationHelper = new NotificationHelper(getApplicationContext());
        notificationHelper.sendBreakReminder();
    }

}