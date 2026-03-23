package arman.papoyan.zentreesecond.fragments;

import android.app.AlertDialog;
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
import com.google.firebase.auth.UserProfileChangeRequest;

import arman.papoyan.zentreesecond.MainActivity;
import arman.papoyan.zentreesecond.R;
import arman.papoyan.zentreesecond.utils.FirstLaunchManager;

public class RegistrationFragment extends Fragment {
    FirebaseAuth mAuth;
    EditText editTextEmail;
    EditText editTextPassword;
    EditText editTextName;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registration , container, false);
        Button start = view.findViewById(R.id.button_start);
        editTextEmail = view.findViewById(R.id.edit_text_email);
        editTextPassword = view.findViewById(R.id.edit_text_password);
        mAuth = FirebaseAuth.getInstance();
        editTextName = view.findViewById(R.id.edit_text_name);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editTextEmail.getText().toString();
                String password = editTextPassword.getText().toString();
                String name = editTextName.getText().toString();
                if(email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(getActivity(), "Заполните все поля", Toast.LENGTH_SHORT).show();
                } else {
                    mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(getActivity(), task -> {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    user.sendEmailVerification();
                                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                            .setDisplayName(name)
                                            .build();
                                    user.updateProfile(profileUpdates);
                                    showVerificationDialog(user);
                                } else {
                                    Toast.makeText(getActivity(), "Ошибка: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                }
            }
        });
        return view;
    }
    private void showVerificationDialog(FirebaseUser user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Подтверждение email");
        builder.setMessage("Письмо отправлено на " + user.getEmail() +
                "\n\nНажмите на ссылку в письме, затем вернитесь в приложение");
        builder.setPositiveButton("Я подтвердил", (dialog, which) -> {
            user.reload().addOnCompleteListener(task -> {
                if (user.isEmailVerified()) {
                    FirstLaunchManager firstLaunchManager = new FirstLaunchManager(getActivity());
                    firstLaunchManager.setFirstLaunchDone();
                    MainActivity activity = (MainActivity) getActivity();
                    activity.goToHomeFragment();
                } else {
                    Toast.makeText(getActivity(), "Email ещё не подтверждён", Toast.LENGTH_SHORT).show();
                }
            });
        });
        builder.setNegativeButton("Отправить снова", (dialog, which) -> {
            user.sendEmailVerification();
            Toast.makeText(getActivity(), "Письмо отправлено повторно", Toast.LENGTH_SHORT).show();
        });
        builder.setCancelable(false);
        builder.show();
    }
}
