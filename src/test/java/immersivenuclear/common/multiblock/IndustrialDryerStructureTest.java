package immersivenuclear.common.multiblock;

import static immersivenuclear.common.multiblock.IndustrialDryerStructure.*;

/** Run by the industrialDryerStructureTest Gradle task (part of check). */
public class IndustrialDryerStructureTest
{
	public static void main(String[] args)
	{
		// Counts read from the dev world build (x200..203, y4..6, z449..455, 26/10, second build)
		expect(count('s'), 20, "steel scaffolding");
		expect(count('F'), 3, "furnace heaters");
		expect(count('l'), 4, "light engineering");
		expect(count('H'), 2, "heavy engineering");
		expect(count('R'), 1, "redstone engineering");
		expect(count('G'), 1, "generators");
		expect(count('r'), 1, "radiators");
		expect(count('B'), 4, "metal barrels");
		expect(count('p'), 3, "fluid pipes");
		expect(count('I'), 13, "iron sheetmetal");
		expect(count('a'), 11, "iron sheetmetal slabs");
		expect(totalParts(), 63, "total parts");

		expect(get(0, 0, 0), TRIGGER, "trigger at origin");
		expect(get(MASTER_DX, MASTER_DY, MASTER_DZ), 'a', "master is a roof slab of the chamber");
		expect(portAt(2, 1, 2), PORT_WET, "wet concentrate port");
		expect(portAt(1, 1, 2), PORT_WET, "wet concentrate under the model's hopper");
		expect(get(2, 1, 2), 'I', "wet concentrate through the feed hopper on the chamber's roof");
		expect(portAt(1, -1, 6), PORT_YELLOWCAKE, "yellowcake port");
		expect(portFace(1, -1, 6)[1], 1, "yellowcake on the south face");
		expect(portAt(3, -1, 5), PORT_CONDENSATE, "condensate port");
		expect(portFace(3, -1, 5)[0], 1, "condensate on the east face (back)");
		expect(portAt(3, -1, 0), PORT_ENERGY, "energy port (bottom)");
		expect(portAt(3, 0, 0), PORT_ENERGY, "energy port (top)");
		expect(get(3, 0, 0), 'H', "energy on heavy engineering");
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
		// The wet concentrate goes in from above: nothing on top of its port
		expect(get(2, 2, 2), '.', "open above the feed");

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

		System.out.println("IndustrialDryerStructureTest OK");
	}

	static void expect(int actual, int expected, String what)
	{
		if(actual != expected)
			throw new AssertionError(what + ": expected " + expected + ", got " + actual);
	}
}
