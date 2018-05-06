package com.ssplugins.rlstats;

import com.ssplugins.rlstats.entities.*;
import com.ssplugins.rlstats.util.PlayerRequest;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface RLStatsAPI {
	
	/**
	 * Sets the {@link Consumer} that will be called when an exception is thrown.
	 * Exceptions due to invalid parameters are thrown as normal and not sent here.
	 * @param exceptionHandler {@link Consumer} to handle exceptions.
	 */
	void setExceptionHandler(Consumer<Exception> exceptionHandler);
	
	/**
	 * Set the number of requests this API should assume your rlstats api key has. (Default 2)
	 * If this is set higher than the amount your account has, an exception may be thrown
	 * for too many requests being sent.
	 * @param i Requests per second.
	 */
	void setRequestsPerSecond(int i);
	
	/**
	 * Sets the auth key used for API requests.
	 * @param key The key to use for API requests.
	 */
	void setAuthKey(String key);
	
	/**
	 * Returns the auth key currently being used for API requests.
	 * @return The current auth key.
	 */
	String getAuthKey();
	
	/**
	 * Sets the API version to use for API requests.
	 * @param version The version to use for API requests.
	 */
	void setAPIVersion(String version);
	
	/**
	 * Returns the API version that is currently used.
	 * @return Current API version.
	 */
	String getAPIVersion();
	
	/**
	 * Requests the list of platforms from the API.
	 * Such as: Steam, Ps4, XboxOne
	 * @return CompletableFuture with a list of platforms known to the API.
	 */
	CompletableFuture<APIReturn<Long, List<PlatformInfo>>> getPlatforms(long messageID);
	
	/**
	 * Requests the list of tiers from the API.
	 * Such as: Gold, Platinum, Diamond
	 * @return CompletableFuture with list of tiers for the latest season.
	 */
	CompletableFuture<APIReturn<Long, List<Tier>>> getTiers(long messageID);
	
	/**
	 * Requests the list of tiers for a specific season.
	 * @param season Season to request tiers for.
	 * @return CompletableFuture with list of tiers for the specified season.
	 */
	CompletableFuture<APIReturn<Long, APIReturn<Integer, List<Tier>>>> getTiers(int season, long messageID);
	
	/**
	 * Requests the list of seasons from the API.
	 * @return CompletableFuture with list of seasons known to the API.
	 */
	CompletableFuture<APIReturn<Long, List<Season>>> getSeasons(long messageID);
	
	/**
	 * Requests playlist info from the API.
	 * Such as number of players in playlists.
	 * @return CompletableFuture with info for known playlists.
	 */
	CompletableFuture<APIReturn<Long, List<Playlist>>> getPlaylistInfo(long messageID);
	
	/**
	 * Requests info for a specific player.
	 * @param id Steam 64 ID, PSN Username, Xbox GamerTag, or XUID.
	 * @param platform platformId to search for.
	 * @see #getPlayer(String, Platform)
	 * @return CompletableFuture with info for the specified player, or null if the player could not be found.
	 */
	CompletableFuture<APIReturn<Long, Player>> getPlayer(String id, int platform, long messageID);
	
	/**
	 * Requests info for a specific player.
	 * @param id Steam 64 ID, PSN Username, Xbox GamerTag, or XUID.
	 * @param platform Which platform to search for.
	 * @return CompletableFuture with info for the specified player, or null if the player could not be found.
	 */
	CompletableFuture<APIReturn<Long, Player>> getPlayer(String id, Platform platform, long messageID);
	
	/**
	 * Request info for multiple players at once.
	 * If requesting players that are not in the RLS database,
	 * the connection may reach the socket timeout and return with nothing.
	 * If hitting the socket timeout, try increasing the value of {@link RLStats#SOCKET_TIMEOUT} (default: 60000L)
	 * @param collection Players to retrieve data for.
	 * @see PlayerRequest
	 * @return CompletableFuture with list of data for the specified players.
	 */
	CompletableFuture<APIReturn<Long, List<Player>>> getPlayers(Collection<PlayerRequest> collection, long messageID);
	
	/**
	 * Request info for multiple players at once.
	 * If requesting players that are not in the RLS database,
	 * the connection may reach the socket timeout and return with nothing.
	 * If hitting the socket timeout, try increasing the value of {@link RLStats#SOCKET_TIMEOUT} (default: 60000L)
	 * @param requests Players to retrieve data for.
	 * @see PlayerRequest
	 * @return CompletableFuture with list of data for the specified players.
	 */
	CompletableFuture<APIReturn<Long, List<Player>>> getPlayers(PlayerRequest[] requests, long messageID);
	
	/**
	 * Searches for players by display name.
	 * Only searches the RLS database, not every Rocket League player.
	 * @param displayName Display name to search for.
	 * @param page Which page of results to return.
	 * @return CompletableFuture with one page worth of results.
	 */
	CompletableFuture<APIReturn<Long, SearchResultPage>> searchPlayers(String displayName, int page, long messageID);
	
	/**
	 * Searches for players by display name.
	 * Only searches the RLS database, not every Rocket League player.
	 * @param displayName Display name to search for.
	 * @return CompletableFuture with the first page of results.
	 */
	CompletableFuture<APIReturn<Long, SearchResultPage>> searchPlayers(String displayName, long messageID);
	
	/**
	 * Requests list of 100 players sorted by latest season ranking.
	 * @param playlistId Which playlist to search in.
	 * @see Playlist
	 * @return 100 players sorted by latest season ranking.
	 */
	CompletableFuture<APIReturn<Long, List<Player>>> getRankedLeaderboard(int playlistId, long messageID);
	
	/**
	 * Requests list of 100 players sorted by specified stat amount.
	 * @param stat Which stat to search for.
	 * @return 100 players sorted by specified stat amount.
	 */
	CompletableFuture<APIReturn<Long, List<Player>>> getStatLeaderboard(Stat stat, long messageID);
	
}
