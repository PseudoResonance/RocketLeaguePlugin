package com.ssplugins.rlstats;

import com.mashape.unirest.http.JsonNode;
import com.ssplugins.rlstats.entities.*;
import com.ssplugins.rlstats.util.PlayerRequest;
import com.ssplugins.rlstats.util.Query;
import com.ssplugins.rlstats.util.RequestQueue;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

public class IRLStatsAPI implements RLStatsAPI {
	
	private String key = null;
	private String apiVersion = "v1";
	private int requestsPerSecond = 2;
	private Consumer<Exception> exceptionHandler;
	
	private List<Season> seasonsCache = null;
	private long seasonsTime = 0;
	
	private List<PlatformInfo> platformsCache = null;
	private long platformsTime = 0;
	
	private List<Tier> tierCache = null;
	private long tierTime = 0;
	
	private HashMap<Integer, List<Tier>> tiersCache = new HashMap<Integer, List<Tier>>();
	private HashMap<Integer, Long> tiersTime = new HashMap<Integer, Long>();
	
	private List<Playlist> playlistsCache = null;
	private long playlistsTime = 0;
	
	private HashMap<String, Player> playerCache = new HashMap<String, Player>();
	private HashMap<String, Long> playerTime = new HashMap<String, Long>();
	
	private HashMap<String, SearchResultPage> playerSearchCache = new HashMap<String, SearchResultPage>();
	private HashMap<String, Long> playerSearchTime = new HashMap<String, Long>();
	
	private RequestQueue queue = new RequestQueue(this);
	
	IRLStatsAPI() {}
	
	private void basicCheck() {
		if (key == null || key.isEmpty()) throw new IllegalStateException("No API key was provided.");
	}
	
	// Utility Methods
	
	private <T> List<T> jsonNodeToObjectList(JsonNode node, Function<JSONObject, T> converter) {
		List<T> list = new ArrayList<>();
		if (node == null || !node.isArray()) throw new IllegalArgumentException("No data was returned.");
		node.getArray().forEach(o -> list.add(converter.apply((JSONObject) o)));
		return list;
	}
	
	public void exception(Exception e) {
		if (exceptionHandler != null) exceptionHandler.accept(e);
	}
	
	public int getRequestsPerSecond() {
		return requestsPerSecond;
	}
	
	@Override
	public void setExceptionHandler(Consumer<Exception> exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}
	
	@Override
	public void setRequestsPerSecond(int i) {
		this.requestsPerSecond = i;
	}
	
	@Override
	public void setAuthKey(String key) {
		this.key = key;
	}
	
	@Override
	public String getAuthKey() {
		return key;
	}
	
	@Override
	public void setAPIVersion(String version) {
		this.apiVersion = version;
	}
	
	@Override
	public String getAPIVersion() {
		return apiVersion;
	}
	
	@Override
	public CompletableFuture<APIReturn<Long, List<PlatformInfo>>> getPlatforms(long messageID) {
		basicCheck();
		return CompletableFuture.supplyAsync(() -> {
			if (platformsCache != null) {
				if (System.currentTimeMillis() - platformsTime <= 86400000) {
					return new APIReturn<Long, List<PlatformInfo>>(messageID, platformsCache);
				}
			}
			CompletableFuture<JsonNode> response = queue.get(key, apiVersion, "/data/platforms", null);
			try {
				List<PlatformInfo> list = jsonNodeToObjectList(response.get(), PlatformInfo::new);
				platformsCache = list;
				platformsTime = System.currentTimeMillis();
				return new APIReturn<Long, List<PlatformInfo>>(messageID, list);
			} catch (ExecutionException e) {
				throw new CompletionException(e.getCause());
			} catch (InterruptedException | IllegalArgumentException e) {
				throw new CompletionException(e);
			}
		});
	}
	
	@Override
	public CompletableFuture<APIReturn<Long, List<Tier>>> getTiers(long messageID) {
		basicCheck();
		return CompletableFuture.supplyAsync(() -> {
			if (tierCache != null) {
				if (System.currentTimeMillis() - tierTime <= 86400000) {
					return new APIReturn<Long, List<Tier>>(messageID, tierCache);
				}
			}
			CompletableFuture<JsonNode> response = queue.get(key, apiVersion, "/data/tiers", null);
			List<Tier> list = getTiers(response);
			tierCache = list;
			tierTime = System.currentTimeMillis();
			return new APIReturn<Long, List<Tier>>(messageID, list);
		});
	}
	
	@Override
	public CompletableFuture<APIReturn<Long, APIReturn<Integer, List<Tier>>>> getTiers(int season, long messageID) {
		basicCheck();
		return CompletableFuture.supplyAsync(() -> {
			List<Tier> tierList = tiersCache.get(season);
			if (tierList != null) {
				if (System.currentTimeMillis() - tiersTime.get(season) <= 86400000) {
					return new APIReturn<Long, APIReturn<Integer, List<Tier>>>(messageID, new APIReturn<Integer, List<Tier>>(season, tierList));
				}
			}
			CompletableFuture<JsonNode> response = queue.get(key, apiVersion, "/data/tiers/" + season, null);
			List<Tier> list = getTiers(response);
			tiersCache.put(season, list);
			tiersTime.put(season, System.currentTimeMillis());
			return new APIReturn<Long, APIReturn<Integer, List<Tier>>>(messageID, new APIReturn<Integer, List<Tier>>(season, list));
		});
	}
	
	private List<Tier> getTiers(CompletableFuture<JsonNode> response) {
		try {
			return jsonNodeToObjectList(response.get(), Tier::new);
		} catch (ExecutionException e) {
			throw new CompletionException(e.getCause());
		} catch (InterruptedException | IllegalArgumentException e) {
			throw new CompletionException(e);
		}
	}
	
	@Override
	public CompletableFuture<APIReturn<Long, List<Season>>> getSeasons(long messageID) {
		basicCheck();
		return CompletableFuture.supplyAsync(() -> {
			if (seasonsCache != null) {
				if (System.currentTimeMillis() - seasonsTime <= 86400000) {
					return new APIReturn<Long, List<Season>>(messageID, seasonsCache);
				}
			}
			CompletableFuture<JsonNode> response = queue.get(key, apiVersion, "/data/seasons", null);
			try {
				List<Season> list = jsonNodeToObjectList(response.get(), Season::new);
				seasonsCache = list;
				seasonsTime = System.currentTimeMillis();
				return new APIReturn<Long, List<Season>>(messageID, list);
			} catch (ExecutionException e) {
				throw new CompletionException(e.getCause());
			} catch (InterruptedException | IllegalArgumentException e) {
				throw new CompletionException(e);
			}
		});
	}
	
	@Override
	public CompletableFuture<APIReturn<Long, List<Playlist>>> getPlaylistInfo(long messageID) {
		basicCheck();
		return CompletableFuture.supplyAsync(() -> {
			if (playlistsCache != null) {
				if (System.currentTimeMillis() - playlistsTime <= 300000) {
					return new APIReturn<Long, List<Playlist>>(messageID, playlistsCache);
				}
			}
			CompletableFuture<JsonNode> response = queue.get(key, apiVersion, "/data/playlists", null);
			try {
				List<Playlist> list = jsonNodeToObjectList(response.get(), Playlist::new);
				playlistsCache = list;
				playlistsTime = System.currentTimeMillis();
				return new APIReturn<Long, List<Playlist>>(messageID, list);
			} catch (ExecutionException e) {
				throw new CompletionException(e.getCause());
			} catch (InterruptedException | IllegalArgumentException e) {
				throw new CompletionException(e);
			}
		});
	}
	
	@Override
	public CompletableFuture<APIReturn<Long, Player>> getPlayer(String id, int platform, long messageID) {
		basicCheck();
		return CompletableFuture.supplyAsync(() -> {
			Player player = playerCache.get(platform + "|" + id);
			if (player != null) {
				if (playerTime.get(platform + "|" + id) > System.currentTimeMillis()) {
					return new APIReturn<Long, Player>(messageID, player);
				}
			}
			CompletableFuture<JsonNode> response = queue.get(key, apiVersion, "/player", Query.create("unique_id", id).add("platform_id", String.valueOf(platform)));
			try {
				JsonNode node = response.get();
				Player pl = new Player(node.getObject());
				playerCache.put(platform + "|" + id, pl);
				playerTime.put(platform + "|" + id, pl.getNextUpdate());
				return new APIReturn<Long, Player>(messageID, pl);
			} catch (ExecutionException e) {
				throw new CompletionException(e.getCause());
			} catch (InterruptedException e) {
				throw new CompletionException(e);
			}
		});
	}
	
	@Override
	public CompletableFuture<APIReturn<Long, Player>> getPlayer(String id, Platform platform, long messageID) {
		return getPlayer(id, platform.getId(), messageID);
	}
	
	@Override
	public CompletableFuture<APIReturn<Long, List<Player>>> getPlayers(Collection<PlayerRequest> collection, long messageID) {
		if (collection.size() > 10) throw new IllegalArgumentException("Cannot have more than 10 players requested.");
		basicCheck();
		return CompletableFuture.supplyAsync(() -> {
			JSONArray array = new JSONArray();
			collection.forEach(playerRequest -> array.put(playerRequest.toJSONObject()));
			Future<JsonNode> response = queue.post(key, apiVersion, "/player/batch", null, array.toString());
			try {
				return new APIReturn<Long, List<Player>>(messageID, jsonNodeToObjectList(response.get(), Player::new));
			} catch (ExecutionException e) {
				throw new CompletionException(e.getCause());
			} catch (InterruptedException | IllegalArgumentException e) {
				throw new CompletionException(e);
			}
		});
	}
	
	@Override
	public CompletableFuture<APIReturn<Long, List<Player>>> getPlayers(PlayerRequest[] requests, long messageID) {
		return getPlayers(Arrays.asList(requests), messageID);
	}
	
	@Override
	public CompletableFuture<APIReturn<Long, SearchResultPage>> searchPlayers(String displayName, int page, long messageID) {
		basicCheck();
		return CompletableFuture.supplyAsync(() -> {
			SearchResultPage searchPage = playerSearchCache.get(displayName.toLowerCase() + "|" + page);
			if (searchPage != null) {
				if (System.currentTimeMillis() - playerSearchTime.get(displayName.toLowerCase() + "|" + page) <= 300000) {
					return new APIReturn<Long, SearchResultPage>(messageID, searchPage);
				}
			}
			Future<JsonNode> response = queue.get(key, apiVersion, "/search/players", Query.create("display_name", displayName).add("page", String.valueOf(page)));
			try {
				JsonNode node = response.get();
				SearchResultPage search = new SearchResultPage(node.getObject());
				playerSearchCache.put(displayName.toLowerCase() + "|" + page, search);
				playerSearchTime.put(displayName.toLowerCase() + "|" + page, System.currentTimeMillis());
				return new APIReturn<Long, SearchResultPage>(messageID, search);
			} catch (ExecutionException e) {
				throw new CompletionException(e.getCause());
			} catch (InterruptedException | IllegalArgumentException e) {
				throw new CompletionException(e);
			}
		});
	}
	
	@Override
	public CompletableFuture<APIReturn<Long, SearchResultPage>> searchPlayers(String displayName, long messageID) {
		return searchPlayers(displayName, 0, messageID);
	}
	
	@Override
	public CompletableFuture<APIReturn<Long, List<Player>>> getRankedLeaderboard(int playlistId, long messageID) {
		basicCheck();
		return CompletableFuture.supplyAsync(() -> {
			Future<JsonNode> response = queue.get(key, apiVersion, "/leaderboard/ranked", Query.create("playlist_id", String.valueOf(playlistId)));
			try {
				return new APIReturn<Long, List<Player>>(messageID, jsonNodeToObjectList(response.get(), Player::new));
			} catch (ExecutionException e) {
				throw new CompletionException(e.getCause());
			} catch (InterruptedException | IllegalArgumentException e) {
				throw new CompletionException(e);
			}
		});
	}
	
	@Override
	public CompletableFuture<APIReturn<Long, List<Player>>> getStatLeaderboard(Stat stat, long messageID) {
		if (stat == null) throw new IllegalArgumentException("Stat parameter is null.");
		basicCheck();
		return CompletableFuture.supplyAsync(() -> {
			CompletableFuture<JsonNode> response = queue.get(key, apiVersion, "/leaderboard/stat", Query.create("type", stat.getQueryName()));
			try {
				return new APIReturn<Long, List<Player>>(messageID, jsonNodeToObjectList(response.get(), Player::new));
			} catch (ExecutionException e) {
				throw new CompletionException(e.getCause());
			} catch (InterruptedException | IllegalArgumentException e) {
				throw new CompletionException(e);
			}
		});
	}
}
