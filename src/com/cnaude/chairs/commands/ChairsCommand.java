package com.cnaude.chairs.commands;

import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.cnaude.chairs.core.Chairs;
import com.cnaude.chairs.core.ChairsConfig;
import com.cnaude.chairs.core.PlayerSitData;

public class ChairsCommand implements CommandExecutor, TabCompleter {

	protected final Chairs plugin;
	protected final ChairsConfig config;
	protected final PlayerSitData sitdata;

	public ChairsCommand(Chairs plugin) {
		this.plugin = plugin;
		this.config = plugin.getChairsConfig();
		this.sitdata = plugin.getPlayerSitData();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		try {
			if (command.getName().equalsIgnoreCase("chair")) {
				return handleChairCommand(sender, args);
			}

			if (args.length == 0) {
				return false;
			}
			if (args[0].equalsIgnoreCase("reload")) {
				if (sender.hasPermission("chairs.reload")) {
					plugin.reloadConfig();
					sender.sendMessage(ChatColor.GREEN + "Chairs configuration reloaded.");
				} else {
					sender.sendMessage(ChatColor.RED + "You do not have permission to do this!");
				}
				return true;
			}

			if (args[0].equalsIgnoreCase("block")) {
				return handleBlockCommand(sender, args);
			}

			if (sender instanceof Player) {
				Player player = (Player) sender;
				if (args[0].equalsIgnoreCase("off")) {
					sitdata.disableSitting(player);
					player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.msgSitDisabled));
					return true;
				} else if (args[0].equalsIgnoreCase("on")) {
					sitdata.enableSitting(player);
					player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.msgSitEnabled));
					return true;
				}
			} else if (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off")) {
				sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
				return true;
			}
			return false;
		} catch (RuntimeException ex) {
			plugin.getLogger().log(java.util.logging.Level.WARNING, "[CMD] Command execution failed for '/" + label + "'", ex);
			sender.sendMessage(ChatColor.RED + "An internal error occurred. Please check server logs.");
			return true;
		}
	}

	protected boolean handleChairCommand(CommandSender sender, String[] args) {
		if (args.length == 0) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
				return true;
			}
			Player self = (Player) sender;
			if (trySitNearby(self, false)) {
				return true;
			}
			self.sendMessage(ChatColor.RED + "You cannot sit here right now.");
			return true;
		}

		if (args.length != 1) {
			return false;
		}

		if (!sender.hasPermission("chairs.sit.force")) {
			sender.sendMessage(ChatColor.RED + "You do not have permission to do this!");
			return true;
		}

		Player target = Bukkit.getPlayerExact(args[0]);
		if ((target == null) || !target.isOnline()) {
			sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
			return true;
		}
		if (sitdata.isSitting(target)) {
			sender.sendMessage(ChatColor.YELLOW + target.getName() + " is already sitting.");
			return true;
		}
		if (config.sitDisabledWorlds.contains(target.getWorld().getName())) {
			sender.sendMessage(ChatColor.RED + "Sitting is disabled in " + target.getWorld().getName() + ".");
			return true;
		}

		if (trySitNearby(target, true)) {
			sender.sendMessage(ChatColor.GREEN + "Forced " + target.getName() + " to sit.");
			return true;
		}

		sender.sendMessage(ChatColor.RED + "Could not seat " + target.getName() + " at current position.");
		return true;
	}

	protected boolean trySitNearby(Player player, boolean forceIgnoringSitToggle) {
		Block playerBlock = player.getLocation().getBlock();
		Block[] candidateBlocks = new Block[] {
			playerBlock.getRelative(BlockFace.DOWN),
			playerBlock
		};

		boolean wasDisabled = sitdata.isSittingDisabled(player);
		if (forceIgnoringSitToggle && wasDisabled) {
			sitdata.enableSitting(player);
		}
		try {
			for (Block candidateBlock : candidateBlocks) {
				Location sitLocation = plugin.getSitUtils().calculateSitLocation(player, candidateBlock);
				if ((sitLocation != null) && sitdata.sitPlayer(player, candidateBlock, sitLocation)) {
					return true;
				}
			}
		} finally {
			if (forceIgnoringSitToggle && wasDisabled) {
				sitdata.disableSitting(player);
			}
		}
		return false;
	}

	protected boolean handleBlockCommand(CommandSender sender, String[] args) {
		if (!sender.hasPermission("chairs.block.edit")) {
			sender.sendMessage(ChatColor.RED + "You do not have permission to do this!");
			return true;
		}
		if (args.length < 2) {
			sender.sendMessage(ChatColor.YELLOW + "Usage: /chairs block <list|disable|enable> [MATERIAL|PATTERN]");
			return true;
		}

		if (args[1].equalsIgnoreCase("list")) {
			Set<String> disabled = new TreeSet<>(config.getDisabledChairPatterns());
			if (disabled.isEmpty()) {
				sender.sendMessage(ChatColor.GREEN + "No chair block ids are disabled.");
			} else {
				sender.sendMessage(ChatColor.GREEN + "Disabled chair block ids: " + String.join(", ", disabled));
			}
			return true;
		}

		if (args.length < 3) {
			sender.sendMessage(ChatColor.YELLOW + "Usage: /chairs block <disable|enable> <MATERIAL|PATTERN>");
			return true;
		}

		boolean disable;
		if (args[1].equalsIgnoreCase("disable")) {
			disable = true;
		} else if (args[1].equalsIgnoreCase("enable")) {
			disable = false;
		} else {
			sender.sendMessage(ChatColor.YELLOW + "Usage: /chairs block <list|disable|enable> [MATERIAL|PATTERN]");
			return true;
		}

		String normalizedId = ChairsConfig.normalizeMaterialPattern(args[2]);
		if (!ChairsConfig.isValidMaterialPattern(normalizedId)) {
			sender.sendMessage(ChatColor.RED + "Invalid material id.");
			return true;
		}
		if (!normalizedId.contains("*")) {
			Material material = Material.getMaterial(normalizedId);
			if (material == null) {
				sender.sendMessage(ChatColor.RED + "Unknown material id: " + normalizedId);
				return true;
			}
		}

		boolean changed = config.setChairDisabledPattern(normalizedId, disable);
		if (!changed) {
			sender.sendMessage(ChatColor.YELLOW + "No changes were made for id: " + normalizedId);
			return true;
		}
		if (!config.saveConfigFile()) {
			sender.sendMessage(ChatColor.RED + "Failed to save config.yml. Check server logs.");
			return true;
		}
		sender.sendMessage(ChatColor.GREEN + "Chair block id " + normalizedId + (disable ? " disabled." : " enabled."));
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (command.getName().equalsIgnoreCase("chair")) {
			if ((args.length == 1) && sender.hasPermission("chairs.sit.force")) {
				String prefix = args[0].toLowerCase(Locale.ROOT);
				List<String> completions = new ArrayList<>();
				for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
					String name = onlinePlayer.getName();
					if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
						completions.add(name);
					}
				}
				Collections.sort(completions);
				return completions;
			}
			return Collections.emptyList();
		}

		if (!command.getName().equalsIgnoreCase("chairs")) {
			return Collections.emptyList();
		}

		if (args.length == 1) {
			return filterByPrefix(args[0], "reload", "on", "off", "block");
		}
		if ((args.length == 2) && args[0].equalsIgnoreCase("block")) {
			return filterByPrefix(args[1], "list", "disable", "enable");
		}
		if ((args.length == 3) && args[0].equalsIgnoreCase("block") &&
			(args[1].equalsIgnoreCase("disable") || args[1].equalsIgnoreCase("enable"))) {
			String prefix = ChairsConfig.normalizeMaterialPattern(args[2]);
			List<String> results = new ArrayList<>();
			if ("*_STAIRS".startsWith(prefix)) {
				results.add("*_STAIRS");
			}
			for (Material material : Material.values()) {
				String name = material.name();
				if (name.startsWith(prefix)) {
					results.add(name);
				}
			}
			Collections.sort(results);
			return results;
		}

		return Collections.emptyList();
	}

	protected static List<String> filterByPrefix(String current, String... options) {
		String lowered = current.toLowerCase(Locale.ROOT);
		List<String> results = new ArrayList<>();
		for (String option : options) {
			if (option.toLowerCase(Locale.ROOT).startsWith(lowered)) {
				results.add(option);
			}
		}
		return results;
	}

}
