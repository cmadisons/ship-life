package com.example;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * The star, the distance and the clock.
 *
 * All of it is written to the action bar rather than drawn, so it works
 * without a single line of rendering code and shows up the same on a server as
 * it does in single player. The star sits at the front of the line and is
 * coloured by how far away the quest is -- green under 500 blocks, yellow to
 * 999, red beyond that -- which is the same colour the book uses.
 *
 * Once you are close enough to see it, green sparks rise off the spot itself
 * so the star is somewhere in the world and not only on the screen.
 *
 * Anything that needs the action bar for itself -- the plunger meter, say --
 * calls {@link #busy} to keep this quiet for a moment.
 */
public final class Hud {
	private Hud() {
	}

	/** Players who have something more urgent on the action bar, and until when. */
	private static final java.util.Map<java.util.UUID, Long> BUSY = new java.util.HashMap<>();

	/** Keep the star off the action bar for this many ticks. */
	public static void busy(ServerPlayer player, int ticks) {
		BUSY.put(player.getUUID(), player.level().getGameTime() + ticks);
	}

	private static boolean isBusy(ServerPlayer player) {
		Long until = BUSY.get(player.getUUID());
		return until != null && player.level().getGameTime() < until;
	}

	/** The colour a distance is shown in. */
	public static ChatFormatting colourFor(double blocks) {
		if (blocks >= 1000) {
			return ChatFormatting.RED;
		}
		if (blocks >= 500) {
			return ChatFormatting.YELLOW;
		}
		return ChatFormatting.GREEN;
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			// Four times a second: smooth enough to watch a distance count down.
			if (server.getTickCount() % 5 != 0) {
				return;
			}
			for (ServerLevel level : server.getAllLevels()) {
				if (!ShipLifeMod.isShipLife(level)) {
					continue;
				}
				for (ServerPlayer player : level.players()) {
					show(player, level);
				}
			}
		});
	}

	private static void show(ServerPlayer player, ServerLevel level) {
		if (isBusy(player)) {
			return;
		}
		Quests.Part part = Quests.currentPart(player);
		Component line;
		if (part == null) {
			line = Component.literal("Every quest done  ")
					.withStyle(ChatFormatting.GREEN)
					.append(clockPart());
		} else {
			BlockPos where = part.where();
			int blocks = (int) Math.round(Math.sqrt(player.distanceToSqr(
					where.getX() + 0.5, where.getY(), where.getZ() + 0.5)));
			line = Component.literal("★ " + blocks + " blocks")
					.withStyle(colourFor(blocks))
					.append(Component.literal("  ·  " + part.todo() + "  ")
							.withStyle(ChatFormatting.WHITE))
					.append(clockPart());
			if (blocks < 48) {
				level.sendParticles(player, ParticleTypes.HAPPY_VILLAGER, true,
						true, where.getX() + 0.5, where.getY() + 1.6, where.getZ() + 0.5,
						2, 0.2, 0.4, 0.2, 0.0);
			}
		}
		player.sendOverlayMessage(line);
	}

	/** The clock and your money, at the end of the line. */
	private static Component clockPart() {
		return Component.literal("·  " + Cal.clock() + "  " + Cal.dayOfMonth()
				+ " " + Cal.month()).withStyle(ChatFormatting.GRAY);
	}
}
