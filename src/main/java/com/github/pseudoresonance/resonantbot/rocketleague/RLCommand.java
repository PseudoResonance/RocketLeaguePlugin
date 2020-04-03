package com.github.pseudoresonance.resonantbot.rocketleague;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.github.pseudoresonance.resonantbot.Config;
import com.github.pseudoresonance.resonantbot.api.Command;
import com.github.pseudoresonance.resonantbot.apiplugin.RequestTimeoutException;
import com.github.pseudoresonance.resonantbot.data.Data;
import com.github.pseudoresonance.resonantbot.language.LanguageManager;
import com.github.pseudoresonance.resonantbot.rocketleague.api.InvalidPlayerException;
import com.github.pseudoresonance.resonantbot.rocketleague.api.RocketLeagueStats;
import com.github.pseudoresonance.resonantbot.rocketleague.api.entities.APIReturn;
import com.github.pseudoresonance.resonantbot.rocketleague.api.entities.Leaderboard;
import com.github.pseudoresonance.resonantbot.rocketleague.api.entities.LeaderboardEntry;
import com.github.pseudoresonance.resonantbot.rocketleague.api.entities.LeaderboardStat;
import com.github.pseudoresonance.resonantbot.rocketleague.api.entities.Platform;
import com.github.pseudoresonance.resonantbot.rocketleague.api.entities.Player;
import com.github.pseudoresonance.resonantbot.rocketleague.api.entities.PlaylistStats;
import com.github.pseudoresonance.resonantbot.rocketleague.api.entities.RewardLevel;
import com.github.pseudoresonance.resonantbot.rocketleague.api.entities.SeasonStats;
import com.github.pseudoresonance.resonantbot.rocketleague.api.entities.Stats;
import com.github.pseudoresonance.resonantbot.rocketleague.api.entities.PlaylistStats.StreakType;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class RLCommand implements Command {

	private static RocketLeagueStats stats = null;
	
	private final static DecimalFormat df = new DecimalFormat("#.#");

	private static LinkedHashMap<Long, ImmutablePair<Long, MessageChannel>> returnQueue = new LinkedHashMap<Long, ImmutablePair<Long, MessageChannel>>();

	private static LinkedHashMap<Long, Integer> leaderboardPage = new LinkedHashMap<Long, Integer>();
	private static LinkedHashMap<Long, Integer> playerSeason = new LinkedHashMap<Long, Integer>();
	
	private static int season = 0;

	public void onCommand(MessageReceivedEvent e, String command, String[] args) {
		if (stats == null)
			stats = new RocketLeagueStats();
		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("ratelimit")) {
				if (e.getAuthor().getIdLong() == Config.getOwner()) {
					if (args.length > 1) {
						try {
							int rateLimit = Integer.valueOf(args[1]);
							Data.setBotSetting("rocketleaguelimit", rateLimit);
							stats.setRateLimit(TimeUnit.SECONDS, 1, rateLimit);
							e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("gameutils.savedRateLimit")).queue();
							return;
						} catch (NullPointerException | NumberFormatException ex) {
							e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("gameutils.validRateLimit")).queue();
							return;
						}
					} else {
						e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("gameutils.validRateLimit")).queue();
						return;
					}
				} else {
					e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("gameutils.botOwnerChangeRateLimit")).queue();
					return;
				}
			} else {
				switch (args[0].toLowerCase()) {
				case "leaderboard":
					LeaderboardStat stat = LeaderboardStat.TRACKER_SCORE;
					int page = 1;
					if (args.length == 1) {
						e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("main.validSubcommands", "`score`, `rewardlevel`, `goalshot`, `goals`, `wins`, `shots`, `mvps`, `saves`, `assists`")).queue();
						return;
					}
					if (args.length >= 2) {
						try {
							page = Integer.valueOf(args[1]);
						} catch (NumberFormatException ex) {
							stat = LeaderboardStat.fromName(args[1]);
							if (stat == null) {
								e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("main.validSubcommands", "`score`, `rewardlevel`, `goalshot`, `goals`, `wins`, `shots`, `mvps`, `saves`, `assists`")).queue();
								return;
							}
						}
					}
					if (args.length >= 3) {
						try {
							page = Integer.valueOf(args[2]);
						} catch (NumberFormatException ex) {}
					}
					if (page > 10)
						page = 10;
					if (page > 1) {
						CompletableFuture<APIReturn<Long, Leaderboard>> leaderboardRet = stats.getLeaderboard(e.getMessageIdLong(), stat);
						leaderboardRet.thenAcceptAsync(RLCommand::notifyLeaderboardPage)
						.exceptionally(ex -> {if (ex instanceof RequestTimeoutException) e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("main.rateLimit")).queue(); else e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("main.errorOccurred")).queue(); returnQueue.remove(e.getMessageIdLong()); return null;});
						returnQueue.put(e.getMessageIdLong(), new ImmutablePair<Long, MessageChannel>((e.getChannelType() == ChannelType.PRIVATE ? e.getChannel() : e.getGuild()).getIdLong(), e.getChannel()));
						leaderboardPage.put(e.getMessageIdLong(), page);
						return;
					}
					CompletableFuture<APIReturn<Long, Leaderboard>> leaderboardRet = stats.getLeaderboard(e.getMessageIdLong(), stat);
					leaderboardRet.thenAcceptAsync(RLCommand::notifyLeaderboard)
					.exceptionally(ex -> {if (ex instanceof RequestTimeoutException) e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("main.rateLimit")).queue(); else e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("main.errorOccurred")).queue(); returnQueue.remove(e.getMessageIdLong()); return null;});
					returnQueue.put(e.getMessageIdLong(), new ImmutablePair<Long, MessageChannel>((e.getChannelType() == ChannelType.PRIVATE ? e.getChannel() : e.getGuild()).getIdLong(), e.getChannel()));
					return;
				case "player":
					Platform pl = Platform.STEAM;
					String name = "";
					int season = -1;
					if (args.length >= 2) {
						if (args.length == 2)
							name = args[1];
						else if (args.length >= 3) {
							Platform plTest = Platform.fromName(args[1]);
							if (plTest != null) {
								if (args[2].length() >= 3) {
									name = args[2];
									pl = plTest;
									if (args.length >= 4) {
										try {
											int seasonTest = Integer.valueOf(args[3]);
											if (seasonTest > 0)
												season = seasonTest;
											else {
												e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("gameutils.validSeason")).queue();
												return;
											}
										} catch (NumberFormatException ex) {
											e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("gameutils.validSeason")).queue();
											return;
										}
									}
								} else {
									name = args[1];
									try {
										int seasonTest = Integer.valueOf(args[2]);
										if (seasonTest > 0)
											season = seasonTest;
										else {
											e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("gameutils.validSeason")).queue();
											return;
										}
									} catch (NumberFormatException ex) {
										e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("gameutils.validSeason")).queue();
										return;
									}
								}
							} else {
								name = args[1];
								try {
									int seasonTest = Integer.valueOf(args[2]);
									if (seasonTest > 0)
										season = seasonTest;
									else {
										e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("gameutils.validSeason")).queue();
										return;
									}
								} catch (NumberFormatException ex) {
									e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("gameutils.validSeason")).queue();
									return;
								}
							}
						}
					} else {
						e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("gameutils.validPlayerNameID")).queue();
						return;
					}
					if (!name.equals("")) {
						if (season >= 1) {
							CompletableFuture<APIReturn<Long, Player>> playerRet = stats.getPlayer(e.getMessageIdLong(), pl, name);
							playerRet.thenAcceptAsync(RLCommand::notifyPlayer)
							.exceptionally(ex -> { if (ex instanceof InvalidPlayerException || (ex.getCause() != null && ex.getCause() instanceof InvalidPlayerException)) e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("gameutils.invalidPlayer")).queue(); else if (ex instanceof RequestTimeoutException) e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("main.rateLimit")).queue(); else e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("main.errorOccurred")).queue(); returnQueue.remove(e.getMessageIdLong()); return null;});
							returnQueue.put(e.getMessageIdLong(), new ImmutablePair<Long, MessageChannel>((e.getChannelType() == ChannelType.PRIVATE ? e.getChannel() : e.getGuild()).getIdLong(), e.getChannel()));
							playerSeason.put(e.getMessageIdLong(), season);
							return;
						}
						CompletableFuture<APIReturn<Long, Player>> playerRet = stats.getPlayer(e.getMessageIdLong(), pl, name);
						playerRet.thenAcceptAsync(RLCommand::notifyPlayer)
						.exceptionally(ex -> { if (ex instanceof InvalidPlayerException || (ex.getCause() != null && ex.getCause() instanceof InvalidPlayerException)) e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("gameutils.invalidPlayer")).queue(); else if (ex instanceof RequestTimeoutException) e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("main.rateLimit")).queue(); else e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("main.errorOccurred")).queue(); returnQueue.remove(e.getMessageIdLong()); return null;});
						returnQueue.put(e.getMessageIdLong(), new ImmutablePair<Long, MessageChannel>((e.getChannelType() == ChannelType.PRIVATE ? e.getChannel() : e.getGuild()).getIdLong(), e.getChannel()));
						return;
					}
				}
			}
		}
		if (e.getAuthor().getIdLong() == Config.getOwner()) {
			e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("main.validSubcommands", "`ratelimit`, `playlists`, `player`, `leaderboard`")).queue();
		} else {
			e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("main.validSubcommands", "`playlists`, `player`, `leaderboard`")).queue();
		}
		return;
	}

	private static void notifyLeaderboard(APIReturn<Long, Leaderboard> ret) {
		ImmutablePair<Long, MessageChannel> pair = returnQueue.remove(ret.getID());
		MessageChannel channel = pair.getValue();
		Leaderboard leaderboard = ret.getValue();
		String title = LanguageManager.getLanguage(pair.getKey()).getMessage("gameutils.top", leaderboard.getName());
		ArrayList<LeaderboardEntry> rankings = leaderboard.getEntries();
		sendLeaderboard(channel, pair.getKey(), rankings, 0, 10, title, leaderboard.getURL(), 1);
	}

	private static void notifyLeaderboardPage(APIReturn<Long, Leaderboard> ret) {
		ImmutablePair<Long, MessageChannel> pair = returnQueue.remove(ret.getID());
		MessageChannel channel = pair.getValue();
		int page = leaderboardPage.remove(ret.getID());
		Leaderboard leaderboard = ret.getValue();
		String title = LanguageManager.getLanguage(pair.getKey()).getMessage("gameutils.top", leaderboard.getName());
		ArrayList<LeaderboardEntry> rankings = leaderboard.getEntries();
		sendLeaderboard(channel, pair.getKey(), rankings, 10 * (page - 1), 10 * page, title, leaderboard.getURL(), page);
	}
	
	private static void sendLeaderboard(MessageChannel channel, long langId, ArrayList<LeaderboardEntry> rankings, int start, int max, String title, String url, int page) {
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(new Color(6, 128, 211));
		String board = "";
		for (int i = start; i < max; i++) {
			LeaderboardEntry entry = rankings.get(i);
			if (entry != null) {
				if (i > start)
					board += "\n";
				board += LanguageManager.getLanguage(langId).getMessage("gameutils.leaderboardEntry", entry.getPosition(), LanguageManager.escape(entry.getName()), entry.getPlatform().getName(), df.format(entry.getScore()) + entry.getSuffix());
			}
		}
		embed.addField(LanguageManager.getLanguage(langId).getMessage("gameutils.page", page), board, false);
		embed.setTitle(title, url);
		channel.sendMessage(embed.build()).queue();
	}
	
	private static void notifyPlayer(APIReturn<Long, Player> ret) {
		ImmutablePair<Long, MessageChannel> pair = returnQueue.remove(ret.getID());
		if (!playerSeason.containsKey(ret.getID()))
			sendPlayer(pair.getValue(), pair.getKey(), ret.getValue());
		else {
			int season = playerSeason.remove(ret.getID());
			sendPlayer(pair.getValue(), pair.getKey(), ret.getValue(), season);
		}
	}
	
	private static void sendPlayer(MessageChannel channel, long langId, Player p, int seasonID) {
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(new Color(6, 128, 211));
		embed.setTitle(LanguageManager.getLanguage(langId).getMessage("gameutils.playerStatsOnPlatform", LanguageManager.escape(p.getDisplayName()), p.getPlatform().getName()), p.getProfileURL());
		String season = "";
		SeasonStats info = p.getSeason(seasonID);
		if (info != null) {
			season += getRankedInfo(langId, info.getPlaylists());
		} else {
			season += LanguageManager.getLanguage(langId).getMessage("gameutils.didNotParticipate");
		}
		embed.addField(LanguageManager.getLanguage(langId).getMessage("gameutils.season", seasonID), season, true);
		channel.sendMessage(embed.build()).queue();
	}
	
	private static void sendPlayer(MessageChannel channel, long langId, Player p) {
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(new Color(6, 128, 211));
		embed.setTitle(LanguageManager.getLanguage(langId).getMessage("gameutils.playerStatsOnPlatform", LanguageManager.escape(p.getDisplayName()), p.getPlatform().getName()), p.getProfileURL());
		Stats s = p.getStats();
		String stats = "";
		stats += LanguageManager.getLanguage(langId).getMessage("gameutils.trackerScore", s.getTrackerScore()) + "\n";
		stats += LanguageManager.getLanguage(langId).getMessage("gameutils.wins", s.getWins()) + "\n";
		stats += LanguageManager.getLanguage(langId).getMessage("gameutils.shots", s.getShots()) + "\n";
		stats += LanguageManager.getLanguage(langId).getMessage("gameutils.goals", s.getGoals()) + "\n";
		stats += LanguageManager.getLanguage(langId).getMessage("gameutils.shotAccuracy", s.getShotAccuracy()) + "\n";
		stats += LanguageManager.getLanguage(langId).getMessage("gameutils.assists", s.getAssists()) + "\n";
		stats += LanguageManager.getLanguage(langId).getMessage("gameutils.saves", s.getSaves()) + "\n";
		stats += LanguageManager.getLanguage(langId).getMessage("gameutils.mvps", s.getMvps()) + "\n";
		stats += LanguageManager.getLanguage(langId).getMessage("gameutils.mvpRate", s.getMvpRate());
		embed.addField(LanguageManager.getLanguage(langId).getMessage("gameutils.stats"), stats, true);
		String style = "";
		style += LanguageManager.getLanguage(langId).getMessage("gameutils.playStyleGoals", s.getPlayStyleGoals()) + "\n";
		style += LanguageManager.getLanguage(langId).getMessage("gameutils.playStyleAssists", s.getPlayStyleAssists()) + "\n";
		style += LanguageManager.getLanguage(langId).getMessage("gameutils.playStyleSaves", s.getPlayStyleSaves());
		embed.addField(LanguageManager.getLanguage(langId).getMessage("gameutils.playStyle"), style, true);
		RewardLevel level = p.getRewardLevel();
		if (level != null) {
			String reward = "";
			reward += LanguageManager.getLanguage(langId).getMessage("gameutils.rank", level.getRank()) + "\n";
			reward += LanguageManager.getLanguage(langId).getMessage("gameutils.wins", level.getWins());
			embed.addField(LanguageManager.getLanguage(langId).getMessage("gameutils.rewardLevel"), reward, true);
		}
		String season = "";
		SeasonStats info = p.getSeason(RLCommand.season);
		if (info != null) {
			season += getRankedInfo(langId, info.getPlaylists());
		} else {
			season += LanguageManager.getLanguage(langId).getMessage("gameutils.didNotParticipate");
		}
		embed.addField(LanguageManager.getLanguage(langId).getMessage("gameutils.season", RLCommand.season), season, true);
		channel.sendMessage(embed.build()).queue();
	}
	
	private static String getRankedInfo(long langId, ArrayList<PlaylistStats> playlists) {
		String season = "";
		for (PlaylistStats playlist : playlists) {
			season += "**" + playlist.getName() + ":** " + playlist.getRank() + "\n";
			if (playlist.getGames() >= 0) {
				if (playlist.getStreak() > 0) {
					if (playlist.getStreakType() == StreakType.WINNING)
						season += LanguageManager.getLanguage(langId).getMessage("gameutils.seasonStatsWin", playlist.getRating(), playlist.getGames(), playlist.getStreak()) + "\n";
					else
						season += LanguageManager.getLanguage(langId).getMessage("gameutils.seasonStatsLoss", playlist.getRating(), playlist.getGames(), playlist.getStreak()) + "\n";
				} else
					season += LanguageManager.getLanguage(langId).getMessage("gameutils.seasonStats", playlist.getRating(), playlist.getGames()) + "\n";
			} else {
				season += LanguageManager.getLanguage(langId).getMessage("gameutils.seasonStats", playlist.getRating(), "n/a") + "\n";
			}
		}
		if (season.equals(""))
			season = LanguageManager.getLanguage(langId).getMessage("gameutils.didNotParticipate");
		return season;
	}
	
	public static void setSeason(int season) {
		RLCommand.season = season;
	}
	
	public static int getSeason() {
		return season;
	}

	public String getDesc(long id) {
		return LanguageManager.getLanguage(id).getMessage("gameutils.rlCommandDescription");
	}

	public boolean isHidden() {
		return false;
	}

}
