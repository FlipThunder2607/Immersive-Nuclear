package immersivenuclear.common.multiblock;

import static immersivenuclear.common.multiblock.ElectrolyzerStructure.*;

/** Run by the electrolyzerStructureTest Gradle task (part of check). */
public class ElectrolyzerStructureTest
{
	public static void main(String[] args)
	{
		// Counts read from the dev world build (x200..204, y4..8, z476..482, 01/11)
		expect(count('S'), 25, "steel sheetmetal");
		expect(count('B'), 20, "metal barrels");
		expect(count('c'), 12, "HV capacitors");
		expect(count('b'), 11, "steel sheetmetal slabs");
		expect(count('s'), 10, "steel scaffolding");
		expect(count('G'), 8, "generators");
		expect(count('H'), 4, "heavy engineering");
		expect(count('f'), 4, "steel fences");
		expect(count('p'), 4, "fluid pipes");
		expect(count('l'), 3, "light engineering");
		expect(count('r'), 2, "radiators");
		expect(count('v'), 2, "HV relays");
		expect(count('R'), 1, "redstone engineering");
		expect(totalParts(), 106, "total parts");

		expect(get(0, 0, 0), TRIGGER, "trigger at origin");
		expect(get(MASTER_DX, MASTER_DY, MASTER_DZ), 'c', "master is a cell stack");
		expect(portAt(2, -1, 0), PORT_WATER, "water port");
		expect(get(2, -1, 0), 'l', "water on light engineering");
		expect(portFace(2, -1, 0)[1], -1, "water on the north face");
		expect(portAt(4, -1, 0), PORT_OXYGEN, "oxygen port");
		expect(portFace(4, -1, 0)[0], 1, "oxygen on the back face");
		expect(portAt(4, -1, 6), PORT_HYDROGEN, "hydrogen port");
		expect(portFace(4, -1, 6)[0], 1, "hydrogen on the back face");
		expect(get(0, 0, 6), 'H', "energy on heavy engineering");
		expect(portAt(0, -1, 5), PORT_ENERGY, "energy port");
		expect(portAt(0, 0, 0), PORT_REDSTONE, "redstone port is the trigger");
		expect(portAt(MASTER_DX, MASTER_DY, MASTER_DZ), PORT_NONE, "master is not a port");
		// The sounds play from these parts (TileElectrolyzer)
		expect(get(1, 0, 5), 'G', "rectifier sound from the rectifier");
		expect(get(4, 0, 3), 'p', "pump sound from the circulation line");
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
		System.out.println("ElectrolyzerStructureTest OK");
	}

	static void expect(int actual, int expected, String what)
	{
		if(actual != expected)
			throw new AssertionError(what + ": expected " + expected + ", got " + actual);
	}
}
