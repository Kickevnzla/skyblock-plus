package com.SkyblockBot;

import static com.SkyblockBot.Miscellaneous.BotUtils.botToken;
import static com.SkyblockBot.Miscellaneous.BotUtils.setBotSettings;

import java.io.IOException;

import javax.security.auth.login.LoginException;

import com.SkyblockBot.Apply.Apply;
import com.SkyblockBot.Auction.AuctionCommands;
import com.SkyblockBot.Auction.AuctionCommandsAlias;
import com.SkyblockBot.Auction.BinCommands;
import com.SkyblockBot.Dungeons.CatacombsCommand;
import com.SkyblockBot.Dungeons.CatacombsCommandAlias;
import com.SkyblockBot.Guilds.GuildCommands;
import com.SkyblockBot.Miscellaneous.AboutCommand;
import com.SkyblockBot.Miscellaneous.ChannelDeleter;
import com.SkyblockBot.Miscellaneous.HelpCommand;
import com.SkyblockBot.Miscellaneous.ShutdownCommand;
import com.SkyblockBot.Skills.SkillsCommands;
import com.SkyblockBot.Slayer.SlayerCommands;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

public class Main {
    public static void main(String[] args)
            throws IOException, LoginException, IllegalArgumentException, RateLimitedException {
        EventWaiter waiter = new EventWaiter();
        CommandClientBuilder client = new CommandClientBuilder();
        client.useDefaultGame();
        client.setOwnerId("385939031596466176");
        client.setCoOwnerIds("413716199751286784", "726329299895975948", "488019433240002565", "632708098657878028");
        client.setEmojis("✅", "⚠️", "❌");
        client.useHelpBuilder(false);
        client.setPrefix("/");

        client.addCommands(new AboutCommand(), new SlayerCommands(), new HelpCommand(waiter), new GuildCommands(waiter),
                new AuctionCommands(), new AuctionCommandsAlias(), new BinCommands(), new SkillsCommands(),
                new CatacombsCommand(), new CatacombsCommandAlias(), new ShutdownCommand()

        );
        setBotSettings();
        JDABuilder.createDefault(botToken).setStatus(OnlineStatus.DO_NOT_DISTURB).setChunkingFilter(ChunkingFilter.ALL)
                .setMemberCachePolicy(MemberCachePolicy.ALL).enableIntents(GatewayIntent.GUILD_MEMBERS)
                .setActivity(Activity.playing("Loading...")).addEventListeners(waiter, client.build())
                .addEventListeners(new Apply()).addEventListeners(new ChannelDeleter()).build();

    }

}