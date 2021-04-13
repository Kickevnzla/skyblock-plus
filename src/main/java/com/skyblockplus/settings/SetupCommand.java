package com.skyblockplus.settings;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.PaginatorExtras;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

import static com.skyblockplus.utils.Utils.*;

public class SetupCommand extends Command {
    private final EventWaiter waiter;

    public SetupCommand(EventWaiter waiter) {
        this.name = "setup";
        this.cooldown = globalCooldown;
        this.userPermissions = new Permission[]{Permission.ADMINISTRATOR};
        this.waiter = waiter;
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            EmbedBuilder eb = loadingEmbed();
            Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();

            logCommand(event.getGuild(), event.getAuthor(), BOT_PREFIX + "setup");

            String[] pageTitles = new String[]{"Overview", "Features", "Automatic Verify", "Automatic Apply",
                    "Automatic Roles", "Automatic Guild Roles", "More Help"};

            String overview = "This will walk you through how to setup custom settings for the bot!\n\n**Pages:**\n• Page 2 - Features\n• Page 3 - Automatic Verify\n• Page 4 - Automatic Apply\n• Page 5 - Automatic Guild Roles\n• Page 6 - Automatic Roles\n• Page 7 - More Help";

            String features = "**I have many default features including, but not limited to:**\n• Slayer\n• Skills\n• Dungeons\n• Guilds\n• Auction house and Bazaar\n• Inventory\n• Skyblock Events\n• And much more that can be seen by running `" + BOT_PREFIX + "help`!\n\nI also have customizable features such as automatic verify, automatic apply, automatic roles, and automatic guild role and ranks!";

            String verify = "**__Overview__**\n" +
                    "1) When a user runs `link [IGN]` their username is compared with the Hypixel API\n" +
                    "2) If it's valid they will be linked, given the verified role, and their nickname will be updated if set\n" +
                    "• Example video linked [__here__](https://i.imgur.com/VzQUJrE.mp4)\n\n" +
                    "**__Setup__**\n" +
                    "In order to enable automatic verify, all the following settings must be set:\n" +
                    "- `settings verify message [message]` - the message that users will see and react to in order to verify.\n" +
                    "- `settings verify role [@role]` - the role users will get once verified. This role cannot be @everyone or a managed role.\n" +
                    "- `settings verify channel [#channel]` - the channel where the message to react too will be sent.\n\n" +
                    "Optional settings:\n" +
                    "- `settings verify nickname <prefix> [IGN] <postfix>` - the nickname template where IGN will be the players name. Can also be set to none.\n" +
                    "• Tutorial video linked [__here__](https://streamable.com/pibmus)\n\n" +
                    "**__Enable__**\n" +
                    "- Once all these settings are set run `settings verify enable` to enable verify*.\n" +
                    "- For more help type `help settings_verify` or watch the video linked above\n" +
                    "\n" +
                    "*You __must__ run `reload` in order for the changes to take effect";

            String apply = "**__Overview__**\n" +
                    "1) When a user reacts to the apply message, the application process is started.\n" +
                    "2) If the user is not linked to the bot or is linked to an invalid account, they will receive a DM.\n" +
                    "3) Otherwise, a new channel is created and a message is sent with reactions representing the user's profiles\n" +
                    "4) The user will react to the emoji for the profile they want to apply with and then confirm the stats which sends it to staff\n" +
                    "6) The staff role will be pinged with the stats of the user\n" +
                    "7) The user will receive the a message depending on if they are accepted, denied, or waitlisted\n\n" +
//                    "• Example video linked [__here__](https://i.imgur.com/9XZTbSz.mp4)\n\n" +
                    "**__Setup__**\n" +
                    "In order to enable automatic apply, all the following settings must be set:\n" +
                    "- `settings apply message [message]` - the message that users will see and react to in order to apply.\n" +
                    "- `settings apply staff_role [@role]` - the role that will be pinged when a new application is received. Cannot be @everyone or a managed role.\n" +
                    "- `settings apply channel [#channel]` - the channel where the message to react to will be sent.\n" +
                    "- `settings apply prefix [prefix]` - the prefix that new apply channels will start with.\n" +
                    "- `settings apply staff_channel [#channel]` - the channel where new applications will be sent to be reviewed by staff.\n" +
                    "- `settings apply accept_message [message]` - accepted message.\n" +
                    "- `settings apply deny_message [message]` - denied message.\n" +
                    "- `settings apply category [category id]` - the category where new apply channels will be put. Get all category IDs by running `categories`\n\n" +
                    "Optional settings:\n" +
                    "- `settings apply waitlist [message]` - waitlisted message.\n" +
                    "- `settings apply requirements [type] [value]` - requirement that the user must meet. Type can be slayer, skills, catacombs, or weight. Value can be an integer or none.\n" +
                    "• Tutorial video linked [__here__](https://streamable.com/f5cl7r)\n\n" +
                    "**__Enable__**\n" +
                    "- Once all these settings are set run `settings apply enable` to enable apply*\n" +
                    "- For more help type `help settings_apply` or watch the video linked above\n\n" +
                    "*You __must__ run `reload` in order for the changes to take effect";


            String roles = "**__Overview__**\n" +
                    "1) When a user runs `roles claim [ign]` their stats are fetched\n" +
                    "2) Depending on the roles setup for this server and the users stats, the corresponding roles will be given\n\n" +
                    "**__Setup__**\n" +
                    "- In order to enable automatic roles, there must be at least one role setting enabled:\n" +
                    "- `settings roles add [roleName] [value] [@role]` - add a level to a role. The role cannot be @everyone or managed.\n" +
                    "- `settings roles remove [roleName] [value]` - remove a level from a role.\n" +
                    "- `settings roles stackable [roleName] [true|false]` - make a role stackable or not stackable.\n" +
                    "- `settings roles enable [roleName]` - enable a role.\n" +
                    "- `settings roles set [roleName] [@role]` - set a one level role's role\n" +
                    "• Tutorial video linked [__here__](https://streamable.com/wninsw)\n\n" +
                    "**__Enable__**\n" +
                    "- Once all these settings are set run `settings roles enable` to enable roles.\n" +
                    "- To view all the roles, their descriptions, and examples, type `settings roles`\n" +
                    "- For more help type `help settings_roles` or watch the video linked above\n";

            String guildRoles = "**__Overview__**\n" +
                    "1) Every 3 hours, the bot will loop through all players **linked to the bot** in the discord server\n" +
                    "2) If a guild member role is set, then the player will be given the corresponding role if they are in the Hypixel guild\n" +
                    "3) If the guild ranks are set, then the player will given the corresponding role if they are in the Hypixel guild and have the certain rank\n\n" +
                    "**__Setup__**\n" +
                    "- In order to enable automatic guild roles, the guild name and either the guild member role or one guild rank role must be set.\n" +
                    "- `settings guild set [guild_name]` - set the guild name.\n" +
                    "- `settings guild role [@role]` - set the guild member role. The role cannot be @everyone or managed.\n" +
                    "- `settings guild add [rank_name] [@role]` - add a guild rank and its role.\n" +
                    "- `settings guild remove [rank_name]` - remove a guild rank.\n\n" +
                    //"• Tutorial video linked [__here__]()\n\n" +
                    "**__Enable__**\n" +
                    "- Once these settings are set run `settings guild enable role` to enable guild roles and/or `settings guild enable rank` to enable guild ranks.\n" +
                    "- To view all the current settings type `settings guild`\n" +
                    "- For more help type `help settings_guild`\n";

            String moreHelp = "If you need any help, have suggestions, or find any bugs for this bot be sure to join the discord server [here](https://discord.gg/DpcCAwMXwp)!\n" +
                    "You can also view the forums post [here](https://hypixel.net/threads/discord-bot-skyblock-plus-skyblock-focused.3980092/)\n";

            CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(1).setItemsPerPage(1);

            paginateBuilder.addItems(overview, features, verify, apply, roles, guildRoles, moreHelp);
            paginateBuilder.setPaginatorExtras(new PaginatorExtras().setTitles(pageTitles));
            ebMessage.delete().queue();
            paginateBuilder.build().paginate(event.getChannel(), 0);
        }).start();
    }
}