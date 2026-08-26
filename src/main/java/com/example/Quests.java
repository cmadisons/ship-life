package com.example;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * The quests, and how you move through them.
 *
 * A quest is a name, a line about what it gives you, and a list of parts. A
 * part is one line of "what you need to do right now", where it is, and how
 * many times you have to do it. The book shows every part of a quest and what
 * it pays up front -- that was the point of listing them -- but you still do
 * them in order, and the star only ever points at the part you are on.
 *
 * Progress is only ever pushed forward from here, by {@link #did}, so there is
 * one place that decides when a part is finished and one place that pays.
 */
public final class Quests {
	private Quests() {
	}

	/** One thing to do, where to do it, and how many times. */
	public record Part(String todo, BlockPos where, int need) {
		public Part(String todo, BlockPos where) {
			this(todo, where, 1);
		}
	}

	/** A quest: what it is called, what it pays, and the parts of it. */
	public record Quest(String chapter, String name, String reward, List<Part> parts) {
	}

	public static final Quest[] ALL = {
			new Quest("Chapter 1", "Make the Money",
					"$100.00 in all -- a month on the ship",
					List.of(
							new Part("Wash the 10 dishes -- sponge, then towel  ($5.00)",
									Places.dish(0), 10),
							new Part("Mow the lawn and take out the 10 weeds  ($94.99)",
									Places.HOUSE_TWO, 20),
							new Part("Find a penny in one of the 5 bushes  ($0.01)",
									Places.bush(0), 1))),

			new Quest("Chapter 2", "Go in the Ship",
					"Your passport -- floors 1 and 5",
					List.of(
							new Part("Go in the ship", Places.DOOR),
							new Part("Talk to security at the desk", Places.DESK))),

			new Quest("Chapter 2", "Your Floor",
					"Somewhere to live",
					List.of(
							new Part("Go to the elevator", Places.panel(1)),
							new Part("Press the floor 5 button", Places.panel(1)),
							new Part("Check out your floor -- the bathroom, the fridge, "
									+ "your bed and your TV", Places.BED, 4))),

			new Quest("Chapter 3", "The Call",
					"A word with the manager",
					List.of(
							new Part("Go to floor 1", Places.TABLE),
							new Part("Talk to the person at the table", Places.TABLE))),

			new Quest("Chapter 4", "Charlie's Quest",
					"Floors 2, 3 and 4, a phone and a bed",
					List.of(
							new Part("Your toilet is plugged -- unplug it", Places.TOILET),
							new Part("Your bed is not made -- make it", Places.BED),
							new Part("Your fridge has moldy food -- throw it out",
									Places.FRIDGE)))
	};

	/** The quest you are on, or null once they are all done. */
	public static Quest current(ServerPlayer player) {
		int index = State.quest(player);
		return index < ALL.length ? ALL[index] : null;
	}

	/** The part you are on, or null once they are all done. */
	public static Part currentPart(ServerPlayer player) {
		Quest quest = current(player);
		if (quest == null) {
			return null;
		}
		int part = State.part(player);
		return part < quest.parts().size() ? quest.parts().get(part) : null;
	}

	/** Are we on this exact quest and part? Used by everything that counts. */
	public static boolean on(ServerPlayer player, int quest, int part) {
		return State.quest(player) == quest && State.part(player) == part;
	}

	/**
	 * You did the thing once more.
	 *
	 * When the counter reaches what the part needs, the part is done: it pays
	 * out, and either the next part or the next quest starts.
	 */
	public static void did(ServerPlayer player) {
		Part part = currentPart(player);
		if (part == null) {
			return;
		}
		int count = State.count(player) + 1;
		if (count < part.need()) {
			State.count(player, count);
			player.sendOverlayMessage(Component.literal(
					count + " / " + part.need() + "  " + part.todo())
					.withStyle(ChatFormatting.GRAY));
			return;
		}
		finishPart(player);
	}

	/** Finish the part you are on outright, however far in you were. */
	public static void finishPart(ServerPlayer player) {
		Quest quest = current(player);
		if (quest == null) {
			return;
		}
		int index = State.part(player);
		payFor(player, State.quest(player), index);
		State.count(player, 0);

		player.level().playSound(null, player.blockPosition(),
				SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.7f, 1.4f);

		if (index + 1 < quest.parts().size()) {
			player.setAttached(State.PART, index + 1);
			Part next = quest.parts().get(index + 1);
			player.sendSystemMessage(Component.literal("Next: ")
					.withStyle(ChatFormatting.GRAY)
					.append(Component.literal(next.todo()).withStyle(ChatFormatting.YELLOW)));
			return;
		}

		// The whole quest is done.
		player.setAttached(State.PART, 0);
		player.setAttached(State.QUEST, State.quest(player) + 1);
		player.sendSystemMessage(Component.literal("Quest done: ")
				.withStyle(ChatFormatting.GREEN)
				.append(Component.literal(quest.name()).withStyle(ChatFormatting.WHITE)));
		reward(player, State.quest(player) - 1);

		Quest next = current(player);
		if (next != null) {
			player.sendSystemMessage(Component.literal(next.chapter() + " -- " + next.name())
					.withStyle(ChatFormatting.AQUA));
		}
	}

	/** What a single part pays the moment you finish it. */
	private static void payFor(ServerPlayer player, int quest, int part) {
		if (quest != 0) {
			return;                       // only the first quest pays per part
		}
		int cents = switch (part) {
			case 0 -> 500;
			case 1 -> 9499;
			default -> 1;
		};
		State.pay(player, cents);
		player.sendSystemMessage(Component.literal("Paid " + State.dollars(cents)
				+ "  --  you have " + State.dollars(State.money(player)))
				.withStyle(ChatFormatting.GREEN));
	}

	/** What finishing a whole quest gives you. */
	private static void reward(ServerPlayer player, int quest) {
		switch (quest) {
			case 0 -> player.sendSystemMessage(Component.literal(
					"$100. That is a month on the ship. Head east down the walkway.")
					.withStyle(ChatFormatting.YELLOW));
			case 1 -> {
				State.unlock(player, 1);
				State.unlock(player, 5);
				player.getInventory().setItem(Slots.PASSPORT_SLOT, Kit.passport());
				player.sendSystemMessage(Component.literal(
						"Your passport opens floor 1 and floor 5.")
						.withStyle(ChatFormatting.AQUA));
			}
			case 4 -> {
				State.unlock(player, 2);
				State.unlock(player, 3);
				State.unlock(player, 4);
				player.sendSystemMessage(Component.literal(
						"Charlie unlocks floors 2, 3 and 4. Your phone is by your bed.")
						.withStyle(ChatFormatting.AQUA));
			}
			default -> {
			}
		}
	}
}
