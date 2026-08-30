package com.example;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Fishing off the balcony.
 *
 * There is water sunk into the deck out there and an ordinary rod pulls
 * ordinary fish out of it, because that is Minecraft's own fishing and it
 * works. What this adds is the ship's part: stand out there with a rod in the
 * water and every so often something worth having comes up as well -- event
 * tickets, mostly, and once in a while a plate from the buffet.
 */
public final class Fishing {
	private Fishing() {
	}

	/** How often a cast can pay, in ticks. */
	private static final int EVERY = 200;

	/** What a catch is worth. */
	private static final int PAYS = 8;

	private static final java.util.Map<java.util.UUID, Long> LAST =
			new java.util.HashMap<>();

	private static final java.util.Random RANDOM = new java.util.Random();

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 40 != 0) {
				return;
			}
			for (ServerLevel level : server.getAllLevels()) {
				if (!ShipLifeMod.isShipLife(level)) {
					continue;
				}
				for (ServerPlayer player : level.players()) {
					if (player.fishing != null && near(player)) {
						bite(player, level);
					}
				}
			}
		});
	}

	/** Is their float in the ship's water rather than the pool? */
	private static boolean near(ServerPlayer player) {
		return player.fishing != null
				&& player.fishing.distanceToSqr(
						Places.FISHING.getX() + 0.5, Places.FISHING.getY() + 0.5,
						Places.FISHING.getZ() + 0.5) < 36.0;
	}

	private static void bite(ServerPlayer player, ServerLevel level) {
		long now = level.getGameTime();
		Long last = LAST.get(player.getUUID());
		if (last != null && now - last < EVERY) {
			return;
		}
		LAST.put(player.getUUID(), now);

		level.playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_SPLASH,
				SoundSource.PLAYERS, 0.7f, 1.4f);

		// One cast in eight brings up dinner instead of tickets.
		if (RANDOM.nextInt(8) == 0) {
			if (!player.getInventory().add(Kit.meal())) {
				player.drop(Kit.meal(), false);
			}
			player.sendSystemMessage(Component.literal(
					"Something the kitchen will take off you -- a plate's worth.")
					.withStyle(ChatFormatting.GOLD));
			return;
		}
		Events.payTickets(player, PAYS, "you caught something off the balcony");
	}
}
