package com.example;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The frame the three arcade games are drawn in.
 *
 * An arcade cabinet has a screen, and Minecraft has one already: a double
 * chest is nine slots across and six down, and an item in a slot is a pixel
 * you can colour. So the games are played inside a chest screen -- the top
 * five rows are the picture, the bottom row is the buttons -- and the whole
 * thing runs on the server with no rendering code at all.
 *
 * Redrawing is just writing new items into the container and telling the open
 * menu to send them; the screen updates in place while you watch.
 */
public abstract class Game {
	/** Nine across, five down: the playfield. */
	public static final int COLUMNS = 9;
	public static final int ROWS = 5;

	/** Everyone with a game running. */
	private static final Map<UUID, Game> PLAYING = new HashMap<>();

	protected final ServerPlayer player;
	protected final SimpleContainer screen = new SimpleContainer(54);

	/** Ticks since the game started. */
	protected int age;

	/** Set when the game is finished; the next press starts a new one. */
	protected boolean over;

	protected Game(ServerPlayer player) {
		this.player = player;
	}

	/** What the machine is called. */
	public abstract String title();

	/** How many ticks between one step of the game and the next. */
	public abstract int speed();

	/** Move the game on one step. */
	public abstract void step();

	/** Paint the playfield into the screen. */
	public abstract void draw();

	/** A button in the bottom row was pressed, 0 to 8 from the left. */
	public abstract void press(int button);

	/** A cell of the picture itself was clicked. Most games ignore this. */
	public void pick(int cell) {
	}

	// ------------------------------------------------------------- the frame

	public void open() {
		PLAYING.put(player.getUUID(), this);
		draw();
		player.openMenu(new SimpleMenuProvider(
				(id, inventory, who) -> new ReadOnlyMenu(id, inventory, screen,
						(clicker, slot) -> {
							if (slot >= 45) {
								press(slot - 45);
							} else {
								pick(slot);
							}
						}),
				Component.literal(title())));
	}

	/** Put a picture cell in place. Row 0 is the top. */
	protected void put(int column, int row, ItemStack cell) {
		screen.setItem(row * COLUMNS + column, cell);
	}

	/** Clear the playfield to a dark screen. */
	protected void blank() {
		for (int slot = 0; slot < COLUMNS * ROWS; slot++) {
			screen.setItem(slot, cell(Items.BLACK_STAINED_GLASS_PANE, " "));
		}
	}

	/** A button along the bottom. */
	protected void button(int index, Item item, String name) {
		screen.setItem(45 + index, cell(item, name));
	}

	/** One coloured square of the screen, or a button. */
	public static ItemStack cell(Item item, String name) {
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(name)
				.withStyle(ChatFormatting.WHITE).withStyle(style -> style.withItalic(false)));
		return stack;
	}

	/**
	 * Pay out, and say so.
	 *
	 * Summer Break doubles what the arcade pays, which is the whole of that
	 * event -- there is nothing to play, you just come in that weekend.
	 */
	protected void win(int tickets, String why) {
		if ("Summer Break".equals(Events.running(player))) {
			tickets *= 2;
			why = why + ", doubled for Summer Break";
		}
		State.arcade(player, tickets);
		State.add(player, State.EARNED, tickets);
		player.sendSystemMessage(Component.literal("+" + tickets + " ticket"
				+ (tickets == 1 ? "" : "s") + "  --  " + why + ". You have "
				+ State.arcade(player) + ".").withStyle(ChatFormatting.GREEN));
	}

	// -------------------------------------------------------------- the clock

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (PLAYING.isEmpty()) {
				return;
			}
			PLAYING.entrySet().removeIf(entry -> {
				Game game = entry.getValue();
				ServerPlayer who = game.player;
				// Walking away from the machine ends the game -- and so does
				// opening something else, such as the Quest Book, which is a
				// screen of the same kind but not this one.
				if (who.isRemoved()
						|| !(who.containerMenu instanceof ReadOnlyMenu menu)
						|| menu.getContainer() != game.screen) {
					return true;
				}
				game.age++;
				if (!game.over && game.age % game.speed() == 0) {
					game.step();
				}
				game.draw();
				who.containerMenu.broadcastChanges();
				return false;
			});
		});
	}
}
