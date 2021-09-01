package com.skyblockplus.price;

import static com.skyblockplus.utils.Utils.executor;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class BinSlashCommand extends SlashCommand {

	public BinSlashCommand() {
		this.name = "bin";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		executor.submit(() -> {
			event.logCommandGuildUserCommand();

			EmbedBuilder eb = BinCommand.getLowestBin(event.getOptionStr("item"));

			event.getHook().editOriginalEmbeds(eb.build()).queue();
		});
	}
}
