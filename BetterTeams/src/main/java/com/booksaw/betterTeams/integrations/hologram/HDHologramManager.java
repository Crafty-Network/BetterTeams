package com.booksaw.betterTeams.integrations.hologram;

import com.booksaw.betterTeams.Main;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import org.bukkit.Location;

public class HDHologramManager extends HologramManager {
	
	@Override
	public LocalHologram createLocalHolo(Location location, HologramType type) {
		return new HDHologramImpl(HologramsAPI.createHologram(Main.plugin, location));
	}

	private static class HDHologramImpl implements LocalHologram {
		private final Hologram hologram;

		public HDHologramImpl(Hologram hologram) {
			this.hologram = hologram;
		}

		@Override
		public void appendText(String text) {
			hologram.appendTextLine(text);
		}

		@Override
		public void clearLines() {
			hologram.clearLines();
		}

		@Override
		public void delete() {
			hologram.delete();
		}

		@Override
		public Location getLocation() {
			return hologram.getLocation();
		}
	}
}
