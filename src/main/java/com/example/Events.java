package com.example;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Floor 7: the events.
 *
 * There is one on most Sundays and it lasts the day, and you cannot do any of
 * them without this floor -- which is why the cat, the race track and floor 7
 * are the spine of the whole game. The board here says what is on today and
 * what is coming, and lets you into whatever is running.
 *
 * The calendar itself lives in {@link Cal}: a day is twenty real minutes, a
 * month is thirty of those, and the dates the events fall on are read off that
 * rather than off your computer's clock.
 */
public final class Events {
	private Events() {
	}

	/** Every event, in the order the board lists them. */
	public record Listing(String name, String when, String what) {
	}

	public static final Listing[] ALL = {
			new Listing("Spooky Shooter", "Every Sunday in October",
					"A crowd of 100. Find the one in the photo."),
			new Listing("Christmas", "Every Sunday in December",
					"100 Santas, no two the same. 250 a hit."),
			new Listing("Summer Break", "Weekends in summer and March break",
					"Double arcade tickets, all day."),
			new Listing("Quest Day", "Every other Monday",
					"Four quests, and 500 tickets for all four."),
			new Listing("May the Fourth", "May 4",
					"The ship flies to a Star Wars planet."),
	};

	public static void register() {
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (player instanceof ServerPlayer who && world instanceof ServerLevel level
					&& ShipLifeMod.isShipLife(level)
					&& (Places.local(hit.getBlockPos()).equals(Places.EVENT_BOARD)
							|| Places.local(hit.getBlockPos()).equals(Places.EVENT_BOARD.above()))) {
				board(who);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});
	}

	/**
	 * Pay event tickets, and say what for.
	 *
	 * Earning at an event is also how floor 10 opens: the quest was always
	 * "do an event", and having been paid by one is the proof of it.
	 */
	public static void payTickets(ServerPlayer player, int tickets, String why) {
		if (tickets > 0) {
			tickets = Shops.multiplied(player, tickets);
			State.add(player, State.EVENT_EARNED, tickets);
			didAnEvent(player, "you earned event tickets");
		}
		State.event(player, tickets);
		player.sendSystemMessage(Component.literal("+" + tickets + " event tickets  --  "
				+ why + ". You have " + State.event(player) + ".")
				.withStyle(ChatFormatting.GREEN));
	}

	/**
	 * You have done an event, whatever doing one turned out to mean.
	 *
	 * Three things count, because all three are you having been at one:
	 * earning event tickets, walking into whatever is running on floor 7, and
	 * taking the doubled arcade tickets that Summer Break and March break
	 * hand out. That last one is an event you can attend without noticing, so
	 * it counts too.
	 */
	public static void didAnEvent(ServerPlayer player, String how) {
		if (State.hasFloor(player, 10)) {
			return;
		}
		State.unlock(player, 10);
		player.sendSystemMessage(Component.literal(how
				+ ". Floor 10 -- the boss room -- is open.")
				.withStyle(ChatFormatting.AQUA));
	}

	/** The board on the wall of floor 7. */
	private static void board(ServerPlayer player) {
		SimpleContainer page = new SimpleContainer(54);
		ItemStack filler = Game.cell(Items.GRAY_STAINED_GLASS_PANE, " ");
		for (int slot = 0; slot < 54; slot++) {
			page.setItem(slot, filler.copy());
		}

		String today = Cal.eventToday();
		page.setItem(4, Book.entry(Items.CLOCK, Cal.date(), ChatFormatting.AQUA,
				today == null ? "No event today." : "Today: " + today,
				"A day is 20 real minutes.",
				"You have " + State.event(player) + " event tickets."));

		for (int i = 0; i < ALL.length; i++) {
			Listing listing = ALL[i];
			boolean on = listing.name().equals(today);
			boolean playable = on && playable(listing.name());
			page.setItem(20 + i, Book.entry(icon(listing.name()), listing.name(),
					on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY,
					listing.when(),
					listing.what(),
					"",
					on ? (playable ? "On now. Click to go in."
							: "On now -- nothing to click, it just happens.")
						: "Not today."));
		}

		page.setItem(49, Book.entry(Items.BARRIER, "Close", ChatFormatting.RED,
				"Press Escape."));

		player.openMenu(new SimpleMenuProvider(
				(id, inventory, who) -> new ReadOnlyMenu(id, inventory, page, Events::click),
				Component.literal("Events -- " + Cal.weekday())));
	}

	/** Is there something to walk into, or does the event just happen to you? */
	private static boolean playable(String name) {
		return !name.equals("Summer Break");
	}

	private static void click(ServerPlayer player, int slot) {
		if (slot == 49) {
			player.closeContainer();
			return;
		}
		int index = slot - 20;
		if (index < 0 || index >= ALL.length) {
			return;
		}
		String name = ALL[index].name();
		if (!name.equals(Cal.eventToday())) {
			player.sendSystemMessage(Component.literal(name + " is not on today. "
					+ ALL[index].when() + ".").withStyle(ChatFormatting.GRAY));
			return;
		}
		didAnEvent(player, "you went to an event");
		switch (name) {
			case "Spooky Shooter" -> new Shooter(player, false).open();
			case "Christmas" -> new Shooter(player, true).open();
			case "Quest Day" -> QuestDay.open(player);
			case "May the Fourth" -> new Duel(player).open();
			case "Summer Break" -> player.sendSystemMessage(Component.literal(
					"Summer Break is on: the arcade pays double all day.")
					.withStyle(ChatFormatting.GREEN));
			default -> player.sendSystemMessage(Component.literal(
					name + " is on, but it is not built yet.")
					.withStyle(ChatFormatting.GRAY));
		}
	}

	private static net.minecraft.world.item.Item icon(String name) {
		return switch (name) {
			case "Spooky Shooter" -> Items.CARVED_PUMPKIN;
			case "Christmas" -> Items.RED_WOOL;
			case "Summer Break" -> Items.SUNFLOWER;
			case "Quest Day" -> Items.WRITTEN_BOOK;
			default -> Items.NETHER_STAR;
		};
	}
}
