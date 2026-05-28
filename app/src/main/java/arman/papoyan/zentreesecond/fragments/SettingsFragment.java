package arman.papoyan.zentreesecond.fragments;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import arman.papoyan.zentreesecond.MainActivity;
import arman.papoyan.zentreesecond.R;
import arman.papoyan.zentreesecond.views.FallingLeavesView;

public class SettingsFragment extends Fragment {

    private SharedPreferences settingsPrefs;
    private Switch switchLeaves;
    private SeekBar seekBarLeafAlpha;
    private TextView textViewAlphaValue;
    private Button btnChangeName;
    private Button btnChangeSkin;
    private FallingLeavesView fallingLeavesView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        settingsPrefs = requireActivity().getSharedPreferences("settings_prefs", android.content.Context.MODE_PRIVATE);

        if (getActivity() instanceof MainActivity) {
            fallingLeavesView = ((MainActivity) getActivity()).getFallingLeavesView();
        }

        switchLeaves = view.findViewById(R.id.switch_leaves);
        seekBarLeafAlpha = view.findViewById(R.id.seekbar_leaf_alpha);
        textViewAlphaValue = view.findViewById(R.id.text_view_alpha_value);
        btnChangeName = view.findViewById(R.id.btn_change_name);
        btnChangeSkin = view.findViewById(R.id.btn_change_skin);

        loadSettings();

        switchLeaves.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsPrefs.edit().putBoolean("leaves_enabled", isChecked).apply();
            if (fallingLeavesView != null) {
                if (isChecked) {
                    fallingLeavesView.startFalling();
                } else {
                    fallingLeavesView.stopAnimation();
                }
            }
        });

        seekBarLeafAlpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int alpha = progress * 255 / 100;
                textViewAlphaValue.setText(progress + "%");
                settingsPrefs.edit().putInt("leaves_alpha", progress).apply();
                if (fallingLeavesView != null) {
                    fallingLeavesView.setLeafAlpha(alpha);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnChangeName.setOnClickListener(v -> showChangeNameDialog());
        btnChangeSkin.setOnClickListener(v -> showSkinPickerDialog());

        return view;
    }

    private void loadSettings() {
        boolean leavesEnabled = settingsPrefs.getBoolean("leaves_enabled", true);
        int alphaProgress = settingsPrefs.getInt("leaves_alpha", 20);

        switchLeaves.setChecked(leavesEnabled);
        seekBarLeafAlpha.setProgress(alphaProgress);
        textViewAlphaValue.setText(alphaProgress + "%");

        if (fallingLeavesView != null) {
            int alpha = alphaProgress * 255 / 100;
            fallingLeavesView.setLeafAlpha(alpha);
        }

        String savedSkin = settingsPrefs.getString("leaf_skin", "spring");
        if (fallingLeavesView != null) {
            fallingLeavesView.setSkin(savedSkin);
        }
    }

    private void showSkinPickerDialog() {
        String[] skins = {"🌸 Весенний", "🍂 Осенний", "❄️ Зимний", "🌧️ Дождливый"};
        String[] skinValues = {"spring", "autumn", "snow", "rain"};
        int currentSkin = getCurrentSkinIndex();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("🎨 Выберите скин");
        builder.setSingleChoiceItems(skins, currentSkin, (dialog, which) -> {
            String selectedSkin = skinValues[which];
            settingsPrefs.edit().putString("leaf_skin", selectedSkin).apply();

            if (fallingLeavesView != null) {
                fallingLeavesView.setSkin(selectedSkin);
            }

            dialog.dismiss();
            Toast.makeText(getContext(), "Скин изменён!", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private int getCurrentSkinIndex() {
        String currentSkin = settingsPrefs.getString("leaf_skin", "spring");
        switch (currentSkin) {
            case "spring": return 0;
            case "autumn": return 1;
            case "snow": return 2;
            case "rain": return 3;
            default: return 0;
        }
    }

    private void showChangeNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.change_name));

        EditText input = new EditText(getActivity());
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getDisplayName() != null) {
            input.setText(user.getDisplayName());
        }
        input.setHint("Введите новое имя");

        builder.setView(input);
        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                updateUserName(newName);
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void updateUserName(String newName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();

        user.updateProfile(request)
                .addOnSuccessListener(aVoid -> {
                    FirebaseFirestore.getInstance()
                            .collection("users").document(user.getUid())
                            .update("name", newName)
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(getContext(), "Имя обновлено", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}