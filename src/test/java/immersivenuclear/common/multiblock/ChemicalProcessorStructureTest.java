package immersivenuclear.common.multiblock;

import static immersivenuclear.common.multiblock.ChemicalProcessorStructure.*;

/** Run by the chemicalProcessorStructureTest Gradle task (part of check). */
public class ChemicalProcessorStructureTest
{
	public static void main(String[] args)
	{
		// Counts read from the dev world build (x193..195, y4..9, z413..415)
		expect(count('B'), 16, "metal barrels");
		expect(count('F'), 10, "steel fences");
		expect(count('l'), 6, "light engineering");
		expect(count('H'), 2, "heavy engineering");
		expect(count('D'), 2, "covered steel ladders");
		expect(count('s'), 1, "steel scaffolding");
		expect(count('P'), 4, "fluid pipes");
		expect(count('b'), 0, "steel scaffolding slabs");
		expect(count('A'), 2, "radiators");
		expect(count('G'), 1, "generator");
		expect(count('R'), 1, "redstone engineering");
		expect(totalParts(), 45, "total parts");

		expect(get(0, 0, 0), TRIGGER, "trigger at origin");
		expect(get(MASTER_DX, MASTER_DY, MASTER_DZ), 'B', "master is a tank barrel");
		// Ports from the user's model.bbmodel
		expect(get(1, -1, 0), 'l', "water input, right (south)");
		expect(get(2, -1, -2), 'l', "uranium powder input, back (east)");
		expect(get(2, -1, -1), 'l', "reagent input, back (east)");
		expect(get(1, -1, -2), 'l', "polpa output, left (north)");
		expect(get(2, -1, 0), 'H', "energy input, heavy engineering column");
		expect(portAt(1, -1, 0), PORT_WATER, "water port");
		expect(portFace(1, -1, 0)[1], 1, "water on the south face");
		expect(portAt(2, -1, -2), PORT_URANIUM, "uranium port");
		expect(portAt(2, -1, -1), PORT_REAGENT, "reagent port");
		expect(portFace(2, -1, -1)[0], 1, "item inputs on the east face");
		expect(portAt(1, -1, -2), PORT_POLPA, "polpa port");
		expect(portFace(1, -1, -2)[1], -1, "polpa on the north face");
		expect(portAt(2, -1, 0), PORT_ENERGY, "energy, lower heavy engineering");
		expect(portAt(2, 0, 0), PORT_ENERGY, "energy, upper heavy engineering");
		expect(portAt(0, 0, 0), PORT_REDSTONE, "redstone port is the trigger");
		expect(portAt(MASTER_DX, MASTER_DY, MASTER_DZ), PORT_NONE, "master is not a port");
		for(int[] p : PORTS)
			if(get(p[0], p[1], p[2]) == '.')
				throw new AssertionError("port on air at " + p[0] + "," + p[1] + "," + p[2]);
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

		// One clockwise turn takes the reference "north" (dz -1) to east (x +1)
		int[] east = rotate(0, -1, 1, false);
		if(east[0] != 1 || east[1] != 0)
			throw new AssertionError("north should rotate to east, got " + east[0] + "," + east[1]);

		System.out.println("ChemicalProcessorStructureTest OK");
	}

	static void expect(int actual, int expected, String what)
	{
		if(actual != expected)
			throw new AssertionError(what + ": expected " + expected + ", got " + actual);
	}
}
