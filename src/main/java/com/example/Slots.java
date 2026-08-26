package com.example;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The two slots that are not yours.
 *
 * Slot 9 is the Quest Book and slot 8 is the passport, and neither can be
 * dropped, spent or lost -- put something else there and it is moved aside
 * rather than eaten, and the book comes straight back. Losing the book would
 * lose the game, so it simply cannot happen.
 *
 * The passport only appears once security has handed it to you.
 */
public final class Slots {
	private Slots() {
	}

	/** Counting from zero, so slot 9 on the hotbar. */
	public static final int BOOK_SLOT = 8;

	/** Slot 8 on the hotbar. */
	public static final int PASSPORT_SLOT = 7;

	/** Checking once a second is plenty. */
	private static final int EVERY = 20;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % EVERY != 0) {
				return;
			}
			for (ServerLevel level : server.getAllLevels()) {
				if (!ShipLifeMod.isShipLife(level)) {
					continue;
				}
				for (ServerPlayer player : level.players()) {
					keep(player, BOOK_SLOT, Kit.QUEST_BOOK, Kit.questBook());
					if (State.hasFloor(player, 1)) {
						keep(player, PASSPORT_SLOT, Kit.PASSPORT, Kit.passport());
					}
				}
			}
		});
	}

	/** Make sure the right thing is in the slot, without losing what was. */
	private static void keep(ServerPlayer player, int slot, String name, ItemStack want) {
		ItemStack there = player.getInventory().getItem(slot);
		if (Kit.is(there, name)) {
			return;
		}
		if (!there.isEmpty() && !player.getInventory().add(there.copy())) {
			player.drop(there.copy(), false);
		}
		player.getInventory().setItem(slot, want);
	}
}
