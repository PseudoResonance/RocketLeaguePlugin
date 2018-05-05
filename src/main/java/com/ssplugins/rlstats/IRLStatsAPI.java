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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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
	
	private RequestQueue queue = new RequestQueue(this);
	
	IRLStatsAPI() {}
	
	private void basicCheck() {
		if (key == null || key.isEmpty()) throw new IllegalStateException("No API key was provided.");
	}
	
	// Utility Methods
	
	private <T> T getFutureBlocking(CompletableFuture<T> future, Supplier<T> fallback) {
		try {
			return future.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return fallback.get();
	}
	
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
			} catch (InterruptedException | ExecutionException | IllegalArgumentException e) {
				exception(e);
			}
			return new APIReturn<Long, List<PlatformInfo>>(messageID, new ArrayList<>());
		});
	}
	
	@Override
	public APIReturn<Long, List<PlatformInfo>> getPlatformsBlocking(long messageID) {
		return getFutureBlocking(getPlatforms(messageID), () -> new APIReturn<Long, List<PlatformInfo>>(0L, null));
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
	
	private List<Tier> getTiers(Future<JsonNode> response) {
		try {
			return jsonNodeToObjectList(response.get(), Tier::new);
		} catch (InterruptedException | ExecutionException | IllegalArgumentException e) {
			exception(e);
		}
		return new ArrayList<>();
	}
	
	@Override
	public APIReturn<Long, List<Tier>> getTiersBlocking(long messageID) {
		return getFutureBlocking(getTiers(messageID), () -> new APIReturn<Long, List<Tier>>(0L, null));
	}
	
	@Override
	public APIReturn<Long, APIReturn<Integer, List<Tier>>> getTiersBlocking(int season, long messageID) {
		return getFutureBlocking(getTiers(season, messageID), () -> new APIReturn<Long, APIReturn<Integer, List<Tier>>>(0L, null));
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
			} catch (InterruptedException | ExecutionException | IllegalArgumentException e) {
				exception(e);
			}
			return new APIReturn<Long, List<Season>>(messageID, new ArrayList<>());
		});
	}
	
	@Override
	public APIReturn<Long, List<Season>> getSeasonsBlocking(long messageID) {
		return getFutureBlocking(getSeasons(messageID), () -> new APIReturn<Long, List<Season>>(0L, null));
	}
	
	@Override
	public CompletableFuture<APIReturn<Long, List<Playlist>>> getPlaylistInfo(long messageID) {
		basicCheck();
		return CompletableFuture.supplyAsync(() -> {
			CompletableFuture<JsonNode> response = queue.get(key, apiVersion, "/data/playlists", null);
			try {
				return new APIReturn<Long, List<Playlist>>(messageID, jsonNodeToObjectList(response.get(), Playlist::new));
			} catch (InterruptedException | ExecutionException | IllegalArgumentException e) {
				exception(e);
			}
			return new APIReturn<Long, List<Playlist>>(messageID, new ArrayList<>());
		});
	}
	
	@Override
	public APIReturn<Long, List<Playlist>> getPlaylistInfoBlocking(long messageID) {
		return getFutureBlocking(getPlaylistInfo(messageID), () -> new APIReturn<Long, List<Playlist>>(0L, null));
	}
	
	@Override
	public CompletableFuture<APIReturn<Long, Player>> getPlayer(String id, int platform, long messageID) {
		basicCheck();
		return CompletableFuture.supplyAsync(() -> {
			CompletableFuture<JsonNode> response = queue.get(key, apiVersion, "/player", Query.create("unique_id", id).add("platform_id", String.valueOf(platform)));
			try {
				JsonNode node = response.get();
				return new APIReturn<Long, Player>(messageID, new Player(node.getObject()));
			} catch (InterruptedException | ExecutionException e) {
				exception(e);
			}
			return null;
		});
	}
	
	@Override
	public CompletableFuture<APIReturn<Long, Player>> getPlayer(String id, Platform platform, long messageID) {
		return getPlayer(id, platform.getId(), messageID);
	}
	
	@Override
	public APIReturn<Long, Player> getPlayerBlocking(String id, int platform, long messageID) {
		return getFutureBlocking(getPlayer(id, platform, messageID), () -> null);
	}
	
	@Override
	public APIReturn<Long, Player> getPlayerBlocking(String id, Platform platform, long messageID) {
		return getPlayerBlocking(id, platform.getId(), messageID);
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
			} catch (InterruptedException | ExecutionException | IllegalArgumentException e) {
				exception(e);
			}
			return new APIReturn<Long, List<Player>>(messageID, new ArrayList<>());
		});
	}
	
	@Override
	public CompletableFuture<APIReturn<Long, List<Player>>> getPlayers(PlayerRequest[] requests, long messageID) {
		return getPlayers(Arrays.asList(requests), messageID);
	}
	
	@Override
	public APIReturn<Long, List<Player>> getPlayersBlocking(Collection<PlayerRequest> collection, long messageID) {
		return getFutureBlocking(getPlayers(collection, messageID), () -> new APIReturn<Long, List<Player>>(0L, null));
	}
	
	@Override
	public APIReturn<Long, List<Player>> getPlayersBlocking(PlayerRequest[] requests, long messageID) {
		return getFutureBlocking(getPlayers(requests, messageID), () -> new APIReturn<Long, List<Player>>(0L, null));
	}
	
	@Override
	public CompletableFuture<APIReturn<Long, SearchResultPage>> searchPlayers(String displayName, int page, long messageID) {
		basicCheck();
		return CompletableFuture.supplyAsync(() -> {
			Future<JsonNode> response = queue.get(key, apiVersion, "/search/players", Query.create("display_name", displayName).add("page", String.valueOf(page)));
			try {
				JsonNode node = response.get();
				return new APIReturn<Long, SearchResultPage>(messageID, new SearchResultPage(node.getObject()));
			} catch (InterruptedException | ExecutionException e) {
				exception(e);
			}
			return null;
		});
	}
	
	@Override
	public CompletableFuture<APIReturn<Long, SearchResultPage>> searchPlayers(String displayName, long messageID) {
		return searchPlayers(displayName, 0);
	}
	
	@Override
	public APIReturn<Long, SearchResultPage> searchPlayersBlocking(String displayName, int page, long messageID) {
		return getFutureBlocking(searchPlayers(displayName, page), () -> null);
	}
	
	@Override
	public APIReturn<Long, SearchResultPage> searchPlayersBlocking(String displayName, long messageID) {
		return getFutureBlocking(searchPlayers(displayName, messageID), () -> null);
	}
	
	@Override
	public CompletableFuture<APIReturn<Long, List<Player>>> getRankedLeaderboard(int playlistId, long messageID) {
		basicCheck();
		return CompletableFuture.supplyAsync(() -> {
			Future<JsonNode> response = queue.get(key, apiVersion, "/leaderboard/ranked", Query.create("playlist_id", String.valueOf(playlistId)));
			try {
				return new APIReturn<Long, List<Player>>(messageID, jsonNodeToObjectList(response.get(), Player::new));
			} catch (InterruptedException | ExecutionException | IllegalArgumentException e) {
				exception(e);
			}
			return new APIReturn<Long, List<Player>>(messageID, new ArrayList<>());
		});
	}
	
	@Override
	public APIReturn<Long, List<Player>> getRankedLeaderboardBlocking(int playlistId, long messageID) {
		return getFutureBlocking(getRankedLeaderboard(playlistId, messageID), () -> new APIReturn<Long, List<Player>>(0L, null));
	}
	
	@Override
	public CompletableFuture<APIReturn<Long, List<Player>>> getStatLeaderboard(Stat stat, long messageID) {
		if (stat == null) throw new IllegalArgumentException("Stat parameter is null.");
		basicCheck();
		return CompletableFuture.supplyAsync(() -> {
			CompletableFuture<JsonNode> response = queue.get(key, apiVersion, "/leaderboard/stat", Query.create("type", stat.getQueryName()));
			try {
				return new APIReturn<Long, List<Player>>(messageID, jsonNodeToObjectList(response.get(), Player::new));
			} catch (InterruptedException | ExecutionException | IllegalArgumentException e) {
				exception(e);
			}
			return new APIReturn<Long, List<Player>>(messageID, new ArrayList<>());
		});
	}
	
	@Override
	public APIReturn<Long, List<Player>> getStatLeaderboardBlocking(Stat stat, long messageID) {
		return getFutureBlocking(getStatLeaderboard(stat, messageID), () -> new APIReturn<Long, List<Player>>(0L, null));
	}
}
