package com.github.pseudoresonance.resonantbot.rocketleague.api.entities;

import java.util.ArrayList;

public class SeasonStats {
	
	private final int season;
	private final ArrayList<PlaylistStats> playlists;
	
	/**
	 * Constructs a new {@link SeasonStats} object with the given values.
	 * 
	 * @param season Season number
	 * @param playlists ArrayList of {@link PlaylistStats} for the season
	 */
	public SeasonStats(int season, ArrayList<PlaylistStats> playlists) {
		this.season = season;
		this.playlists = playlists;
	}
	
	/**
	 * @return Season number
	 */
	public int getSeason() {
		return this.season;
	}
	
	/**
	 * @return ArrayList of {@link PlaylistStats} for the season
	 */
	public ArrayList<PlaylistStats> getPlaylists() {
		return this.playlists;
	}

}
