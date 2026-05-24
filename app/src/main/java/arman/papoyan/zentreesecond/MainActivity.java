package arman.papoyan.zentreesecond;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Locale;

import arman.papoyan.zentreesecond.fragments.FocusFragment;
import arman.papoyan.zentreesecond.fragments.HomeFragment;
import arman.papoyan.zentreesecond.fragments.LoginFragment;
import arman.papoyan.zentreesecond.fragments.ProfileFragment;
import arman.papoyan.zentreesecond.fragments.RegistrationFragment;
import arman.papoyan.zentreesecond.fragments.StatisticsFragment;
import arman.papoyan.zentreesecond.fragments.TasksFragment;
import arman.papoyan.zentreesecond.utils.TaskNotificationReceiver;
import arman.papoyan.zentreesecond.services.TrackerForegroundService;
import arman.papoyan.zentreesecond.utils.NotificationCleaner;
import arman.papoyan.zentreesecond.utils.SyncHelper;
import arman.papoyan.zentreesecond.utils.TreeManager;

public class MainActivity extends AppCompatActivity {

    public BottomNavigationView bottomNav;
    private Fragment currentFragment;
    private int currentNavItemId = R.id.nav_home;
    private static final int OVERLAY_PERMISSION_REQUEST = 100;
    private boolean isOnline = true;
    private Handler returnCheckHandler = new Handler();
    private Runnable returnCheckRunnable;
    private static final String KEY_NAV_ITEM = "current_nav_item";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("TestReceiver", "Экран: " + intent.getAction());
            }
        }, filter);

        SharedPreferences themePrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        int nightMode = themePrefs.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(nightMode);

        loadSavedLanguage();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);

        boolean keepLogin = getIntent().getBooleanExtra("keep_login", false);
        if (keepLogin) {
            getIntent().removeExtra("keep_login");
        }


        if (savedInstanceState != null) {
            currentNavItemId = savedInstanceState.getInt(KEY_NAV_ITEM, R.id.nav_home);
        } else {
            int savedNavId = getIntent().getIntExtra("selected_nav_id", -1);
            if (savedNavId != -1) {
                currentNavItemId = savedNavId;
                getIntent().removeExtra("selected_nav_id");
            }
        }

        if (getIntent().getBooleanExtra("logout", false)) {
            clearUserData();
            if (bottomNav != null) {
                bottomNav.setVisibility(View.GONE);
            }
            currentFragment = new LoginFragment();
            loadFragment(currentFragment, false);
            return;
        }

        checkOverlayPermission();
        checkUsageStatsPermission();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
        }

        updateNavbarText();

        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);

        getApplication().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(@NonNull Activity activity) {}

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                if (activity == MainActivity.this) {
                    stopReturnChecker();
                }
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {}

            @Override
            public void onActivityStopped(@NonNull Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {}
        });

        SharedPreferences regPrefs = getSharedPreferences("registration_prefs", MODE_PRIVATE);
        boolean isRegistering = regPrefs.getBoolean("is_registering", false);
        Log.d("MainActivity", "isRegistering = " + isRegistering);

        if (isRegistering) {
            if (bottomNav != null) {
                bottomNav.setVisibility(View.GONE);
            }
            currentFragment = new RegistrationFragment();
            loadFragment(currentFragment, false);
            return;
        }
        if (!keepLogin) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser != null) {
                bottomNav.setVisibility(View.VISIBLE);
            } else {
                bottomNav.setVisibility(View.GONE);
                currentFragment = new LoginFragment();
                loadFragment(currentFragment, false);
            }
        } else {
            bottomNav.setVisibility(View.VISIBLE);
            int savedNavId = getIntent().getIntExtra("selected_nav_id", R.id.nav_home);
            Fragment fragment = getFragmentForNavItem(savedNavId);
            currentFragment = fragment;
            loadFragment(fragment, false);
            bottomNav.setSelectedItemId(savedNavId);
        }


        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            if (bottomNav != null) {
                bottomNav.setVisibility(View.VISIBLE);
            }
            Fragment fragment = getFragmentForNavItem(currentNavItemId);
            currentFragment = fragment;
            loadFragment(fragment, false);
            setupNavigation();
            if (getIntent().getBooleanExtra("open_focus_tab", false)) {
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_focus);
                }
                getIntent().removeExtra("open_focus_tab");
            }
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(currentNavItemId);
            }
            syncPendingData();
        } else {
            if (bottomNav != null) {
                bottomNav.setVisibility(View.GONE);
            }
            currentFragment = new LoginFragment();
            loadFragment(currentFragment, false);
        }
    }

    private void clearUserData() {
        SharedPreferences loginPrefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        loginPrefs.edit().clear().apply();

        SharedPreferences regPrefs = getSharedPreferences("registration_prefs", MODE_PRIVATE);
        regPrefs.edit().clear().apply();

        NotificationCleaner.clearAllNotifications(this);
        FirebaseAuth.getInstance().signOut();
    }
    private void cancelAllTaskNotifications() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        Intent intent = new Intent(this, TaskNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
        Log.d("MainActivity", "Все уведомления отменены");
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_NAV_ITEM, currentNavItemId);
    }
    private void loadSavedLanguage() {
        SharedPreferences prefs = getSharedPreferences("settings_prefs", MODE_PRIVATE);
        String languageCode = prefs.getString("language", "ru");

        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);

        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        updateNavbarText();
    }

    private void syncPendingData() {
        TreeManager treeManager = new TreeManager(this);
        treeManager.syncPendingTree();
        Fragment tasksFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (tasksFragment instanceof TasksFragment) {
            ((TasksFragment) tasksFragment).syncPendingTasks();
        }
    }
    private void updateNavbarText() {
        if (bottomNav == null) return;

        bottomNav.getMenu().findItem(R.id.nav_home).setTitle(R.string.tree);
        bottomNav.getMenu().findItem(R.id.nav_tasks).setTitle(R.string.tasks);
        bottomNav.getMenu().findItem(R.id.nav_focus).setTitle(R.string.Focus);
        bottomNav.getMenu().findItem(R.id.nav_stats).setTitle(R.string.Stats);
        bottomNav.getMenu().findItem(R.id.nav_profile).setTitle(R.string.Profile);
    }
    private void startReturnChecker() {
        if (returnCheckRunnable != null) return;

        returnCheckRunnable = new Runnable() {
            private int retryCount = 0;

            @Override
            public void run() {
                Fragment currentFrag = getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_container);
                if (currentFrag instanceof FocusFragment) {
                    FocusFragment focusFragment = (FocusFragment) currentFrag;
                    boolean isBreakActive = focusFragment.isBreakActive();
                    boolean isOpeningDialer = focusFragment.isOpeningDialer();

                    if (!isBreakActive || !isOpeningDialer) {
                        stopReturnChecker();
                        return;
                    }

                    long dialerOpenedAt = focusFragment.getDialerOpenedAt();
                    long timeSinceDialer = System.currentTimeMillis() - dialerOpenedAt;
                    if (timeSinceDialer < 2000) {
                        returnCheckHandler.postDelayed(this, 1000);
                        return;
                    }

                    String currentPackage = getForegroundPackage();
                    boolean isDialer = isDialerPackage(currentPackage);
                    boolean isZenTree = getPackageName().equals(currentPackage);

                    if (!isDialer && !isZenTree) {
                        Log.d("MainActivity", "⚠️ Возвращаем (попытка " + (retryCount+1) + ") из: " + currentPackage);
                        boolean success = bringAppToFront();
                        if (success) {
                            stopReturnChecker();
                        } else {
                            retryCount++;
                            long delay = Math.min(3000, 1000 * retryCount);
                            returnCheckHandler.postDelayed(this, delay);
                        }
                        return;
                    }
                }
                returnCheckHandler.postDelayed(this, 1000);
            }
        };
        returnCheckHandler.post(returnCheckRunnable);
    }

    private void stopReturnChecker() {
        if (returnCheckRunnable != null) {
            returnCheckHandler.removeCallbacks(returnCheckRunnable);
            returnCheckRunnable = null;
        }
    }

    private String getForegroundPackage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
                long endTime = System.currentTimeMillis();
                long startTime = endTime - 5000;
                List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
                if (stats != null && !stats.isEmpty()) {
                    String topPackage = null;
                    long lastTime = 0;
                    for (UsageStats stat : stats) {
                        if (stat.getLastTimeUsed() > lastTime) {
                            lastTime = stat.getLastTimeUsed();
                            topPackage = stat.getPackageName();
                        }
                    }
                    return topPackage != null ? topPackage : "unknown";
                }
            } catch (Exception e) {
                Log.e("MainActivity", "UsageStats error: " + e.getMessage());
            }
        }
        return getCurrentForegroundPackageLegacy();
    }

    private String getCurrentForegroundPackageLegacy() {
        try {
            android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<android.app.ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
            if (!tasks.isEmpty()) {
                return tasks.get(0).topActivity.getPackageName();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "unknown";
    }

    private boolean isDialerPackage(String packageName) {
        if (packageName == null) return false;
        return packageName.equals("com.android.dialer") ||
                packageName.equals("com.google.android.dialer") ||
                packageName.equals("com.samsung.android.dialer") ||
                packageName.equals("com.oneplus.dialer") ||
                packageName.equals("com.xiaomi.dialer") ||
                packageName.equals("com.android.incallui") ||
                packageName.contains("dialer") ||
                packageName.contains("incallui");
    }

    private void checkUsageStatsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            long currentTime = System.currentTimeMillis();
            List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, currentTime - 5000, currentTime);
            if (stats == null || stats.isEmpty()) {
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                startActivity(intent);
                Toast.makeText(this, "Разрешите доступ к использованию приложений для корректной работы фокус-режима", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopReturnChecker();
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Intent intent = new Intent(this, TrackerForegroundService.class);
        stopService(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Без этого разрешения блокировка экрана не будет работать", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST);
        }
    }

    private Fragment getFragmentForNavItem(int navItemId) {
        if (navItemId == R.id.nav_home) return new HomeFragment();
        if (navItemId == R.id.nav_tasks) return new TasksFragment();
        if (navItemId == R.id.nav_focus) return new FocusFragment();
        if (navItemId == R.id.nav_stats) return new StatisticsFragment();
        if (navItemId == R.id.nav_profile) return new ProfileFragment();
        return new HomeFragment();
    }

    public void disableAllFirestoreListeners() {
        Fragment tasksFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (tasksFragment instanceof TasksFragment) {
            ((TasksFragment) tasksFragment).removeListener();
        }
    }

    private void setupNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            currentNavItemId = item.getItemId();
            int id = item.getItemId();
            Fragment fragment = null;

            if (id == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (id == R.id.nav_tasks) {
                fragment = new TasksFragment();
            } else if (id == R.id.nav_focus) {
                fragment = new FocusFragment();
            } else if (id == R.id.nav_stats) {
                fragment = new StatisticsFragment();
            } else if (id == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            if (fragment != null) {
                currentFragment = fragment;
                loadFragment(fragment, true);
            }
            return true;
        });
    }

    private void loadFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out);
        transaction.replace(R.id.fragment_container, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    public void goToHomeFragment() {
        bottomNav.setVisibility(View.VISIBLE);
        currentFragment = new HomeFragment();
        loadFragment(currentFragment, false);
        setupNavigation();
    }

    public void goToHomeAfterLogin() {
        bottomNav.setVisibility(View.VISIBLE);
        setupNavigation();
        currentFragment = new HomeFragment();
        loadFragment(currentFragment, false);
        bottomNav.setSelectedItemId(R.id.nav_home);
    }

    public void hideNavigation() {
        bottomNav.setVisibility(View.GONE);
    }

    public void switchAuthFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    public int getCurrentNavItemId() {
        return currentNavItemId;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Fragment currentFrag = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFrag instanceof FocusFragment) {
            FocusFragment focusFragment = (FocusFragment) currentFrag;
            focusFragment.onAppWindowFocusChanged(hasFocus);
            if (hasFocus) {
                focusFragment.setOpeningDialer(false);
                stopReturnChecker();
            }
        }
    }
    public void onDialerOpened() {
        startReturnChecker();
    }

    private boolean bringAppToFront() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.AppTask> tasks = am.getAppTasks();
            if (tasks != null && !tasks.isEmpty()) {
                tasks.get(0).moveToFront();
                return true;
            }
        } catch (Exception e) { /* DATARK */ }

        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (intent != null) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
        } catch (Exception e) { /* DATARK */ }
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            pendingIntent.send();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    private void checkConnectivity() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.Network currentNetwork = cm.getActiveNetwork();
        boolean wasOnline = isOnline;
        isOnline = currentNetwork != null;

        if (isOnline && !wasOnline) {
            new SyncHelper(this).syncAll();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        checkConnectivity();
        syncPendingData();
    }
}