
package com.booksaw.betterTeams.integrations.placeholder.provider;

import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.integrations.placeholder.IndividualTeamPlayerPlaceholderProvider;
import com.booksaw.betterTeams.message.MessageManager;

public class TitlePlaceholderProvider implements IndividualTeamPlayerPlaceholderProvider {

	@Override
	public String getPlaceholderForTeamPlayer(Team team, TeamPlayer player) {

		if (player.getTitle() == null || player.getTitle().isEmpty()) {
			return MessageManager.getMessage("placeholder.noTitle");
		}

		return player.getTitle();
	}

}
