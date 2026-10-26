package immersivenuclear.common.multiblock;

/**
 * Precipitation Unit, block by block: 5 wide x 7 long x 5 high, 94 blocks. On a scaffolding floor, the agitated tank
 * (metal barrels around a heavy engineering core, its agitator on top) stands on fences at the north end; under it the
 * conical bottom (iron sheetmetal) drops the slurry into the screw dewaterer, an iron sheetmetal slab ramp climbing
 * south to the discharge head (steel sheetmetal) by the redstone port. Power comes in at the south-east corner.
 *
 * Offsets are relative to the trigger (redstone engineering block, layer 2, hit with the hammer on its front face),
 * in the reference orientation (facing NORTH): the front is WEST, so the player stands west of the structure
 * looking east; right = south, back = east, left = north. dx grows east, dz grows south.
 * Ports, all on the bottom layer, where the model draws them: purified solution (2,-1,-6) in on the north end (the
 * left, seen from the front); precipitating agent (4,-1,-5) in on the back; chemical effluent (0,-1,-2) out on the front,
 * at the foot of the screw; wet concentrate (2,-1,0) out on the south end, under the discharge head; energy on the
 * heavy engineering pair of the south-east corner (4,-1..0,-1..0).
 * No Minecraft classes here, so it can be tested on its own.
 */
public final class PrecipitationUnitStructure
{
	public static final int MIN_DX = 0, MAX_DX = 4;
	public static final int MIN_DY = -1, MAX_DY = 3;
	public static final int MIN_DZ = -6, MAX_DZ = 0;

	/** Master part (the heavy engineering core of the tank); draws the whole model. */
	public static final int MASTER_DX = 2, MASTER_DY = 1, MASTER_DZ = -5;

	/**
	 * s steel scaffolding, f steel fence, l light engineering, H heavy engineering, R redstone engineering (trigger),
	 * G generator, r radiator, B metal barrel, p fluid pipe, S steel sheetmetal, I iron sheetmetal,
	 * a / A / D iron sheetmetal slab (lower / upper / double), b steel sheetmetal slab (lower), . air (not checked).
	 * LAYERS[dy - MIN_DY][dz - MIN_DZ].charAt(dx - MIN_DX); layer 0 is the bottom, row 0 the north end.
	 */
	static final String[][] LAYERS = {
			{ // y 4
					"sssss",
					"ssssl",
					"sssss",
					"lllss",
					"sssss",
					"ssssH",
					"sslsH"},
			{ // y 5
					".fGf.",
					".fIff",
					".fIf.",
					"ppIG.",
					"..A..",
					"....H",
					"RSSSH"},
			{ // y 6
					".BBB.",
					".BHBf",
					".BBB.",
					"..a..",
					"..D..",
					"..A..",
					".SSS."},
			{ // y 7
					".BlB.",
					".BHBf",
					".BBB.",
					".....",
					".....",
					"..a..",
					"..b.."},
			{ // y 8
					".brl.",
					".bGlf",
					".brl.",
					".....",
					".....",
					".....",
					"....."},
	};

	public static final char TRIGGER = 'R';

	/** What a part does besides being structure. */
	public static final int PORT_NONE = 0, PORT_SOLUTION = 1, PORT_AGENT = 2, PORT_EFFLUENT = 3, PORT_CONCENTRATE = 4, PORT_ENERGY = 5,
			PORT_REDSTONE = 6;

	/**
	 * Port parts and the face they work on, as a reference-orientation {dx, dz} step out of the part
	 * ({0, 0} = every side). Faces follow the model's port marks (blue in, orange out).
	 */
	static final int[][] PORTS = {
			// dx, dy, dz, port, face dx, face dz
			{2, -1, -6, PORT_SOLUTION, 0, -1},
			{4, -1, -5, PORT_AGENT, 1, 0},
			{0, -1, -2, PORT_EFFLUENT, -1, 0},
			{2, -1, 0, PORT_CONCENTRATE, 0, 1},
			{4, -1, -1, PORT_ENERGY, 0, 0},
			{4, -1, 0, PORT_ENERGY, 0, 0},
			{4, 0, -1, PORT_ENERGY, 0, 0},
			{4, 0, 0, PORT_ENERGY, 0, 0},
			{0, 0, 0, PORT_REDSTONE, 0, 0},
	};

	private PrecipitationUnitStructure()
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
