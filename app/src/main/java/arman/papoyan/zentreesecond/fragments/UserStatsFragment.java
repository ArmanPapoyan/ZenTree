package arman.papoyan.zentreesecond.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import arman.papoyan.zentreesecond.R;
import arman.papoyan.zentreesecond.adapter.StatisticsAdapter;
import arman.papoyan.zentreesecond.models.FocusStats;

public class UserStatsFragment extends Fragment {

    private String userId;
    private String userName;
    private String userEmail;

    private TextView textViewName;
    private TextView textViewEmail;
    private TextView textViewTotalHours;
    private TextView textViewTotalDays;
    private RecyclerView recyclerViewStats;
    private StatisticsAdapter adapter;
    private List<FocusStats> statsList;

    private FirebaseFirestore db;

    public static UserStatsFragment newInstance(String userId, String userName, String userEmail) {
        UserStatsFragment fragment = new UserStatsFragment();
        Bundle args = new Bundle();
        args.putString("user_id", userId);
        args.putString("user_name", userName);
        args.putString("user_email", userEmail);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_stats, container, false);

        if (getArguments() != null) {
            userId = getArguments().getString("user_id");
            userName = getArguments().getString("user_name");
            userEmail = getArguments().getString("user_email");
        }

        textViewName = view.findViewById(R.id.text_view_user_name);
        textViewEmail = view.findViewById(R.id.text_view_user_email);
        textViewTotalHours = view.findViewById(R.id.text_view_total_hours);
        textViewTotalDays = view.findViewById(R.id.text_view_total_days);
        recyclerViewStats = view.findViewById(R.id.recycler_view_user_stats);

        db = FirebaseFirestore.getInstance();
        statsList = new ArrayList<>();
        adapter = new StatisticsAdapter(statsList);
        recyclerViewStats.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewStats.setAdapter(adapter);

        displayUserInfo();
        loadUserStats();

        return view;
    }

    private void displayUserInfo() {
        textViewName.setText(userName != null ? userName : "User");
        textViewEmail.setText(userEmail != null ? userEmail : "Email не указан");
    }

    private void loadUserStats() {
        db.collection("users").document(userId).collection("stats")
                .get()
                .addOnSuccessListener(snapshots -> {
                    long totalMinutes = 0;
                    int totalDays = snapshots.size();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        FocusStats stats = doc.toObject(FocusStats.class);
                        totalMinutes += stats.getFocusMinutes();
                    }

                    long totalHours = totalMinutes / 60;
                    textViewTotalHours.setText(String.format(Locale.getDefault(), "%d %s", totalHours, "ч"));
                    textViewTotalDays.setText(String.format(Locale.getDefault(), "%d %s", totalDays, "дн."));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка загрузки статистики", Toast.LENGTH_SHORT).show();
                });

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        Date weekAgo = calendar.getTime();
        String weekAgoStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(weekAgo);

        db.collection("users").document(userId).collection("stats")
                .whereGreaterThanOrEqualTo("date", weekAgoStr)
                .orderBy("date")
                .get()
                .addOnSuccessListener(snapshots -> {
                    statsList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        FocusStats stats = doc.toObject(FocusStats.class);
                        statsList.add(stats);
                    }
                    adapter.setStats(statsList);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка загрузки статистики", Toast.LENGTH_SHORT).show();
                });
    }
}