package com.example;

/**
 * What is on each floor, and what opens it.
 *
 * This is the answer to "how do I get to floor 9", and it lives in one place
 * so the Quest Book can show it rather than you having to be told. A floor
 * whose way in is not written here is a floor nobody can find.
 */
public final class Floors {
	private Floors() {
	}

	public static String name(int floor) {
		return switch (floor) {
			case 1 -> "Lobby";
			case 2 -> "Arcade";
			case 3 -> "Swimming Pool";
			case 4 -> "Buffet";
			case 5 -> "Your Room";
			case 6 -> "Race Track";
			case 7 -> "Events";
			case 8 -> "Store";
			case 9 -> "Fight Room";
			case 10 -> "Boss Room";
			case 11 -> "Rewards";
			case 12 -> "Pet Store";
			case 13 -> "The Keg";
			case 14 -> "Passports";
			case 15 -> "Ben's Room";
			default -> "Floor 16";
		};
	}

	public static String how(int floor) {
		return switch (floor) {
			case 1, 5 -> "your passport";
			case 2, 3, 4 -> "Charlie's quest";
			case 6 -> "buy a cat, 10 arcade tickets";
			case 7 -> "own one of every pet";
			case 8 -> "earn 50 arcade tickets";
			case 9 -> "swim a lap in 15 seconds";
			case 10 -> "do an event -- Quest Day counts once you finish one of its four";
			case 11, 12, 13 -> "1000 event tickets, all three";
			case 14 -> "the 4.9% reward on floor 11";
			case 15 -> "upgrade your passport on floor 14";
			default -> "clear wave 2 and a boss, or clear wave 3";
		};
	}
}
