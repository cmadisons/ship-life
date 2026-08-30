package com.example;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The things you carry.
 *
 * None of these are new items with new textures -- they are ordinary Minecraft
 * items wearing a name. A sponge really is a sponge; the towel is white wool
 * because that is what a folded towel looks like. Naming rather than modelling
 * keeps the whole mod to code and no art, and it means everything already has
 * a sensible icon in every resource pack.
 *
 * The name is also how the mod recognises them, so {@link #is} checks the name
 * rather than the item, and an ordinary sponge off the floor is not a Sponge.
 */
public final class Kit {
	private Kit() {
	}

	public static final String QUEST_BOOK = "Quest Book";
	public static final String PASSPORT = "Passport";
	public static final String SPONGE = "Sponge";
	public static final String TOWEL = "Towel";
	public static final String MOWER = "Lawn Mower";
	public static final String WHACKER = "Weed Whacker";
	public static final String PLUNGER = "Plunger";
	public static final String MOP = "Mop";
	public static final String MEAL = "Buffet Plate";
	public static final String ARMOUR = "Ben's Armour";
	public static final String BOMB = "Ben's Bomb";
	public static final String STAR = "Go To Event Star";
	public static final String BOOTS = "Izzy's Boots";
	public static final String HELMET = "Izzy's Helmet";
	public static final String LEGGINGS = "Izzy's Leggings";
	public static final String PENNY = "Penny";

	public static ItemStack make(Item item, String name, ChatFormatting colour, String... lore) {
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(colour));
		if (lore.length > 0) {
			java.util.List<Component> lines = new java.util.ArrayList<>();
			for (String line : lore) {
				lines.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
			}
			stack.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(lines));
		}
		return stack;
	}

	/** Is this stack the named thing, rather than an ordinary one? */
	public static boolean is(ItemStack stack, String name) {
		if (stack.isEmpty()) {
			return false;
		}
		Component custom = stack.get(DataComponents.CUSTOM_NAME);
		return custom != null && custom.getString().equals(name);
	}

	/**
	 * Take back the tools there is nothing left to use.
	 *
	 * The sponge, the towel, the mower and the whacker go when chapter 1 is
	 * behind you; the plunger and the mop go when Charlie's quest is, since
	 * that is the last blocked toilet on the ship.
	 */
	public static void dropChores(ServerPlayer player) {
		player.getInventory().clearOrCountMatchingItems(
				stack -> is(stack, SPONGE) || is(stack, TOWEL)
						|| is(stack, "Dirty " + SPONGE) || is(stack, "Dirty " + TOWEL)
						|| is(stack, MOWER) || is(stack, WHACKER),
				-1, player.inventoryMenu.getCraftSlots());
	}

	/** The plunger and the mop, once the bathroom quest is done with. */
	public static void dropBathroom(ServerPlayer player) {
		player.getInventory().clearOrCountMatchingItems(
				stack -> is(stack, PLUNGER) || is(stack, MOP),
				-1, player.inventoryMenu.getCraftSlots());
	}

	/** A plate from the buffet. Three helpings, because it is a buffet. */
	public static ItemStack meal() {
		ItemStack plate = make(Items.COOKED_BEEF, MEAL, ChatFormatting.GOLD,
				"From the buffet on floor 4.",
				"Eat it and your hearts come back.");
		plate.setCount(3);
		return plate;
	}

	public static ItemStack questBook() {
		return make(Items.WRITABLE_BOOK, QUEST_BOOK, ChatFormatting.YELLOW,
				"Right-click to open.",
				"Everything you have to do,",
				"and how far away it is.");
	}

	public static ItemStack passport() {
		return make(Items.PAPER, PASSPORT, ChatFormatting.AQUA,
				"Opens the floors you are allowed on.",
				"Upgrade it on floor 14 for floor 15.");
	}

	public static ItemStack sponge() {
		return make(Made.sponge, SPONGE, ChatFormatting.YELLOW,
				"Hold left-click on a dish to wash it.");
	}

	public static ItemStack towel() {
		return make(Made.towel, TOWEL, ChatFormatting.WHITE,
				"Hold right-click on a dish to dry it.");
	}

	public static ItemStack mower() {
		return make(Made.lawnMower, MOWER, ChatFormatting.GREEN,
				"Right-click every square of grass.");
	}

	public static ItemStack whacker() {
		return make(Made.weedWhacker, WHACKER, ChatFormatting.GREEN,
				"Right-click a weed to take it out.");
	}

	public static ItemStack plunger() {
		return make(Made.plunger, PLUNGER, ChatFormatting.GRAY,
				"Hold right-click on the toilet.",
				"Let go while the bar is green.");
	}

	/**
	 * Ben's armour: a coat with plants growing all over it.
	 *
	 * What it does is not in the item at all -- {@link Gear} watches for it on
	 * your chest and does the work.
	 */
	public static ItemStack armour() {
		ItemStack coat = make(Made.benArmour, ARMOUR, ChatFormatting.GREEN,
				"A tenth of every hit does not land.",
				"What it stops goes onto your next swing.",
				"From Ben, on floor 15.");
		coat.set(DataComponents.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE);
		return coat;
	}

	/** One of Ben's bombs. Right-click to put the gas down. */
	public static ItemStack bomb(int howMany) {
		ItemStack stack = make(Made.bomb, BOMB, ChatFormatting.DARK_GREEN,
				"Right-click to drop it.",
				"Green gas: 10 damage to every enemy in it.",
				"It stays until they are all dead.");
		stack.setCount(howMany);
		return stack;
	}

	public static ItemStack penny() {
		return make(Items.GOLD_NUGGET, PENNY, ChatFormatting.GOLD,
				"One cent.",
				"The last of your hundred dollars.");
	}

	/**
	 * The rest of the set, from Izzy on floor 16.
	 *
	 * Same leaves as Ben's coat and the same job: every piece you are wearing
	 * keeps a tenth of the hit off you and banks it, so the whole set stops
	 * four tenths of everything and hands it all to your next swing.
	 */
	public static ItemStack boots() {
		return piece(Made.benBoots, BOOTS);
	}

	public static ItemStack helmet() {
		return piece(Made.benHelmet, HELMET);
	}

	public static ItemStack leggings() {
		return piece(Made.benLeggings, LEGGINGS);
	}

	private static ItemStack piece(net.minecraft.world.item.Item item, String name) {
		ItemStack made = make(item, name, ChatFormatting.GREEN,
				"A tenth of every hit does not land.",
				"What it stops goes onto your next swing.",
				"From Izzy, on floor 16.");
		made.set(DataComponents.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE);
		return made;
	}

	/** The one-use star off floor 7. {@link Star} is what it does. */
	public static ItemStack star() {
		ItemStack star = make(Items.NETHER_STAR, STAR, ChatFormatting.AQUA,
				"Right-click to pick an event.",
				"It is on for the rest of the day.",
				"Three uses.");
		star.setCount(3);
		return star;
	}

	public static ItemStack mop() {
		return make(Items.BRUSH, MOP, ChatFormatting.GRAY,
				"Right-click each spot, 3 seconds.");
	}
}
