package immersivenuclear.common;

import net.minecraft.item.Item;
import net.minecraftforge.fluids.Fluid;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Content that is in the game but not finished (16/11/2026, release 0.2.0): its tooltip ends with "Work in progress"
 * (HazardTooltips). The computers' blocks with 3D models (devices, conduit, data coil, link connector and relay) and the
 * new line 2 chain's items and fluids that nothing makes or uses yet (uranium dioxide, green salt, dilute hydrofluoric
 * acid, 90% hexafluoride; the fluids through their buckets).
 */
public final class INRelease
{
	private static Set<Item> items;
	private static Set<Fluid> fluids;

	private INRelease()
	{
	}

	public static Set<Item> unfinishedItems()
	{
		if(items == null)
		{
			items = new HashSet<>();
			if(INContent.itemComputerDevices != null)
				items.addAll(Arrays.asList(INContent.itemComputerDevices));
			items.addAll(Arrays.asList(INContent.itemConduit, INContent.itemDataCoil, INContent.itemLinkConnector, INContent.itemLinkRelay,
					INContent.itemUraniumDioxide, INContent.itemGreenSalt));
			// the modules that do nothing yet (the user, 16/11/2026): the station's modules and jigs (the station is not in
			// the game), the operator panel's modules, the computer parts without a use (the electronic and relay modules
			// already work in the Control Engineering block) and the station-only blueprints
			add(INAssembly.itemModules);
			add(INAssembly.itemJigs);
			add(INContent.itemPanelModules);
			if(INContent.itemComputerParts != null)
				for(int i = 2; i < INContent.itemComputerParts.length; i++)
					items.add(INContent.itemComputerParts[i]);
			if(INAssembly.itemBlueprints != null)
				for(int i = INAssembly.BENCH_BLUEPRINTS; i < INAssembly.itemBlueprints.length; i++)
					items.add(INAssembly.itemBlueprints[i]);
			items.remove(null);
		}
		return items;
	}

	private static void add(Item[] list)
	{
		if(list != null)
			items.addAll(Arrays.asList(list));
	}

	public static Set<Fluid> unfinishedFluids()
	{
		if(fluids == null)
		{
			fluids = new HashSet<>();
			fluids.addAll(Arrays.asList(INContent.fluidDiluteHydrofluoricAcid, INContent.fluidUF6VeryHigh));
			fluids.remove(null);
		}
		return fluids;
	}

	public static boolean unfinished(Item item)
	{
		return unfinishedItems().contains(item);
	}

	public static boolean unfinished(Fluid fluid)
	{
		return unfinishedFluids().contains(fluid);
	}
}
