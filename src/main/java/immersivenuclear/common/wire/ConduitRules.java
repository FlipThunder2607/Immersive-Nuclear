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

	/** The lane for a new wire, given the lanes taken along its way (one bit each), or -1 if there is none: a wire keeps its lane from end to end. */
	public static int pickLane(int used, int kind)
	{
		if(kind == POWER)
			return (used & POWER_LANES) == 0 ? 0 : -1;
		for(int lane = LANES - 1; lane >= 0; lane--)
			if((used & 1 << lane) == 0)
				return lane;
		return -1;
	}
}
