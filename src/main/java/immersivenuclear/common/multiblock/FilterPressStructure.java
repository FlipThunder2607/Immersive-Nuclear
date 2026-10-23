package immersivenuclear.common.multiblock;

/**
 * Filtration Unit (filter press), read block by block from the user's build in the dev world
 * (New World, x 194..196, y 4..6, z 425..433, the user's third version: under the plates, steel scaffolding slabs
 * instead of iron sheetmetal and light engineering for the two filter ports at the back), 3 wide x 3 high x 9 long,
 * 56 blocks.
 *
 * Offsets are relative to the trigger (redstone engineering block, layer 2, hit with the hammer on its front face),
 * in the reference orientation (facing NORTH): the front is WEST, so the player stands west of the structure
 * looking east; right = south, back = east, left = north. dx grows east, dz grows south.
 * The press runs along dz: the pump end (polpa in) at dz -8, the plates over the conveyor at dz -7..-1, the
 * outputs and the redstone at dz 0.
 * Ports (as in the user's filtro-prensa.bbmodel): polpa input front-left (0,-1,-8), crude solution output
 * front-right (0,-1,0), residue output at the end of the conveyor (1,-1,0) on the right side, on the back the filter
 * input (2,-1,-3) and the spent filter output (2,-1,-5) for automating the filter swap, energy on the heavy
 * engineering column (2,-1..0,0).
 * No Minecraft classes here, so it can be tested on its own.
 */
public final class FilterPressStructure
{
	public static final int MIN_DX = 0, MAX_DX = 2;
	public static final int MIN_DY = -1, MAX_DY = 1;
	public static final int MIN_DZ = -8, MAX_DZ = 0;

	/** Master part (the middle steel fence over the plates); draws the whole model. */
	public static final int MASTER_DX = 1, MASTER_DY = 0, MASTER_DZ = -4;

	/**
	 * l light engineering, s steel scaffolding, a steel scaffolding slab (lower half), H heavy engineering,
	 * S steel sheetmetal, b steel sheetmetal slab (lower half), C conveyor belt (any direction), B metal barrel, P fluid pump (both halves),
	 * G generator, F steel fence, R redstone engineering (trigger), . air (not checked).
	 * LAYERS[dy - MIN_DY][dz - MIN_DZ].charAt(dx - MIN_DX); layer 0 is the bottom, row 0 the pump end (north).
	 */
	static final String[][] LAYERS = {
			{ // y 4
					"lls",
					"SlS",
					"aCa",
					"aCl",
					"aCa",
					"aCl",
					"aCa",
					"SlS",
					"llH"},
			{ // y 5
					"BPB",
					"SGS",
					".Fb",
					".Fb",
					".Fb",
					".Fb",
					".Fb",
					"SGS",
					"RbH"},
			{ // y 6
					".P.",
					"bbb",
					"...",
					"...",
					"...",
					"...",
					"...",
					"bbb",
					"..."},
	};

	public static final char TRIGGER = 'R';

	/** What a part does besides being structure. */
	public static final int PORT_NONE = 0, PORT_POLPA = 1, PORT_SOLUTION = 2, PORT_RESIDUE = 3, PORT_FILTER = 4, PORT_ENERGY = 5, PORT_REDSTONE = 6, PORT_FILTER_OUT = 7;

	/**
	 * Port parts and the face they work on, as a reference-orientation {dx, dz} step out of the part
	 * ({0, 0} = every side). Faces follow the user's model: polpa in and crude solution out on the front (west),
	 * residue out on the right side (south) where the conveyor ends, filters in and spent filters out on the back
	 * (east, the user's filtro-prensa-v2-abertura.bbmodel: blue at z 443, orange at z 441 of the build); energy takes
	 * any side of both heavy engineering blocks; the redstone port is the trigger block.
	 */
	static final int[][] PORTS = {
			// dx, dy, dz, port, face dx, face dz
			{0, -1, -8, PORT_POLPA, -1, 0},
			{0, -1, 0, PORT_SOLUTION, -1, 0},
			{1, -1, 0, PORT_RESIDUE, 0, 1},
			{2, -1, -3, PORT_FILTER, 1, 0},
			{2, -1, -5, PORT_FILTER_OUT, 1, 0},
			{2, -1, 0, PORT_ENERGY, 0, 0},
			{2, 0, 0, PORT_ENERGY, 0, 0},
			{0, 0, 0, PORT_REDSTONE, 0, 0},
	};

	private FilterPressStructure()
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
