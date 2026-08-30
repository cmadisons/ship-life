package com.example;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * The fridge in your room: five shelves you can actually put food on.
 *
 * Five because a fridge with a chest inside it is a chest, and the room is
 * meant to be a room rather than storage. Five slots is a hopper's worth,
 * which is why the screen is a hopper's -- Minecraft already draws that one
 * five slots wide.
 *
 * What is in it is saved on you rather than in the block, so it survives the
 * ship being repaired around it and comes with you if the room is ever
 * rebuilt.
 */
public final class Fridge {
	private Fridge() {
	}

	/** How many shelves. */
	public static final int SHELVES = 5;

	/** What is in your fridge, saved with you. */
	public static final AttachmentType<List<ItemStack>> INSIDE =
			AttachmentRegistry.<List<ItemStack>>builder()
					.initializer(ArrayList::new)
					.persistent(ItemStack.CODEC.listOf())
					.copyOnDeath()
					.buildAndRegister(ShipLifeMod.id("fridge"));

	/** Open it: the five shelves, with whatever you left on them. */
	public static void open(ServerPlayer player) {
		// Saved on every change rather than when the screen closes, so pulling
		// the game out from under it cannot lose what is inside.
		SimpleContainer shelves = new SimpleContainer(SHELVES) {
			@Override
			public void setChanged() {
				super.setChanged();
				save(player, this);
			}
		};
		List<ItemStack> saved = player.getAttachedOrCreate(INSIDE);
		for (int slot = 0; slot < SHELVES && slot < saved.size(); slot++) {
			shelves.setItem(slot, saved.get(slot).copy());
		}

		player.level().playSound(null, Places.FRIDGE, SoundEvents.IRON_DOOR_OPEN,
				SoundSource.BLOCKS, 0.6f, 1.4f);
		player.openMenu(new SimpleMenuProvider(
				(id, inventory, who) -> new HopperMenu(id, inventory, shelves),
				Component.literal("Fridge").withStyle(ChatFormatting.AQUA)));
	}

	private static void save(ServerPlayer player, net.minecraft.world.Container shelves) {
		List<ItemStack> keep = new ArrayList<>();
		for (int slot = 0; slot < shelves.getContainerSize(); slot++) {
			keep.add(shelves.getItem(slot).copy());
		}
		player.setAttached(INSIDE, keep);
	}

	/**
	 * Make sure the attachment exists before any world is read.
	 *
	 * The same reason {@link State} has one: an attachment nobody has
	 * registered by the time a player is loaded is an attachment whose saved
	 * data is quietly thrown away.
	 */
	public static void register() {
		ShipLifeMod.LOGGER.info("Ship Life fridges hold {} things.", SHELVES);
	}
}
