package immersivenuclear.common.multiblock;

import static immersivenuclear.common.multiblock.DeconversionUnitStructure.*;

/** Run by the deconversionUnitStructureTest Gradle task (part of check). */
public class DeconversionUnitStructureTest
{
	public static void main(String[] args)
	{
		// Counts read from the dev world build (x200..205, y4..9, z531..539, 13/11/2026)
		expect(count('S'), 36, "steel sheetmetal");
		expect(count('I'), 21, "iron sheetmetal");
		expect(count('s'), 19, "steel scaffolding");
		expect(count('p'), 14, "fluid pipes");
		expect(count('L'), 9, "LV coils");
		expect(count('T'), 8, "steel blocks");
		expect(count('h'), 6, "furnace heaters");
		expect(count('l'), 6, "light engineering");
		expect(count('P'), 6, "lead sheetmetal");
		expect(count('q'), 6, "lead sheetmetal slabs");
		expect(count('H'), 5, "heavy engineering");
		expect(count('X'), 4, "radioactive handling");
		expect(count('f'), 4, "steel fences");
		expect(count('c'), 4, "copper sheetmetal slabs");
		expect(count('i'), 3, "iron sheetmetal slabs");
		expect(count('G'), 2, "generators");
		expect(count('b'), 2, "steel sheetmetal slabs");
		expect(count('t'), 2, "steel block slabs");
		expect(count('R'), 1, "redstone engineering");
		expect(totalParts(), 158, "total parts");

		expect(get(0, 0, 0), TRIGGER, "trigger at origin");
		expect(get(MASTER_DX, MASTER_DY, MASTER_DZ), 'L', "master is a furnace coil");
		expect(portAt(-4, -1, -4), PORT_UF6, "depleted UF6 port");
		expect(get(-4, -1, -4), 'X', "UF6 on radioactive handling");
		expect(portFace(-4, -1, -4)[1], -1, "UF6 on the north face");
		expect(portAt(-1, -1, -4), PORT_STEAM, "steam port");
		expect(get(-1, -1, -4), 'X', "steam on radioactive handling");
		expect(portAt(-4, -1, 4), PORT_HF, "hydrofluoric acid port");
		expect(get(-4, -1, 4), 'X', "HF on radioactive handling");
		expect(portFace(-4, -1, 4)[1], 1, "HF on the south face");
		expect(portAt(0, -1, 3), PORT_OXIDE, "oxide port");
		expect(get(0, -1, 3), 'X', "oxide on radioactive handling");
		expect(portFace(0, -1, 3)[0], 1, "oxide on the east (back) face");
		expect(portAt(0, 2, 1), PORT_ENERGY, "energy port");
		expect(get(0, 2, 1), 'H', "energy on the cabinet's top heavy engineering");
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
		System.out.println("DeconversionUnitStructureTest OK");
	}

	static void expect(int actual, int expected, String what)
	{
		if(actual != expected)
			throw new AssertionError(what + ": expected " + expected + ", got " + actual);
	}
}
