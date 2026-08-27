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
 * A lap is the length of the pool and back, timed from the moment you push
 * off. Fifteen seconds is the number that matters, and a bare lap is nowhere
 * near it -- the dog's swimming boost is not a way round that time, it is the
 * way to it. That is what the dog was always for.
 *
 * The clock is kept where the swimming happens rather than in a stopwatch you
 * carry: touch the near end to start, touch the far end to turn, touch the
 * near end again and that is your time.
 */
public final class Pool {
	private Pool() {
	}


	/** Where in a lap someone is. */
	private record Swim(int leg, long started) {
	}

	/** A lap in fifteen seconds is what opens floor 9. */
	public static final int TARGET_TICKS = 300;

	private static final Map<UUID, Swim> SWIMMING = new HashMap<>();

	public static void register() {
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (player instanceof ServerPlayer who && world instanceof ServerLevel level
					&& ShipLifeMod.isShipLife(level)
					&& (Places.local(hit.getBlockPos()).equals(Places.POOL_BOARD)
							|| Places.local(hit.getBlockPos()).equals(Places.POOL_BOARD.above()))) {
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
		if (!Places.inPool(player.getX(), player.getY(), player.getZ())
				|| !player.isInWater()) {
			SWIMMING.remove(player.getUUID());
			return;
		}
		int x = (int) Math.round(Places.localX(player.getX()));
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

		State.add(player, State.LAPS, 1);
		int best = State.bestLap(player);            // your best before this one
		boolean record = best == 0 || ticks < best;
		boolean boosted = Pets.swimBoosted(player);

		player.sendSystemMessage(Component.literal("Lap: " + time(ticks)
				+ (record ? "  --  a new record." : "  --  your best is " + time(best) + "."))
				.withStyle(record ? ChatFormatting.GREEN : ChatFormatting.GRAY));

		if (!State.hasFloor(player, 9)) {
			floorNine(player, ticks);
		}
		if (record) {
			State.bestLap(player, ticks);
		}
	}

	/**
	 * Floor 9 is a flat fifteen seconds, and the dog is allowed.
	 *
	 * A bare lap is nowhere near it, so the swimming boost is not a way round
	 * the time -- it is the way to it, which is what the dog was always for.
	 */
	private static void floorNine(ServerPlayer player, int ticks) {
		if (ticks > TARGET_TICKS) {
			player.sendSystemMessage(Component.literal("Floor 9 wants a lap in "
					+ time(TARGET_TICKS) + ". You will need a dog for that.")
					.withStyle(ChatFormatting.GRAY));
			return;
		}
		State.unlock(player, 9);
		player.sendSystemMessage(Component.literal("A lap in " + time(ticks)
				+ ". Floor 9 -- the fight room -- is open, and it is in your lift"
				+ " from now on.").withStyle(ChatFormatting.AQUA));
	}

	/** The record board on the wall. */
	private static void records(ServerPlayer player) {
		State.add(player, State.LAPS, 1);
		int best = State.bestLap(player);
		player.sendSystemMessage(Component.literal("Pool records -- "
				+ (best == 0 ? "you have not done a lap yet."
						: "your best lap is " + time(best) + "."))
				.withStyle(ChatFormatting.AQUA));
		player.sendSystemMessage(Component.literal("A lap is the length of the pool"
				+ " and back. Fifteen seconds opens floor 9.")
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
