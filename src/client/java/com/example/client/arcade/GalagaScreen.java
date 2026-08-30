package com.example.client.arcade;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import org.lwjgl.glfw.GLFW;

/**
 * Galaga, by the arcade's rules.
 *
 * A formation of forty enemies that flies in, sits there swaying, and sends
 * them at you in ones and twos -- a bee is worth 50 in formation and 100 while
 * diving, a butterfly 80 and 160, and the flagship at the top 150, or 400 if
 * you take it with its two escorts. Diving is worth more than sitting still,
 * which is the whole of the game's economy.
 *
 * You get two shots on the screen at once and no more, so firing wildly leaves
 * you unable to answer what is coming down at you. Three lives, and clearing
 * the formation starts the next stage a little quicker.
 *
 * Five tickets a stage, paid as you clear it.
 */
public class GalagaScreen extends ArcadeScreen {
	private static final int COLUMNS = 24;
	private static final int ROWS = 22;

	/** How many shots you may have in the air. The original allowed two. */
	private static final int SHOTS = 2;

	private static final int ROW_FLAGSHIP = 0;
	private static final int ROW_BUTTERFLY = 1;
	private static final int ROW_BEE = 3;

	/** One of the formation: where it belongs, where it is, and what it is. */
	private static final class Enemy {
		int homeX;
		int homeY;
		double x;
		double y;
		int kind;
		boolean diving;
		double driftX;
		double driftY;
	}

	private final Random random = new Random();
	private final List<Enemy> enemies = new ArrayList<>();
	private final List<double[]> shots = new ArrayList<>();
	private final List<double[]> bombs = new ArrayList<>();
	private int ship = COLUMNS / 2;
	private int score;
	private int lives = 3;
	private int stage = 1;
	private int sway;

	public GalagaScreen() {
		super("Galaga", COLUMNS, ROWS);
		restart();
	}

	@Override
	protected void restart() {
		score = 0;
		lives = 3;
		stage = 1;
		formation();
	}

	/** Two flagships, sixteen butterflies, twenty bees: forty in all. */
	private void formation() {
		enemies.clear();
		shots.clear();
		bombs.clear();
		ship = COLUMNS / 2;

		// Every fifth stage is a boss: one thing across the top of the screen
		// that takes a lot of hits, with a short guard either side of it.
		if (stage % 5 == 0) {
			for (int i = 0; i < 6; i++) {
				Enemy boss = new Enemy();
				boss.homeX = COLUMNS / 2 - 5 + i * 2;
				boss.homeY = 2;
				boss.x = boss.homeX;
				boss.y = -2;
				boss.kind = ROW_FLAGSHIP;
				enemies.add(boss);
			}
			for (int i = 0; i < 6; i++) {
				Enemy guard = new Enemy();
				guard.homeX = COLUMNS / 2 - 5 + i * 2;
				guard.homeY = 5;
				guard.x = guard.homeX;
				guard.y = -4;
				guard.kind = ROW_BUTTERFLY;
				enemies.add(guard);
			}
			return;
		}
		for (int row = 0; row < 5; row++) {
			int howMany = row == 0 ? 4 : 8;
			for (int i = 0; i < howMany; i++) {
				Enemy enemy = new Enemy();
				enemy.homeX = (COLUMNS - howMany * 2) / 2 + i * 2;
				enemy.homeY = 2 + row * 2;
				// They fly in from off the top, as they always did.
				enemy.x = enemy.homeX;
				enemy.y = -2 - row;
				enemy.kind = row == 0 ? ROW_FLAGSHIP : row <= 2 ? ROW_BUTTERFLY : ROW_BEE;
				enemies.add(enemy);
			}
		}
	}

	@Override
	protected void step() {
		if (age % 2 == 0) {
			sway = (int) (Math.sin(age / 20.0) * 1.5);
		}

		for (Enemy enemy : enemies) {
			if (enemy.diving) {
				enemy.x += enemy.driftX;
				enemy.y += enemy.driftY;
				// A diver that reaches the bottom comes round the top again.
				if (enemy.y > ROWS) {
					enemy.y = -1;
					enemy.diving = false;
					enemy.x = enemy.homeX;
				}
				if (random.nextInt(30) == 0) {
					bombs.add(new double[] { enemy.x, enemy.y });
				}
			} else if (enemy.y < enemy.homeY) {
				enemy.y += 0.25;          // still flying in
			} else {
				enemy.x = enemy.homeX + sway;
				enemy.y = enemy.homeY;
			}
		}

		// Somebody peels off. The later the stage, the more often.
		if (age % Math.max(20, 70 - stage * 6) == 0) {
			List<Enemy> sitting = enemies.stream()
					.filter(enemy -> !enemy.diving && enemy.y >= enemy.homeY).toList();
			if (!sitting.isEmpty()) {
				Enemy diver = sitting.get(random.nextInt(sitting.size()));
				diver.diving = true;
				diver.driftX = (ship - diver.x) / 24.0;
				diver.driftY = 0.32;
			}
		}

		shots.removeIf(shot -> {
			shot[1] -= 0.7;
			return shot[1] < 0;
		});

		// Bombs are walked by hand rather than with removeIf, because being
		// hit clears the list, and clearing a list part way through removing
		// from it leaves holes in it. That crash was real.
		boolean bombed = false;
		for (int i = bombs.size() - 1; i >= 0; i--) {
			double[] bomb = bombs.get(i);
			bomb[1] += 0.35;
			if (bomb[1] >= ROWS - 1 && Math.abs(bomb[0] - ship) < 0.9) {
				bombed = true;
				bombs.remove(i);
			} else if (bomb[1] > ROWS) {
				bombs.remove(i);
			}
		}
		if (bombed) {
			hit();
			return;
		}

		hits();

		for (Enemy enemy : enemies) {
			if (enemy.diving && enemy.y >= ROWS - 2 && Math.abs(enemy.x - ship) < 1.0) {
				hit();
				break;
			}
		}

		if (enemies.isEmpty()) {
			tell("galaga", "stage", stage);
			stage++;
			formation();
		}
	}

	/**
	 * Shots meeting enemies, and what each one is worth.
	 *
	 * Both lists are walked backwards by index. Taking an enemy out of the
	 * list you are looping over is the other way to break this.
	 */
	private void hits() {
		for (int s = shots.size() - 1; s >= 0; s--) {
			double[] shot = shots.get(s);
			for (int e = enemies.size() - 1; e >= 0; e--) {
				Enemy enemy = enemies.get(e);
				if (Math.abs(enemy.x - shot[0]) < 0.9 && Math.abs(enemy.y - shot[1]) < 0.9) {
					score += worth(enemy);
					enemies.remove(e);
					shots.remove(s);
					break;
				}
			}
		}
	}

	/** Diving is worth double, as it always has been. */
	private int worth(Enemy enemy) {
		int base = switch (enemy.kind) {
			case ROW_FLAGSHIP -> 150;
			case ROW_BUTTERFLY -> 80;
			default -> 50;
		};
		return enemy.diving ? base * 2 : base;
	}

	private void hit() {
		lives--;
		bombs.clear();
		if (lives <= 0) {
			over = true;
			tell("galaga", "dead", score);
			return;
		}
		ship = COLUMNS / 2;
	}

	@Override
	protected void steer(int dx, int dy) {
		if (dx != 0) {
			ship = Math.max(0, Math.min(COLUMNS - 1, ship + dx));
		}
		if (dy < 0) {
			fire();
		}
	}

	@Override
	protected void pressed(int key) {
		if (key == GLFW.GLFW_KEY_SPACE && !over) {
			fire();
		}
	}

	private void fire() {
		if (shots.size() < SHOTS) {
			shots.add(new double[] { ship, ROWS - 2 });
		}
	}

	@Override
	protected void paint(GuiGraphicsExtractor graphics) {
		// A few stars, so the black is space rather than nothing.
		for (int i = 0; i < 30; i++) {
			int x = (i * 7919) % columns;
			int y = ((i * 104729) + age / 3) % rows;
			dot(graphics, x, y, Math.max(1, cell / 5), 0xFF404860);
		}
		for (Enemy enemy : enemies) {
			int colour = switch (enemy.kind) {
				case ROW_FLAGSHIP -> 0xFF40E0E0;
				case ROW_BUTTERFLY -> 0xFFFF4040;
				default -> 0xFFFFD040;
			};
			dot(graphics, (int) Math.round(enemy.x), (int) Math.round(enemy.y),
					cell - 1, colour);
		}
		for (double[] shot : shots) {
			dot(graphics, (int) Math.round(shot[0]), (int) Math.round(shot[1]),
					Math.max(2, cell / 3), 0xFFFFFFFF);
		}
		for (double[] bomb : bombs) {
			dot(graphics, (int) Math.round(bomb[0]), (int) Math.round(bomb[1]),
					Math.max(2, cell / 3), 0xFFFF8040);
		}
		square(graphics, ship, ROWS - 1, 0xFF60C0FF);

		if (over) {
			graphics.text(font, Component.literal("GAME OVER"),
					originX + columns * cell / 2 - 24, originY + rows * cell / 2 - 4,
					0xFFFF5555);
		}
	}

	@Override
	protected Component status() {
		return Component.literal("Score " + score + "   Lives " + lives
				+ "   Stage " + stage + "   Up or Space to fire");
	}
}
