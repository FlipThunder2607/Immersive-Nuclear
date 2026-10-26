package immersivenuclear.common;

import java.util.HashMap;
import java.util.Map;

import blusunrize.immersiveengineering.api.MultiblockHandler;
import blusunrize.immersiveengineering.api.crafting.CokeOvenRecipe;
import blusunrize.immersiveengineering.api.crafting.CrusherRecipe;
import blusunrize.immersiveengineering.api.crafting.MixerRecipe;
import blusunrize.immersiveengineering.api.energy.wires.WireApi;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.block.BlockChemicalProcessor;
import immersivenuclear.common.block.BlockChemicalSeparator;
import immersivenuclear.common.block.BlockEngineering;
import immersivenuclear.common.block.BlockLinkConnector;
import immersivenuclear.common.block.BlockFilterPress;
import immersivenuclear.common.block.BlockMetalExtractor;
import immersivenuclear.common.block.BlockPrecipitationUnit;
import immersivenuclear.common.block.TileChemicalProcessor;
import immersivenuclear.common.block.TileChemicalSeparator;
import immersivenuclear.common.block.TileFilterPress;
import immersivenuclear.common.block.TileLinkConnector;
import immersivenuclear.common.block.TileMetalExtractor;
import immersivenuclear.common.block.TilePrecipitationUnit;
import immersivenuclear.common.item.ItemElectrode;
import immersivenuclear.common.item.ItemFilter;
import immersivenuclear.common.item.ItemLinkCable;
import immersivenuclear.common.wire.LinkWireType;
import immersivenuclear.common.multiblock.MultiblockChemicalProcessor;
import immersivenuclear.common.multiblock.MultiblockChemicalSeparator;
import immersivenuclear.common.multiblock.MultiblockFilterPress;
import immersivenuclear.common.multiblock.MultiblockMetalExtractor;
import immersivenuclear.common.multiblock.MultiblockPrecipitationUnit;
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
	public static BlockMetalExtractor blockMetalExtractor;
	public static BlockPrecipitationUnit blockPrecipitationUnit;

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

	/** The same for the Metal Extractor. */
	public static Item itemMetalExtractor;

	/** The same for the Precipitation Unit. */
	public static Item itemPrecipitationUnit;

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
	public static Item itemQuicklime, itemCake, itemPrecipitatingAgent, itemWetConcentrate, itemYellowcake, itemNeutralizedResidue;

	/** Mixed Metal Deposit: scraped off the Metal Extractor's plates; crushed in IE's Crusher into nuggets. */
	public static Item itemMixedDeposit;

	/** The Metal Extractor's electrode plates: steel, lead-silver, electrum (see {@link ItemElectrode}). */
	public static ItemElectrode itemElectrodeSteel, itemElectrodeLeadSilver, itemElectrodeElectrum;

	/** Impure Agglomerate: heavy building block made of neutralisation cake or filter residue. */
	public static Block blockAgglomerate;
	public static Item itemAgglomerate;

	/** Engineering blocks, casings of the next machines, in the order of {@link BlockEngineering#KINDS} (no recipe yet). */
	public static Block[] blockEngineering;
	public static Item[] itemEngineering;

	/** Parts of the computers (in development): only the items for now, no recipe and no use yet. */
	public static final String[] COMPUTER_PARTS = {"electronic_module", "relay_module", "core_memory", "punched_card", "punched_paper_tape",
			"disk_pack", "link_cable", "magnetic_tape", "continuous_form_paper"};
	public static Item[] itemComputerParts;
	/** Link connector and link relay of the computers' data network (blocks with IE's connector model). */
	public static Block blockLinkConnector, blockLinkRelay;
	public static Item itemLinkConnector, itemLinkRelay;

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

	/** Metal Extractor: the plates energised (start, loop, stop); one clip per movement of the platform and the tray. */
	public static SoundEvent soundMePlatesStart, soundMePlatesLoop, soundMePlatesStop, soundMePlatformUp, soundMePlatformDown, soundMeTrayIn, soundMeTrayOut, soundMeCleanerDown, soundMeCleanerUp;

	/** Precipitation Unit: the screw dewaterer dragging the slurry up (start, loop, stop); its agitator is the CS mixer. */
	public static SoundEvent soundPuScrewStart, soundPuScrewLoop, soundPuScrewStop;

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
		soundMePlatesStart = sound(event, "metal_extractor_plates_start");
		soundMePlatesLoop = sound(event, "metal_extractor_plates_loop");
		soundMePlatesStop = sound(event, "metal_extractor_plates_stop");
		soundMePlatformUp = sound(event, "metal_extractor_platform_up");
		soundMePlatformDown = sound(event, "metal_extractor_platform_down");
		soundMeTrayIn = sound(event, "metal_extractor_tray_in");
		soundMeTrayOut = sound(event, "metal_extractor_tray_out");
		soundMeCleanerDown = sound(event, "metal_extractor_cleaner_down");
		soundMeCleanerUp = sound(event, "metal_extractor_cleaner_up");
		soundPuScrewStart = sound(event, "precipitation_screw_start");
		soundPuScrewLoop = sound(event, "precipitation_screw_loop");
		soundPuScrewStop = sound(event, "precipitation_screw_stop");
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
		// The link cable's wire type has to exist before any world loads: IE reads saved wires by name and turns
		// an unknown name into LV copper (this also files it under its category, like IE's own wires)
		WireApi.registerWireType(LinkWireType.INSTANCE);
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
		itemMetalExtractor = new Item().setRegistryName(ImmersiveNuclear.MODID, "metal_extractor").setTranslationKey(ImmersiveNuclear.MODID + ".metal_extractor").setCreativeTab(TAB);
		event.getRegistry().register(itemMetalExtractor);
		itemPrecipitationUnit = new Item().setRegistryName(ImmersiveNuclear.MODID, "precipitation_unit").setTranslationKey(ImmersiveNuclear.MODID + ".precipitation_unit").setCreativeTab(TAB);
		event.getRegistry().register(itemPrecipitationUnit);
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
		itemNeutralizedResidue = item(event, "neutralized_residue");
		itemMixedDeposit = item(event, "mixed_metal_deposit");
		// Life in batches, share of the deposit (6 steel plates = the base 6 deposits), share of the recovery's energy
		itemElectrodeSteel = new ItemElectrode("electrode_steel", 1, 32, 1F, 1F, new float[]{1F, 1F, 1F});
		itemElectrodeLeadSilver = new ItemElectrode("electrode_lead_silver", 2, 64, 7 / 6F, 0.9F, new float[]{0.78F, 0.82F, 0.95F});
		itemElectrodeElectrum = new ItemElectrode("electrode_electrum", 3, 128, 8 / 6F, 0.75F, new float[]{1F, 0.86F, 0.5F});
		event.getRegistry().register(itemElectrodeSteel);
		event.getRegistry().register(itemElectrodeLeadSilver);
		event.getRegistry().register(itemElectrodeElectrum);
		itemAgglomerate = new ItemBlock(blockAgglomerate).setRegistryName(blockAgglomerate.getRegistryName());
		event.getRegistry().register(itemAgglomerate);
		itemEngineering = new Item[blockEngineering.length];
		for(int i = 0; i < blockEngineering.length; i++)
		{
			itemEngineering[i] = new ItemBlock(blockEngineering[i]).setRegistryName(blockEngineering[i].getRegistryName());
			event.getRegistry().register(itemEngineering[i]);
		}
		itemComputerParts = new Item[COMPUTER_PARTS.length];
		for(int i = 0; i < COMPUTER_PARTS.length; i++)
			itemComputerParts[i] = i == 6 ? null : item(event, COMPUTER_PARTS[i]);
		itemComputerParts[6] = new ItemLinkCable();
		event.getRegistry().register(itemComputerParts[6]);
		itemLinkConnector = new ItemBlock(blockLinkConnector).setRegistryName(blockLinkConnector.getRegistryName());
		event.getRegistry().register(itemLinkConnector);
		itemLinkRelay = new ItemBlock(blockLinkRelay).setRegistryName(blockLinkRelay.getRegistryName());
		event.getRegistry().register(itemLinkRelay);
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
		blockMetalExtractor = new BlockMetalExtractor();
		event.getRegistry().register(blockMetalExtractor);
		GameRegistry.registerTileEntity(TileMetalExtractor.class, new ResourceLocation(ImmersiveNuclear.MODID, "metal_extractor"));
		blockPrecipitationUnit = new BlockPrecipitationUnit();
		event.getRegistry().register(blockPrecipitationUnit);
		GameRegistry.registerTileEntity(TilePrecipitationUnit.class, new ResourceLocation(ImmersiveNuclear.MODID, "precipitation_unit"));
		// Heavy like IE's concrete (provisional values)
		blockAgglomerate = new Block(Material.ROCK).setHardness(3f).setResistance(60f).setRegistryName(ImmersiveNuclear.MODID, "impure_agglomerate")
				.setTranslationKey(ImmersiveNuclear.MODID + ".impure_agglomerate").setCreativeTab(TAB);
		blockAgglomerate.setHarvestLevel("pickaxe", 1);
		event.getRegistry().register(blockAgglomerate);
		blockEngineering = new Block[BlockEngineering.KINDS.length];
		for(int i = 0; i < blockEngineering.length; i++)
		{
			blockEngineering[i] = new BlockEngineering(BlockEngineering.KINDS[i]);
			event.getRegistry().register(blockEngineering[i]);
		}
		blockLinkConnector = new BlockLinkConnector("link_connector", false);
		event.getRegistry().register(blockLinkConnector);
		GameRegistry.registerTileEntity(TileLinkConnector.class, new ResourceLocation(ImmersiveNuclear.MODID, "link_connector"));
		blockLinkRelay = new BlockLinkConnector("link_relay", true);
		event.getRegistry().register(blockLinkRelay);
	}

	public static void init()
	{
		MultiblockHandler.registerMultiblock(MultiblockChemicalProcessor.instance);
		MultiblockHandler.registerMultiblock(MultiblockFilterPress.instance);
		MultiblockHandler.registerMultiblock(MultiblockChemicalSeparator.instance);
		MultiblockHandler.registerMultiblock(MultiblockMetalExtractor.instance);
		MultiblockHandler.registerMultiblock(MultiblockPrecipitationUnit.instance);
		mixerRecipes();
		cokeOvenRecipes();
		crusherRecipes();
	}

	// The Wet Mineral Residue, neutralized with quicklime, is crushed in IE's Crusher for what the leaching left in it
	public static final int RESIDUE_CRUSH_ENERGY = 3200;
	public static final float RESIDUE_IRON = .15F, RESIDUE_CLAY = .10F, RESIDUE_URANIUM = .03F;
	// The Mixed Metal Deposit: one per 2000 mB of effluent, so each chance is twice the old Recovery Unit's per 1000 mB
	public static final int DEPOSIT_CRUSH_ENERGY = 4800, SLAG_META = 7;
	public static final float DEPOSIT_IRON = .20F, DEPOSIT_ALUMINUM = .12F, DEPOSIT_COPPER = .08F, DEPOSIT_LEAD = .06F, DEPOSIT_NICKEL = .04F,
			DEPOSIT_URANIUM = .02F, DEPOSIT_CLAY = .30F;

	/**
	 * Neutralized residue: mostly gangue (sand), with some iron minerals, clay and the little uranium the leaching did
	 * not dissolve. The uranium stays a small bonus, so reworking residue never beats the line itself.
	 */
	static void crusherRecipes()
	{
		// The Mixed Metal Deposit: slag (the plates' scale) and, by chance, the metals of the effluent
		CrusherRecipe.addRecipe(new ItemStack(blusunrize.immersiveengineering.common.IEContent.itemMaterial, 1, SLAG_META), new ItemStack(itemMixedDeposit), DEPOSIT_CRUSH_ENERGY)
				.addToSecondaryOutput(new ItemStack(net.minecraft.init.Items.IRON_NUGGET), DEPOSIT_IRON, "nuggetAluminum", DEPOSIT_ALUMINUM, "nuggetCopper", DEPOSIT_COPPER,
						"nuggetLead", DEPOSIT_LEAD, "nuggetNickel", DEPOSIT_NICKEL, "nuggetUranium", DEPOSIT_URANIUM, new ItemStack(net.minecraft.init.Items.CLAY_BALL), DEPOSIT_CLAY);
		CrusherRecipe.addRecipe(new ItemStack(net.minecraft.init.Blocks.SAND), new ItemStack(itemNeutralizedResidue), RESIDUE_CRUSH_ENERGY)
				.addToSecondaryOutput(new ItemStack(net.minecraft.init.Items.IRON_NUGGET), RESIDUE_IRON, new ItemStack(net.minecraft.init.Items.CLAY_BALL), RESIDUE_CLAY,
						"nuggetUranium", RESIDUE_URANIUM);
	}

	// Recipes in IE's machines (provisional amounts)
	public static final int SOLVENT_PER_MIX = 1000, STRIPPING_PER_MIX = 1000, SLUDGE_PER_MIX = 1000;
	public static final int SOLVENT_MIX_ENERGY = 3200, STRIPPING_MIX_ENERGY = 1600, SLUDGE_MIX_ENERGY = 1600;
	public static final int QUICKLIME_TICKS = 900;
	public static final int CONCRETE_PER_MIX = 500, CONCRETE_MIX_ENERGY = 3200;

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
		// IE's concrete with the neutralization cake in place of the clay (IE: 500 mB water, 2 sand, clay, gravel, 3200 IF)
		MixerRecipe.addRecipe(new FluidStack(blusunrize.immersiveengineering.common.IEContent.fluidConcrete, CONCRETE_PER_MIX), new FluidStack(FluidRegistry.WATER, CONCRETE_PER_MIX),
				new Object[]{"sand", "sand", new ItemStack(itemCake), "gravel"}, CONCRETE_MIX_ENERGY);
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
