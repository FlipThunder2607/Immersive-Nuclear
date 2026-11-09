package immersivenuclear.common.multiblock;

/**
 * The Enrichment Unit (first assembly: two banks of centrifuges), read from the user's build in the dev world (x200..207, y4..10, z498..506, saved 08/11/2026 19:49).
 * Offsets are relative to the trigger (the redstone engineering block in the middle of the front edge), in the reference orientation (facing NORTH): the front is WEST, so the player stands
 * west of the structure looking east; right = south, back = east, left = north. dx grows east, dz grows south. No Minecraft classes.
 */
public final class EnrichmentUnitStructure
{
	public static final int MIN_DX = 0, MAX_DX = 7;
	public static final int MIN_DY = 0, MAX_DY = 6;
	public static final int MIN_DZ = -4, MAX_DZ = 4;

	/** Master part: the chemical engineering core in the middle of the first bank, one level above the deck. */
	public static final int MASTER_DX = 4, MASTER_DY = 1, MASTER_DZ = 0;
	public static final char TRIGGER = 'R';

	/** s steel scaffolding, f steel fence, S steel sheetmetal, I iron sheetmetal, b steel sheetmetal slab, R redstone engineering (trigger), H heavy engineering, l light engineering, G generator, p fluid pipe, Q chemical engineering, X radioactive handling, . air (not checked). */
	static final String[][] LAYERS = {
		{
			"sQSSSSSs",
			"sSSSSSSs",
			"sSSSSSSs",
			"sSSSSSSs",
			"RSSSSSSs",
			"sSSSSSSs",
			"sSSSSSSs",
			"sSSSSSSs",
			"sQSQSSSs"
		},
		{
			".BfSSSSf",
			".B.SQQS.",
			".GGSQQSf",
			".GGSQQS.",
			".GGSQQS.",
			".GGSQQSf",
			".GGSQQS.",
			"..fSSSSf",
			".B.B..l."
		},
		{
			".BfSSSSf",
			".B.SXXS.",
			".GGSXXSf",
			".GGSXXS.",
			".GGSXXS.",
			".GGSXXSf",
			".GGSXXS.",
			"..fSSSSf",
			".B.B..G."
		},
		{
			"..fSSSSf",
			".p.SXXS.",
			".bbSXXSf",
			".HbSXXS.",
			".HbSXXS.",
			".HbSXXSf",
			".bbSXXS.",
			"..fSSSSf",
			".B.B..p."
		},
		{
			"..fSSSSf",
			".p.SQQS.",
			"...SQQSf",
			"...SQQS.",
			"...SQQS.",
			"...SQQSf",
			"...SQQS.",
			"..fSSSSf",
			".p.p..p."
		},
		{
			"..fbbbbf",
			".p.bllb.",
			"...bllbf",
			"...bllb.",
			"...bllb.",
			"...bllbf",
			"...bllb.",
			"..fbbbbf",
			".p.p..p."
		},
		{
			"..ffffff",
			".ppppp..",
			"....ppff",
			"....pp..",
			"....pp..",
			"....ppff",
			"....pp..",
			"..ffppff",
			".pppppp."
		}
	};

	public static final int PORT_NONE = 0, PORT_FEED = 1, PORT_PRODUCT = 2, PORT_TAILS = 3, PORT_ENERGY = 4, PORT_REDSTONE = 5;

	/** dx, dy, dz, port, face dx, face dz ({0, 0} = every side). Natural hexafluoride in on the north face; product and tails out on the south face. Energy on the heavy engineering blocks of the converters' roof; the player places the HV connector on them. */
	static final int[][] PORTS = {
			{1, 0, -4, PORT_FEED, 0, -1},
			{1, 0, 4, PORT_PRODUCT, 0, 1},
			{3, 0, 4, PORT_TAILS, 0, 1},
			{1, 3, -1, PORT_ENERGY, 0, 0},
			{1, 3, 0, PORT_ENERGY, 0, 0},
			{1, 3, 1, PORT_ENERGY, 0, 0},
			{0, 0, 0, PORT_REDSTONE, 0, 0},
	};

	// COLLISION-ONLY START (ferramentas/enriquecimento_colisao.py)
	static final int[][] COLLISION_ONLY = {
		{0, 1, -4}, {0, 1, -3}, {2, 1, -3}, {7, 1, -3}, {0, 1, -2}, {0, 1, -1}, {7, 1, -1}, {0, 1, 0},
		{7, 1, 0}, {0, 1, 1}, {0, 1, 2}, {7, 1, 2}, {0, 1, 3}, {1, 1, 3}, {0, 1, 4}, {2, 1, 4},
		{4, 1, 4}, {5, 1, 4}, {7, 1, 4}, {0, 2, -4}, {0, 2, -3}, {2, 2, -3}, {7, 2, -3}, {0, 2, -2},
		{0, 2, -1}, {7, 2, -1}, {0, 2, 0}, {7, 2, 0}, {0, 2, 1}, {0, 2, 2}, {7, 2, 2}, {0, 2, 3},
		{1, 2, 3}, {0, 2, 4}, {2, 2, 4}, {4, 2, 4}, {1, 3, -4}, {7, 3, -3}, {0, 3, -2}, {0, 3, -1},
		{7, 3, -1}, {0, 3, 0}, {7, 3, 0}, {0, 3, 1}, {0, 3, 2}, {7, 3, 2}, {0, 3, 3}, {1, 3, 3},
		{0, 3, 4}, {2, 3, 4}, {4, 3, 4}, {1, 4, -4}, {7, 4, -3}, {7, 4, -1}, {7, 4, 0}, {7, 4, 2},
		{1, 4, 3}, {2, 5, -3}, {7, 5, -3}, {7, 5, -1}, {7, 5, 0}, {7, 5, 2}, {1, 5, 3}, {6, 6, -3},
		{7, 6, -3}, {3, 6, -2}, {3, 6, -1}, {6, 6, -1}, {7, 6, -1}, {3, 6, 0}, {6, 6, 0}, {7, 6, 0},
		{3, 6, 1}, {3, 6, 2}, {6, 6, 2}, {7, 6, 2}, {1, 6, 3}, {7, 6, 4},
	};
	// COLLISION-ONLY END

	private EnrichmentUnitStructure()
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

	public static int[] rotate(int dx, int dz, int turns, boolean mirror)
	{
		return ChemicalProcessorStructure.rotate(dx, dz, turns, mirror);
	}

	public static int[] unrotate(int x, int z, int turns, boolean mirror)
	{
		return ChemicalProcessorStructure.unrotate(x, z, turns, mirror);
	}
}
