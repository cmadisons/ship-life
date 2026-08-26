package com.example;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;

/**
 * Snake. A ticket for every food you eat.
 *
 * The snake never stops moving, the walls are the walls, and eating your own
 * tail is the end of it. Tickets are paid the moment you eat rather than at
 * the end, so a run that goes wrong still leaves you with what you earned.
 */
public class Snake extends Game {
	private final Deque<int[]> body = new ArrayDeque<>();
	private final Random random = new Random();
	private int dx = 1;
	private int dy = 0;
	private int[] food;
	private int eaten;

	public Snake(ServerPlayer player) {
		super(player);
		body.addFirst(new int[] { 4, 2 });
		body.addLast(new int[] { 3, 2 });
		dropFood();
	}

	@Override
	public String title() {
		return "Snake";
	}

	@Override
	public int speed() {
		return 6;
	}

	@Override
	public void step() {
		int[] head = body.peekFirst();
		int x = head[0] + dx;
		int y = head[1] + dy;

		if (x < 0 || y < 0 || x >= COLUMNS || y >= ROWS || hits(x, y)) {
			over = true;
			player.level().playSound(null, player.blockPosition(),
					SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.7f, 0.6f);
			player.sendSystemMessage(Component.literal("Snake over -- " + eaten
					+ " food eaten.").withStyle(ChatFormatting.GRAY));
			return;
		}

		body.addFirst(new int[] { x, y });
		if (x == food[0] && y == food[1]) {
			eaten++;
			win(1, "one food");
			player.level().playSound(null, player.blockPosition(),
					SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.5f, 1.8f);
			dropFood();
		} else {
			body.removeLast();
		}
	}

	private boolean hits(int x, int y) {
		for (int[] part : body) {
			if (part[0] == x && part[1] == y) {
				return true;
			}
		}
		return false;
	}

	private void dropFood() {
		do {
			food = new int[] { random.nextInt(COLUMNS), random.nextInt(ROWS) };
		} while (hits(food[0], food[1]));
	}

	@Override
	public void draw() {
		blank();
		put(food[0], food[1], cell(Items.APPLE, "Food"));
		boolean head = true;
		for (int[] part : body) {
			put(part[0], part[1], cell(head ? Items.LIME_CONCRETE : Items.GREEN_CONCRETE,
					head ? "Snake" : " "));
			head = false;
		}
		button(0, Items.ARROW, "◀ Left");
		button(1, Items.SPECTRAL_ARROW, "▲ Up");
		button(2, Items.SPECTRAL_ARROW, "▼ Down");
		button(3, Items.ARROW, "▶ Right");
		button(4, Items.APPLE, "Eaten: " + eaten);
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
				new Snake(player).open();
			}
			return;
		}
		switch (button) {
			// You cannot turn back on yourself -- that would be eating your neck.
			case 0 -> turn(-1, 0);
			case 1 -> turn(0, -1);
			case 2 -> turn(0, 1);
			case 3 -> turn(1, 0);
			default -> {
			}
		}
	}

	private void turn(int toX, int toY) {
		if (dx + toX == 0 && dy + toY == 0) {
			return;
		}
		dx = toX;
		dy = toY;
	}
}
