package arman.papoyan.zentreesecond.fragments;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import arman.papoyan.zentreesecond.R;
import arman.papoyan.zentreesecond.models.TreeModel;
import arman.papoyan.zentreesecond.services.TrackerForegroundService;
import arman.papoyan.zentreesecond.utils.NotificationHelper;
import arman.papoyan.zentreesecond.utils.ScreenStateReceiver;
import arman.papoyan.zentreesecond.utils.TreeManager;
import arman.papoyan.zentreesecond.workers.StartFocusWorker;
import arman.papoyan.zentreesecond.workers.UsageTrackerWorker;

public class HomeFragment extends Fragment implements ScreenStateReceiver.ScreenStateListener {
    private SharedPreferences dayPrefs;
    private FrameLayout treeContainer;
    private ImageView treeImage;
    private TextView treeLevelText;
    private TextView motivationText;
    private TextView growthStatusText;
    private ProgressBar treeProgressBar;
    private Button test;
    private TextView textViewTime;
    private TextView textViewProgress;
    private TreeManager treeManager;
    private TreeModel tree;
    private ScreenStateReceiver screenReceiver;
    private final boolean isFocusModeActive = true;
    private Handler growthHandler;
    private Runnable growthUpdateRunnable;
    private long screenOffTime = 0;
    private static final String TAG = "HomeFragment";
    private int continuousScreenMinutes = 0;
    private long screenOnStartTime = 0;
    private long lastScreenOffTime = 0;
    private NotificationHelper notificationHelper;
    private Handler continuousCheckHandler;
    private SharedPreferences notificationPrefs;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        treeContainer = view.findViewById(R.id.tree_container);
        treeImage = view.findViewById(R.id.tree_image);
        treeLevelText = view.findViewById(R.id.tree_level_text);
        motivationText = view.findViewById(R.id.motivation_text);
        growthStatusText = view.findViewById(R.id.growth_status_text);
        treeProgressBar = view.findViewById(R.id.tree_progress_bar);
        textViewTime = view.findViewById(R.id.text_view_time);
        textViewProgress = view.findViewById(R.id.text_view_progress);
        test = view.findViewById(R.id.test);

        treeManager = new TreeManager(requireActivity());
        tree = treeManager.loadTree();

        loadUserDataFromFirestore();

        dayPrefs = getActivity().getSharedPreferences("day_check", Context.MODE_PRIVATE);
        String lastOpenDate = dayPrefs.getString("last_open_date", "");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        notificationPrefs = requireActivity().getSharedPreferences("notification_prefs", Context.MODE_PRIVATE);
        continuousScreenMinutes = notificationPrefs.getInt("continuous_minutes", 0);

        if (!today.equals(lastOpenDate)) {
            dayPrefs.edit().putString("last_open_date", today).apply();
            tree.resetToDefault(today);
            treeManager.saveTree(tree);
            updateTreeUI();
            showNewDayAnimation();
            continuousScreenMinutes = 0;
            saveContinuousMinutes();
            saveNotificationSent(false);
        }
        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission();
        }
        growthHandler = new Handler();

        screenReceiver = new ScreenStateReceiver(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        requireActivity().registerReceiver(screenReceiver, filter);

        notificationHelper = new NotificationHelper(requireContext());
        screenOnStartTime = System.currentTimeMillis();
        startContinuousCheck();
        startWorkManager();
        startTrackerService();

        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(Color.parseColor("#E8F5E9"));
        drawable.setStroke(4, Color.parseColor("#C8E6C9"));
        treeContainer.setBackground(drawable);

        updateTreeImage(tree.getCurrentStage());
        updateTreeUI();

        test.setOnClickListener(v -> {
            SharedPreferences prefs = requireActivity().getSharedPreferences("growth_prefs", Context.MODE_PRIVATE);
            int x = prefs.getInt("x", 60);
            float motivation = prefs.getFloat("motivation", 1.0f);
            tree.addMinutes(30, x, motivation);
            updateTreeUI();
            Toast.makeText(getActivity(), "Тест: +30 минут к росту", Toast.LENGTH_LONG).show();
        });

        return view;
    }
    private void saveContinuousCheckRunning(boolean running) {
        notificationPrefs.edit().putBoolean("continuous_check_running", running).apply();
    }
    private boolean isContinuousCheckRunning() {
        return notificationPrefs.getBoolean("continuous_check_running", false);
    }

    private void saveContinuousMinutes() {
        notificationPrefs.edit().putInt("continuous_minutes", continuousScreenMinutes).apply();
    }

    private void saveNotificationSent(boolean sent) {
        notificationPrefs.edit().putBoolean("notification_sent", sent).apply();
    }

    private void startContinuousCheck() {
        if (continuousCheckHandler != null) return;
        continuousCheckHandler = new Handler();
        continuousCheckHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (screenOnStartTime > 0) {
                    continuousScreenMinutes++;
                    saveContinuousMinutes();
                    Log.d(TAG, "Минута использования: " + continuousScreenMinutes);
                    checkAndSendNotification();
                }
                if (continuousCheckHandler != null) {
                    continuousCheckHandler.postDelayed(this, 60000);
                }
            }
        }, 60000);
    }


    private int getCurrentScreenTime() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) requireActivity().getSystemService(Context.USAGE_STATS_SERVICE);
        long endTime = System.currentTimeMillis();
        long startTime = endTime - 24 * 60 * 60 * 1000;
        Map<String, UsageStats> stats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime);
        long totalTime = 0;
        for (UsageStats usageStats : stats.values()) {
            totalTime += usageStats.getTotalTimeInForeground();
        }
        return (int) (totalTime / (60 * 60 * 1000));
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) requireActivity().getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), requireActivity().getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void requestUsageStatsPermission() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Разрешение на отслеживание экранного времени")
                .setMessage("Для работы мотивационного коэффициента приложению нужен доступ к статистике использования. Вы можете включить его в настройках.")
                .setPositiveButton("Открыть настройки", (dialog, which) -> startActivity(new Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void loadUserDataFromFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String wakeUpTime = documentSnapshot.getString("wakeUpTime");
                        Long screenTimeGoal = documentSnapshot.getLong("screenTimeGoal");
                        if (wakeUpTime != null && screenTimeGoal != null) {
                            saveUserDataToPrefs(wakeUpTime, screenTimeGoal);
                            calculateGrowthParameters(wakeUpTime, screenTimeGoal);
                            scheduleFocusAtWakeUpTime(wakeUpTime);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Ошибка загрузки данных: " + e.getMessage()));
    }

    private void saveUserDataToPrefs(String wakeUpTime, long screenTimeGoal) {
        requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("wakeUpTime", wakeUpTime)
                .putLong("screenTimeGoal", screenTimeGoal)
                .apply();
    }

    private void scheduleFocusAtWakeUpTime(String wakeUpTime) {
        String[] parts = wakeUpTime.split(":");
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
        calendar.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
        calendar.set(Calendar.SECOND, 0);
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        long delay = calendar.getTimeInMillis() - System.currentTimeMillis();
        WorkManager.getInstance(requireContext()).enqueue(new OneTimeWorkRequest.Builder(StartFocusWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build());
    }

    private void calculateGrowthParameters(String wakeUpTime, long screenTimeGoal) {
        int wakeUpHour = Integer.parseInt(wakeUpTime.split(":")[0]);
        int totalMinutesLeft = (24 - wakeUpHour) * 60;
        int x = totalMinutesLeft / 15;
        int currentScreenTime = getCurrentScreenTime();
        double motivation = 1 + (double) (currentScreenTime - screenTimeGoal) / screenTimeGoal;
        if (motivation < 0.5) motivation = 0.5;
        if (motivation > 3.0) motivation = 3.0;
        requireActivity().getSharedPreferences("growth_prefs", Context.MODE_PRIVATE)
                .edit()
                .putInt("x", x)
                .putFloat("motivation", (float) motivation)
                .putInt("wakeUpHour", wakeUpHour)
                .apply();
    }

    @Override
    public void onScreenOff() {
        lastScreenOffTime = System.currentTimeMillis();
        if (isFocusModeActive) {
            screenOffTime = System.currentTimeMillis();
            tree.startGrowth();
            growthStatusText.setText("🌱 Дерево растёт (экран выключен)");
            growthStatusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_green));
            startGrowthUpdates();
        }
    }

    @Override
    public void onScreenOn() {
        long now = System.currentTimeMillis();
        if (lastScreenOffTime > 0 && (now - lastScreenOffTime) >= 5 * 60 * 1000) {
            notificationPrefs.edit().putLong("last_notification_time", 0).apply();
        }

        screenOnStartTime = now;
        if (isFocusModeActive && tree.isGrowing()) {
            tree.stopGrowth();
            treeManager.saveTree(tree);
            updateTreeUI();
            growthStatusText.setText("⏸️ Рост на паузе (экран включён)");
            growthStatusText.setTextColor(Color.parseColor("#FF9800"));
            stopGrowthUpdates();
        }
    }

    private void checkAndSendNotification() {
        long now = System.currentTimeMillis();
        long lastNotify = notificationPrefs.getLong("last_notification_time", 0);
        if (now - lastNotify >= 60 * 60 * 1000) {
            notificationHelper.sendBreakReminder();
            notificationPrefs.edit().putLong("last_notification_time", now).apply();
            saveNotificationSent(false);
        }
        if (!hasUsageStatsPermission()) {
            Log.d("TrackerService", "Нет разрешения PACKAGE_USAGE_STATS");
            return;
        }
    }

    private void startGrowthUpdates() {
        stopGrowthUpdates();
        growthUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isFocusModeActive && tree.isGrowing()) {
                    long growthDuration = System.currentTimeMillis() - screenOffTime;
                    int secondsPassed = (int) (growthDuration / 1000);
                    if (secondsPassed >= 10) {
                        int minutesToAdd = secondsPassed / 10;
                        if (minutesToAdd > 0) {
                            SharedPreferences prefs = requireActivity().getSharedPreferences("growth_prefs", Context.MODE_PRIVATE);
                            int x = prefs.getInt("x", 60);
                            float motivation = prefs.getFloat("motivation", 1.0f);
                            int currentStage = tree.getCurrentStage();
                            if (currentStage < 6) {
                                int neededForNext = (int) (x * motivation * currentStage);
                                int currentProgress = tree.getProgressPercentage();
                                int remainingForNext = neededForNext - (currentProgress * neededForNext / 100);
                                if (minutesToAdd > remainingForNext) minutesToAdd = remainingForNext;
                            }
                            tree.addMinutes(minutesToAdd, x, motivation);
                            treeManager.saveTree(tree);
                            updateTreeUI();
                            screenOffTime = System.currentTimeMillis();
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

    private void updateTreeUI() {
        int totalMinutes = tree.getTotalMinutes();
        int progress = tree.getProgressPercentage();
        treeLevelText.setText("Уровень " + tree.getLevel() + " • Стадия " + tree.getCurrentStage());
        treeProgressBar.setProgress(progress);
        textViewTime.setText((totalMinutes / 60) + " ч " + (totalMinutes % 60) + " мин");
        textViewProgress.setText(progress + "%");
        updateTreeImage(tree.getCurrentStage());
        updateMotivationText();
    }

    private void updateTreeImage(int stage) {
        int drawableId;
        switch (stage) {
            case 1: drawableId = R.drawable.tree_stage1; break;
            case 2: drawableId = R.drawable.tree_stage2; break;
            case 3: drawableId = R.drawable.tree_stage3; break;
            case 4: drawableId = R.drawable.tree_stage4; break;
            case 5: drawableId = R.drawable.tree_stage5; break;
            case 6: drawableId = R.drawable.tree_stage6; break;
            default: drawableId = R.drawable.tree_stage1;
        }
        if (treeImage.getTag() == null || !treeImage.getTag().equals(stage)) {
            treeImage.setImageResource(drawableId);
            treeImage.setTag(stage);
        }
    }

    private void updateMotivationText() {
        String[] motivations = {
                getString(R.string.Q1), getString(R.string.Q2), getString(R.string.Q3),
                getString(R.string.Q4), getString(R.string.Q5), getString(R.string.Q6)
        };
        motivationText.setText(motivations[new Random().nextInt(motivations.length)]);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopGrowthUpdates();
        if (continuousCheckHandler != null) {
            continuousCheckHandler.removeCallbacksAndMessages(null);
            continuousCheckHandler = null;
        }
        if (growthHandler != null) {
            growthHandler.removeCallbacksAndMessages(null);
        }
        if (screenReceiver != null) {
            try {
                requireActivity().unregisterReceiver(screenReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при отписке от receiver", e);
            }
        }
        saveContinuousCheckRunning(false);
    }

    private void showNewDayAnimation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.TransparentDialog);
        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_new_day, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();

        TextView emojiView = dialogView.findViewById(R.id.text_view_new_day);
        TextView titleView = dialogView.findViewById(R.id.text_view_title);
        TextView messageView = dialogView.findViewById(R.id.text_view_message);

        emojiView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(500).setInterpolator(new OvershootInterpolator()).start();
        titleView.animate().alpha(1f).setDuration(400).setStartDelay(300).start();
        messageView.animate().alpha(1f).setDuration(400).setStartDelay(600).withEndAction(() -> new Handler().postDelayed(dialog::dismiss, 1500)).start();
    }
    private void startWorkManager() {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                UsageTrackerWorker.class,
                15, TimeUnit.MINUTES).build();
        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork("usage_tracker", ExistingPeriodicWorkPolicy.KEEP, workRequest);
    }
    private void startTrackerService() {
        Intent intent = new Intent(requireContext(), TrackerForegroundService.class);
        requireContext().startService(intent);
    }
}