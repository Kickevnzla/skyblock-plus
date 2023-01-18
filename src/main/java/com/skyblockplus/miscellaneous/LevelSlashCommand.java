/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021 kr45732
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

package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.command.SelectMenuPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.SkillsStruct;
import java.util.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.apache.groovy.util.Maps;
import org.springframework.stereotype.Component;

@Component
public class LevelSlashCommand extends SlashCommand {

	public LevelSlashCommand() {
		this.name = "level";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(getLevel(event.player, event.getOptionStr("profile"), event));
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Get a player's Skyblock level")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOption(OptionType.STRING, "profile", "Profile name");
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static EmbedBuilder getLevel(String username, String profileName, SlashCommandEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (!player.isValid()) {
			return player.getFailEmbed();
		}

		LevelRecord coreTasks = getCoreTasksEmbed(player);
		LevelRecord eventTasks = getEventTasks(player);
		LevelRecord dungeonTasks = getDungeonTasks(player);
		LevelRecord essenceShopTasks = getEssenceShopTasks(player);
		LevelRecord slayingTasks = getSlayingTasks(player);
		LevelRecord skillRelatedTasks = getSkillRelatedTasks(player);
		LevelRecord miscellaneousTasks = getMiscellaneousTasks(player);
		LevelRecord storyTasks = getStoryTasks(player);

		EmbedBuilder eb = player.defaultPlayerEmbed();
		eb.setDescription(
			"**Level:** " +
			roundAndFormat(player.getLevel()) +
			"\n**Level Color:** ?" +
			"\n\nCore Tasks: " +
			coreTasks.total() +
			"\nEvent Tasks: " +
			eventTasks.total() +
			"\nDungeon Tasks: " +
			dungeonTasks.total() +
			"\nEssence Shop Tasks: " +
			essenceShopTasks.total() +
			"\nSlaying Tasks: " +
			slayingTasks.total() +
			"\nSkill Related Tasks: " +
			skillRelatedTasks.total() +
			"\nMiscellaneous Tasks: " +
			miscellaneousTasks.total() +
			"\nStory Tasks: " +
			storyTasks.total()
		);

		Map<SelectOption, EmbedBuilder> pages = new LinkedHashMap<>();
		pages.put(SelectOption.of("Overview", "overview"), eb);
		pages.put(SelectOption.of("Core Tasks", "core_tasks"), coreTasks.eb());
		pages.put(SelectOption.of("Event Tasks", "event_tasks"), eventTasks.eb());
		pages.put(SelectOption.of("Dungeon Tasks", "dungeon_tasks"), dungeonTasks.eb());
		pages.put(SelectOption.of("Essence Shop Tasks", "essence_shop_tasks"), essenceShopTasks.eb());
		pages.put(SelectOption.of("Slaying Tasks", "slaying_tasks"), slayingTasks.eb());
		pages.put(SelectOption.of("Skill Related Tasks", "skill_related_tasks"), skillRelatedTasks.eb());
		pages.put(SelectOption.of("Miscellaneous Tasks", "miscellaneous_tasks"), miscellaneousTasks.eb());
		pages.put(SelectOption.of("Story Tasks", "story_tasks"), storyTasks.eb());

		new SelectMenuPaginator("overview", new PaginatorExtras().setSelectPages(pages), event);

		return null;
	}

	private static LevelRecord getCoreTasksEmbed(Player player) {
		EmbedBuilder eb = player.defaultPlayerEmbed(" | Core Tasks");

		// Skills
		int skillsSbXp = 0;
		for (String skill : SKILL_NAMES) {
			SkillsStruct skillsStruct = player.getSkill(skill);
			if (skillsStruct != null) {
				for (int i = 1; i <= skillsStruct.currentLevel(); i++) {
					if (i <= 10) {
						skillsSbXp += 5;
					} else if (i <= 25) {
						skillsSbXp += 10;
					} else if (i <= 50) {
						skillsSbXp += 20;
					} else {
						skillsSbXp += 30;
					}
				}
			}
		}
		eb.appendDescription("\nSkill Level Up: " + formatNumber(skillsSbXp) + " / 7,500");

		// Museum
		eb.appendDescription("\nMuseum Progression: ? / 2,700");

		// Fairy souls
		int fairySoulSbXp = player.getFairySouls() / 5 * 10;
		eb.appendDescription("\nFairy Souls: " + formatNumber(fairySoulSbXp) + " / 470");

		// Accessories
		int magicPowerSbXp = player.getMagicPower();
		eb.appendDescription("\nAccessory Bag: " + formatNumber(magicPowerSbXp));

		// Pets
		int petScoreSbXp = player.getPetScore() * 3;
		eb.appendDescription("\nPet Score: " + formatNumber(petScoreSbXp));

		// Collections
		Map<String, Long> collections = new HashMap<>();
		for (Map.Entry<String, JsonElement> member : higherDepth(player.getOuterProfileJson(), "members").getAsJsonObject().entrySet()) {
			try {
				for (Map.Entry<String, JsonElement> collection : higherDepth(member.getValue(), "collection")
					.getAsJsonObject()
					.entrySet()) {
					collections.compute(collection.getKey(), (k, v) -> (v == null ? 0 : v) + collection.getValue().getAsLong());
				}
			} catch (Exception ignored) {}
		}
		int collectionsSbXp = 0;
		for (Map.Entry<String, Long> collection : collections.entrySet()) {
			JsonElement tiers = higherDepth(getCollectionsJson(), collection.getKey() + ".tiers");
			if (tiers != null) {
				for (JsonElement amtRequired : tiers.getAsJsonArray()) {
					if (collection.getValue() >= amtRequired.getAsLong()) {
						collectionsSbXp += 4;
					} else {
						break;
					}
				}
			}
		}
		eb.appendDescription("\nCollections: " + formatNumber(collectionsSbXp) + " / 2,452");

		// Minions
		Set<String> uniqueCraftedMinions = new HashSet<>();
		for (Map.Entry<String, JsonElement> member : higherDepth(player.getOuterProfileJson(), "members").getAsJsonObject().entrySet()) {
			try {
				for (JsonElement minion : higherDepth(member.getValue(), "crafted_generators").getAsJsonArray()) {
					uniqueCraftedMinions.add(minion.getAsString());
				}
			} catch (Exception ignored) {}
		}
		int minionsSbXp = 0;
		for (String uniqueCraftedMinion : uniqueCraftedMinions) {
			int idx = uniqueCraftedMinion.lastIndexOf("_");
			minionsSbXp += higherDepth(getMiscJson(), "minionXp." + uniqueCraftedMinion.substring(idx + 1)).getAsInt();
		}
		eb.appendDescription("\nCraft Minions: " + formatNumber(minionsSbXp) + " / 2,801");

		// Bank upgrades
		eb.appendDescription("\nBank Upgrades: ? / 200");

		// Core tasks total
		String totalSbXp =
			formatNumber(skillsSbXp + fairySoulSbXp + magicPowerSbXp + petScoreSbXp + collectionsSbXp + minionsSbXp) + " / 15,430";
		eb.getDescriptionBuilder().insert(0, "Core Tasks: " + totalSbXp + "\n");

		return new LevelRecord(eb, totalSbXp);
	}

	private static LevelRecord getEventTasks(Player player) {
		EmbedBuilder eb = player.defaultPlayerEmbed(" | Event Tasks");

		// Mining fiesta
		eb.appendDescription("\nMining Fiesta: ? / 200");

		// Fishing festival
		eb.appendDescription("\nFishing Festival: ? / 100");

		// Spooky festival
		eb.appendDescription("\nSpooky Festival: ? / 225");

		// Event tasks total
		String totalSbXp = "? / 525";
		eb.getDescriptionBuilder().insert(0, "Event Tasks: " + totalSbXp + "\n");

		return new LevelRecord(eb, totalSbXp);
	}

	private static LevelRecord getDungeonTasks(Player player) {
		EmbedBuilder eb = player.defaultPlayerEmbed(" | Dungeon Tasks");

		// Catacombs
		int catacombsSbXp = 0;
		SkillsStruct cataSkill = player.getCatacombs();
		if (cataSkill != null) {
			for (int i = 1; i <= cataSkill.currentLevel(); i++) {
				if (i < 40) {
					catacombsSbXp += 20;
				} else if (i <= 50) {
					catacombsSbXp += 40;
				}
			}
		}
		eb.appendDescription("\nCatacombs Level Up: " + formatNumber(catacombsSbXp) + " / 1220");

		// Classes
		int classSbXp = 0;
		for (String className : DUNGEON_CLASS_NAMES) {
			SkillsStruct classInfo = player.getDungeonClass(className);
			if (classInfo != null) {
				classSbXp += Math.min(classInfo.currentLevel(), 50) * 4;
			}
		}
		eb.appendDescription("\nClass Level Up: " + formatNumber(classSbXp) + " / 1000");

		// Regular floor completions
		int floorCompletionSbXp = 0;
		JsonElement cataTierCompletions = higherDepth(player.profileJson(), "dungeons.dungeon_types.catacombs.tier_completions");
		if (cataTierCompletions != null) {
			for (Map.Entry<String, JsonElement> completion : cataTierCompletions.getAsJsonObject().entrySet()) {
				if (completion.getValue().getAsInt() > 0) {
					if (Integer.parseInt(completion.getKey()) <= 4) {
						floorCompletionSbXp += 20;
					} else {
						floorCompletionSbXp += 30;
					}
				}
			}
		}
		eb.appendDescription("\nComplete The Catacombs: " + formatNumber(floorCompletionSbXp) + " / 190");

		// Master flor completions
		int masterFloorCompletionSbXp = 0;
		JsonElement masterTierCompletions = higherDepth(player.profileJson(), "dungeons.dungeon_types.master_catacombs.tier_completions");
		if (masterTierCompletions != null) {
			for (Map.Entry<String, JsonElement> completion : masterTierCompletions.getAsJsonObject().entrySet()) {
				if (completion.getValue().getAsInt() > 0) {
					masterFloorCompletionSbXp += 50;
				}
			}
		}
		eb.appendDescription("\nComplete The Catacombs Master Mode: " + formatNumber(masterFloorCompletionSbXp) + " / 350");

		// Dungeon tasks total
		String totalSbXp = formatNumber(catacombsSbXp + classSbXp + floorCompletionSbXp + masterFloorCompletionSbXp) + " / 2,760";
		eb.getDescriptionBuilder().insert(0, "Dungeon Tasks: " + totalSbXp + "\n");

		return new LevelRecord(eb, totalSbXp);
	}

	private static LevelRecord getEssenceShopTasks(Player player) {
		EmbedBuilder eb = player.defaultPlayerEmbed(" | Essence Shop Tasks");

		// Essence shop tasks total
		String totalSbXp = formatNumber(0) + " / 856";
		eb.getDescriptionBuilder().insert(0, "Essence Shop Tasks: " + totalSbXp + "\n");

		return new LevelRecord(eb, totalSbXp);
	}

	private static LevelRecord getSlayingTasks(Player player) {
		EmbedBuilder eb = player.defaultPlayerEmbed(" | Slaying Tasks");

		// Slaying tasks total
		String totalSbXp = formatNumber(0) + " / 6,125";
		eb.getDescriptionBuilder().insert(0, "Slaying Tasks: " + totalSbXp + "\n");

		return new LevelRecord(eb, totalSbXp);
	}

	private static LevelRecord getSkillRelatedTasks(Player player) {
		EmbedBuilder eb = player.defaultPlayerEmbed(" | Skill Related Tasks");

		// Mining
		String miningStr = "";

		// Hotm
		int hotmSbXp = 0;
		SkillsStruct hotmSkill = player.getHOTM();
		if (hotmSkill != null) {
			for (int i = 1; i <= hotmSkill.currentLevel(); i++) {
				hotmSbXp +=
					switch (i) {
						case 1 -> 35;
						case 2 -> 45;
						case 3 -> 60;
						case 4 -> 75;
						case 5 -> 90;
						case 6 -> 110;
						default -> 130; // 7
					};
			}
		}
		miningStr += "\nHeart Of The Mountain: " + formatNumber(hotmSbXp) + " / 545";

		// Powder
		int powderSbXp = 0;

		long mithrilPower = higherDepth(player.profileJson(), "mining_core.powder_spent_mithril", 0L);
		powderSbXp += Math.min(mithrilPower, 350000) / 2400;
		powderSbXp +=
			mithrilPower <= 350000 ? 0 : 3.75 * (Math.sqrt(1 + 8 * (Math.sqrt((1758267 / 12500000D) * (mithrilPower - 350000) + 9))) - 3);

		long gemstonePowder = higherDepth(player.profileJson(), "mining_core.powder_spent_gemstone", 0L);
		powderSbXp += Math.min(gemstonePowder, 350000) / 2500;
		powderSbXp +=
			gemstonePowder <= 350000
				? 0
				: 4.25 * (Math.sqrt(1 + 8 * (Math.sqrt((1758267 / 20000000D) * (gemstonePowder - 350000) + 9))) - 3);

		miningStr += "\nPowder: " + formatNumber(powderSbXp) + " / 1,080";

		// Commissions
		int commissionsSbXp = 0;
		int[] commissionMilestoneXpArray = { 20, 30, 30, 50, 50, 75 };
		JsonElement tutorialArray = higherDepth(player.profileJson(), "tutorial");
		if (tutorialArray != null) {
			for (JsonElement tutorial : tutorialArray.getAsJsonArray()) {
				if (
					tutorial.getAsJsonPrimitive().isString() &&
					tutorial.getAsString().startsWith("commission_milestone_reward_skyblock_xp_tier")
				) for (int i = 1; i <= commissionMilestoneXpArray.length; i++) {
					if (tutorial.getAsString().equals("commission_milestone_reward_skyblock_xp_tier_" + i)) {
						commissionsSbXp += commissionMilestoneXpArray[i - 1];
					}
				}
			}
		}
		miningStr += "\nCommission Milestones: " + formatNumber(commissionsSbXp) + " / 255";

		// Peak of the mountain
		int peakOfTheMountainSbXp = 0;
		int potmLevel = higherDepth(player.profileJson(), "mining_core.nodes.special_0", 0);
		for (int i = 1; i <= potmLevel; i++) {
			peakOfTheMountainSbXp +=
				switch (i) {
					case 1 -> 25;
					case 2 -> 35;
					case 3 -> 50;
					case 4 -> 65;
					case 5 -> 75;
					case 6 -> 100;
					default -> 125; // 7
				};
		}
		miningStr += "\nPeak Of The Mountain: " + formatNumber(peakOfTheMountainSbXp) + " / 475";

		// Rock pet milestones
		int rockPetSbXp = 0;

		int rockPetMilestone = higherDepth(player.profileJson(), "stats.pet_milestone_ores_mined", 0);
		int[] rockMilestonesRequired = { 2500, 7500, 20000, 100000, 250000 };
		for (int milestone : rockMilestonesRequired) {
			if (rockPetMilestone >= milestone) {
				rockPetSbXp += 20;
			}
		}
		miningStr += "\nRock Milestones: " + formatNumber(rockPetSbXp) + " / 100";

		int miningTotalSbXp = hotmSbXp + powderSbXp + commissionsSbXp + peakOfTheMountainSbXp + rockPetSbXp;
		eb.addField("Mining | " + formatNumber(miningTotalSbXp) + " / 2,655", miningStr, false);

		// Farming
		String farmingStr = "";

		// Anita shop upgrades
		int doubleDrops = higherDepth(player.profileJson(), "jacob2.perks.double_drops", 0);
		int farmingLevelCap = player.getFarmingCapUpgrade();
		int anitaShopUpgradeSbXp = (doubleDrops + farmingLevelCap) * 10;
		farmingStr += "\nAnita's Shop Upgrades: " + formatNumber(anitaShopUpgradeSbXp) + " / 250";

		eb.addField("Farming | " + formatNumber(anitaShopUpgradeSbXp), farmingStr, false);

		// Fishing
		String fishingStr = "";

		// Trophy fishing
		int trophyFishingSbXp = 0;
		if (higherDepth(player.profileJson(), "trophy_fish") != null) {
			JsonObject trophyFish = higherDepth(player.profileJson(), "trophy_fish").getAsJsonObject();
			for (Map.Entry<String, JsonElement> tropyFishEntry : trophyFish.entrySet()) {
				String key = tropyFishEntry.getKey();
				if (tropyFishEntry.getValue().isJsonPrimitive()) {
					if (key.endsWith("_bronze")) {
						trophyFishingSbXp += 4;
					} else if (key.endsWith("_silver")) {
						trophyFishingSbXp += 8;
					} else if (key.endsWith("_gold")) {
						trophyFishingSbXp += 16;
					} else if (key.endsWith("_diamond")) {
						trophyFishingSbXp += 32;
					}
				}
			}
		}
		fishingStr += "\nTrophy Fish: " + formatNumber(trophyFishingSbXp) + " / 1,080";

		// Dolphin pet milestones
		int dolphinPetSbXp = 0;
		int dolphinMilestoneXp = higherDepth(player.profileJson(), "stats.pet_milestone_sea_creatures_killed", 0);
		int[] dolphinMilestoneRequired = { 250, 1000, 2500, 5000, 10000 };
		for (int milestone : dolphinMilestoneRequired) {
			if (dolphinMilestoneXp >= milestone) {
				dolphinPetSbXp += dolphinMilestoneXp;
			}
		}
		fishingStr += "\nDolphin Milestones: " + formatNumber(dolphinMilestoneXp) + " / 1,080";

		int fishingTotalSbXp = trophyFishingSbXp + dolphinPetSbXp;
		eb.addField("Fishing | " + formatNumber(fishingTotalSbXp), fishingStr, false);

		// Total xp
		String totalSbXp = formatNumber(miningTotalSbXp + anitaShopUpgradeSbXp + fishingTotalSbXp) + " / 1,180";
		eb.getDescriptionBuilder().insert(0, "Skill Related Tasks: " + totalSbXp + "\n");

		return new LevelRecord(eb, totalSbXp);
	}

	private static LevelRecord getMiscellaneousTasks(Player player) {
		EmbedBuilder eb = player.defaultPlayerEmbed(" | Miscellaneous Tasks");

		// Accessory bag upgrade count
		int accessoryBagUpgradeSbXp = higherDepth(player.profileJson(), "accessory_bag_storage.bag_upgrades_purchased", 0) * 2;
		eb.appendDescription("\nAccessory Bag Upgrades: " + formatNumber(accessoryBagUpgradeSbXp) + " / 396");

		// Reaper peppers
		int reaperPepperSbXp = higherDepth(player.profileJson(), "reaper_peppers_eaten", 0) * 10;
		eb.appendDescription("\nReaper Peppers: " + formatNumber(reaperPepperSbXp) + " / 50");

		// Unlocking accessory bag powers
		int unlockingPowersSbXp = 0;
		JsonElement unlockedPowers = higherDepth(player.profileJson(), "accessory_bag_storage.unlocked_powers");
		if (unlockedPowers != null) {
			unlockingPowersSbXp = unlockedPowers.getAsJsonArray().size() * 15;
		}
		eb.appendDescription("\nUnlocking Powers: " + formatNumber(unlockingPowersSbXp) + " / 255");

		// Dojo
		int dojoSbXp = 0;
		JsonElement dojoScores = higherDepth(player.profileJson(), "nether_island_player_data.dojo");
		if (dojoScores != null) {
			int dojoPoints = player.getDojoPoints();

			if (dojoPoints >= 7000) {
				dojoSbXp += 150;
			}
			if (dojoPoints >= 6000) {
				dojoSbXp += 100;
			}
			if (dojoPoints >= 4000) {
				dojoSbXp += 75;
			}
			if (dojoPoints >= 2000) {
				dojoSbXp += 50;
			}
			if (dojoPoints >= 1000) {
				dojoSbXp += 30;
			}
			dojoSbXp += 20;
		}
		eb.appendDescription("\nThe Dojo: " + formatNumber(dojoSbXp) + " / 425");

		// Harp
		int harpSbXp = 0;
		Map<String, Integer> harpSongToSbXp = Maps.of(
			"song_hymn_joy_perfect_completions",
			4,
			"song_frere_jacques_perfect_completions",
			4,
			"song_amazing_grace_perfect_completions",
			4,
			"song_brahms_perfect_completions",
			8,
			"song_happy_birthday_perfect_completions",
			8,
			"song_greensleeves_perfect_completions",
			8,
			"song_jeopardy_perfect_completions",
			16,
			"song_minuet_perfect_completions",
			16,
			"song_joy_world_perfect_completions",
			16,
			"song_pure_imagination_perfect_completions",
			28,
			"song_vie_en_rose_perfect_completions",
			28,
			"song_fire_and_flames_perfect_completions",
			48,
			"song_pachelbel_perfect_completions",
			48
		);
		JsonElement harpQuests = higherDepth(player.profileJson(), "harp_quest");
		if (harpQuests != null) {
			for (Map.Entry<String, Integer> harpSong : harpSongToSbXp.entrySet()) {
				if (harpQuests.getAsJsonObject().has(harpSong.getKey())) {
					harpSbXp += harpSong.getValue();
				}
			}
		}
		eb.appendDescription("\nHarp Songs: " + formatNumber(harpSbXp) + " / 236");

		// Abiphone
		int abiphoneSbXp = 0;
		JsonElement abiphoneContacts = higherDepth(player.profileJson(), "nether_island_player_data.abiphone.active_contacts");
		if (abiphoneContacts != null) {
			abiphoneSbXp = abiphoneContacts.getAsJsonArray().size() * 10;
		}
		eb.appendDescription("\nAbiphone Contacts: " + formatNumber(abiphoneSbXp) + " / 410");

		// Community shop
		int communityShopSbXp = 0;
		Map<String, Integer> communityShopUpgradesMax = Maps.of(
			"island_size",
			1,
			"minion_slots",
			5,
			"guests_count",
			1,
			"coins_allowance",
			5
		);
		JsonElement communityUpgrades = higherDepth(player.getOuterProfileJson(), "community_upgrades.upgrade_states");
		if (communityUpgrades != null) {
			for (JsonElement upgradeState : communityUpgrades.getAsJsonArray()) {
				if (upgradeState.isJsonObject()) {
					JsonObject value = upgradeState.getAsJsonObject();
					String upgrade = value.get("upgrade").getAsString();
					int tier = value.get("tier").getAsInt();
					if (communityShopUpgradesMax.containsKey(upgrade)) {
						int max = communityShopUpgradesMax.get(upgrade);
						if (max >= tier) {
							communityShopSbXp += 10;
						}
					}
				}
			}
		}
		eb.appendDescription("\nCommunity Shop Upgrades: " + formatNumber(communityShopSbXp) + " / 120");

		// Personal bank upgrades
		int personalBankSbXp = 0;
		int personalBankUpgrade = higherDepth(player.profileJson(), "personal_bank_upgrade", 0);
		for (int i = 2; i <= personalBankUpgrade; i++) {
			personalBankSbXp +=
				switch (i) {
					case 2 -> 25;
					case 3 -> 35;
					default -> 50; // 4
				};
		}
		eb.appendDescription("\nPersonal Bank Upgrades: " + formatNumber(personalBankSbXp) + " / 110");

		// Miscellaneous tasks total
		String totalSbXp =
			formatNumber(
				accessoryBagUpgradeSbXp +
				reaperPepperSbXp +
				unlockingPowersSbXp +
				dojoSbXp +
				harpSbXp +
				abiphoneSbXp +
				communityShopSbXp +
				personalBankSbXp
			) +
			" / 1,351";
		eb.getDescriptionBuilder().insert(0, "Miscellaneous Tasks: " + totalSbXp + "\n");

		return new LevelRecord(eb, totalSbXp);
	}

	private static LevelRecord getStoryTasks(Player player) {
		EmbedBuilder eb = player.defaultPlayerEmbed(" | Story Tasks");

		// Objectives/quests
		int objectivesSbXp = 0;
		String[] storyTaskNames = {
			"explore_hub",
			"talk_to_lumberjack",
			"talk_to_fisherman_2",
			"kill_danger_mobs",
			"talk_to_guber_1",
			"talk_to_farmer",
			"talk_to_librarian",
			"explore_village",
			"complete_the_woods_race_4",
			"increase_foraging_skill_5",
			"public_island",
			"talk_to_lazy_miner",
			"increase_farming_skill_5",
			"talk_to_farmhand_1",
			"mine_coal",
			"talk_to_gulliver_1",
			"complete_the_chicken_race_4",
			"complete_the_end_race_4",
			"talk_to_gustave_1",
			"talk_to_banker",
			"help_elle",
		};
		JsonElement objectives = higherDepth(player.profileJson(), "objectives");
		if (objectives != null) {
			for (String storyTaskName : storyTaskNames) {
				if (objectives.getAsJsonObject().has(storyTaskName)) {
					JsonObject objective = objectives.getAsJsonObject().getAsJsonObject(storyTaskName);
					if (objective.has("status") && objective.get("status").getAsString().equals("COMPLETE")) {
						objectivesSbXp += 5;
					}
				}
			}
		}
		eb.appendDescription("\nComplete Objectives: " + formatNumber(objectivesSbXp) + " / 105");

		// Story tasks total
		String totalSbXp = formatNumber(objectivesSbXp) + " / 105";
		eb.getDescriptionBuilder().insert(0, "Story Tasks: " + totalSbXp + "\n");

		return new LevelRecord(eb, totalSbXp);
	}

	private record LevelRecord(EmbedBuilder eb, String total) {}
}
