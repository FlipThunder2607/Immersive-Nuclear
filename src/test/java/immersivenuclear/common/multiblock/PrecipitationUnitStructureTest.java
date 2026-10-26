package immersivenuclear.common.multiblock;

import static immersivenuclear.common.multiblock.PrecipitationUnitStructure.*;

/** Run by the precipitationUnitStructureTest Gradle task (part of check). */
public class PrecipitationUnitStructureTest
{
	public static void main(String[] args)
	{
		// Counts read from the dev world build (x200..204, y4..8, z440..446, rebuilt 26/10)
		expect(count('s'), 28, "steel scaffolding");
		expect(count('f'), 10, "steel fences");
		expect(count('l'), 9, "light engineering");
		expect(count('H'), 6, "heavy engineering");
		expect(count('R'), 1, "redstone engineering");
		expect(count('G'), 3, "generators");
		expect(count('r'), 2, "radiators");
		expect(count('B'), 15, "metal barrels");
		expect(count('p'), 2, "fluid pipes");
		expect(count('S'), 6, "steel sheetmetal");
		expect(count('I'), 5, "iron sheetmetal");
		expect(count('a') + count('A') + count('D'), 2, "iron sheetmetal slabs");
		expect(count('b'), 4, "steel sheetmetal slabs");
		expect(totalParts(), 93, "total parts");

		expect(get(0, 0, 0), TRIGGER, "trigger at origin");
		expect(get(MASTER_DX, MASTER_DY, MASTER_DZ), 'H', "master is the tank's core");
		expect(portAt(2, -1, -6), PORT_SOLUTION, "solution port");
		expect(portFace(2, -1, -6)[1], -1, "solution on the north face (left)");
		expect(portAt(4, -1, -5), PORT_AGENT, "agent port");
		expect(portFace(4, -1, -5)[0], 1, "agent on the east face (back)");
		expect(portAt(0, -1, -2), PORT_EFFLUENT, "effluent port");
		expect(portFace(0, -1, -2)[0], -1, "effluent on the west face");
		expect(portAt(2, -1, 0), PORT_CONCENTRATE, "concentrate port");
		expect(portFace(2, -1, 0)[1], 1, "concentrate on the south face");
		expect(portAt(4, 0, 0), PORT_ENERGY, "energy port");
		expect(get(4, 0, 0), 'H', "energy on heavy engineering");
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

		System.out.println("PrecipitationUnitStructureTest OK");
	}

	static void expect(int actual, int expected, String what)
	{
		if(actual != expected)
			throw new AssertionError(what + ": expected " + expected + ", got " + actual);
	}
}
