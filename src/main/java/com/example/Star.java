package com.example;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

import net.minecraft.ChatFormatting;
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

/**
 * The Go To Event Star, off the counter on floor 7.
 *
 * Most of the events are weeks apart -- October is ten real hours of Sundays
 * away in August -- so the star is the way to see one without waiting for the
 * calendar to come round. Right-click it, pick any of the five, and that is
 * what is on for the rest of the day.
 *
 * It is one use. The event it puts on pays what it always pays and opens
 * floor 10 the same, because as far as everything downstream is concerned it
 * genuinely is on.
 */
public final class Star {
	private Star() {
	}

	/** Where the five events sit in the picker. */
	private static final int FIRST = 10;

	public static void register() {
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (!Kit.is(player.getItemInHand(hand), Kit.STAR)) {
				return InteractionResult.PASS;
			}
			if (player instanceof ServerPlayer who && world instanceof ServerLevel) {
				pick(who);
			}
			return InteractionResult.SUCCESS;
		});

		// Aiming at a block goes to the block first, so a star pointed at the
		// floor has to be caught here as well or it does nothing.
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (!Kit.is(player.getItemInHand(hand), Kit.STAR)) {
				return InteractionResult.PASS;
			}
			if (player instanceof ServerPlayer who && world instanceof ServerLevel) {
				pick(who);
			}
			return InteractionResult.SUCCESS;
		});
	}

	/** The five events, and what happens if you pick one. */
	private static void pick(ServerPlayer player) {
		SimpleContainer page = new SimpleContainer(27);
		ItemStack filler = Game.cell(Items.GRAY_STAINED_GLASS_PANE, " ");
		for (int slot = 0; slot < 27; slot++) {
			page.setItem(slot, filler.copy());
		}

		String on = Events.running(player);
		page.setItem(4, Book.entry(Items.NETHER_STAR, "Go To Event Star",
				ChatFormatting.AQUA,
				"Pick one and it is on for the rest of today.",
				"On now: " + on,
				"One use -- the star goes."));

		for (int i = 0; i < Events.ALL.length; i++) {
			Events.Listing listing = Events.ALL[i];
			page.setItem(FIRST + i, Book.entry(icon(listing.name()), listing.name(),
					ChatFormatting.AQUA,
					listing.when(),
					listing.what(),
					"",
					"Click to go."));
		}

		page.setItem(22, Book.entry(Items.BARRIER, "Keep it", ChatFormatting.RED,
				"Close without using the star."));

		player.openMenu(new SimpleMenuProvider(
				(id, inventory, who) -> new ReadOnlyMenu(id, inventory, page, Star::chosen),
				Component.literal("Go To Event Star")));
	}

	private static void chosen(ServerPlayer player, int slot) {
		if (slot == 22) {
			player.closeContainer();
			return;
		}
		int index = slot - FIRST;
		if (index < 0 || index >= Events.ALL.length) {
			return;
		}
		if (!spend(player)) {
			player.sendSystemMessage(Component.literal("You are not carrying a star.")
					.withStyle(ChatFormatting.RED));
			player.closeContainer();
			return;
		}

		String name = Events.ALL[index].name();
		Events.put(player, name);
		player.closeContainer();

		player.level().playSound(null, player.blockPosition(),
				SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0f, 1.2f);
		player.sendSystemMessage(Component.literal(name
				+ " is on for the rest of today.").withStyle(ChatFormatting.AQUA));
		Events.start(player, name);
	}

	/** Take one star off them. False if there wasn't one to take. */
	private static boolean spend(ServerPlayer player) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (Kit.is(stack, Kit.STAR)) {
				stack.shrink(1);
				return true;
			}
		}
		return false;
	}

	private static net.minecraft.world.item.Item icon(String name) {
		return switch (name) {
			case "Spooky Shooter" -> Items.CARVED_PUMPKIN;
			case "Christmas" -> Items.RED_WOOL;
			case "Summer Break" -> Items.SUNFLOWER;
			case "Quest Day" -> Items.WRITTEN_BOOK;
			default -> Items.NETHER_STAR;
		};
	}
}
