package immersivenuclear.common.multiblock;

/**
 * Conversion Unit, block by block: 7 wide x 11 long x 12 high (436 blocks), the first machine of line 2's chain that turns
 * yellowcake into uranium hexafluoride. A one-block deck; along the front, a reactor tower (radioactive handling core
 * in steel, copper, lead and iron sheetmetal, north end) and barrels at the south end; along the back,
 * barrels (hydrogen, north end), chemical engineering under copper slabs, two columns of scaffolding, steel blocks and
 * chemical engineering up to the top layer, and radiators. Fluid pipes join the parts.
 *
 * Offsets are relative to the trigger (the redstone engineering block on the deck's east edge, 9th row from the north),
 * in the reference orientation (facing NORTH): the front is WEST, so the player stands west of the structure looking
 * east; right = south, back = east, left = north. dx grows east, dz grows south (the structure is west of the trigger,
 * so dx is -6..0).
 * Ports, where the user's model draws them: HF (-5,0,-8) and hydrogen (-1,0,-8) in, on the north (feed) face; UF6 (-5,0,2)
 * and the fluorinated effluent (-1,0,2) out, on the south face (all four on chemical engineering); yellowcake in on the hatch on
 * top of the reactor tower (-5,10,-7); the fluorination ash out on the radioactive handling block of the east wall, at the back (0,0,-7); energy on the heavy
 * engineering of the east wall (0,0,-4..-2); redstone on the trigger.
 * Read from the user's build in the dev world (x200..206, y4..15, z485..495, saved 08/11/2026 11:31). No Minecraft classes.
 */
public final class ConversionUnitStructure
{
	public static final int MIN_DX = -6, MAX_DX = 0;
	public static final int MIN_DY = 0, MAX_DY = 11;
	public static final int MIN_DZ = -8, MAX_DZ = 2;

	/** Master part (radioactive handling core of the reactor tower, first level above the deck); holds the machine. */
	public static final int MASTER_DX = -5, MASTER_DY = 2, MASTER_DZ = -7;

	/**
	 * L metal ladder (the column at x204, z491), s steel scaffolding, S steel sheetmetal, P lead sheetmetal, C copper sheetmetal, I iron sheetmetal,
	 * b steel sheetmetal slab, c copper sheetmetal slab, H heavy engineering, R redstone engineering (trigger),
	 * r radiator (cold traps), B metal barrel, p fluid pipe, f steel fence, T steel block, Q chemical engineering,
	 * X radioactive handling, . air (not checked).
	 * LAYERS[dy - MIN_DY][dz - MIN_DZ].charAt(dx - MIN_DX); layer 0 is the bottom, row 0 the north end.
	 */
	static final String[][] LAYERS = {
			{ // y 4
					"sQSSSQs",
					"sSSSSSX",
					"sSSSSSs",
					"sSSSSSs",
					"sSSSSSH",
					"sSSSSSH",
					"sSSSSSH",
					"sSSSSSs",
					"sSSSSSR",
					"sSSSSSs",
					"sQSSSQs"},
			{ // y 5
					"fff..BB",
					"fXfppBB",
					"fff..QQ",
					"PPP..QQ",
					"fff..QQ",
					"fXf..sT",
					"fff.LsQ",
					".....rr",
					"BBB..rr",
					"BBB..QS",
					"BBB..TS"},
			{ // y 6
					"TTT..BB",
					"TXTp.BB",
					"TTT..QQ",
					"PPP..QQ",
					"TTT..QQ",
					"TXT..sT",
					"TTT.LsQ",
					".....rr",
					"BBB..rr",
					"BBB..QS",
					"BBB..TS"},
			{ // y 7
					"CSC....",
					"SXSp...",
					"CSC..QQ",
					"PPP..QQ",
					"SSS..QQ",
					"SXS..sT",
					"SSS.LsQ",
					".....rr",
					".p...rr",
					".....QS",
					".....TS"},
			{ // y 8
					"CSC....",
					"SXSQ...",
					"CSC..cc",
					"PPP..cc",
					"III..cc",
					"IXI..sT",
					"III.LsQ",
					".....CC",
					".ppppCC",
					".....QS",
					".....TS"},
			{ // y 9
					"CSC....",
					"SXSQ...",
					"CSC....",
					"bbb....",
					"SSS....",
					"SXS..sT",
					"SSS.LsQ",
					".......",
					".......",
					".....QS",
					".....TS"},
			{ // y 10
					"CSC....",
					"SXSQ...",
					"CSC....",
					".......",
					"SSS....",
					"SXS..sT",
					"SSS.LsQ",
					".......",
					".......",
					".....QS",
					".....TS"},
			{ // y 11
					"TTT....",
					"TXTp...",
					"TTTp...",
					"...p...",
					"TTTp...",
					"TTTp.sT",
					"TTTpLsQ",
					"...p...",
					"...p...",
					"...ppQS",
					".....TS"},
			{ // y 12
					"SSS....",
					"SXSp...",
					"SSS....",
					".......",
					"bpb....",
					"bSb..sT",
					"bbb.LsQ",
					".......",
					".......",
					".....QS",
					".....TS"},
			{ // y 13
					"TTT....",
					"TXT....",
					"TTT....",
					".......",
					".pppppp",
					".....sT",
					"....LsQ",
					".......",
					".......",
					".....QS",
					".....TS"},
			{ // y 14
					"fff....",
					"fXf....",
					"f.f....",
					".......",
					".......",
					".....sT",
					"....LsQ",
					".......",
					".......",
					".....QS",
					".....TS"},
			{ // y 15
					".......",
					".......",
					".......",
					".......",
					".......",
					".....sT",
					"....LsQ",
					".......",
					".......",
					".....QS",
					".....TS"},
	};

	public static final char TRIGGER = 'R';

	/** What a part does besides being structure. */
	public static final int PORT_NONE = 0, PORT_HYDROGEN = 1, PORT_HF = 2, PORT_UF6 = 3, PORT_EFFLUENT = 4, PORT_ITEM = 5, PORT_ENERGY = 6, PORT_REDSTONE = 7, PORT_ASH = 8;

	/**
	 * Port parts and the face they work on, as a reference-orientation {dx, dz} step out of the part
	 * ({0, 0} = every side). Faces follow the model's port marks (blue in, orange out).
	 */
	static final int[][] PORTS = {
			// dx, dy, dz, port, face dx, face dz
			{-1, 0, -8, PORT_HYDROGEN, 0, -1},
			{-5, 0, -8, PORT_HF, 0, -1},
			{-5, 0, 2, PORT_UF6, 0, 1},
			{-1, 0, 2, PORT_EFFLUENT, 0, 1},
			{-5, 10, -7, PORT_ITEM, 0, 0},
			{0, 0, -7, PORT_ASH, 1, 0},
			{0, 0, -4, PORT_ENERGY, 0, 0},
			{0, 0, -3, PORT_ENERGY, 0, 0},
			{0, 0, -2, PORT_ENERGY, 0, 0},
			{0, 0, 0, PORT_REDSTONE, 0, 0},
	};

	private ConversionUnitStructure()
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

	/** Empty in the structure, but the model draws there: an invisible part gives it the model's collision. */
	public static boolean isCollisionOnly(int dx, int dy, int dz)
	{
		for(int[] c : COLLISION_ONLY)
			if(c[0] == dx && c[1] == dy && c[2] == dz)
				return true;
		return false;
	}

	// COLLISION-ONLY START (ferramentas/conversao_colisao.py)
	static final int[][] COLLISION_ONLY = {
		{-2, 1, -8}, {-2, 1, -6}, {-3, 1, -5}, {-2, 1, -5}, {-2, 1, -4}, {-2, 1, -3}, {-6, 1, -1}, {-5, 1, -1},
		{-4, 1, -1}, {-2, 1, -1}, {-2, 1, 0}, {-2, 1, 1}, {-2, 2, -8}, {-2, 2, -7}, {-2, 2, -6}, {-3, 2, -5},
		{-2, 2, -5}, {-2, 2, -4}, {-2, 2, -3}, {-6, 2, -1}, {-5, 2, -1}, {-4, 2, -1}, {-2, 2, -1}, {-2, 2, 0},
		{-2, 2, 1}, {-1, 3, -8}, {0, 3, -8}, {-1, 3, -7}, {0, 3, -7}, {-2, 3, -6}, {-3, 3, -5}, {-2, 3, -5},
		{-2, 3, -4}, {-2, 3, -3}, {-6, 3, -1}, {-5, 3, -1}, {-4, 3, -1}, {-2, 3, -1}, {-6, 3, 0}, {-4, 3, 0},
		{-2, 3, 0}, {-6, 3, 1}, {-5, 3, 1}, {-4, 3, 1}, {-2, 3, 1}, {-6, 3, 2}, {-5, 3, 2}, {-4, 3, 2},
		{-3, 4, -6}, {-3, 4, -5}, {-3, 4, -4}, {-2, 4, -4}, {-3, 4, -3}, {-2, 4, -3}, {-2, 4, -1}, {-2, 4, 1},
		{-2, 5, -3}, {-2, 5, -1}, {-1, 5, -1}, {0, 5, -1}, {-2, 6, -3}, {-2, 6, -1}, {-1, 6, -1}, {0, 6, -1},
		{-4, 7, -5}, {-2, 7, -3}, {-4, 7, -1}, {-2, 7, -1}, {-1, 7, -1}, {0, 7, -1}, {-4, 7, 0}, {-4, 7, 1},
		{-4, 7, 2}, {-3, 7, 2}, {-2, 7, 2}, {-4, 8, -5}, {-3, 8, -5}, {-3, 8, -4}, {-2, 8, -4}, {-1, 8, -4},
		{-2, 8, -3}, {-2, 8, -1}, {0, 8, -1}, {-2, 9, -3}, {-2, 9, -1}, {-1, 9, -1}, {0, 9, -1}, {-5, 10, -6},
		{-2, 10, -3}, {-2, 10, -1}, {0, 10, -1}, {-2, 11, -3}, {-2, 11, -1}, {-1, 11, -1}, {0, 11, -1},
	};
	// COLLISION-ONLY END

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
