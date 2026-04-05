package com.booksaw.betterTeams;

import com.booksaw.betterTeams.message.MessageManager;

public enum PlayerRank {
	
	DEFAULT(0),

	OWNER(2),

	ADMIN(1);

	public final int value;

	PlayerRank(int value) {
		this.value = value;
	}

	public static PlayerRank getRank(String string) {

		switch (string.toUpperCase()) {
			case "DEFAULT":
				return DEFAULT;
			case "OWNER":
				return OWNER;
			case "ADMIN":
				return ADMIN;
			default:
				return null;
		}

	}

	public static PlayerRank getRank(int value) {
		for (PlayerRank rank : values()) {
			if (rank.value == value) {
				return rank;
			}
		}
		return null;
	}

	public String getPrefix() {
		return MessageManager.getMessage("prefix." + this.toString().toLowerCase());
	}

}
