package com.github.pseudoresonance.resonantbot.gameutils;

import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashMap;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.HttpsURLConnection;

import com.github.pseudoresonance.resonantbot.Config;

public class SteamAPI {
	
	private static LinkedHashMap<String, SteamUser> usernames = new LinkedHashMap<String, SteamUser>();
	
	private static String token = "";
	
	public static String getToken() {
		if (token.equals("")) {
			Object t = Config.get("steamtoken");
			if (t != null)
				token = (String) t;
		}
		return token;
	}
	
	public static void updateToken() {
		if (token.equals("")) {
			Object t = Config.get("steamtoken");
			if (t != null)
				token = (String) t;
		} else {
			Object t = Config.get("steamtoken");
			if (t != null) {
				String tok = (String) t;
				if (!tok.equals("")) {
					token = tok;
				}
			}
		}
	}
	
	public static void updateToken(String token) {
		if (!token.equals("")) {
			SteamAPI.token = token;
		}
	}
	
	public static Long getSteamID(String name) {
		if (token.equals("")) {
			updateToken();
		}
		Long id = 0L;
		if (!usernames.containsKey(name) || usernames.get(name).isExpired()) {
			try {
				URL url = new URL("https://api.steampowered.com/ISteamUser/ResolveVanityURL/v0001/?key=" + token + "&vanityurl=" + name);
				HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
				connection.setRequestMethod("GET");
				connection.connect();
				int code = connection.getResponseCode();
				if (code == 403) {
					throw new IllegalStateException("Please add a Steam API token!");
				}
				try (InputStream in = connection.getInputStream()) {
					try (JsonReader jr = Json.createReader(in)) {
						JsonObject jo = jr.readObject();
						JsonObject response = jo.getJsonObject("response");
						id = Long.valueOf(response.getString("steamid"));
						SteamUser user = new SteamUser(name, id);
						usernames.put(name, user);
					} catch (JsonException | NullPointerException ex) {
						return 0L;
					}
				}
			} catch (Exception ex) {
				if (ex instanceof IllegalStateException && ex.getMessage().equals("Please add a Steam API token!"))
					throw new IllegalStateException("Error with Steam API! Please try again!", ex);
				ex.printStackTrace();
				throw new IllegalStateException("Error with Steam API! Please try again!");
			}
		} else {
			id = usernames.get(name).getId();
		}
		return id;
	}
	
	public static void purge() {
		for (String name : usernames.keySet()) {
			if (usernames.get(name).isExpired())
				usernames.remove(name);
			else
				break;
		}
	}
	
	public static class SteamUser {
		
		private final String username;
		private final Long id;
		private final long creation;
		
		public SteamUser(String username, Long id) {
			this.username = username;
			this.id = id;
			this.creation = System.currentTimeMillis();
		}
		
		public String getUsername() {
			return this.username;
		}
		
		public Long getId() {
			return this.id;
		}
		
		public boolean isExpired() {
			if (System.currentTimeMillis() - creation >= 3600000)
				return true;
			return false;
		}

	}

}
