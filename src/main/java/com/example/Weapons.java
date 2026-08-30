package com.example;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;

/**
 * Floor 17: the weapon store, and what it sells.
 *
 * Four things. A sword and a bow, which are a sword and a bow, and the
 * teleport pair, which are the reason to come up here: the sword puts you
 * where you are looking, and the bow puts you where the arrow lands.
 *
 * You cannot reach this floor without the whole plant set on, so everything
 * here is priced for somebody who has been to floor 10 and back.
 */
public final class Weapons {
	private Weapons() {
	}

	public static final String SWORD = "Sword";
	public static final String TELEPORT_SWORD = "Teleport Sword";
	public static final String BOW = "Bow";
	public static final String TELEPORT_BOW = "Teleport Bow";

	public static final int SWORD_COST = 100;
	public static final int TELEPORT_SWORD_COST = 250;
	public static final int BOW_COST = 100;
	public static final int TELEPORT_BOW_COST = 250;

	/** How far either of them will throw you. */
	private static final double REACH = 32.0;

	/** What a teleport costs, what you can hold, and what comes back a second. */
	public static final int MANA_COST = 25;
	public static final int MANA_MAX = 100;
	public static final int MANA_BACK = 4;

	// ------------------------------------------------------------- the stock

	public static ItemStack sword() {
		return Kit.make(Items.IRON_SWORD, SWORD, ChatFormatting.WHITE,
				"A sword. It does what a sword does.");
	}

	public static ItemStack teleportSword() {
		ItemStack made = Kit.make(Items.DIAMOND_SWORD, TELEPORT_SWORD,
				ChatFormatting.LIGHT_PURPLE,
				"Right-click and you are where you are pointing.",
				MANA_COST + " mana a throw.");
		made.set(net.minecraft.core.component.DataComponents.UNBREAKABLE,
				net.minecraft.util.Unit.INSTANCE);
		return made;
	}

	public static ItemStack bow() {
		return Kit.make(Items.BOW, BOW, ChatFormatting.WHITE,
				"A bow. Arrows are yours to find.");
	}

	public static ItemStack teleportBow() {
		ItemStack made = Kit.make(Items.BOW, TELEPORT_BOW, ChatFormatting.LIGHT_PURPLE,
				"Right-click and you are where you are pointing.",
				MANA_COST + " mana a throw.");
		made.set(net.minecraft.core.component.DataComponents.UNBREAKABLE,
				net.minecraft.util.Unit.INSTANCE);
		return made;
	}

	// -------------------------------------------------------------- the shop

	/** The counter on floor 17. */
	public static void shop(ServerPlayer player) {
		SimpleContainer page = Comforts.blank();
		page.setItem(4, Book.entry(Items.SMITHING_TABLE, "The Weapon Store",
				ChatFormatting.AQUA,
				"You got up here in the whole set.",
				"You have " + State.event(player) + " event tickets."));

		page.setItem(20, Book.entry(Items.IRON_SWORD, SWORD, ChatFormatting.WHITE,
				"A sword.", SWORD_COST + " event tickets", "Click to buy."));
		page.setItem(22, Book.entry(Items.DIAMOND_SWORD, TELEPORT_SWORD,
				ChatFormatting.LIGHT_PURPLE,
				"Right-click and you are where you were looking.",
				TELEPORT_SWORD_COST + " event tickets", "Click to buy."));
		page.setItem(29, Book.entry(Items.BOW, BOW, ChatFormatting.WHITE,
				"A bow, and thirty-two arrows.",
				BOW_COST + " event tickets", "Click to buy."));
		page.setItem(31, Book.entry(Items.SPECTRAL_ARROW, TELEPORT_BOW,
				ChatFormatting.LIGHT_PURPLE,
				"Right-click and you are where you are pointing.",
				MANA_COST + " mana a throw. Arrows with it.",
				TELEPORT_BOW_COST + " event tickets", "Click to buy."));

		page.setItem(49, Book.entry(Items.BARRIER, "Close", ChatFormatting.RED,
				"Press Escape."));
		player.openMenu(new SimpleMenuProvider(
				(id, inventory, who) -> new ReadOnlyMenu(id, inventory, page, Weapons::buy),
				Component.literal("Weapon Store")));
	}

	private static void buy(ServerPlayer player, int slot) {
		int cost = switch (slot) {
			case 20 -> SWORD_COST;
			case 22 -> TELEPORT_SWORD_COST;
			case 29 -> BOW_COST;
			case 31 -> TELEPORT_BOW_COST;
			default -> 0;
		};
		if (slot == 49) {
			player.closeContainer();
			return;
		}
		if (cost == 0) {
			return;
		}
		if (State.event(player) < cost) {
			player.sendSystemMessage(Component.literal("That is " + cost
					+ " event tickets and you have " + State.event(player) + ".")
					.withStyle(ChatFormatting.RED));
			return;
		}
		State.spendEvent(player, cost);

		switch (slot) {
			case 20 -> give(player, sword());
			case 22 -> give(player, teleportSword());
			case 29 -> {
				give(player, bow());
				give(player, new ItemStack(Items.ARROW, 32));
			}
			default -> {
				give(player, teleportBow());
				give(player, new ItemStack(Items.ARROW, 32));
			}
		}
		player.closeContainer();
	}

	private static void give(ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
	}

	// --------------------------------------------------------- what they do

	public static void register() {
		// The sword: where you are looking is where you go.
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (!Kit.is(player.getItemInHand(hand), TELEPORT_SWORD)) {
				return InteractionResult.PASS;
			}
			if (player instanceof ServerPlayer who && world instanceof ServerLevel level) {
				throwThem(who, level);
			}
			return InteractionResult.SUCCESS;
		});
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (!Kit.is(player.getItemInHand(hand), TELEPORT_SWORD)) {
				return InteractionResult.PASS;
			}
			if (player instanceof ServerPlayer who && world instanceof ServerLevel level) {
				throwThem(who, level);
			}
			return InteractionResult.SUCCESS;
		});

		// The bow teleports too. Both of them cost mana, which is the reason
		// you cannot simply cross the ship whenever you feel like it.
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (!Kit.is(player.getItemInHand(hand), TELEPORT_BOW)) {
				return InteractionResult.PASS;
			}
			if (player instanceof ServerPlayer who && world instanceof ServerLevel level) {
				throwThem(who, level);
			}
			return InteractionResult.SUCCESS;
		});
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (!Kit.is(player.getItemInHand(hand), TELEPORT_BOW)) {
				return InteractionResult.PASS;
			}
			if (player instanceof ServerPlayer who && world instanceof ServerLevel level) {
				throwThem(who, level);
			}
			return InteractionResult.SUCCESS;
		});

		// Mana comes back on its own, a little at a time.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) {
				return;
			}
			for (ServerLevel level : server.getAllLevels()) {
				if (!ShipLifeMod.isShipLife(level)) {
					continue;
				}
				for (ServerPlayer player : level.players()) {
					if (State.tally(player, State.MANA) < MANA_MAX) {
						State.add(player, State.MANA, MANA_BACK);
						if (State.tally(player, State.MANA) > MANA_MAX) {
							player.setAttached(State.MANA, MANA_MAX);
						}
					}
					openEighteen(player);
				}
			}
		});
	}

	/**
	 * Floor 18 opens when you are carrying a sword and a bow.
	 *
	 * Either sword and either bow: the plain ones off the counter downstairs
	 * count, so it is a floor you get to by kitting yourself out rather than
	 * by spending the most.
	 */
	public static void openEighteen(ServerPlayer player) {
		if (State.hasFloor(player, 18)) {
			return;
		}
		boolean sword = false;
		boolean bow = false;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			sword = sword || Kit.is(stack, SWORD) || Kit.is(stack, TELEPORT_SWORD);
			bow = bow || Kit.is(stack, BOW) || Kit.is(stack, TELEPORT_BOW);
		}
		if (!sword || !bow) {
			return;
		}
		State.unlock(player, 18);
		player.sendSystemMessage(Component.literal(
				"A sword and a bow. Floor 18 is open -- there is a portal up there.")
				.withStyle(ChatFormatting.AQUA));
	}

	/**
	 * Put them wherever they are pointing, for twenty-five mana.
	 *
	 * Wherever the crosshair is is where you go -- a wall across the room, a
	 * floor below a railing, the far side of the boss room. Mana is what
	 * stops that being a way of never walking anywhere again: four back a
	 * second, a hundred held, twenty-five a throw.
	 */
	private static void throwThem(ServerPlayer player, ServerLevel level) {
		int mana = State.tally(player, State.MANA);
		if (mana < MANA_COST) {
			Hud.busy(player, 20);
			player.sendOverlayMessage(Component.literal("Not enough mana  --  "
					+ mana + " / " + MANA_MAX).withStyle(ChatFormatting.DARK_AQUA));
			return;
		}
		HitResult sight = player.pick(REACH, 0.0f, false);
		if (sight.getType() == HitResult.Type.MISS) {
			player.sendSystemMessage(Component.literal("Nothing that way to land on.")
					.withStyle(ChatFormatting.GRAY));
			return;
		}
		State.add(player, State.MANA, -MANA_COST);
		Hud.busy(player, 20);
		player.sendOverlayMessage(Component.literal("Mana  "
				+ State.tally(player, State.MANA) + " / " + MANA_MAX)
				.withStyle(ChatFormatting.DARK_AQUA));

		BlockPos landing = BlockPos.containing(sight.getLocation()).above();
		if (!level.getBlockState(landing).isAir()) {
			landing = landing.above();
		}
		level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
				SoundSource.PLAYERS, 0.8f, 1.4f);
		player.teleportTo(landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5);
		level.playSound(null, landing, SoundEvents.ENDERMAN_TELEPORT,
				SoundSource.PLAYERS, 0.8f, 1.0f);
	}

}
