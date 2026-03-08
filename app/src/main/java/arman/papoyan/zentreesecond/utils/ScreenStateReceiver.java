package arman.papoyan.zentreesecond.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ScreenStateReceiver extends BroadcastReceiver {

    private static final String TAG = "ScreenStateReceiver";
    private ScreenStateListener listener;

    public interface ScreenStateListener {
        void onScreenOn();
        void onScreenOff();
    }

    public ScreenStateReceiver(ScreenStateListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        switch (action) {
            case Intent.ACTION_SCREEN_ON:
                Log.d(TAG, "ACTION_SCREEN_ON detected");
                if (listener != null) {
                    listener.onScreenOn();
                }
                break;
            case Intent.ACTION_SCREEN_OFF:
                Log.d(TAG, "ACTION_SCREEN_OFF detected");
                if (listener != null) {
                    listener.onScreenOff();
                }
                break;
        }
    }
}