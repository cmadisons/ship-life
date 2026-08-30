package com.example;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Turning up.
 *
 * A day on the ship is twenty real minutes, so a reward for every day would
 * be a reward every twenty minutes. This pays on the first day you play in
 * each real-world stretch instead: come back after a break and there is
 * something waiting, come back after lunch and there is not.
 */
public final class Daily {
	private Daily() {
	}

	/** What turning up is worth. */
	public static final int PAYS = 40;

	/** Days that have to pass before it pays again: six hours of ship days. */
	private static final int APART = 18;

	/** Check on join, and pay if it is time. */
	public static void check(ServerPlayer player) {
		int last = State.tally(player, State.LAST_DAY);
		int today = (int) Cal.dayNumber();
		if (last == 0) {
			player.setAttached(State.LAST_DAY, today);
			return;                        // your first day is not a return
		}
		if (today - last < APART) {
			return;
		}
		player.setAttached(State.LAST_DAY, today);
		State.event(player, PAYS);
		player.sendSystemMessage(Component.literal("Welcome back. " + PAYS
				+ " event tickets for turning up.").withStyle(ChatFormatting.GREEN));
	}
}
