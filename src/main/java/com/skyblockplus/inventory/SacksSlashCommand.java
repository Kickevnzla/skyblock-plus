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

package com.skyblockplus.inventory;

import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public class SacksSlashCommand extends SlashCommand {

	public SacksSlashCommand() {
		this.name = "sacks";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(
			SacksCommand.getPlayerSacks(
				event.player,
				event.getOptionStr("profile"),
				event.getOptionBoolean("npc", false),
				new PaginatorEvent(event)
			)
		);
	}

	@Override
	public CommandData getCommandData() {
		return new CommandData(name, "Get a player's sacks' content bag represented in a list")
			.addOption(OptionType.STRING, "player", "Player username or mention")
			.addOption(OptionType.STRING, "profile", "Profile name")
			.addOption(OptionType.BOOLEAN, "npc", "Use npc sell prices (bazaar will be used for items that don't have an npc price)");
	}
}