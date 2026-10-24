package immersivenuclear.common.multiblock;

import static immersivenuclear.common.multiblock.FilterPressStructure.*;

/** Run by the filterPressStructureTest Gradle task (part of check). */
public class FilterPressStructureTest
{
	public static void main(String[] args)
	{
		// Counts read from the dev world build (x194..196, y4..6, z425..433, third version)
		expect(count('l'), 8, "light engineering");
		expect(count('s'), 1, "steel scaffolding");
		expect(count('H'), 2, "heavy engineering");
		expect(count('I'), 0, "iron sheetmetal");
		expect(count('a'), 8, "steel scaffolding slabs");
		expect(count('S'), 8, "steel sheetmetal");
		expect(count('b'), 12, "steel sheetmetal slabs");
		expect(count('C'), 5, "conveyor belts");
		expect(count('B'), 2, "metal barrels");
		expect(count('P'), 2, "fluid pump halves");
		expect(count('G'), 2, "generators");
		expect(count('F'), 5, "steel fences");
		expect(count('R'), 1, "redstone engineering");
		expect(totalParts(), 56, "total parts");

		expect(get(0, 0, 0), TRIGGER, "trigger at origin");
		expect(get(MASTER_DX, MASTER_DY, MASTER_DZ), 'F', "master is the middle fence");
		// Ports, as on the model
		expect(get(0, -1, -8), 'l', "slurry input, front of the pump end");
		expect(get(0, -1, 0), 'l', "crude solution output, front of the outlet end");
		expect(get(1, -1, 0), 'l', "residue output, end of the conveyor");
		expect(get(1, -1, -1), 'l', "conveyor feeds the residue port");
		expect(get(2, -1, -3), 'l', "filter input, back");
		expect(get(2, -1, -5), 'l', "spent filter output, back");
		expect(get(2, -1, -4), 'a', "plain slab between the two filter ports");
		expect(get(2, -1, 0), 'H', "energy input, heavy engineering column");
		expect(get(1, 0, -8), 'P', "pump at the slurry end");
		expect(get(1, 1, -8), 'P', "pump top half");
		expect(portAt(0, -1, -8), PORT_SLURRY, "slurry port");
		expect(portFace(0, -1, -8)[0], -1, "slurry on the west face");
		expect(portAt(0, -1, 0), PORT_SOLUTION, "solution port");
		expect(portFace(0, -1, 0)[0], -1, "solution on the west face");
		expect(portAt(1, -1, 0), PORT_RESIDUE, "residue port");
		expect(portFace(1, -1, 0)[1], 1, "residue on the south face");
		expect(portAt(2, -1, -3), PORT_FILTER, "filter port");
		expect(portFace(2, -1, -3)[0], 1, "filters in on the east face");
		expect(portAt(2, -1, -5), PORT_FILTER_OUT, "spent filter port");
		expect(portFace(2, -1, -5)[0], 1, "spent filters out on the east face");
		expect(portAt(2, -1, -4), PORT_NONE, "the old middle port is plain now");
		expect(portAt(2, -1, 0), PORT_ENERGY, "energy, lower heavy engineering");
		expect(portAt(2, 0, 0), PORT_ENERGY, "energy, upper heavy engineering");
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

		// The mirror swaps left and right (along the press, dz) and keeps the front at the front (dx)
		int[] m = rotate(2, -3, 0, true);
		if(m[0] != 2 || m[1] != 3)
			throw new AssertionError("mirror should flip dz only, got " + m[0] + "," + m[1]);

		System.out.println("FilterPressStructureTest OK");
	}

	static void expect(int actual, int expected, String what)
	{
		if(actual != expected)
			throw new AssertionError(what + ": expected " + expected + ", got " + actual);
	}
}
