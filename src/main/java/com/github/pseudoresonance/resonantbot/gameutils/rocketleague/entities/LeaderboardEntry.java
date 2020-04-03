package com.github.pseudoresonance.resonantbot.gameutils.rocketleague.entities;

public class LeaderboardEntry {

	private final Platform platform;
	private final String name;
	private final int position;
	private final double score;
	private final String suffix;
	
	/**
	 * Constructs {@link LeaderboardEntry} with the given values.
	 * 
	 * @param platform Platform of the player
	 * @param name Unique name of the player
	 * @param position Position on leaderboard
	 * @param score Leaderboard score
	 * @param suffix Score suffix
	 */
	public LeaderboardEntry(Platform platform, String name, int position, double score, String suffix) {
		this.platform = platform;
		this.name = name;
		this.position = position;
		this.score = score;
		this.suffix = suffix;
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
	 * @return Position on leaderboard
	 */
	public int getPosition() {
		return this.position;
	}
	
	/**
	 * @return Leaderboard score
	 */
	public double getScore() {
		return this.score;
	}
	
	/**
	 * @return Score suffix
	 */
	public String getSuffix() {
		return this.suffix;
	}

}
