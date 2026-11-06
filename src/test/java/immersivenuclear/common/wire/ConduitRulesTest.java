package immersivenuclear.common.wire;

import static immersivenuclear.common.wire.ConduitRules.*;

/** Run by the conduitRulesTest Gradle task (part of check). */
public class ConduitRulesTest
{
	public static void main(String[] args)
	{
		// 1 power wire and 4 data cables, or 8 data cables
		check(fits(0, 0, POWER), "power in an empty conduit");
		check(!fits(1, 0, POWER), "a second power wire");
		check(fits(0, 4, POWER), "power beside 4 data cables");
		check(!fits(0, 5, POWER), "power beside 5 data cables");
		check(fits(1, 3, DATA), "a 4th data cable beside power");
		check(!fits(1, 4, DATA), "a 5th data cable beside power");
		check(fits(0, 7, DATA), "an 8th data cable");
		check(!fits(0, 8, DATA), "a 9th data cable");
		// 1 item per 3 blocks, rounded up
		expect(cost(1), 1, "cost of 1 block");
		expect(cost(3), 1, "cost of 3 blocks");
		expect(cost(4), 2, "cost of 4 blocks");
		expect(cost(9), 3, "cost of 9 blocks");
		// Power in its lanes, data from the far side
		lanes(new int[]{0}, POWER, 0);
		lanes(new int[]{0b11110000, 0}, POWER, 0, 0);
		lanes(new int[]{0, 1}, POWER, (int[])null);
		lanes(new int[]{0b1000}, POWER, (int[])null);
		lanes(new int[]{0, 0, 0}, DATA, 7, 7, 7);
		lanes(new int[]{0b10000000}, DATA, 6);
		lanes(new int[]{0b11110001}, DATA, 3);
		lanes(new int[]{0b11111110}, DATA, 0);
		lanes(new int[]{0b11111111}, DATA, (int[])null);
		// 4 data cables, then a power wire and 2 data cables: room in each, but no lane free in both (dev world, 06/11/2026)
		lanes(new int[]{0b11110000, 0b11110000, 0b11001111, 0b11001111}, DATA, 3, 3, 5, 5);
		// It takes the lane that lasts
		lanes(new int[]{0b00000000, 0b10000000, 0b10000000}, DATA, 6, 6, 6);
		System.out.println("Conduit rules OK");
	}

	static void lanes(int[] used, int kind, int... expected)
	{
		if(!java.util.Arrays.equals(assignLanes(used, kind), expected))
			throw new AssertionError(java.util.Arrays.toString(used) + ": expected " + java.util.Arrays.toString(expected) + ", got " + java.util.Arrays.toString(assignLanes(used, kind)));
	}

	static void check(boolean ok, String what)
	{
		if(!ok)
			throw new AssertionError(what);
	}

	static void expect(int actual, int expected, String what)
	{
		if(actual != expected)
			throw new AssertionError(what + ": expected " + expected + ", got " + actual);
	}
}
