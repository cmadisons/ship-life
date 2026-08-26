package com.example;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;

/**
 * Galaga. Five tickets for every round you pass.
 *
 * A wall of ships comes down the screen a row at a time and you shoot up at
 * the column you are standing under. Clear the wall and the next round starts,
 * faster and one row deeper; let it reach your row and you are done.
 *
 * Tickets are paid per round passed, so the run is worth something as soon as
 * you clear the first wall.
 */
public class Galaga extends Game {
	private final boolean[][] enemies = new boolean[ROWS][COLUMNS];
	private int ship = COLUMNS / 2;
	private int round = 1;
	private int shotColumn = -1;
	private int shotRow = -1;

	public Galaga(ServerPlayer player) {
		super(player);
		fill();
	}

	@Override
	public String title() {
		return "Galaga";
	}

	@Override
	public int speed() {
		return Math.max(6, 22 - round * 2);
	}

	/** A wall of ships, one row deeper each round, up to three. */
	private void fill() {
		for (int row = 0; row < ROWS; row++) {
			for (int column = 0; column < COLUMNS; column++) {
				enemies[row][column] = row < Math.min(3, 1 + round / 2);
			}
		}
	}

	@Override
	public void step() {
		// The shot travels up a row at a time, and takes the first ship it meets.
		if (shotRow >= 0) {
			if (enemies[shotRow][shotColumn]) {
				enemies[shotRow][shotColumn] = false;
				shotRow = -1;
				player.level().playSound(null, player.blockPosition(),
						SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.4f, 1.8f);
			} else if (--shotRow < 0) {
				shotRow = -1;
			}
		}

		if (cleared()) {
			win(5, "round " + round + " passed");
			round++;
			fill();
			return;
		}

		// Every other step, the wall comes down one row.
		if (age % (speed() * 2) == 0 && !dropAll()) {
			over = true;
			player.level().playSound(null, player.blockPosition(),
					SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.7f, 0.6f);
			player.sendSystemMessage(Component.literal("Galaga over -- you passed "
					+ (round - 1) + " round" + (round == 2 ? "" : "s") + ".")
					.withStyle(ChatFormatting.GRAY));
		}
	}

	private boolean cleared() {
		for (int row = 0; row < ROWS; row++) {
			for (int column = 0; column < COLUMNS; column++) {
				if (enemies[row][column]) {
					return false;
				}
			}
		}
		return true;
	}

	/** Shuffle the wall down. False if it has reached your row. */
	private boolean dropAll() {
		for (int column = 0; column < COLUMNS; column++) {
			if (enemies[ROWS - 1][column]) {
				return false;
			}
		}
		for (int row = ROWS - 1; row > 0; row--) {
			System.arraycopy(enemies[row - 1], 0, enemies[row], 0, COLUMNS);
		}
		java.util.Arrays.fill(enemies[0], false);
		return true;
	}

	@Override
	public void draw() {
		blank();
		for (int row = 0; row < ROWS; row++) {
			for (int column = 0; column < COLUMNS; column++) {
				if (enemies[row][column]) {
					put(column, row, cell(Items.PURPLE_CONCRETE, "Ship"));
				}
			}
		}
		if (shotRow >= 0) {
			put(shotColumn, shotRow, cell(Items.YELLOW_STAINED_GLASS_PANE, "Shot"));
		}
		put(ship, ROWS - 1, cell(Items.LIGHT_BLUE_CONCRETE, "You"));

		button(0, Items.ARROW, "◀ Left");
		button(1, Items.FIRE_CHARGE, "Fire");
		button(2, Items.ARROW, "▶ Right");
		button(4, Items.NETHER_STAR, "Round " + round);
		button(6, Items.GOLD_NUGGET, State.arcade(player) + " tickets");
		button(7, over ? Items.LIME_DYE : Items.GRAY_DYE, over ? "Play again" : "Playing");
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
				new Galaga(player).open();
			}
			return;
		}
		switch (button) {
			case 0 -> ship = Math.max(0, ship - 1);
			case 2 -> ship = Math.min(COLUMNS - 1, ship + 1);
			case 1 -> {
				if (shotRow < 0) {
					shotColumn = ship;
					shotRow = ROWS - 2;
				}
			}
			default -> {
			}
		}
	}
}
