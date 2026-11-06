package immersivenuclear.common.multiblock;

/**
 * The computers' multiblocks, block by block. They all work the same way, so a new one is one line in {@link #ALL} (and its names in the lang files, and its model in ferramentas/): the hammer works on any
 * of its control engineering blocks (the Printer: its light engineering), it has no function yet (the user, 05/11/2026), and it is never formed mirrored: the redstone engineering block, on those wide enough to have one, goes on the back corner on the left seen from the front.
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
	}, 1, 2);

	/** I/O Processor, 2 wide x 2 deep x 4 high, the same make. The model's conduit entry is at (1, 0, 1). Read from the dev world, x210..211, y4..7, z415..416 (05/11/2026). */
	public static final ComputerStructure IO_PROCESSOR = new ComputerStructure(1, "io_processor", "IOProcessor", 0, 1, 0, new String[][]{
			{"SR", "ST"},
			{"CS", "CS"},
			{"CS", "CS"},
			{"Dr", "Dr"},
	}, 1);

	/** Memory Cabinet, 2 wide x 2 deep x 4 high: the I/O Processor's make, with no radiators. Read from the dev world, x210..211, y4..7, z419..420 (05/11/2026). */
	public static final ComputerStructure MEMORY_CABINET = new ComputerStructure(2, "memory_cabinet", "MemoryCabinet", 0, 1, 0, new String[][]{
			{"SR", "ST"},
			{"CS", "CS"},
			{"CS", "CS"},
			{"DS", "DS"},
	}, 1);

	/** Disk Unit, 2 wide x 2 deep x 2 high: data cable coils under the control engineering, light engineering (the drive) at the back. Read from the dev world, x210..211, y4..5, z423..424 (05/11/2026). */
	public static final ComputerStructure DISK_UNIT = new ComputerStructure(3, "disk_unit", "DiskUnit", 0, 1, 0, new String[][]{
			{"DR", "DT"},
			{"CL", "CL"},
	}, 1);

	/** Tape Unit, 2 wide x 2 deep x 4 high: data cable coils under the control engineering at the front, steel sheetmetal behind them, under a top of light engineering (the reels' drive). Read from the dev world, x210..211, y4..7, z427..428 (06/11/2026). */
	public static final ComputerStructure TAPE_UNIT = new ComputerStructure(4, "tape_unit", "TapeUnit", 0, 2, 0, new String[][]{
			{"SR", "ST"},
			{"DS", "DS"},
			{"CS", "CS"},
			{"LL", "LL"},
	}, 1);

	/** Operator Console, 4 wide x 2 deep x 2 high: a row of control engineering on data cable coils at the front, steel sheetmetal slabs on a floor of steel blocks behind them (either half of the slab will do). Read from the dev world, x210..211, y4..5, z431..434 (06/11/2026). */
	public static final ComputerStructure OPERATOR_CONSOLE = new ComputerStructure(5, "operator_console", "OperatorConsole", 0, 1, 1, new String[][]{
			{"DR", "DT", "DT", "DT"},
			{"Cs", "Cs", "Cs", "Cs"},
	}, 0, 3);

	/**
	 * Printer, 2 wide x 1 deep x 2 high: light engineering on the redstone engineering block, a data cable coil on a steel block. It has no control engineering: the hammer works on the light engineering.
	 * Seen from the front the redstone engineering is on the left. Read from the dev world, x211, y4..5, z440..441 (06/11/2026).
	 */
	public static final ComputerStructure PRINTER = new ComputerStructure(6, "printer", "Printer", 'L', 0, 1, 0, new String[][]{
			{"R", "T"},
			{"L", "D"},
	}, 0, 1);

	/**
	 * Teletype, a single column 1 wide x 1 deep x 2 high: control engineering on a data cable coil, too small for a redstone engineering block. The columns stand inside other builds, so they stay after them
	 * in {@link #ALL}. A column looks the same from every side: it faces the side the hammer hit. Read from the dev world, x211, y4..5, z437 (06/11/2026).
	 */
	public static final ComputerStructure TELETYPE = new ComputerStructure(7, "teletype", "Teletype", 0, 1, 0, new String[][]{
			{"D"},
			{"C"},
	}, 0);

	/** Power Cabinet, a column: an HV capacitor on control engineering. Read from the dev world, x211, y4..5, z444 (06/11/2026). */
	public static final ComputerStructure POWER_CABINET = new ComputerStructure(8, "power_cabinet", "PowerCabinet", 0, 0, 0, new String[][]{
			{"C"},
			{"P"},
	}, 0);

	/** Data Cabinet, a column: a data cable coil on control engineering, the teletype upside down. Read from the dev world, x211, y4..5, z447 (06/11/2026). */
	public static final ComputerStructure DATA_CABINET = new ComputerStructure(9, "data_cabinet", "DataCabinet", 0, 0, 0, new String[][]{
			{"C"},
			{"D"},
	}, 0);

	/** In the order they are tried by the hammer: the bigger first, as a smaller one can stand inside its build. */
	public static final ComputerStructure[] ALL = {MAINFRAME, IO_PROCESSOR, MEMORY_CABINET, DISK_UNIT, TAPE_UNIT, OPERATOR_CONSOLE, PRINTER, TELETYPE, POWER_CABINET, DATA_CABINET};

	public static final char TRIGGER = 'C';

	/** Place in {@link #ALL}. */
	public final int index;
	/** The code of the blocks the hammer works on. */
	public final char trigger;
	/** Registry name; also the name of its model, hit boxes and blockstate. */
	public final String name;
	/** The name in IE's multiblock list. */
	public final String uniqueName;
	/** The master part. */
	public final int masterDx, masterDy, masterDz;
	/**
	 * S steel sheetmetal, s steel sheetmetal slab, T steel block, C control engineering (where the hammer works), r radiator, R redstone engineering (for the redstone port, later), D data cable coil, L light engineering, P HV capacitor.
	 * layers[dy][dz].charAt(dx); layer 0 is the bottom, row 0 the north end, column 0 the front.
	 */
	final String[][] layers;
	public final int maxDx, maxDy, maxDz;
	/** The rows (dz) with a conduit entry in the model: on the back of the bottom layer, at the centre of the block. */
	final int[] conduitRows;

	private ComputerStructure(int index, String name, String uniqueName, int masterDx, int masterDy, int masterDz, String[][] layers, int... conduitRows)
	{
		this(index, name, uniqueName, TRIGGER, masterDx, masterDy, masterDz, layers, conduitRows);
	}

	private ComputerStructure(int index, String name, String uniqueName, char trigger, int masterDx, int masterDy, int masterDz, String[][] layers, int... conduitRows)
	{
		this.conduitRows = conduitRows;
		this.index = index;
		this.trigger = trigger;
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

	/** Whether that part has a conduit entry on its back. */
	public boolean hasConduitEntry(int dx, int dy, int dz)
	{
		if(dx != maxDx || dy != 0)
			return false;
		for(int row : conduitRows)
			if(row == dz)
				return true;
		return false;
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
