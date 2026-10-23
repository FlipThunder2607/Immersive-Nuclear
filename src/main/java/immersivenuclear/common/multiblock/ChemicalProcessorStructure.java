package immersivenuclear.common.multiblock;

/**
 * Chemical Processing Unit, read block by block from the user's build in the dev world
 * (New World, x 193..195, y 4..9, z 413..415), 3 wide x 6 high x 3 deep, 45 blocks (the user removed the ladder
 * column at z 412, the heavy engineering column above y5, and the front-right fences at x 193 z 415 y 6..9 and
 * x 194 z 415 y 9).
 *
 * Offsets are relative to the trigger (redstone engineering block, layer 2, hit with the hammer on its front face),
 * in the reference orientation (facing NORTH): the front is WEST, so the player stands west of the structure
 * looking east; right = south, back = east, left = north. dx grows east, dz grows south.
 * Ports (as in the user's model.bbmodel): right water (1,-1,0), back uranium powder (2,-1,-2) and reagent (2,-1,-1),
 * left polpa output (1,-1,-2), energy on the heavy engineering column (2,-1..0,0). No Minecraft classes here, so it can be tested on its own.
 */
public final class ChemicalProcessorStructure
{
	public static final int MIN_DX = 0, MAX_DX = 2;
	public static final int MIN_DY = -1, MAX_DY = 4;
	public static final int MIN_DZ = -2, MAX_DZ = 0;

	/** Master part (a tank barrel in the middle of the structure); draws the whole model. */
	public static final int MASTER_DX = 1, MASTER_DY = 1, MASTER_DZ = -1;

	/**
	 * D covered steel ladder, l light engineering, s steel scaffolding, b steel scaffolding slab (lower half),
	 * H heavy engineering, B metal barrel, F steel fence, R redstone engineering (trigger), P fluid pipe,
	 * A radiator, G generator, . air (not checked).
	 * LAYERS[dy - MIN_DY][dz - MIN_DZ].charAt(dx - MIN_DX); layer 0 is the bottom, row 0 the back.
	 */
	static final String[][] LAYERS = {
			{ // y 4
					"Dll",
					"Dsl",
					"llH"},
			{ // y 5
					"BBF",
					"BBF",
					"RPH"},
			{ // y 6
					"BBF",
					"BBF",
					".P."},
			{ // y 7
					"BBF",
					"BBF",
					".P."},
			{ // y 8
					"BBF",
					"BBF",
					".P."},
			{ // y 9
					"lAF",
					"AGF",
					"..."},
	};

	public static final char TRIGGER = 'R';

	/** What a part does besides being structure. */
	public static final int PORT_NONE = 0, PORT_WATER = 1, PORT_URANIUM = 2, PORT_REAGENT = 3, PORT_POLPA = 4, PORT_ENERGY = 5, PORT_REDSTONE = 6;

	/**
	 * Port parts and the face they work on, as a reference-orientation {dx, dz} step out of the part
	 * ({0, 0} = every side). Faces follow the user's model: water on the right (south), the two item inputs on the
	 * back (east), polpa on the left (north); energy takes any side of both heavy engineering blocks; the redstone
	 * port is the trigger block.
	 */
	static final int[][] PORTS = {
			// dx, dy, dz, port, face dx, face dz
			{1, -1, 0, PORT_WATER, 0, 1},
			{2, -1, -2, PORT_URANIUM, 1, 0},
			{2, -1, -1, PORT_REAGENT, 1, 0},
			{1, -1, -2, PORT_POLPA, 0, -1},
			{2, -1, 0, PORT_ENERGY, 0, 0},
			{2, 0, 0, PORT_ENERGY, 0, 0},
			{0, 0, 0, PORT_REDSTONE, 0, 0},
	};

	private ChemicalProcessorStructure()
	{
	}

	public static int portAt(int dx, int dy, int dz)
	{
		int[] p = portRow(dx, dy, dz);
		return p == null ? PORT_NONE : p[3];
	}

	/** Reference-orientation {dx, dz} of the face a port works on; {0, 0} for every side, null if not a port. */
	public static int[] portFace(int dx, int dy, int dz)
	{
		int[] p = portRow(dx, dy, dz);
		return p == null ? null : new int[]{p[4], p[5]};
	}

	private static int[] portRow(int dx, int dy, int dz)
	{
		for(int[] p : PORTS)
			if(p[0] == dx && p[1] == dy && p[2] == dz)
				return p;
		return null;
	}

	public static char get(int dx, int dy, int dz)
	{
		if(dx < MIN_DX || dx > MAX_DX || dy < MIN_DY || dy > MAX_DY || dz < MIN_DZ || dz > MAX_DZ)
			return '.';
		return LAYERS[dy - MIN_DY][dz - MIN_DZ].charAt(dx - MIN_DX);
	}

	public static boolean isPart(int dx, int dy, int dz)
	{
		return get(dx, dy, dz) != '.';
	}

	public static int count(char c)
	{
		int n = 0;
		for(String[] layer : LAYERS)
			for(String row : layer)
				for(int i = 0; i < row.length(); i++)
					if(row.charAt(i) == c)
						n++;
		return n;
	}

	public static int totalParts()
	{
		int n = 0;
		for(String[] layer : LAYERS)
			for(String row : layer)
				for(int i = 0; i < row.length(); i++)
					if(row.charAt(i) != '.')
						n++;
		return n;
	}

	/**
	 * Horizontal world offset {x, z} of a template offset. {@code turns} = clockwise quarter turns from
	 * the reference orientation (NORTH 0, EAST 1, SOUTH 2, WEST 3); {@code mirror} first swaps left and right
	 * (flips dz, which runs from the left to the right of the front), so the front stays the front.
	 */
	public static int[] rotate(int dx, int dz, int turns, boolean mirror)
	{
		int x = dx;
		int z = mirror ? -dz : dz;
		for(int i = 0; i < (turns & 3); i++)
		{
			int t = x;
			x = -z;
			z = t;
		}
		return new int[]{x, z};
	}

	/** Inverse of {@link #rotate}: template {dx, dz} of a world offset. */
	public static int[] unrotate(int x, int z, int turns, boolean mirror)
	{
		for(int i = 0; i < (turns & 3); i++)
		{
			int t = z;
			z = -x;
			x = t;
		}
		return new int[]{x, mirror ? -z : z};
	}
}
