package com.booksaw.betterTeams.commands;

import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.Utils;
import com.booksaw.betterTeams.message.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;

public abstract class SubCommand {

	public String getHelpMessage(ParentCommand parent) {

		String prefix = (parent.getCommand().equals("team")) ? "" : parent.getCommand() + ".";
		String message = MessageManager.getDefaultMessages().getString("help." + prefix + getCommand());
		if (message == null || message.isEmpty()) {
			message = getHelp();
			MessageManager.getDefaultMessages().set("help." + prefix + getCommand(), getHelp());

			File f = MessageManager.getFile();
			try {
				MessageManager.getDefaultMessages().save(f);
			} catch (IOException ex) {
				Main.plugin.getLogger().log(Level.SEVERE, "Could not save config to " + f, ex);
			}
		}
		return message;
	}

	public String getCommandAndArgMessage(ParentCommand parent) {
		String argMsg = getArgMessage(parent);

		return getCommand() + ((!argMsg.isEmpty()) ? " " + argMsg : "");
	}

	public String getArgMessage(ParentCommand parent) {

		String prefix = (parent.getCommand().equals("team")) ? "" : parent.getCommand() + ".";
		String message = MessageManager.getDefaultMessages().getString("helpArg." + prefix + getCommand());
		if (message == null || message.isEmpty()) {
			message = getArguments();
			MessageManager.getDefaultMessages().set("helpArg." + prefix + getCommand(), getArguments());

			File f = MessageManager.getFile();
			try {
				MessageManager.getDefaultMessages().save(f);
			} catch (IOException ex) {
				Main.plugin.getLogger().log(Level.SEVERE, "Could not save config to " + f, ex);
			}
		}
		return message;
	}

	public abstract CommandResponse onCommand(CommandSender sender, String label, String[] args);

	public abstract String getCommand();

	public abstract String getNode();

	public abstract String getHelp();

	public abstract String getArguments();

	public abstract int getMinimumArguments();

	public abstract int getMaximumArguments();

	public boolean needPlayer() {
		return false;
	}

	public abstract void onTabComplete(List<String> options, CommandSender sender, String label, String[] args);

	protected boolean runAsync(String[] args) {
		return true;
	}

	public boolean checkAsync(final String[] args) {
		return this.runAsync(args);
	}

	public void addPlayerStringList(List<String> options, String argument) {
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (p.getName().toLowerCase().startsWith(argument.toLowerCase()) && !Utils.isVanished(p)) {
				options.add(p.getName());
			}
		}
	}

	public void addTeamStringList(List<String> options, String argument) {
		addTeamStringList(options, argument, null, null);
	}

	public void addTeamStringList(List<String> options, String argument, @Nullable Collection<UUID> ignoreTheseTeams, @Nullable Collection<UUID> onlyAllowTheseTeams) {
		argument = argument.toLowerCase();
		for (Entry<UUID, Team> team : Team.getTeamManager().getLoadedTeamListClone().entrySet()) {
			if (ignoreTheseTeams != null && ignoreTheseTeams.contains(team.getKey())) {
				continue;
			} else if (onlyAllowTheseTeams != null && !onlyAllowTheseTeams.contains(team.getKey())) {
				continue;
			}

			final String teamName = team.getValue().getName();
			if (teamName.toLowerCase().startsWith(argument)) {
				options.add(teamName);
			}
		}
	}

	public void addMetaStringList(List<String> options, Team team, String argument) {
		if (team != null) {
			team.getMeta().get().getAll().keySet().stream()
					.filter(key -> key.toLowerCase().startsWith(argument.toLowerCase()))
					.forEach(options::add);
		} else {
			options.add("<key>");
		}
	}

}
