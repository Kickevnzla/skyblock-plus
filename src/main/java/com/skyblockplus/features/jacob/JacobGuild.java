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

package com.skyblockplus.features.jacob;

import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.skyblockplus.api.serversettings.automatedroles.RoleObject;
import com.skyblockplus.features.listeners.AutomaticGuild;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;

public class JacobGuild {

	public final AutomaticGuild parent;
	public boolean enable = false;
	public List<RoleObject> wantedCrops;
	public TextChannel channel;

	public JacobGuild(JsonElement jacobSettings, AutomaticGuild parent) {
		this.parent = parent;
		reloadSettingsJson(jacobSettings);
	}

	public boolean onFarmingContest(List<String> crops, MessageEmbed embed) {
		try {
			if (enable) {
				if (!channel.canTalk()) {
					parent.logAction(
						defaultEmbed("Jacob Notifications")
							.setDescription("Missing permissions to view or send messages in " + channel.getAsMention())
					);
					return false;
				}

				List<String> roleMentions = new ArrayList<>();
				for (RoleObject wantedCrop : wantedCrops) {
					if (crops.contains(wantedCrop.getValue())) {
						roleMentions.add("<@&" + wantedCrop.getRoleId() + ">");
					}
				}

				if (!roleMentions.isEmpty()) {
					channel.sendMessage(String.join(" ", roleMentions)).setEmbeds(embed).queue();
					return true;
				}
			}
		} catch (Exception e) {
			AutomaticGuild.getLogger().error(parent.guildId, e);
		}
		return false;
	}

	public void reloadSettingsJson(JsonElement jacobSettings) {
		try {
			enable = higherDepth(jacobSettings, "enable", false);
			if (enable) {
				channel = jda.getGuildById(parent.guildId).getTextChannelById(higherDepth(jacobSettings, "channel").getAsString());
				wantedCrops =
					gson.fromJson(higherDepth(jacobSettings, "crops").getAsJsonArray(), new TypeToken<List<RoleObject>>() {}.getType());
			}
		} catch (Exception e) {
			AutomaticGuild.getLogger().error(parent.guildId, e);
		}
	}
}
