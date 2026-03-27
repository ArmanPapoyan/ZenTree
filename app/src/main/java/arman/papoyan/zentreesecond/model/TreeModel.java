package arman.papoyan.zentreesecond.model;

public class TreeModel {
    private int level = 1;
    private int totalMinutes = 0;
    private int currentStage = 1;
    private long growthStartTime = 0;
    private boolean isGrowing = false;
    private String userId;
    public int getLevel() {
        return level;
    }

    public int getTotalMinutes() {
        return totalMinutes;
    }

    public int getCurrentStage() {
        return currentStage;
    }

    public boolean isGrowing() {
        return isGrowing;
    }

    public void addMinutes(int minutes) {
        this.totalMinutes += minutes;

        int newLevel = totalMinutes / 60;
        if (newLevel > level) {
            level = newLevel;
        }

        int newStage = Math.min(level, 6);
        if (newStage > currentStage) {
            currentStage = newStage;
        }
    }

    public int getProgressPercentage() {
        int minutesInCurrentHour = totalMinutes % 60;
        return (minutesInCurrentHour * 100) / 60;
    }

    public void startGrowth() {
        isGrowing = true;
        growthStartTime = System.currentTimeMillis();
    }

    public void stopGrowth() {
        if (isGrowing) {
            isGrowing = false;
            long growthDuration = System.currentTimeMillis() - growthStartTime;
            int minutesEarned = (int) (growthDuration / 60000);
            if (minutesEarned > 0) {
                addMinutes(minutesEarned);
            }
        }
    }
}