package com.example;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
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

	public static ItemStack questBook() {
		return make(Items.WRITABLE_BOOK, QUEST_BOOK, ChatFormatting.YELLOW,
				"Right-click to open.",
				"Everything you have to do,",
				"and how far away it is.");
	}

	public static ItemStack passport() {
		return make(Items.PAPER, PASSPORT, ChatFormatting.AQUA,
				"Opens the floors you are allowed on.");
	}

	public static ItemStack sponge() {
		return make(Items.SPONGE, SPONGE, ChatFormatting.YELLOW,
				"Hold left-click on a dish to wash it.");
	}

	public static ItemStack towel() {
		return make(Items.WHITE_WOOL, TOWEL, ChatFormatting.WHITE,
				"Hold right-click on a dish to dry it.");
	}

	public static ItemStack mower() {
		return make(Items.SHEARS, MOWER, ChatFormatting.GREEN,
				"Right-click every square of grass.");
	}

	public static ItemStack whacker() {
		return make(Items.IRON_HOE, WHACKER, ChatFormatting.GREEN,
				"Right-click a weed to take it out.");
	}

	public static ItemStack plunger() {
		return make(Items.STICK, PLUNGER, ChatFormatting.GRAY,
				"Hold right-click on the toilet.",
				"Let go while the bar is green.");
	}

	public static ItemStack penny() {
		return make(Items.GOLD_NUGGET, PENNY, ChatFormatting.GOLD,
				"One cent.",
				"The last of your hundred dollars.");
	}

	public static ItemStack mop() {
		return make(Items.BRUSH, MOP, ChatFormatting.GRAY,
				"Hold it and walk over the mess.");
	}
}
