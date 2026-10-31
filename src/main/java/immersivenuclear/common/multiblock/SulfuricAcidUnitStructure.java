package immersivenuclear.common.multiblock;

/**
 * Sulfuric Acid Unit, block by block: 5 wide x 7 long x 8 high, 126 blocks. On a scaffolding floor (steel sheetmetal
 * under the burner), the contact process from the north end to the south end: the blower (generator and two radiators
 * beside the redstone engineering; power on the heavy engineering pair of the north-east corner), the sulfur burner
 * (blast bricks, two high, with the sulfur hopper set into its top), behind it the waste heat boiler under its steam drum (iron
 * sheetmetal) and the converter (metal barrels under a roof of steel slabs), the absorption tower at the south end
 * (metal barrels, iron sheetmetal and steel slabs on top, the stack above) and the pump tank beside it. Fluid pipes
 * run up the back (steam), from the boiler to the converter and from the pump tank back to the top of the tower.
 *
 * Offsets are relative to the trigger (redstone engineering block, bottom layer of the machine, hit with the hammer on
 * its front face), in the reference orientation (facing NORTH): the front is WEST, so the player stands west of the
 * structure looking east; right = south, back = east, left = north. dx grows east, dz grows south.
 * Ports, where the model draws them: sulfur (1,1,1) in through the hopper set into the burner's top; water (1,4,4) in on the
 * absorption tower's north face; sulfuric acid (3,-1,6) out on the south end, under the pump tank; steam (4,-1,1) out
 * on the back, at the foot of the steam line; energy on the heavy engineering pair of the north-east corner (4,-1..0,0).
 * No Minecraft classes here, so it can be tested on its own.
 */
public final class SulfuricAcidUnitStructure
{
	public static final int MIN_DX = 0, MAX_DX = 4;
	public static final int MIN_DY = -1, MAX_DY = 6;
	public static final int MIN_DZ = 0, MAX_DZ = 6;

	/** Master part (a converter barrel, in the middle of the machine); holds the machine. */
	public static final int MASTER_DX = 2, MASTER_DY = 1, MASTER_DZ = 2;

	/**
	 * s steel scaffolding, S steel sheetmetal, I iron sheetmetal, b steel sheetmetal slab (lower), H heavy engineering,
	 * l light engineering, R redstone engineering (trigger), G generator, r radiator, K blast brick, B metal barrel,
	 * p fluid pipe, . air (not checked).
	 * LAYERS[dy - MIN_DY][dz - MIN_DZ].charAt(dx - MIN_DX); layer 0 is the bottom, row 0 the north end.
	 * Read from the dev world build, x200..204, y4..12, z458..464 (31/10/2026, third build: the sulfur hopper set into the burner's top, no roof slabs on the converter).
	 */
	static final String[][] LAYERS = {
			{ // y 4
					"ssssH",
					"SSssl",
					"SSsss",
					"SSsss",
					"sssss",
					"sssss",
					"sssls"},
			{ // y 5
					"RGrrH",
					"KKBBp",
					"KKBB.",
					"KKBB.",
					"BBpSS",
					"BB.B.",
					"SSr.."},
			{ // y 6
					".....",
					"KlBBp",
					"KKBBp",
					"KKBBp",
					"BB...",
					"BB.B.",
					".b..."},
			{ // y 7
					".....",
					"..BBp",
					".pBBp",
					"..BBp",
					"BB...",
					"BB.p.",
					"....."},
			{ // y 8
					".....",
					"..IIp",
					".pBB.",
					".pBB.",
					"BB...",
					"BB.p.",
					"....."},
			{ // y 9
					".....",
					".....",
					".....",
					".....",
					"Blpp.",
					"BB.p.",
					"....."},
			{ // y 10
					".....",
					".....",
					".....",
					".....",
					"bI...",
					"bb...",
					"....."},
			{ // y 11
					".....",
					".....",
					".....",
					".....",
					".p...",
					".....",
					"....."},
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
			{1, 1, 1, PORT_SULFUR, 0, 0},
			{1, 4, 4, PORT_WATER, 0, -1},
			{3, -1, 6, PORT_ACID, 0, 1},
			{4, -1, 1, PORT_STEAM, 1, 0},
			{4, -1, 0, PORT_ENERGY, 0, 0},
			{4, 0, 0, PORT_ENERGY, 0, 0},
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
