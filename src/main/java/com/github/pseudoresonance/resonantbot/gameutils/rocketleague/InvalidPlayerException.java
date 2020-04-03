package com.github.pseudoresonance.resonantbot.gameutils.rocketleague;

public class InvalidPlayerException extends IllegalArgumentException {
	
	private static final long serialVersionUID = -4502810922089606876L;
	
	private String player;
	
	public InvalidPlayerException(String player) {
		this.player = player;
	}
	
	public InvalidPlayerException(String message, String player) {
		super(message);
		this.player = player;
	}
	
	public String getPlayer() {
		return this.player;
	}
	
}
