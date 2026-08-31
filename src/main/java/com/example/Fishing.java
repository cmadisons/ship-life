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

	/** What comes up that nobody wanted. */
	private static net.minecraft.world.item.ItemStack junk() {
		return switch (RANDOM.nextInt(6)) {
			case 0 -> Kit.make(net.minecraft.world.item.Items.LEATHER_BOOTS,
					"A Wet Boot", ChatFormatting.GRAY, "Off the bottom of the pond.");
			case 1 -> Kit.make(net.minecraft.world.item.Items.STICK,
					"A Bit of Rail", ChatFormatting.GRAY, "Off the track on 6, probably.");
			case 2 -> Kit.make(net.minecraft.world.item.Items.PAPER,
					"A Soggy Ticket", ChatFormatting.GRAY, "Nobody is honouring this.");
			case 3 -> Kit.make(net.minecraft.world.item.Items.BOWL,
					"A Bowl from the Buffet", ChatFormatting.GRAY, "The cook will want it back.");
			case 4 -> Kit.make(net.minecraft.world.item.Items.STRING,
					"Tangled Line", ChatFormatting.GRAY, "Somebody else's, once.");
			default -> Kit.make(net.minecraft.world.item.Items.LILY_PAD,
					"Pond Weed", ChatFormatting.GRAY, "It grows fast out here.");
		};
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

		// One cast in five brings up something nobody wanted. It is a ship in
		// space with a pond on the balcony; of course there is junk in it.
		if (RANDOM.nextInt(5) == 0) {
			net.minecraft.world.item.ItemStack junk = junk();
			if (!player.getInventory().add(junk)) {
				player.drop(junk, false);
			}
			player.sendSystemMessage(Component.literal("You pull up "
					+ junk.getHoverName().getString() + ". Somebody's, once.")
					.withStyle(ChatFormatting.GRAY));
			return;
		}

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
