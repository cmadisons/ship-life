package com.example;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

/**
 * Do this in a moment.
 *
 * The elevator and the phone both want to do something a second or two from
 * now -- open the doors, ring -- and the server has no timer of its own, so
 * this is one: a small list of jobs with a tick to run them on.
 */
public final class Ticker {
	private Ticker() {
	}

	private record Job(long when, Runnable what) {
	}

	private static final List<Job> JOBS = new ArrayList<>();
	private static MinecraftServer server;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(running -> {
			server = running;
			if (JOBS.isEmpty()) {
				return;
			}
			long now = running.getTickCount();
			List<Job> due = new ArrayList<>();
			JOBS.removeIf(job -> {
				if (job.when() <= now) {
					due.add(job);
					return true;
				}
				return false;
			});
			for (Job job : due) {
				job.what().run();
			}
		});
	}

	/** Run this after so many ticks. */
	public static void after(int ticks, Runnable what) {
		if (server == null) {
			what.run();
			return;
		}
		JOBS.add(new Job(server.getTickCount() + ticks, what));
	}
}
