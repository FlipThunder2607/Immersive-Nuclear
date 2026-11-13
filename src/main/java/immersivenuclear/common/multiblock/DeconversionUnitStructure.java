package immersivenuclear.common.multiblock;

/**
 * Deconversion Unit, block by block: 6 wide x 9 long x 6 high, 134 blocks. A rotary kiln plant: the depleted uranium
 * hexafluoride and steam come in at the north end (the feed hood and hydrolysis reactor), the kiln runs along the machine
 * through its electric furnace (the LV coils) turned by its motor (generator), the oxide leaves at the south end's discharge
 * hood into a screw to the back, and the gas goes through the filters and the condenser to the hydrofluoric acid port at
 * the south end. The electrical cabinet (heavy engineering) stands at the back with the redstone engineering block (the
 * trigger) at its foot.
 *
 * Offsets are relative to the trigger (redstone engineering block, the cabinet's foot, hit with the hammer on its back
 * face), in the reference orientation (facing NORTH): the front is WEST, so the player stands west of the structure
 * looking east; back = east, left = north. dx grows east, dz grows south.
 * Ports, where the model draws them: depleted UF6 (-4,-1,-4) and steam (-1,-1,-4) in on the north face; hydrofluoric
 * acid (-4,-1,4) out on the south face; the oxide (0,-1,3) out on the east (back) face; energy on the cabinet's top
 * heavy engineering block (0,2,1), where the model draws the roof connector.
 * No Minecraft classes here, so it can be tested on its own.
 */
public final class DeconversionUnitStructure
{
	public static final int MIN_DX = -5, MAX_DX = 0;
	public static final int MIN_DY = -1, MAX_DY = 4;
	public static final int MIN_DZ = -4, MAX_DZ = 4;

	/** Master part (an LV coil of the furnace, in the middle of the machine); holds the machine. */
	public static final int MASTER_DX = -3, MASTER_DY = 0, MASTER_DZ = 0;

	/**
	 * s steel scaffolding, S steel sheetmetal, I iron sheetmetal, P lead sheetmetal (the kiln's shielding), T steel block,
	 * b steel sheetmetal slab, i iron sheetmetal slab, q lead sheetmetal slab, c copper sheetmetal slab (the bus bars),
	 * t steel block slab (all slabs lower), h furnace heater (under the electric furnace), L LV coil, f steel fence,
	 * l light engineering, G generator, H heavy engineering, R redstone engineering (trigger), X radioactive handling
	 * (the ports), p fluid pipe, . air (not checked).
	 * LAYERS[dy - MIN_DY][dz - MIN_DZ].charAt(dx - MIN_DX); layer 0 is the bottom, row 0 the north end.
	 * Read from the dev world build, x200..205, y4..9, z531..539 (13/11/2026, aligned with the model).
	 */
	static final String[][] LAYERS = {
			{ // y 4
					"sXSSXs",
					"sSSSSs",
					"sSSSSs",
					"shhSSs",
					"shhSSs",
					"shhSSs",
					"sSSSSs",
					"sSSSSX",
					"sXSSSs"},
			{ // y 5
					"ITT.I.",
					"IllG..",
					"...ls.",
					".SL.s.",
					".SLf.R",
					".SLf.H",
					".llG..",
					".TTlbb",
					"IpppI."},
			{ // y 6
					"ITTpI.",
					"IPPp..",
					".PPpI.",
					".SLpI.",
					".SLf.H",
					".SLf.H",
					".PP...",
					".TT...",
					"I...I."},
			{ // y 7
					".II.I.",
					".qq...",
					".qq.I.",
					".SL.I.",
					".SLccH",
					".SLccH",
					".qq...",
					".tt...",
					"....I."},
			{ // y 8
					".II.i.",
					"......",
					"....p.",
					"....p.",
					"....p.",
					"....p.",
					"....p.",
					"....p.",
					"....I."},
			{ // y 9
					".ii...",
					"......",
					"......",
					"......",
					"......",
					"......",
					"......",
					"......",
					"....p."},
	};

	public static final char TRIGGER = 'R';

	/** What a part does besides being structure. */
	public static final int PORT_NONE = 0, PORT_UF6 = 1, PORT_STEAM = 2, PORT_HF = 3, PORT_OXIDE = 4, PORT_ENERGY = 5, PORT_REDSTONE = 6;

	/**
	 * Port parts and the face they work on, as a reference-orientation {dx, dz} step out of the part
	 * ({0, 0} = every side). Faces follow the model's port marks (blue in, orange out).
	 */
	static final int[][] PORTS = {
			// dx, dy, dz, port, face dx, face dz
			{-4, -1, -4, PORT_UF6, 0, -1},
			{-1, -1, -4, PORT_STEAM, 0, -1},
			{-4, -1, 4, PORT_HF, 0, 1},
			{0, -1, 3, PORT_OXIDE, 1, 0},
			{0, 2, 1, PORT_ENERGY, 0, 0},
			{0, 0, 0, PORT_REDSTONE, 0, 0},
	};

	private DeconversionUnitStructure()
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
