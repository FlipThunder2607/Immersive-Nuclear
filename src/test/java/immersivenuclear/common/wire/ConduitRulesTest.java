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
		// Power on the left, data from the right
		expect(pickLane(0, POWER), 0, "power in its lanes");
		expect(pickLane(0b11110000, POWER), 0, "power beside 4 data cables");
		expect(pickLane(1, POWER), -1, "the power lanes taken");
		expect(pickLane(0b1000, POWER), -1, "a data cable in the power lanes");
		expect(pickLane(0, DATA), 7, "data on the far right");
		expect(pickLane(0b10000000, DATA), 6, "the next data cable");
		expect(pickLane(0b11110001, DATA), 3, "data beside the power wire");
		expect(pickLane(0b11111110, DATA), 0, "the last lane");
		expect(pickLane(0b11111111, DATA), -1, "no lane left");
		System.out.println("Conduit rules OK");
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
