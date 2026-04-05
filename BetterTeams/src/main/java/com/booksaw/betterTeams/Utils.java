package com.booksaw.betterTeams;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class Utils {

	private Utils() {
		
	}

	public static @Nullable OfflinePlayer getOfflinePlayer(String name) {
		@SuppressWarnings("deprecation")
		OfflinePlayer player = Bukkit.getOfflinePlayer(name);

		if (!player.hasPlayedBefore()) {
			for (Team team : Team.getTeamManager().getLoadedTeamListClone().values()) {
				for (OfflinePlayer offlinePlayer : team.getMembers().getOfflinePlayers()) {
					String offlinePlayerName = offlinePlayer.getName();
					if (offlinePlayerName != null && offlinePlayerName.equalsIgnoreCase(name)) {
						return offlinePlayer;
					}
				}
			}

			return null;
		}

		return player;
	}

	public static @NotNull String serializeInventory(@NotNull Inventory inventory) {
		YamlConfiguration json = new YamlConfiguration();
		int idx = 0;
		HashMap<String, ItemStack> items = new HashMap<>();
		for (ItemStack item : inventory.getContents()) {
			int i = idx++;
			if (item == null) {
				continue;
			}
			items.put("" + i, item);
		}
		json.createSection("items", items);
		return json.saveToString();
	}

	public static @NotNull String dumpItem(ItemStack itemStack) {
		YamlConfiguration json = new YamlConfiguration();
		json.set("item", itemStack);
		return json.saveToString();
	}

	public static void deserializeIntoInventory(@NotNull Inventory inventory, @NotNull String jsons) {
		try {
			YamlConfiguration json = new YamlConfiguration();
			json.loadFromString(jsons);

			Map<String, Object> items = json.getConfigurationSection("items").getValues(false);
			for (Map.Entry<String, Object> item : items.entrySet()) {
				ItemStack itemstack = (ItemStack) item.getValue();
				int idx = Integer.parseInt(item.getKey());
				inventory.setItem(idx, itemstack);
			}
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}
	}

	public static boolean isVanished(final @NotNull Player player) {
		if (!player.isOnline())
			return false;

		final List<MetadataValue> values = player.getMetadata("vanished");

		for (final MetadataValue meta : values)
			if (meta.asBoolean())
				return true;

		return false;
	}

	public static @NotNull <T> List<T> filterNonNull(Collection<T> collection) {
		if (collection == null || collection.isEmpty()) return Collections.emptyList();
		return collection.stream()
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toList());
	}

	public static boolean isComponentEmpty(Component component) {
		if (component == null || component.equals(Component.empty())) {
			return true;
		}

		String plainText = component.toString();
		if (plainText == null || plainText.isEmpty()) {
			return true;
		}

		return component.children().isEmpty();
	}
}
