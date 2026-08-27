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

	/** What a cleared wave pays. */
	public static final int WAVE_PAYS = 25;

	/** Everything alive that this floor put there, per player. */
	private static final List<Mob> SPAWNED = new ArrayList<>();

	private static final Random RANDOM = new Random();

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

	/** One wave on floor 9, harder each time. */
	private static void wave(ServerPlayer player, ServerLevel level) {
		if (!SPAWNED.isEmpty()) {
			player.sendSystemMessage(Component.literal("Finish what is already in here first.")
					.withStyle(ChatFormatting.GRAY));
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
				+ " of them. Clearing it pays " + WAVE_PAYS + " event tickets.")
				.withStyle(ChatFormatting.RED));
		level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(),
				SoundSource.PLAYERS, 1.0f, 0.6f);
	}

	/** Arachnes, or the dragon. */
	private static void boss(ServerPlayer player, ServerLevel level, boolean arachnes) {
		if (!SPAWNED.isEmpty()) {
			player.sendSystemMessage(Component.literal("There is already a fight going on.")
					.withStyle(ChatFormatting.GRAY));
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
			return;
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
