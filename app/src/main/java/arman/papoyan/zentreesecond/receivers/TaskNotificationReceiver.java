package arman.papoyan.zentreesecond.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import arman.papoyan.zentreesecond.MainActivity;
import arman.papoyan.zentreesecond.R;

public class TaskNotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "task_reminder_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskId = intent.getStringExtra("task_id");
        String taskTitle = intent.getStringExtra("task_title");
        int taskPriority = intent.getIntExtra("task_priority", 1);

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
                .setContentText(taskTitle)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(taskTitle + "\n" + priorityText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(taskId.hashCode(), builder.build());
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