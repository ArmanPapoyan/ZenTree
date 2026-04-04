package arman.papoyan.zentreesecond.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import arman.papoyan.zentreesecond.MainActivity;
import arman.papoyan.zentreesecond.R;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private TextView textViewName;
    private TextView textViewEmail;
    private SharedPreferences prefs;
    private boolean isGuest;
    private SwitchCompat switchTheme;
    private SharedPreferences themePrefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        switchTheme = view.findViewById(R.id.switch_theme);
        themePrefs = requireActivity().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE);
        textViewName = view.findViewById(R.id.text_view_name);
        textViewEmail = view.findViewById(R.id.text_view_email);
        Button out = view.findViewById(R.id.button_logout);
        int savedMode = themePrefs.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        switchTheme.setChecked(savedMode == AppCompatDelegate.MODE_NIGHT_YES);

        prefs = getActivity().getSharedPreferences("login_prefs", MODE_PRIVATE);
        isGuest = prefs.getBoolean("is_guest", false);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        displayUserInfo(user);

        out.setOnClickListener(v -> logout());
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int mode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            themePrefs.edit().putInt("night_mode", mode).apply();
            AppCompatDelegate.setDefaultNightMode(mode);
            int currentNavId = 0;
            if (getActivity() instanceof MainActivity) {
                currentNavId = ((MainActivity) getActivity()).getCurrentNavItemId();
            }
            Intent intent = requireActivity().getIntent();
            intent.putExtra("selected_nav_id", currentNavId);
            requireActivity().finish();
            startActivity(intent);
        });

        return view;
    }

    private void displayUserInfo(FirebaseUser user) {
        if (user != null && !user.isAnonymous()) {
            textViewEmail.setText(user.getEmail());
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                textViewName.setText(displayName);
            } else {
                textViewName.setText("Не указано");
            }
        } else if (isGuest) {
            textViewName.setText("Гость");
            textViewEmail.setText("Гостевой режим");
        } else {
            textViewName.setText("Не авторизован");
            textViewEmail.setText("Не авторизован");
            goToLoginFragment();
        }
    }

    private void logout() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.disableAllFirestoreListeners();
        }
        disableFirestoreListeners();

        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null && user.isAnonymous()) {
            prefs.edit().clear().apply();
            mAuth.signOut();
            goToLoginFragment();
        } else if (user != null) {
            mAuth.signOut();
            prefs.edit().clear().apply();
            goToLoginFragment();
        } else {
            prefs.edit().clear().apply();
            goToLoginFragment();
        }
    }

    private void disableFirestoreListeners() {
        Fragment tasksFragment = getParentFragmentManager().findFragmentByTag("tasks_fragment");
        if (tasksFragment instanceof TasksFragment) {
            ((TasksFragment) tasksFragment).removeListener();
        }

        if (tasksFragment == null) {
            tasksFragment = getParentFragmentManager().findFragmentById(R.id.fragment_container);
            if (tasksFragment instanceof TasksFragment) {
                ((TasksFragment) tasksFragment).removeListener();
            }
        }
    }

    private void goToLoginFragment() {
        if (getActivity() == null || !isAdded()) {
            return;
        }

        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.hideNavigation();
        }

        LoginFragment loginFragment = new LoginFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container, loginFragment)
                .commit();
    }
}