package com.example;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The elevator.
 *
 * Floors you can't go to aren't listed at all -- the panel simply doesn't have
 * a button for them, which is how you can tell the ship is bigger than what
 * you have seen. The one exception is a floor you are currently on a quest to
 * open: that one shows with a lock beside it, so you know it is there and that
 * it is coming.
 *
 * Riding is not a teleport with the sound turned off. The doors close, the car
 * whirs while it moves, and the doors open at the other end, and the floor you
 * are going to is named the whole way.
 */
public final class Elevator {
	private Elevator() {
	}

	/** What is on each floor, for the panel to say. */
	private static final String[] NAMES = {
			"", "Lobby", "Arcade", "Swimming Pool", "Buffet", "Your Room",
			"Race Track", "Events", "Store", "Fight Room", "Boss Room",
			"Rewards", "Pet Store", "The Keg", "Passports", "Ben's Room",
			"Izzy's Room"
	};

	/** How long the ride takes, in ticks. */
	private static final int RIDE = 40;

	/** Open the floor panel. */
	/** Who is stood on a plate, so it fires once rather than every tick. */
	private static final java.util.Map<java.util.UUID, Long> ON_PLATE =
			new java.util.HashMap<>();

	/**
	 * The plates each side of the two doors.
	 *
	 * A plate that reopened the lift every tick you stood on it would be
	 * unusable, so one press is one opening: you have to step off and back on.
	 */
	public static void register() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK
				.register(server -> {
			if (server.getTickCount() % 4 != 0) {
				return;
			}
			for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
				if (!ShipLifeMod.isShipLife(level)) {
					continue;
				}
				for (ServerPlayer player : level.players()) {
					standing(player);
				}
			}
		});
	}

	private static void standing(ServerPlayer player) {
		int floor = Places.floorAt(player.getY());
		net.minecraft.core.BlockPos feet = Places.local(player.blockPosition());
		boolean on = false;
		if (floor >= 1 && floor <= Places.TOP_FLOOR) {
			for (net.minecraft.core.BlockPos plate : Places.liftPlates(floor)) {
				if (plate.equals(feet)) {
					on = true;
					break;
				}
			}
		}
		if (!on) {
			ON_PLATE.remove(player.getUUID());
			return;
		}
		if (ON_PLATE.put(player.getUUID(), player.level().getGameTime()) == null) {
			open(player);
		}
	}

	public static void open(ServerPlayer player) {
		SimpleContainer page = new SimpleContainer(54);
		ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
		filler.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
		for (int slot = 0; slot < 54; slot++) {
			page.setItem(slot, filler.copy());
		}

		int on = Places.floorAt(player.getY());
		for (int floor = 1; floor <= Places.TOP_FLOOR; floor++) {
			int slot = 10 + ((floor - 1) / 7) * 9 + ((floor - 1) % 7);
			if (State.hasFloor(player, floor)) {
				page.setItem(slot, Book.entry(
						floor == on ? Items.LIME_DYE : Items.WHITE_DYE,
						"Floor " + floor, ChatFormatting.WHITE,
						NAMES[floor],
						floor == on ? "You are here." : "Click to go."));
			} else if (questFloor(player) == floor) {
				page.setItem(slot, Book.entry(Items.IRON_DOOR,
						"Floor " + floor + " 🔒", ChatFormatting.GRAY,
						"Locked.",
						"Your quest opens this one."));
			}
		}

		page.setItem(49, Book.entry(Items.BARRIER, "Close", ChatFormatting.RED, "Press Escape."));

		player.openMenu(new SimpleMenuProvider(
				(id, inventory, who) -> new ReadOnlyMenu(id, inventory, page, Elevator::click),
				Component.literal("Elevator")));
	}

	/** The one locked floor worth showing, because a quest is opening it. */
	private static int questFloor(ServerPlayer player) {
		return switch (State.quest(player)) {
			case 4 -> 2;                   // Charlie's quest opens 2, 3 and 4
			default -> 0;
		};
	}

	private static void click(ServerPlayer player, int slot) {
		if (slot == 49) {
			player.closeContainer();
			return;
		}
		int row = slot / 9;
		int column = slot % 9;
		if (row < 1 || row > 3 || column < 1 || column > 7) {
			return;
		}
		int floor = (row - 1) * 7 + column;
		if (floor < 1 || floor > Places.TOP_FLOOR || !State.hasFloor(player, floor)) {
			return;
		}
		player.closeContainer();
		ride(player, floor, Places.shipOf(player.getX()));
	}

	/** Doors close, the car moves, doors open. */
	public static void ride(ServerPlayer player, int floor, int ship) {
		ServerLevel level = (ServerLevel) player.level();
		BlockPos from = player.blockPosition();

		level.playSound(null, from, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.8f, 1.0f);
		player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, RIDE + 10, 0, true, false, false));
		Hud.busy(player, RIDE + 10);
		player.sendOverlayMessage(Component.literal("▲  Ship " + ship + ", floor " + floor
				+ " -- " + NAMES[floor]).withStyle(ChatFormatting.AQUA));

		// The whirr while it moves, then the doors at the far end.
		Ticker.after(20, () -> level.playSound(null, from,
				SoundEvents.ELYTRA_FLYING, SoundSource.BLOCKS, 0.5f, 0.8f));
		Ticker.after(RIDE, () -> {
			BlockPos to = Places.onShip(Places.lift(floor), ship);
			player.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
			level.playSound(null, to, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.8f, 1.0f);
			player.sendOverlayMessage(Component.literal("Ship " + ship + ", floor " + floor
					+ " -- " + NAMES[floor]).withStyle(ChatFormatting.AQUA));

			// Quest 3: pressing the button for floor 5 is the whole part.
			if (floor == 5 && Quests.on(player, 2, 1)) {
				Quests.finishPart(player);
			}
			// Quest 4 starts with getting yourself back down to the lobby.
			if (floor == 1 && Quests.on(player, 3, 0)) {
				Quests.finishPart(player);
			}
		});
	}
}
