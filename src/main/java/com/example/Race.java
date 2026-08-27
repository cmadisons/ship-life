package com.example;

import java.util.Random;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;

/**
 * Floor 6: the race track.
 *
 * Three lanes, a curvy track and a field of cars that get in your way. Hit one
 * and you slow down; they hit you and you slow down; and one sitting beside
 * you cannot be barged out of the road, so the lane you want is not always the
 * lane you can have.
 *
 * You start with 250 gas and boosting burns one a second for twice the speed.
 * Run the tank dry and you are out of the race, so the whole thing is deciding
 * where the speed is worth paying for -- and refilling costs you the time you
 * spend doing it.
 *
 * Charlie's quest is five laps in two minutes. A gentle lap is about
 * twenty-five seconds, and five of those is five seconds too slow, so it
 * cannot be done without pushing.
 */
public class Race extends Game {
	/** Five laps in two minutes opens floor 7. */
	public static final int LAPS = 5;
	public static final int TARGET_TICKS = 2400;

	/** How far one lap is, in the track's own units. */
	private static final int LAP_LENGTH = 340;

	private final Random random = new Random();

	/** Which lane you are in, 0 to 2. */
	private int lane = 1;

	/** Cars ahead: how far along they are, and which lane. */
	private final int[] carAt = new int[6];
	private final int[] carLane = new int[6];

	private int gas = 250;
	private int distance;
	private int lap;
	private boolean boosting;
	private boolean refilling;
	private int slowedFor;
	private int startedAt = -1;

	public Race(ServerPlayer player) {
		super(player);
		for (int i = 0; i < carAt.length; i++) {
			carAt[i] = 40 + i * 55;
			carLane[i] = random.nextInt(3);
		}
	}

	@Override
	public String title() {
		return "Race Track";
	}

	@Override
	public int speed() {
		return 2;
	}

	@Override
	public void step() {
		if (startedAt < 0) {
			startedAt = age;
		}

		// Refilling stops you dead; boosting doubles you; being hit halves you.
		int move = refilling ? 0 : boosting ? 4 : 2;
		if (slowedFor > 0) {
			move = Math.max(1, move / 2);
			slowedFor--;
		}
		if (refilling) {
			gas = Math.min(250, gas + 3);
		} else if (boosting) {
			gas -= 1;
			if (gas <= 0) {
				out();
				return;
			}
		}

		distance += move;
		if (distance / LAP_LENGTH > lap) {
			lap = distance / LAP_LENGTH;
			player.level().playSound(null, player.blockPosition(),
					SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
			if (lap >= LAPS) {
				finish();
				return;
			}
		}

		// The field moves at its own pace, and drifts across the lanes.
		for (int i = 0; i < carAt.length; i++) {
			carAt[i] += 1 + random.nextInt(2);
			if (random.nextInt(40) == 0) {
				carLane[i] = Math.max(0, Math.min(2, carLane[i] + random.nextInt(3) - 1));
			}
			int gap = carAt[i] - distance;
			if (gap > -3 && gap < 3 && carLane[i] == lane && slowedFor == 0) {
				slowedFor = 12;
				player.level().playSound(null, player.blockPosition(),
						SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.6f, 0.8f);
				say("You hit one -- slowed.");
			}
		}
	}

	private void out() {
		over = true;
		player.sendSystemMessage(Component.literal("Out of gas on lap " + (lap + 1)
				+ ". That is the race over.").withStyle(ChatFormatting.RED));
	}

	private void finish() {
		over = true;
		int ticks = age - startedAt;
		player.sendSystemMessage(Component.literal(LAPS + " laps in "
				+ Pool.time(ticks) + ".").withStyle(ChatFormatting.GREEN));
		if (ticks <= TARGET_TICKS) {
			if (!State.hasFloor(player, 7)) {
				State.unlock(player, 7);
				player.sendSystemMessage(Component.literal(
						"Under two minutes. Floor 7 -- the events -- is open.")
						.withStyle(ChatFormatting.AQUA));
			}
		} else {
			player.sendSystemMessage(Component.literal(
					"Charlie wants five laps in two minutes. Boost more, refill less.")
					.withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	public void draw() {
		blank();
		// The road: three lanes wide, drawn three columns apiece.
		for (int row = 0; row < ROWS; row++) {
			for (int laneIndex = 0; laneIndex < 3; laneIndex++) {
				for (int part = 0; part < 3; part++) {
					int column = laneIndex * 3 + part;
					// A curve, drawn by shading the road as it goes by.
					boolean kerb = part == 1 && ((distance / 8 + row) % 4 == 0);
					put(column, row, cell(kerb ? Items.GRAY_CONCRETE : Items.BLACK_CONCRETE,
							" "));
				}
			}
		}
		// Cars ahead of you, drawn by how far ahead they are.
		for (int i = 0; i < carAt.length; i++) {
			int gap = carAt[i] - distance;
			int row = ROWS - 2 - gap / 6;
			if (row >= 0 && row < ROWS - 1) {
				put(carLane[i] * 3 + 1, row, cell(Items.RED_CONCRETE, "Car"));
			}
		}
		put(lane * 3 + 1, ROWS - 1, cell(slowedFor > 0 ? Items.ORANGE_CONCRETE
				: Items.LIGHT_BLUE_CONCRETE, "You"));

		button(0, Items.ARROW, "◀ Left  (A)");
		button(1, Items.BLAZE_POWDER, boosting ? "Boost ON  (W)" : "Boost  (W)");
		button(2, Items.WATER_BUCKET, refilling ? "Refilling  (S)" : "Refill gas  (S)");
		button(3, Items.ARROW, "▶ Right  (D)");
		button(4, Items.BUCKET, "Gas " + gas);
		button(5, Items.CLOCK, startedAt < 0 ? "0.0s" : Pool.time(age - startedAt));
		button(6, Items.NETHER_STAR, "Lap " + Math.min(lap + 1, LAPS) + " of " + LAPS);
		button(7, over ? Items.LIME_DYE : Items.GRAY_DYE, over ? "Race again" : "Racing");
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
				new Race(player).open();
			}
			return;
		}
		switch (button) {
			case 0 -> move(-1);
			case 3 -> move(1);
			case 1 -> {
				boosting = !boosting;
				refilling = false;
			}
			case 2 -> {
				refilling = !refilling;
				boosting = false;
			}
			default -> {
			}
		}
	}

	/** You cannot barge through a car sitting beside you. */
	private void move(int by) {
		int want = Math.max(0, Math.min(2, lane + by));
		for (int i = 0; i < carAt.length; i++) {
			int gap = carAt[i] - distance;
			if (carLane[i] == want && gap > -4 && gap < 4) {
				say("There is a car right there.");
				return;
			}
		}
		lane = want;
	}

	private void say(String text) {
		Hud.busy(player, 20);
		player.sendOverlayMessage(Component.literal(text).withStyle(ChatFormatting.YELLOW));
	}
}
