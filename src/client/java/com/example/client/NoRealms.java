package com.example.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

import net.minecraft.client.gui.screens.TitleScreen;

/**
 * No Realms button.
 *
 * Ship Life is one person on one ship in one world, and the first screen of
 * the game offered to sell a server. The button is taken off the title screen
 * rather than hidden: nothing else changes, and the buttons under it move up
 * to close the gap the way the screen already lays them out.
 */
public final class NoRealms {
	private NoRealms() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof TitleScreen)) {
				return;
			}
			Screens.getWidgets(screen).removeIf(widget -> {
				String label = widget.getMessage().getString().toLowerCase();
				return label.contains("realms");
			});
		});
	}
}
