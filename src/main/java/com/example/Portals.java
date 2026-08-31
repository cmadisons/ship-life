package com.example;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * The portal on floor 18, and what is on the other side of it.
 *
 * Ship 2, floating in the Nether. It is this ship again, floor for floor,
 * built in the Nether at the same coordinates and standing in the middle of
 * nothing -- so stepping through the frame on 18 puts you in the lobby of a
 * ship hanging over a lava sea.
 *
 * The lift, the shops, the arcade and everything else work over there because
 * the Nether is registered as a Ship Life world the moment the ship is built
 * in it, so all the position checks find what they expect.
 */
public final class Portals {
	private Portals() {
	}

	/** How long before the frame will take you again. */
	private static final int SETTLE = 100;

	private static final java.util.Map<java.util.UUID, Long> WENT =
			new java.util.HashMap<>();

	public static void register() {
		// The frame takes you itself.
		//
		// It used to be an ordinary nether portal doing an ordinary nether
		// portal's job, and on this ship it did nothing you could see: the
		// blocks would not light in a frame the game had just been handed,
		// and the travel it does is not the travel this wants anyway. So
		// standing in the frame is the whole mechanism now -- no portal
		// block, no cooldown of the game's, no linking.
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK
				.register(server -> {
			if (server.getTickCount() % 10 != 0) {
				return;
			}
			for (ServerLevel level : server.getAllLevels()) {
				if (!ShipLifeMod.isShipLife(level)) {
					continue;
				}

				// Both frames stay lit, so the way off a ship is never a dead
				// hole in an obsidian ring.
				Ship.keepLit(level, Places.PORTAL);
				if (level.dimension() == Level.NETHER) {
					Ship.keepLit(level, Places.PORTAL_TWO);
				}

				// A copy, because stepping through takes the player out of
				// this level's list -- and changing the list underneath the
				// loop that is walking it crashed the server.
				for (ServerPlayer player : java.util.List.copyOf(level.players())) {
					if (!inTheFrame(player)) {
						WENT.remove(player.getUUID());
						continue;
					}
					Long last = WENT.get(player.getUUID());
					if (last != null && level.getGameTime() - last < SETTLE) {
						continue;
					}
					WENT.put(player.getUUID(), level.getGameTime());
					step(player, level);
				}
			}
		});

		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
				(player, origin, destination) -> {
			if (destination.dimension() == Level.NETHER) {
				arrive(player, destination);
			} else if (origin.dimension() == Level.NETHER
					&& destination.dimension() == Level.OVERWORLD) {
				comeBack(player, destination);
			}
		});
	}

	/**
	 * Are they standing in a frame?
	 *
	 * Ship 1 has one, on floor 18. Ship 2 has that one and another on floor 1,
	 * where the portal puts you down, so the way home is where you arrive.
	 */
	private static boolean inTheFrame(ServerPlayer player) {
		if (standingIn(player, Places.PORTAL)) {
			return true;
		}
		return player.level().dimension() == Level.NETHER
				&& standingIn(player, Places.PORTAL_TWO);
	}

	private static boolean standingIn(ServerPlayer player, BlockPos frame) {
		return Math.abs(player.getX() - (frame.getX() + 0.5)) < 2.0
				&& Math.abs(player.getZ() - (frame.getZ() + 0.5)) < 1.2
				&& player.getY() >= frame.getY() - 0.5
				&& player.getY() <= frame.getY() + 3.5;
	}

	/**
	 * The lift's way across, which is the frame's way across.
	 *
	 * Once you have stood on ship 2's floor 1 the lift will take you between
	 * the ships without walking you back to a portal first.
	 */
	public static void cross(ServerPlayer player) {
		if (player.level() instanceof ServerLevel level && ShipLifeMod.isShipLife(level)) {
			step(player, level);
		}
	}

	/** Through it: to the Nether from home, and home from the Nether. */
	private static void step(ServerPlayer player, ServerLevel from) {
		var server = from.getServer();
		if (from.dimension() == Level.OVERWORLD) {
			ServerLevel nether = server.getLevel(Level.NETHER);
			if (nether == null) {
				player.sendSystemMessage(Component.literal(
						"The portal is dark. This world has no Nether.")
						.withStyle(ChatFormatting.RED));
				return;
			}
			Ship.buildInTheNether(nether);
			ShipLifeMod.claim(nether);
			BlockPos lobby = Places.lift(1);
			player.teleportTo(nether, lobby.getX() + 0.5, lobby.getY(), lobby.getZ() + 0.5,
					java.util.Set.of(), player.getYRot(), player.getXRot(), true);
			WENT.put(player.getUUID(), nether.getGameTime());
			nether.playSound(null, lobby, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS,
					0.4f, 1.0f);
			Pets.bringThemAlong(player, nether);
			State.unlockTwo(player, 1);
			player.sendSystemMessage(Component.literal(
					"Ship 2, floor 1 -- the pool deck, floating in the Nether.")
					.withStyle(ChatFormatting.LIGHT_PURPLE));
			Log.write(player, "went through the portal");
			return;
		}

		ServerLevel home = server.overworld();
		BlockPos back = Places.PORTAL.south(2);
		player.teleportTo(home, back.getX() + 0.5, back.getY(), back.getZ() + 0.5,
				java.util.Set.of(), player.getYRot(), player.getXRot(), true);
		WENT.put(player.getUUID(), home.getGameTime());
		Pets.bringThemAlong(player, home);
		player.sendSystemMessage(Component.literal("Floor 18, and home.")
				.withStyle(ChatFormatting.AQUA));
	}

	/**
	 * Off the ship and onto the other one.
	 *
	 * The ship is built the first time somebody comes through rather than
	 * with the world, because most worlds will never get to floor 18 and a
	 * whole second tower is a lot of blocks to lay down on the chance.
	 */
	private static void arrive(ServerPlayer player, ServerLevel nether) {
		ServerLevel home = nether.getServer().overworld();
		if (!ShipLifeMod.isShipLife(home)) {
			return;                       // an ordinary world's Nether
		}
		Ship.buildInTheNether(nether);
		ShipLifeMod.claim(nether);

		BlockPos lobby = Places.lift(1);
		player.teleportTo(lobby.getX() + 0.5, lobby.getY(), lobby.getZ() + 0.5);
		Pets.bringThemAlong(player, nether);
		State.unlockTwo(player, 1);
		nether.playSound(null, lobby, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS,
				0.4f, 1.0f);
		player.sendSystemMessage(Component.literal(
				"Ship 2, floor 1. It is the same ship, and it is floating in the Nether.")
				.withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	/** And back again, at the frame you left through. */
	private static void comeBack(ServerPlayer player, ServerLevel overworld) {
		if (!ShipLifeMod.isShipLife(overworld)) {
			return;
		}
		BlockPos back = Places.PORTAL.south(2);
		player.teleportTo(back.getX() + 0.5, back.getY(), back.getZ() + 0.5);
		Pets.bringThemAlong(player, overworld);
		player.sendSystemMessage(Component.literal("Floor 18, and home.")
				.withStyle(ChatFormatting.AQUA));
	}
}
