package com.example.client;

import com.example.HudPacket;
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

		HudElementRegistry.addLast(ShipLifeMod.id("hud"), Screen::draw);
	}
}
