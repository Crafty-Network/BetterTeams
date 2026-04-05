package com.booksaw.betterTeams.team.level;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class TeamLevel {

	private final int level;

	private final int teamLimit;
	private final int maxChests;
	private final int maxWarps;
	private final double maxBalance;
	private final int maxAdmins;
	private final int maxOwners;

	private final String price;

	private final List<String> startCommands;
	private final List<String> endCommands;

	private final List<String> rankLore;

	public double getCostValue() {
		if (price == null || price.isEmpty()) return 0;
		try {
			String number = price.substring(0, price.length() - 1);
			return Double.parseDouble(number);
		} catch (Exception e) {
			return 0;
		}
	}

	public boolean isScoreCost() {
		return price != null && price.toLowerCase().endsWith("s");
	}

	public boolean isMoneyCost() {
		return price != null && price.toLowerCase().endsWith("m");
	}

	public List<String> getColoredLore() {
		List<String> colored = new ArrayList<>();
		if (rankLore != null) {
			for (String line : rankLore) {
				colored.add(LegacyComponentSerializer.legacySection().serialize(
					LegacyComponentSerializer.legacyAmpersand().deserialize(line)));
			}
		}
		return colored;
	}
}
