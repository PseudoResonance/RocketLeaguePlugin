package com.github.pseudoresonance.resonantbot.rocketleague.api.entities;

public class PlaylistStats {
	
	private final String name;
	private final String rank;
	private final int rating;
	private final int games;
	private final int streak;
	private final StreakType streakType;
	
	/**
	 * Constructs a player {@link PlaylistStats} statistics object with the given values.
	 * 
	 * @param name Playlist name
	 * @param rank Rank in playlist
	 * @param rating Playing rating
	 * @param games Games played
	 * @param streak Current streak
	 * @param streakType Streak type
	 */
	public PlaylistStats(String name, String rank, int rating, int games, int streak, StreakType streakType) {
		this.name = name;
		this.rank = rank;
		this.rating = rating;
		this.games = games;
		this.streak = streak;
		this.streakType = streakType;
	}
	
	/**
	 * Constructs a player {@link PlaylistStats} statistics object with the given values.
	 * 
	 * @param name Playlist name
	 * @param rank Rank in playlist
	 * @param rating Playing rating
	 * @param games Games played
	 */
	public PlaylistStats(String name, String rank, int rating, int games) {
		this.name = name;
		this.rank = rank;
		this.rating = rating;
		this.games = games;
		this.streak = -1;
		this.streakType = StreakType.WINNING;
	}
	
	/**
	 * @return Playlist name
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * @return Rank in playlist
	 */
	public String getRank() {
		return this.rank;
	}
	
	/**
	 * @return Playlist rating
	 */
	public int getRating() {
		return this.rating;
	}
	
	/**
	 * @return Games played
	 */
	public int getGames() {
		return this.games;
	}
	
	/**
	 * @return Current streak
	 */
	public int getStreak() {
		return this.streak;
	}
	
	/**
	 * @return Streak type
	 */
	public StreakType getStreakType() {
		return this.streakType;
	}
	
	public enum StreakType {
		WINNING,
		LOSING;
	}

}
