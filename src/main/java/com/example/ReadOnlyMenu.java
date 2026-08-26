package com.example;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/**
 * A chest screen whose contents are buttons, not items.
 *
 * The pages are built out of ordinary items -- a diamond sword for Skills, a
 * bone for Pets -- purely so they can be drawn as icons. Without this you could
 * pick them up and walk off with a free diamond sword, and shift-clicking your
 * own things in would lose them.
 *
 * Two doors have to be shut, not one:
 *
 *   clicked()         nothing in the page can be picked up, put down or
 *                     swapped; instead the click is handed to the page as a
 *                     button press
 *
 *   quickMoveStack()  shift-clicking doesn't go through clicked(), so it is
 *                     blocked separately, in both directions
 */
public class ReadOnlyMenu extends ChestMenu {
	/** Slots 0-53 are the page; anything above is the player's own bag. */
	private static final int PAGE_SLOTS = 54;

	/** What to do when a slot in the page is clicked. */
	public interface OnClick {
		void press(ServerPlayer player, int slot);
	}

	private final OnClick handler;

	public ReadOnlyMenu(int id, Inventory playerInventory, Container page, OnClick handler) {
		super(MenuType.GENERIC_9x6, id, playerInventory, page, 6);
		this.handler = handler;
	}

	@Override
	public void clicked(int slot, int button, ContainerInput input, Player player) {
		if (slot >= 0 && slot < PAGE_SLOTS) {
			// A click on the page is a button press, never an item move.
			if (handler != null && player instanceof ServerPlayer serverPlayer) {
				handler.press(serverPlayer, slot);
			}
			return;
		}
		super.clicked(slot, button, input, player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;         // shift-click moves nothing, either way
	}
}
