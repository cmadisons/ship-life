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
	 * Three a food on Snake, fifteen a stage on Galaga, and twenty for beating
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
				// One ticket a food was an evening at the machines for the
				// price of a pet. The cabinets pay like the rest of the ship
				// now: a good run is worth an event.
				pay(player, 3 * amount, amount + (amount == 1 ? " food" : " foods"));
				remember(player, "snake", amount);
			}
			case "galaga:stage" -> {
				State.add(player, State.ROUNDS, amount);
				pay(player, 15 * amount, "stage " + amount + " cleared");
				remember(player, "galaga", amount);
			}
			case "pacman:score" -> {
				if (amount > State.best(player)) {
					State.best(player, amount);
					pay(player, 20, "a new record of " + amount);
					remember(player, "pacman", amount);
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

	/**
	 * The board at each cabinet: your best five, kept in order.
	 *
	 * A machine that only remembered your very best gave you nothing to chase
	 * on the way there. Five is enough to see yourself getting better.
	 */
	public static void remember(ServerPlayer player, String game, int score) {
		java.util.List<String> games = new java.util.ArrayList<>(
				java.util.List.of(State.get(player, State.TOP_FIVE).split("\\|")));
		java.util.List<Integer> mine = new java.util.ArrayList<>();
		games.removeIf(entry -> entry.isEmpty());
		for (String entry : new java.util.ArrayList<>(games)) {
			if (entry.startsWith(game + ":")) {
				for (String number : entry.substring(game.length() + 1).split(",")) {
					if (!number.isEmpty()) {
						mine.add(Integer.parseInt(number));
					}
				}
				games.remove(entry);
			}
		}
		mine.add(score);
		mine.sort(java.util.Comparator.reverseOrder());
		while (mine.size() > 5) {
			mine.remove(mine.size() - 1);
		}
		StringBuilder line = new StringBuilder(game + ":");
		for (int i = 0; i < mine.size(); i++) {
			line.append(i == 0 ? "" : ",").append(mine.get(i));
		}
		games.add(line.toString());
		State.set(player, State.TOP_FIVE, String.join("|", games));
	}

	/** The five, in words, for a screen to show. */
	public static java.util.List<String> board(ServerPlayer player, String game) {
		java.util.List<String> lines = new java.util.ArrayList<>();
		for (String entry : State.get(player, State.TOP_FIVE).split("\\|")) {
			if (!entry.startsWith(game + ":")) {
				continue;
			}
			String[] numbers = entry.substring(game.length() + 1).split(",");
			for (int i = 0; i < numbers.length; i++) {
				lines.add((i + 1) + ".  " + numbers[i]);
			}
		}
		if (lines.isEmpty()) {
			lines.add("Nothing on this one yet.");
		}
		return lines;
	}

	/** Pay arcade tickets, doubled if it is Summer Break. */
	private static void pay(ServerPlayer player, int tickets, String why) {
		if ("Summer Break".equals(Events.running(player))) {
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
