/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2023 kr45732
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

import com.skyblockplus.utils.utils.StringUtils;
import java.util.Objects;

public record UsernameUuidStruct(String username, String uuid, String failCause) {
	public UsernameUuidStruct(String username, String uuid) {
		this(username, uuid.replace("-", ""), null);
	}

	public UsernameUuidStruct(String failCause) {
		this(null, null, failCause);
	}

	public boolean isValid() {
		return username != null && uuid != null;
	}

	public String getAvatarUrl() {
		return StringUtils.getAvatarUrl(uuid);
	}

	public String getAuctionUrl() {
		return StringUtils.getAuctionUrl(uuid);
	}

	public String nameMcHyperLink() {
		return StringUtils.nameMcHyperLink(username, uuid);
	}

	public String escapeUsername() {
		return StringUtils.escapeText(username);
	}

	public boolean isRateLimited() {
		return !isValid() && Objects.equals(failCause, "Mojang has rate limited this request.");
	}
}
