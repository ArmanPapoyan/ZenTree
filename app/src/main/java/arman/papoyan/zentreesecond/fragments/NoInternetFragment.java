package arman.papoyan.zentreesecond.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import arman.papoyan.zentreesecond.MainActivity;
import arman.papoyan.zentreesecond.R;

public class NoInternetFragment extends Fragment {

    private Button bt;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_no_internet, container, false);
        bt = view.findViewById(R.id.button_retry);
        bt.setOnClickListener(View ->{
            ((MainActivity) getActivity()).retryConnection();
        });
        return view;
    }
}
