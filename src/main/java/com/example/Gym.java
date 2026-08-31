package com.example;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The gym, in the corner of floor 3.
 *
 * Three anvils to lift, and lifting is holding right-click on one. Ten lifts
 * is a heart, and a heart is yours for good -- the count is saved on you and
 * put back on every time you log in, so it survives dying, reloading and the
 * ship being rebuilt around you.
 *
 * Ten hearts is as far as it goes. Twenty hearts of your own on top of Ben's
 * armour would make floor 10 a walk.
 */
public final class Gym {
	private Gym() {
	}

	/** Lifts per heart, and the most hearts the gym will give you. */
	public static final int PER_HEART = 10;
	public static final int MOST_HEARTS = 10;

	/** How many lifts you have done, all told. */
	public static final AttachmentType<Integer> LIFTS =
			AttachmentRegistry.<Integer>builder()
					.initializer(() -> 0)
					.persistent(Codec.INT)
					.copyOnDeath()
					.buildAndRegister(ShipLifeMod.id("gym_lifts"));

	private static final net.minecraft.resources.Identifier EXTRA =
			ShipLifeMod.id("gym_hearts");

	public static void register() {
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (!(player instanceof ServerPlayer who) || !(world instanceof ServerLevel level)
					|| !ShipLifeMod.isShipLife(level)) {
				return InteractionResult.PASS;
			}
			BlockPos pos = Places.local(hit.getBlockPos());
			if (!pos.equals(Places.GYM) && !pos.equals(Places.GYM.west(2))
					&& !pos.equals(Places.GYM.north(2))) {
				return InteractionResult.PASS;
			}
			lift(who, level);
			return InteractionResult.SUCCESS;
		});
	}

	/** One lift. */
	private static void lift(ServerPlayer player, ServerLevel level) {
		int done = State.tally(player, LIFTS) + 1;
		player.setAttached(LIFTS, done);
		level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND,
				SoundSource.BLOCKS, 0.4f, 1.6f);

		if (done % PER_HEART == 0 && hearts(player) < MOST_HEARTS) {
			apply(player);
			player.setHealth(player.getMaxHealth());
			player.sendSystemMessage(Component.literal("That is another heart. You have "
					+ (int) (player.getMaxHealth() / 2) + " now.")
					.withStyle(ChatFormatting.LIGHT_PURPLE));
			return;
		}
		Hud.busy(player, 20);
		player.sendOverlayMessage(Component.literal("Lift " + done + "  --  "
				+ (PER_HEART - done % PER_HEART) + " to the next heart  ·  "
				+ hearts(player) + "/" + MOST_HEARTS + " hearts")
				.withStyle(ChatFormatting.GRAY));

		// The board on the wall: what you have done, and what is left.
		if (done % 25 == 0) {
			player.sendSystemMessage(Component.literal(done + " lifts. "
					+ (hearts(player) >= MOST_HEARTS
							? "Nobody on this ship has done more."
							: (MOST_HEARTS - hearts(player)) + " hearts still in it."))
					.withStyle(ChatFormatting.LIGHT_PURPLE));
			Log.write(player, done + " lifts in the gym");
		}
	}

	/** How many hearts the gym has given them. */
	public static int hearts(ServerPlayer player) {
		return Math.min(MOST_HEARTS, State.tally(player, LIFTS) / PER_HEART);
	}

	/**
	 * Put the hearts back on.
	 *
	 * Called on every join, because an attribute modifier is not saved with
	 * you -- only the count of lifts is, and this is what turns that count
	 * back into hearts.
	 */
	public static void apply(ServerPlayer player) {
		var health = player.getAttribute(Attributes.MAX_HEALTH);
		if (health == null) {
			return;
		}
		health.removeModifier(EXTRA);
		int hearts = hearts(player);
		if (hearts <= 0) {
			return;
		}
		health.addPermanentModifier(new AttributeModifier(EXTRA, hearts * 2.0,
				AttributeModifier.Operation.ADD_VALUE));
	}
}
