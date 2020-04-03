package com.github.pseudoresonance.resonantbot.gameutils.rocketleague.entities;

public enum LeaderboardStat {
	
	TRACKER_SCORE("Score"),
	SEASON_REWARD_LEVEL("SeasonRewardLevel"),
	GOAL_SHOT_RATIO("GoalShotRatio"),
	GOALS("Goals"),
	WINS("Wins"),
	SHOTS("Shots"),
	MVPS("MVPs"),
	SAVES("Saves"),
	ASSISTS("Assists");
	
	private String url;
	
	LeaderboardStat(String url) {
		this.url = url;
	}
	
	/**
	 * Get the internal name of this leaderboard statistic
	 * 
	 * @return Platform name.
	 */
	public String getInternalName() {
		return this.url;
	}

	/**
	 * Get a {@link LeaderboardStat} by name.
	 * 
	 * @param name Name of leaderboard statistic to get.
	 * @return LeaderboardStat or null if not found.
	 */
	public static LeaderboardStat fromName(String name) {
		String test = name.toLowerCase();
		if (test.equals("trackerscore") || test.equals("score"))
			return TRACKER_SCORE;
		else if (test.equals("seasonrewardlevel") || test.equals("rewardlevel") || test.equals("reward") || test.equals("level"))
			return SEASON_REWARD_LEVEL;
		else if (test.equals("goalshotratio") || test.equals("goalshot"))
			return GOAL_SHOT_RATIO;
		else if (test.equals("goals"))
			return GOALS;
		else if (test.equals("wins"))
			return WINS;
		else if (test.equals("shots"))
			return SHOTS;
		else if (test.equals("mvps"))
			return MVPS;
		else if (test.equals("saves"))
			return SAVES;
		else if (test.equals("assists"))
			return ASSISTS;
		return null;
	}

}
