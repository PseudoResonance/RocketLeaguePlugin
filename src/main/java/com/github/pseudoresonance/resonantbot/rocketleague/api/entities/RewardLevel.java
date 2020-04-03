package com.github.pseudoresonance.resonantbot.rocketleague.api.entities;

public class RewardLevel {
	
	private final String rank;
	private final int wins;
	
	/**
	 * Constructs player {@link RewardLevel} statistics with the given values.
	 * 
	 * @param rank Current reward rank
	 * @param wins Total wins in rank
	 */
	public RewardLevel(String rank, int wins) {
		this.rank = rank;
		this.wins = wins;
	}
	
	/**
	 * @return Current reward rank
	 */
	public String getRank() {
		return this.rank;
	}
	
	/**
	 * @return Total wins in rank
	 */
	public int getWins() {
		return this.wins;
	}

}
