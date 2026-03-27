package arman.papoyan.zentreesecond.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import arman.papoyan.zentreesecond.MainActivity;
import arman.papoyan.zentreesecond.R;
import arman.papoyan.zentreesecond.model.TreeModel;
import arman.papoyan.zentreesecond.utils.TreeFirestoreManager;
import arman.papoyan.zentreesecond.utils.TreeManager;

public class LoginFragment extends Fragment {

    private FirebaseAuth mAuth;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private CheckBox checkBoxRememberMe;
    private SharedPreferences prefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        Button buttonLogin = view.findViewById(R.id.button_login);
        Button buttonRegister = view.findViewById(R.id.button_register);
        Button buttonGuest = view.findViewById(R.id.guest);
        editTextEmail = view.findViewById(R.id.edit_text_email);
        editTextPassword = view.findViewById(R.id.edit_text_password);
        checkBoxRememberMe = view.findViewById(R.id.checkBox_remember_me);
        prefs = getActivity().getSharedPreferences("login_prefs", MODE_PRIVATE);

        mAuth = FirebaseAuth.getInstance();

        buttonLogin.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getActivity(), "Заполните все поля", Toast.LENGTH_SHORT).show();
            } else {
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(getActivity(), task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user.isEmailVerified()) {
                                    if (checkBoxRememberMe.isChecked()) {
                                        prefs.edit()
                                                .putBoolean("remember_me", true)
                                                .putString("email", email)
                                                .putString("password", password)
                                                .apply();
                                    } else {
                                        prefs.edit().clear().apply();
                                    }
                                    TreeFirestoreManager firestoreManager = new TreeFirestoreManager();
                                    firestoreManager.loadTree(new TreeFirestoreManager.TreeLoadCallback() {
                                        @Override
                                        public void onSuccess(TreeModel tree) {
                                            TreeManager treeManager = new TreeManager(getActivity());
                                            treeManager.saveTree(tree);
                                            MainActivity activity = (MainActivity) getActivity();
                                            activity.goToHomeAfterLogin();
                                        }

                                        @Override
                                        public void onError(String error) {
                                            TreeModel newTree = new TreeModel();
                                            TreeManager treeManager = new TreeManager(getActivity());
                                            treeManager.saveTree(newTree);

                                            MainActivity activity = (MainActivity) getActivity();
                                            activity.goToHomeAfterLogin();
                                        }
                                    });
                                } else {
                                    Toast.makeText(getActivity(), "Подтвердите email", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(getActivity(), "Ошибка: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

        buttonRegister.setOnClickListener(v -> {
            RegistrationFragment registrationFragment = new RegistrationFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .replace(R.id.fragment_container, registrationFragment)
                    .commit();
        });
        boolean isRemembered = prefs.getBoolean("remember_me", false);
        if (isRemembered) {
            String savedEmail = prefs.getString("email", "");
            String savedPassword = prefs.getString("password", "");
            editTextEmail.setText(savedEmail);
            editTextPassword.setText(savedPassword);
            checkBoxRememberMe.setChecked(true);
        }
        buttonGuest.setOnClickListener(v -> {
            Guest();
        });
        return view;
    }

    private void Guest() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isAnonymous()) {
            currentUser.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    createNewAnonymousUser();
                } else {
                    createNewAnonymousUser();
                }
            });
        } else {
            createNewAnonymousUser();
        }
    }
    private void createNewAnonymousUser() {
        mAuth.signInAnonymously()
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            SharedPreferences prefs = requireActivity().getSharedPreferences("login_prefs", MODE_PRIVATE);
                            prefs.edit()
                                    .putBoolean("is_guest", true)
                                    .putString("guest_uid", user.getUid())
                                    .apply();
                            TreeModel newTree = new TreeModel();
                            TreeManager treeManager = new TreeManager(getActivity());
                            treeManager.saveTree(newTree);
                            Toast.makeText(requireActivity(), "Вход как гость", Toast.LENGTH_SHORT).show();
                            MainActivity activity = (MainActivity) getActivity();
                            if (activity != null) {
                                activity.goToHomeAfterLogin();
                            }
                        }
                    } else {
                        Exception e = task.getException();
                        Log.e("AUTH_ERROR", e.getMessage());
                        Toast.makeText(requireActivity(), "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}