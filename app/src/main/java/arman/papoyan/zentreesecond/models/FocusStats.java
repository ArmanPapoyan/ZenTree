package arman.papoyan.zentreesecond.models;

public class FocusStats {
    private String date;
    private long focusMinutes;
    private int tasksCompleted;
    private int treeLevel;

    public FocusStats() {}

    public FocusStats(String date, long focusMinutes, int tasksCompleted, int treeLevel) {
        this.date = date;
        this.focusMinutes = focusMinutes;
        this.tasksCompleted = tasksCompleted;
        this.treeLevel = treeLevel;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public long getFocusMinutes() { return focusMinutes; }
    public void setFocusMinutes(long focusMinutes) { this.focusMinutes = focusMinutes; }

    public int getTasksCompleted() { return tasksCompleted; }
    public void setTasksCompleted(int tasksCompleted) { this.tasksCompleted = tasksCompleted; }

    public int getTreeLevel() { return treeLevel; }
    public void setTreeLevel(int treeLevel) { this.treeLevel = treeLevel; }
}