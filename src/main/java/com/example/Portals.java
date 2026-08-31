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

	public static void register() {
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
