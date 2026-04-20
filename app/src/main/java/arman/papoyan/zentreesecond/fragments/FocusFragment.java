package arman.papoyan.zentreesecond.fragments;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import arman.papoyan.zentreesecond.R;
import arman.papoyan.zentreesecond.model.TreeModel;
import arman.papoyan.zentreesecond.utils.ScreenBlocker;
import arman.papoyan.zentreesecond.utils.TreeManager;

public class FocusFragment extends Fragment {

    private TextView tvTimer, tvStatus;
    private Button btnStartBreak, btnCancel;
    private Button btnTime5, btnTime15, btnTime25, btnTime45;
    private CountDownTimer countDownTimer;
    private ScreenBlocker screenBlocker;
    private boolean isBreakActive = false;
    private long selectedTimeMillis = 5 * 60 * 1000;
    private EditText etCustomTime;
    private Button btnSetCustom;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_focus, container, false);

        tvTimer = view.findViewById(R.id.tv_timer);
        tvStatus = view.findViewById(R.id.tv_status);
        btnStartBreak = view.findViewById(R.id.btn_start_break);
        btnCancel = view.findViewById(R.id.btn_cancel);

        btnTime5 = view.findViewById(R.id.btn_time_5);
        btnTime15 = view.findViewById(R.id.btn_time_15);
        btnTime25 = view.findViewById(R.id.btn_time_25);
        btnTime45 = view.findViewById(R.id.btn_time_45);

        etCustomTime = view.findViewById(R.id.et_custom_time);
        btnSetCustom = view.findViewById(R.id.btn_set_custom);

        screenBlocker = new ScreenBlocker(requireActivity());

        btnTime5.setOnClickListener(v -> selectTime(5 * 60 * 1000, "05:00"));
        btnTime15.setOnClickListener(v -> selectTime(15 * 60 * 1000, "15:00"));
        btnTime25.setOnClickListener(v -> selectTime(25 * 60 * 1000, "25:00"));
        btnTime45.setOnClickListener(v -> selectTime(45 * 60 * 1000, "45:00"));
        btnSetCustom.setOnClickListener(v -> {
            String timeStr = etCustomTime.getText().toString().trim();
            if (!timeStr.isEmpty()) {
                int minutes = Integer.parseInt(timeStr);
                if (minutes >= 1 && minutes <= 180) {
                    selectedTimeMillis = minutes * 60 * 1000L;
                    tvTimer.setText(String.format("%02d:%02d", minutes, 0));
                    updateTimeButtons();
                    etCustomTime.setText("");
                } else {
                    Toast.makeText(getActivity(), "Введите число от 1 до 180", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnStartBreak.setOnClickListener(v -> startBreak());
        btnCancel.setOnClickListener(v -> cancelBreak());

        updateTimeButtons();
        tvTimer.setText("05:00");

        return view;
    }

    private void selectTime(long millis, String displayText) {
        if (isBreakActive) return;
        selectedTimeMillis = millis;
        tvTimer.setText(displayText);
        updateTimeButtons();
    }

    private void updateTimeButtons() {
        resetButtonStyle(btnTime5);
        resetButtonStyle(btnTime15);
        resetButtonStyle(btnTime25);
        resetButtonStyle(btnTime45);

        if (selectedTimeMillis == 5 * 60 * 1000) setActiveButton(btnTime5);
        else if (selectedTimeMillis == 15 * 60 * 1000) setActiveButton(btnTime15);
        else if (selectedTimeMillis == 25 * 60 * 1000) setActiveButton(btnTime25);
        else if (selectedTimeMillis == 45 * 60 * 1000) setActiveButton(btnTime45);
    }

    private void resetButtonStyle(Button btn) {
        btn.setBackgroundTintList(getResources().getColorStateList(R.color.default_indicator));
        btn.setTextColor(getResources().getColor(R.color.text_primary));
    }

    private void setActiveButton(Button btn) {
        btn.setBackgroundTintList(getResources().getColorStateList(R.color.primary_green));
        btn.setTextColor(getResources().getColor(R.color.white));
    }

    private void startBreak() {
        if (isBreakActive) return;

        isBreakActive = true;
        btnStartBreak.setVisibility(View.GONE);
        btnCancel.setVisibility(View.VISIBLE);

        btnTime5.setVisibility(View.GONE);
        btnTime15.setVisibility(View.GONE);
        btnTime25.setVisibility(View.GONE);
        btnTime45.setVisibility(View.GONE);

        tvStatus.setText("Отдыхаем... Не пользуйтесь телефоном");
        tvStatus.setTextColor(getResources().getColor(R.color.primary_green));

        if (android.provider.Settings.canDrawOverlays(requireContext())) {
            screenBlocker.showBlocker("Отдых " + (selectedTimeMillis / 60000) + " минут\nНе пользуйтесь телефоном");
        }

        startTimer(selectedTimeMillis);
    }

    private void startTimer(long millis) {
        countDownTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                tvTimer.setText(String.format("%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                finishBreak();
            }
        }.start();
    }

    private void finishBreak() {
        if (screenBlocker.isShowing()) {
            screenBlocker.hideBlocker();
        }
        isBreakActive = false;
        btnStartBreak.setVisibility(View.VISIBLE);
        btnCancel.setVisibility(View.GONE);

        btnTime5.setVisibility(View.VISIBLE);
        btnTime15.setVisibility(View.VISIBLE);
        btnTime25.setVisibility(View.VISIBLE);
        btnTime45.setVisibility(View.VISIBLE);

        TreeManager treeManager = new TreeManager(requireActivity());
        TreeModel tree = treeManager.getCurrentTree();
        int bonusMinutes = (int) (selectedTimeMillis / 60000);
        tree.addBonusMinutes(bonusMinutes);
        treeManager.saveTree(tree);
        Toast.makeText(getActivity(), "🌳 +" + bonusMinutes + " минут к дереву!", Toast.LENGTH_SHORT).show();

        updateTimeButtons();
        tvTimer.setText(String.format("%02d:%02d", selectedTimeMillis / 60000, 0));
        tvStatus.setText("Отдых завершён! 🎉");
        tvStatus.setTextColor(getResources().getColor(R.color.text_secondary));
    }

    private void cancelBreak() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (screenBlocker.isShowing()) {
            screenBlocker.hideBlocker();
        }
        isBreakActive = false;
        btnStartBreak.setVisibility(View.VISIBLE);
        btnCancel.setVisibility(View.GONE);

        btnTime5.setVisibility(View.VISIBLE);
        btnTime15.setVisibility(View.VISIBLE);
        btnTime25.setVisibility(View.VISIBLE);
        btnTime45.setVisibility(View.VISIBLE);

        updateTimeButtons();
        tvTimer.setText(String.format("%02d:%02d", selectedTimeMillis / 60000, 0));
        tvStatus.setText("Отдых прерван");
        tvStatus.setTextColor(getResources().getColor(R.color.text_secondary));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (screenBlocker != null && screenBlocker.isShowing()) {
            screenBlocker.hideBlocker();
        }
    }
}