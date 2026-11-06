package immersivenuclear.common.multiblock;

/**
 * The computers' multiblocks, block by block. They all work the same way, so a new one is one line in {@link #ALL} (and its names in the lang files, and its model in ferramentas/): the hammer works on any
 * of its control engineering blocks, it has no function yet (the user, 05/11/2026), and it is never formed mirrored: the redstone engineering block goes on the back corner on the left seen from the front.
 *
 * Offsets are relative to the bottom front block of the north end, in the reference orientation (facing NORTH): the front is WEST, so the player stands west of it looking east; right = south,
 * back = east. dx grows east, dz grows south. No Minecraft classes here, so it can be tested on its own.
 */
public final class ComputerStructure
{
	/**
	 * Mainframe, 4 wide x 2 deep x 4 high: a front of control engineering on a steel sheetmetal base, under a row of data cable coils; behind it steel sheetmetal with a core of control engineering, on a
	 * floor of steel blocks with a redstone engineering block on one corner, under a row of radiators. The model's two conduit entries are on the back of the bottom layer, at (1, 0, 1) and (1, 0, 2).
	 * Read from the dev world, x210..211, y4..7, z409..412 (05/11/2026).
	 */
	public static final ComputerStructure MAINFRAME = new ComputerStructure(0, "mainframe", "Mainframe", 0, 1, 1, new String[][]{
			{"SR", "ST", "ST", "ST"},
			{"CS", "CC", "CC", "CS"},
			{"CS", "CC", "CC", "CS"},
			{"Dr", "Dr", "Dr", "Dr"},
	});

	/** I/O Processor, 2 wide x 2 deep x 4 high, the same make. The model's conduit entry is at (1, 0, 1). Read from the dev world, x210..211, y4..7, z415..416 (05/11/2026). */
	public static final ComputerStructure IO_PROCESSOR = new ComputerStructure(1, "io_processor", "IOProcessor", 0, 1, 0, new String[][]{
			{"SR", "ST"},
			{"CS", "CS"},
			{"CS", "CS"},
			{"Dr", "Dr"},
	});

	/** Memory Cabinet, 2 wide x 2 deep x 4 high: the I/O Processor's make, with no radiators. Read from the dev world, x210..211, y4..7, z419..420 (05/11/2026). */
	public static final ComputerStructure MEMORY_CABINET = new ComputerStructure(2, "memory_cabinet", "MemoryCabinet", 0, 1, 0, new String[][]{
			{"SR", "ST"},
			{"CS", "CS"},
			{"CS", "CS"},
			{"DS", "DS"},
	});

	/** Disk Unit, 2 wide x 2 deep x 2 high: data cable coils under the control engineering, light engineering (the drive) at the back. Read from the dev world, x210..211, y4..5, z423..424 (05/11/2026). */
	public static final ComputerStructure DISK_UNIT = new ComputerStructure(3, "disk_unit", "DiskUnit", 0, 1, 0, new String[][]{
			{"DR", "DT"},
			{"CL", "CL"},
	});

	/** In the order they are tried by the hammer: the bigger first, as a smaller one can stand inside its build. */
	public static final ComputerStructure[] ALL = {MAINFRAME, IO_PROCESSOR, MEMORY_CABINET, DISK_UNIT};

	public static final char TRIGGER = 'C';

	/** Place in {@link #ALL}. */
	public final int index;
	/** Registry name; also the name of its model, hit boxes and blockstate. */
	public final String name;
	/** The name in IE's multiblock list. */
	public final String uniqueName;
	/** The master part. */
	public final int masterDx, masterDy, masterDz;
	/**
	 * S steel sheetmetal, T steel block, C control engineering (where the hammer works), r radiator, R redstone engineering (for the redstone port, later), D data cable coil, L light engineering.
	 * layers[dy][dz].charAt(dx); layer 0 is the bottom, row 0 the north end, column 0 the front.
	 */
	final String[][] layers;
	public final int maxDx, maxDy, maxDz;

	private ComputerStructure(int index, String name, String uniqueName, int masterDx, int masterDy, int masterDz, String[][] layers)
	{
		this.index = index;
		this.name = name;
		this.uniqueName = "IN:" + uniqueName;
		this.masterDx = masterDx;
		this.masterDy = masterDy;
		this.masterDz = masterDz;
		this.layers = layers;
		maxDy = layers.length - 1;
		maxDz = layers[0].length - 1;
		maxDx = layers[0][0].length() - 1;
	}

	/** The structure of that name; the Mainframe for a part saved before there were others. */
	public static ComputerStructure byName(String name)
	{
		for(ComputerStructure s : ALL)
			if(s.name.equals(name))
				return s;
		return MAINFRAME;
	}

	public char get(int dx, int dy, int dz)
	{
		if(dx < 0 || dx > maxDx || dy < 0 || dy > maxDy || dz < 0 || dz > maxDz)
			return '.';
		return layers[dy][dz].charAt(dx);
	}

	public boolean isPart(int dx, int dy, int dz)
	{
		return get(dx, dy, dz) != '.';
	}

	public int count(char c)
	{
		int n = 0;
		for(String[] layer : layers)
			for(String row : layer)
				for(int i = 0; i < row.length(); i++)
					if(row.charAt(i) == c)
						n++;
		return n;
	}

	/** See {@link ChemicalProcessorStructure#rotate}. Never mirrored. */
	public static int[] rotate(int dx, int dz, int turns)
	{
		return ChemicalProcessorStructure.rotate(dx, dz, turns, false);
	}

	/** Inverse of {@link #rotate}. */
	public static int[] unrotate(int x, int z, int turns)
	{
		return ChemicalProcessorStructure.unrotate(x, z, turns, false);
	}
}
