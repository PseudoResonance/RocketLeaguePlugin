package com.github.pseudoresonance.resonantbot.gameutils;

import java.awt.Color;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashMap;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.HttpsURLConnection;

import com.github.pseudoresonance.resonantbot.Language;
import com.github.pseudoresonance.resonantbot.api.Command;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class MCCommand implements Command {
	
	private static LinkedHashMap<String, MCPlayer> players = new LinkedHashMap<String, MCPlayer>();

	public void onCommand(MessageReceivedEvent e, String command, String[] args) {
		if (args.length > 0) {
			String uuid = "";
			String name = "";
			MCPlayer player = getPlayer(args[0]);
			if (player == null) {
				try {
					URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + args[0]);
					HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
					try (InputStream in = connection.getInputStream()) {
						try (JsonReader jr = Json.createReader(in)) {
							JsonObject jo = jr.readObject();
							uuid = jo.getString("id");
							name = jo.getString("name");
						} catch (JsonException ex) {
							e.getChannel().sendMessage("Please specify a valid Minecraft account!").queue();
							return;
						}
					}
				} catch (Exception ex) {
					e.getChannel().sendMessage("Please specify a valid Minecraft account!").queue();
					ex.printStackTrace();
					return;
				}
			} else {
				sendMessage(e.getChannel(), player.getUUID(), player.getName());
			}
			if (!uuid.equals("") && !name.equals("")) {
				player = new MCPlayer(uuid, name);
				players.put(name.toLowerCase(), player);
				sendMessage(e.getChannel(), uuid, name);
			}
		} else {
			e.getChannel().sendMessage("Please specify a valid Minecraft account!").queue();
		}
	}

	private static void sendMessage(MessageChannel channel, String uuid, String name) {
		EmbedBuilder build = new EmbedBuilder();
		build.setTitle("Minecraft Account Details: " + Language.escape(name));
		build.setColor(new Color(0, 255, 0));
		build.setThumbnail("https://crafatar.com/renders/head/" + uuid + "?overlay");
		build.appendDescription("[UUID: " + uuid + "](https://crafatar.com/renders/body/" + uuid + "?overlay)");
		channel.sendMessage(build.build()).queue();
	}

	public String getDesc(long guildID) {
		return "Displays Minecraft character info";
	}

	public boolean isHidden() {
		return false;
	}
	
	public static MCPlayer getPlayer(String name) {
		purge();
		if (players.containsKey(name.toLowerCase())) {
			MCPlayer player = players.get(name.toLowerCase());
			if (player.isExpired()) {
				players.remove(name.toLowerCase());
			} else
				return player;
		}
		return null;
	}
	
	public static void purge() {
		for (String name : players.keySet()) {
			if (players.get(name).isExpired())
				players.remove(name);
			else
				break;
		}
	}
	
	public class MCPlayer {
		
		private final String uuid;
		private final String name;
		private final long creation;
		
		public MCPlayer(String uuid, String name) {
			this.uuid = uuid;
			this.name = name;
			this.creation = System.currentTimeMillis();
		}
		
		public String getUUID() {
			return this.uuid;
		}
		
		public String getName() {
			return this.name;
		}
		
		public boolean isExpired() {
			if (System.currentTimeMillis() - creation >= 3600000)
				return true;
			return false;
		}

	}

}
