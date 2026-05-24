package arman.papoyan.zentreesecond.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import arman.papoyan.zentreesecond.MainActivity;
import arman.papoyan.zentreesecond.R;
import arman.papoyan.zentreesecond.utils.FirstLaunchManager;

public class RegistrationFragment extends Fragment {

    private FirebaseAuth mAuth;
    private int currentStep = 1;

    private View step1Container, step2Container, step3Container;

    private EditText etEmail, etPass, etConfirmPass, etName, etWakeUpTime;
    private Button buttonSelectWakeUpTime;
    private String selectedWakeUpTime;
    private Button btnNext;
    private TextView textViewTitle;
    private EditText etScreenTimeGoal;
    private String userEmail;

    private static final String KEY_CURRENT_STEP = "current_step";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_CONFIRM_PASSWORD = "confirm_password";
    private static final String KEY_NAME = "name";
    private static final String KEY_WAKE_UP_TIME = "wake_up_time";

    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 1002;
    private boolean isGoogleUser = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registration, container, false);

        mAuth = FirebaseAuth.getInstance();

        step1Container = view.findViewById(R.id.step_1_container);
        step2Container = view.findViewById(R.id.step_2_container);
        step3Container = view.findViewById(R.id.step_3_container);

        etEmail = view.findViewById(R.id.edit_text_email);
        etPass = view.findViewById(R.id.edit_text_password);
        etConfirmPass = view.findViewById(R.id.edit_text_confirm_password);
        etName = view.findViewById(R.id.edit_text_name);
        etWakeUpTime = view.findViewById(R.id.edit_text_wake_up_time);
        etScreenTimeGoal = view.findViewById(R.id.edit_text_screen_time_goal);
        buttonSelectWakeUpTime = view.findViewById(R.id.button_select_wake_up_time);
        btnNext = view.findViewById(R.id.button_next);
        textViewTitle = view.findViewById(R.id.text_view_title);
        ImageButton btnLanguage = view.findViewById(R.id.btn_language);

        btnNext.setOnClickListener(v -> handleNextStep());
        buttonSelectWakeUpTime.setOnClickListener(v -> showTimePickerDialog());

        view.findViewById(R.id.button_go_to_login).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchAuthFragment(new LoginFragment());
            }
        });

        SharedPreferences prefs = requireActivity().getSharedPreferences("login_prefs", MODE_PRIVATE);
        String tempEmail = prefs.getString("temp_email", "");
        String tempName = prefs.getString("temp_name", "");
        boolean isGoogleUserTemp = prefs.getBoolean("is_google_user", false);
        if (!tempEmail.isEmpty()) {
            etEmail.setText(tempEmail);
            etEmail.setEnabled(false);
            isGoogleUser = true;
            currentStep = 3;
            showStep(3);
            btnNext.setText(getString(R.string.action_finish));
            btnNext.setOnClickListener(v -> registerUser());
        }

        if (!tempName.isEmpty()) {
            etName.setText(tempName);
        }

        prefs.edit().putBoolean("is_registering", true).apply();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(getActivity(), gso);

        Button btnGoogleSignUp = view.findViewById(R.id.btn_google_sign_up);
        btnGoogleSignUp.setOnClickListener(v -> signInWithGoogle());
        btnLanguage.setOnClickListener(v -> showLanguageDialog());

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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_STEP, currentStep);
        outState.putString(KEY_EMAIL, etEmail.getText().toString());
        outState.putString(KEY_PASSWORD, etPass.getText().toString());
        outState.putString(KEY_CONFIRM_PASSWORD, etConfirmPass.getText().toString());
        outState.putString(KEY_NAME, etName.getText().toString());
        outState.putString(KEY_WAKE_UP_TIME, selectedWakeUpTime);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            currentStep = savedInstanceState.getInt(KEY_CURRENT_STEP, 1);
            etEmail.setText(savedInstanceState.getString(KEY_EMAIL, ""));
            etPass.setText(savedInstanceState.getString(KEY_PASSWORD, ""));
            etConfirmPass.setText(savedInstanceState.getString(KEY_CONFIRM_PASSWORD, ""));
            etName.setText(savedInstanceState.getString(KEY_NAME, ""));
            selectedWakeUpTime = savedInstanceState.getString(KEY_WAKE_UP_TIME);

            if (selectedWakeUpTime != null && !selectedWakeUpTime.isEmpty()) {
                etWakeUpTime.setText(selectedWakeUpTime);
            }

            showStep(currentStep);
        }
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
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
                Toast.makeText(getActivity(), getString(R.string.error_sign_in_failed, e.getMessage()), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.google_registration_wait));
        progressDialog.setCancelable(false);
        progressDialog.show();

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(getActivity(), task -> {
                    progressDialog.dismiss();

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        isGoogleUser = true;
                        etEmail.setText(user.getEmail());
                        etName.setText(user.getDisplayName());
                        currentStep = 3;
                        showStep(3);
                        btnNext.setText(getString(R.string.action_finish));
                        btnNext.setOnClickListener(v -> registerUser());

                        Toast.makeText(getActivity(), getString(R.string.toast_google_data_loaded), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getActivity(), getString(R.string.error_auth_failed), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePicker = new TimePickerDialog(getActivity(), (view, hourOfDay, minuteOfHour) -> {
            selectedWakeUpTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour);
            etWakeUpTime.setText(selectedWakeUpTime);
        }, hour, minute, true);
        timePicker.show();
    }

    private void handleNextStep() {
        switch (currentStep) {
            case 1:
                validateEmailStep();
                break;
            case 2:
                validatePasswordStep();
                break;
            case 3:
                registerUser();
                break;
        }
    }

    private void validateEmailStep() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError(getString(R.string.email_empty_error));
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.error_invalid_email));
            return;
        }

        checkEmailAndProceed(email);
    }

    private void validatePasswordStep() {
        String pass = etPass.getText().toString();
        String confirmPass = etConfirmPass.getText().toString();

        if (pass.isEmpty()) {
            etPass.setError(getString(R.string.password_empty_error));
            return;
        }

        if (pass.length() < 6) {
            etPass.setError(getString(R.string.password_too_short_custom));
            return;
        }

        if (confirmPass.isEmpty()) {
            etConfirmPass.setError(getString(R.string.password_confirm_empty_error));
            return;
        }

        if (!pass.equals(confirmPass)) {
            etConfirmPass.setError(getString(R.string.password_not_match_custom));
            return;
        }

        createUserAndProceed(pass, confirmPass);
    }

    private void showStep(int step) {
        currentStep = step;

        step1Container.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        step2Container.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        step3Container.setVisibility(step == 3 ? View.VISIBLE : View.GONE);

        if (step == 1) textViewTitle.setText(getString(R.string.title_step_email));
        if (step == 2) textViewTitle.setText(getString(R.string.title_step_password));
        if (step == 3) {
            textViewTitle.setText(getString(R.string.title_step_final));
            btnNext.setText(getString(R.string.action_finish));
        }
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPass.getText().toString();
        String name = etName.getText().toString().trim();
        String wakeUpTime = selectedWakeUpTime != null ? selectedWakeUpTime : "07:00";
        String screenTimeGoalStr = etScreenTimeGoal.getText().toString().trim();

        if (isGoogleUser) {
            if (wakeUpTime.isEmpty()) {
                etWakeUpTime.setError(getString(R.string.error_select_wakeup_time));
                return;
            }
            if (screenTimeGoalStr.isEmpty()) {
                etScreenTimeGoal.setError(getString(R.string.error_enter_goal));
                return;
            }
            saveUserToFirestore(email, name, wakeUpTime, screenTimeGoalStr);
            return;
        }

        if (selectedWakeUpTime == null || selectedWakeUpTime.isEmpty()) {
            etWakeUpTime.setError(getString(R.string.error_select_wakeup_time));
            return;
        }
        if (name.isEmpty()) {
            etName.setError(getString(R.string.error_enter_name));
            return;
        }
        if (screenTimeGoalStr.isEmpty()) {
            etScreenTimeGoal.setError(getString(R.string.error_enter_goal));
            return;
        }

        int screenTimeGoal = Integer.parseInt(screenTimeGoalStr);
        if (screenTimeGoal < 1 || screenTimeGoal > 24) {
            etScreenTimeGoal.setError(getString(R.string.error_invalid_goal_range));
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.account_creation_wait));
        progressDialog.setCancelable(false);
        progressDialog.show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    progressDialog.dismiss();

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification();
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            user.updateProfile(profileUpdates);

                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            String userId = user.getUid();

                            Map<String, Object> userData = new HashMap<>();
                            userData.put("name", name);
                            userData.put("email", email);
                            userData.put("wakeUpTime", selectedWakeUpTime);
                            userData.put("createdAt", System.currentTimeMillis());
                            userData.put("screenTimeGoal", screenTimeGoal);

                            db.collection("users").document(userId).set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("Registration", getString(R.string.log_user_data_saved));
                                        Toast.makeText(getActivity(), getString(R.string.account_creation_success), Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Registration", getString(R.string.error_with_message, e.getMessage()));
                                    });
                            showVerificationDialog(user);
                        }
                    } else {
                        String errorMessage = task.getException().getMessage();
                        if (errorMessage.contains("email already in use")) {
                            Toast.makeText(getActivity(), getString(R.string.email_already_in_use), Toast.LENGTH_LONG).show();
                        } else if (errorMessage.contains("invalid email")) {
                            Toast.makeText(getActivity(), getString(R.string.error_invalid_email), Toast.LENGTH_LONG).show();
                        } else if (errorMessage.contains("weak password")) {
                            Toast.makeText(getActivity(), getString(R.string.password_too_short_custom), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getActivity(), getString(R.string.error_with_message, task.getException().getMessage()), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void showVerificationDialog(FirebaseUser user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.dialog_title_verify_email));

        String dialogMessage = getString(R.string.dialog_message_verify_email, user.getEmail());
        builder.setMessage(dialogMessage);

        builder.setPositiveButton(getString(R.string.action_verified_confirm), (dialog, which) -> {
            ProgressDialog progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage(getString(R.string.email_verification_check_wait));
            progressDialog.setCancelable(false);
            progressDialog.show();

            user.reload().addOnCompleteListener(task -> {
                progressDialog.dismiss();

                if (user.isEmailVerified()) {
                    SharedPreferences prefs = requireActivity().getSharedPreferences("registration_prefs", Context.MODE_PRIVATE);
                    prefs.edit().putBoolean("is_registering", false).apply();
                    new FirstLaunchManager(getActivity()).setFirstLaunchDone();
                    ((MainActivity) getActivity()).goToHomeFragment();
                } else {
                    Toast.makeText(getActivity(), getString(R.string.toast_email_not_verified_yet), Toast.LENGTH_SHORT).show();
                    showVerificationDialog(user);
                }
            });
        });

        builder.setNegativeButton(getString(R.string.action_resend_email), (dialog, which) -> {
            user.sendEmailVerification();
            Toast.makeText(getActivity(), getString(R.string.toast_email_resent), Toast.LENGTH_SHORT).show();
            showVerificationDialog(user);
        });

        builder.setCancelable(false);
        builder.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        SharedPreferences prefs = requireActivity().getSharedPreferences("registration_prefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("is_registering", false).apply();
    }

    private void saveUserToFirestore(String email, String name, String wakeUpTime, String screenTimeGoalStr) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        int screenTimeGoal = Integer.parseInt(screenTimeGoalStr);
        String userId = user.getUid();

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();
        user.updateProfile(profileUpdates);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("wakeUpTime", wakeUpTime);
        userData.put("screenTimeGoal", screenTimeGoal);
        userData.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(userId).set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Registration", getString(R.string.log_user_data_saved));
                    SharedPreferences prefs = requireActivity().getSharedPreferences("login_prefs", MODE_PRIVATE);
                    prefs.edit().remove("temp_email").remove("temp_name").remove("is_google_user").apply();

                    SharedPreferences regPrefs = requireActivity().getSharedPreferences("registration_prefs", MODE_PRIVATE);
                    regPrefs.edit().putBoolean("is_registering", false).apply();

                    if (isGoogleUser) {
                        new FirstLaunchManager(getActivity()).setFirstLaunchDone();
                        ((MainActivity) getActivity()).goToHomeFragment();
                    } else {
                        showVerificationDialog(user);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Registration", getString(R.string.error_with_message, e.getMessage()));
                    Toast.makeText(getActivity(), getString(R.string.error_save_data_failed), Toast.LENGTH_LONG).show();
                });
    }

    private void checkEmailAndProceed(String email) {
        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.email_check_wait));
        progressDialog.setCancelable(false);
        progressDialog.show();

        FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();

                    if (task.isSuccessful()) {
                        boolean isEmailTaken = task.getResult().getSignInMethods().size() > 0;

                        if (isEmailTaken) {
                            new AlertDialog.Builder(getActivity())
                                    .setTitle(getString(R.string.email_already_taken_title))
                                    .setMessage(getString(R.string.email_already_taken_message))
                                    .setPositiveButton(getString(R.string.email_already_taken_login), (dialog, which) -> {
                                        if (getActivity() instanceof MainActivity) {
                                            ((MainActivity) getActivity()).switchAuthFragment(new LoginFragment());
                                        }
                                    })
                                    .setNegativeButton(getString(R.string.email_already_taken_back), null)
                                    .show();
                        } else {
                            userEmail = email;
                            showStep(2);
                        }
                    } else {
                        String error = task.getException().getMessage();
                        if (error.contains("NETWORK_ERROR")) {
                            Toast.makeText(getActivity(), getString(R.string.email_network_error), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getActivity(), getString(R.string.email_check_error, error), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void createUserAndProceed(String password, String confirmPassword) {
        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.account_creation_wait));
        progressDialog.setCancelable(false);
        progressDialog.show();

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(userEmail, password)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();

                    if (task.isSuccessful()) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification()
                                    .addOnCompleteListener(verifyTask -> {
                                        if (verifyTask.isSuccessful()) {
                                            Toast.makeText(getActivity(), getString(R.string.email_verification_sent, userEmail), Toast.LENGTH_LONG).show();
                                            showStep(3);
                                        } else {
                                            Toast.makeText(getActivity(), getString(R.string.email_verification_failed), Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    } else {
                        String errorMessage = task.getException().getMessage();
                        if (errorMessage.contains("email already in use")) {
                            Toast.makeText(getActivity(), getString(R.string.email_already_in_use), Toast.LENGTH_LONG).show();
                        } else if (errorMessage.contains("invalid email")) {
                            Toast.makeText(getActivity(), getString(R.string.error_invalid_email), Toast.LENGTH_LONG).show();
                        } else if (errorMessage.contains("weak password")) {
                            Toast.makeText(getActivity(), getString(R.string.password_too_short_custom), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getActivity(), getString(R.string.error_with_message, errorMessage), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}