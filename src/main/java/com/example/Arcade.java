package com.example;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

/**
 * Floor 2: the arcade.
 *
 * Three cabinets along the far wall and a prize counter facing them. Walk up
 * to a cabinet and right-click it to play; what it pays is what the machine
 * pays on the real thing -- a ticket per food on Snake, five a round on
 * Galaga, five for a new record on Pac-Man.
 */
public final class Arcade {
	private Arcade() {
	}

	public static void register() {
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (!(player instanceof ServerPlayer who) || !(world instanceof ServerLevel level)
					|| !ShipLifeMod.isShipLife(level)) {
				return InteractionResult.PASS;
			}
			BlockPos pos = hit.getBlockPos();
			BlockPos base = level.getBlockState(pos.below()).isAir() ? pos : pos.below();

			if (matches(pos, base, Places.SNAKE)) {
				new Snake(who).open();
				return InteractionResult.SUCCESS;
			}
			if (matches(pos, base, Places.PACMAN)) {
				new PacMan(who).open();
				return InteractionResult.SUCCESS;
			}
			if (matches(pos, base, Places.GALAGA)) {
				new Galaga(who).open();
				return InteractionResult.SUCCESS;
			}
			if (matches(pos, base, Places.PRIZES)) {
				prizes(who);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});
	}

	/** A cabinet is two blocks tall, so either half of it counts. */
	private static boolean matches(BlockPos hit, BlockPos base, BlockPos machine) {
		return hit.equals(machine) || hit.equals(machine.above()) || base.equals(machine);
	}

	/** The prize counter. Nothing to spend on yet -- that comes next. */
	private static void prizes(ServerPlayer player) {
		player.sendSystemMessage(Component.literal("Prize counter -- you have "
				+ State.arcade(player) + " arcade tickets.")
				.withStyle(ChatFormatting.GOLD));
		player.sendSystemMessage(Component.literal(
				"10 tickets buys a pet and 25 buys the next 3 quests. "
				+ "Neither is built yet -- keep your tickets.")
				.withStyle(ChatFormatting.GRAY));
	}
}
