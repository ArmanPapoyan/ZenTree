package arman.papoyan.zentreesecond.utils;
import android.app.Activity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import arman.papoyan.zentreesecond.R;

public class ScreenBlocker {
    private WindowManager windowManager;
    private View overlayView;
    private Activity activity;

    public ScreenBlocker(Activity activity) {
        this.activity = activity;
        windowManager = (WindowManager) activity.getSystemService(activity.WINDOW_SERVICE);
    }

    public void showBlocker(String message) {
        if (overlayView != null) return;

        LayoutInflater inflater = LayoutInflater.from(activity);
        overlayView = inflater.inflate(R.layout.layout_screen_blocker, null);
        TextView textView = overlayView.findViewById(R.id.tv_blocker_message);
        textView.setText(message);

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

    public void hideBlocker() {
        if (overlayView != null && overlayView.isAttachedToWindow()) {
            windowManager.removeView(overlayView);
            overlayView = null;
        }
    }

    public boolean isShowing() {
        return overlayView != null && overlayView.isAttachedToWindow();
    }
}