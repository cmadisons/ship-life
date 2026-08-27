package com.example;

import java.util.Random;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Quest Day, every other Monday.
 *
 * Four quests -- a super easy, an easy, a medium and a hard -- and five
 * hundred event tickets for finishing all four. Do that and four more arrive,
 * worth another five hundred, for as long as the day lasts.
 *
 * The quests here are the ones the game can actually check. QUESTS.md has six
 * hundred and fifty written, but most of them are about the race track, the
 * bosses and the shops, and a quest the mod cannot see you finish is a quest
 * on the honour system. As those floors get built their quests move in here.
 */
public final class QuestDay {
	private QuestDay() {
	}

	/** Something the game keeps count of. */
	public enum Stat {
		SNAKE_FOOD("food eaten in Snake"),
		GALAGA_ROUNDS("rounds passed in Galaga"),
		PACMAN_BEST("Pac-Man record"),
		ARCADE("arcade tickets earned"),
		LAPS("laps swum"),
		PETS("pets owned"),
		WAVES("waves cleared on floor 9"),
		BOSSES("bosses beaten"),
		RACES("races finished"),
		EVENT("event tickets earned"),
		MONEY("dollars");

		public final String label;

		Stat(String label) {
			this.label = label;
		}

		/** Where you are now. */
		public int of(ServerPlayer player) {
			return switch (this) {
				case SNAKE_FOOD -> State.tally(player, State.FOODS);
				case GALAGA_ROUNDS -> State.tally(player, State.ROUNDS);
				case PACMAN_BEST -> State.best(player);
				case ARCADE -> State.tally(player, State.EARNED);
				case LAPS -> State.tally(player, State.LAPS);
				case PETS -> Pets.total(player);
				case WAVES -> State.tally(player, State.WAVES);
				case BOSSES -> State.tally(player, State.BOSSES);
				case RACES -> State.tally(player, State.RACES);
				case EVENT -> State.tally(player, State.EVENT_EARNED);
				case MONEY -> State.money(player) / 100;
			};
		}

		/** A record is a height to reach, not a number to add to. */
		public boolean absolute() {
			return this == PACMAN_BEST || this == PETS || this == MONEY;
		}
	}

	private record Goal(Stat stat, int amount) {
		String text() {
			return stat.absolute()
					? "Get your " + stat.label + " to " + amount
					: amount + " " + stat.label;
		}
	}

	private static final Goal[][] TIERS = {
			// super easy
			{ new Goal(Stat.SNAKE_FOOD, 3), new Goal(Stat.GALAGA_ROUNDS, 1),
			  new Goal(Stat.LAPS, 1), new Goal(Stat.ARCADE, 3) },
			// easy
			{ new Goal(Stat.SNAKE_FOOD, 10), new Goal(Stat.GALAGA_ROUNDS, 3),
			  new Goal(Stat.LAPS, 3), new Goal(Stat.ARCADE, 10),
			  new Goal(Stat.PETS, 1) },
			// medium
			{ new Goal(Stat.SNAKE_FOOD, 25), new Goal(Stat.GALAGA_ROUNDS, 6),
			  new Goal(Stat.LAPS, 8), new Goal(Stat.ARCADE, 40),
			  new Goal(Stat.PACMAN_BEST, 15) },
			// hard
			{ new Goal(Stat.SNAKE_FOOD, 60), new Goal(Stat.GALAGA_ROUNDS, 12),
			  new Goal(Stat.LAPS, 20), new Goal(Stat.ARCADE, 120),
			  new Goal(Stat.PACMAN_BEST, 30) },
	};

	private static final String[] TIER_NAMES = { "Super easy", "Easy", "Medium", "Hard" };

	/** What finishing all four is worth. */
	public static final int PAYOUT = 500;

	/** Open the board. Rolls a fresh set if there isn't one running. */
	public static void open(ServerPlayer player) {
		if (State.questDay(player).isEmpty()) {
			roll(player);
		}
		check(player);
		show(player);
	}

	/** Four quests, one of each tier, and the numbers you have to reach. */
	private static void roll(ServerPlayer player) {
		Random random = new Random();
		StringBuilder set = new StringBuilder();
		for (int tier = 0; tier < TIERS.length; tier++) {
			Goal goal = TIERS[tier][random.nextInt(TIERS[tier].length)];
			int target = goal.stat().absolute()
					? Math.max(goal.amount(), goal.stat().of(player) + 1)
					: goal.stat().of(player) + goal.amount();
			if (tier > 0) {
				set.append(',');
			}
			set.append(goal.stat().ordinal()).append(':').append(target)
					.append(':').append(goal.amount());
		}
		State.questDay(player, set.toString());
		player.sendSystemMessage(Component.literal("Quest Day: four quests. All four is "
				+ PAYOUT + " event tickets.").withStyle(ChatFormatting.AQUA));
	}

	/** Anything finished? And is the whole set done? */
	public static void check(ServerPlayer player) {
		String set = State.questDay(player);
		if (set.isEmpty()) {
			return;
		}
		boolean all = true;
		for (String part : set.split(",")) {
			if (!done(player, part)) {
				all = false;
			}
		}
		if (all) {
			Events.payTickets(player, PAYOUT, "all four Quest Day quests");
			State.questDay(player, "");
			roll(player);
		}
	}

	private static boolean done(ServerPlayer player, String part) {
		String[] bits = part.split(":");
		Stat stat = Stat.values()[Integer.parseInt(bits[0])];
		return stat.of(player) >= Integer.parseInt(bits[1]);
	}

	private static void show(ServerPlayer player) {
		SimpleContainer page = new SimpleContainer(54);
		ItemStack filler = Game.cell(Items.GRAY_STAINED_GLASS_PANE, " ");
		for (int slot = 0; slot < 54; slot++) {
			page.setItem(slot, filler.copy());
		}

		page.setItem(4, Book.entry(Items.WRITTEN_BOOK, "Quest Day", ChatFormatting.AQUA,
				"Four quests. All four pays " + PAYOUT + " event tickets,",
				"and then four more arrive.",
				"You have " + State.event(player) + " event tickets."));

		String[] parts = State.questDay(player).split(",");
		for (int i = 0; i < parts.length && i < 4; i++) {
			String[] bits = parts[i].split(":");
			Stat stat = Stat.values()[Integer.parseInt(bits[0])];
			int target = Integer.parseInt(bits[1]);
			int now = stat.of(player);
			boolean finished = now >= target;
			page.setItem(20 + i, Book.entry(
					finished ? Items.LIME_DYE : Items.PAPER,
					TIER_NAMES[i], finished ? ChatFormatting.GREEN : ChatFormatting.YELLOW,
					stat.absolute()
							? "Get your " + stat.label + " to " + target
							: "Reach " + target + " " + stat.label,
					"You are at " + now + ".",
					finished ? "Done." : "Still going."));
		}

		page.setItem(49, Book.entry(Items.BARRIER, "Close", ChatFormatting.RED,
				"Press Escape."));

		player.openMenu(new SimpleMenuProvider(
				(id, inventory, who) -> new ReadOnlyMenu(id, inventory, page,
						(clicker, slot) -> {
							if (slot == 49) {
								clicker.closeContainer();
							} else {
								check(clicker);
								show(clicker);
							}
						}),
				Component.literal("Quest Day")));
	}
}
