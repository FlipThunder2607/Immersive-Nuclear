package immersivenuclear.common.multiblock;

import static immersivenuclear.common.multiblock.ConversionUnitStructure.*;

/** Run by the conversionUnitStructureTest Gradle task (part of check). */
public class ConversionUnitStructureTest
{
	public static void main(String[] args)
	{
		// Counts read from the user's build in the dev world (x194..200, y4..15, z496..506, saved 07/11 19:47)
		expect(count('S'), 122, "steel sheetmetal");
		expect(count('T'), 63, "steel blocks");
		expect(count('Q'), 47, "chemical engineering");
		expect(count('s'), 39, "steel scaffolding");
		expect(count('f'), 23, "steel fences");
		expect(count('p'), 27, "fluid pipes");
		expect(count('B'), 26, "metal barrels");
		expect(count('C'), 20, "copper sheetmetal");
		expect(count('X'), 17, "radioactive handling");
		expect(count('P'), 12, "lead sheetmetal");
		expect(count('r'), 12, "radiators");
		expect(count('b'), 10, "steel sheetmetal slabs");
		expect(count('I'), 8, "iron sheetmetal");
		expect(count('c'), 6, "copper sheetmetal slabs");
		expect(count('H'), 3, "heavy engineering");
		expect(count('R'), 1, "redstone engineering");
		expect(count('L'), 11, "metal ladder");
		expect(get(-2, 11, -2), 'L', "ladder up to the top layer");
		expect(totalParts(), 447, "total parts");

		expect(get(0, 0, 0), TRIGGER, "trigger at origin (the redstone engineering block on the deck's east edge)");
		expect(get(MASTER_DX, MASTER_DY, MASTER_DZ), 'X', "master in the reactor tower's core");
		expect(portAt(-1, 0, -8), PORT_HYDROGEN, "hydrogen port");
		expect(portAt(-5, 0, -8), PORT_HF, "acid port");
		expect(portFace(-1, 0, -8)[1], -1, "inputs on the north face");
		expect(portAt(-5, 0, 2), PORT_UF6, "hexafluoride port");
		expect(portAt(-1, 0, 2), PORT_EFFLUENT, "effluent port");
		expect(portFace(-5, 0, 2)[1], 1, "outputs on the south face");
		for(int[] fluid : new int[][]{{-1, -8}, {-5, -8}, {-5, 2}, {-1, 2}})
			expect(get(fluid[0], 0, fluid[1]), 'Q', "fluid ports on chemical engineering");
		expect(portAt(-5, 10, -7), PORT_ITEM, "item hatch on top of the reactor tower");
		expect(get(-5, 10, -7), 'X', "item hatch on radioactive handling");
		if(isPart(-5, 11, -7))
			throw new AssertionError("item hatch covered");
		expect(get(0, 0, -2), 'H', "energy on heavy engineering");
		expect(portAt(0, 0, -7), PORT_ASH, "ash port at the back");
		expect(portFace(0, 0, -7)[0], 1, "ash out through the back face");
		expect(portAt(0, 0, -4), PORT_ENERGY, "energy port");
		expect(portAt(0, 0, 0), PORT_REDSTONE, "redstone port is the trigger");
		expect(portAt(MASTER_DX, MASTER_DY, MASTER_DZ), PORT_NONE, "master is not a port");
		// The parts TileConversionUnit plays its sounds from (FLUORINATOR_PART, CELLS_PART, COLD_TRAP_PART, SCRUBBER_PART)
		expect(get(-5, 1, -3), 'X', "fluorinator sound on the second reactor's core");
		expect(get(-1, 1, -5), 'Q', "cells sound on the fluorine cells");
		expect(get(-1, 1, -1), 'r', "cold trap sound on a radiator");
		expect(get(-1, 1, 1), 'Q', "scrubber sound on the scrubber's column");
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
		System.out.println("ConversionUnitStructureTest OK");
	}

	static void expect(int actual, int expected, String what)
	{
		if(actual != expected)
			throw new AssertionError(what + ": expected " + expected + ", got " + actual);
	}
}
