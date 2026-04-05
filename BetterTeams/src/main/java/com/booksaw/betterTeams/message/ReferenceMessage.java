package com.booksaw.betterTeams.message;

import java.util.Collection;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReferenceMessage implements Message {

	final String reference;

	public ReferenceMessage(String reference) {
		this.reference = reference;
	}

	@Override
	public void sendMessage(CommandSender recipient) {
		MessageManager.sendMessage(recipient, reference);
	}

	@Override
	public void sendMessage(Collection<? extends CommandSender> recipients) {
		MessageManager.sendMessage(recipients, reference);
	}

	@Override
	public void sendTitle(Player recipient) {
		MessageManager.sendTitle(recipient, reference);
	}

	@Override
	public void sendTitle(Collection<Player> recipients) {
		MessageManager.sendTitle(recipients, reference);
	}
}
