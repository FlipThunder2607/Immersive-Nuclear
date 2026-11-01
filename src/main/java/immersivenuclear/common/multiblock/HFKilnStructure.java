package immersivenuclear.common.multiblock;

/**
 * Hydrofluoric Acid Kiln, block by block: 5 wide x 7 long x 3 high, 84 blocks. On a scaffolding floor, the rotary kiln
 * runs from the north end (feed) to the south end (discharge): a drum of copper sheetmetal with the heating coil (LV
 * coils) round its middle and furnace heaters under it, the feed head at the north end (the light engineering blocks:
 * fluorite in on top, sulfuric acid in at the foot), the discharge head at the south end (gypsum out), the motor
 * (generator) and gearbox (steel block) behind the drum, the gas line (fluid pipes) along the back to the condenser
 * (radiators) and the hydrofluoric acid tank (metal barrels) at the south-east corner; a steel fence along the front.
 *
 * Offsets are relative to the trigger (redstone engineering block, middle layer, hit with the hammer on its front
 * face), in the reference orientation (facing NORTH): the front is WEST, so the player stands west of the structure
 * looking east; right = south, back = east, left = north. dx grows east, dz grows south.
 * Ports, where the model draws them: fluorite (1,1,0) in on top of the feed head; sulfuric acid (1,-1,0) in at the
 * feed head's foot, north face; gypsum (1,-1,6) out at the discharge head's foot, south face; hydrofluoric acid
 * (3,-1,6) out under the tank, south face; energy on the heavy engineering pair of the north-east corner (4,-1..0,0).
 * No Minecraft classes here, so it can be tested on its own.
 */
public final class HFKilnStructure
{
	public static final int MIN_DX = 0, MAX_DX = 4;
	public static final int MIN_DY = -1, MAX_DY = 1;
	public static final int MIN_DZ = 0, MAX_DZ = 6;

	/** Master part (a drum sheet, in the middle of the machine); holds the machine. */
	public static final int MASTER_DX = 2, MASTER_DY = 0, MASTER_DZ = 3;

	/**
	 * s steel scaffolding, S steel sheetmetal, C copper sheetmetal (the drum), b steel sheetmetal slab (lower),
	 * H heavy engineering, l light engineering, R redstone engineering (trigger), G generator (the motor), T steel block
	 * (the gearbox), L LV coil (the heating coil), h furnace heater, f steel fence, r radiator, B metal barrel,
	 * p fluid pipe, . air (not checked).
	 * LAYERS[dy - MIN_DY][dz - MIN_DZ].charAt(dx - MIN_DX); layer 0 is the bottom, row 0 the north end.
	 * Read from the dev world build, x200..204, y4..6, z467..473 (31/10/2026).
	 */
	static final String[][] LAYERS = {
			{ // y 4
					"slssH",
					"sssss",
					"shhSs",
					"shhSs",
					"sssss",
					"sssss",
					"slsls"},
			{ // y 5
					"RSS.H",
					"fCC..",
					"fLLG.",
					"fCCT.",
					"fCC..",
					"fCCrp",
					".SSBB"},
			{ // y 6
					".lbp.",
					".CCp.",
					".LLp.",
					".CCp.",
					".CCp.",
					".CCr.",
					".bbbb"},
	};

	public static final char TRIGGER = 'R';

	/** What a part does besides being structure. */
	public static final int PORT_NONE = 0, PORT_FLUORITE = 1, PORT_ACID = 2, PORT_GYPSUM = 3, PORT_HF = 4, PORT_ENERGY = 5, PORT_REDSTONE = 6;

	/**
	 * Port parts and the face they work on, as a reference-orientation {dx, dz} step out of the part
	 * ({0, 0} = every side). Faces follow the model's port marks (blue in, orange out).
	 */
	static final int[][] PORTS = {
			// dx, dy, dz, port, face dx, face dz
			{1, 1, 0, PORT_FLUORITE, 0, 0},
			{1, -1, 0, PORT_ACID, 0, -1},
			{1, -1, 6, PORT_GYPSUM, 0, 1},
			{3, -1, 6, PORT_HF, 0, 1},
			{4, -1, 0, PORT_ENERGY, 0, 0},
			{4, 0, 0, PORT_ENERGY, 0, 0},
			{0, 0, 0, PORT_REDSTONE, 0, 0},
	};

	private HFKilnStructure()
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
