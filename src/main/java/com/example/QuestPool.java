package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * The two hundred and fifty side quests.
 *
 * QUESTS.md has them written out in prose, and every one of them is here as a
 * thing the game can actually watch you do: a count to reach and what it pays
 * in event tickets. A quest the mod cannot see you finish would be a checkbox
 * you tick yourself, which is not a quest.
 *
 * They come from two places -- the free one at a time from the store on floor
 * 8, and three at once for twenty-five arcade tickets at the prize counter,
 * which is why the counter waits for floors 8, 9 and 10: the pool is mostly
 * about the fight room, the boss room and the shops.
 */
public final class QuestPool {
	private QuestPool() {
	}

	/** One quest: a thing to reach, how much more of it, and what it pays. */
	public record Entry(QuestDay.Stat stat, int amount, int tickets) {
		public String text() {
			return stat.absolute()
					? "Get your " + stat.label + " to " + amount
					: amount + " more " + stat.label;
		}
	}

	/** The whole pool, built once. */
	public static final List<Entry> ALL = build();

	/**
	 * Two hundred and fifty of them, laid out the way the written list is:
	 * a run of each kind, getting steeper, and paying more as it goes.
	 */
	private static List<Entry> build() {
		List<Entry> pool = new ArrayList<>();
		// stat, how many steps, the step size, the pay per step
		add(pool, QuestDay.Stat.SNAKE_FOOD, 25, 6, 12);
		add(pool, QuestDay.Stat.GALAGA_ROUNDS, 25, 2, 15);
		add(pool, QuestDay.Stat.ARCADE, 25, 8, 10);
		add(pool, QuestDay.Stat.LAPS, 25, 2, 14);
		add(pool, QuestDay.Stat.PETS, 25, 1, 25);
		add(pool, QuestDay.Stat.WAVES, 25, 1, 30);
		add(pool, QuestDay.Stat.RACES, 25, 1, 22);
		add(pool, QuestDay.Stat.BOSSES, 25, 1, 60);
		add(pool, QuestDay.Stat.EVENT, 25, 60, 18);
		add(pool, QuestDay.Stat.MONEY, 25, 40, 16);
		return List.copyOf(pool);
	}

	private static void add(List<Entry> pool, QuestDay.Stat stat, int howMany,
			int step, int payPerStep) {
		for (int i = 1; i <= howMany; i++) {
			int amount = step * i;
			pool.add(new Entry(stat, amount, Math.min(300, payPerStep * i)));
		}
	}

	/** Take one at random and write it down against where you are now. */
	public static void give(ServerPlayer player, int howMany) {
		Random random = new Random();
		StringBuilder carrying = new StringBuilder(State.side(player));
		for (int i = 0; i < howMany; i++) {
			Entry entry = ALL.get(random.nextInt(ALL.size()));
			int target = entry.stat().absolute()
					? Math.max(entry.amount(), entry.stat().of(player) + 1)
					: entry.stat().of(player) + entry.amount();
			if (!carrying.isEmpty()) {
				carrying.append(',');
			}
			carrying.append(entry.stat().ordinal()).append(':').append(target)
					.append(':').append(entry.tickets());
			player.sendSystemMessage(Component.literal("Quest: " + entry.text()
					+ "  --  " + entry.tickets() + " event tickets")
					.withStyle(ChatFormatting.YELLOW));
		}
		State.side(player, carrying.toString());
	}

	/** How many you are carrying. */
	public static int carrying(ServerPlayer player) {
		String side = State.side(player);
		return side.isEmpty() ? 0 : side.split(",").length;
	}

	/** Anything finished? Pay for it and take it off the list. */
	public static void check(ServerPlayer player) {
		String side = State.side(player);
		if (side.isEmpty()) {
			return;
		}
		List<String> left = new ArrayList<>();
		for (String part : side.split(",")) {
			String[] bits = part.split(":");
			QuestDay.Stat stat = QuestDay.Stat.values()[Integer.parseInt(bits[0])];
			int target = Integer.parseInt(bits[1]);
			int tickets = Integer.parseInt(bits[2]);
			if (stat.of(player) >= target) {
				Events.payTickets(player, tickets, "a side quest done");
			} else {
				left.add(part);
			}
		}
		State.side(player, String.join(",", left));
	}

	/** What you are carrying, in words, for the book to show. */
	public static List<String> lines(ServerPlayer player) {
		List<String> lines = new ArrayList<>();
		String side = State.side(player);
		if (side.isEmpty()) {
			return lines;
		}
		for (String part : side.split(",")) {
			String[] bits = part.split(":");
			QuestDay.Stat stat = QuestDay.Stat.values()[Integer.parseInt(bits[0])];
			int target = Integer.parseInt(bits[1]);
			lines.add(stat.of(player) + " / " + target + "  " + stat.label
					+ "  (" + bits[2] + " tickets)");
		}
		return lines;
	}
}
