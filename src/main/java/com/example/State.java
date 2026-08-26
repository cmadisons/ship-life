package com.example;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.server.level.ServerPlayer;

/**
 * Everything the game remembers about you.
 *
 * All of it hangs off the player and is saved with them, so it survives dying
 * and logging out. Money is kept in cents rather than dollars because the very
 * first quest pays $94.99 and $0.01, and a penny that rounds away would break
 * the one sum the whole chapter is built on.
 *
 * Quest progress is three numbers, not a list: which quest you are on, which
 * part of it, and a counter for that part. Nearly every part is "do this thing
 * N times" -- wash 10 dishes, whack 10 weeds -- so one counter covers them. The
 * one part that isn't (find four things in your room) uses the same counter as
 * four bits instead.
 */
public final class State {
	private State() {
	}

	private static <T> AttachmentType<T> of(String name, T start, Codec<T> codec) {
		return AttachmentRegistry.<T>builder()
				.initializer(() -> start)
				.persistent(codec)
				.copyOnDeath()
				.buildAndRegister(ShipLifeMod.id(name));
	}

	/** Money, in cents. */
	public static final AttachmentType<Integer> MONEY = of("money", 0, Codec.INT);

	/** Arcade tickets. */
	public static final AttachmentType<Integer> ARCADE = of("arcade_tickets", 0, Codec.INT);

	/** Event tickets. These are allowed to go below zero. */
	public static final AttachmentType<Integer> EVENT = of("event_tickets", 0, Codec.INT);

	/** Which quest you are on, counting from zero. */
	public static final AttachmentType<Integer> QUEST = of("quest", 0, Codec.INT);

	/** Which part of that quest. */
	public static final AttachmentType<Integer> PART = of("part", 0, Codec.INT);

	/** How far into that part you are. */
	public static final AttachmentType<Integer> COUNT = of("count", 0, Codec.INT);

	/** Your best Pac-Man score. Beating it is what pays. */
	public static final AttachmentType<Integer> BEST = of("pacman_best", 0, Codec.INT);

	/** Which floors your passport opens, one bit per floor. */
	public static final AttachmentType<Integer> FLOORS = of("floors", 0, Codec.INT);

	/** Whether the ship has told you where to go yet, one bit per call. */
	public static final AttachmentType<Integer> CALLS = of("calls", 0, Codec.INT);

	// ---------------------------------------------------------------- getters

	public static int money(ServerPlayer player) {
		return player.getAttachedOrCreate(MONEY);
	}

	public static void pay(ServerPlayer player, int cents) {
		player.setAttached(MONEY, money(player) + cents);
	}

	public static int arcade(ServerPlayer player) {
		return player.getAttachedOrCreate(ARCADE);
	}

	public static void arcade(ServerPlayer player, int change) {
		player.setAttached(ARCADE, arcade(player) + change);
	}

	public static int event(ServerPlayer player) {
		return player.getAttachedOrCreate(EVENT);
	}

	public static void event(ServerPlayer player, int change) {
		player.setAttached(EVENT, event(player) + change);
	}

	public static int best(ServerPlayer player) {
		return player.getAttachedOrCreate(BEST);
	}

	public static void best(ServerPlayer player, int score) {
		player.setAttached(BEST, score);
	}

	public static int quest(ServerPlayer player) {
		return player.getAttachedOrCreate(QUEST);
	}

	public static int part(ServerPlayer player) {
		return player.getAttachedOrCreate(PART);
	}

	public static int count(ServerPlayer player) {
		return player.getAttachedOrCreate(COUNT);
	}

	public static void count(ServerPlayer player, int value) {
		player.setAttached(COUNT, value);
	}

	/** Money written the way a price tag writes it: $94.99. */
	public static String dollars(int cents) {
		return String.format("$%d.%02d", cents / 100, Math.abs(cents % 100));
	}

	// ----------------------------------------------------------------- floors

	public static boolean hasFloor(ServerPlayer player, int floor) {
		return (player.getAttachedOrCreate(FLOORS) & (1 << floor)) != 0;
	}

	public static void unlock(ServerPlayer player, int floor) {
		player.setAttached(FLOORS, player.getAttachedOrCreate(FLOORS) | (1 << floor));
	}

	/** Has this one-off message already been sent? Marks it sent if not. */
	public static boolean firstTime(ServerPlayer player, int which) {
		int seen = player.getAttachedOrCreate(CALLS);
		if ((seen & (1 << which)) != 0) {
			return false;
		}
		player.setAttached(CALLS, seen | (1 << which));
		return true;
	}
}
