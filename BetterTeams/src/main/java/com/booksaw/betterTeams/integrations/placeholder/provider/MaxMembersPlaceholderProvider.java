
/**
*
*/
package com.booksaw.betterTeams.integrations.placeholder.provider;

import com.booksaw.betterTeams.Team;
/**
* @author booksaw
*/
import com.booksaw.betterTeams.integrations.placeholder.IndividualTeamPlaceholderProvider;

public class MaxMembersPlaceholderProvider implements IndividualTeamPlaceholderProvider {

	@Override
	public String getPlaceholderForTeam(Team team) {
		return Integer.toString(team.getTeamLimit());
	}

}
