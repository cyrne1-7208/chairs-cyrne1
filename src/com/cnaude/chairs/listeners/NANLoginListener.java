package com.cnaude.chairs.listeners;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class NANLoginListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		Location loc = player.getLocation();
		if (Double.isNaN(loc.getY()) || Double.isInfinite(loc.getY())) {
			World world = player.getWorld();
			if (world != null) {
				player.teleport(world.getSpawnLocation());
			}
		}
	}

}
