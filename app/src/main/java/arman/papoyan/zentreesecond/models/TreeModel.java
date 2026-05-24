package arman.papoyan.zentreesecond.models;

import java.io.Serializable;

public class TreeModel implements Serializable {
    private int totalMinutes;
    private int level;
    private int currentStage;
    private int progressInCurrentStage;
    private int progressPercentage;
    private boolean isGrowing;
    private String lastUpdateDate;

    public TreeModel() {
        this.totalMinutes = 0;
        this.level = 1;
        this.currentStage = 1;
        this.progressInCurrentStage = 0;
        this.progressPercentage = 0;
        this.isGrowing = false;
        this.lastUpdateDate = "";
    }

    public TreeModel(int totalMinutes, int level, int currentStage, int progressInCurrentStage, int progressPercentage) {
        this.totalMinutes = totalMinutes;
        this.level = level;
        this.currentStage = currentStage;
        this.progressInCurrentStage = progressInCurrentStage;
        this.progressPercentage = progressPercentage;
        this.isGrowing = false;
        this.lastUpdateDate = "";
    }

    public int getTotalMinutes() {
        return totalMinutes;
    }

    public void setTotalMinutes(int totalMinutes) {
        this.totalMinutes = totalMinutes;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(int currentStage) {
        this.currentStage = currentStage;
    }

    public int getProgressInCurrentStage() {
        return progressInCurrentStage;
    }

    public void setProgressInCurrentStage(int progressInCurrentStage) {
        this.progressInCurrentStage = progressInCurrentStage;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public boolean isGrowing() {
        return isGrowing;
    }

    public void setGrowing(boolean growing) {
        isGrowing = growing;
    }

    public String getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(String lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public void startGrowth() {
        this.isGrowing = true;
    }

    public void stopGrowth() {
        this.isGrowing = false;
    }

    public void addMinutes(int minutes, int x, float motivation) {
        this.totalMinutes += minutes;
        recalculateProgress(this.totalMinutes, x, motivation);
    }

    public void recalculateProgress(int totalMinutes, int x, float motivation) {
        this.totalMinutes = totalMinutes;
        int remainingMinutes = totalMinutes;
        int stage = 1;
        int progressInStage = 0;

        while (stage < 6) {
            int neededForStage = (int) (x * motivation * stage);
            if (remainingMinutes >= neededForStage) {
                remainingMinutes -= neededForStage;
                stage++;
            } else {
                progressInStage = remainingMinutes;
                remainingMinutes = 0;
                break;
            }
        }

        if (stage <= 6) {
            this.currentStage = stage;
            this.progressInCurrentStage = progressInStage;
            int neededForCurrent = (int) (x * motivation * stage);
            if (neededForCurrent > 0) {
                this.progressPercentage = (progressInStage * 100) / neededForCurrent;
            } else {
                this.progressPercentage = 0;
            }
        } else {
            this.currentStage = 6;
            this.progressPercentage = 100;
        }

        this.level = ((this.currentStage - 1) / 6) + 1;
    }

    public void resetToDefault(String todayDate) {
        this.totalMinutes = 0;
        this.level = 1;
        this.currentStage = 1;
        this.progressInCurrentStage = 0;
        this.progressPercentage = 0;
        this.isGrowing = false;
        this.lastUpdateDate = todayDate;
    }
    public void addBonusMinutes(int bonusMinutes) {
        int x = 60;
        float motivation = 1.0f;

        this.totalMinutes += bonusMinutes;
        recalculateProgress(this.totalMinutes, x, motivation);
    }
}