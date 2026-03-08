package arman.papoyan.zentreesecond.fragments;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import java.util.Random;
import arman.papoyan.zentreesecond.R;
import arman.papoyan.zentreesecond.model.TreeModel;
import arman.papoyan.zentreesecond.utils.ScreenStateReceiver;
import arman.papoyan.zentreesecond.utils.TreeManager;

public class HomeFragment extends Fragment implements ScreenStateReceiver.ScreenStateListener {

    private FrameLayout treeContainer;
    private ImageView treeImage;
    private ImageView treeGrowthOverlay;
    private TextView treeLevelText;
    private TextView motivationText;
    private TextView growthStatusText;
    private ProgressBar treeProgressBar;
    private Button startFocusButton;

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
        startFocusButton = view.findViewById(R.id.btn_start_focus);
        growthStatusText = view.findViewById(R.id.growth_status_text);

        treeManager = new TreeManager(requireActivity());
        tree = treeManager.loadTree();

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

        startFocusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFocusMode();
            }
        });

        updateUIFromState();

        int currentStage = tree.getCurrentStage();
        updateTreeImage(currentStage);
        updateTreeUI();

        return view;
    }

    private void toggleFocusMode() {
        if (!isFocusModeActive) {
            startFocusMode();
        } else {
            stopFocusMode();
        }
    }

    private void startFocusMode() {
        isFocusModeActive = true;
        updateUIFromState();
        Log.d(TAG, "Фокус-режим ВКЛЮЧЕН");
        Toast.makeText(getActivity(),
                "Фокус-режим включен! Выключи экран, чтобы дерево росло.",
                Toast.LENGTH_LONG).show();
    }

    private void stopFocusMode() {
        isFocusModeActive = false;
        tree.stopGrowth();
        treeManager.saveTree(tree);
        stopGrowthUpdates();
        updateUIFromState();
        Log.d(TAG, "Фокус-режим ВЫКЛЮЧЕН");
        Toast.makeText(getActivity(),
                "Фокус-режим выключен.",
                Toast.LENGTH_SHORT).show();
    }

    private void updateUIFromState() {
        if (isFocusModeActive) {
            startFocusButton.setText("Остановить рост");
            growthStatusText.setText("✅ Фокус-режим включён");
            growthStatusText.setTextColor(Color.parseColor("#388E3C"));
        } else {
            startFocusButton.setText("Начать фокус-сессию");
            growthStatusText.setText("💤 Фокус-режим выключен");
            growthStatusText.setTextColor(Color.DKGRAY);
        }
    }

    @Override
    public void onScreenOff() {
        Log.d(TAG, "Экран ВЫКЛЮЧЕН");
        if (isFocusModeActive) {
            screenOffTime = System.currentTimeMillis();
            tree.startGrowth();
            growthStatusText.setText("🌱 Дерево растёт (экран выключен)");
            growthStatusText.setTextColor(Color.parseColor("#388E3C"));
            startGrowthUpdates();
            Toast.makeText(getActivity(),
                    "Экран выключен - дерево начинает расти!",
                    Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getActivity(),
                    "Экран включен - рост остановлен",
                    Toast.LENGTH_SHORT).show();
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
                        int experienceToAdd = secondsPassed / 10;
                        if (experienceToAdd > 0) {
                            tree.addExperience(experienceToAdd);
                            treeManager.saveTree(tree);
                            updateTreeUI();
                            screenOffTime = currentTime;
                            Log.d(TAG, "Добавлено опыта: " + experienceToAdd);
                        }
                    }
                    growthHandler.postDelayed(this, 5000);
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
        int currentStage = tree.getCurrentStage();
        int progress = tree.getProgressPercentage();
        treeLevelText.setText("Уровень " + tree.getLevel() + " • Стадия " + currentStage);
        treeProgressBar.setProgress(progress);
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
                "Каждое маленькое усилие ведёт к большим переменам 🌱",
                "Твой фокус — твоя сила 💪",
                "Дерево растёт вместе с тобой 🌳",
                "Один шаг за раз, но всегда вперёд 🚶‍♂️",
                "Сегодняшние семена — завтрашний лес 🌲",
                "Каждая минута фокуса делает тебя сильнее ⏰"
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
}