package arman.papoyan.zentreesecond;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import arman.papoyan.zentreesecond.fragments.HomeFragment;
import arman.papoyan.zentreesecond.fragments.FocusFragment;
import arman.papoyan.zentreesecond.fragments.LoginFragment;
import arman.papoyan.zentreesecond.fragments.TasksFragment;
import arman.papoyan.zentreesecond.fragments.StatisticsFragment;
import arman.papoyan.zentreesecond.fragments.ProfileFragment;

public class MainActivity extends AppCompatActivity {

    public BottomNavigationView bottomNav;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        boolean isGuest = prefs.getBoolean("is_guest", false);
        String savedGuestUid = prefs.getString("guest_uid", "");
        if (currentUser != null && currentUser.isAnonymous() && !isGuest) {
            isGuest = true;
            prefs.edit().putBoolean("is_guest", true).putString("guest_uid", currentUser.getUid()).apply();
        }
        if (savedInstanceState == null) {
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
    }

    private void setupNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();

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

            if (fragment != null && fragment.getClass() != currentFragment.getClass()) {
                currentFragment = fragment;
                loadFragment(fragment, true);
            }

            return true;
        });
    }

    private void loadFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.fade_out
        );
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
}