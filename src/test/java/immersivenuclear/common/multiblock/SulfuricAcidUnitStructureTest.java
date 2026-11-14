package immersivenuclear.common.multiblock;

import static immersivenuclear.common.multiblock.SulfuricAcidUnitStructure.*;

/** Run by the sulfuricAcidUnitStructureTest Gradle task (part of check). */
public class SulfuricAcidUnitStructureTest
{
	public static void main(String[] args)
	{
		// Counts read from the dev world build (x200..204, y4..11, z457..465, 14/11/2026, the octagonal model's build)
		expect(count('s'), 16, "steel scaffolding");
		expect(count('S'), 25, "steel sheetmetal");
		expect(count('I'), 51, "iron sheetmetal");
		expect(count('a'), 2, "aluminium sheetmetal");
		expect(count('c'), 1, "copper sheetmetal");
		expect(count('b'), 0, "steel sheetmetal slabs");
		expect(count('i'), 3, "iron sheetmetal slabs");
		expect(count('K'), 8, "blast bricks");
		expect(count('B'), 17, "metal barrels");
		expect(count('p'), 27, "fluid pipes");
		expect(count('l'), 3, "light engineering");
		expect(count('Q'), 9, "chemical engineering");
		expect(count('H'), 5, "heavy engineering");
		expect(count('R'), 1, "redstone engineering");
		expect(count('G'), 1, "generators");
		expect(count('r'), 0, "radiators");
		expect(count('f'), 4, "steel fences");
		expect(count('T'), 1, "steel blocks");
		expect(totalParts(), 174, "total parts");

		expect(get(0, 0, 0), TRIGGER, "trigger at origin");
		expect(get(MASTER_DX, MASTER_DY, MASTER_DZ), 'I', "master is the boiler's front block");
		expect(portAt(0, -1, 2), PORT_SULFUR, "sulfur port");
		expect(get(0, -1, 2), 'l', "sulfur in through light engineering on the back");
		expect(portFace(0, -1, 2)[0], 1, "sulfur on the east face (back)");
		expect(portAt(-1, -1, 0), PORT_WATER, "water port");
		expect(get(-1, -1, 0), 'l', "water in through light engineering");
		expect(portFace(-1, -1, 0)[1], -1, "water on the north face");
		expect(portAt(-2, -1, 8), PORT_ACID, "sulfuric acid port");
		expect(get(-2, -1, 8), 'Q', "acid out of the chemical engineering block");
		expect(portFace(-2, -1, 8)[1], 1, "acid on the south face");
		expect(portAt(0, -1, 8), PORT_STEAM, "steam port");
		expect(get(0, -1, 8), 'l', "steam out of light engineering");
		expect(portFace(0, -1, 8)[1], 1, "steam on the south face");
		expect(portAt(0, 2, 1), PORT_ENERGY, "energy port");
		expect(get(0, 2, 1), 'H', "energy on the cabinet's top heavy engineering");
		expect(get(0, 3, 1), '.', "open above the energy port");
		expect(portAt(0, 0, 0), PORT_REDSTONE, "redstone port is the trigger");
		expect(portAt(MASTER_DX, MASTER_DY, MASTER_DZ), PORT_NONE, "master is not a port");
		// The sounds play from these parts (TileSulfuricAcidUnit.BURNER_PART, PUMP_PART, DRUM_PART)
		expect(get(-3, 0, 1), 'K', "burner sound from the burner's brick");
		expect(get(-1, 0, 7), 'B', "pump sound from the pump tank");
		expect(get(-3, 2, 3), 'B', "relief valve's hiss from the steam drum");
		for(int[] p : PORTS)
		{
			if(get(p[0], p[1], p[2]) == '.')
				throw new AssertionError("port on air at " + p[0] + "," + p[1] + "," + p[2]);
			// A port face must open to the outside of the structure
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

		System.out.println("SulfuricAcidUnitStructureTest OK");
	}

	static void expect(int actual, int expected, String what)
	{
		if(actual != expected)
			throw new AssertionError(what + ": expected " + expected + ", got " + actual);
	}
}
