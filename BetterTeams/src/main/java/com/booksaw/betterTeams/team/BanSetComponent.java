package com.booksaw.betterTeams.team;

import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.team.storage.team.TeamStorage;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class BanSetComponent extends UuidSetComponent {

	@Override
	public String getSectionHeading() {
		return "bans";
	}

	public boolean contains(OfflinePlayer player) {
		return contains(player.getUniqueId());
	}

	@Override
	public void load(TeamStorage section) {
		load(section.getBanList());
	}

	@Override
	public void save(TeamStorage storage) {
		storage.setBanList(getConvertedList());
	}

	@Override
	public void add(Team team, UUID component) {
		super.add(team, component);
		team.getStorage().addBan(component);
	}

	@Override
	public void remove(Team team, UUID component) {
		super.remove(team, component);
		team.getStorage().removeBan(component);
	}

}
