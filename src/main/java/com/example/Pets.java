package com.example;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;

/**
 * The pets, ten arcade tickets each.
 *
 * What a pet is for is the boost it gives you: the lion fights alongside you,
 * the dog makes you quicker through water, the cat opens floor 6 for good, and
 * the dolphin does whatever one of the other three feels like that day. They
 * all follow you at once, and two of a kind doubles the boost -- except the
 * cat, whose floor is either open or it isn't.
 *
 * They are tamed wolves and cats wearing a name, because that is what follows
 * you around in Minecraft without a single new model. A lion that looks like a
 * lion needs art this mod does not have yet.
 */
public final class Pets {
	private Pets() {
	}

	public enum Kind {
		LION("Lion", "Helps you fight"),
		DOG("Dog", "A swimming boost every 7 seconds"),
		CAT("Cat", "Opens floor 6, for good"),
		DOLPHIN("Dolphin", "A random one of the others, each day");

		public final String label;
		public final String what;

		Kind(String label, String what) {
			this.label = label;
			this.what = what;
		}
	}

	/** What a pet costs at the arcade counter. */
	public static final int PRICE = 10;

	private static final AttachmentType<Integer>[] OWNED = counts();

	@SuppressWarnings("unchecked")
	private static AttachmentType<Integer>[] counts() {
		AttachmentType<Integer>[] types = new AttachmentType[Kind.values().length];
		for (Kind kind : Kind.values()) {
			types[kind.ordinal()] = AttachmentRegistry.<Integer>builder()
					.initializer(() -> 0)
					.persistent(Codec.INT)
					.copyOnDeath()
					.buildAndRegister(ShipLifeMod.id("pet_" + kind.name().toLowerCase()));
		}
		return types;
	}

	public static int owned(ServerPlayer player, Kind kind) {
		return player.getAttachedOrCreate(OWNED[kind.ordinal()]);
	}

	public static int total(ServerPlayer player) {
		int all = 0;
		for (Kind kind : Kind.values()) {
			all += owned(player, kind);
		}
		return all;
	}

	/**
	 * Buy one.
	 *
	 * Two of a kind doubles that pet's boost, so buying a second of something
	 * is worth as much as buying your first of something else -- which is the
	 * whole reason you are allowed more than one.
	 */
	public static boolean buy(ServerPlayer player, Kind kind) {
		if (State.arcade(player) < PRICE) {
			player.sendSystemMessage(Component.literal("That costs " + PRICE
					+ " tickets and you have " + State.arcade(player) + ".")
					.withStyle(ChatFormatting.RED));
			return false;
		}
		State.arcade(player, -PRICE);
		player.setAttached(OWNED[kind.ordinal()], owned(player, kind) + 1);
		spawn(player, kind);

		if (kind == Kind.CAT && !State.hasFloor(player, 6)) {
			State.unlock(player, 6);
			player.sendSystemMessage(Component.literal(
					"The cat opens floor 6 -- the race track.")
					.withStyle(ChatFormatting.AQUA));
		}
		player.sendSystemMessage(Component.literal("A " + kind.label + " follows you now. "
				+ (owned(player, kind) == 2 && kind != Kind.CAT
						? "Two of a kind: double boost."
						: kind.what + "."))
				.withStyle(ChatFormatting.GREEN));
		return true;
	}

	/** Put the animal in the world, tamed, so it comes with you. */
	private static void spawn(ServerPlayer player, Kind kind) {
		ServerLevel level = (ServerLevel) player.level();
		BlockPos at = player.blockPosition();
		TamableAnimal pet = kind == Kind.CAT
				? EntityType.CAT.spawn(level, at, EntitySpawnReason.MOB_SUMMONED)
				: EntityType.WOLF.spawn(level, at, EntitySpawnReason.MOB_SUMMONED);
		if (pet == null) {
			return;
		}
		pet.tame(player);
		pet.setCustomName(Component.literal(kind.label).withStyle(ChatFormatting.AQUA));
		pet.setCustomNameVisible(true);
		pet.setPersistenceRequired();
		level.playSound(null, at, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.7f, 1.2f);
	}

	// ------------------------------------------------------------- the boosts

	/** Which pet the dolphin is copying today. Same for everyone, all day. */
	public static Kind dolphinToday() {
		Kind[] copies = { Kind.LION, Kind.DOG, Kind.CAT };
		return copies[(int) Math.floorMod(Cal.dayNumber() * 31L, copies.length)];
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) {
				return;
			}
			for (ServerLevel level : server.getAllLevels()) {
				if (!ShipLifeMod.isShipLife(level)) {
					continue;
				}
				for (ServerPlayer player : level.players()) {
					boosts(player);
				}
			}
		});
	}

	private static void boosts(ServerPlayer player) {
		int lions = owned(player, Kind.LION);
		int dogs = owned(player, Kind.DOG);
		int dolphins = owned(player, Kind.DOLPHIN);

		// The dolphin stands in for one of the others, and doubles like the
		// pet it is copying rather than on its own.
		Kind copying = dolphinToday();
		if (copying == Kind.LION) {
			lions += dolphins;
		} else if (copying == Kind.DOG) {
			dogs += dolphins;
		}

		if (lions > 0) {
			player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40,
					lions >= 2 ? 1 : 0, true, false, false));
		}
		// Every seven seconds, and only when there is water to be quick in.
		if (dogs > 0 && player.isInWater()
				&& player.level().getGameTime() % 140 < 20) {
			player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60,
					dogs >= 2 ? 1 : 0, true, false, false));
		}
	}
}
