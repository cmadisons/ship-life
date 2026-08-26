package com.example;

/**
 * The ship's calendar.
 *
 * A day is twenty real minutes and it runs on the real clock, not on how long
 * you have been playing -- close the game on a Saturday afternoon and the ship
 * carries on into Sunday without you. Thirty days make a month, which is ten
 * real hours, and twelve months make a year.
 *
 * Everything here is worked out from the wall clock, so nothing has to be
 * saved and two players in the same world always agree on the date.
 */
public final class Cal {
	private Cal() {
	}

	/** Twenty real minutes, in milliseconds. */
	public static final long DAY_MS = 20L * 60L * 1000L;

	public static final int DAYS_IN_MONTH = 30;
	public static final int MONTHS_IN_YEAR = 12;

	private static final String[] MONTHS = {
			"January", "February", "March", "April", "May", "June",
			"July", "August", "September", "October", "November", "December"
	};

	private static final String[] WEEKDAYS = {
			"Sunday", "Monday", "Tuesday", "Wednesday",
			"Thursday", "Friday", "Saturday"
	};

	/** Days since the epoch. Day 0 is a Sunday, which the events lean on. */
	public static long dayNumber() {
		return System.currentTimeMillis() / DAY_MS;
	}

	/** The day of the month, 1 to 30. */
	public static int dayOfMonth() {
		return (int) (dayNumber() % DAYS_IN_MONTH) + 1;
	}

	/** 0 for January, 11 for December. */
	public static int monthNumber() {
		return (int) ((dayNumber() / DAYS_IN_MONTH) % MONTHS_IN_YEAR);
	}

	public static String month() {
		return MONTHS[monthNumber()];
	}

	public static String weekday() {
		return WEEKDAYS[(int) (dayNumber() % 7)];
	}

	/** How far through today we are, 0.0 at midnight and 1.0 at the next. */
	public static double throughDay() {
		return (System.currentTimeMillis() % DAY_MS) / (double) DAY_MS;
	}

	/** The time of day as a clock reads it. */
	public static String clock() {
		int minutes = (int) (throughDay() * 24 * 60);
		return String.format("%02d:%02d", minutes / 60, minutes % 60);
	}

	/** "Sunday 4 May, 13:20" -- the whole date in one line. */
	public static String date() {
		return weekday() + " " + dayOfMonth() + " " + month() + ", " + clock();
	}

	// ----------------------------------------------------------------- events

	/** Which event is on today, or null on an ordinary day. */
	public static String eventToday() {
		boolean sunday = weekday().equals("Sunday");
		boolean weekend = sunday || weekday().equals("Saturday");
		int month = monthNumber();
		if (month == 4 && dayOfMonth() == 4) {
			return "May the Fourth";
		}
		if (sunday && month == 9) {
			return "Spooky Shooter";
		}
		if (sunday && month == 11) {
			return "Christmas";
		}
		// Summer is June through August; March break is the third week of March.
		boolean summer = month >= 5 && month <= 7;
		boolean marchBreak = month == 2 && dayOfMonth() >= 15 && dayOfMonth() <= 21;
		if (weekend && (summer || marchBreak)) {
			return "Summer Break";
		}
		// Quest Day is every other Monday, counting from the epoch.
		if (weekday().equals("Monday") && (dayNumber() / 7) % 2 == 0) {
			return "Quest Day";
		}
		return null;
	}
}
