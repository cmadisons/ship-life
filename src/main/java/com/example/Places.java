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
	public static final int TOP_FLOOR = 16;

	/** The y of a floor's own floor. */
	public static int floorY(int floor) {
		return GROUND + (floor - 1) * FLOOR_HEIGHT;
	}

	/** Which floor a y belongs to, or 0 if it is not in the ship at all. */
	public static int floorAt(double y) {
		int floor = (int) Math.floor((y - GROUND) / FLOOR_HEIGHT) + 1;
		return floor >= 1 && floor <= TOP_FLOOR ? floor : 0;
	}

	/**
	 * How far east the second ship stands.
	 *
	 * Ship 2 is the same ship again, floor for floor, so rather than a second
	 * set of coordinates for everything it is built at an offset and anything
	 * that happens over there is shifted back here before it is read. One
	 * arcade, one pool, one set of positions -- see {@link #local}.
	 */
	public static final int SHIP_TWO_OFFSET = 200;

	/** Which ship a position is on, 1 or 2. */
	public static int shipOf(double x) {
		return x > SHIP_X + SHIP_TWO_OFFSET - ROOM - 4 ? 2 : 1;
	}

	/** The same spot, as ship 1 would number it. */
	public static BlockPos local(BlockPos pos) {
		return shipOf(pos.getX()) == 2 ? pos.offset(-SHIP_TWO_OFFSET, 0, 0) : pos;
	}

	/** The same x, as ship 1 would number it. */
	public static double localX(double x) {
		return shipOf(x) == 2 ? x - SHIP_TWO_OFFSET : x;
	}

	/** A ship-1 spot, moved to whichever ship you mean. */
	public static BlockPos onShip(BlockPos pos, int ship) {
		return ship == 2 ? pos.offset(SHIP_TWO_OFFSET, 0, 0) : pos;
	}

	/** Ben's door on floor 15. He is the first friend you make. */
	public static final BlockPos BEN = new BlockPos(SHIP_X, floorY(15) + 1, SHIP_Z - 8);

	/** Izzy's door, on floor 16. */
	public static final BlockPos IZZY = new BlockPos(SHIP_X, floorY(16) + 1, SHIP_Z - 8);

	/** The desk on floor 14 that upgrades your passport. */
	public static final BlockPos PASSPORT_DESK =
			new BlockPos(SHIP_X, floorY(14) + 1, SHIP_Z - 8);

	/**
	 * The panel you press to call the lift, on each floor.
	 *
	 * It sits in the near left corner -- the corner closest to the town, so
	 * you meet it on the way in rather than crossing the room to find it.
	 */
	public static BlockPos panel(int floor) {
		return new BlockPos(LIFT_X + 3, floorY(floor) + 1, LIFT_Z + 4);
	}

	/** Where the lift puts you down: the middle of the car. */
	public static BlockPos lift(int floor) {
		return new BlockPos(LIFT_X + 2, floorY(floor) + 1, LIFT_Z + 2);
	}

	// ------------------------------------------------------------ the lift car

	/** The near left corner of the car, which is the corner of the room. */
	public static final int LIFT_X = SHIP_X - ROOM + 1;
	public static final int LIFT_Z = SHIP_Z - ROOM + 1;

	/** The car is five by five by five, walls included. */
	public static final int LIFT_SIZE = 5;

	/** The two ways out: one facing along the room, one facing across it. */
	public static BlockPos liftDoorEast(int floor) {
		return new BlockPos(LIFT_X + LIFT_SIZE - 1, floorY(floor) + 1, LIFT_Z + 2);
	}

	public static BlockPos liftDoorSouth(int floor) {
		return new BlockPos(LIFT_X + 2, floorY(floor) + 1, LIFT_Z + LIFT_SIZE - 1);
	}

	/** The plate each side of each door: step on one and the lift opens. */
	public static BlockPos[] liftPlates(int floor) {
		int y = floorY(floor) + 1;
		return new BlockPos[] {
			new BlockPos(LIFT_X + LIFT_SIZE, y, LIFT_Z + 2),        // outside east
			new BlockPos(LIFT_X + LIFT_SIZE - 2, y, LIFT_Z + 2),    // inside east
			new BlockPos(LIFT_X + 2, y, LIFT_Z + LIFT_SIZE),        // outside south
			new BlockPos(LIFT_X + 2, y, LIFT_Z + LIFT_SIZE - 2),    // inside south
		};
	}

	/** Where the button used to be, before the car was built round it. */
	public static BlockPos oldPanel(int floor) {
		return new BlockPos(SHIP_X - ROOM + 1, floorY(floor) + 1, SHIP_Z - ROOM + 1);
	}

	/** Where the lift used to be, which is a door now. */
	public static BlockPos oldLift(int floor) {
		return new BlockPos(SHIP_X - ROOM + 1, floorY(floor) + 1, SHIP_Z);
	}

	/** The way in, at the bottom of the gangway. */
	public static final BlockPos DOOR = new BlockPos(SHIP_X - ROOM - 1, GROUND + 1, SHIP_Z);

	/** The security desk in the lobby. */
	public static final BlockPos DESK = new BlockPos(SHIP_X + 4, GROUND + 1, SHIP_Z - 3);

	/** Charlie's table in the lobby. */
	/**
	 * The two giant chairs in the lobby, and the table between them.
	 *
	 * Each is an X of black wool three blocks each way with a 3x3 seat on
	 * top, a back and two arms three high, and a ladder up the front.
	 * Charlie has the south one; the north one faces him and is yours, with
	 * one wool table between them.
	 */
	public static final BlockPos CHAIR = new BlockPos(SHIP_X + 4, GROUND + 4, SHIP_Z + 6);
	public static final BlockPos CHAIR_TWO = new BlockPos(SHIP_X + 4, GROUND + 4, SHIP_Z - 6);
	public static final BlockPos BIG_TABLE = new BlockPos(SHIP_X + 4, GROUND + 1, SHIP_Z);

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

	/** How deep the pool is cut into floor 3's deck. */
	public static final int POOL_DEPTH = 4;

	/**
	 * Are you in the pool?
	 *
	 * The water is sunk below the deck, so {@link #floorAt} reads the floor
	 * below once you are actually swimming in it. Anything watching the pool
	 * has to ask this instead.
	 */
	public static boolean inPool(double x, double y, double z) {
		double lx = localX(x);
		return y >= floorY(3) - POOL_DEPTH && y <= floorY(3) + 2
				&& lx >= POOL_START - 1 && lx <= POOL_END + 1
				&& z >= SHIP_Z - POOL_HALF_WIDTH - 1 && z <= SHIP_Z + POOL_HALF_WIDTH + 1;
	}

	/** The record board on the wall of floor 3. */
	public static final BlockPos POOL_BOARD =
			new BlockPos(SHIP_X - 11, floorY(3) + 1, SHIP_Z + 8);

	/** Where you get in the kart, in the pits on floor 6. */
	public static final BlockPos RACE_CAR =
			new BlockPos(SHIP_X, floorY(6) + 1, SHIP_Z - 9);

	/** How far the track reaches from the middle of floor 6. */
	public static final int KART_HALF_WIDTH = 9;
	public static final int KART_HALF_DEPTH = 6;

	/** The rails sit on top of the floor. */
	public static final int KART_Y = floorY(6) + 1;

	/** The start and finish line, in the middle of the near straight. */
	public static final BlockPos KART_LINE =
			new BlockPos(SHIP_X, KART_Y, SHIP_Z - KART_HALF_DEPTH);

	/** Where the other karts start from, spread along the near straight. */
	public static BlockPos kartGrid(int index) {
		return new BlockPos(SHIP_X - 6 + index * 3, KART_Y, SHIP_Z - KART_HALF_DEPTH);
	}

	// ----------------------------------------------------- the buffet, floor 4

	/** The cook, behind the counter on floor 4. */
	public static final BlockPos BUFFET_COOK =
			new BlockPos(SHIP_X, floorY(4) + 1, SHIP_Z - 9);

	/** How many tables the buffet is laid with. */
	public static final int TABLES = 4;

	/** A table, and the chair either side of it faces along z. */
	public static BlockPos table(int index) {
		int x = SHIP_X - 6 + (index % 2) * 12;
		int z = SHIP_Z + (index / 2) * 7 - 1;
		return new BlockPos(x, floorY(4) + 1, z);
	}

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

	/** The counter in the store on floor 8. */
	public static final BlockPos STORE = new BlockPos(SHIP_X, floorY(8) + 1, SHIP_Z - 8);

	/** The event ticket shop, next to the board on floor 7. */
	public static final BlockPos TICKET_SHOP =
			new BlockPos(SHIP_X + 4, floorY(7) + 1, SHIP_Z - 10);

	/** The desk on floor 11 that hands out the monthly reward. */
	public static final BlockPos REWARD_DESK =
			new BlockPos(SHIP_X, floorY(11) + 1, SHIP_Z - 8);

	/** The counter in the pet store on floor 12. */
	public static final BlockPos PET_STORE = new BlockPos(SHIP_X, floorY(12) + 1, SHIP_Z - 8);

	/** The bar at The Keg on floor 13. */
	public static final BlockPos KEG = new BlockPos(SHIP_X, floorY(13) + 1, SHIP_Z - 8);

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
