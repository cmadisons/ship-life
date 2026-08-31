package com.example;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * Floor 2 of ship 2: the maze.
 *
 * Ten cells square, and thirty seconds to get from the green corner to the
 * red one. Thirty seconds is not enough to find the way, and it is not meant
 * to be -- the maze is the same maze every time, drawn from a fixed seed, so
 * what the clock is really asking is whether you have learned it. Floor 3
 * opens when you have.
 *
 * The clock starts when you stand on the green and stops when you reach the
 * red. Stepping back on the green starts it again, so a bad run costs you
 * nothing but the walk back.
 */
public final class Maze {
	private Maze() {
	}

	/** Thirty seconds. */
	public static final int TARGET_TICKS = 600;

	/** When each player set off, for those who are running it. */
	private static final Map<UUID, Long> RUNNING = new HashMap<>();

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerLevel level : server.getAllLevels()) {
				// The maze is only ever on ship 2, which is the Nether one.
				if (!ShipLifeMod.isShipLife(level) || level.dimension() != Level.NETHER) {
					continue;
				}
				for (ServerPlayer player : level.players()) {
					run(player, level);
				}
			}
		});
	}

	private static void run(ServerPlayer player, ServerLevel level) {
		if (Places.floorAt(player.getY()) != 2) {
			RUNNING.remove(player.getUUID());
			return;
		}
		BlockPos feet = player.blockPosition();
		long now = level.getGameTime();
		Long started = RUNNING.get(player.getUUID());

		if (feet.getX() == Places.mazeStart().getX() && feet.getZ() == Places.mazeStart().getZ()) {
			// Standing on the green. Start, or start again after a bad run.
			if (started == null) {
				RUNNING.put(player.getUUID(), now);
				level.playSound(null, feet, SoundEvents.NOTE_BLOCK_BELL.value(),
						SoundSource.PLAYERS, 0.8f, 1.6f);
				player.sendSystemMessage(Component.literal("Go. "
						+ Pool.time(TARGET_TICKS) + " to the red corner.")
						.withStyle(ChatFormatting.GREEN));
			}
			return;
		}
		if (started == null) {
			return;
		}

		if (feet.getX() == Places.mazeEnd().getX() && feet.getZ() == Places.mazeEnd().getZ()) {
			RUNNING.remove(player.getUUID());
			finished(player, level, (int) (now - started));
			return;
		}
		// The clock on your screen, four times a second.
		if (now % 5 == 0) {
			long taken = now - started;
			player.sendOverlayMessage(Component.literal(Pool.time(taken))
					.withStyle(taken <= TARGET_TICKS
							? ChatFormatting.GREEN : ChatFormatting.RED));
		}
	}

	private static void finished(ServerPlayer player, ServerLevel level, int ticks) {
		level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(),
				SoundSource.PLAYERS, 0.9f, 1.4f);
		player.sendSystemMessage(Component.literal("Out in " + Pool.time(ticks) + ".")
				.withStyle(ticks <= TARGET_TICKS ? ChatFormatting.GREEN : ChatFormatting.GRAY));

		if (ticks > TARGET_TICKS) {
			player.sendSystemMessage(Component.literal("Floor 3 wants it in "
					+ Pool.time(TARGET_TICKS) + ". It is the same maze every time.")
					.withStyle(ChatFormatting.GRAY));
			return;
		}
		if (State.hasFloorTwo(player, 3)) {
			return;
		}
		State.unlockTwo(player, 3);
		player.sendSystemMessage(Component.literal(
				"Under thirty seconds. Floor 3 of this ship is open.")
				.withStyle(ChatFormatting.AQUA));
	}
}
