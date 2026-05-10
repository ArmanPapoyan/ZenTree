package arman.papoyan.zentreesecond.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
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

import arman.papoyan.zentreesecond.MainActivity;
import arman.papoyan.zentreesecond.R;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private TextView textViewName;
    private TextView textViewEmail;
    private SharedPreferences prefs;
    private boolean isGuest;
    private SharedPreferences themePrefs;
    private Button deleteButton;
    private Button btnThemeSystem, btnThemeLight, btnThemeDark;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        themePrefs = requireActivity().getSharedPreferences("theme_prefs", MODE_PRIVATE);
        textViewName = view.findViewById(R.id.text_view_name);
        textViewEmail = view.findViewById(R.id.text_view_email);
        Button out = view.findViewById(R.id.button_logout);
        deleteButton = view.findViewById(R.id.button_delete);
        prefs = getActivity().getSharedPreferences("login_prefs", MODE_PRIVATE);
        isGuest = prefs.getBoolean("is_guest", false);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        btnThemeSystem = view.findViewById(R.id.btn_theme_system);
        btnThemeLight = view.findViewById(R.id.btn_theme_light);
        btnThemeDark = view.findViewById(R.id.btn_theme_dark);


        displayUserInfo(user);

        if (isGuest || (user != null && user.isAnonymous())) {
            deleteButton.setVisibility(View.GONE);
        } else {
            deleteButton.setVisibility(View.VISIBLE);
        }
        out.setOnClickListener(v -> logout());


        btnThemeSystem.setOnClickListener(v -> setThemeMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
        btnThemeLight.setOnClickListener(v -> setThemeMode(AppCompatDelegate.MODE_NIGHT_NO));
        btnThemeDark.setOnClickListener(v -> setThemeMode(AppCompatDelegate.MODE_NIGHT_YES));

        deleteButton.setOnClickListener(v -> deleteAccount());
        return view;
    }
    private void setThemeMode(int mode) {
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
    }
    private void showPasswordDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String email = user.getEmail();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        EditText passwordInput = new EditText(getActivity());
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setHint("Введите пароль");
        passwordInput.setPadding(40, 20, 40, 20);

        builder.setTitle("Введите пароль")
                .setMessage(R.string.password_text)
                .setView(passwordInput)
                .setPositiveButton("Подтвердить", (dialog, which) -> {
                    String password = passwordInput.getText().toString();
                    if (password.isEmpty()) {
                        Toast.makeText(getActivity(), "Введите пароль", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    reauthenticateUser(email, password);
                })
                .setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss())
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
                    Toast.makeText(getActivity(), "Неверный пароль: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
    private void showDeleteDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Удалить аккаунт?")
                .setMessage(R.string.delete_text)
                .setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user == null) {
                            return;
                        }
                        String userId = user.getUid();
                        ProgressDialog progressDialog = new ProgressDialog(getActivity());
                        progressDialog.setMessage("Удаление аккаунта...");
                        progressDialog.setCancelable(false);
                        progressDialog.show();
                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                        db.collection("tasks").document(userId).collection("userTasks")
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                        document.getReference().delete();
                                    }
                                    db.collection("users").document(userId).collection("tree").document("progress")
                                            .delete()
                                            .addOnSuccessListener(aVoid -> {
                                                user.delete()
                                                        .addOnSuccessListener(aVoid2 -> {
                                                            prefs.edit().clear().apply();
                                                            FirebaseAuth.getInstance().signOut();
                                                            progressDialog.dismiss();
                                                            goToLoginFragment();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            progressDialog.dismiss();
                                                            Toast.makeText(getActivity(), "Ошибка удаления аккаунта: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                        });
                                            })
                                            .addOnFailureListener(e -> {
                                                progressDialog.dismiss();
                                                Toast.makeText(getActivity(), "Ошибка удаления дерева: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(getActivity(), "Не удалось получить задачи: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    }
                })
                .setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss())
                .setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
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
            Toast.makeText(getContext(), "Неизвестный тип аккаунта", Toast.LENGTH_SHORT).show();
        }
    }
    private void deleteGoogleAccount() {
        showDeleteDialogForGoogle();
    }
    private void showDeleteDialogForGoogle() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Удалить аккаунт?")
                .setMessage(R.string.delete_text)
                .setPositiveButton("Удалить", (dialog, which) -> {
                    performGoogleAccountDeletion();
                })
                .setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss())
                .setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
    }
    private void performGoogleAccountDeletion() {
        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage("Удаление аккаунта...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(requireContext(), gso);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.delete()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            googleSignInClient.revokeAccess()
                                    .addOnCompleteListener(revokeTask -> {
                                        progressDialog.dismiss();
                                        if (revokeTask.isSuccessful()) {
                                            deleteUserDataFromFirestore();
                                            Toast.makeText(getContext(), "Аккаунт удалён", Toast.LENGTH_SHORT).show();
                                            navigateToLogin();
                                        } else {
                                            Toast.makeText(getContext(), "Ошибка при отзыве доступа Google", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(getContext(), "Ошибка удаления: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
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

}