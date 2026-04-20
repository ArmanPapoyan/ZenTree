package arman.papoyan.zentreesecond.services;

import static android.content.ContentValues.TAG;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import arman.papoyan.zentreesecond.R;
import arman.papoyan.zentreesecond.model.TreeModel;
import arman.papoyan.zentreesecond.utils.ScreenStateReceiver;
import arman.papoyan.zentreesecond.utils.TreeManager;

public class FocusForegroundService extends Service {
    private static final String CHANNEL_ID = "focus_channel";
    private static final int NOTIFICATION_ID = 1;

    private ScreenStateReceiver screenReceiver;
    private TreeManager treeManager;
    private TreeModel tree;
    private Handler growthHandler;
    private Runnable growthUpdateRunnable;
    private long screenOffTime = 0;
    private boolean isGrowing = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        treeManager = new TreeManager(this);
        tree = treeManager.loadTree();
        growthHandler = new Handler();

        registerScreenReceiver();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Фокус-режим",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Zen Tree")
                .setContentText("Фокус-режим активен. Дерево растёт, когда экран выключен.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void registerScreenReceiver() {
        screenReceiver = new ScreenStateReceiver(new ScreenStateReceiver.ScreenStateListener() {
            @Override
            public void onScreenOn() {
                if (isGrowing) {
                    tree.stopGrowth();
                    treeManager.saveTree(tree);
                    stopGrowthUpdates();
                    isGrowing = false;
                }
            }

            @Override
            public void onScreenOff() {
                if (!isGrowing) {
                    screenOffTime = System.currentTimeMillis();
                    tree.startGrowth();
                    startGrowthUpdates();
                    isGrowing = true;
                }
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, filter);
    }

    private void startGrowthUpdates() {
        stopGrowthUpdates();
        growthUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isGrowing && tree.isGrowing()) {
                    long currentTime = System.currentTimeMillis();
                    long growthDuration = currentTime - screenOffTime;
                    int secondsPassed = (int) (growthDuration / 1000);
                    if (secondsPassed >= 10) {
                        int minutesToAdd = secondsPassed / 10;

                        if (minutesToAdd > 0) {
                            SharedPreferences prefs = getApplicationContext().getSharedPreferences("growth_prefs", Context.MODE_PRIVATE);
                            int x = prefs.getInt("x", 60);
                            float motivation = prefs.getFloat("motivation", 1.0f);
                            int currentStage = tree.getCurrentStage();

                            if (currentStage < 6) {
                                int neededForNext = (int) (x * motivation * currentStage);
                                int currentProgress = tree.getProgressPercentage();
                                int minutesForCurrentStage = (currentProgress * neededForNext) / 100;
                                int remainingForNext = neededForNext - minutesForCurrentStage;

                                if (minutesToAdd > remainingForNext) {
                                    minutesToAdd = remainingForNext;
                                }
                            }

                            tree.addMinutes(30,x,motivation);
                            treeManager.saveTree(tree);
                            screenOffTime = currentTime;
                            Log.d(TAG, "Сервис: добавлено минут " + minutesToAdd);
                        }
                        growthHandler.postDelayed(this, 5000);
                    } else {
                        growthHandler.postDelayed(this, 1000);
                    }
                }
            }
        };
        growthHandler.post(growthUpdateRunnable);
    }

    private void stopGrowthUpdates() {
        if (growthUpdateRunnable != null) {
            growthHandler.removeCallbacks(growthUpdateRunnable);
            growthUpdateRunnable = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (screenReceiver != null) {
            unregisterReceiver(screenReceiver);
        }
        stopGrowthUpdates();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}