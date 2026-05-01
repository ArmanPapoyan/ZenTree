package arman.papoyan.zentreesecond.utils;

import android.app.Activity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import arman.papoyan.zentreesecond.R;
import arman.papoyan.zentreesecond.fragments.FocusFragment;

public class ScreenBlocker {
    private WindowManager windowManager;
    private View overlayView;
    private Activity activity;
    private FocusFragment fragment;
    public ScreenBlocker(Activity activity, FocusFragment fragment) {
        this.activity = activity;
        this.fragment = fragment;
        windowManager = (WindowManager) activity.getSystemService(activity.WINDOW_SERVICE);
    }

    public void showBlocker(String message) {
        if (overlayView != null) return;

        LayoutInflater inflater = LayoutInflater.from(activity);
        overlayView = inflater.inflate(R.layout.layout_screen_blocker, null);
        TextView textView = overlayView.findViewById(R.id.tv_blocker_message);
        textView.setText(message);

        Button btnDialer = overlayView.findViewById(R.id.btn_open_dialer);
        btnDialer.setOnClickListener(v -> {
            hideBlockerWithoutStop();  // 👈 СНАЧАЛА СКРЫВАЕМ
            if (fragment != null) {
                fragment.openDialer();
            }
        });

        int layoutFlag;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                android.graphics.PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;

        overlayView.setOnTouchListener((v, event) -> {
            return true;
        });

        windowManager.addView(overlayView, params);
    }
    public boolean isShowing() {
        return overlayView != null && overlayView.isAttachedToWindow();
    }
    public void hideBlocker() {
        if (overlayView != null && overlayView.isAttachedToWindow()) {
            try {
                windowManager.removeView(overlayView);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            overlayView = null;
        }
    }
    public void hideBlockerWithoutStop() {
        if (overlayView != null) {
            try {
                if (overlayView.isAttachedToWindow()) {
                    windowManager.removeView(overlayView);
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            overlayView = null;
        }
    }
}