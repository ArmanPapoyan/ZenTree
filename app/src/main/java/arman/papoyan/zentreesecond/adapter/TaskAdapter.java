package arman.papoyan.zentreesecond.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import arman.papoyan.zentreesecond.R;
import arman.papoyan.zentreesecond.models.Task;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> tasks = new ArrayList<>();
    private OnTaskClickListener listener;
    private OnTaskCheckedChangeListener checkListener;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onTaskLongClick(Task task);
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
                return Integer.compare(t1.getPriority(), t2.getPriority());
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
        View completedIndicator = holder.itemView.findViewById(R.id.view_completed_indicator);

        holder.checkBoxCompleted.setOnCheckedChangeListener(null);
        holder.checkBoxCompleted.setChecked(currentTask.isCompleted());
        holder.checkBoxCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (checkListener != null) {
                checkListener.onTaskChecked(currentTask, isChecked);
            }
        });

        if (currentTask.isCompleted()) {
            completedIndicator.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_green));
        } else {
            completedIndicator.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.default_indicator));
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onTaskLongClick(currentTask);
            }
            return true;
        });

        String timeText = formatTimeText(holder.itemView.getContext(), currentTask);
        holder.textViewTime.setText(timeText);

        holder.textViewPriority.setText(String.valueOf(currentTask.getPriority()));
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String taskDate = currentTask.getTargetDate();


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

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClick(currentTask);
            }
        });
        if (taskDate != null && taskDate.compareTo(today) > 0) {
            holder.itemView.setAlpha(0.5f);
            holder.checkBoxCompleted.setEnabled(false);
        } else {
            holder.itemView.setAlpha(1f);
            holder.checkBoxCompleted.setEnabled(true);
        }

    }

    private String formatTimeText(Context context, Task task) {
        String timeString = "";
        String targetTime = String.format("%02d:%02d", task.getTargetHour(), task.getTargetMinute());
        String endTime = String.format("%02d:%02d", task.getEndHour(), task.getEndMinute());

        switch (task.getTimeType()) {
            case 1:
                timeString = context.getString(R.string.task_time_before, targetTime);
                break;
            case 2:
                timeString = context.getString(R.string.task_time_at, targetTime);
                break;
            case 3:
                timeString = context.getString(R.string.task_time_after, targetTime);
                break;
            case 4:
                timeString = context.getString(R.string.task_time_range, targetTime, endTime);
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