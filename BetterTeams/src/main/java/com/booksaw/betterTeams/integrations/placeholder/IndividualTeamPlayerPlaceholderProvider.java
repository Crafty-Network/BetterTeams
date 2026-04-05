package com.booksaw.betterTeams.integrations.placeholder;

import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;

public interface IndividualTeamPlayerPlaceholderProvider {

	String getPlaceholderForTeamPlayer(Team team, TeamPlayer player);

}
