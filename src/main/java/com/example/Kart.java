package com.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Floor 6: the go-karts.
 *
 * This was a game on a screen -- lanes and numbers and a field of cars that
 * only existed as an array. It is a real track now: a loop of rail round the
 * floor, a kart you actually sit in, and five more karts going round it with
 * you. The difference is that you can look sideways and see one.
 *
 * The karts are ordinary minecarts, which means the track does the driving.
 * Powered rail spaced along both straights keeps everything moving, so a kart
 * left alone laps for ever and the floor is never an empty room.
 *
 * A lap is counted the way the pool counts one: you cannot cross the line
 * twice for two laps, you have to go round the far side in between.
 */
public final class Kart {
	private Kart() {
	}

	/** Charlie's quest: five laps, two minutes, and floor 7 opens. */
	public static final int LAPS = 5;
	public static final int TARGET_TICKS = 2400;

	/** How many karts are out there without you. */
	private static final int RIVALS = 5;

	/** The colours the rival karts come in, so no two beside you match. */
	private static final net.minecraft.world.level.block.Block[] COLOURS = {
		Blocks.BLUE_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE,
		Blocks.ORANGE_CONCRETE, Blocks.PURPLE_CONCRETE,
	};

	/**
	 * Dress a minecart up as a go-kart.
	 *
	 * A minecart is a grey tub. Sitting a coloured block in it and dropping it
	 * low in the frame gives it a body and a driver sitting down in it, which
	 * is as close to a kart as you get without a model of your own.
	 */
	private static void paint(Minecart kart, net.minecraft.world.level.block.Block colour) {
		kart.setCustomDisplayBlockState(java.util.Optional.of(colour.defaultBlockState()));
		kart.setDisplayOffset(-2);
	}

	/** Where someone is in their race: laps done, whether they are round the far side. */
	private record Run(int laps, boolean far, long started) {
	}

	private static final Map<UUID, Run> RACING = new HashMap<>();

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerLevel level : server.getAllLevels()) {
				if (!ShipLifeMod.isShipLife(level)) {
					continue;
				}
				if (server.getTickCount() % 40 == 0) {
					keepRivalsRunning(level);
				}
				for (ServerPlayer player : level.players()) {
					lap(player);
				}
			}
		});
	}

	// ------------------------------------------------------------ getting in

	/** Right-clicking the kart in the pits puts you in one on the line. */
	public static void getIn(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		if (player.getVehicle() instanceof Minecart yours) {
			player.stopRiding();
			yours.discard();          // or the track silts up with empty karts
			RACING.remove(player.getUUID());
			say(player, "Out of the kart.");
			return;
		}

		BlockPos line = Places.KART_LINE;
		Minecart kart = EntityType.MINECART.create(level, EntitySpawnReason.TRIGGERED);
		if (kart == null) {
			return;
		}
		kart.setPos(line.getX() + 0.5, line.getY(), line.getZ() + 0.5);
		paint(kart, Blocks.RED_CONCRETE);        // yours is the red one
		level.addFreshEntity(kart);
		player.startRiding(kart);
		RACING.put(player.getUUID(), new Run(0, false, level.getGameTime()));

		level.playSound(null, line, SoundEvents.NOTE_BLOCK_BASS.value(),
				SoundSource.PLAYERS, 0.8f, 0.6f);
		player.sendSystemMessage(Component.literal("Go. " + LAPS + " laps in "
				+ Pool.time(TARGET_TICKS) + " opens floor 7.")
				.withStyle(ChatFormatting.GREEN));
	}

	// -------------------------------------------------------------- the laps

	private static void lap(ServerPlayer player) {
		Run run = RACING.get(player.getUUID());
		if (run == null) {
			return;
		}
		// Out of the kart, or off the floor, and the race is over.
		if (!(player.getVehicle() instanceof Minecart) || Places.floorAt(player.getY()) != 6) {
			RACING.remove(player.getUUID());
			return;
		}

		double z = player.getZ() - Places.SHIP_Z;
		boolean far = z >= Places.KART_HALF_DEPTH - 1;
		boolean line = z <= -(Places.KART_HALF_DEPTH - 1);

		if (far && !run.far()) {
			RACING.put(player.getUUID(), new Run(run.laps(), true, run.started()));
			return;
		}
		if (!line || !run.far()) {
			return;
		}

		int laps = run.laps() + 1;
		long ticks = player.level().getGameTime() - run.started();
		if (laps < LAPS) {
			RACING.put(player.getUUID(), new Run(laps, false, run.started()));
			player.sendOverlayMessage(Component.literal("Lap " + laps + " / " + LAPS
					+ "  --  " + Pool.time(ticks)).withStyle(ChatFormatting.YELLOW));
			return;
		}
		RACING.remove(player.getUUID());
		finished(player, (int) ticks);
	}

	/**
	 * The time to beat, and who set it.
	 *
	 * The track had a clock and nothing else in it -- you raced the number two
	 * minutes. The racer drives one of the five karts out there and holds the
	 * ship record, which is something to race rather than something to read.
	 * Called what they are, like everybody else on the ship.
	 */
	public static final String RIVAL = "The racer";
	public static final int RIVAL_TICKS = 1900;          // one minute thirty-five
	public static final int BEATING_VIC_PAYS = 60;

	private static void finished(ServerPlayer player, int ticks) {
		player.level().playSound(null, player.blockPosition(),
				SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
		State.add(player, State.RACES, 1);
		player.sendSystemMessage(Component.literal(LAPS + " laps in " + Pool.time(ticks) + ".")
				.withStyle(ChatFormatting.GREEN));

		// Vic's time, which is the race behind the race.
		if (ticks < RIVAL_TICKS) {
			player.sendSystemMessage(Component.literal(RIVAL + " does five laps in "
					+ Pool.time(RIVAL_TICKS) + ". You just beat them.")
					.withStyle(ChatFormatting.LIGHT_PURPLE));
			Events.payTickets(player, BEATING_VIC_PAYS, "you beat the racer");
		} else {
			player.sendSystemMessage(Component.literal(RIVAL + " does it in "
					+ Pool.time(RIVAL_TICKS) + ".").withStyle(ChatFormatting.GRAY));
		}

		if (ticks > TARGET_TICKS) {
			player.sendSystemMessage(Component.literal(
					"Charlie wants five laps in two minutes. Stay off the brakes.")
					.withStyle(ChatFormatting.GRAY));
			return;
		}
		if (!State.hasFloor(player, 7)) {
			State.unlock(player, 7);
			player.sendSystemMessage(Component.literal(
					"Under two minutes. Floor 7 -- the events -- is open.")
					.withStyle(ChatFormatting.AQUA));
		}
	}

	// ----------------------------------------------------------- the rivals

	/**
	 * Keep five karts circulating, with someone sitting in each.
	 *
	 * The driver is an armour stand rather than a mob on purpose: a mob would
	 * wander off the moment it was not in a cart, and an empty cart going
	 * round a track does not read as a race.
	 */
	private static void keepRivalsRunning(ServerLevel level) {
		AABB floor = new AABB(
				Places.SHIP_X - Places.ROOM, Places.floorY(6), Places.SHIP_Z - Places.ROOM,
				Places.SHIP_X + Places.ROOM, Places.floorY(6) + 6, Places.SHIP_Z + Places.ROOM);
		List<Minecart> karts = level.getEntitiesOfClass(Minecart.class, floor,
				cart -> cart.getPassengers().stream().anyMatch(ArmorStand.class::isInstance));

		for (Minecart kart : karts) {
			// A kart that has stopped on flat rail needs a shove to get going.
			if (kart.getDeltaMovement().horizontalDistanceSqr() < 0.001) {
				kart.setDeltaMovement(0.3, 0.0, 0.0);
			}
		}
		for (int i = karts.size(); i < RIVALS; i++) {
			spawnRival(level, i);
		}
	}

	private static void spawnRival(ServerLevel level, int index) {
		BlockPos spot = Places.kartGrid(index);
		Minecart kart = EntityType.MINECART.create(level, EntitySpawnReason.TRIGGERED);
		ArmorStand driver = EntityType.ARMOR_STAND.create(level, EntitySpawnReason.TRIGGERED);
		if (kart == null || driver == null) {
			return;
		}
		kart.setPos(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
		paint(kart, COLOURS[index % COLOURS.length]);
		driver.setPos(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
		driver.setCustomName(Component.literal("Racer " + (index + 1)));
		driver.setInvulnerable(true);
		driver.setNoGravity(true);
		level.addFreshEntity(kart);
		level.addFreshEntity(driver);
		driver.startRiding(kart);
		kart.setDeltaMovement(0.3, 0.0, 0.0);
	}

	private static void say(ServerPlayer player, String text) {
		player.sendOverlayMessage(Component.literal(text).withStyle(ChatFormatting.YELLOW));
	}
}
