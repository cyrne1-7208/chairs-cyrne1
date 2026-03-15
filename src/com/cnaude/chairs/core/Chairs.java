package com.cnaude.chairs.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.spigotmc.event.entity.EntityDismountEvent;

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
		try {
			getClass().getClassLoader().loadClass(EntityDismountEvent.class.getName());
		} catch (Throwable t) {
			getLogger().log(Level.SEVERE, "Missing EntityDismountEvent", t);
			setEnabled(false);
			return;
		}
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
