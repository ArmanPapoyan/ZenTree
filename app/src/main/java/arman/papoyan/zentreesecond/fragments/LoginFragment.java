package arman.papoyan.zentreesecond.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import arman.papoyan.zentreesecond.MainActivity;
import arman.papoyan.zentreesecond.R;

public class LoginFragment extends Fragment {

    private FirebaseAuth mAuth;
    private EditText editTextEmail;
    private EditText editTextPassword;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        Button buttonLogin = view.findViewById(R.id.button_login);
        Button buttonRegister = view.findViewById(R.id.button_register);
        editTextEmail = view.findViewById(R.id.edit_text_email);
        editTextPassword = view.findViewById(R.id.edit_text_password);

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
                                    MainActivity activity = (MainActivity) getActivity();
                                    activity.goToHomeAfterLogin();
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

        return view;
    }
}