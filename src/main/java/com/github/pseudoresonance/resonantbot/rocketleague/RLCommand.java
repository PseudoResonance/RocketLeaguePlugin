package com.github.pseudoresonance.resonantbot.rocketleague;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.github.pseudoresonance.resonantbot.api.CommandHandler;
import com.github.pseudoresonance.resonantbot.api.Plugin;
import com.github.pseudoresonance.resonantbot.apiplugin.RequestTimeoutException;
import com.github.pseudoresonance.resonantbot.data.Data;
import com.github.pseudoresonance.resonantbot.language.LanguageManager;
import com.github.pseudoresonance.resonantbot.permissions.PermissionGroup;
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
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;

public class RLCommand {

	private static CommandHandler cmd = null;

	private static RocketLeagueStats stats = null;

	private final static DecimalFormat df = new DecimalFormat("#.#");

	private static LinkedHashMap<UUID, RequestData> returnQueue = new LinkedHashMap<UUID, RequestData>();

	private static int season = 0;

	public static void setup(Plugin plugin) {
		if (stats == null) {
			Object limitO = Data.getBotSetting("rocketleaguelimit");
			int limit = 2;
			if (limitO instanceof Integer)
				limit = (int) limitO;
			stats = new RocketLeagueStats(TimeUnit.SECONDS, 1, limit);
		}
		
		cmd = new CommandHandler("rocketleague", "rocketleague.help");
		cmd.registerSubcommand("ratelimit", (e, command, args) -> {
			if (args.length > 0) {
				try {
					int rateLimit = Integer.valueOf(args[0]);
					Data.setBotSetting("rocketleaguelimit", rateLimit);
					stats.setRateLimit(TimeUnit.SECONDS, 1, rateLimit);
					e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("rocketleague.savedRateLimit")).queue();
					return true;
				} catch (NullPointerException | NumberFormatException ex) {
					e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("rocketleague.validRateLimit")).queue();
					return false;
				}
			} else {
				e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("rocketleague.validRateLimit")).queue();
				return false;
			}
		}, PermissionGroup.BOT_OWNER);
		cmd.registerSubcommand("leaderboard", (e, command, args) -> {
			LeaderboardStat stat = LeaderboardStat.TRACKER_SCORE;
			int page = 1;
			boolean showBoardTypes = false;
			if (args.length == 1) {
				try {
					page = Integer.valueOf(args[1]);
				} catch (NumberFormatException ex) {
					stat = LeaderboardStat.fromName(args[1]);
					if (stat == null)
						showBoardTypes = true;
				}
			} else if (args.length >= 2) {
				stat = LeaderboardStat.fromName(args[1]);
				if (stat == null)
					showBoardTypes = true;
				else {
					try {
						page = Integer.valueOf(args[2]);
					} catch (NumberFormatException ex) {
						page = 1;
					}
				}
			}
			if (showBoardTypes) {
				String options = "";
				for (LeaderboardStat s : LeaderboardStat.values())
					options += "`" + s.getInternalName().toLowerCase() + "`, ";
				options = options.substring(0, options.length() - 1);
				e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("main.validSubcommands", options)).queue();
				return true;
			}
			CompletableFuture<Message> placeholder = e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("main.fetchingData")).submit();
			UUID uuid = UUID.randomUUID();
			if (page > 10)
				page = 10;
			if (page < 1)
				page = 1;
			CompletableFuture<APIReturn<UUID, Leaderboard>> leaderboardRet = stats.getLeaderboard(uuid, stat);
			leaderboardRet.thenAcceptAsync(RLCommand::notifyLeaderboardPage).exceptionally(ex -> {
				if (ex instanceof RequestTimeoutException) {
					String text = LanguageManager.getLanguage(e).getMessage("main.rateLimit");
					try {
						Message msg = placeholder.get();
						msg.editMessage(text).queue();
					} catch (InterruptedException | ExecutionException e1) {
						e.getChannel().sendMessage(text).queue();
					}
				} else {
					String text = LanguageManager.getLanguage(e).getMessage("main.errorOccurred");
					try {
						Message msg = placeholder.get();
						msg.editMessage(text).queue();
					} catch (InterruptedException | ExecutionException e1) {
						e.getChannel().sendMessage(text).queue();
					}
				}
				returnQueue.remove(uuid);
				return null;
			});
			try {
				returnQueue.put(uuid, new RequestData(placeholder, (e.getChannelType() == ChannelType.PRIVATE ? e.getChannel() : e.getGuild()).getIdLong(), e.getChannel(), page));
			} catch (Exception ex) {
				returnQueue.put(uuid, new RequestData(placeholder, (e.getChannelType() == ChannelType.PRIVATE ? e.getChannel() : e.getGuild()).getIdLong(), e.getChannel(), page));
			}
			return true;
		});
		cmd.registerSubcommand("player", (e, command, args) -> {
			Platform pl = Platform.STEAM;
			String name = "";
			int season = -1;
			if (args.length >= 1) {
				if (args.length == 1)
					name = args[0];
				else if (args.length >= 2) {
					Platform plTest = Platform.fromName(args[0]);
					if (plTest != null) {
						if (args[1].length() >= 2) {
							name = args[1];
							pl = plTest;
							if (args.length >= 3) {
								try {
									int seasonTest = Integer.valueOf(args[2]);
									if (seasonTest > 0)
										season = seasonTest;
									else {
										e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("rocketleague.validSeason")).queue();
										return true;
									}
								} catch (NumberFormatException ex) {
									e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("rocketleague.validSeason")).queue();
									return true;
								}
							}
						} else {
							name = args[0];
							try {
								int seasonTest = Integer.valueOf(args[1]);
								if (seasonTest > 0)
									season = seasonTest;
								else {
									e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("rocketleague.validSeason")).queue();
									return true;
								}
							} catch (NumberFormatException ex) {
								e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("rocketleague.validSeason")).queue();
								return true;
							}
						}
					} else {
						name = args[0];
						try {
							int seasonTest = Integer.valueOf(args[1]);
							if (seasonTest > 0)
								season = seasonTest;
							else {
								e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("rocketleague.validSeason")).queue();
								return true;
							}
						} catch (NumberFormatException ex) {
							e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("rocketleague.validSeason")).queue();
							return true;
						}
					}
				}
			} else {
				e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("rocketleague.validPlayerNameID")).queue();
				return true;
			}
			if (!name.equals("")) {
				CompletableFuture<Message> placeholder = e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("main.fetchingData")).submit();
				UUID uuid = UUID.randomUUID();
				CompletableFuture<APIReturn<UUID, Player>> playerRet = stats.getPlayer(uuid, pl, name);
				playerRet.thenAcceptAsync(RLCommand::notifyPlayer).exceptionally(ex -> {
					if (ex instanceof InvalidPlayerException || (ex.getCause() != null && ex.getCause() instanceof InvalidPlayerException)) {
						String text = LanguageManager.getLanguage(e).getMessage("rocketleague.invalidPlayer");
						try {
							Message msg = placeholder.get();
							msg.editMessage(text).queue();
						} catch (InterruptedException | ExecutionException e1) {
							e.getChannel().sendMessage(text).queue();
						}
					} else if (ex instanceof RequestTimeoutException) {
						String text = LanguageManager.getLanguage(e).getMessage("main.rateLimit");
						try {
							Message msg = placeholder.get();
							msg.editMessage(text).queue();
						} catch (InterruptedException | ExecutionException e1) {
							e.getChannel().sendMessage(text).queue();
						}
					} else {
						String text = LanguageManager.getLanguage(e).getMessage("main.errorOccurred");
						try {
							Message msg = placeholder.get();
							msg.editMessage(text).queue();
						} catch (InterruptedException | ExecutionException e1) {
							e.getChannel().sendMessage(text).queue();
						}
					}
					returnQueue.remove(uuid);
					return null;
				});
				returnQueue.put(uuid, new RequestData(placeholder, (e.getChannelType() == ChannelType.PRIVATE ? e.getChannel() : e.getGuild()).getIdLong(), e.getChannel(), season));
				return true;
			}
			return true;
		});
		cmd.register(plugin);
	}

	private static void notifyLeaderboardPage(APIReturn<UUID, Leaderboard> ret) {
		RequestData dat = returnQueue.remove(ret.getID());
		MessageChannel channel = dat.getChannel();
		int page = dat.getData();
		Leaderboard leaderboard = ret.getValue();
		String title = LanguageManager.getLanguage(dat.getLangId()).getMessage("rocketleague.top", leaderboard.getName());
		ArrayList<LeaderboardEntry> rankings = leaderboard.getEntries();
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(new Color(6, 128, 211));
		String board = "";
		for (int i = 10 * (page - 1); i < 10 * page; i++) {
			LeaderboardEntry entry = rankings.get(i);
			if (entry != null) {
				if (i > 10 * (page - 1))
					board += "\n";
				board += LanguageManager.getLanguage(dat.getLangId()).getMessage("rocketleague.leaderboardEntry", entry.getPosition(), LanguageManager.escape(entry.getName()), entry.getPlatform().getName(), df.format(entry.getScore()) + entry.getSuffix());
			}
		}
		embed.addField(LanguageManager.getLanguage(dat.getLangId()).getMessage("main.page", page), board, false);
		embed.setTitle(title, leaderboard.getURL());
		try {
			Message msg = dat.getPlaceholder().get();
			msg.editMessage(embed.build()).override(true).queue();
		} catch (InterruptedException | ExecutionException e) {
			channel.sendMessage(embed.build()).queue();
		}
	}

	private static void notifyPlayer(APIReturn<UUID, Player> ret) {
		RequestData dat = returnQueue.remove(ret.getID());
		if (dat.getData() < 1)
			sendPlayer(dat.getPlaceholder(), dat.getChannel(), dat.getLangId(), ret.getValue());
		else
			sendPlayer(dat.getPlaceholder(), dat.getChannel(), dat.getLangId(), ret.getValue(), dat.getData());
	}

	private static void sendPlayer(CompletableFuture<Message> placeholder, MessageChannel channel, long langId, Player p, int seasonID) {
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(new Color(6, 128, 211));
		embed.setTitle(LanguageManager.getLanguage(langId).getMessage("rocketleague.playerStatsOnPlatform", LanguageManager.escape(p.getDisplayName()), p.getPlatform().getName()), p.getProfileURL());
		String season = "";
		SeasonStats info = p.getSeason(seasonID);
		if (info != null) {
			season += getRankedInfo(langId, info.getPlaylists());
		} else {
			season += LanguageManager.getLanguage(langId).getMessage("rocketleague.didNotParticipate");
		}
		embed.addField(LanguageManager.getLanguage(langId).getMessage("rocketleague.season", seasonID), season, true);
		try {
			Message msg = placeholder.get();
			msg.editMessage(embed.build()).override(true).queue();
		} catch (InterruptedException | ExecutionException e) {
			channel.sendMessage(embed.build()).queue();
		}
	}

	private static void sendPlayer(CompletableFuture<Message> placeholder, MessageChannel channel, long langId, Player p) {
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(new Color(6, 128, 211));
		embed.setTitle(LanguageManager.getLanguage(langId).getMessage("rocketleague.playerStatsOnPlatform", LanguageManager.escape(p.getDisplayName()), p.getPlatform().getName()), p.getProfileURL());
		Stats s = p.getStats();
		String stats = "";
		stats += LanguageManager.getLanguage(langId).getMessage("rocketleague.trackerScore", s.getTrackerScore()) + "\n";
		stats += LanguageManager.getLanguage(langId).getMessage("rocketleague.wins", s.getWins()) + "\n";
		stats += LanguageManager.getLanguage(langId).getMessage("rocketleague.shots", s.getShots()) + "\n";
		stats += LanguageManager.getLanguage(langId).getMessage("rocketleague.goals", s.getGoals()) + "\n";
		stats += LanguageManager.getLanguage(langId).getMessage("rocketleague.shotAccuracy", s.getShotAccuracy()) + "\n";
		stats += LanguageManager.getLanguage(langId).getMessage("rocketleague.assists", s.getAssists()) + "\n";
		stats += LanguageManager.getLanguage(langId).getMessage("rocketleague.saves", s.getSaves()) + "\n";
		stats += LanguageManager.getLanguage(langId).getMessage("rocketleague.mvps", s.getMvps()) + "\n";
		stats += LanguageManager.getLanguage(langId).getMessage("rocketleague.mvpRate", s.getMvpRate());
		embed.addField(LanguageManager.getLanguage(langId).getMessage("rocketleague.stats"), stats, true);
		String style = "";
		style += LanguageManager.getLanguage(langId).getMessage("rocketleague.playStyleGoals", s.getPlayStyleGoals()) + "\n";
		style += LanguageManager.getLanguage(langId).getMessage("rocketleague.playStyleAssists", s.getPlayStyleAssists()) + "\n";
		style += LanguageManager.getLanguage(langId).getMessage("rocketleague.playStyleSaves", s.getPlayStyleSaves());
		embed.addField(LanguageManager.getLanguage(langId).getMessage("rocketleague.playStyle"), style, true);
		RewardLevel level = p.getRewardLevel();
		if (level != null) {
			String reward = "";
			reward += LanguageManager.getLanguage(langId).getMessage("rocketleague.rank", level.getRank()) + "\n";
			reward += LanguageManager.getLanguage(langId).getMessage("rocketleague.wins", level.getWins());
			embed.addField(LanguageManager.getLanguage(langId).getMessage("rocketleague.rewardLevel"), reward, true);
		}
		String season = "";
		SeasonStats info = p.getSeason(RLCommand.season);
		if (info != null) {
			season += getRankedInfo(langId, info.getPlaylists());
		} else {
			season += LanguageManager.getLanguage(langId).getMessage("rocketleague.didNotParticipate");
		}
		embed.addField(LanguageManager.getLanguage(langId).getMessage("rocketleague.season", RLCommand.season), season, true);
		try {
			Message msg = placeholder.get();
			msg.editMessage(embed.build()).override(true).queue();
		} catch (InterruptedException | ExecutionException e) {
			channel.sendMessage(embed.build()).queue();
		}
	}

	private static String getRankedInfo(long langId, ArrayList<PlaylistStats> playlists) {
		String season = "";
		for (PlaylistStats playlist : playlists) {
			season += "**" + playlist.getName() + ":** " + playlist.getRank() + "\n";
			if (playlist.getGames() >= 0) {
				if (playlist.getStreak() > 0) {
					if (playlist.getStreakType() == StreakType.WINNING)
						season += LanguageManager.getLanguage(langId).getMessage("rocketleague.seasonStatsWin", playlist.getRating(), playlist.getGames(), playlist.getStreak()) + "\n";
					else
						season += LanguageManager.getLanguage(langId).getMessage("rocketleague.seasonStatsLoss", playlist.getRating(), playlist.getGames(), playlist.getStreak()) + "\n";
				} else
					season += LanguageManager.getLanguage(langId).getMessage("rocketleague.seasonStats", playlist.getRating(), playlist.getGames()) + "\n";
			} else {
				season += LanguageManager.getLanguage(langId).getMessage("rocketleague.seasonStats", playlist.getRating(), "n/a") + "\n";
			}
		}
		if (season.equals(""))
			season = LanguageManager.getLanguage(langId).getMessage("rocketleague.didNotParticipate");
		return season;
	}

	public static void setSeason(int season) {
		RLCommand.season = season;
	}

	public static int getSeason() {
		return season;
	}

	private static class RequestData {

		private CompletableFuture<Message> placeholder;
		private long langId;
		private MessageChannel channel;
		private int data;

		public RequestData(CompletableFuture<Message> placeholder, long langId, MessageChannel channel, int data) {
			this.placeholder = placeholder;
			this.langId = langId;
			this.channel = channel;
			this.data = data;
		}

		public CompletableFuture<Message> getPlaceholder() {
			return placeholder;
		}

		public long getLangId() {
			return langId;
		}

		public MessageChannel getChannel() {
			return channel;
		}

		public int getData() {
			return data;
		}
	}

}
