/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2022 kr45732
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.skyblockplus.utils.database;

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.skyblockplus.features.jacob.JacobData;
import com.skyblockplus.features.jacob.JacobHandler;
import com.skyblockplus.features.listeners.AutomaticGuild;
import com.skyblockplus.features.party.Party;
import com.skyblockplus.price.AuctionTracker;
import com.skyblockplus.utils.oauth.TokenData;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.lang.reflect.Type;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheDatabase {

	private static final Logger log = LoggerFactory.getLogger(CacheDatabase.class);

	private final HikariDataSource dataSource;
	private final ConcurrentHashMap<String, Instant> uuidToTimeSkyblockProfiles = new ConcurrentHashMap<>();
	public final Map<String, List<Party>> partyCaches = new HashMap<>();

	public CacheDatabase() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(PLANET_SCALE_URL);
		dataSource = new HikariDataSource(config);
	}

	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	public void cacheJson(String uuid, JsonElement json) {
		executor.submit(() -> {
			try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
				Instant now = Instant.now();
				uuidToTimeSkyblockProfiles.put(uuid, now);

				statement.executeUpdate(
					"INSERT INTO profiles VALUES ('" +
					uuid +
					"', " +
					now.toEpochMilli() +
					", '" +
					json +
					"') ON DUPLICATE KEY UPDATE uuid = VALUES(uuid), time = VALUES(time), data = VALUES(data)"
				);
			} catch (Exception ignored) {}
		});
	}

	public JsonElement getCachedJson(String uuid) {
		Instant lastUpdated = uuidToTimeSkyblockProfiles.getOrDefault(uuid, null);
		if (lastUpdated != null && Duration.between(lastUpdated, Instant.now()).toMillis() > 90000) {
			deleteCachedJson(uuid);
		} else {
			try (
				Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM profiles where uuid = ?")
			) {
				statement.setString(1, uuid);
				try (ResultSet response = statement.executeQuery()) {
					if (response.next()) {
						Instant lastUpdatedResponse = Instant.ofEpochMilli(response.getLong("time"));
						if (Duration.between(lastUpdatedResponse, Instant.now()).toMillis() > 90000) {
							deleteCachedJson(uuid);
						} else {
							uuidToTimeSkyblockProfiles.put(uuid, lastUpdatedResponse);
							return JsonParser.parseString(response.getString("data"));
						}
					}
				}
			} catch (Exception ignored) {}
		}
		return null;
	}

	public void deleteCachedJson(String... uuids) {
		if (uuids.length == 0) {
			return;
		}

		executor.submit(() -> {
			StringBuilder query = new StringBuilder();
			for (String uuid : uuids) {
				uuidToTimeSkyblockProfiles.remove(uuid);
				query.append("'").append(uuid).append("',");
			}
			if (query.charAt(query.length() - 1) == ',') {
				query.deleteCharAt(query.length() - 1);
			}

			try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
				statement.executeUpdate("DELETE FROM profiles WHERE uuid IN (" + query + ")");
			} catch (Exception ignored) {}
		});
	}

	public void updateCache() {
		long now = Instant.now().minusSeconds(90).toEpochMilli();
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement("SELECT uuid FROM profiles WHERE time < ?")
		) {
			statement.setLong(1, now);
			try (ResultSet response = statement.executeQuery()) {
				List<String> expiredCacheUuidList = new ArrayList<>();
				while (response.next()) {
					expiredCacheUuidList.add(response.getString("uuid"));
				}
				deleteCachedJson(expiredCacheUuidList.toArray(new String[0]));
			}
		} catch (Exception ignored) {}

		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement("DELETE FROM profiles WHERE time < ?")
		) {
			statement.setLong(1, now);
			statement.executeUpdate();
		} catch (Exception ignored) {}
	}

	public void cachePartyData() {
		if (!isMainBot()) {
			return;
		}

		log.info("Caching Parties");
		long startTime = System.currentTimeMillis();
		for (Map.Entry<String, AutomaticGuild> automaticGuild : guildMap.entrySet()) {
			try {
				List<Party> partyList = automaticGuild.getValue().partyList;
				if (!partyList.isEmpty()) {
					String partySettingsJson = gson.toJson(partyList);
					try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
						statement.executeUpdate(
							"INSERT INTO party VALUES ('" +
							automaticGuild.getValue().guildId +
							"', '" +
							partySettingsJson +
							"') ON DUPLICATE KEY UPDATE guild_id = VALUES(guild_id), data = VALUES(data)"
						);
						log.info("Successfully cached PartyList | " + automaticGuild.getKey() + " | " + partyList.size());
					}
				}
			} catch (Exception e) {
				log.error(automaticGuild.getKey(), e);
			}
		}

		log.info("Cached parties in " + roundAndFormat((System.currentTimeMillis() - startTime) / 1000.0) + "s");
	}

	public void cacheCommandUsesData() {
		if (!isMainBot()) {
			return;
		}

		log.info("Caching Command Uses");
		long startTime = System.currentTimeMillis();
		String json = gson.toJson(getCommandUses());
		try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
			statement.executeUpdate("INSERT INTO commands VALUES (0, '" + json + "') ON DUPLICATE KEY UPDATE data = VALUES(data)");
			log.info("Cached command uses in " + roundAndFormat((System.currentTimeMillis() - startTime) / 1000.0) + "s");
		} catch (Exception e) {
			log.error("Failed to cache command uses in " + roundAndFormat((System.currentTimeMillis() - startTime) / 1000.0) + "s");
		}
	}

	public void cacheAhTrackerData() {
		if (!isMainBot()) {
			return;
		}

		log.info("Caching Auction Tracker");
		long startTime = System.currentTimeMillis();
		String json = gson.toJson(AuctionTracker.commandAuthorToTrackingUser);
		try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
			statement.executeUpdate("INSERT INTO ah_track VALUES (0, '" + json + "') ON DUPLICATE KEY UPDATE data = VALUES(data)");
			log.info(
				"Cached auction tracker in " +
				roundAndFormat((System.currentTimeMillis() - startTime) / 1000.0) +
				"s | " +
				AuctionTracker.commandAuthorToTrackingUser.size()
			);
		} catch (Exception e) {
			log.error("Failed to cache auction tracker in " + roundAndFormat((System.currentTimeMillis() - startTime) / 1000.0) + "s");
		}
	}

	public void cacheJacobData() {
		if (!isMainBot()) {
			return;
		}

		long startTime = System.currentTimeMillis();
		String json = gson.toJson(JacobHandler.getJacobData());
		try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
			statement.executeUpdate("INSERT INTO jacob VALUES (0, '" + json + "') ON DUPLICATE KEY UPDATE data = VALUES(data)");
			log.info("Cached jacob data in " + roundAndFormat((System.currentTimeMillis() - startTime) / 1000.0) + "s");
		} catch (Exception e) {
			log.error("Failed to cache jacob data in " + roundAndFormat((System.currentTimeMillis() - startTime) / 1000.0) + "s");
		}
	}

	public void cacheTokensData() {
		if (!isMainBot()) {
			return;
		}

		long startTime = System.currentTimeMillis();
		String json = gson.toJson(oAuthClient.getTokensMap());
		try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
			statement.executeUpdate("INSERT INTO tokens VALUES (0, '" + json + "') ON DUPLICATE KEY UPDATE data = VALUES(data)");
			log.info("Cached token data in " + roundAndFormat((System.currentTimeMillis() - startTime) / 1000.0) + "s");
		} catch (Exception e) {
			log.error("Failed to cache token data in " + roundAndFormat((System.currentTimeMillis() - startTime) / 1000.0) + "s");
		}
	}

	public void initializeAhTracker() {
		if (!isMainBot()) {
			return;
		}

		try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT * FROM ah_track")) {
			try (ResultSet response = statement.executeQuery()) {
				response.next();
				JsonObject data = JsonParser.parseString(response.getString("data")).getAsJsonObject();
				for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
					AuctionTracker.insertAhTrack(
						entry.getKey(),
						new UsernameUuidStruct(
							higherDepth(entry.getValue(), "username").getAsString(),
							higherDepth(entry.getValue(), "uuid").getAsString()
						)
					);
				}
				log.info("Retrieved auction tracker | " + data.size());
			}
		} catch (Exception e) {
			log.error("", e);
		}
	}

	public void initializeCommandUses() {
		if (!isMainBot()) {
			return;
		}

		try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT * FROM commands")) {
			try (ResultSet response = statement.executeQuery()) {
				response.next();
				Map<String, Integer> commandUsage = gson.fromJson(
					response.getString("data"),
					new TypeToken<Map<String, Integer>>() {}.getType()
				);
				slashCommandClient.setCommandUses(commandUsage);
				log.info("Retrieved command uses");
			}
		} catch (Exception e) {
			log.error("", e);
		}
	}

	public void initializeJacobData() {
		if (!isMainBot()) {
			return;
		}

		try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT * FROM jacob")) {
			try (ResultSet response = statement.executeQuery()) {
				response.next();
				JacobHandler.setJacobData(gson.fromJson(response.getString("data"), JacobData.class));
				log.info("Retrieved jacob data");
			}
		} catch (Exception e) {
			log.error("", e);
		}
	}

	public void initializeTokens() {
		if (!isMainBot()) {
			return;
		}

		try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT * FROM tokens")) {
			try (ResultSet response = statement.executeQuery()) {
				response.next();
				oAuthClient
					.getTokensMap()
					.putAll(gson.fromJson(response.getString("data"), new TypeToken<Map<String, TokenData>>() {}.getType()));
				log.info("Retrieved tokens data");
			}
		} catch (Exception e) {
			log.error("", e);
		}
	}

	public void initializeParties() {
		if (!isMainBot()) {
			return;
		}

		List<String> toDeleteIds = new ArrayList<>();
		try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT * FROM party")) {
			try (ResultSet response = statement.executeQuery()) {
				Type partyListType = new TypeToken<List<Party>>() {}.getType();
				while (response.next()) {
					String guildId = null;
					try {
						guildId = response.getString("guild_id");
						toDeleteIds.add(guildId);
						List<Party> partyList = gson.fromJson(response.getString("data"), partyListType);
						partyCaches.put(guildId, partyList);
						log.info("Retrieved party cache (" + partyList.size() + ") - guildId={" + guildId + "}");
					} catch (Exception e) {
						log.error("guildId={" + guildId + "}", e);
					}
				}
			}
		} catch (Exception ignored) {}

		try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
			statement.executeUpdate("DELETE FROM party WHERE guild_id IN (" + String.join(",", toDeleteIds) + ")");
		} catch (Exception ignored) {}
	}
}
