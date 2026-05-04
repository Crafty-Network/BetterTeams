package com.booksaw.betterTeams.events;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.message.MessageManager;
import com.booksaw.betterTeams.text.Formatter;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;

public class ChatManagement implements Listener {

	private static PrefixType doPrefix;
	private final List<CommandSender> spy = new ArrayList<>();

	public static void enable() {
		doPrefix = PrefixType.getType(Main.plugin.getConfig().getString("prefix"));
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onChat(AsyncChatEvent event) {

		if (event.isCancelled()) {
			return;
		}

		Player p = event.getPlayer();
		Team team = Team.getTeam(p);

		if (team == null) {
			return;
		}

		TeamPlayer teamPlayer = team.getTeamPlayer(p);

		if (teamPlayer == null) {
			throw new IllegalStateException("Player " + p.getName() + " is registered to be in a team, yet has no playerdata associated with that team");
		}

		String rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message());

		String anyChatToGlobalPrefix = Main.plugin.getConfig().getString("chatPrefixes.teamOrAllyToGlobal", "!");
		String globalToTeamPrefix = Main.plugin.getConfig().getString("chatPrefixes.globalToTeam", "!");
		String globalToAllyPrefix = Main.plugin.getConfig().getString("chatPrefixes.globalToAlly", "?");

		if (teamPlayer.isInTeamChat() || teamPlayer.isInAllyChat()) {
			if (!anyChatToGlobalPrefix.isEmpty() && rawMessage.startsWith(anyChatToGlobalPrefix) && rawMessage.length() > anyChatToGlobalPrefix.length()) {
				event.message(Component.text(rawMessage.substring(anyChatToGlobalPrefix.length())));
			} else {
				
				event.setCancelled(true);

				if (teamPlayer.isInTeamChat()) {
					team.getTeamMessageController().sendTeamChatMessage(teamPlayer, rawMessage);
				} else {
					team.getTeamMessageController().sendAllyChatMessage(teamPlayer, rawMessage);
				}
				return;
			}
		} else if (
				(!globalToTeamPrefix.isEmpty() && rawMessage.startsWith(globalToTeamPrefix) && rawMessage.length() > globalToTeamPrefix.length())
						|| (!globalToAllyPrefix.isEmpty() && rawMessage.startsWith(globalToAllyPrefix) && rawMessage.length() > globalToAllyPrefix.length())
		) {
			
			event.setCancelled(true);

			if (rawMessage.startsWith(globalToTeamPrefix)) {
				team.getTeamMessageController().sendTeamChatMessage(teamPlayer, rawMessage.substring(globalToTeamPrefix.length()));
			} else {
				team.getTeamMessageController().sendAllyChatMessage(teamPlayer, rawMessage.substring(globalToAllyPrefix.length()));
			}
			return;
		}

		if (doPrefix != PrefixType.NONE) {
			Component prefix = Formatter.absolute().process(doPrefix.getUpdatedFormat(p, "", team));
			event.renderer((src, displayName, message, viewer) ->
					prefix.append(displayName).append(Component.text(": ")).append(message));
		}

	}

	@EventHandler
	public void spyQuit(PlayerQuitEvent e) {
		spy.removeIf(s -> s.equals(e.getPlayer()));
	}

	public List<CommandSender> getSpy() {
		return spy;
	}

	enum PrefixType {
		NONE, NAME, TAG;

		public static PrefixType getType(String str) {
			str = str.toLowerCase().trim();
			switch (str) {
				case "name":
				case "true":
					return NAME;
				case "tag":
					return TAG;
				default:
					return NONE;
			}
		}

		public String getUpdatedFormat(Player p, String format, Team team) {
			switch (this) {
				case NAME:
					return MessageManager.getMessage(p, "prefixSyntax", team.getDisplayName(), format);
				case TAG:
					
					return MessageManager.getMessage(p, "prefixSyntax", team.getTag(), format);
				default:
					return format;
			}
		}

	}

}
