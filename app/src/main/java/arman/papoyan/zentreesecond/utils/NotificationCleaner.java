package arman.papoyan.zentreesecond.utils;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationCleaner {

    private static final String TAG = "NotificationCleaner";
    public static void cancelAllScheduledNotifications(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, TaskNotificationReceiver.class);

        for (int i = 0; i < 1000; i++) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    i,
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
        Log.d(TAG, "Все запланированные уведомления отменены");
    }

    public static void dismissAllNotifications(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
        Log.d(TAG, "Все уведомления из панели удалены");
    }

    public static void clearAllNotifications(Context context) {
        cancelAllScheduledNotifications(context);
        dismissAllNotifications(context);
    }
}