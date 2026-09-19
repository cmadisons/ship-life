package com.example.client.arcade;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import org.lwjgl.glfw.GLFW;

/**
 * Breakout, the fourth cabinet.
 *
 * A wall of bricks, a paddle, and one ball you are not allowed to drop. Clear
 * the wall and another one comes down a row lower with a quicker ball; drop
 * the ball three times and that is the run.
 *
 * The ball is the only thing here that does not live on the grid. Snake and
 * Pac-Man both move a whole cell at a time, which is what those games are, but
 * a ball that moved a cell a tick could only ever travel at forty-five
 * degrees -- so this one keeps its position in cells as a fraction and is
 * drawn at whatever pixel that lands on. Everything it hits is still grid.
 *
 * A ticket a brick, and twenty-five for clearing the wall, paid as you go like
 * the other three -- a run that ends on a dropped ball still leaves you with
 * what you knocked down.
 */
public class BreakoutScreen extends ArcadeScreen {
	private static final int COLUMNS = 24;
	private static final int ROWS = 18;

	/** Bricks are two cells wide, so the wall divides evenly. */
	private static final int BRICK_WIDE = 2;
	/** The top row of bricks, leaving a gap above for the ball to loop into. */
	private static final int FIRST_ROW = 2;
	/** How many rows of bricks the first wall has. */
	private static final int START_ROWS = 4;

	private static final int PADDLE_ROW = ROWS - 1;
	private static final double PADDLE_WIDE = 4.0;
	private static final double PADDLE_STEP = 0.9;

	/** How long a key press keeps the paddle moving, in ticks. */
	private static final int DRIFT_TICKS = 3;

	/** Brick colours by row, top to bottom, the way the original went. */
	private static final int[] ROW_COLOUR = {
			0xFFD94C4C, 0xFFD9884C, 0xFFD9CC4C, 0xFF6BCC5A, 0xFF4C9BD9, 0xFF9B6BD9,
	};

	/** The wall. null where a brick has been knocked out. */
	private boolean[][] bricks = new boolean[ROWS][COLUMNS];

	private double ballX;
	private double ballY;
	private double ballVX;
	private double ballVY;

	private double paddleX;
	private int drift;
	private int driftUntil;

	/** True before the first push, so the ball sits on the paddle. */
	private boolean waiting;

	private int lives;
	private int level;
	private int broken;
	private int cleared;

	public BreakoutScreen() {
		super("Breakout", COLUMNS, ROWS);
		restart();
	}

	@Override
	protected void restart() {
		lives = 3;
		level = 1;
		broken = 0;
		cleared = 0;
		buildWall();
		serve();
	}

	/**
	 * Lay out a wall.
	 *
	 * Each level adds a row and starts one row lower, so the wall creeps down
	 * towards the paddle and the game ends on its own even if you never miss.
	 */
	private void buildWall() {
		bricks = new boolean[ROWS][COLUMNS];
		int rows = Math.min(START_ROWS + level - 1, ROW_COLOUR.length);
		int top = Math.min(FIRST_ROW + (level - 1), PADDLE_ROW - rows - 3);
		for (int row = top; row < top + rows; row++) {
			for (int column = 0; column < COLUMNS; column++) {
				bricks[row][column] = true;
			}
		}
	}

	/** Put the ball back on the paddle and wait for a push. */
	private void serve() {
		paddleX = COLUMNS / 2.0 - PADDLE_WIDE / 2.0;
		ballX = COLUMNS / 2.0;
		ballY = PADDLE_ROW - 0.5;
		// Up and slightly to the right, at whatever this level's pace is.
		double speed = 0.30 + 0.05 * (level - 1);
		ballVX = speed * 0.6;
		ballVY = -speed;
		waiting = true;
	}

	@Override
	protected void step() {
		movePaddle();

		if (waiting) {
			// The ball rides the paddle until you push it off.
			ballX = paddleX + PADDLE_WIDE / 2.0;
			ballY = PADDLE_ROW - 0.5;
			return;
		}

		// Step the ball one axis at a time. Doing both at once and then asking
		// what it hit cannot tell a wall it came at sideways from one it came
		// at head on, and the bounce comes out wrong on brick corners.
		ballX += ballVX;
		if (ballX < 0.5) {
			ballX = 0.5;
			ballVX = -ballVX;
		} else if (ballX > COLUMNS - 0.5) {
			ballX = COLUMNS - 0.5;
			ballVX = -ballVX;
		}
		if (hitBrick()) {
			ballVX = -ballVX;
		}

		ballY += ballVY;
		if (ballY < 0.5) {
			ballY = 0.5;
			ballVY = -ballVY;
		}
		if (hitBrick()) {
			ballVY = -ballVY;
		}

		bouncePaddle();

		if (ballY > ROWS) {
			dropped();
		}
	}

	private void movePaddle() {
		if (age >= driftUntil) {
			drift = 0;
		}
		if (drift != 0) {
			paddleX += drift * PADDLE_STEP;
			paddleX = Math.max(0, Math.min(COLUMNS - PADDLE_WIDE, paddleX));
		}
	}

	/**
	 * Knock out whatever the ball is sitting in, if anything.
	 *
	 * Returns whether it hit, so the caller can turn the ball round on the
	 * axis it just moved along.
	 */
	private boolean hitBrick() {
		int column = (int) Math.floor(ballX);
		int row = (int) Math.floor(ballY);
		if (row < 0 || row >= ROWS || column < 0 || column >= COLUMNS) {
			return false;
		}
		if (!bricks[row][column]) {
			return false;
		}
		// Bricks are two cells wide and break as one.
		int from = column - (column % BRICK_WIDE);
		for (int part = from; part < Math.min(from + BRICK_WIDE, COLUMNS); part++) {
			bricks[row][part] = false;
		}
		broken++;
		tell("breakout", "bricks", 1);

		if (wallEmpty()) {
			cleared++;
			tell("breakout", "level", level);
			level++;
			buildWall();
			serve();
		}
		return true;
	}

	private boolean wallEmpty() {
		for (int row = 0; row < ROWS; row++) {
			for (int column = 0; column < COLUMNS; column++) {
				if (bricks[row][column]) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * The paddle.
	 *
	 * Where along it the ball lands decides which way the ball goes, which is
	 * the whole skill of the game -- a paddle that only reversed the ball
	 * would leave you watching it rather than aiming it.
	 */
	private void bouncePaddle() {
		if (ballVY <= 0 || ballY < PADDLE_ROW || ballY > PADDLE_ROW + 1) {
			return;
		}
		if (ballX < paddleX || ballX > paddleX + PADDLE_WIDE) {
			return;
		}
		double where = (ballX - paddleX) / PADDLE_WIDE;   // 0 left, 1 right
		double angle = (where - 0.5) * 2.0;               // -1 .. 1
		double speed = Math.hypot(ballVX, ballVY);
		ballVX = speed * angle * 0.85;
		ballVY = -Math.sqrt(Math.max(0.01, speed * speed - ballVX * ballVX));
		ballY = PADDLE_ROW - 0.01;
	}

	private void dropped() {
		lives--;
		if (lives <= 0) {
			over = true;
			tell("breakout", "dead", broken);
			return;
		}
		serve();
	}

	@Override
	protected void steer(int toX, int toY) {
		if (toX == 0) {
			return;                        // up and down do nothing here
		}
		drift = toX;
		driftUntil = age + DRIFT_TICKS;
	}

	@Override
	protected void pressed(int key) {
		// Space serves. The base screen keeps space for restarting once you
		// are out, so this only ever sees it while the run is still going.
		if (key == GLFW.GLFW_KEY_SPACE && waiting) {
			waiting = false;
		}
	}

	@Override
	protected void paint(GuiGraphicsExtractor graphics) {
		for (int row = 0; row < ROWS; row++) {
			for (int column = 0; column < COLUMNS; column += BRICK_WIDE) {
				if (!bricks[row][column]) {
					continue;
				}
				int colour = ROW_COLOUR[Math.min(row, ROW_COLOUR.length - 1)];
				// One block short of the full width, so the wall reads as
				// bricks with mortar between them rather than a slab.
				box(graphics, originX + column * cell, originY + row * cell,
						BRICK_WIDE * cell - 1, cell - 1, colour);
			}
		}

		box(graphics, originX + (int) (paddleX * cell), originY + PADDLE_ROW * cell,
				(int) (PADDLE_WIDE * cell), Math.max(2, cell / 2), 0xFFE8E8E8);

		int size = Math.max(2, cell / 2);
		box(graphics, originX + (int) (ballX * cell) - size / 2,
				originY + (int) (ballY * cell) - size / 2, size, size, 0xFFFFFFFF);

		if (waiting && !over) {
			graphics.text(font, Component.literal("SPACE TO SERVE"),
					originX + columns * cell / 2 - 38, originY + rows * cell / 2 - 4,
					0xFFCBD3E0);
		}
		if (over) {
			graphics.text(font, Component.literal("GAME OVER"),
					originX + columns * cell / 2 - 24, originY + rows * cell / 2 - 4,
					0xFFFF5555);
		}
	}

	@Override
	protected Component status() {
		return Component.literal("Level " + level + "   Balls " + lives
				+ "   Bricks " + broken + "   " + (broken + cleared * 25)
				+ " tickets earned");
	}
}
