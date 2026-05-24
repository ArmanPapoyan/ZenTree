package arman.papoyan.zentreesecond.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import arman.papoyan.zentreesecond.MainActivity;
import arman.papoyan.zentreesecond.R;
import arman.papoyan.zentreesecond.models.TreeModel;
import arman.papoyan.zentreesecond.utils.TreeFirestoreManager;
import arman.papoyan.zentreesecond.utils.TreeManager;

public class LoginFragment extends Fragment {

    private FirebaseAuth mAuth;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private CheckBox checkBoxRememberMe;
    private SharedPreferences prefs;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 1001;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        Button buttonLogin = view.findViewById(R.id.button_login);
        Button buttonRegister = view.findViewById(R.id.button_register);
        Button btnTestUser = view.findViewById(R.id.test_user);
        editTextEmail = view.findViewById(R.id.edit_text_email);
        editTextPassword = view.findViewById(R.id.edit_text_password);
        checkBoxRememberMe = view.findViewById(R.id.checkBox_remember_me);
        prefs = getActivity().getSharedPreferences("login_prefs", MODE_PRIVATE);
        Button btnGoogle = view.findViewById(R.id.btn_google_sign_in);
        btnGoogle.setOnClickListener(v -> signInWithGoogle());
        ImageButton btnLanguage = view.findViewById(R.id.btn_language);

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(getActivity(), gso);
        btnLanguage.setOnClickListener(v -> showLanguageDialog());

        buttonLogin.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getActivity(), getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(getActivity(), getString(R.string.error_invalid_email_format), Toast.LENGTH_LONG).show();
                return;
            }

            ProgressDialog progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage(getString(R.string.login_please_wait));
            progressDialog.setCancelable(false);
            progressDialog.show();

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(getActivity(), task -> {
                        progressDialog.dismiss();

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
                                        ((MainActivity) getActivity()).goToHomeAfterLogin();
                                    }
                                    @Override
                                    public void onError(String error) {
                                        TreeModel newTree = new TreeModel();
                                        TreeManager treeManager = new TreeManager(getActivity());
                                        treeManager.saveTree(newTree);
                                        ((MainActivity) getActivity()).goToHomeAfterLogin();
                                    }
                                });
                            } else {
                                Toast.makeText(getActivity(), getString(R.string.error_email_not_verified), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            String errorMessage = task.getException().getMessage();

                            if (errorMessage.contains("There is no user record")) {
                                Toast.makeText(getActivity(), getString(R.string.error_user_not_found), Toast.LENGTH_LONG).show();
                            } else if (errorMessage.contains("password is invalid") || errorMessage.contains("incorrect")) {
                                Toast.makeText(getActivity(), getString(R.string.error_wrong_password), Toast.LENGTH_LONG).show();
                            } else if (errorMessage.contains("too many requests")) {
                                Toast.makeText(getActivity(), getString(R.string.error_too_many_attempts), Toast.LENGTH_LONG).show();
                            } else if (errorMessage.contains("network error")) {
                                Toast.makeText(getActivity(), getString(R.string.error_network), Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getActivity(), getString(R.string.error_with_message, errorMessage), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });

        buttonRegister.setOnClickListener(v -> {
            RegistrationFragment registrationFragment = new RegistrationFragment();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchAuthFragment(registrationFragment);
            }
        });

        boolean isRemembered = prefs.getBoolean("remember_me", false);
        if (isRemembered) {
            String savedEmail = prefs.getString("email", "");
            String savedPassword = prefs.getString("password", "");
            editTextEmail.setText(savedEmail);
            editTextPassword.setText(savedPassword);
            checkBoxRememberMe.setChecked(true);
        }

        btnTestUser.setOnClickListener(v -> {
            ProgressDialog progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage(getString(R.string.login_test_user_wait));
            progressDialog.setCancelable(false);
            progressDialog.show();

            FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword("innovationcampus26@gmail.com", "@Test1")
                    .addOnCompleteListener(task -> {
                        progressDialog.dismiss();

                        if (task.isSuccessful()) {
                            SharedPreferences prefs = requireActivity().getSharedPreferences("login_prefs", MODE_PRIVATE);
                            prefs.edit().putBoolean("is_guest", false).apply();

                            Intent intent = new Intent(getActivity(), MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            requireActivity().finish();
                        } else {
                            String error = task.getException().getMessage();
                            if (error.contains("password")) {
                                Toast.makeText(getContext(), getString(R.string.login_test_user_error), Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getContext(), getString(R.string.login_test_user_failed, error), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });

        return view;
    }

    private void showLanguageDialog() {
        String[] languages = {getString(R.string.language_russian), getString(R.string.language_english), getString(R.string.language_armenian)};
        int currentLang = getCurrentLanguageIndex();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.language_dialog_title))
                .setSingleChoiceItems(languages, currentLang, (dialog, which) -> {
                    String langCode = "";
                    switch (which) {
                        case 0: langCode = "ru"; break;
                        case 1: langCode = "en"; break;
                        case 2: langCode = "hy"; break;
                    }
                    setLanguage(langCode);
                    dialog.dismiss();
                });
        builder.setNegativeButton(getString(R.string.language_cancel), null);
        builder.show();
    }

    private int getCurrentLanguageIndex() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("settings_prefs", MODE_PRIVATE);
        String currentLang = prefs.getString("language", "ru");
        switch (currentLang) {
            case "ru": return 0;
            case "en": return 1;
            case "hy": return 2;
            default: return 0;
        }
    }

    private void setLanguage(String languageCode) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("settings_prefs", MODE_PRIVATE);
        prefs.edit().putString("language", languageCode).apply();

        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);

        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        requireActivity().recreate();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(getActivity(), getString(R.string.error_google_sign_in, e.getMessage()), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void checkAndSaveUserToFirestore(FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("name", user.getDisplayName() != null ? user.getDisplayName() : "");
                        userData.put("email", user.getEmail());
                        userData.put("wakeUpTime", "07:00");
                        userData.put("screenTimeGoal", 6);
                        userData.put("createdAt", System.currentTimeMillis());
                        db.collection("users").document(user.getUid()).set(userData);
                    }
                });
    }

    private void checkUserInFirestore(FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = user.getUid();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        SharedPreferences prefs = requireActivity().getSharedPreferences("login_prefs", MODE_PRIVATE);
                        prefs.edit().putBoolean("is_guest", false).apply();

                        TreeFirestoreManager firestoreManager = new TreeFirestoreManager();
                        firestoreManager.loadTree(new TreeFirestoreManager.TreeLoadCallback() {
                            @Override
                            public void onSuccess(TreeModel tree) {
                                TreeManager treeManager = new TreeManager(getActivity());
                                treeManager.saveTree(tree);
                                ((MainActivity) getActivity()).goToHomeAfterLogin();
                            }

                            @Override
                            public void onError(String error) {
                                TreeModel newTree = new TreeModel();
                                TreeManager treeManager = new TreeManager(getActivity());
                                treeManager.saveTree(newTree);
                                ((MainActivity) getActivity()).goToHomeAfterLogin();
                            }
                        });
                    } else {
                        goToRegistrationWithGoogleData(user);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("LoginFragment", "Ошибка проверки пользователя: " + e.getMessage());
                    goToRegistrationWithGoogleData(user);
                });
    }

    private void goToRegistrationWithGoogleData(FirebaseUser user) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("login_prefs", MODE_PRIVATE);
        prefs.edit()
                .putString("temp_email", user.getEmail())
                .putString("temp_name", user.getDisplayName() != null ? user.getDisplayName() : "")
                .putBoolean("is_google_user", true)
                .apply();
        SharedPreferences regPrefs = requireActivity().getSharedPreferences("registration_prefs", MODE_PRIVATE);
        regPrefs.edit().putBoolean("is_registering", true).apply();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).switchAuthFragment(new RegistrationFragment());
            ((MainActivity) getActivity()).hideNavigation();
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.login_google_wait));
        progressDialog.setCancelable(false);
        progressDialog.show();

        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(getActivity(), task -> {
                    progressDialog.dismiss();

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        checkUserInFirestore(user);
                    } else {
                        String errorMessage = task.getException().getMessage();
                        if (errorMessage.contains("credential")) {
                            Toast.makeText(getActivity(), getString(R.string.login_google_error), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getActivity(), getString(R.string.error_auth_failed), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}