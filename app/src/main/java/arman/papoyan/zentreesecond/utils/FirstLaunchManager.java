package arman.papoyan.zentreesecond.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class FirstLaunchManager {
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "first_launch_prefs";
    private static final String KEY_FIRST_LAUNCH = "is_first_launch";

    public FirstLaunchManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isFirstLaunch() {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true);
    }

    public void setFirstLaunchDone() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
    }
}