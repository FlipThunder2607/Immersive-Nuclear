package immersivenuclear.common.multiblock;

import static immersivenuclear.common.multiblock.HFKilnStructure.*;

/** Run by the hfKilnStructureTest Gradle task (part of check). */
public class HFKilnStructureTest
{
	public static void main(String[] args)
	{
		// Counts read from the dev world build (x200..204, y4..8, z469..477, 14/11/2026, the octagonal model's build)
		expect(count('s'), 20, "steel scaffolding");
		expect(count('S'), 26, "steel sheetmetal");
		expect(count('C'), 27, "copper sheetmetal");
		expect(count('a'), 3, "aluminium sheetmetal");
		expect(count('b'), 2, "steel sheetmetal slabs");
		expect(count('A'), 3, "aluminium sheetmetal slabs");
		expect(count('L'), 15, "LV coils");
		expect(count('h'), 9, "furnace heaters");
		expect(count('f'), 6, "steel fences");
		expect(count('p'), 4, "fluid pipes");
		expect(count('l'), 10, "light engineering");
		expect(count('H'), 17, "heavy engineering");
		expect(count('Q'), 2, "chemical engineering");
		expect(count('R'), 1, "redstone engineering");
		expect(count('G'), 1, "generators");
		expect(count('T'), 7, "steel blocks");
		expect(count('B'), 0, "metal barrels");
		expect(count('r'), 0, "radiators");
		expect(totalParts(), 153, "total parts");

		expect(get(0, 0, 0), TRIGGER, "trigger at origin");
		expect(get(MASTER_DX, MASTER_DY, MASTER_DZ), 'C', "master is a drum sheet");
		expect(portAt(0, -1, -2), PORT_FLUORITE, "fluorite port");
		expect(get(0, -1, -2), 'l', "fluorite in at the elevator's foot");
		expect(portFace(0, -1, -2)[0], 1, "fluorite on the east face (back)");
		expect(portAt(-3, -1, -2), PORT_ACID, "sulfuric acid port");
		expect(get(-3, -1, -2), 'Q', "acid in through the chemical engineering block");
		expect(portFace(-3, -1, -2)[1], -1, "acid on the north face");
		expect(portAt(-3, -1, 6), PORT_GYPSUM, "gypsum port");
		expect(portFace(-3, -1, 6)[1], 1, "gypsum on the south face");
		expect(portAt(-1, -1, 6), PORT_HF, "hydrofluoric acid port");
		expect(get(-1, -1, 6), 'Q', "hydrofluoric acid out of the chemical engineering block");
		expect(portFace(-1, -1, 6)[1], 1, "hydrofluoric acid on the south face");
		expect(portAt(0, 2, 1), PORT_ENERGY, "energy port");
		expect(get(0, 2, 1), 'H', "energy on the cabinet's top heavy engineering");
		expect(get(0, 3, 1), '.', "open above the energy port");
		expect(portAt(0, 0, 0), PORT_REDSTONE, "redstone port is the trigger");
		expect(portAt(MASTER_DX, MASTER_DY, MASTER_DZ), PORT_NONE, "master is not a port");
		// The sounds play from these parts (TileHFKiln.DRIVE_PART, HEATER_PART, DRUM_PART)
		expect(get(-1, 0, -1), 'T', "drive sound from the gearbox");
		expect(get(-3, 0, 2), 'h', "heater sound from a furnace heater");
		expect(get(-2, 1, 1), 'L', "drum sound from a heating coil on the drum");
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
