package immersivenuclear.common.multiblock;

import static immersivenuclear.common.multiblock.EnrichmentUnitStructure.*;

/** Run by the enrichmentUnitStructureTest Gradle task (part of check). */
public class EnrichmentUnitStructureTest
{
	public static void main(String[] args)
	{
		expect(totalParts(), 351, "total parts");
		expect(count('B'), 10, "code B");
		expect(count('G'), 21, "code G");
		expect(count('H'), 3, "code H");
		expect(count('Q'), 27, "code Q");
		expect(count('R'), 1, "code R");
		expect(count('S'), 131, "code S");
		expect(count('X'), 24, "code X");
		expect(count('b'), 27, "code b");
		expect(count('f'), 44, "code f");
		expect(count('l'), 13, "code l");
		expect(count('p'), 33, "code p");
		expect(count('s'), 17, "code s");
		expect(portAt(1, 3, -1), PORT_ENERGY, "energy");
		expect(portAt(1, 3, 0), PORT_ENERGY, "energy");
		expect(portAt(1, 3, 1), PORT_ENERGY, "energy");
		expect(get(0, 0, 0), TRIGGER, "trigger at origin");
		expect(get(MASTER_DX, MASTER_DY, MASTER_DZ), 'Q', "master in the bank's core");
		expect(portAt(1, 0, -4), PORT_FEED, "feed port");
		expect(portAt(1, 0, 4), PORT_PRODUCT, "product port");
		expect(portAt(3, 0, 4), PORT_TAILS, "tails port");
		expect(portAt(1, 3, -1), PORT_ENERGY, "energy on the converters' roof");
		expect(portAt(1, 3, 0), PORT_ENERGY, "energy on the converters' roof");
		expect(portAt(1, 3, 1), PORT_ENERGY, "energy on the converters' roof");
		for(int[] p : PORTS)
		{
			if(get(p[0], p[1], p[2]) == '.')
				throw new AssertionError("port on air at " + p[0] + "," + p[1] + "," + p[2]);
			if((p[4] != 0 || p[5] != 0) && isPart(p[0] + p[4], p[1], p[2] + p[5]))
				throw new AssertionError("port face covered at " + p[0] + "," + p[1] + "," + p[2]);
		}
		System.out.println("EnrichmentUnitStructure OK");
	}

	static void expect(int got, int want, String what)
	{
		if(got != want)
			throw new AssertionError(what + ": got " + got + ", expected " + want);
	}
}
