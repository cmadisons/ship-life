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

	/** Your best lap in the pool, in ticks. Zero until you swim one. */
	public static final AttachmentType<Integer> BEST_LAP = of("best_lap", 0, Codec.INT);

	/** Running tallies, for the quests that ask you to do a thing N times. */
	public static final AttachmentType<Integer> FOODS = of("snake_foods", 0, Codec.INT);
	public static final AttachmentType<Integer> ROUNDS = of("galaga_rounds", 0, Codec.INT);
	public static final AttachmentType<Integer> LAPS = of("pool_laps", 0, Codec.INT);
	public static final AttachmentType<Integer> EARNED = of("tickets_earned", 0, Codec.INT);
	public static final AttachmentType<Integer> WAVES = of("waves_cleared", 0, Codec.INT);
	public static final AttachmentType<Integer> BOSSES = of("bosses_beaten", 0, Codec.INT);
	public static final AttachmentType<Integer> RACES = of("races_finished", 0, Codec.INT);
	public static final AttachmentType<Integer> EVENT_EARNED = of("event_earned", 0, Codec.INT);

	/** Side quests you are carrying, as "stat:target:tickets" separated by commas. */
	public static final AttachmentType<String> SIDE = of("side_quests", "", Codec.STRING);

	/** How many ships your passport is good for. One until it is upgraded. */
	public static final AttachmentType<Integer> SHIPS = of("ships", 1, Codec.INT);

	/** The in-game month you last took the floor 11 reward in. Zero for never. */
	public static final AttachmentType<Integer> REWARD_MONTH = of("reward_month", 0, Codec.INT);

	/** Which phone shops you can call, one bit each: store, arcade, event. */
	public static final AttachmentType<Integer> PHONES = of("phone_shops", 0, Codec.INT);

	/** Food boosts running today: swim, race, fight -- as the day they run out. */
	public static final AttachmentType<String> BOOSTS = of("keg_boosts", "", Codec.STRING);

	/** Events left on a x2.5, and whether it is the permanent one. */
	public static final AttachmentType<Integer> MULTIPLIER = of("multiplier_left", 0, Codec.INT);
	public static final AttachmentType<Integer> FOREVER = of("multiplier_forever", 0, Codec.INT);

	/** Pet food eaten, per pet kind, as "kind:count" pairs. */
	public static final AttachmentType<String> PET_FOOD = of("pet_food", "", Codec.STRING);

	/** The four quests running today, as "stat:target:amount" four times over. */
	public static final AttachmentType<String> QUEST_DAY = of("quest_day", "", Codec.STRING);

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

	public static int bestLap(ServerPlayer player) {
		return player.getAttachedOrCreate(BEST_LAP);
	}

	public static void bestLap(ServerPlayer player, int ticks) {
		player.setAttached(BEST_LAP, ticks);
	}

	public static int tally(ServerPlayer player, AttachmentType<Integer> what) {
		return player.getAttachedOrCreate(what);
	}

	public static void add(ServerPlayer player, AttachmentType<Integer> what, int by) {
		player.setAttached(what, tally(player, what) + by);
	}

	public static String side(ServerPlayer player) {
		return player.getAttachedOrCreate(SIDE);
	}

	public static void side(ServerPlayer player, String quests) {
		player.setAttached(SIDE, quests);
	}

	public static String boosts(ServerPlayer player) {
		return player.getAttachedOrCreate(BOOSTS);
	}

	public static void boosts(ServerPlayer player, String running) {
		player.setAttached(BOOSTS, running);
	}

	public static String petFood(ServerPlayer player) {
		return player.getAttachedOrCreate(PET_FOOD);
	}

	public static void petFood(ServerPlayer player, String fed) {
		player.setAttached(PET_FOOD, fed);
	}

	public static String questDay(ServerPlayer player) {
		return player.getAttachedOrCreate(QUEST_DAY);
	}

	public static void questDay(ServerPlayer player, String set) {
		player.setAttached(QUEST_DAY, set);
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
