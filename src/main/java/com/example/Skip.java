package com.example;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * The commands: /shiplife, /11reward and /skipsidequest.
 *
 * /shiplife starts you at the ship instead of at the sink.
 *
 * Chapter 1 is the dishes, the lawn and the penny, and it is the same twenty
 * minutes every time you want to look at something further up the ship. So the
 * command pays out what that chapter is worth, takes back the tools there is
 * nothing left to use on, and puts you down in the lobby on floor 1.
 *
 * It leaves you at the top of chapter 2 rather than skipping that too: you are
 * standing in the ship, so boarding it is done, and what is left is the walk to
 * the security desk for your passport. Everything above floor 1 still wants
 * that passport, and handing it over here would skip a chapter nobody asked to
 * skip.
 */
public final class Skip {
	private Skip() {
	}

	/** What chapter 1 pays in all: $5.00 + $94.99 + $0.01. */
	private static final int CHAPTER_ONE = 10_000;

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
			dispatcher.register(Commands.literal("shiplife")
					.executes(context -> run(context.getSource()))
					// Every floor at once, for looking at the ones you have not
					// earned yet.
					.then(Commands.literal("allfloors")
							.executes(context -> allFloors(context.getSource()))));
			// Floor 11 hands out one a month, and a month is ten real hours.
			// This is the same roll with the wait taken off, as many times as
			// you want it.
			dispatcher.register(Commands.literal("11reward")
					.executes(context -> extraReward(context.getSource())));
			// Every side quest you are carrying, finished and paid for.
			dispatcher.register(Commands.literal("skipsidequest")
					.executes(context -> skipSide(context.getSource())));
		});
	}

	/** /11reward -- another floor 11 reward, no waiting, as often as you like. */
	private static int extraReward(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("Only a player can use /11reward."));
			return 0;
		}
		Shops.reward(player);
		return 1;
	}

	/** /skipsidequest -- clear the lot, and pay what they were worth. */
	private static int skipSide(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("Only a player can use /skipsidequest."));
			return 0;
		}
		int done = QuestPool.skip(player);
		if (done == 0) {
			source.sendFailure(Component.literal("You are not carrying any side quests."));
			return 0;
		}
		player.sendSystemMessage(Component.literal(done + " side quest"
				+ (done == 1 ? "" : "s") + " skipped and paid.")
				.withStyle(ChatFormatting.YELLOW));
		return 1;
	}

	/**
	 * /shiplife allfloors -- open the whole ship at once.
	 *
	 * The lift reads your passport rather than a list, so opening the floors
	 * without one in your pocket would leave the buttons dark. It hands one
	 * over if you have not been given yours yet.
	 */
	private static int allFloors(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("Only a player can use /shiplife allfloors."));
			return 0;
		}
		if (!(player.level() instanceof ServerLevel level) || !ShipLifeMod.isShipLife(level)) {
			source.sendFailure(Component.literal("This is not a Ship Life world."));
			return 0;
		}

		int opened = 0;
		for (int floor = 1; floor <= Places.TOP_FLOOR; floor++) {
			if (!State.hasFloor(player, floor)) {
				State.unlock(player, floor);
				opened++;
			}
		}
		if (!Kit.is(player.getInventory().getItem(Slots.PASSPORT_SLOT), Kit.PASSPORT)) {
			player.getInventory().setItem(Slots.PASSPORT_SLOT, Kit.passport());
		}

		if (opened == 0) {
			source.sendFailure(Component.literal("Every floor is already open."));
			return 0;
		}
		player.sendSystemMessage(Component.literal(opened + " floor"
				+ (opened == 1 ? "" : "s") + " opened -- all " + Places.TOP_FLOOR
				+ " of them are yours.").withStyle(ChatFormatting.AQUA));
		return 1;
	}

	private static int run(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("Only a player can use /shiplife."));
			return 0;
		}
		if (!(player.level() instanceof ServerLevel level) || !ShipLifeMod.isShipLife(level)) {
			source.sendFailure(Component.literal("This is not a Ship Life world."));
			return 0;
		}
		if (State.quest(player) > 0) {
			source.sendFailure(Component.literal("Chapter 1 is already behind you."));
			return 0;
		}

		// The hundred dollars, however much of it you had already earned.
		if (State.money(player) < CHAPTER_ONE) {
			State.pay(player, CHAPTER_ONE - State.money(player));
		}
		Kit.dropChores(player);

		// Chapter 2, standing in the ship rather than out front of house one.
		player.setAttached(State.QUEST, 1);
		player.setAttached(State.PART, 0);
		State.count(player, 0);

		BlockPos lobby = Places.lift(1);
		player.teleportTo(lobby.getX() + 0.5, lobby.getY(), lobby.getZ() + 0.5);
		Quests.finishPart(player);          // you are inside it: boarding is done

		player.sendSystemMessage(Component.literal("Chapter 1 skipped -- "
				+ State.dollars(State.money(player)) + " and you are on floor 1.")
				.withStyle(ChatFormatting.YELLOW));
		return 1;
	}
}
