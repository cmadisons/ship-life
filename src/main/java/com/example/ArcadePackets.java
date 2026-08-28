package com.example;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * The two lines the cabinets and the screen say to each other.
 *
 * The arcade games are played on the client, because a game you steer with the
 * keys has to be drawn where the keys are. So the machine says "open Snake"
 * and the screen says back "ate a food" or "cleared stage 3", and the tickets
 * are counted here, on the server, where they are kept.
 */
public final class ArcadePackets {
	private ArcadePackets() {
	}

	/** Server to client: put this game on the screen. */
	public record Open(String game) implements CustomPacketPayload {
		public static final Type<Open> TYPE = new Type<>(ShipLifeMod.id("arcade_open"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC =
				StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Open::game, Open::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Client to server: something happened worth counting, as "game:what:how many". */
	public record Score(String line) implements CustomPacketPayload {
		public static final Type<Score> TYPE = new Type<>(ShipLifeMod.id("arcade_score"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Score> CODEC =
				StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Score::line, Score::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Tickets earned at the arcade before the store opens. */
	public static final int STORE_AT = 50;

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(Open.TYPE, Open.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Score.TYPE, Score.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Score.TYPE,
				(payload, context) -> context.server().execute(
						() -> score(context.player(), payload.line())));
	}

	/** Send someone to a machine. */
	public static void open(ServerPlayer player, String game) {
		ServerPlayNetworking.send(player, new Open(game));
	}

	/**
	 * What the machines pay.
	 *
	 * A ticket a food on Snake, five a stage on Galaga, and five for beating
	 * your own record on Pac-Man -- which is why that one sends its score and
	 * the others send what they did.
	 */
	private static void score(ServerPlayer player, String line) {
		String[] bits = line.split(":");
		if (bits.length < 3) {
			return;
		}
		int amount = Integer.parseInt(bits[2]);
		switch (bits[0] + ":" + bits[1]) {
			case "snake:food" -> {
				State.add(player, State.FOODS, amount);
				pay(player, amount, amount + (amount == 1 ? " food" : " foods"));
			}
			case "galaga:stage" -> {
				State.add(player, State.ROUNDS, amount);
				pay(player, 5 * amount, "stage " + amount + " cleared");
			}
			case "pacman:score" -> {
				if (amount > State.best(player)) {
					State.best(player, amount);
					pay(player, 5, "a new record of " + amount);
				} else {
					player.sendSystemMessage(Component.literal(amount
							+ " -- your best is still " + State.best(player) + ".")
							.withStyle(ChatFormatting.GRAY));
				}
			}
			default -> {
			}
		}
	}

	/** Pay arcade tickets, doubled if it is Summer Break. */
	private static void pay(ServerPlayer player, int tickets, String why) {
		if ("Summer Break".equals(Cal.eventToday())) {
			tickets *= 2;
			why = why + ", doubled for Summer Break";
			// Summer Break is an event you attend by turning up at the arcade.
			Events.didAnEvent(player, "you took Summer Break's double tickets");
		}
		State.arcade(player, tickets);
		State.add(player, State.EARNED, tickets);
		player.sendSystemMessage(Component.literal("+" + tickets + " ticket"
				+ (tickets == 1 ? "" : "s") + "  --  " + why + ". You have "
				+ State.arcade(player) + ".").withStyle(ChatFormatting.GREEN));

		// Fifty tickets earned at the arcade opens the store on floor 8. It is
		// counted on what you have won rather than what you are holding, so
		// spending your tickets on a pet cannot take the floor away again.
		if (State.tally(player, State.EARNED) >= STORE_AT && !State.hasFloor(player, 8)) {
			State.unlock(player, 8);
			player.sendSystemMessage(Component.literal(
					"Fifty tickets earned. Floor 8 -- the store -- is open.")
					.withStyle(ChatFormatting.AQUA));
		}
	}
}
