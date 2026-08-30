package com.example;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The people on the ship.
 *
 * Charlie in the lobby, the two on the front desk, Ben on 15 and Izzy on 16.
 * They were doors and blocks you clicked before this, which works and reads
 * like nobody being there.
 *
 * A person is built the way you are -- a head, a body, two arms and two legs,
 * on the same model the player hangs on -- and what makes one different from
 * another is the skin: the clothes, the hair, the face. Those are drawn by
 * tools/make_skins.py, one 64x64 skin each, and picked out by name at the
 * other end, so adding somebody is a name and a set of colours and nothing
 * else.
 *
 * They stand where they are put. No wandering, no fighting, nothing can hurt
 * them, and they turn to watch you when you come near.
 */
public class Person extends PathfinderMob {
	public static final ResourceKey<EntityType<?>> KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, ShipLifeMod.id("person"));

	public static EntityType<Person> TYPE;

	public Person(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
	}

	/** Called at start-up, before any world exists. */
	public static void register() {
		TYPE = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
				EntityType.Builder.<Person>of(Person::new, MobCategory.MISC)
						.sized(0.6f, 1.8f)
						.build(KEY));
		FabricDefaultAttributeRegistry.register(TYPE, PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.0));
	}

	@Override
	protected void registerGoals() {
		// Watching you is the whole of their behaviour.
		this.goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, 8.0f));
		this.goalSelector.addGoal(1, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(net.minecraft.world.entity.Entity other) {
		// Standing in a doorway is their job. Nothing shoves them out of it.
	}

	/**
	 * What each one is called.
	 *
	 * Their jobs, not their names: the two on the desk are Staff, Charlie is
	 * the Manager, Ben and Izzy are Friends. Which means two people can share
	 * a label, so the colour of the label is what tells their skins apart --
	 * see PersonRenderer -- and where they are standing is what tells the
	 * game which of them you just talked to.
	 */
	public static final String CHARLIE = "Manager";
	public static final String BEN = "Friend";
	public static final String IZZY = "Friend";
	public static final String DESK = "Staff";
	public static final String LOBBY = "Staff";
	public static final String COOK = "Cook";

	/**
	 * Everyone, in their place.
	 *
	 * Run on every join. Anybody already standing where they should be is
	 * left alone, so this can be called as often as it likes.
	 */
	public static void everyone(ServerLevel level) {
		place(level, Places.CHAIR, CHARLIE, ChatFormatting.YELLOW);
		sit(level, Places.CHAIR, CHARLIE);
		place(level, Places.DESK.north(), DESK, ChatFormatting.WHITE);
		place(level, Places.DOOR.east(2), LOBBY, ChatFormatting.GRAY);
		place(level, Places.BEN.south(), BEN, ChatFormatting.AQUA);
		place(level, Places.IZZY.south(), IZZY, ChatFormatting.LIGHT_PURPLE);
		place(level, Places.BUFFET_COOK.east(), COOK, ChatFormatting.GOLD);
	}

	/**
	 * Sit somebody down on the chair they are standing at.
	 *
	 * Minecraft has no such thing as a chair, so sitting is riding: an
	 * invisible stand at seat height that nothing can see, hurt or hear, with
	 * the person on it. Riding is what bends a person's legs, so once he
	 * is on it he is sitting rather than standing in the furniture.
	 */
	public static void sit(ServerLevel level, BlockPos chair, String name) {
		Person person = null;
		for (Person each : level.getEntitiesOfClass(Person.class,
				new net.minecraft.world.phys.AABB(chair).inflate(3.0))) {
			if (each.getCustomName() != null
					&& each.getCustomName().getString().equals(name)) {
				person = each;
			}
		}
		if (person == null || person.isPassenger()) {
			return;
		}

		net.minecraft.world.entity.decoration.ArmorStand seat = null;
		for (net.minecraft.world.entity.decoration.ArmorStand each
				: level.getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class,
						new net.minecraft.world.phys.AABB(chair).inflate(1.0))) {
			if (each.isInvisible() && each.isNoGravity()) {
				seat = each;
			}
		}
		if (seat == null) {
			seat = net.minecraft.world.entity.EntityType.ARMOR_STAND.create(
					level, EntitySpawnReason.COMMAND);
			if (seat == null) {
				return;
			}
			seat.snapTo(chair.getX() + 0.5, chair.getY() + 0.2, chair.getZ() + 0.5, 180.0f, 0.0f);
			seat.setInvisible(true);
			seat.setNoGravity(true);
			seat.setInvulnerable(true);
			seat.setSilent(true);
			level.addFreshEntity(seat);
		}
		person.snapTo(chair.getX() + 0.5, chair.getY() + 0.2, chair.getZ() + 0.5, 180.0f, 0.0f);
		person.startRiding(seat, true, true);
	}

	/**
	 * Put somebody somewhere, or leave the one already standing there.
	 *
	 * Run on every join, so a world built before the people existed gets them
	 * without being started again -- and a world that already has them does
	 * not end up with two.
	 */
	public static void place(ServerLevel level, BlockPos where, String name,
			ChatFormatting colour) {
		if (!level.getEntitiesOfClass(Person.class,
				new net.minecraft.world.phys.AABB(where).inflate(3.0),
				person -> person.getCustomName() != null
						&& person.getCustomName().getString().equals(name)).isEmpty()) {
			return;
		}
		Person person = TYPE.create(level, EntitySpawnReason.COMMAND);
		if (person == null) {
			return;
		}
		person.snapTo(where.getX() + 0.5, where.getY(), where.getZ() + 0.5, 0.0f, 0.0f);
		person.setCustomName(Component.literal(name).withStyle(colour));
		person.setCustomNameVisible(true);
		person.setInvulnerable(true);
		person.setPersistenceRequired();
		person.setSilent(true);
		level.addFreshEntity(person);
	}
}
