/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2022 kr45732
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

package com.skyblockplus.utils;

import static com.skyblockplus.utils.Constants.ENCHANT_NAMES;
import static com.skyblockplus.utils.Constants.PET_NAMES;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.api.Git;

public class EmojiUpdater {

	public static JsonObject idToEmoji = new JsonObject();

	public static JsonElement getMissing(String url) {
		Set<String> processedItemsSet = getJson(url).getAsJsonObject().keySet();
		Set<String> allItems = getJson("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/InternalNameMappings.json")
			.getAsJsonObject()
			.keySet();
		allItems.removeIf(processedItemsSet::contains);
		return gson.toJsonTree(allItems);
	}

	public static JsonObject processAll() {
		try {
			JsonArray sbItems = getSkyblockItemsJson();
			Set<String> allSbItems = getJson("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/InternalNameMappings.json")
				.getAsJsonObject()
				.keySet();
			JsonObject out = new JsonObject();
			File skyCryptFiles = new File("src/main/java/com/skyblockplus/json/skycrypt_images");
			if (!skyCryptFiles.exists()) {
				skyCryptFiles.mkdirs();
			}
			File fsrImages = new File("src/main/java/com/skyblockplus/json/fsr_images");
			if (!fsrImages.exists()) {
				fsrImages.mkdirs();
			}

			// Heads
			for (JsonElement i : sbItems) {
				String id = higherDepth(i, "id").getAsString();
				if (higherDepth(i, "skin") != null && allSbItems.contains(id)) {
					try {
						String url =
							"https://sky.shiiyu.moe/head/" +
							higherDepth(
								JsonParser.parseString(new String(Base64.getDecoder().decode(higherDepth(i, "skin").getAsString()))),
								"textures.SKIN.url",
								""
							)
								.split("://textures.minecraft.net/texture/")[1];
						File imgFile;
						File[] imgFiles = skyCryptFiles.listFiles(f -> f.getName().split(".png")[0].equals(id));
						if (imgFiles.length == 0) {
							imgFile = new File(skyCryptFiles.getPath() + "/" + id + ".png");
							ImageIO.write(ImageIO.read(new URL(url)), "png", imgFile);
							TimeUnit.MILLISECONDS.sleep(200);
						} else {
							imgFile = imgFiles[0];
						}
						out.addProperty(id, imgFile.getPath());
					} catch (Exception e) {
						e.printStackTrace();
					}
					allSbItems.remove(id);
				}
			}
			System.out.println("Finished processing SkyCrypt heads");

			// Gets all images from resource pack
			JsonObject processedImages = processDir(new File("src/main/java/com/skyblockplus/json/cit"));
			for (Map.Entry<String, JsonElement> entry : processedImages.entrySet()) {
				if (allSbItems.contains(entry.getKey()) && !out.has(entry.getKey())) {
					out.add(entry.getKey(), entry.getValue());
					allSbItems.remove(entry.getKey());
				}
			}

			// Enchants and pets
			File enchantedBook = new File(skyCryptFiles.getPath() + "/ENCHANTED_BOOK_COPY.png");
			ImageIO.write(ImageIO.read(new URL("https://sky.shiiyu.moe/item/ENCHANTED_BOOK")), "png", enchantedBook);
			for (String sbItem : allSbItems) {
				String split = sbItem.split(";")[0];
				if (PET_NAMES.contains(split)) {
					try {
						File imgFile;
						File[] imgFiles = skyCryptFiles.listFiles(f -> f.getName().split(".png")[0].equals(sbItem));
						if (imgFiles.length == 0) {
							imgFile = new File(skyCryptFiles.getPath() + "/" + sbItem + ".png");
							ImageIO.write(ImageIO.read(new URL(getPetUrl(split))), "png", imgFile);
							TimeUnit.MILLISECONDS.sleep(250);
						} else {
							imgFile = imgFiles[0];
						}
						out.addProperty(sbItem, imgFile.getPath());
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else if (ENCHANT_NAMES.contains(split)) {
					out.addProperty(sbItem, enchantedBook.getPath());
				}
			}
			allSbItems.removeIf(out::has);

			System.out.println("Attempting to get " + allSbItems.size() + " items from SkyCrypt...");
			int count = 0;
			for (String sbItem : allSbItems) {
				try {
					File imgFile;
					File[] imgFiles = skyCryptFiles.listFiles(f -> f.getName().split(".png")[0].equals(sbItem));
					if (imgFiles.length == 0) {
						imgFile = new File(skyCryptFiles.getPath() + "/" + sbItem + ".png");
						ImageIO.write(ImageIO.read(new URL("https://sky.shiiyu.moe/item/" + sbItem)), "png", imgFile);
						TimeUnit.MILLISECONDS.sleep(200);
					} else {
						imgFile = imgFiles[0];
					}
					out.addProperty(sbItem, imgFile.getPath());
				} catch (Exception ignored) {}

				if (count != 0 && count % 50 == 0) {
					System.out.println("Finished " + count + "/" + allSbItems.size());
				}
				count++;
			}
			System.out.println("Finished processing SkyCrypt items");

			processEnchantedEmojis("https://hst.sh/raw/ponicuqufo");
			processCompressedImages();
			File enchantedImagesDir = new File("src/main/java/com/skyblockplus/json/enchanted_images");
			File compressedImagesDir = new File("src/main/java/com/skyblockplus/json/compressed_images");
			for (File file : enchantedImagesDir.listFiles()) {
				out.addProperty(file.getName().replace(".gif", ""), file.getPath());
			}
			for (File file : compressedImagesDir.listFiles()) {
				out.addProperty(file.getName().replace(".gif", ""), file.getPath());
			}

			return out;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static List<String> getEnchantedItems() {
		try {
			File neuDir = new File("src/main/java/com/skyblockplus/json/neu");
			if (neuDir.exists()) {
				FileUtils.deleteDirectory(neuDir);
			}
			neuDir.mkdir();

			Git.cloneRepository().setURI("https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO.git").setDirectory(neuDir).call();

			List<String> enchantedItems = Arrays
				.stream(
					new File(neuDir.getPath() + "/items")
						.listFiles(f -> {
							try {
								JsonElement json = JsonParser.parseReader(new FileReader(f));
								String id = higherDepth(json, "internalname").getAsString();
								return (
									higherDepth(json, "nbttag").getAsString().startsWith("{ench:[") &&
									!(
										id.endsWith("_MINIBOSS") ||
										id.endsWith("_MONSTER") ||
										id.endsWith("_ANIMAL") ||
										id.endsWith("_SC") ||
										id.endsWith("_BOSS")
									)
								);
							} catch (Exception e) {
								e.printStackTrace();
							}
							return false;
						})
				)
				.map(f -> f.getName().split(".json")[0])
				.collect(Collectors.toList());

			FileUtils.deleteDirectory(neuDir);

			return enchantedItems;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static JsonObject processDir(File dir) {
		JsonObject out = new JsonObject();
		try {
			for (File file : dir.listFiles()) {
				if (file.isDirectory()) {
					if (!file.getName().equals("model")) {
						JsonObject o = processDir(file);
						for (Map.Entry<String, JsonElement> e : o.entrySet()) {
							out.add(e.getKey(), e.getValue());
						}
					}
				} else {
					if (file.getName().endsWith(".properties")) {
						Properties appProps = new Properties();
						try {
							appProps.load(new FileInputStream(file));
						} catch (Exception e) {
							continue;
						}

						Object id = appProps.getOrDefault("nbt.ExtraAttributes.id", null);
						if (id == null) {
							continue;
						}

						String sId = ((String) id);
						String altId = null;
						File textureFile = Arrays
							.stream(file.getParentFile().listFiles())
							.filter(file1 -> file1.getName().equals(file.getName().replace(".properties", ".png")))
							.findFirst()
							.orElse(null);
						if (sId.equals("ipattern:*STONE_BLADE")) {
							sId = "STONE_BLADE";
						} else if (sId.equals("pattern:*STARRED_STONE_BLADE")) {
							sId = "STARRED_STONE_BLADE";
						} else if (sId.equals("ipattern:*LAST_BREATH")) {
							sId = "LAST_BREATH";
						} else if (sId.equals("GENERALS_HOPE_OF_THE_RESISTANCE")) {
							textureFile = new File(file.getParentFile() + "/staff_of_the_rising_sun.png");
						} else if (sId.equals("SPIDER_QUEENS_STINGER")) {
							textureFile = new File(file.getParentFile() + "/spider_queen_stinger.png");
						} else if (sId.equals("STARRED_SPIDER_QUEENS_STINGER")) {
							textureFile = new File(file.getParentFile() + "/starred_spider_queen_stinger.png");
						} else if (sId.equals("STARRED_VENOMS_TOUCH")) {
							textureFile = new File(file.getParentFile() + "/starred_venom_touch.png");
						} else if (sId.equals("GOD_POTION")) {
							textureFile = new File(file.getParentFile() + "/god_potion.png");
						} else if (sId.equals("REFUND_COOKIE") || sId.equals("FREE_COOKIE")) {
							textureFile = new File(file.getParentFile() + "/booster_cookie.png");
						} else if (sId.startsWith("regex:")) {
							String sIdTemp = sId.split("regex:")[1];
							sId = sIdTemp.replaceAll("\\(\\?:.*\\)\\?", "");
							altId = sIdTemp.replaceAll("\\(\\?:(.*)\\)\\?", "$1");
						}
						if (textureFile == null) {
							continue;
						}

						if (getInternalJsonMappings().keySet().contains(sId)) {
							BufferedImage image = ImageIO.read(textureFile);
							if (image.getWidth() != 16) {
								throw new IllegalArgumentException("Image width is not 16 pixels: " + file.getPath());
							}
							if (image.getHeight() != 16) {
								File mcMetaFile = Arrays
									.stream(file.getParentFile().listFiles())
									.filter(file1 -> file1.getName().equals(file.getName().replace(".properties", ".png") + ".mcmeta"))
									.findFirst()
									.orElse(null);
								if (id.equals("REFUND_COOKIE") || id.equals("FREE_COOKIE")) {
									mcMetaFile = new File(file.getParentFile() + "/booster_cookie.png.mcmeta");
								}

								int frameTime = higherDepth(JsonParser.parseReader(new FileReader(mcMetaFile)), "animation.frametime", -1);

								int rows = image.getHeight() / 16;
								int subImageHeight = image.getHeight() / rows;
								BufferedImage[] images = new BufferedImage[rows];
								for (int i = 0; i < rows; i++) {
									images[i] = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
									Graphics2D imgCreator = images[i].createGraphics();
									int srcFirstY = subImageHeight * i;
									int dstCornerY = subImageHeight * i + subImageHeight;
									imgCreator.setRenderingHint(
										RenderingHints.KEY_INTERPOLATION,
										RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
									);
									imgCreator.drawImage(image, 0, 0, 128, 128, 0, srcFirstY, image.getWidth(), dstCornerY, null);
									imgCreator.dispose();
								}
								File gifFile = new File("src/main/java/com/skyblockplus/json/fsr_images/" + sId + ".gif");
								if (gifFile.exists()) {
									gifFile.delete();
								}
								ImageIO.write(images[images.length - 1], "gif", gifFile);
								BufferedImage firstImage = images[0];
								try (ImageOutputStream output = new FileImageOutputStream(gifFile)) {
									try (GifWriter writer = new GifWriter(output, firstImage.getType(), frameTime, true)) {
										for (int i = 0; i < images.length - 1; i++) {
											writer.writeToSequence(images[i]);
										}
									}
								}
								out.addProperty(sId, gifFile.getPath());
								if (altId != null) {
									out.addProperty(altId, gifFile.getPath());
								}
							} else {
								if (
									id.equals("GEMSTONE_GAUNTLET") ||
									id.equals("BONE_BOOMERANG") ||
									id.equals("MIDAS_SWORD") ||
									id.equals("SCORPION_FOIL")
								) {
									continue;
								}

								File scaledFile = new File("src/main/java/com/skyblockplus/json/fsr_images/" + sId + ".png");
								if (scaledFile.exists()) {
									scaledFile.delete();
								}
								BufferedImage scaledImage = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
								Graphics2D graphics2D = scaledImage.createGraphics();
								graphics2D.setRenderingHint(
									RenderingHints.KEY_INTERPOLATION,
									RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
								);
								graphics2D.drawImage(image, 0, 0, 128, 128, null);
								graphics2D.dispose();
								ImageIO.write(scaledImage, "png", scaledFile);
								out.addProperty(sId, scaledFile.getPath());
								if (altId != null) {
									out.addProperty(altId, scaledFile.getPath());
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return out;
	}

	public static void runEmojis(String parsedItemsUrl) {
		idToEmoji = new JsonObject();
		String last = "";
		try {
			JsonObject allParsedItems = getJson(parsedItemsUrl).getAsJsonObject();
			Set<String> added = JsonParser
				.parseReader(new FileReader("src/main/java/com/skyblockplus/json/IdToEmojiMappings.json"))
				.getAsJsonObject()
				.keySet();
			JsonObject regItems = new JsonObject();
			JsonObject enchItems = new JsonObject();
			for (Map.Entry<String, JsonElement> entry : allParsedItems.get("regular").getAsJsonObject().entrySet()) {
				if (!added.contains(entry.getKey())) {
					regItems.add(entry.getKey(), entry.getValue());
				}
			}
			for (Map.Entry<String, JsonElement> entry : allParsedItems.get("enchanted").getAsJsonObject().entrySet()) {
				if (!added.contains(entry.getKey())) {
					enchItems.add(entry.getKey(), entry.getValue());
				}
			}

			List<Guild> guildList = jda
				.getGuilds()
				.stream()
				.filter(g -> {
					try {
						return Integer.parseInt(g.getName().split("Skyblock Plus - Emoji Server ")[1]) > 0;
					} catch (Exception e) {
						return false;
					}
				})
				.filter(g -> g.getEmotes().size() < g.getMaxEmotes() * 2)
				.sorted(Comparator.comparingInt(g -> Integer.parseInt(g.getName().split("Skyblock Plus - Emoji Server ")[1])))
				.collect(Collectors.toList());

			int guildCount = 0;
			for (Map.Entry<String, JsonElement> entry : enchItems.entrySet()) { // Regular or enchanted
				try {
					last = entry.toString();
					String name = idToName(entry.getKey())
						.toLowerCase()
						.replace("⚚ ", "starred ")
						.replace(" ", "_")
						.replace("-", "_")
						.replace("+", "plus")
						.replace("&", "and")
						.replaceAll("[™./()#'⸕❁✧❈☘✎❤]", "")
						.replace("colossal_experience_bottle_upgrade", "colossal_exp_bottle_upgrade")
						.replace("very_official_yellow_rock_of_love!", "official_yellow_rock_of_love")
						.replace("exceedingly_rare_ender_artifact_upgrader", "ender_artifact_upgrader")
						.replace("century_cake_of_the_next_dungeon_floor", "dungeon_century_cake")
						.replace("basket_of_hope_from_the_great_potato_war", "basket_of_hope")
						.replace("travel_scroll_to_blazing_fortress", "travel_scroll_blazing_fortress")
						.replace("travel_scroll_to_spiders_den_top_of_nest", "travel_scroll_top_of_spider_nest")
						.replace("travel_scroll_to_the_void_sepulture", "travel_scroll_void_sepulture")
						.replace("starred_shadow_assassin_chestplate", "star_shadow_assassin_chestplate")
						.replace("travel_scroll_to_the_crystal_hollows", "travel_scroll_crystal_hollows")
						.replace("travel_scroll_to_the_dwarven_forge", "travel_scroll_dwarven_forge");

					Guild curGuild = guildList.get(guildCount);
					if (curGuild.getEmotes().size() >= curGuild.getMaxEmotes() * 2) {
						guildCount++;
						curGuild = guildList.get(guildCount);
						TimeUnit.SECONDS.sleep(5);
						System.out.println("Switched to " + curGuild.getName());
					}

					String urlOrPath = entry.getValue().getAsString();
					Emote emoji;
					if (urlOrPath.startsWith("src/main/java/com/skyblockplus/json")) {
						emoji = curGuild.createEmote(name, Icon.from(new File(urlOrPath))).complete();
					} else {
						URLConnection urlConn = new URL(urlOrPath).openConnection();
						urlConn.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)");
						emoji = curGuild.createEmote(name, Icon.from(urlConn.getInputStream())).complete();
					}

					System.out.println("Created emoji - " + last);
					idToEmoji.addProperty(entry.getKey(), emoji.getAsMention());
					TimeUnit.MILLISECONDS.sleep(100);
				} catch (Exception e) {
					System.out.println("Failed emoji - " + last);
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			System.out.println("Failed - " + last);
			e.printStackTrace();
		}
	}

	public static void processCompressedImages() {
		File compressedImagesDir = new File("src/main/java/com/skyblockplus/json/compressed_images");
		if (!compressedImagesDir.exists()) {
			compressedImagesDir.mkdirs();
		}

		for (File file : new File("src/main/java/com/skyblockplus/json/enchanted_images").listFiles()) {
			if (file.length() > 250000) {
				String command =
					"gifsicle -i " +
					file.getPath() +
					" -O3 --colors 256 -o src/main/java/com/skyblockplus/json/compressed_images" +
					file.getName();
				try {
					ProcessBuilder builder = new ProcessBuilder(
						"gifsicle",
						"-i",
						file.getPath(),
						"-O3",
						"--colors",
						"256",
						"-o",
						compressedImagesDir.getPath() + "/" + file.getName()
					);
					builder.redirectErrorStream(true);
					Process p = builder.start();
					BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
					String line;
					do {
						line = r.readLine();
					} while (line != null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * @param url https://hst.sh/raw/ponicuqufo
	 */
	public static void processEnchantedEmojis(String url) throws IOException {
		List<File> glintFiles = Arrays
			.stream(new File("src/main/java/com/skyblockplus/json/glint_images").listFiles())
			.sorted(Comparator.comparing(File::getName))
			.toList();
		List<String> enchantList = streamJsonArray(getJson(url).getAsJsonArray())
			.map(JsonElement::getAsString)
			.collect(Collectors.toList());
		File outputFileDir = new File("src/main/java/com/skyblockplus/json/enchanted_images");
		if (!outputFileDir.exists()) {
			outputFileDir.mkdir();
		}

		for (File inputFile : new File("src/main/java/com/skyblockplus/json/skycrypt_images")
			.listFiles(f -> enchantList.contains(f.getName().replace(".png", "")))) {
			BufferedImage inputImage = ImageIO.read(inputFile);

			List<BufferedImage> frames = new ArrayList<>();
			for (File glintFile : glintFiles) {
				int inputHeight = inputImage.getHeight();
				int inputWidth = inputImage.getWidth();
				int glintSize = 64;
				float ratioHeight = (float) glintSize / inputHeight;
				float ratioWidth = (float) glintSize / inputWidth;

				BufferedImage glintImage = ImageIO.read(glintFile);
				BufferedImage outputImage = new BufferedImage(glintSize, glintSize, BufferedImage.TYPE_INT_ARGB);
				for (int x = 0; x < glintSize; x++) {
					for (int y = 0; y < glintSize; y++) {
						Color color = new Color(inputImage.getRGB((int) (x / ratioWidth), (int) (y / ratioHeight)), true);
						if (color.getAlpha() == 0) {
							outputImage.setRGB(x, y, color.getRGB());
						} else {
							outputImage.setRGB(x, y, addColor(color, new Color(glintImage.getRGB(x, y), true)).getRGB());
						}
					}
				}
				frames.add(outputImage);
			}

			File gifFile = new File(outputFileDir.getPath() + "/" + inputFile.getName().replace(".png", ".gif"));
			try (ImageOutputStream output = new FileImageOutputStream(gifFile)) {
				try (GifWriter writer = new GifWriter(output, frames.get(0).getType(), 70, true, true)) {
					for (BufferedImage frame : frames) {
						writer.writeToSequence(frame);
					}
				}
			}
		}
	}

	public static int clampColor(float x) {
		return Math.min(Math.max(Math.round(x), 0), 255);
	}

	public static Color addColor(Color back, Color front) {
		float x = front.getAlpha() / 255F;
		return new Color(
			clampColor((back.getRed() + front.getRed() * x)),
			clampColor((back.getGreen() + front.getGreen() * x)),
			clampColor((back.getBlue() + front.getBlue() * x)),
			back.getAlpha()
		);
	}
}