package com.example.client.arcade;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Snake, by the rules the game has always had.
 *
 * The snake never stops. It grows by one for every apple, the walls and its
 * own body are fatal, and you cannot turn back on yourself -- the neck is
 * where you would turn into. It speeds up as it grows, which is the only
 * difficulty the game has ever needed.
 *
 * A ticket an apple, paid as you eat rather than at the end, so a run that
 * ends badly still leaves you with what you earned.
 */
public class SnakeScreen extends ArcadeScreen {
	private static final int COLUMNS = 24;
	private static final int ROWS = 18;

	private final Random random = new Random();
	private final Deque<int[]> body = new ArrayDeque<>();
	private int dx;
	private int dy;
	private int wantX;
	private int wantY;
	private int[] apple;
	private int eaten;

	public SnakeScreen() {
		super("Snake", COLUMNS, ROWS);
		restart();
	}

	@Override
	protected void restart() {
		body.clear();
		body.addFirst(new int[] { COLUMNS / 2, ROWS / 2 });
		body.addLast(new int[] { COLUMNS / 2 - 1, ROWS / 2 });
		body.addLast(new int[] { COLUMNS / 2 - 2, ROWS / 2 });
		dx = 1;
		dy = 0;
		wantX = 1;
		wantY = 0;
		eaten = 0;
		dropApple();
	}

	/** How many ticks between moves. It quickens as you grow. */
	private int pace() {
		return Math.max(2, 6 - eaten / 12);
	}

	@Override
	protected void step() {
		if (age % pace() != 0) {
			return;
		}
		// The turn only takes effect on the step, so two keys in one tick
		// cannot double back through the neck.
		dx = wantX;
		dy = wantY;

		int[] head = body.peekFirst();
		int x = head[0] + dx;
		int y = head[1] + dy;

		if (x < 0 || y < 0 || x >= COLUMNS || y >= ROWS || occupied(x, y, true)) {
			over = true;
			tell("snake", "dead", eaten);
			return;
		}

		body.addFirst(new int[] { x, y });
		if (x == apple[0] && y == apple[1]) {
			eaten++;
			tell("snake", "food", 1);
			dropApple();
		} else {
			body.removeLast();
		}
	}

	/** Is this square snake? The tail is about to move, so it does not count. */
	private boolean occupied(int x, int y, boolean freeTail) {
		int index = 0;
		int last = body.size() - 1;
		for (int[] part : body) {
			if (part[0] == x && part[1] == y && !(freeTail && index == last)) {
				return true;
			}
			index++;
		}
		return false;
	}

	private void dropApple() {
		do {
			apple = new int[] { random.nextInt(COLUMNS), random.nextInt(ROWS) };
		} while (occupied(apple[0], apple[1], false));
	}

	@Override
	protected void steer(int toX, int toY) {
		if (dx + toX == 0 && dy + toY == 0) {
			return;                       // that is the neck
		}
		wantX = toX;
		wantY = toY;
	}

	@Override
	protected void paint(GuiGraphicsExtractor graphics) {
		dot(graphics, apple[0], apple[1], cell - 2, 0xFFE03A3A);
		boolean head = true;
		for (int[] part : body) {
			square(graphics, part[0], part[1], head ? 0xFF7CE86A : 0xFF3FA843);
			head = false;
		}
		if (over) {
			graphics.text(font, Component.literal("GAME OVER"),
					originX + columns * cell / 2 - 24, originY + rows * cell / 2 - 4,
					0xFFFF5555);
		}
	}

	@Override
	protected Component status() {
		return Component.literal("Apples " + eaten + "   Length " + body.size()
				+ "   " + eaten + " tickets earned");
	}
}
