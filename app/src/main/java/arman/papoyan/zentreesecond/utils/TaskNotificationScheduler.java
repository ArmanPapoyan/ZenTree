package arman.papoyan.zentreesecond.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.Date;

import arman.papoyan.zentreesecond.models.Task;
import arman.papoyan.zentreesecond.receivers.TaskNotificationReceiver;

public class TaskNotificationScheduler {

    private static final String TAG = "TaskNotificationScheduler";

    public static final String EXTRA_NOTIFICATION_SUBTYPE = "notification_subtype";
    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_TASK_TITLE = "task_title";
    public static final String EXTRA_TASK_PRIORITY = "task_priority";
    public static final String EXTRA_IS_REPEATING = "is_repeating";

    public static final int SUBTYPE_BEFORE_1H = 101;
    public static final int SUBTYPE_BEFORE_30M = 102;
    public static final int SUBTYPE_BEFORE_5M = 103;

    public static final int SUBTYPE_AT_EXACT = 201;
    public static final int SUBTYPE_AT_REPEAT = 202;

    public static final int SUBTYPE_AFTER_EXACT = 301;
    public static final int SUBTYPE_AFTER_5M = 302;
    public static final int SUBTYPE_AFTER_30M = 303;
    public static final int SUBTYPE_AFTER_1H = 304;
    public static final int SUBTYPE_AFTER_REPEAT = 305;

    public static final int SUBTYPE_RANGE_START = 401;
    public static final int SUBTYPE_RANGE_MID = 402;
    public static final int SUBTYPE_RANGE_END = 403;
    public static final int SUBTYPE_RANGE_REPEAT = 404;

    public static void scheduleTaskNotifications(Context context, Task task) {
        if (context == null || task == null) {
            Log.d(TAG, "context или task == null");
            return;
        }

        if (task.isCompleted()) {
            Log.d(TAG, "Задача уже выполнена, уведомления не планируем");
            return;
        }

        cancelAllTaskNotifications(context, task);

        Log.d(TAG, "=== Планируем уведомления для задачи: " + task.getTitle());
        Log.d(TAG, "TimeType: " + task.getTimeType());

        long now = System.currentTimeMillis();

        switch (task.getTimeType()) {
            case 1:
                scheduleBeforeNotifications(context, task, now);
                break;
            case 2:
                scheduleAtNotifications(context, task, now);
                break;
            case 3:
                scheduleAfterNotifications(context, task, now);
                break;
            case 4:
                scheduleRangeNotifications(context, task, now);
                break;
        }
    }

    private static void scheduleBeforeNotifications(Context context, Task task, long now) {
        long targetTime = getTargetTime(task);
        if (targetTime == 0) return;

        Log.d(TAG, "BEFORE: targetTime = " + new Date(targetTime));

        long time1h = targetTime - 60 * 60 * 1000;
        if (time1h > now) {
            scheduleOneNotification(context, task, time1h, SUBTYPE_BEFORE_1H, false);
        }

        long time30m = targetTime - 30 * 60 * 1000;
        if (time30m > now) {
            scheduleOneNotification(context, task, time30m, SUBTYPE_BEFORE_30M, false);
        }

        long time5m = targetTime - 5 * 60 * 1000;
        if (time5m > now) {
            scheduleOneNotification(context, task, time5m, SUBTYPE_BEFORE_5M, false);
        }

        Log.d(TAG, "BEFORE: запланировано 3 уведомления");
    }

    private static void scheduleAtNotifications(Context context, Task task, long now) {
        long targetTime = getTargetTime(task);
        if (targetTime == 0) return;

        Log.d(TAG, "AT: targetTime = " + new Date(targetTime));

        if (targetTime > now) {
            scheduleOneNotification(context, task, targetTime, SUBTYPE_AT_EXACT, false);
        }

        long repeatTime = targetTime + 15 * 60 * 1000;
        scheduleOneNotification(context, task, repeatTime, SUBTYPE_AT_REPEAT, true);

        Log.d(TAG, "AT: запланировано уведомление в точное время + повторяющееся");
    }

    private static void scheduleAfterNotifications(Context context, Task task, long now) {
        long targetTime = getTargetTime(task);
        if (targetTime == 0) return;

        Log.d(TAG, "AFTER: targetTime = " + new Date(targetTime));

        if (targetTime > now) {
            scheduleOneNotification(context, task, targetTime, SUBTYPE_AFTER_EXACT, false);
        }

        long time5m = targetTime + 5 * 60 * 1000;
        scheduleOneNotification(context, task, time5m, SUBTYPE_AFTER_5M, false);

        long time30m = targetTime + 30 * 60 * 1000;
        scheduleOneNotification(context, task, time30m, SUBTYPE_AFTER_30M, false);

        long time1h = targetTime + 60 * 60 * 1000;
        scheduleOneNotification(context, task, time1h, SUBTYPE_AFTER_1H, false);

        long repeatTime = targetTime + 2 * 60 * 60 * 1000;
        scheduleOneNotification(context, task, repeatTime, SUBTYPE_AFTER_REPEAT, true);

        Log.d(TAG, "AFTER: запланировано 4 уведомления + повторяющееся");
    }

    private static void scheduleRangeNotifications(Context context, Task task, long now) {
        long startTime = getTargetTime(task);
        long endTime = getEndTime(task);

        if (startTime == 0 || endTime == 0) return;

        Log.d(TAG, "RANGE: startTime = " + new Date(startTime));
        Log.d(TAG, "RANGE: endTime = " + new Date(endTime));

        long duration = endTime - startTime;
        long midTime = startTime + duration / 2;

        if (startTime > now) {
            scheduleOneNotification(context, task, startTime, SUBTYPE_RANGE_START, false);
        }

        if (midTime > now) {
            scheduleOneNotification(context, task, midTime, SUBTYPE_RANGE_MID, false);
        }

        if (endTime > now) {
            scheduleOneNotification(context, task, endTime, SUBTYPE_RANGE_END, false);
        }

        long repeatTime = endTime + 15 * 60 * 1000;
        scheduleOneNotification(context, task, repeatTime, SUBTYPE_RANGE_REPEAT, true);

        Log.d(TAG, "RANGE: запланировано 3 уведомления + повторяющееся");
    }

    private static long getTargetTime(Task task) {
        try {
            String dateTimeString = task.getTargetDate() + " " +
                    String.format("%02d:%02d", task.getTargetHour(), task.getTargetMinute());
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
            java.util.Date date = sdf.parse(dateTimeString);
            return date != null ? date.getTime() : 0;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка парсинга targetTime", e);
            return 0;
        }
    }

    private static long getEndTime(Task task) {
        try {
            String dateTimeString = task.getTargetDate() + " " +
                    String.format("%02d:%02d", task.getEndHour(), task.getEndMinute());
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
            java.util.Date date = sdf.parse(dateTimeString);
            return date != null ? date.getTime() : 0;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка парсинга endTime", e);
            return 0;
        }
    }

    public static void scheduleOneNotification(Context context, Task task, long triggerTime, int subtype, boolean isRepeating) {
        if (triggerTime <= System.currentTimeMillis()) {
            Log.d(TAG, "Время уведомления в прошлом, пропускаем");
            return;
        }

        checkExactAlarmPermission(context);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, TaskNotificationReceiver.class);
        intent.putExtra(EXTRA_TASK_ID, task.getId());
        intent.putExtra(EXTRA_TASK_TITLE, task.getTitle());
        intent.putExtra(EXTRA_TASK_PRIORITY, task.getPriority());
        intent.putExtra(EXTRA_NOTIFICATION_SUBTYPE, subtype);
        intent.putExtra(EXTRA_IS_REPEATING, isRepeating);

        int requestCode = (task.getId().hashCode() + subtype);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }

        Log.d(TAG, "Запланировано уведомление подтип " + subtype + " на " + new Date(triggerTime));
    }

    private static void checkExactAlarmPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                Toast.makeText(context, "Разрешите точные уведомления для напоминаний", Toast.LENGTH_LONG).show();
            }
        }
    }

    public static void cancelAllTaskNotifications(Context context, Task task) {
        if (context == null || task == null) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        int[] subtypes = {
                SUBTYPE_BEFORE_1H, SUBTYPE_BEFORE_30M, SUBTYPE_BEFORE_5M,
                SUBTYPE_AT_EXACT, SUBTYPE_AT_REPEAT,
                SUBTYPE_AFTER_EXACT, SUBTYPE_AFTER_5M, SUBTYPE_AFTER_30M, SUBTYPE_AFTER_1H, SUBTYPE_AFTER_REPEAT,
                SUBTYPE_RANGE_START, SUBTYPE_RANGE_MID, SUBTYPE_RANGE_END, SUBTYPE_RANGE_REPEAT
        };

        for (int subtype : subtypes) {
            int requestCode = task.getId().hashCode() + subtype;
            Intent intent = new Intent(context, TaskNotificationReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }

        Log.d(TAG, "Отменены все уведомления для задачи: " + task.getTitle());
    }
}