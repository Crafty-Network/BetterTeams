package com.booksaw.betterTeams.integrations.placeholder;

import com.booksaw.betterTeams.Team;

public interface IndividualTeamWithDataPlaceholderProvider {

	String getPlaceholderForTeam(Team team, String data);
}
