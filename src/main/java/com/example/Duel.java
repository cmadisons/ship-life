package com.example;

import java.util.Random;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;

/**
 * May the Fourth: a lightsaber fight on the Star Wars planet.
 *
 * Turning up is worth twenty-five event tickets and winning is worth a
 * hundred, so a fight you lose still pays for having had it. Strike, block or
 * dodge: a strike beats a dodge, a block beats a strike, and a dodge beats a
 * block, and whoever picks better takes a hit off the other.
 */
public class Duel extends Game {
	private static final String[] MOVES = { "Strike", "Block", "Dodge" };

	private final Random random = new Random();
	private int yourHealth = 5;
	private int theirHealth = 5;
	private String last = "Pick a move.";
	private boolean paid;

	public Duel(ServerPlayer player) {
		super(player);
	}

	@Override
	public String title() {
		return "Lightsaber Fight";
	}

	@Override
	public int speed() {
		return 20;
	}

	@Override
	public void step() {
		// Turning up pays, whatever happens next.
		if (!paid) {
			paid = true;
			Events.payTickets(player, 25, "for taking the fight");
		}
	}

	@Override
	public void draw() {
		blank();
		for (int i = 0; i < 5; i++) {
			put(i, 1, cell(i < yourHealth ? Items.LIME_CONCRETE : Items.GRAY_CONCRETE, "You"));
			put(8 - i, 3, cell(i < theirHealth ? Items.RED_CONCRETE : Items.GRAY_CONCRETE,
					"Them"));
		}
		put(4, 2, cell(Items.NETHER_STAR, last));

		button(0, Items.IRON_SWORD, "Strike");
		button(1, Items.SHIELD, "Block");
		button(2, Items.FEATHER, "Dodge");
		button(4, Items.GOLD_NUGGET, State.event(player) + " event tickets");
		button(6, Items.PAPER, last);
		button(7, over ? Items.LIME_DYE : Items.GRAY_DYE, over ? "Fight again" : "Fighting");
		button(8, Items.BARRIER, "Leave");
	}

	@Override
	public void press(int button) {
		if (button == 8) {
			player.closeContainer();
			return;
		}
		if (over) {
			if (button == 7) {
				new Duel(player).open();
			}
			return;
		}
		if (button > 2) {
			return;
		}

		int yours = button;
		int theirs = random.nextInt(3);
		// Strike beats dodge, block beats strike, dodge beats block.
		int result = (yours - theirs + 3) % 3;
		if (result == 1) {
			theirHealth--;
			last = MOVES[yours] + " beat " + MOVES[theirs];
			player.level().playSound(null, player.blockPosition(),
					SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.6f, 1.8f);
		} else if (result == 2) {
			yourHealth--;
			last = MOVES[theirs] + " beat your " + MOVES[yours];
			player.level().playSound(null, player.blockPosition(),
					SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.6f, 0.8f);
		} else {
			last = "Both " + MOVES[yours] + " -- nothing in it";
		}

		if (theirHealth <= 0) {
			over = true;
			Events.payTickets(player, 100, "you won the fight");
		} else if (yourHealth <= 0) {
			over = true;
			player.sendSystemMessage(Component.literal(
					"You lost that one. The 25 for turning up is yours.")
					.withStyle(ChatFormatting.GRAY));
		}
	}
}
