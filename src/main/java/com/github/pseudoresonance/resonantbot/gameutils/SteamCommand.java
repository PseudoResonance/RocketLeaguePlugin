package com.github.pseudoresonance.resonantbot.gameutils;

import com.github.pseudoresonance.resonantbot.Config;
import com.github.pseudoresonance.resonantbot.api.Command;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class SteamCommand implements Command {

	@Override
	public void onCommand(MessageReceivedEvent e, String command, String[] args) {
		if (args.length == 0) {
			if (SteamAPI.getToken() == null) {
				e.getChannel().sendMessage("The bot owner has not added a Steam API token!").queue();
				return;
			}
		} else if (args.length > 0) {
			if (args[0].equalsIgnoreCase("token")) {
				if (e.getAuthor().getIdLong() == Config.getOwner()) {
					if (args.length > 1) {
						Config.put("steamtoken", args[1]);
						e.getChannel().sendMessage("Saved Steam API token!").queue();
						e.getMessage().delete().queue();
						SteamAPI.updateToken(args[1]);
						return;
					} else {
						e.getChannel().sendMessage("Please add a Steam API token!").queue();
						return;
					}
				} else {
					e.getChannel().sendMessage("Only the bot owner can add a Steam API token!").queue();
					return;
				}
			}
		}
		if (e.getAuthor().getIdLong() == Config.getOwner()) {
			e.getChannel().sendMessage("Valid subcommands: `token`").queue();
		} else {
			e.getChannel().sendMessage("Valid subcommands: ").queue();
		}
	}

	@Override
	public String getDesc() {
		return "Allows access to Steam API data";
	}

	@Override
	public boolean isHidden() {
		return false;
	}

}
