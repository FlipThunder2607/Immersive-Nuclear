package immersivenuclear.common.multiblock;

import static immersivenuclear.common.multiblock.HFKilnStructure.*;

/** Run by the hfKilnStructureTest Gradle task (part of check). */
public class HFKilnStructureTest
{
	public static void main(String[] args)
	{
		// Counts read from the dev world build (x200..204, y4..6, z467..473, 31/10)
		expect(count('s'), 15, "steel scaffolding");
		expect(count('S'), 16, "steel sheetmetal");
		expect(count('C'), 16, "copper sheetmetal");
		expect(count('b'), 2, "steel sheetmetal slabs");
		expect(count('L'), 4, "LV coils");
		expect(count('h'), 4, "furnace heaters");
		expect(count('f'), 5, "steel fences");
		expect(count('B'), 2, "metal barrels");
		expect(count('p'), 4, "fluid pipes");
		expect(count('l'), 4, "light engineering");
		expect(count('H'), 4, "heavy engineering");
		expect(count('Q'), 5, "chemical engineering");
		expect(count('R'), 1, "redstone engineering");
		expect(count('G'), 1, "generators");
		expect(count('T'), 1, "steel blocks");
		expect(count('r'), 2, "radiators");
		expect(totalParts(), 86, "total parts");

		expect(get(0, 0, 0), TRIGGER, "trigger at origin");
		expect(get(MASTER_DX, MASTER_DY, MASTER_DZ), 'C', "master is a drum sheet");
		expect(portAt(1, 1, 0), PORT_FLUORITE, "fluorite port");
		expect(get(1, 1, 0), 'l', "fluorite on top of the feed head");
		expect(portAt(1, -1, 0), PORT_ACID, "sulfuric acid port");
		expect(portFace(1, -1, 0)[1], -1, "acid on the north face");
		expect(portAt(1, -1, 6), PORT_GYPSUM, "gypsum port");
		expect(portFace(1, -1, 6)[1], 1, "gypsum on the south face");
		expect(portAt(3, -1, 6), PORT_HF, "hydrofluoric acid port");
		expect(portFace(3, -1, 6)[1], 1, "hydrofluoric acid on the south face");
		expect(portAt(4, -1, 0), PORT_ENERGY, "energy port (bottom)");
		expect(portAt(4, 0, 0), PORT_ENERGY, "energy port (top)");
		expect(get(4, 0, 0), 'H', "energy on heavy engineering");
		expect(portAt(0, 0, 0), PORT_REDSTONE, "redstone port is the trigger");
		expect(portAt(MASTER_DX, MASTER_DY, MASTER_DZ), PORT_NONE, "master is not a port");
		// The sounds play from these parts (TileHFKiln.DRIVE_PART, HEATER_PART)
		expect(get(3, 0, 2), 'G', "drive sound from the motor");
		expect(get(1, 0, 2), 'L', "heater sound from the heating coil");
		expect(get(2, 0, 3), 'C', "drum sound from the drum (the master)");
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
		System.out.println("HFKilnStructureTest OK");
	}

	static void expect(int actual, int expected, String what)
	{
		if(actual != expected)
			throw new AssertionError(what + ": expected " + expected + ", got " + actual);
	}
}
