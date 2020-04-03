package com.github.pseudoresonance.resonantbot.gameutils.rocketleague.entities;

import java.text.DecimalFormat;

public class Stats {

	private static final DecimalFormat df = new DecimalFormat("#.##");

	private final double trackerScore;
	private final int wins;
	private final int shots;
	private final int goals;
	private final int assists;
	private final int saves;
	private final int mvps;
	private final double shotAccuracy;
	private final double mvpRate;
	private final double playStyleGoals;
	private final double playStyleAssists;
	private final double playStyleSaves;
	
	/**
	 * Constructs a player {@link Stats} object with the given statistics
	 * 
	 * @param trackerScore RocketLeague Tracker Network score
	 * @param wins Wins
	 * @param shots Shots
	 * @param goals Goals
	 * @param assists Assists
	 * @param saves Saves
	 * @param mvps MVPs
	 */
	public Stats(double trackerScore, int wins, int shots, int goals, int assists, int saves, int mvps) {
		this.trackerScore = trackerScore;
		this.wins = wins;
		this.shots = shots;
		this.goals = goals;
		this.assists = assists;
		this.saves = saves;
		this.mvps = mvps;
		this.shotAccuracy = Double.valueOf(df.format((goals / Double.valueOf(shots)) * 100.0));
		this.mvpRate = Double.valueOf(df.format((mvps / Double.valueOf(wins)) * 100.0));
		double total = goals + assists + saves;
		this.playStyleGoals = Double.valueOf(df.format((goals / total) * 100.0));
		this.playStyleAssists = Double.valueOf(df.format((assists / total) * 100.0));
		this.playStyleSaves = Double.valueOf(df.format((saves / total) * 100.0));
	}
	
	/**
	 * @return RocketLeague Tracker Network score
	 */
	public double getTrackerScore() {
		return this.trackerScore;
	}
	
	/**
	 * @return Wins
	 */
	public int getWins() {
		return this.wins;
	}
	
	/**
	 * @return Shots
	 */
	public int getShots() {
		return this.shots;
	}
	
	/**
	 * @return Goals
	 */
	public int getGoals() {
		return this.goals;
	}
	
	/**
	 * @return Assists
	 */
	public int getAssists() {
		return this.assists;
	}
	
	/**
	 * @return Saves
	 */
	public int getSaves() {
		return this.saves;
	}
	
	/**
	 * @return MVPs
	 */
	public int getMvps() {
		return this.mvps;
	}
	
	/**
	 * @return Shot accuracy to 2 decimal points
	 */
	public double getShotAccuracy() {
		return this.shotAccuracy;
	}
	
	/**
	 * @return MVP Rate to 2 decimal points
	 */
	public double getMvpRate() {
		return this.mvpRate;
	}
	
	/**
	 * @return Goal percentage in play style to 2 decimal points
	 */
	public double getPlayStyleGoals() {
		return this.playStyleGoals;
	}
	
	/**
	 * @return Assists percentage in play style to 2 decimal points
	 */
	public double getPlayStyleAssists() {
		return this.playStyleAssists;
	}
	
	/**
	 * @return Saves percentage in play style to 2 decimal points
	 */
	public double getPlayStyleSaves() {
		return this.playStyleSaves;
	}

}
