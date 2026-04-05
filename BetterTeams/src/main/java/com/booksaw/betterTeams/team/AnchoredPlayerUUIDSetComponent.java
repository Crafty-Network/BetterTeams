package com.booksaw.betterTeams.team;

import java.util.UUID;

import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.team.storage.team.TeamStorage;

public class AnchoredPlayerUUIDSetComponent extends UuidSetComponent {

    public enum AnchorResult {
         
        SUCCESS,
        
        NOT_IN_TEAM,
        
        ALREADY_ANCHORED,
        
        NOT_ANCHORED
    }

    public AnchorResult add(Team team, TeamPlayer player) {
        if (!team.getMembers().getClone().contains(player))
            return AnchorResult.NOT_IN_TEAM;
        else if(team.isPlayerAnchored(player)) {
            return AnchorResult.ALREADY_ANCHORED;
        }
        player.setAnchor(true);
        add(team, player.getPlayerUUID());
        return AnchorResult.SUCCESS;
    }

    @Override
    public void add(Team team, UUID playerUUID) {
        set.add(playerUUID);
    }

    public AnchorResult remove(Team team, TeamPlayer player) {
        if (!team.getMembers().getClone().contains(player))
            return AnchorResult.NOT_IN_TEAM;
        else if (!team.isPlayerAnchored(player)) {
            return AnchorResult.NOT_ANCHORED;
        }
        player.setAnchor(false);
        remove(team, player.getPlayerUUID());
        return AnchorResult.SUCCESS;
    }

    @Override
    public void remove(Team team, UUID playerUUID) {
        set.remove(playerUUID);
    }

    @Override
    public String getSectionHeading() {
        return "anchoredPlayers";
    }

    @Override
    public void load(TeamStorage section) {
        set.clear();
        set.addAll(section.getAnchoredPlayerList());
    }

    @Override
    public void save(TeamStorage storage) {
        storage.setAnchoredPlayerList(getConvertedList());
    }
}
