package arman.papoyan.zentreesecond.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import arman.papoyan.zentreesecond.R;
import arman.papoyan.zentreesecond.models.FocusStats;

public class StatisticsAdapter extends RecyclerView.Adapter<StatisticsAdapter.ViewHolder> {

    private List<FocusStats> statsList;
    private Context context;

    public StatisticsAdapter(List<FocusStats> statsList) {
        this.statsList = statsList;
    }

    public void setStats(List<FocusStats> stats) {
        this.statsList = stats;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_statistics, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FocusStats stats = statsList.get(position);

        holder.textViewDate.setText(stats.getDate());
        long hours = stats.getFocusMinutes() / 60;
        long minutes = stats.getFocusMinutes() % 60;

        String focusText = String.format(Locale.getDefault(),
                context.getString(R.string.stats_focus_hours), hours, minutes);
        holder.textViewFocus.setText(focusText);

        String tasksText = String.format(Locale.getDefault(),
                context.getString(R.string.stats_tasks_completed), stats.getTasksCompleted());
        holder.textViewTasks.setText(tasksText);

        String levelText = String.format(Locale.getDefault(),
                context.getString(R.string.stats_tree_level), stats.getTreeLevel());
        holder.textViewLevel.setText(levelText);
    }

    @Override
    public int getItemCount() {
        return statsList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewDate, textViewFocus, textViewTasks, textViewLevel;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewDate = itemView.findViewById(R.id.text_view_date);
            textViewFocus = itemView.findViewById(R.id.text_view_focus);
            textViewTasks = itemView.findViewById(R.id.text_view_tasks);
            textViewLevel = itemView.findViewById(R.id.text_view_level);
        }
    }
}