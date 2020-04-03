package com.github.pseudoresonance.resonantbot.gameutils.rocketleague.entities;

public enum Platform {
	
	STEAM("Steam", "steam"),
	PS4("PS4", "ps"),
	XBOX("XboxOne", "xbox");
	
	private String name;
	private String url;
	
	Platform(String name, String url) {
		this.name = name;
		this.url = url;
	}
	
	/**
	 * Get the name of this platform.
	 * 
	 * @return Platform name.
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Get the internal name of this platform.
	 * 
	 * @return Platform name.
	 */
	public String getInternalName() {
		return this.url;
	}

	/**
	 * Get a {@link Platform} by name.
	 * 
	 * @param name Name of platform to get.
	 * @return Platform or null if not found.
	 */
	public static Platform fromName(String name) {
		String test = name.toLowerCase();
		if (test.equals("steam") || test.equals("pc"))
			return STEAM;
		else if (test.equals("ps4") || test.equals("ps") || test.equals("playstation") || test.equals("playstation4"))
			return PS4;
		else if (test.equals("xbox") || test.equals("xboxone"))
			return XBOX;
		return null;
	}

}
