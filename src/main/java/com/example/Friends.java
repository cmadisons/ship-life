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

/**
 * The people on the ship who are not staff.
 *
 * Charlie said in chapter 4 that he would get you new friends, and Ben on
 * floor 15 is the first of them. The floor comes free with the passport
 * upgrade, so the first thing a better passport buys you is somebody to knock
 * on rather than something to spend tickets in.
 *
 * What a friend is for is what Ben hands over the first time you knock: his
 * armour, which keeps a tenth of every hit off you and saves it for your next
 * swing, and three bombs that put green gas on the floor. The gear itself is
 * in {@link Gear}; Ben is only the man who gives it to you.
 */
public final class Friends {
	private Friends() {
	}

	/** Who you have met, one bit each. Ben is the first. */
	public static final AttachmentType<Integer> MET =
			AttachmentRegistry.<Integer>builder()
					.initializer(() -> 0)
					.persistent(Codec.INT)
					.copyOnDeath()
					.buildAndRegister(ShipLifeMod.id("friends_met"));

	public static final int BEN = 0;

	public static boolean knows(ServerPlayer player, int friend) {
		return (player.getAttachedOrCreate(MET) & (1 << friend)) != 0;
	}

	public static void register() {
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (!(player instanceof ServerPlayer who) || !(world instanceof ServerLevel level)
					|| !ShipLifeMod.isShipLife(level)) {
				return InteractionResult.PASS;
			}
			BlockPos pos = Places.local(hit.getBlockPos());
			if (pos.equals(Places.BEN) || pos.equals(Places.BEN.above())) {
				ben(who, level);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});
	}

	/**
	 * The armour and the three bombs, handed over once.
	 *
	 * It is a one-off, so it is remembered rather than counted: lose the coat
	 * down a hole and Ben will not replace it.
	 */
	private static boolean gift(ServerPlayer player) {
		if (!State.firstTime(player, 2)) {
			return false;
		}
		player.sendSystemMessage(Component.literal(
				"Ben: \"Here. Wear the coat -- it takes a tenth off everything that "
				+ "hits you and saves it up for the next one you land. And these "
				+ "three, drop one in a room full of them and stand back.\"")
				.withStyle(ChatFormatting.WHITE));
		give(player, Kit.armour());
		give(player, Kit.bomb(3));
		player.sendSystemMessage(Component.literal("Ben gave you his armour and 3 bombs.")
				.withStyle(ChatFormatting.LIGHT_PURPLE));
		return true;
	}

	/** What three more of his bombs cost. */
	public static final int BOMBS_COST = 250;

	/** How many of his bombs you are carrying. */
	private static int bombsOn(ServerPlayer player) {
		int found = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(slot);
			if (Kit.is(stack, Kit.BOMB)) {
				found += stack.getCount();
			}
		}
		return found;
	}

	/** Ben's counter: three more bombs, and nothing else. */
	private static void counter(ServerPlayer player) {
		// Six rows, because the menu it opens in is a chest.
		net.minecraft.world.SimpleContainer page = new net.minecraft.world.SimpleContainer(54);
		net.minecraft.world.item.ItemStack filler = Game.cell(
				net.minecraft.world.item.Items.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
		for (int slot = 0; slot < 54; slot++) {
			page.setItem(slot, filler.copy());
		}
		boolean afford = State.event(player) >= BOMBS_COST;
		page.setItem(22, Book.entry(Made.bomb, "3 of Ben's Bombs",
				afford ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY,
				"Green gas: 10 a second off every",
				"enemy standing in it.",
				"It stays until they are all dead.",
				"",
				BOMBS_COST + " event tickets",
				"You have " + State.event(player) + ".",
				afford ? "Click to buy." : "Not enough yet."));
		page.setItem(49, Book.entry(net.minecraft.world.item.Items.BARRIER, "Close",
				ChatFormatting.RED, "Press Escape."));
		player.openMenu(new net.minecraft.world.SimpleMenuProvider(
				(id, inventory, who) -> new ReadOnlyMenu(id, inventory, page, Friends::buy),
				Component.literal("Ben's Bombs")));
	}

	private static void buy(ServerPlayer player, int slot) {
		if (slot == 49) {
			player.closeContainer();
			return;
		}
		if (slot != 22) {
			return;
		}
		if (State.event(player) < BOMBS_COST) {
			player.sendSystemMessage(Component.literal("That is " + BOMBS_COST
					+ " event tickets and you have " + State.event(player) + ".")
					.withStyle(ChatFormatting.RED));
			return;
		}
		State.event(player, -BOMBS_COST);
		give(player, Kit.bomb(3));
		player.sendSystemMessage(Component.literal("Ben: \"Three more. Stand back this time.\"")
				.withStyle(ChatFormatting.WHITE));
		player.closeContainer();
	}

	/** Into the pack, or on the floor if there is no room for it. */
	private static void give(ServerPlayer player, net.minecraft.world.item.ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
	}

	private static void ben(ServerPlayer player, ServerLevel level) {
		level.playSound(null, Places.BEN, SoundEvents.NOTE_BLOCK_BELL.value(),
				SoundSource.PLAYERS, 0.6f, 1.4f);

		if (!knows(player, BEN)) {
			player.setAttached(MET, player.getAttachedOrCreate(MET) | (1 << BEN));
			player.sendSystemMessage(Component.literal(
					"Ben: \"You made it up here. I'm Ben -- floor 15 is mine. "
					+ "Charlie said he'd send someone.\"")
					.withStyle(ChatFormatting.WHITE));
			player.sendSystemMessage(Component.literal("Ben is your first friend.")
					.withStyle(ChatFormatting.LIGHT_PURPLE));
			gift(player);
			return;
		}

		// Anyone who met him before he had anything to give still gets it.
		if (gift(player)) {
			return;
		}

		// Out of bombs? Then that is what he is for today.
		if (bombsOn(player) == 0) {
			player.sendSystemMessage(Component.literal(
					"Ben: \"Used them all, then. I can do you three more for "
					+ BOMBS_COST + " event tickets.\"")
					.withStyle(ChatFormatting.WHITE));
			counter(player);
			return;
		}

		// He keeps up with what you have been doing, which is what a friend
		// who lives two floors up would actually do.
		int laps = State.tally(player, State.LAPS);
		int waves = State.tally(player, State.WAVES);
		int pets = Pets.total(player);
		String about;
		if (waves > 0) {
			about = "Heard you were down on 9. " + waves + " wave"
					+ (waves == 1 ? "" : "s") + " is not nothing.";
		} else if (laps > 0) {
			about = laps + " lap" + (laps == 1 ? "" : "s") + " in that pool. "
					+ "Better you than me.";
		} else if (pets > 0) {
			about = "That lot follow you everywhere, don't they.";
		} else {
			about = "Quiet day? Try the arcade on 2.";
		}
		player.sendSystemMessage(Component.literal("Ben: \"" + about + "\"")
				.withStyle(ChatFormatting.WHITE));
	}
}
