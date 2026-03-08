package arman.papoyan.zentreesecond.model;

public class TreeModel {
    private int level = 1;
    private int experience = 0;
    private int maxExperience = 100;
    private int currentStage = 1;
    private long growthStartTime = 0;
    private boolean isGrowing = false;

    public int getLevel() {
        return level;
    }

    public int getExperience() {
        return experience;
    }

    public int getMaxExperience() {
        return maxExperience;
    }

    public int getCurrentStage() {
        return currentStage;
    }

    public boolean isGrowing() {
        return isGrowing;
    }

    public void addExperience(int exp) {
        this.experience += exp;
        int newStage = calculateStage();
        if (newStage > currentStage) {
            currentStage = newStage;
        }
        if (this.experience >= maxExperience) {
            levelUp();
        }
    }

    private int calculateStage() {
        int totalExpForNextStage = 0;
        for (int i = 1; i <= 6; i++) {
            totalExpForNextStage += i * 100;
            if (experience < totalExpForNextStage) {
                return i;
            }
        }
        return 6;
    }

    private void levelUp() {
        level++;
        experience = experience - maxExperience;
        maxExperience = level * 100;
    }

    public int getProgressInCurrentStage() {
        int expForPreviousStages = 0;
        for (int i = 1; i < currentStage; i++) {
            expForPreviousStages += i * 100;
        }
        int expInCurrentStage = experience - expForPreviousStages;
        int expNeededForThisStage = currentStage * 100;
        if (expNeededForThisStage == 0) {
            return 0;
        }
        return (expInCurrentStage * 100) / expNeededForThisStage;
    }

    public int getProgressPercentage() {
        return getProgressInCurrentStage();
    }

    public void startGrowth() {
        isGrowing = true;
        growthStartTime = System.currentTimeMillis();
    }

    public void stopGrowth() {
        if (isGrowing) {
            isGrowing = false;
            long growthDuration = System.currentTimeMillis() - growthStartTime;
            int experienceEarned = (int) (growthDuration / 10000);
            if (experienceEarned > 0) {
                addExperience(experienceEarned);
            }
        }
    }
}