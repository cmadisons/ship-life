package com.example;

import java.util.HashSet;
import java.util.Set;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ship Life: you live on a giant space ship.
 *
 * Pick "Ship Life" on the Create World screen. It is a world type like any
 * other, which is why it sits next to survival, creative and hardcore -- but
 * whichever of those you picked alongside it, you play survival. The whole
 * game is earning your way onto the ship and then earning your way up it, and
 * neither means anything if you can fly and have everything already.
 *
 * The world itself is an empty void that this mod builds into: a town with
 * three houses where you make your first hundred dollars, and the ship out to
 * the east with all fourteen floors standing from the start. See {@link Ship}.
 *
 * What is here so far is chapters 1 to 4 -- the dishes, the lawn, the penny,
 * boarding, your passport, your room, Charlie and his quest -- along with the
 * Quest Book, the star, the clock and the elevator. The arcade, the pool, the
 * race track, the pets and the events are floors waiting to be built into.
 */
public class ShipLifeMod implements ModInitializer {
	public static final String MOD_ID = "shiplife";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Worlds we have decided are Ship Life worlds, so we only work it out once. */
	private static final Set<String> SHIP_WORLDS = new HashSet<>();

	@Override
	public void onInitialize() {
		Ticker.register();
		Slots.register();
		Hud.register();
		Chores.register();

		// Right-clicking the book in mid-air, rather than at a block.
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (player instanceof ServerPlayer who && world instanceof ServerLevel level
					&& isShipLife(level) && Kit.is(player.getItemInHand(hand), Kit.QUEST_BOOK)) {
				Book.open(who);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				onJoin(handler.getPlayer()));

		LOGGER.info("Ship Life is aboard.");
	}

	/**
	 * Set the world up the first time anyone joins it.
	 *
	 * A Ship Life world is recognised by being an empty void where the ship
	 * should be. Once the ship is built that is no longer true, so the answer
	 * is remembered rather than worked out again.
	 */
	private static void onJoin(ServerPlayer player) {
		ServerLevel level = player.level() instanceof ServerLevel server ? server : null;
		if (level == null || level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
			return;
		}
		String key = level.dimension().identifier().toString();
		boolean known = SHIP_WORLDS.contains(key);
		boolean fresh = !known && isVoid(level);

		if (!known && !fresh && !isShipBuilt(level)) {
			return;                       // an ordinary world -- leave it alone
		}
		SHIP_WORLDS.add(key);

		if (fresh) {
			level.getGameRules().set(GameRules.KEEP_INVENTORY, true, level.getServer());
			Ship.build(level);
		}

		// Nothing we build is worth losing to a creeper or an enderman. Set on
		// every join rather than only on a fresh world, so worlds made before
		// this get it too.
		level.getGameRules().set(GameRules.MOB_GRIEFING, false, level.getServer());

		// However you made the world, you play survival.
		if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
			player.setGameMode(GameType.SURVIVAL);
		}

		if (State.firstTime(player, 0)) {
			BlockPos spawn = Places.SPAWN;
			player.teleportTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
			player.getInventory().setItem(Slots.BOOK_SLOT, Kit.questBook());
			player.getInventory().add(Kit.sponge());
			player.getInventory().add(Kit.towel());
			player.sendSystemMessage(Component.literal(
					"A letter for you: get $100 and you can live on the ship for a month.")
					.withStyle(ChatFormatting.YELLOW));
			player.sendSystemMessage(Component.literal(
					"Chapter 1 -- Make the Money. Start with the dishes in the first house.")
					.withStyle(ChatFormatting.AQUA));
			player.sendSystemMessage(Component.literal(
					"Your Quest Book is in slot 9. Right-click it any time.")
					.withStyle(ChatFormatting.GRAY));
		}
	}

	/** Is this one of our worlds? Everything else in the mod asks this first. */
	public static boolean isShipLife(ServerLevel level) {
		return SHIP_WORLDS.contains(level.dimension().identifier().toString());
	}

	/** Nothing at all where the town goes: a brand new Ship Life world. */
	private static boolean isVoid(ServerLevel level) {
		BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
		for (int y = level.getMinY(); y < level.getMaxY(); y++) {
			probe.set(0, y, 0);
			if (!level.getBlockState(probe).isAir()) {
				return false;
			}
		}
		return true;
	}

	/** A world we built earlier, coming back after a restart. */
	private static boolean isShipBuilt(ServerLevel level) {
		return level.getBlockState(Places.panel(1)).is(
				net.minecraft.world.level.block.Blocks.LODESTONE);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
