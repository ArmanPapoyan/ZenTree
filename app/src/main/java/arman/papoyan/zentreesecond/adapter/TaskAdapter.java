package arman.papoyan.zentreesecond.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import arman.papoyan.zentreesecond.R;
import arman.papoyan.zentreesecond.model.Task;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> tasks = new ArrayList<>();
    private OnTaskClickListener listener;
    private OnTaskCheckedChangeListener checkListener;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public interface OnTaskCheckedChangeListener {
        void onTaskChecked(Task task, boolean isChecked);
    }

    public void setTasks(List<Task> tasks) {
        Collections.sort(tasks, new Comparator<Task>() {
            @Override
            public int compare(Task t1, Task t2) {
                if (t1.isCompleted() != t2.isCompleted()) {
                    return Boolean.compare(t1.isCompleted(), t2.isCompleted());
                }
                if (t1.getPriority() != t2.getPriority()) {
                    return Integer.compare(t1.getPriority(), t2.getPriority());
                }
                int time1 = t1.getTargetHour() * 60 + t1.getTargetMinute();
                int time2 = t2.getTargetHour() * 60 + t2.getTargetMinute();
                return Integer.compare(time1, time2);
            }
        });
        this.tasks = tasks;
        notifyDataSetChanged();
    }

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.listener = listener;
    }

    public void setOnTaskCheckedChangeListener(OnTaskCheckedChangeListener checkListener) {
        this.checkListener = checkListener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task currentTask = tasks.get(position);
        holder.textViewTitle.setText(currentTask.getTitle());
        holder.textViewDescription.setText(currentTask.getDescription());
        holder.checkBoxCompleted.setChecked(currentTask.isCompleted());

        String timeText = formatTimeText(currentTask);
        holder.textViewTime.setText(timeText);

        holder.textViewPriority.setText(String.valueOf(currentTask.getPriority()));

        switch (currentTask.getPriority()) {
            case 1:
                holder.textViewPriority.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_red_dark));
                break;
            case 2:
                holder.textViewPriority.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_orange_dark));
                break;
            case 3:
                holder.textViewPriority.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_green_dark));
                break;
        }

        holder.checkBoxCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (checkListener != null) {
                currentTask.setCompleted(isChecked);
                checkListener.onTaskChecked(currentTask, isChecked);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClick(currentTask);
            }
        });
    }

    private String formatTimeText(Task task) {
        String timeString = "";
        String targetTime = String.format("%02d:%02d", task.getTargetHour(), task.getTargetMinute());
        String endTime = String.format("%02d:%02d", task.getEndHour(), task.getEndMinute());

        switch (task.getTimeType()) {
            case 1:
                timeString = "До " + targetTime;
                break;
            case 2:
                timeString = "В " + targetTime;
                break;
            case 3:
                timeString = "После " + targetTime;
                break;
            case 4:
                timeString = targetTime + " - " + endTime;
                break;
        }
        return timeString;
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewTitle;
        private TextView textViewDescription;
        private TextView textViewPriority;
        private TextView textViewTime;
        private CheckBox checkBoxCompleted;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.text_view_title);
            textViewDescription = itemView.findViewById(R.id.text_view_description);
            textViewPriority = itemView.findViewById(R.id.text_view_priority);
            textViewTime = itemView.findViewById(R.id.text_view_time);
            checkBoxCompleted = itemView.findViewById(R.id.checkbox_completed);
        }
    }
}