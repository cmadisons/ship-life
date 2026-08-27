package com.example.client.arcade;

import java.util.Random;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Pac-Man, by the arcade's rules.
 *
 * The maze, the two hundred and forty dots at ten points each, the four power
 * pellets at fifty, and the four ghosts -- who are not four copies of the same
 * ghost. Blinky comes straight at you. Pinky aims four squares in front of
 * you, so running in a straight line is how she catches you. Inky takes the
 * line from Blinky through the square ahead of you and doubles it. Clyde
 * chases until he is within eight squares and then loses his nerve and goes
 * home. That difference is the whole game.
 *
 * Eat a power pellet and they turn blue and run: the first is 200, then 400,
 * 800 and 1600, and the count resets with the next pellet. Three lives, and
 * clearing the maze starts it again with the ghosts a little braver.
 *
 * Five tickets for a new record -- the machine pays for beating yourself.
 */
public class PacManScreen extends ArcadeScreen {
	/** # wall, . dot, o power pellet, space empty, - the ghost house door. */
	private static final String[] MAZE = {
			"###################",
			"#........#........#",
			"#o##.###.#.###.##o#",
			"#.................#",
			"#.##.#.#####.#.##.#",
			"#....#...#...#....#",
			"####.###.#.###.####",
			"   #.#.......#.#   ",
			"####.# ##-## #.####",
			"    .  #   #  .    ",
			"####.# ##### #.####",
			"   #.#.......#.#   ",
			"####.#.#####.#.####",
			"#........#........#",
			"#.##.###.#.###.##.#",
			"#o.#.....P.....#.o#",
			"##.#.#.#####.#.#.##",
			"#....#...#...#....#",
			"#.######.#.######.#",
			"#.................#",
			"###################",
	};

	private static final int COLUMNS = 19;
	private static final int ROWS = 21;

	/** Where each ghost sits when it is sent home. */
	private static final int[][] HOME = { { 9, 9 }, { 8, 9 }, { 10, 9 }, { 9, 8 } };

	private static final int[] GHOST_COLOURS = {
			0xFFFF4040, 0xFFFFAEE0, 0xFF40E0E0, 0xFFFFA020,
	};

	private final Random random = new Random();
	private char[][] board;
	private int pacX;
	private int pacY;
	private int dx;
	private int dy;
	private int wantX;
	private int wantY;
	private final int[][] ghost = new int[4][2];
	private final int[][] ghostWay = new int[4][2];
	private final boolean[] home = new boolean[4];
	private int frightened;
	private int eatenThisPellet;
	private int score;
	private int lives = 3;
	private int level = 1;
	private int dots;

	public PacManScreen() {
		super("Pac-Man", COLUMNS, ROWS);
		restart();
	}

	@Override
	protected void restart() {
		board = new char[ROWS][];
		dots = 0;
		for (int y = 0; y < ROWS; y++) {
			board[y] = MAZE[y].toCharArray();
			for (int x = 0; x < COLUMNS; x++) {
				if (board[y][x] == '.' || board[y][x] == 'o') {
					dots++;
				}
			}
		}
		score = 0;
		lives = 3;
		level = 1;
		place();
	}

	/** Put everyone back where a life starts. */
	private void place() {
		pacX = 9;
		pacY = 15;
		board[15][9] = ' ';
		dx = 0;
		dy = 0;
		wantX = 0;
		wantY = 0;
		frightened = 0;
		eatenThisPellet = 0;
		for (int i = 0; i < 4; i++) {
			ghost[i][0] = HOME[i][0];
			ghost[i][1] = HOME[i][1];
			ghostWay[i][0] = 0;
			ghostWay[i][1] = -1;
			home[i] = false;
		}
	}

	private boolean wall(int x, int y) {
		if (y < 0 || y >= ROWS) {
			return true;
		}
		x = (x + COLUMNS) % COLUMNS;
		char at = board[y][x];
		return at == '#' || at == '-';
	}

	/** Pac-Man moves every 4 ticks; the ghosts a shade slower, faster later. */
	private int pace() {
		return 4;
	}

	private int ghostPace() {
		return frightened > 0 ? 8 : Math.max(4, 6 - level / 2);
	}

	@Override
	protected void step() {
		if (frightened > 0) {
			frightened--;
			if (frightened == 0) {
				eatenThisPellet = 0;
			}
		}

		if (age % pace() == 0) {
			movePac();
		}
		if (age % ghostPace() == 0) {
			for (int i = 0; i < 4; i++) {
				moveGhost(i);
			}
		}
		touch();
	}

	private void movePac() {
		// Turn as soon as the way you asked for is open -- that is the game's
		// own forgiveness, and why cornering feels the way it does.
		if ((wantX != 0 || wantY != 0) && !wall(pacX + wantX, pacY + wantY)) {
			dx = wantX;
			dy = wantY;
		}
		if (wall(pacX + dx, pacY + dy)) {
			return;
		}
		pacX = (pacX + dx + COLUMNS) % COLUMNS;
		pacY += dy;

		char at = board[pacY][pacX];
		if (at == '.') {
			board[pacY][pacX] = ' ';
			score += 10;
			dots--;
		} else if (at == 'o') {
			board[pacY][pacX] = ' ';
			score += 50;
			dots--;
			frightened = 140;
			eatenThisPellet = 0;
		}

		if (dots == 0) {
			level++;
			for (int y = 0; y < ROWS; y++) {
				board[y] = MAZE[y].toCharArray();
				for (int x = 0; x < COLUMNS; x++) {
					if (board[y][x] == '.' || board[y][x] == 'o') {
						dots++;
					}
				}
			}
			place();
		}
	}

	/**
	 * Where each ghost is trying to get to.
	 *
	 * This is the part that makes them four characters rather than four
	 * sprites, and it is worth reading in that order: Blinky, Pinky, Inky,
	 * Clyde.
	 */
	private int[] target(int which) {
		if (frightened > 0) {
			return new int[] { random.nextInt(COLUMNS), random.nextInt(ROWS) };
		}
		return switch (which) {
			case 0 -> new int[] { pacX, pacY };
			case 1 -> new int[] { pacX + dx * 4, pacY + dy * 4 };
			case 2 -> new int[] {
					pacX + dx * 2 + (pacX + dx * 2 - ghost[0][0]),
					pacY + dy * 2 + (pacY + dy * 2 - ghost[0][1]) };
			default -> {
				int away = Math.abs(pacX - ghost[3][0]) + Math.abs(pacY - ghost[3][1]);
				yield away > 8 ? new int[] { pacX, pacY } : new int[] { 1, ROWS - 2 };
			}
		};
	}

	/** One step towards the target, and never straight back the way it came. */
	private void moveGhost(int which) {
		int[] want = target(which);
		int[][] ways = { { 0, -1 }, { -1, 0 }, { 0, 1 }, { 1, 0 } };
		int bestX = ghostWay[which][0];
		int bestY = ghostWay[which][1];
		double best = Double.MAX_VALUE;

		for (int[] way : ways) {
			if (way[0] == -ghostWay[which][0] && way[1] == -ghostWay[which][1]) {
				continue;                 // ghosts do not turn round
			}
			int x = (ghost[which][0] + way[0] + COLUMNS) % COLUMNS;
			int y = ghost[which][1] + way[1];
			if (wall(x, y)) {
				continue;
			}
			double far = (x - want[0]) * (x - want[0]) + (y - want[1]) * (y - want[1]);
			if (far < best) {
				best = far;
				bestX = way[0];
				bestY = way[1];
			}
		}
		ghostWay[which][0] = bestX;
		ghostWay[which][1] = bestY;
		int x = (ghost[which][0] + bestX + COLUMNS) % COLUMNS;
		int y = ghost[which][1] + bestY;
		if (!wall(x, y)) {
			ghost[which][0] = x;
			ghost[which][1] = y;
		}
	}

	/** Caught, or eaten -- depending on who is blue. */
	private void touch() {
		for (int i = 0; i < 4; i++) {
			if (ghost[i][0] != pacX || ghost[i][1] != pacY) {
				continue;
			}
			if (frightened > 0) {
				eatenThisPellet++;
				score += 200 * (1 << Math.min(3, eatenThisPellet - 1));
				ghost[i][0] = HOME[i][0];
				ghost[i][1] = HOME[i][1];
			} else {
				lives--;
				if (lives <= 0) {
					over = true;
					tell("pacman", "score", score);
					return;
				}
				place();
				return;
			}
		}
	}

	@Override
	protected void steer(int toX, int toY) {
		wantX = toX;
		wantY = toY;
	}

	@Override
	protected void paint(GuiGraphicsExtractor graphics) {
		for (int y = 0; y < ROWS; y++) {
			for (int x = 0; x < COLUMNS; x++) {
				char at = board[y][x];
				if (at == '#') {
					square(graphics, x, y, 0xFF2038C0);
				} else if (at == '-') {
					box(graphics, originX + x * cell, originY + y * cell + cell / 2,
							cell, Math.max(1, cell / 6), 0xFFFFAEE0);
				} else if (at == '.') {
					dot(graphics, x, y, Math.max(2, cell / 4), 0xFFFFE0A0);
				} else if (at == 'o') {
					dot(graphics, x, y, Math.max(3, cell - 4), 0xFFFFE0A0);
				}
			}
		}
		for (int i = 0; i < 4; i++) {
			int colour = frightened > 0
					? (frightened < 40 && (age / 4) % 2 == 0 ? 0xFFFFFFFF : 0xFF2020C0)
					: GHOST_COLOURS[i];
			dot(graphics, ghost[i][0], ghost[i][1], cell - 1, colour);
		}
		dot(graphics, pacX, pacY, cell - 1, 0xFFFFE500);

		if (over) {
			graphics.text(font, Component.literal("GAME OVER"),
					originX + columns * cell / 2 - 24, originY + rows * cell / 2 - 4,
					0xFFFF5555);
		}
	}

	@Override
	protected Component status() {
		return Component.literal("Score " + score + "   Lives " + lives
				+ "   Level " + level + (frightened > 0 ? "   BLUE" : ""));
	}
}
