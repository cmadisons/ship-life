package com.example;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * The things in your room and the lobby that are not chores.
 *
 * The shower, the wardrobe, the wall you hang your bosses on, the map on the
 * lobby wall and the intercom that tells you what is on today. None of them
 * are worth a class each: they are all one block, one click, one thing that
 * happens.
 */
public final class Comforts {
	private Comforts() {
	}

	/** How often the ship's speakers pick the record up again. */
	private static final int EVERY = 400;

	public static void register() {
		// A record player in one room is a record player in one room. This
		// one is wired to the ship: while there is a disc turning on floor 5,
		// everybody aboard hears it, wherever they are.
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK
				.register(server -> {
			if (server.getTickCount() % EVERY != 0) {
				return;
			}
			for (ServerLevel level : server.getAllLevels()) {
				if (!ShipLifeMod.isShipLife(level)) {
					continue;
				}
				playToTheShip(level);
			}
		});

		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (!(player instanceof ServerPlayer who) || !(world instanceof ServerLevel level)
					|| !ShipLifeMod.isShipLife(level)) {
				return InteractionResult.PASS;
			}
			BlockPos pos = Places.local(hit.getBlockPos());

			if (pos.equals(Places.SHOWER) || pos.equals(Places.SHOWER.below())) {
				shower(who, level);
				return InteractionResult.SUCCESS;
			}
			// The locker in your room, and the one on ship 2's pool deck --
			// the same locker, since what is in it is saved on you.
			if (pos.equals(Places.LOCKER) || pos.equals(Places.POOL_LOCKER)) {
				Fridge.openLocker(who);
				return InteractionResult.SUCCESS;
			}
			if (pos.equals(Places.WARDROBE) || pos.equals(Places.WARDROBE.above())
					|| pos.equals(Places.POOL_WARDROBE)
					|| pos.equals(Places.POOL_WARDROBE.above())) {
				wardrobe(who);
				return InteractionResult.SUCCESS;
			}
			if (isPhotoWall(pos)) {
				trophies(who, level);
				photos(who);
				return InteractionResult.SUCCESS;
			}
			if (pos.equals(Places.LOBBY_MAP)) {
				Book.map(who);
				return InteractionResult.SUCCESS;
			}
			if (pos.equals(Places.INTERCOM)) {
				intercom(who, level);
				return InteractionResult.SUCCESS;
			}
			if (pos.equals(Places.TELESCOPE) || pos.equals(Places.TELESCOPE.above())) {
				telescope(who, level);
				return InteractionResult.SUCCESS;
			}
			if (pos.equals(Places.GUEST_BELL)) {
				ringForSomebody(who, level);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});
	}

	/** Whatever is on the record player, played to everybody aboard. */
	private static void playToTheShip(ServerLevel level) {
		if (!(level.getBlockEntity(Places.JUKEBOX)
				instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity box)) {
			return;
		}
		ItemStack disc = box.getTheItem();
		if (disc.isEmpty()) {
			return;
		}
		var playable = disc.get(net.minecraft.core.component.DataComponents.JUKEBOX_PLAYABLE);
		if (playable == null) {
			return;
		}
		var song = playable.song().value();
		for (ServerPlayer listener : level.players()) {
			if (Places.floorAt(listener.getY()) == 0) {
				continue;                      // not aboard
			}
			level.playSound(null, listener.blockPosition(), song.soundEvent().value(),
					SoundSource.RECORDS, 0.5f, 1.0f);
		}
	}

	private static boolean isPhotoWall(BlockPos pos) {
		return pos.getX() == Places.PHOTOS.getX()
				&& Math.abs(pos.getZ() - Places.PHOTOS.getZ()) <= 1
				&& pos.getY() >= Places.PHOTOS.getY()
				&& pos.getY() <= Places.PHOTOS.getY() + 1;
	}

	// ---------------------------------------------------------------- shower

	/** Water, steam, and clean hearts. */
	private static void shower(ServerPlayer player, ServerLevel level) {
		level.playSound(null, Places.SHOWER, SoundEvents.BUCKET_EMPTY,
				SoundSource.BLOCKS, 0.8f, 1.6f);
		level.sendParticles(ParticleTypes.FALLING_WATER,
				Places.SHOWER.getX() + 0.5, Places.SHOWER.getY() - 0.2,
				Places.SHOWER.getZ() + 0.5, 40, 0.3, 1.0, 0.3, 0.0);
		player.heal(4.0f);
		player.removeAllEffects();
		player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
				net.minecraft.world.effect.MobEffects.REGENERATION, 200, 0));
		player.sendSystemMessage(Component.literal(
				"A hot shower. That is better.").withStyle(ChatFormatting.AQUA));
	}

	// -------------------------------------------------------------- wardrobe

	/** Four outfits, and you wear the one you pick. */
	private static void wardrobe(ServerPlayer player) {
		SimpleContainer page = blank();
		page.setItem(4, Book.entry(Items.LEATHER_CHESTPLATE, "Your Wardrobe",
				ChatFormatting.AQUA, "Pick something to wear.",
				"Ben and Izzy's armour goes over the top."));
		for (int i = 0; i < OUTFITS.length; i++) {
			Outfit outfit = OUTFITS[i];
			page.setItem(20 + i, Book.entry(Items.LEATHER_CHESTPLATE, outfit.name(),
					ChatFormatting.WHITE, outfit.what(), "", "Click to put it on."));
		}
		page.setItem(29, Book.entry(Items.BARRIER, "Take it all off",
				ChatFormatting.GRAY, "Back to nothing."));
		page.setItem(49, Book.entry(Items.BARRIER, "Close", ChatFormatting.RED,
				"Press Escape."));
		player.openMenu(new SimpleMenuProvider(
				(id, inventory, who) -> new ReadOnlyMenu(id, inventory, page,
						Comforts::wear),
				Component.literal("Wardrobe")));
	}

	private record Outfit(String name, String what, int colour) {
	}

	private static final Outfit[] OUTFITS = {
			new Outfit("Ship Uniform", "What the staff wear.", 0xE8ECF0),
			new Outfit("Swimming Kit", "For floor 3.", 0x3C9BE0),
			new Outfit("Racing Overalls", "For floor 6.", 0xD84A3C),
			new Outfit("Evening Wear", "For the events on 7.", 0x2B2F45),
	};

	private static void wear(ServerPlayer player, int slot) {
		if (slot == 49) {
			player.closeContainer();
			return;
		}
		if (slot == 29) {
			for (net.minecraft.world.entity.EquipmentSlot where : DRESSED) {
				if (!Kit.is(player.getItemBySlot(where), Kit.ARMOUR)) {
					player.setItemSlot(where, ItemStack.EMPTY);
				}
			}
			player.closeContainer();
			return;
		}
		int index = slot - 20;
		if (index < 0 || index >= OUTFITS.length) {
			return;
		}
		Outfit outfit = OUTFITS[index];
		put(player, net.minecraft.world.entity.EquipmentSlot.HEAD,
				Items.LEATHER_HELMET, outfit);
		put(player, net.minecraft.world.entity.EquipmentSlot.CHEST,
				Items.LEATHER_CHESTPLATE, outfit);
		put(player, net.minecraft.world.entity.EquipmentSlot.LEGS,
				Items.LEATHER_LEGGINGS, outfit);
		put(player, net.minecraft.world.entity.EquipmentSlot.FEET,
				Items.LEATHER_BOOTS, outfit);
		player.sendSystemMessage(Component.literal("You put on the " + outfit.name() + ".")
				.withStyle(ChatFormatting.AQUA));
		player.closeContainer();
	}

	private static final net.minecraft.world.entity.EquipmentSlot[] DRESSED = {
			net.minecraft.world.entity.EquipmentSlot.HEAD,
			net.minecraft.world.entity.EquipmentSlot.CHEST,
			net.minecraft.world.entity.EquipmentSlot.LEGS,
			net.minecraft.world.entity.EquipmentSlot.FEET,
	};

	/**
	 * Put one piece on, unless Ben or Izzy's is already there.
	 *
	 * Clothes are clothes; the armour is the thing that keeps a tenth of every
	 * hit off you, and nobody wants to lose that to a change of shirt.
	 */
	private static void put(ServerPlayer player, net.minecraft.world.entity.EquipmentSlot where,
			net.minecraft.world.item.Item item, Outfit outfit) {
		ItemStack worn = player.getItemBySlot(where);
		if (Kit.isPiece(worn, Kit.ARMOUR) || Kit.isPiece(worn, Kit.BOOTS)
				|| Kit.isPiece(worn, Kit.HELMET) || Kit.isPiece(worn, Kit.LEGGINGS)) {
			return;
		}
		ItemStack piece = Kit.make(item, outfit.name(), ChatFormatting.WHITE, outfit.what());
		piece.set(net.minecraft.core.component.DataComponents.DYED_COLOR,
				new net.minecraft.world.item.component.DyedItemColor(outfit.colour()));
		piece.set(net.minecraft.core.component.DataComponents.UNBREAKABLE,
				net.minecraft.util.Unit.INSTANCE);
		player.setItemSlot(where, piece);
	}

	// ------------------------------------------------------------ the photos

	/**
	 * Put your trophies up on the wall for real.
	 *
	 * The screen listed them; the wall did not show them. Every boss you have
	 * put down and every floor you have earned puts a block on the shelf
	 * opposite your bed, so the room fills up as you get on with it.
	 */
	public static void trophies(ServerPlayer player, ServerLevel level) {
		BlockPos shelf = Places.PHOTOS.above(2);
		int bosses = State.tally(player, State.BOSSES);
		int floors = 0;
		for (int floor = 1; floor <= Places.TOP_FLOOR; floor++) {
			if (State.hasFloor(player, floor)) {
				floors++;
			}
		}

		put(level, shelf.north(), bosses >= 1 ? Blocks.COBWEB : null);
		put(level, shelf, bosses >= 2 ? Blocks.DRAGON_EGG : null);
		put(level, shelf.south(), bosses >= 3 ? Blocks.SOUL_LANTERN : null);
		put(level, shelf.north().above(), State.tally(player, State.WAVES) >= 5
				? Blocks.IRON_BLOCK : null);
		put(level, shelf.above(), floors >= 10 ? Blocks.GOLD_BLOCK : null);
		put(level, shelf.south().above(), floors >= Places.TOP_FLOOR
				? Blocks.DIAMOND_BLOCK : null);
	}

	/** One trophy, or nothing if it has not been won. */
	private static void put(ServerLevel level, BlockPos where,
			net.minecraft.world.level.block.Block block) {
		level.setBlockAndUpdate(where, block == null
				? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
				: block.defaultBlockState());
	}

	/** The wall of what you have put down. */
	private static void photos(ServerPlayer player) {
		SimpleContainer page = blank();
		int bosses = State.tally(player, State.BOSSES);
		int waves = State.tally(player, State.WAVES);

		page.setItem(4, Book.entry(Items.ITEM_FRAME, "Your Wall", ChatFormatting.AQUA,
				"Everything you have put down.",
				bosses == 0 ? "Nothing on it yet." : bosses + " bosses beaten"));
		page.setItem(20, Book.entry(bosses >= 1 ? Items.COBWEB : Items.GRAY_DYE,
				"Arachnes", bosses >= 1 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY,
				bosses >= 1 ? "Beaten." : "Still down there, floor 10."));
		page.setItem(22, Book.entry(bosses >= 2 ? Items.DRAGON_HEAD : Items.GRAY_DYE,
				"The Dragon", bosses >= 2 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY,
				bosses >= 2 ? "Beaten." : "Still down there, floor 10."));
		page.setItem(24, Book.entry(waves > 0 ? Items.IRON_SWORD : Items.GRAY_DYE,
				"The Waves", waves > 0 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY,
				waves + " cleared on floor 9"));
		page.setItem(49, Book.entry(Items.BARRIER, "Close", ChatFormatting.RED,
				"Press Escape."));
		player.openMenu(new SimpleMenuProvider(
				(id, inventory, who) -> new ReadOnlyMenu(id, inventory, page,
						(clicker, slot) -> clicker.closeContainer()),
				Component.literal("Your Wall")));
	}

	// ----------------------------------------------------------- the tannoy

	/** What is on today, said out loud. */
	public static void intercom(ServerPlayer player, ServerLevel level) {
		level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(),
				SoundSource.BLOCKS, 0.7f, 1.2f);
		String on = Events.running(player);
		String next = Cal.eventTomorrow();
		player.sendSystemMessage(Component.literal("*ding* ").withStyle(ChatFormatting.GOLD)
				.append(Component.literal(Cal.date() + ". ")
						.withStyle(ChatFormatting.GRAY))
				.append(Component.literal(on == null
								? "Nothing on today."
								: "Today on floor 7: " + on + ".")
						.withStyle(ChatFormatting.WHITE))
				.append(Component.literal(next == null
								? "  Nothing tomorrow either."
								: "  Tomorrow: " + next + ".")
						.withStyle(ChatFormatting.GRAY)));
	}

	/**
	 * The telescope on the balcony: it tells you what you are looking at.
	 *
	 * Which way you are facing is the whole of it. The ship is west, the town
	 * is west of that, the Nether is straight down through eighteen floors,
	 * and everything else out there is space.
	 */
	private static void telescope(ServerPlayer player, ServerLevel level) {
		level.playSound(null, Places.TELESCOPE, SoundEvents.SPYGLASS_USE,
				SoundSource.BLOCKS, 0.8f, 1.0f);
		float facing = player.getYRot();
		String seen;
		if (player.getXRot() > 45) {
			seen = "Straight down. Eighteen floors of ship, and then nothing.";
		} else if (player.getXRot() < -45) {
			seen = "Stars. They do not move, and neither do we, whatever the brochure said.";
		} else if (facing > -45 && facing < 45) {
			seen = "The far rail, and past it the dark.";
		} else if (facing >= 45 && facing < 135) {
			seen = "The hull, all the way up. Eighteen decks and the nose over the top.";
		} else if (facing >= -135 && facing <= -45) {
			seen = "Open space, and something a long way off that might be moving.";
		} else {
			seen = "The town: three houses, a lawn, and the walkway you came in on.";
		}
		player.sendSystemMessage(Component.literal("Through the telescope: ")
				.withStyle(ChatFormatting.GRAY)
				.append(Component.literal(seen).withStyle(ChatFormatting.WHITE)));
	}

	/**
	 * The bell in your room: somebody comes round.
	 *
	 * The guest cabin is your room with a friend in it. Ring the bell and
	 * whoever you know best turns up for a while -- Izzy if you have met her,
	 * Ben if not -- and stands about the way a guest does.
	 */
	private static void ringForSomebody(ServerPlayer player, ServerLevel level) {
		level.playSound(null, Places.GUEST_BELL, SoundEvents.BELL_BLOCK,
				SoundSource.BLOCKS, 0.8f, 1.0f);

		boolean izzy = Friends.knows(player, Friends.IZZY);
		boolean ben = Friends.knows(player, Friends.BEN);
		if (!ben && !izzy) {
			player.sendSystemMessage(Component.literal(
					"Nobody to ring for yet. Ben is on 15.").withStyle(ChatFormatting.GRAY));
			return;
		}
		BlockPos spot = Places.GUEST_BELL.south(2);
		String who = izzy ? Person.IZZY : Person.BEN;
		ChatFormatting colour = izzy ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA;

		// One guest at a time, and they go home when you next ring.
		for (Person guest : level.getEntitiesOfClass(Person.class,
				new net.minecraft.world.phys.AABB(spot).inflate(6.0))) {
			if (guest.getCustomName() != null
					&& guest.getCustomName().getString().equals(who)) {
				guest.discard();
				player.sendSystemMessage(Component.literal("They head back upstairs.")
						.withStyle(ChatFormatting.GRAY));
				return;
			}
		}
		Person.place(level, spot, who, colour);
		player.sendSystemMessage(Component.literal(
				"Somebody comes round. Ring again when you want the room back.")
				.withStyle(ChatFormatting.WHITE));
	}

	static SimpleContainer blank() {
		SimpleContainer page = new SimpleContainer(54);
		ItemStack filler = Game.cell(Items.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
		for (int slot = 0; slot < 54; slot++) {
			page.setItem(slot, filler.copy());
		}
		return page;
	}
}
