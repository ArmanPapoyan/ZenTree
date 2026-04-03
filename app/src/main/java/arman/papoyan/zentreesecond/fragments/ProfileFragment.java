package arman.papoyan.zentreesecond.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        textViewName = view.findViewById(R.id.text_view_name);
        textViewEmail = view.findViewById(R.id.text_view_email);
        Button out = view.findViewById(R.id.button_logout);

        prefs = getActivity().getSharedPreferences("login_prefs", MODE_PRIVATE);
        isGuest = prefs.getBoolean("is_guest", false);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        displayUserInfo(user);

        out.setOnClickListener(v -> logout());

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