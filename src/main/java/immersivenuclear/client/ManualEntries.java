package immersivenuclear.client;

import blusunrize.immersiveengineering.api.ManualHelper;
import blusunrize.immersiveengineering.api.ManualPageMultiblock;
import blusunrize.lib.manual.IManualPage;
import blusunrize.lib.manual.ManualInstance;
import blusunrize.lib.manual.ManualPages;
import immersivenuclear.common.INAssembly;
import immersivenuclear.common.INContent;
import immersivenuclear.common.INFuel;
import immersivenuclear.common.INRawMaterials;
import immersivenuclear.common.multiblock.MultiblockChemicalProcessor;
import immersivenuclear.common.multiblock.MultiblockChemicalSeparator;
import immersivenuclear.common.multiblock.MultiblockFilterPress;
import immersivenuclear.common.multiblock.MultiblockIndustrialDryer;
import immersivenuclear.common.multiblock.MultiblockMetalExtractor;
import immersivenuclear.common.multiblock.MultiblockPrecipitationUnit;
import immersivenuclear.common.multiblock.MultiblockSulfuricAcidUnit;
import immersivenuclear.common.multiblock.MultiblockHFKiln;
import immersivenuclear.common.multiblock.MultiblockElectrolyzer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Our categories in IE's Engineer's Manual (16/11/2026, the user's layout, with Immersive Intelligence's manual as the
 * level of detail): IE's manual has two levels, categories and entries, so each stage of the mod is a category of its own
 * (as II does), and each stage opens with its related items (picture and text). Resources holds what fits no line; the
 * general machines serve every line. IE builds the manual in its client postInit, so this runs in ours.
 * Texts: ie.manual.category.&lt;category&gt;.name and ie.manual.entry.&lt;entry&gt;[.name|.subtext|N] in the lang files.
 */
public class ManualEntries
{
	public static final String RESOURCES = "in_resources", MACHINES = "in_machines", PURIFICATION = "in_purification",
			ENRICHMENT = "in_enrichment", FUEL = "in_fuel", REACTOR = "in_reactor", POWER = "in_power", CONTROL = "in_control";
	/** Items per page of a related items list (IE's item display keeps them big up to this many). */
	static final int PER_PAGE = 8;

	static ManualInstance manual;

	public static void register()
	{
		manual = ManualHelper.getManual();
		resources();
		machines();
		purification();
		enrichment();
		fuel();
		reactor();
		power();
		control();
	}

	// ---------- Resources: what every line uses and fits none ----------
	static void resources()
	{
		List<IManualPage> p = new ArrayList<>();
		p.add(text("in_res_blocks0"));
		items(p, "in_res_blocks1", cat(INContent.itemEngineering, INContent.itemLeadedGlass));
		items(p, "in_res_blocks2", INContent.itemAgglomerate, INContent.itemAgglomerateBlock, INContent.itemAgglomerateTile, INContent.itemAgglomerateBricks,
				INContent.itemPlaster, INContent.itemPlasterTile, INContent.itemPlasterBricks);
		entry("in_res_blocks", RESOURCES, p);
		p = new ArrayList<>();
		p.add(text("in_res_materials0"));
		items(p, "in_res_materials1", cat(new Item[]{INContent.itemFluoriteOre, INContent.itemFluorite, INContent.itemFluoriteDust}, INRawMaterials.oreItems()));
		items(p, "in_res_materials2", INRawMaterials.ingotItems());
		items(p, "in_res_materials3", INRawMaterials.plateItems());
		items(p, "in_res_materials4", cat(INRawMaterials.dustItems(), INContent.itemQuicklime, INContent.itemGypsum));
		items(p, "in_res_materials5", INAssembly.itemSilica, INAssembly.itemSilicon, INAssembly.itemSiliconWafer, INAssembly.itemMoldWafer, INAssembly.itemFerriteIngot);
		entry("in_res_materials", RESOURCES, p);
		add("in_supplies", RESOURCES,
				text("in_supplies0"), text("in_supplies1"),
				new ManualPages.Crafting(manual, "in_supplies2", new ItemStack(INContent.itemPrecipitatingAgent)));
		add("in_hazards", RESOURCES,
				text("in_hazards0"), text("in_hazards1"));
	}

	// ---------- General machines: the ones that serve every line ----------
	static void machines()
	{
		List<IManualPage> p = new ArrayList<>();
		p.add(new ManualPages.ItemDisplay(manual, "in_workbench0", new ItemStack(INAssembly.itemINWorkbench)));
		p.add(text("in_workbench1"));
		items(p, "in_workbench2", INAssembly.itemComponents);
		items(p, "in_workbench3", cat(new Item[]{INAssembly.itemEmptyReel, INAssembly.itemComponentTape}, INAssembly.itemJigs));
		items(p, "in_workbench4", INAssembly.itemModules);
		items(p, "in_workbench5", INAssembly.blueprintGroup());
		entry("in_workbench", MACHINES, p);
		add("in_assembly_station", MACHINES, text("in_assembly_station0"), text("in_assembly_station1"));
		add("in_acheson", MACHINES, text("in_acheson0"));
		add("in_sulfuric_acid_unit", MACHINES,
				new ManualPageMultiblock(manual, "in_sulfuric_acid_unit0", MultiblockSulfuricAcidUnit.instance),
				text("in_sulfuric_acid_unit1"));
		add("in_hf_kiln", MACHINES,
				new ManualPageMultiblock(manual, "in_hf_kiln0", MultiblockHFKiln.instance),
				text("in_hf_kiln1"));
		add("in_electrolyzer", MACHINES,
				new ManualPageMultiblock(manual, "in_electrolyzer0", MultiblockElectrolyzer.instance),
				text("in_electrolyzer1"), text("in_electrolyzer2"));
		add("in_metal_extractor", MACHINES,
				new ManualPageMultiblock(manual, "in_metal_extractor0", MultiblockMetalExtractor.instance),
				text("in_metal_extractor1"), text("in_metal_extractor2"),
				new ManualPages.Crafting(manual, "in_metal_extractor3", new ItemStack(INContent.itemElectrodeSteel),
						new ItemStack(INContent.itemElectrodeLeadSilver), new ItemStack(INContent.itemElectrodeElectrum)));
	}

	// ---------- Line 1: ore to yellowcake ----------
	static void purification()
	{
		List<IManualPage> p = new ArrayList<>();
		p.add(text("in_purification_items0"));
		items(p, "in_purification_items1", INContent.itemReagent, INContent.itemFilter, INContent.itemPrecipitatingAgent, INContent.itemWetConcentrate,
				INContent.itemYellowcake, INContent.itemConcentrateDrum);
		items(p, "in_purification_items2", buckets(INContent.fluidSlurry, INContent.fluidCrudeSolution, INContent.fluidSolvent, INContent.fluidStrippingSolution,
				INContent.fluidPurifiedSolution, INContent.fluidEffluent, INContent.fluidSludge, INContent.fluidDepletedEffluent));
		items(p, "in_purification_items3", cat(new Item[]{INContent.itemResidue, INContent.itemNeutralizedResidue, INContent.itemCake, INContent.itemMixedDeposit},
				INRawMaterials.precipitateItems()));
		entry("in_purification_items", PURIFICATION, p);
		add("in_chemical_processor", PURIFICATION,
				new ManualPageMultiblock(manual, "in_chemical_processor0", MultiblockChemicalProcessor.instance),
				text("in_chemical_processor1"), text("in_chemical_processor2"),
				new ManualPages.Crafting(manual, "in_chemical_processor3", new ItemStack(INContent.itemReagent)));
		add("in_filter_press", PURIFICATION,
				new ManualPageMultiblock(manual, "in_filter_press0", MultiblockFilterPress.instance),
				text("in_filter_press1"), text("in_filter_press2"),
				new ManualPages.Crafting(manual, "in_filter_press3", new ItemStack(INContent.itemFilter)));
		add("in_chemical_separator", PURIFICATION,
				new ManualPageMultiblock(manual, "in_chemical_separator0", MultiblockChemicalSeparator.instance),
				text("in_chemical_separator1"), text("in_chemical_separator2"));
		add("in_precipitation_unit", PURIFICATION,
				new ManualPageMultiblock(manual, "in_precipitation_unit0", MultiblockPrecipitationUnit.instance),
				text("in_precipitation_unit1"), text("in_precipitation_unit2"),
				new ManualPages.Crafting(manual, "in_precipitation_unit3", new ItemStack(INContent.itemPrecipitatingAgent)));
		add("in_industrial_dryer", PURIFICATION,
				new ManualPageMultiblock(manual, "in_industrial_dryer0", MultiblockIndustrialDryer.instance),
				text("in_industrial_dryer1"), text("in_industrial_dryer2"), text("in_industrial_dryer3"),
				new ManualPages.Crafting(manual, "in_industrial_dryer4", new ItemStack(INContent.itemConcentrateDrum)));
		add("in_byproducts", PURIFICATION,
				text("in_byproducts0"), text("in_byproducts1"), text("in_byproducts2"),
				new ManualPages.Crafting(manual, "in_byproducts3", new ItemStack(INContent.itemNeutralizedResidue), new ItemStack(INContent.itemAgglomerate)));
	}

	// ---------- Line 2: yellowcake to enriched hexafluoride ----------
	static void enrichment()
	{
		List<IManualPage> p = new ArrayList<>();
		p.add(text("in_enrichment_items0"));
		items(p, "in_enrichment_items1", buckets(INContent.fluidSulfuricAcid, INContent.fluidHydrofluoricAcid, INContent.fluidHydrogen, INContent.fluidOxygen,
				INContent.fluidSteam));
		items(p, "in_enrichment_items2", buckets(INContent.fluidUF6, INContent.fluidUF6Low, INContent.fluidUF6Medium, INContent.fluidUF6High, INContent.fluidUF6Depleted));
		items(p, "in_enrichment_items3", INContent.itemFluorinationAsh, INContent.itemDepletedUraniumOxide, INContent.itemDepletedUraniumIngot);
		entry("in_enrichment_items", ENRICHMENT, p);
		add("in_conversion_unit", ENRICHMENT,
				new ManualPageMultiblock(manual, "in_conversion_unit0", immersivenuclear.common.multiblock.MultiblockConversionUnit.instance),
				text("in_conversion_unit1"), text("in_conversion_unit2"), text("in_conversion_unit3"));
		add("in_enrichment_unit", ENRICHMENT,
				new ManualPageMultiblock(manual, "in_enrichment_unit0", immersivenuclear.common.multiblock.MultiblockEnrichmentUnit.instance),
				new ManualPageMultiblock(manual, "in_enrichment_unit1", immersivenuclear.common.multiblock.MultiblockEnrichmentUnit4.instance),
				new ManualPageMultiblock(manual, "in_enrichment_unit2", immersivenuclear.common.multiblock.MultiblockEnrichmentUnit6.instance),
				text("in_enrichment_unit3"), text("in_enrichment_unit4"));
		add("in_deconversion_unit", ENRICHMENT,
				new ManualPageMultiblock(manual, "in_deconversion_unit0", immersivenuclear.common.multiblock.MultiblockDeconversionUnit.instance),
				text("in_deconversion_unit1"), text("in_deconversion_unit2"));
	}

	// ---------- Line 3: the fuel (machines to come) ----------
	static void fuel()
	{
		List<IManualPage> p = new ArrayList<>();
		p.add(text("in_fuel_items0"));
		items(p, "in_fuel_items1", INFuel.items());
		entry("in_fuel_items", FUEL, p);
		add("in_fuel_dev", FUEL, text("in_fuel_dev0"), text("in_fuel_dev1"));
	}

	// ---------- Line 4: the reactor (in development) ----------
	static void reactor()
	{
		List<IManualPage> p = new ArrayList<>();
		p.add(text("in_reactor_items0"));
		items(p, "in_reactor_items1", INFuel.itemFuelElement, INFuel.itemHafniumControlRod);
		entry("in_reactor_items", REACTOR, p);
		add("in_reactor_dev", REACTOR, text("in_reactor_dev0"), text("in_reactor_dev1"));
	}

	// ---------- Line 5: power generation (in development) ----------
	static void power()
	{
		add("in_power_items", POWER, text("in_power_items0"));
		add("in_power_dev", POWER, text("in_power_dev0"));
	}

	// ---------- Control and automation: the computers ----------
	static void control()
	{
		List<IManualPage> p = new ArrayList<>();
		p.add(text("in_control_items0"));
		items(p, "in_control_items1", INContent.itemComputerParts);
		items(p, "in_control_items2", INContent.itemComputerDevices);
		items(p, "in_control_items3", INContent.itemConduit, INContent.itemDataCoil, INContent.itemLinkConnector, INContent.itemLinkRelay);
		entry("in_control_items", CONTROL, p);
		p = new ArrayList<>();
		p.add(text("in_console_modules0"));
		List<Item> commands = new ArrayList<>(), indicators = new ArrayList<>(), text = new ArrayList<>();
		for(Item m : INContent.itemPanelModules)
		{
			int kind = ((immersivenuclear.common.item.ItemPanelModule)m).kind;
			(kind == 0 ? commands : kind == 1 ? indicators : text).add(m);
		}
		items(p, "in_console_modules1", commands.toArray(new Item[0]));
		items(p, "in_console_modules2", indicators.toArray(new Item[0]));
		items(p, "in_console_modules3", text.toArray(new Item[0]));
		entry("in_console_modules", CONTROL, p);
		for(immersivenuclear.common.multiblock.MultiblockComputer m : immersivenuclear.common.multiblock.MultiblockComputer.ALL)
			add("in_" + m.structure.name, CONTROL, new ManualPageMultiblock(manual, "in_" + m.structure.name + "0", m));
	}

	// ---------- helpers ----------
	static IManualPage text(String key)
	{
		return new ManualPages.Text(manual, key);
	}

	static void entry(String name, String category, List<IManualPage> pages)
	{
		add(name, category, pages.toArray(new IManualPage[0]));
	}

	/** IE's addEntry, with each page's continuation pages (<key>.more1, .more2...: a text too long for its page, split by
	 * ferramentas/manual_paginas.py, since IE's manual does not break a text into pages) right after it. */
	static void add(String name, String category, IManualPage... pages)
	{
		List<IManualPage> all = new ArrayList<>();
		for(IManualPage page : pages)
		{
			all.add(page);
			String key = keyOf(page);
			if(key != null)
				for(int i = 1; net.minecraft.client.resources.I18n.hasKey("ie.manual.entry." + key + ".more" + i); i++)
					all.add(text(key + ".more" + i));
		}
		ManualHelper.addEntry(name, category, all.toArray(new IManualPage[0]));
	}

	static java.lang.reflect.Field textField;

	/** The text key of one of IE's pages (ManualPages.text, protected). */
	static String keyOf(IManualPage page)
	{
		if(!(page instanceof ManualPages))
			return null;
		try
		{
			if(textField == null)
			{
				textField = ManualPages.class.getDeclaredField("text");
				textField.setAccessible(true);
			}
			return (String)textField.get(page);
		}
		catch(ReflectiveOperationException e)
		{
			return null;
		}
	}

	/** A list of related items: PER_PAGE to a page, the text under the first page only. */
	static void items(List<IManualPage> pages, String key, Item... items)
	{
		List<ItemStack> stacks = new ArrayList<>();
		for(Item i : items)
			if(i != null)
				stacks.add(new ItemStack(i));
		items(pages, key, stacks);
	}

	static void items(List<IManualPage> pages, String key, List<ItemStack> stacks)
	{
		for(int i = 0; i < stacks.size(); i += PER_PAGE)
			pages.add(new ManualPages.ItemDisplay(manual, i == 0 ? key : null,
					stacks.subList(i, Math.min(stacks.size(), i + PER_PAGE)).toArray(new ItemStack[0])));
	}

	/** The buckets of some of our fluids, for the related items pages. */
	static List<ItemStack> buckets(Fluid... fluids)
	{
		List<ItemStack> l = new ArrayList<>();
		for(Fluid f : fluids)
			if(f != null)
			{
				ItemStack b = FluidUtil.getFilledBucket(new FluidStack(f, Fluid.BUCKET_VOLUME));
				if(!b.isEmpty())
					l.add(b);
			}
		return l;
	}

	static Item[] cat(Item[] a, Item... b)
	{
		List<Item> l = new ArrayList<>(Arrays.asList(a));
		l.addAll(Arrays.asList(b));
		return l.toArray(new Item[0]);
	}
}
