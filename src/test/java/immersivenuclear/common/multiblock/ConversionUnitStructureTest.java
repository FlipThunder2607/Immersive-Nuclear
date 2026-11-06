package immersivenuclear.common.multiblock;

import static immersivenuclear.common.multiblock.ConversionUnitStructure.*;

/** Run by the conversionUnitStructureTest Gradle task (part of check). */
public class ConversionUnitStructureTest
{
	public static void main(String[] args)
	{
		// Counts read from the user's build in the dev world (x200..206, y4..14, z485..495, saved 03/11 21:44)
		expect(count('S'), 109, "steel sheetmetal");
		expect(count('s'), 50, "steel scaffolding");
		expect(count('f'), 26, "steel fences");
		expect(count('p'), 34, "fluid pipes");
		expect(count('B'), 24, "metal barrels");
		expect(count('Q'), 17, "chemical engineering");
		expect(count('r'), 12, "radiators");
		expect(count('P'), 3, "lead sheetmetal");
		expect(count('M'), 8, "covered steel ladders");
		expect(count('T'), 16, "steel blocks");
		expect(count('X'), 12, "radioactive handling");
		expect(count('b'), 25, "steel sheetmetal slabs");
		expect(count('l'), 5, "light engineering");
		expect(count('C'), 18, "copper sheetmetal");
		expect(count('H'), 3, "heavy engineering");
		expect(count('R'), 1, "redstone engineering");
		expect(totalParts(), 363, "total parts");

		expect(get(0, 0, 0), TRIGGER, "trigger at origin (the redstone engineering block on the deck's east edge)");
		if(!isPart(MASTER_DX, MASTER_DY, MASTER_DZ))
			throw new AssertionError("master is not part of the structure");
		expect(portAt(-1, 0, -7), PORT_HYDROGEN, "hydrogen port");
		expect(portAt(-5, 0, -7), PORT_HF, "acid port");
		expect(portFace(-1, 0, -7)[1], -1, "inputs on the north face");
		expect(portAt(-5, 0, 3), PORT_UF6, "hexafluoride port");
		expect(portAt(-1, 0, 3), PORT_EFFLUENT, "effluent port");
		expect(portFace(-5, 0, 3)[1], 1, "outputs on the south face");
		expect(get(-1, 0, -7), 'l', "fluid ports on light engineering");
		expect(portAt(-5, 9, -6), PORT_ITEM, "item hatch on the roof");
		expect(get(-5, 9, -6), 'l', "item hatch on light engineering");
		expect(get(0, 0, -5), 'H', "energy on heavy engineering");
		expect(portAt(0, 0, -3), PORT_ENERGY, "energy port");
		expect(portAt(0, 0, 0), PORT_REDSTONE, "redstone port is the trigger");
		expect(portAt(MASTER_DX, MASTER_DY, MASTER_DZ), PORT_NONE, "master is not a port");
		for(int[] p : PORTS)
		{
			if(get(p[0], p[1], p[2]) == '.')
				throw new AssertionError("port on air at " + p[0] + "," + p[1] + "," + p[2]);
			if((p[4] != 0 || p[5] != 0) && isPart(p[0] + p[4], p[1], p[2] + p[5]))
				throw new AssertionError("port face blocked at " + p[0] + "," + p[1] + "," + p[2]);
		}
		for(int turns = 0; turns < 4; turns++)
			for(boolean mirror : new boolean[]{false, true})
				for(int dx = MIN_DX; dx <= MAX_DX; dx++)
					for(int dz = MIN_DZ; dz <= MAX_DZ; dz++)
					{
						int[] w = rotate(dx, dz, turns, mirror);
						int[] back = unrotate(w[0], w[1], turns, mirror);
						if(back[0] != dx || back[1] != dz)
							throw new AssertionError("rotate/unrotate mismatch turns=" + turns + " mirror=" + mirror + " dx=" + dx + " dz=" + dz);
					}
		System.out.println("ConversionUnitStructureTest OK");
	}

	static void expect(int actual, int expected, String what)
	{
		if(actual != expected)
			throw new AssertionError(what + ": expected " + expected + ", got " + actual);
	}
}
