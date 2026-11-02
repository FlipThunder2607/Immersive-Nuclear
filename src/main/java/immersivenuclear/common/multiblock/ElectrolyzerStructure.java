package immersivenuclear.common.multiblock;

/**
 * Electrolyzer, block by block: 5 wide x 7 long x 5 high. On a scaffolding floor with a steel sheetmetal deck, the two
 * cell stacks (HV capacitors) run along the middle, the rectifier (generators, heavy engineering) stands at the south
 * end with two HV relays on its roof, the gas separators and vessels (metal barrels, radiators for the cooler) stand
 * along the east side and the north end, the electrolyte tank (steel sheetmetal, slabs) over the stacks, the vent stack
 * (a fluid pipe) on top; a steel fence along the front.
 *
 * Offsets are relative to the trigger (redstone engineering block, second layer, hit with the hammer on its front
 * face), in the reference orientation (facing NORTH): the front is WEST, so the player stands west of the structure
 * looking east; right = south, back = east, left = north. dx grows east, dz grows south.
 * Ports, where the model draws them: water (2,-1,0) in on the north face; oxygen (4,-1,0) and hydrogen (4,-1,6) out
 * on the back (east) face, at its north and south corners (the user, 01/11); energy on the heavy engineering of the south-west corner (0,-1..0,5..6),
 * the rectifier's inlets (the user, 01/11). No Minecraft classes here, so it can be tested on its own.
 */
public final class ElectrolyzerStructure
{
	public static final int MIN_DX = 0, MAX_DX = 4;
	public static final int MIN_DY = -1, MAX_DY = 3;
	public static final int MIN_DZ = 0, MAX_DZ = 6;

	/** Master part (a cell stack, in the middle of the machine); holds the machine. */
	public static final int MASTER_DX = 2, MASTER_DY = 0, MASTER_DZ = 3;

	/**
	 * s steel scaffolding, S steel sheetmetal, b steel sheetmetal slab (lower), H heavy engineering, l light
	 * engineering, R redstone engineering (trigger), G generator (the rectifier), r radiator, c HV capacitor (the cell
	 * stacks), B metal barrel, p fluid pipe, f steel fence, v HV relay, . air (not checked).
	 * LAYERS[dy - MIN_DY][dz - MIN_DZ].charAt(dx - MIN_DX); layer 0 is the bottom, row 0 the north end.
	 * Read from the dev world build, x200..204, y4..8, z476..482 (01/11/2026).
	 */
	static final String[][] LAYERS = {
			{ // y 4
					"sSlSl",
					"sSSSs",
					"sSSSs",
					"sSSSs",
					"sSSSs",
					"HSSSs",
					"HSSSl"},
			{ // y 5
					"RBrBB",
					"fcccB",
					"fcccB",
					"fcccp",
					"fcccB",
					"HGGSB",
					"HGGBB"},
			{ // y 6
					".BrBB",
					"....B",
					".pS.B",
					"..S..",
					".pS.B",
					".GGSB",
					".GGBB"},
			{ // y 7
					".bbbb",
					"....b",
					"..b.B",
					"..b..",
					"..b.B",
					"....b",
					".vvbb"},
			{ // y 8
					".....",
					".....",
					"....p",
					".....",
					".....",
					".....",
					"....."},
	};

	public static final char TRIGGER = 'R';

	/** What a part does besides being structure. */
	public static final int PORT_NONE = 0, PORT_WATER = 1, PORT_OXYGEN = 2, PORT_HYDROGEN = 3, PORT_ENERGY = 4, PORT_REDSTONE = 5;

	/**
	 * Port parts and the face they work on, as a reference-orientation {dx, dz} step out of the part
	 * ({0, 0} = every side). Faces follow the model's port marks (blue in, orange out).
	 */
	static final int[][] PORTS = {
			// dx, dy, dz, port, face dx, face dz
			{2, -1, 0, PORT_WATER, 0, -1},
			{4, -1, 0, PORT_OXYGEN, 1, 0},
			{4, -1, 6, PORT_HYDROGEN, 1, 0},
			{0, -1, 5, PORT_ENERGY, 0, 0},
			{0, 0, 5, PORT_ENERGY, 0, 0},
			{0, -1, 6, PORT_ENERGY, 0, 0},
			{0, 0, 6, PORT_ENERGY, 0, 0},
			{0, 0, 0, PORT_REDSTONE, 0, 0},
	};

	private ElectrolyzerStructure()
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
	 * the reference orientation (NORTH 0, EAST 1, SOUTH 2, WEST 3); {@code mirror} flips dx first.
	 */
	public static int[] rotate(int dx, int dz, int turns, boolean mirror)
	{
		return ChemicalProcessorStructure.rotate(dx, dz, turns, mirror);
	}

	/** Inverse of {@link #rotate}: template {dx, dz} of a world offset. */
	public static int[] unrotate(int x, int z, int turns, boolean mirror)
	{
		return ChemicalProcessorStructure.unrotate(x, z, turns, mirror);
	}
}
