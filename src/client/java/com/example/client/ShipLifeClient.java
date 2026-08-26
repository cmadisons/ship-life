package com.example.client;

import net.fabricmc.api.ClientModInitializer;

/**
 * The client half of Ship Life.
 *
 * Nothing to do here yet. Everything the player sees -- the quest star, the
 * clock, the meters on the chores -- is written to the action bar by the
 * server, so it works in single player and on a server without the client
 * having to draw anything. A real heads-up display, with the star drawn in the
 * world and the clock pinned to the top-right corner, belongs here when it
 * comes.
 */
public class ShipLifeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Intentionally empty -- see the class comment above.
	}
}
