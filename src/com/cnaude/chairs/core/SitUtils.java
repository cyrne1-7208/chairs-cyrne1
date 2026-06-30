package com.cnaude.chairs.core;

import java.text.MessageFormat;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected.Half;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SitUtils {

	protected final Chairs plugin;
	protected final ChairsConfig config;
	protected final PlayerSitData sitdata;

	private static final Vector ZERO_UP = new Vector(0, 1, 0);

	public SitUtils(Chairs plugin) {
		this.plugin = plugin;
		this.config = plugin.getChairsConfig();
		this.sitdata = plugin.getPlayerSitData();
	}

	public Entity spawnChairEntity(Location location) {
		World world = location.getWorld();
		if (world == null) {
			plugin.getLogger().warning("[SIT-SPAWN] Sit location has no world. Skipping chair entity spawn.");
			return null;
		}
		switch (config.sitChairEntityType) {
			case ARROW: {
				Arrow arrow = world.spawnArrow(location, ZERO_UP, 0, 0);
				arrow.setGravity(false);
				arrow.setInvulnerable(true);
				arrow.setPickupStatus(PickupStatus.DISALLOWED);
				return arrow;
			}
			case ARMOR_STAND: {
				location = location.clone().add(0, 0.4, 0);
				return world.spawn(
					location, ArmorStand.class, armorstand -> {
						armorstand.setGravity(false);
						armorstand.setInvulnerable(true);
						armorstand.setMarker(true);
						armorstand.setVisible(false);
					}
				);
			}
			default: {
				throw new IllegalArgumentException("Unknown sit chair entity type " + config.sitChairEntityType);
			}
		}
	}

	protected boolean canSitGeneric(Player player, Block block) {

		if (player.isSneaking()) {
			return false;
		}
		if (!player.hasPermission("chairs.sit")) {
			return false;
		}

		if (config.sitDisabledWorlds.contains(player.getWorld().getName())) {
			return false;
		}
		if ((config.sitMaxDistance > 0) && (player.getLocation().distance(block.getLocation().add(0.5, 0, 0.5)) > config.sitMaxDistance)) {
			return false;
		}
		if (config.sitRequireEmptyHand && (player.getInventory().getItemInMainHand().getType() != Material.AIR)) {
			return false;
		}

		if (sitdata.isSittingDisabled(player)) {
			return false;
		}
		if (sitdata.isSitting(player)) {
			return false;
		}
		return !sitdata.isBlockOccupied(block);
	}

	public Location calculateSitLocation(Player player, Block block) {
		Location playerLoc = player.getLocation();
		Location blockLoc = block.getLocation();

		if (!canSitGeneric(player, block, playerLoc, blockLoc)) {
			return null;
		}

		BlockData blockdata = block.getBlockData();
		if (config.isMaterialExplicitlyDisabled(blockdata.getMaterial())) {
			return null;
		}
		float yaw = playerLoc.getYaw();
		Double sitHeight = null;

		if ((blockdata instanceof Stairs) && config.stairsEnabled) {
			sitHeight = 0.5;
			Stairs stairs = (Stairs) blockdata;
			if (!isStairsSittable(stairs)) {
				return null;
			}
			BlockFace ascendingFacing = stairs.getFacing();
			if (config.stairsAutoRotate) {
				switch (ascendingFacing.getOppositeFace()) {
					case NORTH: {
						yaw = 180;
						break;
					}
					case EAST: {
						yaw = -90;
						break;
					}
					case SOUTH: {
						yaw = 0;
						break;
					}
					case WEST: {
						yaw = 90;
						break;
					}
					default: {
					}
				}
			}
			if (config.stairsMaxWidth > 0) {
				BlockFace facingLeft = rotL(ascendingFacing);
				BlockFace facingRight = rotR(ascendingFacing);
				int widthLeft = calculateStairsWidth(ascendingFacing, block, facingLeft, config.stairsMaxWidth);
				int widthRight = calculateStairsWidth(ascendingFacing, block, facingRight, config.stairsMaxWidth);
				if ((widthLeft + widthRight + 1) > config.stairsMaxWidth) {
					return null;
				}
				if (config.stairsSpecialEndEnabled) {
					boolean specialEndCheckSuccess = false;
					Block blockLeft = block.getRelative(facingLeft, widthLeft + 1);
					Block blockRight = block.getRelative(facingRight, widthRight + 1);
					if (
						config.stairsSpecialEndSign &&
						isStairsEndingSign(facingLeft, blockLeft) &&
						isStairsEndingSign(facingRight, blockRight)
					) {
						specialEndCheckSuccess = true;
					}
					if (
						config.stairsSpecialEndCornerStairs && (
							isStairsEndingCornerStairs(facingLeft, Stairs.Shape.INNER_RIGHT, blockLeft) ||
							isStairsEndingCornerStairs(ascendingFacing, Stairs.Shape.INNER_LEFT, blockLeft)
						) && (
							isStairsEndingCornerStairs(facingRight, Stairs.Shape.INNER_LEFT, blockRight) ||
							isStairsEndingCornerStairs(ascendingFacing, Stairs.Shape.INNER_RIGHT, blockRight)
						)
					) {
						specialEndCheckSuccess = true;
					}
					if (!specialEndCheckSuccess) {
						return null;
					}
				}
			}
		}

		if (sitHeight == null) {
			sitHeight = config.resolveAdditionalChairHeight(blockdata.getMaterial());
			if (sitHeight == null) {
				return null;
			}
		}

		if (sitHeight <= 0) {
			return null;
		}

		Location plocation = blockLoc.clone();
		plocation.setYaw(yaw);
		plocation.add(0.5D, (sitHeight - 0.5D), 0.5D);
		return plocation;
	}

	protected boolean canSitGeneric(Player player, Block block, Location playerLoc, Location blockLoc) {

		if (player.isSneaking()) {
			return false;
		}
		if (!player.hasPermission("chairs.sit")) {
			return false;
		}

		if (config.sitDisabledWorlds.contains(player.getWorld().getName())) {
			return false;
		}
		if ((config.sitMaxDistance > 0) && (playerLoc.distance(blockLoc.add(0.5, 0, 0.5)) > config.sitMaxDistance)) {
			return false;
		}
		if (config.sitRequireEmptyHand && (player.getInventory().getItemInMainHand().getType() != Material.AIR)) {
			return false;
		}

		if (sitdata.isSittingDisabled(player)) {
			return false;
		}
		if (sitdata.isSitting(player)) {
			return false;
		}
		return !sitdata.isBlockOccupied(block);
	}

	protected static boolean isStairsSittable(Stairs stairs) {
		return stairs.getHalf() == Half.BOTTOM;
	}

	protected static boolean isStairsEndingSign(BlockFace expectedFacing, Block block) {
		BlockData blockdata = block.getBlockData();
		if (blockdata instanceof WallSign) {
			return expectedFacing == ((WallSign) blockdata).getFacing();
		}
		return false;
	}

	protected static boolean isStairsEndingCornerStairs(BlockFace expectedFacing, Stairs.Shape expectedShape, Block block) {
		BlockData blockdata = block.getBlockData();
		if (blockdata instanceof Stairs) {
			Stairs stairs = (Stairs) blockdata;
			return (stairs.getHalf() == Half.BOTTOM) && (stairs.getFacing() == expectedFacing) && (stairs.getShape() == expectedShape);
		}
		return false;
	}

	protected int calculateStairsWidth(BlockFace expectedFace, Block block, BlockFace searchFace, int limit) {
		for (int i = 0; i < limit; i++) {
			block = block.getRelative(searchFace);
			BlockData blockdata = block.getBlockData();
			if (!(blockdata instanceof Stairs)) {
				return i;
			}
			Stairs stairs = (Stairs) blockdata;
			if (!isStairsSittable(stairs) || (stairs.getFacing() != expectedFace)) {
				return i;
			}
		}
		return limit;
	}

	protected static BlockFace rotL(BlockFace face) {
		switch (face) {
			case NORTH: {
				return BlockFace.WEST;
			}
			case WEST: {
				return BlockFace.SOUTH;
			}
			case SOUTH: {
				return BlockFace.EAST;
			}
			case EAST: {
				return BlockFace.NORTH;
			}
			default: {
				throw new IllegalArgumentException(MessageFormat.format("Cant rotate blockface {0}", face));
			}
		}
	}

	protected static BlockFace rotR(BlockFace face) {
		switch (face) {
			case NORTH: {
				return BlockFace.EAST;
			}
			case EAST: {
				return BlockFace.SOUTH;
			}
			case SOUTH: {
				return BlockFace.WEST;
			}
			case WEST: {
				return BlockFace.NORTH;
			}
			default: {
				throw new IllegalArgumentException(MessageFormat.format("Cant rotate blockface {0}", face));
			}
		}
	}

//	private boolean checkFrame(Block block, BlockFace face, Player player) {
//		// Go through the blocks next to the clicked block and check if are signs on the end.
//
//		for (int i = 1; i <= plugin.maxChairWidth + 2; i++) {
//			Block relative = block.getRelative(face, i);
//			if (checkDirection(block, relative)) {
//				continue;
//			}
//			if (relative.getType().equals(Material.AIR)) {
//				int x = relative.getLocation().getBlockX();
//				int y = relative.getLocation().getBlockY();
//				int z = relative.getLocation().getBlockZ();
//				for (Entity e : player.getNearbyEntities(plugin.maxDistance, plugin.maxDistance, plugin.maxDistance)) {
//					if (e instanceof ItemFrame && plugin.validSigns.contains(Material.ITEM_FRAME)) {
//						int x2 = e.getLocation().getBlockX();
//						int y2 = e.getLocation().getBlockY();
//						int z2 = e.getLocation().getBlockZ();
//						if (x == x2 && y == y2 && z == z2) {
//							return true;
//						}
//					}
//				}
//				return false;
//			} else {
//				return false;
//			}
//		}
//		return false;
//	}
}