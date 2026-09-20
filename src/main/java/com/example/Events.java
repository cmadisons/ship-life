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
			new Listing("Quest Day", "Every day nothing else is on",
					"Four quests, and 500 tickets for all four."),
			new Listing("May the Fourth", "May 4",
					"The ship flies to a Star Wars planet."),
	};

	// ------------------------------------------------------------- the music

	/**
	 * A tune for each event, played on the ship's note blocks.
	 *
	 * Floor 7 used to be silent, which made the one day a month something is
	 * on feel like every other day. Each event now has an instrument and a
	 * short motif that loops while you are up there, so you know what is on
	 * before you have read the board -- and a sting when you walk into it.
	 *
	 * The notes are note-block numbers, 0 to 24, with -1 for a rest. Anything
	 * longer than a bar or two becomes wallpaper on a loop, so none of these
	 * are.
	 */
	private record Tune(net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> instrument,
			int[] notes, int[] sting) {
	}

	/** How many ticks a note lasts. Five a second is a walking pace. */
	private static final int NOTE_TICKS = 4;

	private static Tune tune(String event) {
		return switch (event) {
			// Low and falling, and it never resolves.
			case "Spooky Shooter" -> new Tune(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS,
					new int[] { 6, 5, 3, -1, 6, 5, 2, -1 },
					new int[] { 6, 3, 1 });
			// Bells, up and back down, the way a carol turns round.
			case "Christmas" -> new Tune(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL,
					new int[] { 12, 16, 19, 16, 12, -1, 14, -1 },
					new int[] { 12, 16, 19 });
			// Xylophone, bright and skipping, nothing on its mind.
			case "Summer Break" -> new Tune(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_XYLOPHONE,
					new int[] { 12, 14, 16, 19, 16, 14, -1, -1 },
					new int[] { 12, 16, 21 });
			// A chime that asks a question, for a day that hands you four.
			case "Quest Day" -> new Tune(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME,
					new int[] { 9, 12, 14, -1, 14, 12, 16, -1 },
					new int[] { 9, 14, 16 });
			// Electronic and marching, for the day the ship leaves.
			case "May the Fourth" -> new Tune(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BIT,
					new int[] { 7, 7, 7, 3, -1, 7, 3, -1 },
					new int[] { 7, 3, 0 });
			default -> null;
		};
	}

	/** Note-block number to the pitch the sound system wants. */
	private static float pitch(int note) {
		return (float) Math.pow(2.0, (note - 12) / 12.0);
	}

	/**
	 * The theme, looping on floor 7 while something is on.
	 *
	 * Only up there, and only for whoever is up there -- an event tune
	 * following you round the ship would wear out inside a day.
	 */
	private static void music() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK
				.register(server -> {
			if (server.getTickCount() % NOTE_TICKS != 0) {
				return;
			}
			for (ServerLevel level : server.getAllLevels()) {
				if (!ShipLifeMod.isShipLife(level)) {
					continue;
				}
				for (ServerPlayer player : level.players()) {
					if (STINGS.containsKey(player.getUUID())) {
						// The opening notes, and nothing over the top of them.
						playSting(level, player, server.getTickCount());
						continue;
					}
					if (Places.floorAt(player.getY()) != 7) {
						continue;
					}
					String on = running(player);
					if (on == null) {
						continue;
					}
					Tune tune = tune(on);
					if (tune == null) {
						continue;
					}
					int beat = (server.getTickCount() / NOTE_TICKS) % tune.notes().length;
					int note = tune.notes()[beat];
					if (note < 0) {
						continue;
					}
					level.playSound(null, player.blockPosition(),
							tune.instrument().value(),
							net.minecraft.sounds.SoundSource.RECORDS, 0.4f, pitch(note));
				}
			}
		});
	}

	/**
	 * The three notes an event opens on.
	 *
	 * An arpeggio rather than a chord, because three note blocks struck
	 * together on the same instrument come out as one muddy note. The server
	 * has no timer of its own to hang this on, so the notes are queued
	 * against the tick counter and played by {@link #music()} on its way
	 * round.
	 */
	private static void sting(ServerPlayer player, String event) {
		Tune tune = tune(event);
		if (tune == null) {
			return;
		}
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}
		STINGS.put(player.getUUID(), new Sting(tune, level.getServer().getTickCount()));
	}

	/** An opening arpeggio part way through being played. */
	private record Sting(Tune tune, int startedAt) {
	}

	private static final java.util.Map<java.util.UUID, Sting> STINGS =
			new java.util.HashMap<>();

	/** Play whatever note of the opening arpeggio is due, if any. */
	private static void playSting(ServerLevel level, ServerPlayer player, int tick) {
		Sting sting = STINGS.get(player.getUUID());
		if (sting == null) {
			return;
		}
		int step = (tick - sting.startedAt()) / NOTE_TICKS;
		if (step < 0 || step >= sting.tune().sting().length) {
			STINGS.remove(player.getUUID());
			return;
		}
		level.playSound(null, player.blockPosition(),
				sting.tune().instrument().value(),
				net.minecraft.sounds.SoundSource.RECORDS,
				0.8f, pitch(sting.tune().sting()[step]));
	}

	/**
	 * Fireworks off the balcony, on the days something is on.
	 *
	 * Not on Quest Day, which is most days and would make them wallpaper --
	 * only on the five that come round rarely enough to be worth marking.
	 */
	private static void fireworks() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK
				.register(server -> {
			if (server.getTickCount() % 200 != 0) {
				return;
			}
			for (ServerLevel level : server.getAllLevels()) {
				if (!ShipLifeMod.isShipLife(level)) {
					continue;
				}
				for (ServerPlayer player : level.players()) {
					String on = running(player);
					if (on == null || on.equals("Quest Day")) {
						continue;
					}
					// Only if somebody is out there to see them.
					if (player.blockPosition().distSqr(Places.BALCONY) > 400) {
						continue;
					}
					net.minecraft.world.item.ItemStack rocket =
							new net.minecraft.world.item.ItemStack(Items.FIREWORK_ROCKET);
					net.minecraft.world.entity.projectile.FireworkRocketEntity shot =
							new net.minecraft.world.entity.projectile.FireworkRocketEntity(
									level, Places.BALCONY.getX() + 3.5,
									Places.BALCONY.getY() + 1.0,
									Places.BALCONY.getZ() + 0.5, rocket);
					level.addFreshEntity(shot);
				}
			}
		});
	}

	public static void register() {
		fireworks();
		music();
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
		SimpleContainer page = Book.page(Items.GRAY_STAINED_GLASS_PANE);
		String today = running(player);
		page.setItem(4, Book.entry(Items.CLOCK, Cal.date(), ChatFormatting.AQUA,
				today == null ? "No event today." : "Today: " + today,
				Cal.eventTomorrow() == null
						? "Nothing tomorrow."
						: "Tomorrow: " + Cal.eventTomorrow(),
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

	/**
	 * What is on for this player.
	 *
	 * Normally that is whatever day it is, but a Go To Event Star puts an
	 * event of your choosing on for the rest of the day -- and it has to be
	 * the rest of the day rather than the one visit, because Summer Break is
	 * not something you visit at all.
	 */
	public static String running(ServerPlayer player) {
		String set = State.starEvent(player);
		if (!set.isEmpty()) {
			int mark = set.lastIndexOf(':');
			if (mark > 0 && parse(set.substring(mark + 1)) == Cal.dayNumber()) {
				return set.substring(0, mark);
			}
			// Yesterday's star. It has had its day.
			State.starEvent(player, "");
		}
		return Cal.eventToday();
	}

	private static long parse(String number) {
		try {
			return Long.parseLong(number);
		} catch (NumberFormatException wrong) {
			return -1;
		}
	}

	/** Put an event on for the rest of today. What the star buys. */
	public static void put(ServerPlayer player, String name) {
		State.starEvent(player, name + ":" + Cal.dayNumber());
	}

	/**
	 * Go in to whatever is on.
	 *
	 * The board and the star both end up here, so an event started with a
	 * star is the same event in every way that matters -- it pays the same and
	 * it opens floor 10 the same.
	 */
	public static void start(ServerPlayer player, String name) {
		// Turning up is enough at every event but Quest Day. That one is on
		// most days, so walking in and walking out again would hand you floor
		// 10 for nothing -- there you have to finish one of the four.
		if (!name.equals("Quest Day")) {
			didAnEvent(player, "you went to an event");
		}
		sting(player, name);
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
		if (!name.equals(running(player))) {
			player.sendSystemMessage(Component.literal(name + " is not on today. "
					+ ALL[index].when() + ".").withStyle(ChatFormatting.GRAY));
			return;
		}
		start(player, name);
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
