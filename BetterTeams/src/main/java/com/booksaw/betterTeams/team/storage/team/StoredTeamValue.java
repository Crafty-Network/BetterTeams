package com.booksaw.betterTeams.team.storage.team;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StoredTeamValue {

	NAME("name"),

	TAG("tag"),

	OPEN("open", TeamStorageType.BOOLEAN),

	DESCRIPTION("description"),

	COLOR("color"),

	HOME("home"),

	ANCHOR("anchor", TeamStorageType.BOOLEAN),

	SCORE("score", TeamStorageType.INTEGER),

	MONEY("money", TeamStorageType.DOUBLE),

	LEVEL("level", TeamStorageType.INTEGER),

	PVP("pvp", TeamStorageType.BOOLEAN);

	private final String reference;

	private final TeamStorageType storageType;

	StoredTeamValue(String reference) {
		this(reference, TeamStorageType.STRING);
	}

}
