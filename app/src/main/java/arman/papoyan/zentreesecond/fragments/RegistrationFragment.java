package arman.papoyan.zentreesecond.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

    private EditText etEmail, etPass, etConfirmPass, etName, etScreenTime, etWakeUpTime;
    private Button buttonSelectWakeUpTime;
    private String selectedWakeUpTime;
    private Button btnNext;
    private TextView textViewTitle;
    private EditText etScreenTimeGoal;

    private static final String KEY_CURRENT_STEP = "current_step";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_CONFIRM_EMAIL = "confirm_email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_CONFIRM_PASSWORD = "confirm_password";
    private static final String KEY_NAME = "name";
    private static final String KEY_SCREEN_TIME = "screen_time";
    private static final String KEY_WAKE_UP_TIME = "wake_up_time";

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
        etScreenTime = view.findViewById(R.id.edit_text_screen_time);
        etWakeUpTime = view.findViewById(R.id.edit_text_wake_up_time);
        etScreenTimeGoal = view.findViewById(R.id.edit_text_screen_time_goal);
        buttonSelectWakeUpTime = view.findViewById(R.id.button_select_wake_up_time);
        btnNext = view.findViewById(R.id.button_next);
        textViewTitle = view.findViewById(R.id.text_view_title);

        btnNext.setOnClickListener(v -> handleNextStep());
        buttonSelectWakeUpTime.setOnClickListener(v -> showTimePickerDialog());

        view.findViewById(R.id.button_go_to_login).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchAuthFragment(new LoginFragment());
            }
        });
        SharedPreferences prefs = requireActivity().getSharedPreferences("registration_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("is_registering", true).apply();

        return view;
    }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_STEP, currentStep);
        outState.putString(KEY_EMAIL, etEmail.getText().toString());
        outState.putString(KEY_PASSWORD, etPass.getText().toString());
        outState.putString(KEY_CONFIRM_PASSWORD, etConfirmPass.getText().toString());
        outState.putString(KEY_NAME, etName.getText().toString());
        outState.putString(KEY_SCREEN_TIME, etScreenTime.getText().toString());
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
            etScreenTime.setText(savedInstanceState.getString(KEY_SCREEN_TIME, ""));
            selectedWakeUpTime = savedInstanceState.getString(KEY_WAKE_UP_TIME);

            if (selectedWakeUpTime != null && !selectedWakeUpTime.isEmpty()) {
                etWakeUpTime.setText(selectedWakeUpTime);
            }

            showStep(currentStep);
        }
    }
    private void showTimePickerDialog(){
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

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Введите корректный email");
            return;
        }
        showStep(2);
    }

    private void validatePasswordStep() {
        String pass = etPass.getText().toString();
        String confirmPass = etConfirmPass.getText().toString();

        if (pass.length() < 6) {
            etPass.setError("Минимум 6 символов");
            return;
        }
        if (!pass.equals(confirmPass)) {
            etConfirmPass.setError("Пароли не совпадают");
            return;
        }
        showStep(3);
    }

    private void showStep(int step) {
        currentStep = step;

        step1Container.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        step2Container.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        step3Container.setVisibility(step == 3 ? View.VISIBLE : View.GONE);

        if (step == 1) textViewTitle.setText("Ваша почта");
        if (step == 2) textViewTitle.setText("Придумайте пароль");
        if (step == 3) {
            textViewTitle.setText("Почти готово!");
            btnNext.setText("Завершить");
        }
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPass.getText().toString();
        String name = etName.getText().toString().trim();
        String screenTime = etScreenTime.getText().toString().trim();
        String wakeUpTime = selectedWakeUpTime != null ? selectedWakeUpTime : "07:00";
        String screenTimeGoalStr = etScreenTimeGoal.getText().toString().trim();

        if (selectedWakeUpTime == null || selectedWakeUpTime.isEmpty()) {
            etWakeUpTime.setError("Выберите время пробуждения");
            return;
        }
        if (name.isEmpty()) {
            etName.setError("Введите ваше имя");
            return;
        }
        if (screenTimeGoalStr.isEmpty()) {
            etScreenTimeGoal.setError("Введите цель");
            return;
        }
        int screenTimeGoal = Integer.parseInt(screenTimeGoalStr);
        if (screenTimeGoal < 1 || screenTimeGoal > 24) {
            etScreenTimeGoal.setError("Цель от 1 до 24 часов");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
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
                                        Log.d("Registration", "Данные пользователя сохранены");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Registration", "Ошибка: " + e.getMessage());
                                    });
                            showVerificationDialog(user);
                        }
                    } else {
                        Toast.makeText(getActivity(), "Ошибка: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showVerificationDialog(FirebaseUser user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Подтвердите email");
        builder.setMessage("Письмо отправлено на " + user.getEmail() +
                "\n\nНажмите на ссылку в письме, затем вернитесь в приложение.");
        builder.setPositiveButton("Я подтвердил", (dialog, which) -> {
            user.reload().addOnCompleteListener(task -> {
                if (user.isEmailVerified()) {
                    SharedPreferences prefs = requireActivity().getSharedPreferences("registration_prefs", Context.MODE_PRIVATE);
                    prefs.edit().putBoolean("is_registering", false).apply();
                    new FirstLaunchManager(getActivity()).setFirstLaunchDone();
                    ((MainActivity) getActivity()).goToHomeFragment();
                } else {
                    Toast.makeText(getActivity(), "Email ещё не подтверждён", Toast.LENGTH_SHORT).show();
                    showVerificationDialog(user);
                }
            });
        });
        builder.setNegativeButton("Отправить снова", (dialog, which) -> {
            user.sendEmailVerification();
            Toast.makeText(getActivity(), "Письмо отправлено повторно", Toast.LENGTH_SHORT).show();
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
}