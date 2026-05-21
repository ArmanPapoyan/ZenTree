package arman.papoyan.zentreesecond.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import arman.papoyan.zentreesecond.R;
import arman.papoyan.zentreesecond.adapter.TaskAdapter;
import arman.papoyan.zentreesecond.models.Task;
import arman.papoyan.zentreesecond.utils.SyncQueueManager;
import arman.papoyan.zentreesecond.utils.TaskNotificationScheduler;

public class TasksFragment extends Fragment {

    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private List<Task> taskList;
    private FloatingActionButton fabAddTask;

    private FirebaseFirestore db;
    private String userId;

    private ListenerRegistration tasksListener;

    private int selectedTimeType = 1;
    private int selectedTargetHour = 12;
    private int selectedTargetMinute = 0;
    private int selectedEndHour = 13;
    private int selectedEndMinute = 0;
    private int dailyTaskMinutes = 0;
    private String selectedDate;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_tasks);
        fabAddTask = view.findViewById(R.id.fab_add_task);

        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            loadTasksFromFirestore();
        } else {
            Toast.makeText(getActivity(), getString(R.string.error_sign_in_required), Toast.LENGTH_SHORT).show();
            return view;
        }

        SharedPreferences prefs = getActivity().getSharedPreferences("task_rewards", Context.MODE_PRIVATE);
        String lastDate = prefs.getString("last_date", "");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (!today.equals(lastDate)) {
            dailyTaskMinutes = 0;
            prefs.edit().putInt("daily_minutes", 0).putString("last_date", today).apply();
        } else {
            dailyTaskMinutes = prefs.getInt("daily_minutes", 0);
        }

        taskList = new ArrayList<>();
        adapter = new TaskAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);

        adapter.setOnTaskClickListener(new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(Task task) {
            }

            @Override
            public void onTaskLongClick(Task task) {
                showEditTaskDialog(task);
            }
        });
        setupSwipeToDelete();
        adapter.setOnTaskCheckedChangeListener((task, isChecked) -> {
            if (task.isCompleted() == isChecked) return;

            String taskId = task.getId();
            task.setCompleted(isChecked);

            if (isChecked) {
                TaskNotificationScheduler.cancelAllTaskNotifications(getContext(), task);
            }

            db.collection("tasks").document(userId)
                    .collection("userTasks").document(taskId)
                    .update("completed", isChecked)
                    .addOnFailureListener(e -> {
                        task.setCompleted(!isChecked);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(getActivity(), getString(R.string.error_sync_failed), Toast.LENGTH_SHORT).show();
                    });
        });


        fabAddTask.setOnClickListener(v -> showAddTaskDialog());

        return view;
    }

    private void loadTasksFromFirestore() {
        if (tasksListener != null) {
            tasksListener.remove();
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || !currentUser.getUid().equals(userId)) {
            return;
        }

        tasksListener = db.collection("tasks").document(userId).collection("userTasks")
                .addSnapshotListener((value, error) -> {
                    if (getActivity() == null || !isAdded()) {
                        return;
                    }

                    if (error != null) {
                        if (error.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            return;
                        }
                        Toast.makeText(getActivity(), getString(R.string.error_load_failed, error.getMessage()), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value == null) return;

                    List<Task> newTasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        Task task = doc.toObject(Task.class);
                        task.setId(doc.getId());
                        newTasks.add(task);
                    }

                    taskList.clear();
                    taskList.addAll(newTasks);
                    adapter.setTasks(taskList);
                });
    }

    public void removeListener() {
        if (tasksListener != null) {
            tasksListener.remove();
            tasksListener = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        removeListener();
    }


    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        EditText editTextTitle = dialogView.findViewById(R.id.edit_text_title);
        EditText editTextDescription = dialogView.findViewById(R.id.edit_text_description);
        RadioGroup radioGroupPriority = dialogView.findViewById(R.id.radio_group_priority);
        Button buttonSelectTime = dialogView.findViewById(R.id.button_select_time);
        TextView textViewSelectedTime = dialogView.findViewById(R.id.text_view_selected_time);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Button buttonSelectDate = dialogView.findViewById(R.id.button_select_date);
        TextView textViewSelectedDate = dialogView.findViewById(R.id.text_view_selected_date);

        buttonSelectTime.setOnClickListener(v -> showTimePickerDialog(textViewSelectedTime));
        buttonSelectDate.setOnClickListener(v -> showDatePickerDialog(textViewSelectedDate));

        builder.setTitle(getString(R.string.dialog_title_new_task))
                .setPositiveButton(getString(R.string.action_add), (dialog, which) -> {
                    String title = editTextTitle.getText().toString().trim();
                    String description = editTextDescription.getText().toString().trim();
                    int priority = 1;

                    int selectedId = radioGroupPriority.getCheckedRadioButtonId();
                    if (selectedId == R.id.radio_priority_2) {
                        priority = 2;
                    } else if (selectedId == R.id.radio_priority_3) {
                        priority = 3;
                    }

                    if (title.isEmpty()) {
                        Toast.makeText(getActivity(), getString(R.string.error_enter_title), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (selectedDate != null && selectedDate.compareTo(today) < 0) {
                        Toast.makeText(getActivity(), getString(R.string.error_past_date_invalid), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (selectedDate == null) {
                        selectedDate = today;
                    }

                    Task newTask = new Task(title, description, priority,
                            selectedTimeType, selectedTargetHour, selectedTargetMinute,
                            selectedEndHour, selectedEndMinute);


                    newTask.setTargetDate(selectedDate != null ? selectedDate : today);

                    String taskId = db.collection("tasks").document(userId)
                            .collection("userTasks").document().getId();
                    newTask.setId(taskId);

                    db.collection("tasks").document(userId)
                            .collection("userTasks").document(taskId)
                            .set(newTask)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getActivity(), getString(R.string.toast_task_added), Toast.LENGTH_SHORT).show();
                                scheduleTaskNotification(newTask);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getActivity(), getString(R.string.error_with_message, e.getMessage()), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton(getString(R.string.action_cancel), null);

        builder.create().show();
    }

    private void showTimePickerDialog(TextView textViewSelectedTime) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_time_picker, null);
        builder.setView(dialogView);

        RadioGroup radioGroupTimeType = dialogView.findViewById(R.id.radio_group_time_type);
        TimePicker timePickerStart = dialogView.findViewById(R.id.time_picker_start);
        TimePicker timePickerEnd = dialogView.findViewById(R.id.time_picker_end);
        TextView textViewEndLabel = dialogView.findViewById(R.id.text_view_end_label);

        timePickerStart.setIs24HourView(true);
        timePickerEnd.setIs24HourView(true);

        Calendar calendar = Calendar.getInstance();
        timePickerStart.setHour(calendar.get(Calendar.HOUR_OF_DAY));
        timePickerStart.setMinute(0);
        timePickerEnd.setHour(calendar.get(Calendar.HOUR_OF_DAY) + 1);
        timePickerEnd.setMinute(0);

        radioGroupTimeType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_range) {
                textViewEndLabel.setVisibility(View.VISIBLE);
                timePickerEnd.setVisibility(View.VISIBLE);
            } else {
                textViewEndLabel.setVisibility(View.GONE);
                timePickerEnd.setVisibility(View.GONE);
            }
        });

        builder.setTitle(getString(R.string.dialog_title_select_time))
                .setPositiveButton(getString(R.string.action_done), (dialog, which) -> {
                    int selectedId = radioGroupTimeType.getCheckedRadioButtonId();

                    selectedTargetHour = timePickerStart.getHour();
                    selectedTargetMinute = timePickerStart.getMinute();

                    if (selectedId == R.id.radio_before) {
                        selectedTimeType = 1;
                    } else if (selectedId == R.id.radio_at) {
                        selectedTimeType = 2;
                    } else if (selectedId == R.id.radio_after) {
                        selectedTimeType = 3;
                    } else if (selectedId == R.id.radio_range) {
                        selectedTimeType = 4;
                        selectedEndHour = timePickerEnd.getHour();
                        selectedEndMinute = timePickerEnd.getMinute();
                    }

                    updateSelectedTimeText(textViewSelectedTime);
                })
                .setNegativeButton(getString(R.string.action_cancel), null);

        AlertDialog dialog = builder.create();
        dialog.show();

        if (radioGroupTimeType.getCheckedRadioButtonId() == R.id.radio_range) {
            textViewEndLabel.setVisibility(View.VISIBLE);
            timePickerEnd.setVisibility(View.VISIBLE);
        }

    }

    private void updateSelectedTimeText(TextView textView) {
        String timeText = "";
        String targetTime = String.format("%02d:%02d", selectedTargetHour, selectedTargetMinute);
        String endTime = String.format("%02d:%02d", selectedEndHour, selectedEndMinute);

        switch (selectedTimeType) {
            case 1:
                timeText = getString(R.string.time_prefix_before, targetTime);
                break;
            case 2:
                timeText = getString(R.string.time_prefix_at, targetTime);
                break;
            case 3:
                timeText = getString(R.string.time_prefix_after, targetTime);
                break;
            case 4:
                timeText = getString(R.string.time_range_format, targetTime, endTime);
                break;
        }
        textView.setText(timeText);
    }
    private void showDatePickerDialog(TextView textView) {
        Calendar calendar = Calendar.getInstance();
        long todayMillis = calendar.getTimeInMillis();

        DatePickerDialog datePicker = new DatePickerDialog(getActivity(),
                (view, year, month, dayOfMonth) -> {
                    selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month+1, dayOfMonth);
                    textView.setText(getString(R.string.date_prefix, selectedDate));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePicker.getDatePicker().setMinDate(todayMillis);

        datePicker.show();
    }
    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Task taskToDelete = taskList.get(position);

                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.dialog_title_delete_task))
                        .setMessage(getString(R.string.dialog_message_delete_confirm, taskToDelete.getTitle()))
                        .setPositiveButton(getString(R.string.action_yes), (dialog, which) -> {
                            deleteTask(taskToDelete, position);
                        })
                        .setNegativeButton(getString(R.string.action_cancel), (dialog, which) -> {
                            adapter.notifyItemChanged(position);
                        })
                        .show();
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }
    private void deleteTask(Task task, int position) {
        TaskNotificationScheduler.cancelAllTaskNotifications(getContext(), task);

        db.collection("tasks").document(userId)
                .collection("userTasks").document(task.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), getString(R.string.toast_task_deleted), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), getString(R.string.error_delete_failed), Toast.LENGTH_SHORT).show();
                    adapter.notifyItemChanged(position);
                });
    }
    private void scheduleTaskNotification(Task task) {
        Log.d("TasksFragment", "=== scheduleTaskNotification START ===");
        if (getContext() == null) {
            Log.d("TasksFragment", "getContext() == null, возврат");
            return;
        }

        long notificationTime = calculateNotificationTime(task);
        Log.d("TasksFragment", "Итоговое notificationTime: " + notificationTime);

        if (notificationTime > 0 && notificationTime > System.currentTimeMillis()) {
            task.setNotificationTime(notificationTime);
            task.setNotificationEnabled(true);
            task.setNotificationSent(false);

            Log.d("TasksFragment", "ВЫЗЫВАЕМ TaskNotificationScheduler.scheduleTaskNotification");
            TaskNotificationScheduler.scheduleTaskNotifications(getContext(), task);

            db.collection("tasks").document(userId)
                    .collection("userTasks").document(task.getId())
                    .update("notificationEnabled", true, "notificationTime", notificationTime, "notificationSent", false)
                    .addOnSuccessListener(aVoid -> Log.d("TasksFragment", "Firestore обновлён"))
                    .addOnFailureListener(e -> Log.e("TasksFragment", "Ошибка обновления Firestore", e));
        } else {
            Log.d("TasksFragment", "notificationTime = " + notificationTime + ", текущее время = " + System.currentTimeMillis());
            Log.d("TasksFragment", "Уведомление НЕ запланировано");
        }
    }
    private long calculateNotificationTime(Task task) {
        try {
            Log.d("TasksFragment", "=== calculateNotificationTime ===");
            Log.d("TasksFragment", "Task title: " + task.getTitle());
            Log.d("TasksFragment", "Task targetDate: " + task.getTargetDate());
            Log.d("TasksFragment", "Task timeType: " + task.getTimeType());
            Log.d("TasksFragment", "Task targetHour: " + task.getTargetHour());
            Log.d("TasksFragment", "Task targetMinute: " + task.getTargetMinute());
            Log.d("TasksFragment", "Task endHour: " + task.getEndHour());
            Log.d("TasksFragment", "Task endMinute: " + task.getEndMinute());

            long deadlineTime = 0;

            switch (task.getTimeType()) {
                case 1:
                case 2:
                case 3:
                    String timeStr = String.format("%02d:%02d", task.getTargetHour(), task.getTargetMinute());
                    deadlineTime = parseDateTime(task.getTargetDate(), timeStr);
                    Log.d("TasksFragment", "deadlineTime (target): " + deadlineTime + " = " + new Date(deadlineTime));
                    break;
                case 4:
                    long startTime = parseDateTime(task.getTargetDate(),
                            String.format("%02d:%02d", task.getTargetHour(), task.getTargetMinute()));
                    long endTime = parseDateTime(task.getTargetDate(),
                            String.format("%02d:%02d", task.getEndHour(), task.getEndMinute()));
                    deadlineTime = startTime + (endTime - startTime) / 2;
                    Log.d("TasksFragment", "startTime: " + startTime);
                    Log.d("TasksFragment", "endTime: " + endTime);
                    Log.d("TasksFragment", "deadlineTime (mid): " + deadlineTime + " = " + new Date(deadlineTime));
                    break;
            }

            if (deadlineTime == 0) {
                Log.d("TasksFragment", "deadlineTime = 0, возвращаем 0");
                return 0;
            }

            long notificationTime = 0;
            switch (task.getTimeType()) {
                case 1:
                    notificationTime = deadlineTime - 30 * 60 * 1000;
                    Log.d("TasksFragment", "Тип 'До', уведомление за 30 минут: " + notificationTime + " = " + new Date(notificationTime));
                    break;
                case 2:
                    notificationTime = deadlineTime;
                    Log.d("TasksFragment", "Тип 'В', уведомление в время: " + notificationTime + " = " + new Date(notificationTime));
                    break;
                case 3:
                    notificationTime = deadlineTime + 15 * 60 * 1000;
                    Log.d("TasksFragment", "Тип 'После', уведомление через 15 минут: " + notificationTime + " = " + new Date(notificationTime));
                    break;
                case 4:
                    notificationTime = deadlineTime;
                    Log.d("TasksFragment", "Тип 'Промежуток', уведомление в середине: " + notificationTime + " = " + new Date(notificationTime));
                    break;
            }

            Log.d("TasksFragment", "Текущее время: " + System.currentTimeMillis() + " = " + new Date());

            if (notificationTime <= System.currentTimeMillis()) {
                Log.d("TasksFragment", "⚠️ Время уведомления в прошлом! Не планируем.");
                return 0;
            }

            return notificationTime;
        } catch (Exception e) {
            Log.e("TasksFragment", "Ошибка расчета", e);
            return 0;
        }
    }
    private long parseDateTime(String date, String time) throws Exception {
        String dateTimeString = date + " " + time;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        Date dateObj = sdf.parse(dateTimeString);
        Log.d("TasksFragment", "parseDateTime: " + dateTimeString + " -> " + (dateObj != null ? dateObj.getTime() : 0));
        return dateObj != null ? dateObj.getTime() : 0;
    }


    private void showEditTaskDialog(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        EditText editTextTitle = dialogView.findViewById(R.id.edit_text_title);
        EditText editTextDescription = dialogView.findViewById(R.id.edit_text_description);
        RadioGroup radioGroupPriority = dialogView.findViewById(R.id.radio_group_priority);
        Button buttonSelectTime = dialogView.findViewById(R.id.button_select_time);
        TextView textViewSelectedTime = dialogView.findViewById(R.id.text_view_selected_time);

        editTextTitle.setText(task.getTitle());
        editTextDescription.setText(task.getDescription());

        switch (task.getPriority()) {
            case 1:
                radioGroupPriority.check(R.id.radio_priority_1);
                break;
            case 2:
                radioGroupPriority.check(R.id.radio_priority_2);
                break;
            case 3:
                radioGroupPriority.check(R.id.radio_priority_3);
                break;
        }

        selectedTimeType = task.getTimeType();
        selectedTargetHour = task.getTargetHour();
        selectedTargetMinute = task.getTargetMinute();
        selectedEndHour = task.getEndHour();
        selectedEndMinute = task.getEndMinute();
        updateSelectedTimeText(textViewSelectedTime);

        buttonSelectTime.setOnClickListener(v -> showTimePickerDialog(textViewSelectedTime));

        builder.setTitle(getString(R.string.dialog_title_edit_task))
                .setPositiveButton(getString(R.string.action_save), (dialog, which) -> {
                    String title = editTextTitle.getText().toString().trim();
                    String description = editTextDescription.getText().toString().trim();
                    int priority = 1;
                    int selectedId = radioGroupPriority.getCheckedRadioButtonId();
                    if (selectedId == R.id.radio_priority_2) {
                        priority = 2;
                    } else if (selectedId == R.id.radio_priority_3) {
                        priority = 3;
                    }
                    if (title.isEmpty()) {
                        Toast.makeText(getActivity(), getString(R.string.error_enter_title), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    task.setTitle(title);
                    task.setDescription(description);
                    task.setPriority(priority);
                    task.setTimeType(selectedTimeType);
                    task.setTargetHour(selectedTargetHour);
                    task.setTargetMinute(selectedTargetMinute);
                    task.setEndHour(selectedEndHour);
                    task.setEndMinute(selectedEndMinute);

                    db.collection("tasks").document(userId)
                            .collection("userTasks").document(task.getId())
                            .set(task)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getActivity(), getString(R.string.toast_task_updated), Toast.LENGTH_SHORT).show();
                                TaskNotificationScheduler.cancelAllTaskNotifications(getContext(), task);
                                scheduleTaskNotification(task);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getActivity(), getString(R.string.error_with_message, e.getMessage()), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton(getString(R.string.action_cancel), null);
        builder.create().show();
    }
    public void syncPendingTasks() {
        if (userId == null || getContext() == null) return;

        Gson gson = new Gson();
        List<String> pendingTasks = SyncQueueManager.getInstance(getContext()).getPendingTasks();
        if (pendingTasks.isEmpty()) return;

        for (int i = 0; i < pendingTasks.size(); i++) {
            final int index = i;
            String taskJson = pendingTasks.get(index);
            Map<String, Object> taskMap = gson.fromJson(taskJson, Map.class);
            String taskId = (String) taskMap.get("id");

            db.collection("tasks").document(userId)
                    .collection("userTasks").document(taskId)
                    .set(taskMap)
                    .addOnSuccessListener(aVoid -> {
                        SyncQueueManager.getInstance(getContext()).removePendingTask(index);
                        Log.d("TasksFragment", "Task synced: " + taskId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("TasksFragment", "Failed to sync task: " + taskId, e);
                    });
        }
    }
}