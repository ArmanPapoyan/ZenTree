package arman.papoyan.zentreesecond.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import arman.papoyan.zentreesecond.R;
import arman.papoyan.zentreesecond.adapter.StatisticsAdapter;
import arman.papoyan.zentreesecond.models.FocusStats;

public class StatisticsFragment extends Fragment {

    private TextView textViewMyTotalHours;
    private TextView textViewMyStreak;
    private TextView textViewWeekTotal;
    private TextView textViewMonthTotal;
    private EditText editTextSearch;
    private ImageButton buttonSearch;
    private TextView textViewSearchResult;
    private RecyclerView recyclerViewStats;
    private BarChart barChart;
    private ListenerRegistration treeListener;
    private StatisticsAdapter adapter;
    private List<FocusStats> statsList;
    private FirebaseFirestore db;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        textViewMyTotalHours = view.findViewById(R.id.text_view_my_total_hours);
        textViewMyStreak = view.findViewById(R.id.text_view_my_streak);
        textViewWeekTotal = view.findViewById(R.id.text_view_week_total);
        textViewMonthTotal = view.findViewById(R.id.text_view_month_total);
        editTextSearch = view.findViewById(R.id.edit_text_search);
        buttonSearch = view.findViewById(R.id.button_search);
        textViewSearchResult = view.findViewById(R.id.text_view_search_result);
        recyclerViewStats = view.findViewById(R.id.recycler_view_stats);
        barChart = view.findViewById(R.id.bar_chart);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        statsList = new ArrayList<>();
        adapter = new StatisticsAdapter(statsList);
        recyclerViewStats.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewStats.setAdapter(adapter);
        ImageButton btnShareStats = view.findViewById(R.id.btn_share_stats);
        btnShareStats.setOnClickListener(v -> shareStats());

        loadMyStats();
        loadWeekStats();
        loadMonthStats();

        buttonSearch.setOnClickListener(v -> searchUser());

        return view;
    }
    private void listenToTreeChanges() {
        if (currentUserId == null) return;

        treeListener = db.collection("users").document(currentUserId)
                .collection("tree").document("progress")
                .addSnapshotListener((snapshot, error) -> {
                    if (snapshot != null && snapshot.exists()) {
                        loadMyStats();
                        loadWeekStats();
                        loadMonthStats();
                    }
                });
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (treeListener != null) {
            treeListener.remove();
        }
    }
    private String getStatsShareText() {
        String totalHours = textViewMyTotalHours.getText().toString();
        String streak = textViewMyStreak.getText().toString();
        String weekTotal = textViewWeekTotal.getText().toString();
        String monthTotal = textViewMonthTotal.getText().toString();

        return String.format(Locale.getDefault(),
                "📊 Моя статистика в Zen Tree!\n\n" +
                        "🌳 Всего часов: %s\n" +
                        "🔥 Дней подряд: %s\n" +
                        "📈 %s\n" +
                        "📅 %s\n\n" +
                        "#ZenTree #Focus #Statistics",
                totalHours, streak, weekTotal, monthTotal);
    }
    private Bitmap takeScreenshot(View view) {
        view.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
        view.setDrawingCacheEnabled(false);
        return bitmap;
    }
    private String saveBitmapToCache(Bitmap bitmap) {
        try {
            File cacheDir = requireContext().getCacheDir();
            File file = new File(cacheDir, "stats_share_" + System.currentTimeMillis() + ".png");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private void shareStats() {
        View rootView = getView();
        if (rootView == null) return;

        Bitmap screenshot = takeScreenshot(rootView);
        if (screenshot == null) return;

        String path = saveBitmapToCache(screenshot);
        if (path == null) return;

        String shareText = getStatsShareText();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/png");
        shareIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                new File(path)
        ));
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_stats_title)));
    }

    private void loadMyStats() {
        if (currentUserId == null) return;

        db.collection("users").document(currentUserId).collection("stats")
                .get()
                .addOnSuccessListener(snapshots -> {
                    long totalMinutes = 0;
                    int currentStreak = 0;
                    String lastDate = null;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        FocusStats stats = doc.toObject(FocusStats.class);
                        totalMinutes += stats.getFocusMinutes();

                        if (stats.getFocusMinutes() > 0) {
                            currentStreak++;
                        } else {
                            currentStreak = 0;
                        }
                    }

                    long totalHours = totalMinutes / 60;
                    textViewMyTotalHours.setText(String.format(Locale.getDefault(), "%d ч", totalHours));
                    textViewMyStreak.setText(currentStreak + " дн.");
                });
    }
    @Override
    public void onResume() {
        super.onResume();
        loadMyStats();
        loadWeekStats();
        loadMonthStats();
    }
    private void loadWeekStats() {
        if (currentUserId == null) return;

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        Date weekAgo = calendar.getTime();
        String weekAgoStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(weekAgo);

        db.collection("users").document(currentUserId).collection("stats")
                .whereGreaterThanOrEqualTo("date", weekAgoStr)
                .orderBy("date")
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<BarEntry> entries = new ArrayList<>();
                    List<String> dates = new ArrayList<>();
                    long weekTotal = 0;
                    int index = 0;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        FocusStats stats = doc.toObject(FocusStats.class);
                        float hours = stats.getFocusMinutes() / 60f;
                        entries.add(new BarEntry(index, hours));
                        dates.add(stats.getDate().substring(5));
                        weekTotal += stats.getFocusMinutes();
                        index++;
                    }

                    long weekHours = weekTotal / 60;
                    textViewWeekTotal.setText(String.format(Locale.getDefault(), "За неделю: %d ч", weekHours));

                    setupBarChart(entries, dates);
                });
    }

    private void loadMonthStats() {
        if (currentUserId == null) return;

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -30);
        Date monthAgo = calendar.getTime();
        String monthAgoStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(monthAgo);

        db.collection("users").document(currentUserId).collection("stats")
                .whereGreaterThanOrEqualTo("date", monthAgoStr)
                .orderBy("date")
                .get()
                .addOnSuccessListener(snapshots -> {
                    long monthTotal = 0;
                    for (QueryDocumentSnapshot doc : snapshots) {
                        FocusStats stats = doc.toObject(FocusStats.class);
                        monthTotal += stats.getFocusMinutes();
                    }
                    long monthHours = monthTotal / 60;
                    textViewMonthTotal.setText(String.format(Locale.getDefault(), "За месяц: %d ч", monthHours));
                });
    }

    private void setupBarChart(List<BarEntry> entries, List<String> dates) {
        BarDataSet dataSet = new BarDataSet(entries, "Часы фокуса");
        dataSet.setColor(getResources().getColor(R.color.primary_green));
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);

        barChart.setData(barData);
        barChart.setFitBars(true);
        barChart.getDescription().setEnabled(false);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(dates));
        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisRight().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private void searchUser() {
        String searchText = editTextSearch.getText().toString().trim();
        if (searchText.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.stats_enter_email), Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Поиск...", Toast.LENGTH_SHORT).show();

        db.collection("users")
                .whereGreaterThanOrEqualTo("email", searchText)
                .whereLessThanOrEqualTo("email", searchText + "\uf8ff")
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        textViewSearchResult.setText(getString(R.string.stats_user_not_found));
                        statsList.clear();
                        adapter.notifyDataSetChanged();
                        barChart.clear();
                        barChart.invalidate();
                    } else {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            String userName = doc.getString("name");
                            String userEmail = doc.getString("email");
                            textViewSearchResult.setText("👤 " + userName + "\n📧 " + userEmail);
                            loadUserStats(doc.getId());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    textViewSearchResult.setText(getString(R.string.stats_search_error));
                    Log.e("SearchUser", "Ошибка поиска", e);
                });
    }

    private void loadUserStats(String userId) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        String weekAgoStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        db.collection("users").document(userId).collection("stats")
                .whereGreaterThanOrEqualTo("date", weekAgoStr)
                .orderBy("date")
                .get()
                .addOnSuccessListener(snapshots -> {
                    statsList.clear();
                    List<BarEntry> entries = new ArrayList<>();
                    List<String> dates = new ArrayList<>();
                    int index = 0;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        FocusStats stats = doc.toObject(FocusStats.class);
                        statsList.add(stats);
                        float hours = stats.getFocusMinutes() / 60f;
                        entries.add(new BarEntry(index, hours));
                        dates.add(stats.getDate().substring(5));
                        index++;
                    }

                    adapter.setStats(statsList);
                    setupBarChart(entries, dates);
                });
    }
}