package com.cnaude.chairs.sitaddons;

import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.inventory.ItemStack;

import com.cnaude.chairs.core.Chairs;
import com.cnaude.chairs.core.ChairsConfig;
import com.cnaude.chairs.core.PlayerSitData;

public class ChairEffects {

	protected final Chairs plugin;
	protected final ChairsConfig config;
	protected final PlayerSitData sitdata;
	protected int healTaskID = -1;
	protected int pickupTaskID = -1;

	private static final Attribute MAX_HEALTH_ATTRIBUTE = resolveMaxHealthAttribute();

	private static Attribute resolveMaxHealthAttribute() {
		Attribute result;
		result = resolveViaRegistry();
		if (result != null) return result;
		result = resolveViaValueOf("MAX_HEALTH");
		if (result != null) return result;
		result = resolveViaValueOf("GENERIC_MAX_HEALTH");
		return result;
	}

	private static Attribute resolveViaRegistry() {
		try {
			Class<?> registryClass = Class.forName("org.bukkit.Registry");
			Object attrRegistry = registryClass.getField("ATTRIBUTE").get(null);
			Method getMethod = attrRegistry.getClass().getMethod("get", NamespacedKey.class);
			Object result = getMethod.invoke(attrRegistry, new NamespacedKey("minecraft", "max_health"));
			if (result instanceof Attribute) {
				return (Attribute) result;
			}
		} catch (Exception ignored) {}
		return null;
	}

	private static Attribute resolveViaValueOf(String name) {
		try {
			Method valueOf = Attribute.class.getMethod("valueOf", String.class);
			Object result = valueOf.invoke(null, name);
			if (result instanceof Attribute) {
				return (Attribute) result;
			}
		} catch (Exception ignored) {}
		return null;
	}

	public ChairEffects(Chairs plugin) {
		this.plugin = plugin;
		this.config = plugin.getChairsConfig();
		this.sitdata = plugin.getPlayerSitData();
	}

	protected void startHealing() {
		if (MAX_HEALTH_ATTRIBUTE == null) {
			plugin.getLogger().warning("[EFFECT-HEAL] Could not resolve max health attribute. Healing disabled.");
			return;
		}
		healTaskID = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(
			plugin,
			() -> {
				for (Player p : Bukkit.getOnlinePlayers()) {
					if (!sitdata.isSitting(p)) {
						continue;
					}
					if (!p.hasPermission("chairs.sit.health")) {
						continue;
					}
					try {
						double health = p.getHealth();
						AttributeInstance maxHealthAttribute = p.getAttribute(MAX_HEALTH_ATTRIBUTE);
						if (maxHealthAttribute == null) {
							continue;
						}
						double maxHealth = maxHealthAttribute.getValue();
						if (maxHealth <= 0) {
							continue;
						}
						if ((((health / maxHealth) * 100d) < config.effectsHealMaxPercent) && (health < maxHealth)) {
							double newHealth = config.effectsHealHealthPerInterval + health;
							if (newHealth > maxHealth) {
								newHealth = maxHealth;
							}
							p.setHealth(newHealth);
						}
					} catch (RuntimeException ex) {
						plugin.getLogger().log(java.util.logging.Level.WARNING, "[EFFECT-HEAL] Failed to heal " + p.getName(), ex);
					}
				}
			},
			config.effectsHealInterval, config.effectsHealInterval
		);
	}

	public void cancelHealing() {
		if (healTaskID != -1) {
			plugin.getServer().getScheduler().cancelTask(healTaskID);
			healTaskID = -1;
		}
	}

	public void restartHealing() {
		cancelHealing();
		startHealing();
	}

	protected void startPickup() {
		pickupTaskID = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(
			plugin,
			() -> {
				for (Player p : Bukkit.getOnlinePlayers()) {
					if (!sitdata.isSitting(p)) {
						continue;
					}
					try {
						for (Entity entity : p.getNearbyEntities(1, 2, 1)) {
							if (entity instanceof Item) {
								Item item = (Item) entity;
								if (item.getPickupDelay() == 0) {
									if (p.getInventory().firstEmpty() != -1) {
										int amount = item.getItemStack().getAmount();
										EntityPickupItemEvent pickupevent = new EntityPickupItemEvent(p, item, amount);
										Bukkit.getPluginManager().callEvent(pickupevent);
										if (!pickupevent.isCancelled()) {
											java.util.Map<Integer, ? extends ItemStack> leftovers = p.getInventory().addItem(item.getItemStack().clone());
											int totalLeftover = 0;
											for (ItemStack leftover : leftovers.values()) {
												totalLeftover += leftover.getAmount();
											}
											if (totalLeftover == 0) {
												entity.remove();
											} else {
												item.getItemStack().setAmount(totalLeftover);
											}
										}
									}
								}
							} else if (entity instanceof ExperienceOrb) {
								ExperienceOrb eorb = (ExperienceOrb) entity;
								int exptoadd = eorb.getExperience();
								if (exptoadd > 0) {
									int oldLevel = p.getLevel();
									PlayerExpChangeEvent expchangeevent = new PlayerExpChangeEvent(p, exptoadd);
									Bukkit.getPluginManager().callEvent(expchangeevent);
									int adjustedAmount = Math.max(0, expchangeevent.getAmount());
									if (adjustedAmount > 0) {
										p.giveExp(adjustedAmount);
										int newLevel = p.getLevel();
										if (newLevel > oldLevel) {
											PlayerLevelChangeEvent levelchangeevent = new PlayerLevelChangeEvent(p, oldLevel, newLevel);
											Bukkit.getPluginManager().callEvent(levelchangeevent);
										}
										entity.remove();
									}
								}
							}
						}
					} catch (RuntimeException ex) {
						plugin.getLogger().log(java.util.logging.Level.WARNING, "[EFFECT-PICKUP] Failed pickup tick for " + p.getName(), ex);
					}
				}
			},
			1,1
		);
	}

	public void cancelPickup() {
		if (pickupTaskID != -1) {
			plugin.getServer().getScheduler().cancelTask(pickupTaskID);
		}
		pickupTaskID = -1;
	}

	public void restartPickup() {
		cancelPickup();
		startPickup();
	}

}
