package com.example;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * The television in your room.
 *
 * Grey and off until you press it, and what it shows is the last fight you
 * were in -- a wave off floor 9 or a boss off floor 10, how it went and what
 * it paid. The ship keeps one fight, the last one, so this is the replay and
 * not a history.
 *
 * The screen lights up while it is on and goes grey again by itself, which is
 * the only animation a wall of concrete is going to manage.
 */
public final class Telly {
	private Telly() {
	}

	/** How long the screen stays lit, in ticks. */
	private static final int ON_FOR = 200;

	/**
	 * When the screen goes off again, or zero while it is dark.
	 *
	 * This was a Map<BlockPos, Long> keyed on the screen, iterated with a
	 * defensive copy each tick. There is one television on the ship and it is
	 * at Places.TV, so the map never held more than a single entry -- a long
	 * says the same thing.
	 */
	private static long offAt = 0L;

	public static void register() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK
				.register(server -> {
			if (offAt == 0L || server.getTickCount() % 20 != 0) {
				return;
			}
			for (ServerLevel level : server.getAllLevels()) {
				if (!ShipLifeMod.isShipLife(level)) {
					continue;
				}
				if (level.getGameTime() >= offAt) {
					offAt = 0L;
					paint(level, Blocks.GRAY_CONCRETE);
				}
			}
		});
	}

	/** Is this one of the blocks the screen is made of? */
	public static boolean isScreen(BlockPos pos) {
		BlockPos tv = Places.TV;
		return pos.getZ() == tv.getZ()
				&& Math.abs(pos.getX() - tv.getX()) <= 1
				&& pos.getY() >= tv.getY() && pos.getY() <= tv.getY() + 2;
	}

	/** Turn it on: light the screen and put the last fight on it. */
	public static void watch(ServerPlayer player, ServerLevel level) {
		String last = State.lastFight(player);

		paint(level, Blocks.LIGHT_BLUE_CONCRETE);
		offAt = level.getGameTime() + ON_FOR;
		level.playSound(null, Places.TV, SoundEvents.NOTE_BLOCK_HAT.value(),
				SoundSource.BLOCKS, 0.6f, 1.6f);

		SimpleContainer page = Book.page(Items.GRAY_STAINED_GLASS_PANE);
		page.setItem(4, Book.entry(Items.TARGET, "The Board", ChatFormatting.AQUA,
				"Everything you have a record in.",
				"Your last fight is in the middle."));
		page.setItem(19, Book.entry(Items.GOLDEN_APPLE, "The Machines",
				ChatFormatting.YELLOW,
				"Pac-Man record: " + State.best(player),
				State.tally(player, State.FOODS) + " food eaten in Snake",
				State.tally(player, State.ROUNDS) + " Galaga rounds passed",
				State.tally(player, State.EARNED) + " arcade tickets earned"));
		page.setItem(21, Book.entry(Items.HEART_OF_THE_SEA, "The Pool",
				ChatFormatting.AQUA,
				State.tally(player, State.LAPS) + " laps swum",
				"Best lap: " + (State.bestLap(player) == 0
						? "none yet" : Pool.time(State.bestLap(player)))));
		page.setItem(23, Book.entry(Items.MINECART, "The Track", ChatFormatting.RED,
				State.tally(player, State.RACES) + " races finished",
				"Your best: " + (State.tally(player, State.BEST_RACE) == 0
						? "none yet" : Pool.time(State.tally(player, State.BEST_RACE))),
				// The racer keeps up with you now, so this has to ask rather
				// than print the constant it used to.
				"The racer does five laps in " + Pool.time(Kart.rivalTicks(player))));
		page.setItem(25, Book.entry(Items.IRON_SWORD, "The Fighting", ChatFormatting.RED,
				State.tally(player, State.WAVES) + " waves cleared",
				State.tally(player, State.BOSSES) + " bosses beaten",
				State.tally(player, State.BOMBS_USED) + " bombs used"));

		if (last.isEmpty()) {
			page.setItem(40, Book.entry(Items.GRAY_DYE, "No Fight Yet", ChatFormatting.GRAY,
					"You have not been in a fight yet.",
					"Floor 9 is the waves; floor 10 is the bosses.",
					"Whatever you do down there is on here after."));
		} else {
			String[] bits = last.split("\\|");
			String what = bits.length > 0 ? bits[0] : "A fight";
			String how = bits.length > 1 ? bits[1] : "";
			String paid = bits.length > 2 ? bits[2] : "0";
			String when = bits.length > 3 ? bits[3] : "";

			page.setItem(40, Book.entry(Items.IRON_SWORD, "Your Last Fight",
					ChatFormatting.RED,
					what + " -- " + how,
					paid + " event tickets",
					when));
		}

		page.setItem(49, Book.entry(Items.BARRIER, "Off", ChatFormatting.RED,
				"Press Escape."));

		player.openMenu(new SimpleMenuProvider(
				(id, inventory, who) -> new ReadOnlyMenu(id, inventory, page,
						(clicker, slot) -> clicker.closeContainer()),
				Component.literal("Television")));
	}

	/** The three-by-two screen, all one colour. */
	private static void paint(ServerLevel level, net.minecraft.world.level.block.Block block) {
		BlockPos tv = Places.TV;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = 0; dy <= 2; dy++) {
				BlockPos pos = tv.offset(dx, dy, 0);
				if (level.getBlockState(pos).is(Blocks.GRAY_CONCRETE)
						|| level.getBlockState(pos).is(Blocks.LIGHT_BLUE_CONCRETE)) {
					level.setBlockAndUpdate(pos, block.defaultBlockState());
				}
			}
		}
	}
}
