package immersivenuclear.common;

import java.util.HashMap;
import java.util.Map;

import blusunrize.immersiveengineering.api.MultiblockHandler;
import blusunrize.immersiveengineering.api.crafting.CokeOvenRecipe;
import blusunrize.immersiveengineering.api.crafting.MixerRecipe;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.block.BlockChemicalProcessor;
import immersivenuclear.common.block.BlockChemicalSeparator;
import immersivenuclear.common.block.BlockFilterPress;
import immersivenuclear.common.block.TileChemicalProcessor;
import immersivenuclear.common.block.TileChemicalSeparator;
import immersivenuclear.common.block.TileFilterPress;
import immersivenuclear.common.item.ItemFilter;
import immersivenuclear.common.multiblock.MultiblockChemicalProcessor;
import immersivenuclear.common.multiblock.MultiblockChemicalSeparator;
import immersivenuclear.common.multiblock.MultiblockFilterPress;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod.EventBusSubscriber(modid = ImmersiveNuclear.MODID)
public class INContent
{
	public static BlockChemicalProcessor blockChemicalProcessor;
	public static BlockFilterPress blockFilterPress;
	public static BlockChemicalSeparator blockChemicalSeparator;

	/** The mod's creative tab; JEI also lists what is in it. */
	public static final CreativeTabs TAB = new CreativeTabs(ImmersiveNuclear.MODID)
	{
		@Override
		public ItemStack createIcon()
		{
			return new ItemStack(itemChemicalProcessor);
		}
	};

	/** Shown by pick block, Hwyla/Jade and JEI (as the machine of its recipes) for every formed part; drawn as the
	 * 3D model (blockstate variant "inventory"); it places nothing. */
	public static Item itemChemicalProcessor;

	/** The same for the Filtration Unit. */
	public static Item itemFilterPress;

	/** The same for the Chemical Separation Unit. */
	public static Item itemChemicalSeparator;

	/** Processing Reagent: consumed one per cycle by the Chemical Processing Unit. */
	public static Item itemReagent;

	/** Uranium Slurry: the unit's product, input of the filtration unit. Olive brown, opaque. */
	public static Fluid fluidSlurry;

	/** Crude Uranium Solution: the filtration unit's product (uranium still dissolved with impurities), input of
	 * the chemical separation. Dark amber. */
	public static Fluid fluidCrudeSolution;

	/** Wet Mineral Residue: the filtration unit's solid residue (the cake left between the plates). */
	public static Item itemResidue;

	/** Filter: the filter cloths of the Filtration Unit (28 per machine, worn by every batch). */
	public static ItemFilter itemFilter;

	/** Items of the next steps of the line; no machine uses them yet. */
	public static Item itemQuicklime, itemCake, itemPrecipitatingAgent, itemWetConcentrate, itemYellowcake;

	/** Impure Agglomerate: heavy building block made of neutralisation cake or filter residue. */
	public static Block blockAgglomerate;
	public static Item itemAgglomerate;

	/** Fluids of the chemical separation, the effluent treatment and the dryer (catalogue); only tanks and pipes
	 * use them for now. */
	public static Fluid fluidSolvent, fluidStrippingSolution, fluidPurifiedSolution, fluidEffluent, fluidSludge, fluidDepletedEffluent, fluidCondensate;

	/** Every fluid of the mod, for texture stitching and the hazard notes. */
	public static Fluid[] fluids;

	/** The Filtration Unit's steps: close, release, filter (loop), open; the pressurising is the pump sound. */
	public static SoundEvent soundFpClose, soundFpRelease, soundFpFilterLoop, soundFpOpen;

	/** Running sound of the Chemical Processing Unit: start, loop and stop. */
	public static SoundEvent soundStart, soundLoop, soundStop;

	/** The Chemical Separation Unit's mixers: start, loop and stop. */
	public static SoundEvent soundCsMixerStart, soundCsMixerLoop, soundCsMixerStop;

	/** The pump every machine plays while fluid moves inside it (see {@link immersivenuclear.common.block.PumpSound}). */
	public static SoundEvent soundPumpStart, soundPumpLoop, soundPumpStop;

	@SubscribeEvent
	public static void registerSounds(RegistryEvent.Register<SoundEvent> event)
	{
		soundStart = sound(event, "chemical_processor_start");
		soundLoop = sound(event, "chemical_processor_loop");
		soundStop = sound(event, "chemical_processor_stop");
		soundFpClose = sound(event, "filter_press_close");
		soundFpRelease = sound(event, "filter_press_release");
		soundFpFilterLoop = sound(event, "filter_press_filter_loop");
		soundFpOpen = sound(event, "filter_press_open");
		soundCsMixerStart = sound(event, "chemical_separator_mixer_start");
		soundCsMixerLoop = sound(event, "chemical_separator_mixer_loop");
		soundCsMixerStop = sound(event, "chemical_separator_mixer_stop");
		soundPumpStart = sound(event, "pump_start");
		soundPumpLoop = sound(event, "pump_loop");
		soundPumpStop = sound(event, "pump_stop");
	}

	static SoundEvent sound(RegistryEvent.Register<SoundEvent> event, String name)
	{
		ResourceLocation id = new ResourceLocation(ImmersiveNuclear.MODID, name);
		SoundEvent e = new SoundEvent(id).setRegistryName(id);
		event.getRegistry().register(e);
		return e;
	}

	/** Fluids have to exist before blocks and items are registered (buckets look them up). */
	public static void preInit()
	{
		fluidSlurry = fluid("uranium_slurry", 1400, 3000);
		fluidCrudeSolution = fluid("crude_uranium_solution", 1100, 1200);
		fluidSolvent = fluid("extraction_solvent", 800, 1100);
		fluidStrippingSolution = fluid("stripping_solution", 1050, 1000);
		fluidPurifiedSolution = fluid("purified_uranium_solution", 1080, 1100);
		fluidEffluent = fluid("chemical_effluent", 1100, 1200);
		fluidSludge = fluid("neutralized_sludge", 1500, 3500);
		fluidDepletedEffluent = fluid("depleted_effluent", 1050, 1100);
		fluidCondensate = fluid("process_condensate", 1000, 1000);
		fluids = new Fluid[]{fluidSlurry, fluidCrudeSolution, fluidSolvent, fluidStrippingSolution, fluidPurifiedSolution, fluidEffluent, fluidSludge, fluidDepletedEffluent, fluidCondensate};
	}

	static Item item(RegistryEvent.Register<Item> event, String name)
	{
		Item i = new Item().setRegistryName(ImmersiveNuclear.MODID, name).setTranslationKey(ImmersiveNuclear.MODID + "." + name).setCreativeTab(TAB);
		event.getRegistry().register(i);
		return i;
	}

	/** A fluid with its textures in blocks/fluid/<name>_still/_flow and a universal bucket. */
	static Fluid fluid(String name, int density, int viscosity)
	{
		Fluid f = new Fluid(name, new ResourceLocation(ImmersiveNuclear.MODID, "blocks/fluid/" + name + "_still"),
				new ResourceLocation(ImmersiveNuclear.MODID, "blocks/fluid/" + name + "_flow")).setDensity(density).setViscosity(viscosity).setUnlocalizedName(ImmersiveNuclear.MODID + "." + name);
		if(!FluidRegistry.registerFluid(f))
			f = FluidRegistry.getFluid(name);
		FluidRegistry.addBucketForFluid(f);
		return f;
	}

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> event)
	{
		itemChemicalProcessor = new Item().setRegistryName(ImmersiveNuclear.MODID, "chemical_processor").setTranslationKey(ImmersiveNuclear.MODID + ".chemical_processor").setCreativeTab(TAB);
		event.getRegistry().register(itemChemicalProcessor);
		itemFilterPress = new Item().setRegistryName(ImmersiveNuclear.MODID, "filter_press").setTranslationKey(ImmersiveNuclear.MODID + ".filter_press").setCreativeTab(TAB);
		event.getRegistry().register(itemFilterPress);
		itemChemicalSeparator = new Item().setRegistryName(ImmersiveNuclear.MODID, "chemical_separator").setTranslationKey(ImmersiveNuclear.MODID + ".chemical_separator").setCreativeTab(TAB);
		event.getRegistry().register(itemChemicalSeparator);
		itemReagent = new Item().setRegistryName(ImmersiveNuclear.MODID, "processing_reagent").setTranslationKey(ImmersiveNuclear.MODID + ".processing_reagent").setCreativeTab(TAB);
		event.getRegistry().register(itemReagent);
		itemResidue = new Item().setRegistryName(ImmersiveNuclear.MODID, "wet_mineral_residue").setTranslationKey(ImmersiveNuclear.MODID + ".wet_mineral_residue").setCreativeTab(TAB);
		event.getRegistry().register(itemResidue);
		itemFilter = new ItemFilter();
		event.getRegistry().register(itemFilter);
		itemQuicklime = item(event, "quicklime");
		itemCake = item(event, "neutralization_cake");
		itemPrecipitatingAgent = item(event, "precipitating_agent");
		itemWetConcentrate = item(event, "wet_uranium_concentrate");
		itemYellowcake = item(event, "yellowcake");
		itemAgglomerate = new ItemBlock(blockAgglomerate).setRegistryName(blockAgglomerate.getRegistryName());
		event.getRegistry().register(itemAgglomerate);
	}

	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> event)
	{
		blockChemicalProcessor = new BlockChemicalProcessor();
		event.getRegistry().register(blockChemicalProcessor);
		GameRegistry.registerTileEntity(TileChemicalProcessor.class, new ResourceLocation(ImmersiveNuclear.MODID, "chemical_processor"));
		blockFilterPress = new BlockFilterPress();
		event.getRegistry().register(blockFilterPress);
		GameRegistry.registerTileEntity(TileFilterPress.class, new ResourceLocation(ImmersiveNuclear.MODID, "filter_press"));
		blockChemicalSeparator = new BlockChemicalSeparator();
		event.getRegistry().register(blockChemicalSeparator);
		GameRegistry.registerTileEntity(TileChemicalSeparator.class, new ResourceLocation(ImmersiveNuclear.MODID, "chemical_separator"));
		// Heavy like IE's concrete (provisional values)
		blockAgglomerate = new Block(Material.ROCK).setHardness(3f).setResistance(60f).setRegistryName(ImmersiveNuclear.MODID, "impure_agglomerate")
				.setTranslationKey(ImmersiveNuclear.MODID + ".impure_agglomerate").setCreativeTab(TAB);
		blockAgglomerate.setHarvestLevel("pickaxe", 1);
		event.getRegistry().register(blockAgglomerate);
	}

	public static void init()
	{
		MultiblockHandler.registerMultiblock(MultiblockChemicalProcessor.instance);
		MultiblockHandler.registerMultiblock(MultiblockFilterPress.instance);
		MultiblockHandler.registerMultiblock(MultiblockChemicalSeparator.instance);
		mixerRecipes();
		cokeOvenRecipes();
	}

	// Recipes in IE's machines (provisional amounts)
	public static final int SOLVENT_PER_MIX = 1000, STRIPPING_PER_MIX = 1000, SLUDGE_PER_MIX = 1000;
	public static final int SOLVENT_MIX_ENERGY = 3200, STRIPPING_MIX_ENERGY = 1600, SLUDGE_MIX_ENERGY = 1600;
	public static final int QUICKLIME_TICKS = 900;

	/** Quicklime: diorite (or limestone, when a mod adds it) burnt in IE's Coke Oven. */
	static void cokeOvenRecipes()
	{
		CokeOvenRecipe.addRecipe(new ItemStack(itemQuicklime), "stoneDiorite", QUICKLIME_TICKS, 0);
		if(!net.minecraftforge.oredict.OreDictionary.getOres("stoneLimestone").isEmpty())
			CokeOvenRecipe.addRecipe(new ItemStack(itemQuicklime, 2), "stoneLimestone", QUICKLIME_TICKS, 0);
	}

	/**
	 * IE's Mixer makes the two liquids the separation needs: extraction solvent from biodiesel and saltpeter, stripping
	 * solution from water and saltpeter; and neutralizes the effluent with quicklime into sludge. Biodiesel comes from
	 * IE's refinery; without it (no IE fluid) the solvent recipe is left out.
	 */
	static void mixerRecipes()
	{
		Fluid biodiesel = FluidRegistry.getFluid("biodiesel");
		if(biodiesel != null)
			MixerRecipe.addRecipe(new FluidStack(fluidSolvent, SOLVENT_PER_MIX), new FluidStack(biodiesel, SOLVENT_PER_MIX), new Object[]{"dustSaltpeter"}, SOLVENT_MIX_ENERGY);
		else
			ImmersiveNuclear.logger.warn("No biodiesel fluid: the extraction solvent has no Mixer recipe");
		MixerRecipe.addRecipe(new FluidStack(fluidStrippingSolution, STRIPPING_PER_MIX), new FluidStack(FluidRegistry.WATER, STRIPPING_PER_MIX), new Object[]{"dustSaltpeter"}, STRIPPING_MIX_ENERGY);
		// Neutralizing the effluent (or what is left of it after the Metal Extractor) with quicklime
		for(Fluid effluent : new Fluid[]{fluidEffluent, fluidDepletedEffluent})
			MixerRecipe.addRecipe(new FluidStack(fluidSludge, SLUDGE_PER_MIX), new FluidStack(effluent, SLUDGE_PER_MIX), new Object[]{new ItemStack(itemQuicklime)}, SLUDGE_MIX_ENERGY);
	}

	/** Registry names used up to 0.1.0-alpha.17, mapped to the current ones so existing worlds keep their items. */
	static final Map<String, String> RENAMED = new HashMap<>();

	static
	{
		RENAMED.put("reagente_processamento", "processing_reagent");
		RENAMED.put("rejeito_mineral_umido", "wet_mineral_residue");
		RENAMED.put("filtro", "filter");
		RENAMED.put("cal", "quicklime");
		RENAMED.put("torta_neutralizacao", "neutralization_cake");
		RENAMED.put("agente_precipitante", "precipitating_agent");
		RENAMED.put("concentrado_umido_uranio", "wet_uranium_concentrate");
		RENAMED.put("aglomerado_impuro", "impure_agglomerate");
	}

	@SubscribeEvent
	public static void remapItems(RegistryEvent.MissingMappings<Item> event)
	{
		for(RegistryEvent.MissingMappings.Mapping<Item> m : event.getAllMappings())
		{
			String name = m.key.getNamespace().equals(ImmersiveNuclear.MODID) ? RENAMED.get(m.key.getPath()) : null;
			if(name != null)
				m.remap(ForgeRegistries.ITEMS.getValue(new ResourceLocation(ImmersiveNuclear.MODID, name)));
		}
	}

	@SubscribeEvent
	public static void remapBlocks(RegistryEvent.MissingMappings<Block> event)
	{
		for(RegistryEvent.MissingMappings.Mapping<Block> m : event.getAllMappings())
		{
			String name = m.key.getNamespace().equals(ImmersiveNuclear.MODID) ? RENAMED.get(m.key.getPath()) : null;
			if(name != null)
				m.remap(ForgeRegistries.BLOCKS.getValue(new ResourceLocation(ImmersiveNuclear.MODID, name)));
		}
	}
}
