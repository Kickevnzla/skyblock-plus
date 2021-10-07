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

package com.skyblockplus.weight.lily;

import static com.skyblockplus.utils.Constants.SKILL_NAMES;
import static com.skyblockplus.utils.Constants.SLAYER_NAMES;
import static com.skyblockplus.utils.Utils.getJson;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.WeightStruct;

public class Weight {

	public static final JsonElement lilyWeightConstants = getJson(
		"https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/LilyWeight.json"
	);

	private final SlayerWeight slayerWeight;
	private final SkillsWeight skillsWeight;
	private final DungeonsWeight dungeonsWeight;

	public Weight(Player player) {
		this.slayerWeight = new SlayerWeight(player);
		this.skillsWeight = new SkillsWeight(player);
		this.dungeonsWeight = new DungeonsWeight(player);
	}

	public SkillsWeight getSkillsWeight() {
		return skillsWeight;
	}

	public SlayerWeight getSlayerWeight() {
		return slayerWeight;
	}

	public DungeonsWeight getDungeonsWeight() {
		return dungeonsWeight;
	}

	public WeightStruct getTotalWeight(boolean needToCalc) {
		if (needToCalc) {
			for (String slayerName : SLAYER_NAMES) {
				slayerWeight.getSlayerWeight(slayerName);
			}
			for (String skillName : SKILL_NAMES) {
				skillsWeight.getSkillsWeight(skillName);
			}
			dungeonsWeight.getDungeonWeight();
			dungeonsWeight.getDungeonCompletionWeight("normal");
			dungeonsWeight.getDungeonCompletionWeight("master");
		}

		WeightStruct w = new WeightStruct();
		w.add(slayerWeight.getWeightStruct());
		w.add(skillsWeight.getWeightStruct());
		w.add(dungeonsWeight.getWeightStruct());

		return w;
	}
}