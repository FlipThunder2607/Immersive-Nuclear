package immersivenuclear.common.wire;

/**
 * What a conduit can carry. Each block of a conduit takes 1 power wire and 4 data cables, or 8 data cables: the tray has 8 lanes and the power wire is as wide as 4 data cables, so either way it is full.
 * Looking at a port with its back to the east, as the computers in the dev world stand, the power wire is on the left (lanes 0 to 3) and the data cables fill in from the right (lane 7 inwards).
 * No Minecraft classes here, so it can be tested on its own.
 */
public final class ConduitRules
{
	public static final int POWER = 0, DATA = 1;
	public static final int LANES = 8;
	/** The lanes the power wire takes. */
	public static final int POWER_LANES = 0b1111;
	/** The lanes the data cables keep to beside a power wire. */
	public static final int DATA_LANES = 0b11110000;
	/** Blocks of conduit one item of wire covers. */
	public static final int BLOCKS_PER_ITEM = 3;

	private ConduitRules()
	{
	}

	/** Whether a conduit holding that many wires takes one more of the given kind. */
	public static boolean fits(int power, int data, int kind)
	{
		if(kind == POWER)
			return power == 0 && data <= 4;
		return data < (power > 0 ? 4 : 8);
	}

	/** The items a run of that many conduit blocks takes. */
	public static int cost(int length)
	{
		return Math.max(1, (length + BLOCKS_PER_ITEM - 1) / BLOCKS_PER_ITEM);
	}

	/**
	 * The lane of a new wire at each joint along its way, given the lanes taken there (one bit each), or null if it does not fit. The power wire has its own lanes. A data cable stays in a lane as long as it can
	 * and moves over where it must, keeping out of the power wire's lanes while there is another: otherwise a conduit with room could be left with no lane free from end to end.
	 */
	public static int[] assignLanes(int[] used, int kind)
	{
		int[] lanes = new int[used.length];
		for(int i = 0; i < used.length; i++)
		{
			if(kind == POWER)
			{
				if((used[i] & POWER_LANES) != 0)
					return null;
				lanes[i] = 0;
			}
			else if(i > 0 && (used[i] & 1 << lanes[i - 1]) == 0)
				lanes[i] = lanes[i - 1];
			else
			{
				lanes[i] = longestRun(used, i);
				if(lanes[i] < 0)
					return null;
			}
		}
		return lanes;
	}

	/** The free lane at that joint which stays free the furthest, the rightmost of those. -1 if none is free. */
	private static int longestRun(int[] used, int from)
	{
		boolean outside = (used[from] & DATA_LANES) != DATA_LANES;
		int best = -1, bestRun = 0;
		for(int lane = LANES - 1; lane >= (outside ? 4 : 0); lane--)
		{
			int run = 0;
			while(from + run < used.length && (used[from + run] & 1 << lane) == 0)
				run++;
			if(run > bestRun)
			{
				best = lane;
				bestRun = run;
			}
		}
		return best;
	}
}
