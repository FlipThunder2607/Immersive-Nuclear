package immersivenuclear.common.multiblock;

import static immersivenuclear.common.multiblock.ComputerStructure.*;

/** Run by the computerStructureTest Gradle task (part of check). */
public class ComputerStructureTest
{
	public static void main(String[] args)
	{
		// Counts read from the dev world builds (05/11/2026)
		parts(MAINFRAME, 8, 3, 12, 4, 4, 0);
		expect(MAINFRAME.get(1, 1, 1), 'C', "control engineering core at the back");
		expect(MAINFRAME.get(1, 3, 0), 'r', "radiator on the back");
		parts(IO_PROCESSOR, 6, 1, 4, 2, 2, 0);
		parts(MEMORY_CABINET, 8, 1, 4, 2, 0, 0);
		parts(DISK_UNIT, 0, 1, 2, 2, 0, 2);
		parts(TAPE_UNIT, 6, 1, 2, 2, 0, 4);
		parts(OPERATOR_CONSOLE, 0, 3, 4, 4, 0, 0);
		expect(OPERATOR_CONSOLE.count('s'), 4, "operator_console steel sheetmetal slabs");
		parts(PRINTER, 0, 1, 0, 1, 0, 1);
		expect(PRINTER.count('R'), 1, "printer redstone engineering");
		expect(PRINTER.trigger, 'L', "printer formed on the light engineering");
		parts(TELETYPE, 0, 0, 1, 1, 0, 0);
		parts(POWER_CABINET, 0, 0, 1, 0, 0, 0);
		expect(POWER_CABINET.count('P'), 1, "power_cabinet capacitor");
		parts(DATA_CABINET, 0, 0, 1, 1, 0, 0);
		// The conduit entries of the models, on the back of the bottom layer
		entries(MAINFRAME, 1, 2);
		entries(IO_PROCESSOR, 1);
		entries(MEMORY_CABINET, 1);
		entries(DISK_UNIT, 1);
		entries(TAPE_UNIT, 1);
		entries(OPERATOR_CONSOLE, 0, 3);
		entries(PRINTER, 0, 1);
		entries(TELETYPE, 0);
		entries(POWER_CABINET, 0);
		entries(DATA_CABINET, 0);
		for(int i = 0; i < ALL.length; i++)
		{
			ComputerStructure s = ALL[i];
			expect(s.index, i, s.name + " in its place");
			expect(s.get(s.masterDx, s.masterDy, s.masterDz), s.trigger, s.name + " master on a trigger block");
			if(byName(s.name) != s)
				throw new AssertionError("byName " + s.name);
			if(s.maxDy == 0)
				throw new AssertionError("flat structure " + s.name);
			if(s.maxDx > 0)
			{
				expect(s.count('R'), 1, s.name + " one redstone engineering");
				expect(s.get(1, 0, 0), 'R', s.name + " redstone engineering on the back corner, bottom layer");
			}
		}
		if(ALL[0].count(TRIGGER) < ALL[1].count(TRIGGER))
			throw new AssertionError("the bigger structures go first");
		// The columns stand inside bigger builds (control engineering on or under a data cable coil): they are tried after all of those
		boolean column = false;
		for(ComputerStructure s : ALL)
		{
			boolean c = s.maxDx == 0 && s.maxDz == 0;
			if(column && !c)
				throw new AssertionError(s.name + " tried after a column");
			column = c;
		}
		// The hammer takes the first that fits: no two structures may be the same build
		for(int i = 0; i < ALL.length; i++)
			for(int j = i + 1; j < ALL.length; j++)
				if(java.util.Arrays.deepEquals(ALL[i].layers, ALL[j].layers))
					throw new AssertionError(ALL[i].name + " and " + ALL[j].name + " are the same build");
		for(int turns = 0; turns < 4; turns++)
		{
			int[] w = rotate(1, 3, turns);
			int[] t = unrotate(w[0], w[1], turns);
			if(t[0] != 1 || t[1] != 3)
				throw new AssertionError("rotate and unrotate disagree at turn " + turns);
		}
		System.out.println("Computer structures OK");
	}

	static void entries(ComputerStructure s, int... rows)
	{
		int found = 0;
		for(int dy = 0; dy <= s.maxDy; dy++)
			for(int dz = 0; dz <= s.maxDz; dz++)
				for(int dx = 0; dx <= s.maxDx; dx++)
					if(s.hasConduitEntry(dx, dy, dz))
						found++;
		expect(found, rows.length, s.name + " conduit entries");
		for(int row : rows)
			if(!s.hasConduitEntry(s.maxDx, 0, row))
				throw new AssertionError(s.name + " conduit entry on row " + row);
	}

	static void parts(ComputerStructure s, int sheetmetal, int steel, int control, int coils, int radiators, int light)
	{
		expect(s.count('S'), sheetmetal, s.name + " steel sheetmetal");
		expect(s.count('T'), steel, s.name + " steel blocks");
		expect(s.count('C'), control, s.name + " control engineering");
		expect(s.count('D'), coils, s.name + " data cable coils");
		expect(s.count('r'), radiators, s.name + " radiators");
		expect(s.count('L'), light, s.name + " light engineering");
		expect(s.count('.'), 0, s.name + " no gaps");
	}

	static void expect(int actual, int expected, String what)
	{
		if(actual != expected)
			throw new AssertionError(what + ": expected " + expected + ", got " + actual);
	}
}
