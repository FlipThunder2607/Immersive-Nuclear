package immersivenuclear.common.multiblock;

/**
 * Conversion Unit, block by block: 7 wide x 11 long x 11 high (363 blocks), the first machine of line 2's chain that turns
 * yellowcake into uranium hexafluoride. A one-block deck, a front row of three blocks with the reduction reactor (north),
 * a lead hot cell (middle) and the hydrofluorinator (south), a two-block service aisle (only deck), and a back row with the
 * hydrogen tank (barrels), the fluorine cells (chemical engineering with copper slabs on top), the flame tower (steel column
 * with its covered ladder on the aisle side), the cold traps (radiators with copper plates) and the scrubber (chemical
 * engineering column) with its stack (steel fences); the UF6 autoclave (barrels) is at the south end of the front row.
 *
 * Offsets are relative to the trigger (the redstone engineering block on the deck's east edge, 8th row from the north),
 * in the reference orientation (facing NORTH): the front is WEST, so the player stands west of the structure looking
 * east; right = south, back = east, left = north. dx grows east, dz grows south (the structure is west of the trigger,
 * so dx is -6..0).
 * Ports, where the user's model draws them: HF (-5,0,-7) and hydrogen (-1,0,-7) in, on the north (feed) face; UF6 (-5,0,3)
 * and the fluorinated effluent (-1,0,3) out, on the south face; yellowcake in (and the fluorination ash out) on the roof
 * hatch (-5,9,-6); energy on the heavy engineering of the east wall (0,0,-5..-3); redstone on the trigger.
 * Read from the user's build in the dev world (x200..206, y4..14, z485..495, saved 03/11/2026 21:44). No Minecraft classes.
 */
public final class ConversionUnitStructure
{
	public static final int MIN_DX = -6, MAX_DX = 0;
	public static final int MIN_DY = 0, MAX_DY = 10;
	public static final int MIN_DZ = -7, MAX_DZ = 3;

	/** Master part (steel in the middle of the deck's first level); holds the machine. */
	public static final int MASTER_DX = -5, MASTER_DY = 2, MASTER_DZ = -6;

	/**
	 * s steel scaffolding, S steel sheetmetal, P lead sheetmetal, C copper sheetmetal, b steel sheetmetal slab,
	 * c copper sheetmetal slab, M covered steel ladder, H heavy engineering, l light engineering,
	 * R redstone engineering (trigger), G generator (fluorine cells), r radiator (cold traps), B metal barrel
	 * (autoclave), p fluid pipe, f steel fence, L metal ladder, T steel block, Q chemical engineering, X radioactive
	 * handling, . air (not checked).
	 * LAYERS[dy - MIN_DY][dz - MIN_DZ].charAt(dx - MIN_DX); layer 0 is the bottom, row 0 the north end.
	 * Written for the 7 x 10 build: x200..206, y4..13, z485..494 (03/11/2026, from the 5 x 10 build plus the aisle).
	 */
	static final String[][] LAYERS = {
			{ // y 4
					"slSSSls",
					"sSSSSSs",
					"sSSSSSH",
					"sSSSSSH",
					"sSSSSSH",
					"sSSSSSs",
					"sSSSSSs",
					"sSSSSSR",
					"sSSSSSs",
					"sSSSSSs",
					"slSSSls"},
			{ // y 5
					"fpf..BB",
					"pppppBB",
					"fff..QQ",
					"pPX..QQ",
					"f.f..QQ",
					"pp...p.",
					"f.f..SS",
					"BB..MTS",
					"BB...rr",
					"BB...rr",
					"BB...QS"},
			{ // y 6
					"sbs..BB",
					"bSbp.BB",
					"sbs..QQ",
					".PX..QQ",
					"sbs..QQ",
					"bSb....",
					"sbs..SS",
					"BB..MTS",
					"BB...rr",
					"BB...rr",
					"BB...QS"},
			{ // y 7
					"sSs....",
					"SXSS...",
					"sSs..CC",
					".PX..CC",
					"sSs..CC",
					"SXSfff.",
					"sSs..SS",
					"....MTS",
					".p...rr",
					".....rr",
					".....QS"},
			{ // y 8
					"sCs....",
					"CXCS...",
					"sCs....",
					".bb....",
					"sSs....",
					"SXS....",
					"sSs..SS",
					"....MTS",
					".ppppCC",
					".....CC",
					".....QS"},
			{ // y 9
					"sCs....",
					"CXCS...",
					"sCs....",
					".......",
					"sSs....",
					"SXS....",
					"sSs..SS",
					"....MTS",
					".......",
					".......",
					".....QS"},
			{ // y 10
					"SSS....",
					"SXpp...",
					"SSSp...",
					"...p...",
					".b.p...",
					"bSbp...",
					".b.p.SS",
					"...pMTS",
					"...p...",
					"...p...",
					"...ppff"},
			{ // y 11
					"TTT....",
					"TXTp...",
					"TTT....",
					".......",
					".......",
					".ppppp.",
					".....SS",
					"....MTS",
					".......",
					".......",
					".....f."},
			{ // y 12
					"fff....",
					"fXf....",
					"fff....",
					".......",
					".......",
					".......",
					".....SS",
					"....MTS",
					".......",
					".......",
					".....f."},
			{ // y 13
					"bbb....",
					"blb....",
					"bbb....",
					".......",
					".......",
					".......",
					".....bb",
					"......b",
					".......",
					".......",
					".....f."},
			{ // y 14
					".......",
					".......",
					".......",
					".......",
					".......",
					".......",
					".......",
					".......",
					".......",
					".......",
					".....f."},
	};

	public static final char TRIGGER = 'R';

	/** What a part does besides being structure. */
	public static final int PORT_NONE = 0, PORT_HYDROGEN = 1, PORT_HF = 2, PORT_UF6 = 3, PORT_EFFLUENT = 4, PORT_ITEM = 5, PORT_ENERGY = 6, PORT_REDSTONE = 7;

	/**
	 * Port parts and the face they work on, as a reference-orientation {dx, dz} step out of the part
	 * ({0, 0} = every side). Faces follow the model's port marks (blue in, orange out).
	 */
	static final int[][] PORTS = {
			// dx, dy, dz, port, face dx, face dz
			{-1, 0, -7, PORT_HYDROGEN, 0, -1},
			{-5, 0, -7, PORT_HF, 0, -1},
			{-5, 0, 3, PORT_UF6, 0, 1},
			{-1, 0, 3, PORT_EFFLUENT, 0, 1},
			{-5, 9, -6, PORT_ITEM, 0, 0},
			{0, 0, -5, PORT_ENERGY, 0, 0},
			{0, 0, -4, PORT_ENERGY, 0, 0},
			{0, 0, -3, PORT_ENERGY, 0, 0},
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
