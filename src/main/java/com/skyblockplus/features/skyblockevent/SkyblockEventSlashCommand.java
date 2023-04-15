/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2023 kr45732
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

package com.skyblockplus.features.skyblockevent;

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Constants.profilesCommandOption;
import static com.skyblockplus.utils.utils.JsonUtils.getJsonKeys;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.api.serversettings.skyblockevent.EventMember;
import com.skyblockplus.api.serversettings.skyblockevent.EventSettings;
import com.skyblockplus.features.listeners.AutomaticGuild;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.command.Subcommand;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.springframework.stereotype.Component;

@Component
public class SkyblockEventSlashCommand extends SlashCommand {

	public SkyblockEventSlashCommand() {
		this.name = "event";
	}

	public static List<EventMember> getEventLeaderboardList(JsonElement runningSettings, String guildId) {
		List<EventMember> eventLeaderboardList = new ArrayList<>();
		List<CompletableFuture<EventMember>> futuresList = new ArrayList<>();
		List<Player.Profile> players = new ArrayList<>();
		JsonArray membersArr = higherDepth(runningSettings, "membersList").getAsJsonArray();
		String eventType = higherDepth(runningSettings, "eventType").getAsString();

		String key = database.getServerHypixelApiKey(guildId);
		key = checkHypixelKey(key) == null ? key : null; // Set key to null if invalid
		if (membersArr.size() > 40 && key == null) {
			return null;
		}
		String hypixelKey = key;

		guildMap.get(guildId).setEventCurrentlyUpdating(true);
		for (JsonElement eventMember : membersArr) {
			String uuid = higherDepth(eventMember, "uuid").getAsString();
			String profileName = higherDepth(eventMember, "profileName").getAsString();

			try {
				if (hypixelKey != null ? keyCooldownMap.get(hypixelKey).isRateLimited() : remainingLimit.get() < 5) {
					System.out.println(
						"Sleeping for " +
						(hypixelKey != null ? keyCooldownMap.get(hypixelKey).getTimeTillReset() : timeTillReset) +
						" seconds"
					);
					TimeUnit.SECONDS.sleep(hypixelKey != null ? keyCooldownMap.get(hypixelKey).getTimeTillReset() : timeTillReset.get());
				}
			} catch (Exception ignored) {}

			futuresList.add(
				asyncSkyblockProfilesFromUuid(uuid, hypixelKey != null ? hypixelKey : HYPIXEL_API_KEY)
					.thenApplyAsync(
						memberProfileJsonResponse -> {
							Player.Profile player = new Player(
								uuidToUsername(uuid).username(),
								uuid,
								profileName,
								memberProfileJsonResponse,
								false
							)
								.getSelectedProfile();

							if (player.isValid()) {
								players.add(player);

								double startingAmount = higherDepth(eventMember, "startingAmount").getAsDouble();
								Double curChange =
									switch (eventType) {
										case "slayer" -> player.getTotalSlayer() - startingAmount;
										case "catacombs" -> player.getCatacombs().totalExp() - startingAmount;
										default -> {
											if (eventType.startsWith("collection.")) {
												yield higherDepth(player.profileJson(), eventType.split("-")[0], 0.0) - startingAmount;
											} else if (eventType.startsWith("skills.")) {
												if (player.isSkillsApiEnabled()) {
													double skillsXp = 0;

													String[] skillTypes = eventType.split("skills.")[1].split("-");
													for (String skillType : skillTypes) {
														skillsXp += Math.max(player.getSkillXp(skillType), 0);
													}

													yield skillsXp - startingAmount;
												}
											} else if (eventType.startsWith("weight.")) {
												String[] weightTypes = eventType.split("weight.")[1].split("-");
												double weightAmt = player.getWeight(weightTypes);

												if (weightAmt != -1) {
													yield weightAmt - startingAmount;
												}
											}

											yield null;
										}
									};

								if (curChange != null) {
									return new EventMember(
										player.getUsername(),
										uuid,
										"" + curChange,
										higherDepth(eventMember, "profileName").getAsString()
									);
								}
							}
							return null;
						},
						executor
					)
			);
		}

		for (CompletableFuture<EventMember> future : futuresList) {
			try {
				EventMember playerFutureResponse = future.get();
				if (playerFutureResponse != null) {
					eventLeaderboardList.add(playerFutureResponse);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		leaderboardDatabase.insertIntoLeaderboard(players);

		eventLeaderboardList.sort(Comparator.comparingDouble(o1 -> -Double.parseDouble(o1.getStartingAmount())));

		guildMap.get(guildId).setEventCurrentlyUpdating(false);
		return eventLeaderboardList;
	}

	public static String getEventTypeFormatted(String eventType) {
		if (eventType.startsWith("collection.")) {
			return eventType.split("-")[1] + " collection";
		} else if (eventType.startsWith("skills.")) {
			String[] types = eventType.split("skills.")[1].split("-");
			return (types.length == 9 ? "skills" : String.join(", ", types) + " skill" + (types.length > 1 ? "s" : "")) + " xp";
		} else if (eventType.startsWith("weight.")) {
			String[] types = eventType.split("weight.")[1].split("-");
			return types.length == 20 ? "weight" : String.join(", ", types) + " weight" + (types.length > 1 ? "s" : "");
		}

		return eventType;
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands.slash(name, "Main event command");
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static class CreateSubcommand extends Subcommand {

		public CreateSubcommand() {
			this.name = "create";
			this.userPermissions = new Permission[] { Permission.ADMINISTRATOR };
		}

		public static EmbedBuilder createSkyblockEvent(SlashCommandEvent event) {
			if (!higherDepth(database.getSkyblockEventSettings(event.getGuild().getId()), "eventType", "").isEmpty()) {
				return errorEmbed("Event already running");
			} else if (guildMap.containsKey(event.getGuild().getId())) {
				AutomaticGuild automaticGuild = guildMap.get(event.getGuild().getId());
				if (automaticGuild.skyblockEventHandler == null) {
					automaticGuild.setSkyblockEventHandler(new SkyblockEventHandler(event));
					return null;
				} else {
					return errorEmbed("Someone is already creating an event in this server");
				}
			} else {
				return errorEmbed("Cannot find server");
			}
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.paginate(createSkyblockEvent(event));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData("create", "Interactive message to create a Skyblock event");
		}
	}

	public static class CurrentSubcommand extends Subcommand {

		public CurrentSubcommand() {
			this.name = "current";
		}

		public static EmbedBuilder getCurrentSkyblockEvent(String guildId) {
			JsonElement currentSettings = database.getSkyblockEventSettings(guildId);
			if (!higherDepth(currentSettings, "eventType", "").isEmpty()) {
				EmbedBuilder eb = defaultEmbed("Current Event");

				if (!higherDepth(currentSettings, "eventGuildId", "").isEmpty()) {
					HypixelResponse guildJson = getGuildFromId(higherDepth(currentSettings, "eventGuildId").getAsString());
					if (!guildJson.isValid()) {
						return guildJson.getErrorEmbed();
					}
					eb.addField("Guild", guildJson.get("name").getAsString(), false);
				}

				eb.addField(
					"Event Type",
					capitalizeString(getEventTypeFormatted(higherDepth(currentSettings, "eventType").getAsString())),
					false
				);

				Instant eventInstantEnding = Instant.ofEpochSecond(higherDepth(currentSettings, "timeEndingSeconds").getAsLong());

				eb.addField("End Date", "Ends <t:" + eventInstantEnding.getEpochSecond() + ":R>", false);

				StringBuilder ebString = new StringBuilder();
				for (Map.Entry<String, JsonElement> prize : higherDepth(currentSettings, "prizeMap").getAsJsonObject().entrySet()) {
					ebString.append("`").append(prize.getKey()).append(")` ").append(prize.getValue().getAsString()).append("\n");
				}

				if (ebString.length() == 0) {
					ebString = new StringBuilder("None");
				}

				eb.addField("Prizes", ebString.toString(), false);
				eb.addField("Members Joined", "" + higherDepth(currentSettings, "membersList").getAsJsonArray().size(), false);

				return eb;
			} else {
				return defaultEmbed("No event running");
			}
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.embed(getCurrentSkyblockEvent(event.getGuild().getId()));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData("current", "Get information about the current event");
		}
	}

	public static class JoinSubcommand extends Subcommand {

		public JoinSubcommand() {
			this.name = "join";
		}

		public static EmbedBuilder joinSkyblockEvent(String username, String profile, Member member, String guildId) {
			JsonElement eventSettings = database.getSkyblockEventSettings(guildId);
			if (!higherDepth(eventSettings, "eventType", "").isEmpty()) {
				String uuid;
				if (member != null) {
					LinkedAccount linkedAccount = database.getByDiscord(member.getId());
					if (linkedAccount == null) {
						return errorEmbed("You must be linked to run this command. Use `/link <player>` to link");
					}

					uuid = linkedAccount.uuid();
					username = linkedAccount.username();
				} else {
					UsernameUuidStruct uuidStruct = usernameToUuid(username);
					if (!uuidStruct.isValid()) {
						return errorEmbed(uuidStruct.failCause());
					}

					uuid = usernameToUuid(username).uuid();
					username = uuidStruct.username();
				}

				if (database.eventHasMemberByUuid(guildId, uuid)) {
					return errorEmbed(
						member != null
							? "You are already in the event! If you want to leave or change profile use `/event leave`"
							: "Player is already in the event"
					);
				}

				if (member != null) {
					if (!higherDepth(eventSettings, "eventGuildId", "").isEmpty()) {
						HypixelResponse guildJson = getGuildFromPlayer(uuid);
						if (!guildJson.isValid()) {
							return guildJson.getErrorEmbed();
						}

						if (!guildJson.get("_id").getAsString().equals(higherDepth(eventSettings, "eventGuildId").getAsString())) {
							return errorEmbed("You must be in the guild to join the event");
						}
					}

					String requiredRole = higherDepth(eventSettings, "whitelistRole", "");
					if (!requiredRole.isEmpty() && member.getRoles().stream().noneMatch(r -> r.getId().equals(requiredRole))) {
						return errorEmbed("You must have the <@&" + requiredRole + "> role to join this event");
					}
				}

				Player.Profile player = Player.create(username, profile);
				if (player.isValid()) {
					try {
						double startingAmount = 0;
						String startingAmountFormatted = "";

						String eventType = higherDepth(eventSettings, "eventType").getAsString();

						if ((eventType.startsWith("skills") || eventType.startsWith("weight")) && !player.isSkillsApiEnabled()) {
							return errorEmbed(
								member != null ? "Please enable your skills API before joining" : "Player's skills API is disabled"
							);
						}

						switch (eventType) {
							case "slayer" -> {
								startingAmount = player.getTotalSlayer();
								startingAmountFormatted = formatNumber(startingAmount) + " total slayer xp";
							}
							case "catacombs" -> {
								startingAmount = player.getCatacombs().totalExp();
								startingAmountFormatted = formatNumber(startingAmount) + " total catacombs xp";
							}
							default -> {
								if (eventType.startsWith("collection.")) {
									startingAmount =
										higherDepth(player.profileJson(), eventType.split("-")[0]) != null
											? higherDepth(player.profileJson(), eventType.split("-")[0]).getAsDouble()
											: 0;
									startingAmountFormatted = formatNumber(startingAmount) + " " + getEventTypeFormatted(eventType);
								} else if (eventType.startsWith("skills.")) {
									String[] skillTypes = eventType.split("skills.")[1].split("-");
									for (String skillType : skillTypes) {
										startingAmount += Math.max(player.getSkillXp(skillType), 0);
									}

									startingAmountFormatted = formatNumber(startingAmount) + " " + getEventTypeFormatted(eventType);
								} else if (eventType.startsWith("weight.")) {
									String weightTypes = eventType.split("weight.")[1];
									startingAmount = player.getWeight(weightTypes.split("-"));
									startingAmountFormatted = formatNumber(startingAmount) + " " + getEventTypeFormatted(eventType);
								}
							}
						}

						try {
							int minAmt = Integer.parseInt(higherDepth(eventSettings, "minAmount").getAsString());
							if (minAmt != -1 && startingAmount < minAmt) {
								return errorEmbed(
									(member != null ? "You" : "Player") +
									" must have at least " +
									formatNumber(minAmt) +
									" " +
									getEventTypeFormatted(eventType) +
									" to join"
								);
							}
						} catch (Exception ignored) {}

						try {
							int maxAmt = Integer.parseInt(higherDepth(eventSettings, "maxAmount").getAsString());
							if (maxAmt != -1 && startingAmount > maxAmt) {
								return errorEmbed(
									(member != null ? "You" : "Player") +
									" must have no more than " +
									formatNumber(maxAmt) +
									" " +
									getEventTypeFormatted(eventType) +
									" to join"
								);
							}
						} catch (Exception ignored) {}

						int code = database.addMemberToSkyblockEvent(
							guildId,
							new EventMember(player.getUsername(), player.getUuid(), "" + startingAmount, player.getProfileName())
						);

						if (code == 200) {
							return defaultEmbed(member != null ? "Joined event" : "Added player to event")
								.setDescription(
									"**Username:** " +
									player.getUsername() +
									"\n**Profile:** " +
									player.getProfileName() +
									"\n**Starting amount:** " +
									startingAmountFormatted
								);
						} else {
							return errorEmbed("API returned code " + code);
						}
					} catch (Exception ignored) {}
				}

				return player.getErrorEmbed();
			} else {
				return errorEmbed("No event running");
			}
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.embed(joinSkyblockEvent(null, event.getOptionStr("profile"), event.getMember(), event.getGuild().getId()));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData("join", "Join the current event").addOptions(profilesCommandOption);
		}
	}

	public static class AddSubcommand extends Subcommand {

		public AddSubcommand() {
			this.name = "add";
			this.userPermissions = new Permission[] { Permission.ADMINISTRATOR };
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.embed(
				JoinSubcommand.joinSkyblockEvent(
					event.getOptionStr("player"),
					event.getOptionStr("profile"),
					null,
					event.getGuild().getId()
				)
			);
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData("add", "Force add a player to the current event")
				.addOption(OptionType.STRING, "player", "Player username or mention", true, true)
				.addOptions(profilesCommandOption);
		}
	}

	public static class LeaveSubcommand extends Subcommand {

		public LeaveSubcommand() {
			this.name = "leave";
		}

		public static EmbedBuilder leaveSkyblockEvent(String guildId, String userId) {
			if (!higherDepth(database.getSkyblockEventSettings(guildId), "eventType", "").isEmpty()) {
				LinkedAccount linkedAccount = database.getByDiscord(userId);
				if (linkedAccount != null) {
					int code = database.removeMemberFromSkyblockEvent(guildId, linkedAccount.uuid());

					if (code == 200) {
						return defaultEmbed("Success").setDescription("You left the event");
					} else {
						return errorEmbed("An error occurred when leaving the event");
					}
				} else {
					return defaultEmbed("You must be linked to run this command. Use `/link <player>` to link");
				}
			} else {
				return defaultEmbed("No event running");
			}
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.embed(leaveSkyblockEvent(event.getGuild().getId(), event.getUser().getId()));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData("leave", "Leave the current event");
		}
	}

	public static class RemoveSubcommand extends Subcommand {

		public RemoveSubcommand() {
			this.name = "remove";
		}

		public static EmbedBuilder removeFromSkyblockEvent(String guildId, String player) {
			if (!higherDepth(database.getSkyblockEventSettings(guildId), "eventType", "").isEmpty()) {
				UsernameUuidStruct usernameUuidStruct = usernameToUuid(player);
				if (!usernameUuidStruct.isValid()) {
					return errorEmbed(usernameUuidStruct.failCause());
				}

				int code = database.removeMemberFromSkyblockEvent(guildId, usernameUuidStruct.uuid());
				if (code == 200) {
					return defaultEmbed("Success").setDescription("Removed " + usernameUuidStruct.username() + " from the event");
				} else {
					return errorEmbed("An error occurred when leaving the event");
				}
			} else {
				return defaultEmbed("No event running");
			}
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.embed(removeFromSkyblockEvent(event.getGuild().getId(), event.getOptionStr("player")));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData("remove", "Force remove a player from the current event")
				.addOption(OptionType.STRING, "player", "Player username or mention", true, true);
		}
	}

	public static class LeaderboardSubcommand extends Subcommand {

		public LeaderboardSubcommand() {
			this.name = "leaderboard";
			this.cooldown = GLOBAL_COOLDOWN + 2;
		}

		public static EmbedBuilder getEventLeaderboard(
			Guild guild,
			User user,
			SlashCommandEvent slashCommandEvent,
			ButtonInteractionEvent buttonEvent
		) {
			String guildId = guild.getId();

			JsonElement runningSettings = database.getSkyblockEventSettings(guildId);
			if (higherDepth(runningSettings, "eventType", "").isEmpty()) {
				return defaultEmbed("No event running");
			}

			AutomaticGuild currentGuild = guildMap.get(guildId);

			CustomPaginator.Builder paginateBuilder = defaultPaginator(user).setColumns(1).setItemsPerPage(25);

			if (
				(currentGuild.eventMemberList != null) &&
				(currentGuild.eventMemberListLastUpdated != null) &&
				(Duration.between(currentGuild.eventMemberListLastUpdated, Instant.now()).toMinutes() < 15)
			) {
				List<EventMember> eventMemberList = currentGuild.eventMemberList;
				for (int i = 0; i < eventMemberList.size(); i++) {
					EventMember eventMember = eventMemberList.get(i);
					double amt = Double.parseDouble(eventMember.getStartingAmount());
					paginateBuilder.addItems(
						"`" +
						(i + 1) +
						")` " +
						fixUsername(eventMember.getUsername()) +
						" | " +
						(amt >= 0 ? "+ " + formatNumber(amt) : "API disabled")
					);
				}

				if (paginateBuilder.size() > 0) {
					if (slashCommandEvent != null) {
						slashCommandEvent.paginate(
							paginateBuilder.updateExtras(extra ->
								extra
									.setEveryPageTitle("Event Leaderboard")
									.setEveryPageText(
										"**Last Updated <t:" + currentGuild.eventMemberListLastUpdated.getEpochSecond() + ":R>**\n"
									)
							)
						);
					} else {
						paginateBuilder
							.updateExtras(extra ->
								extra
									.setEveryPageTitle("Event Leaderboard")
									.setEveryPageText(
										"**Last Updated:** <t:" + currentGuild.eventMemberListLastUpdated.getEpochSecond() + ":R>\n"
									)
							)
							.build()
							.paginate(buttonEvent.getHook(), 0);
					}
					return null;
				}

				return defaultEmbed("Event Leaderboard").setDescription("No one joined the event");
			}

			if (currentGuild.eventCurrentlyUpdating) {
				return errorEmbed("The leaderboard is currently updating, please try again in a few seconds");
			}

			List<EventMember> eventLeaderboardList = getEventLeaderboardList(runningSettings, guildId);
			if (eventLeaderboardList == null) {
				return errorEmbed("A Hypixel API key must be set for events with over 45 members");
			}

			for (int i = 0; i < eventLeaderboardList.size(); i++) {
				EventMember eventMember = eventLeaderboardList.get(i);
				double amt = Double.parseDouble(eventMember.getStartingAmount());
				paginateBuilder.addItems(
					"`" +
					(i + 1) +
					")` " +
					fixUsername(eventMember.getUsername()) +
					" | " +
					(amt >= 0 ? "+ " + formatNumber(amt) : "API disabled")
				);
			}

			paginateBuilder.getExtras().setEveryPageTitle("Event Leaderboard");

			guildMap.get(guildId).setEventMemberList(eventLeaderboardList);
			guildMap.get(guildId).setEventMemberListLastUpdated(Instant.now());

			if (paginateBuilder.size() > 0) {
				if (slashCommandEvent != null) {
					slashCommandEvent.paginate(paginateBuilder);
				} else {
					paginateBuilder.build().paginate(buttonEvent.getHook(), 0);
				}
				return null;
			}

			return defaultEmbed("Event Leaderboard").setDescription("No one joined the event");
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.paginate(getEventLeaderboard(event.getGuild(), event.getUser(), event, null));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData("leaderboard", "Get the leaderboard for current event");
		}
	}

	public static class EndSubcommand extends Subcommand {

		public EndSubcommand() {
			this.name = "end";
			this.userPermissions = new Permission[] { Permission.ADMINISTRATOR };
		}

		public static EmbedBuilder endSkyblockEvent(Guild guild, boolean silent) {
			String guildId = guild.getId();
			JsonElement eventSettings = database.getSkyblockEventSettings(guildId);
			if (higherDepth(eventSettings, "eventType", "").isEmpty()) {
				return defaultEmbed("No event running");
			}

			if (silent) {
				try {
					guild
						.getTextChannelById(higherDepth(eventSettings, "announcementId").getAsString())
						.retrieveMessageById(higherDepth(eventSettings, "announcementMessageId").getAsString())
						.queue(m ->
							m
								.editMessageEmbeds(defaultEmbed("Skyblock Event").setDescription("Event canceled").build())
								.setComponents()
								.queue()
						);
				} catch (Exception ignored) {}

				guildMap.get(guild.getId()).setEventMemberListLastUpdated(null);
				int code = database.setSkyblockEventSettings(guildId, new EventSettings());

				if (code == 200) {
					return defaultEmbed("Event canceled");
				} else {
					return defaultEmbed("API returned code " + code);
				}
			}

			TextChannel announcementChannel = jda.getTextChannelById(higherDepth(eventSettings, "announcementId").getAsString());
			guildMap.get(guildId).setEventMemberListLastUpdated(null);
			List<EventMember> eventLeaderboardList = getEventLeaderboardList(eventSettings, guildId);
			if (eventLeaderboardList == null) {
				return errorEmbed("A Hypixel API key must be set for events over 45 members so the leaderboard can be calculated");
			}
			guildMap.get(guildId).setEventMemberListLastUpdated(null);

			try {
				announcementChannel
					.retrieveMessageById(higherDepth(eventSettings, "announcementMessageId").getAsString())
					.queue(m ->
						m
							.editMessageEmbeds(defaultEmbed("Skyblock Event").setDescription("Event has ended").build())
							.setComponents()
							.queue()
					);
			} catch (Exception ignored) {}

			CustomPaginator.Builder paginateBuilder = defaultPaginator()
				.setColumns(1)
				.setItemsPerPage(25)
				.updateExtras(extra -> extra.setEveryPageTitle("Event Leaderboard"))
				.setTimeout(24, TimeUnit.HOURS);

			for (int i = 0; i < eventLeaderboardList.size(); i++) {
				EventMember eventMember = eventLeaderboardList.get(i);
				double amt = Double.parseDouble(eventMember.getStartingAmount());
				paginateBuilder.addItems(
					"`" +
					(i + 1) +
					")` " +
					fixUsername(eventMember.getUsername()) +
					" | " +
					(amt >= 0 ? "+ " + formatNumber(amt) : "API disabled")
				);
			}

			try {
				if (paginateBuilder.size() > 0) {
					paginateBuilder.build().paginate(announcementChannel, 0);
				} else {
					announcementChannel
						.sendMessageEmbeds(defaultEmbed("Event Leaderboard").setDescription("No one joined the event").build())
						.complete();
				}
			} catch (Exception ignored) {}

			try {
				paginateBuilder =
					defaultPaginator()
						.setColumns(1)
						.setItemsPerPage(25)
						.updateExtras(extra -> extra.setEveryPageTitle("Prizes"))
						.setTimeout(24, TimeUnit.HOURS);

				ArrayList<String> prizeListKeys = getJsonKeys(higherDepth(eventSettings, "prizeMap"));
				for (int i = 0; i < prizeListKeys.size(); i++) {
					try {
						paginateBuilder.addItems(
							"`" +
							(i + 1) +
							")` " +
							higherDepth(eventSettings, "prizeMap." + prizeListKeys.get(i)).getAsString() +
							" - " +
							(i < eventLeaderboardList.size() ? fixUsername(eventLeaderboardList.get(i).getUsername()) : " None")
						);
					} catch (Exception ignored) {}
				}

				if (paginateBuilder.size() > 0) {
					paginateBuilder.build().paginate(announcementChannel, 0);
				} else {
					announcementChannel.sendMessageEmbeds(defaultEmbed("Prizes").setDescription("None").build()).complete();
				}
			} catch (Exception ignored) {}

			database.setSkyblockEventSettings(guildId, new EventSettings());
			guildMap.get(guildId).cancelSbEventFuture();
			return defaultEmbed("Success").setDescription("Ended Skyblock event");
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.embed(endSkyblockEvent(event.getGuild(), event.getOptionBoolean("silent", false)));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData("end", "Force end the event")
				.addOption(OptionType.BOOLEAN, "silent", "If the event should silently be canceled");
		}
	}
}
