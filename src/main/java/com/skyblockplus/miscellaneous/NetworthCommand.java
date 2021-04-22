package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.Utils.*;
import static java.lang.String.join;
import static java.util.Collections.nCopies;

import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.InvItemStruct;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class NetworthCommand extends Command {
    private JsonElement lowestBinJson;
    private JsonElement averageAuctionJson;
    private JsonElement bazaarJson;
    private JsonArray sbzPrices;
    private int failedCount;
    private Set<String> tempSet;
    private List<InvItemStruct> invPets;
    private List<InvItemStruct> petsPets;
    private List<InvItemStruct> enderChestPets;
    private double enderChestTotal;
    private double petsTotal;
    private double invTotal;

    public NetworthCommand() {
        this.name = "networth";
        this.cooldown = globalCooldown;
        this.aliases = new String[] { "nw" };
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            EmbedBuilder eb = loadingEmbed();
            Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
            String content = event.getMessage().getContentRaw();
            String[] args = content.split(" ");

            logCommand(event.getGuild(), event.getAuthor(), content);

            if (args.length == 2) {
                ebMessage.editMessage(getPlayerNetworth(args[1], null).build()).queue();
                return;
            } else if (args.length == 3) {
                ebMessage.editMessage(getPlayerNetworth(args[1], args[2]).build()).queue();
                return;
            }

            ebMessage.editMessage(errorMessage(this.name).build()).queue();
        }).start();
    }

    private EmbedBuilder getPlayerNetworth(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            EmbedBuilder eb = player.defaultPlayerEmbed();
            eb.setThumbnail(player.getThumbnailUrl());

            lowestBinJson = getJson("https://moulberry.codes/lowestbin.json");
            averageAuctionJson = getJson("http://moulberry.codes/auction_averages/3day.json");
            bazaarJson = higherDepth(getJson("https://api.hypixel.net/skyblock/bazaar"), "products");
            sbzPrices = getJson("https://raw.githubusercontent.com/skyblockz/pricecheckbot/master/data.json")
                    .getAsJsonArray();

            failedCount = 0;
            tempSet = new HashSet<>();
            invPets = new ArrayList<>();
            petsPets = new ArrayList<>();
            enderChestPets = new ArrayList<>();
            double bankBalance = player.getBankBalance();
            double purseCoins = player.getPurseCoins();
            invTotal = 0;
            double talismanTotal = 0;
            double invArmor = 0;
            double wardrobeTotal = 0;
            petsTotal = 0;
            enderChestTotal = 0;

            Map<Integer, InvItemStruct> playerInventory = player.getInventoryMap();
            if (playerInventory == null) {
                return defaultEmbed(player.getUsername() + "'s inventory API is disabled");
            }
            for (InvItemStruct item : playerInventory.values()) {
                invTotal += calculateItemPrice(item, "inventory");
            }

            Map<Integer, InvItemStruct> playerTalismans = player.getTalismanBagMap();
            for (InvItemStruct item : playerTalismans.values()) {
                talismanTotal += calculateItemPrice(item);
            }

            Map<Integer, InvItemStruct> invArmorMap = player.getInventoryArmorMap();
            for (InvItemStruct item : invArmorMap.values()) {
                invArmor += calculateItemPrice(item);
            }

            Map<Integer, InvItemStruct> wardrobeMap = player.getWardrobeMap();
            for (InvItemStruct item : wardrobeMap.values()) {
                wardrobeTotal += calculateItemPrice(item);
            }

            List<InvItemStruct> petsMap = player.getPetsMapNames();
            for (InvItemStruct item : petsMap) {
                petsTotal += calculateItemPrice(item, "pets");
            }

            Map<Integer, InvItemStruct> enderChest = player.getEnderChestMap();
            for (InvItemStruct item : enderChest.values()) {
                enderChestTotal += calculateItemPrice(item, "enderchest");
            }

            calculateAllPetsPrice();

            double totalNetworth = bankBalance + purseCoins + invTotal + talismanTotal + invArmor + wardrobeTotal
                    + petsTotal + enderChestTotal;

            eb.setDescription("Total Networth: " + simplifyNumber(totalNetworth));
            eb.addField("Bank", simplifyNumber(bankBalance), true);
            eb.addField("Purse", simplifyNumber(purseCoins), true);
            eb.addField("Inventory", simplifyNumber(invTotal), true);
            eb.addField("Talisman", simplifyNumber(talismanTotal), true);
            eb.addField("Armor", simplifyNumber(invArmor), true);
            eb.addField("Wardrobe", simplifyNumber(wardrobeTotal), true);
            eb.addField("Pets", simplifyNumber(petsTotal), true);
            eb.addField("Ender Chest", simplifyNumber(enderChestTotal), true);
            if (failedCount != 0) {
                eb.appendDescription("\nUnable to get " + failedCount + " items");
            }

            tempSet.forEach(System.out::println);

            return eb;
        }
        return defaultEmbed("Unable to fetch player data");
    }

    private static JsonArray queryAhApi(String query) {
        CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        try {
            HttpGet httpget = new HttpGet("https://api.eastarctica.tk/auctions/");
            httpget.addHeader("content-type", "application/json; charset=UTF-8");

            URI uri = new URIBuilder(httpget.getURI())
                    .addParameter("query", "{\"item_name\":{\"$in\":[" + query + "]},\"bin\":true}")
                    .addParameter("sort", "{\"starting_bid\":1}").build();
            httpget.setURI(uri);

            HttpResponse httpresponse = httpclient.execute(httpget);
            return JsonParser.parseReader(new InputStreamReader(httpresponse.getEntity().getContent()))
                    .getAsJsonArray();
        } catch (Exception ignored) {
        } finally {
            try {
                httpclient.close();
            } catch (Exception e) {
                System.out.println("== Stack Trace (Nw Query Close Http Client) ==");
                e.printStackTrace();
            }
        }
        return null;
    }

    private void calculateAllPetsPrice() {
        String queryStr = "";
        for (InvItemStruct item : invPets) {
            String petName = capitalizeString(item.getName()).replace("lvl", "Lvl");
            queryStr += "\"" + petName + "\",";
        }
        for (InvItemStruct item : petsPets) {
            String petName = capitalizeString(item.getName()).replace("lvl", "Lvl");
            queryStr += "\"" + petName + "\",";
        }
        for (InvItemStruct item : enderChestPets) {
            String petName = capitalizeString(item.getName()).replace("lvl", "Lvl");
            queryStr += "\"" + petName + "\",";
        }

        if (queryStr.length() == 0) {
            return;
        }

        queryStr = queryStr.substring(0, queryStr.length() - 1);

        JsonArray ahQuery = queryAhApi(queryStr);

        for (JsonElement auction : ahQuery) {
            String auctionName = higherDepth(auction, "item_name").getAsString();
            double auctionPrice = higherDepth(auction, "starting_bid").getAsDouble();
            String auctionRarity = higherDepth(auction, "tier").getAsString();

            for (Iterator<InvItemStruct> iterator = invPets.iterator(); iterator.hasNext();) {
                InvItemStruct item = iterator.next();
                if (item.getName().equalsIgnoreCase(auctionName) && item.getRarity().equalsIgnoreCase(auctionRarity)) {
                    invTotal += auctionPrice
                            + (item.getExtraStats().size() == 1 ? getLowestPrice(item.getExtraStats().get(0), " ") : 0);
                    iterator.remove();
                }
            }

            for (Iterator<InvItemStruct> iterator = petsPets.iterator(); iterator.hasNext();) {
                InvItemStruct item = iterator.next();
                if (item.getName().equalsIgnoreCase(auctionName) && item.getRarity().equalsIgnoreCase(auctionRarity)) {
                    petsTotal += auctionPrice
                            + (item.getExtraStats().size() == 1 ? getLowestPrice(item.getExtraStats().get(0), " ") : 0);
                    iterator.remove();
                }
            }

            for (Iterator<InvItemStruct> iterator = enderChestPets.iterator(); iterator.hasNext();) {
                InvItemStruct item = iterator.next();
                if (item.getName().equalsIgnoreCase(auctionName) && item.getRarity().equalsIgnoreCase(auctionRarity)) {
                    enderChestTotal += auctionPrice
                            + (item.getExtraStats().size() == 1 ? getLowestPrice(item.getExtraStats().get(0), " ") : 0);
                    iterator.remove();
                }
            }
        }

        Map<String, String> rarityMap = new HashMap<>();
        rarityMap.put("LEGENDARY", ";4");
        rarityMap.put("EPIC", ";3");
        rarityMap.put("RARE", ";2");
        rarityMap.put("UNCOMMON", ";1");
        rarityMap.put("COMMON", ";0");

        for (InvItemStruct item : invPets) {
            try {
                invTotal += higherDepth(lowestBinJson,
                        item.getName().split("] ")[1].toLowerCase().trim() + rarityMap.get(item.getRarity()))
                                .getAsDouble();
            } catch (Exception ignored) {
            }
        }

        for (InvItemStruct item : petsPets) {
            try {
                petsTotal += higherDepth(lowestBinJson,
                        item.getName().split("] ")[1].toLowerCase().trim() + rarityMap.get(item.getRarity()))
                                .getAsDouble();
            } catch (Exception ignored) {
            }
        }

        for (InvItemStruct item : enderChestPets) {
            try {
                enderChestTotal += higherDepth(lowestBinJson,
                        item.getName().split("] ")[1].toLowerCase().trim() + rarityMap.get(item.getRarity()))
                                .getAsDouble();
            } catch (Exception ignored) {
            }
        }

    }

    public double calculateItemPrice(InvItemStruct item) {
        return calculateItemPrice(item, null);
    }

    public double calculateItemPrice(InvItemStruct item, String location) {
        if (item == null) {
            return 0;
        }

        double itemCost = 0;
        double itemCount = 1;
        double recombobulatedExtra = 0;
        double hbpExtras = 0;
        double enchantsExtras = 0;
        double fumingExtras = 0;
        double reforgeExtras = 0;
        double miscExtras = 0;
        double backpackExtras = 0;

        try {
            if (item.getId().equals("PET") && location != null) {
                switch (location) {
                case "inventory":
                    invPets.add(item);
                    break;
                case "pets":
                    petsPets.add(item);
                    break;
                case "enderchest":
                    enderChestPets.add(item);
                    break;
                }
                return 0;
            } else {
                itemCost = getLowestPrice(item.getId().toUpperCase(), item.getName());
            }
        } catch (Exception ignored) {
        }

        try {
            itemCount = item.getCount();
        } catch (Exception ignored) {
        }

        try {
            if (item.isRecombobulated()) {
                recombobulatedExtra = higherDepth(
                        higherDepth(higherDepth(bazaarJson, "RECOMBOBULATOR_3000"), "quick_status"), "sellPrice")
                                .getAsDouble();
            }
        } catch (Exception ignored) {
        }

        try {
            hbpExtras = item.getHbpCount()
                    * higherDepth(higherDepth(higherDepth(bazaarJson, "HOT_POTATO_BOOK"), "quick_status"), "sellPrice")
                            .getAsDouble();
        } catch (Exception ignored) {
        }

        try {
            fumingExtras = item.getFumingCount()
                    * higherDepth(higherDepth(higherDepth(bazaarJson, "FUMING_POTATO_BOOK"), "quick_status"),
                            "sellPrice").getAsDouble();
        } catch (Exception ignored) {
        }

        try {
            List<String> enchants = item.getEnchantsFormatted();
            for (String enchant : enchants) {
                try {
                    enchantsExtras += getLowestPriceEnchant(enchant.toUpperCase());
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }

        try {
            reforgeExtras = calculateReforgePrice(item.getModifier(), item.getRarity());
        } catch (Exception ignored) {

        }

        try {
            List<String> extraStats = item.getExtraStats();
            for (String extraItem : extraStats) {
                miscExtras += getLowestPrice(extraItem, " ");
            }
        } catch (Exception ignored) {
        }

        try {
            List<InvItemStruct> backpackItems = item.getBackpackItems();
            for (InvItemStruct backpackItem : backpackItems) {
                backpackExtras += calculateItemPrice(backpackItem);
            }
        } catch (Exception ignored) {
        }

        return itemCount * (itemCost + recombobulatedExtra + hbpExtras + enchantsExtras + fumingExtras + reforgeExtras
                + miscExtras + backpackExtras);
    }

    private double calculateReforgePrice(String reforgeName, String itemRarity) {
        JsonElement reforgesStonesJson = getReforgeStonesJson();
        List<String> reforgeStones = getJsonKeys(reforgesStonesJson);

        for (String reforgeStone : reforgeStones) {
            JsonElement reforgeStoneInfo = higherDepth(reforgesStonesJson, reforgeStone);
            if (higherDepth(reforgeStoneInfo, "reforgeName").getAsString().equalsIgnoreCase(reforgeName)) {
                String reforgeStoneName = higherDepth(reforgeStoneInfo, "internalName").getAsString();
                double reforgeStoneCost = getLowestPrice(reforgeStoneName, " ");
                double reforgeApplyCost = higherDepth(higherDepth(reforgeStoneInfo, "reforgeCosts"),
                        itemRarity.toUpperCase()).getAsDouble();
                return reforgeStoneCost + reforgeApplyCost;
            }
        }

        return 0;
    }

    public double getLowestPriceEnchant(String enchantId) {
        double lowestBin = -1;
        double averageAuction = -1;
        String enchantName = enchantId.split(";")[0];
        int enchantLevel = Integer.parseInt(enchantId.split(";")[1]);

        for (int i = enchantLevel; i >= 1; i--) {
            try {
                lowestBin = higherDepth(lowestBinJson, enchantName + ";" + i).getAsDouble();
            } catch (Exception ignored) {
            }

            try {
                JsonElement avgInfo = higherDepth(averageAuctionJson, enchantName + ";" + i);
                averageAuction = higherDepth(avgInfo, "clean_price") != null
                        ? higherDepth(avgInfo, "clean_price").getAsDouble()
                        : higherDepth(avgInfo, "price").getAsDouble();
            } catch (Exception ignored) {
            }

            if (lowestBin == -1 && averageAuction != -1) {
                return Math.pow(2, enchantLevel - i) * averageAuction;
            } else if (lowestBin != -1 && averageAuction == -1) {
                return Math.pow(2, enchantLevel - i) * lowestBin;
            } else if (lowestBin != -1 && averageAuction != -1) {
                return Math.pow(2, enchantLevel - i) * Math.min(lowestBin, averageAuction);
            }
        }

        return 0;
    }

    public double getLowestPrice(String itemId, String tempName) {
        double lowestBin = -1;
        double averageAuction = -1;

        try {
            lowestBin = higherDepth(lowestBinJson, itemId).getAsDouble();
        } catch (Exception ignored) {
        }

        try {
            JsonElement avgInfo = higherDepth(averageAuctionJson, itemId);
            averageAuction = higherDepth(avgInfo, "clean_price") != null
                    ? higherDepth(avgInfo, "clean_price").getAsDouble()
                    : higherDepth(avgInfo, "price").getAsDouble();
        } catch (Exception ignored) {
        }

        if (lowestBin == -1 && averageAuction != -1) {
            return averageAuction;
        } else if (lowestBin != -1 && averageAuction == -1) {
            return lowestBin;
        } else if (lowestBin != -1 && averageAuction != -1) {
            return Math.min(lowestBin, averageAuction);
        }

        try {
            return higherDepth(higherDepth(higherDepth(bazaarJson, itemId), "quick_status"), "sellPrice").getAsDouble();
        } catch (Exception ignored) {
        }

        try {
            itemId = itemId.toLowerCase();
            if (itemId.contains("generator")) {
                String minionName = itemId.split("_generator_")[0];
                int level = Integer.parseInt(itemId.split("_generator_")[1]);

                itemId = minionName + "_minion_" + join("", nCopies(level, "i")).replace("iiiii", "v")
                        .replace("iiii", "iv").replace("vv", "x").replace("viv", "ix");
            } else if (itemId.equals("magic_mushroom_soup")) {
                itemId = "magical_mushroom_soup";
            } else if (itemId.startsWith("theoretical_hoe_")) {
                String parseHoe = itemId.split("theoretical_hoe_")[1];
                String hoeType = parseHoe.split("_")[0];
                int hoeLevel = Integer.parseInt(parseHoe.split("_")[1]);

                for (JsonElement itemPrice : sbzPrices) {
                    String itemNamePrice = higherDepth(itemPrice, "name").getAsString();
                    if (itemNamePrice.startsWith("tier_" + hoeLevel) && itemNamePrice.endsWith(hoeType + "_hoe")) {
                        return higherDepth(itemPrice, "low").getAsDouble();
                    }
                }
            } else if (itemId.equals("mine_talisman")) {
                itemId = "mine_affinity_talisman";
            } else if (itemId.equals("village_talisman")) {
                itemId = "village_affinity_talisman";
            } else if (itemId.equals("coin_talisman")) {
                itemId = "talisman_of_coins";
            } else if (itemId.equals("melody_hair")) {
                itemId = "melodys_hair";
            } else if (itemId.equals("theoretical_hoe")) {
                itemId = "mathematical_hoe_blueprint";
            } else if (itemId.equals("dctr_space_helm")) {
                itemId = "dctrs_space_helmet";
            }

            for (JsonElement itemPrice : sbzPrices) {
                if (higherDepth(itemPrice, "name").getAsString().equalsIgnoreCase(itemId)) {
                    return higherDepth(itemPrice, "low").getAsDouble();
                }
            }
        } catch (Exception ignored) {
        }

        if (isIgnoredItem(itemId)) {
            return 0;
        }

        tempSet.add(itemId + " - " + tempName);
        failedCount++;
        return 0;
    }

    private boolean isIgnoredItem(String s) {
        if (s.equalsIgnoreCase("none")) {
            return true;
        }

        if (s.startsWith("stained_glass_pane")) {
            return true;
        }

        if (s.equals("skyblock_menu")) {
            return true;
        }

        return false;
    }
}
