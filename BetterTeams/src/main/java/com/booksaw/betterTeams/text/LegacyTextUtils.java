package com.booksaw.betterTeams.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LegacyTextUtils {

	private LegacyTextUtils() {
	}

	private static final Pattern MOJANG_COLOR_PATTERN = Pattern.compile("(?i)&([0-9A-FK-OR])");
	private static final Pattern STANDARD_HEX_PATTERN = Pattern.compile("(?i)&#([0-9A-F]{6})");
	private static final Pattern BUNGEE_HEX_PATTERN   = Pattern.compile("(?i)&x(&[0-9A-F]){6}");

	private static final Map<Character, String> CHAR_TO_MINI_NAME;
	
	static final Map<Character, NamedTextColor> CHAR_TO_NAMED_COLOR;
	
	static final Map<NamedTextColor, Character> NAMED_COLOR_TO_CHAR;
	
	private static final Set<Character> COLOR_CHARS;

	static {
		Map<Character, String> names = new LinkedHashMap<>();
		names.put('0', "black");       names.put('1', "dark_blue");
		names.put('2', "dark_green");  names.put('3', "dark_aqua");
		names.put('4', "dark_red");    names.put('5', "dark_purple");
		names.put('6', "gold");        names.put('7', "gray");
		names.put('8', "dark_gray");   names.put('9', "blue");
		names.put('a', "green");       names.put('b', "aqua");
		names.put('c', "red");         names.put('d', "light_purple");
		names.put('e', "yellow");      names.put('f', "white");
		names.put('k', "obfuscated");  names.put('l', "bold");
		names.put('m', "strikethrough"); names.put('n', "underlined");
		names.put('o', "italic");
		CHAR_TO_MINI_NAME = Collections.unmodifiableMap(names);

		Map<Character, NamedTextColor> colorsByChar = new LinkedHashMap<>();
		colorsByChar.put('0', NamedTextColor.BLACK);
		colorsByChar.put('1', NamedTextColor.DARK_BLUE);
		colorsByChar.put('2', NamedTextColor.DARK_GREEN);
		colorsByChar.put('3', NamedTextColor.DARK_AQUA);
		colorsByChar.put('4', NamedTextColor.DARK_RED);
		colorsByChar.put('5', NamedTextColor.DARK_PURPLE);
		colorsByChar.put('6', NamedTextColor.GOLD);
		colorsByChar.put('7', NamedTextColor.GRAY);
		colorsByChar.put('8', NamedTextColor.DARK_GRAY);
		colorsByChar.put('9', NamedTextColor.BLUE);
		colorsByChar.put('a', NamedTextColor.GREEN);
		colorsByChar.put('b', NamedTextColor.AQUA);
		colorsByChar.put('c', NamedTextColor.RED);
		colorsByChar.put('d', NamedTextColor.LIGHT_PURPLE);
		colorsByChar.put('e', NamedTextColor.YELLOW);
		colorsByChar.put('f', NamedTextColor.WHITE);
		CHAR_TO_NAMED_COLOR = Collections.unmodifiableMap(colorsByChar);

		Map<NamedTextColor, Character> reverse = new LinkedHashMap<>();
		colorsByChar.forEach((ch, nc) -> reverse.put(nc, ch));
		NAMED_COLOR_TO_CHAR = Collections.unmodifiableMap(reverse);

		COLOR_CHARS = Collections.unmodifiableSet(new HashSet<>(colorsByChar.keySet()));
	}

	public static @Nullable NamedTextColor namedColorByChar(char c) {
		return CHAR_TO_NAMED_COLOR.get(Character.toLowerCase(c));
	}

	public static char namedColorToChar(@NotNull NamedTextColor color) {
		return NAMED_COLOR_TO_CHAR.getOrDefault(color, '6');
	}

	public static @Nullable NamedTextColor parseNamedColor(@NotNull String input) {
		if (input.isEmpty()) return null;
		if (input.length() == 1) return namedColorByChar(input.charAt(0));
		return NamedTextColor.NAMES.value(input.toLowerCase());
	}

	public static String sectionToAmpersand(String s) {
		return s.replace("§", "&");
	}

	public static String bungeeHexToStandardHex(String input) {
		Matcher matcher = BUNGEE_HEX_PATTERN.matcher(input);
		StringBuffer buffer = new StringBuffer();
		while (matcher.find()) {
			String hexColor = matcher.group().replace("&x", "").replace("&", "");
			matcher.appendReplacement(buffer, "&#" + hexColor.toUpperCase());
		}
		matcher.appendTail(buffer);
		return buffer.toString();
	}

	public static String bungeeHexToAdventure(String input) {
		Matcher matcher = BUNGEE_HEX_PATTERN.matcher(input);
		StringBuffer buffer = new StringBuffer();
		while (matcher.find()) {
			String hexColor = matcher.group().replace("&x", "").replace("&", "");
			matcher.appendReplacement(buffer, "<lr><c:#" + hexColor + ">");
		}
		matcher.appendTail(buffer);
		return buffer.toString();
	}

	public static String standardHexToAdventure(String input) {
		Matcher matcher = STANDARD_HEX_PATTERN.matcher(input);
		StringBuffer buffer = new StringBuffer();
		while (matcher.find()) {
			matcher.appendReplacement(buffer, "<c:#" + matcher.group(1) + ">");
		}
		matcher.appendTail(buffer);
		return buffer.toString();
	}

	public static String colorToAdventure(String input) {
		Matcher matcher = MOJANG_COLOR_PATTERN.matcher(input);
		StringBuffer buffer = new StringBuffer();
		List<String> closeList = new ArrayList<>();

		while (matcher.find()) {
			char code = matcher.group(1).toLowerCase().charAt(0);
			String replacement;

			if (code == 'r') {
				
				Collections.reverse(closeList);
				replacement = String.join("", closeList) + "<lr>";
				closeList.clear();
			} else {
				String name = CHAR_TO_MINI_NAME.get(code);
				if (name == null) continue; 
				if (COLOR_CHARS.contains(code)) {
					
					Collections.reverse(closeList);
					replacement = String.join("", closeList);
					closeList.clear();
				} else {
					replacement = "";
				}
				closeList.add("</" + name + ">");
				replacement += "<" + name + ">";
			}
			matcher.appendReplacement(buffer, replacement);
		}
		matcher.appendTail(buffer);
		return buffer.toString();
	}

	public static String toAdventure(String input) {
		return toAdventure(input, true, true, true, true);
	}

	public static String toAdventure(String input, boolean sectionToAmpersand, boolean bungeeHex, boolean standardHex, boolean chatColor) {
		if (input == null || input.isEmpty()) return "";
		String output = input;
		if (sectionToAmpersand) output = sectionToAmpersand(output);
		if (bungeeHex) output = bungeeHexToAdventure(output);
		if (standardHex) output = standardHexToAdventure(output);
		if (chatColor) output = colorToAdventure(output);
		return output;
	}

	public static String parseAllAdventure(String input) {
		return LegacyComponentSerializer.legacySection().serializeOr(Formatter.absolute().process(input), "");
	}

	public static String parseAdventure(String input) {
		return LegacyComponentSerializer.legacySection().serializeOr(Formatter.legacy().process(input), "");
	}

	public static String serialize(Component input) {
		return LegacyComponentSerializer.legacySection().serializeOr(input, "");
	}
}
