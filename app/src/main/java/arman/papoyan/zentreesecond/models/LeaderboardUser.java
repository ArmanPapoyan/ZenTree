package arman.papoyan.zentreesecond.models;

public class LeaderboardUser {
    private String userId;
    private String name;
    private long totalMinutes;
    private int rank;
    private String email;

    public LeaderboardUser() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getTotalMinutes() { return totalMinutes; }
    public void setTotalMinutes(long totalMinutes) { this.totalMinutes = totalMinutes; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public long getTotalHours() { return totalMinutes / 60; }
}