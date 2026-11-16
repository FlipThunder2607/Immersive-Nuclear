package immersivenuclear.common;

import blusunrize.immersiveengineering.api.crafting.ArcFurnaceRecipe;
import blusunrize.immersiveengineering.api.crafting.BlueprintCraftingRecipe;
import blusunrize.immersiveengineering.api.crafting.IngredientStack;
import blusunrize.immersiveengineering.api.crafting.MetalPressRecipe;
import immersivenuclear.common.item.ItemComponentTape;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.oredict.OreDictionary;

/**
 * The electronics and the Modular Assembly Station's items (15/11/2026, planned with the user):
 * <ul>
 * <li>silicon: quartz dissolved in hydrofluoric acid (Chemical Processor), the silica precipitated (Precipitation Unit), reduced
 * with coke in IE's Arc Furnace; IE's Metal Press with the wafer mold cuts it into wafers. Ore names shared with Immersive
 * Intelligence (itemSilicon, bouleSilicon, plateSilicon).</li>
 * <li>ferrite: iron and nickel dust in the Arc Furnace; its cores go into the inductors and the core memory.</li>
 * <li>the electronic components (made on the IN Workbench) and their component tapes (128 of one component on a reel, loaded
 * on the IN Workbench, used up by the station, the empty tape comes back).</li>
 * <li>the station's nine modules (made on the IN Workbench, later automated by the station), the four jigs (station only: the
 * line it works for and the modules it takes) and the sepia blueprints (the family of items one can make: the IN Workbench takes
 * only the blueprint, the station the jig and the blueprint).</li>
 * </ul>
 * Amounts are provisional (playable now, balancing later). The IN Workbench and the station come next: until then the
 * components, tapes, modules and jigs have no recipe.
 */
public final class INAssembly
{
	public static final String[] COMPONENTS = {"resistor", "capacitor", "ferrite_core", "inductor", "diode", "transistor", "integrated_circuit",
			"crystal_oscillator", "relay", "nixie_tube"};
	public static final String[] MODULES = {"arm", "welder", "press", "machining", "feeder", "pick_and_place", "oven", "optical_inspection",
			"nondestructive_inspection"};
	public static final String[] JIGS = {"fuel", "reactor", "electronics", "turbine"};
	/** The blueprint families; the first five work on the IN Workbench, all in the station (with its jig). */
	public static final String[] BLUEPRINTS = {"components", "component_tapes", "computer_modules", "station_modules", "jigs", "media",
			"panel_commands", "panel_indicators", "panel_text", "fuel_assembly"};

	/** The first BENCH_BLUEPRINTS blueprints work on the IN Workbench too; the rest only in the station. */
	public static final int BENCH_BLUEPRINTS = 5;
	public static final int SILICA_PER_SILICON = 6, WAFERS_PER_SILICON = 8, FERRITE_PER_BATCH = 3;
	public static final int ARC_TICKS = 400, ARC_ENERGY = 512, PRESS_ENERGY = 2400;

	public static net.minecraft.block.Block blockINWorkbench;
	public static Item itemINWorkbench;
	public static Item itemSilica, itemSilicon, itemSiliconWafer, itemMoldWafer, itemFerriteIngot, itemEmptyReel;
	public static ItemComponentTape itemComponentTape;
	public static Item[] itemComponents, itemModules, itemJigs, itemBlueprints;

	private INAssembly()
	{
	}

	static Item[] each(RegistryEvent.Register<Item> event, String prefix, String[] ids, int stack)
	{
		Item[] out = new Item[ids.length];
		for(int i = 0; i < ids.length; i++)
			out[i] = INContent.item(event, prefix + ids[i]).setMaxStackSize(stack);
		return out;
	}

	/** The IN Workbench's block (its two halves are one block type). */
	static void blocks(RegistryEvent.Register<net.minecraft.block.Block> event)
	{
		blockINWorkbench = new immersivenuclear.common.block.BlockINWorkbench();
		event.getRegistry().register(blockINWorkbench);
		net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(immersivenuclear.common.block.TileINWorkbench.class,
				new net.minecraft.util.ResourceLocation(immersivenuclear.ImmersiveNuclear.MODID, "in_workbench"));
	}

	/*
	 * Registered in their kinds' places, as the rest of the mod (JEI's list and the creative tab follow the registration, the
	 * user, 14/11 and 15/11/2026): the bench after the engineering blocks, ferrite with the ingots, the wafer with the plates,
	 * silicon and silica with the crystals and dusts, the wafer mold and the blueprints as their own group, the modules and
	 * jigs with the machine parts, the components and tapes opening the computers.
	 */
	static void bench(RegistryEvent.Register<Item> event)
	{
		itemINWorkbench = new net.minecraft.item.ItemBlock(blockINWorkbench).setRegistryName(blockINWorkbench.getRegistryName());
		event.getRegistry().register(itemINWorkbench);
	}

	static void ingots(RegistryEvent.Register<Item> event)
	{
		itemFerriteIngot = INContent.item(event, "ferrite_ingot");
	}

	static void plates(RegistryEvent.Register<Item> event)
	{
		itemSiliconWafer = INContent.item(event, "silicon_wafer");
	}

	static void dusts(RegistryEvent.Register<Item> event)
	{
		itemSilicon = INContent.item(event, "silicon");
		itemSilica = INContent.item(event, "precipitated_silica");
	}

	static void blueprints(RegistryEvent.Register<Item> event)
	{
		itemMoldWafer = INContent.item(event, "mold_wafer").setMaxStackSize(1);
		itemBlueprints = new Item[BLUEPRINTS.length];
		for(int i = 0; i < BLUEPRINTS.length; i++)
		{
			itemBlueprints[i] = new immersivenuclear.common.item.ItemINBlueprint(BLUEPRINTS[i], i < BENCH_BLUEPRINTS);
			event.getRegistry().register(itemBlueprints[i]);
		}
	}

	static void parts(RegistryEvent.Register<Item> event)
	{
		itemModules = each(event, "assembly_module_", MODULES, 16);
		itemJigs = each(event, "assembly_jig_", JIGS, 1);
	}

	static void electronics(RegistryEvent.Register<Item> event)
	{
		itemComponents = each(event, "", COMPONENTS, 64);
		itemEmptyReel = INContent.item(event, "empty_reel");
		itemComponentTape = new ItemComponentTape();
		event.getRegistry().register(itemComponentTape);
	}

	/** The creative tab's groups (INContent.TAB). */
	public static Item[] blueprintGroup()
	{
		java.util.List<Item> l = new java.util.ArrayList<>();
		l.add(itemMoldWafer);
		l.addAll(java.util.Arrays.asList(itemBlueprints));
		return l.toArray(new Item[0]);
	}

	public static Item[] partsGroup()
	{
		java.util.List<Item> l = new java.util.ArrayList<>(java.util.Arrays.asList(itemModules));
		l.addAll(java.util.Arrays.asList(itemJigs));
		return l.toArray(new Item[0]);
	}

	public static Item[] electronicsGroup()
	{
		java.util.List<Item> l = new java.util.ArrayList<>(java.util.Arrays.asList(itemComponents));
		l.add(itemEmptyReel);
		l.add(itemComponentTape);
		return l.toArray(new Item[0]);
	}

	public static Item[] items()
	{
		java.util.List<Item> l = new java.util.ArrayList<>(java.util.Arrays.asList(itemINWorkbench, itemSilica, itemSilicon, itemSiliconWafer, itemMoldWafer, itemFerriteIngot));
		l.addAll(java.util.Arrays.asList(itemComponents));
		l.add(itemEmptyReel);
		l.add(itemComponentTape);
		l.addAll(java.util.Arrays.asList(itemModules));
		l.addAll(java.util.Arrays.asList(itemJigs));
		l.addAll(java.util.Arrays.asList(itemBlueprints));
		return l.toArray(new Item[0]);
	}

	/** The items with a JEI page of their own (texts in the lang files). */
	public static Item[] infoItems()
	{
		java.util.List<Item> l = new java.util.ArrayList<>(java.util.Arrays.asList(itemINWorkbench, itemSilica, itemSilicon, itemSiliconWafer, itemMoldWafer,
				itemFerriteIngot, itemComponentTape));
		l.addAll(java.util.Arrays.asList(itemModules));
		l.addAll(java.util.Arrays.asList(itemJigs));
		l.addAll(java.util.Arrays.asList(itemBlueprints));
		return l.toArray(new Item[0]);
	}

	static void oreDict()
	{
		OreDictionary.registerOre("dustSilica", itemSilica);
		OreDictionary.registerOre("itemSilicon", itemSilicon);
		OreDictionary.registerOre("bouleSilicon", itemSilicon);
		OreDictionary.registerOre("plateSilicon", itemSiliconWafer);
		OreDictionary.registerOre("ingotFerrite", itemFerriteIngot);
	}

	static void recipes()
	{
		oreDict();
		INWorkbenchRecipe.register();
		// silicon: the precipitated silica reduced with coke (the carbon takes the oxygen away)
		ArcFurnaceRecipe.addRecipe(new ItemStack(itemSilicon), new IngredientStack("dustSilica", SILICA_PER_SILICON), ItemStack.EMPTY, ARC_TICKS,
				ARC_ENERGY, "dustCoke");
		// ferrite: iron and nickel oxides fired together
		ArcFurnaceRecipe.addRecipe(new ItemStack(itemFerriteIngot, FERRITE_PER_BATCH), new IngredientStack("dustIron", 2), ItemStack.EMPTY, ARC_TICKS,
				ARC_ENERGY, "dustNickel");
		// the wafer mold, like IE's molds on the Engineer's Workbench; IE's Metal Press slices the silicon into wafers
		BlueprintCraftingRecipe.addRecipe("molds", new ItemStack(itemMoldWafer), "plateSteel", "plateSteel", "plateSteel", "plateSteel",
				"plateSteel", "dustRedstone");
		MetalPressRecipe.addRecipe(new ItemStack(itemSiliconWafer, WAFERS_PER_SILICON), "itemSilicon", new ItemStack(itemMoldWafer), PRESS_ENERGY);
	}
}
