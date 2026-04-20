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
import androidx.work.OneTimeWorkRequest;
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
import arman.papoyan.zentreesecond.model.TreeModel;
import arman.papoyan.zentreesecond.utils.ScreenStateReceiver;
import arman.papoyan.zentreesecond.utils.TreeManager;
import arman.papoyan.zentreesecond.workers.StartFocusWorker;

public class HomeFragment extends Fragment implements ScreenStateReceiver.ScreenStateListener {
    private SharedPreferences dayPrefs;
    private FrameLayout treeContainer;
    private ImageView treeImage;
    private ImageView treeGrowthOverlay;
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

    private boolean isFocusModeActive = false;
    private Handler growthHandler;
    private Runnable growthUpdateRunnable;
    private long screenOffTime = 0;

    private static final String TAG = "HomeFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        treeContainer = view.findViewById(R.id.tree_container);
        treeImage = view.findViewById(R.id.tree_image);
        treeGrowthOverlay = view.findViewById(R.id.tree_growth_overlay);
        treeLevelText = view.findViewById(R.id.tree_level_text);
        motivationText = view.findViewById(R.id.motivation_text);
        treeProgressBar = view.findViewById(R.id.tree_progress_bar);
        growthStatusText = view.findViewById(R.id.growth_status_text);
        textViewTime = view.findViewById(R.id.text_view_time);
        textViewProgress = view.findViewById(R.id.text_view_progress);
        test = view.findViewById(R.id.test);

        treeManager = new TreeManager(requireActivity());
        tree = treeManager.loadTree();

        loadUserDataFromFirestore();
        isFocusModeActive = true;
        dayPrefs = getActivity().getSharedPreferences("day_check", Context.MODE_PRIVATE);
        String lastOpenDate = dayPrefs.getString("last_open_date", "");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (!today.equals(lastOpenDate)) {
            dayPrefs.edit().putString("last_open_date", today).apply();
            tree.resetToDefault(today);
            treeManager.saveTree(tree);
            updateTreeUI();
            showNewDayAnimation();
        }

        growthHandler = new Handler();

        screenReceiver = new ScreenStateReceiver(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        requireActivity().registerReceiver(screenReceiver, filter);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(Color.parseColor("#E8F5E9"));
        drawable.setStroke(4, Color.parseColor("#C8E6C9"));
        treeContainer.setBackground(drawable);


        int currentStage = tree.getCurrentStage();
        updateTreeImage(currentStage);
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
    private int getCurrentScreenTime() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) requireActivity().getSystemService(Context.USAGE_STATS_SERVICE);
        long endTime = System.currentTimeMillis();
        long startTime = endTime - 24 * 60 * 60 * 1000; // последние 24 часа

        Map<String, UsageStats> stats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime);
        long totalTime = 0;

        for (UsageStats usageStats : stats.values()) {
            totalTime += usageStats.getTotalTimeInForeground();
        }

        return (int) (totalTime / (60 * 60 * 1000)); // конвертируем в часы
    }
    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) requireActivity().getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), requireActivity().getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
    private void requestUsageStatsPermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Разрешение на отслеживание экранного времени")
                .setMessage("Для работы мотивационного коэффициента приложению нужен доступ к статистике использования. Вы можете включить его в настройках.")
                .setPositiveButton("Открыть настройки", (dialog, which) -> {
                    startActivity(new Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS));
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
    private void loadUserDataFromFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String wakeUpTime = documentSnapshot.getString("wakeUpTime");
                        Long screenTimeGoal = documentSnapshot.getLong("screenTimeGoal");

                        if (wakeUpTime != null && screenTimeGoal != null) {
                            saveUserDataToPrefs(wakeUpTime, screenTimeGoal);
                            calculateGrowthParameters(wakeUpTime, screenTimeGoal);
                            scheduleFocusAtWakeUpTime(wakeUpTime);
                        } else {
                            Log.e(TAG, "Данные не найдены: wakeUpTime или screenTimeGoal = null");
                        }
                    } else {
                        Log.e(TAG, "Документ пользователя не существует");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка загрузки данных: " + e.getMessage());
                });
    }
    private void saveUserDataToPrefs(String wakeUpTime, long screenTimeGoal) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("wakeUpTime", wakeUpTime)
                .putLong("screenTimeGoal", screenTimeGoal)
                .apply();

        Log.d(TAG, "Данные сохранены: wakeUpTime=" + wakeUpTime + ", goal=" + screenTimeGoal);
    }
    private void scheduleFocusAtWakeUpTime(String wakeUpTime) {
        String[] parts = wakeUpTime.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        long delay = calendar.getTimeInMillis() - System.currentTimeMillis();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(StartFocusWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(requireContext()).enqueue(workRequest);
    }
    private void calculateGrowthParameters(String wakeUpTime, long screenTimeGoal) {
        String[] parts = wakeUpTime.split(":");
        int wakeUpHour = Integer.parseInt(parts[0]);

        int hoursLeft = 24 - wakeUpHour;

        int totalMinutesLeft = hoursLeft * 60;
        int x = totalMinutesLeft / 15;

        int currentScreenTime = getCurrentScreenTime();


        double motivation = 1 + (double)(currentScreenTime - screenTimeGoal) / screenTimeGoal;
        if (motivation < 0.5) motivation = 0.5;
        if (motivation > 3.0) motivation = 3.0;

        SharedPreferences prefs = requireActivity().getSharedPreferences("growth_prefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putInt("x", x)
                .putFloat("motivation", (float) motivation)
                .putInt("wakeUpHour", wakeUpHour)
                .apply();

        Log.d(TAG, "x = " + x + " мин, мотивация = " + motivation);
    }
    @Override
    public void onScreenOff() {
        Log.d(TAG, "Экран ВЫКЛЮЧЕН");
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
        Log.d(TAG, "Экран ВКЛЮЧЕН");
        if (isFocusModeActive && tree.isGrowing()) {
            tree.stopGrowth();
            treeManager.saveTree(tree);
            updateTreeUI();
            growthStatusText.setText("⏸️ Рост на паузе (экран включён)");
            growthStatusText.setTextColor(Color.parseColor("#FF9800"));
            stopGrowthUpdates();
        }
    }
    private void startGrowthUpdates() {
        stopGrowthUpdates();
        growthUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isFocusModeActive && tree.isGrowing()) {
                    long currentTime = System.currentTimeMillis();
                    long growthDuration = currentTime - screenOffTime;
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
                                int minutesForCurrentStage = (currentProgress * neededForNext) / 100;
                                int remainingForNext = neededForNext - minutesForCurrentStage;

                                if (minutesToAdd > remainingForNext) {
                                    minutesToAdd = remainingForNext;
                                }
                            }

                            tree.addMinutes(minutesToAdd,x,motivation);
                            treeManager.saveTree(tree);
                            updateTreeUI();
                            screenOffTime = currentTime;
                            Log.d(TAG, "Добавлено минут: " + minutesToAdd);
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
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        int progress = tree.getProgressPercentage();
        int currentStage = tree.getCurrentStage();

        treeLevelText.setText("Уровень " + tree.getLevel() + " • Стадия " + currentStage);
        treeProgressBar.setProgress(progress);
        textViewTime.setText(hours + " ч " + minutes + " мин");
        textViewProgress.setText(progress + "%");

        updateTreeImage(currentStage);
        updateMotivationText();
    }

    private void updateTreeImage(int newStage) {
        Object currentTag = treeImage.getTag();
        if (currentTag == null || !currentTag.equals(newStage)) {
            int drawableId = getTreeImageResource(newStage);
            treeImage.setImageResource(drawableId);
            treeImage.setTag(newStage);
            Log.d(TAG, "Установлена картинка стадии " + newStage);
        }
    }

    private int getTreeImageResource(int stage) {
        switch (stage) {
            case 1: return R.drawable.tree_stage1;
            case 2: return R.drawable.tree_stage2;
            case 3: return R.drawable.tree_stage3;
            case 4: return R.drawable.tree_stage4;
            case 5: return R.drawable.tree_stage5;
            case 6: return R.drawable.tree_stage6;
            default: return R.drawable.tree_stage1;
        }
    }

    private void updateMotivationText() {
        String[] motivations = {
                getString(R.string.Q1),
                getString(R.string.Q2),
                getString(R.string.Q3),
                getString(R.string.Q4),
                getString(R.string.Q5),
                getString(R.string.Q6)
        };
        Random random = new Random();
        String motivation = motivations[random.nextInt(motivations.length)];
        motivationText.setText(motivation);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopGrowthUpdates();
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
    }
    private void showNewDayAnimation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.TransparentDialog);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_new_day, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();

        TextView emojiView = dialogView.findViewById(R.id.text_view_new_day);
        TextView titleView = dialogView.findViewById(R.id.text_view_title);
        TextView messageView = dialogView.findViewById(R.id.text_view_message);

        emojiView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator())
                .start();

        titleView.animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(300)
                .start();

        messageView.animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(600)
                .withEndAction(() -> {
                    new Handler().postDelayed(() -> dialog.dismiss(), 1500);
                })
                .start();
    }
}