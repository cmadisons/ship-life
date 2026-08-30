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
			BlockPos pos = Places.local(hit.getBlockPos());
			BlockPos base = level.getBlockState(pos.below()).isAir() ? pos : pos.below();

			if (matches(pos, base, Places.SNAKE)) {
				ArcadePackets.open(who, "snake");
				return InteractionResult.SUCCESS;
			}
			if (matches(pos, base, Places.PACMAN)) {
				ArcadePackets.open(who, "pacman");
				return InteractionResult.SUCCESS;
			}
			if (matches(pos, base, Places.GALAGA)) {
				ArcadePackets.open(who, "galaga");
				return InteractionResult.SUCCESS;
			}
			if (matches(pos, base, Places.RACE_CAR)) {
				Kart.getIn(who);
				return InteractionResult.SUCCESS;
			}
			if (matches(pos, base, Places.PRIZES)) {
				prizes(who);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});
	}

	/**
	 * Did they click this machine?
	 *
	 * A cabinet is three wide, four tall and has a control panel sticking out
	 * at the front, and clicking any part of it should start the game -- so
	 * the whole box counts rather than one particular block of it.
	 */
	private static boolean matches(BlockPos hit, BlockPos base, BlockPos machine) {
		int dx = hit.getX() - machine.getX();
		int dy = hit.getY() - machine.getY();
		int dz = hit.getZ() - machine.getZ();
		return Math.abs(dx) <= 1 && dy >= 0 && dy <= 3 && dz >= 0 && dz <= 1;
	}

	/**
	 * The prize counter: a pet for ten tickets, or the next three quests for
	 * twenty-five.
	 *
	 * The quest bundle is drawn from a pool of two hundred and fifty, and the
	 * pool only opens once you own floors 8, 9 and 10 -- so until then the
	 * counter says what is missing rather than pretending it is for sale.
	 */
	private static void prizes(ServerPlayer player) {
		net.minecraft.world.SimpleContainer page = new net.minecraft.world.SimpleContainer(54);
		net.minecraft.world.item.ItemStack filler = Game.cell(
				net.minecraft.world.item.Items.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
		for (int slot = 0; slot < 54; slot++) {
			page.setItem(slot, filler.copy());
		}

		page.setItem(4, Book.entry(net.minecraft.world.item.Items.GOLD_NUGGET,
				"Your Tickets", ChatFormatting.GOLD,
				State.arcade(player) + " arcade tickets",
				State.event(player) + " event tickets"));

		Pets.Kind[] kinds = Pets.ARCADE_PETS;
		for (int i = 0; i < kinds.length; i++) {
			Pets.Kind kind = kinds[i];
			int have = Pets.owned(player, kind);
			page.setItem(19 + i, Book.entry(icon(kind), kind.label, ChatFormatting.AQUA,
					kind.what,
					Pets.PRICE + " arcade tickets",
					have == 0 ? "You have none." : "You have " + have + ".",
					kind == Pets.Kind.CAT
							? "Buying more does not stack."
							: "Two of a kind doubles it.",
					kind == Pets.Kind.DOLPHIN
							? "Today it is copying the " + Pets.dolphinToday().label + "."
							: "",
					"",
					"Click to buy."));
		}

		// Events, the store and the fight room open the pool. Until then the
		// counter says what is missing rather than taking the click and
		// refusing it.
		boolean ready = State.hasFloor(player, 7) && State.hasFloor(player, 8)
				&& State.hasFloor(player, 9);
		page.setItem(25, Book.entry(net.minecraft.world.item.Items.WRITTEN_BOOK,
				"The Next 3 Quests", ready ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY,
				"Three quests, drawn at random",
				"from a pool of 250.",
				"25 arcade tickets",
				ready ? "Click to buy." : "Needs floors 7, 8 and 9 first.",
				"You are carrying " + QuestPool.carrying(player) + "."));

		// The boards, one a machine, so there is something to beat besides
		// your own best.
		page.setItem(38, Book.entry(net.minecraft.world.item.Items.LIME_DYE,
				"Snake -- your best five", ChatFormatting.GREEN,
				ArcadePackets.board(player, "snake").toArray(new String[0])));
		page.setItem(40, Book.entry(net.minecraft.world.item.Items.YELLOW_DYE,
				"Pac-Man -- your best five", ChatFormatting.YELLOW,
				ArcadePackets.board(player, "pacman").toArray(new String[0])));
		page.setItem(42, Book.entry(net.minecraft.world.item.Items.LIGHT_BLUE_DYE,
				"Galaga -- your best five", ChatFormatting.AQUA,
				ArcadePackets.board(player, "galaga").toArray(new String[0])));

		page.setItem(49, Book.entry(net.minecraft.world.item.Items.BARRIER, "Close",
				ChatFormatting.RED, "Press Escape."));

		player.openMenu(new net.minecraft.world.SimpleMenuProvider(
				(id, inventory, who) -> new ReadOnlyMenu(id, inventory, page,
						Arcade::buy),
				Component.literal("Prize Counter")));
	}

	private static void buy(ServerPlayer player, int slot) {
		if (slot == 49) {
			player.closeContainer();
			return;
		}
		Pets.Kind[] kinds = Pets.ARCADE_PETS;
		if (slot >= 19 && slot < 19 + kinds.length) {
			if (Pets.buy(player, kinds[slot - 19])) {
				player.closeContainer();
			}
			return;
		}
		if (slot == 25) {
			if (!(State.hasFloor(player, 7) && State.hasFloor(player, 8)
					&& State.hasFloor(player, 9))) {
				player.sendSystemMessage(Component.literal(
						"The quest pool opens once you own floors 7, 8 and 9.")
						.withStyle(ChatFormatting.GRAY));
				return;
			}
			if (State.arcade(player) < 25) {
				player.sendSystemMessage(Component.literal("That is 25 arcade tickets and you have "
						+ State.arcade(player) + ".").withStyle(ChatFormatting.RED));
				return;
			}
			State.arcade(player, -25);
			QuestPool.give(player, 3);
			player.closeContainer();
		}
	}

	private static net.minecraft.world.item.Item icon(Pets.Kind kind) {
		return switch (kind) {
			case LION -> net.minecraft.world.item.Items.ORANGE_DYE;
			case DOG -> net.minecraft.world.item.Items.BONE;
			case CAT -> net.minecraft.world.item.Items.STRING;
			case DOLPHIN -> net.minecraft.world.item.Items.LIGHT_BLUE_DYE;
			case SKELETON -> net.minecraft.world.item.Items.BONE;
			case SHADOW -> net.minecraft.world.item.Items.ENDER_PEARL;
		};
	}
}
