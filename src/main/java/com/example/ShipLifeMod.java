package com.example;

import java.util.HashSet;
import java.util.Set;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ship Life: you live on a giant space ship.
 *
 * Pick "Ship Life" on the Create World screen. It is a world type like any
 * other, which is why it sits next to survival, creative and hardcore -- but
 * whichever of those you picked alongside it, you play survival. The whole
 * game is earning your way onto the ship and then earning your way up it, and
 * neither means anything if you can fly and have everything already.
 *
 * The world itself is an empty void that this mod builds into: a town with
 * three houses where you make your first hundred dollars, and the ship out to
 * the east with all fourteen floors standing from the start. See {@link Ship}.
 *
 * What is here so far is chapters 1 to 4 -- the dishes, the lawn, the penny,
 * boarding, your passport, your room, Charlie and his quest -- along with the
 * Quest Book, the star, the clock and the elevator. The arcade, the pool, the
 * race track, the pets and the events are floors waiting to be built into.
 */
public class ShipLifeMod implements ModInitializer {
	public static final String MOD_ID = "shiplife";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Worlds we have decided are Ship Life worlds, so we only work it out once. */
	private static final Set<String> SHIP_WORLDS = new HashSet<>();

	@Override
	public void onInitialize() {
		// First, before a world can be loaded: everything the game remembers.
		State.register();
		Fridge.register();
		Telly.register();
		Person.register();
		Person.watch();
		Made.register();
		Ticker.register();
		Slots.register();
		Hud.register();
		Chores.register();
		Arcade.register();
		Game.register();
		ArcadePackets.register();
		Pets.register();
		Events.register();
		Pool.register();
		Fight.register();
		Shops.register();
		Friends.register();
		Gear.register();
		Star.register();
		Seats.register();
		Seats.tidy();
		Comforts.register();
		Gym.register();
		Fishing.register();
		Weapons.register();
		Portals.register();
		Buffet.register();
		Elevator.register();
		Kart.register();
		Maze.register();
		Skip.register();

		// Right-clicking the book in mid-air, rather than at a block.
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (player instanceof ServerPlayer who && world instanceof ServerLevel level
					&& isShipLife(level) && Kit.is(player.getItemInHand(hand), Kit.QUEST_BOOK)) {
				Book.open(who);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				onJoin(handler.getPlayer()));

		// The ship is not yours to knock down.
		//
		// Nothing hostile could break a block already -- mob griefing is off in
		// every Ship Life world -- and now nothing you do can either. Creative
		// is the exception, because creative is the mod's edit mode and the
		// whole point of it is changing the ship.
		//
		// Not even the dishes. Washing one is holding the sponge on it, and
		// knocking it off the counter was never the point of the chore.
		net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register(
				(world, player, pos, state, entity) -> {
					if (!(world instanceof ServerLevel level) || !isShipLife(level)) {
						return true;
					}
					if (player.isCreative()) {
						return true;
					}
					if (player instanceof ServerPlayer who) {
						Hud.busy(who, 20);
						who.sendOverlayMessage(Component.literal(
								"That is part of the ship.")
								.withStyle(ChatFormatting.GRAY));
					}
					return false;
				});

		// Dying puts you back on the ground rather than wherever the world
		// happened to think its spawn was.
		ServerPlayerEvents.AFTER_RESPAWN.register((was, now, alive) -> {
			if (now.level() instanceof ServerLevel level && isShipLife(level)) {
				putSomewhereSafe(now);
			}
		});

		// Nobody falls out of the world. The town is a platform in an empty
		// void, so walking off the edge would otherwise be the end of it.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 10 != 0) {
				return;
			}
			for (ServerLevel level : server.getAllLevels()) {
				if (!isShipLife(level)) {
					continue;
				}
				for (ServerPlayer player : level.players()) {
					if (player.getY() < Places.GROUND - 8) {
						// The ship catches you and costs you nothing for it.
						// What can hurt you is the fall itself, the cliffs,
						// and floors 9 and 10 -- that is the whole list.
						putSomewhereSafe(player);
						player.sendSystemMessage(Component.literal(
								"Careful -- that is the edge of the world.")
								.withStyle(ChatFormatting.GRAY));
					}
				}
			}
		});

		// Weather over the town. The island had none at all: the sky was the
		// same blue every day of a game about days. One ship day in four it
		// rains, and it stops when the day does.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 600 != 0) {
				return;
			}
			for (ServerLevel level : server.getAllLevels()) {
				if (!isShipLife(level)
						|| level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
					continue;
				}
				boolean wet = Math.floorMod(Cal.dayNumber() * 31L, 4L) == 0;
				var weather = level.getWeatherData();
				if (wet != weather.isRaining()) {
					weather.setRaining(wet);
					weather.setRainTime(wet ? 12000 : 0);
					weather.setClearWeatherTime(wet ? 0 : 12000);
				}
			}
		});

		LOGGER.info("Ship Life is aboard.");
	}

	/**
	 * Set the world up the first time anyone joins it.
	 *
	 * A Ship Life world is recognised by being an empty void where the ship
	 * should be. Once the ship is built that is no longer true, so the answer
	 * is remembered rather than worked out again.
	 */
	private static void onJoin(ServerPlayer player) {
		ServerLevel level = player.level() instanceof ServerLevel server ? server : null;
		if (level == null) {
			return;
		}

		// Which worlds are ours, worked out again from the world itself.
		recognise(level.getServer());

		if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
			// Joining on ship 2, which is in the Nether. The world is already
			// built and already ours; what is left is the handful of things
			// that are put back on every arrival.
			if (isShipLife(level)) {
				Gym.apply(player);
				Daily.check(player);
				Log.check(player);
				if (!standingOnSomething(player)) {
					putSomewhereSafe(player);
				}
			}
			return;
		}

		String key = level.dimension().identifier().toString();
		boolean known = SHIP_WORLDS.contains(key);
		boolean fresh = !known && isVoid(level);

		if (!known && !fresh && !isShipBuilt(level)) {
			return;                       // an ordinary world -- leave it alone
		}
		SHIP_WORLDS.add(key);
		claimTheNether(level.getServer());

		if (fresh) {
			level.getGameRules().set(GameRules.KEEP_INVENTORY, true, level.getServer());
			Ship.build(level);
		}

		// Nothing spawns on its own anywhere. The fight room and the boss room
		// put their own enemies there deliberately, which the rule does not
		// touch -- see Fight. Set on every join so older worlds get it too.
		level.getGameRules().set(GameRules.SPAWN_MOBS, false, level.getServer());

		// Creative is the mod's edit mode: the ship and the world are yours to
		// change, so nothing here puts anything back where it thinks it should
		// be. Repairs only run for people playing the game rather than
		// building it -- otherwise a wall you took out would be back by
		// morning.
		boolean building = player.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
		if (!building) {
			Ship.repair(level);
		}

		// Everything new since this world was made, in every world -- creative
		// too, because a world you are building in still wants the floors and
		// the people the mod has grown since. This one only ever adds.
		Ship.catchUp(level);
		for (ServerLevel each : level.getServer().getAllLevels()) {
			if (isShipLife(each)) {
				Kart.sweep(each);
			}
		}
		Person.everyone(level);

		// The hearts the gym gave you are not saved with you -- the lifts are
		// -- so they go back on every time you arrive. And turning up pays.
		Gym.apply(player);
		Daily.check(player);
		Gear.openSeventeen(player);
		Weapons.openEighteen(player);
		Comforts.trophies(player, level);
		Log.check(player);

		// Where the world puts people who have not chosen a bed. Set on every
		// join rather than only on a fresh world, so worlds made before this
		// get it too.
		level.setRespawnData(net.minecraft.world.level.storage.LevelData.RespawnData.of(
				net.minecraft.world.level.Level.OVERWORLD, Places.SPAWN, 0.0f, 0.0f));

		// Nothing we build is worth losing to a creeper or an enderman. Set on
		// every join rather than only on a fresh world, so worlds made before
		// this get it too.
		level.getGameRules().set(GameRules.MOB_GRIEFING, false, level.getServer());

		// Hardcore, adventure, spectator: they all become survival, because
		// the game is earning your way onto the ship and up it. Creative is
		// the exception and is left alone -- picking it says you are here to
		// change the ship rather than to live on it.
		if (!building && player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
			player.setGameMode(GameType.SURVIVAL);
		}

		if (building && State.firstTime(player, State.Once.CREATIVE_NOTE)) {
			player.sendSystemMessage(Component.literal(
					"Creative: the ship and the world are yours to change. Nothing "
					+ "will be put back.").withStyle(ChatFormatting.LIGHT_PURPLE));
		}

		if (!State.firstTime(player, State.Once.ARRIVED)) {
			// Coming back. Only move them if where they left off is thin air --
			// joining used to drop you into the void beside the island.
			if (!standingOnSomething(player)) {
				putSomewhereSafe(player);
			}
		} else {
			putSomewhereSafe(player);
			player.getInventory().setItem(Slots.BOOK_SLOT, Kit.questBook());
			player.getInventory().add(Kit.sponge());
			player.getInventory().add(Kit.towel());
			player.sendSystemMessage(Component.literal(
					"A letter for you: get $100 and you can live on the ship forever.")
					.withStyle(ChatFormatting.YELLOW));
			player.sendSystemMessage(Component.literal(
					"Chapter 1 -- Make the Money. Start with the dishes in the first house.")
					.withStyle(ChatFormatting.AQUA));
			player.sendSystemMessage(Component.literal(
					"Your Quest Book is in slot 9. Right-click it any time.")
					.withStyle(ChatFormatting.GRAY));
		}
	}

	/**
	 * Put someone back on solid ground.
	 *
	 * Which ground depends on how far they have got: once you live on the ship
	 * you come back to the lift on the lowest floor your passport opens, and
	 * before that you come back to the town.
	 */
	private static void putSomewhereSafe(ServerPlayer player) {
		BlockPos to = Places.SPAWN;
		ServerLevel where = (ServerLevel) player.level();
		for (int floor = 1; floor <= Places.TOP_FLOOR; floor++) {
			if (State.hasFloor(player, where, floor)) {
				to = Places.lift(floor);
				break;
			}
		}
		player.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
		player.setDeltaMovement(0.0, 0.0, 0.0);
		player.resetFallDistance();
	}

	/** Is there anything at all under their feet? */
	private static boolean standingOnSomething(ServerPlayer player) {
		if (player.getY() < Places.GROUND - 8) {
			return false;
		}
		BlockPos under = player.blockPosition().below();
		ServerLevel level = (ServerLevel) player.level();
		for (int drop = 0; drop < 6; drop++) {
			if (!level.getBlockState(under.below(drop)).isAir()) {
				return true;
			}
		}
		return false;
	}

	/** Is this one of our worlds? Everything else in the mod asks this first. */
	/**
	 * Say that this level is one of ours.
	 *
	 * Used by the Nether, which becomes a Ship Life world the moment ship 2
	 * is built in it -- everything from the lift to the arcade works over
	 * there because of this one line.
	 */
	public static void claim(ServerLevel level) {
		SHIP_WORLDS.add(level.dimension().identifier().toString());
	}

	/**
	 * Which worlds are ours, asked of the world rather than of memory.
	 *
	 * The answer used to live only in the set above, and the only thing that
	 * ever filled the set was somebody joining in the overworld. So logging
	 * out on ship 2 -- which is in the Nether -- and coming back in claimed
	 * nothing at all: the lift, the HUD, the shops and every command switched
	 * themselves off, and the game said this was not a Ship Life world.
	 *
	 * Now the question is put to the world on the way in, wherever you happen
	 * to be standing when you arrive.
	 */
	private static void recognise(net.minecraft.server.MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		String key = overworld.dimension().identifier().toString();
		if (!SHIP_WORLDS.contains(key) && !isShipBuilt(overworld)) {
			return;                       // an ordinary world -- leave it alone
		}
		SHIP_WORLDS.add(key);
		claimTheNether(server);
	}

	/**
	 * The Nether of a Ship Life world is a Ship Life world.
	 *
	 * Ship 2 is the only thing that is ever built out there, and it is built
	 * the first time anybody comes through the portal -- so waiting for the
	 * ship before claiming the level is what left the level unclaimed after
	 * every restart.
	 */
	private static void claimTheNether(net.minecraft.server.MinecraftServer server) {
		ServerLevel nether = server.getLevel(net.minecraft.world.level.Level.NETHER);
		if (nether != null) {
			claim(nether);
		}
	}

	public static boolean isShipLife(ServerLevel level) {
		return SHIP_WORLDS.contains(level.dimension().identifier().toString());
	}

	/** Nothing at all where the town goes: a brand new Ship Life world. */
	private static boolean isVoid(ServerLevel level) {
		BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
		for (int y = level.getMinY(); y < level.getMaxY(); y++) {
			probe.set(0, y, 0);
			if (!level.getBlockState(probe).isAir()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * A world we built earlier, coming back after a restart.
	 *
	 * This used to ask whether there was a lift button at panel(1), and then
	 * the lift moved: a world built before the move has its button at the old
	 * corner, so the question came back no, the world stopped being a Ship
	 * Life world, and every last thing in the mod switched itself off in it.
	 *
	 * So the question is now about the ship rather than about one block of
	 * it. Floor one's deck is laid before anything else is and never moves,
	 * and no ordinary world has a concrete floor at these coordinates.
	 */
	private static boolean isShipBuilt(ServerLevel level) {
		if (level.getBlockState(Places.panel(1)).is(Made.elevatorButton)) {
			return true;
		}
		net.minecraft.world.level.block.state.BlockState deck =
				level.getBlockState(new BlockPos(Places.SHIP_X, Places.floorY(1), Places.SHIP_Z));
		return deck.is(net.minecraft.world.level.block.Blocks.BLACK_CONCRETE)
				|| deck.is(net.minecraft.world.level.block.Blocks.WHITE_CONCRETE)
				|| deck.is(net.minecraft.world.level.block.Blocks.GRAY_CONCRETE)
				|| deck.is(net.minecraft.world.level.block.Blocks.QUARTZ_BLOCK);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
