package com.example;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
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
	/**
	 * Pull ship 2 down.
	 *
	 * It was this ship again floor for floor with nothing new on it and no
	 * way in since the lift stopped offering it, so what is left is a
	 * hundred-block tower nobody can reach. This clears the whole footprint
	 * back to air, once, the first time a world with one in it is loaded.
	 */
	public static void clearSecond(ServerLevel level) {
		int x = Places.SHIP_X + Places.SHIP_TWO_OFFSET;
		int z = Places.SHIP_Z;
		int r = Places.ROOM + 1;
		if (!level.getBlockState(Places.onShip(Places.panel(1), 2)).is(Made.elevatorButton)) {
			return;
		}
		fill(level, x - r, Places.GROUND, z - r,
				x + r, Places.floorY(Places.TOP_FLOOR) + 8, z + r, Blocks.AIR);
		ShipLifeMod.LOGGER.info("Pulled ship 2 down.");
	}

	private static void buildShip(ServerLevel level) {
		for (int floor = 1; floor <= Places.TOP_FLOOR; floor++) {
			buildFloor(level, floor);
		}
		furnishLobby(level);
		furnishArcade(level);
		furnishPool(level);
		furnishBuffet(level);
		furnishRace(level);
		furnishFighting(level);
		furnishEvents(level);
		furnishShops(level);
		furnishBensRoom(level);
		furnishIzzysRoom(level);
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

		// The hull is two blocks thick, and the second block goes on the
		// outside -- putting it inside would take a block off every room and
		// bury the lift car in the corner.
		int outer = r + 1;
		fill(level, x - outer, y, z - outer, x + outer, y, z + outer, Blocks.BLACK_CONCRETE);
		fill(level, x - outer, y + 7, z - outer, x + outer, y + 7, z + outer,
				Blocks.GRAY_CONCRETE);
		for (int dx = -outer; dx <= outer; dx++) {
			for (int dz = -outer; dz <= outer; dz++) {
				if (Math.abs(dx) < r && Math.abs(dz) < r) {
					continue;                       // the room itself, left hollow
				}
				for (int dy = 1; dy <= 6; dy++) {
					BlockPos pos = new BlockPos(x + dx, y + dy, z + dz);
					boolean window = dy >= 3 && dy <= 4
							&& Math.abs(dx) >= r && Math.abs(dz) % 4 == 0;
					// Glass again, so there is something to look out of. Only
					// the lift stays metal all the way round.
					set(level, pos, window ? Blocks.GLASS : Blocks.BLACK_CONCRETE);
				}
			}
		}
		// Lights in the ceiling.
		for (int dx = -8; dx <= 8; dx += 8) {
			for (int dz = -8; dz <= 8; dz += 8) {
				set(level, new BlockPos(x + dx, y + 6, z + dz), Blocks.SEA_LANTERN);
			}
		}
		// A door where the lift used to be, in the middle of the near wall.
		// Only on floor 1, where it is the way in off the gangway: on every
		// other floor it opened onto the black concrete of the hull, which is
		// a door to nowhere.
		if (floor == 1) {
			door(level, Places.onShip(Places.oldLift(floor), ship));
		}

		liftCar(level, floor, ship);

		// There used to be a light blue block here, meant as a marker. It only
		// ever read as one odd block in the floor.
		set(level, new BlockPos(x - r + 2, y, z), Blocks.BLACK_CONCRETE);

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

		// The red wool chairs that stood here before the black ones. Clearing
		// the space first means an older world does not end up with one
		// inside the other.
		fill(level, Places.SHIP_X, Places.GROUND + 1, Places.SHIP_Z - 9,
				Places.SHIP_X + 8, Places.GROUND + 7, Places.SHIP_Z + 9, Blocks.AIR);

		// The two giant chairs, facing each other over a table big enough to
		// match them. Charlie is in the south one.
		giantChair(level, Places.CHAIR, true);
		giantChair(level, Places.CHAIR_TWO, false);
		bigTable(level, Places.BIG_TABLE);
		fill(level, Places.SHIP_X + 2, Places.GROUND, Places.SHIP_Z - 6,
				Places.SHIP_X + 6, Places.GROUND, Places.SHIP_Z + 6, Blocks.POLISHED_ANDESITE);
	}

	/** Three cabinets along the far wall, and a counter facing them. */
	/**
	 * A chair you have to climb into.
	 *
	 * Black wool. An X three blocks each way along the floor, one block
	 * standing on the middle of it, and a 3x3 seat on top of that. Behind the
	 * seat a back three wide and three high; either side of it an arm three
	 * high; and a ladder up the front, which is the side facing the table.
	 *
	 * `north` says which way it looks: true for the chair on the south side,
	 * which faces north across the room at the other one.
	 */
	private static void giantChair(ServerLevel level, BlockPos seat, boolean north) {
		int x = seat.getX();
		int z = seat.getZ();
		int y = seat.getY() - 3;               // the floor the chair stands on
		int backZ = north ? z + 2 : z - 2;     // behind you
		int frontZ = north ? z - 2 : z + 2;    // the side you climb

		// The X: three blocks out along each diagonal, and the middle of it.
		set(level, new BlockPos(x, y, z), Blocks.BLACK_WOOL);
		for (int step = 1; step <= 3; step++) {
			set(level, new BlockPos(x - step, y, z - step), Blocks.BLACK_WOOL);
			set(level, new BlockPos(x + step, y, z - step), Blocks.BLACK_WOOL);
			set(level, new BlockPos(x - step, y, z + step), Blocks.BLACK_WOOL);
			set(level, new BlockPos(x + step, y, z + step), Blocks.BLACK_WOOL);
		}

		// The one block standing on the middle of the X, and the 3x3 seat on
		// top of it.
		set(level, new BlockPos(x, y + 1, z), Blocks.BLACK_WOOL);
		fill(level, x - 1, y + 2, z - 1, x + 1, y + 2, z + 1, Blocks.BLACK_WOOL);

		// The back: three wide, three high, behind the seat.
		fill(level, x - 1, y + 3, backZ, x + 1, y + 5, backZ, Blocks.BLACK_WOOL);

		// An arm each side, three high, alongside the seat.
		fill(level, x - 2, y + 3, z - 1, x - 2, y + 5, z + 1, Blocks.BLACK_WOOL);
		fill(level, x + 2, y + 3, z - 1, x + 2, y + 5, z + 1, Blocks.BLACK_WOOL);

		// Something for the ladder to hang on, under the front of the seat,
		// and then the ladder itself from the floor to the seat.
		int lip = north ? z - 1 : z + 1;
		fill(level, x, y, lip, x, y + 1, lip, Blocks.BLACK_WOOL);
		for (int dy = 0; dy <= 2; dy++) {
			ladder(level, new BlockPos(x, y + dy, frontZ), north
					? net.minecraft.core.Direction.NORTH
					: net.minecraft.core.Direction.SOUTH);
		}
	}

	/** One rung, hung on the face of the chair it climbs. */
	private static void ladder(ServerLevel level, BlockPos where,
			net.minecraft.core.Direction facing) {
		level.setBlockAndUpdate(where, Blocks.LADDER.defaultBlockState()
				.setValue(net.minecraft.world.level.block.LadderBlock.FACING, facing));
	}

	/**
	 * The table between the two chairs: a T, and wool like they are.
	 *
	 * A leg three blocks high with a 3x3 top laid across it, so it stands at
	 * about the height of the seats either side of it.
	 */
	private static void bigTable(ServerLevel level, BlockPos foot) {
		int x = foot.getX();
		int y = foot.getY();
		int z = foot.getZ();
		fill(level, x, y, z, x, y + 2, z, Blocks.BLACK_WOOL);
		fill(level, x - 1, y + 3, z - 1, x + 1, y + 3, z + 1, Blocks.BLACK_WOOL);
	}

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

	/**
	 * An upright arcade cabinet, built the way the real ones are.
	 *
	 * Three blocks wide and four tall: coloured side panels, a black glass
	 * screen at eye height, a lit marquee across the top, and a control panel
	 * sloping out towards you at waist height. You play it by right-clicking
	 * any part of it.
	 */
	private static void cabinet(ServerLevel level, BlockPos pos, Block colour) {
		// The two side panels, full height, in the machine's own colour.
		for (int dx = -1; dx <= 1; dx += 2) {
			for (int dy = 0; dy <= 2; dy++) {
				set(level, pos.offset(dx, dy, 0), colour);
			}
		}
		// The middle: a dark base, the screen, and the marquee above it.
		set(level, pos, Blocks.POLISHED_BLACKSTONE);
		set(level, pos.above(), Blocks.BLACK_STAINED_GLASS);
		set(level, pos.above(2), colour);
		// The marquee is lit, which is what makes a row of them look like an
		// arcade rather than a row of boxes.
		for (int dx = -1; dx <= 1; dx++) {
			set(level, pos.offset(dx, 3, 0), Blocks.SEA_LANTERN);
		}
		// The control panel, sloping out towards whoever is standing there.
		BlockState panel = Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState()
				.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
						Direction.NORTH);
		for (int dx = -1; dx <= 1; dx++) {
			level.setBlockAndUpdate(pos.offset(dx, 0, 1), panel);
		}
	}

	/**
	 * The pool, sunk into the floor of floor 3.
	 *
	 * Two blocks of water with the surface flush with the deck, so you step
	 * down into it the way you step into a real pool rather than climbing into
	 * a glass tank.
	 *
	 * Digging two blocks takes the bottom through what was the arcade's
	 * ceiling, so the pool lays its own floor a block lower and seals itself.
	 * The arcade below loses one block of headroom under the pool footprint
	 * and nothing else.
	 */
	private static void furnishPool(ServerLevel level) {
		int y = Places.floorY(3);
		int half = Places.POOL_HALF_WIDTH;

		// The hole, its own sealed bottom, and the water in it. Under the
		// bottom the casing carries on down to the arcade's own floor.
		for (int x = Places.POOL_START; x <= Places.POOL_END; x++) {
			for (int z = Places.SHIP_Z - half; z <= Places.SHIP_Z + half; z++) {
				for (int dy = Places.POOL_DEPTH + 1; dy <= Places.FLOOR_HEIGHT; dy++) {
					set(level, new BlockPos(x, y - dy, z), Blocks.PRISMARINE);
				}
				for (int dy = Places.POOL_DEPTH; dy >= 0; dy--) {
					set(level, new BlockPos(x, y - dy, z), Blocks.WATER);
				}
			}
		}

		// A line of colour at each end, on the bottom, so a turn is visible.
		for (int z = Places.SHIP_Z - half; z <= Places.SHIP_Z + half; z++) {
			set(level, new BlockPos(Places.POOL_START, y - Places.POOL_DEPTH - 1, z),
					Blocks.LIME_CONCRETE);
			set(level, new BlockPos(Places.POOL_END, y - Places.POOL_DEPTH - 1, z),
					Blocks.RED_CONCRETE);
		}

		// A tiled lip round the edge, and the wall that holds the water in.
		//
		// The lip is the part you can see. The wall under it is the part that
		// matters: the hole passes through the arcade's airspace, so without a
		// side to every level of it the pool would empty itself into floor 2
		// the moment anything disturbed the water.
		for (int x = Places.POOL_START - 1; x <= Places.POOL_END + 1; x++) {
			for (int z = Places.SHIP_Z - half - 1; z <= Places.SHIP_Z + half + 1; z++) {
				boolean edge = x == Places.POOL_START - 1 || x == Places.POOL_END + 1
						|| z == Places.SHIP_Z - half - 1 || z == Places.SHIP_Z + half + 1;
				if (!edge) {
					continue;
				}
				set(level, new BlockPos(x, y, z), Blocks.PRISMARINE_BRICKS);
				// A full storey of casing, not just enough to hold the water.
				// The pool hangs into the arcade either way, so it may as well
				// be a solid column that nothing can ever get through.
				for (int dy = 1; dy <= Places.FLOOR_HEIGHT; dy++) {
					set(level, new BlockPos(x, y - dy, z), Blocks.PRISMARINE_BRICKS);
				}
			}
		}

		set(level, Places.POOL_BOARD, Blocks.CHISELED_QUARTZ_BLOCK);
		set(level, Places.POOL_BOARD.above(), Blocks.SEA_LANTERN);
	}

	/**
	 * Floor 4: the buffet -- a counter with a cook behind it, and four tables.
	 *
	 * The tables are laid for two and are real furniture rather than scenery:
	 * a post with a top on it and a chair pulled up either side, so a room you
	 * are told is a restaurant looks like one.
	 */
	private static void furnishBuffet(ServerLevel level) {
		int y = Places.floorY(4);

		// The counter, and the cook standing behind it.
		for (int dx = -4; dx <= 4; dx++) {
			set(level, new BlockPos(Places.SHIP_X + dx, y + 1, Places.SHIP_Z - 8),
					Blocks.SMOOTH_QUARTZ_STAIRS);
		}
		set(level, Places.BUFFET_COOK, Blocks.SMOKER);
		set(level, Places.BUFFET_COOK.above(), Blocks.LANTERN);

		// The food along the counter, under glass the way a buffet keeps it.
		for (int dx = -3; dx <= 3; dx += 3) {
			set(level, new BlockPos(Places.SHIP_X + dx, y + 2, Places.SHIP_Z - 8), Blocks.GLASS);
		}

		for (int i = 0; i < Places.TABLES; i++) {
			table(level, Places.table(i));
		}
	}

	/**
	 * The lift car: five by five by five, standing in the corner of the floor.
	 *
	 * It was a button on a wall and an alcove to stand in, which is a lift the
	 * way a bus stop is a bus. This is a room you walk into: quartz walls, a
	 * glass front so you can see out of it, the button inside where you can
	 * reach it, and two doors -- one facing along the floor and one across it,
	 * so you are never walking round the car to get in.
	 *
	 * A pressure plate sits each side of both doors. The button still works;
	 * the plates are for when your hands are full.
	 */
	private static void liftCar(ServerLevel level, int floor, int ship) {
		int y = Places.floorY(floor);
		int n = Places.LIFT_SIZE;
		int x0 = Places.LIFT_X + (ship == 2 ? Places.SHIP_TWO_OFFSET : 0);
		int z0 = Places.LIFT_Z;

		for (int dx = 0; dx < n; dx++) {
			for (int dz = 0; dz < n; dz++) {
				boolean edge = dx == 0 || dx == n - 1 || dz == 0 || dz == n - 1;
				for (int dy = 1; dy <= n; dy++) {
					BlockPos pos = new BlockPos(x0 + dx, y + dy, z0 + dz);
					if (dy == n) {
						set(level, pos, Blocks.GRAY_CONCRETE);        // the roof
					} else if (!edge) {
						set(level, pos, Blocks.AIR);                  // stand in here
					} else {
						// Glass at eye level on the two open sides.
						boolean front = dx == n - 1 || dz == n - 1;
						// Metal at eye level as well: the whole ship is
						// metal, and a glass lift in it looked borrowed.
						set(level, pos, front && dy >= 2 && dy <= 3
								? Blocks.IRON_BLOCK : Blocks.QUARTZ_BLOCK);
					}
				}
			}
		}

		// A second skin on the outside of the car, so its walls are two thick
		// like the ship's are. The two doorways are left open, and anything
		// that would land on the room's own wall is left alone -- the hull is
		// already two blocks there.
		for (int dx = -1; dx <= n; dx++) {
			for (int dz = -1; dz <= n; dz++) {
				if (dx != -1 && dx != n && dz != -1 && dz != n) {
					continue;                        // the car itself
				}
				int wx = x0 + dx;
				int wz = z0 + dz;
				if (wx <= Places.SHIP_X - Places.ROOM || wz <= Places.SHIP_Z - Places.ROOM) {
					continue;                        // that is the ship's wall
				}
				boolean doorway = dx == n && dz == 2;
				for (int dy = 1; dy <= n; dy++) {
					if (doorway && dy <= 2) {
						continue;                    // walk through here
					}
					set(level, new BlockPos(wx, y + dy, wz),
							dy == n ? Blocks.GRAY_CONCRETE : Blocks.QUARTZ_BLOCK);
				}
			}
		}

		set(level, Places.onShip(Places.panel(floor), ship), Made.elevatorButton);
		// Iron over the button. A redstone lamp up there was never wired to
		// anything, so it was a light that never lit.
		set(level, Places.onShip(Places.panel(floor), ship).above(), Blocks.IRON_BLOCK);
		set(level, new BlockPos(x0 + 2, y + n - 1, z0 + 2), Blocks.SEA_LANTERN);

		// One door, the far one from the button.
		ironDoor(level, Places.onShip(Places.liftDoorEast(floor), ship), Direction.EAST);
		BlockPos wasDoor = Places.onShip(Places.oldDoorSouth(floor), ship);
		set(level, wasDoor, Blocks.QUARTZ_BLOCK);
		set(level, wasDoor.above(), Blocks.QUARTZ_BLOCK);

		for (BlockPos plate : Places.liftPlates(floor)) {
			BlockPos at = Places.onShip(plate, ship);
			set(level, at.below(), Blocks.POLISHED_ANDESITE);
			set(level, at, Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE);
		}
	}

	/** A table for two: a post, a top, and a chair pulled up either side. */
	private static void table(ServerLevel level, BlockPos pos) {
		for (int dx = 0; dx <= 1; dx++) {
			set(level, pos.offset(dx, 0, 0), Blocks.SPRUCE_FENCE);
			set(level, pos.offset(dx, 1, 0), Blocks.SPRUCE_PRESSURE_PLATE);
			set(level, pos.offset(dx, 0, -1), Blocks.SPRUCE_STAIRS);
			set(level, pos.offset(dx, 0, 1), Blocks.SPRUCE_STAIRS);
		}
	}

	/**
	 * Floor 6: a loop of rail, with powered rail keeping it turning.
	 *
	 * Every piece is laid with the shape it needs spelled out -- straights
	 * along the straights, the right curve in each corner -- and with block
	 * updates off. Left to work it out, a rail decides its shape from the
	 * neighbours it can see at the moment it is placed, and the first one down
	 * cannot see any, so the loop came out as a row of disconnected sleepers.
	 *
	 * Powered rail carries its own POWERED as well, for the same reason: with
	 * updates off nothing recalculates it. The redstone block under each one
	 * is what keeps it true afterwards.
	 */
	private static void furnishRace(ServerLevel level) {
		int y = Places.floorY(6);
		int w = Places.KART_HALF_WIDTH;
		int d = Places.KART_HALF_DEPTH;
		int north = Places.SHIP_Z - d;
		int south = Places.SHIP_Z + d;
		int west = Places.SHIP_X - w;
		int east = Places.SHIP_X + w;

		// Tarmac inside the loop, so the track reads as a track.
		fill(level, west, y, north, east, y, south, Blocks.BLACK_CONCRETE);

		// The two straights, corners included.
		for (int x = west; x <= east; x++) {
			rail(level, new BlockPos(x, Places.KART_Y, north), RailShape.EAST_WEST, x % 5 == 0);
			rail(level, new BlockPos(x, Places.KART_Y, south), RailShape.EAST_WEST, x % 5 == 0);
		}
		// The two sides.
		for (int z = north + 1; z <= south - 1; z++) {
			rail(level, new BlockPos(west, Places.KART_Y, z), RailShape.NORTH_SOUTH, z % 5 == 0);
			rail(level, new BlockPos(east, Places.KART_Y, z), RailShape.NORTH_SOUTH, z % 5 == 0);
		}
		// And the four corners, which is what actually joins it up. North is
		// -z, so the near-left corner turns east and south.
		rail(level, new BlockPos(west, Places.KART_Y, north), RailShape.SOUTH_EAST, false);
		rail(level, new BlockPos(east, Places.KART_Y, north), RailShape.SOUTH_WEST, false);
		rail(level, new BlockPos(west, Places.KART_Y, south), RailShape.NORTH_EAST, false);
		rail(level, new BlockPos(east, Places.KART_Y, south), RailShape.NORTH_WEST, false);

		// The line, and the pits behind it.
		set(level, new BlockPos(Places.SHIP_X, y, north - 1), Blocks.WHITE_CONCRETE);
		set(level, Places.RACE_CAR, Blocks.RED_CONCRETE);
		set(level, Places.RACE_CAR.above(), Blocks.BLACK_CONCRETE);
		set(level, Places.RACE_CAR.above(2), Blocks.SEA_LANTERN);
	}

	/** One piece of track, laid with its shape said out loud. */
	private static void rail(ServerLevel level, BlockPos pos, RailShape shape, boolean powered) {
		// Only a straight can be powered; a corner asked to be is a broken loop.
		boolean straight = shape == RailShape.EAST_WEST || shape == RailShape.NORTH_SOUTH;
		if (powered && straight) {
			set(level, pos.below(), Blocks.REDSTONE_BLOCK);
			level.setBlock(pos, Blocks.POWERED_RAIL.defaultBlockState()
					.setValue(PoweredRailBlock.SHAPE, shape)
					.setValue(PoweredRailBlock.POWERED, true), 2);
			return;
		}
		level.setBlock(pos, Blocks.RAIL.defaultBlockState()
				.setValue(RailBlock.SHAPE, shape), 2);
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

	/** Floor 15: Ben's, and it looks lived in rather than fitted out. */
	/** Floor 16: Izzy's, laid out like Ben's but hers. */
	private static void furnishIzzysRoom(ServerLevel level) {
		int y = Places.floorY(16);
		set(level, Places.IZZY, Blocks.BIRCH_DOOR);
		door(level, Places.IZZY);
		set(level, Places.IZZY.above(2), Blocks.SEA_LANTERN);
		fill(level, Places.SHIP_X - 6, y, Places.SHIP_Z - 6,
				Places.SHIP_X + 6, y, Places.SHIP_Z + 6, Blocks.BIRCH_PLANKS);
		bed(level, new BlockPos(Places.SHIP_X + 4, y + 1, Places.SHIP_Z + 4));
		set(level, new BlockPos(Places.SHIP_X - 4, y + 1, Places.SHIP_Z + 4), Blocks.BOOKSHELF);
		set(level, new BlockPos(Places.SHIP_X - 4, y + 2, Places.SHIP_Z + 4),
				Blocks.POTTED_FERN);
		set(level, new BlockPos(Places.SHIP_X - 4, y + 1, Places.SHIP_Z - 4), Blocks.JUKEBOX);
	}

	private static void furnishBensRoom(ServerLevel level) {
		int y = Places.floorY(15);
		set(level, Places.BEN, Blocks.OAK_DOOR);
		door(level, Places.BEN);
		set(level, Places.BEN.above(2), Blocks.SEA_LANTERN);
		fill(level, Places.SHIP_X - 6, y, Places.SHIP_Z - 6,
				Places.SHIP_X + 6, y, Places.SHIP_Z + 6, Blocks.OAK_PLANKS);
		bed(level, new BlockPos(Places.SHIP_X - 4, y + 1, Places.SHIP_Z + 4));
		set(level, new BlockPos(Places.SHIP_X + 4, y + 1, Places.SHIP_Z + 4), Blocks.BOOKSHELF);
		set(level, new BlockPos(Places.SHIP_X + 4, y + 2, Places.SHIP_Z + 4), Blocks.FLOWER_POT);
	}

	/** Floor 7 is mostly an empty hall with a board on the wall. */
	private static void furnishEvents(ServerLevel level) {
		set(level, Places.EVENT_BOARD, Blocks.CHISELED_BOOKSHELF);
		set(level, Places.EVENT_BOARD.above(), Blocks.SEA_LANTERN);
		fill(level, Places.SHIP_X - 4, Places.floorY(7), Places.SHIP_Z - 4,
				Places.SHIP_X + 4, Places.floorY(7), Places.SHIP_Z + 4,
				Blocks.POLISHED_BLACKSTONE);
	}

	/**
	 * The toilet, built like the chairs are.
	 *
	 * The cauldron is the bowl and it sits where it always did. Around it is
	 * a square of white brick -- the floor of the bathroom, one block wide all
	 * the way round -- and behind it the cistern, three blocks high, so it
	 * reads as a toilet from across the room rather than as a pot on the
	 * floor.
	 */
	private static void toilet(ServerLevel level, BlockPos where) {
		int x = where.getX();
		int y = where.getY();
		int z = where.getZ();

		// The square of white brick around it, laid into the floor.
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx == 0 && dz == 0) {
					continue;                       // the bowl stands here
				}
				set(level, new BlockPos(x + dx, y, z + dz), Blocks.QUARTZ_BRICKS);
			}
		}

		// The back: the three blocks along the north edge of the square, four
		// blocks high. That is one side round from where the back was, and it
		// is the side you are looking at from the rest of the room.
		fill(level, x - 1, y, z - 1, x + 1, y + 3, z - 1, Blocks.QUARTZ_BRICKS);

		// The old back, west of the bowl, goes back to being floor.
		fill(level, x - 1, y + 1, z, x - 1, y + 3, z, Blocks.AIR);

		// The stall: three walls of white brick three high, and a door on the
		// side you walk in from.
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (Math.abs(dx) != 2 && Math.abs(dz) != 2) {
					continue;
				}
				boolean doorway = dz == 2 && dx == 0;
				for (int dy = 0; dy <= 2; dy++) {
					BlockPos pos = new BlockPos(x + dx, y + dy, z + dz);
					set(level, pos, doorway ? Blocks.AIR : Blocks.QUARTZ_BRICKS);
				}
			}
		}
		door(level, new BlockPos(x, y, z + 2), Direction.NORTH);

		set(level, where, Blocks.CAULDRON);

		// Where the handle used to be: the middle of the cistern, and then the
		// front of its top left.
		set(level, where.above(2), Blocks.AIR);
		set(level, where.west().above(3), Blocks.AIR);

		// The handle, on the side of the cistern.
		level.setBlockAndUpdate(Places.FLUSH, Blocks.LEVER.defaultBlockState()
				.setValue(net.minecraft.world.level.block.LeverBlock.FACE,
						net.minecraft.world.level.block.state.properties.AttachFace.WALL)
				.setValue(net.minecraft.world.level.block.LeverBlock.FACING,
						Direction.WEST));
	}

	private static void furnishYourRoom(ServerLevel level) {
		toilet(level, Places.TOILET);
		set(level, Places.FRIDGE, Blocks.IRON_BLOCK);
		set(level, Places.FRIDGE.above(), Blocks.IRON_BLOCK);
		set(level, Places.FRIDGE.above(2), Blocks.IRON_BLOCK);
		// A screen on the wall: three across and two high, with a frame, so it
		// looks like a television from the other side of the room rather than
		// like two dark blocks.
		fill(level, Places.TV.getX() - 1, Places.TV.getY(), Places.TV.getZ() - 1,
				Places.TV.getX() + 1, Places.TV.getY() + 2, Places.TV.getZ() - 1,
				Blocks.POLISHED_BLACKSTONE);
		fill(level, Places.TV.getX() - 1, Places.TV.getY() + 1, Places.TV.getZ(),
				Places.TV.getX() + 1, Places.TV.getY() + 2, Places.TV.getZ(),
				Blocks.BLACK_CONCRETE);
		set(level, Places.TV, Blocks.BLACK_CONCRETE);
		set(level, Places.TV.above(), Blocks.BLACK_CONCRETE);

		// A record player next to it. This is Minecraft's own jukebox, so a
		// disc goes in it and the disc plays -- nothing here had to make that
		// happen, only put one in the room.
		set(level, Places.JUKEBOX, Blocks.JUKEBOX);
		set(level, Places.PHONE, Blocks.OAK_PRESSURE_PLATE);
		bed(level, Places.BED);

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
	/**
	 * Bring a world built by an older Ship Life up to this one.
	 *
	 * This runs on every join in every world, creative included, and it only
	 * ever adds: a floor whose shell was never laid gets laid, a room whose
	 * fittings are not there gets them, ship 2 comes down, and the people are
	 * put back. Nothing that is already there is touched, so a wall you moved
	 * in creative stays moved and a world from before floor 16 existed gets
	 * floor 16 rather than needing to be started again.
	 */
	public static void catchUp(ServerLevel level) {
		// Any floor that was never built at all. The centre of a floor's own
		// floor is concrete in every room there is, so air there means the
		// storey does not exist yet.
		for (int floor = 1; floor <= Places.TOP_FLOOR; floor++) {
			BlockPos middle = new BlockPos(Places.SHIP_X, Places.floorY(floor), Places.SHIP_Z);
			if (level.getBlockState(middle).isAir()) {
				buildFloor(level, floor);
				ShipLifeMod.LOGGER.info("Built floor {} into an older world.", floor);
			}
		}

		// Rooms whose fittings came after the world did. Each one is asked
		// for by a block that only that room has.
		// The giant chairs came after most worlds did.
		if (!level.getBlockState(Places.CHAIR.below()).is(Blocks.BLACK_WOOL)) {
			furnishLobby(level);
		}
		// The toilet was a cauldron on its own, then it had a low back, and
		// then it had no handle.
		if (!level.getBlockState(Places.TOILET.north().above(3)).is(Blocks.QUARTZ_BRICKS)
				|| !level.getBlockState(Places.FLUSH).is(Blocks.LEVER)
				|| !level.getBlockState(Places.TOILET.east(2)).is(Blocks.QUARTZ_BRICKS)) {
			toilet(level, Places.TOILET);
		}

		// The television used to stand in the lift's corner. Anything left of
		// it there comes out.
		for (int dx = -7; dx <= -3; dx++) {
			for (int dy = 0; dy <= 2; dy++) {
				for (int dz = -9; dz <= -8; dz++) {
					BlockPos pos = new BlockPos(Places.SHIP_X + dx, Places.floorY(5) + 1 + dy,
							Places.SHIP_Z + dz);
					if (level.getBlockState(pos).is(Blocks.BLACK_CONCRETE)
							|| level.getBlockState(pos).is(Blocks.POLISHED_BLACKSTONE)
							|| level.getBlockState(pos).is(Blocks.JUKEBOX)) {
						set(level, pos, Blocks.AIR);
					}
				}
			}
		}

		// The TV, its screen, and the record player that came after it.
		if (!level.getBlockState(Places.TV).is(Blocks.BLACK_CONCRETE)
				|| !level.getBlockState(Places.TV.above(2)).is(Blocks.BLACK_CONCRETE)
				|| !level.getBlockState(Places.JUKEBOX).is(Blocks.JUKEBOX)) {
			furnishYourRoom(level);
		}

		// The doors to nowhere, in the wall where the lift used to be. Floor
		// 1 keeps its one; the rest opened onto the hull.
		for (int floor = 2; floor <= Places.TOP_FLOOR; floor++) {
			BlockPos was = Places.oldLift(floor);
			if (level.getBlockState(was).getBlock()
					instanceof net.minecraft.world.level.block.DoorBlock) {
				set(level, was, Blocks.BLACK_CONCRETE);
				set(level, was.above(), Blocks.BLACK_CONCRETE);
			}
		}

		// The white wall that used to stand beside the toilet.
		for (int dy = 1; dy <= 4; dy++) {
			for (int dz = -12; dz <= -5; dz++) {
				BlockPos pos = new BlockPos(Places.SHIP_X + 5, Places.floorY(5) + dy,
						Places.SHIP_Z + dz);
				if (level.getBlockState(pos).is(Blocks.WHITE_CONCRETE)) {
					set(level, pos, Blocks.AIR);
				}
			}
		}
		if (!level.getBlockState(Places.BEN).is(Blocks.OAK_DOOR)) {
			furnishBensRoom(level);
		}
		if (!level.getBlockState(Places.IZZY).is(Blocks.BIRCH_DOOR)) {
			furnishIzzysRoom(level);
		}
		if (!level.getBlockState(Places.FIGHT_BUTTON).is(Blocks.REDSTONE_BLOCK)) {
			furnishFighting(level);
		}
		if (!level.getBlockState(Places.PASSPORT_DESK).is(Blocks.LECTERN)) {
			furnishShops(level);
		}
		// A lift car on every floor, including any floor this world has only
		// just been given.
		if (!level.getBlockState(Places.panel(Places.TOP_FLOOR)).is(Made.elevatorButton)) {
			for (int floor = 1; floor <= Places.TOP_FLOOR; floor++) {
				liftCar(level, floor, 1);
			}
		}

		// The hull was one block thick before it was two.
		if (!level.getBlockState(new BlockPos(Places.SHIP_X + Places.ROOM + 1,
				Places.floorY(1) + 3, Places.SHIP_Z)).is(Blocks.BLACK_CONCRETE)) {
			for (int floor = 1; floor <= Places.TOP_FLOOR; floor++) {
				buildFloor(level, floor);
			}
			ShipLifeMod.LOGGER.info("Thickened the hull to two blocks.");
		}

		// The lift car had glass sides, one-block walls, and its button on
		// another wall.
		if (level.getBlockState(new BlockPos(Places.LIFT_X + Places.LIFT_SIZE - 1,
				Places.floorY(1) + 2, Places.LIFT_Z + 1)).is(Blocks.GLASS)
				|| !level.getBlockState(Places.panel(1)).is(Made.elevatorButton)
				|| !level.getBlockState(new BlockPos(Places.LIFT_X + Places.LIFT_SIZE,
						Places.floorY(1) + 4, Places.LIFT_Z + 2)).is(Blocks.QUARTZ_BLOCK)) {
			for (int floor = 1; floor <= Places.TOP_FLOOR; floor++) {
				liftCar(level, floor, 1);
			}
		}

		// The windows went metal for a while. They are glass again, and the
		// blue marker block in each floor is gone.
		for (int floor = 1; floor <= Places.TOP_FLOOR; floor++) {
			int y = Places.floorY(floor);
			set(level, new BlockPos(Places.SHIP_X - Places.ROOM + 2, y, Places.SHIP_Z),
					Blocks.BLACK_CONCRETE);
			for (int dx = -Places.ROOM - 1; dx <= Places.ROOM + 1; dx++) {
				for (int dz = -Places.ROOM - 1; dz <= Places.ROOM + 1; dz++) {
					if (Math.abs(dx) < Places.ROOM && Math.abs(dz) < Places.ROOM) {
						continue;
					}
					boolean window = Math.abs(dx) >= Places.ROOM && Math.abs(dz) % 4 == 0;
					for (int dy = 3; dy <= 4; dy++) {
						BlockPos pos = new BlockPos(Places.SHIP_X + dx, y + dy,
								Places.SHIP_Z + dz);
						if (window && level.getBlockState(pos).is(Blocks.IRON_BLOCK)) {
							set(level, pos, Blocks.GLASS);
						}
					}
				}
			}
			// And the lift's doors were wood.
			if (level.getBlockState(Places.liftDoorEast(floor)).is(Blocks.OAK_DOOR)) {
				ironDoor(level, Places.liftDoorEast(floor), Direction.EAST);
			}
		}

		// Ship 2 came out of the lift, so it comes out of the world.
		clearSecond(level);
	}

	public static void repair(ServerLevel level) {
		// The hull was one block thick before it was two.
		if (!level.getBlockState(new BlockPos(Places.SHIP_X + Places.ROOM + 1,
				Places.floorY(1) + 3, Places.SHIP_Z)).is(Blocks.BLACK_CONCRETE)) {
			for (int floor = 1; floor <= Places.TOP_FLOOR; floor++) {
				buildFloor(level, floor);
			}
			ShipLifeMod.LOGGER.info("Thickened the hull to two blocks.");
		}

		// The lift car had glass sides, one-block walls, and its button on
		// another wall.
		if (level.getBlockState(new BlockPos(Places.LIFT_X + Places.LIFT_SIZE - 1,
				Places.floorY(1) + 2, Places.LIFT_Z + 1)).is(Blocks.GLASS)
				|| !level.getBlockState(Places.panel(1)).is(Made.elevatorButton)
				|| !level.getBlockState(new BlockPos(Places.LIFT_X + Places.LIFT_SIZE,
						Places.floorY(1) + 4, Places.LIFT_Z + 2)).is(Blocks.QUARTZ_BLOCK)) {
			for (int floor = 1; floor <= Places.TOP_FLOOR; floor++) {
				liftCar(level, floor, 1);
			}
		}

		// The windows went metal for a while. They are glass again, and the
		// blue marker block in each floor is gone.
		for (int floor = 1; floor <= Places.TOP_FLOOR; floor++) {
			int y = Places.floorY(floor);
			set(level, new BlockPos(Places.SHIP_X - Places.ROOM + 2, y, Places.SHIP_Z),
					Blocks.BLACK_CONCRETE);
			for (int dx = -Places.ROOM - 1; dx <= Places.ROOM + 1; dx++) {
				for (int dz = -Places.ROOM - 1; dz <= Places.ROOM + 1; dz++) {
					if (Math.abs(dx) < Places.ROOM && Math.abs(dz) < Places.ROOM) {
						continue;
					}
					boolean window = Math.abs(dx) >= Places.ROOM && Math.abs(dz) % 4 == 0;
					for (int dy = 3; dy <= 4; dy++) {
						BlockPos pos = new BlockPos(Places.SHIP_X + dx, y + dy,
								Places.SHIP_Z + dz);
						if (window && level.getBlockState(pos).is(Blocks.IRON_BLOCK)) {
							set(level, pos, Blocks.GLASS);
						}
					}
				}
			}
			// And the lift's doors were wood.
			if (level.getBlockState(Places.liftDoorEast(floor)).is(Blocks.OAK_DOOR)) {
				ironDoor(level, Places.liftDoorEast(floor), Direction.EAST);
			}
		}

		// Ship 2 came out of the lift, so it comes out of the world.
		clearSecond(level);

		// The walls went black. Rebuilding a floor only lays the shell -- the
		// furniture sits a block above it -- but the fittings are put back
		// afterwards anyway, so nothing is lost either way.
		BlockPos wall = new BlockPos(Places.SHIP_X, Places.floorY(1) + 3,
				Places.SHIP_Z - Places.ROOM);
		BlockPos deck = new BlockPos(Places.SHIP_X + 6, Places.floorY(1), Places.SHIP_Z + 6);
		if ((!level.getBlockState(wall).is(Blocks.BLACK_CONCRETE)
				&& !level.getBlockState(wall).is(Blocks.GLASS)
				&& !level.getBlockState(wall).is(Blocks.IRON_BLOCK))
				|| !level.getBlockState(deck).is(Blocks.BLACK_CONCRETE)) {
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
		furnishBensRoom(level);
		furnishIzzysRoom(level);
			furnishYourRoom(level);
			ShipLifeMod.LOGGER.info("Repainted the ship's walls black.");
		}
		// The lift moved to the near left corner, and left a door behind.
		if (!level.getBlockState(Places.panel(1)).is(Made.elevatorButton)) {
			for (int floor = 1; floor <= Places.TOP_FLOOR; floor++) {
				for (int ship = 1; ship <= 2; ship++) {
					BlockPos was = Places.onShip(Places.oldLift(floor), ship);
					if (!level.getBlockState(was.offset(2, 0, 0)).isAir()
							&& ship == 2) {
						continue;             // ship 2 was never built here
					}
					set(level, Places.onShip(Places.panel(floor), ship), Made.elevatorButton);
					set(level, Places.onShip(Places.panel(floor), ship).above(),
							Blocks.IRON_BLOCK);
					set(level, Places.onShip(Places.lift(floor), ship), Blocks.AIR);
					set(level, Places.onShip(Places.lift(floor), ship).above(), Blocks.AIR);
					door(level, was);
				}
			}
			ShipLifeMod.LOGGER.info("Moved the lifts to the near corner.");
		}
		// The arcade came after the first worlds were built.
		if (!level.getBlockState(Places.PACMAN.above(3)).is(Blocks.SEA_LANTERN)
				|| !level.getBlockState(Places.PACMAN).is(Blocks.POLISHED_BLACKSTONE)) {
			furnishArcade(level);
		furnishPool(level);
		furnishBuffet(level);
		furnishRace(level);
		furnishFighting(level);
		furnishEvents(level);
		furnishShops(level);
		furnishBensRoom(level);
		furnishIzzysRoom(level);
			ShipLifeMod.LOGGER.info("Put the arcade into a world built before it.");
		}
		// The pool used to be a glass tank standing on the floor.
		if (!level.getBlockState(new BlockPos(Places.SHIP_X, Places.floorY(3), Places.SHIP_Z))
				.is(Blocks.WATER)) {
			furnishPool(level);
			ShipLifeMod.LOGGER.info("Sank the pool into the floor of floor 3.");
		}
		// The lift was a button on a wall before it was a car you walk into.
		if (!level.getBlockState(Places.liftDoorEast(1)).is(Blocks.OAK_DOOR)) {
			for (int floor = 1; floor <= Places.TOP_FLOOR; floor++) {
				liftCar(level, floor, 1);
				set(level, Places.oldPanel(floor), Blocks.AIR);
				set(level, Places.oldPanel(floor).above(), Blocks.AIR);
			}
			ShipLifeMod.LOGGER.info("Built a lift car on all {} floors.", Places.TOP_FLOOR);
		}
		// Floor 6 was a strip of concrete before the karts had a loop to run.
		// A corner that is not a curve means the loop was laid before the
		// shapes were spelled out, and is a ring of disconnected sleepers.
		BlockPos corner = new BlockPos(Places.SHIP_X - Places.KART_HALF_WIDTH,
				Places.KART_Y, Places.SHIP_Z - Places.KART_HALF_DEPTH);
		if (!level.getBlockState(corner).is(Blocks.RAIL)
				|| level.getBlockState(corner).getValue(RailBlock.SHAPE) != RailShape.SOUTH_EAST) {
			furnishRace(level);
			ShipLifeMod.LOGGER.info("Laid the kart track round floor 6.");
		}
		// Floor 4 was an empty room until the buffet was built into it.
		if (!level.getBlockState(Places.BUFFET_COOK).is(Blocks.SMOKER)) {
			furnishBuffet(level);
			ShipLifeMod.LOGGER.info("Laid the buffet out on floor 4.");
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

	/**
	 * A door, which like a bed is two blocks that have to agree.
	 *
	 * The square in front of it is cleared as well, so the doorway is a
	 * doorway rather than a door with a wall behind it.
	 */
	private static void door(ServerLevel level, BlockPos bottom) {
		door(level, bottom, Direction.EAST);
	}

	/**
	 * A lift door: iron, and nothing you can pull open by hand.
	 *
	 * Which is the point of it. The plate on the floor works these; see
	 * {@link Elevator}.
	 */
	private static void ironDoor(ServerLevel level, BlockPos bottom, Direction facing) {
		BlockState base = Blocks.IRON_DOOR.defaultBlockState()
				.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
						facing);
		level.setBlockAndUpdate(bottom, base.setValue(
				net.minecraft.world.level.block.DoorBlock.HALF,
				net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
		level.setBlockAndUpdate(bottom.above(), base.setValue(
				net.minecraft.world.level.block.DoorBlock.HALF,
				net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
	}

	private static void door(ServerLevel level, BlockPos bottom, Direction facing) {
		BlockState base = Blocks.OAK_DOOR.defaultBlockState()
				.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
						facing);
		level.setBlockAndUpdate(bottom, base.setValue(
				net.minecraft.world.level.block.DoorBlock.HALF,
				net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
		level.setBlockAndUpdate(bottom.above(), base.setValue(
				net.minecraft.world.level.block.DoorBlock.HALF,
				net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
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
