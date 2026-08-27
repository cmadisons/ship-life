package com.example.client;

import com.example.HudPacket;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3fc;

/**
 * The heads-up display: the star out in the world, and the clock in the corner.
 *
 * The star is drawn where the quest actually is. Its place on the screen is
 * worked out by hand rather than by the game's own machinery -- take the line
 * from the camera to the quest, measure it against the way the camera is
 * facing, and divide -- which is why it shows through walls and through the
 * floors of the ship without anything having to be transparent.
 *
 * A quest behind you, or off the side, is pinned to the edge of the screen
 * pointing the way, so it never simply vanishes: the promise was that you can
 * always see where you are going.
 *
 * Everything shown here arrives from the server four times a second, in
 * {@link HudPacket}. Nothing is worked out twice.
 */
public final class Screen {
	private Screen() {
	}

	/** The last thing the server told us. Null until the first packet. */
	private static String[] state;

	public static void take(String line) {
		state = line.split("\\|", -1);
	}

	public static void forget() {
		state = null;
	}

	public static void draw(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft client = Minecraft.getInstance();
		if (state == null || state.length < 13 || client.player == null
				|| client.options.hideGui) {
			return;
		}
		Font font = client.font;
		int width = graphics.guiWidth();

		// --- the corner: the ship's clock, the date and what you have --------
		String clock = state[7];
		String date = state[8];
		String money = state[9];
		String tickets = state[10] + " arcade  ·  " + state[11] + " event";
		String event = state[12];

		int y = 4;
		y = corner(graphics, font, width, y, clock + "  " + date, 0xFFFFFF);
		y = corner(graphics, font, width, y, money, 0xFFD700);
		y = corner(graphics, font, width, y, tickets, 0xA0A8B8);
		if (!event.isEmpty()) {
			corner(graphics, font, width, y, event, 0x55FF55);
		}

		// --- the star, out in the world --------------------------------------
		String questName = state[0];
		if (questName.isEmpty()) {
			return;
		}
		int blocks = Integer.parseInt(state[2]);
		int colour = Integer.parseInt(state[3]);
		Vec3 target = new Vec3(Integer.parseInt(state[4]) + 0.5,
				Integer.parseInt(state[5]) + 1.0, Integer.parseInt(state[6]) + 0.5);
		star(graphics, font, client, target, blocks, colour, state[1]);
	}

	/** One line of the corner block, right-aligned. */
	private static int corner(GuiGraphicsExtractor graphics, Font font, int width,
			int y, String text, int colour) {
		graphics.text(font, Component.literal(text),
				width - font.width(text) - 4, y, colour);
		return y + 10;
	}

	/**
	 * Put the star where the quest is.
	 *
	 * The camera gives three vectors at right angles -- where it looks, what
	 * is up, what is left -- so the line to the quest measured against those
	 * three is the quest in the camera's own terms. Divide the sideways and
	 * upward parts by the forward part and you have the point on the screen;
	 * a forward part at or behind zero means the quest is behind you, and then
	 * the only honest thing to draw is an arrow at the edge.
	 */
	private static void star(GuiGraphicsExtractor graphics, Font font, Minecraft client,
			Vec3 target, int blocks, int colour, String todo) {
		var camera = client.gameRenderer.getMainCamera();
		Vec3 line = target.subtract(camera.position());

		Vector3fc forwards = camera.forwardVector();
		Vector3fc up = camera.upVector();
		Vector3fc left = camera.leftVector();

		double ahead = line.x * forwards.x() + line.y * forwards.y() + line.z * forwards.z();
		double sideways = line.x * left.x() + line.y * left.y() + line.z * left.z();
		double above = line.x * up.x() + line.y * up.y() + line.z * up.z();

		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		double half = Math.tan(Math.toRadians(client.options.fov().get()) / 2.0);

		int x;
		int y;
		boolean edge = false;
		if (ahead > 0.1) {
			x = (int) (width / 2.0 - (sideways / ahead) / half * (height / 2.0));
			y = (int) (height / 2.0 - (above / ahead) / half * (height / 2.0));
			edge = x < 12 || x > width - 12 || y < 12 || y > height - 24;
		} else {
			// Behind you: park it on the side it is on.
			x = sideways > 0 ? 12 : width - 12;
			y = height / 2;
			edge = true;
		}
		x = Math.max(12, Math.min(width - 12, x));
		y = Math.max(12, Math.min(height - 24, y));

		String mark = edge ? "◆" : "★";
		graphics.text(font, Component.literal(mark), x - font.width(mark) / 2, y, colour);
		String far = blocks + " blocks";
		graphics.text(font, Component.literal(far), x - font.width(far) / 2, y + 10, colour);
		if (!edge && !todo.isEmpty()) {
			graphics.text(font, Component.literal(todo),
					x - font.width(todo) / 2, y + 20, 0xFFFFFF);
		}
	}
}
