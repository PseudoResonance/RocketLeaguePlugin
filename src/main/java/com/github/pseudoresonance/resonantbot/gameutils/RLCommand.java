package com.github.pseudoresonance.resonantbot.gameutils;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import com.ssplugins.rlstats.entities.Platform;
import com.ssplugins.rlstats.entities.PlatformInfo;
import com.ssplugins.rlstats.entities.Player;
import com.ssplugins.rlstats.entities.Playlist;
import com.ssplugins.rlstats.entities.SearchResultPage;
import com.ssplugins.rlstats.entities.Season;
import com.ssplugins.rlstats.entities.Stat;
import com.ssplugins.rlstats.entities.Stats;
import com.ssplugins.rlstats.entities.Tier;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class RLCommand implements Command {

	private static RLStatsAPI stats = null;

	private static HashMap<Long, MessageChannel> returnQueue = new HashMap<Long, MessageChannel>();

	private static HashMap<Long, Platform> playlistRequest = new HashMap<Long, Platform>();
	private static HashMap<Long, Platform> playerRequest = new HashMap<Long, Platform>();

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
				case "playlists":
					if (args.length > 1) {
						Platform pl = Platform.fromName(args[1].toUpperCase());
						if (pl != null) {
							CompletableFuture<APIReturn<Long, List<Playlist>>> playlists = stats.getPlaylistInfo(e.getMessageIdLong());
							playlists.thenAccept(this::notifyPlaylistsPlatform);
							returnQueue.put(e.getMessageIdLong(), e.getChannel());
							playlistRequest.put(e.getMessageIdLong(), pl);
						} else {
							e.getChannel().sendMessage("Please add a valid platform name!").queue();
						}
					} else {
						CompletableFuture<APIReturn<Long, List<Playlist>>> playlists = stats.getPlaylistInfo(e.getMessageIdLong());
						playlists.thenAccept(this::notifyPlaylists);
						returnQueue.put(e.getMessageIdLong(), e.getChannel());
					}
					return;
				case "player":
					if (args.length > 2) {
						Platform pl = Platform.fromName(args[1].toUpperCase());
						if (pl != null) {
							CompletableFuture<APIReturn<Long, SearchResultPage>> search = stats.searchPlayers(args[2], e.getMessageIdLong());
							search.thenAccept(this::notifySearch);
							returnQueue.put(e.getMessageIdLong(), e.getChannel());
							playerRequest.put(e.getMessageIdLong(), pl);
						} else {
							e.getChannel().sendMessage("Please use a valid platform name!").queue();
						}
					} else if (args.length == 2) {
						CompletableFuture<APIReturn<Long, SearchResultPage>> search = stats.searchPlayers(args[1], e.getMessageIdLong());
						search.thenAccept(this::notifySearch);
						returnQueue.put(e.getMessageIdLong(), e.getChannel());
					} else {
						e.getChannel().sendMessage("Please add a player name or platform and player name to search for!").queue();
					}
					return;
				}
			}
		}
		if (e.getAuthor().getIdLong() == Config.getOwner()) {
			e.getChannel().sendMessage("Valid subcommands: `token`, `ratelimit`, `platforms`, `seasons`, `ranks`, `playlists`, `player`").queue();
		} else {
			e.getChannel().sendMessage("Valid subcommands: `platforms`, `seasons`, `ranks`, `playlists`, `player`").queue();
		}
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
				ret += "`" + s.getId() + "`: Started: `" + formatDate(s.getStartTime()) + "` Ended: `No`";
			} else {
				ret += "`" + s.getId() + "`: Started: `" + formatDate(s.getStartTime()) + "` Ended: `" + formatDate(s.getEndTime()) + "`";
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

	void notifyPlaylists(APIReturn<Long, List<Playlist>> data) {
		MessageChannel channel = returnQueue.remove(data.getID());
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(new Color(6, 128, 211));
		embed.setFooter("https://rocketleaguestats.com/", null);
		HashMap<Integer, Integer> players = new HashMap<Integer, Integer>();
		for (Playlist p : data.getValue()) {
			int i = 0;
			if (players.containsKey(p.getId())) {
				i = players.get(p.getId());
			}
			i += p.getPlayers();
			players.put(p.getId(), i);
		}
		String ret = "";
		ret += "Ranked Duel: `" + players.get(Playlist.RANKED_DUEL) + " Playing`\n";
		ret += "Ranked Doubles: `" + players.get(Playlist.RANKED_DOUBLES) + " Playing`\n";
		ret += "Ranked Solo Standard: `" + players.get(Playlist.RANKED_SOLO_STANDARD) + " Playing`\n";
		ret += "Ranked Standard: `" + players.get(Playlist.RANKED_STANDARD) + " Playing`\n";
		ret += "Duel: `" + players.get(Playlist.DUEL) + " Playing`\n";
		ret += "Doubles: `" + players.get(Playlist.DOUBLES) + " Playing`\n";
		ret += "Standard: `" + players.get(Playlist.STANDARD) + " Playing`\n";
		ret += "Snow Day: `" + players.get(Playlist.SNOW_DAY) + " Playing`\n";
		ret += "Rocket Labs: `" + players.get(Playlist.ROCKET_LABS) + " Playing`\n";
		ret += "Hoops: `" + players.get(Playlist.HOOPS) + " Playing`\n";
		ret += "Rumble: `" + players.get(Playlist.RUMBLE) + " Playing`\n";
		ret += "Drop Shot: `" + players.get(Playlist.DROPSHOT) + " Playing`\n";
		ret += "Chaos: `" + players.get(Playlist.CHAOS) + " Playing`\n";
		ret += "Mutator Mashup: `" + players.get(Playlist.MUTATOR_MASHUP) + " Playing`\n";
		embed.addField("Global Playlists:", ret, true);
		channel.sendMessage(embed.build()).queue();
	}

	void notifyPlaylistsPlatform(APIReturn<Long, List<Playlist>> data) {
		MessageChannel channel = returnQueue.remove(data.getID());
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(new Color(6, 128, 211));
		embed.setFooter("https://rocketleaguestats.com/", null);
		HashMap<Integer, Integer> players = new HashMap<Integer, Integer>();
		Platform pl = playlistRequest.remove(data.getID());
		for (Playlist p : data.getValue()) {
			if (p.getPlatform() == pl) {
				int i = 0;
				if (players.containsKey(p.getId())) {
					i = players.get(p.getId());
				}
				i += p.getPlayers();
				players.put(p.getId(), i);
			}
		}
		String ret = "";
		ret += "Ranked Duel: `" + players.get(Playlist.RANKED_DUEL) + " Playing`\n";
		ret += "Ranked Doubles: `" + players.get(Playlist.RANKED_DOUBLES) + " Playing`\n";
		ret += "Ranked Solo Standard: `" + players.get(Playlist.RANKED_SOLO_STANDARD) + " Playing`\n";
		ret += "Ranked Standard: `" + players.get(Playlist.RANKED_STANDARD) + " Playing`\n";
		ret += "Duel: `" + players.get(Playlist.DUEL) + " Playing`\n";
		ret += "Doubles: `" + players.get(Playlist.DOUBLES) + " Playing`\n";
		ret += "Standard: `" + players.get(Playlist.STANDARD) + " Playing`\n";
		ret += "Snow Day: `" + players.get(Playlist.SNOW_DAY) + " Playing`\n";
		ret += "Rocket Labs: `" + players.get(Playlist.ROCKET_LABS) + " Playing`\n";
		ret += "Hoops: `" + players.get(Playlist.HOOPS) + " Playing`\n";
		ret += "Rumble: `" + players.get(Playlist.RUMBLE) + " Playing`\n";
		ret += "Drop Shot: `" + players.get(Playlist.DROPSHOT) + " Playing`\n";
		ret += "Chaos: `" + players.get(Playlist.CHAOS) + " Playing`\n";
		ret += "Mutator Mashup: `" + players.get(Playlist.MUTATOR_MASHUP) + " Playing`\n";
		embed.addField(pl.getName() + " Playlists:", ret, true);
		channel.sendMessage(embed.build()).queue();
	}

	void notifySearch(APIReturn<Long, SearchResultPage> data) {
		MessageChannel channel = returnQueue.remove(data.getID());
		SearchResultPage result = data.getValue();
		Platform platform = playerRequest.remove(data.getID());
		List<Player> players = result.getResults();
		Player p = null;
		if (result.getTotalResults() > 0) {
			if (platform != null) {
				for (Player pl : players) {
					if (pl.getPlatform() == platform) {
						p = pl;
						break;
					}
				}
			} else {
				p = players.get(0);
			}
			if (p != null) {
				EmbedBuilder embed = new EmbedBuilder();
				embed.setColor(new Color(6, 128, 211));
				embed.setFooter("https://rocketleaguestats.com/", null);
				String url = p.getAvatarUrl();
				if (url == null)
					url = "https://cdn.discordapp.com/attachments/376557695420989441/435138854693896192/rls_partner_vertical_small.png";
				embed.setThumbnail(url);
				embed.setDescription("[" + p.getDisplayName() + "'s Stats on " + p.getPlatform().getName() + ":](" + p.getProfileUrl() + ")");
				Stats s = p.getStats();
				String stats = "";
				int wins = s.getStat(Stat.WINS);
				int shots = s.getStat(Stat.SHOTS);
				int goals = s.getStat(Stat.GOALS);
				int assists = s.getStat(Stat.ASSISTS);
				int saves = s.getStat(Stat.SAVES);
				int mvp = s.getStat(Stat.MVP);
				double shotAccuracy = ((double) goals / (double) shots) * 100.0;
				double mvpAccuracy = ((double) mvp / (double) wins) * 100.0;
				stats += "**Wins:** " + wins + "\n";
				stats += "**Shots:** " + shots + "\n";
				stats += "**Goals:** " + goals + "\n";
				stats += "**Shot Accuracy:** " + new BigDecimal(String.valueOf(shotAccuracy)).setScale(2, RoundingMode.HALF_UP) + "%\n";
				stats += "**Assists:** " + assists + "\n";
				stats += "**Saves:** " + saves + "\n";
				stats += "**MVPs:** " + mvp + "\n";
				stats += "**MVP:** " + new BigDecimal(String.valueOf(mvpAccuracy)).setScale(2, RoundingMode.HALF_UP) + "%";
				embed.addField("Stats:", stats, true);
				int total = goals + assists + saves;
				double goal = ((double) goals / (double) total) * 100.0;
				double assist = ((double) assists / (double) total) * 100.0;
				double save = ((double) saves / (double) total) * 100.0;
				String style = "";
				style += "**Goals:** " + new BigDecimal(String.valueOf(goal)).setScale(2, RoundingMode.HALF_EVEN) + "%\n";
				style += "**Assists:** " + new BigDecimal(String.valueOf(assist)).setScale(2, RoundingMode.HALF_EVEN) + "%\n";
				style += "**Saves:** " + new BigDecimal(String.valueOf(save)).setScale(2, RoundingMode.HALF_EVEN) + "%";
				embed.addField("Play Style:", style, true);
				String time = "";
				time += "**Joined:** " + formatDate(p.getCreated()) + "\n";
				time += "**Updated:** " + formatDateTime(p.getUpdated()) + "\n";
				time += "**Next Update:** " + formatDateTime(p.getNextUpdate()) + "\n";
				embed.addField("Data:", time, true);
				channel.sendMessage(embed.build()).queue();
				return;
			}
		}
		channel.sendMessage("Please add a valid player name or platform and player name to search for!").queue();
	}

	public String getDesc() {
		return "Displays Rocket League stats";
	}

	public boolean isHidden() {
		return false;
	}

	public static String formatDate(long unix) {
		Date date = new Date(unix * 1000L);
		SimpleDateFormat sdf = new SimpleDateFormat("M/dd/yyyy");
		sdf.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
		String out = sdf.format(date);
		return out;
	}

	public static String formatDateTime(long unix) {
		Date date = new Date(unix * 1000L);
		SimpleDateFormat sdf = new SimpleDateFormat("M/dd/yyyy HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
		String out = sdf.format(date);
		return out;
	}

}
