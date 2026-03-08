package arman.papoyan.zentreesecond.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import arman.papoyan.zentreesecond.R;
import arman.papoyan.zentreesecond.adapter.TaskAdapter;
import arman.papoyan.zentreesecond.model.Task;

public class TasksFragment extends Fragment {

    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private List<Task> taskList;
    private FloatingActionButton fabAddTask;

    private int selectedTimeType = 1;
    private int selectedTargetHour = 12;
    private int selectedTargetMinute = 0;
    private int selectedEndHour = 13;
    private int selectedEndMinute = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_tasks);
        fabAddTask = view.findViewById(R.id.fab_add_task);

        taskList = new ArrayList<>();

        adapter = new TaskAdapter();
        adapter.setTasks(taskList);

        adapter.setOnTaskClickListener(task -> {
            Toast.makeText(getActivity(), "Задача: " + task.getTitle(), Toast.LENGTH_SHORT).show();
        });

        adapter.setOnTaskCheckedChangeListener((task, isChecked) -> {
            Toast.makeText(getActivity(), "Задача " + (isChecked ? "выполнена" : "не выполнена"), Toast.LENGTH_SHORT).show();
            adapter.setTasks(taskList);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);

        fabAddTask.setOnClickListener(v -> showAddTaskDialog());

        return view;
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

        buttonSelectTime.setOnClickListener(v -> showTimePickerDialog(textViewSelectedTime));

        builder.setTitle("Новая задача")
                .setPositiveButton("Добавить", (dialog, which) -> {
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
                        Toast.makeText(getActivity(), "Введите название", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Task newTask = new Task(title, description, priority,
                            selectedTimeType, selectedTargetHour, selectedTargetMinute,
                            selectedEndHour, selectedEndMinute);
                    taskList.add(newTask);
                    adapter.setTasks(taskList);
                    Toast.makeText(getActivity(), "Задача добавлена", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null);

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

        builder.setTitle("Выберите время")
                .setPositiveButton("Готово", (dialog, which) -> {
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
                .setNegativeButton("Отмена", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        if (radioGroupTimeType.getCheckedRadioButtonId() == R.id.radio_range) {
            textViewEndLabel.setVisibility(View.VISIBLE);
            timePickerEnd.setVisibility(View.VISIBLE);
        } else {
            textViewEndLabel.setVisibility(View.GONE);
            timePickerEnd.setVisibility(View.GONE);
        }
    }

    private void updateSelectedTimeText(TextView textView) {
        String timeText = "";
        String targetTime = String.format("%02d:%02d", selectedTargetHour, selectedTargetMinute);
        String endTime = String.format("%02d:%02d", selectedEndHour, selectedEndMinute);

        switch (selectedTimeType) {
            case 1:
                timeText = "До " + targetTime;
                break;
            case 2:
                timeText = "В " + targetTime;
                break;
            case 3:
                timeText = "После " + targetTime;
                break;
            case 4:
                timeText = targetTime + " - " + endTime;
                break;
        }
        textView.setText(timeText);
    }
}