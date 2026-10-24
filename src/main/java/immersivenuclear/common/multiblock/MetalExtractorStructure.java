package immersivenuclear.common.multiblock;

/**
 * Metal Extractor, block by block: 4 deep x 4 high x 4 long, 44 blocks. The electrolysis vat (steel sheetmetal walls,
 * with the heavy engineering beam the plates hang from, and the scaffolding guides of the plates' platform above it)
 * fills the front three quarters; the power controller (light engineering under three HV connectors) stands at the
 * back, with the pump and the energy column on the right end and the two generators on top of the right end.
 *
 * Offsets are relative to the trigger (redstone engineering block, layer 2, hit with the hammer on its front face),
 * in the reference orientation (facing NORTH): the front is WEST, so the player stands west of the structure
 * looking east; right = south, back = east, left = north. dx grows east, dz grows south.
 * Ports, all on the bottom layer: chemical effluent (1,-1,-3) in on the left end, depleted effluent (1,-1,0) out on
 * the right end, metal deposit (3,-1,-2) out on the back; energy on the heavy engineering column (3,-1..0,0).
 * No Minecraft classes here, so it can be tested on its own.
 */
public final class MetalExtractorStructure
{
	public static final int MIN_DX = 0, MAX_DX = 3;
	public static final int MIN_DY = -1, MAX_DY = 2;
	public static final int MIN_DZ = -3, MAX_DZ = 0;

	/** Master part (the middle of the plates' beam, over the vat); draws the whole model. */
	public static final int MASTER_DX = 1, MASTER_DY = 0, MASTER_DZ = -2;

	/**
	 * s steel scaffolding, l light engineering, H heavy engineering, R redstone engineering (trigger), G generator,
	 * S steel sheetmetal, b steel sheetmetal slab (lower half), P fluid pump (either half), p fluid pipe,
	 * C HV connector, . air (not checked).
	 * LAYERS[dy - MIN_DY][dz - MIN_DZ].charAt(dx - MIN_DX); layer 0 is the bottom, row 0 the left end (north).
	 */
	static final String[][] LAYERS = {
			{ // y 4
					"SlSs",
					"SSSl",
					"SSSs",
					"slPH"},
			{ // y 5
					"bHbl",
					"bHbl",
					"bHbl",
					"RpPH"},
			{ // y 6
					".s.C",
					".s.C",
					".s.C",
					"...."},
			{ // y 7
					".b..",
					".b..",
					".b..",
					"GSG."},
	};

	public static final char TRIGGER = 'R';

	/** What a part does besides being structure. */
	public static final int PORT_NONE = 0, PORT_EFFLUENT = 1, PORT_DEPLETED = 2, PORT_DEPOSIT = 3, PORT_ENERGY = 4, PORT_REDSTONE = 5;

	/**
	 * Port parts and the face they work on, as a reference-orientation {dx, dz} step out of the part
	 * ({0, 0} = every side). Faces follow the model: the effluent in on the left end (north), the depleted effluent
	 * out on the right end (south), the deposit out on the back (east); energy takes any side of both heavy
	 * engineering blocks of the right end; the redstone port is the trigger block.
	 */
	static final int[][] PORTS = {
			// dx, dy, dz, port, face dx, face dz
			{1, -1, -3, PORT_EFFLUENT, 0, -1},
			{1, -1, 0, PORT_DEPLETED, 0, 1},
			{3, -1, -2, PORT_DEPOSIT, 1, 0},
			{3, -1, 0, PORT_ENERGY, 0, 0},
			{3, 0, 0, PORT_ENERGY, 0, 0},
			{0, 0, 0, PORT_REDSTONE, 0, 0},
	};

	private MetalExtractorStructure()
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
