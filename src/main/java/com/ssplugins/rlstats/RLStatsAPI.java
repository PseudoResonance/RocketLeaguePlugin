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
	 * Same as {@link #getPlatforms()} but blocks thread until result is returned.
	 * @return A list of platforms known to the API.
	 */
	APIReturn<Long, List<PlatformInfo>> getPlatformsBlocking(long messageID);
	
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
	 * Same as {@link #getTiers()} but blocks thread until result is returned.
	 * @return List of tiers for the latest season.
	 */
	APIReturn<Long, List<Tier>> getTiersBlocking(long messageID);
	
	/**
	 * Same as {@link #getTiers(int)} but blocks thread until result is returned.
	 * @param season Season to request tiers for.
	 * @return List of tiers for the specified season.
	 */
	APIReturn<Long, APIReturn<Integer, List<Tier>>> getTiersBlocking(int season, long messageID);
	
	/**
	 * Requests the list of seasons from the API.
	 * @return CompletableFuture with list of seasons known to the API.
	 */
	CompletableFuture<APIReturn<Long, List<Season>>> getSeasons(long messageID);
	
	/**
	 * Same as {@link #getSeasons()} but blocks thread until result is returned.
	 * @return List of seasons known to the API.
	 */
	APIReturn<Long, List<Season>> getSeasonsBlocking(long messageID);
	
	/**
	 * Requests playlist info from the API.
	 * Such as number of players in playlists.
	 * @return CompletableFuture with info for known playlists.
	 */
	CompletableFuture<APIReturn<Long, List<Playlist>>> getPlaylistInfo(long messageID);
	
	/**
	 * Same as {@link #getPlaylistInfo()} but blocks thread until result is returned.
	 * @return Info for known playlists.
	 */
	APIReturn<Long, List<Playlist>> getPlaylistInfoBlocking(long messageID);
	
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
	 * Same as {@link #getPlayer(String, int)} but blocks thread until result is returned.
	 * @param id Steam 64 ID, PSN Username, Xbox GamerTag, or XUID.
	 * @param platform platformId to search for.
	 * @see #getPlayerBlocking(String, Platform)
	 * @return Info for the specified player, or null if the player could not be found.
	 */
	APIReturn<Long, Player> getPlayerBlocking(String id, int platform, long messageID);
	
	/**
	 * Same as {@link #getPlayer(String, Platform)} but blocks thread until result is returned.
	 * @param id Steam 64 ID, PSN Username, Xbox GamerTag, or XUID.
	 * @param platform Which platform to search for.
	 * @return Info for the specified player, or null if the player could not be found.
	 */
	APIReturn<Long, Player> getPlayerBlocking(String id, Platform platform, long messageID);
	
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
	 * Same as {@link #getPlayers(Collection)} but blocks thread until result is returned.
	 * @param collection Players to retrieve data for.
	 * @return List of data for the specified players.
	 */
	APIReturn<Long, List<Player>> getPlayersBlocking(Collection<PlayerRequest> collection, long messageID);
	
	/**
	 * Same as {@link #getPlayers(PlayerRequest[])} but blocks thread until result is returned.
	 * @param requests Players to retrieve data for.
	 * @return List of data for the specified players.
	 */
	APIReturn<Long, List<Player>> getPlayersBlocking(PlayerRequest[] requests, long messageID);
	
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
	 * Same as {@link #searchPlayers(String, int)} but blocks thread until result is returned.
	 * @param displayName Display name to search for.
	 * @param page Which page of results to return.
	 * @return One page worth of results.
	 */
	APIReturn<Long, SearchResultPage> searchPlayersBlocking(String displayName, int page, long messageID);
	
	/**
	 * Same as {@link #searchPlayers(String)} but blocks thread until result is returned.
	 * @param displayName Display name to search for.
	 * @return First page of results.
	 */
	APIReturn<Long, SearchResultPage> searchPlayersBlocking(String displayName, long messageID);
	
	/**
	 * Requests list of 100 players sorted by latest season ranking.
	 * @param playlistId Which playlist to search in.
	 * @see Playlist
	 * @return 100 players sorted by latest season ranking.
	 */
	CompletableFuture<APIReturn<Long, List<Player>>> getRankedLeaderboard(int playlistId, long messageID);
	
	/**
	 * Same as {@link #getRankedLeaderboard(int)} but blocks thread until result is returned.
	 * @param playlistId Which playlist to search in.
	 * @return 100 players sorted by latest season ranking.
	 */
	APIReturn<Long, List<Player>> getRankedLeaderboardBlocking(int playlistId, long messageID);
	
	/**
	 * Requests list of 100 players sorted by specified stat amount.
	 * @param stat Which stat to search for.
	 * @return 100 players sorted by specified stat amount.
	 */
	CompletableFuture<APIReturn<Long, List<Player>>> getStatLeaderboard(Stat stat, long messageID);
	
	/**
	 * Same as {@link #getStatLeaderboard(Stat)} but blocks thread until result is returned.
	 * @param stat Which stat to search for.
	 * @return 100 players sorted by specified stat amount.
	 */
	APIReturn<Long, List<Player>> getStatLeaderboardBlocking(Stat stat, long messageID);
	
}
