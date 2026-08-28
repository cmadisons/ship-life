package com.example;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The shops: floor 8, floors 11 to 13, and the event ticket counter on 7.
 *
 * Floor 8 gives quests away rather than selling anything -- one at a time, so
 * it is a thing you come back to rather than a stack you hoard.
 *
 * Floor 11 is the one that pays you for turning up: a free reward once per
 * in-game month, which is ten real hours, rolled off a table that is mostly
 * phones and occasionally a floor. The one-in-a-thousand on it is a x2.5 on
 * event tickets that never runs out.
 *
 * Floors 12 and 13 sell what makes the rest easier: food that makes a pet
 * better, and food that makes you better, for a day.
 */
public final class Shops {
	private Shops() {
	}

	/** What the three floors cost together. */
	public static final int FLOORS_COST = 1000;

	/** The one-use jump to any event. */
	public static final int STAR_COST = 250;

	/** Two and a half times the tickets, for the next three events. */
	public static final int MULTIPLIER_COST = 1000;
	public static final int MULTIPLIER_EVENTS = 3;

	public static final int PET_FOOD_COST = 100;
	public static final int SKELETON_COST = 50;
	public static final int SHADOW_COST = 250;
	public static final int KEG_COST = 25;

	/** What the passport upgrade costs on floor 14. */
	public static final int PASSPORT_COST = 250;

	public static void register() {
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (!(player instanceof ServerPlayer who) || !(world instanceof ServerLevel level)
					|| !ShipLifeMod.isShipLife(level)) {
				return InteractionResult.PASS;
			}
			BlockPos pos = Places.local(hit.getBlockPos());
			if (at(pos, Places.STORE)) {
				store(who);
				return InteractionResult.SUCCESS;
			}
			if (at(pos, Places.TICKET_SHOP)) {
				ticketShop(who);
				return InteractionResult.SUCCESS;
			}
			if (at(pos, Places.REWARD_DESK)) {
				monthlyReward(who);
				return InteractionResult.SUCCESS;
			}
			if (at(pos, Places.PET_STORE)) {
				petStore(who);
				return InteractionResult.SUCCESS;
			}
			if (at(pos, Places.KEG)) {
				keg(who);
				return InteractionResult.SUCCESS;
			}
			if (at(pos, Places.PASSPORT_DESK)) {
				upgradePassport(who, level);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});

		// Side quests finish quietly, wherever you happen to be standing.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) {
				return;
			}
			for (ServerLevel level : server.getAllLevels()) {
				if (!ShipLifeMod.isShipLife(level)) {
					continue;
				}
				for (ServerPlayer player : level.players()) {
					QuestPool.check(player);
					QuestDay.check(player);
				}
			}
		});
	}

	private static boolean at(BlockPos hit, BlockPos shop) {
		return hit.equals(shop) || hit.equals(shop.above());
	}

	// ------------------------------------------------------- floor 8: quests

	private static void store(ServerPlayer player) {
		if (QuestPool.carrying(player) > 0) {
			player.sendSystemMessage(Component.literal(
					"One free quest at a time. Finish the one you have:")
					.withStyle(ChatFormatting.GRAY));
			for (String line : QuestPool.lines(player)) {
				player.sendSystemMessage(Component.literal("  " + line)
						.withStyle(ChatFormatting.YELLOW));
			}
			return;
		}
		QuestPool.give(player, 1);
	}

	// ---------------------------------------------- floor 7: the ticket shop

	private static void ticketShop(ServerPlayer player) {
		SimpleContainer page = blank();
		boolean hasFloors = State.hasFloor(player, 11);
		page.setItem(4, Book.entry(Items.GOLD_NUGGET, "Event Tickets",
				ChatFormatting.GOLD, State.event(player) + " event tickets"));
		page.setItem(20, Book.entry(Items.IRON_DOOR, "Floors 11, 12 and 13",
				hasFloors ? ChatFormatting.DARK_GRAY : ChatFormatting.AQUA,
				"Rewards, the pet store and The Keg.",
				FLOORS_COST + " event tickets, all three",
				hasFloors ? "Already yours." : "Click to buy."));
		page.setItem(22, Book.entry(Items.NETHER_STAR, "Go To Event Star",
				ChatFormatting.AQUA,
				"One use. Right-click it and pick any event --",
				"it is then on for the rest of the day.",
				STAR_COST + " event tickets",
				"Click to buy."));
		page.setItem(24, Book.entry(Items.EXPERIENCE_BOTTLE, "x2.5 Tickets",
				ChatFormatting.AQUA,
				"Two and a half times what events pay,",
				"for your next " + MULTIPLIER_EVENTS + " ticket events.",
				MULTIPLIER_COST + " event tickets",
				State.tally(player, State.FOREVER) > 0
						? "You already have it forever."
						: "Click to buy."));
		page.setItem(49, closeButton());
		open(player, page, "Event Ticket Shop", Shops::buyTickets);
	}

	private static void buyTickets(ServerPlayer player, int slot) {
		switch (slot) {
			case 20 -> {
				if (State.hasFloor(player, 11)) {
					return;
				}
				if (spend(player, FLOORS_COST)) {
					State.unlock(player, 11);
					State.unlock(player, 12);
					State.unlock(player, 13);
					player.sendSystemMessage(Component.literal(
							"Floors 11, 12 and 13 are open.").withStyle(ChatFormatting.AQUA));
					player.closeContainer();
				}
			}
			case 22 -> {
				if (spend(player, STAR_COST)) {
					player.getInventory().add(Kit.star());
					player.sendSystemMessage(Component.literal(
							"Right-click the star to pick an event.")
							.withStyle(ChatFormatting.AQUA));
					player.closeContainer();
				}
			}
			case 24 -> {
				if (spend(player, MULTIPLIER_COST)) {
					State.add(player, State.MULTIPLIER, MULTIPLIER_EVENTS);
					player.sendSystemMessage(Component.literal("x2.5 on your next "
							+ MULTIPLIER_EVENTS + " ticket events.")
							.withStyle(ChatFormatting.GREEN));
					player.closeContainer();
				}
			}
			case 49 -> player.closeContainer();
			default -> {
			}
		}
	}

	/** What a payout becomes once the multipliers have had a go at it. */
	public static int multiplied(ServerPlayer player, int tickets) {
		if (State.tally(player, State.FOREVER) > 0) {
			return (int) Math.round(tickets * 2.5);
		}
		if (State.tally(player, State.MULTIPLIER) > 0) {
			State.add(player, State.MULTIPLIER, -1);
			return (int) Math.round(tickets * 2.5);
		}
		return tickets;
	}

	// ------------------------------------------------- floor 11: the rewards

	/**
	 * One free reward a month, and a month is ten real hours.
	 *
	 * Seventy in a hundred is a phone -- being able to call a shop from
	 * anywhere is the thing you will use most, so it is the thing you get
	 * most. Four and nine tenths is floor 14. One in a thousand is a x2.5 on
	 * event tickets that never expires, which is the reason to keep coming.
	 */
	private static void monthlyReward(ServerPlayer player) {
		int month = (int) (Cal.dayNumber() / Cal.DAYS_IN_MONTH) + 1;
		if (State.tally(player, State.REWARD_MONTH) >= month) {
			player.sendSystemMessage(Component.literal(
					"You have had this month's. The next is " + Cal.DAYS_IN_MONTH
					+ " days away -- ten real hours.").withStyle(ChatFormatting.GRAY));
			return;
		}
		player.setAttached(State.REWARD_MONTH, month);
		reward(player);
	}

	/**
	 * Roll one reward and hand it over.
	 *
	 * The month gate is the caller's business, not this method's, so
	 * /11reward can take as many as it likes.
	 */
	public static void reward(ServerPlayer player) {
		double roll = new java.util.Random().nextDouble() * 100.0;
		if (roll < 0.1) {
			State.add(player, State.FOREVER, 1);
			player.sendSystemMessage(Component.literal(
					"One in a thousand: x2.5 on event tickets, forever.")
					.withStyle(ChatFormatting.LIGHT_PURPLE));
		} else if (roll < 5.0) {
			if (State.hasFloor(player, 14)) {
				State.add(player, State.MULTIPLIER, MULTIPLIER_EVENTS);
				player.sendSystemMessage(Component.literal(
						"Floor 14 again -- have a x2.5 instead.")
						.withStyle(ChatFormatting.GREEN));
			} else {
				State.unlock(player, 14);
				player.sendSystemMessage(Component.literal("Floor 14 is open.")
						.withStyle(ChatFormatting.LIGHT_PURPLE));
			}
		} else if (roll < 30.0) {
			State.add(player, State.MULTIPLIER, 1);
			player.sendSystemMessage(Component.literal(
					"x2.5 on the next event that pays tickets.")
					.withStyle(ChatFormatting.GREEN));
		} else {
			int which = new java.util.Random().nextInt(3);
			String name = switch (which) {
				case 0 -> "the store";
				case 1 -> "the arcade counter";
				default -> "the event shop";
			};
			player.setAttached(State.PHONES,
					State.tally(player, State.PHONES) | (1 << which));
			player.sendSystemMessage(Component.literal("A phone line to " + name
					+ ". Call it from anywhere with your phone.")
					.withStyle(ChatFormatting.GREEN));
		}
	}

	// ----------------------------------------------- floor 12: the pet store

	private static void petStore(ServerPlayer player) {
		SimpleContainer page = blank();
		page.setItem(4, Book.entry(Items.GOLD_NUGGET, "Event Tickets",
				ChatFormatting.GOLD, State.event(player) + " event tickets"));

		Pets.Kind[] kinds = Pets.Kind.values();
		for (int i = 0; i < kinds.length; i++) {
			Pets.Kind kind = kinds[i];
			page.setItem(19 + i, Book.entry(Items.BEEF, kind.label + " food",
					ChatFormatting.AQUA,
					"Makes your " + kind.label + " x1.1 better.",
					"It stacks, up to x2.",
					"Now: x" + String.format("%.2f", Pets.strength(player, kind)),
					PET_FOOD_COST + " event tickets",
					"Click to buy."));
		}

		page.setItem(30, Book.entry(Items.BONE, "Skeleton",
				ChatFormatting.AQUA, "A combat boost.",
				SKELETON_COST + " event tickets",
				"You have " + Pets.owned(player, Pets.Kind.SKELETON) + ".",
				"Click to buy."));
		page.setItem(32, Book.entry(Items.ENDER_PEARL, "Shadow",
				ChatFormatting.AQUA, "Adds 0.25 of every pet's boost.",
				SHADOW_COST + " event tickets",
				"You have " + Pets.owned(player, Pets.Kind.SHADOW) + ".",
				"Click to buy."));
		page.setItem(49, closeButton());
		open(player, page, "Pet Store", Shops::buyPet);
	}

	private static void buyPet(ServerPlayer player, int slot) {
		Pets.Kind[] kinds = Pets.Kind.values();
		if (slot >= 19 && slot < 19 + kinds.length) {
			Pets.feed(player, kinds[slot - 19]);
			petStore(player);
			return;
		}
		switch (slot) {
			case 30 -> {
				if (spend(player, SKELETON_COST)) {
					Pets.grant(player, Pets.Kind.SKELETON);
					player.closeContainer();
				}
			}
			case 32 -> {
				if (spend(player, SHADOW_COST)) {
					Pets.grant(player, Pets.Kind.SHADOW);
					player.closeContainer();
				}
			}
			case 49 -> player.closeContainer();
			default -> {
			}
		}
	}

	// --------------------------------------------------- floor 13: The Keg

	private static void keg(ServerPlayer player) {
		SimpleContainer page = blank();
		page.setItem(4, Book.entry(Items.GOLD_NUGGET, "The Keg", ChatFormatting.GOLD,
				State.event(player) + " event tickets",
				"Every dish lasts one day -- 20 real minutes."));
		page.setItem(20, Book.entry(Items.POTION, "Water", ChatFormatting.AQUA,
				"Swim faster, all day.", KEG_COST + " event tickets",
				running(player, "swim") ? "Running now." : "Click to order."));
		page.setItem(22, Book.entry(Items.COOKED_BEEF, "The racing dish",
				ChatFormatting.AQUA, "More gas, and a faster car.",
				KEG_COST + " event tickets",
				running(player, "race") ? "Running now." : "Click to order."));
		page.setItem(24, Book.entry(Items.GOLDEN_APPLE, "The fighting dish",
				ChatFormatting.AQUA, "More hearts.",
				KEG_COST + " event tickets",
				running(player, "fight") ? "Running now." : "Click to order."));
		page.setItem(49, closeButton());
		open(player, page, "The Keg", Shops::buyKeg);
	}

	private static void buyKeg(ServerPlayer player, int slot) {
		String dish = switch (slot) {
			case 20 -> "swim";
			case 22 -> "race";
			case 24 -> "fight";
			default -> null;
		};
		if (dish == null) {
			if (slot == 49) {
				player.closeContainer();
			}
			return;
		}
		if (running(player, dish)) {
			player.sendSystemMessage(Component.literal("That one is already running today.")
					.withStyle(ChatFormatting.GRAY));
			return;
		}
		if (spend(player, KEG_COST)) {
			String had = State.boosts(player);
			State.boosts(player, (had.isEmpty() ? "" : had + ",")
					+ dish + ":" + (Cal.dayNumber() + 1));
			player.sendSystemMessage(Component.literal("Ordered. It lasts the day.")
					.withStyle(ChatFormatting.GREEN));
			player.closeContainer();
		}
	}

	/** Is one of The Keg's dishes still working? */
	public static boolean running(ServerPlayer player, String dish) {
		for (String part : State.boosts(player).split(",")) {
			String[] bits = part.split(":");
			if (bits.length == 2 && bits[0].equals(dish)
					&& Long.parseLong(bits[1]) > Cal.dayNumber()) {
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------- floor 14: the passport desk

	/**
	 * Two hundred and fifty tickets upgrades your passport, and floor 15
	 * comes with it.
	 *
	 * It used to put a second ship in the lift as well. That ship was this
	 * one again floor for floor with nothing new on it, so the lift does not
	 * offer it any more -- the upgrade is worth having for Ben.
	 */
	private static void upgradePassport(ServerPlayer player, ServerLevel level) {
		if (State.tally(player, State.SHIPS) > 1) {
			player.sendSystemMessage(Component.literal(
					"Your passport is already upgraded. Floor 15 is yours.")
					.withStyle(ChatFormatting.GRAY));
			State.unlock(player, 15);
			return;
		}
		if (!spend(player, PASSPORT_COST)) {
			return;
		}
		player.setAttached(State.SHIPS, 2);
		// Floor 15 comes with the upgrade rather than being bought: the first
		// thing a better passport gets you is somebody to knock on.
		State.unlock(player, 15);
		Ship.buildSecond(level);
		player.getInventory().setItem(Slots.PASSPORT_SLOT, Kit.passport());
		player.sendSystemMessage(Component.literal("Your passport is upgraded.")
				.withStyle(ChatFormatting.LIGHT_PURPLE));
		player.sendSystemMessage(Component.literal(
				"Floor 15 came with it -- somebody called Ben lives up there.")
				.withStyle(ChatFormatting.AQUA));
	}

	// ------------------------------------------------------------------ bits

	/** Take the tickets, or say why not. */
	private static boolean spend(ServerPlayer player, int tickets) {
		if (State.event(player) < tickets) {
			player.sendSystemMessage(Component.literal("That is " + tickets
					+ " event tickets and you have " + State.event(player) + ".")
					.withStyle(ChatFormatting.RED));
			return false;
		}
		State.event(player, -tickets);
		return true;
	}

	private static SimpleContainer blank() {
		SimpleContainer page = new SimpleContainer(54);
		ItemStack filler = Game.cell(Items.GRAY_STAINED_GLASS_PANE, " ");
		for (int slot = 0; slot < 54; slot++) {
			page.setItem(slot, filler.copy());
		}
		return page;
	}

	private static ItemStack closeButton() {
		return Book.entry(Items.BARRIER, "Close", ChatFormatting.RED, "Press Escape.");
	}

	private static void open(ServerPlayer player, SimpleContainer page, String title,
			ReadOnlyMenu.OnClick click) {
		player.openMenu(new SimpleMenuProvider(
				(id, inventory, who) -> new ReadOnlyMenu(id, inventory, page, click),
				Component.literal(title)));
	}
}
