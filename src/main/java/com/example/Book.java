package com.example;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

/**
 * The Quest Book: what you have to do, what it pays, and how far away it is.
 *
 * One row per quest. The quest you are on is a written book and shows all of
 * its parts with the one you are on marked, because the deal from the start
 * was that you can see the whole job before you take it -- you just can't do
 * the third part before the first. Quests you have finished go grey, and ones
 * you haven't reached yet stay shut.
 *
 * Clicking a quest sets its star. There is only ever one star, so clicking a
 * different quest moves it rather than adding another.
 */
public final class Book {
	private Book() {
	}

	public static void open(ServerPlayer player) {
		SimpleContainer page = new SimpleContainer(54);
		ItemStack filler = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
		filler.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
		for (int slot = 0; slot < 54; slot++) {
			page.setItem(slot, filler.copy());
		}

		int here = State.quest(player);

		// Your money and the date, along the top.
		page.setItem(4, entry(Items.GOLD_NUGGET, "Your Money", ChatFormatting.GOLD,
				State.dollars(State.money(player)),
				State.arcade(player) + " arcade tickets",
				State.event(player) + " event tickets",
				"",
				Cal.date(),
				Events.running(player) == null
						? "Nothing on today."
						: "Today: " + Events.running(player)));

		// One quest per slot along the middle two rows.
		for (int index = 0; index < Quests.ALL.length && index < 14; index++) {
			int slot = 19 + (index / 7) * 9 + (index % 7);
			page.setItem(slot, questIcon(player, index, here));
		}

		// The ship itself: every floor, whether it is yours, and what opens it.
		java.util.List<String> floors = new ArrayList<>();
		for (int floor = 1; floor <= Places.TOP_FLOOR; floor++) {
			floors.add((State.hasFloor(player, floor) ? "✓ " : "☐ ")
					+ "Floor " + floor + " -- " + Floors.name(floor)
					+ (State.hasFloor(player, floor) ? "" : "  (" + Floors.how(floor) + ")"));
		}
		page.setItem(38, entry(Items.IRON_DOOR, "The Ship", ChatFormatting.AQUA,
				floors.toArray(new String[0])));

		java.util.List<String> side = QuestPool.lines(player);
		if (!side.isEmpty()) {
			java.util.List<String> lore = new ArrayList<>(side);
			lore.add("");
			lore.add("From the store on floor 8 and the prize counter.");
			page.setItem(40, entry(Items.PAPER, "Side Quests", ChatFormatting.YELLOW,
					lore.toArray(new String[0])));
		}

		// What your pets are actually doing, which was a number nobody could
		// see: the boost each one gives and how far up the food has taken it.
		java.util.List<String> pets = new ArrayList<>();
		for (Pets.Kind kind : Pets.Kind.values()) {
			int have = Pets.owned(player, kind);
			if (have == 0) {
				continue;
			}
			double strength = Pets.strength(player, kind);
			pets.add(have + "x " + kind.label + "  --  " + kind.what
					+ (strength > 1.0
							? String.format("  (fed to x%.1f)", strength)
							: "")
					+ (have > 1 && kind != Pets.Kind.CAT ? "  (doubled)" : ""));
		}
		if (pets.isEmpty()) {
			pets.add("You have no pets yet.");
			pets.add("Ten arcade tickets each, floor 2.");
		}
		page.setItem(44, entry(Items.BONE, "Your Pets", ChatFormatting.AQUA,
				pets.toArray(new String[0])));

		page.setItem(42, entry(Items.FILLED_MAP, "Map of the Ship", ChatFormatting.AQUA,
				"Every floor, drawn as a tower.",
				"Floor " + Places.floorAt(player.getY()) + " is where you are.",
				"",
				"Click to open it."));

		page.setItem(45, entry(Items.WRITTEN_BOOK, "Everything Else", ChatFormatting.YELLOW,
				"Your money, your tickets, your records,",
				"what you have beaten and what is on.",
				"",
				"Click for the second page."));
		page.setItem(49, entry(Items.BARRIER, "Close", ChatFormatting.RED, "Press Escape."));

		player.openMenu(new SimpleMenuProvider(
				(id, inventory, who) -> new ReadOnlyMenu(id, inventory, page,
						(clicker, slot) -> click(clicker, slot, here)),
				Component.literal("Quest Book")));
	}

	/**
	 * The second page.
	 *
	 * One chest is fifty-four squares and the quests fill most of them, so
	 * everything that is a number rather than a job lives through here: what
	 * you have, what you have done, and what is on today.
	 */
	public static void more(ServerPlayer player) {
		SimpleContainer page = new SimpleContainer(54);
		ItemStack filler = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
		filler.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
		for (int slot = 0; slot < 54; slot++) {
			page.setItem(slot, filler.copy());
		}

		page.setItem(4, entry(Items.CLOCK, Cal.date(), ChatFormatting.AQUA,
				Events.running(player) == null
						? "Nothing on today."
						: "Today: " + Events.running(player),
				"A day is 20 real minutes."));

		page.setItem(19, entry(Items.GOLD_NUGGET, "What You Have", ChatFormatting.GOLD,
				State.dollars(State.money(player)),
				State.arcade(player) + " arcade tickets",
				State.event(player) + " event tickets",
				State.tally(player, State.EVENT_SPENT) + " event tickets paid out"));

		page.setItem(21, entry(Items.IRON_SWORD, "The Fighting", ChatFormatting.RED,
				State.tally(player, State.WAVES) + " waves cleared",
				State.tally(player, State.BOSSES) + " bosses beaten",
				State.tally(player, State.BOMBS_USED) + " bombs used",
				"Floor 16 wants 3 waves, a boss, 6 bombs",
				"and 1500 tickets paid out."));

		page.setItem(23, entry(Items.GOLDEN_APPLE, "The Machines", ChatFormatting.YELLOW,
				"Pac-Man record: " + State.best(player),
				State.tally(player, State.FOODS) + " food eaten in Snake",
				State.tally(player, State.ROUNDS) + " Galaga rounds",
				State.tally(player, State.EARNED) + " arcade tickets earned"));

		page.setItem(25, entry(Items.HEART_OF_THE_SEA, "The Rest", ChatFormatting.AQUA,
				State.tally(player, State.LAPS) + " laps swum",
				"Best lap: " + (State.bestLap(player) == 0
						? "none yet" : Pool.time(State.bestLap(player))),
				State.tally(player, State.RACES) + " races finished",
				Gym.hearts(player) + " hearts from the gym"));

		page.setItem(45, entry(Items.WRITABLE_BOOK, "Back", ChatFormatting.YELLOW,
				"Back to the quests."));
		page.setItem(49, entry(Items.BARRIER, "Close", ChatFormatting.RED, "Press Escape."));

		player.openMenu(new SimpleMenuProvider(
				(id, inventory, who) -> new ReadOnlyMenu(id, inventory, page,
						(clicker, slot) -> {
							if (slot == 45) {
								open(clicker);
							} else if (slot == 49) {
								clicker.closeContainer();
							}
						}),
				Component.literal("Quest Book  --  Everything Else")));
	}

	/**
	 * The ship drawn as a ship.
	 *
	 * Sixteen floors will not fit in one column of a chest, so the tower is
	 * cut into three stacks of six and stood side by side, highest floor at
	 * the top left. Where you are is lit up, what you own is white, and what
	 * you do not own says what would open it.
	 */
	public static void map(ServerPlayer player) {
		SimpleContainer page = new SimpleContainer(54);
		ItemStack filler = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
		filler.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
		for (int slot = 0; slot < 54; slot++) {
			page.setItem(slot, filler.copy());
		}

		int on = Places.floorAt(player.getY());
		for (int floor = 1; floor <= Places.TOP_FLOOR; floor++) {
			// Highest floor top-left, counting down each stack of six.
			int fromTop = Places.TOP_FLOOR - floor;
			int column = 2 + (fromTop / 6) * 2;
			int row = fromTop % 6;
			boolean yours = State.hasFloor(player, floor);
			page.setItem(row * 9 + column, entry(
					floor == on ? Items.LIME_DYE : yours ? Items.WHITE_DYE : Items.IRON_DOOR,
					"Floor " + floor + " -- " + Floors.name(floor),
					floor == on ? ChatFormatting.GREEN
							: yours ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY,
					floor == on ? "You are here." : yours ? "Yours." : "Locked.",
					yours ? "" : "Opens with: " + Floors.how(floor)));
		}

		page.setItem(8, entry(Items.COMPASS, "The Ship", ChatFormatting.AQUA,
				Places.TOP_FLOOR + " floors.",
				"Highest at the top left, counting down.",
				"Green is where you are standing."));
		page.setItem(53, entry(Items.WRITABLE_BOOK, "Back", ChatFormatting.YELLOW,
				"Back to the Quest Book."));

		player.openMenu(new SimpleMenuProvider(
				(id, inventory, who) -> new ReadOnlyMenu(id, inventory, page,
						(clicker, slot) -> {
							if (slot == 53) {
								open(clicker);
							}
						}),
				Component.literal("Map of the Ship")));
	}

	private static ItemStack questIcon(ServerPlayer player, int index, int here) {
		Quests.Quest quest = Quests.ALL[index];
		List<String> lines = new ArrayList<>();
		lines.add(quest.chapter());
		lines.add("");

		if (index < here) {
			lines.add("Done.");
			lines.add("Reward: " + quest.reward());
			return entry(Items.BOOK, quest.name(), ChatFormatting.DARK_GRAY,
					lines.toArray(new String[0]));
		}
		if (index > here) {
			lines.add("Not yet. Finish the one before it.");
			return entry(Items.BOOK, quest.name(), ChatFormatting.DARK_GRAY,
					lines.toArray(new String[0]));
		}

		// The one you are on: every part, with the one you are on marked.
		int part = State.part(player);
		for (int i = 0; i < quest.parts().size(); i++) {
			Quests.Part step = quest.parts().get(i);
			String mark = i < part ? "✓ " : i == part ? "▶ " : "☐ ";
			String progress = "";
			if (i == part && step.need() > 1) {
				progress = "   (" + (Quests.on(player, 0, 1)
						&& player.level() instanceof net.minecraft.server.level.ServerLevel level
						? Chores.lawnLine(level)
						: State.count(player) + " / " + step.need()) + ")";
			}
			lines.add(mark + step.todo() + progress);
		}
		lines.add("");
		lines.add("Reward: " + quest.reward());
		Quests.Part step = quest.parts().get(part);
		BlockPos where = step.where();
		int blocks = (int) Math.round(Math.sqrt(player.distanceToSqr(
				where.getX() + 0.5, where.getY(), where.getZ() + 0.5)));
		lines.add(blocks + " blocks away");
		lines.add("");
		lines.add("Click to put the star on it.");

		return entry(Items.WRITTEN_BOOK, quest.name(), ChatFormatting.YELLOW,
				lines.toArray(new String[0]));
	}

	private static void click(ServerPlayer player, int slot, int here) {
		if (slot == 49) {
			player.closeContainer();
			return;
		}
		if (slot == 42) {
			map(player);
			return;
		}
		if (slot == 45) {
			more(player);
			return;
		}
		// Clicking the quest you are on re-points the star at it.
		Quests.Part part = Quests.currentPart(player);
		if (part != null) {
			BlockPos where = part.where();
			int blocks = (int) Math.round(Math.sqrt(player.distanceToSqr(
					where.getX() + 0.5, where.getY(), where.getZ() + 0.5)));
			player.sendSystemMessage(Component.literal("★ ")
					.withStyle(Hud.colourFor(blocks))
					.append(Component.literal(part.todo() + " -- " + blocks + " blocks")
							.withStyle(ChatFormatting.WHITE)));
		}
	}

	static ItemStack entry(Item item, String name, ChatFormatting colour, String... lines) {
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponents.CUSTOM_NAME,
				Component.literal(name).withStyle(colour).withStyle(style -> style.withItalic(false)));
		List<Component> lore = new ArrayList<>();
		for (String line : lines) {
			lore.add(Component.literal(line).withStyle(ChatFormatting.GRAY)
					.withStyle(style -> style.withItalic(false)));
		}
		stack.set(DataComponents.LORE, new ItemLore(lore));
		return stack;
	}
}
