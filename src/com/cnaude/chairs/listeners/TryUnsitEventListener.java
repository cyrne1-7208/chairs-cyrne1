package com.cnaude.chairs.listeners;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.cnaude.chairs.core.Chairs;

public class TryUnsitEventListener implements Listener {

	protected final Chairs plugin;
	public TryUnsitEventListener(Chairs plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority=EventPriority.LOWEST)
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		final Player player = event.getPlayer();
		if (plugin.getPlayerSitData().isSitting(player)) {
			plugin.getPlayerSitData().unsitPlayerForce(player, false);
		}
	}

	@EventHandler(priority=EventPriority.LOWEST)
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		if (plugin.getPlayerSitData().isSitting(player)) {
			plugin.getPlayerSitData().unsitPlayerForce(player, true);
		}
	}

	@EventHandler(priority=EventPriority.LOWEST)
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		if (plugin.getPlayerSitData().isSitting(player)) {
			plugin.getPlayerSitData().unsitPlayerForce(player, false);
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onBlockBreak(BlockBreakEvent event) {
		unseatIfBlockOccupied(event.getBlock());
	}

	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onBlockExplode(BlockExplodeEvent event) {
		for (Block block : new ArrayList<>(event.blockList())) {
			unseatIfBlockOccupied(block);
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onPistonExtend(BlockPistonExtendEvent event) {
		for (Block block : event.getBlocks()) {
			unseatIfBlockOccupied(block);
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onPistonRetract(BlockPistonRetractEvent event) {
		for (Block block : event.getBlocks()) {
			unseatIfBlockOccupied(block);
		}
	}

	private void unseatIfBlockOccupied(Block block) {
		if (plugin.getPlayerSitData().isBlockOccupied(block)) {
			Player player = plugin.getPlayerSitData().getPlayerOnChair(block);
			if (player != null) {
				plugin.getPlayerSitData().unsitPlayerForce(player, true);
			}
		}
	}

}
