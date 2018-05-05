package com.github.pseudoresonance.resonantbot.gameutils;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;

import com.github.pseudoresonance.resonantbot.Config;
import com.github.pseudoresonance.resonantbot.api.Command;
import com.ssplugins.rlstats.APIReturn;
import com.ssplugins.rlstats.RLStats;
import com.ssplugins.rlstats.RLStatsAPI;
import com.ssplugins.rlstats.entities.PlatformInfo;
import com.ssplugins.rlstats.entities.Season;
import com.ssplugins.rlstats.entities.Tier;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class RLCommand implements Command {

	private static RLStatsAPI stats = null;

	private static HashMap<Long, MessageChannel> returnQueue = new HashMap<Long, MessageChannel>();

	public void onCommand(MessageReceivedEvent e, String command, String[] args) {
		if (args.length == 0) {
			if (stats == null) {
				Object t = Config.get("rocketleaguetoken");
				if (t != null) {
					stats = RLStats.getAPI((String) t);
					Object l = Config.get("rocketleaguelimit");
					if (l != null) {
						stats.setRequestsPerSecond((Integer) l);
					}
				} else {
					e.getChannel().sendMessage("The bot owner has not added a rocket league API token!").queue();
					return;
				}
			}
		} else if (args.length > 0) {
			if (args[0].equalsIgnoreCase("token")) {
				if (e.getAuthor().getIdLong() == Config.getOwner()) {
					if (args.length > 1) {
						Config.put("rocketleaguetoken", args[1]);
						e.getChannel().sendMessage("Saved rocket league API token!").queue();
						e.getMessage().delete().queue();
						return;
					} else {
						e.getChannel().sendMessage("Please add a rocket league API token!").queue();
						return;
					}
				} else {
					e.getChannel().sendMessage("Only the bot owner can add a rocket league API token!").queue();
					return;
				}
			} else if (args[0].equalsIgnoreCase("ratelimit")) {
				if (e.getAuthor().getIdLong() == Config.getOwner()) {
					if (args.length > 1) {
						try {
							int rateLimit = Integer.valueOf(args[1]);
							Config.put("rocketleaguelimit", rateLimit);
							stats.setRequestsPerSecond(rateLimit);
							e.getChannel().sendMessage("Saved rate limit!").queue();
							return;
						} catch (NullPointerException | NumberFormatException ex) {
							e.getChannel().sendMessage("Please add a valid number of requests per second!").queue();
							return;
						}
					} else {
						e.getChannel().sendMessage("Please add the number of requests per second!").queue();
						return;
					}
				} else {
					e.getChannel().sendMessage("Only the bot owner can change the rate limit!").queue();
					return;
				}
			} else {
				if (stats == null) {
					Object t = Config.get("rocketleaguetoken");
					if (t != null) {
						stats = RLStats.getAPI((String) t);
						Object l = Config.get("rocketleaguelimit");
						if (l != null) {
							stats.setRequestsPerSecond((Integer) l);
						}
					} else {
						e.getChannel().sendMessage("The bot owner has not added a rocket league API token!").queue();
						return;
					}
				}
				switch (args[0].toLowerCase()) {
				case "platforms":
					CompletableFuture<APIReturn<Long, List<PlatformInfo>>> platforms = stats.getPlatforms(e.getMessageIdLong());
					platforms.thenAccept(this::notifyPlatforms);
					returnQueue.put(e.getMessageIdLong(), e.getChannel());
					return;
				case "seasons":
					CompletableFuture<APIReturn<Long, List<Season>>> seasons = stats.getSeasons(e.getMessageIdLong());
					seasons.thenAccept(this::notifySeasons);
					returnQueue.put(e.getMessageIdLong(), e.getChannel());
					return;
				case "ranks":
					if (args.length > 1) {
						try {
							int season = Integer.valueOf(args[1]);
							CompletableFuture<APIReturn<Long, APIReturn<Integer, List<Tier>>>> tiers = stats.getTiers(season, e.getMessageIdLong());
							tiers.thenAccept(this::notifyTiersSeason);
							returnQueue.put(e.getMessageIdLong(), e.getChannel());
						} catch (NullPointerException | NumberFormatException ex) {
							e.getChannel().sendMessage("Please add a valid season number!").queue();
						}
					} else {
						CompletableFuture<APIReturn<Long, List<Tier>>> tiers = stats.getTiers(e.getMessageIdLong());
						tiers.thenAccept(this::notifyTiers);
						returnQueue.put(e.getMessageIdLong(), e.getChannel());
					}
					return;
				}
			}
		}
		e.getChannel().sendMessage("Valid subcommands: `platforms`, `seasons`, `ranks`").queue();
		return;
	}

	void notifyPlatforms(APIReturn<Long, List<PlatformInfo>> data) {
		MessageChannel channel = returnQueue.remove(data.getID());
		String ret = "Available Platforms: ";
		boolean first = true;
		for (PlatformInfo pi : data.getValue()) {
			if (first) {
				first = false;
			} else {
				ret += ", ";
			}
			ret += "`" + pi.getName() + "`";
		}
		channel.sendMessage(ret).queue();
	}

	void notifySeasons(APIReturn<Long, List<Season>> data) {
		MessageChannel channel = returnQueue.remove(data.getID());
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(new Color(6, 128, 211));
		embed.setFooter("https://rocketleaguestats.com/", null);
		String ret = "";
		boolean first = true;
		for (Season s : data.getValue()) {
			if (first) {
				first = false;
			} else {
				ret += "\n";
			}
			if (s.getEndTime() == -1) {
				ret += "`" + s.getId() + "`: Started: `" + formatTime(s.getStartTime()) + "` Ended: `No`";
			} else {
				ret += "`" + s.getId() + "`: Started: `" + formatTime(s.getStartTime()) + "` Ended: `" + formatTime(s.getEndTime()) + "`";
			}
		}
		embed.addField("Seasons:", ret, false);
		channel.sendMessage(embed.build()).queue();
	}

	void notifyTiers(APIReturn<Long, List<Tier>> data) {
		MessageChannel channel = returnQueue.remove(data.getID());
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(new Color(6, 128, 211));
		embed.setFooter("https://rocketleaguestats.com/", null);
		String ret = "";
		boolean first = true;
		for (Tier t : data.getValue()) {
			if (first) {
				first = false;
			} else {
				ret += "\n";
			}
			ret += "`" + t.getId() + "`: `" + t.getName() + "`";
		}
		embed.addField("Ranks:", ret, false);
		channel.sendMessage(embed.build()).queue();
	}

	void notifyTiersSeason(APIReturn<Long, APIReturn<Integer, List<Tier>>> data) {
		MessageChannel channel = returnQueue.remove(data.getID());
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(new Color(6, 128, 211));
		embed.setFooter("https://rocketleaguestats.com/", null);
		String ret = "";
		boolean first = true;
		APIReturn<Integer, List<Tier>> value = data.getValue();
		int season = value.getID();
		for (Tier t : value.getValue()) {
			if (first) {
				first = false;
			} else {
				ret += "\n";
			}
			ret += "`" + t.getId() + "`: `" + t.getName() + "`";
		}
		embed.addField("Season " + season + " Ranks:", ret, false);
		channel.sendMessage(embed.build()).queue();
	}

	public String getDesc() {
		return "Displays Rocket League stats";
	}

	public boolean isHidden() {
		return false;
	}

	public static String formatTime(long unix) {
		Date date = new Date(unix * 1000L);
		SimpleDateFormat sdf = new SimpleDateFormat("M/dd/yyyy");
		sdf.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
		String out = sdf.format(date);
		return out;
	}

}
