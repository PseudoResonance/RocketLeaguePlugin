package com.github.pseudoresonance.resonantbot.rocketleague.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.github.pseudoresonance.resonantbot.apiplugin.RateLimiter;
import com.github.pseudoresonance.resonantbot.apiplugin.RequestTimeoutException;
import com.github.pseudoresonance.resonantbot.rocketleague.RLCommand;
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

public class RocketLeagueStats {
	
	private static final ArrayList<RocketLeagueStats> instances = new ArrayList<RocketLeagueStats>();
	
	private final static String baseUrl = "https://rocketleague.tracker.network/";
	
	private RateLimiter rateLimiter;
    private final ExecutorService exPool = Executors.newCachedThreadPool();
	
	private final ConcurrentHashMap<String, Player> playerCache = new ConcurrentHashMap<String, Player>();
	private final ConcurrentHashMap<String, Object> playerBlock = new ConcurrentHashMap<String, Object>();
	
	private final ConcurrentHashMap<String, Leaderboard> leaderboardCache = new ConcurrentHashMap<String, Leaderboard>();
	private final ConcurrentHashMap<String, Object> leaderboardBlock = new ConcurrentHashMap<String, Object>();
	
	/**
	 * Constructs {@link RocketLeagueStats} with the default rate limit of 2.
	 */
	public RocketLeagueStats() {
		this.rateLimiter = RateLimiter.create(2, 1, TimeUnit.SECONDS);
		instances.add(this);
	}
	
	/**
	 * Constructs {@link RocketLeagueStats} with the given rate limit.
	 * 
	 * @param unit Rate limit time unit
	 * @param duration Rate limit duration
	 * @param rateLimit Rate limit
	 */
	public RocketLeagueStats(TimeUnit unit, long duration, int rateLimit) {
		this.rateLimiter = RateLimiter.create(rateLimit, duration, unit);
		instances.add(this);
	}


	/**
	 * Shuts down this instance of {@link RocketLeagueStats}
	 */
	public void shutdown() {
		exPool.shutdown();
		instances.remove(this);
	}
	
	/**
	 * Shuts down all instances of {@link RocketLeagueStats}
	 */
	public static void shutdownAll() {
		for (RocketLeagueStats inst : instances) {
			inst.exPool.shutdown();
		}
		instances.clear();
	}
	
	/**
	 * Checks if the player with the given name on the given platform is in the cache.
	 * 
	 * @param platform Platform of the player
	 * @param name Name of the player
	 * @return boolean for whether or not the player is cached
	 */
	public boolean isPlayerCached(Platform platform, String name) {
		String key = platform.toString() + "|" + name;
		if (playerCache.containsKey(key))
			if (!playerCache.get(key).isExpired())
				return true;
		return false;
	}
	
	/**
	 * Gets the {@link Player} with the given name on the given platform.
	 * 
	 * @param messageId ID of the requesting message
	 * @param platform Platform of the player
	 * @param name Name of the player
	 * @return CompletableFuture for APIReturn encompassing the messageID and player
	 */
	public CompletableFuture<APIReturn<Long, Player>> getPlayer(Long messageId, Platform platform, String name) {
		String key = platform.toString() + "|" + name;
		if (playerBlock.containsKey(key)) {
			try {
				Object o = playerBlock.get(key);
				synchronized(o) {
					o.wait(10000);
				}
			} catch (InterruptedException e) {}
		}
		if (playerCache.containsKey(key))
			if (!playerCache.get(key).isExpired())
				return CompletableFuture.completedFuture(new APIReturn<Long, Player>(messageId, playerCache.get(key)));
		String endpoint = "profile/" + platform.getInternalName() + "/" + name;
		return CompletableFuture.supplyAsync(() -> {
			playerBlock.put(key, new Object());
			try (WebClient client = new WebClient()) {
				client.getOptions().setCssEnabled(false);
				client.getOptions().setJavaScriptEnabled(false);
				String url = baseUrl + endpoint;
				try {
					rateLimiter.acquire(15, TimeUnit.SECONDS);
				} catch (RequestTimeoutException e) {
					throw e;
				} catch (InterruptedException e) {
					throw new RequestTimeoutException();
				}
				HtmlPage page = client.getPage(url);
				while (checkError(page)) {
					Thread.sleep(1000);
					try {
						rateLimiter.acquire(15, TimeUnit.SECONDS);
					} catch (RequestTimeoutException e) {
						throw e;
					} catch (InterruptedException e) {
						throw new RequestTimeoutException();
					}
					page.refresh();
				}
				if (checkNoExist(page))
					throw new InvalidPlayerException(name);
				String displayName = name;
				List<HtmlElement> displayList = page.getByXPath("/html/body/div[@class='container content-container']/div/div[@class='trn-profile']/header[@class='card profile-header']/div[@class='top-bit']/div[@class='info']/h1[@class='name']");
				if (displayList.size() > 0)
					displayName = displayList.get(0).getChildNodes().get(2).toString().trim();
				String statXPath = "/html/body/div[@class='container content-container']/div/div[@class='trn-profile']/div[@class='profile-main']/div[@class='content']/div[@class='card']/div[@class='stats-large']/div/";
				List<HtmlElement> trackerScoreList = page.getByXPath(statXPath + "div[@data-stat='Score']");
				double trackerScore = -1d;
				if (trackerScoreList.size() > 0)
					trackerScore = Double.valueOf(trackerScoreList.get(0).asText().replaceAll("[,%]", ""));
				List<HtmlElement> winsList = page.getByXPath(statXPath + "div[@data-stat='Wins']");
				int wins = -1;
				if (winsList.size() > 0)
					wins = Integer.valueOf(winsList.get(0).asText().replaceAll("[,%]", ""));
				List<HtmlElement> goalsList = page.getByXPath(statXPath + "div[@data-stat='Goals']");
				int goals = -1;
				if (goalsList.size() > 0)
					goals = Integer.valueOf(goalsList.get(0).asText().replaceAll("[,%]", ""));
				List<HtmlElement> savesList = page.getByXPath(statXPath + "div[@data-stat='Saves']");
				int saves = -1;
				if (savesList.size() > 0)
					saves = Integer.valueOf(savesList.get(0).asText().replaceAll("[,%]", ""));
				List<HtmlElement> shotsList = page.getByXPath(statXPath + "div[@data-stat='Shots']");
				int shots = -1;
				if (shotsList.size() > 0)
					shots = Integer.valueOf(shotsList.get(0).asText().replaceAll("[,%]", ""));
				List<HtmlElement> mvpsList = page.getByXPath(statXPath + "div[@data-stat='MVPs']");
				int mvps = -1;
				if (mvpsList.size() > 0)
					mvps = Integer.valueOf(mvpsList.get(0).asText().replaceAll("[,%]", ""));
				List<HtmlElement> assistsList = page.getByXPath(statXPath + "div[@data-stat='Assists']");
				int assists = -1;
				if (assistsList.size() > 0)
					assists = Integer.valueOf(assistsList.get(0).asText().replaceAll("[,%]", ""));
				Stats stats = new Stats(trackerScore, wins, shots, goals, assists, saves, mvps);
				HashMap<Integer, SeasonStats> seasonStats = new HashMap<Integer, SeasonStats>();
				String seasonXPath = "/html/body/div[@class='container content-container']/div/div[@class='trn-profile']/div[@class='profile-main']/div[@class='aside']/div[@class='card-table-container']/div";
				List<HtmlElement> seasonList = page.getByXPath(seasonXPath);
				RewardLevel rewardLevel = null;
				int highestSeason = 0;
				for (HtmlElement elem : seasonList) {
					if (elem.getId().startsWith("season-")) {
						ArrayList<PlaylistStats> playlists = new ArrayList<PlaylistStats>();
						int id = Integer.valueOf(elem.getId().substring(7, elem.getId().length()));
						if (id > highestSeason)
							highestSeason = id;
						List<HtmlElement> tableList = elem.getByXPath("table");
						for (int i = 0; i < tableList.size(); i++) {
							HtmlElement table = tableList.get(i);
							if (!elem.getAttribute("style").equals("display:none;") && table.getByXPath("thead/tr/th").size() > 2) {
								List<HtmlElement> rankList = table.getByXPath("tbody/tr");
								for (HtmlElement rankElem : rankList) {
									String playlistName = "";
									String rank = "";
									int rating = -1;
									int games = -1;
									int streak = -1;
									StreakType streakType = StreakType.WINNING;
									List<HtmlElement> second = rankElem.getByXPath("td[2]");
									if (second.size() > 0) {
										HtmlElement inner = second.get(0);
										playlistName = inner.getFirstChild().toString().trim();
										List<HtmlElement> small = inner.getByXPath("small");
										if (small.size() > 0) {
											HtmlElement smallInner = small.get(0);
											rank = smallInner.asText().trim();
										}
									}
									List<HtmlElement> fourth = rankElem.getByXPath("td[4]");
									if (fourth.size() > 0) {
										HtmlElement inner = fourth.get(0);
										rating = Integer.valueOf(inner.getFirstChild().toString().trim().replaceAll("[,%]", ""));
									}
									List<HtmlElement> sixth = rankElem.getByXPath("td[6]");
									if (sixth.size() > 0) {
										HtmlElement inner = sixth.get(0);
										String content = inner.getFirstChild().toString().trim();
										if (!content.equals("n/a")) {
											games = Integer.valueOf(inner.getFirstChild().toString().trim().replaceAll("[,%]", ""));
											streak = 0;
											List<HtmlElement> small = inner.getByXPath("small");
											if (small.size() > 0) {
												HtmlElement smallInner = small.get(0);
												String uncleaned = smallInner.getFirstChild().toString().trim();
												String[] split = uncleaned.split(": ");
												String cleaned = split[1];
												streak = Integer.valueOf(cleaned.replaceAll("[,%]", ""));
												if (split[0].equalsIgnoreCase("Losing Streak"))
													streakType = StreakType.LOSING;
											}
										} else {
											games = -1;
											streak = -1;
										}
									}
									PlaylistStats playlistStats = new PlaylistStats(playlistName, rank, rating, games, streak, streakType);
									playlists.add(playlistStats);
								}
								SeasonStats season = new SeasonStats(id, playlists);
								seasonStats.put(id, season);
							} else if (elem.getAttribute("style").equals("display:none;")) {
								List<HtmlElement> rankList = table.getByXPath("tbody/tr");
								for (HtmlElement rankElem : rankList) {
									String playlistName = "";
									String rank = "";
									int rating = -1;
									int games = -1;
									List<HtmlElement> second = rankElem.getByXPath("td[2]");
									if (second.size() > 0) {
										HtmlElement inner = second.get(0);
										playlistName = inner.getFirstChild().toString().trim();
										List<HtmlElement> small = inner.getByXPath("small");
										if (small.size() > 0) {
											HtmlElement smallInner = small.get(0);
											rank = smallInner.asText().trim();
										}
									}
									List<HtmlElement> fourth = rankElem.getByXPath("td[3]");
									if (fourth.size() > 0) {
										HtmlElement inner = fourth.get(0);
										rating = Integer.valueOf(inner.getFirstChild().toString().trim().replaceAll("[,%]", ""));
									}
									List<HtmlElement> sixth = rankElem.getByXPath("td[4]");
									if (sixth.size() > 0) {
										HtmlElement inner = sixth.get(0);
										String content = inner.getFirstChild().toString().trim();
										if (!content.equals("n/a")) {
											games = Integer.valueOf(inner.getFirstChild().toString().trim().replaceAll("[,%]", ""));
										} else {
											games = -1;
										}
									}
									PlaylistStats playlistStats = new PlaylistStats(playlistName, rank, rating, games);
									playlists.add(playlistStats);
								}
								SeasonStats season = new SeasonStats(id, playlists);
								seasonStats.put(id, season);
							} else if (!elem.getAttribute("style").equals("display:none;") && table.getByXPath("thead/tr/th").size() == 2) {
								List<HtmlElement> rowList = table.getByXPath("tbody/tr");
								for (HtmlElement rowElem : rowList) {
									String rank = "";
									int seasonWins = -1;
									List<HtmlElement> second = rowElem.getByXPath("td[2]");
									if (second.size() > 0) {
										HtmlElement inner = second.get(0);
										List<HtmlElement> small = inner.getByXPath("small");
										if (small.size() > 0) {
											HtmlElement smallInner = small.get(0);
											rank = smallInner.asText().trim();
										}
									}
									List<HtmlElement> fourth = rowElem.getByXPath("td[3]");
									if (fourth.size() > 0) {
										HtmlElement inner = fourth.get(0);
										seasonWins = Integer.valueOf(inner.getFirstChild().toString().trim().replaceAll("[,%]", ""));
									}
									rewardLevel = new RewardLevel(rank, seasonWins);
								}
							}
						}
					}
				}
				RLCommand.setSeason(highestSeason);
				Player player = new Player(platform, name, displayName, stats, seasonStats, rewardLevel, baseUrl + endpoint);
				playerCache.put(key, player);
				Object o = playerBlock.get(key);
				synchronized(o) {
					o.notifyAll();
					playerBlock.remove(key);
				}
				return new APIReturn<Long, Player>(messageId, player);
			} catch (FailingHttpStatusCodeException | IOException | InterruptedException e) {
				throw new RuntimeException(e);
			} catch (InvalidPlayerException | RequestTimeoutException e) {
				throw e;
			}
		}, exPool);
	}
	
	/**
	 * Checks if the leaderboard with the given stat type is in the cache.
	 * 
	 * @param stat {@link LeaderboardStat} to get
	 * @return boolean for whether or not the player is cached
	 */
	public boolean isLeaderboardCached(LeaderboardStat stat) {
		String key = stat.toString();
		if (leaderboardCache.containsKey(key))
			if (!leaderboardCache.get(key).isExpired())
				return true;
		return false;
	}
	
	/**
	 * Gets the {@link Leaderboard} with the given stat type.
	 * 
	 * @param messageId ID of the requesting message
	 * @param stat {@link LeaderboardStat} to get
	 * @return CompletableFuture for APIReturn encompassing the messageID and leaderboard
	 */
	public CompletableFuture<APIReturn<Long, Leaderboard>> getLeaderboard(Long messageId, LeaderboardStat stat) {
		String key = stat.toString();
		if (leaderboardBlock.containsKey(key)) {
			try {
				Object o = leaderboardBlock.get(key);
				synchronized(o) {
					o.wait(10000);
				}
			} catch (InterruptedException e) {}
		}
		if (leaderboardCache.containsKey(key))
			if (!leaderboardCache.get(key).isExpired())
				return CompletableFuture.completedFuture(new APIReturn<Long, Leaderboard>(messageId, leaderboardCache.get(key)));
		String endpoint = "leaderboards/all/" + stat.getInternalName();
		return CompletableFuture.supplyAsync(() -> {
			leaderboardBlock.put(key, new Object());
			try (WebClient client = new WebClient()) {
				client.getOptions().setCssEnabled(false);
				client.getOptions().setJavaScriptEnabled(false);
				String url = baseUrl + endpoint;
				try {
					rateLimiter.acquire(15, TimeUnit.SECONDS);
				} catch (RequestTimeoutException e) {
					throw e;
				} catch (InterruptedException e) {
					throw new RequestTimeoutException();
				}
				HtmlPage page = client.getPage(url);
				String platformName = stat.getInternalName();
				List<HtmlElement> nameList = page.getByXPath("/html/body/div[@class='container content-container']/div/div[@class='trn-container']/div[@class='row']/div[@class='col-md-8']/div[@class='well well-sm clearfix nomargin']/div[@class='pull-left']/h4");
				if (nameList.size() > 0)
					platformName = nameList.get(0).asText().trim();
				ArrayList<LeaderboardEntry> rankings = new ArrayList<LeaderboardEntry>();
				List<HtmlElement> rankList = page.getByXPath("/html/body/div[@class='container content-container']/div/div[@class='trn-container']/div[@class='row']/div[@class='col-md-8']/table[@class='table table-bordered table-striped']/tbody/tr");
				for (int i = 1; i < rankList.size(); i++) {
					if (rankings.size() >= 100)
						break;
					String name = "";
					Platform platform = Platform.STEAM;
					double score = 0d;
					int position = i - 1;
					String suffix = "";
					List<HtmlElement> columns = rankList.get(i).getByXPath("td");
					if (columns.size() < 3)
						continue;
					for (int c = 0; c < columns.size(); c++) {
						HtmlElement column = columns.get(c);
						if (c == 0) {
							position = Integer.valueOf(column.getFirstChild().asText().trim().replaceAll("[,%]", ""));
						} else if (c == 1) {
							Iterator<DomElement> children = column.getChildElements().iterator();
							int l = -1;
							while (children.hasNext()) {
								DomElement element = children.next();
								l++;
								if (l == 1) {
									name = element.asText().trim();
								} else if (l == 2) {
									String clazz = element.toString().trim();
									if (clazz.contains("steam")) {
										platform = Platform.STEAM;
									} else if (clazz.contains("playstation")) {
										platform = Platform.PS4;
									} else if (clazz.contains("xbox")) {
										platform = Platform.XBOX;
									}
								}
							}
						} else if (c == 2) {
							String temp = column.getFirstElementChild().asText().trim().replaceAll(",", "");
							if (temp.endsWith("%")) {
								score = Double.valueOf(temp.substring(0, temp.length() - 1));
								suffix = "%";
							} else
								score = Double.valueOf(temp);
						}
					}
					LeaderboardEntry entry = new LeaderboardEntry(platform, name, position, score, suffix);
					rankings.add(entry);
				}
				Leaderboard leaderboard = new Leaderboard(platformName, stat, rankings, baseUrl + endpoint);
				leaderboardCache.put(key, leaderboard);
				Object o = leaderboardBlock.get(key);
				synchronized(o) {
					o.notifyAll();
					leaderboardBlock.remove(key);
				}
				return new APIReturn<Long, Leaderboard>(messageId, leaderboard);
			} catch (FailingHttpStatusCodeException | IOException e) {
				throw new RuntimeException(e);
			} catch (RequestTimeoutException e) {
				throw e;
			}
		}, exPool);
	}
	
	private boolean checkNoExist(HtmlPage page) {
		String errorXPath = "/html/body/div[@class='container content-container']/div/div[@class='alert alert-danger alert-dismissable']";
		List<HtmlElement> errorList = page.getByXPath(errorXPath);
		for (HtmlElement elem : errorList) {
			if (elem.asText().contains("We could not find your stats")) {
				return true;
			}
		}
		return false;
	}
	
	private boolean checkError(HtmlPage page) {
		String errorXPath = "/html/body/div[@class='container content-container']/div/div[@class='alert alert-danger alert-dismissable']";
		List<HtmlElement> errorList = page.getByXPath(errorXPath);
		for (HtmlElement elem : errorList) {
			if (elem.asText().contains("Error updating your stats")) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Sets rate limit
	 * 
	 * @param unit Rate limit time unit
	 * @param duration Rate limit duration
	 * @param rateLimit Rate limit
	 */
	public void setRateLimit(TimeUnit unit, long duration, int rateLimit) {
		this.rateLimiter = RateLimiter.create(rateLimit, duration, unit);
	}
	
	/**
	 * @return Current rate limit time unit
	 */
	public TimeUnit getRateLimitUnit() {
		return this.rateLimiter.getTimeUnit();
	}
	
	/**
	 * @return Current rate limit duration
	 */
	public long getRateLimitDuration() {
		return this.rateLimiter.getTimePeriod();
	}
	
	/**
	 * @return Current rate limit
	 */
	public int getRateLimit() {
		return this.rateLimiter.getMaxPermits();
	}

}
