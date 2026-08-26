package com.example;

import java.util.Random;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;

/**
 * Pac-Man. Five tickets for every new record.
 *
 * A small maze, a screen full of pellets and two ghosts that come after you.
 * Unlike the other two this one pays for beating yourself rather than for the
 * playing: your best score is kept, and passing it is what earns the tickets,
 * so the second run is worth as much as the first only if it is better.
 */
public class PacMan extends Game {
	/** The maze. # is a wall, everything else starts as a pellet. */
	private static final String[] MAZE = {
			"....#....",
			".##.#.##.",
			".........",
			".##.#.##.",
			"....#....",
	};

	private final boolean[][] pellet = new boolean[ROWS][COLUMNS];
	private final Random random = new Random();
	private int x = 4;
	private int y = 2;
	private int dx = 1;
	private int dy = 0;
	private final int[][] ghosts = { { 0, 0 }, { COLUMNS - 1, ROWS - 1 } };
	private int score;

	public PacMan(ServerPlayer player) {
		super(player);
		for (int row = 0; row < ROWS; row++) {
			for (int column = 0; column < COLUMNS; column++) {
				pellet[row][column] = !wall(column, row);
			}
		}
		pellet[y][x] = false;
	}

	@Override
	public String title() {
		return "Pac-Man  ·  best " + State.best(player);
	}

	@Override
	public int speed() {
		return 8;
	}

	private static boolean wall(int column, int row) {
		return MAZE[row].charAt(column) == '#';
	}

	@Override
	public void step() {
		// Keep going the way you were pointed, if you can.
		if (!wall(wrap(x + dx, COLUMNS), wrap(y + dy, ROWS))) {
			x = wrap(x + dx, COLUMNS);
			y = wrap(y + dy, ROWS);
		}
		if (pellet[y][x]) {
			pellet[y][x] = false;
			score++;
			player.level().playSound(null, player.blockPosition(),
					SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.3f, 2.0f);
		}

		// The ghosts step towards you, badly.
		for (int[] ghost : ghosts) {
			int toX = Integer.compare(x, ghost[0]);
			int toY = Integer.compare(y, ghost[1]);
			if (random.nextInt(4) == 0) {
				toX = random.nextInt(3) - 1;
				toY = 0;
			}
			if (toX != 0 && !wall(wrap(ghost[0] + toX, COLUMNS), ghost[1])) {
				ghost[0] = wrap(ghost[0] + toX, COLUMNS);
			} else if (toY != 0 && !wall(ghost[0], wrap(ghost[1] + toY, ROWS))) {
				ghost[1] = wrap(ghost[1] + toY, ROWS);
			}
			if (ghost[0] == x && ghost[1] == y) {
				finish("A ghost got you");
				return;
			}
		}

		if (cleared()) {
			finish("Screen cleared");
		}
	}

	private boolean cleared() {
		for (int row = 0; row < ROWS; row++) {
			for (int column = 0; column < COLUMNS; column++) {
				if (pellet[row][column]) {
					return false;
				}
			}
		}
		return true;
	}

	private void finish(String why) {
		over = true;
		if (score > State.best(player)) {
			State.best(player, score);
			win(5, "a new record of " + score);
		} else {
			player.sendSystemMessage(Component.literal(why + " -- " + score
					+ ", and your best is " + State.best(player) + ".")
					.withStyle(ChatFormatting.GRAY));
		}
	}

	private static int wrap(int value, int size) {
		return (value + size) % size;
	}

	@Override
	public void draw() {
		blank();
		for (int row = 0; row < ROWS; row++) {
			for (int column = 0; column < COLUMNS; column++) {
				if (wall(column, row)) {
					put(column, row, cell(Items.BLUE_CONCRETE, "Wall"));
				} else if (pellet[row][column]) {
					put(column, row, cell(Items.WHITE_STAINED_GLASS_PANE, "·"));
				}
			}
		}
		for (int[] ghost : ghosts) {
			put(ghost[0], ghost[1], cell(Items.RED_CONCRETE, "Ghost"));
		}
		put(x, y, cell(Items.YELLOW_CONCRETE, "You"));

		button(0, Items.ARROW, "◀ Left");
		button(1, Items.SPECTRAL_ARROW, "▲ Up");
		button(2, Items.SPECTRAL_ARROW, "▼ Down");
		button(3, Items.ARROW, "▶ Right");
		button(4, Items.SNOWBALL, "Score " + score + "  ·  best " + State.best(player));
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
				new PacMan(player).open();
			}
			return;
		}
		switch (button) {
			case 0 -> point(-1, 0);
			case 1 -> point(0, -1);
			case 2 -> point(0, 1);
			case 3 -> point(1, 0);
			default -> {
			}
		}
	}

	private void point(int toX, int toY) {
		dx = toX;
		dy = toY;
	}
}
