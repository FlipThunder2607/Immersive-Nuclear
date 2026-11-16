package immersivenuclear.common;

import blusunrize.immersiveengineering.api.crafting.BlueprintCraftingRecipe;
import blusunrize.immersiveengineering.api.crafting.MetalPressRecipe;
import immersivenuclear.common.item.ItemGraded;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.RegistryEvent;

/**
 * The fuel line's items (15/11/2026). The chain, decided with the user: enriched hexafluoride -> Reconversion Unit -> enriched
 * UO2 powder -> Pellet Press -> green pellets -> Sintering Furnace -> sintered pellets -> Assembly Unit: pellets + Zircaloy tube
 * -> fuel rod; 19 rods + common zirconium plates -> fuel element (the cylindrical bundle the reactor takes). Powder, pellets,
 * rods and elements carry the grade (ItemGraded). The Zircaloy tube comes from IE's Metal Press with the tube mold; the
 * hafnium control rod is a reactor part (made by the Reactor Parts Assembler).
 * The line 3 machines are still designs: their recipes come with them; here, the items and IE's part (mold, Metal Press).
 * Amounts are provisional (playable now, balancing later).
 */
public final class INFuel
{
	public static final String[] GRADES = {"3%", "5%", "20%", "90%"};
	public static final int PRESS_ENERGY = 2400, TUBES_PER_INGOT = 2;

	public static Item itemEnrichedUraniumDioxide, itemGreenPellet, itemSinteredPellet, itemFuelRod, itemFuelElement;
	public static Item itemZircaloyTube, itemMoldTube, itemHafniumControlRod;

	private INFuel()
	{
	}

	static Item graded(RegistryEvent.Register<Item> event, String name, boolean iconByGrade)
	{
		Item i = new ItemGraded(name, iconByGrade);
		event.getRegistry().register(i);
		return i;
	}

	/** After the process materials, as their own group (powder, pellets, tube, rod, element, mold, control rod). */
	static void items(RegistryEvent.Register<Item> event)
	{
		itemEnrichedUraniumDioxide = graded(event, "enriched_uranium_dioxide", true);
		itemGreenPellet = graded(event, "green_fuel_pellet", false);
		itemSinteredPellet = graded(event, "sintered_fuel_pellet", false);
		itemZircaloyTube = INContent.item(event, "zircaloy_tube");
		itemFuelRod = graded(event, "fuel_rod", false);
		itemFuelElement = graded(event, "fuel_element", false);
		itemMoldTube = INContent.item(event, "mold_tube").setMaxStackSize(1);
		itemHafniumControlRod = INContent.item(event, "hafnium_control_rod");
	}

	public static Item[] items()
	{
		return new Item[]{itemEnrichedUraniumDioxide, itemGreenPellet, itemSinteredPellet, itemZircaloyTube, itemFuelRod, itemFuelElement,
				itemMoldTube, itemHafniumControlRod};
	}

	static void recipes()
	{
		// the tube mold, made like IE's own molds on the Engineer's Workbench (the "molds" blueprint): steel plates and the wire cutters
		BlueprintCraftingRecipe.addRecipe("molds", new ItemStack(itemMoldTube), "plateSteel", "plateSteel", "plateSteel", "plateSteel",
				"plateSteel", new ItemStack(blusunrize.immersiveengineering.common.IEContent.itemTool, 1, 1));
		// IE's Metal Press: a Zircaloy ingot drawn into the cladding tube
		MetalPressRecipe.addRecipe(new ItemStack(itemZircaloyTube, TUBES_PER_INGOT), "ingotZircaloy", new ItemStack(itemMoldTube), PRESS_ENERGY);
	}
}
