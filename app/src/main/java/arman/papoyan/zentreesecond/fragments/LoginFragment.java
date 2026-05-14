package arman.papoyan.zentreesecond.fragments;

import static android.content.Context.MODE_PRIVATE;

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
        Button buttonGuest = view.findViewById(R.id.guest);
        editTextEmail = view.findViewById(R.id.edit_text_email);
        editTextPassword = view.findViewById(R.id.edit_text_password);
        checkBoxRememberMe = view.findViewById(R.id.checkBox_remember_me);
        prefs = getActivity().getSharedPreferences("login_prefs", MODE_PRIVATE);
        Button btnGoogle = view.findViewById(R.id.btn_google_sign_in);
        btnGoogle.setOnClickListener(v -> signInWithGoogle());

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(getActivity(), gso);

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
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(getActivity(), "Ошибка входа: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(getActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        checkUserInFirestore(user);
                    } else {
                        Toast.makeText(getActivity(), "Ошибка аутентификации", Toast.LENGTH_LONG).show();
                    }
                });
    }
}