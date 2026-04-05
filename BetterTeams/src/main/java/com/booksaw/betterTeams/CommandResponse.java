package com.booksaw.betterTeams;

import com.booksaw.betterTeams.message.Message;
import com.booksaw.betterTeams.message.ReferenceMessage;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.minecart.CommandMinecart;

public class CommandResponse {

	final boolean success;

	final Message message;

	public CommandResponse(boolean success, String message) {
		this.success = success;
		this.message = new ReferenceMessage(message);
	}

	public CommandResponse(String message) {
		this(false, message);
	}

	public CommandResponse(boolean success, Message message) {
		this.message = message;
		this.success = success;
	}

	public CommandResponse(Message message) {
		this(false, message);
	}

	public CommandResponse(boolean success) {
		message = null;
		this.success = success;
	}

	public void sendResponseMessage(CommandSender sender) {

		if (message == null) return;
		if (sender instanceof BlockCommandSender || sender instanceof CommandMinecart) return;

		message.sendMessage(sender);
	}

	public boolean wasSuccessful() {
		return success;
	}

}
