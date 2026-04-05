package com.booksaw.betterTeams.util;

import com.booksaw.betterTeams.Main;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StringUtil {
	private StringUtil() {
	}

	public static final String[] EMPTY_STRING_ARRAY = new String[0];

	public static @NotNull String setPlaceholders(Player player, String text) {
		if (text == null || text.isEmpty()) {
			return "";
		}

		if (player == null || !Main.placeholderAPI) {
			return text;
		}

		return PlaceholderAPI.setPlaceholders((OfflinePlayer) player, text);
	}

	public static @NotNull String setPlaceholders(OfflinePlayer player, String text, Object... replacements) {
		if (text == null || text.isEmpty()) {
			return "";
		}

		if (player != null && Main.placeholderAPI) {
			text = PlaceholderAPI.setPlaceholders(player, text);
		}

		if (replacements.length != 0) {
			text = setPlaceholders(text, replacements);
		}

		return text;
	}

	public static @NotNull String setPlaceholders(String text, Object... replacements) {
		if (text == null || text.isEmpty()) {
			return "";
		}

		if (replacements == null || replacements.length == 0) {
			return text;
		}

		StringBuilder formatted = new StringBuilder(text);
		for (int i = 0; i < replacements.length; i++) {
			String placeholder = "{" + i + "}";
			String replacement = replacements[i] != null ? replacements[i].toString() : "";

			int index;
			while ((index = formatted.indexOf(placeholder)) != -1) {
				formatted.replace(index, index + placeholder.length(), replacement);
			}
		}
		return formatted.toString();
	}

}
