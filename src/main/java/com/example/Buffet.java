package com.example;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

	/** How long the cook makes you wait before another plate: fifteen seconds. */
	private static final int WAIT = 300;

	/** When each player was last served. */
	private static final Map<UUID, Long> SERVED = new HashMap<>();

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

	private static void serve(ServerPlayer player) {
		long now = player.level().getGameTime();
		Long last = SERVED.get(player.getUUID());
		if (last != null && now - last < WAIT) {
			say(player, "\"Finish what you have got and come back.\"");
			return;
		}
		SERVED.put(player.getUUID(), now);

		player.getInventory().add(Kit.meal());
		player.level().playSound(null, player.blockPosition(),
				SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.7f, 1.2f);
		say(player, "\"There you go -- sit down and eat it.\"");
	}

	private static void say(ServerPlayer player, String text) {
		player.sendSystemMessage(Component.literal("The cook: ")
				.withStyle(ChatFormatting.GRAY)
				.append(Component.literal(text).withStyle(ChatFormatting.YELLOW)));
	}
}
