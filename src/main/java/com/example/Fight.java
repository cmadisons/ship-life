package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Floor 9, the fight room, and floor 10, where the bosses are.
 *
 * Floor 9 is waves: press the button and endermen, creepers, ghasts and
 * whatever else the ship keeps down there come at you, one wave harder than
 * the last. Clear a wave and it pays.
 *
 * Floor 10 is Arachnes and the Ender Dragon. Arachnes is a spider the size of
 * the room with her brood around her. The dragon is a Wither wearing the name,
 * because a real Ender Dragon spawned outside the End flies off to the world
 * origin looking for crystals that are not there, and a boss that leaves is
 * not a boss.
 */
public final class Fight {
	private Fight() {
	}

	/** What the first wave pays, and what each one after adds. */
	public static final int WAVE_PAYS = 25;
	public static final int WAVE_STEP = 15;

	/** What clearing wave n is worth. Wave 1 pays 25, wave 20 pays 310. */
	public static int wavePay(int number) {
		return Math.min(500, WAVE_PAYS + WAVE_STEP * (number - 1));
	}

	/** Everything alive that this floor put there, per player. */
	private static final List<Mob> SPAWNED = new ArrayList<>();

	private static final Random RANDOM = new Random();

	/**
	 * The only floors anything hostile is allowed on.
	 *
	 * Add a number here and that floor becomes a place things can be; leave it
	 * out and anything hostile that turns up there is taken away.
	 */
	private static final java.util.Set<Integer> FIGHTING_FLOORS = java.util.Set.of(9, 10);

	public static void register() {
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (!(player instanceof ServerPlayer who) || !(world instanceof ServerLevel level)
					|| !ShipLifeMod.isShipLife(level)) {
				return InteractionResult.PASS;
			}
			BlockPos pos = Places.local(hit.getBlockPos());
			if (pos.equals(Places.FIGHT_BUTTON) || pos.equals(Places.FIGHT_BUTTON.above())) {
				wave(who, level);
				return InteractionResult.SUCCESS;
			}
			if (pos.equals(Places.ARACHNES_DOOR) || pos.equals(Places.ARACHNES_DOOR.above())) {
				boss(who, level, true);
				return InteractionResult.SUCCESS;
			}
			if (pos.equals(Places.DRAGON_DOOR) || pos.equals(Places.DRAGON_DOOR.above())) {
				boss(who, level, false);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});

		// Nothing hostile anywhere but the fighting floors. The town is a place
		// to do chores in and the ship is a place to live, and neither is
		// improved by a creeper turning up in it.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 40 != 0) {
				return;
			}
			for (ServerLevel level : server.getAllLevels()) {
				if (!ShipLifeMod.isShipLife(level)) {
					continue;
				}
				for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
					if (entity instanceof net.minecraft.world.entity.monster.Monster
							&& !FIGHTING_FLOORS.contains(Places.floorAt(entity.getY()))) {
						entity.discard();
					}
				}
			}
		});

		// When everything a fight spawned is dead, the fight is over and it pays.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0 || SPAWNED.isEmpty()) {
				return;
			}
			SPAWNED.removeIf(mob -> !mob.isAlive() || mob.isRemoved());
			if (SPAWNED.isEmpty()) {
				for (ServerLevel level : server.getAllLevels()) {
					if (!ShipLifeMod.isShipLife(level)) {
						continue;
					}
					for (ServerPlayer player : level.players()) {
						int floor = Places.floorAt(player.getY());
						if (floor == 9) {
							State.add(player, State.WAVES, 1);
							Events.payTickets(player, WAVE_PAYS, "the wave is clear");
						} else if (floor == 10) {
							int paid = State.tally(player, State.BOSSES) == 0 ? 150 : 200;
							State.add(player, State.BOSSES, 1);
							Events.payTickets(player, paid, "the boss is down");
						}
					}
				}
			}
		});
	}

	/**
	 * Is there a fight in the way?
	 *
	 * Only if it is one you could walk to. A wave abandoned on floor 9 used
	 * to lock the boss doors on floor 10 for the rest of the world's life,
	 * which read as the bosses being broken -- so anything left in another
	 * room is cleared out and the new fight starts.
	 */
	private static boolean inTheWay(ServerPlayer player) {
		busy();
		if (SPAWNED.isEmpty()) {
			return false;
		}
		int floor = Places.floorAt(player.getY());
		for (Mob mob : SPAWNED) {
			if (Places.floorAt(mob.getY()) == floor) {
				player.sendSystemMessage(Component.literal("Finish what is already in here first -- "
						+ SPAWNED.size() + " left.").withStyle(ChatFormatting.GRAY));
				return true;
			}
		}
		clear();
		return false;
	}

	/** What floor 16 asks for, all four of them. */
	public static final int SIXTEEN_TICKETS = 1500;
	public static final int SIXTEEN_BOMBS = 6;
	public static final int SIXTEEN_WAVES = 3;

	/**
	 * Floor 16 opens on four things at once.
	 *
	 * Fifteen hundred event tickets paid out, six of Ben's bombs used, wave
	 * three cleared and a boss put down -- so it is the floor you get to by
	 * having played all of it, not by grinding one thing.
	 *
	 * Called from everywhere any of the four can change, because the one that
	 * finishes the set is as likely to be a purchase as a kill.
	 */
	public static void openSixteen(ServerPlayer player) {
		if (State.hasFloor(player, 16)) {
			return;
		}
		if (State.tally(player, State.WAVES) < SIXTEEN_WAVES
				|| State.tally(player, State.BOSSES) < 1
				|| State.tally(player, State.EVENT_SPENT) < SIXTEEN_TICKETS
				|| State.tally(player, State.BOMBS_USED) < SIXTEEN_BOMBS) {
			return;
		}
		State.unlock(player, 16);
		player.sendSystemMessage(Component.literal(
				"Floor 16 is open. Izzy lives up there.")
				.withStyle(ChatFormatting.AQUA));
	}

	/** How far off floor 16 you are, in words, for the book to show. */
	public static String sixteenLeft(ServerPlayer player) {
		return State.tally(player, State.EVENT_SPENT) + "/" + SIXTEEN_TICKETS + " tickets paid, "
				+ State.tally(player, State.BOMBS_USED) + "/" + SIXTEEN_BOMBS + " bombs used, "
				+ State.tally(player, State.WAVES) + "/" + SIXTEEN_WAVES + " waves, "
				+ State.tally(player, State.BOSSES) + "/1 boss";
	}

	/** One wave on floor 9, harder each time. */
	private static void wave(ServerPlayer player, ServerLevel level) {
		if (inTheWay(player)) {
			return;
		}
		int number = State.tally(player, State.WAVES) + 1;
		int howMany = Math.min(10, 2 + number);
		for (int i = 0; i < howMany; i++) {
			EntityType<? extends Mob> kind = switch (RANDOM.nextInt(5)) {
				case 0 -> EntityType.ENDERMAN;
				case 1 -> EntityType.CREEPER;
				case 2 -> EntityType.GHAST;
				case 3 -> EntityType.WITCH;
				default -> EntityType.VINDICATOR;
			};
			spawn(level, kind, spot(level, 9), 1.0 + number * 0.1, null);
		}
		player.sendSystemMessage(Component.literal("Wave " + number + " -- " + howMany
				+ " of them. Clearing it pays " + wavePay(number) + " event tickets.")
				.withStyle(ChatFormatting.RED));
		level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(),
				SoundSource.PLAYERS, 1.0f, 0.6f);
	}

	/** Arachnes, or the dragon. */
	private static void boss(ServerPlayer player, ServerLevel level, boolean arachnes) {
		if (inTheWay(player)) {
			return;
		}
		if (arachnes) {
			spawn(level, EntityType.SPIDER, Places.BOSS_SPOT, 6.0, "Arachnes");
			for (int i = 0; i < 4; i++) {
				spawn(level, EntityType.CAVE_SPIDER, spot(level, 10), 1.0, "Arachne");
			}
			player.sendSystemMessage(Component.literal(
					"Arachnes and her brood. 150 event tickets if you put her down.")
					.withStyle(ChatFormatting.RED));
		} else {
			spawn(level, EntityType.WITHER, Places.BOSS_SPOT, 1.0, "Ender Dragon");
			player.sendSystemMessage(Component.literal(
					"The dragon. 200 event tickets if you put it down.")
					.withStyle(ChatFormatting.RED));
		}
		level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(),
				SoundSource.PLAYERS, 1.0f, 0.5f);
	}

	/** Put one in the world, as big as it needs to be, and remember it. */
	private static void spawn(ServerLevel level, EntityType<? extends Mob> kind,
			BlockPos where, double toughness, String name) {
		Mob mob = kind.spawn(level, where, EntitySpawnReason.EVENT);
		if (mob == null) {
			ShipLifeMod.LOGGER.warn("Ship Life could not put a {} at {}.", kind, where);
			return;
		}
		// A wither spends its first ten seconds untouchable and then blows a
		// hole in the room. Neither is any use as a boss fight, so it comes in
		// awake and ready to be hit.
		if (mob instanceof net.minecraft.world.entity.boss.wither.WitherBoss wither) {
			wither.setInvulnerableTicks(0);
		}
		if (toughness != 1.0 && mob.getAttribute(Attributes.MAX_HEALTH) != null) {
			double health = mob.getAttribute(Attributes.MAX_HEALTH).getBaseValue() * toughness;
			mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
			mob.setHealth((float) health);
		}
		if (name != null) {
			mob.setCustomName(Component.literal(name).withStyle(ChatFormatting.RED));
			mob.setCustomNameVisible(true);
		}
		mob.setPersistenceRequired();
		SPAWNED.add(mob);
	}

	/** Somewhere in the room, but not on top of you. */
	private static BlockPos spot(ServerLevel level, int floor) {
		return new BlockPos(
				Places.SHIP_X + RANDOM.nextInt(17) - 8,
				Places.floorY(floor) + 1,
				Places.SHIP_Z + RANDOM.nextInt(17) - 8);
	}

	/** Is anything still alive from a fight? Used by the room to say so. */
	public static boolean busy() {
		SPAWNED.removeIf(mob -> !mob.isAlive() || mob.isRemoved());
		return !SPAWNED.isEmpty();
	}

	/** Alive count, for anything that wants to show it. */
	public static int left() {
		busy();
		return SPAWNED.size();
	}

	/** Kill everything a fight spawned. Used when a player gives up and leaves. */
	public static void clear() {
		for (Mob mob : SPAWNED) {
			if (mob.isAlive()) {
				mob.discard();
			}
		}
		SPAWNED.clear();
	}

	/** Anything the player has to be alive to see. */
	public static void announce(ServerPlayer player, LivingEntity target) {
		player.sendSystemMessage(Component.literal(target.getName().getString()
				+ " is still up.").withStyle(ChatFormatting.GRAY));
	}
}
