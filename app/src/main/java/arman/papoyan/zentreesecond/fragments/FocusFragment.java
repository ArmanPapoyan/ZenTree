package arman.papoyan.zentreesecond.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import arman.papoyan.zentreesecond.MainActivity;
import arman.papoyan.zentreesecond.R;
import arman.papoyan.zentreesecond.models.TreeModel;
import arman.papoyan.zentreesecond.utils.ScreenBlocker;
import arman.papoyan.zentreesecond.utils.TreeManager;

public class FocusFragment extends Fragment {

    private TextView tvTimer, tvStatus;
    private Button btnStartBreak, btnCancel;
    private Button btnTime5, btnTime15, btnTime25, btnTime45;
    private CountDownTimer countDownTimer;
    private ScreenBlocker screenBlocker;
    public boolean isBreakActive = false;
    private long selectedTimeMillis = 5 * 60 * 1000;
    private EditText etCustomTime;
    private Button btnSetCustom;
    private boolean waitingForDialerReturn = false;
    private ActivityResultLauncher<Intent> dialerLauncher;
    private boolean isOpeningDialer = false;
    private long dialerOpenedAt = 0;
    private static final String PREFS_BREAK = "break_state";
    private static final String KEY_BREAK_ACTIVE = "break_active";
    private static final String KEY_SELECTED_TIME = "selected_time";
    private static final String KEY_REMAINING_TIME = "remaining_time";
    private static final String KEY_DIALER_OPENED_AT = "dialer_opened_at";
    private long remainingTimeMillis = 0;
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

        screenBlocker = new ScreenBlocker(requireActivity(), this);

        dialerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (isBreakActive && !screenBlocker.isShowing()) {
                        screenBlocker.showBlocker(getString(R.string.blocker_break_message, selectedTimeMillis / 60000));
                    }
                    waitingForDialerReturn = false;
                }
        );

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
                    Toast.makeText(getActivity(), getString(R.string.error_invalid_custom_time), Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnStartBreak.setOnClickListener(v -> startBreak());
        btnCancel.setOnClickListener(v -> cancelBreak());

        restoreBreakState();
        if (!isBreakActive) {
            updateTimeButtons();
            tvTimer.setText("05:00");
        }

        updateTimeButtons();
        tvTimer.setText("05:00");

        return view;
    }
    private void saveBreakState() {
        if (!isBreakActive) return;
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_BREAK, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_BREAK_ACTIVE, true);
        editor.putLong(KEY_SELECTED_TIME, selectedTimeMillis);
        editor.putLong(KEY_DIALER_OPENED_AT, dialerOpenedAt);

        long remaining = 0;
        if (countDownTimer != null) {
            remaining = remainingTimeMillis;
        }
        editor.putLong(KEY_REMAINING_TIME, remaining);
        editor.apply();
    }
    private void clearBreakState() {
        requireActivity().getSharedPreferences(PREFS_BREAK, Context.MODE_PRIVATE).edit().clear().apply();
    }
    private void restoreBreakState() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_BREAK, Context.MODE_PRIVATE);
        boolean wasBreakActive = prefs.getBoolean(KEY_BREAK_ACTIVE, false);
        if (!wasBreakActive) return;
        selectedTimeMillis = prefs.getLong(KEY_SELECTED_TIME, 5 * 60 * 1000);
        dialerOpenedAt = prefs.getLong(KEY_DIALER_OPENED_AT, 0);
        long remaining = prefs.getLong(KEY_REMAINING_TIME, selectedTimeMillis);
        if (remaining <= 0) {
            clearBreakState();
            return;
        }
        isBreakActive = true;
        btnStartBreak.setVisibility(View.GONE);
        btnCancel.setVisibility(View.VISIBLE);
        btnTime5.setVisibility(View.GONE);
        btnTime15.setVisibility(View.GONE);
        btnTime25.setVisibility(View.GONE);
        btnTime45.setVisibility(View.GONE);
        tvStatus.setText(getString(R.string.status_resting));
        tvStatus.setTextColor(getResources().getColor(R.color.primary_green));
        startTimer(remaining);
        if (Settings.canDrawOverlays(requireContext())) {
            screenBlocker.showBlocker(getString(R.string.blocker_break_message, selectedTimeMillis / 60000));
        }
        clearBreakState();
    }

    public void openDialer() {
        isOpeningDialer = true;
        dialerOpenedAt = System.currentTimeMillis();
        if (screenBlocker.isShowing()) {
            screenBlocker.setTransparent(true);
        }
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).onDialerOpened();
        }
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
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

        tvStatus.setText(getString(R.string.status_resting));
        tvStatus.setTextColor(getResources().getColor(R.color.primary_green));

        if (android.provider.Settings.canDrawOverlays(requireContext())) {
            screenBlocker.showBlocker(getString(R.string.blocker_break_message, selectedTimeMillis / 60000));
        }

        startTimer(selectedTimeMillis);
    }

    private void startTimer(long millis) {
        countDownTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingTimeMillis = millisUntilFinished;
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
    @Override
    public void onPause() {
        super.onPause();
        if (isBreakActive) {
            saveBreakState();
        }
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
        Toast.makeText(getActivity(), getString(R.string.toast_bonus_minutes, bonusMinutes), Toast.LENGTH_SHORT).show();

        updateTimeButtons();
        tvTimer.setText(String.format("%02d:%02d", selectedTimeMillis / 60000, 0));
        tvStatus.setText(getString(R.string.status_break_finished));
        tvStatus.setTextColor(getResources().getColor(R.color.text_secondary));

        isOpeningDialer = false;
        waitingForDialerReturn = false;
        clearBreakState();
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
        tvStatus.setText(getString(R.string.status_break_cancelled));
        tvStatus.setTextColor(getResources().getColor(R.color.text_secondary));

        isOpeningDialer = false;
        waitingForDialerReturn = false;
        clearBreakState();
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
        if (isBreakActive) {
            saveBreakState();
        }
    }
    public void onAppWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            isOpeningDialer = false;
            waitingForDialerReturn = false;

            if (isBreakActive) {
                if (screenBlocker.isShowing()) {
                    screenBlocker.setTransparent(false);
                } else {
                    screenBlocker.showBlocker(getString(R.string.blocker_break_message, selectedTimeMillis / 60000));
                }
            }
        }
    }


    public boolean isBreakActive() {
        return isBreakActive;
    }
    public long getDialerOpenedAt() {
        return dialerOpenedAt;
    }
    public boolean isOpeningDialer() {
        return isOpeningDialer;
    }

    public void setOpeningDialer(boolean opening) {
        this.isOpeningDialer = opening;
    }
    @Override
    public void onResume() {
        super.onResume();
        if (isBreakActive && !screenBlocker.isShowing()) {
            screenBlocker.showBlocker(getString(R.string.blocker_break_message, selectedTimeMillis / 60000));
        }
    }
}