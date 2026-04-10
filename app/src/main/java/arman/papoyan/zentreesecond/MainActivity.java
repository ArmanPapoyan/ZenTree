package arman.papoyan.zentreesecond;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import arman.papoyan.zentreesecond.fragments.FocusFragment;
import arman.papoyan.zentreesecond.fragments.HomeFragment;
import arman.papoyan.zentreesecond.fragments.LoginFragment;
import arman.papoyan.zentreesecond.fragments.NoInternetFragment;
import arman.papoyan.zentreesecond.fragments.ProfileFragment;
import arman.papoyan.zentreesecond.fragments.StatisticsFragment;
import arman.papoyan.zentreesecond.fragments.TasksFragment;
import arman.papoyan.zentreesecond.utils.NetworkUtils;

public class MainActivity extends AppCompatActivity {

    public BottomNavigationView bottomNav;
    private Fragment currentFragment;
    private int currentNavItemId = R.id.nav_home;
    private NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
            SharedPreferences themePrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);
            int nightMode = themePrefs.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            AppCompatDelegate.setDefaultNightMode(nightMode);

            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            networkCallback = new NetworkCallback(this);
            cm.registerDefaultNetworkCallback(networkCallback);
            bottomNav = findViewById(R.id.bottom_navigation);
        if (NetworkUtils.isInternetAvailable(this)) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
            boolean isGuest = prefs.getBoolean("is_guest", false);

            if (isGuest || currentUser != null) {
                bottomNav.setVisibility(View.VISIBLE);

                Fragment fragment = getFragmentForNavItem(currentNavItemId);
                currentFragment = fragment;
                loadFragment(fragment, false);

                setupNavigation();
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
    @Override
    protected void onDestroy() {
        super.onDestroy();
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.unregisterNetworkCallback(networkCallback);
    }
    public void retryConnection() {
        if (NetworkUtils.isInternetAvailable(this)) {
            recreate();
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
        bottomNav.setVisibility(View.GONE);
        currentFragment = new NoInternetFragment();
        loadFragment(currentFragment, false);
    }
    public void showMainContent() {
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
        public void onAvailable(Network network) {
            // Ничего не делаем, ждём onCapabilitiesChanged
        }

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