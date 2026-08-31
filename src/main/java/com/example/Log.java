package com.example;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * The ship's log, and the list of things worth having done.
 *
 * The log is what you did, written down a line a day and kept for the last
 * ten days. The achievements are what you have done at all, each with a name
 * -- both live in the Quest Book, on the second page.
 *
 * Neither is a system with rules. They are the game noticing.
 */
public final class Log {
	private Log() {
	}

	/** How many days the log keeps. */
	private static final int DAYS = 10;

	/** Write a line under today's date. */
	public static void write(ServerPlayer player, String what) {
		String day = String.valueOf(Cal.dayNumber());
		java.util.List<String> lines = new java.util.ArrayList<>(
				java.util.List.of(State.get(player, State.LOG).split("\n")));
		lines.removeIf(String::isEmpty);

		// One line a day, added to rather than replaced, so a day reads as a
		// day: "cleared 2 waves, beat Arachnes, swam 3 laps".
		String today = null;
		for (String line : lines) {
			if (line.startsWith(day + "|")) {
				today = line;
			}
		}
		if (today == null) {
			lines.add(day + "|" + what);
		} else {
			lines.remove(today);
			String had = today.substring(day.length() + 1);
			if (!had.contains(what)) {
				lines.add(day + "|" + had + ", " + what);
			} else {
				lines.add(today);
			}
		}
		while (lines.size() > DAYS) {
			lines.remove(0);
		}
		State.set(player, State.LOG, String.join("\n", lines));
	}

	/** The log in words, newest first. */
	public static java.util.List<String> lines(ServerPlayer player) {
		java.util.List<String> out = new java.util.ArrayList<>();
		String[] lines = State.get(player, State.LOG).split("\n");
		for (int i = lines.length - 1; i >= 0; i--) {
			if (lines[i].isEmpty()) {
				continue;
			}
			int bar = lines[i].indexOf('|');
			if (bar < 0) {
				continue;
			}
			long day = Long.parseLong(lines[i].substring(0, bar));
			long ago = Cal.dayNumber() - day;
			out.add((ago == 0 ? "Today" : ago == 1 ? "Yesterday" : ago + " days ago")
					+ ":  " + lines[i].substring(bar + 1));
		}
		if (out.isEmpty()) {
			out.add("Nothing written down yet.");
		}
		return out;
	}

	/**
	 * What the ship calls you.
	 *
	 * Worked out from how much of it you have done rather than kept anywhere,
	 * so it can never disagree with the rest of the game. It sits on the HUD
	 * beside the clock.
	 */
	public static String rank(ServerPlayer player) {
		int done = count(player);
		if (done >= FEATS.length) {
			return "Captain";
		}
		if (done >= 12) {
			return "First Mate";
		}
		if (done >= 9) {
			return "Bosun";
		}
		if (done >= 6) {
			return "Able Seaman";
		}
		if (done >= 3) {
			return "Deckhand";
		}
		return "Passenger";
	}

	// ------------------------------------------------------- what you have done

	/** One thing worth having done. */
	public record Feat(String name, String what, java.util.function.Predicate<ServerPlayer> done) {
	}

	public static final Feat[] FEATS = {
			new Feat("Paid Up", "Make your hundred dollars",
					player -> State.money(player) >= 10_000),
			new Feat("Resident", "Get your passport",
					player -> State.hasFloor(player, 5)),
			new Feat("Regular", "Own one of every pet", Pets::everyPet),
			new Feat("High Score", "Beat 100 at Pac-Man",
					player -> State.best(player) >= 100),
			new Feat("Swimmer", "Swim a lap in fifteen seconds",
					player -> State.bestLap(player) > 0 && State.bestLap(player) <= 300),
			new Feat("Driver", "Win a race outright",
					player -> State.tally(player, State.RACES) > 0),
			new Feat("Doorman", "Do an event",
					player -> State.hasFloor(player, 10)),
			new Feat("Ten Waves", "Clear ten waves on floor 9",
					player -> State.tally(player, State.WAVES) >= 10),
			new Feat("Boss", "Put a boss down",
					player -> State.tally(player, State.BOSSES) >= 1),
			new Feat("All Five", "Put all five bosses down",
					player -> State.tally(player, State.BOSSES) >= 5),
			new Feat("Dressed", "Wear the whole plant set",
					player -> Gear.wearing(player) >= 4),
			new Feat("Armed", "Carry a sword and a bow",
					player -> State.hasFloor(player, 18)),
			new Feat("Strong", "Get every heart the gym gives",
					player -> Gym.hearts(player) >= Gym.MOST_HEARTS),
			new Feat("Traveller", "Go through the portal on 18",
					player -> State.hasFloor(player, 18)
							&& State.tally(player, State.EVENT_SPENT) > 0),
			new Feat("Aboard", "Own every floor on the ship", player -> {
				for (int floor = 1; floor <= Places.TOP_FLOOR; floor++) {
					if (!State.hasFloor(player, floor)) {
						return false;
					}
				}
				return true;
			}),
	};

	/** The list, ticked. */
	public static java.util.List<String> feats(ServerPlayer player) {
		java.util.List<String> out = new java.util.ArrayList<>();
		for (Feat feat : FEATS) {
			out.add((feat.done().test(player) ? "✓ " : "☐ ") + feat.name()
					+ "  --  " + feat.what());
		}
		return out;
	}

	/** How many of them are done. */
	public static int count(ServerPlayer player) {
		int done = 0;
		for (Feat feat : FEATS) {
			if (feat.done().test(player)) {
				done++;
			}
		}
		return done;
	}

	/** Tell them when something new is ticked, once. */
	public static void check(ServerPlayer player) {
		int now = count(player);
		if (now > State.tally(player, State.FEATS)) {
			player.setAttached(State.FEATS, now);
			player.sendSystemMessage(Component.literal("That is " + now + " of "
					+ FEATS.length + " things done. The book has the list.")
					.withStyle(ChatFormatting.LIGHT_PURPLE));
		}
	}
}
