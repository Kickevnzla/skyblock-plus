package com.skyblockplus.mainlistener;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.skyblockplus.apply.ApplyGuild;
import com.skyblockplus.verify.VerifyGuild;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import java.io.File;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.higherDepth;

public class AutomaticGuild {
    private ApplyGuild applyGuild = new ApplyGuild(false);
    private VerifyGuild verifyGuild = new VerifyGuild(false);

    public AutomaticGuild(GuildReadyEvent event) {
        applyConstructor(event);
        verifyConstructor(event);
    }

    public boolean allowApplyReload() {
        return applyGuild.applyUserListSize() == 0;
    }

    public void verifyConstructor(GuildReadyEvent event) {
        JsonElement currentSettings = database.getVerifySettings(event.getGuild().getId());
        if (currentSettings == null) {
            return;
        }

        try {
            if (higherDepth(currentSettings, "enable").getAsBoolean()) {
                TextChannel reactChannel = event.getGuild()
                        .getTextChannelById(higherDepth(currentSettings, "messageTextChannelId").getAsString());
                try {
                    Message reactMessage = reactChannel.retrieveMessageById(higherDepth(currentSettings, "previousMessageId").getAsString()).complete();
                    if (reactMessage != null) {
                        reactMessage.editMessage(higherDepth(currentSettings, "messageText").getAsString()).queue();

                        verifyGuild = new VerifyGuild(reactChannel, reactMessage);
                        return;
                    }
                } catch (Exception ignored) {
                }

                Message reactMessage = reactChannel.sendMessage(higherDepth(currentSettings, "messageText").getAsString())
                        .addFile(new File("src/main/java/com/skyblockplus/verify/Link_Discord_To_Hypixel.mp4"))
                        .complete();

                JsonObject newSettings = currentSettings.getAsJsonObject();
                newSettings.remove("previousMessageId");
                newSettings.addProperty("previousMessageId", reactMessage.getId());
                database.updateVerifySettings(event.getGuild().getId(), newSettings);

                verifyGuild = new VerifyGuild(reactChannel, reactMessage);
            }
        } catch (Exception e) {
            System.out.println("Verify constructor error");
        }
    }

    public void applyConstructor(GuildReadyEvent event) {
        JsonElement currentSettings = database.getApplySettings(event.getGuild().getId());
        if (currentSettings == null) {
            return;
        }

        try {
            if (higherDepth(currentSettings, "enable").getAsBoolean()) {
                TextChannel reactChannel = event.getGuild()
                        .getTextChannelById(higherDepth(currentSettings, "messageTextChannelId").getAsString());

                EmbedBuilder eb = defaultEmbed("Apply For Guild");
                eb.setDescription(higherDepth(currentSettings, "messageText").getAsString());

                try {
                    Message reactMessage = reactChannel.retrieveMessageById(higherDepth(currentSettings, "previousMessageId").getAsString()).complete();
                    reactMessage.editMessage(eb.build()).queue();

                    applyGuild = new ApplyGuild(reactMessage, currentSettings);
                    return;
                } catch (Exception ignored) {
                }

                Message reactMessage = reactChannel.sendMessage(eb.build()).complete();
                reactMessage.addReaction("✅").queue();

                JsonObject newSettings = currentSettings.getAsJsonObject();
                newSettings.remove("previousMessageId");
                newSettings.addProperty("previousMessageId", reactMessage.getId());
                database.updateApplySettings(event.getGuild().getId(), newSettings);

                applyGuild = new ApplyGuild(reactMessage, currentSettings);
            }
        } catch (Exception e) {
            System.out.println("Apply constructor error");
        }
    }

    public String reloadApplyConstructor(String guildId) {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            return "Invalid guild";
        }


        JsonElement currentSettings = database.getApplySettings(guild.getId());
        if (currentSettings == null) {
            return "No settings found";
        }

        try {
            if (higherDepth(currentSettings, "enable").getAsBoolean()) {
                TextChannel reactChannel = guild
                        .getTextChannelById(higherDepth(currentSettings, "messageTextChannelId").getAsString());

                EmbedBuilder eb = defaultEmbed("Apply For Guild");
                eb.setDescription(higherDepth(currentSettings, "messageText").getAsString());

                try {
                    Message reactMessage = reactChannel.retrieveMessageById(higherDepth(currentSettings, "previousMessageId").getAsString()).complete();
                    reactMessage.editMessage(eb.build()).queue();

                    applyGuild = new ApplyGuild(reactMessage, currentSettings);
                    return "Reloaded";
                } catch (Exception ignored) {
                }

                Message reactMessage = reactChannel.sendMessage(eb.build()).complete();
                reactMessage.addReaction("✅").queue();

                JsonObject newSettings = currentSettings.getAsJsonObject();
                newSettings.remove("previousMessageId");
                newSettings.addProperty("previousMessageId", reactMessage.getId());
                database.updateApplySettings(guild.getId(), newSettings);

                applyGuild = new ApplyGuild(reactMessage, currentSettings);
                return "Reloaded";
            } else {
                applyGuild = new ApplyGuild(false);
                return "Not enabled";
            }
        } catch (Exception e) {
            System.out.println("Reload apply constructor error");
        }
        return "Error Reloading";
    }

    public String reloadVerifyConstructor(String guildId) {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            return "Invalid guild";
        }


        JsonElement currentSettings = database.getVerifySettings(guild.getId());
        if (currentSettings == null) {
            return "No settings found";
        }

        try {
            if (higherDepth(currentSettings, "enable").getAsBoolean()) {
                TextChannel reactChannel = guild
                        .getTextChannelById(higherDepth(currentSettings, "messageTextChannelId").getAsString());
                try {
                    Message reactMessage = reactChannel.retrieveMessageById(higherDepth(currentSettings, "previousMessageId").getAsString()).complete();
                    if (reactMessage != null) {
                        reactMessage.editMessage(higherDepth(currentSettings, "messageText").getAsString()).queue();

                        verifyGuild = new VerifyGuild(reactChannel, reactMessage);
                        return "Reloaded";
                    }
                } catch (Exception ignored) {
                }

                Message reactMessage = reactChannel.sendMessage(higherDepth(currentSettings, "messageText").getAsString())
                        .addFile(new File("src/main/java/com/skyblockplus/verify/Link_Discord_To_Hypixel.mp4"))
                        .complete();

                JsonObject newSettings = currentSettings.getAsJsonObject();
                newSettings.remove("previousMessageId");
                newSettings.addProperty("previousMessageId", reactMessage.getId());
                database.updateVerifySettings(guild.getId(), newSettings);

                verifyGuild = new VerifyGuild(reactChannel, reactMessage);
                return "Reloaded";
            } else {
                verifyGuild = new VerifyGuild(false);
                return "Not enabled";
            }
        } catch (Exception e) {
            System.out.println("Reload verify constructor error");
        }
        return "Error Reloading";
    }

    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        applyGuild.onMessageReactionAdd(event);
    }

    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        verifyGuild.onGuildMessageReceived(event);
    }
}