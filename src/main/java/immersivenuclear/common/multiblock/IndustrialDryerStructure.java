package immersivenuclear.common.multiblock;

/**
 * Industrial Dryer, block by block: 4 wide x 7 long x 3 high, 63 blocks. On a scaffolding floor with three furnace
 * heaters under it, the drying chamber (iron sheetmetal, two blocks wide, under a roof of iron slabs; the feed hopper
 * stands up from the roof as a full block) runs from the drive at the north end (generator beside the redstone
 * engineering, light engineering under them, power on the heavy engineering pair of the north-east corner) to the
 * discharge at the south end. Along the back, the condensate tank (metal barrels); over it, the vapour pipes and the
 * condenser (radiator).
 *
 * Offsets are relative to the trigger (redstone engineering block, middle layer, hit with the hammer on its front face),
 * in the reference orientation (facing NORTH): the front is WEST, so the player stands west of the structure
 * looking east; right = south, back = east, left = north. dx grows east, dz grows south.
 * Ports, where the model draws them: wet concentrate (2,1,2) in through the feed hopper on the chamber's roof (the
 * model draws the hopper over the roof slab beside it, (1,1,2), which takes it too);
 * yellowcake (1,-1,6) out on the south end, under the discharge; process condensate (3,-1,5) out on the back, by the
 * condensate tank; energy on the heavy engineering pair of the north-east corner (3,-1..0,0).
 * No Minecraft classes here, so it can be tested on its own.
 */
public final class IndustrialDryerStructure
{
	public static final int MIN_DX = 0, MAX_DX = 3;
	public static final int MIN_DY = -1, MAX_DY = 1;
	public static final int MIN_DZ = 0, MAX_DZ = 6;

	/** Master part (a roof slab in the middle of the drying chamber); draws the whole model. */
	public static final int MASTER_DX = 1, MASTER_DY = 1, MASTER_DZ = 3;

	/**
	 * s steel scaffolding, l light engineering, H heavy engineering, R redstone engineering (trigger), G generator,
	 * r radiator, F furnace heater, B metal barrel, p fluid pipe, I iron sheetmetal, a iron sheetmetal slab (lower),
	 * . air (not checked).
	 * LAYERS[dy - MIN_DY][dz - MIN_DZ].charAt(dx - MIN_DX); layer 0 is the bottom, row 0 the north end.
	 */
	static final String[][] LAYERS = {
			{ // y 4
					"sllH",
					"ssss",
					"sFss",
					"sFss",
					"sFss",
					"sssl",
					"slss"},
			{ // y 5
					"RG.H",
					".II.",
					".II.",
					".IIB",
					".IIB",
					".IIB",
					".IIB"},
			{ // y 6
					"....",
					".aa.",
					".aI.",
					".aap",
					".aap",
					".aar",
					".aap"},
	};

	public static final char TRIGGER = 'R';

	/** What a part does besides being structure. */
	public static final int PORT_NONE = 0, PORT_WET = 1, PORT_YELLOWCAKE = 2, PORT_CONDENSATE = 3, PORT_ENERGY = 4, PORT_REDSTONE = 5;

	/**
	 * Port parts and the face they work on, as a reference-orientation {dx, dz} step out of the part
	 * ({0, 0} = every side). Faces follow the model's port marks (blue in, orange out).
	 */
	static final int[][] PORTS = {
			// dx, dy, dz, port, face dx, face dz
			{2, 1, 2, PORT_WET, 0, 0},
			{1, 1, 2, PORT_WET, 0, 0},
			{1, -1, 6, PORT_YELLOWCAKE, 0, 1},
			{3, -1, 5, PORT_CONDENSATE, 1, 0},
			{3, -1, 0, PORT_ENERGY, 0, 0},
			{3, 0, 0, PORT_ENERGY, 0, 0},
			{0, 0, 0, PORT_REDSTONE, 0, 0},
	};

	private IndustrialDryerStructure()
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
