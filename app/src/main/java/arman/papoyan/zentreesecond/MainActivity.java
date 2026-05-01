package arman.papoyan.zentreesecond;

import android.app.Activity;
import android.app.Application;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
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

import arman.papoyan.zentreesecond.fragments.FocusFragment;
import arman.papoyan.zentreesecond.fragments.HomeFragment;
import arman.papoyan.zentreesecond.fragments.LoginFragment;
import arman.papoyan.zentreesecond.fragments.NoInternetFragment;
import arman.papoyan.zentreesecond.fragments.ProfileFragment;
import arman.papoyan.zentreesecond.fragments.RegistrationFragment;
import arman.papoyan.zentreesecond.fragments.StatisticsFragment;
import arman.papoyan.zentreesecond.fragments.TasksFragment;
import arman.papoyan.zentreesecond.services.TrackerForegroundService;
import arman.papoyan.zentreesecond.utils.NetworkUtils;

public class MainActivity extends AppCompatActivity {

    public BottomNavigationView bottomNav;
    private Fragment currentFragment;
    private int currentNavItemId = R.id.nav_home;
    private NetworkCallback networkCallback;
    private static final int OVERLAY_PERMISSION_REQUEST = 100;

    private Handler returnCheckHandler = new Handler();
    private Runnable returnCheckRunnable;

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

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkOverlayPermission();
        checkUsageStatsPermission();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
        }

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new NetworkCallback(this);
        cm.registerDefaultNetworkCallback(networkCallback);
        bottomNav = findViewById(R.id.bottom_navigation);

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
            public void onActivityPaused(@NonNull Activity activity) {
                if (activity == MainActivity.this) {
                    startReturnChecker();
                }
            }

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
            bottomNav.setVisibility(View.GONE);
            currentFragment = new RegistrationFragment();
            loadFragment(currentFragment, false);
            return;
        }

        if (NetworkUtils.isInternetAvailable(this)) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            Log.d("MainActivity", "currentUser = " + (currentUser != null ? currentUser.getUid() : "null"));
            SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
            boolean isGuest = prefs.getBoolean("is_guest", false);

            if (isGuest || currentUser != null) {
                bottomNav.setVisibility(View.VISIBLE);
                Fragment fragment = getFragmentForNavItem(currentNavItemId);
                currentFragment = fragment;
                loadFragment(fragment, false);
                setupNavigation();
                if (getIntent().getBooleanExtra("open_focus_tab", false)) {
                    bottomNav.setSelectedItemId(R.id.nav_focus);
                    getIntent().removeExtra("open_focus_tab");
                }
                bottomNav.setSelectedItemId(currentNavItemId);
            } else {
                bottomNav.setVisibility(View.GONE);
                currentFragment = new LoginFragment();
                loadFragment(currentFragment, false);
            }
        } else {
            bottomNav.setVisibility(View.GONE);
            currentFragment = new NoInternetFragment();
            loadFragment(currentFragment, false);
        }
    }

    private void startReturnChecker() {
        stopReturnChecker();

        returnCheckRunnable = new Runnable() {
            @Override
            public void run() {
                Fragment currentFrag = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (currentFrag instanceof FocusFragment) {
                    FocusFragment focusFragment = (FocusFragment) currentFrag;
                    boolean isBreakActive = focusFragment.isBreakActive();
                    long dialerOpenedAt = focusFragment.getDialerOpenedAt();
                    long timeSinceDialer = System.currentTimeMillis() - dialerOpenedAt;

                    String currentPackage = getForegroundPackage();
                    boolean isCurrentPackageDialer = isDialerPackage(currentPackage);
                    boolean isCurrentPackageZenTree = getPackageName().equals(currentPackage);

                    Log.d("MainActivity", "=========================================");
                    Log.d("MainActivity", "ReturnChecker:");
                    Log.d("MainActivity", "  isBreakActive = " + isBreakActive);
                    Log.d("MainActivity", "  timeSinceDialer = " + timeSinceDialer + "ms");
                    Log.d("MainActivity", "  currentPackage = " + currentPackage);
                    Log.d("MainActivity", "  isCurrentPackageDialer = " + isCurrentPackageDialer);
                    Log.d("MainActivity", "  isCurrentPackageZenTree = " + isCurrentPackageZenTree);
                    Log.d("MainActivity", "=========================================");

                    if (isBreakActive && !isCurrentPackageDialer && !isCurrentPackageZenTree && timeSinceDialer > 2000) {
                        Log.d("MainActivity", "⚠️⚠️⚠️ ВОЗВРАЩАЕМ ПРИЛОЖЕНИЕ из " + currentPackage);
                        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                        if (intent != null) {
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                            startActivity(intent);
                        }
                    } else {
                        Log.d("MainActivity", "✅ НЕ возвращаем. Причина:");
                        if (!isBreakActive) Log.d("MainActivity", "   - isBreakActive = false");
                        if (isCurrentPackageDialer) Log.d("MainActivity", "   - isCurrentPackageDialer = true (пользователь в звонилке)");
                        if (isCurrentPackageZenTree) Log.d("MainActivity", "   - isCurrentPackageZenTree = true (уже в ZenTree)");
                        if (timeSinceDialer <= 2000) Log.d("MainActivity", "   - timeSinceDialer <= 2000 (ждём)");
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
        cm.unregisterNetworkCallback(networkCallback);
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

    public void retryConnection() {
        if (NetworkUtils.isInternetAvailable(this)) {
            recreate();
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

    public void showNoInternetFragment() {
        if (currentFragment instanceof NoInternetFragment) {
            return;
        }

        bottomNav.setVisibility(View.GONE);
        currentFragment = new NoInternetFragment();
        loadFragment(currentFragment, false);
    }

    public void showMainContent() {
        if (!(currentFragment instanceof NoInternetFragment)) {
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        boolean isGuest = prefs.getBoolean("is_guest", false);

        if (isGuest || currentUser != null) {
            bottomNav.setVisibility(View.VISIBLE);
            currentFragment = new HomeFragment();
            loadFragment(currentFragment, false);
            setupNavigation();
        } else {
            bottomNav.setVisibility(View.GONE);
            currentFragment = new LoginFragment();
            loadFragment(currentFragment, false);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Fragment currentFrag = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFrag instanceof FocusFragment) {
            ((FocusFragment) currentFrag).onAppWindowFocusChanged(hasFocus);
        }
    }

    public class NetworkCallback extends ConnectivityManager.NetworkCallback {
        private MainActivity activity;

        public NetworkCallback(MainActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onLost(Network network) {
            activity.runOnUiThread(() -> activity.showNoInternetFragment());
        }

        @Override
        public void onAvailable(Network network) {}

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
            boolean hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            boolean isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

            activity.runOnUiThread(() -> {
                if (hasInternet && isValidated) {
                    activity.showMainContent();
                } else {
                    activity.showNoInternetFragment();
                }
            });
        }
    }
}