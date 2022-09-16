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

package com.skyblockplus.utils.structs;

import static com.skyblockplus.utils.Utils.fixUsername;

import com.skyblockplus.utils.Utils;

public record UsernameUuidStruct(String username, String uuid, String failCause) {
	public UsernameUuidStruct(String username, String uuid) {
		this(username, uuid, null);
	}

	public UsernameUuidStruct(String failCause) {
		this(null, null, failCause);
	}

	public UsernameUuidStruct() {
		this("Unknown fail cause");
	}

	public boolean isValid() {
		return username != null && uuid != null;
	}

	public String getAvatarlUrl() {
		return Utils.getAvatarlUrl(uuid);
	}

	public String getAuctionUrl() {
		return "https://auctions.craftlink.xyz/players/" + uuid;
	}

	public String nameMcHyperLink() {
		return Utils.nameMcHyperLink(fixUsername(username), uuid);
	}

	public String usernameFixed() {
		return fixUsername(username());
	}
}
