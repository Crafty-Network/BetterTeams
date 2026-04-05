package com.booksaw.betterTeams.message;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;

public class ReferencedFormatMessage implements Message {

	final String reference;
	final Object[] replacements;

	public ReferencedFormatMessage(String reference, Object... replacements) {
		this.reference = reference;
		this.replacements = replacements;
	}

	@Override
	public void sendMessage(CommandSender recipient) {
		MessageManager.sendMessage(recipient, reference, replacements);
	}

	@Override
	public void sendTitle(Player recipient) {
		MessageManager.sendTitle(recipient, reference, replacements);
	}

	@Override
	public void sendMessage(Collection<? extends CommandSender> recipients) {
		MessageManager.sendMessage(recipients, reference, replacements);
	}

	@Override
	public void sendTitle(Collection<Player> recipients) {
		MessageManager.sendTitle(recipients, reference, replacements);
	}
}
