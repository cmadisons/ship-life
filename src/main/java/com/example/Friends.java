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
	public static final int IZZY = 1;

	public static boolean knows(ServerPlayer player, int friend) {
		return (player.getAttachedOrCreate(MET) & (1 << friend)) != 0;
	}

	public static void register() {
		// Clicking the person themselves, which is what anybody would do.
		net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register(
				(player, world, hand, entity, hit) -> {
					if (!(player instanceof ServerPlayer who) || !(world instanceof ServerLevel level)
							|| !(entity instanceof Person) || entity.getCustomName() == null) {
						return InteractionResult.PASS;
					}
					// Two people can share a label -- both friends are called
					// Friend -- so which floor they are standing on is what
					// says which of them this is.
					String label = entity.getCustomName().getString();
					int floor = Places.floorAt(entity.getY());
					if (label.equals(Person.BEN)) {
						if (floor >= 16) {
							izzy(who, level);
						} else {
							ben(who, level);
						}
					} else if (label.equals(Person.CHARLIE)) {
						Chores.charlie(who);
					} else if (label.equals(Person.COOK)) {
						Buffet.serve(who);
					} else if (label.equals(Person.DESK)) {
						Chores.security(who);
					} else {
						return InteractionResult.PASS;
					}
					return InteractionResult.SUCCESS;
				});

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
			if (pos.equals(Places.IZZY) || pos.equals(Places.IZZY.above())) {
				izzy(who, level);
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

	/** What the rest of Ben's set costs off Izzy. */
	public static final int SET_COST = 100;

	/**
	 * Izzy, on floor 16.
	 *
	 * Ben has the coat; Izzy has the boots that go with it, and she hands
	 * those over the first time you knock. The helmet and the leggings that
	 * finish the set are a hundred event tickets, which is what her counter
	 * is for.
	 */
	private static void izzy(ServerPlayer player, ServerLevel level) {
		level.playSound(null, Places.IZZY, SoundEvents.NOTE_BLOCK_BELL.value(),
				SoundSource.PLAYERS, 0.6f, 1.8f);

		if (!knows(player, IZZY)) {
			player.setAttached(MET, player.getAttachedOrCreate(MET) | (1 << IZZY));
			player.sendSystemMessage(Component.literal(
					"Izzy: \"You came up the hard way, then -- nobody gets to 16 "
					+ "without going through the fight room. I'm Izzy.\"")
					.withStyle(ChatFormatting.WHITE));
			player.sendSystemMessage(Component.literal("Izzy is your second friend.")
					.withStyle(ChatFormatting.LIGHT_PURPLE));
			bootsFor(player);
			return;
		}

		// Anyone who met her before she had anything to give still gets them.
		if (bootsFor(player)) {
			return;
		}

		// The rest of the set, if they have not got it yet.
		if (!Kit.is(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD),
				Kit.HELMET)) {
			counter(player, true);
			return;
		}

		int waves = State.tally(player, State.WAVES);
		int bosses = State.tally(player, State.BOSSES);
		String about;
		if (bosses > 0) {
			about = bosses + " boss" + (bosses == 1 ? "" : "es") + " down. "
					+ "You are getting good at this.";
		} else if (waves > 2) {
			about = waves + " waves and no boss yet. The doors are right there.";
		} else {
			about = "Been down on 9 lately?";
		}
		player.sendSystemMessage(Component.literal("Izzy: \"" + about + "\"")
				.withStyle(ChatFormatting.WHITE));
	}

	/** The boots, handed over once. */
	private static boolean bootsFor(ServerPlayer player) {
		if (!State.firstTime(player, 3)) {
			return false;
		}
		player.sendSystemMessage(Component.literal(
				"Izzy: \"Ben gave you the coat, did he. These go with it -- "
				+ "same leaves, same trick. The hat and the legs I want "
				+ SET_COST + " event tickets for.\"")
				.withStyle(ChatFormatting.WHITE));
		give(player, Kit.boots());
		player.sendSystemMessage(Component.literal("Izzy gave you the boots.")
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

	private static void counter(ServerPlayer player) {
		counter(player, false);
	}

	/** Ben's counter, or Izzy's -- three more bombs, or the rest of the set. */
	private static void counter(ServerPlayer player, boolean izzy) {
		// Six rows, because the menu it opens in is a chest.
		net.minecraft.world.SimpleContainer page = new net.minecraft.world.SimpleContainer(54);
		net.minecraft.world.item.ItemStack filler = Game.cell(
				net.minecraft.world.item.Items.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
		for (int slot = 0; slot < 54; slot++) {
			page.setItem(slot, filler.copy());
		}
		int cost = izzy ? SET_COST : BOMBS_COST;
		boolean afford = State.event(player) >= cost;
		page.setItem(22, izzy
				? Book.entry(Made.benHelmet, "The Rest of the Set",
						afford ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY,
						"The helmet and the leggings.",
						"Every piece you wear stops another",
						"tenth of the hit and banks it.",
						"",
						cost + " event tickets",
						"You have " + State.event(player) + ".",
						afford ? "Click to buy." : "Not enough yet.")
				: Book.entry(Made.bomb, "3 of Ben's Bombs",
						afford ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY,
						"Green gas: 10 a second off every",
						"enemy standing in it.",
						"It stays until they are all dead.",
						"",
						cost + " event tickets",
						"You have " + State.event(player) + ".",
						afford ? "Click to buy." : "Not enough yet."));
		page.setItem(49, Book.entry(net.minecraft.world.item.Items.BARRIER, "Close",
				ChatFormatting.RED, "Press Escape."));
		player.openMenu(new net.minecraft.world.SimpleMenuProvider(
				(id, inventory, who) -> new ReadOnlyMenu(id, inventory, page,
						izzy ? Friends::buySet : Friends::buy),
				Component.literal(izzy ? "Izzy's Armoury" : "Ben's Bombs")));
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
		State.spendEvent(player, BOMBS_COST);
		give(player, Kit.bomb(3));
		player.sendSystemMessage(Component.literal("Ben: \"Three more. Stand back this time.\"")
				.withStyle(ChatFormatting.WHITE));
		player.closeContainer();
	}

	private static void buySet(ServerPlayer player, int slot) {
		if (slot == 49) {
			player.closeContainer();
			return;
		}
		if (slot != 22) {
			return;
		}
		if (State.event(player) < SET_COST) {
			player.sendSystemMessage(Component.literal("That is " + SET_COST
					+ " event tickets and you have " + State.event(player) + ".")
					.withStyle(ChatFormatting.RED));
			return;
		}
		State.spendEvent(player, SET_COST);
		give(player, Kit.helmet());
		give(player, Kit.leggings());
		player.sendSystemMessage(Component.literal(
				"Izzy: \"That is the lot. Wear all four and nothing much gets through.\"")
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
