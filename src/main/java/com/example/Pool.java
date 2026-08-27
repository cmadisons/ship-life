package com.example;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;

/**
 * Floor 3: the swimming pool.
 *
 * A lap is the length of the pool and back, and it is timed from the moment
 * you push off. Thirty seconds is the number that matters -- do a lap in
 * thirty and floor 9 opens, and the dog's swimming boost is what turns a
 * thirty-five second lap into a thirty second one. That is the point of the
 * dog: the quest is set just out of reach without it.
 *
 * The clock is kept where the swimming happens rather than in a stopwatch you
 * carry: touch the near end to start, touch the far end to turn, touch the
 * near end again and that is your time.
 */
public final class Pool {
	private Pool() {
	}

	/** A lap in thirty seconds is what opens floor 9. */
	public static final int TARGET_TICKS = 600;

	/** Where in a lap someone is. */
	private record Swim(int leg, long started) {
	}

	private static final Map<UUID, Swim> SWIMMING = new HashMap<>();

	public static void register() {
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (player instanceof ServerPlayer who && world instanceof ServerLevel level
					&& ShipLifeMod.isShipLife(level)
					&& (hit.getBlockPos().equals(Places.POOL_BOARD)
							|| hit.getBlockPos().equals(Places.POOL_BOARD.above()))) {
				records(who);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 2 != 0) {
				return;
			}
			for (ServerLevel level : server.getAllLevels()) {
				if (!ShipLifeMod.isShipLife(level)) {
					continue;
				}
				for (ServerPlayer player : level.players()) {
					swim(player);
				}
			}
		});
	}

	private static void swim(ServerPlayer player) {
		if (Places.floorAt(player.getY()) != 3 || !player.isInWater()) {
			SWIMMING.remove(player.getUUID());
			return;
		}
		int x = (int) Math.round(player.getX());
		boolean nearEnd = x <= Places.POOL_START + 1;
		boolean farEnd = x >= Places.POOL_END - 1;
		long now = player.level().getGameTime();
		Swim swim = SWIMMING.get(player.getUUID());

		if (swim == null) {
			if (nearEnd) {
				SWIMMING.put(player.getUUID(), new Swim(1, now));
				say(player, "Go.");
			}
			return;
		}

		if (swim.leg() == 1) {
			if (farEnd) {
				SWIMMING.put(player.getUUID(), new Swim(2, swim.started()));
				say(player, "Turn -- " + time(now - swim.started()) + " on the way out");
			} else {
				say(player, "Swimming -- " + time(now - swim.started()));
			}
			return;
		}

		if (nearEnd) {
			SWIMMING.remove(player.getUUID());
			finished(player, (int) (now - swim.started()));
		} else {
			say(player, "Coming back -- " + time(now - swim.started()));
		}
	}

	private static void finished(ServerPlayer player, int ticks) {
		player.level().playSound(null, player.blockPosition(),
				SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.8f, 1.4f);

		int best = State.bestLap(player);
		boolean record = best == 0 || ticks < best;
		if (record) {
			State.bestLap(player, ticks);
		}
		player.sendSystemMessage(Component.literal("Lap: " + time(ticks)
				+ (record ? "  --  a new record." : "  --  your best is " + time(best) + "."))
				.withStyle(record ? ChatFormatting.GREEN : ChatFormatting.GRAY));

		if (ticks <= TARGET_TICKS && !State.hasFloor(player, 9)) {
			State.unlock(player, 9);
			player.sendSystemMessage(Component.literal(
					"Under thirty seconds. Floor 9 -- the fight room -- is open.")
					.withStyle(ChatFormatting.AQUA));
		} else if (ticks > TARGET_TICKS && !State.hasFloor(player, 9)) {
			player.sendSystemMessage(Component.literal(
					"Thirty seconds opens floor 9. A dog helps -- that is what it is for.")
					.withStyle(ChatFormatting.GRAY));
		}
	}

	/** The record board on the wall. */
	private static void records(ServerPlayer player) {
		int best = State.bestLap(player);
		player.sendSystemMessage(Component.literal("Pool records -- "
				+ (best == 0 ? "you have not done a lap yet."
						: "your best lap is " + time(best) + "."))
				.withStyle(ChatFormatting.AQUA));
		player.sendSystemMessage(Component.literal(
				"A lap is the length of the pool and back. Thirty seconds opens floor 9.")
				.withStyle(ChatFormatting.GRAY));
	}

	/** Ticks as a stopwatch reads them. */
	public static String time(long ticks) {
		return String.format("%.1fs", ticks / 20.0);
	}

	private static void say(ServerPlayer player, String text) {
		Hud.busy(player, 10);
		player.sendOverlayMessage(Component.literal(text).withStyle(ChatFormatting.AQUA));
	}
}
