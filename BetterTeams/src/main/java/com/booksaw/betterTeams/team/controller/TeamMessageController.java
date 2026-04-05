package com.booksaw.betterTeams.team.controller;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.customEvents.TeamSendMessageEvent;
import com.booksaw.betterTeams.customEvents.post.PostTeamSendMessageEvent;
import com.booksaw.betterTeams.message.ChatMessage;
import com.booksaw.betterTeams.message.MessageManager;
import com.booksaw.betterTeams.text.Formatter;
import com.booksaw.betterTeams.text.LegacyTextUtils;
import com.booksaw.betterTeams.util.StringUtil;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class TeamMessageController {

	public enum TeamMessageType {
		TEAM_CHAT_MESSAGE("chat.syntax", "spy.team",
				(syntax, prefix, team, sender) ->
						Formatter.absolute().process(StringUtil.setPlaceholders(syntax,
								(prefix == null ? "" : prefix) + sender.getDisplayName()))),
		ALLY_CHAT_MESSAGE("allychat.syntax", "spy.ally",
				(syntax, prefix, team, sender) ->
						Formatter.absolute().process(StringUtil.setPlaceholders(syntax, team.getDisplayName(),
								(prefix == null ? "" : prefix) + sender.getDisplayName())));

		public final String chatFormat;
		public final String chatSpyFormat;

		public final ChatMessageFormattingReplacementInterface formattingFunction;

		TeamMessageType(String chatFormat, String chatSpyFormat, ChatMessageFormattingReplacementInterface formattingFunction) {
			this.chatFormat = chatFormat;
			this.chatSpyFormat = chatSpyFormat;
			this.formattingFunction = formattingFunction;
		}
	}

	public interface ChatMessageFormattingReplacementInterface {
		Component getTeamMessagePrefix(String syntax, String prefix, Team team, Player sender);
	}

	private final Team team;

	public void sendTeamChatMessage(TeamPlayer sender, String message) {
		Set<TeamPlayer> recipients = team.getMembers().getClone();

		sendTeamMessage(sender, message, recipients, TeamMessageType.TEAM_CHAT_MESSAGE);
	}

	public void sendAllyChatMessage(TeamPlayer sender, String message) {
		Set<TeamPlayer> recipients = team.getMembers().getClone();
		team.getAllies().getClone().stream().map(Team::getTeam).filter(Objects::nonNull)
				.forEach(team -> recipients.addAll(team.getMembers().getClone()));

		sendTeamMessage(sender, message, recipients, TeamMessageType.ALLY_CHAT_MESSAGE);
	}

	private void sendTeamMessage(TeamPlayer sender, String message, Set<TeamPlayer> recipients, TeamMessageType messageType) {
		recipients.removeIf(teamPlayer -> !teamPlayer.getPlayer().isOnline()); 
		String format = getChatSyntax(sender, messageType);

		TeamSendMessageEvent teamSendMessageEvent = new TeamSendMessageEvent(team, sender, message, format,
				sender.getPlayerPrefix() + getPreviousChatColor(format), recipients, messageType);
		Bukkit.getPluginManager().callEvent(teamSendMessageEvent);

		if (teamSendMessageEvent.isCancelled()) {
			Main.plugin.getLogger().log(Level.FINE, "Team send message event is cancelled");
			return;
		}

		message = teamSendMessageEvent.getRawMessage();
		format = teamSendMessageEvent.getFormat();
		String prefix = teamSendMessageEvent.getSenderNamePrefix();
		recipients = teamSendMessageEvent.getRecipients();

		ChatMessage chatMsg = sendApprovedTeamMessage(sender, prefix, message, format, recipients, messageType);

		String fMessage = LegacyTextUtils.serialize(chatMsg.getMessage());
		
		Bukkit.getPluginManager().callEvent(new PostTeamSendMessageEvent(team, sender, fMessage, recipients, messageType));
	}

	public String getChatSyntax(TeamPlayer sender, TeamMessageType messageType) {
		if (sender != null && sender.getPlayer() != null && sender.getPlayer().isOnline() && (sender.getPlayer().getPlayer() != null)) {
			return MessageManager.getMessage(sender.getPlayer().getPlayer(), messageType.chatFormat).replace("$name$", "{1}").replace("$message$", "{2}");
		}

		return MessageManager.getMessage(messageType.chatFormat).replace("$name$", "{1}").replace("$message$", "{2}");
	}

	private static final java.util.Set<Character> LEGACY_COLOR_CHARS;
	static {
		java.util.Set<Character> s = new java.util.HashSet<>();
		for (char c : new char[]{'0','1','2','3','4','5','6','7','8','9',
				'a','b','c','d','e','f','k','l','m','n','o','r',
				'A','B','C','D','E','F','K','L','M','N','O','R'}) {
			s.add(c);
		}
		LEGACY_COLOR_CHARS = java.util.Collections.unmodifiableSet(s);
	}

	private static @NotNull String getPreviousChatColor(String toTest) {
		Matcher matcher = Pattern.compile("\\{\\d+}").matcher(toTest);
		if (matcher.find()) {
			int value = matcher.start();
			if (value > 3) {
				for (int i = value - 1; i >= 0; i--) {
					if (toTest.charAt(i) == '§' && i + 1 < toTest.length()
							&& LEGACY_COLOR_CHARS.contains(toTest.charAt(i + 1))) {
						return "§" + toTest.charAt(i + 1);
					}
				}
			}
		}

		return "§r"; 
	}

	private Collection<CommandSender> getOnlineChatSpyPlayers() {
		return Main.plugin.chatManagement.getSpy().stream()
				.filter(Objects::nonNull)
				.filter(temp -> !(temp instanceof Player && team.getTeamPlayer((Player) temp) != null))
				.collect(Collectors.toList());
	}

	private ChatMessage sendApprovedTeamMessage(TeamPlayer sender, String prefix, String message, String format, Collection<TeamPlayer> recipients, TeamMessageType messageType) {
		Collection<Player> playerRecipients = recipients.stream().map(r -> r.getPlayer().getPlayer())
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		ChatMessage chatMsg = ChatMessage.teamChat(team, sender, prefix, message, format, messageType);
		chatMsg.sendMessage(playerRecipients);
		chatMsg.sendSpyMessage(getOnlineChatSpyPlayers());

		if (Team.getTeamManager().isLogChat()) {
			MessageManager.sendFullMessage(Bukkit.getConsoleSender(), chatMsg.getMessage());
		}
		return chatMsg;
	}

}
