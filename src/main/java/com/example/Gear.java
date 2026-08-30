package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What Ben gives you: the armour and the bombs.
 *
 * The armour keeps a tenth of every hit off you and remembers what it kept.
 * The next time you land one, that goes on top -- so a long fight where you
 * are taking hits makes the swing that ends it hurt more. The number is a
 * bank, not a buff: it empties the moment it lands.
 *
 * The bombs put green gas on the floor. Everything hostile standing in it
 * loses ten a second, and what the gas takes goes into the same bank the
 * armour fills. The gas does not time out -- it sits there until nothing
 * hostile is left alive, which is the whole point of throwing one into a wave.
 */
public final class Gear {
	private Gear() {
	}

	/** How much of a hit the armour keeps off you. */
	public static final float STOPS = 0.10f;

	/** What the gas takes off each enemy, each go. */
	public static final float GAS_BITE = 10.0f;

	/** How wide the gas spreads. */
	public static final double GAS_REACH = 4.0;

	/** Ticks between doses -- once a second. */
	private static final int GAS_EVERY = 20;

	/** With nothing hostile about, gas still hangs around this long. */
	private static final int GAS_LINGER = 100;

	/** Damage banked and waiting for your next swing. */
	public static final AttachmentType<Float> CHARGE =
			AttachmentRegistry.<Float>builder()
					.initializer(() -> 0.0f)
					.persistent(Codec.FLOAT)
					.copyOnDeath()
					.buildAndRegister(ShipLifeMod.id("banked_damage"));

	/** A cloud of gas: where it is, who put it there, and when. */
	private record Cloud(ServerLevel level, Vec3 at, UUID owner, long born) {
	}

	private static final List<Cloud> CLOUDS = new ArrayList<>();

	/**
	 * True while we are applying the banked damage.
	 *
	 * The bonus hit is a hit like any other, so without this it would come
	 * back through the same listener that fired it and bank itself again.
	 */
	private static boolean applying = false;

	public static void register() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register(Gear::afterDamage);
		bombInHand();
		gasTick();
	}

	// ----------------------------------------------------------------- armour

	/**
	 * How many pieces of the set they have on.
	 *
	 * Each one keeps a tenth of the hit off you, so the coat on its own stops
	 * a tenth and the whole set stops four.
	 */
	public static int wearing(ServerPlayer player) {
		int pieces = 0;
		if (Kit.is(player.getItemBySlot(EquipmentSlot.CHEST), Kit.ARMOUR)) {
			pieces++;
		}
		if (Kit.is(player.getItemBySlot(EquipmentSlot.FEET), Kit.BOOTS)) {
			pieces++;
		}
		if (Kit.is(player.getItemBySlot(EquipmentSlot.HEAD), Kit.HELMET)) {
			pieces++;
		}
		if (Kit.is(player.getItemBySlot(EquipmentSlot.LEGS), Kit.LEGGINGS)) {
			pieces++;
		}
		return pieces;
	}

	public static float charge(ServerPlayer player) {
		return player.getAttachedOrCreate(CHARGE);
	}

	/** Put damage in the bank and say so. */
	public static void bank(ServerPlayer player, float amount) {
		if (amount <= 0) {
			return;
		}
		float total = charge(player) + amount;
		player.setAttached(CHARGE, total);
		Hud.busy(player, 30);
		player.sendOverlayMessage(Component.literal("+" + Math.round(total)
				+ " on your next hit").withStyle(ChatFormatting.GREEN));
	}

	private static void afterDamage(LivingEntity hurt, net.minecraft.world.damagesource.DamageSource source,
			float before, float taken, boolean blocked) {
		if (applying || taken <= 0) {
			return;
		}

		// A hit on you: every piece keeps a tenth of it off and remembers it.
		if (hurt instanceof ServerPlayer player && wearing(player) > 0) {
			float stopped = taken * STOPS * wearing(player);
			player.heal(stopped);
			bank(player, stopped);
			return;
		}

		// A hit by you: everything in the bank goes on top of it.
		if (!(hurt instanceof ServerPlayer)
				&& source.getEntity() instanceof ServerPlayer hitter
				&& hurt.level() instanceof ServerLevel level
				&& ShipLifeMod.isShipLife(level)) {
			spend(hitter, hurt, level);
		}
	}

	/** Empty the bank into whatever they just hit. */
	private static void spend(ServerPlayer player, LivingEntity target, ServerLevel level) {
		float banked = charge(player);
		if (banked < 1.0f) {
			return;
		}
		player.setAttached(CHARGE, 0.0f);
		applying = true;
		try {
			// It has already been hit this tick, so it is still sore from that
			// one; without this the extra would simply be ignored.
			target.invulnerableTime = 0;
			target.hurtServer(level, level.damageSources().magic(), banked);
		} finally {
			applying = false;
		}
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
				target.getX(), target.getY() + 1.0, target.getZ(), 12, 0.4, 0.6, 0.4, 0.02);
		level.playSound(null, target.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(),
				SoundSource.PLAYERS, 0.7f, 1.6f);
		Hud.busy(player, 30);
		player.sendOverlayMessage(Component.literal("+" + Math.round(banked) + " landed")
				.withStyle(ChatFormatting.GREEN));
	}

	// ------------------------------------------------------------------ bombs

	/** Right-click with a bomb, at a block or at nothing, and it goes down. */
	private static void bombInHand() {
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (!Kit.is(player.getItemInHand(hand), Kit.BOMB)) {
				return InteractionResult.PASS;
			}
			if (!(player instanceof ServerPlayer who) || !(world instanceof ServerLevel level)) {
				return InteractionResult.SUCCESS;
			}
			drop(who, level);
			player.getItemInHand(hand).shrink(1);
			used(who);
			return InteractionResult.SUCCESS;
		});

		// Aiming at a block goes to the block first, so it has to be caught
		// here too -- otherwise a bomb pointed at the floor does nothing.
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (!Kit.is(player.getItemInHand(hand), Kit.BOMB)) {
				return InteractionResult.PASS;
			}
			if (!(player instanceof ServerPlayer who) || !(world instanceof ServerLevel level)) {
				return InteractionResult.SUCCESS;
			}
			CLOUDS.add(new Cloud(level, Vec3.atCenterOf(hit.getBlockPos().above()),
					who.getUUID(), level.getGameTime()));
			player.getItemInHand(hand).shrink(1);
			announce(who, level, Vec3.atCenterOf(hit.getBlockPos().above()));
			used(who);
			return InteractionResult.SUCCESS;
		});
	}

	/** Put one down where they are standing. */
	private static void drop(ServerPlayer player, ServerLevel level) {
		Vec3 where = player.position();
		CLOUDS.add(new Cloud(level, where, player.getUUID(), level.getGameTime()));
		announce(player, level, where);
	}

	/** A bomb has gone down. Six of them is one of floor 16's four conditions. */
	private static void used(ServerPlayer player) {
		State.add(player, State.BOMBS_USED, 1);
		Fight.openSixteen(player);
	}

	private static void announce(ServerPlayer player, ServerLevel level, Vec3 where) {
		level.playSound(null, net.minecraft.core.BlockPos.containing(where),
				SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0f, 0.5f);
		player.sendSystemMessage(Component.literal(
				"The gas is down. It stays until everything in here is dead.")
				.withStyle(ChatFormatting.GREEN));
	}

	private static void gasTick() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (CLOUDS.isEmpty()) {
				return;
			}
			long now = server.getTickCount();
			boolean bites = now % GAS_EVERY == 0;

			// Backwards, because a cloud can clear itself out of the list.
			for (int i = CLOUDS.size() - 1; i >= 0; i--) {
				Cloud cloud = CLOUDS.get(i);
				if (now % 2 == 0) {
					show(cloud);
				}
				if (!bites) {
					continue;
				}
				List<LivingEntity> caught = cloud.level().getEntitiesOfClass(LivingEntity.class,
						new AABB(cloud.at(), cloud.at()).inflate(GAS_REACH),
						living -> living instanceof Monster && living.isAlive());
				ServerPlayer owner = server.getPlayerList().getPlayer(cloud.owner());
				float took = 0;
				for (LivingEntity enemy : caught) {
					enemy.invulnerableTime = 0;
					float had = enemy.getHealth();
					enemy.hurtServer(cloud.level(), cloud.level().damageSources().magic(), GAS_BITE);
					took += Math.min(had, GAS_BITE);
				}
				// What the gas takes is yours, same as what the armour stops.
				if (owner != null && took > 0) {
					bank(owner, took);
				}
				// It only lifts once there is nothing left for it to work on.
				boolean anythingLeft = !cloud.level().getEntitiesOfClass(Monster.class,
						new AABB(cloud.at(), cloud.at()).inflate(48.0),
						LivingEntity::isAlive).isEmpty();
				if (!anythingLeft && now - cloud.born() > GAS_LINGER) {
					CLOUDS.remove(i);
					if (owner != null) {
						owner.sendSystemMessage(Component.literal("The gas clears.")
								.withStyle(ChatFormatting.GRAY));
					}
				}
			}
		});
	}

	/** Draw it: a green fog with sparks coming off it. */
	private static void show(Cloud cloud) {
		ServerLevel level = cloud.level();
		Vec3 at = cloud.at();
		level.sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0x55DD44),
				at.x, at.y + 0.6, at.z, 14, GAS_REACH * 0.5, 0.8, GAS_REACH * 0.5, 0.0);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
				at.x, at.y + 0.4, at.z, 4, GAS_REACH * 0.5, 0.6, GAS_REACH * 0.5, 0.0);
	}

	/** Nothing hostile should be able to keep gas alive across a reload. */
	public static void clear() {
		CLOUDS.clear();
	}
}
