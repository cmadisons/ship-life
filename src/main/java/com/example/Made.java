package com.example;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The things Ship Life adds that Minecraft does not have.
 *
 * Everything you actually handle is here: the sponge and the towel, the weed
 * whacker and the lawn mower, the plunger for the toilet, one of Ben's bombs,
 * the dish on the counter and the button you press to call the lift. They were vanilla items wearing a name to begin with,
 * which reads well enough in a list and badly in your hand -- a towel is not a
 * block of wool, and a dish is not a flower pot.
 *
 * The pictures are drawn by tools/make_textures.py, sixteen pixels square, one
 * character of string art a pixel. Change the picture there, not here, and
 * never the PNGs.
 */
public final class Made {
	private Made() {
	}

	public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
	public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();

	public static Item sponge;
	public static Item towel;
	public static Item weedWhacker;
	public static Item lawnMower;
	public static Item plunger;
	public static Item bomb;
	public static Item benArmour;
	public static Item benBoots;
	public static Item benHelmet;
	public static Item benLeggings;

	/**
	 * The picture the game uses for Ben's armour once it is on you.
	 *
	 * Held in your hand an item is its own 16x16 picture, but worn it is a
	 * separate sheet drawn over the body -- this is the name the game looks
	 * that sheet up by, and assets/shiplife/equipment/ben_armour.json is what
	 * it finds.
	 */
	private static final ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> LEAF_ASSET =
			ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID,
					ShipLifeMod.id("ben_armour"));

	/** Leaves and cloth: about as tough as iron, because Ben means it. */
	private static final net.minecraft.world.item.equipment.ArmorMaterial LEAF =
			new net.minecraft.world.item.equipment.ArmorMaterial(
					15,
					java.util.Map.of(
							net.minecraft.world.item.equipment.ArmorType.HELMET, 3,
							net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, 6,
							net.minecraft.world.item.equipment.ArmorType.LEGGINGS, 5,
							net.minecraft.world.item.equipment.ArmorType.BOOTS, 3),
					9,
					net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER,
					1.0f,
					0.0f,
					net.minecraft.tags.ItemTags.REPAIRS_LEATHER_ARMOR,
					LEAF_ASSET);
	public static Block dish;
	public static Block elevatorButton;

	/** Called once at start-up, before any world exists. */
	public static void register() {
		sponge = item("sponge");
		towel = item("towel");
		weedWhacker = item("weed_whacker");
		lawnMower = item("lawn_mower");
		plunger = item("plunger");
		// Ben hands you three at once, so this one has to stack.
		bomb = item("bomb", 8);
		benArmour = armour("ben_armour",
				net.minecraft.world.item.equipment.ArmorType.CHESTPLATE);
		benBoots = armour("ben_boots", net.minecraft.world.item.equipment.ArmorType.BOOTS);
		benHelmet = armour("ben_helmet", net.minecraft.world.item.equipment.ArmorType.HELMET);
		benLeggings = armour("ben_leggings",
				net.minecraft.world.item.equipment.ArmorType.LEGGINGS);

		// The dish is china: it breaks by hand and it is not worth a tool.
		dish = block("dish", 0.4f, SoundType.GLASS);
		elevatorButton = block("elevator_button", 1.5f, SoundType.METAL);

		ShipLifeMod.LOGGER.info("Ship Life added {} items and {} blocks.",
				ITEMS.size(), BLOCKS.size());
	}

	private static Item item(String id) {
		return item(id, 1);
	}

	private static Item item(String id, int stack) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ShipLifeMod.id(id));
		Item made = Registry.register(BuiltInRegistries.ITEM, key,
				new Item(new Item.Properties().setId(key).stacksTo(stack)));
		ITEMS.put(id, made);
		return made;
	}

	/** A chestplate you can actually wear, with its own worn picture. */
	private static Item armour(String id, net.minecraft.world.item.equipment.ArmorType type) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ShipLifeMod.id(id));
		Item made = Registry.register(BuiltInRegistries.ITEM, key,
				new Item(new Item.Properties().setId(key)
						.humanoidArmor(LEAF, type)));
		ITEMS.put(id, made);
		return made;
	}

	private static Block block(String id, float hardness, SoundType sound) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, ShipLifeMod.id(id));
		Block made = Registry.register(BuiltInRegistries.BLOCK, blockKey,
				new Block(BlockBehaviour.Properties.of()
						.strength(hardness)
						.sound(sound)
						.setId(blockKey)));
		BLOCKS.put(id, made);

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, ShipLifeMod.id(id));
		Registry.register(BuiltInRegistries.ITEM, itemKey,
				new BlockItem(made, new Item.Properties().setId(itemKey)
						.useBlockDescriptionPrefix()));
		return made;
	}
}
