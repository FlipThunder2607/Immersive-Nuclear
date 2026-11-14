package immersivenuclear.common.multiblock;

/**
 * Sulfuric Acid Unit, block by block: 5 wide x 9 long x 8 high, 174 blocks (the octagonal model, 14/11/2026). On a
 * scaffolding and steel sheetmetal floor, the contact process from the north end to the south end: the blower
 * (a steel block under a generator) and the instrument cabinet (heavy engineering, the redstone engineering at its
 * foot), the sulfur burner (blast bricks) beside the sulfur melter (iron and copper sheetmetal under a barrel, the
 * bucket elevator of steel fences behind it), the waste heat boiler (iron sheetmetal) under its steam drum (barrels),
 * the acid cooler (aluminium sheetmetal), the converter (iron sheetmetal round a chemical engineering core, iron slabs
 * on top) and the absorption tower (iron sheetmetal and barrels round the same core, the stack above) with the pump
 * tank (barrels) beside it. Fluid pipes run along the back and over the top.
 *
 * Offsets are relative to the trigger (redstone engineering block, at the cabinet's foot on the back, one block above
 * the floor; hit with the hammer), in the reference orientation (facing NORTH): the front is WEST, so the player stands
 * west of the structure looking east; right = south, back = east, left = north. dx grows east, dz grows south.
 * Ports, where the model draws them: sulfur (0,-1,2) in on the back, beside the melter; water (-1,-1,0) in on the
 * north end; sulfuric acid (-2,-1,8) out on the south end (the chemical engineering block), steam (0,-1,8) out beside
 * it; energy on the heavy engineering of the cabinet's top (0,2,1); redstone on the trigger.
 * No Minecraft classes here, so it can be tested on its own.
 */
public final class SulfuricAcidUnitStructure
{
	public static final int MIN_DX = -4, MAX_DX = 0;
	public static final int MIN_DY = -1, MAX_DY = 6;
	public static final int MIN_DZ = 0, MAX_DZ = 8;

	/** Master part (the waste heat boiler's front block, in the middle of the machine); holds the machine. */
	public static final int MASTER_DX = -3, MASTER_DY = 0, MASTER_DZ = 3;

	/**
	 * s steel scaffolding, S steel sheetmetal, I iron sheetmetal, a aluminium sheetmetal, c copper sheetmetal, i iron
	 * sheetmetal slab (lower), H heavy engineering, l light engineering, R redstone engineering (trigger), G generator,
	 * f steel fence, K blast brick, B metal barrel, p fluid pipe, Q chemical engineering (the acid-proof casing), T steel
	 * block, . air (not checked).
	 * LAYERS[dy - MIN_DY][dz - MIN_DZ].charAt(dx - MIN_DX); layer 0 is the bottom, row 0 the north end.
	 * Read from the dev world build, x200..204, y4..11, z457..465 (14/11/2026, the octagonal model's build).
	 */
	static final String[][] LAYERS = {
			{ // y 4
					"sSSls",
					"sSSSs",
					"sSSSl",
					"sSSSs",
					"sSSSs",
					"sSSSs",
					"sSSSs",
					"sSSSs",
					"sSQSl"},
			{ // y 5
					".T.pR",
					"KK..H",
					"KKpIf",
					"II.pp",
					"II.ap",
					"IIIap",
					"IQI.p",
					"IQIBp",
					"IIIBp"},
			{ // y 6
					".G.pH",
					"KK..H",
					"KKpcf",
					"II...",
					"II...",
					"III..",
					"IQI..",
					"BQBB.",
					"BBBB."},
			{ // y 7
					"...pH",
					"....H",
					".ppBf",
					".B...",
					".B...",
					"III..",
					"IQI..",
					"IQI..",
					"III.."},
			{ // y 8
					"...p.",
					"...p.",
					"...pf",
					"...p.",
					"...p.",
					"IIIp.",
					"IIIp.",
					"BQBp.",
					"BBBp."},
			{ // y 9
					".....",
					".....",
					".....",
					".....",
					".....",
					".....",
					"iii..",
					"IQI..",
					"IIIp."},
			{ // y 10
					".....",
					".....",
					".....",
					".....",
					".....",
					".....",
					".....",
					"III..",
					"IIIp."},
			{ // y 11
					".....",
					".....",
					".....",
					".....",
					".....",
					".....",
					".....",
					".p...",
					".p..."},
	};

	public static final char TRIGGER = 'R';

	/** What a part does besides being structure. */
	public static final int PORT_NONE = 0, PORT_SULFUR = 1, PORT_WATER = 2, PORT_ACID = 3, PORT_STEAM = 4, PORT_ENERGY = 5, PORT_REDSTONE = 6;

	/**
	 * Port parts and the face they work on, as a reference-orientation {dx, dz} step out of the part
	 * ({0, 0} = every side). Faces follow the model's port marks (blue in, orange out).
	 */
	static final int[][] PORTS = {
			// dx, dy, dz, port, face dx, face dz
			{0, -1, 2, PORT_SULFUR, 1, 0},
			{-1, -1, 0, PORT_WATER, 0, -1},
			{-2, -1, 8, PORT_ACID, 0, 1},
			{0, -1, 8, PORT_STEAM, 0, 1},
			{0, 2, 1, PORT_ENERGY, 0, 0},
			{0, 0, 0, PORT_REDSTONE, 0, 0},
	};

	private SulfuricAcidUnitStructure()
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
