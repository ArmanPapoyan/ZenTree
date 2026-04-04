package arman.papoyan.zentreesecond.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
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

import arman.papoyan.zentreesecond.MainActivity;
import arman.papoyan.zentreesecond.R;
import arman.papoyan.zentreesecond.utils.FirstLaunchManager;

public class RegistrationFragment extends Fragment {

    private FirebaseAuth mAuth;
    private int currentStep = 1;

    private View step1Container, step2Container, step3Container;

    private EditText etEmail, etPass, etConfirmPass, etName, etScreenTime;

    private Button btnNext;
    private TextView textViewTitle;

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

        btnNext = view.findViewById(R.id.button_next);
        textViewTitle = view.findViewById(R.id.text_view_title);

        btnNext.setOnClickListener(v -> handleNextStep());

        view.findViewById(R.id.button_go_to_login).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchAuthFragment(new LoginFragment());
            }
        });

        return view;
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

        if (name.isEmpty()) {
            etName.setError("Введите ваше имя");
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
}