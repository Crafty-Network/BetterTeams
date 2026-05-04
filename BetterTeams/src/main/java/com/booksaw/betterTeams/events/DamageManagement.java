package com.booksaw.betterTeams.events;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.Team;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
/**
* This class is used to ensure that members of the same team cannot hit each
* other
*
* @author booksaw
*/
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Collection;
import java.util.Objects;

public class DamageManagement implements Listener {

	private final boolean disablePotions;
	private final boolean disableSelf;

	public DamageManagement() {
		disablePotions = Main.plugin.getConfig().getBoolean("disablePotions");
		disableSelf = Main.plugin.getConfig().getBoolean("playerDamageSelf");
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onDamage(EntityDamageByEntityEvent e) {

		if (!(e.getEntity() instanceof Player)) {
			return;
		}
		Team temp = Team.getTeam((Player) e.getEntity());
		if (temp == null) {
			return;
		}
		try {
			if (e.getDamager() instanceof Player) {
				if (!Objects.requireNonNull(Team.getTeam((Player) e.getDamager())).canDamage(temp,
						(Player) e.getDamager())) {
					
					e.setCancelled(true);
				}
			} else if (e.getDamager() instanceof Projectile && !(e.getDamager() instanceof ThrownPotion)) {
				Projectile arrow = (Projectile) e.getDamager();
				ProjectileSource source = arrow.getShooter();
				if (source instanceof Player
						&& !Objects.requireNonNull(Team.getTeam((Player) source)).canDamage(temp, (Player) source)) {
					
					if (disableSelf && source == e.getEntity()) {
						return;
					}
					e.setCancelled(true);
				}
			} else if (e.getDamager() instanceof ThrownPotion && disablePotions) {
				ThrownPotion arrow = (ThrownPotion) e.getDamager();
				ProjectileSource source = arrow.getShooter();
				if (source instanceof Player
						&& !Objects.requireNonNull(Team.getTeam((Player) source)).canDamage(temp, (Player) source)) {
					
					e.setCancelled(true);
				}
			} else if (e.getDamager() instanceof TNTPrimed) {
				TNTPrimed explosive = (TNTPrimed) e.getDamager();
				Entity source = explosive.getSource();
				if (source instanceof Player
						&& !Objects.requireNonNull(Team.getTeam((Player) source)).canDamage(temp, (Player) source)) {
					
					if (disableSelf && source == e.getEntity()) {
						return;
					}
					e.setCancelled(true);
				}
			}
		} catch (NullPointerException ex) {
			
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPotion(PotionSplashEvent e) {
		if (!(e.getEntity().getShooter() instanceof Player) || !disablePotions) {
			return;
		}
		Player thrower = (Player) e.getEntity().getShooter();
		Team team = Team.getTeam(thrower);
		
		if (team == null) {
			return;
		}

		Collection<PotionEffect> effects = e.getPotion().getEffects();
		boolean cancel = false;
		for (PotionEffect effect : effects) {
			String type = effect.getType().getName();
			if (type.equals(PotionEffectType.BAD_OMEN.getName()) || type.equals(PotionEffectType.BLINDNESS.getName())
					|| type.equals(PotionEffectType.NAUSEA.getName()) || type.equals(PotionEffectType.INSTANT_DAMAGE.getName())
					|| type.equals(PotionEffectType.HUNGER.getName())
					|| type.equals(PotionEffectType.MINING_FATIGUE.getName())
					|| type.equals(PotionEffectType.UNLUCK.getName())
					|| type.equals(PotionEffectType.WEAKNESS.getName())
					|| type.equals(PotionEffectType.POISON.getName())) {
				cancel = true;
			}
		}

		if (cancel) {
			Collection<LivingEntity> affectedEntities = e.getAffectedEntities();
			for (LivingEntity entity : affectedEntities) {
				try {
					if (entity instanceof Player
							&& !Objects.requireNonNull(Team.getTeam((Player) entity)).canDamage(team, thrower)) {
						if (disableSelf && entity == thrower) {
							continue;
						}
						e.setIntensity(entity, 0);
					}
				} catch (NullPointerException ex) {
					
				}
			}
		}
	}
}
