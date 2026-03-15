package com.cnaude.chairs.core;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import com.cnaude.chairs.api.PlayerChairSitEvent;
import com.cnaude.chairs.api.PlayerChairUnsitEvent;

public class PlayerSitData {

	protected final Chairs plugin;

	protected final NamespacedKey sitDisabledKey;

	protected final Map<Player, SitData> sittingPlayers = new HashMap<>();
	protected final Map<Block, Player> occupiedBlocks = new HashMap<>();

	public PlayerSitData(Chairs plugin) {
		this.plugin = plugin;
		this.sitDisabledKey = new NamespacedKey(plugin, "SitDisabled");
	}

	public void disableSitting(Player player) {
		player.getPersistentDataContainer().set(sitDisabledKey, PersistentDataType.BYTE, (byte) 1);
	}

	public void enableSitting(Player player) {
		player.getPersistentDataContainer().remove(sitDisabledKey);
	}

	public boolean isSittingDisabled(Player player) {
		Byte sitDisabled = player.getPersistentDataContainer().get(sitDisabledKey, PersistentDataType.BYTE);
		return (sitDisabled != null) && (sitDisabled != 0);
	}

	public boolean isSitting(Player player) {
		SitData sitdata = sittingPlayers.get(player);
		return (sitdata != null) && sitdata.sitting;
	}

	public boolean isBlockOccupied(Block block) {
		return occupiedBlocks.containsKey(block);
	}

	public Player getPlayerOnChair(Block chair) {
		return occupiedBlocks.get(chair);
	}

	public boolean sitPlayer(final Player player,  Block blocktooccupy, Location sitlocation) {
		if (sitlocation == null) {
			return false;
		}
		if (isSitting(player) || isBlockOccupied(blocktooccupy)) {
			return false;
		}
		PlayerChairSitEvent playersitevent = new PlayerChairSitEvent(player, sitlocation.clone());
		Bukkit.getPluginManager().callEvent(playersitevent);
		if (playersitevent.isCancelled()) {
			return false;
		}
		sitlocation = playersitevent.getSitLocation().clone();
		if ((sitlocation.getWorld() == null) || !sitlocation.getWorld().isChunkLoaded(sitlocation.getBlockX() >> 4, sitlocation.getBlockZ() >> 4)) {
			return false;
		}
		Entity chairentity;
		try {
			chairentity = plugin.getSitUtils().spawnChairEntity(sitlocation);
		} catch (RuntimeException ex) {
			plugin.getLogger().log(java.util.logging.Level.WARNING, "[SIT-START] Failed to spawn chair entity for " + player.getName(), ex);
			return false;
		}
		if (chairentity == null) {
			return false;
		}
		SitData sitdata;
		if(chairentity.getType().equals(EntityType.ARMOR_STAND)){
			sitdata= new SitData(chairentity, player.getLocation(), blocktooccupy);
		}else{
			sitdata= new SitData(
					chairentity, player.getLocation(), blocktooccupy,
					Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> resitPlayer(player), 1000, 1000)
			);
		}
		if (!player.teleport(sitlocation) || !chairentity.addPassenger(player)) {
			if (sitdata.resitTaskId != -1) {
				Bukkit.getScheduler().cancelTask(sitdata.resitTaskId);
			}
			chairentity.remove();
			return false;
		}
		if (plugin.getChairsConfig().msgEnabled) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getChairsConfig().msgSitEnter));
		}
		sittingPlayers.put(player, sitdata);
		occupiedBlocks.put(blocktooccupy, player);
		sitdata.sitting = true;
		return true;
	}

	public void resitPlayer(final Player player) {
		SitData sitdata = sittingPlayers.get(player);
		if ((sitdata == null) || !sitdata.sitting) {
			return;
		}
		sitdata.sitting = false;
		Entity oldentity = sitdata.entity;
		if ((oldentity == null) || !oldentity.isValid()) {
			unsitPlayerForce(player, false);
			return;
		}
		Entity chairentity;
		try {
			chairentity = plugin.getSitUtils().spawnChairEntity(oldentity.getLocation());
		} catch (RuntimeException ex) {
			plugin.getLogger().log(java.util.logging.Level.WARNING, "[SIT-RESIT] Failed to respawn chair entity for " + player.getName(), ex);
			sitdata.sitting = true;
			return;
		}
		if ((chairentity == null) || !chairentity.addPassenger(player)) {
			if (chairentity != null) {
				chairentity.remove();
			}
			sitdata.sitting = true;
			return;
		}
		sitdata.entity = chairentity;
		oldentity.remove();
		sitdata.sitting = true;
	}

	public boolean unsitPlayer(Player player) {
		return unsitPlayer(player, true, true);
	}

	public void unsitPlayerForce(Player player, boolean teleport) {
		unsitPlayer(player, false, teleport);
	}

	private boolean unsitPlayer(final Player player, boolean canCancel, boolean teleport) {
		SitData sitdata = sittingPlayers.get(player);
		if (sitdata == null) {
			return true;
		}
		Location teleportBackLocation = (sitdata.teleportBackLocation != null) ? sitdata.teleportBackLocation.clone() : player.getLocation();
		final PlayerChairUnsitEvent playerunsitevent = new PlayerChairUnsitEvent(player, teleportBackLocation, canCancel);
		Bukkit.getPluginManager().callEvent(playerunsitevent);
		if (playerunsitevent.isCancelled() && playerunsitevent.canBeCancelled()) {
			sitdata.sitting = true;
			return false;
		}
		sitdata.sitting = false;
		try {
			player.leaveVehicle();
		} catch (RuntimeException ex) {
			plugin.getLogger().log(java.util.logging.Level.WARNING, "[SIT-UNSIT] leaveVehicle failed for " + player.getName(), ex);
		}
		player.setSneaking(false);
		occupiedBlocks.remove(sitdata.occupiedBlock);
		if(sitdata.resitTaskId != -1) {
			Bukkit.getScheduler().cancelTask(sitdata.resitTaskId);
		}
		if (sitdata.entity != null) {
			sitdata.entity.remove();
		}
		sittingPlayers.remove(player);
		if (teleport) {
			Location teleportLocation = playerunsitevent.getTeleportLocation();
			if (teleportLocation != null) {
				player.teleport(teleportLocation.clone());
			}
		}
		if (plugin.getChairsConfig().msgEnabled) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getChairsConfig().msgSitLeave));
		}
		return true;
	}

	protected static class SitData {

		protected final Location teleportBackLocation;
		protected final Block occupiedBlock;
		protected int resitTaskId;

		protected boolean sitting;
		protected Entity entity;

		public SitData(Entity arrow, Location teleportLocation, Block block, int resitTaskId) {
			this.entity = arrow;
			this.teleportBackLocation = (teleportLocation != null) ? teleportLocation.clone() : null;
			this.occupiedBlock = block;
			this.resitTaskId = resitTaskId;
		}

		public SitData(Entity arrow, Location teleportLocation, Block block) {
			this.entity = arrow;
			this.teleportBackLocation = (teleportLocation != null) ? teleportLocation.clone() : null;
			this.occupiedBlock = block;
			this.resitTaskId = -1;
		}

	}

}
