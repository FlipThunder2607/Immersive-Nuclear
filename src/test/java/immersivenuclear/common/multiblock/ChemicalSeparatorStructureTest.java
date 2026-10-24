package immersivenuclear.common.multiblock;

import static immersivenuclear.common.multiblock.ChemicalSeparatorStructure.*;

/** Run by the chemicalSeparatorStructureTest Gradle task (part of check). */
public class ChemicalSeparatorStructureTest
{
	public static void main(String[] args)
	{
		// Counts read from the dev world build (x200..204, y4..7, z425..431)
		expect(count('s'), 29, "steel scaffolding");
		expect(count('l'), 5, "light engineering");
		expect(count('H'), 2, "heavy engineering");
		expect(count('R'), 1, "redstone engineering");
		expect(count('G'), 2, "generators");
		expect(count('S'), 4, "steel sheetmetal");
		expect(count('b'), 10, "steel sheetmetal slabs");
		expect(count('B'), 38, "metal barrels");
		expect(count('P'), 6, "fluid pump halves");
		expect(count('p'), 21, "fluid pipes");
		expect(totalParts(), 118, "total parts");

		expect(get(0, 0, 0), TRIGGER, "trigger at origin");
		expect(get(MASTER_DX, MASTER_DY, MASTER_DZ), 'p', "master is the middle pipe");
		// Ports, as on the model
		expect(get(3, -1, -3), 'l', "stripping solution input");
		expect(get(4, -1, -3), 'l', "crude solution input");
		expect(get(4, -1, 0), 'l', "solvent input");
		expect(get(3, -1, 3), 'l', "purified solution output");
		expect(get(4, -1, 3), 'l', "effluent output");
		expect(get(4, 0, 0), 'H', "energy, lower heavy engineering");
		expect(get(4, 1, 0), 'H', "energy, upper heavy engineering");
		expect(portAt(3, -1, -3), PORT_STRIPPING, "stripping solution port");
		expect(portFace(3, -1, -3)[1], -1, "stripping solution on the north face");
		expect(portAt(4, -1, -3), PORT_CRUDE, "crude solution port");
		expect(portFace(4, -1, -3)[1], -1, "crude solution on the north face");
		expect(portAt(4, -1, 0), PORT_SOLVENT, "solvent port");
		expect(portFace(4, -1, 0)[0], 1, "solvent on the east face");
		expect(portAt(3, -1, 3), PORT_PURIFIED, "purified solution port");
		expect(portFace(3, -1, 3)[1], 1, "purified solution on the south face");
		expect(portAt(4, -1, 3), PORT_EFFLUENT, "effluent port");
		expect(portFace(4, -1, 3)[1], 1, "effluent on the south face");
		expect(portAt(4, 0, 0), PORT_ENERGY, "energy port");
		expect(portAt(0, 0, 0), PORT_REDSTONE, "redstone port is the trigger");
		expect(portAt(MASTER_DX, MASTER_DY, MASTER_DZ), PORT_NONE, "master is not a port");
		for(int[] p : PORTS)
		{
			if(get(p[0], p[1], p[2]) == '.')
				throw new AssertionError("port on air at " + p[0] + "," + p[1] + "," + p[2]);
			// A port face must open to the outside of the structure
			if((p[4] != 0 || p[5] != 0) && isPart(p[0] + p[4], p[1], p[2] + p[5]))
				throw new AssertionError("port face blocked at " + p[0] + "," + p[1] + "," + p[2]);
		}
		// The hammer face (west of the trigger) must be open, otherwise the build cannot be triggered from there
		expect(get(-1, 0, 0), '.', "air west of trigger");

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

		System.out.println("ChemicalSeparatorStructureTest OK");
	}

	static void expect(int actual, int expected, String what)
	{
		if(actual != expected)
			throw new AssertionError(what + ": expected " + expected + ", got " + actual);
	}
}
