package com.skyblockplus;

import static com.skyblockplus.utils.MainClassUtils.*;
import static com.skyblockplus.utils.Utils.BOT_TOKEN;
import static com.skyblockplus.utils.Utils.DEFAULT_PREFIX;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.CommandListener;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.dev.*;
import com.skyblockplus.dungeons.*;
import com.skyblockplus.features.listeners.MainListener;
import com.skyblockplus.features.skyblockevent.SkyblockEventCommand;
import com.skyblockplus.guilds.*;
import com.skyblockplus.help.HelpCommand;
import com.skyblockplus.help.HelpSlashCommand;
import com.skyblockplus.inventory.*;
import com.skyblockplus.link.LinkAccountCommand;
import com.skyblockplus.link.LinkSlashCommand;
import com.skyblockplus.link.UnlinkAccountCommand;
import com.skyblockplus.link.UnlinkSlashCommand;
import com.skyblockplus.miscellaneous.*;
import com.skyblockplus.networth.NetworthCommand;
import com.skyblockplus.price.*;
import com.skyblockplus.settings.Database;
import com.skyblockplus.settings.SettingsCommand;
import com.skyblockplus.settings.SetupCommand;
import com.skyblockplus.skills.SkillsCommand;
import com.skyblockplus.skills.SkillsSlashCommand;
import com.skyblockplus.slayer.SlayerCommand;
import com.skyblockplus.slayer.SlayerSlashCommand;
import com.skyblockplus.timeout.MessageTimeout;
import com.skyblockplus.utils.*;
import com.skyblockplus.utils.exceptionhandlers.ExceptionEventListener;
import com.skyblockplus.utils.exceptionhandlers.GlobalExceptionHandler;
import com.skyblockplus.utils.slashcommands.SlashCommandClient;
import com.skyblockplus.weight.WeightCommand;
import com.skyblockplus.weight.WeightSlashCommand;
import javax.annotation.PreDestroy;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.RestAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {

	private static final Logger log = LoggerFactory.getLogger(Main.class);

	public static JDA jda;
	public static Database database;
	public static EventWaiter waiter;
	public static GlobalExceptionHandler globalExceptionHandler;

	public static void main(String[] args) throws LoginException, IllegalArgumentException {
		Main.globalExceptionHandler = new GlobalExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(globalExceptionHandler);
		RestAction.setDefaultFailure(e -> globalExceptionHandler.uncaughtException(null, e));

		Utils.setApplicationSettings();
		Constants.initialize();

		Main.database = SpringApplication.run(Main.class, args).getBean(Database.class);

		Main.waiter = new EventWaiter();
		CommandClientBuilder client = new CommandClientBuilder();
		client.setActivity(Activity.watching(DEFAULT_PREFIX + "help"));
		client.setOwnerId("385939031596466176");
		client.setEmojis("✅", "⚠️", "❌");
		client.useHelpBuilder(false);
		client.setGuildSettingsManager(GuildPrefixManager::new);
		client.addCommands(
			new InformationCommand(),
			new SlayerCommand(),
			new HelpCommand(),
			new GuildCommand(),
			new AuctionCommand(),
			new BinCommand(),
			new SkillsCommand(),
			new DungeonsCommand(),
			new ShutdownCommand(),
			new RoleCommands(),
			new GuildRanksCommand(),
			new EssenceCommand(),
			new BankCommand(),
			new WardrobeCommand(),
			new TalismanBagCommand(),
			new InventoryCommand(),
			new SacksCommand(),
			new InviteCommand(),
			new WeightCommand(),
			new HypixelCommand(),
			new UuidCommand(),
			new SkyblockCommand(),
			new BaldCommand(),
			new SettingsCommand(),
			new ReloadCommand(),
			new SetupCommand(),
			new CategoriesCommand(),
			new PartyFinderCommand(),
			new QuickSetupTestCommand(),
			new EmojiMapServerCommand(),
			new EnderChestCommand(),
			new InstantTimeNow(),
			new GetEventListenersCommand(),
			new GetAllGuildsIn(),
			new LinkAccountCommand(),
			new GetSettingsFile(),
			new UnlinkAccountCommand(),
			new LinkedUserDev(),
			new BazaarCommand(),
			new AverageAuctionCommand(),
			new PetsCommand(),
			new SkyblockEventCommand(),
			new DeleteMessagesCommand(),
			new PlaceholderCommand(),
			new ProfilesCommand(),
			new NetworthCommand(),
			new QueryAuctionCommand(),
			new BidsCommand(),
			new GetThreadPools(),
			new BitsCommand(),
			new EvaluateCommand(),
			new GuildRequirementsCommand(),
			new GuildKickerCommand(),
			new MissingTalismansCommand(),
			new UpdateSlashCommands(),
			new PriceCommand(),
			new EmojiFromUrlCommand(),
			new GuildLeaderboardsCommand(),
			new ArmorCommand()
		);

		SlashCommandClient slashCommands = new SlashCommandClient();
		slashCommands.addSlashCommands(
			new InviteSlashCommand(),
			new InformationSlashCommand(),
			new LinkSlashCommand(),
			new UnlinkSlashCommand(),
			new SlayerSlashCommand(),
			new SkillsSlashCommand(),
			new DungeonsSlashCommand(),
			new EssenceSlashCommand(),
			new PartyFinderSlashCommand(),
			new GuildSlashCommand(),
			new HelpSlashCommand(),
			new AuctionsSlashCommand(),
			new BinSlashCommand(),
			new BazaarSlashCommand(),
			new AverageAuctionSlashCommand(),
			new BidsSlashCommand(),
			new QueryAuctionsSlashCommand(),
			new BitsSlashCommand(),
			new RolesSlashCommand(),
			new BankSlashCommand(),
			new WeightSlashCommand(),
			new HypixelSlashCommand(),
			new ProfilesSlashCommand(),
			new MissingTalismansSlashCommand(),
			new PriceSlashCommand()
		);

		client.setListener(
			new CommandListener() {
				@Override
				public void onCommandException(CommandEvent event, Command command, Throwable throwable) {
					globalExceptionHandler.uncaughtException(event, command, throwable);
				}
			}
		);

		jda =
			JDABuilder
				.createDefault(BOT_TOKEN)
				.setStatus(OnlineStatus.DO_NOT_DISTURB)
				.addEventListeners(
					new ExceptionEventListener(waiter),
					client.build(),
					new ExceptionEventListener(new MessageTimeout()),
					new ExceptionEventListener(new MainListener()),
					slashCommands
				)
				.setActivity(Activity.playing("Loading..."))
				.build();
		try {
			jda.awaitReady();
		} catch (Exception ignored) {}
		jda.getPresence().setActivity(Activity.watching(DEFAULT_PREFIX + "help in " + jda.getGuilds().size() + " servers"));
		//				scheduleUpdateLinkedAccounts();
		//				AuctionFlipper.scheduleFlipper();
	}

	@PreDestroy
	public void onExit() {
		log.info("Stopping");

		log.info("Caching Apply Users");
		cacheApplyGuildUsers();

		log.info("Closing Http Client");
		closeHttpClient();

		log.info("Closing Async Http Client");
		closeAsyncHttpClient();

		log.info("Finished");
	}
}
