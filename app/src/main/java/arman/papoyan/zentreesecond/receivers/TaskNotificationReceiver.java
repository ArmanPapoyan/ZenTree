package arman.papoyan.zentreesecond.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import arman.papoyan.zentreesecond.MainActivity;
import arman.papoyan.zentreesecond.R;
import arman.papoyan.zentreesecond.models.Task;
import arman.papoyan.zentreesecond.utils.TaskNotificationScheduler;

public class TaskNotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "task_reminder_channel";
    private static final String TAG = "TaskNotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskId = intent.getStringExtra(TaskNotificationScheduler.EXTRA_TASK_ID);
        String taskTitle = intent.getStringExtra(TaskNotificationScheduler.EXTRA_TASK_TITLE);
        int taskPriority = intent.getIntExtra(TaskNotificationScheduler.EXTRA_TASK_PRIORITY, 1);
        int subtype = intent.getIntExtra(TaskNotificationScheduler.EXTRA_NOTIFICATION_SUBTYPE, 0);
        boolean isRepeating = intent.getBooleanExtra(TaskNotificationScheduler.EXTRA_IS_REPEATING, false);

        if (taskId == null || taskTitle == null) {
            Log.e(TAG, "Нет данных задачи");
            return;
        }

        if (isTaskCompleted(context, taskId)) {
            Log.d(TAG, "Задача уже выполнена, уведомление отменяется: " + taskTitle);
            return;
        }

        String message = getNotificationMessage(context, taskTitle, subtype);

        if (isRepeating) {
            scheduleNextRepeat(context, taskId, taskTitle, taskPriority, subtype);
        }

        sendNotification(context, taskId, taskTitle, taskPriority, message);
    }

    private String getNotificationMessage(Context context, String taskTitle, int subtype) {
        switch (subtype) {
            case TaskNotificationScheduler.SUBTYPE_BEFORE_1H:
                return "⏰ Через 1 час: " + taskTitle;
            case TaskNotificationScheduler.SUBTYPE_BEFORE_30M:
                return "⏰ Через 30 минут: " + taskTitle;
            case TaskNotificationScheduler.SUBTYPE_BEFORE_5M:
                return "⚠️ Через 5 минут: " + taskTitle;

            case TaskNotificationScheduler.SUBTYPE_AT_EXACT:
                return "🔔 Пора выполнять: " + taskTitle;
            case TaskNotificationScheduler.SUBTYPE_AT_REPEAT:
                return "🔄 Напоминание (каждые 15 минут): " + taskTitle;

            case TaskNotificationScheduler.SUBTYPE_AFTER_EXACT:
                return "🔔 Время пришло: " + taskTitle;
            case TaskNotificationScheduler.SUBTYPE_AFTER_5M:
                return "🕐 5 минут прошло. Выполните: " + taskTitle;
            case TaskNotificationScheduler.SUBTYPE_AFTER_30M:
                return "🕜 30 минут прошло. Выполните: " + taskTitle;
            case TaskNotificationScheduler.SUBTYPE_AFTER_1H:
                return "🕙 1 час прошёл. Выполните: " + taskTitle;
            case TaskNotificationScheduler.SUBTYPE_AFTER_REPEAT:
                return "🔄 Напоминание (каждый час): " + taskTitle;

            case TaskNotificationScheduler.SUBTYPE_RANGE_START:
                return "🔔 Начало интервала: " + taskTitle;
            case TaskNotificationScheduler.SUBTYPE_RANGE_MID:
                return "⏸️ Середина интервала: " + taskTitle;
            case TaskNotificationScheduler.SUBTYPE_RANGE_END:
                return "⏹️ Конец интервала: " + taskTitle;
            case TaskNotificationScheduler.SUBTYPE_RANGE_REPEAT:
                return "🔄 Напоминание (каждые 15 минут): " + taskTitle;

            default:
                return "Напоминание: " + taskTitle;
        }
    }

    private void scheduleNextRepeat(Context context, String taskId, String taskTitle, int taskPriority, int currentSubtype) {
        Task tempTask = new Task();
        tempTask.setId(taskId);
        tempTask.setTitle(taskTitle);
        tempTask.setPriority(taskPriority);
        tempTask.setCompleted(false);

        long nextDelay = 0;
        int nextSubtype = currentSubtype;

        switch (currentSubtype) {
            case TaskNotificationScheduler.SUBTYPE_AT_REPEAT:
                nextDelay = 15 * 60 * 1000;
                nextSubtype = TaskNotificationScheduler.SUBTYPE_AT_REPEAT;
                break;
            case TaskNotificationScheduler.SUBTYPE_AFTER_REPEAT:
                nextDelay = 60 * 60 * 1000;
                nextSubtype = TaskNotificationScheduler.SUBTYPE_AFTER_REPEAT;
                break;
            case TaskNotificationScheduler.SUBTYPE_RANGE_REPEAT:
                nextDelay = 15 * 60 * 1000;
                nextSubtype = TaskNotificationScheduler.SUBTYPE_RANGE_REPEAT;
                break;
            default:
                return;
        }

        long nextTime = System.currentTimeMillis() + nextDelay;
        TaskNotificationScheduler.scheduleOneNotification(context, tempTask, nextTime, nextSubtype, true);
        Log.d(TAG, "Запланировано следующее повторное уведомление через " + nextDelay/60000 + " минут");
    }

    private boolean isTaskCompleted(Context context, String taskId) {
        return false;
    }

    private void sendNotification(Context context, String taskId, String taskTitle, int taskPriority, String message) {
        createNotificationChannel(context);

        String priorityText = "";
        switch (taskPriority) {
            case 1: priorityText = context.getString(R.string.priority_high_emoji); break;
            case 2: priorityText = context.getString(R.string.priority_medium_emoji); break;
            case 3: priorityText = context.getString(R.string.priority_low_emoji); break;
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        openIntent.putExtra("open_tasks_tab", true);
        openIntent.putExtra("task_id", taskId);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                taskId.hashCode(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.notification_title_task_reminder))
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message + "\n" + priorityText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(taskId.hashCode(), builder.build());

        Log.d(TAG, "Уведомление отправлено: " + message);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(context.getString(R.string.notification_channel_description));
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
}