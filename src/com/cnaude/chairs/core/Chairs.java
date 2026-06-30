package com.cnaude.chairs.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.java.JavaPlugin;

import com.cnaude.chairs.commands.ChairsCommand;
import com.cnaude.chairs.listeners.NANLoginListener;
import com.cnaude.chairs.listeners.TrySitEventListener;
import com.cnaude.chairs.listeners.TryUnsitEventListener;
import com.cnaude.chairs.sitaddons.ChairEffects;
import com.cnaude.chairs.sitaddons.CommandRestrict;

public class Chairs extends JavaPlugin {

	private static Chairs instance;

	public static Chairs getInstance() {
		return instance;
	}

	public Chairs() {
		instance = this;
	}

	private final ChairsConfig config = new ChairsConfig(this);

	public ChairsConfig getChairsConfig() {
		return config;
	}

	private final PlayerSitData psitdata = new PlayerSitData(this);

	public PlayerSitData getPlayerSitData() {
		return psitdata;
	}

	private final ChairEffects chairEffects = new ChairEffects(this);

	public ChairEffects getChairEffects() {
		return chairEffects;
	}

	private final SitUtils utils = new SitUtils(this);

	public SitUtils getSitUtils() {
		return utils;
	}

	@Override
	public void onEnable() {
		if (!getDataFolder().exists()) {
			if (!getDataFolder().mkdirs() && !getDataFolder().exists()) {
				getLogger().warning("Could not create plugin data folder: " + getDataFolder().getAbsolutePath());
			}
		}
		try (InputStream configHelpStream = getClass().getClassLoader().getResourceAsStream("config_help.txt")) {
			if (configHelpStream != null) {
				Files.copy(configHelpStream, new File(getDataFolder(), "config_help.txt").toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			getLogger().log(Level.WARNING, "Failed to copy config_help.txt", e);
		}
		reloadConfig();
		getServer().getPluginManager().registerEvents(new NANLoginListener(), this);
		getServer().getPluginManager().registerEvents(new TrySitEventListener(this), this);
		getServer().getPluginManager().registerEvents(new TryUnsitEventListener(this), this);
		getServer().getPluginManager().registerEvents(new CommandRestrict(this), this);

		if (!registerDismountHandler()) {
			getLogger().log(Level.SEVERE, "Missing EntityDismountEvent. Plugin cannot enable.");
			setEnabled(false);
			return;
		}

		ChairsCommand commandExecutor = new ChairsCommand(this);

		PluginCommand chairsCommand = getCommand("chairs");
		if (chairsCommand != null) {
			chairsCommand.setExecutor(commandExecutor);
			chairsCommand.setTabCompleter(commandExecutor);
		} else {
			getLogger().severe("Missing command registration for 'chairs' in plugin.yml");
		}
		PluginCommand chairCommand = getCommand("chair");
		if (chairCommand != null) {
			chairCommand.setExecutor(commandExecutor);
			chairCommand.setTabCompleter(commandExecutor);
		} else {
			getLogger().severe("Missing command registration for 'chair' in plugin.yml");
		}
	}

	@SuppressWarnings("unchecked")
	private boolean registerDismountHandler() {
		Class<?> eventClass = null;
		for (String className : new String[] {
			"org.spigotmc.event.entity.EntityDismountEvent",
			"io.papermc.paper.event.entity.EntityDismountEvent",
			"org.bukkit.event.entity.EntityDismountEvent"
		}) {
			try {
				eventClass = Class.forName(className);
				break;
			} catch (ClassNotFoundException ignored) {
			}
		}
		if (eventClass == null) {
			return false;
		}

		try {
			Method getEntityMethod = eventClass.getMethod("getEntity");
			Method setCancelledMethod = eventClass.getMethod("setCancelled", boolean.class);
			Map<UUID, Location> dismountTeleport = new HashMap<>();

			getServer().getPluginManager().registerEvent(
				(Class<? extends Event>) eventClass,
				new Listener() {},
				EventPriority.LOWEST,
				(l, event) -> {
					try {
						Object entity = getEntityMethod.invoke(event);
						if (entity instanceof Player) {
							Player player = (Player) entity;
							if (psitdata.isSitting(player)) {
								Location preDismount = player.getLocation();
								if (!psitdata.unsitPlayer(player)) {
									setCancelledMethod.invoke(event, true);
								} else {
									UUID uuid = player.getUniqueId();
									dismountTeleport.put(uuid, preDismount);
									Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> dismountTeleport.remove(uuid));
								}
							}
						}
					} catch (Exception ex) {
						getLogger().log(Level.WARNING, "[SIT-UNSIT] Dismount handling failed", ex);
					}
				},
				this
			);

			getServer().getPluginManager().registerEvents(new Listener() {
				@EventHandler(priority = EventPriority.LOWEST)
				public void onTeleportUnknown(PlayerTeleportEvent event) {
					if (event.getCause() == TeleportCause.UNKNOWN) {
						Location preDismount = dismountTeleport.remove(event.getPlayer().getUniqueId());
						if (preDismount != null) {
							event.setCancelled(true);
						}
					}
				}
			}, this);

			return true;
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Could not register dismount handler", e);
			return false;
		}
	}

	@Override
	public void onDisable() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (psitdata.isSitting(player)) {
				psitdata.unsitPlayerForce(player, true);
			}
		}
		chairEffects.cancelHealing();
		chairEffects.cancelPickup();
	}

	@Override
	public void reloadConfig() {
		super.reloadConfig();
		config.reloadConfig();
		if (config.effectsHealEnabled) {
			chairEffects.restartHealing();
		} else {
			chairEffects.cancelHealing();
		}
		if (config.effectsItemPickupEnabled) {
			chairEffects.restartPickup();
		} else {
			chairEffects.cancelPickup();
		}
	}

}
