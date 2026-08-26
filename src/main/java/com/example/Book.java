package com.example;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

/**
 * The Quest Book: what you have to do, what it pays, and how far away it is.
 *
 * One row per quest. The quest you are on is a written book and shows all of
 * its parts with the one you are on marked, because the deal from the start
 * was that you can see the whole job before you take it -- you just can't do
 * the third part before the first. Quests you have finished go grey, and ones
 * you haven't reached yet stay shut.
 *
 * Clicking a quest sets its star. There is only ever one star, so clicking a
 * different quest moves it rather than adding another.
 */
public final class Book {
	private Book() {
	}

	public static void open(ServerPlayer player) {
		SimpleContainer page = new SimpleContainer(54);
		ItemStack filler = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
		filler.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
		for (int slot = 0; slot < 54; slot++) {
			page.setItem(slot, filler.copy());
		}

		int here = State.quest(player);

		// Your money and the date, along the top.
		page.setItem(4, entry(Items.GOLD_NUGGET, "Your Money", ChatFormatting.GOLD,
				State.dollars(State.money(player)),
				State.arcade(player) + " arcade tickets",
				State.event(player) + " event tickets",
				"",
				Cal.date(),
				Cal.eventToday() == null
						? "No event today."
						: "Today: " + Cal.eventToday()));

		// One quest per slot along the middle two rows.
		for (int index = 0; index < Quests.ALL.length && index < 14; index++) {
			int slot = 19 + (index / 7) * 9 + (index % 7);
			page.setItem(slot, questIcon(player, index, here));
		}

		page.setItem(49, entry(Items.BARRIER, "Close", ChatFormatting.RED, "Press Escape."));

		player.openMenu(new SimpleMenuProvider(
				(id, inventory, who) -> new ReadOnlyMenu(id, inventory, page,
						(clicker, slot) -> click(clicker, slot, here)),
				Component.literal("Quest Book")));
	}

	private static ItemStack questIcon(ServerPlayer player, int index, int here) {
		Quests.Quest quest = Quests.ALL[index];
		List<String> lines = new ArrayList<>();
		lines.add(quest.chapter());
		lines.add("");

		if (index < here) {
			lines.add("Done.");
			lines.add("Reward: " + quest.reward());
			return entry(Items.BOOK, quest.name(), ChatFormatting.DARK_GRAY,
					lines.toArray(new String[0]));
		}
		if (index > here) {
			lines.add("Not yet. Finish the one before it.");
			return entry(Items.BOOK, quest.name(), ChatFormatting.DARK_GRAY,
					lines.toArray(new String[0]));
		}

		// The one you are on: every part, with the one you are on marked.
		int part = State.part(player);
		for (int i = 0; i < quest.parts().size(); i++) {
			Quests.Part step = quest.parts().get(i);
			String mark = i < part ? "✓ " : i == part ? "▶ " : "☐ ";
			String progress = "";
			if (i == part && step.need() > 1) {
				progress = "   (" + (Quests.on(player, 0, 1)
						&& player.level() instanceof net.minecraft.server.level.ServerLevel level
						? Chores.lawnLine(level)
						: State.count(player) + " / " + step.need()) + ")";
			}
			lines.add(mark + step.todo() + progress);
		}
		lines.add("");
		lines.add("Reward: " + quest.reward());
		Quests.Part step = quest.parts().get(part);
		BlockPos where = step.where();
		int blocks = (int) Math.round(Math.sqrt(player.distanceToSqr(
				where.getX() + 0.5, where.getY(), where.getZ() + 0.5)));
		lines.add(blocks + " blocks away");
		lines.add("");
		lines.add("Click to put the star on it.");

		return entry(Items.WRITTEN_BOOK, quest.name(), ChatFormatting.YELLOW,
				lines.toArray(new String[0]));
	}

	private static void click(ServerPlayer player, int slot, int here) {
		if (slot == 49) {
			player.closeContainer();
			return;
		}
		// Clicking the quest you are on re-points the star at it.
		Quests.Part part = Quests.currentPart(player);
		if (part != null) {
			BlockPos where = part.where();
			int blocks = (int) Math.round(Math.sqrt(player.distanceToSqr(
					where.getX() + 0.5, where.getY(), where.getZ() + 0.5)));
			player.sendSystemMessage(Component.literal("★ ")
					.withStyle(Hud.colourFor(blocks))
					.append(Component.literal(part.todo() + " -- " + blocks + " blocks")
							.withStyle(ChatFormatting.WHITE)));
		}
	}

	static ItemStack entry(Item item, String name, ChatFormatting colour, String... lines) {
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponents.CUSTOM_NAME,
				Component.literal(name).withStyle(colour).withStyle(style -> style.withItalic(false)));
		List<Component> lore = new ArrayList<>();
		for (String line : lines) {
			lore.add(Component.literal(line).withStyle(ChatFormatting.GRAY)
					.withStyle(style -> style.withItalic(false)));
		}
		stack.set(DataComponents.LORE, new ItemLore(lore));
		return stack;
	}
}
