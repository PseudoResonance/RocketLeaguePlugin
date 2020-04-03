package com.github.pseudoresonance.resonantbot.rocketleague.api.entities;

import java.util.HashMap;

public class Player {
	
	private final static long expiration = 150000000000L;
	
	private final Platform platform;
	private final String name;
	private final String displayName;
	private final Stats stats;
	private final HashMap<Integer, SeasonStats> seasonStats;
	private final RewardLevel rewardLevel;
	private final String url;
	
	private final long creationTime;
	
	/**
	 * Constructs {@link Player} with the given values.
	 * 
	 * @param platform Platform of the player
	 * @param name Unique name of the player
	 * @param displayName Display name of the player
	 * @param stats Player statistics
	 * @param seasonStats HashMap containing season statistics of the player
	 * @param rewardLevel Reward level statistics for the player
	 * @param url Profile URL
	 */
	public Player(Platform platform, String name, String displayName, Stats stats, HashMap<Integer, SeasonStats> seasonStats, RewardLevel rewardLevel, String url) {
		this.platform = platform;
		this.name = name;
		this.displayName = displayName;
		this.stats = stats;
		this.seasonStats = seasonStats;
		this.rewardLevel = rewardLevel;
		this.url = url;
		this.creationTime = System.nanoTime();
	}
	
	/**
	 * @return Platform of the player
	 */
	public Platform getPlatform() {
		return this.platform;
	}
	
	/**
	 * @return Unique name of the player
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * @return Display name of the player
	 */
	public String getDisplayName() {
		return this.displayName;
	}
	
	/**
	 * @return Player statistics
	 */
	public Stats getStats() {
		return this.stats;
	}
	
	/**
	 * @return HashMap containing all season statistics
	 */
	public HashMap<Integer, SeasonStats> getSeasonStats() {
		return this.seasonStats;
	}
	
	/**
	 * @param season Season number to get statistics for
	 * @return Season statistics for given number
	 */
	public SeasonStats getSeason(int season) {
		return seasonStats.get(season);
	}
	
	/**
	 * @return Reward level statistics for the player
	 */
	public RewardLevel getRewardLevel() {
		return this.rewardLevel;
	}
	
	/**
	 * @return Profile URL
	 */
	public String getProfileURL() {
		return this.url;
	}
	
	/**
	 * @return Whether or not this {@link Player} has expired
	 */
	public boolean isExpired() {
		if (System.nanoTime() - creationTime > expiration)
			return true;
		return false;
	}

}
