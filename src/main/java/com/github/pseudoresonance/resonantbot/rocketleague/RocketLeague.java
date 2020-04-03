package com.github.pseudoresonance.resonantbot.rocketleague;

import com.github.pseudoresonance.resonantbot.CommandManager;
import com.github.pseudoresonance.resonantbot.api.Plugin;
import com.github.pseudoresonance.resonantbot.rocketleague.api.RocketLeagueStats;

public class RocketLeague extends Plugin {

	public void onEnable() {
		CommandManager.registerCommand("rl", new RLCommand(), this);
	}
	
	public void onDisable() {
		RocketLeagueStats.shutdownAll();
	}
	
}
