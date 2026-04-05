package com.booksaw.betterTeams.message;

import java.util.Collection;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public interface Message {

	void sendMessage(CommandSender recipient);

	void sendMessage(Collection<? extends CommandSender> recipients);

	void sendTitle(Player recipient);

	void sendTitle(Collection<Player> recipients);
	
}
