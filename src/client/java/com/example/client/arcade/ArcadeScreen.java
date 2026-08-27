package com.example.client.arcade;

import com.example.ArcadePackets;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import org.lwjgl.glfw.GLFW;

/**
 * The cabinet you actually stand in front of.
 *
 * The three arcade games are played here rather than in a chest screen,
 * because a game you steer with the keys has to be drawn where the keys are.
 * Each one is its own screen, ticking twenty times a second like the world
 * does, drawn as flat coloured squares -- which is all the original cabinets
 * ever were.
 *
 * Nothing about the game is on the server. What the server hears is what you
 * did -- a food eaten, a stage cleared, a score at the end -- and it decides
 * what that is worth. See {@link ArcadePackets}.
 */
public abstract class ArcadeScreen extends Screen {
	/** How wide and tall the playfield is, in cells. */
	protected final int columns;
	protected final int rows;

	/** Pixels to a cell. Worked out from the window when the screen opens. */
	protected int cell = 12;

	/** Where the playfield starts on screen. */
	protected int originX;
	protected int originY;

	/** Ticks since the game started. */
	protected int age;

	protected boolean over;
	protected boolean paused;

	protected ArcadeScreen(String title, int columns, int rows) {
		super(Component.literal(title));
		this.columns = columns;
		this.rows = rows;
	}

	@Override
	protected void init() {
		// Fit the board to the window, leaving room for the score line.
		cell = Math.max(4, Math.min((width - 40) / columns, (height - 80) / rows));
		originX = (width - columns * cell) / 2;
		originY = (height - rows * cell) / 2 + 8;
	}

	/** Move the game on one tick. */
	protected abstract void step();

	/** Paint it. */
	protected abstract void paint(GuiGraphicsExtractor graphics);

	/** A key went down: -1, 0 or 1 for each direction. */
	protected abstract void steer(int dx, int dy);

	/** Anything else the game wants off the keyboard. */
	protected void pressed(int key) {
	}

	/** Start again after a game over. */
	protected abstract void restart();

	/** The line along the top. */
	protected abstract Component status();

	@Override
	public void tick() {
		age++;
		if (!over && !paused) {
			step();
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			float partial) {
		super.extractRenderState(graphics, mouseX, mouseY, partial);
		// The cabinet: a black board with a thin surround.
		box(graphics, originX - 2, originY - 2, columns * cell + 4, rows * cell + 4, 0xFF303848);
		box(graphics, originX, originY, columns * cell, rows * cell, 0xFF000000);
		paint(graphics);

		graphics.text(font, status(), originX, originY - 14, 0xFFFFFF);
		Component help = Component.literal(over
				? "Space to play again  ·  Escape to leave"
				: "Arrow keys or WASD  ·  Escape to leave");
		graphics.text(font, help, originX, originY + rows * cell + 6, 0xFF9AA3B4);
	}

	/** One filled rectangle, in screen pixels. */
	protected void box(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int colour) {
		graphics.fill(x, y, x + w, y + h, colour);
	}

	/** One cell of the board, filled. */
	protected void square(GuiGraphicsExtractor graphics, int column, int row, int colour) {
		box(graphics, originX + column * cell, originY + row * cell, cell, cell, colour);
	}

	/** A smaller mark in the middle of a cell, for pellets and bullets. */
	protected void dot(GuiGraphicsExtractor graphics, int column, int row, int size, int colour) {
		int inset = Math.max(1, (cell - size) / 2);
		box(graphics, originX + column * cell + inset, originY + row * cell + inset,
				Math.max(1, cell - inset * 2), Math.max(1, cell - inset * 2), colour);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		switch (key) {
			case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A -> steer(-1, 0);
			case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D -> steer(1, 0);
			case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_W -> steer(0, -1);
			case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_S -> steer(0, 1);
			case GLFW.GLFW_KEY_SPACE -> {
				if (over) {
					over = false;
					age = 0;
					restart();
				} else {
					pressed(key);
				}
			}
			default -> {
				pressed(key);
				return super.keyPressed(event);
			}
		}
		return true;
	}

	/** Tell the server what happened. */
	protected static void tell(String game, String what, int amount) {
		ClientPlayNetworking.send(new ArcadePackets.Score(game + ":" + what + ":" + amount));
	}

	@Override
	public boolean isPauseScreen() {
		return false;                     // the world carries on while you play
	}
}
