package com.example;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

/**
 * The jobs themselves: the dishes, the lawn, the bushes, and the three things
 * wrong with your room.
 *
 * Two of them are the same game. The plunger and the moldy food both put a bar
 * on your screen that runs red for five seconds, green for three, and then
 * goes off -- let go while it is green and the job is done, hold on too long
 * and the toilet explodes or the food hits the floor, and either way you are
 * mopping. Press to start, press again to let go.
 *
 * Everything is matched by where it is rather than what it is made of, so the
 * blocks are only there to look like a sink and a fridge.
 */
public final class Chores {
	private Chores() {
	}

	/** A meter someone has running, and the tick it started on. */
	private static final Map<UUID, Long> METER = new HashMap<>();

	/** Which piece of the mess someone is wiping, and the tick they started. */
	private record Wipe(net.minecraft.world.entity.item.ItemEntity piece, long started) {
	}

	private static final Map<UUID, Wipe> WIPING = new HashMap<>();

	/** Three seconds a spot. */
	private static final int WIPE_TICKS = 60;

	/** The food actually lying on the floor, so it can be swept up later. */
	private static final Map<UUID, java.util.List<net.minecraft.world.entity.item.ItemEntity>>
			SPILLED = new HashMap<>();

	/** Which dishes have been washed but not yet dried. */
	private static final Map<UUID, Integer> WASHED = new HashMap<>();

	/** For the mower: the patches of lawn you have already been over. */
	private static final Map<UUID, Set<Long>> MOWED = new HashMap<>();

	private static final int RED_TICKS = 100;      // five seconds
	private static final int GREEN_TICKS = 160;    // then three more

	public static void register() {
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (!(player instanceof ServerPlayer who) || !(world instanceof ServerLevel level)
					|| !ShipLifeMod.isShipLife(level)) {
				return InteractionResult.PASS;
			}
			return rightClick(who, level, Places.local(hit.getBlockPos()), who.getItemInHand(hand));
		});

		AttackBlockCallback.EVENT.register((player, world, hand, pos, side) -> {
			if (!(player instanceof ServerPlayer who) || !(world instanceof ServerLevel level)
					|| !ShipLifeMod.isShipLife(level)) {
				return InteractionResult.PASS;
			}
			return leftClick(who, pos, who.getItemInHand(hand));
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerLevel level : server.getAllLevels()) {
				if (!ShipLifeMod.isShipLife(level)) {
					continue;
				}
				for (ServerPlayer player : level.players()) {
					tickMeter(player);
					tickMop(player);
					tickArrival(player);
				}
			}
		});
	}

	// ------------------------------------------------------------ left clicks

	private static InteractionResult leftClick(ServerPlayer player, BlockPos pos, ItemStack held) {
		if (!Kit.is(held, Kit.SPONGE) || !Quests.on(player, 0, 0)) {
			return InteractionResult.PASS;
		}
		int dish = dishAt(pos);
		if (dish < 0) {
			return InteractionResult.PASS;
		}
		int washed = WASHED.getOrDefault(player.getUUID(), -1);
		if (washed == dish) {
			say(player, "That one is washed. Dry it with the towel.");
			return InteractionResult.SUCCESS;
		}
		if (dish != State.count(player)) {
			say(player, "Work along the row -- dish " + (State.count(player) + 1) + " next.");
			return InteractionResult.SUCCESS;
		}
		WASHED.put(player.getUUID(), dish);
		player.level().playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.6f, 1.4f);
		say(player, "Washed. Now the towel.");
		return InteractionResult.SUCCESS;
	}

	// ----------------------------------------------------------- right clicks

	private static InteractionResult rightClick(ServerPlayer player, ServerLevel level,
			BlockPos pos, ItemStack held) {
		// The Quest Book and the elevator panel come first -- they work anywhere.
		if (Kit.is(held, Kit.QUEST_BOOK)) {
			Book.open(player);
			return InteractionResult.SUCCESS;
		}
		for (int floor = 1; floor <= Places.TOP_FLOOR; floor++) {
			if (pos.equals(Places.panel(floor))) {
				if (Quests.on(player, 2, 0)) {
					Quests.finishPart(player);
				}
				Elevator.open(player);
				return InteractionResult.SUCCESS;
			}
		}

		if (mopping(player)) {
			if (Kit.is(held, Kit.MOP)) {
				if (startWipe(player, pos)) {
					say(player, "Hold it there.");
				} else {
					say(player, "Aim at the mess on the floor.");
				}
			} else {
				say(player, "Clean it up first -- right-click each spot with the mop.");
			}
			return InteractionResult.SUCCESS;
		}

		// --- the dishes -------------------------------------------------------
		int dish = dishAt(pos);
		if (dish >= 0 && Kit.is(held, Kit.TOWEL) && Quests.on(player, 0, 0)) {
			if (WASHED.getOrDefault(player.getUUID(), -1) != dish) {
				say(player, "Wash it with the sponge first.");
				return InteractionResult.SUCCESS;
			}
			WASHED.remove(player.getUUID());
			int done = State.count(player) + 1;
			State.count(player, done);
			level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.6f, 1.6f);
			if (done < 10) {
				say(player, done + " / 10 dishes");
			} else {
				dirtyUp(player);
				say(player, "All ten done. The sponge and towel are filthy -- bin them.");
			}
			return InteractionResult.SUCCESS;
		}

		// --- the garbage, once the dishes are done ----------------------------
		if (pos.equals(Places.GARBAGE) && Quests.on(player, 0, 0) && State.count(player) >= 10) {
			player.getInventory().clearOrCountMatchingItems(
					stack -> Kit.is(stack, "Dirty " + Kit.SPONGE)
							|| Kit.is(stack, "Dirty " + Kit.TOWEL), -1, player.inventoryMenu.getCraftSlots());
			level.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 0.8f, 1.0f);
			Quests.finishPart(player);
			player.getInventory().add(Kit.mower());
			player.getInventory().add(Kit.whacker());
			say(player, "Next door: the lawn.");
			return InteractionResult.SUCCESS;
		}

		// --- the lawn ---------------------------------------------------------
		if (Kit.is(held, Kit.MOWER) && Quests.on(player, 0, 1)) {
			return mow(player, level, pos);
		}

		// --- the weeds --------------------------------------------------------
		if (Kit.is(held, Kit.WHACKER) && Quests.on(player, 0, 1)) {
			for (int i = 0; i < 10; i++) {
				if (pos.equals(Places.weed(i)) || pos.equals(Places.weed(i).below())) {
					if (level.getBlockState(Places.weed(i)).isAir()) {
						return InteractionResult.SUCCESS;
					}
					level.setBlockAndUpdate(Places.weed(i),
							net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
					level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 0.8f, 1.2f);
					Quests.did(player);
					return InteractionResult.SUCCESS;
				}
			}
		}

		// --- the bushes -------------------------------------------------------
		if (Quests.on(player, 0, 2)) {
			for (int i = 0; i < 5; i++) {
				if (pos.equals(Places.bush(i))) {
					if (i == Places.PENNY_BUSH) {
						// It falls out of the bush rather than appearing in your
						// pocket, so finding it is something you watch happen.
						net.minecraft.world.entity.item.ItemEntity dropped =
								new net.minecraft.world.entity.item.ItemEntity(level,
										pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5,
										Kit.penny());
						dropped.setDeltaMovement(0.0, 0.18, 0.0);
						level.addFreshEntity(dropped);
						level.playSound(null, pos, SoundEvents.ITEM_PICKUP,
								SoundSource.PLAYERS, 0.8f, 1.8f);
						say(player, "A penny! Pick it up.");
					} else {
						level.playSound(null, pos, SoundEvents.GRASS_HIT,
								SoundSource.BLOCKS, 0.8f, 1.0f);
						say(player, "Nothing in that one.");
					}
					return InteractionResult.SUCCESS;
				}
			}
		}

		// --- the security desk and Charlie's table ----------------------------
		if (pos.equals(Places.DESK) || pos.equals(Places.DESK.above())) {
			if (Quests.on(player, 1, 1)) {
				player.sendSystemMessage(Component.literal(
						"Security: \"Here is your passport. It opens floor 1 and floor 5 "
						+ "-- floor 5 is yours.\"").withStyle(ChatFormatting.WHITE));
				Quests.finishPart(player);
			} else {
				player.sendSystemMessage(Component.literal("Security: \"Enjoy your stay.\"")
						.withStyle(ChatFormatting.GRAY));
			}
			return InteractionResult.SUCCESS;
		}
		if (pos.equals(Places.TABLE) || pos.equals(Places.TABLE.above())) {
			if (Quests.on(player, 3, 1)) {
				player.sendSystemMessage(Component.literal(
						"Charlie: \"Hi, I am Charlie the manager. I will give you quests to "
						+ "unlock floors, upgrade your passport, get new friends and do "
						+ "activities. I have a quest for you.\"")
						.withStyle(ChatFormatting.WHITE));
				Quests.finishPart(player);
				player.getInventory().add(Kit.plunger());
				player.getInventory().add(Kit.mop());
			} else {
				player.sendSystemMessage(Component.literal("Charlie: \"Keep at it.\"")
						.withStyle(ChatFormatting.GRAY));
			}
			return InteractionResult.SUCCESS;
		}

		// --- looking round your room, quest 3 ---------------------------------
		if (Quests.on(player, 2, 2)) {
			for (int i = 0; i < Places.ROOM_THINGS.length; i++) {
				if (pos.equals(Places.ROOM_THINGS[i])) {
					int found = State.count(player);
					if ((found & (1 << i)) == 0) {
						found |= 1 << i;
						State.count(player, found);
						say(player, Integer.bitCount(found) + " / 4 found");
						if (Integer.bitCount(found) == 4) {
							State.count(player, 0);
							Quests.finishPart(player);
						}
					}
					return InteractionResult.SUCCESS;
				}
			}
		}

		// --- Charlie's quest: the toilet, the bed, the fridge ------------------
		if (pos.equals(Places.TOILET) && Quests.on(player, 4, 0) && Kit.is(held, Kit.PLUNGER)) {
			toggleMeter(player, "toilet");
			return InteractionResult.SUCCESS;
		}
		if (pos.equals(Places.BED) && Quests.on(player, 4, 1)) {
			level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8f, 1.0f);
			Quests.finishPart(player);
			return InteractionResult.SUCCESS;
		}
		if (pos.equals(Places.FRIDGE) && Quests.on(player, 4, 2)) {
			toggleMeter(player, "fridge");
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	// ---------------------------------------------------------------- the bar

	private static final Map<UUID, String> METER_KIND = new HashMap<>();

	/** Press once to start the bar, press again to let go. */
	private static void toggleMeter(ServerPlayer player, String kind) {
		if (METER.containsKey(player.getUUID())) {
			release(player);
			return;
		}
		METER.put(player.getUUID(), player.level().getGameTime());
		METER_KIND.put(player.getUUID(), kind);
		say(player, "Let go while the bar is green.");
	}

	private static void tickMeter(ServerPlayer player) {
		Long started = METER.get(player.getUUID());
		if (started == null) {
			return;
		}
		long held = player.level().getGameTime() - started;
		if (held >= GREEN_TICKS) {
			blowUp(player);
			return;
		}
		Hud.busy(player, 5);
		player.sendOverlayMessage(bar(held));
	}

	/** The bar itself: red while it fills, then green, then gone. */
	private static Component bar(long held) {
		int cells = 20;
		int filled = (int) (held * cells / GREEN_TICKS);
		Component line = Component.literal("");
		for (int i = 0; i < cells; i++) {
			boolean green = i >= cells * RED_TICKS / GREEN_TICKS;
			ChatFormatting colour = i <= filled
					? (green ? ChatFormatting.GREEN : ChatFormatting.RED)
					: ChatFormatting.DARK_GRAY;
			line = line.copy().append(Component.literal("|").withStyle(colour));
		}
		return line.copy().append(Component.literal("  let go on green")
				.withStyle(ChatFormatting.GRAY));
	}

	private static void release(ServerPlayer player) {
		Long started = METER.remove(player.getUUID());
		String kind = METER_KIND.remove(player.getUUID());
		if (started == null) {
			return;
		}
		long held = player.level().getGameTime() - started;
		if (held < RED_TICKS) {
			say(player, "Too early -- the bar was still red.");
			return;
		}
		player.level().playSound(null, player.blockPosition(),
				SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.8f, 1.6f);
		say(player, "fridge".equals(kind) ? "Thrown out." : "Unplugged.");
		Quests.finishPart(player);
	}

	private static void blowUp(ServerPlayer player) {
		String kind = METER_KIND.remove(player.getUUID());
		METER.remove(player.getUUID());
		boolean fridge = "fridge".equals(kind);
		player.level().playSound(null, player.blockPosition(),
				fridge ? SoundEvents.SLIME_BLOCK_FALL : SoundEvents.GENERIC_EXPLODE.value(),
				SoundSource.PLAYERS, 0.9f, 1.0f);
		player.sendSystemMessage(Component.literal(fridge
				? "The food fell on the floor. Mop it up."
				: "It exploded. Mop it up.").withStyle(ChatFormatting.RED));
		spill(player, fridge);
	}

	/**
	 * Put the mess on the floor where you can see it.
	 *
	 * The food that fell is really there -- five pieces of it, scattered in
	 * front of the fridge -- and it cannot be picked up, only mopped. Being
	 * told you made a mess and seeing the mess are not the same thing.
	 */
	private static void spill(ServerPlayer player, boolean fridge) {
		ServerLevel level = (ServerLevel) player.level();
		BlockPos from = Places.onShip(fridge ? Places.FRIDGE : Places.TOILET,
				Places.shipOf(player.getX()));
		java.util.List<net.minecraft.world.entity.item.ItemEntity> dropped =
				new java.util.ArrayList<>();
		java.util.Random random = new java.util.Random();
		for (int i = 0; i < 5; i++) {
			ItemStack piece = Kit.make(
					fridge ? net.minecraft.world.item.Items.ROTTEN_FLESH
							: net.minecraft.world.item.Items.SLIME_BALL,
					fridge ? "Moldy Food" : "Mess",
					ChatFormatting.DARK_GREEN, "Mop it up.");
			net.minecraft.world.entity.item.ItemEntity mess =
					new net.minecraft.world.entity.item.ItemEntity(level,
							from.getX() + 0.5 + (random.nextDouble() - 0.5) * 3,
							from.getY() + 0.4,
							from.getZ() + 0.5 + (random.nextDouble() - 0.5) * 3,
							piece);
			mess.setDeltaMovement((random.nextDouble() - 0.5) * 0.15, 0.12,
					(random.nextDouble() - 0.5) * 0.15);
			// It is rubbish on the floor, not something to put in your pocket.
			mess.setNeverPickUp();
			mess.setUnlimitedLifetime();
			level.addFreshEntity(mess);
			dropped.add(mess);
		}
		SPILLED.put(player.getUUID(), dropped);
	}

	private static boolean mopping(ServerPlayer player) {
		java.util.List<net.minecraft.world.entity.item.ItemEntity> mess =
				SPILLED.get(player.getUUID());
		if (mess == null) {
			return false;
		}
		mess.removeIf(piece -> !piece.isAlive());
		if (mess.isEmpty()) {
			SPILLED.remove(player.getUUID());
			return false;
		}
		return true;
	}

	/**
	 * Mopping: hold right-click on a spot for three seconds and it is gone.
	 *
	 * One spot at a time, five spots to a mess. Walk off in the middle of one
	 * and it stops where it stopped -- the mop has to be over the thing you
	 * are mopping.
	 */
	private static void tickMop(ServerPlayer player) {
		Wipe wipe = WIPING.get(player.getUUID());
		if (wipe == null) {
			return;
		}
		if (!wipe.piece().isAlive() || !Kit.is(player.getMainHandItem(), Kit.MOP)
				|| player.distanceToSqr(wipe.piece()) > 9.0) {
			WIPING.remove(player.getUUID());
			return;
		}
		long held = player.level().getGameTime() - wipe.started();
		if (held < WIPE_TICKS) {
			Hud.busy(player, 5);
			player.sendOverlayMessage(Component.literal("Mopping  " + wiped(held))
					.withStyle(ChatFormatting.GRAY));
			return;
		}
		WIPING.remove(player.getUUID());
		wipe.piece().discard();
		player.level().playSound(null, player.blockPosition(),
				SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 0.5f, 1.4f);
		if (!mopping(player)) {
			player.sendSystemMessage(Component.literal("All clean. Try again.")
					.withStyle(ChatFormatting.GREEN));
		} else {
			say(player, SPILLED.get(player.getUUID()).size() + " spots left.");
		}
	}

	/** A bar that fills over the three seconds. */
	private static String wiped(long held) {
		int cells = 10;
		int done = (int) (held * cells / WIPE_TICKS);
		return "|".repeat(Math.max(0, done)) + ".".repeat(Math.max(0, cells - done));
	}

	/** Start on whichever piece of the mess is under where you clicked. */
	private static boolean startWipe(ServerPlayer player, BlockPos pos) {
		java.util.List<net.minecraft.world.entity.item.ItemEntity> mess =
				SPILLED.get(player.getUUID());
		if (mess == null) {
			return false;
		}
		for (net.minecraft.world.entity.item.ItemEntity piece : mess) {
			if (piece.isAlive() && piece.blockPosition().closerThan(pos, 2.0)) {
				WIPING.put(player.getUUID(), new Wipe(piece, player.level().getGameTime()));
				return true;
			}
		}
		return false;
	}

	/** The mop got it: take the food off the floor. */
	private static void sweepUp(ServerPlayer player) {
		java.util.List<net.minecraft.world.entity.item.ItemEntity> dropped =
				SPILLED.remove(player.getUUID());
		if (dropped == null) {
			return;
		}
		for (net.minecraft.world.entity.item.ItemEntity mess : dropped) {
			if (mess.isAlive()) {
				mess.discard();
			}
		}
	}

	// -------------------------------------------------------------- the mower

	/** Ten patches of the lawn, one right click each, is the lawn mowed. */
	private static InteractionResult mow(ServerPlayer player, ServerLevel level, BlockPos pos) {
		if (!Places.onLawn(pos)
				|| !level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
			say(player, "Mow the lawn inside the second house.");
			return InteractionResult.SUCCESS;
		}
		if (!MOWED.computeIfAbsent(player.getUUID(), who -> new HashSet<>()).add(pos.asLong())) {
			return InteractionResult.SUCCESS;          // already been over this one
		}
		// Cut grass goes to moss, so the yard shows how much is left to do.
		level.setBlockAndUpdate(pos,
				net.minecraft.world.level.block.Blocks.MOSS_BLOCK.defaultBlockState());
		level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 0.6f, 0.8f);
		Quests.did(player);
		return InteractionResult.SUCCESS;
	}

	// ------------------------------------------------------- getting somewhere

	/** Some parts are just "be there" -- boarding the ship, reaching a floor. */
	private static void tickArrival(ServerPlayer player) {
		if (player.tickCount % 10 != 0) {
			return;
		}
		if (Quests.on(player, 1, 0)
				&& player.blockPosition().closerThan(Places.DOOR, 3.0)) {
			Quests.finishPart(player);
		}
		// The penny counts once it is actually in your pocket.
		if (Quests.on(player, 0, 2) && hasPenny(player)) {
			Quests.finishPart(player);
		}
	}

	/**
	 * How the lawn is coming along, counted off the yard rather than kept in a
	 * number somewhere.
	 *
	 * A cut square is moss and a pulled weed is air, so the yard already knows
	 * both answers, and they stay right across a restart without being saved.
	 */
	public static String lawnLine(ServerLevel level) {
		int mowed = 0;
		BlockPos.MutableBlockPos square = new BlockPos.MutableBlockPos();
		for (int dx = -Places.LAWN_REACH; dx <= Places.LAWN_REACH; dx++) {
			for (int dz = -Places.LAWN_REACH; dz <= Places.LAWN_REACH; dz++) {
				square.set(Places.HOUSE_TWO.getX() + dx, Places.GROUND,
						Places.HOUSE_TWO.getZ() + dz);
				if (level.getBlockState(square).is(
						net.minecraft.world.level.block.Blocks.MOSS_BLOCK)) {
					mowed++;
				}
			}
		}
		int weeds = 0;
		for (int i = 0; i < 10; i++) {
			if (level.getBlockState(Places.weed(i)).isAir()) {
				weeds++;
			}
		}
		return "Mowed " + mowed + " / " + Places.LAWN_SQUARES
				+ "  ·  Weeds " + weeds + " / 10";
	}

	// ------------------------------------------------------------------ small

	/** Is the penny in their pockets? */
	private static boolean hasPenny(ServerPlayer player) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			if (Kit.is(player.getInventory().getItem(slot), Kit.PENNY)) {
				return true;
			}
		}
		return false;
	}

	private static int dishAt(BlockPos pos) {
		for (int i = 0; i < 10; i++) {
			if (pos.equals(Places.dish(i))) {
				return i;
			}
		}
		return -1;
	}

	private static void dirtyUp(ServerPlayer player) {
		player.getInventory().clearOrCountMatchingItems(
				stack -> Kit.is(stack, Kit.SPONGE) || Kit.is(stack, Kit.TOWEL),
				-1, player.inventoryMenu.getCraftSlots());
		player.getInventory().add(Kit.make(Made.sponge,
				"Dirty " + Kit.SPONGE, ChatFormatting.DARK_GRAY, "Bin it."));
		player.getInventory().add(Kit.make(Made.towel,
				"Dirty " + Kit.TOWEL, ChatFormatting.DARK_GRAY, "Bin it."));
	}

	private static void say(ServerPlayer player, String text) {
		Hud.busy(player, 30);
		player.sendOverlayMessage(Component.literal(text).withStyle(ChatFormatting.YELLOW));
	}
}
