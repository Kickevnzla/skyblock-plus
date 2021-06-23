package com.skyblockplus.utils.structs;

import com.skyblockplus.utils.Player;
import java.time.Instant;
import java.util.List;

public class HypixelGuildCache {

	public final Instant lastUpdated;
	public final List<String> membersCache;

	public HypixelGuildCache(Instant lastUpdated, List<String> membersCache) {
		this.lastUpdated = lastUpdated;
		this.membersCache = membersCache;
	}

	public static String memberCacheFromPlayer(Player player) {
		return (
			player.getUsername() +
			"=:=" +
			player.getHighestAmount("slayer") +
			"=:=" +
			player.getHighestAmount("skills") +
			"=:=" +
			player.getHighestAmount("catacombs") +
			"=:=" +
			player.getHighestAmount("weight") +
			"=:=" +
			player.getHighestAmount("svenXp") +
			"=:=" +
			player.getHighestAmount("revXp") +
			"=:=" +
			player.getHighestAmount("taraXp") +
			"=:=" +
			player.getHighestAmount("endermanXp")
		);
	}
	// [0] - username
	// [1] - slayer
	// [2] - skills
	// [3] - catacombs
	// [4] - weight
	// [5] - sven XP
	// [6] - rev XP
	// [7] - tara XP
	// [8] - enderman XP
}
