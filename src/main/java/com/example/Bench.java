package com.example;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * The bench on floor 17: putting work into the plant set.
 *
 * Bosses leave drops and this is what the drops are for. Two of anything a
 * boss left buys one enchantment on one piece you are wearing, and the piece
 * keeps it -- so the set gets better the further down floor 10 you have been,
 * rather than only being the same four pieces forever.
 */
public final class Bench {
	private Bench() {
	}

	/** What one enchantment costs in boss drops. */
	public static final int DROPS = 2;

	/** The work you can have done, and what it does. */
	private record Work(String name, String what,
			net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> what2,
			int level) {
	}

	private static final Work[] WORK = {
			new Work("Thicker Leaves", "Protection IV -- less of everything gets through.",
					Enchantments.PROTECTION, 4),
			new Work("Thorned", "Thorns III -- what hits you takes some back.",
					Enchantments.THORNS, 3),
			new Work("Fireproof Sap", "Fire Protection IV. The Magma Boss is on 10.",
					Enchantments.FIRE_PROTECTION, 4),
			new Work("Deep Roots", "Blast Protection IV -- creepers on 9 stop mattering.",
					Enchantments.BLAST_PROTECTION, 4),
			new Work("Light Growth", "Feather Falling IV, for the balcony.",
					Enchantments.FEATHER_FALLING, 4),
	};

	/** Open the bench. */
	public static void open(ServerPlayer player) {
		SimpleContainer page = Comforts.blank();
		int drops = counted(player);

		page.setItem(4, Book.entry(Items.ENCHANTING_TABLE, "The Bench", ChatFormatting.AQUA,
				"Two boss drops buys one piece of work.",
				"It goes on whatever plant piece you are wearing.",
				"You have " + drops + " drop" + (drops == 1 ? "" : "s") + "."));

		for (int i = 0; i < WORK.length; i++) {
			page.setItem(19 + i, Book.entry(Items.LAPIS_LAZULI, WORK[i].name(),
					drops >= DROPS ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_GRAY,
					WORK[i].what(),
					DROPS + " boss drops",
					drops >= DROPS ? "Click to have it done." : "Not enough drops."));
		}

		page.setItem(49, Book.entry(Items.BARRIER, "Close", ChatFormatting.RED,
				"Press Escape."));
		player.openMenu(new SimpleMenuProvider(
				(id, inventory, who) -> new ReadOnlyMenu(id, inventory, page, Bench::work),
				Component.literal("The Bench")));
	}

	/** How many boss drops they are carrying. */
	private static int counted(ServerPlayer player) {
		int found = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (Kit.isDrop(stack)) {
				found += stack.getCount();
			}
		}
		return found;
	}

	/** Take the drops off them, whichever bosses they came from. */
	private static boolean take(ServerPlayer player, int howMany) {
		int left = howMany;
		for (int slot = 0; slot < player.getInventory().getContainerSize() && left > 0; slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (!Kit.isDrop(stack)) {
				continue;
			}
			int taken = Math.min(left, stack.getCount());
			stack.shrink(taken);
			left -= taken;
		}
		return left == 0;
	}

	private static void work(ServerPlayer player, int slot) {
		if (slot == 49) {
			player.closeContainer();
			return;
		}
		int index = slot - 19;
		if (index < 0 || index >= WORK.length) {
			return;
		}
		if (counted(player) < DROPS) {
			player.sendSystemMessage(Component.literal("That is " + DROPS
					+ " boss drops. Floor 10 has five of them.")
					.withStyle(ChatFormatting.RED));
			return;
		}

		// Whatever piece of the set is on you, starting at the head.
		ItemStack piece = ItemStack.EMPTY;
		for (EquipmentSlot where : new EquipmentSlot[] {
				EquipmentSlot.HEAD, EquipmentSlot.CHEST,
				EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
			ItemStack worn = player.getItemBySlot(where);
			if (Kit.isPiece(worn, Kit.HELMET) || Kit.isPiece(worn, Kit.ARMOUR)
					|| Kit.isPiece(worn, Kit.LEGGINGS) || Kit.isPiece(worn, Kit.BOOTS)) {
				piece = worn;
				break;
			}
		}
		if (piece.isEmpty()) {
			player.sendSystemMessage(Component.literal(
					"Put a piece of the plant set on first -- the work goes on what "
					+ "you are wearing.").withStyle(ChatFormatting.GRAY));
			return;
		}

		take(player, DROPS);
		Work chosen = WORK[index];
		var registry = player.level().registryAccess()
				.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
		piece.enchant(registry.getOrThrow(chosen.what2()), chosen.level());

		player.level().playSound(null, player.blockPosition(),
				SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8f, 1.2f);
		player.sendSystemMessage(Component.literal(chosen.name() + " -- done. "
				+ piece.getHoverName().getString() + " is better than it was.")
				.withStyle(ChatFormatting.LIGHT_PURPLE));
		player.closeContainer();
	}
}
