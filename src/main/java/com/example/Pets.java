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
 * The lion is an ocelot and the dolphin is a dolphin, because those are the
 * models Minecraft has that look like the animal on the tin. Neither can be
 * tamed and neither follows anybody, so the ship keeps them with you itself:
 * stray more than a dozen blocks and your pet turns up beside you. The dolphin
 * is kept from drying out the same way -- a pet that dies of being indoors is
 * no pet.
 */
public final class Pets {
	private Pets() {
	}

	public enum Kind {
		LION("Lion", "Helps you fight"),
		DOG("Dog", "A swimming boost every 7 seconds"),
		CAT("Cat", "Opens floor 6, for good"),
		DOLPHIN("Dolphin", "A random one of the others, each day"),
		SKELETON("Skeleton", "A combat boost"),
		SHADOW("Shadow", "0.25 of every pet's boost");

		public final String label;
		public final String what;

		Kind(String label, String what) {
			this.label = label;
			this.what = what;
		}
	}

	/** The four the arcade sells. The other two are the pet store's. */
	public static final Kind[] ARCADE_PETS = { Kind.LION, Kind.DOG, Kind.CAT, Kind.DOLPHIN };

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

	/**
	 * Keep the ones that cannot follow with you.
	 *
	 * A wolf follows its owner on its own. An ocelot and a dolphin do not, so
	 * once a second anything of yours more than a dozen blocks off is put back
	 * beside you, and its air is topped up while we are there.
	 */
	private static void herd(ServerPlayer player, ServerLevel level) {
		for (net.minecraft.world.entity.Mob pet : level.getEntitiesOfClass(
				net.minecraft.world.entity.Mob.class,
				player.getBoundingBox().inflate(64.0),
				mob -> mob.getCustomName() != null && !(mob instanceof TamableAnimal))) {
			String name = pet.getCustomName().getString();
			boolean ours = false;
			for (Kind kind : Kind.values()) {
				ours = ours || kind.label.equals(name);
			}
			if (!ours) {
				continue;
			}
			pet.setAirSupply(pet.getMaxAirSupply());
			if (pet.distanceToSqr(player) > 12 * 12) {
				pet.teleportTo(player.getX() + 1, player.getY(), player.getZ() + 1);
			}
		}
	}

	/**
	 * The lion fights.
	 *
	 * "Helps you fight" was a line in a shop menu and nothing else. Now, on
	 * the fighting floors, anything hostile within a few blocks of your lion
	 * gets swiped at once a second -- harder if you have two of them, harder
	 * again if you have been feeding it.
	 */
	private static void lionFights(ServerPlayer player, ServerLevel level) {
		int lions = owned(player, Kind.LION);
		if (lions == 0) {
			return;
		}
		int floor = Places.floorAt(player.getY());
		if (floor != 9 && floor != 10) {
			return;
		}
		double bite = 4.0 * Math.min(2, lions) * strength(player, Kind.LION);
		for (net.minecraft.world.entity.monster.Monster prey : level.getEntitiesOfClass(
				net.minecraft.world.entity.monster.Monster.class,
				player.getBoundingBox().inflate(8.0),
				monster -> monster.isAlive())) {
			prey.invulnerableTime = 0;
			prey.hurtServer(level, level.damageSources().magic(), (float) bite);
			level.playSound(null, prey.blockPosition(),
					net.minecraft.sounds.SoundEvents.RAVAGER_ROAR,
					SoundSource.PLAYERS, 0.35f, 1.6f);
			return;                        // one swipe a second, not a massacre
		}
	}

	public static int owned(ServerPlayer player, Kind kind) {
		return player.getAttachedOrCreate(OWNED[kind.ordinal()]);
	}

	/** Do they have one of each of the four the arcade sells? */
	public static boolean everyPet(ServerPlayer player) {
		for (Kind kind : ARCADE_PETS) {
			if (owned(player, kind) == 0) {
				return false;
			}
		}
		return true;
	}

	/** What each pet looks like. */
	private static EntityType<? extends net.minecraft.world.entity.Mob> model(Kind kind) {
		return switch (kind) {
			case LION -> EntityType.OCELOT;
			case DOLPHIN -> EntityType.DOLPHIN;
			case CAT -> EntityType.CAT;
			default -> EntityType.WOLF;
		};
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
		// A second cat does nothing at all: the floor it opens is open. So
		// the counter says so rather than taking ten tickets for a pet that
		// is already yours.
		if (kind == Kind.CAT && owned(player, kind) > 0) {
			player.sendSystemMessage(Component.literal(
					"You have a cat, and a second one does nothing -- floor 6 is "
					+ "already open.").withStyle(ChatFormatting.GRAY));
			return false;
		}
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

		// One of each of the four the arcade sells opens floor 7.
		//
		// The other two are sold on floor 12, which is bought with event
		// tickets, which come from the events on floor 7 -- so asking for
		// those as well would be asking you to have floor 7 before you can
		// have floor 7.
		if (!State.hasFloor(player, 7) && everyPet(player)) {
			State.unlock(player, 7);
			player.sendSystemMessage(Component.literal(
					"One of every pet. Floor 7 -- the events -- is open.")
					.withStyle(ChatFormatting.AQUA));
		}
		player.sendSystemMessage(Component.literal("A " + kind.label + " follows you now. "
				+ (owned(player, kind) == 2 && kind != Kind.CAT
						? "Two of a kind: double boost."
						: kind.what + "."))
				.withStyle(ChatFormatting.GREEN));
		return true;
	}

	/** Hand one over without charging arcade tickets for it. */
	public static void grant(ServerPlayer player, Kind kind) {
		player.setAttached(OWNED[kind.ordinal()], owned(player, kind) + 1);
		spawn(player, kind);
		player.sendSystemMessage(Component.literal("A " + kind.label
				+ " follows you now. " + kind.what + ".").withStyle(ChatFormatting.GREEN));
	}

	/**
	 * How much better than standard one kind of your pets is.
	 *
	 * Food compounds at a tenth each time and stops at twice, so the eighth
	 * one is the last that does anything -- 1.1 to the seventh is 1.95.
	 */
	public static double strength(ServerPlayer player, Kind kind) {
		return Math.min(2.0, Math.pow(1.1, fed(player, kind)));
	}

	private static int fed(ServerPlayer player, Kind kind) {
		for (String part : State.petFood(player).split(",")) {
			String[] bits = part.split(":");
			if (bits.length == 2 && bits[0].equals(kind.name())) {
				return Integer.parseInt(bits[1]);
			}
		}
		return 0;
	}

	/** Feed one kind of pet, if it is not already as good as it gets. */
	public static void feed(ServerPlayer player, Kind kind) {
		if (owned(player, kind) == 0) {
			player.sendSystemMessage(Component.literal("You do not have a " + kind.label + ".")
					.withStyle(ChatFormatting.RED));
			return;
		}
		if (strength(player, kind) >= 2.0) {
			player.sendSystemMessage(Component.literal("Your " + kind.label
					+ " is already at x2 -- as good as they go.")
					.withStyle(ChatFormatting.GRAY));
			return;
		}
		if (State.event(player) < Shops.PET_FOOD_COST) {
			player.sendSystemMessage(Component.literal("Pet food is "
					+ Shops.PET_FOOD_COST + " event tickets and you have "
					+ State.event(player) + ".").withStyle(ChatFormatting.RED));
			return;
		}
		State.spendEvent(player, Shops.PET_FOOD_COST);

		int now = fed(player, kind) + 1;
		java.util.List<String> parts = new java.util.ArrayList<>();
		boolean replaced = false;
		for (String part : State.petFood(player).split(",")) {
			if (part.isEmpty()) {
				continue;
			}
			if (part.startsWith(kind.name() + ":")) {
				parts.add(kind.name() + ":" + now);
				replaced = true;
			} else {
				parts.add(part);
			}
		}
		if (!replaced) {
			parts.add(kind.name() + ":" + now);
		}
		State.petFood(player, String.join(",", parts));
		player.sendSystemMessage(Component.literal("Your " + kind.label + " is now x"
				+ String.format("%.2f", strength(player, kind)) + ".")
				.withStyle(ChatFormatting.GREEN));
	}

	/** Put the animal in the world, tamed, so it comes with you. */
	private static void spawn(ServerPlayer player, Kind kind) {
		ServerLevel level = (ServerLevel) player.level();
		BlockPos at = player.blockPosition();
		net.minecraft.world.entity.Mob pet = model(kind).spawn(level, at,
				EntitySpawnReason.MOB_SUMMONED);
		if (pet == null) {
			return;
		}
		if (pet instanceof TamableAnimal tame) {
			tame.tame(player);
		} else {
			// An ocelot or a dolphin cannot be tamed, and neither should be
			// able to come to any harm indoors.
			pet.setInvulnerable(true);
		}
		// A lion is not an ocelot's size, and a pet dolphin indoors is not a
		// wild one's. Scale is the one thing Minecraft will let a model do
		// without a model, and it is enough to tell them apart across a room.
		var scale = pet.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
		if (scale != null) {
			scale.setBaseValue(switch (kind) {
				case LION -> 1.9;
				case DOLPHIN -> 1.2;
				case SHADOW -> 0.8;
				default -> 1.0;
			});
		}
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
					herd(player, level);
					lionFights(player, level);
				}
			}
		});
	}

	/** Is anything making you faster in the water right now? */
	public static boolean swimBoosted(ServerPlayer player) {
		int dogs = owned(player, Kind.DOG);
		if (dolphinToday() == Kind.DOG) {
			dogs += owned(player, Kind.DOLPHIN);
		}
		return dogs > 0 || Shops.running(player, "swim");
	}

	private static void boosts(ServerPlayer player) {
		int lions = owned(player, Kind.LION) + owned(player, Kind.SKELETON);
		int dogs = owned(player, Kind.DOG);
		int dolphins = owned(player, Kind.DOLPHIN);
		// A Shadow is a quarter of everything else you have.
		int shadows = owned(player, Kind.SHADOW);

		// The dolphin stands in for one of the others, and doubles like the
		// pet it is copying rather than on its own.
		Kind copying = dolphinToday();
		if (copying == Kind.LION) {
			lions += dolphins;
		} else if (copying == Kind.DOG) {
			dogs += dolphins;
		}

		double lionPower = lions * strength(player, Kind.LION) * (1 + 0.25 * shadows);
		double dogPower = dogs * strength(player, Kind.DOG) * (1 + 0.25 * shadows);
		// The Keg's fighting dish is another hand in the same fight.
		if (Shops.running(player, "fight")) {
			lionPower += 1;
		}
		if (Shops.running(player, "swim")) {
			dogPower += 1;
		}

		if (lionPower > 0) {
			player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40,
					lionPower >= 2 ? 1 : 0, true, false, false));
		}
		// Every seven seconds, and only when there is water to be quick in.
		if (dogPower > 0 && player.isInWater()
				&& player.level().getGameTime() % 140 < 20) {
			player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60,
					dogPower >= 2 ? 1 : 0, true, false, false));
		}
	}
}
