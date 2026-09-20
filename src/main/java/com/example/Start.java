package com.example;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * How you want to start: a floor at a time, or every floor open.
 *
 * Asked the first time you arrive, on a page like the lift's -- two buttons
 * and nothing else. It is asked here rather than on the Create World screen
 * because the floors are yours, not the world's: they hang off the player, so
 * a second person joining the same world gets asked too, and a world made
 * before this existed gets asked the next time somebody new turns up in it.
 * The Create World screen would also have meant reaching into Minecraft's own
 * screen, which nothing else in the mod does.
 *
 * Escape is not an answer. The page comes back every time you join until you
 * click one of the two, but only while you have not started -- nobody halfway
 * up the ship is asked how they would like to begin.
 *
 * Every floor open is only the floors. Chapter 1 is still the dishes, and
 * every quest, shop and pet still does what it did; the floors they would
 * have opened are just already yours.
 */
public final class Start {
	private Start() {
	}

	/** Where the two buttons sit on the page. */
	private static final int NORMAL = 20;
	private static final int EVERYTHING = 24;

	/**
	 * Put the question in front of you, if it still wants asking.
	 *
	 * Two seconds after you arrive rather than the moment you do: the first
	 * tick of a join is still loading the world around you, and a page opened
	 * then is a page the game shuts again.
	 */
	public static void offer(ServerPlayer player) {
		if (State.happened(player, State.Once.START_CHOSEN)) {
			return;
		}
		if (State.quest(player) > 0 || player.getAttachedOrCreate(State.FLOORS) != 0) {
			return;                       // already on the way -- too late to ask
		}
		Ticker.after(40, () -> {
			if (!player.isRemoved() && !State.happened(player, State.Once.START_CHOSEN)) {
				open(player);
			}
		});
	}

	private static void open(ServerPlayer player) {
		SimpleContainer page = Book.page(Items.GRAY_STAINED_GLASS_PANE);
		page.setItem(4, Book.entry(Items.PAPER, "How do you want to start?",
				ChatFormatting.YELLOW,
				"Pick one. Escape and it asks again",
				"the next time you join."));
		page.setItem(NORMAL, Book.entry(Items.IRON_DOOR, "Start normally",
				ChatFormatting.WHITE,
				"Floors open one at a time, as",
				"you earn them -- quests, tickets,",
				"pets, the pool and the track."));
		page.setItem(EVERYTHING, Book.entry(Items.DIAMOND, "💎 Start with every floor open",
				ChatFormatting.AQUA,
				"All " + Places.TOP_FLOOR + " floors in the lift from the",
				"start, and ship 2's too. Your",
				"passport comes now. The quests",
				"and the shops are all still there."));

		player.openMenu(new SimpleMenuProvider(
				(id, inventory, who) -> new ReadOnlyMenu(id, inventory, page, Start::click),
				Component.literal("Ship Life")));
	}

	private static void click(ServerPlayer player, int slot) {
		if (slot != NORMAL && slot != EVERYTHING) {
			return;
		}
		if (!State.firstTime(player, State.Once.START_CHOSEN)) {
			player.closeContainer();      // a second click on the way out
			return;
		}
		player.closeContainer();
		player.level().playSound(null, player.blockPosition(),
				SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.8f, 1.4f);

		if (slot == NORMAL) {
			player.sendSystemMessage(Component.literal(
					"A floor at a time, then. The first one is a hundred dollars away.")
					.withStyle(ChatFormatting.AQUA));
			return;
		}

		everyFloor(player);
		player.sendSystemMessage(Component.literal("Every floor is open -- all "
				+ Places.TOP_FLOOR + " of them, on both ships. Your passport is in slot 8.")
				.withStyle(ChatFormatting.AQUA));
		player.sendSystemMessage(Component.literal(
				"Chapter 1 is still the dishes, if you want the hundred dollars.")
				.withStyle(ChatFormatting.GRAY));
		Log.write(player, "started with every floor open");
	}

	/**
	 * Both passports, every floor.
	 *
	 * The lift reads ship 1's, whichever ship it is in; ship 2's is what its
	 * pool laps and its maze open, and /floorN over there reads it. Open one
	 * and not the other and the Nether would ask for the laps again.
	 */
	private static void everyFloor(ServerPlayer player) {
		for (int floor = 1; floor <= Places.TOP_FLOOR; floor++) {
			State.unlock(player, floor);
			State.unlockTwo(player, floor);
		}
		// The lift reads your passport rather than a list, so it comes now
		// rather than off the security desk in chapter 2.
		if (!Kit.is(player.getInventory().getItem(Slots.PASSPORT_SLOT), Kit.PASSPORT)) {
			player.getInventory().setItem(Slots.PASSPORT_SLOT, Kit.passport());
		}
	}
}
