/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2022 kr45732
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

package com.skyblockplus.utils.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

@Getter
public class PaginatorExtras {

	private final List<String> titles = new ArrayList<>();
	private final List<String> titleUrls = new ArrayList<>();
	private final List<Field> embedFields = new ArrayList<>();
	private final List<EmbedBuilder> embedPages = new ArrayList<>();
	private final List<ReactiveButton> reactiveButtons = new ArrayList<>();
	private final PaginatorType type;
	private String everyPageText = null;
	private String everyPageTitle = null;
	private String everyPageTitleUrl = null;
	private String everyPageThumbnail = null;
	private String everyPageFirstFieldTitle = null;

	public PaginatorExtras() {
		this.type = PaginatorType.DEFAULT;
	}

	public PaginatorExtras(PaginatorType type) {
		this.type = type;
	}

	public String getTitle(int index) {
		try {
			return titles.get(index);
		} catch (Exception e) {
			return null;
		}
	}

	public String getTitleUrl(int index) {
		try {
			return titleUrls.get(index);
		} catch (Exception e) {
			return null;
		}
	}

	public PaginatorExtras setTitles(List<String> titles) {
		this.titles.clear();
		this.titles.addAll(titles);
		return this;
	}

	public PaginatorExtras setTitles(String[] titles) {
		this.titles.clear();
		this.titles.addAll(Arrays.asList(titles));
		return this;
	}

	public PaginatorExtras setTitleUrls(List<String> titleUrls) {
		this.titleUrls.clear();
		this.titleUrls.addAll(titleUrls);
		return this;
	}

	public PaginatorExtras setEveryPageText(String everyPageText) {
		this.everyPageText = everyPageText;
		return this;
	}

	public PaginatorExtras setEveryPageTitle(String everyPageTitle) {
		this.everyPageTitle = everyPageTitle;
		return this;
	}

	public PaginatorExtras setEveryPageThumbnail(String everyPageThumbnail) {
		this.everyPageThumbnail = everyPageThumbnail;
		return this;
	}

	public PaginatorExtras addEmbedField(String name, String value, boolean inline) {
		this.embedFields.add(new Field(name, value, inline));
		return this;
	}

	public PaginatorExtras addBlankField(boolean inline) {
		this.embedFields.add(new Field(EmbedBuilder.ZERO_WIDTH_SPACE, EmbedBuilder.ZERO_WIDTH_SPACE, inline));
		return this;
	}

	public PaginatorExtras setEveryPageTitleUrl(String everyPageTitleUrl) {
		this.everyPageTitleUrl = everyPageTitleUrl;
		return this;
	}

	public PaginatorExtras addEmbedPage(EmbedBuilder embedBuilder) {
		this.embedPages.add(embedBuilder);
		return this;
	}

	public PaginatorExtras setEmbedPages(EmbedBuilder... embedBuilder) {
		this.embedPages.clear();
		Collections.addAll(this.embedPages, embedBuilder);
		return this;
	}

	/**
	 * Only on one row after arrows
	 */
	public PaginatorExtras addButton(Button button) {
		this.reactiveButtons.add(new ReactiveButton(button));
		return this;
	}

	public List<Button> getButtons() {
		return reactiveButtons.stream().filter(ReactiveButton::isVisible).map(ReactiveButton::getButton).collect(Collectors.toList());
	}

	public PaginatorExtras toggleReactiveButton(String id, boolean visible) {
		for (ReactiveButton reactiveButton : reactiveButtons) {
			if (reactiveButton.getId().equals(id)) {
				reactiveButton.setVisible(visible);
			}
		}
		return this;
	}

	public PaginatorExtras addReactiveButtons(ReactiveButton... buttons) {
		Collections.addAll(reactiveButtons, buttons);
		return this;
	}

	public PaginatorExtras setEveryPageFirstFieldTitle(String everyPageFirstFieldTitle) {
		this.everyPageFirstFieldTitle = everyPageFirstFieldTitle;
		return this;
	}

	public enum PaginatorType {
		DEFAULT,
		EMBED_FIELDS,
		EMBED_PAGES,
	}

	@Data
	public static class ReactiveButton {

		private final Consumer<CustomPaginator> action;
		private final Button button;
		private final boolean reactive;
		private boolean visible;

		public ReactiveButton(Button button) {
			this.action = event -> {};
			this.button = button;
			this.reactive = false;
			this.visible = true;
		}

		public ReactiveButton(Button button, Consumer<CustomPaginator> action, boolean visible) {
			this.action = action;
			this.button = button;
			this.reactive = true;
			this.visible = visible;
		}

		public String getId() {
			return button.getId();
		}

		/**
		 * @return visible and reactive
		 */
		public boolean isReacting() {
			return visible && reactive;
		}
	}
}
