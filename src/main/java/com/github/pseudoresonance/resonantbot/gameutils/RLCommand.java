package com.github.pseudoresonance.resonantbot.gameutils;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.json.JsonNumber;

import com.github.pseudoresonance.resonantbot.Config;
import com.github.pseudoresonance.resonantbot.Language;
import com.github.pseudoresonance.resonantbot.api.Command;
import com.ssplugins.rlstats.APIReturn;
import com.ssplugins.rlstats.RLStats;
import com.ssplugins.rlstats.RLStatsAPI;
import com.ssplugins.rlstats.entities.Platform;
import com.ssplugins.rlstats.entities.PlatformInfo;
import com.ssplugins.rlstats.entities.Player;
import com.ssplugins.rlstats.entities.Playlist;
import com.ssplugins.rlstats.entities.PlaylistInfo;
import com.ssplugins.rlstats.entities.SearchResultPage;
import com.ssplugins.rlstats.entities.Season;
import com.ssplugins.rlstats.entities.SeasonInfo;
import com.ssplugins.rlstats.entities.Stat;
import com.ssplugins.rlstats.entities.Stats;
import com.ssplugins.rlstats.entities.Tier;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class RLCommand implements Command {

	private static RLStatsAPI stats = null;

	private static LinkedHashMap<Long, MessageChannel> returnQueue = new LinkedHashMap<Long, MessageChannel>();

	private static LinkedHashMap<Long, Platform> playlistRequest = new LinkedHashMap<Long, Platform>();

	private static LinkedHashMap<Long, Integer> playerSeason = new LinkedHashMap<Long, Integer>();

	private static LinkedHashMap<Long, Stat> leaderboardStat = new LinkedHashMap<Long, Stat>();
	
	private static int season = 0;
	private static long seasonUpdate = 0;
	
	private static HashMap<Integer, String> tiers = new HashMap<Integer, String>();
	private static long tiersUpdate = 0;

	public void onCommand(MessageReceivedEvent e, String command, String[] args) {
		if (args.length == 0) {
			if (stats == null) {
				Object t = Config.get("rocketleaguetoken");
				if (t != null) {
					stats = RLStats.getAPI((String) t);
					Object l = Config.get("rocketleaguelimit");
					if (l != null) {
						stats.setRequestsPerSecond(((JsonNumber) l).intValueExact());
					}
				} else {
					e.getChannel().sendMessage("The bot owner has not added a Rocket League API token!").queue();
					return;
				}
			}
		} else if (args.length > 0) {
			if (args[0].equalsIgnoreCase("token")) {
				if (e.getAuthor().getIdLong() == Config.getOwner()) {
					if (args.length > 1) {
						Config.put("rocketleaguetoken", args[1]);
						Config.save();
						e.getChannel().sendMessage("Saved Rocket League API token!").queue();
						e.getMessage().delete().queue();
						if (stats != null)
							stats.setAuthKey(args[1]);
						else
							stats = RLStats.getAPI(args[1]);
						return;
					} else {
						e.getChannel().sendMessage("Please add a Rocket League API token!").queue();
						return;
					}
				} else {
					e.getChannel().sendMessage("Only the bot owner can add a Rocket League API token!").queue();
					return;
				}
			} else if (args[0].equalsIgnoreCase("ratelimit")) {
				if (e.getAuthor().getIdLong() == Config.getOwner()) {
					if (args.length > 1) {
						try {
							int rateLimit = Integer.valueOf(args[1]);
							Config.put("rocketleaguelimit", rateLimit);
							Config.save();
							if (stats != null)
								stats.setRequestsPerSecond(rateLimit);
							else
								e.getChannel().sendMessage("Please add a Rocket League API token!").queue();
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
							stats.setRequestsPerSecond(((JsonNumber) l).intValueExact());
						}
					} else {
						e.getChannel().sendMessage("The bot owner has not added a Rocket League API token!").queue();
						return;
					}
				}
				switch (args[0].toLowerCase()) {
				case "platforms":
					CompletableFuture<APIReturn<Long, List<PlatformInfo>>> platforms = stats.getPlatforms(e.getMessageIdLong());
					platforms.thenAccept(this::notifyPlatforms);
					returnQueue.put(e.getMessageIdLong(), e.getChannel());
					platforms.exceptionally(ex -> {if (ex.getCause() instanceof IllegalStateException) if (ex.getCause().getMessage().equals("Unauthorized. API key is wrong. (E:401)")) {e.getChannel().sendMessage("Please add a valid Rocket League API token!").queue(); return null;} e.getChannel().sendMessage("Could not get platforms! Please try again!").queue(); returnQueue.remove(e.getMessageIdLong()); ex.printStackTrace(); return null;});
					return;
				case "seasons":
					CompletableFuture<APIReturn<Long, List<Season>>> seasons = stats.getSeasons(e.getMessageIdLong());
					seasons.thenAccept(this::notifySeasons);
					returnQueue.put(e.getMessageIdLong(), e.getChannel());
					seasons.exceptionally(ex -> {if (ex.getCause() instanceof IllegalStateException) if (ex.getCause().getMessage().equals("Unauthorized. API key is wrong. (E:401)")) {e.getChannel().sendMessage("Please add a valid Rocket League API token!").queue(); return null;} e.getChannel().sendMessage("Could not get seasons! Please try again!").queue(); returnQueue.remove(e.getMessageIdLong()); ex.printStackTrace(); return null;});
					return;
				case "leaderboard":
					if (args.length > 1) {
						Stat stat = null;
						switch (args[1].toLowerCase()) {
						case "wins":
							stat = Stat.WINS;
							break;
						case "shots":
							stat = Stat.SHOTS;
							break;
						case "goals":
							stat = Stat.GOALS;
							break;
						case "assists":
							stat = Stat.ASSISTS;
							break;
						case "saves":
							stat = Stat.SAVES;
							break;
						case "mvps":
							stat = Stat.MVP;
							break;
						default:
							stat = Stat.WINS;
							break;
						}
						if (stat != null) {
							CompletableFuture<APIReturn<Long, List<Player>>> leaderboard = stats.getStatLeaderboard(stat, e.getMessageIdLong());
							leaderboard.thenAccept(this::notifyLeaderboard);
							returnQueue.put(e.getMessageIdLong(), e.getChannel());
							leaderboardStat.put(e.getMessageIdLong(), stat);
							leaderboard.exceptionally(ex -> {if (ex.getCause() instanceof IllegalStateException) if (ex.getCause().getMessage().equals("Unauthorized. API key is wrong. (E:401)")) {e.getChannel().sendMessage("Please add a valid Rocket League API token!").queue(); return null;} e.getChannel().sendMessage("Could not get leaderboard! Please try again!").queue(); returnQueue.remove(e.getMessageIdLong()); leaderboardStat.remove(e.getMessageIdLong()); ex.printStackTrace(); return null;});
							return;
						}
					}
					e.getChannel().sendMessage("Please add a valid stat type: `Wins`, `Shots`, `Goals`, `Assists`, `Saves`, `MVPs`").queue();
					return;
				case "ranks":
					if (args.length > 1) {
						try {
							int season = Integer.valueOf(args[1]);
							CompletableFuture<APIReturn<Long, APIReturn<Integer, List<Tier>>>> tiers = stats.getTiers(season, e.getMessageIdLong());
							tiers.thenAccept(this::notifyTiersSeason);
							returnQueue.put(e.getMessageIdLong(), e.getChannel());
							tiers.exceptionally(ex -> {if (ex.getCause() instanceof IllegalStateException) if (ex.getCause().getMessage().equals("Unauthorized. API key is wrong. (E:401)")) {e.getChannel().sendMessage("Please add a valid Rocket League API token!").queue(); return null;} e.getChannel().sendMessage("Could not get ranks! Please try again!").queue(); returnQueue.remove(e.getMessageIdLong()); ex.printStackTrace(); return null;});
						} catch (NullPointerException | NumberFormatException ex) {
							e.getChannel().sendMessage("Please add a valid season number!").queue();
						}
					} else {
						CompletableFuture<APIReturn<Long, List<Tier>>> tiers = stats.getTiers(e.getMessageIdLong());
						tiers.thenAccept(this::notifyTiers);
						returnQueue.put(e.getMessageIdLong(), e.getChannel());
						tiers.exceptionally(ex -> {if (ex.getCause() instanceof IllegalStateException) if (ex.getCause().getMessage().equals("Unauthorized. API key is wrong. (E:401)")) {e.getChannel().sendMessage("Please add a valid Rocket League API token!").queue(); return null;} e.getChannel().sendMessage("Could not get ranks! Please try again!").queue(); returnQueue.remove(e.getMessageIdLong()); ex.printStackTrace(); return null;});
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
							playlists.exceptionally(ex -> {if (ex.getCause() instanceof IllegalStateException) if (ex.getCause().getMessage().equals("Unauthorized. API key is wrong. (E:401)")) {e.getChannel().sendMessage("Please add a valid Rocket League API token!").queue(); return null;} e.getChannel().sendMessage("Could not get playlists! Please try again!").queue(); returnQueue.remove(e.getMessageIdLong()); playlistRequest.remove(e.getMessageIdLong()); ex.printStackTrace(); return null;});
						} else {
							e.getChannel().sendMessage("Please add a valid platform name!").queue();
						}
					} else {
						CompletableFuture<APIReturn<Long, List<Playlist>>> playlists = stats.getPlaylistInfo(e.getMessageIdLong());
						playlists.thenAccept(this::notifyPlaylists);
						returnQueue.put(e.getMessageIdLong(), e.getChannel());
						playlists.exceptionally(ex -> {if (ex.getCause() instanceof IllegalStateException) if (ex.getCause().getMessage().equals("Unauthorized. API key is wrong. (E:401)")) {e.getChannel().sendMessage("Please add a valid Rocket League API token!").queue(); return null;} e.getChannel().sendMessage("Could not get playlists! Please try again!").queue(); returnQueue.remove(e.getMessageIdLong()); ex.printStackTrace(); return null;});
					}
					return;
				case "player":
					if (args.length >= 2) {
						Platform pl = Platform.fromName(args[1].toUpperCase());
						if (pl != null) {
							if (pl == Platform.PS4 || pl == Platform.XBOX) {
								if (args.length >= 4) {
									try {
										int season = Integer.valueOf(args[3]);
										if (season > 0)
											playerSeason.put(e.getMessageIdLong(), season);
										else {
											e.getChannel().sendMessage("Please use a valid season number!").queue();
											return;
										}
									} catch (NumberFormatException ex) {
										e.getChannel().sendMessage("Please use a valid season number!").queue();
										return;
									}
								}
								CompletableFuture<APIReturn<Long, Player>> search = stats.getPlayer(args[2], pl, e.getMessageIdLong());
								search.thenAccept(this::notifyPlayer);
								returnQueue.put(e.getMessageIdLong(), e.getChannel());
								search.exceptionally(ex -> {if (ex.getCause() instanceof IllegalStateException) {if (ex.getCause().getMessage().equals("Unauthorized. API key is wrong. (E:401)")) {e.getChannel().sendMessage("Please add a valid Rocket League API token!").queue(); return null;} else {ex.printStackTrace(); e.getChannel().sendMessage("An error has occurred! Please try again!").queue();}} else if (ex.getCause() instanceof NullPointerException) e.getChannel().sendMessage("Player not found!").queue(); returnQueue.remove(e.getMessageIdLong()); return null;});
								return;
							} else if (pl == Platform.STEAM) {
								try {
									Long.valueOf(args[2]);
									if (args[2].startsWith("7656119")) {
										if (args.length >= 4) {
											try {
												int season = Integer.valueOf(args[3]);
												if (season > 0)
													playerSeason.put(e.getMessageIdLong(), season);
												else {
													e.getChannel().sendMessage("Please use a valid season number!").queue();
													return;
												}
											} catch (NumberFormatException ex) {
												e.getChannel().sendMessage("Please use a valid season number!").queue();
												return;
											}
										}
										CompletableFuture<APIReturn<Long, Player>> search = stats.getPlayer(args[2], pl, e.getMessageIdLong());
										search.thenAccept(this::notifyPlayer);
										returnQueue.put(e.getMessageIdLong(), e.getChannel());
										search.exceptionally(ex -> {if (ex.getCause() instanceof IllegalStateException) {if (ex.getCause().getMessage().equals("Unauthorized. API key is wrong. (E:401)")) {e.getChannel().sendMessage("Please add a valid Rocket League API token!").queue(); return null;} else {ex.printStackTrace(); e.getChannel().sendMessage("An error has occurred! Please try again!").queue();}} else if (ex.getCause() instanceof NullPointerException) e.getChannel().sendMessage("Player not found!").queue(); returnQueue.remove(e.getMessageIdLong()); return null;});
										return;
									}
								} catch (NullPointerException | NumberFormatException ex) {}
								if (args[2].length() >= 3) {
									if (args.length >= 4) {
										try {
											int season = Integer.valueOf(args[3]);
											if (season > 0)
												playerSeason.put(e.getMessageIdLong(), season);
											else {
												e.getChannel().sendMessage("Please use a valid season number!").queue();
												return;
											}
										} catch (NumberFormatException ex) {
											e.getChannel().sendMessage("Please use a valid season number!").queue();
											return;
										}
									}
									Long id = 0L;
									try {
										id = SteamAPI.getSteamID(args[2]);
									} catch (IllegalStateException ex) {
										if (ex.getCause() instanceof IllegalStateException)
											e.getChannel().sendMessage(ex.getCause().getMessage()).queue();
										else
											e.getChannel().sendMessage(ex.getMessage()).queue();
										return;
									}
									if (id != 0) {
										CompletableFuture<APIReturn<Long, Player>> search = stats.getPlayer(String.valueOf(id), pl, e.getMessageIdLong());
										search.thenAccept(this::notifyPlayer);
										returnQueue.put(e.getMessageIdLong(), e.getChannel());
										search.exceptionally(ex -> {if (ex.getCause() instanceof IllegalStateException) {if (ex.getCause().getMessage().equals("Unauthorized. API key is wrong. (E:401)")) {e.getChannel().sendMessage("Please add a valid Rocket League API token!").queue(); return null;} else {ex.printStackTrace(); e.getChannel().sendMessage("An error has occurred! Please try again!").queue();}} else if (ex.getCause() instanceof NullPointerException) e.getChannel().sendMessage("Player not found!").queue(); returnQueue.remove(e.getMessageIdLong()); return null;});
										return;
									} else {
										e.getChannel().sendMessage("Player not found!").queue();
										return;
									}
								}
								e.getChannel().sendMessage("Please add a valid player name or id to get stats for!").queue();
								return;
							} else {
								e.getChannel().sendMessage("Please add a player name or platform and player name with ranked season number to get stats for!").queue();
								return;
							}
						} else {
							if (args.length >= 3) {
								try {
									int season = Integer.valueOf(args[2]);
									if (season > 0)
										playerSeason.put(e.getMessageIdLong(), season);
									else {
										e.getChannel().sendMessage("Please use a valid season number!").queue();
										return;
									}
								} catch (NumberFormatException ex) {
									e.getChannel().sendMessage("Please use a valid season number!").queue();
									return;
								}
							}
							CompletableFuture<APIReturn<Long, SearchResultPage>> search = stats.searchPlayers(args[1], e.getMessageIdLong());
							search.thenAccept(this::notifySearch);
							returnQueue.put(e.getMessageIdLong(), e.getChannel());
							search.exceptionally(ex -> {if (ex.getCause() instanceof IllegalStateException) {if (ex.getCause().getMessage().equals("Unauthorized. API key is wrong. (E:401)")) {e.getChannel().sendMessage("Please add a valid Rocket League API token!").queue(); return null;} else {ex.printStackTrace(); e.getChannel().sendMessage("An error has occurred! Please try again!").queue();}} else if (ex.getCause() instanceof NullPointerException) e.getChannel().sendMessage("Player not found!").queue(); returnQueue.remove(e.getMessageIdLong()); return null;});
							return;
						}
					} else {
						e.getChannel().sendMessage("Please add a player name or platform and player name with ranked season number to get stats for!").queue();
						return;
					}
				}
			}
		}
		if (e.getAuthor().getIdLong() == Config.getOwner()) {
			e.getChannel().sendMessage("Valid subcommands: `token`, `ratelimit`, `platforms`, `seasons`, `ranks`, `playlists`, `player`, `leaderboard`").queue();
		} else {
			e.getChannel().sendMessage("Valid subcommands: `platforms`, `seasons`, `ranks`, `playlists`, `player`, `leaderboard`").queue();
		}
		return;
	}

	void notifyPlatforms(APIReturn<Long, List<PlatformInfo>> data) {
		MessageChannel channel = returnQueue.remove(data.getID());
		String ret = "Available Platforms: ";
		boolean first = true;
		for (PlatformInfo pi : data.getValue()) {
			if (first)
				first = false;
			else
				ret += ", ";
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
			if (first)
				first = false;
			else
				ret += "\n";
			if (s.getEndTime() == -1) {
				ret += "`" + s.getId() + "`: Started: `" + formatDate(s.getStartTime()) + "` Ended: `No`";
			} else {
				ret += "`" + s.getId() + "`: Started: `" + formatDate(s.getStartTime()) + "` Ended: `" + formatDate(s.getEndTime()) + "`";
			}
		}
		season = data.getValue().size();
		seasonUpdate = System.currentTimeMillis();
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
		HashMap<Integer, String> tierMap = new HashMap<Integer, String>();
		for (Tier t : data.getValue()) {
			tierMap.put(t.getId(), t.getName());
			if (first)
				first = false;
			else
				ret += "\n";
			ret += "`" + t.getId() + "`: `" + t.getName() + "`";
		}
		tiers = tierMap;
		tiersUpdate = System.currentTimeMillis();
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
		HashMap<Integer, String> tierMap = new HashMap<Integer, String>();
		for (Tier t : value.getValue()) {
			if (season == RLCommand.season)
				tierMap.put(t.getId(), t.getName());
			if (first)
				first = false;
			else
				ret += "\n";
			ret += "`" + t.getId() + "`: `" + t.getName() + "`";
		}
		if (season == RLCommand.season) {
			tiers = tierMap;
			tiersUpdate = System.currentTimeMillis();
		}
		embed.addField("Season " + season + " Ranks:", ret, false);
		channel.sendMessage(embed.build()).queue();
	}

	void notifyPlaylists(APIReturn<Long, List<Playlist>> data) {
		MessageChannel channel = returnQueue.remove(data.getID());
		List<Playlist> playlists = data.getValue();
		sendPlaylists(channel, playlists, null);
	}

	void notifyPlaylistsPlatform(APIReturn<Long, List<Playlist>> data) {
		MessageChannel channel = returnQueue.remove(data.getID());
		List<Playlist> playlists = data.getValue();
		Platform pl = playlistRequest.remove(data.getID());
		sendPlaylists(channel, playlists, pl);
	}

	void notifyLeaderboard(APIReturn<Long, List<Player>> data) {
		MessageChannel channel = returnQueue.remove(data.getID());
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(new Color(6, 128, 211));
		embed.setFooter("https://rocketleaguestats.com/", null);
		String ret = "";
		List<Player> players = data.getValue();
		Stat s = leaderboardStat.remove(data.getID());
		String statType = "";
		switch (s) {
		case WINS:
			statType = "Wins";
			break;
		case SHOTS:
			statType = "Shots";
			break;
		case GOALS:
			statType = "Goals";
			break;
		case ASSISTS:
			statType = "Assists";
			break;
		case SAVES:
			statType = "Saves";
			break;
		case MVP:
			statType = "MVPs";
			break;
		default:
			statType = "Wins";
			break;
		}
		for (int i = 0; i < 10; i++) {
			if (i > 0)
				ret += "\n";
			Player p = players.get(i);
			ret += "**" + (i + 1) + "**: `" + Language.escape(p.getDisplayName()) + "` on " + p.getPlatform().getName() + ": " + statType + ": " + p.getStats().getStat(s);
		}
		embed.addField("Top " + statType, ret, false);
		embed.setDescription("[Leaderboard](https://rocketleaguestats.com/leaderboards)");
		channel.sendMessage(embed.build()).queue();
	}
	
	private static void sendPlaylists(MessageChannel channel, List<Playlist> data, Platform pl) {
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(new Color(6, 128, 211));
		embed.setFooter("https://rocketleaguestats.com/", null);
		HashMap<Integer, Integer> players = new HashMap<Integer, Integer>();
		for (Playlist p : data) {
			if (pl == null) {
				int i = 0;
				if (players.containsKey(p.getId())) {
					i = players.get(p.getId());
				}
				i += p.getPlayers();
				players.put(p.getId(), i);
			} else if (p.getPlatform() == pl) {
				int i = 0;
				if (players.containsKey(p.getId())) {
					i = players.get(p.getId());
				}
				i += p.getPlayers();
				players.put(p.getId(), i);
			}
		}
		if (pl == null)
			embed.setTitle("Global Playlists:");
		else
			embed.setTitle(pl.getName() + " Playlists:");
		String ranked = "";
		ranked += "Ranked Standard (3v3): `" + players.get(Playlist.RANKED_STANDARD) + " Playing`\n";
		ranked += "Ranked Doubles (2v2): `" + players.get(Playlist.RANKED_DOUBLES) + " Playing`\n";
		ranked += "Ranked Solo Duel (1v1): `" + players.get(Playlist.RANKED_DUEL) + " Playing`\n";
		ranked += "Ranked Solo Standard (3v3): `" + players.get(Playlist.RANKED_SOLO_STANDARD) + " Playing`\n";
		embed.addField("Ranked:", ranked, true);
		String soccar = "";
		soccar += "Standard (3v3): `" + players.get(Playlist.STANDARD) + " Playing`\n";
		soccar += "Doubles (2v2): `" + players.get(Playlist.DOUBLES) + " Playing`\n";
		soccar += "Duel (1v1): `" + players.get(Playlist.DUEL) + " Playing`\n";
		soccar += "Chaos (4v4): `" + players.get(Playlist.CHAOS) + " Playing`\n";
		embed.addField("Soccar:", soccar, true);
		String sports = "";
		sports += "Drop Shot (3v3): `" + players.get(Playlist.DROPSHOT) + " Playing`\n";
		sports += "Rumble (3v3): `" + players.get(Playlist.RUMBLE) + " Playing`\n";
		sports += "Snow Day (3v3): `" + players.get(Playlist.SNOW_DAY) + " Playing`\n";
		sports += "Hoops (2v2): `" + players.get(Playlist.HOOPS) + " Playing`\n";
		sports += "Rocket Labs (3v3): `" + players.get(Playlist.ROCKET_LABS) + " Playing`\n";
		sports += "Mutator Mashup (3v3): `" + players.get(Playlist.MUTATOR_MASHUP) + " Playing`\n";
		embed.addField("Sports:", sports, true);
		channel.sendMessage(embed.build()).queue();
	}

	void notifyPlayer(APIReturn<Long, Player> data) {
		MessageChannel channel = returnQueue.remove(data.getID());
		int season = -1;
		if (playerSeason.containsKey(data.getID()))
			season = playerSeason.remove(data.getID());
		Player p = data.getValue();
		if (p != null) {
			if (season == -1)
				sendPlayer(channel, p);
			else
				sendPlayer(channel, p, season);
			return;
		}
		channel.sendMessage("Please add a valid player name or id to get stats for!").queue();
	}

	void notifySearch(APIReturn<Long, SearchResultPage> data) {
		MessageChannel channel = returnQueue.remove(data.getID());
		SearchResultPage result = data.getValue();
		List<Player> players = result.getResults();
		int season = -1;
		if (playerSeason.containsKey(data.getID()))
			season = playerSeason.remove(data.getID());
		Player p = null;
		if (result.getTotalResults() > 0) {
			p = players.get(0);
			if (season == -1)
				sendPlayer(channel, p);
			else
				sendPlayer(channel, p, season);
			return;
		}
		channel.sendMessage("Please add a valid player name or id to get stats for!").queue();
	}
	
	private static void sendPlayer(MessageChannel channel, Player p, int seasonID) {
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(new Color(6, 128, 211));
		embed.setFooter("https://rocketleaguestats.com/", null);
		String url = p.getAvatarUrl();
		if (url == null)
			url = "https://cdn.discordapp.com/attachments/376557695420989441/435138854693896192/rls_partner_vertical_small.png";
		embed.setThumbnail(url);
		embed.setDescription("**[" + Language.escape(p.getDisplayName()) + "'s Stats on " + p.getPlatform().getName() + ":](" + p.getProfileUrl() + ")**");
		String season = "";
		HashMap<Integer, String> tierMap = new HashMap<Integer, String>();
		if (System.currentTimeMillis() - tiersUpdate >= 86400000) {
			CompletableFuture<APIReturn<Long, List<Tier>>> tiers = RLCommand.stats.getTiers(0L);
			try {
				APIReturn<Long, List<Tier>> ret = tiers.get();
				for (Tier t : ret.getValue()) {
					tierMap.put(t.getId(), t.getName());
				}
				RLCommand.tiers = tierMap;
				tiersUpdate = System.currentTimeMillis();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		SeasonInfo info = p.getSeasonInfo(seasonID);
		if (info != null) {
			season += getRankedInfo(info.getAllPlaylistInfo());
		} else {
			season += "Did Not Participate";
		}
		embed.addField("Season " + seasonID + ":", season, true);
		String time = "";
		time += "**First Data Fetched:** " + formatDate(p.getCreated()) + "\n";
		time += "**Updated:** " + formatDateTime(p.getUpdated()) + "\n";
		embed.addField("Data:", time, true);
		channel.sendMessage(embed.build()).queue();
	}
	
	private static void sendPlayer(MessageChannel channel, Player p) {
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(new Color(6, 128, 211));
		embed.setFooter("https://rocketleaguestats.com/", null);
		String url = p.getAvatarUrl();
		if (url == null)
			url = "https://cdn.discordapp.com/attachments/376557695420989441/435138854693896192/rls_partner_vertical_small.png";
		embed.setThumbnail(url);
		embed.setDescription("**[" + Language.escape(p.getDisplayName()) + "'s Stats on " + p.getPlatform().getName() + ":](" + p.getProfileUrl() + ")**");
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
		stats += "**MVP Rate:** " + new BigDecimal(String.valueOf(mvpAccuracy)).setScale(2, RoundingMode.HALF_UP) + "%";
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
		time += "**First Data Fetched:** " + formatDate(p.getCreated()) + "\n";
		time += "**Updated:** " + formatDateTime(p.getUpdated()) + "\n";
		embed.addField("Data:", time, true);
		String season = "";
		if (System.currentTimeMillis() - seasonUpdate >= 86400000) {
			CompletableFuture<APIReturn<Long, List<Season>>> seasons = RLCommand.stats.getSeasons(0L);
			try {
				APIReturn<Long, List<Season>> ret = seasons.get();
				RLCommand.season = ret.getValue().size();
				seasonUpdate = System.currentTimeMillis();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		HashMap<Integer, String> tierMap = new HashMap<Integer, String>();
		if (System.currentTimeMillis() - tiersUpdate >= 86400000) {
			CompletableFuture<APIReturn<Long, List<Tier>>> tiers = RLCommand.stats.getTiers(0L);
			try {
				APIReturn<Long, List<Tier>> ret = tiers.get();
				for (Tier t : ret.getValue()) {
					tierMap.put(t.getId(), t.getName());
				}
				RLCommand.tiers = tierMap;
				tiersUpdate = System.currentTimeMillis();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		SeasonInfo info = p.getSeasonInfo(RLCommand.season);
		if (info != null) {
			season += getRankedInfo(info.getAllPlaylistInfo());
		} else {
			season += "Did Not Participate";
		}
		embed.addField("Season " + RLCommand.season + ":", season, true);
		channel.sendMessage(embed.build()).queue();
	}
	
	private static String getRankedInfo(Map<Integer, PlaylistInfo> playlists) {
		String season = "";
		PlaylistInfo standard = playlists.get(Playlist.RANKED_STANDARD);
		PlaylistInfo doubles = playlists.get(Playlist.RANKED_DOUBLES);
		PlaylistInfo duel = playlists.get(Playlist.RANKED_DUEL);
		PlaylistInfo soloStandard = playlists.get(Playlist.RANKED_SOLO_STANDARD);
		if (standard != null) {
			if (standard.getMatchesPlayed() > 0) {
				season += "**Ranked Standard:** " + tiers.get(standard.getTier()) + " Division " + standard.getDivision() + "\n";
				season += "− Ranking Points: " + standard.getRankPoints() + " Matches Played: " + standard.getMatchesPlayed() + "\n";
			}
		}
		if (doubles != null) {
			if (doubles.getMatchesPlayed() > 0) {
				season += "**Ranked Doubles:** " + tiers.get(doubles.getTier()) + " Division " + standard.getDivision() + "\n";
				season += "− Ranking Points: " + standard.getRankPoints() + " Matches Played: " + standard.getMatchesPlayed() + "\n";
			}
		}
		if (duel != null) {
			if (duel.getMatchesPlayed() > 0) {
				season += "**Ranked Duel:** " + tiers.get(duel.getTier()) + " Division " + duel.getDivision() + "\n";
				season += "− Ranking Points: " + duel.getRankPoints() + " Matches Played: " + duel.getMatchesPlayed() + "\n";
			}
		}
		if (soloStandard != null) {
			if (soloStandard.getMatchesPlayed() > 0) {
				season += "**Ranked Solo Standard:** " + tiers.get(soloStandard.getTier()) + " Division " + soloStandard.getDivision() + "\n";
				season += "− Ranking Points: " + soloStandard.getRankPoints() + " Matches Played: " + soloStandard.getMatchesPlayed() + "\n";
			}
		}
		if (season.equals(""))
			season = "Did Not Participate";
		return season;
	}

	public String getDesc(long guildID) {
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
