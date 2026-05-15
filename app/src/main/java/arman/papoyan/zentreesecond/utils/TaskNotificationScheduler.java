package arman.papoyan.zentreesecond.utils;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import arman.papoyan.zentreesecond.models.Task;
import arman.papoyan.zentreesecond.receivers.TaskNotificationReceiver;

public class TaskNotificationScheduler {

    @SuppressLint("ScheduleExactAlarm")
    public static void scheduleTaskNotification(Context context, Task task) {
        if (context == null) {
            Log.d("TaskNotification", "context == null, возврат");
            return;
        }

        Log.d("TaskNotification", "scheduleTaskNotification вызван для задачи: " + task.getTitle());
        Log.d("TaskNotification", "notificationEnabled: " + task.isNotificationEnabled());
        Log.d("TaskNotification", "notificationTime: " + task.getNotificationTime());

        if (!task.isNotificationEnabled() || task.isNotificationSent()) {
            Log.d("TaskNotification", "Уведомление отключено или уже отправлено");
            return;
        }

        long notificationTime = task.getNotificationTime();
        long now = System.currentTimeMillis();
        Log.d("TaskNotification", "notificationTime: " + notificationTime + ", now: " + now);

        if (notificationTime <= now) {
            Log.d("TaskNotification", "Время уведомления в прошлом");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, TaskNotificationReceiver.class);
        intent.putExtra("task_id", task.getId());
        intent.putExtra("task_title", task.getTitle());
        intent.putExtra("task_priority", task.getPriority());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                task.getId().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    notificationTime,
                    pendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    notificationTime,
                    pendingIntent
            );
        }
        Log.d("TaskNotification", "Уведомление запланировано на " + notificationTime);
    }

    public static void cancelTaskNotification(Context context, Task task) {
        if (context == null) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, TaskNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                task.getId().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
        Log.d("TaskNotification", "Уведомление отменено для задачи: " + task.getTitle());
    }
}