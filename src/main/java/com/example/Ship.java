package com.example;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Building the world: the town you start in and the ship you move on to.
 *
 * The world type is an empty flat void, so everything you can stand on is put
 * there by this. The town is three houses in a row -- the kitchen with the
 * dishes, the lawn with the weeds, and the one with the bushes -- with a
 * walkway east to the ship.
 *
 * The ship is fourteen floors stacked eight blocks apart, all of them built at
 * once so the lift has somewhere to go and so a floor you unlock later is
 * already standing when you get there. Only floor 1 and floor 5 are furnished
 * for now; the rest are the rooms waiting for the arcade, the pool, the buffet
 * and the rest to be built into them.
 */
public final class Ship {
	private Ship() {
	}

	public static void build(ServerLevel level) {
		buildTown(level);
		buildWalkway(level);
		buildShip(level);
		ShipLifeMod.LOGGER.info("Built the town and all {} floors of the ship.",
				Places.TOP_FLOOR);
	}

	// ------------------------------------------------------------------- town

	private static void buildTown(ServerLevel level) {
		// The ground everything stands on.
		fill(level, -40, Places.GROUND, -14, 40, Places.GROUND, 14, Blocks.GRASS_BLOCK);
		fill(level, -40, Places.GROUND - 1, -14, 40, Places.GROUND - 1, 14, Blocks.DIRT);

		house(level, Places.HOUSE_ONE, Blocks.OAK_PLANKS);
		house(level, Places.HOUSE_TWO, Blocks.SPRUCE_PLANKS);
		house(level, Places.HOUSE_THREE, Blocks.BIRCH_PLANKS);

		// House one: a counter with ten dishes on it, and a bin at the end.
		for (int i = 0; i < 10; i++) {
			BlockPos dish = Places.dish(i);
			set(level, dish.below(), Blocks.SMOOTH_QUARTZ);
			set(level, dish, Made.dish);
		}
		set(level, Places.GARBAGE.below(), Blocks.SMOOTH_QUARTZ);
		set(level, Places.GARBAGE, Blocks.COMPOSTER);

		// House two: the lawn is left open, with ten weeds dotted over it.
		for (int i = 0; i < 10; i++) {
			set(level, Places.weed(i), Blocks.DEAD_BUSH);
		}

		// House three: five bushes in a row. The penny is in the second one.
		for (int i = 0; i < 5; i++) {
			bush(level, Places.bush(i));
		}
	}

	/** Four walls and a roof, with the front left open so you can walk in. */
	private static void house(ServerLevel level, BlockPos middle, Block wall) {
		int x = middle.getX();
		int z = middle.getZ();
		int y = Places.GROUND + 1;
		for (int dx = -6; dx <= 6; dx++) {
			for (int dz = -6; dz <= 6; dz++) {
				boolean edge = Math.abs(dx) == 6 || Math.abs(dz) == 6;
				boolean doorway = dz == 6 && Math.abs(dx) <= 1;
				if (edge && !doorway) {
					for (int dy = 0; dy < 4; dy++) {
						set(level, new BlockPos(x + dx, y + dy, z + dz), wall);
					}
				}
				set(level, new BlockPos(x + dx, y + 4, z + dz), Blocks.STONE_BRICKS);
			}
		}
		set(level, new BlockPos(x, y + 3, z), Blocks.SEA_LANTERN);
	}

	private static void buildWalkway(ServerLevel level) {
		fill(level, 40, Places.GROUND, -1, Places.SHIP_X - Places.ROOM, Places.GROUND, 1,
				Blocks.SMOOTH_STONE);
	}

	// ------------------------------------------------------------------- ship

	/**
	 * The second ship: the same fourteen floors again, two hundred blocks east.
	 *
	 * It is built the moment it is bought rather than with the first ship,
	 * because most worlds will never buy it and a second empty tower is a lot
	 * of blocks to lay down on the chance.
	 */
	public static void buildSecond(ServerLevel level) {
		if (level.getBlockState(Places.onShip(Places.panel(1), 2)).is(Made.elevatorButton)) {
			return;
		}
		for (int floor = 1; floor <= Places.TOP_FLOOR; floor++) {
			buildFloor(level, floor, 2);
		}
		ShipLifeMod.LOGGER.info("Built ship 2.");
	}

	private static void buildShip(ServerLevel level) {
		for (int floor = 1; floor <= Places.TOP_FLOOR; floor++) {
			buildFloor(level, floor);
		}
		furnishLobby(level);
		furnishArcade(level);
		furnishPool(level);
		furnishRace(level);
		furnishFighting(level);
		furnishEvents(level);
		furnishShops(level);
		furnishYourRoom(level);
	}

	private static void buildFloor(ServerLevel level, int floor) {
		buildFloor(level, floor, 1);
	}

	private static void buildFloor(ServerLevel level, int floor, int ship) {
		int y = Places.floorY(floor);
		int x = Places.SHIP_X + (ship == 2 ? Places.SHIP_TWO_OFFSET : 0);
		int z = Places.SHIP_Z;
		int r = Places.ROOM;

		fill(level, x - r, y, z - r, x + r, y, z + r, Blocks.SMOOTH_QUARTZ);
		fill(level, x - r, y + 7, z - r, x + r, y + 7, z + r, Blocks.GRAY_CONCRETE);
		// Walls, hollow inside.
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (Math.abs(dx) != r && Math.abs(dz) != r) {
					continue;
				}
				for (int dy = 1; dy <= 6; dy++) {
					BlockPos pos = new BlockPos(x + dx, y + dy, z + dz);
					boolean window = dy >= 3 && dy <= 4 && Math.abs(dx) == r
							&& Math.abs(dz) % 4 == 0;
					set(level, pos, window ? Blocks.GLASS : Blocks.QUARTZ_BLOCK);
				}
			}
		}
		// Lights in the ceiling.
		for (int dx = -8; dx <= 8; dx += 8) {
			for (int dz = -8; dz <= 8; dz += 8) {
				set(level, new BlockPos(x + dx, y + 6, z + dz), Blocks.SEA_LANTERN);
			}
		}
		// The lift: a panel to press, and a lit alcove to stand in.
		set(level, Places.onShip(Places.panel(floor), ship), Made.elevatorButton);
		set(level, Places.onShip(Places.panel(floor), ship).above(), Blocks.REDSTONE_LAMP);
		set(level, Places.onShip(Places.lift(floor), ship), Blocks.AIR);
		set(level, Places.onShip(Places.lift(floor), ship).above(), Blocks.AIR);

		// A sign of sorts: the floor number spelled out in the floor itself.
		set(level, new BlockPos(x - r + 2, y, z), Blocks.LIGHT_BLUE_CONCRETE);

		if (floor == 1 && ship == 1) {
			// The way in from the walkway.
			set(level, new BlockPos(x - r, y + 1, z), Blocks.AIR);
			set(level, new BlockPos(x - r, y + 2, z), Blocks.AIR);
		}
	}

	private static void furnishLobby(ServerLevel level) {
		set(level, Places.DESK, Blocks.SMOOTH_QUARTZ_STAIRS);
		set(level, Places.DESK.above(), Blocks.LANTERN);
		set(level, Places.TABLE, Blocks.OAK_PLANKS);
		set(level, Places.TABLE.above(), Blocks.FLOWER_POT);
		fill(level, Places.SHIP_X + 2, Places.GROUND, Places.SHIP_Z - 6,
				Places.SHIP_X + 6, Places.GROUND, Places.SHIP_Z + 6, Blocks.POLISHED_ANDESITE);
	}

	/** Three cabinets along the far wall, and a counter facing them. */
	private static void furnishArcade(ServerLevel level) {
		cabinet(level, Places.SNAKE, Blocks.LIME_CONCRETE);
		cabinet(level, Places.PACMAN, Blocks.YELLOW_CONCRETE);
		cabinet(level, Places.GALAGA, Blocks.PURPLE_CONCRETE);
		fill(level, Places.PRIZES.getX() - 3, Places.PRIZES.getY(), Places.PRIZES.getZ(),
				Places.PRIZES.getX() + 3, Places.PRIZES.getY(), Places.PRIZES.getZ(),
				Blocks.SMOOTH_QUARTZ_STAIRS);
		set(level, Places.PRIZES, Blocks.CHISELED_QUARTZ_BLOCK);
		set(level, Places.PRIZES.above(), Blocks.LANTERN);
	}

	/** A machine: a dark screen on a coloured box, two blocks tall. */
	private static void cabinet(ServerLevel level, BlockPos pos, Block colour) {
		set(level, pos, colour);
		set(level, pos.above(), Blocks.BLACK_CONCRETE);
		set(level, pos.above(2), Blocks.SEA_LANTERN);
	}

	/**
	 * The pool: a glass tank standing on floor 3.
	 *
	 * It is built up rather than dug down, because two blocks of digging would
	 * come out through the ceiling of the arcade below.
	 */
	private static void furnishPool(ServerLevel level) {
		int y = Places.floorY(3);
		int half = Places.POOL_HALF_WIDTH;
		// The glass sides, two high, all the way round.
		for (int x = Places.POOL_START - 1; x <= Places.POOL_END + 1; x++) {
			for (int z = Places.SHIP_Z - half - 1; z <= Places.SHIP_Z + half + 1; z++) {
				boolean edge = x == Places.POOL_START - 1 || x == Places.POOL_END + 1
						|| z == Places.SHIP_Z - half - 1 || z == Places.SHIP_Z + half + 1;
				for (int dy = 1; dy <= 2; dy++) {
					set(level, new BlockPos(x, y + dy, z),
							edge ? Blocks.GLASS : Blocks.WATER);
				}
			}
		}
		// A line of colour at each end, so you can see where a lap turns.
		for (int z = Places.SHIP_Z - half; z <= Places.SHIP_Z + half; z++) {
			set(level, new BlockPos(Places.POOL_START, y, z), Blocks.LIME_CONCRETE);
			set(level, new BlockPos(Places.POOL_END, y, z), Blocks.RED_CONCRETE);
		}
		set(level, Places.POOL_BOARD, Blocks.CHISELED_QUARTZ_BLOCK);
		set(level, Places.POOL_BOARD.above(), Blocks.SEA_LANTERN);
	}

	/** Floor 6: a strip of track and a car to get into. */
	private static void furnishRace(ServerLevel level) {
		int y = Places.floorY(6);
		fill(level, Places.SHIP_X - 9, y, Places.SHIP_Z - 6,
				Places.SHIP_X + 9, y, Places.SHIP_Z + 6, Blocks.BLACK_CONCRETE);
		for (int x = Places.SHIP_X - 9; x <= Places.SHIP_X + 9; x += 2) {
			set(level, new BlockPos(x, y, Places.SHIP_Z), Blocks.WHITE_CONCRETE);
		}
		set(level, Places.RACE_CAR, Blocks.RED_CONCRETE);
		set(level, Places.RACE_CAR.above(), Blocks.BLACK_CONCRETE);
		set(level, Places.RACE_CAR.above(2), Blocks.SEA_LANTERN);
	}

	/** Floors 9 and 10: an empty room with a button, and one with two doors. */
	private static void furnishFighting(ServerLevel level) {
		set(level, Places.FIGHT_BUTTON, Blocks.REDSTONE_BLOCK);
		set(level, Places.FIGHT_BUTTON.above(), Blocks.REDSTONE_LAMP);
		fill(level, Places.SHIP_X - 9, Places.floorY(9), Places.SHIP_Z - 9,
				Places.SHIP_X + 9, Places.floorY(9), Places.SHIP_Z + 9,
				Blocks.POLISHED_BLACKSTONE_BRICKS);

		set(level, Places.ARACHNES_DOOR, Blocks.COBWEB);
		set(level, Places.ARACHNES_DOOR.above(), Blocks.SEA_LANTERN);
		set(level, Places.DRAGON_DOOR, Blocks.OBSIDIAN);
		set(level, Places.DRAGON_DOOR.above(), Blocks.SEA_LANTERN);
		fill(level, Places.SHIP_X - 9, Places.floorY(10), Places.SHIP_Z - 9,
				Places.SHIP_X + 9, Places.floorY(10), Places.SHIP_Z + 9,
				Blocks.POLISHED_BLACKSTONE);
	}

	/** A counter apiece on floors 8, 11, 12 and 13, and one more on 7. */
	private static void furnishShops(ServerLevel level) {
		counter(level, Places.STORE, Blocks.BOOKSHELF);
		counter(level, Places.TICKET_SHOP, Blocks.GOLD_BLOCK);
		counter(level, Places.REWARD_DESK, Blocks.CHEST);
		counter(level, Places.PET_STORE, Blocks.HAY_BLOCK);
		counter(level, Places.KEG, Blocks.BARREL);
		counter(level, Places.PASSPORT_DESK, Blocks.LECTERN);
	}

	/** A shop counter: something to click, lit so you can find it. */
	private static void counter(ServerLevel level, BlockPos pos, Block front) {
		set(level, pos, front);
		set(level, pos.above(), Blocks.LANTERN);
		fill(level, pos.getX() - 3, pos.getY() - 1, pos.getZ(),
				pos.getX() + 3, pos.getY() - 1, pos.getZ(), Blocks.POLISHED_ANDESITE);
	}

	/** Floor 7 is mostly an empty hall with a board on the wall. */
	private static void furnishEvents(ServerLevel level) {
		set(level, Places.EVENT_BOARD, Blocks.CHISELED_BOOKSHELF);
		set(level, Places.EVENT_BOARD.above(), Blocks.SEA_LANTERN);
		fill(level, Places.SHIP_X - 4, Places.floorY(7), Places.SHIP_Z - 4,
				Places.SHIP_X + 4, Places.floorY(7), Places.SHIP_Z + 4,
				Blocks.POLISHED_BLACKSTONE);
	}

	private static void furnishYourRoom(ServerLevel level) {
		set(level, Places.TOILET, Blocks.CAULDRON);
		set(level, Places.FRIDGE, Blocks.IRON_BLOCK);
		set(level, Places.FRIDGE.above(), Blocks.IRON_BLOCK);
		set(level, Places.TV, Blocks.BLACK_CONCRETE);
		set(level, Places.TV.above(), Blocks.BLACK_CONCRETE);
		set(level, Places.PHONE, Blocks.OAK_PRESSURE_PLATE);
		bed(level, Places.BED);
		// A bathroom corner, so the toilet isn't in the middle of the room.
		fill(level, Places.SHIP_X + 5, Places.floorY(5) + 1, Places.SHIP_Z - 12,
				Places.SHIP_X + 5, Places.floorY(5) + 4, Places.SHIP_Z - 5,
				Blocks.WHITE_CONCRETE);
	}

	/**
	 * A bush.
	 *
	 * Leaves with no tree under them are dead leaves as far as Minecraft is
	 * concerned, and they quietly rot away -- which is what happened to the
	 * first five. Marking them persistent is what a player placing leaves by
	 * hand does, and it is why theirs stay put.
	 */
	private static void bush(ServerLevel level, BlockPos pos) {
		level.setBlockAndUpdate(pos, Blocks.OAK_LEAVES.defaultBlockState()
				.setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
	}

	/**
	 * Put back anything that has gone missing from a world built earlier.
	 *
	 * Run on every join, so a world made before the bushes were persistent
	 * gets its bushes back rather than needing to be started again.
	 */
	public static void repair(ServerLevel level) {
		// The arcade came after the first worlds were built.
		if (!level.getBlockState(Places.PACMAN).is(Blocks.YELLOW_CONCRETE)) {
			furnishArcade(level);
		furnishPool(level);
		furnishRace(level);
		furnishFighting(level);
		furnishEvents(level);
		furnishShops(level);
			ShipLifeMod.LOGGER.info("Put the arcade into a world built before it.");
		}
		int replaced = 0;
		for (int i = 0; i < 5; i++) {
			BlockPos pos = Places.bush(i);
			if (!level.getBlockState(pos).is(Blocks.OAK_LEAVES)) {
				bush(level, pos);
				replaced++;
			}
		}
		if (replaced > 0) {
			ShipLifeMod.LOGGER.info("Put {} bush(es) back at house three.", replaced);
		}
	}

	/** A bed is two blocks that have to agree which way round they are. */
	private static void bed(ServerLevel level, BlockPos foot) {
		BlockState base = Blocks.WHITE_BED.defaultBlockState()
				.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
						Direction.EAST);
		level.setBlockAndUpdate(foot, base.setValue(
				net.minecraft.world.level.block.BedBlock.PART,
				net.minecraft.world.level.block.state.properties.BedPart.FOOT));
		level.setBlockAndUpdate(foot.east(), base.setValue(
				net.minecraft.world.level.block.BedBlock.PART,
				net.minecraft.world.level.block.state.properties.BedPart.HEAD));
	}

	// ------------------------------------------------------------------ small

	private static void set(ServerLevel level, BlockPos pos, Block block) {
		level.setBlockAndUpdate(pos, block.defaultBlockState());
	}

	private static void fill(ServerLevel level, int x1, int y1, int z1,
			int x2, int y2, int z2, Block block) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
			for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
				for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
					pos.set(x, y, z);
					level.setBlockAndUpdate(pos, block.defaultBlockState());
				}
			}
		}
	}
}
