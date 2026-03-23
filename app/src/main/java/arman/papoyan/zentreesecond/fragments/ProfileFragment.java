package arman.papoyan.zentreesecond.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import arman.papoyan.zentreesecond.R;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private TextView textViewName;
    private TextView textViewEmail;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        textViewName = view.findViewById(R.id.text_view_name);
        textViewEmail = view.findViewById(R.id.text_view_email);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            textViewEmail.setText(user.getEmail());
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                textViewName.setText(displayName);
            } else {
                textViewName.setText("Не указано");
            }
        } else {
            textViewName.setText("Не авторизован");

        }

        return view;
    }
}