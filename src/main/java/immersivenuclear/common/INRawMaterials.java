package immersivenuclear.common;

import java.util.ArrayList;
import java.util.List;

import blusunrize.immersiveengineering.api.crafting.ArcFurnaceRecipe;
import blusunrize.immersiveengineering.api.crafting.CrusherRecipe;
import blusunrize.immersiveengineering.api.crafting.MetalPressRecipe;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.block.BlockINOre;
import immersivenuclear.common.block.BlockSheetmetalSlab;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSlab;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;

/**
 * The raw materials of line 3 (14/11/2026), in two scales (the user's rule):
 * - NORMAL, IE's way, for general use: ore -> IE's Crusher -> dust -> IE's Arc Furnace -> ingot (zirconium, ferroboron);
 * - NUCLEAR, for the mod's own things (above all the reactor): the dust through line 1's purification machines, into nuclear
 *   zirconium and hafnium (cladding, control rods) and boric acid and boron carbide. Line 1's machines take these recipes
 *   in the next step; here are the items, the fluids and IE's part (Arc Furnace, Crusher, Metal Press).
 * The mod's metals also get, as IE's: a plate (Metal Press or hammer), a storage block, a sheet metal block and its slab;
 * the depleted uranium too.
 * Amounts, times and energy are provisional (balancing later).
 */
public final class INRawMaterials
{
	/**
	 * A metal: ingot and nugget, and, unless it is a ferro-alloy, IE's family of plate, storage block, sheet metal block and sheet
	 * metal slab. The ferro-alloys (ferroboron, ferrochrome) are additives that go into other alloys, broken lumps out of the
	 * furnace: no plate nor blocks (the user, 15/11/2026). Names shared with other mods through the ore dictionary
	 * (NuclearCraft's ingotFerroboron / ingotZircaloy, GregTech's ingotStainlessSteel, everyone's ingotTin...).
	 */
	public enum Metal
	{
		ZIRCONIUM("zirconium", "Zirconium", true), ZIRCONIUM_NUCLEAR("zirconium_nuclear", "ZirconiumNuclear", true),
		ZIRCALOY("zircaloy", "Zircaloy", true), HAFNIUM("hafnium", "Hafnium", true), TIN("tin", "Tin", true),
		FERROBORON("ferroboron", "Ferroboron", false), FERROCHROME("ferrochrome", "Ferrochrome", false),
		STAINLESS_STEEL("stainless_steel", "StainlessSteel", true), BORATED_STEEL("borated_steel", "BoratedSteel", true),
		DEPLETED_URANIUM("depleted_uranium", "DepletedUranium", true);

		public final String id, ore;
		/** Has the plate and the blocks (false for the ferro-alloys). */
		public final boolean family;
		public Block storage, sheetmetal;
		public BlockSheetmetalSlab slab, slabDouble;
		public Item ingot, nugget, plate, storageItem, sheetmetalItem, slabItem;

		Metal(String id, String ore, boolean family)
		{
			this.id = id;
			this.ore = ore;
			this.family = family;
		}
	}

	public static Block zirconOreGranite, zirconOreSand, boraxOreTerracotta, boraxOreSand, tinOre, chromiteOre;
	public static Item itemZirconOreGranite, itemZirconOreSand, itemBoraxOreTerracotta, itemBoraxOreSand, itemTinOre, itemChromiteOre;
	public static Item itemTinDust, itemChromiteDust;
	public static Item itemZirconDust, itemBoraxDust, itemZirconiumPrecipitate, itemHafniumPrecipitate, itemBoricAcid, itemBoronCarbide;
	public static Fluid fluidZirconiumSolution, fluidZirconiumSolutionPurified, fluidHafniumSolution, fluidBoricAcidSolution, fluidFluorosilicicAcid;

	private INRawMaterials()
	{
	}

	/** Called with the mod's fluids: the nuclear scale's solutions. */
	static Fluid[] fluids()
	{
		fluidZirconiumSolution = INContent.fluid("zirconium_solution", 1150, 1200);
		fluidZirconiumSolutionPurified = INContent.fluid("zirconium_solution_purified", 1080, 1100);
		fluidHafniumSolution = INContent.fluid("hafnium_solution", 1100, 1100);
		fluidBoricAcidSolution = INContent.fluid("boric_acid_solution", 1050, 1000);
		fluidFluorosilicicAcid = INContent.fluid("fluorosilicic_acid", 1220, 1100);    // quartz dissolved in hydrofluoric acid (the silicon's chain)
		return new Fluid[]{fluidZirconiumSolution, fluidZirconiumSolutionPurified, fluidHafniumSolution, fluidBoricAcidSolution, fluidFluorosilicicAcid};
	}

	static Block metalBlock(String name)
	{
		return new immersivenuclear.common.block.BlockINMetal(name);
	}

	static void blocks(RegistryEvent.Register<Block> event)
	{
		zirconOreGranite = new BlockINOre("zircon_ore_granite", BlockINOre.Ground.STONE);
		zirconOreSand = new BlockINOre("zircon_ore_sand", BlockINOre.Ground.SAND);
		boraxOreTerracotta = new BlockINOre("borax_ore_terracotta", BlockINOre.Ground.CLAY);
		boraxOreSand = new BlockINOre("borax_ore_sand", BlockINOre.Ground.SAND);
		tinOre = new BlockINOre("tin_ore", BlockINOre.Ground.STONE);
		chromiteOre = new BlockINOre("chromite_ore", BlockINOre.Ground.STONE);
		event.getRegistry().registerAll(zirconOreGranite, zirconOreSand, boraxOreTerracotta, boraxOreSand, tinOre, chromiteOre);
		for(Metal m : Metal.values())
		{
			if(!m.family)
				continue;
			m.storage = metalBlock(m.id + "_block");
			m.sheetmetal = metalBlock(m.id + "_sheetmetal");
			m.slab = new BlockSheetmetalSlab.Half(m.id);
			m.slabDouble = new BlockSheetmetalSlab.Double(m.id);
			m.slabDouble.half = m.slab;
			event.getRegistry().registerAll(m.storage, m.sheetmetal, m.slab, m.slabDouble);
		}
	}

	static Item block(RegistryEvent.Register<Item> event, Block b)
	{
		Item i = new ItemBlock(b).setRegistryName(b.getRegistryName());
		event.getRegistry().register(i);
		return i;
	}

	// Registered by kind, in IE's order (the user, 14/11: 'organize os itens, pos, escorias, blocos, chapas... igual no IE'):
	// INContent.registerItems calls these between its own groups, so JEI's list and the creative tab follow kinds

	static void ores(RegistryEvent.Register<Item> event)
	{
		itemZirconOreGranite = block(event, zirconOreGranite);
		itemZirconOreSand = block(event, zirconOreSand);
		itemBoraxOreTerracotta = block(event, boraxOreTerracotta);
		itemBoraxOreSand = block(event, boraxOreSand);
		itemTinOre = block(event, tinOre);
		itemChromiteOre = block(event, chromiteOre);
	}

	/** The storage blocks, then the sheet metal blocks, then their slabs, as IE's. */
	static void metalBlocks(RegistryEvent.Register<Item> event)
	{
		for(Metal m : Metal.values())
			if(m.family)
				m.storageItem = block(event, m.storage);
		for(Metal m : Metal.values())
			if(m.family)
				m.sheetmetalItem = block(event, m.sheetmetal);
		for(Metal m : Metal.values())
		{
			if(!m.family)
				continue;
			m.slabItem = new ItemSlab(m.slab, m.slab, m.slabDouble).setRegistryName(m.slab.getRegistryName());
			event.getRegistry().register(m.slabItem);
		}
	}

	/** After the depleted uranium ingot exists (its family reuses it). */
	static void ingots(RegistryEvent.Register<Item> event)
	{
		for(Metal m : Metal.values())
			m.ingot = m == Metal.DEPLETED_URANIUM ? INContent.itemDepletedUraniumIngot : INContent.item(event, m.id + "_ingot");
	}

	/** The nuggets, after the ingots as IE's. */
	static void nuggets(RegistryEvent.Register<Item> event)
	{
		for(Metal m : Metal.values())
			m.nugget = INContent.item(event, m.id + "_nugget");
	}

	static void plates(RegistryEvent.Register<Item> event)
	{
		for(Metal m : Metal.values())
			if(m.family)
				m.plate = INContent.item(event, m.id + "_plate");
	}

	static void dusts(RegistryEvent.Register<Item> event)
	{
		itemZirconDust = INContent.item(event, "zircon_dust");
		itemBoraxDust = INContent.item(event, "borax_dust");
		itemTinDust = INContent.item(event, "tin_dust");
		itemChromiteDust = INContent.item(event, "chromite_dust");
		itemBoricAcid = INContent.item(event, "boric_acid");
		itemBoronCarbide = INContent.item(event, "boron_carbide");
	}

	static void precipitates(RegistryEvent.Register<Item> event)
	{
		itemZirconiumPrecipitate = INContent.item(event, "zirconium_precipitate");
		itemHafniumPrecipitate = INContent.item(event, "hafnium_precipitate");
	}

	static Item[] oreItems()
	{
		return new Item[]{itemZirconOreGranite, itemZirconOreSand, itemBoraxOreTerracotta, itemBoraxOreSand, itemTinOre, itemChromiteOre};
	}

	static Item[] metalBlockItems()
	{
		List<Item> l = new ArrayList<>();
		for(Metal m : Metal.values())
			if(m.family)
				l.add(m.storageItem);
		for(Metal m : Metal.values())
			if(m.family)
				l.add(m.sheetmetalItem);
		for(Metal m : Metal.values())
			if(m.family)
				l.add(m.slabItem);
		return l.toArray(new Item[0]);
	}

	static Item[] ingotItems()
	{
		List<Item> l = new ArrayList<>();
		for(Metal m : Metal.values())
			l.add(m.ingot);
		return l.toArray(new Item[0]);
	}

	static Item[] nuggetItems()
	{
		List<Item> l = new ArrayList<>();
		for(Metal m : Metal.values())
			l.add(m.nugget);
		return l.toArray(new Item[0]);
	}

	static Item[] plateItems()
	{
		List<Item> l = new ArrayList<>();
		for(Metal m : Metal.values())
			if(m.family)
				l.add(m.plate);
		return l.toArray(new Item[0]);
	}

	static Item[] dustItems()
	{
		return new Item[]{itemZirconDust, itemBoraxDust, itemTinDust, itemChromiteDust, itemBoricAcid, itemBoronCarbide};
	}

	static Item[] precipitateItems()
	{
		return new Item[]{itemZirconiumPrecipitate, itemHafniumPrecipitate};
	}

	/** All of them (models, hazards). */
	static Item[] creativeGroup()
	{
		List<Item> l = new ArrayList<>();
		for(Item[] g : new Item[][]{oreItems(), metalBlockItems(), ingotItems(), nuggetItems(), plateItems(), dustItems(), precipitateItems()})
			l.addAll(java.util.Arrays.asList(g));
		return l.toArray(new Item[0]);
	}

	/** Every item with a plain inventory model (ClientEvents); the depleted uranium ingot has its own already. */
	public static Item[] modelItems()
	{
		List<Item> l = new ArrayList<>();
		for(Item i : creativeGroup())
			if(i != INContent.itemDepletedUraniumIngot)
				l.add(i);
		l.addAll(java.util.Arrays.asList(INFuel.items()));
		l.addAll(java.util.Arrays.asList(INAssembly.items()));
		return l.toArray(new Item[0]);
	}

	/** The items with a JEI page of their own. */
	public static Item[] infoItems()
	{
		return new Item[]{itemZirconOreGranite, itemZirconOreSand, itemZirconDust, itemBoraxOreTerracotta, itemBoraxOreSand, itemBoraxDust,
				Metal.ZIRCONIUM.ingot, Metal.ZIRCONIUM_NUCLEAR.ingot, Metal.HAFNIUM.ingot, Metal.FERROBORON.ingot,
				itemZirconiumPrecipitate, itemHafniumPrecipitate, itemBoricAcid, itemBoronCarbide,
				itemTinOre, itemTinDust, itemChromiteOre, itemChromiteDust, Metal.TIN.ingot, Metal.FERROCHROME.ingot, Metal.STAINLESS_STEEL.ingot,
				Metal.BORATED_STEEL.ingot, Metal.ZIRCALOY.ingot,
				INFuel.itemEnrichedUraniumDioxide, INFuel.itemGreenPellet, INFuel.itemSinteredPellet, INFuel.itemZircaloyTube, INFuel.itemFuelRod,
				INFuel.itemFuelElement, INFuel.itemMoldTube, INFuel.itemHafniumControlRod};
	}

	// provisional amounts (balancing later)
	public static final int ORE_DUST = 2, CRUSH_ENERGY = 4000, ARC_TICKS = 100, ARC_ENERGY = 512, PRESS_ENERGY = 2400;
	public static final int ZIRCON_EXCAVATOR_WEIGHT = 12, BORAX_EXCAVATOR_WEIGHT = 12, CASSITERITE_EXCAVATOR_WEIGHT = 15, CHROMITE_EXCAVATOR_WEIGHT = 8;
	/** Arc Furnace alloys: steel ingots per batch (stainless, borated: + 1 ferro-alloy), nuclear zirconium per Zircaloy batch. */
	public static final int STEEL_PER_ALLOY = 3, ALLOY_OUT = 4, ZIRCONIUM_PER_ZIRCALOY = 6, ZIRCALOY_OUT = 8;
	public static final float EXCAVATOR_FAIL = .1F;

	static void recipes()
	{
		// ore dictionary: shared with other mods (their zirconium, hafnium, boron... work in our machines and the other way)
		OreDictionary.registerOre("oreZircon", zirconOreGranite);
		OreDictionary.registerOre("oreZircon", zirconOreSand);
		OreDictionary.registerOre("oreBorax", boraxOreTerracotta);
		OreDictionary.registerOre("oreBorax", boraxOreSand);
		OreDictionary.registerOre("dustZircon", itemZirconDust);
		OreDictionary.registerOre("dustBorax", itemBoraxDust);
		OreDictionary.registerOre("dustBoricAcid", itemBoricAcid);
		OreDictionary.registerOre("dustBoronCarbide", itemBoronCarbide);
		OreDictionary.registerOre("oreTin", tinOre);
		OreDictionary.registerOre("oreChromite", chromiteOre);
		OreDictionary.registerOre("dustTin", itemTinDust);
		OreDictionary.registerOre("dustChromite", itemChromiteDust);
		for(Metal m : Metal.values())
		{
			if(m != Metal.DEPLETED_URANIUM)
				OreDictionary.registerOre("ingot" + m.ore, m.ingot);
			OreDictionary.registerOre("nugget" + m.ore, m.nugget);
			if(!m.family)
				continue;
			OreDictionary.registerOre("plate" + m.ore, m.plate);
			OreDictionary.registerOre("block" + m.ore, m.storage);
			OreDictionary.registerOre("blockSheetmetal" + m.ore, m.sheetmetal);
			OreDictionary.registerOre("slabSheetmetal" + m.ore, m.slab);
			// IE's Metal Press, the plate mould
			MetalPressRecipe.addRecipe(new ItemStack(m.plate), "ingot" + m.ore,
					new ItemStack(blusunrize.immersiveengineering.common.IEContent.itemMold, 1, 0), PRESS_ENERGY);
		}
		// IE's Crusher: two dusts per ore, as its own metal ores
		CrusherRecipe.addRecipe(new ItemStack(itemZirconDust, ORE_DUST), "oreZircon", CRUSH_ENERGY);
		CrusherRecipe.addRecipe(new ItemStack(itemBoraxDust, ORE_DUST), "oreBorax", CRUSH_ENERGY);
		CrusherRecipe.addRecipe(new ItemStack(itemTinDust, ORE_DUST), "oreTin", CRUSH_ENERGY);
		CrusherRecipe.addRecipe(new ItemStack(itemTinDust, ORE_DUST), "oreCassiterite", CRUSH_ENERGY);     // GregTech's tin ore
		CrusherRecipe.addRecipe(new ItemStack(itemChromiteDust, ORE_DUST), "oreChromite", CRUSH_ENERGY);
		CrusherRecipe.addRecipe(new ItemStack(itemTinDust), "ingotTin", CRUSH_ENERGY / 2);
		// IE's Arc Furnace. Normal scale: the zircon to common zirconium (hafnium left in it), as IE's ores and dusts;
		// the borax with iron and coke to ferroboron (the alloy the steel industry adds boron with)
		ItemStack slag = new ItemStack(blusunrize.immersiveengineering.common.IEContent.itemMaterial, 1, INContent.SLAG_META);
		ArcFurnaceRecipe.addRecipe(new ItemStack(Metal.ZIRCONIUM.ingot, 2), "oreZircon", slag, ARC_TICKS * 2, ARC_ENERGY);
		ArcFurnaceRecipe.addRecipe(new ItemStack(Metal.ZIRCONIUM.ingot), "dustZircon", ItemStack.EMPTY, ARC_TICKS, ARC_ENERGY);
		ArcFurnaceRecipe.addRecipe(new ItemStack(Metal.FERROBORON.ingot), "dustBorax", slag, ARC_TICKS, ARC_ENERGY, "ingotIron", "dustCoke");
		// tin as IE's metals; chromite reduced with coke into ferrochrome (the chromite brings its own iron)
		ArcFurnaceRecipe.addRecipe(new ItemStack(Metal.TIN.ingot, 2), "oreTin", slag, ARC_TICKS * 2, ARC_ENERGY);
		ArcFurnaceRecipe.addRecipe(new ItemStack(Metal.TIN.ingot), "dustTin", ItemStack.EMPTY, ARC_TICKS, ARC_ENERGY);
		ArcFurnaceRecipe.addRecipe(new ItemStack(Metal.FERROCHROME.ingot), "dustChromite", slag, ARC_TICKS, ARC_ENERGY, "dustCoke");
		// the alloys (15/11/2026): steel + a ferro-alloy into stainless or borated steel; nuclear zirconium + tin + ferrochrome into
		// Zircaloy-4. GregTech's pure chrome is taken in place of the ferrochrome
		// (the pure chrome only when a mod has it: an empty ingredient showed in JEI as a Zircaloy without chromium, the user 15/11)
		for(String cr : OreDictionary.getOres("ingotChrome", false).isEmpty() ? new String[]{"ingotFerrochrome"} : new String[]{"ingotFerrochrome", "ingotChrome"})
		{
			ArcFurnaceRecipe.addRecipe(new ItemStack(Metal.STAINLESS_STEEL.ingot, ALLOY_OUT), new blusunrize.immersiveengineering.api.crafting.IngredientStack("ingotSteel", STEEL_PER_ALLOY),
					ItemStack.EMPTY, ARC_TICKS * 2, ARC_ENERGY, cr);
			ArcFurnaceRecipe.addRecipe(new ItemStack(Metal.ZIRCALOY.ingot, ZIRCALOY_OUT),
					new blusunrize.immersiveengineering.api.crafting.IngredientStack("ingotZirconiumNuclear", ZIRCONIUM_PER_ZIRCALOY), ItemStack.EMPTY,
					ARC_TICKS * 2, ARC_ENERGY, "ingotTin", cr);
		}
		ArcFurnaceRecipe.addRecipe(new ItemStack(Metal.BORATED_STEEL.ingot, ALLOY_OUT), new blusunrize.immersiveengineering.api.crafting.IngredientStack("ingotSteel", STEEL_PER_ALLOY),
				ItemStack.EMPTY, ARC_TICKS * 2, ARC_ENERGY, "ingotFerroboron");
		// Nuclear scale: the precipitates (from line 1's Precipitation Unit) reduced with coke; boric acid to boron carbide
		ArcFurnaceRecipe.addRecipe(new ItemStack(Metal.ZIRCONIUM_NUCLEAR.ingot), new ItemStack(itemZirconiumPrecipitate), ItemStack.EMPTY, ARC_TICKS, ARC_ENERGY, "dustCoke");
		ArcFurnaceRecipe.addRecipe(new ItemStack(Metal.HAFNIUM.ingot), new ItemStack(itemHafniumPrecipitate), ItemStack.EMPTY, ARC_TICKS, ARC_ENERGY, "dustCoke");
		ArcFurnaceRecipe.addRecipe(new ItemStack(itemBoronCarbide), "dustBoricAcid", ItemStack.EMPTY, ARC_TICKS, ARC_ENERGY, "dustCoke");
		// the Overworld and IE's Excavator
		GameRegistry.registerWorldGenerator(new immersivenuclear.common.world.WorldGenRawMaterials(), 0);
		if(INConfig.line3ExcavatorVeins)
		{
			// zircon with the heavy minerals it is mined with (iron oxides) and quartz; borax with the gypsum of the same dry lakes
			blusunrize.immersiveengineering.api.tool.ExcavatorHandler.addMineral("Zircon", ZIRCON_EXCAVATOR_WEIGHT, EXCAVATOR_FAIL,
					new String[]{"oreZircon", "oreIron", "gemQuartz"}, new float[]{.65F, .25F, .1F});
			blusunrize.immersiveengineering.api.tool.ExcavatorHandler.addMineral("Borax", BORAX_EXCAVATOR_WEIGHT, EXCAVATOR_FAIL,
					new String[]{"oreBorax", "dustGypsum"}, new float[]{.8F, .2F});
			// cassiterite with the quartz and iron of its granite veins; chromite with the iron and nickel of the deep rocks
			blusunrize.immersiveengineering.api.tool.ExcavatorHandler.addMineral("Cassiterite", CASSITERITE_EXCAVATOR_WEIGHT, EXCAVATOR_FAIL,
					new String[]{"oreTin", "oreIron", "gemQuartz"}, new float[]{.7F, .2F, .1F});
			blusunrize.immersiveengineering.api.tool.ExcavatorHandler.addMineral("Chromite", CHROMITE_EXCAVATOR_WEIGHT, EXCAVATOR_FAIL,
					new String[]{"oreChromite", "oreIron", "oreNickel"}, new float[]{.6F, .25F, .15F});
		}
		INFuel.recipes();
	}
}
