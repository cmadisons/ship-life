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

	/** The names, which are also which skin each one wears. */
	public static final String CHARLIE = "Charlie";
	public static final String BEN = "Ben";
	public static final String IZZY = "Izzy";
	public static final String DESK = "Maria";
	public static final String LOBBY = "Sam";
	public static final String COOK = "Gus";

	/**
	 * Everyone, in their place.
	 *
	 * Run on every join. Anybody already standing where they should be is
	 * left alone, so this can be called as often as it likes.
	 */
	public static void everyone(ServerLevel level) {
		place(level, Places.TABLE.north(), CHARLIE, ChatFormatting.YELLOW);
		place(level, Places.DESK.north(), DESK, ChatFormatting.WHITE);
		place(level, Places.DOOR.east(2), LOBBY, ChatFormatting.WHITE);
		place(level, Places.BEN.south(), BEN, ChatFormatting.AQUA);
		place(level, Places.IZZY.south(), IZZY, ChatFormatting.AQUA);
		place(level, Places.BUFFET_COOK.east(), COOK, ChatFormatting.GOLD);
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
