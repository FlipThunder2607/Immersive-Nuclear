package immersivenuclear.common.multiblock;

import static immersivenuclear.common.multiblock.SulfuricAcidUnitStructure.*;

/** Run by the sulfuricAcidUnitStructureTest Gradle task (part of check). */
public class SulfuricAcidUnitStructureTest
{
	public static void main(String[] args)
	{
		// Counts read from the dev world build (x200..204, y4..12, z458..464, 31/10, third build)
		expect(count('s'), 12, "steel scaffolding");
		expect(count('S'), 18, "steel sheetmetal");
		expect(count('I'), 2, "iron sheetmetal");
		expect(count('b'), 3, "steel sheetmetal slabs");
		expect(count('K'), 11, "blast bricks");
		expect(count('B'), 43, "metal barrels");
		expect(count('p'), 18, "fluid pipes");
		expect(count('l'), 4, "light engineering");
		expect(count('Q'), 5, "chemical engineering");
		expect(count('H'), 2, "heavy engineering");
		expect(count('R'), 1, "redstone engineering");
		expect(count('G'), 1, "generators");
		expect(count('r'), 3, "radiators");
		expect(count('T'), 2, "steel blocks");
		expect(totalParts(), 125, "total parts");

		expect(get(0, 0, 0), TRIGGER, "trigger at origin");
		expect(get(MASTER_DX, MASTER_DY, MASTER_DZ), 'B', "master is a converter barrel");
		expect(portAt(1, 1, 1), PORT_SULFUR, "sulfur port");
		expect(get(1, 1, 1), 'l', "sulfur through the hopper set into the burner's top");
		expect(get(1, 2, 1), '.', "open above the sulfur hopper");
		expect(get(1, 0, 1), 'K', "the hopper sits on the burner's lower course");
		expect(portAt(1, 4, 4), PORT_WATER, "water port");
		expect(portFace(1, 4, 4)[1], -1, "water on the north face of the absorption tower");
		expect(portAt(3, -1, 6), PORT_ACID, "sulfuric acid port");
		expect(portFace(3, -1, 6)[1], 1, "acid on the south face, under the pump tank");
		expect(portAt(4, -1, 1), PORT_STEAM, "steam port");
		expect(portFace(4, -1, 1)[0], 1, "steam on the east face (back)");
		expect(get(4, 0, 1), 'p', "the steam line rises from the steam port");
		expect(portAt(4, -1, 0), PORT_ENERGY, "energy port (bottom)");
		expect(portAt(4, 0, 0), PORT_ENERGY, "energy port (top)");
		expect(get(4, 0, 0), 'H', "energy on heavy engineering");
		expect(portAt(0, 0, 0), PORT_REDSTONE, "redstone port is the trigger");
		expect(portAt(MASTER_DX, MASTER_DY, MASTER_DZ), PORT_NONE, "master is not a port");
		// The sounds play from these parts (TileSulfuricAcidUnit.BURNER_PART, PUMP_PART)
		expect(get(1, 0, 2), 'K', "burner sound from the burner's brick");
		expect(get(3, 0, 5), 'B', "pump sound from the pump tank");
		expect(get(3, 3, 1), 'I', "relief valve's hiss from the steam drum");
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
