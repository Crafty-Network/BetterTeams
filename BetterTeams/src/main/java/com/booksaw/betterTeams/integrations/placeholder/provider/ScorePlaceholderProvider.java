
/**
*
*/
package com.booksaw.betterTeams.integrations.placeholder.provider;

import com.booksaw.betterTeams.Team;
/**
* @author booksaw
*/
import com.booksaw.betterTeams.integrations.placeholder.IndividualTeamPlaceholderProvider;

public class ScorePlaceholderProvider implements IndividualTeamPlaceholderProvider {

	@Override
	public String getPlaceholderForTeam(Team team) {
		return Integer.toString(team.getScore());
	}
}
