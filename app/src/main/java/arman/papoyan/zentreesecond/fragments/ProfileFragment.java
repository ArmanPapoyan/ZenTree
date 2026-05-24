package arman.papoyan.zentreesecond.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Locale;

import arman.papoyan.zentreesecond.MainActivity;
import arman.papoyan.zentreesecond.R;
import arman.papoyan.zentreesecond.utils.NotificationCleaner;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private TextView textViewName;
    private TextView textViewEmail;
    private SharedPreferences prefs;
    private SharedPreferences themePrefs;
    private Button deleteButton;
    private Button btnThemeSystem, btnThemeLight, btnThemeDark;
    private Button btnLangRussian, btnLangEnglish, btnLangArmenian;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        themePrefs = requireActivity().getSharedPreferences("theme_prefs", MODE_PRIVATE);
        textViewName = view.findViewById(R.id.text_view_name);
        textViewEmail = view.findViewById(R.id.text_view_email);
        Button out = view.findViewById(R.id.button_logout);
        deleteButton = view.findViewById(R.id.button_delete);
        prefs = getActivity().getSharedPreferences("login_prefs", MODE_PRIVATE);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        btnThemeSystem = view.findViewById(R.id.btn_theme_system);
        btnThemeLight = view.findViewById(R.id.btn_theme_light);
        btnThemeDark = view.findViewById(R.id.btn_theme_dark);
        btnLangRussian = view.findViewById(R.id.btn_lang_russian);
        btnLangEnglish = view.findViewById(R.id.btn_lang_english);
        btnLangArmenian = view.findViewById(R.id.btn_lang_armenian);

        btnLangRussian.setOnClickListener(v -> setLanguage("ru"));
        btnLangEnglish.setOnClickListener(v -> setLanguage("en"));
        btnLangArmenian.setOnClickListener(v -> setLanguage("hy"));

        displayUserInfo(user);
        out.setOnClickListener(v -> logout());

        btnThemeSystem.setOnClickListener(v -> setThemeMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
        btnThemeLight.setOnClickListener(v -> setThemeMode(AppCompatDelegate.MODE_NIGHT_NO));
        btnThemeDark.setOnClickListener(v -> setThemeMode(AppCompatDelegate.MODE_NIGHT_YES));

        deleteButton.setOnClickListener(v -> deleteAccount());
        updateButtonHighlight();
        return view;
    }

    private void setLanguage(String languageCode) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("settings_prefs", MODE_PRIVATE);
        prefs.edit().putString("language", languageCode).apply();

        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);

        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        requireActivity().recreate();
    }
    private void setThemeMode(int mode) {
        themePrefs.edit().putInt("night_mode", mode).apply();
        SharedPreferences loginPrefs = requireActivity().getSharedPreferences("login_prefs", MODE_PRIVATE);
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        AppCompatDelegate.setDefaultNightMode(mode);
        Intent intent = requireActivity().getIntent();
        intent.putExtra("was_logged_in",userId != null);
        intent.putExtra("selected_nav_id", ((MainActivity) getActivity()).getCurrentNavItemId());

        requireActivity().finish();
        startActivity(intent);
    }
    private void showPasswordDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String email = user.getEmail();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        EditText passwordInput = new EditText(getActivity());
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setHint(getString(R.string.hint_enter_password));
        passwordInput.setPadding(40, 20, 40, 20);

        builder.setTitle(getString(R.string.dialog_title_enter_password))
                .setMessage(R.string.password_text)
                .setView(passwordInput)
                .setPositiveButton(getString(R.string.action_confirm), (dialog, which) -> {
                    String password = passwordInput.getText().toString();
                    if (password.isEmpty()) {
                        Toast.makeText(getActivity(), getString(R.string.hint_enter_password), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    reauthenticateUser(email, password);
                })
                .setNegativeButton(getString(R.string.action_cancel), (dialog, which) -> dialog.dismiss())
                .setCancelable(false);

        builder.show();
    }
    private void reauthenticateUser(String email, String password) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        AuthCredential credential = EmailAuthProvider.getCredential(email, password);

        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    showDeleteDialog();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), getString(R.string.error_invalid_password, e.getMessage()), Toast.LENGTH_LONG).show();
                });
    }
    private void showDeleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.dialog_title_delete_account))
                .setMessage(R.string.delete_text)
                .setPositiveButton(getString(R.string.action_delete), (dialog, which) -> {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) return;

                    String userId = user.getUid();
                    ProgressDialog progressDialog = new ProgressDialog(getActivity());
                    progressDialog.setMessage(getString(R.string.progress_deleting_account));
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    deleteAllUserDataFromFirestore(userId, new OnDataDeletedListener() {
                        @Override
                        public void onDataDeleted() {
                            user.delete()
                                    .addOnSuccessListener(aVoid -> {
                                        prefs.edit().clear().apply();
                                        FirebaseAuth.getInstance().signOut();
                                        progressDialog.dismiss();
                                        goToLoginFragment();
                                    })
                                    .addOnFailureListener(e -> {
                                        progressDialog.dismiss();
                                        Toast.makeText(getActivity(), getString(R.string.error_delete_account_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                                    });
                        }

                        @Override
                        public void onError(Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(getActivity(), getString(R.string.error_delete_failed_with_msg, e.getMessage()), Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton(getString(R.string.action_cancel), (dialog, which) -> dialog.dismiss())
                .setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
    }
    private void displayUserInfo(FirebaseUser user) {
        if (user != null) {
            textViewEmail.setText(user.getEmail());
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                textViewName.setText(displayName);
            } else {
                textViewName.setText(getString(R.string.user_info_not_specified));
            }
        } else {
            textViewName.setText(getString(R.string.user_info_not_authorized));
            textViewEmail.setText(getString(R.string.user_info_not_authorized));
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

        NotificationCleaner.clearAllNotifications(requireContext());

        goToLoginFragment();
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

        prefs.edit().clear().apply();

        SharedPreferences treePrefs =
                requireActivity().getSharedPreferences("ZenTreePrefs", MODE_PRIVATE);
        treePrefs.edit().clear().apply();

        mAuth.signOut();

        NotificationCleaner.clearAllNotifications(requireContext());

        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("logout", true);

        startActivity(intent);

        requireActivity().finish();
    }
    private enum AuthProvider {
        EMAIL_PASSWORD,
        GOOGLE,
        UNKNOWN
    }

    private AuthProvider getCurrentAuthProvider() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return AuthProvider.UNKNOWN;

        for (UserInfo profile : user.getProviderData()) {
            String providerId = profile.getProviderId();
            if (providerId.equals("google.com")) {
                return AuthProvider.GOOGLE;
            }
            if (providerId.equals("password")) {
                return AuthProvider.EMAIL_PASSWORD;
            }
        }
        return AuthProvider.EMAIL_PASSWORD;
    }
    private void deleteAccount() {
        AuthProvider provider = getCurrentAuthProvider();

        if (provider == AuthProvider.GOOGLE) {
            deleteGoogleAccount();
        } else if (provider == AuthProvider.EMAIL_PASSWORD) {
            showPasswordDialog();
        } else {
            Toast.makeText(getContext(), getString(R.string.error_unknown_account_type), Toast.LENGTH_SHORT).show();
        }
    }
    private void deleteGoogleAccount() {
        showDeleteDialogForGoogle();
    }
    private void showDeleteDialogForGoogle() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.dialog_title_delete_account))
                .setMessage(R.string.delete_text)
                .setPositiveButton(getString(R.string.action_delete), (dialog, which) -> {
                    performGoogleAccountDeletion();
                })
                .setNegativeButton(getString(R.string.action_cancel), (dialog, which) -> dialog.dismiss())
                .setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
    }
    private void performGoogleAccountDeletion() {
        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.progress_deleting_account));
        progressDialog.setCancelable(false);
        progressDialog.show();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            progressDialog.dismiss();
            return;
        }

        String userId = user.getUid();

        deleteAllUserDataFromFirestore(userId, new OnDataDeletedListener() {
            @Override
            public void onDataDeleted() {
                user.delete()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestIdToken(getString(R.string.default_web_client_id))
                                        .requestEmail()
                                        .build();
                                GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(requireContext(), gso);
                                googleSignInClient.revokeAccess()
                                        .addOnCompleteListener(revokeTask -> {
                                            progressDialog.dismiss();
                                            Toast.makeText(getContext(), getString(R.string.toast_account_deleted), Toast.LENGTH_SHORT).show();
                                            navigateToLogin();
                                        });
                            } else {
                                progressDialog.dismiss();
                                Toast.makeText(getContext(), getString(R.string.error_delete_account_failed, task.getException().getMessage()), Toast.LENGTH_LONG).show();
                            }
                        });
            }

            @Override
            public void onError(Exception e) {
                progressDialog.dismiss();
                Toast.makeText(getContext(), getString(R.string.error_delete_failed_with_msg, e.getMessage()), Toast.LENGTH_LONG).show();
            }
        });
    }
    interface OnDataDeletedListener {
        void onDataDeleted();
        void onError(Exception e);
    }
    private void deleteAllUserDataFromFirestore(String userId, OnDataDeletedListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("tasks").document(userId).collection("userTasks")
                .get()
                .addOnSuccessListener(tasks -> {
                    for (QueryDocumentSnapshot task : tasks) {
                        task.getReference().delete();
                    }
                    db.collection("users").document(userId).collection("stats")
                            .get()
                            .addOnSuccessListener(stats -> {
                                for (QueryDocumentSnapshot stat : stats) {
                                    stat.getReference().delete();
                                }
                                db.collection("users").document(userId).collection("tree")
                                        .get()
                                        .addOnSuccessListener(trees -> {
                                            for (QueryDocumentSnapshot tree : trees) {
                                                tree.getReference().delete();
                                            }
                                            db.collection("users").document(userId)
                                                    .delete()
                                                    .addOnSuccessListener(aVoid -> {
                                                        Log.d("Delete", "Все данные пользователя удалены из Firestore");
                                                        listener.onDataDeleted();
                                                    })
                                                    .addOnFailureListener(listener::onError);
                                        })
                                        .addOnFailureListener(listener::onError);
                            })
                            .addOnFailureListener(listener::onError);
                })
                .addOnFailureListener(listener::onError);
    }

    private void navigateToLogin() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        SharedPreferences regPrefs = requireActivity().getSharedPreferences("registration_prefs", Context.MODE_PRIVATE);
        regPrefs.edit().clear().apply();

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideNavigation();
        }

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container, new LoginFragment())
                .commit();
    }
    private void deleteUserDataFromFirestore() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (userId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userId).collection("tasks")
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        doc.getReference().delete();
                    }
                });

        db.collection("users").document(userId).collection("tree").document("current")
                .delete();

        db.collection("users").document(userId).delete();
    }
    private void updateButtonHighlight() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("settings_prefs", MODE_PRIVATE);
        String currentLang = prefs.getString("language", "ru");

        resetButtonStyle(btnLangRussian);
        resetButtonStyle(btnLangEnglish);
        resetButtonStyle(btnLangArmenian);

        switch (currentLang) {
            case "ru":
                setActiveButtonStyle(btnLangRussian);
                break;
            case "en":
                setActiveButtonStyle(btnLangEnglish);
                break;
            case "hy":
                setActiveButtonStyle(btnLangArmenian);
                break;
        }
    }

    private void resetButtonStyle(Button button) {
        button.setBackgroundResource(R.drawable.outline_button);
        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_green));
    }

    private void setActiveButtonStyle(Button button) {
        button.setBackgroundResource(R.drawable.button_filled);
        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
    }

}