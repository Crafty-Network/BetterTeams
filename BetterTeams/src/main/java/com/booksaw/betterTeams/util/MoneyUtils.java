package com.booksaw.betterTeams.util;

import com.booksaw.betterTeams.Main;

import java.text.DecimalFormat;

public class MoneyUtils {

	public static boolean useShortFormatting() {
		return Main.plugin.getConfig().getBoolean("useShortMoney");
	}

	public static String getDecimalPlaceFormattingString() {
		String doubleFormat = "0";
		int moneyDecimalPlaces = Main.plugin.getConfig().getInt("moneyDecimalPlaces");
		if (moneyDecimalPlaces > 0) {
			doubleFormat += ".";
			for (int i = 0; i < moneyDecimalPlaces; i++) {
				doubleFormat += "0";
			}
		}
		return doubleFormat;
	}

	public static String getFormattedDouble(double amount) {
		return getFormattedDouble(amount, getDecimalPlaceFormattingString(), useShortFormatting());
	}

	public static String getFormattedDouble(double amount, String doubleFormat, boolean shortFormatting) {
		if (shortFormatting) {
			return getFormattedShortDouble(amount);
		} else {
			DecimalFormat df = new DecimalFormat(doubleFormat);
			df.setGroupingUsed(true);
			df.setGroupingSize(3);
			return df.format(amount);
		}
	}

	public static String getFormattedShortDouble(double amount) {
		if (amount >= 1_000_000_000) {
			return String.format("%.1fB", amount / 1_000_000_000);
		} else if (amount >= 1_000_000) {
			return String.format("%.1fM", amount / 1_000_000);
		} else if (amount >= 1_000) {
			return String.format("%.1fk", amount / 1_000);
		} else {
			return String.format("%.0f", amount);
		}
	}
}
