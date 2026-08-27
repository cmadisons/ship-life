package com.example;

import net.minecraft.core.BlockPos;

/**
 * Where everything is.
 *
 * The whole world is built by this mod at fixed coordinates, which is what
 * lets the rest of it stay simple: a quest doesn't have to search for the
 * kitchen sink, it just knows where the sink is. Interactions are matched by
 * position too, so what a dish is actually made of is only a matter of looks.
 *
 * The town sits at the origin, the three houses in a row, and the ship stands
 * off to the east with a walkway out to it. Floors are stacked eight blocks
 * apart, so floor 1 is at y 64 and floor 14 is at y 168.
 */
public final class Places {
	private Places() {
	}

	/** The ground the town is built on. */
	public static final int GROUND = 64;

	/** House one: the kitchen with the dishes. */
	public static final BlockPos HOUSE_ONE = new BlockPos(-26, GROUND, 0);

	/** House two: the lawn and the weeds. */
	public static final BlockPos HOUSE_TWO = new BlockPos(0, GROUND, 0);

	/** House three: the five bushes. */
	public static final BlockPos HOUSE_THREE = new BlockPos(26, GROUND, 0);

	/** Where you spawn, out front of house one. */
	public static final BlockPos SPAWN = new BlockPos(-26, GROUND + 1, 8);

	/** The ten dishes, in a row along the counter in house one. */
	public static BlockPos dish(int index) {
		return new BlockPos(HOUSE_ONE.getX() - 4 + index, GROUND + 1, HOUSE_ONE.getZ() - 3);
	}

	/** The garbage the dirty sponge and towel go in. */
	public static final BlockPos GARBAGE =
			new BlockPos(HOUSE_ONE.getX() + 5, GROUND + 1, HOUSE_ONE.getZ() - 3);

	/** The ten weeds dotted around the lawn of house two. */
	public static BlockPos weed(int index) {
		int x = HOUSE_TWO.getX() - 4 + (index % 5) * 2;
		int z = HOUSE_TWO.getZ() - 2 + (index / 5) * 4;
		return new BlockPos(x, GROUND + 1, z);
	}

	/** The lawn is the whole floor of house two, right out to the walls. */
	public static final int LAWN_REACH = 5;

	/** How many squares of grass there are to cut. */
	public static final int LAWN_SQUARES = (LAWN_REACH * 2 + 1) * (LAWN_REACH * 2 + 1);

	/** Is this block a patch of that lawn? */
	public static boolean onLawn(BlockPos pos) {
		return pos.getY() == GROUND
				&& Math.abs(pos.getX() - HOUSE_TWO.getX()) <= LAWN_REACH
				&& Math.abs(pos.getZ() - HOUSE_TWO.getZ()) <= LAWN_REACH;
	}

	/** The five bushes at house three, left to right. */
	public static BlockPos bush(int index) {
		return new BlockPos(HOUSE_THREE.getX() - 4 + index * 2, GROUND + 1, HOUSE_THREE.getZ() - 4);
	}

	/** The penny is in the second bush from the left. Always. */
	public static final int PENNY_BUSH = 1;

	// ------------------------------------------------------------------- ship

	/** The middle of every floor of the ship. */
	public static final int SHIP_X = 120;
	public static final int SHIP_Z = 0;

	/** How far a floor reaches from the middle. */
	public static final int ROOM = 12;

	/** How tall one floor is, floor to floor. */
	public static final int FLOOR_HEIGHT = 8;

	/** The top floor the ship has. */
	public static final int TOP_FLOOR = 14;

	/** The y of a floor's own floor. */
	public static int floorY(int floor) {
		return GROUND + (floor - 1) * FLOOR_HEIGHT;
	}

	/** Which floor a y belongs to, or 0 if it is not in the ship at all. */
	public static int floorAt(double y) {
		int floor = (int) Math.floor((y - GROUND) / FLOOR_HEIGHT) + 1;
		return floor >= 1 && floor <= TOP_FLOOR ? floor : 0;
	}

	/** The panel you press to call the lift, on each floor. */
	public static BlockPos panel(int floor) {
		return new BlockPos(SHIP_X - ROOM + 1, floorY(floor) + 1, SHIP_Z);
	}

	/** Where the lift puts you down on a floor. */
	public static BlockPos lift(int floor) {
		return new BlockPos(SHIP_X - ROOM + 3, floorY(floor) + 1, SHIP_Z);
	}

	/** The way in, at the bottom of the gangway. */
	public static final BlockPos DOOR = new BlockPos(SHIP_X - ROOM - 1, GROUND + 1, SHIP_Z);

	/** The security desk in the lobby. */
	public static final BlockPos DESK = new BlockPos(SHIP_X + 4, GROUND + 1, SHIP_Z - 3);

	/** Charlie's table in the lobby. */
	public static final BlockPos TABLE = new BlockPos(SHIP_X + 4, GROUND + 1, SHIP_Z + 3);

	// ----------------------------------------------------- the arcade, floor 2

	private static final int ARCADE_Y = floorY(2) + 1;

	public static final BlockPos SNAKE = new BlockPos(SHIP_X - 5, ARCADE_Y, SHIP_Z - 10);
	public static final BlockPos PACMAN = new BlockPos(SHIP_X, ARCADE_Y, SHIP_Z - 10);
	public static final BlockPos GALAGA = new BlockPos(SHIP_X + 5, ARCADE_Y, SHIP_Z - 10);

	/** The prize counter, facing the cabinets. */
	public static final BlockPos PRIZES = new BlockPos(SHIP_X, ARCADE_Y, SHIP_Z + 8);

	// ------------------------------------------------------- the pool, floor 3

	/** The near end of the pool, where a lap starts and finishes. */
	public static final int POOL_START = SHIP_X - 9;

	/** The far end, where you turn. */
	public static final int POOL_END = SHIP_X + 9;

	/** How wide the pool is either side of the middle. */
	public static final int POOL_HALF_WIDTH = 3;

	/** The record board on the wall of floor 3. */
	public static final BlockPos POOL_BOARD =
			new BlockPos(SHIP_X - 11, floorY(3) + 1, SHIP_Z + 8);

	/** Where you get in the car, on floor 6. */
	public static final BlockPos RACE_CAR =
			new BlockPos(SHIP_X, floorY(6) + 1, SHIP_Z - 8);

	/** The button on floor 9 that calls in a wave. */
	public static final BlockPos FIGHT_BUTTON =
			new BlockPos(SHIP_X - 10, floorY(9) + 1, SHIP_Z);

	/** The two doors on floor 10, one boss behind each. */
	public static final BlockPos ARACHNES_DOOR =
			new BlockPos(SHIP_X - 6, floorY(10) + 1, SHIP_Z - 10);
	public static final BlockPos DRAGON_DOOR =
			new BlockPos(SHIP_X + 6, floorY(10) + 1, SHIP_Z - 10);

	/** Where a boss comes in. */
	public static final BlockPos BOSS_SPOT =
			new BlockPos(SHIP_X, floorY(10) + 1, SHIP_Z + 4);

	/** The board on floor 7 that says what is on. */
	public static final BlockPos EVENT_BOARD =
			new BlockPos(SHIP_X, floorY(7) + 1, SHIP_Z - 10);

	// ------------------------------------------------------- your room, floor 5

	private static final int ROOM_Y = floorY(5) + 1;

	public static final BlockPos TOILET = new BlockPos(SHIP_X + 8, ROOM_Y, SHIP_Z - 8);
	public static final BlockPos BED = new BlockPos(SHIP_X - 6, ROOM_Y, SHIP_Z + 6);
	public static final BlockPos FRIDGE = new BlockPos(SHIP_X + 8, ROOM_Y, SHIP_Z + 6);
	public static final BlockPos TV = new BlockPos(SHIP_X - 6, ROOM_Y, SHIP_Z - 8);
	public static final BlockPos PHONE = new BlockPos(SHIP_X - 4, ROOM_Y, SHIP_Z + 6);

	/** The four things Quest 3 asks you to find, in the order it counts them. */
	public static final BlockPos[] ROOM_THINGS = { TOILET, FRIDGE, BED, TV };
}
