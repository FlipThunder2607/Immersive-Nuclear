package immersivenuclear.common.multiblock;

import static immersivenuclear.common.multiblock.EnrichmentUnit6Structure.*;

/** Run by the enrichmentUnit6StructureTest Gradle task (part of check). */
public class EnrichmentUnit6StructureTest
{
	public static void main(String[] args)
	{
		expect(totalParts(), 963, "total parts");
		expect(count('B'), 10, "code B");
		expect(count('G'), 25, "code G");
		expect(count('H'), 3, "code H");
		expect(count('Q'), 75, "code Q");
		expect(count('R'), 1, "code R");
		expect(count('S'), 381, "code S");
		expect(count('X'), 72, "code X");
		expect(count('b'), 67, "code b");
		expect(count('f'), 160, "code f");
		expect(count('l'), 41, "code l");
		expect(count('p'), 93, "code p");
		expect(count('s'), 35, "code s");
		expect(portAt(1, 3, -1), PORT_ENERGY, "energy");
		expect(portAt(1, 3, 0), PORT_ENERGY, "energy");
		expect(portAt(1, 3, 1), PORT_ENERGY, "energy");
		expect(get(0, 0, 0), TRIGGER, "trigger at origin");
		expect(get(MASTER_DX, MASTER_DY, MASTER_DZ), 'Q', "master in the bank's core");
		expect(portAt(1, 0, -4), PORT_FEED, "feed port");
		expect(portAt(1, 0, 4), PORT_PRODUCT, "product port");
		expect(portAt(3, 0, 4), PORT_TAILS, "tails port");
		for(int[] p : PORTS)
			if(get(p[0], p[1], p[2]) == '.')
				throw new AssertionError("port on air at " + p[0] + "," + p[1] + "," + p[2]);
		System.out.println("EnrichmentUnit6Structure OK");
	}

	static void expect(int got, int want, String what)
	{
		if(got != want)
			throw new AssertionError(what + ": got " + got + ", expected " + want);
	}
}
