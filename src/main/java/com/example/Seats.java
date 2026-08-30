package com.example;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ArmorStand;

/**
 * Sitting down.
 *
 * Minecraft has no chairs, so a seat is an invisible stand at seat height with
 * you riding it -- the same trick that puts Charlie in his chair, pointed at
 * the player instead. Sneak to get up.
 *
 * Anything can be a seat: the window seats in your room, the sofa in the
 * lobby, and the giant chairs themselves.
 */
public final class Seats {
	private Seats() {
	}

	public static void register() {
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (!(player instanceof ServerPlayer who) || !(world instanceof ServerLevel level)
					|| !ShipLifeMod.isShipLife(level)
					|| !player.getItemInHand(hand).isEmpty()) {
				return InteractionResult.PASS;
			}
			BlockPos pos = Places.local(hit.getBlockPos());
			if (!isSeat(pos)) {
				return InteractionResult.PASS;
			}
			sit(who, level, hit.getBlockPos());
			return InteractionResult.SUCCESS;
		});
	}

	/** Is this block something to sit on? */
	private static boolean isSeat(BlockPos pos) {
		for (BlockPos seat : Places.WINDOW_SEATS) {
			if (seat.equals(pos)) {
				return true;
			}
		}
		for (BlockPos seat : Places.sofaSeats()) {
			if (seat.equals(pos)) {
				return true;
			}
		}
		return false;
	}

	/** Put them on it. Sneaking gets them off again, as riding always does. */
	public static void sit(ServerPlayer player, ServerLevel level, BlockPos seat) {
		if (player.isPassenger()) {
			player.stopRiding();
			return;
		}
		ArmorStand stand = net.minecraft.world.entity.EntityType.ARMOR_STAND.create(
				level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		if (stand == null) {
			return;
		}
		stand.snapTo(seat.getX() + 0.5, seat.getY() + 0.2, seat.getZ() + 0.5,
				player.getYRot(), 0.0f);
		stand.setInvisible(true);
		stand.setNoGravity(true);
		stand.setInvulnerable(true);
		stand.setSilent(true);
		level.addFreshEntity(stand);
		player.startRiding(stand, true, true);
	}

	/**
	 * Tidy up seats nobody is on.
	 *
	 * A stand left behind when somebody stands up is an invisible thing in the
	 * middle of the room, and enough of them is a room full of nothing. The
	 * ones holding Charlie and Izzy up have somebody on them, so they stay.
	 */
	public static void tidy() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK
				.register(server -> {
			if (server.getTickCount() % 40 != 0) {
				return;
			}
			for (ServerLevel level : server.getAllLevels()) {
				if (!ShipLifeMod.isShipLife(level)) {
					continue;
				}
				for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class,
						new net.minecraft.world.phys.AABB(
								Places.SHIP_X - 40, Places.GROUND - 8, Places.SHIP_Z - 40,
								Places.SHIP_X + 40,
								Places.floorY(Places.TOP_FLOOR) + 16,
								Places.SHIP_Z + 40))) {
					if (stand.isInvisible() && stand.isNoGravity()
							&& stand.getPassengers().isEmpty()) {
						stand.discard();
					}
				}
			}
		});
	}
}
