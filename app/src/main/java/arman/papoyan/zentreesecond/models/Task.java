package arman.papoyan.zentreesecond.models;

public class Task {
    private String id;
    private String title;
    private String description;
    private boolean completed;
    private long createdAt;
    private int priority;
    private int timeType;
    private int targetHour;
    private int targetMinute;
    private int endHour;
    private int endMinute;

    public Task() {

    }

    public Task(String title, String description, int priority,
                int timeType, int targetHour, int targetMinute, int endHour, int endMinute) {
        this.id = String.valueOf(System.currentTimeMillis());
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.timeType = timeType;
        this.targetHour = targetHour;
        this.targetMinute = targetMinute;
        this.endHour = endHour;
        this.endMinute = endMinute;
        this.createdAt = System.currentTimeMillis();
        this.completed = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public int getTimeType() { return timeType; }
    public void setTimeType(int timeType) { this.timeType = timeType; }

    public int getTargetHour() { return targetHour; }
    public void setTargetHour(int targetHour) { this.targetHour = targetHour; }

    public int getTargetMinute() { return targetMinute; }
    public void setTargetMinute(int targetMinute) { this.targetMinute = targetMinute; }

    public int getEndHour() { return endHour; }
    public void setEndHour(int endHour) { this.endHour = endHour; }

    public int getEndMinute() { return endMinute; }
    public void setEndMinute(int endMinute) { this.endMinute = endMinute; }
}