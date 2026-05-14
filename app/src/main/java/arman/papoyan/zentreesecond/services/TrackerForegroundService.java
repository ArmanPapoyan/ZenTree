package arman.papoyan.zentreesecond.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Map;

import arman.papoyan.zentreesecond.MainActivity;
import arman.papoyan.zentreesecond.R;
import arman.papoyan.zentreesecond.utils.NotificationHelper;

public class TrackerForegroundService extends Service {
    private static final String CHANNEL_ID = "tracker_channel";
    private static final int NOTIFICATION_ID = 2001;
    private Handler handler;
    private Runnable trackerRunnable;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        prefs = getSharedPreferences("notification_prefs", MODE_PRIVATE);
        handler = new Handler();
        startTracking();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Zen Tree — отслеживание экранного времени",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Помогает вовремя напоминать об отдыхе");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Zen Tree")
                .setContentText("Отслеживание экранного времени активно")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void startTracking() {
        trackerRunnable = new Runnable() {
            @Override
            public void run() {
                checkAndSendNotification();
                if (handler != null) {
                    handler.postDelayed(this, 60 * 1000);
                }
            }
        };
        handler.post(trackerRunnable);
    }

    private void checkAndSendNotification() {

        long now = System.currentTimeMillis();
        long lastCheck = prefs.getLong("last_check_time", now);
        long lastNotify = prefs.getLong("last_notification_time", 0);

        long screenTimeMs = getScreenTimeSince(lastCheck);

        long continuousTime = prefs.getLong("continuous_time", 0);
        continuousTime += screenTimeMs;

        if (now - lastCheck > 5 * 60 * 1000) {
            continuousTime = screenTimeMs;
            Log.d("TrackerService", "Длительный перерыв, сброс");
        }

        if (continuousTime >= 60 * 60 * 1000 && (now - lastNotify >= 60 * 60 * 1000)) {
            sendNotification();
            prefs.edit().putLong("last_notification_time", now).apply();
            continuousTime = 0;
            Log.d("TrackerService", "Уведомление отправлено");
        }

        prefs.edit()
                .putLong("continuous_time", continuousTime)
                .putLong("last_check_time", now)
                .apply();
        Log.d("TrackerService", "continuousTime = " + continuousTime + " ms, screenTimeMs = " + screenTimeMs);
    }

    private long getScreenTimeSince(long time) {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long end = System.currentTimeMillis();
        Map<String, UsageStats> stats = usm.queryAndAggregateUsageStats(time, end);
        long total = 0;
        for (UsageStats stat : stats.values()) {
            total += stat.getTotalTimeInForeground();
        }
        return total;
    }

    private void sendNotification() {
        NotificationHelper helper = new NotificationHelper(this);
        helper.sendBreakReminder();
        Log.d("TrackerService", "sendNotification() вызван");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(trackerRunnable);
            handler = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}