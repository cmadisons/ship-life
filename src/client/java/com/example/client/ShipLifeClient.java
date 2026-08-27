package com.example.client;

import com.example.ArcadePackets;
import com.example.HudPacket;
import com.example.client.arcade.GalagaScreen;
import com.example.client.arcade.PacManScreen;
import com.example.client.arcade.SnakeScreen;
import com.example.ShipLifeMod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

/**
 * The client half of Ship Life.
 *
 * Two jobs: take the line the server sends four times a second, and draw it.
 * The drawing itself is in {@link Screen}.
 */
public class ShipLifeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(HudPacket.TYPE,
				(payload, context) -> context.client().execute(
						() -> Screen.take(payload.line())));

		// Leaving a world clears the display, so nothing is left over from it.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> Screen.forget());

		// The cabinets: the machine says which game, the screen puts it on.
		ClientPlayNetworking.registerGlobalReceiver(ArcadePackets.Open.TYPE,
				(payload, context) -> context.client().execute(() -> {
					switch (payload.game()) {
						case "snake" -> context.client().setScreen(new SnakeScreen());
						case "pacman" -> context.client().setScreen(new PacManScreen());
						case "galaga" -> context.client().setScreen(new GalagaScreen());
						default -> {
						}
					}
				}));

		HudElementRegistry.addLast(ShipLifeMod.id("hud"), Screen::draw);
	}
}
