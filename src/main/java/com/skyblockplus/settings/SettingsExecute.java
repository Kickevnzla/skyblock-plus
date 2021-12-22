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

package com.skyblockplus.settings;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.jda;
import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;
import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.serversettings.automatedguild.ApplyBlacklist;
import com.skyblockplus.api.serversettings.automatedguild.ApplyRequirements;
import com.skyblockplus.api.serversettings.automatedguild.AutomatedGuild;
import com.skyblockplus.api.serversettings.automatedroles.RoleModel;
import com.skyblockplus.api.serversettings.automatedroles.RoleObject;
import com.skyblockplus.api.serversettings.managers.ServerSettingsModel;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import com.vdurmont.emoji.EmojiParser;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;

public class SettingsExecute {

	private final Guild guild;
	private final MessageChannel channel;
	private final User author;
	private final String guildPrefix;
	private final JsonObject serverSettings;

	public SettingsExecute(CommandEvent event) {
		this(event.getGuild(), event.getChannel(), event.getAuthor());
	}

	public SettingsExecute(Guild guild, MessageReceivedEvent event) {
		this(guild, event.getChannel(), event.getAuthor());
	}

	public SettingsExecute(Guild guild, MessageChannel channel, User author) {
		this.guild = guild;
		this.channel = channel;
		this.author = author;
		this.guildPrefix = getGuildPrefix(guild.getId());

		if (!database.serverByServerIdExists(guild.getId())) {
			database.newServerSettings(guild.getId(), new ServerSettingsModel(guild.getName(), guild.getId()));
		}
		this.serverSettings = database.getServerSettings(guild.getId()).getAsJsonObject();
	}

	public void execute(Command command, CommandEvent event) {
		new CommandExecute(command, event) {
			@Override
			protected void execute() {
				String content = event.getMessage().getContentRaw();
				if (!content.contains("hypixel_key")) {
					logCommand();
				}

				paginate(getSettingsEmbed(content, args));
			}
		}
			.queue();
	}

	public EmbedBuilder getSettingsEmbed(String content, String[] args) {
		EmbedBuilder eb = null;
		JsonElement currentSettings = database.getServerSettings(guild.getId());
		if (content.split(" ", 4).length == 4 && args[1].equals("set")) {
			switch (args[2]) {
				case "hypixel_key":
					eb = setHypixelKey(args[3]);
					break;
				case "prefix":
					eb = setPrefix(content.split(" ", 4)[3]);
					break;
				case "pf_category":
					eb = setPartyFinderCategory(args[3]);
					break;
				case "guest_role":
					eb = setApplyGuestRole(args[3]);
			}
		} else if (
			(args.length == 4 || args.length == 5 || content.split(" ", 6).length == 6) &&
			args[1].equals("guild") &&
			args[2].equals("blacklist")
		) {
			args = content.split(" ", 6);
			if (args.length == 4 && args[3].equals("list")) {
				eb = listApplyBlacklist();
			} else if ((args.length == 5 || args.length == 6) && args[3].equals("add")) {
				eb = addApplyBlacklist(args[4], args.length == 6 ? args[5] : "not provided");
			} else if (args.length == 5 && args[3].equals("remove")) {
				eb = removeApplyBlacklist(args[4]);
			}
		} else if (args.length >= 2 && args[1].equals("mee6")) {
			if (args.length == 2) {
				eb = getMee6DataSettings();
			} else if (args.length == 3) {
				if (args[2].equals("enable")) {
					eb = setMee6Enable(true);
				} else if (args[2].equals("disable")) {
					eb = setMee6Enable(false);
				}
			} else if (args.length == 4 && args[2].equals("remove")) {
				eb = removeMee6Role(args[3]);
			} else if (args.length == 5 && args[2].equals("add")) {
				eb = addMee6Role(args[3], args[4]);
			}

			if (eb == null) {
				eb = errorEmbed("settings mee6");
			}
		} else if (args.length == 3 && args[1].equals("delete")) {
			switch (args[2]) {
				case "all":
					if (database.deleteServerSettings(guild.getId()) == 200) {
						eb = defaultEmbed("Success").setDescription("Server settings deleted");
					} else {
						eb = invalidEmbed("Error deleting server settings");
					}
					break;
				case "hypixel_key":
					eb = deleteHypixelKey();
					break;
				case "prefix":
					eb = resetPrefix();
					break;
			}

			if (eb == null) {
				eb = errorEmbed("settings delete");
			}
		} else if (args.length == 1) {
			eb = defaultSettingsEmbed();
			eb.addField("General Settings", "Use `" + guildPrefix + "settings general` to see the current settings", false);
			eb.addField("Verify Settings", "Use `" + guildPrefix + "settings verify` to see the current settings", false);
			eb.addField("Guild Settings", "Use `" + guildPrefix + "settings guild` to see the current settings", false);
			eb.addField("Roles Settings", "Use `" + guildPrefix + "settings roles` to see the current settings", false);
			eb.addField("Mee6 Roles Settings", "Use `" + guildPrefix + "settings mee6` to see the current settings", false);
		} else if (args.length == 2 && args[1].equals("general")) {
			eb = defaultSettingsEmbed();
			eb.addField("Prefix", higherDepth(currentSettings, "prefix", DEFAULT_PREFIX), false);
			String hypixelKey = database.getServerHypixelApiKey(guild.getId());
			eb.addField("Hypixel API Key", hypixelKey != null && hypixelKey.length() > 0 ? "Hidden" : "Not set", false);
			String pfCategory = higherDepth(currentSettings, "pfCategoryId", "none");
			eb.addField("Party Finder Category", pfCategory.equals("none") ? "None" : "<#" + pfCategory + ">", false);
			String applyGuestRole = higherDepth(currentSettings, "applyGuestRole", "none");
			eb.addField("Guest Role", applyGuestRole.equals("none") ? "None" : "<@&" + applyGuestRole + ">", false);
		} else if (args.length >= 2 && args[1].equals("roles")) {
			if (args.length == 2) {
				if (higherDepth(currentSettings, "automatedRoles") != null) {
					getRolesSettings(higherDepth(currentSettings, "automatedRoles")).build().paginate(channel, 0);
					return null;
				} else {
					eb = defaultEmbed("Settings").addField("Roles Settings", "Error! Data not found", false);
				}
			} else if (args.length == 3) {
				if (args[2].equals("enable")) {
					eb = setRolesEnable(true);
				} else if (args[2].equals("disable")) {
					eb = setRolesEnable(false);
				} else {
					eb = getRoleSettings(args[2]);
					if (eb == null) {
						return null;
					}
				}
			} else if (args.length == 4) {
				switch (args[2]) {
					case "enable":
						eb = setRoleEnable(args[3], true);
						break;
					case "disable":
						eb = setRoleEnable(args[3], false);
						break;
					case "use_highest":
						if (args[3].equals("true")) {
							eb = setRolesUseHighest(true);
						} else if (args[3].equals("false")) {
							eb = setRolesUseHighest(false);
						}
						break;
				}
			} else if (args.length == 5) {
				if (args[2].equals("remove")) {
					eb = removeRoleLevel(args[3], args[4]);
				} else if (args[2].equals("set")) {
					eb = setOneLevelRole(args[3], args[4]);
				} else if (args[2].equals("add") && args[3].equals("guild_ranks")) {
					eb = addRoleLevel(args[3], args[4], null);
				}
			} else if (args.length == 6 && args[2].equals("add")) {
				eb = addRoleLevel(args[3], args[4], args[5]);
			}

			if (eb == null) {
				eb = errorEmbed("settings roles");
			}
		} else if (content.split(" ", 4).length >= 2 && content.split(" ", 4)[1].equals("verify")) {
			args = content.split(" ", 4);
			if (args.length == 2) {
				eb = defaultEmbed("Settings");
				if (higherDepth(currentSettings, "automatedVerify") != null) {
					eb.setDescription(getCurrentVerifySettings(higherDepth(currentSettings, "automatedVerify")));
				} else {
					eb.setDescription("Error! Data not found");
				}
			} else if (args.length == 3) {
				if (args[2].equals("enable")) {
					if (allowVerifyEnable()) {
						eb = setVerifyEnable("true");
					} else {
						eb = invalidEmbed("All other verify settings must be set before " + "enabling verify!");
					}
				} else if (args[2].equals("disable")) {
					eb = setVerifyEnable("false");
				}
			} else if (args.length == 4) {
				switch (args[2]) {
					case "message":
						eb = setVerifyMessageText(args[3]);
						break;
					case "channel":
						eb = setVerifyMessageTextChannelId(args[3]);
						break;
					case "nickname":
						eb = setVerifyNickname(args[3]);
						break;
					case "role":
						args = content.split(" ");
						if (args[3].equals("add")) {
							eb = addVerifyRole(args[4]);
						} else if (args[3].equals("remove")) {
							eb = removeVerifyRole(args[4]);
						} else {
							eb = invalidEmbed("Invalid setting");
						}
						break;
					case "remove_role":
						eb = setVerifyRemoveRole(args[3]);
						break;
					case "enable":
						if (args[3].equals("sync")) {
							eb = setVerifySyncEnable("true");
						}
						break;
					case "disable":
						if (args[3].equals("sync")) {
							eb = setVerifySyncEnable("false");
						}
						break;
				}
			}

			if (eb == null) {
				eb = errorEmbed("settings verify");
			}
		} else if ((args.length >= 2) && args[1].equals("guild")) {
			if (args.length == 2) {
				eb =
					defaultSettingsEmbed()
						.addField(
							"Automatic Guild One",
							(
								higherDepth(currentSettings, "automatedGuildOne.guildName") != null
									? "Name: " +
									higherDepth(currentSettings, "automatedGuildOne.guildName").getAsString() +
									"\nCommand: `" +
									guildPrefix +
									"settings guild " +
									higherDepth(currentSettings, "automatedGuildOne.guildName").getAsString() +
									"`" +
									""
									: "Not setup"
							),
							false
						)
						.addField(
							"Automatic Guild Two",
							(
								higherDepth(currentSettings, "automatedGuildTwo.guildName") != null
									? "Name: " +
									higherDepth(currentSettings, "automatedGuildTwo.guildName").getAsString() +
									"\nCommand: `" +
									guildPrefix +
									"settings guild " +
									higherDepth(currentSettings, "automatedGuildTwo.guildName").getAsString() +
									"`" +
									""
									: "Not setup"
							),
							false
						);
			} else if (args.length == 3) {
				return getGuildSettings(args[2]);
			} else if (args.length == 4) {
				if (args[2].equals("create")) {
					eb = createNewGuild(args[3]);
				} else if (args[2].equals("remove")) {
					eb = removeGuild(args[3]);
				}
			} else if (args.length == 5) {
				JsonElement guildSettings = database.getGuildSettings(guild.getId(), args[2]);
				if (guildSettings == null || guildSettings.isJsonNull()) {
					eb = invalidEmbed("No automated guild is created for " + args[2]);
				} else if (args[3].equals("member_role")) {
					if (args[4].equals("enable")) {
						eb = setGuildMemberRoleEnable(guildSettings.getAsJsonObject(), true);
					} else if (args[4].equals("disable")) {
						eb = setGuildMemberRoleEnable(guildSettings.getAsJsonObject(), false);
					} else {
						eb = setGuildMemberRole(guildSettings.getAsJsonObject(), args[4]);
					}
				} else if (args[3].equals("ranks")) {
					if (args[4].equals("enable")) {
						eb = setGuildRanksEnable(guildSettings.getAsJsonObject(), true);
					} else if (args[4].equals("disable")) {
						eb = setGuildRanksEnable(guildSettings.getAsJsonObject(), false);
					}
				} else if (args[3].equals("counter")) {
					if (args[4].equals("enable")) {
						eb = setGuildCounterEnable(guildSettings.getAsJsonObject(), true);
					} else if (args[4].equals("disable")) {
						eb = setGuildCounterEnable(guildSettings.getAsJsonObject(), false);
					}
				} else if (args[3].equals("apply")) {
					if (args[4].equals("enable")) {
						eb = setApplyEnable(guildSettings.getAsJsonObject(), true);
					} else if (args[4].equals("disable")) {
						eb = setApplyEnable(guildSettings.getAsJsonObject(), false);
					}
				}
			} else if (
				(args = content.split("\\s+", 6)).length == 6 &&
				!(args[3].equals("ranks") && args[4].equals("add")) &&
				!(
					args[3].equals("apply") &&
					(args[4].equals("staff_role") || args[4].equals("requirements") || args[4].equals("reqs") || args[4].equals("req"))
				)
			) {
				JsonElement guildSettings = database.getGuildSettings(guild.getId(), args[2]);
				if (guildSettings == null || guildSettings.isJsonNull()) {
					eb = invalidEmbed("No automated guild is created for " + args[2]);
				} else if (args[3].equals("ranks")) {
					if (args[4].equals("remove")) {
						eb = removeGuildRank(guildSettings.getAsJsonObject(), args[5]);
					}
				} else if (args[3].equals("apply")) {
					switch (args[4]) {
						case "message":
							eb = setApplyMessage(guildSettings.getAsJsonObject(), args[5]);
							break;
						case "channel":
							eb = setApplyChannel(guildSettings.getAsJsonObject(), args[5]);
							break;
						case "category":
							eb = setApplyCategory(guildSettings.getAsJsonObject(), args[5]);
							break;
						case "staff_channel":
							eb = setApplyStaffChannel(guildSettings.getAsJsonObject(), args[5]);
							break;
						case "waiting_channel":
							eb = setApplyWaitingChannel(guildSettings.getAsJsonObject(), args[5]);
							break;
						case "accept_message":
							eb = setApplyAcceptMessage(guildSettings.getAsJsonObject(), args[5]);
							break;
						case "waitlist_message":
							eb = setApplyWaitlistMessage(guildSettings.getAsJsonObject(), args[5]);
							break;
						case "deny_message":
							eb = setApplyDenyMessage(guildSettings.getAsJsonObject(), args[5]);
							break;
						case "ironman":
							if (args[5].equals("true")) {
								eb = setApplyIronman(guildSettings.getAsJsonObject(), true);
							} else if (args[5].equals("false")) {
								eb = setApplyIronman(guildSettings.getAsJsonObject(), false);
							}
							break;
						case "scammer_check":
							if (args[5].equals("true")) {
								eb = setApplyScammerCheck(guildSettings.getAsJsonObject(), true);
							} else if (args[5].equals("false")) {
								eb = setApplyScammerCheck(guildSettings.getAsJsonObject(), false);
							}
							break;
					}
				}
			} else if ((args = content.split("\\s+", 7)).length == 7) {
				JsonElement guildSettings = database.getGuildSettings(guild.getId(), args[2]);
				if (guildSettings == null || guildSettings.isJsonNull()) {
					eb = invalidEmbed("No automated guild is created for " + args[2]);
				} else if (args[3].equals("ranks")) {
					if (args[4].equals("add")) {
						eb = addGuildRank(guildSettings.getAsJsonObject(), args[5], args[6]);
					}
				} else if (args[3].equals("apply")) {
					if (args[4].equals("staff_role")) {
						if (args[5].equals("add")) {
							eb = addApplyStaffRole(guildSettings.getAsJsonObject(), args[6]);
						} else if (args[5].equals("remove")) {
							eb = removeApplyStaffRole(guildSettings.getAsJsonObject(), args[6]);
						}
					} else if (args[4].equals("requirements") || args[4].equals("reqs") || args[4].equals("req")) {
						if (args[5].equals("add")) {
							eb = addApplyRequirement(guildSettings.getAsJsonObject(), args[6]);
						} else if (args[5].equals("remove")) {
							eb = removeApplyRequirement(guildSettings.getAsJsonObject(), args[6]);
						}
					}
				}
			}

			if (eb == null) {
				eb = errorEmbed("settings guild");
			}
		}

		if (eb == null) {
			eb = errorEmbed("settings");
		}

		return eb;
	}

	public EmbedBuilder createNewGuild(String guildName) {
		HypixelResponse guildResponse = getGuildFromName(guildName);
		if (guildResponse.isNotValid()) {
			return invalidEmbed(guildResponse.getFailCause());
		}

		String guildNameFormatted = guildResponse.get("name").getAsString();
		AutomatedGuild guildSettings = new AutomatedGuild(
			guildNameFormatted.toLowerCase().replace(" ", "_"),
			guildResponse.get("_id").getAsString()
		);

		int responseCode = database.setGuildSettings(guild.getId(), gson.toJsonTree(guildSettings));
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed("Successfully created a new automatic guild for `" + guildNameFormatted + "`");
	}

	public EmbedBuilder setGuildMemberRoleEnable(JsonObject guildSettings, boolean enable) {
		if (!enable) {
			guildSettings.addProperty("guildMemberRoleEnable", "false");
			int responseCode = database.setGuildSettings(guild.getId(), guildSettings);
			if (responseCode != 200) {
				return apiFailMessage(responseCode);
			}

			return defaultSettingsEmbed("Disabled automatic guild role");
		}

		if (higherDepth(guildSettings, "guildMemberRoleEnable", "").isEmpty()) {
			return invalidEmbed("The role must be set");
		}

		guildSettings.addProperty("guildMemberRoleEnable", "true");
		int responseCode = database.setGuildSettings(guild.getId(), guildSettings);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed("Enabled automatic guild member role");
	}

	public EmbedBuilder setGuildMemberRole(JsonObject guildSettings, String roleMention) {
		Object eb = checkRole(roleMention);
		if (eb instanceof EmbedBuilder) {
			return ((EmbedBuilder) eb);
		}
		Role role = ((Role) eb);

		guildSettings.addProperty("guildMemberRole", role.getId());
		int responseCode = database.setGuildSettings(guild.getId(), guildSettings);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed("Set guild member role to " + role.getAsMention());
	}

	public EmbedBuilder setGuildRanksEnable(JsonObject guildSettings, boolean enable) {
		if (!enable) {
			guildSettings.addProperty("guildRanksEnable", "false");
			int responseCode = database.setGuildSettings(guild.getId(), guildSettings);
			if (responseCode != 200) {
				return apiFailMessage(responseCode);
			}

			return defaultSettingsEmbed("Disabled automatic guild ranks");
		}

		if (higherDepth(guildSettings, "guildRanks").getAsJsonArray().size() == 0) {
			return invalidEmbed("At least one guild rank must be set");
		}

		guildSettings.addProperty("guildRanksEnable", "true");
		int responseCode = database.setGuildSettings(guild.getId(), guildSettings);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed("Enabled automatic guild ranks");
	}

	public EmbedBuilder setGuildCounterEnable(JsonObject guildSettings, boolean enable) {
		if (!enable) {
			try {
				guild.getVoiceChannelById(guildSettings.get("guildCounterChannel").getAsString()).delete().queue();
			} catch (Exception ignored) {}

			guildSettings.addProperty("guildCounterEnable", "false");
			int responseCode = database.setGuildSettings(guild.getId(), guildSettings);
			if (responseCode != 200) {
				return apiFailMessage(responseCode);
			}

			return defaultSettingsEmbed("Disabled automatic guild member counter");
		}

		HypixelResponse guildJson = getGuildFromId(higherDepth(guildSettings, "guildId").getAsString());
		if (guildJson.isNotValid()) {
			return invalidEmbed(guildJson.getFailCause());
		}

		VoiceChannel guildMemberCounterChannel = guild
			.createVoiceChannel(
				guildJson.get("name").getAsString() + " Members: " + guildJson.get("members").getAsJsonArray().size() + "/125"
			)
			.addPermissionOverride(guild.getPublicRole(), EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.VOICE_CONNECT))
			.addMemberPermissionOverride(
				jda.getSelfUser().getIdLong(),
				EnumSet.of(Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL, Permission.VOICE_CONNECT),
				null
			)
			.complete();
		guildSettings.addProperty("guildCounterEnable", "true");
		guildSettings.addProperty("guildCounterChannel", guildMemberCounterChannel.getId());

		int responseCode = database.setGuildSettings(guild.getId(), guildSettings);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed("Enabled automatic guild member counter");
	}

	public EmbedBuilder addGuildRank(JsonObject guildSettings, String rankName, String roleMention) {
		Object eb = checkRole(roleMention);
		if (eb instanceof EmbedBuilder) {
			return ((EmbedBuilder) eb);
		}
		Role role = ((Role) eb);

		HypixelResponse guildJson = getGuildFromId(higherDepth(guildSettings, "guildId").getAsString());
		if (guildJson.isNotValid()) {
			return invalidEmbed(guildJson.getFailCause());
		}

		JsonArray guildRanks = guildJson.get("ranks").getAsJsonArray();
		for (JsonElement guildRank : guildRanks) {
			if (higherDepth(guildRank, "name").getAsString().equalsIgnoreCase(rankName.replace("_", " "))) {
				JsonArray currentGuildRanks = guildSettings.get("guildRanks").getAsJsonArray();

				for (int i = currentGuildRanks.size() - 1; i >= 0; i--) {
					if (higherDepth(currentGuildRanks.get(i), "value").getAsString().equalsIgnoreCase(rankName)) {
						currentGuildRanks.remove(i);
					}
				}

				currentGuildRanks.add(gson.toJsonTree(new RoleObject(rankName.toLowerCase(), role.getId())));
				guildSettings.add("guildRanks", currentGuildRanks);

				int responseCode = database.setGuildSettings(guild.getId(), guildSettings);
				if (responseCode != 200) {
					return apiFailMessage(responseCode);
				}

				return defaultSettingsEmbed(
					"Added guild rank: " + higherDepth(guildRank, "name").getAsString() + " - " + role.getAsMention()
				);
			}
		}

		return invalidEmbed(
			"Invalid guild rank. " +
			(
				guildRanks.size() > 0
					? "Valid guild ranks are: " +
					streamJsonArray(guildRanks)
						.map(r -> higherDepth(r, "name").getAsString().replace(" ", "_"))
						.collect(Collectors.joining(", "))
					: "No guild ranks found"
			)
		);
	}

	public EmbedBuilder removeGuildRank(JsonObject guildSettings, String rankName) {
		JsonArray currentGuildRanks = guildSettings.get("guildRanks").getAsJsonArray();

		for (JsonElement guildRank : currentGuildRanks) {
			if (higherDepth(guildRank, "value").getAsString().equalsIgnoreCase(rankName)) {
				currentGuildRanks.remove(guildRank);
				if (currentGuildRanks.size() == 0) {
					guildSettings.addProperty("guildCounterEnable", "false");
				}

				guildSettings.add("guildRanks", currentGuildRanks);
				int responseCode = database.setGuildSettings(guild.getId(), guildSettings);
				if (responseCode != 200) {
					return apiFailMessage(responseCode);
				}

				return defaultSettingsEmbed("Removed guild rank: " + rankName);
			}
		}

		return invalidEmbed("No role set for the provided rank");
	}

	public EmbedBuilder setApplyEnable(JsonObject guildSettings, boolean enable) {
		if (!enable) {
			guildSettings.addProperty("applyEnable", "false");
			int responseCode = database.setGuildSettings(guild.getId(), guildSettings);
			if (responseCode != 200) {
				return apiFailMessage(responseCode);
			}

			return defaultSettingsEmbed("Disabled apply. Run `" + guildPrefix + "reload` to reload the settings");
		}

		if (
			!higherDepth(guildSettings, "applyMessageChannel", "").isEmpty() &&
			!higherDepth(guildSettings, "applyStaffChannel", "").isEmpty() &&
			!higherDepth(guildSettings, "applyCategory", "").isEmpty() &&
			!higherDepth(guildSettings, "applyMessage", "").isEmpty() &&
			!higherDepth(guildSettings, "applyAcceptMessage", "").isEmpty() &&
			!higherDepth(guildSettings, "applyDenyMessage", "").isEmpty()
		) {
			guildSettings.addProperty("applyEnable", "true");
			int responseCode = database.setGuildSettings(guild.getId(), guildSettings);
			if (responseCode != 200) {
				return apiFailMessage(responseCode);
			}

			return defaultSettingsEmbed("Enabled apply\nRun `" + guildPrefix + "reload` to reload the settings");
		}

		return invalidEmbed("All required apply settings must be set before enabling");
	}

	public EmbedBuilder setApplyMessage(JsonObject guildSettings, String message) {
		if (message.length() <= 0 || EmojiParser.parseToAliases(message).length() > 1500) {
			return invalidEmbed("Message cannot by empty or longer than 1500 letters");
		}

		guildSettings.addProperty("applyMessage", EmojiParser.parseToAliases(message));
		int responseCode = database.setGuildSettings(guild.getId(), guildSettings);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed("Apply message set to: " + message);
	}

	public EmbedBuilder setApplyScammerCheck(JsonObject guildSettings, boolean enable) {
		guildSettings.addProperty("applyScammerCheck", "" + enable);
		int responseCode = database.setGuildSettings(guild.getId(), guildSettings);

		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed("Apply SkyblockZ scammer check: " + (enable ? "enabled" : "disabled"));
	}

	public EmbedBuilder setApplyChannel(JsonObject guildSettings, String textChannel) {
		TextChannel applyChannel = guild.getTextChannelById(textChannel.replaceAll("[<#>]", ""));
		EmbedBuilder eb = checkTextChannel(applyChannel);
		if (eb != null) {
			return eb;
		}

		guildSettings.addProperty("applyMessageChannel", applyChannel.getId());
		int responseCode = database.setGuildSettings(guild.getId(), guildSettings);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed("Set apply message channel to: " + applyChannel.getAsMention());
	}

	public EmbedBuilder setApplyCategory(JsonObject guildSettings, String messageCategory) {
		try {
			Category applyCategory = guild.getCategoryById(messageCategory.replaceAll("[<#>]", ""));
			guildSettings.addProperty("applyCategory", applyCategory.getId());
			int responseCode = database.setGuildSettings(guild.getId(), guildSettings);
			if (responseCode != 200) {
				return apiFailMessage(responseCode);
			}

			return defaultSettingsEmbed("Set apply category to: <#" + applyCategory.getId() + ">");
		} catch (Exception ignored) {}
		return invalidEmbed("Invalid guild category");
	}

	public EmbedBuilder setApplyStaffChannel(JsonObject guildSettings, String textChannel) {
		TextChannel staffTextChannel = guild.getTextChannelById(textChannel.replaceAll("[<#>]", ""));
		EmbedBuilder eb = checkTextChannel(staffTextChannel);
		if (eb != null) {
			return eb;
		}

		guildSettings.addProperty("applyStaffChannel", staffTextChannel.getId());
		int responseCode = database.setGuildSettings(guild.getId(), guildSettings);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed("Set apply staff channel to: " + staffTextChannel.getAsMention());
	}

	public EmbedBuilder setApplyWaitingChannel(JsonObject guildSettings, String textChannel) {
		TextChannel waitingChannel = null;
		if (!textChannel.equalsIgnoreCase("none")) {
			waitingChannel = guild.getTextChannelById(textChannel.replaceAll("[<#>]", ""));
			EmbedBuilder eb = checkTextChannel(waitingChannel);
			if (eb != null) {
				return eb;
			}
		}

		guildSettings.addProperty("applyWaitingChannel", waitingChannel == null ? "none" : waitingChannel.getId());
		int responseCode = database.setGuildSettings(guild.getId(), guildSettings);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed(
			"Set apply waiting for invite channel to: " + (waitingChannel == null ? "none" : waitingChannel.getAsMention())
		);
	}

	public EmbedBuilder setApplyAcceptMessage(JsonObject guildSettings, String acceptMessage) {
		if (acceptMessage.length() <= 0 || EmojiParser.parseToAliases(acceptMessage).length() > 1500) {
			return invalidEmbed("Text cannot be empty or greater than 1500 letters");
		}

		guildSettings.addProperty("applyAcceptMessage", EmojiParser.parseToAliases(acceptMessage));
		int responseCode = database.setGuildSettings(guild.getId(), guildSettings);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed("Apply accept message set to: " + acceptMessage);
	}

	public EmbedBuilder setApplyWaitlistMessage(JsonObject guildSettings, String waitlistMessage) {
		if (waitlistMessage.length() <= 0 || EmojiParser.parseToAliases(waitlistMessage).length() > 1500) {
			return invalidEmbed("Text cannot be empty or longer than 1500 letters");
		}

		guildSettings.addProperty(
			"applyWaitlistMessage",
			waitlistMessage.equalsIgnoreCase("none") ? "none" : EmojiParser.parseToAliases(waitlistMessage)
		);
		int responseCode = database.setGuildSettings(guild.getId(), guildSettings);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed("Set apply waitlist message to: " + waitlistMessage);
	}

	public EmbedBuilder setApplyDenyMessage(JsonObject guildSettings, String denyMessage) {
		if (denyMessage.length() <= 0 || EmojiParser.parseToAliases(denyMessage).length() > 1500) {
			return invalidEmbed("Text cannot be empty or greater than 1500 letters");
		}

		guildSettings.addProperty("applyDenyMessage", EmojiParser.parseToAliases(denyMessage));
		int responseCode = database.setGuildSettings(guild.getId(), guildSettings);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed("Apply deny message set to: " + denyMessage);
	}

	public EmbedBuilder setApplyIronman(JsonObject guildSettings, boolean isIronman) {
		guildSettings.addProperty("applyIronmanOnly", "" + isIronman);
		int responseCode = database.setGuildSettings(guild.getId(), guildSettings);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed("Set apply to " + (isIronman ? "ironman mode only" : "any mode"));
	}

	public EmbedBuilder addApplyStaffRole(JsonObject guildSettings, String roleMention) {
		Object eb = checkRole(roleMention);
		if (eb instanceof EmbedBuilder) {
			return ((EmbedBuilder) eb);
		}
		Role role = ((Role) eb);

		JsonArray staffRoles = higherDepth(guildSettings, "applyStaffRoles").getAsJsonArray();
		if (staffRoles.size() >= 3) {
			return defaultEmbed("You have reached the max number of staff ping roles (3/3)");
		}

		for (int i = staffRoles.size() - 1; i >= 0; i--) {
			if (staffRoles.get(i).getAsString().equals(role.getId())) {
				staffRoles.remove(i);
			}
		}

		staffRoles.add(role.getId());
		guildSettings.add("applyStaffRoles", staffRoles);
		int responseCode = database.setGuildSettings(guild.getId(), guildSettings);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed("Added apply staff role: " + role.getAsMention());
	}

	public EmbedBuilder removeApplyStaffRole(JsonObject guildSettings, String roleMention) {
		Object eb = checkRole(roleMention);
		if (eb instanceof EmbedBuilder) {
			return ((EmbedBuilder) eb);
		}
		Role role = ((Role) eb);

		JsonArray staffRoles = higherDepth(guildSettings, "applyStaffRoles").getAsJsonArray();
		for (int i = staffRoles.size() - 1; i >= 0; i--) {
			if (staffRoles.get(i).getAsString().equals(role.getId())) {
				staffRoles.remove(i);
			}
		}

		guildSettings.add("applyStaffRoles", staffRoles);
		int responseCode = database.setGuildSettings(guild.getId(), guildSettings);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed("Removeda apply staff role: " + role.getAsMention());
	}

	public EmbedBuilder addApplyRequirement(JsonObject guildSettings, String reqArgs) {
		JsonArray currentReqs = guildSettings.getAsJsonArray("applyReqs");

		if (currentReqs.size() >= 3) {
			return invalidEmbed("You can only have up to 3 requirements");
		}

		int slayerReq = 0;
		int skillsReq = 0;
		int cataReq = 0;
		int weightReq = 0;

		try {
			slayerReq = Integer.parseInt(reqArgs.split("slayer:")[1].split(" ")[0]);
		} catch (Exception ignored) {}

		try {
			skillsReq = Integer.parseInt(reqArgs.split("skills:")[1].split(" ")[0]);
		} catch (Exception ignored) {}

		try {
			cataReq = Integer.parseInt(reqArgs.split("catacombs:")[1].split(" ")[0]);
		} catch (Exception ignored) {}

		try {
			weightReq = Integer.parseInt(reqArgs.split("weight:")[1].split(" ")[0]);
		} catch (Exception ignored) {}

		ApplyRequirements toAddReq = new ApplyRequirements("" + slayerReq, "" + skillsReq, "" + cataReq, "" + weightReq);
		currentReqs.add(gson.toJsonTree(toAddReq));

		guildSettings.add("applyReqs", currentReqs);
		int responseCode = database.setGuildSettings(guild.getId(), guildSettings);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed(
			"Added an apply requirement:\n• Slayer - " +
			slayerReq +
			"\n• Skills - " +
			skillsReq +
			"\n• Catacombs - " +
			cataReq +
			"\n• Weight - " +
			weightReq
		);
	}

	public EmbedBuilder removeApplyRequirement(JsonObject guildSettings, String reqNumber) {
		JsonArray currentReqs = guildSettings.getAsJsonArray("applyReqs");

		try {
			JsonElement req = currentReqs.get(Integer.parseInt(reqNumber) - 1);
			currentReqs.remove(Integer.parseInt(reqNumber) - 1);

			guildSettings.add("applyReqs", currentReqs);
			int responseCode = database.setGuildSettings(guild.getId(), guildSettings);
			if (responseCode != 200) {
				return apiFailMessage(responseCode);
			}

			return defaultSettingsEmbed(
				"Removed an apply requirement:\n• Slayer - " +
				higherDepth(req, "slayerReq", 0) +
				"\n• Skills - " +
				higherDepth(req, "skillsReq", 0) +
				"\n• Catacombs - " +
				higherDepth(req, "catacombsReq", 0) +
				"\n• Weight - " +
				higherDepth(req, "weightReq", 0)
			);
		} catch (Exception e) {
			return invalidEmbed(
				"Invalid requirement number. Run `" + guildPrefix + "settings guild <name>` to see the current apply requirements"
			);
		}
	}

	public EmbedBuilder removeGuild(String name) {
		JsonElement guildSettings = database.getGuildSettings(guild.getId(), name);
		if (guildSettings == null || guildSettings.isJsonNull()) {
			return invalidEmbed("No automated guild set up for " + name);
		}
		int responseCode = database.removeGuildSettings(guild.getId(), name);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}
		return defaultSettingsEmbed("Removed automated guild for " + name);
	}

	/* Apply Settings */
	public EmbedBuilder removeApplyBlacklist(String username) {
		UsernameUuidStruct uuidStruct = usernameToUuid(username);
		if (uuidStruct.isNotValid()) {
			return invalidEmbed(uuidStruct.getFailCause());
		}

		JsonArray currentBlacklist = database.getApplyBlacklist(guild.getId());
		for (int i = 0; i < currentBlacklist.size(); i++) {
			if (
				higherDepth(currentBlacklist.get(i), "uuid").getAsString().equals(uuidStruct.getUuid()) ||
				higherDepth(currentBlacklist.get(i), "username").getAsString().equals(uuidStruct.getUsername())
			) {
				currentBlacklist.remove(i);
				int responseCode = database.setApplyBlacklist(guild.getId(), currentBlacklist);
				if (responseCode != 200) {
					return apiFailMessage(responseCode);
				}

				return defaultSettingsEmbed()
					.setDescription("Removed " + nameMcHyperLink(uuidStruct.getUsername(), uuidStruct.getUuid()) + " from the blacklist");
			}
		}

		return invalidEmbed(nameMcHyperLink(uuidStruct.getUsername(), uuidStruct.getUuid()) + " is not blacklisted");
	}

	public EmbedBuilder listApplyBlacklist() {
		JsonArray currentBlacklist = database.getApplyBlacklist(guild.getId());
		EmbedBuilder eb = defaultSettingsEmbed();
		if (currentBlacklist.size() == 0) {
			return eb.setDescription("No one is blacklisted.");
		}

		for (JsonElement blacklisted : currentBlacklist) {
			eb.appendDescription(
				"• " +
				nameMcHyperLink(higherDepth(blacklisted, "username").getAsString(), higherDepth(blacklisted, "uuid").getAsString()) +
				" - " +
				higherDepth(blacklisted, "reason").getAsString() +
				"\n"
			);
		}
		return eb;
	}

	public EmbedBuilder addApplyBlacklist(String username, String reason) {
		UsernameUuidStruct uuidStruct = usernameToUuid(username);
		if (uuidStruct.isNotValid()) {
			return invalidEmbed(uuidStruct.getFailCause());
		}

		JsonArray currentBlacklist = database.getApplyBlacklist(guild.getId());
		JsonElement blacklistedUser = streamJsonArray(currentBlacklist)
			.filter(blacklist ->
				higherDepth(blacklist, "uuid").getAsString().equals(uuidStruct.getUuid()) ||
				higherDepth(blacklist, "username").getAsString().equals(uuidStruct.getUsername())
			)
			.findFirst()
			.orElse(null);
		if (blacklistedUser != null) {
			return invalidEmbed(
				nameMcHyperLink(uuidStruct.getUsername(), uuidStruct.getUuid()) +
				" is already blacklisted with reason `" +
				higherDepth(blacklistedUser, "reason").getAsString() +
				"`"
			);
		}

		currentBlacklist.add(gson.toJsonTree(new ApplyBlacklist(uuidStruct.getUsername(), uuidStruct.getUuid(), reason)));
		int responseCode = database.setApplyBlacklist(guild.getId(), currentBlacklist);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed()
			.setDescription(
				"Blacklisted " + nameMcHyperLink(uuidStruct.getUsername(), uuidStruct.getUuid()) + " with reason `" + reason + "`"
			);
	}

	/* Guild Role Settings */
	public EmbedBuilder getGuildSettings(String name) {
		JsonElement settings = database.getGuildSettings(guild.getId(), name);
		if (settings == null || settings.isJsonNull()) {
			return defaultSettingsEmbed("Invalid setting name. Use `" + guildPrefix + "settings guild` to see all current guild settings.");
		}

		CustomPaginator.Builder paginateBuilder = defaultPaginator(author);
		PaginatorExtras extras = new PaginatorExtras(PaginatorExtras.PaginatorType.EMBED_PAGES);
		extras.addEmbedPage(
			defaultSettingsEmbed()
				.setDescription("**" + displaySettings(settings, "applyEnable").replace("•", "").trim() + "**")
				.addField("Button Message Channel", displaySettings(settings, "applyMessageChannel"), true)
				.addField("Staff Message Channel", displaySettings(settings, "applyStaffChannel"), true)
				.addField("Waiting For Invite Channel", displaySettings(settings, "applyWaitingChannel"), true)
				.addField("Staff Ping Roles", displaySettings(settings, "applyStaffRoles"), true)
				.addField("New Channel Category", displaySettings(settings, "applyCategory"), true)
				.addField("Ironman Only", displaySettings(settings, "applyIronmanOnly"), true)
				.addField("Button Message Text", displaySettings(settings, "applyMessage"), true)
				.addField("Accepted Message", displaySettings(settings, "applyAcceptMessage"), true)
				.addField("Waitlisted Message", displaySettings(settings, "applyWaitlistMessage"), true)
				.addField("Denied Message", displaySettings(settings, "applyDenyMessage"), true)
				.addField("Scammer Check", displaySettings(settings, "applyScammerCheck"), true)
				.addField("Requirements", displaySettings(settings, "applyReqs"), true)
		);

		EmbedBuilder eb = defaultSettingsEmbed()
			.addField(
				"Guild Role",
				displaySettings(settings, "guildMemberRoleEnable") +
				"\n• Guild Name: " +
				displaySettings(settings, "guildId") +
				"\n• Guild Member Role: " +
				displaySettings(settings, "guildMemberRole"),
				false
			);

		StringBuilder guildRanksString = new StringBuilder();
		for (JsonElement guildRank : higherDepth(settings, "guildRanks").getAsJsonArray()) {
			guildRanksString
				.append("\n• ")
				.append(higherDepth(guildRank, "value").getAsString())
				.append(" - <@&")
				.append(higherDepth(guildRank, "roleId").getAsString())
				.append(">");
		}
		eb.addField(
			"Guild Ranks",
			displaySettings(settings, "guildRanksEnable") +
			(guildRanksString.length() > 0 ? guildRanksString.toString() : "\n• No guild ranks set"),
			false
		);

		eb.addField("Guild Counter", higherDepth(settings, "guildCounterEnable", "false").equals("true") ? "Enabled" : "Disabled", false);
		extras.addEmbedPage(eb);
		paginateBuilder.setPaginatorExtras(extras).build().paginate(channel, 0);
		return null;
	}

	/* Roles Settings */
	public EmbedBuilder getRoleSettings(String roleName) {
		Map<String, Integer> rolePageMap = new HashMap<>();
		rolePageMap.put("sven", 2);
		rolePageMap.put("rev", 3);
		rolePageMap.put("tara", 4);
		rolePageMap.put("enderman", 5);
		rolePageMap.put("alchemy", 6);
		rolePageMap.put("combat", 7);
		rolePageMap.put("fishing", 8);
		rolePageMap.put("farming", 9);
		rolePageMap.put("foraging", 10);
		rolePageMap.put("carpentry", 11);
		rolePageMap.put("mining", 12);
		rolePageMap.put("taming", 13);
		rolePageMap.put("enchanting", 14);
		rolePageMap.put("catacombs", 15);
		rolePageMap.put("weight", 16);
		rolePageMap.put("guild_member", 17);
		rolePageMap.put("guild_ranks", 18);
		rolePageMap.put("coins", 19);
		rolePageMap.put("fairy_souls", 20);
		rolePageMap.put("slot_collector", 21);
		rolePageMap.put("pet_enthusiast", 22);
		rolePageMap.put("total_slayer", 23);
		rolePageMap.put("slayer_nine", 24);
		rolePageMap.put("skill_average", 25);
		rolePageMap.put("pet_score", 26);
		rolePageMap.put("dungeon_secrets", 27);
		rolePageMap.put("accessory_count", 28);
		rolePageMap.put("networth", 29);
		rolePageMap.put("ironman", 30);
		rolePageMap.put("maxed_collections", 31);

		if (rolePageMap.containsKey(roleName)) {
			getRolesSettings(database.getRolesSettings(guild.getId())).build().paginate(channel, rolePageMap.get(roleName));
			return null;
		} else {
			try {
				int roleIndex = Integer.parseInt(roleName);
				getRolesSettings(database.getRolesSettings(guild.getId())).build().paginate(channel, roleIndex);
				return null;
			} catch (Exception ignored) {}
		}

		return invalidEmbed("Invalid role name or index");
	}

	public CustomPaginator.Builder getRolesSettings(JsonElement rolesSettings) {
		CustomPaginator.Builder paginateBuilder = defaultPaginator(author).setColumns(1).setItemsPerPage(1);

		ArrayList<String> pageTitles = new ArrayList<>();
		pageTitles.add("Roles Settings");
		ArrayList<String> roleNames = getJsonKeys(rolesSettings);
		StringBuilder pageNumbers = new StringBuilder();
		for (int i = 1; i < roleNames.size(); i++) {
			pageNumbers.append("\n**Page ").append(i + 1).append(":** ").append(roleNames.get(i));
		}

		paginateBuilder.addItems(
			"**Automated Roles " +
			(higherDepth(rolesSettings, "enable").getAsString().equals("true") ? "Enabled" : "Disabled") +
			"**" +
			pageNumbers
		);
		roleNames.remove("enable");
		roleNames.remove("useHighest");
		for (String roleName : roleNames) {
			JsonElement currentRoleSettings = higherDepth(rolesSettings, roleName);
			StringBuilder ebFieldString = new StringBuilder();

			if (higherDepth(currentRoleSettings, "enable") == null) {
				database.setRoleSettings(guild.getId(), roleName, gson.toJsonTree(new RoleModel()));
				currentRoleSettings = database.getRoleSettings(guild.getId(), roleName);
			}

			switch (roleName) {
				case "guild_member":
					{
						ebFieldString
							.append("**Member role for Hypixel guilds**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add guild_member skyblock_forceful @sbf guild member`\n");
						break;
					}
				case "sven":
					{
						ebFieldString
							.append("**A player's sven packmaster slayer xp**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add sven 1000000 @sven 9`\n");
						break;
					}
				case "rev":
					{
						ebFieldString
							.append("**A player's revenant horror xp slayer**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add rev 400000 @rev 8`\n");
						break;
					}
				case "tara":
					{
						ebFieldString
							.append("**A player's tarantula broodfather slayer xp**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add tara 100000 @tara 7`\n");
						break;
					}
				case "coins":
					{
						ebFieldString
							.append("**Coins in a player's bank and purse**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add coins 1000000 @millionaire`\n");
						break;
					}
				case "alchemy":
				case "combat":
				case "fishing":
				case "farming":
				case "foraging":
				case "carpentry":
				case "mining":
				case "taming":
				case "enchanting":
				case "skill_average":
				case "pet_score":
				case "catacombs":
					{
						ebFieldString
							.append("**A player's ")
							.append(roleName)
							.append(" level**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add ")
							.append(roleName)
							.append(" 30 @")
							.append(roleName)
							.append(" 30`\n");
						break;
					}
				case "fairy_souls":
					{
						ebFieldString
							.append("**Amount of collected fairy souls**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add fairy_souls 50 @50 souls collected`\n");
						break;
					}
				case "slot_collector":
					{
						ebFieldString
							.append("**Number of minion slots excluding upgrades**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add slot_collector 24 @maxed minion slots`\n");
						break;
					}
				case "maxed_collections":
					{
						ebFieldString
							.append("**Number of a player's individually maxed collections**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add maxed_collections 62 @all collections maxed`\n");
						break;
					}
				case "pet_enthusiast":
					{
						ebFieldString
							.append("**Having a level 100 epic or legendary pet that is not an enchanting or alchemy pet**\nExample: `")
							.append(guildPrefix)
							.append("settings roles set pet_enthusiast @level 100 pet`\n");
						break;
					}
				case "guild_ranks":
					{
						ebFieldString
							.append("**If a player is in the guild set in `")
							.append(guildPrefix)
							.append(
								"settings guild`, they will be given the corresponding rank role set there**\nNote: this role can only be enabled, disabled, and linked here. To modify guild ranks use `"
							)
							.append(guildPrefix)
							.append("settings guild [name]`\n");
						break;
					}
				case "slayer_nine":
					{
						ebFieldString
							.append("**The number of level nine slayers a player has**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add slayer_nine 3 @role`\n");
						break;
					}
				case "ironman":
					{
						ebFieldString
							.append("**Playing on a ironman profile**\nExample: `")
							.append(guildPrefix)
							.append("settings roles set ironman @ironman`\n");
						break;
					}
				case "dungeon_secrets":
					{
						ebFieldString
							.append("**A player's dungeon secrets count**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add dungeon_secrets 25000 @secret sweat`\n");
						break;
					}
				case "accessory_count":
					{
						ebFieldString
							.append("**A player's dungeon unique accessory count**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add accessory_count 75 @accessory collector`\n");
						break;
					}
				case "networth":
					{
						ebFieldString
							.append("**A player's networth**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add networth 1000000000 @billionaire`\n");
						break;
					}
				case "enderman":
					{
						ebFieldString
							.append("**A player's voidgloom seraph slayer xp**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add enderman 100000 @enderman 7`\n");
						break;
					}
				case "weight":
					{
						ebFieldString
							.append("**A player's weight**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add weight 5000 @5k weight`\n");
						break;
					}
				case "total_slayer":
					ebFieldString
						.append("**A player's total slayer xp**\nExample: `")
						.append(guildPrefix)
						.append("settings roles add total_slayer 1000000 @1m slayer`\n");
					break;
			}

			ebFieldString
				.append("\nSettings\n")
				.append("**")
				.append(
					higherDepth(currentRoleSettings, "enable") != null &&
						higherDepth(currentRoleSettings, "enable").getAsString().equals("true")
						? "• Enabled"
						: "• Disabled"
				)
				.append("**");

			if (roleName.equals("guild_ranks")) {
				if (higherDepth(currentRoleSettings, "levels").getAsJsonArray().size() == 0) {
					ebFieldString.append("\n• No ranks added");
				} else {
					for (JsonElement roleLevel : higherDepth(currentRoleSettings, "levels").getAsJsonArray()) {
						String rName = higherDepth(roleLevel, "value").getAsString();
						ebFieldString
							.append("\n• ")
							.append(rName)
							.append(" (view the ranks using `")
							.append(guildPrefix)
							.append("settings guild ")
							.append(rName)
							.append("`)");
					}
				}
				pageTitles.add(roleName);
			} else if (isOneLevelRole(roleName)) {
				ebFieldString.append(
					higherDepth(currentRoleSettings, "levels").getAsJsonArray().size() > 0
						? "\n• <@&" + higherDepth(currentRoleSettings, "levels.[0].roleId").getAsString() + ">"
						: "\n • No role set"
				);
				pageTitles.add(roleName + " (__one level role__)");
			} else {
				if (roleName.equals("guild_member")) {
					for (JsonElement roleLevel : higherDepth(currentRoleSettings, "levels").getAsJsonArray()) {
						String guildId = higherDepth(roleLevel, "value").getAsString();
						HypixelResponse guildJson = getGuildFromId(guildId);
						if (!guildJson.isNotValid()) {
							ebFieldString
								.append("\n• ")
								.append(guildJson.get("name").getAsString())
								.append(" - ")
								.append("<@&")
								.append(higherDepth(roleLevel, "roleId").getAsString())
								.append(">");
						} else {
							ebFieldString
								.append("\n• ")
								.append("Invalid guild")
								.append(" - ")
								.append("<@&")
								.append(higherDepth(roleLevel, "roleId").getAsString())
								.append(">");
						}
					}
				} else {
					for (JsonElement roleLevel : higherDepth(currentRoleSettings, "levels").getAsJsonArray()) {
						ebFieldString
							.append("\n• ")
							.append(higherDepth(roleLevel, "value").getAsString())
							.append(" - ")
							.append("<@&")
							.append(higherDepth(roleLevel, "roleId").getAsString())
							.append(">");
					}
				}

				if (higherDepth(currentRoleSettings, "levels").getAsJsonArray().size() == 0) {
					ebFieldString.append("\n• No levels added");
				}

				pageTitles.add(roleName);
			}
			paginateBuilder.addItems(ebFieldString.toString());
		}

		return paginateBuilder.setPaginatorExtras(new PaginatorExtras().setTitles(pageTitles));
	}

	public boolean allowRolesEnable() {
		JsonObject currentSettings = database.getRolesSettings(guild.getId()).getAsJsonObject();
		currentSettings.remove("enable");
		currentSettings.remove("useHighest");
		return getJsonKeys(currentSettings).stream().anyMatch(role -> higherDepth(currentSettings, role + ".enable", false));
	}

	public EmbedBuilder setRolesEnable(boolean enable) {
		if (!enable) {
			JsonObject newRolesJson = database.getRolesSettings(guild.getId()).getAsJsonObject();
			newRolesJson.addProperty("enable", false);
			int responseCode = database.setRolesSettings(guild.getId(), newRolesJson);
			if (responseCode != 200) {
				return apiFailMessage(responseCode);
			}

			return defaultSettingsEmbed("**Automatic roles:** disabled");
		}

		if (!allowRolesEnable()) {
			return invalidEmbed("At least one individual role must be enabled");
		}

		JsonObject newRolesJson = database.getRolesSettings(guild.getId()).getAsJsonObject();
		newRolesJson.addProperty("enable", true);
		int responseCode = database.setRolesSettings(guild.getId(), newRolesJson);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed("**Roles:** enabled");
	}

	public EmbedBuilder setRolesUseHighest(boolean enable) {
		JsonObject newRolesJson = database.getRolesSettings(guild.getId()).getAsJsonObject();
		newRolesJson.addProperty("useHighest", "" + enable);
		int responseCode = database.setRolesSettings(guild.getId(), newRolesJson);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed("**Use highest amount:** " + enable);
	}

	public EmbedBuilder setRoleEnable(String roleName, boolean enable) {
		if (roleName.equals("all")) {
			JsonObject roleSettings = database.getRolesSettings(guild.getId()).getAsJsonObject();
			if (enable) {
				List<String> enabled = new ArrayList<>();
				for (Entry<String, JsonElement> role : roleSettings.entrySet()) {
					if (!higherDepth(role.getValue(), "enable", false) && higherDepth(role.getValue(), "levels.[0]") != null) {
						JsonObject curRole = role.getValue().getAsJsonObject();
						curRole.addProperty("enable", "true");
						roleSettings.add(role.getKey(), curRole);
						enabled.add(role.getKey());
					}
				}
				int responseCode = database.setRolesSettings(guild.getId(), roleSettings);
				if (responseCode != 200) {
					return apiFailMessage(responseCode);
				}

				return defaultSettingsEmbed("**Enabled:** " + (enabled.size() > 0 ? String.join(", ", enabled) : " no roles"));
			} else {
				for (Entry<String, JsonElement> role : roleSettings.entrySet()) {
					if (!role.getKey().equals("enable")) {
						JsonObject curRole = role.getValue().getAsJsonObject();
						curRole.addProperty("enable", "false");
						roleSettings.add(role.getKey(), curRole);
					}
				}
				int responseCode = database.setRolesSettings(guild.getId(), roleSettings);
				if (responseCode != 200) {
					return apiFailMessage(responseCode);
				}

				return defaultSettingsEmbed("Disabled all automatic roles");
			}
		}

		JsonObject currentRoleSettings = null;
		try {
			currentRoleSettings = database.getRoleSettings(guild.getId(), roleName).getAsJsonObject();
		} catch (Exception ignored) {}
		if (currentRoleSettings == null) {
			return invalidEmbed("Invalid role name");
		}

		if (!enable) {
			currentRoleSettings.addProperty("enable", "false");
			int responseCode = database.setRoleSettings(guild.getId(), roleName, currentRoleSettings);
			if (responseCode != 200) {
				return apiFailMessage(responseCode);
			}

			return defaultSettingsEmbed("**Disabled:** " + roleName);
		}

		if (currentRoleSettings.get("levels").getAsJsonArray().size() != 0) {
			currentRoleSettings.addProperty("enable", "true");
			int responseCode = database.setRoleSettings(guild.getId(), roleName, currentRoleSettings);
			if (responseCode != 200) {
				return apiFailMessage(responseCode);
			}

			return defaultSettingsEmbed("**Enabled:** " + roleName);
		}
		return invalidEmbed("Specified role must have at least one level");
	}

	public EmbedBuilder addRoleLevel(String roleName, String roleValue, String roleMention) {
		String guildName = "";
		if (roleName.equals("guild_member")) {
			HypixelResponse guildJson = getGuildFromName(roleValue);
			if (guildJson.isNotValid()) {
				return invalidEmbed(guildJson.getFailCause());
			}
			roleValue = guildJson.get("_id").getAsString();
			guildName = guildJson.get("name").getAsString();
		} else if (roleName.equals("guild_ranks")) {
			JsonObject newRoleSettings = database.getRoleSettings(guild.getId(), "guild_ranks").getAsJsonObject();
			JsonArray currentLevels = newRoleSettings.get("levels").getAsJsonArray();

			if (currentLevels.size() >= 2) {
				return invalidEmbed("This role has reached the max limit of levels (2/2)");
			}
			JsonElement guildRoleSettings = database.getGuildSettings(guild.getId(), roleValue);
			if (
				guildRoleSettings != null &&
				!guildRoleSettings.isJsonNull() &&
				higherDepth(guildRoleSettings, "guildRanksEnable").getAsString().equalsIgnoreCase("true")
			) {
				for (JsonElement level : currentLevels) {
					if (higherDepth(level, "value").getAsString().equals(roleValue)) {
						currentLevels.remove(level);
						break;
					}
				}

				currentLevels.add(gson.toJsonTree(new RoleObject(roleValue, roleValue)));
				newRoleSettings.add("levels", currentLevels);

				int responseCode = database.setRoleSettings(guild.getId(), roleName, newRoleSettings);
				if (responseCode != 200) {
					return apiFailMessage(responseCode);
				}

				return defaultSettingsEmbed("Added guild ranks for automatic guild - `" + roleValue + "`");
			}

			return invalidEmbed("Invalid guild role name or guild ranks not enabled");
		} else if (isOneLevelRole(roleName)) {
			return invalidEmbed(
				"These roles do not support levels. Use `" + guildPrefix + "settings roles set <roleName> <@role>` instead"
			);
		} else {
			try {
				Long.parseLong(roleValue);
			} catch (Exception e) {
				return invalidEmbed("Role value must be an integer");
			}
		}

		Object eb = checkRole(roleMention);
		if (eb instanceof EmbedBuilder) {
			return ((EmbedBuilder) eb);
		}
		Role role = ((Role) eb);

		JsonObject newRoleSettings;
		try {
			newRoleSettings = database.getRoleSettings(guild.getId(), roleName).getAsJsonObject();
		} catch (Exception e) {
			return invalidEmbed("Invalid role");
		}

		JsonObject allRoleSettings = database.getRolesSettings(guild.getId()).getAsJsonObject();
		int totalRoleCount = 0;
		for (Entry<String, JsonElement> i : allRoleSettings.entrySet()) {
			try {
				totalRoleCount += higherDepth(i.getValue(), "levels").getAsJsonArray().size();
			} catch (Exception ignored) {}
		}
		if (totalRoleCount >= 120) {
			return invalidEmbed("You have reached the max amount of total levels (120/120)");
		}

		JsonArray currentLevels = newRoleSettings.get("levels").getAsJsonArray();
		for (JsonElement level : currentLevels) {
			if (higherDepth(level, "value").getAsString().equals(roleValue)) {
				currentLevels.remove(level);
				break;
			}
		}

		currentLevels.add(gson.toJsonTree(new RoleObject(roleValue, role.getId())));

		if (!roleName.equals("guild_member")) {
			currentLevels =
				collectJsonArray(streamJsonArray(currentLevels).sorted(Comparator.comparingInt(o -> higherDepth(o, "value").getAsInt())));
		} else {
			roleValue = guildName;
		}
		newRoleSettings.add("levels", currentLevels);

		int responseCode = database.setRoleSettings(guild.getId(), roleName, newRoleSettings);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed("Set " + roleName + " " + roleValue + " to " + role.getAsMention());
	}

	public EmbedBuilder removeRoleLevel(String roleName, String value) {
		if (isOneLevelRole(roleName)) {
			return defaultEmbed(
				"These roles do not support levels. Use `" + guildPrefix + "settings roles set <roleName> <@role>` instead"
			);
		}

		JsonObject currentRoleSettings;
		try {
			currentRoleSettings = database.getRoleSettings(guild.getId(), roleName).getAsJsonObject();
		} catch (Exception e) {
			return invalidEmbed("Invalid role name");
		}

		JsonArray currentLevels = currentRoleSettings.get("levels").getAsJsonArray();
		for (JsonElement level : currentLevels) {
			String currentValue = higherDepth(level, "value").getAsString();
			if (roleName.equals("guild_member")) {
				HypixelResponse guildJson = getGuildFromId(higherDepth(level, "value").getAsString());
				if (guildJson.isNotValid()) {
					return invalidEmbed(guildJson.getFailCause());
				}
				currentValue = guildJson.get("name").getAsString();
			}

			if (currentValue.equalsIgnoreCase(value.replace("_", " "))) {
				currentLevels.remove(level);
				currentRoleSettings.add("levels", currentLevels);
				int responseCode = database.setRoleSettings(guild.getId(), roleName, currentRoleSettings);
				if (responseCode != 200) {
					return apiFailMessage(responseCode);
				}

				currentRoleSettings = database.getRoleSettings(guild.getId(), roleName).getAsJsonObject();

				if (currentRoleSettings.get("levels").getAsJsonArray().size() == 0) {
					setRoleEnable(roleName, false);
				}

				if (!allowRolesEnable()) {
					setRolesEnable(false);
				}

				return defaultSettingsEmbed("Removed " + roleName + " " + value);
			}
		}
		return invalidEmbed("Invalid role value");
	}

	public EmbedBuilder setOneLevelRole(String roleName, String roleMention) {
		if (!isOneLevelRole(roleName)) {
			return invalidEmbed(
				"This role is not a one level role. Use `" + guildPrefix + "settings roles add <roleName> <value> <@role>` instead"
			);
		}

		Object eb = checkRole(roleMention);
		if (eb instanceof EmbedBuilder) {
			return ((EmbedBuilder) eb);
		}
		Role role = ((Role) eb);

		JsonObject newRoleSettings;
		try {
			newRoleSettings = database.getRoleSettings(guild.getId(), roleName).getAsJsonObject();
		} catch (Exception e) {
			return invalidEmbed("Invalid role");
		}

		JsonArray currentLevels = new JsonArray();
		currentLevels.add(gson.toJsonTree(new RoleObject("default", role.getId())));
		newRoleSettings.add("levels", currentLevels);

		int responseCode = database.setRoleSettings(guild.getId(), roleName, newRoleSettings);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed(roleName + " set to " + role.getAsMention());
	}

	public static boolean isOneLevelRole(String roleName) {
		return roleName.equals("pet_enthusiast") || roleName.equals("ironman");
	}

	/* Verify Settings */
	public String getCurrentVerifySettings(JsonElement verifySettings) {
		String ebFieldString = "";
		ebFieldString += "**" + displaySettings(verifySettings, "enable") + "**";
		ebFieldString += "\n**• Message Text:** " + displaySettings(verifySettings, "messageText");
		ebFieldString += "\n**• Channel:** " + displaySettings(verifySettings, "messageTextChannelId");
		ebFieldString += "\n**• Verified Role(s):** " + displaySettings(verifySettings, "verifiedRoles");
		ebFieldString += "\n**• Verified Remove Role:** " + displaySettings(verifySettings, "verifiedRemoveRole");
		ebFieldString += "\n**• Nickname Template:** " + displaySettings(verifySettings, "verifiedNickname");
		ebFieldString += "\n**• Member join sync:** " + displaySettings(verifySettings, "enableMemberJoinSync");
		return ebFieldString;
	}

	public boolean allowVerifyEnable() {
		JsonObject currentSettings = database.getVerifySettings(guild.getId()).getAsJsonObject();
		currentSettings.remove("previousMessageId");

		try {
			for (Entry<String, JsonElement> key : currentSettings.entrySet()) {
				if (key.getValue().getAsString().length() == 0) {
					return false;
				}
			}
		} catch (Exception ignored) {}
		return true;
	}

	public EmbedBuilder setVerifyEnable(String enable) {
		if (enable.equalsIgnoreCase("true") || enable.equalsIgnoreCase("false")) {
			int responseCode = updateVerifySettings("enable", enable);
			if (responseCode != 200) {
				return invalidEmbed("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription(
				"**Verify:** " +
				(enable.equalsIgnoreCase("true") ? "enabled" : "disabled") +
				"\nRun `" +
				guildPrefix +
				"reload` to reload the settings"
			);
			return eb;
		}
		return defaultEmbed("Invalid Input");
	}

	public EmbedBuilder setVerifyMessageText(String verifyText) {
		if (verifyText.length() > 0) {
			if (EmojiParser.parseToAliases(verifyText).length() > 1500) {
				return invalidEmbed("Text cannot be longer than 1500 letters!");
			}

			int responseCode = updateVerifySettings("messageText", EmojiParser.parseToAliases(verifyText));
			if (responseCode != 200) {
				return invalidEmbed("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Verify message set to:** " + verifyText);
			return eb;
		}
		return defaultEmbed("Invalid Input");
	}

	public EmbedBuilder setVerifyMessageTextChannelId(String textChannel) {
		try {
			TextChannel verifyMessageTextChannel = guild.getTextChannelById(textChannel.replaceAll("[<#>]", ""));
			try {
				verifyMessageTextChannel.getManager().setSlowmode(5).queue();
			} catch (Exception ignored) {}
			int responseCode = updateVerifySettings("messageTextChannelId", verifyMessageTextChannel.getId());
			if (responseCode != 200) {
				return invalidEmbed("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Verify text channel set to:** " + verifyMessageTextChannel.getAsMention());
			return eb;
		} catch (Exception ignored) {}
		return defaultEmbed("Invalid Text Channel");
	}

	public EmbedBuilder setVerifyNickname(String nickname) {
		if (!nickname.contains("[IGN]")) {
			if (nickname.equalsIgnoreCase("none")) {
				int responseCode = updateVerifySettings("verifiedNickname", "none");

				if (responseCode != 200) {
					return invalidEmbed("API returned response code " + responseCode);
				}

				EmbedBuilder eb = defaultEmbed("Settings");
				eb.setDescription("**Verify nickname disabled**");
				return eb;
			}
			return invalidEmbed("Nickname must contain [IGN] parameter");
		}

		if (nickname.contains("[GUILD_RANK]")) {
			List<AutomatedGuild> guildRoleSettings = database.getAllGuildSettings(guild.getId());
			guildRoleSettings.removeIf(o1 -> {
				try {
					return !o1.getGuildRanksEnable().equalsIgnoreCase("true");
				} catch (Exception e) {
					return true;
				}
			});
			if (guildRoleSettings.size() == 0) {
				return invalidEmbed(
					"At least one guild ranks must be enabled in " + guildPrefix + "`settings guild [name]` to use the [GUILD_RANK] prefix"
				);
			}
		}

		if (nickname.replace("[IGN]", "").length() > 15) {
			return invalidEmbed("Nickname prefix and/or postfix must be less than or equal to 15 letters");
		}

		int responseCode = updateVerifySettings("verifiedNickname", nickname);

		if (responseCode != 200) {
			return invalidEmbed("API returned response code " + responseCode);
		}

		EmbedBuilder eb = defaultEmbed("Settings");
		eb.setDescription("**Verify nickname set to:** " + nickname);
		return eb;
	}

	public EmbedBuilder removeVerifyRole(String roleMention) {
		Role verifyRole;
		try {
			verifyRole = guild.getRoleById(roleMention.replaceAll("[<@&>]", ""));
			if ((verifyRole.isPublicRole() || verifyRole.isManaged())) {
				return invalidEmbed("Invalid role");
			}
		} catch (Exception e) {
			return defaultEmbed("Invalid Role");
		}

		JsonElement verifySettings = database.getVerifySettings(guild.getId());
		JsonArray currentVerifyRoles = higherDepth(verifySettings, "verifiedRoles").getAsJsonArray();

		for (int i = currentVerifyRoles.size() - 1; i >= 0; i--) {
			if (currentVerifyRoles.get(i).getAsString().equals(verifyRole.getId())) {
				currentVerifyRoles.remove(i);
			}
		}

		int responseCode = database.setVerifyRolesSettings(guild.getId(), currentVerifyRoles);

		if (responseCode != 200) {
			return invalidEmbed("API returned response code " + responseCode);
		}

		guildMap.get(guild.getId()).verifyGuild.reloadSettingsJson(verifySettings);

		if (currentVerifyRoles.size() == 0) {
			updateVerifySettings("enable", "false");
		}

		EmbedBuilder eb = defaultEmbed("Settings");
		return eb.setDescription("**Removed verify role:** " + verifyRole.getAsMention());
	}

	public EmbedBuilder addVerifyRole(String roleMention) {
		Role verifyRole;
		try {
			verifyRole = guild.getRoleById(roleMention.replaceAll("[<@&>]", ""));
			if ((verifyRole.isPublicRole() || verifyRole.isManaged())) {
				return invalidEmbed("Role cannot be managed or @everyone");
			}
		} catch (Exception e) {
			return defaultEmbed("Invalid Role");
		}

		JsonElement verifySettings = database.getVerifySettings(guild.getId());
		JsonArray currentVerifyRoles = higherDepth(verifySettings, "verifiedRoles").getAsJsonArray();
		if (currentVerifyRoles.size() >= 3) {
			return defaultEmbed("You have reached the max number of verify roles (3/3)");
		}

		currentVerifyRoles.add(verifyRole.getId());
		int responseCode = database.setVerifyRolesSettings(guild.getId(), currentVerifyRoles);

		if (responseCode != 200) {
			return invalidEmbed("API returned response code " + responseCode);
		}
		guildMap.get(guild.getId()).verifyGuild.reloadSettingsJson(verifySettings);

		EmbedBuilder eb = defaultEmbed("Settings");
		return eb.setDescription("**Verify role added:** " + verifyRole.getAsMention());
	}

	public EmbedBuilder setVerifyRemoveRole(String roleMention) {
		if (roleMention.equalsIgnoreCase("none")) {
			JsonObject verifySettings = database.getVerifySettings(guild.getId()).getAsJsonObject();
			verifySettings.addProperty("verifiedRemoveRole", "none");
			int responseCode = database.setVerifySettings(guild.getId(), verifySettings);
			if (responseCode != 200) {
				return apiFailMessage(responseCode);
			}
			guildMap.get(guild.getId()).verifyGuild.reloadSettingsJson(verifySettings);
			return defaultSettingsEmbed("Verify remove role removed");
		}

		Object eb = checkRole(roleMention);
		if (eb instanceof EmbedBuilder) {
			return ((EmbedBuilder) eb);
		}
		Role role = ((Role) eb);

		JsonObject verifySettings = database.getVerifySettings(guild.getId()).getAsJsonObject();
		verifySettings.addProperty("verifiedRemoveRole", role.getId());
		int responseCode = database.setVerifySettings(guild.getId(), verifySettings);

		if (responseCode != 200) {
			return invalidEmbed("API returned response code " + responseCode);
		}
		guildMap.get(guild.getId()).verifyGuild.reloadSettingsJson(verifySettings);

		return defaultSettingsEmbed("**Verify remove role set:** " + role.getAsMention());
	}

	public EmbedBuilder setVerifySyncEnable(String enable) {
		JsonObject currentSettings = database.getVerifySettings(guild.getId()).getAsJsonObject();

		String nickname = higherDepth(currentSettings, "verifiedNickname").getAsString();
		if ((nickname.isEmpty() || nickname.equals("none")) && higherDepth(currentSettings, "verifiedRoles").getAsJsonArray().size() == 0) {
			return invalidEmbed("You must have at least on verify role or a nickname template set.");
		}

		int responseCode = updateVerifySettings("enableMemberJoinSync", enable);
		if (responseCode != 200) {
			return invalidEmbed("API returned response code " + responseCode);
		}

		EmbedBuilder eb = defaultEmbed("Settings");
		return eb.setDescription("Member join sync " + (enable.equals("true") ? "enabled" : "disabled"));
	}

	public int updateVerifySettings(String key, String newValue) {
		JsonObject newVerifySettings = database.getVerifySettings(guild.getId()).getAsJsonObject();
		newVerifySettings.addProperty(key, newValue);
		if (key.equals("verifiedNickname") || key.equals("enableMemberJoinSync")) {
			guildMap.get(guild.getId()).verifyGuild.reloadSettingsJson(newVerifySettings);
		}
		return database.setVerifySettings(guild.getId(), newVerifySettings);
	}

	/* Mee6 */
	public EmbedBuilder getMee6DataSettings() {
		JsonObject settings = getMee6Json();

		EmbedBuilder eb = defaultSettingsEmbed(higherDepth(settings, "enable", "false").equals("true") ? "**Enabled**" : "**Disabled**");
		JsonArray curRoles = higherDepth(settings, "levels").getAsJsonArray();
		if (curRoles.size() == 0) {
			eb.appendDescription("\n**• Leveling roles:** none");
		} else {
			for (JsonElement curRole : curRoles) {
				eb.appendDescription(
					"\n• **Level " +
					higherDepth(curRole, "value").getAsString() +
					":** <@&" +
					higherDepth(curRole, "roleId").getAsString() +
					">"
				);
			}
		}

		return eb;
	}

	public EmbedBuilder setMee6Enable(boolean enable) {
		if (!enable) {
			int responseCode = setMee6Settings("enable", "false");
			if (responseCode != 200) {
				return apiFailMessage(responseCode);
			}

			return defaultSettingsEmbed("Disabled Mee6 automatic leveling roles.");
		}

		JsonElement settings = getMee6Json();

		if (higherDepth(settings, "levels").getAsJsonArray().size() == 0) {
			return invalidEmbed("You must set at least one leveling role.");
		}
		try {
			if (
				higherDepth(getJson("https://mee6.xyz/api/plugins/levels/leaderboard/" + guild.getId()), "players")
					.getAsJsonArray()
					.size() ==
				0
			) {
				return invalidEmbed("The Mee6 leveling leaderboard must be public for this server.");
			}
		} catch (Exception e) {
			return invalidEmbed("The Mee6 leveling leaderboard must be public for this server.");
		}

		int responseCode = setMee6Settings("enable", "true");
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed("Enabled Mee6 automatic leveling roles.");
	}

	public EmbedBuilder addMee6Role(String level, String roleMention) {
		Object eb = checkRole(roleMention);
		if (eb instanceof EmbedBuilder) {
			return ((EmbedBuilder) eb);
		}
		Role role = ((Role) eb);

		int intLevel = -1;
		try {
			intLevel = Integer.parseInt(level);
		} catch (Exception ignored) {}
		if (intLevel <= 0 || intLevel >= 250) {
			return invalidEmbed("The level must be an integer between 0 and 250.");
		}

		JsonObject settings = getMee6Json();
		JsonArray ranks = settings.getAsJsonArray("levels");

		if (ranks.size() >= 10) {
			return defaultEmbed("You have reached the max amount of Mee6 roles (10/10).");
		}
		for (int i = ranks.size() - 1; i >= 0; i--) {
			if (higherDepth(ranks.get(i), "value").getAsInt() == intLevel) {
				ranks.remove(i);
			}
		}

		ranks.add(gson.toJsonTree(new RoleObject("" + intLevel, role.getId())));
		ranks = collectJsonArray(streamJsonArray(ranks).sorted(Comparator.comparingInt(r -> higherDepth(r, "value").getAsInt())));
		settings.add("levels", ranks);

		int responseCode = setMee6Settings(settings);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed("Added level " + intLevel + " role as " + role.getAsMention() + ".");
	}

	public EmbedBuilder removeMee6Role(String level) {
		int intLevel = -1;
		try {
			intLevel = Integer.parseInt(level);
		} catch (Exception ignored) {}
		if (intLevel <= 0 || intLevel >= 250) {
			return invalidEmbed("The level must be an integer between 0 and 250.");
		}

		JsonObject curSettings = getMee6Json();
		JsonArray curRanks = curSettings.get("levels").getAsJsonArray();
		for (JsonElement rank : curRanks) {
			if (higherDepth(rank, "value").getAsInt() == intLevel) {
				curRanks.remove(rank);
				if (curRanks.size() == 0) {
					curSettings.addProperty("enable", "false");
				}
				curSettings.add("levels", curRanks);

				int responseCode = database.setMee6Settings(guild.getId(), curSettings);
				if (responseCode != 200) {
					return apiFailMessage(responseCode);
				}

				return defaultSettingsEmbed("Removed the role for level " + intLevel + ".");
			}
		}

		return invalidEmbed("There is no role set for level " + intLevel + ".");
	}

	public JsonObject getMee6Json() {
		return serverSettings.getAsJsonObject("mee6Data");
	}

	public int setMee6Settings(String key, String newValue) {
		JsonObject newSettings = getMee6Json();
		newSettings.addProperty(key, newValue);
		return setMee6Settings(newSettings);
	}

	public int setMee6Settings(JsonElement newSettings) {
		return database.setMee6Settings(guild.getId(), newSettings);
	}

	/* Miscellaneous */
	public EmbedBuilder setHypixelKey(String newKey) {
		try {
			newKey = higherDepth(getJson("https://api.hypixel.net/key?key=" + newKey), "record.key").getAsString();
		} catch (Exception e) {
			return invalidEmbed("Provided Hypixel API key is invalid.");
		}

		int responseCode = database.setServerHypixelApiKey(guild.getId(), newKey);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed("Set the Hypixel API key. Note that no one can view the key for the privacy of the key owner.");
	}

	public EmbedBuilder deleteHypixelKey() {
		int responseCode = database.setServerHypixelApiKey(guild.getId(), "");
		if (responseCode != 200) {
			apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed("Deleted the server's Hypixel API key.");
	}

	public EmbedBuilder setPrefix(String prefix) {
		if (prefix.length() == 0 || prefix.length() > 5) {
			return invalidEmbed("The prefix must be a least one character and no more than five.");
		}

		int responseCode = database.setPrefix(guild.getId(), prefix);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		guildMap.get(guild.getId()).setPrefix(prefix);
		return defaultSettingsEmbed("**Set the server's prefix to:** " + prefix);
	}

	public EmbedBuilder resetPrefix() {
		int responseCode = database.setPrefix(guild.getId(), null);

		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		guildMap.get(guild.getId()).setPrefix(DEFAULT_PREFIX);
		return defaultSettingsEmbed("**Reset the server's prefix to:** " + DEFAULT_PREFIX);
	}

	private EmbedBuilder setPartyFinderCategory(String category) {
		try {
			if (category.equalsIgnoreCase("none")) {
				int responseCode = database.setPartyFinderCategoryId(guild.getId(), "none");
				if (responseCode != 200) {
					return invalidEmbed("API returned response code " + responseCode);
				}
				guildMap.get(guild.getId()).setPartyFinderCategory(null);
				return defaultSettingsEmbed("**Party finder new channel category disabled**");
			} else {
				Category pfCategory = guild.getCategoryById(category.replaceAll("[<#>]", ""));
				int responseCode = database.setPartyFinderCategoryId(guild.getId(), pfCategory.getId());
				if (responseCode != 200) {
					return invalidEmbed("API returned response code " + responseCode);
				}
				guildMap.get(guild.getId()).setPartyFinderCategory(pfCategory);

				return defaultSettingsEmbed("**Party finder new channel category set to:** <#" + pfCategory.getId() + ">");
			}
		} catch (Exception ignored) {}
		return invalidEmbed("Invalid guild category id");
	}

	public EmbedBuilder setApplyGuestRole(String roleMention) {
		if (roleMention.equalsIgnoreCase("none")) {
			int responseCode = database.setApplyGuestRole(guild.getId(), "none");
			if (responseCode != 200) {
				return invalidEmbed("API returned responseet code " + responseCode);
			}

			guildMap.get(guild.getId()).setApplyGuestRole(null);
			return defaultSettingsEmbed("Set guest role to: none");
		}

		Object eb = checkRole(roleMention);
		if (eb instanceof EmbedBuilder) {
			return ((EmbedBuilder) eb);
		}
		Role role = ((Role) eb);

		if (
			database
				.getAllGuildSettings(guild.getId())
				.stream()
				.noneMatch(g -> g != null && g.getApplyEnable() != null && g.getApplyEnable().equals("true"))
		) {
			return invalidEmbed("There must be at least one active application system to set a guest role");
		}

		int responseCode = database.setApplyGuestRole(guild.getId(), role.getId());
		if (responseCode != 200) {
			return invalidEmbed("API returned responseet code " + responseCode);
		}

		guildMap.get(guild.getId()).setApplyGuestRole(role);
		return defaultSettingsEmbed("Set guest role to: " + role.getAsMention());
	}

	/* Helper functions */
	public String displaySettings(JsonElement jsonSettings, String settingName) {
		if (higherDepth(jsonSettings, settingName) != null) {
			if (settingName.equals("applyReqs")) {
				JsonArray reqs = higherDepth(jsonSettings, settingName).getAsJsonArray();

				if (reqs.size() == 0) {
					return "None";
				}

				StringBuilder reqsString = new StringBuilder("\n");
				for (int i = 0; i < reqs.size(); i++) {
					JsonElement req = reqs.get(i);
					String slayerReq = higherDepth(req, "slayerReq").getAsString();
					String skillsReq = higherDepth(req, "skillsReq").getAsString();
					String cataReq = higherDepth(req, "catacombsReq").getAsString();
					String weightReq = higherDepth(req, "weightReq").getAsString();

					reqsString
						.append("`")
						.append(i + 1)
						.append(")` ")
						.append(slayerReq)
						.append(" slayer & ")
						.append(skillsReq)
						.append(" skill avg & ")
						.append(cataReq)
						.append(" cata & ")
						.append(weightReq)
						.append(" weight\n");
				}

				return reqsString.toString();
			} else if (settingName.equals("verifiedRoles") || settingName.equals("applyStaffRoles")) {
				JsonArray roles = higherDepth(jsonSettings, settingName).getAsJsonArray();
				StringBuilder ebStr = new StringBuilder();
				for (JsonElement role : roles) {
					ebStr.append("<@&").append(role.getAsString()).append(">").append(" ");
				}

				if (ebStr.length() == 0) {
					ebStr = new StringBuilder("None");
				}

				return ebStr.toString();
			}

			String currentSettingValue = higherDepth(jsonSettings, settingName).getAsString();
			if (currentSettingValue.length() > 0) {
				switch (settingName) {
					case "applyMessageChannel":
					case "applyWaitingChannel":
					case "applyStaffChannel":
					case "messageTextChannelId":
						return "<#" + currentSettingValue + ">";
					case "roleId":
					case "guildMemberRole":
					case "verifiedRemoveRole":
						return "<@&" + currentSettingValue + ">";
					case "applyCategory":
						try {
							return ("<#" + guild.getCategoryById(currentSettingValue).getId() + ">");
						} catch (PermissionException e) {
							if (e.getMessage().contains("Missing permission")) {
								return ("Missing permission: " + e.getMessage().split("Missing permission: ")[1]);
							}
						}
						break;
					case "applyEnable":
					case "enable":
						return currentSettingValue.equals("true") ? "• Enabled" : "• Disabled";
					case "guildId":
						try {
							HypixelResponse guildJson = getGuildFromId(currentSettingValue);
							return guildJson.get("name").getAsString();
						} catch (Exception e) {
							return ("Error finding guild associated with " + currentSettingValue + " id");
						}
					case "guildMemberRoleEnable":
						return currentSettingValue.equals("true") ? "• Guild role enabled" : "• Guild role disabled";
					case "guildRanksEnable":
						return currentSettingValue.equals("true") ? "• Guild ranks enabled" : "• Guild ranks disabled";
				}
				return currentSettingValue;
			}
		}
		return "None";
	}

	public EmbedBuilder apiFailMessage(int responseCode) {
		return invalidEmbed("API returned response code of `" + responseCode + "`. Please report this to the developer.");
	}

	public EmbedBuilder defaultSettingsEmbed() {
		return defaultSettingsEmbed(null);
	}

	public EmbedBuilder defaultSettingsEmbed(String description) {
		return defaultEmbed("Settings").setDescription(description);
	}

	public Object checkRole(String roleMention) {
		Role role;
		try {
			role = guild.getRoleById(roleMention.replaceAll("[<@&>]", ""));
		} catch (Exception e) {
			return invalidEmbed("The provided role is invalid");
		}

		if (role == null) {
			return invalidEmbed("The provided role does not exist");
		} else if (role.isPublicRole()) {
			return invalidEmbed("The role cannot be the everyone role");
		} else if (role.isManaged()) {
			return invalidEmbed("The role cannot be a managed role");
		}

		return role;
	}

	public EmbedBuilder checkTextChannel(TextChannel channel) {
		if (channel == null) {
			return invalidEmbed("The provided text channel doesn't exist");
		} else if (!channel.canTalk()) {
			return invalidEmbed("I do not have the necessary permissions to talk in the provided channel");
		} else {
			return null;
		}
	}
}
