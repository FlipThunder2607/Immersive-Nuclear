package immersivenuclear.common.multiblock;

import static immersivenuclear.common.multiblock.MetalExtractorStructure.*;

/** Run by the metalExtractorStructureTest Gradle task (part of check). */
public class MetalExtractorStructureTest
{
	public static void main(String[] args)
	{
		// Counts read from the dev world build (x200..203, y4..7, z434..437)
		expect(count('s'), 6, "steel scaffolding");
		expect(count('l'), 6, "light engineering");
		expect(count('H'), 5, "heavy engineering");
		expect(count('R'), 1, "redstone engineering");
		expect(count('G'), 2, "generators");
		expect(count('S'), 9, "steel sheetmetal");
		expect(count('b'), 9, "steel sheetmetal slabs");
		expect(count('P'), 2, "fluid pump halves");
		expect(count('p'), 1, "fluid pipes");
		expect(count('C'), 3, "HV connectors");
		expect(totalParts(), 44, "total parts");

		expect(get(0, 0, 0), TRIGGER, "trigger at origin");
		expect(get(MASTER_DX, MASTER_DY, MASTER_DZ), 'H', "master is the middle of the beam");
		// Ports, as on the model
		expect(get(1, -1, -3), 'l', "effluent input");
		expect(get(1, -1, 0), 'l', "depleted effluent output");
		expect(get(3, -1, -2), 'l', "deposit output");
		expect(get(3, -1, 0), 'H', "energy, lower heavy engineering");
		expect(get(3, 0, 0), 'H', "energy, upper heavy engineering");
		expect(portAt(1, -1, -3), PORT_EFFLUENT, "effluent port");
		expect(portFace(1, -1, -3)[1], -1, "effluent on the north face");
		expect(portAt(1, -1, 0), PORT_DEPLETED, "depleted effluent port");
		expect(portFace(1, -1, 0)[1], 1, "depleted effluent on the south face");
		expect(portAt(3, -1, -2), PORT_DEPOSIT, "deposit port");
		expect(portFace(3, -1, -2)[0], 1, "deposit on the east face");
		expect(portAt(3, 0, 0), PORT_ENERGY, "energy port");
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

		System.out.println("MetalExtractorStructureTest OK");
	}

	static void expect(int actual, int expected, String what)
	{
		if(actual != expected)
			throw new AssertionError(what + ": expected " + expected + ", got " + actual);
	}
}
