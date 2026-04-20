package arman.papoyan.zentreesecond.model;

public class TreeModel {
    private int level = 1;
    private int totalMinutes = 0;
    private int currentStage = 1;
    private int progressInCurrentStage = 0;
    private long growthStartTime = 0;
    private boolean isGrowing = false;
    private String userId;
    private String lastUpdateDate = "";

    public int getLevel() { return level; }
    public int getTotalMinutes() { return totalMinutes; }
    public int getCurrentStage() { return currentStage; }
    public int getProgressPercentage() { return progressInCurrentStage; }
    public boolean isGrowing() { return isGrowing; }
    public String getLastUpdateDate() { return lastUpdateDate; }
    public void setLastUpdateDate(String date) { this.lastUpdateDate = date; }

    public void addMinutes(int minutes, int x, float motivation) {
        int currentStageForCalc = Math.min(currentStage, 5);
        int neededForCurrentStage = (int) (x * motivation * currentStageForCalc);

        float percentPerMinute = 100f / neededForCurrentStage;
        int percentToAdd = (int) (minutes * percentPerMinute);

        progressInCurrentStage += percentToAdd;
        totalMinutes += minutes;

        if (progressInCurrentStage >= 100) {
            progressInCurrentStage = 0;
            currentStage++;
            if (currentStage > 6) currentStage = 6;
        }

        level = currentStage;
    }
    public void addBonusMinutes(int minutes) {
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
    public void startGrowth() {
        isGrowing = true;
        growthStartTime = System.currentTimeMillis();
    }

    public void stopGrowth() {
        if (isGrowing) {
            isGrowing = false;
            long growthDuration = System.currentTimeMillis() - growthStartTime;
        }
    }

    public void resetToDefault(String today) {
        this.level = 1;
        this.totalMinutes = 0;
        this.currentStage = 1;
        this.progressInCurrentStage = 0;
        this.lastUpdateDate = today;
    }
}