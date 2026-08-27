package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Spooky Shooter in October, and Christmas in December.
 *
 * The same game twice over. You are in a crowd of a hundred, a photograph of
 * one of them is pinned to the bottom of the screen, and you have to pick that
 * one out. What makes it hard is that people look alike -- everyone is drawn
 * from a small handful of faces, and it is the hat and the shirt that tell two
 * of them apart, which means hovering rather than glancing.
 *
 * Christmas is the harder one on purpose: a hundred Santa Clauses, all the
 * same face, no two of them identical.
 *
 * Getting it right pays. Getting it wrong costs fifty and kills somebody, and
 * the dead do not come back for the rest of the day.
 */
public class Shooter extends Game {
	/** One of the crowd. */
	private record Person(Item face, String hat, String shirt, boolean carrying, String holding) {
		boolean sameAs(Person other) {
			return face == other.face && hat.equals(other.hat)
					&& shirt.equals(other.shirt) && holding.equals(other.holding);
		}
	}

	private static final String[] HATS = {
			"no hat", "a cap", "a beanie", "a hood", "a bowler", "a headband"
	};
	private static final String[] SHIRTS = {
			"a red shirt", "a blue shirt", "a green shirt", "a black shirt",
			"a striped shirt", "a checked shirt"
	};
	private static final Item[] FACES = {
			Items.SKELETON_SKULL, Items.ZOMBIE_HEAD, Items.CREEPER_HEAD,
			Items.PLAYER_HEAD, Items.WITHER_SKELETON_SKULL, Items.PIGLIN_HEAD
	};

	private final Random random = new Random();
	private final List<Person> crowd = new ArrayList<>();
	private final boolean christmas;
	private Person wanted;
	private int page;
	private int hits;
	private int misses;

	public Shooter(ServerPlayer player, boolean christmas) {
		super(player);
		this.christmas = christmas;
		for (int i = 0; i < 100; i++) {
			crowd.add(make());
		}
		pickWanted();
	}

	/**
	 * Somebody in the crowd.
	 *
	 * At Christmas everybody has the same face, so the hat and the shirt are
	 * the only things left to tell them apart. One in twelve is carrying a
	 * lightsaber or a shield, and those are worth more.
	 */
	private Person make() {
		Item face = christmas ? Items.PLAYER_HEAD : FACES[random.nextInt(FACES.length)];
		boolean carrying = random.nextInt(12) == 0;
		String holding = carrying
				? (random.nextBoolean() ? "a lightsaber" : "a shield")
				: "nothing";
		return new Person(face, HATS[random.nextInt(HATS.length)],
				SHIRTS[random.nextInt(SHIRTS.length)], carrying, holding);
	}

	private void pickWanted() {
		wanted = crowd.get(random.nextInt(crowd.size()));
	}

	@Override
	public String title() {
		return christmas ? "Christmas" : "Spooky Shooter";
	}

	@Override
	public int speed() {
		return 40;                        // nothing moves; this only redraws
	}

	@Override
	public void step() {
	}

	/** How many of the crowd fit on one page of the screen. */
	private static final int PER_PAGE = COLUMNS * ROWS;

	@Override
	public void draw() {
		blank();
		int start = page * PER_PAGE;
		for (int i = 0; i < PER_PAGE && start + i < crowd.size(); i++) {
			Person person = crowd.get(start + i);
			screen.setItem(i, Book.entry(person.face(), christmas ? "Santa" : "Someone",
					ChatFormatting.WHITE,
					"Wearing " + person.hat(),
					"In " + person.shirt(),
					person.carrying() ? "Holding " + person.holding() : ""));
		}

		int pages = (crowd.size() + PER_PAGE - 1) / PER_PAGE;
		button(0, Items.ARROW, "◀ Crowd " + (page + 1) + " of " + pages);
		button(1, Items.ARROW, "▶ Next");
		screen.setItem(45 + 3, Book.entry(wanted.face(), "WANTED", ChatFormatting.RED,
				"Wearing " + wanted.hat(),
				"In " + wanted.shirt(),
				wanted.carrying() ? "Holding " + wanted.holding() : "Holding nothing",
				"",
				"Find this one and click them."));
		button(5, Items.GOLD_NUGGET, State.event(player) + " event tickets");
		button(6, Items.TARGET, "Hit " + hits + "  ·  missed " + misses);
		button(7, Items.SKELETON_SKULL, crowd.size() + " left alive");
		button(8, Items.BARRIER, "Leave");
	}

	@Override
	public void press(int button) {
		switch (button) {
			case 0 -> page = Math.max(0, page - 1);
			case 1 -> page = Math.min((crowd.size() - 1) / PER_PAGE, page + 1);
			case 8 -> player.closeContainer();
			default -> {
			}
		}
	}

	/** A click on the crowd itself is a shot. */
	@Override
	public void pick(int slot) {
		int index = page * PER_PAGE + slot;
		if (index >= crowd.size()) {
			return;
		}
		Person shot = crowd.get(index);
		if (shot.sameAs(wanted)) {
			int paid = christmas ? 250 : 1 + random.nextInt(200);
			if (shot.carrying()) {
				paid += christmas ? 100 : 150;
			}
			hits++;
			Events.payTickets(player, paid, "you got the right one");
			player.level().playSound(null, player.blockPosition(),
					SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.8f, 1.6f);
			crowd.remove(index);
			if (crowd.isEmpty()) {
				over = true;
				player.sendSystemMessage(Component.literal("Nobody left. " + hits
						+ " found, " + misses + " missed.").withStyle(ChatFormatting.GRAY));
				return;
			}
			pickWanted();
			page = 0;
			return;
		}

		// Wrong one. That was somebody innocent, and they are gone.
		misses++;
		crowd.remove(index);
		State.event(player, -50);
		player.level().playSound(null, player.blockPosition(),
				SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.8f, 0.6f);
		player.sendSystemMessage(Component.literal(
				"That was somebody innocent. -50 event tickets, and they are gone. You have "
				+ State.event(player) + ".").withStyle(ChatFormatting.RED));
		if (crowd.isEmpty() || !crowd.contains(wanted)) {
			pickWanted();
		}
	}

}
