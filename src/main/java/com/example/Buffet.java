package com.example;


import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

/**
 * Floor 4: the buffet.
 *
 * You do not help yourself. There is a cook behind the counter and you ask him
 * for a plate, the same way you ask security for a passport -- the ship's
 * people are all a block you right-click, so the buffet has one too.
 *
 * Eating is what puts the hearts back rather than the asking, so the cook hands
 * over food and the food does the work. What he will not do is hand out another
 * plate the moment you have one, because a buffet you can empty in a second is
 * a free stack of beef and not a buffet.
 */
public final class Buffet {
	private Buffet() {
	}

	public static void register() {
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			// One click is one click: the off hand is a second helping of the
			// same press, and would take a plate off you for nothing.
			if (!(player instanceof ServerPlayer who) || !(world instanceof ServerLevel level)
					|| !ShipLifeMod.isShipLife(level) || hand != InteractionHand.MAIN_HAND) {
				return InteractionResult.PASS;
			}
			BlockPos pos = Places.local(hit.getBlockPos());
			if (!pos.equals(Places.BUFFET_COOK) && !pos.equals(Places.BUFFET_COOK.above())) {
				return InteractionResult.PASS;
			}
			serve(who);
			return InteractionResult.SUCCESS;
		});

		// The plate is food, and Minecraft will not let you eat food on a full
		// stomach -- which is exactly when you want it, because your hearts
		// and your hunger bar are two different things. So the plate is eaten
		// here instead of by the game, and it works whatever state you are in.
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (isFresh(player.getItemInHand(hand))) {
				if (player instanceof ServerPlayer who) {
					eatFresh(who, who.getItemInHand(hand));
				}
				return InteractionResult.SUCCESS;
			}
			if (!Kit.is(player.getItemInHand(hand), Kit.MEAL)) {
				return InteractionResult.PASS;
			}
			if (player instanceof ServerPlayer who) {
				eat(who, who.getItemInHand(hand));
			}
			return InteractionResult.SUCCESS;
		});

		// Right-clicking at a block goes to the block first, so the plate has
		// to be caught there as well or it only works pointed at the sky.
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (hand != InteractionHand.MAIN_HAND
					|| !Kit.is(player.getItemInHand(hand), Kit.MEAL)) {
				return InteractionResult.PASS;
			}
			if (player instanceof ServerPlayer who) {
				eat(who, who.getItemInHand(hand));
			}
			return InteractionResult.SUCCESS;
		});
	}

	/** Has this been in your fridge? */
	private static boolean isFresh(net.minecraft.world.item.ItemStack stack) {
		net.minecraft.network.chat.Component named =
				stack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
		return named != null && named.getString().startsWith(Fridge.FRESH + " ");
	}

	/** Anything out of the fridge: what it was worth, and six hearts on top. */
	private static void eatFresh(ServerPlayer player, net.minecraft.world.item.ItemStack food) {
		food.shrink(1);
		player.heal(Fridge.FRESH_HEALS);
		player.getFoodData().eat(6, 0.6f);
		player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_BURP,
				SoundSource.PLAYERS, 0.6f, 1.1f);
		player.sendSystemMessage(Component.literal("Straight out of the fridge.")
				.withStyle(ChatFormatting.AQUA));
	}

	/** One helping: full hearts, a full hunger bar, and the plate goes down one. */
	private static void eat(ServerPlayer player, net.minecraft.world.item.ItemStack plate) {
		plate.shrink(1);
		player.setHealth(player.getMaxHealth());
		player.getFoodData().eat(8, 0.8f);
		player.level().playSound(null, player.blockPosition(),
				SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.6f, 1.0f);
		player.sendSystemMessage(Component.literal(
				"You eat a plate from the buffet. Hearts full.")
				.withStyle(ChatFormatting.GREEN));
	}

	/** What the cook will make you, and what each one does. */
	public record Dish(String name, String what, int heals, String effect) {
	}

	public static final Dish[] MENU = {
			new Dish("The Full Plate", "Three helpings. Hearts back to full.", 20, ""),
			new Dish("Fish Supper", "Off the balcony this morning.", 8, "swim"),
			new Dish("Steak and Chips", "What you want before floor 9.", 10, "strong"),
			new Dish("Something Sweet", "Ten minutes of not being hungry.", 6, "quick"),
	};

	/** The cook's board: pick a dish rather than take whatever is going. */
	public static void order(ServerPlayer player) {
		net.minecraft.world.SimpleContainer page = Comforts.blank();
		page.setItem(4, Book.entry(net.minecraft.world.item.Items.COOKED_BEEF,
				"The Cook", ChatFormatting.GOLD,
				"\"Tell me what you want and I will make it.\"",
				"Everything here is free. It is a buffet."));
		for (int i = 0; i < MENU.length; i++) {
			page.setItem(20 + i, Book.entry(icon(i), MENU[i].name(), ChatFormatting.WHITE,
					MENU[i].what(), "", "Click to order it."));
		}
		page.setItem(49, Book.entry(net.minecraft.world.item.Items.BARRIER, "Close",
				ChatFormatting.RED, "Press Escape."));
		player.openMenu(new net.minecraft.world.SimpleMenuProvider(
				(id, inventory, who) -> new ReadOnlyMenu(id, inventory, page, Buffet::cook),
				Component.literal("The Cook")));
	}

	private static net.minecraft.world.item.Item icon(int dish) {
		return switch (dish) {
			case 1 -> net.minecraft.world.item.Items.COOKED_SALMON;
			case 2 -> net.minecraft.world.item.Items.COOKED_BEEF;
			case 3 -> net.minecraft.world.item.Items.CAKE;
			default -> net.minecraft.world.item.Items.BREAD;
		};
	}

	private static void cook(ServerPlayer player, int slot) {
		if (slot == 49) {
			player.closeContainer();
			return;
		}
		int index = slot - 20;
		if (index < 0 || index >= MENU.length) {
			return;
		}
		Dish dish = MENU[index];
		player.heal(dish.heals());
		player.getFoodData().eat(10, 1.0f);
		switch (dish.effect()) {
			case "swim" -> player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
					net.minecraft.world.effect.MobEffects.DOLPHINS_GRACE, 1200, 0));
			case "strong" -> player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
					net.minecraft.world.effect.MobEffects.STRENGTH, 1200, 0));
			case "quick" -> player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
					net.minecraft.world.effect.MobEffects.SPEED, 1200, 0));
			default -> {
			}
		}
		if (index == 0) {
			player.getInventory().add(Kit.meal());
		}
		say(player, "\"" + dish.name() + ". There you go.\"");
		player.closeContainer();
	}

	/**
	 * A plate, whenever you ask for one.
	 *
	 * There was a fifteen second wait on this, which only ever meant standing
	 * in front of a buffet not being served. It is a buffet.
	 */
	public static void serve(ServerPlayer player) {
		player.getInventory().add(Kit.meal());
		player.level().playSound(null, player.blockPosition(),
				SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.7f, 1.2f);
		say(player, "\"There you go -- sit down and eat it.\"");
	}

	/** Have you still got a plate on you? */
	private static boolean carrying(ServerPlayer player) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			if (Kit.is(player.getInventory().getItem(slot), Kit.MEAL)) {
				return true;
			}
		}
		return false;
	}

	private static void say(ServerPlayer player, String text) {
		player.sendSystemMessage(Component.literal("The cook: ")
				.withStyle(ChatFormatting.GRAY)
				.append(Component.literal(text).withStyle(ChatFormatting.YELLOW)));
	}
}
