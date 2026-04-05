package com.booksaw.betterTeams.team;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.Team;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public abstract class LocationSetComponent extends SetTeamComponent<Location> {

	public static Location getLocation(String loc) {
		String[] split = loc.split(":");

		if (split.length < 6) {
			throw new IllegalArgumentException("Invalid location string: " + loc);
		}

		return new Location(Bukkit.getWorld(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]),
				Double.parseDouble(split[3]), Float.parseFloat(split[4]), Float.parseFloat(split[5]));
	}

	public static String getString(Location loc) {
		if (!loc.isWorldLoaded()) {
			Main.plugin.getLogger().warning("Location " + loc + " is not in a loaded world so it will not be stored");
			return null;
		}

		return loc.getWorld().getName() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ() + ":" + loc.getYaw()
				+ ":" + loc.getPitch();
	}

	public static Location normalise(Location loc) {
		return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
	}

	@Override
	public Location fromString(String str) {
		return getLocation(str);
	}

	@Override
	public String toString(Location component) {
		return getString(component);
	}

	@Override
	public void remove(Team team, Location component) {

		set.remove(component);
	}

	@Override
	public boolean contains(Location component) {
		
		return set.contains(component);
	}

}
