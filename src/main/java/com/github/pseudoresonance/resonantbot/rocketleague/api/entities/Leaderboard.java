package com.github.pseudoresonance.resonantbot.rocketleague.api.entities;

import java.util.ArrayList;

public class Leaderboard {
	
	private final static long expiration = 1800000000000L;
	
	private final String name;
	private final LeaderboardStat stat;
	private final ArrayList<LeaderboardEntry> leaderboard;
	private final String url;
	
	private final long creationTime;
	
	/**
	 * Constructs a {@link Leaderboard} with the given values.
	 * 
	 * @param name Name of the leaderboard
	 * @param stat Statistic of the leaderboard
	 * @param leaderboard ArrayList of {@link LeaderboardEntry} ordered by rank
	 * @param url Leaderboard URL
	 */
	public Leaderboard(String name, LeaderboardStat stat, ArrayList<LeaderboardEntry> leaderboard, String url) {
		this.name = name;
		this.stat = stat;
		this.leaderboard = leaderboard;
		this.url = url;
		this.creationTime = System.nanoTime();
	}
	
	/**
	 * @return Name of the leaderboard
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * @return Statistic of the leaderboard
	 */
	public LeaderboardStat getStat() {
		return this.stat;
	}
	
	/**
	 * @return ArrayList of {@link LeaderboardEntry} ordered by rank
	 */
	public ArrayList<LeaderboardEntry> getEntries() {
		return this.leaderboard;
	}
	
	/**
	 * @param id Leaderboard rank
	 * @return {@link LeaderboardEntry} for the specified rank
	 */
	public LeaderboardEntry getEntry(int id) {
		return this.leaderboard.get(id);
	}
	
	/**
	 * @return Leaderboard URL
	 */
	public String getURL() {
		return this.url;
	}
	
	/**
	 * @return Whether or not this {@link Leaderboard} has expired
	 */
	public boolean isExpired() {
		if (System.nanoTime() - creationTime > expiration)
			return true;
		return false;
	}

}
