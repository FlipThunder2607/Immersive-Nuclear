package immersivenuclear.common;

import immersivenuclear.common.block.BlockAgglomerateSlab;
import immersivenuclear.common.block.BlockAgglomerateStairs;
import immersivenuclear.common.block.BlockConcentrateDrum;
import immersivenuclear.common.block.BlockLeadedGlass;
import immersivenuclear.common.block.TileConcentrateDrum;
import immersivenuclear.common.item.ItemConcentrateDrum;
import java.util.HashMap;
import java.util.Map;

import blusunrize.immersiveengineering.api.MultiblockHandler;
import blusunrize.immersiveengineering.api.crafting.ArcFurnaceRecipe;
import blusunrize.immersiveengineering.api.crafting.CokeOvenRecipe;
import blusunrize.immersiveengineering.api.crafting.BottlingMachineRecipe;
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
import immersivenuclear.common.block.BlockIndustrialDryer;
import immersivenuclear.common.block.BlockSulfuricAcidUnit;
import immersivenuclear.common.block.BlockHFKiln;
import immersivenuclear.common.block.BlockPrecipitationUnit;
import immersivenuclear.common.block.TileChemicalProcessor;
import immersivenuclear.common.block.TileChemicalSeparator;
import immersivenuclear.common.block.TileFilterPress;
import immersivenuclear.common.block.TileLinkConnector;
import immersivenuclear.common.block.TileMetalExtractor;
import immersivenuclear.common.block.TileIndustrialDryer;
import immersivenuclear.common.block.TileSulfuricAcidUnit;
import immersivenuclear.common.block.TileHFKiln;
import immersivenuclear.common.block.TilePrecipitationUnit;
import immersivenuclear.common.item.ItemElectrode;
import immersivenuclear.common.item.ItemFilter;
import immersivenuclear.common.item.ItemLinkCable;
import immersivenuclear.common.wire.LinkWireType;
import immersivenuclear.common.multiblock.MultiblockChemicalProcessor;
import immersivenuclear.common.multiblock.MultiblockChemicalSeparator;
import immersivenuclear.common.multiblock.MultiblockFilterPress;
import immersivenuclear.common.multiblock.MultiblockMetalExtractor;
import immersivenuclear.common.multiblock.MultiblockIndustrialDryer;
import immersivenuclear.common.multiblock.MultiblockSulfuricAcidUnit;
import immersivenuclear.common.multiblock.MultiblockHFKiln;
import immersivenuclear.common.multiblock.MultiblockPrecipitationUnit;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemSlab;
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
	public static BlockIndustrialDryer blockIndustrialDryer;
	public static BlockSulfuricAcidUnit blockSulfuricAcidUnit;
	public static BlockHFKiln blockHFKiln;
	public static immersivenuclear.common.block.BlockElectrolyzer blockElectrolyzer;
	public static immersivenuclear.common.block.BlockConversionUnit blockConversionUnit;
	public static BlockConcentrateDrum blockConcentrateDrum;
	public static BlockLeadedGlass blockLeadedGlass;

	/** The single-block devices of the computers: only models for now, placeable on the ground, no function (the user, 04/11/2026). Registry names. */
	public static final String[] COMPUTER_DEVICES = {"card_reader", "card_punch", "paper_tape_unit", "crt_terminal", "chart_recorder", "wall_panel"};
	public static Block[] blockComputerDevices;
	public static Item[] itemComputerDevices;
	/** The Conduit: the computers' cable tray, which adapts to its surroundings and connects by itself (BlockConduit). */
	public static immersivenuclear.common.block.BlockConduit blockConduit;
	/** The data cable coil block, a building block of the computers. */
	public static immersivenuclear.common.block.BlockDataCoil blockDataCoil;
	public static Item itemDataCoil;
	/** The computers' multiblocks (only their models for now), in the order of ComputerStructure.ALL. The items are icons, like the machines'. */
	public static immersivenuclear.common.block.BlockComputerMachine[] blockComputerMachines;
	public static Item[] itemComputerMachines;
	public static Item itemConduit;

	/** The modules of the computers' panels (console and wall panel): {id, kind (0 command, 1 indicator, 2 universal), cells wide, cells high}. Only items for now.
	 * Kept in step with ferramentas/icones_modulos_painel.py. */
	public static final Object[][] PANEL_MODULES = {
			{"button", 0, 3, 3}, {"lit_button", 0, 3, 3}, {"latching_button", 0, 3, 3}, {"lever", 0, 3, 5}, {"selector_switch", 0, 4, 4}, {"key_switch", 0, 4, 4},
			{"knob", 0, 4, 4}, {"slider", 0, 3, 10}, {"emergency_stop", 0, 6, 6}, {"scram_switch", 0, 5, 5}, {"numeric_keypad", 0, 6, 8}, {"redstone_input", 0, 3, 3},
			{"lamp", 1, 2, 2}, {"alarm_lamp", 1, 3, 3}, {"siren", 1, 4, 4}, {"bell", 1, 3, 3}, {"pointer_gauge", 1, 6, 6}, {"column_level", 1, 3, 10}, {"led_bar", 1, 10, 2},
			{"screen", 1, 8, 6}, {"digits", 1, 6, 3}, {"rod_position", 1, 6, 6}, {"neutron_flux", 1, 6, 6}, {"synchroscope", 1, 6, 6}, {"redstone_output", 1, 3, 3},
			{"lit_window", 2, 6, 3}, {"label", 2, 8, 2}, {"color_plate", 2, 4, 4}};
	public static Item[] itemPanelModules;

	/** The mod's creative tab; JEI also lists what is in it. Laid out in groups, one after the other (the user,
	 * 01/11): structure, minerals, line 1, the extractor's plates, line 2, building blocks, engineering parts, computer
	 * parts, then the buckets of our fluids; anything not in a group goes at the end. */
	public static final CreativeTabs TAB = new CreativeTabs(ImmersiveNuclear.MODID)
	{
		@Override
		public ItemStack createIcon()
		{
			return new ItemStack(itemChemicalProcessor);
		}

		@Override
		public void displayAllRelevantItems(net.minecraft.util.NonNullList<ItemStack> list)
		{
			java.util.Set<Item> done = new java.util.HashSet<>();
			java.util.List<Item> structure = new java.util.ArrayList<>(java.util.Arrays.asList(itemEngineering));
			structure.add(itemLeadedGlass);
			Item[][] groups = {
					structure.toArray(new Item[0]),
					{itemFluoriteOre, itemFluorite, itemFluoriteDust},
					{itemReagent, itemQuicklime, itemPrecipitatingAgent, itemFilter, itemWetConcentrate, itemYellowcake, itemConcentrateDrum,
							itemResidue, itemNeutralizedResidue, itemCake, itemMixedDeposit},
					{itemElectrodeSteel, itemElectrodeLeadSilver, itemElectrodeElectrum},
					{itemGypsum, itemFluorinationAsh, itemDepletedUraniumOxide, itemDepletedUraniumIngot},
					{itemAgglomerate, itemAgglomerateBlock, itemAgglomerateTile, itemAgglomerateBricks, itemAgglomerateStairs, itemAgglomerateSlab,
							itemPlaster, itemPlasterTile, itemPlasterBricks, itemPlasterStairs, itemPlasterSlab},
					{itemRemoteManipulator, itemPrecisionBearing},
			};
			for(Item[] group : groups)
				group(list, group, done);
			java.util.List<Item> computer = new java.util.ArrayList<>(java.util.Arrays.asList(itemComputerParts));
			computer.add(itemLinkConnector);
			computer.add(itemLinkRelay);
			if(itemConduit != null)
				computer.add(itemConduit);
		if(itemDataCoil != null)
			computer.add(itemDataCoil);
			if(itemComputerDevices != null)
				computer.addAll(java.util.Arrays.asList(itemComputerDevices));
			if(itemPanelModules != null)
				computer.addAll(java.util.Arrays.asList(itemPanelModules));
			group(list, computer.toArray(new Item[0]), done);
			// The buckets of our fluids
			int start = list.size();
			if(fluids != null)
				for(Fluid f : fluids)
					if(f != null && ImmersiveNuclear.MODID.equals(f.getStill().getNamespace()))
					{
						ItemStack bucket = net.minecraftforge.fluids.FluidUtil.getFilledBucket(new FluidStack(f, net.minecraftforge.fluids.Fluid.BUCKET_VOLUME));
						if(!bucket.isEmpty())
							list.add(bucket);
					}
			// Anything else of ours in this tab
			start = list.size();
			for(Item i : net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS)
				if(!done.contains(i) && i.getCreativeTab() == this)
					i.getSubItems(this, list);
		}

		void group(net.minecraft.util.NonNullList<ItemStack> list, Item[] items, java.util.Set<Item> done)
		{
			int start = list.size();
			for(Item i : items)
				if(i != null && done.add(i))
					i.getSubItems(this, list);
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
	/** The same for the Industrial Dryer. */
	public static Item itemIndustrialDryer;
	public static Item itemSulfuricAcidUnit;
	/** The same for the Hydrofluoric Acid Kiln. */
	public static Item itemHFKiln;

	/** The same for the Electrolyzer. */
	public static Item itemElectrolyzer;
	/** The same for the Conversion Unit. */
	public static Item itemConversionUnit;
	public static Item itemConcentrateDrum;

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
	/** Parts of the engineering blocks' recipes: lead glass (a block, from IE's Arc Furnace), a hot cell's tongs, a precision bearing. */
	public static Item itemLeadedGlass, itemRemoteManipulator, itemPrecisionBearing;
	public static Item itemQuicklime, itemCake, itemPrecipitatingAgent, itemWetConcentrate, itemYellowcake, itemNeutralizedResidue;

	/** Mixed Metal Deposit: scraped off the Metal Extractor's plates; crushed in IE's Crusher into nuggets. */
	public static Item itemMixedDeposit;

	/** The Metal Extractor's electrode plates: steel, lead-silver, electrum (see {@link ItemElectrode}). */
	public static ItemElectrode itemElectrodeSteel, itemElectrodeLeadSilver, itemElectrodeElectrum;

	/** Impure Agglomerate: heavy building block made of neutralisation cake or filter residue. */
	public static Block blockAgglomerate;
	/** Fluorite (line 2): the ore, its crystal and its dust, under the ore dictionary names other mods use. */
	public static Block blockFluoriteOre;
	public static Item itemFluoriteOre, itemFluorite, itemFluoriteDust;
	/** Gypsum, the Hydrofluoric Acid Kiln's solid by-product (dustGypsum). */
	public static Item itemGypsum;
	/** Line 2's uranium by-products: the Conversion Unit's fluorination ash (impurities' fluorides and a little uranium
	 * that did not react, for the Chemical Processing Unit), the Deconversion's depleted uranium oxide and its ingot. */
	public static Item itemFluorinationAsh, itemDepletedUraniumOxide, itemDepletedUraniumIngot;
	/** Building blocks of gypsum plaster, as the agglomerate's: the block (4 gypsum), its tile, its bricks and the bricks'
	 * slab and stairs. */
	public static Block blockPlaster, blockPlasterTile, blockPlasterBricks, blockPlasterStairs;
	public static BlockSlab blockPlasterSlab, blockPlasterSlabDouble;
	public static Item itemPlaster, itemPlasterTile, itemPlasterBricks, itemPlasterSlab, itemPlasterStairs;
	public static Item itemAgglomerate;
	/** The raw agglomerate fired in a furnace (concrete-like), its tile (as IE's concrete tile), its bricks, and the bricks'
	 * stairs and slab. */
	public static Block blockAgglomerateBlock, blockAgglomerateTile, blockAgglomerateBricks, blockAgglomerateStairs;
	public static BlockSlab blockAgglomerateSlab, blockAgglomerateSlabDouble;
	public static Item itemAgglomerateBlock, itemAgglomerateTile, itemAgglomerateBricks, itemAgglomerateStairs, itemAgglomerateSlab;

	/** Engineering blocks, casings of the next machines, in the order of {@link BlockEngineering#KINDS} (no recipe yet). */
	public static Block[] blockEngineering;
	public static Item[] itemEngineering;

	/** Parts of the computers (in development): only the items for now, no recipe and no use yet. */
	public static final String[] COMPUTER_PARTS = {"electronic_module", "relay_module", "core_memory", "punched_card", "punched_paper_tape",
			"disk_pack", "link_cable", "power_wire", "magnetic_tape", "continuous_form_paper"};
	public static Item[] itemComputerParts;
	/** Link connector and link relay of the computers' data network (blocks with IE's connector model). */
	public static Block blockLinkConnector, blockLinkRelay;
	public static Item itemLinkConnector, itemLinkRelay;

	/** Fluids of the chemical separation, the effluent treatment and the dryer (catalogue); only tanks and pipes
	 * use them for now. */
	public static Fluid fluidSolvent, fluidStrippingSolution, fluidPurifiedSolution, fluidEffluent, fluidSludge, fluidDepletedEffluent, fluidCondensate;
	/** Line 2: the Sulfuric Acid Unit's acid and steam (shared with other mods by name: theirs is used when they have one). */
	public static Fluid fluidSulfuricAcid, fluidSteam, fluidHydrofluoricAcid, fluidHydrogen, fluidOxygen;
	/** The uranium hexafluorides of line 2: natural (from the conversion), enriched in three grades and depleted. */
	public static Fluid fluidUF6, fluidUF6Low, fluidUF6Medium, fluidUF6High, fluidUF6Depleted;
	/** What the Conversion Unit's fluorination leaves: spent hydrofluoric acid with the fluorides of the impurities. */
	public static Fluid fluidFluorinatedEffluent;
	/** mB of fluorinated effluent that, with one quicklime, give one fluorite dust. */
	public static final int FLUORINATED_EFFLUENT_PER_DUST = 500;

	/** Every fluid of the mod, for texture stitching and the hazard notes. */
	public static Fluid[] fluids;
	/** The placed fluids (BlockINFluid), for the fluids this mod registered itself; another mod's keeps its own. */
	public static java.util.List<Block> fluidBlocks = new java.util.ArrayList<>();

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

	/** The Industrial Dryer's steps: load (rotary valve), heat (heaters, loop), dry (paddles, loop), discharge (valve). */
	/** The Sulfuric Acid Unit: the burner with its blower, the acid pump, and the steam drum's relief valve venting. */
	public static SoundEvent soundAcidBurnerStart, soundAcidBurnerLoop, soundAcidBurnerStop, soundAcidPumpStart, soundAcidPumpLoop, soundAcidPumpStop,
			soundAcidVentStart, soundAcidVentLoop, soundAcidVentStop, soundKilnDriveStart, soundKilnDriveLoop, soundKilnDriveStop,
			soundKilnHeaterStart, soundKilnHeaterLoop, soundKilnHeaterStop, soundKilnDrumStart, soundKilnDrumLoop, soundKilnDrumStop,
			soundElectrolyzerCellsStart, soundElectrolyzerCellsLoop, soundElectrolyzerCellsStop, soundElectrolyzerRectifierStart,
			soundElectrolyzerRectifierLoop, soundElectrolyzerRectifierStop, soundElectrolyzerPumpStart, soundElectrolyzerPumpLoop,
			soundElectrolyzerPumpStop, soundElectrolyzerFeedStart, soundElectrolyzerFeedLoop, soundElectrolyzerFeedStop, soundElectrolyzerVent,
			soundConversionReactorStart, soundConversionReactorLoop, soundConversionReactorStop, soundConversionFluorinatorStart,
			soundConversionFluorinatorLoop, soundConversionFluorinatorStop, soundConversionCellsStart, soundConversionCellsLoop,
			soundConversionCellsStop, soundConversionColdTrapStart, soundConversionColdTrapLoop, soundConversionColdTrapStop, soundConversionColdTrapDrain,
			soundConversionScrubberStart, soundConversionScrubberLoop, soundConversionScrubberStop;
	public static SoundEvent soundDryerLoad, soundDryerHeatStart, soundDryerHeatLoop, soundDryerHeatStop, soundDryerDryStart, soundDryerDryLoop,
			soundDryerDryStop, soundDryerDischarge, soundDryerFillStart, soundDryerFillLoop, soundDryerFillStop;

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
		soundDryerLoad = sound(event, "industrial_dryer_load");
		soundDryerHeatStart = sound(event, "industrial_dryer_heat_start");
		soundDryerHeatLoop = sound(event, "industrial_dryer_heat_loop");
		soundDryerHeatStop = sound(event, "industrial_dryer_heat_stop");
		soundDryerDryStart = sound(event, "industrial_dryer_dry_start");
		soundDryerDryLoop = sound(event, "industrial_dryer_dry_loop");
		soundDryerDryStop = sound(event, "industrial_dryer_dry_stop");
		soundDryerDischarge = sound(event, "industrial_dryer_discharge");
		soundDryerFillStart = sound(event, "industrial_dryer_fill_start");
		soundDryerFillLoop = sound(event, "industrial_dryer_fill_loop");
		soundDryerFillStop = sound(event, "industrial_dryer_fill_stop");
		soundAcidBurnerStart = sound(event, "sulfuric_acid_unit_burner_start");
		soundAcidBurnerLoop = sound(event, "sulfuric_acid_unit_burner_loop");
		soundAcidBurnerStop = sound(event, "sulfuric_acid_unit_burner_stop");
		soundAcidPumpStart = sound(event, "sulfuric_acid_unit_pump_start");
		soundAcidPumpLoop = sound(event, "sulfuric_acid_unit_pump_loop");
		soundAcidPumpStop = sound(event, "sulfuric_acid_unit_pump_stop");
		soundAcidVentStart = sound(event, "sulfuric_acid_unit_vent_start");
		soundAcidVentLoop = sound(event, "sulfuric_acid_unit_vent_loop");
		soundAcidVentStop = sound(event, "sulfuric_acid_unit_vent_stop");
		soundKilnDriveStart = sound(event, "hf_kiln_drive_start");
		soundKilnDriveLoop = sound(event, "hf_kiln_drive_loop");
		soundKilnDriveStop = sound(event, "hf_kiln_drive_stop");
		soundKilnHeaterStart = sound(event, "hf_kiln_heater_start");
		soundKilnHeaterLoop = sound(event, "hf_kiln_heater_loop");
		soundKilnHeaterStop = sound(event, "hf_kiln_heater_stop");
		soundKilnDrumStart = sound(event, "hf_kiln_drum_start");
		soundKilnDrumLoop = sound(event, "hf_kiln_drum_loop");
		soundKilnDrumStop = sound(event, "hf_kiln_drum_stop");
		soundElectrolyzerCellsStart = sound(event, "electrolyzer_cells_start");
		soundElectrolyzerCellsLoop = sound(event, "electrolyzer_cells_loop");
		soundElectrolyzerCellsStop = sound(event, "electrolyzer_cells_stop");
		soundElectrolyzerRectifierStart = sound(event, "electrolyzer_rectifier_start");
		soundElectrolyzerRectifierLoop = sound(event, "electrolyzer_rectifier_loop");
		soundElectrolyzerRectifierStop = sound(event, "electrolyzer_rectifier_stop");
		soundElectrolyzerPumpStart = sound(event, "electrolyzer_pump_start");
		soundElectrolyzerPumpLoop = sound(event, "electrolyzer_pump_loop");
		soundElectrolyzerPumpStop = sound(event, "electrolyzer_pump_stop");
		soundElectrolyzerFeedStart = sound(event, "electrolyzer_feed_start");
		soundElectrolyzerFeedLoop = sound(event, "electrolyzer_feed_loop");
		soundElectrolyzerFeedStop = sound(event, "electrolyzer_feed_stop");
		soundElectrolyzerVent = sound(event, "electrolyzer_vent");
		soundConversionReactorStart = sound(event, "conversion_reactor_start");
		soundConversionReactorLoop = sound(event, "conversion_reactor_loop");
		soundConversionReactorStop = sound(event, "conversion_reactor_stop");
		soundConversionFluorinatorStart = sound(event, "conversion_fluorinator_start");
		soundConversionFluorinatorLoop = sound(event, "conversion_fluorinator_loop");
		soundConversionFluorinatorStop = sound(event, "conversion_fluorinator_stop");
		soundConversionCellsStart = sound(event, "conversion_cells_start");
		soundConversionCellsLoop = sound(event, "conversion_cells_loop");
		soundConversionCellsStop = sound(event, "conversion_cells_stop");
		soundConversionColdTrapStart = sound(event, "conversion_coldtrap_start");
		soundConversionColdTrapLoop = sound(event, "conversion_coldtrap_loop");
		soundConversionColdTrapStop = sound(event, "conversion_coldtrap_stop");
		soundConversionColdTrapDrain = sound(event, "conversion_coldtrap_drain");
		soundConversionScrubberStart = sound(event, "conversion_scrubber_start");
		soundConversionScrubberLoop = sound(event, "conversion_scrubber_loop");
		soundConversionScrubberStop = sound(event, "conversion_scrubber_stop");
	}

	static SoundEvent sound(RegistryEvent.Register<SoundEvent> event, String name)
	{
		ResourceLocation id = new ResourceLocation(ImmersiveNuclear.MODID, name);
		SoundEvent e = new SoundEvent(id).setRegistryName(id);
		event.getRegistry().register(e);
		return e;
	}

	/**
	 * Steam, as early as a mod can register anything (its constructor): the first mod to register a fluid name makes it
	 * that name's default in new worlds, and other mods (turbines, boilers) take steam by name, so ours is the one they
	 * use there. A world keeps the default it was made with (Forge saves it in level.dat), so in an older world another
	 * mod's steam may stay the default; the machine's interface draws its steam with our texture anyway.
	 */
	public static void constructFluids()
	{
		fluidSteam = fluid("steam", -100, 200);
		if(FluidRegistry.getFluid("steam") == fluidSteam)
			fluidSteam.setGaseous(true);
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
		fluidSulfuricAcid = fluid("sulfuric_acid", 1830, 2600);
		// thin and dense like the real one; shared by name with other mods (Immersive Intelligence has it too)
		fluidHydrofluoricAcid = fluid("hydrofluoric_acid", 1150, 900);
		// the Electrolyzer's gases: light, rising; shared by name with other mods (Immersive Intelligence, Mekanism...)
		fluidHydrogen = gas("hydrogen", -900, 100);
		fluidOxygen = gas("oxygen", -200, 200);
		// the hexafluorides: a heavy gas that stays low and slides along, so a dense, thin fluid
		fluidUF6 = fluid("uranium_hexafluoride", 3000, 600);
		fluidUF6Low = fluid("uranium_hexafluoride_low", 3000, 600);
		fluidUF6Medium = fluid("uranium_hexafluoride_medium", 3000, 600);
		fluidUF6High = fluid("uranium_hexafluoride_high", 3000, 600);
		fluidUF6Depleted = fluid("depleted_uranium_hexafluoride", 3000, 600);
		fluidFluorinatedEffluent = fluid("fluorinated_effluent", 1200, 1100);
		if(fluidSteam == null)
			constructFluids();
		fluids = new Fluid[]{fluidSlurry, fluidCrudeSolution, fluidSolvent, fluidStrippingSolution, fluidPurifiedSolution, fluidEffluent, fluidSludge, fluidDepletedEffluent, fluidCondensate, fluidSulfuricAcid, fluidSteam, fluidHydrofluoricAcid, fluidHydrogen, fluidOxygen,
			fluidUF6, fluidUF6Low, fluidUF6Medium, fluidUF6High, fluidUF6Depleted, fluidFluorinatedEffluent};
	}

	/** A building block of the finished agglomerate: heavy like the raw one and IE's concrete (provisional values). */
	static Block agglomerate(String name)
	{
		Block b = new Block(Material.ROCK).setHardness(3f).setResistance(60f).setRegistryName(ImmersiveNuclear.MODID, name)
				.setTranslationKey(ImmersiveNuclear.MODID + "." + name).setCreativeTab(TAB);
		b.setHarvestLevel("pickaxe", 1);
		return b;
	}

	/** A gypsum plaster building block: lighter than the agglomerate (provisional values). */
	static Block plaster(String name)
	{
		Block b = new Block(Material.ROCK).setHardness(1.5f).setResistance(10f).setRegistryName(ImmersiveNuclear.MODID, name)
				.setTranslationKey(ImmersiveNuclear.MODID + "." + name).setCreativeTab(TAB);
		b.setHarvestLevel("pickaxe", 0);
		return b;
	}

	static Item item(RegistryEvent.Register<Item> event, String name)
	{
		Item i = new Item().setRegistryName(ImmersiveNuclear.MODID, name).setTranslationKey(ImmersiveNuclear.MODID + "." + name).setCreativeTab(TAB);
		event.getRegistry().register(i);
		return i;
	}

	/** A gas: a fluid that rises; gaseous only if it is ours (another mod's keeps its own settings). */
	static Fluid gas(String name, int density, int viscosity)
	{
		Fluid f = fluid(name, density, viscosity);
		if(ImmersiveNuclear.MODID.equals(f.getStill().getNamespace()))
			f.setGaseous(true);
		return f;
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
		// The machine items are only icons (JEI's catalysts and pages, the 3D model): no creative tab, so they never reach
		// an inventory (the user, 01/11); picking a formed machine gives the block the part replaced
		itemChemicalProcessor = new Item().setRegistryName(ImmersiveNuclear.MODID, "chemical_processor").setTranslationKey(ImmersiveNuclear.MODID + ".chemical_processor");
		event.getRegistry().register(itemChemicalProcessor);
		itemFilterPress = new Item().setRegistryName(ImmersiveNuclear.MODID, "filter_press").setTranslationKey(ImmersiveNuclear.MODID + ".filter_press");
		event.getRegistry().register(itemFilterPress);
		itemChemicalSeparator = new Item().setRegistryName(ImmersiveNuclear.MODID, "chemical_separator").setTranslationKey(ImmersiveNuclear.MODID + ".chemical_separator");
		event.getRegistry().register(itemChemicalSeparator);
		itemMetalExtractor = new Item().setRegistryName(ImmersiveNuclear.MODID, "metal_extractor").setTranslationKey(ImmersiveNuclear.MODID + ".metal_extractor");
		event.getRegistry().register(itemMetalExtractor);
		itemPrecipitationUnit = new Item().setRegistryName(ImmersiveNuclear.MODID, "precipitation_unit").setTranslationKey(ImmersiveNuclear.MODID + ".precipitation_unit");
		event.getRegistry().register(itemPrecipitationUnit);
		itemIndustrialDryer = new Item().setRegistryName(ImmersiveNuclear.MODID, "industrial_dryer").setTranslationKey(ImmersiveNuclear.MODID + ".industrial_dryer");
		event.getRegistry().register(itemIndustrialDryer);
		itemSulfuricAcidUnit = new Item().setRegistryName(ImmersiveNuclear.MODID, "sulfuric_acid_unit").setTranslationKey(ImmersiveNuclear.MODID + ".sulfuric_acid_unit");
		event.getRegistry().register(itemSulfuricAcidUnit);
		itemHFKiln = new Item().setRegistryName(ImmersiveNuclear.MODID, "hf_kiln").setTranslationKey(ImmersiveNuclear.MODID + ".hf_kiln");
		event.getRegistry().register(itemHFKiln);
		itemElectrolyzer = new Item().setRegistryName(ImmersiveNuclear.MODID, "electrolyzer").setTranslationKey(ImmersiveNuclear.MODID + ".electrolyzer");
		event.getRegistry().register(itemElectrolyzer);
		itemConversionUnit = new Item().setRegistryName(ImmersiveNuclear.MODID, "conversion_unit").setTranslationKey(ImmersiveNuclear.MODID + ".conversion_unit");
		event.getRegistry().register(itemConversionUnit);
		// The order of registration is the order of JEI's list and of the creative tab: the machines (above), then each
		// line's materials in the order the process meets them, then building blocks, engineering and computer parts
		// Line 1: supplies, the main path, residues and their treatment, the Metal Extractor's
		itemReagent = new Item().setRegistryName(ImmersiveNuclear.MODID, "processing_reagent").setTranslationKey(ImmersiveNuclear.MODID + ".processing_reagent").setCreativeTab(TAB);
		event.getRegistry().register(itemReagent);
		itemFilter = new ItemFilter();
		event.getRegistry().register(itemFilter);
		itemQuicklime = item(event, "quicklime");
		itemPrecipitatingAgent = item(event, "precipitating_agent");
		itemWetConcentrate = item(event, "wet_uranium_concentrate");
		itemYellowcake = item(event, "yellowcake");
		itemConcentrateDrum = new ItemConcentrateDrum(blockConcentrateDrum);
		event.getRegistry().register(itemConcentrateDrum);
		itemResidue = new Item().setRegistryName(ImmersiveNuclear.MODID, "wet_mineral_residue").setTranslationKey(ImmersiveNuclear.MODID + ".wet_mineral_residue").setCreativeTab(TAB);
		event.getRegistry().register(itemResidue);
		itemNeutralizedResidue = item(event, "neutralized_residue");
		itemCake = item(event, "neutralization_cake");
		// Life in batches, share of the deposit (6 steel plates = the base 6 deposits), share of the recovery's energy
		itemElectrodeSteel = new ItemElectrode("electrode_steel", 1, 32, 1F, 1F, new float[]{1F, 1F, 1F});
		itemElectrodeLeadSilver = new ItemElectrode("electrode_lead_silver", 2, 64, 7 / 6F, 0.9F, new float[]{0.78F, 0.82F, 0.95F});
		itemElectrodeElectrum = new ItemElectrode("electrode_electrum", 3, 128, 8 / 6F, 0.75F, new float[]{1F, 0.86F, 0.5F});
		event.getRegistry().register(itemElectrodeSteel);
		event.getRegistry().register(itemElectrodeLeadSilver);
		event.getRegistry().register(itemElectrodeElectrum);
		itemMixedDeposit = item(event, "mixed_metal_deposit");
		// Line 2: fluorite
		itemFluoriteOre = new ItemBlock(blockFluoriteOre).setRegistryName(blockFluoriteOre.getRegistryName());
		event.getRegistry().register(itemFluoriteOre);
		itemFluorite = item(event, "fluorite");
		itemFluoriteDust = item(event, "fluorite_dust");
		itemGypsum = item(event, "gypsum");
		itemFluorinationAsh = item(event, "fluorination_ash");
		itemDepletedUraniumOxide = item(event, "depleted_uranium_oxide");
		itemDepletedUraniumIngot = item(event, "depleted_uranium_ingot");
		// Building blocks: the agglomerate from the residues, and lead glass
		itemAgglomerate = new ItemBlock(blockAgglomerate).setRegistryName(blockAgglomerate.getRegistryName());
		event.getRegistry().register(itemAgglomerate);
		itemAgglomerateBlock = new ItemBlock(blockAgglomerateBlock).setRegistryName(blockAgglomerateBlock.getRegistryName());
		itemAgglomerateTile = new ItemBlock(blockAgglomerateTile).setRegistryName(blockAgglomerateTile.getRegistryName());
		itemAgglomerateBricks = new ItemBlock(blockAgglomerateBricks).setRegistryName(blockAgglomerateBricks.getRegistryName());
		itemAgglomerateStairs = new ItemBlock(blockAgglomerateStairs).setRegistryName(blockAgglomerateStairs.getRegistryName());
		itemAgglomerateSlab = new ItemSlab(blockAgglomerateSlab, blockAgglomerateSlab, blockAgglomerateSlabDouble).setRegistryName(blockAgglomerateSlab.getRegistryName());
		event.getRegistry().registerAll(itemAgglomerateBlock, itemAgglomerateTile, itemAgglomerateBricks, itemAgglomerateStairs, itemAgglomerateSlab);
		itemPlaster = new ItemBlock(blockPlaster).setRegistryName(blockPlaster.getRegistryName());
		itemPlasterTile = new ItemBlock(blockPlasterTile).setRegistryName(blockPlasterTile.getRegistryName());
		itemPlasterBricks = new ItemBlock(blockPlasterBricks).setRegistryName(blockPlasterBricks.getRegistryName());
		itemPlasterStairs = new ItemBlock(blockPlasterStairs).setRegistryName(blockPlasterStairs.getRegistryName());
		itemPlasterSlab = new ItemSlab(blockPlasterSlab, blockPlasterSlab, blockPlasterSlabDouble).setRegistryName(blockPlasterSlab.getRegistryName());
		event.getRegistry().registerAll(itemPlaster, itemPlasterTile, itemPlasterBricks, itemPlasterStairs, itemPlasterSlab);
		itemLeadedGlass = new ItemBlock(blockLeadedGlass).setRegistryName(blockLeadedGlass.getRegistryName());
		event.getRegistry().register(itemLeadedGlass);
		// Engineering blocks and their parts
		itemEngineering = new Item[blockEngineering.length];
		for(int i = 0; i < blockEngineering.length; i++)
		{
			itemEngineering[i] = new ItemBlock(blockEngineering[i]).setRegistryName(blockEngineering[i].getRegistryName());
			event.getRegistry().register(itemEngineering[i]);
		}
		itemRemoteManipulator = item(event, "remote_manipulator");
		itemPrecisionBearing = item(event, "precision_bearing");
		// Computer parts
		itemComputerParts = new Item[COMPUTER_PARTS.length];
		for(int i = 0; i < COMPUTER_PARTS.length; i++)
			itemComputerParts[i] = i == 6 || i == 7 ? null : item(event, COMPUTER_PARTS[i]);
		itemComputerParts[6] = new ItemLinkCable();
		event.getRegistry().register(itemComputerParts[6]);
		itemComputerParts[7] = new immersivenuclear.common.item.ItemPowerWire();
		event.getRegistry().register(itemComputerParts[7]);
		itemLinkConnector = new ItemBlock(blockLinkConnector).setRegistryName(blockLinkConnector.getRegistryName());
		event.getRegistry().register(itemLinkConnector);
		itemLinkRelay = new ItemBlock(blockLinkRelay).setRegistryName(blockLinkRelay.getRegistryName());
		event.getRegistry().register(itemLinkRelay);
		itemDataCoil = new ItemBlock(blockDataCoil).setRegistryName(blockDataCoil.getRegistryName());
		event.getRegistry().register(itemDataCoil);
		itemConduit = new ItemBlock(blockConduit).setRegistryName(blockConduit.getRegistryName());
		event.getRegistry().register(itemConduit);
		itemComputerDevices = new Item[COMPUTER_DEVICES.length];
		for(int i = 0; i < COMPUTER_DEVICES.length; i++)
		{
			itemComputerDevices[i] = new ItemBlock(blockComputerDevices[i]).setRegistryName(blockComputerDevices[i].getRegistryName());
			event.getRegistry().register(itemComputerDevices[i]);
		}
		itemComputerMachines = new Item[immersivenuclear.common.multiblock.ComputerStructure.ALL.length];
		for(immersivenuclear.common.multiblock.ComputerStructure s : immersivenuclear.common.multiblock.ComputerStructure.ALL)
		{
			itemComputerMachines[s.index] = new Item().setRegistryName(ImmersiveNuclear.MODID, s.name).setTranslationKey(ImmersiveNuclear.MODID + "." + s.name);
			event.getRegistry().register(itemComputerMachines[s.index]);
		}
		itemPanelModules = new Item[PANEL_MODULES.length];
		for(int i = 0; i < PANEL_MODULES.length; i++)
		{
			Object[] m = PANEL_MODULES[i];
			itemPanelModules[i] = new immersivenuclear.common.item.ItemPanelModule((String)m[0], (Integer)m[1], (Integer)m[2], (Integer)m[3]);
			event.getRegistry().register(itemPanelModules[i]);
		}
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
		blockIndustrialDryer = new BlockIndustrialDryer();
		event.getRegistry().register(blockIndustrialDryer);
		GameRegistry.registerTileEntity(TileIndustrialDryer.class, new ResourceLocation(ImmersiveNuclear.MODID, "industrial_dryer"));
		blockSulfuricAcidUnit = new BlockSulfuricAcidUnit();
		event.getRegistry().register(blockSulfuricAcidUnit);
		GameRegistry.registerTileEntity(TileSulfuricAcidUnit.class, new ResourceLocation(ImmersiveNuclear.MODID, "sulfuric_acid_unit"));
		blockHFKiln = new BlockHFKiln();
		event.getRegistry().register(blockHFKiln);
		GameRegistry.registerTileEntity(TileHFKiln.class, new ResourceLocation(ImmersiveNuclear.MODID, "hf_kiln"));
		blockElectrolyzer = new immersivenuclear.common.block.BlockElectrolyzer();
		event.getRegistry().register(blockElectrolyzer);
		GameRegistry.registerTileEntity(immersivenuclear.common.block.TileElectrolyzer.class, new ResourceLocation(ImmersiveNuclear.MODID, "electrolyzer"));
		blockConversionUnit = new immersivenuclear.common.block.BlockConversionUnit();
		event.getRegistry().register(blockConversionUnit);
		GameRegistry.registerTileEntity(immersivenuclear.common.block.TileConversionUnit.class, new ResourceLocation(ImmersiveNuclear.MODID, "conversion_unit"));
		blockLeadedGlass = new BlockLeadedGlass();
		event.getRegistry().register(blockLeadedGlass);
		// The computers' single-block devices (selection boxes in blocks, for a device facing north; the two with a glass window are translucent)
		double[][] boxes = {{0.05, 0, 0.1, 0.95, 0.98, 1}, {0.05, 0, 0.1, 0.95, 0.98, 1}, {0.05, 0, 0.1, 0.95, 1, 1}, {0.035, 0, 0.045, 0.965, 1, 1}, {0.05, 0, 0.08, 0.95, 0.85, 1}, {0, 0, 0, 1, 1, 1}};
		boolean[] glass = {false, true, false, false, true, false};
		blockComputerDevices = new Block[COMPUTER_DEVICES.length];
		for(int i = 0; i < COMPUTER_DEVICES.length; i++)
		{
			double[] b = boxes[i];
			blockComputerDevices[i] = new immersivenuclear.common.block.BlockComputerDevice(COMPUTER_DEVICES[i], new net.minecraft.util.math.AxisAlignedBB(b[0], b[1], b[2], b[3], b[4], b[5]), glass[i]);
			if(COMPUTER_DEVICES[i].equals("wall_panel"))
				((immersivenuclear.common.block.BlockComputerDevice)blockComputerDevices[i]).noConduitEntry();
			event.getRegistry().register(blockComputerDevices[i]);
		}
		blockDataCoil = new immersivenuclear.common.block.BlockDataCoil();
		event.getRegistry().register(blockDataCoil);
		blockConduit = new immersivenuclear.common.block.BlockConduit();
		event.getRegistry().register(blockConduit);
		GameRegistry.registerTileEntity(immersivenuclear.common.block.TileConduit.class, new ResourceLocation(ImmersiveNuclear.MODID, "conduit"));
		blockComputerMachines = new immersivenuclear.common.block.BlockComputerMachine[immersivenuclear.common.multiblock.ComputerStructure.ALL.length];
		for(immersivenuclear.common.multiblock.ComputerStructure s : immersivenuclear.common.multiblock.ComputerStructure.ALL)
		{
			blockComputerMachines[s.index] = new immersivenuclear.common.block.BlockComputerMachine(s);
			event.getRegistry().register(blockComputerMachines[s.index]);
		}
		GameRegistry.registerTileEntity(immersivenuclear.common.block.TileComputerMachine.class, new ResourceLocation(ImmersiveNuclear.MODID, "mainframe"));
		blockConcentrateDrum = new BlockConcentrateDrum();
		event.getRegistry().register(blockConcentrateDrum);
		GameRegistry.registerTileEntity(TileConcentrateDrum.class, new ResourceLocation(ImmersiveNuclear.MODID, "concentrate_drum"));
		// Heavy like IE's concrete (provisional values)
		blockAgglomerate = new Block(Material.ROCK).setHardness(3f).setResistance(60f).setRegistryName(ImmersiveNuclear.MODID, "impure_agglomerate")
				.setTranslationKey(ImmersiveNuclear.MODID + ".impure_agglomerate").setCreativeTab(TAB);
		blockAgglomerate.setHarvestLevel("pickaxe", 1);
		event.getRegistry().register(blockAgglomerate);
		blockFluoriteOre = new immersivenuclear.common.block.BlockFluoriteOre();
		event.getRegistry().register(blockFluoriteOre);
		blockAgglomerateBlock = agglomerate("agglomerate_block");
		blockAgglomerateTile = agglomerate("agglomerate_tile");
		blockAgglomerateBricks = agglomerate("agglomerate_bricks");
		blockAgglomerateStairs = new BlockAgglomerateStairs(blockAgglomerateBricks.getDefaultState());
		blockAgglomerateSlab = new BlockAgglomerateSlab.Half();
		blockAgglomerateSlabDouble = new BlockAgglomerateSlab.Double();
		event.getRegistry().registerAll(blockAgglomerateBlock, blockAgglomerateTile, blockAgglomerateBricks, blockAgglomerateStairs, blockAgglomerateSlab, blockAgglomerateSlabDouble);
		// Plaster: lighter than the agglomerate (provisional values)
		blockPlaster = plaster("plaster_block");
		blockPlasterTile = plaster("plaster_tile");
		blockPlasterBricks = plaster("plaster_bricks");
		blockPlasterStairs = new immersivenuclear.common.block.BlockPlasterStairs(blockPlasterBricks.getDefaultState());
		blockPlasterSlab = new immersivenuclear.common.block.BlockPlasterSlab.Half();
		blockPlasterSlabDouble = new immersivenuclear.common.block.BlockPlasterSlab.Double();
		event.getRegistry().registerAll(blockPlaster, blockPlasterTile, blockPlasterBricks, blockPlasterStairs, blockPlasterSlab, blockPlasterSlabDouble);
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
		for(Fluid f : fluids)
			if(f.getBlock() == null && ImmersiveNuclear.MODID.equals(f.getStill().getNamespace()))
			{
				Block b = new immersivenuclear.common.block.BlockINFluid(f);
				event.getRegistry().register(b);
				fluidBlocks.add(b);
			}
	}

	public static void init()
	{
		MultiblockHandler.registerMultiblock(MultiblockChemicalProcessor.instance);
		MultiblockHandler.registerMultiblock(MultiblockFilterPress.instance);
		MultiblockHandler.registerMultiblock(MultiblockChemicalSeparator.instance);
		MultiblockHandler.registerMultiblock(MultiblockMetalExtractor.instance);
		MultiblockHandler.registerMultiblock(MultiblockPrecipitationUnit.instance);
		MultiblockHandler.registerMultiblock(MultiblockIndustrialDryer.instance);
		MultiblockHandler.registerMultiblock(MultiblockSulfuricAcidUnit.instance);
		MultiblockHandler.registerMultiblock(MultiblockHFKiln.instance);
		MultiblockHandler.registerMultiblock(immersivenuclear.common.multiblock.MultiblockElectrolyzer.instance);
		MultiblockHandler.registerMultiblock(immersivenuclear.common.multiblock.MultiblockConversionUnit.instance);
		for(immersivenuclear.common.multiblock.MultiblockComputer m : immersivenuclear.common.multiblock.MultiblockComputer.ALL)
			MultiblockHandler.registerMultiblock(m);
		fluorite();
		mixerRecipes();
		cokeOvenRecipes();
		crusherRecipes();
	}

	// Fluorite: the Crusher gets more out of the ore than breaking it does (1-2 crystals), as IE's ores
	public static final int FLUORITE_ORE_DUST = 3, FLUORITE_CRUSH_ENERGY = 4000;
	public static final int FLUORITE_EXCAVATOR_WEIGHT = 15;
	public static final float FLUORITE_EXCAVATOR_FAIL = .1F;

	/**
	 * Fluorite: ore dictionary names shared with other mods (Immersive Intelligence has fluorite too, so either mod's
	 * works in the other's machines), the Crusher, the furnace, the Overworld's veins and a vein of IE's Excavator:
	 * fluorite with the galena and quartz it is found with.
	 */
	static void fluorite()
	{
		net.minecraftforge.oredict.OreDictionary.registerOre("oreFluorite", blockFluoriteOre);
		net.minecraftforge.oredict.OreDictionary.registerOre("gemFluorite", itemFluorite);
		net.minecraftforge.oredict.OreDictionary.registerOre("dustFluorite", itemFluoriteDust);
		net.minecraftforge.oredict.OreDictionary.registerOre("dustGypsum", itemGypsum);
		net.minecraftforge.oredict.OreDictionary.registerOre("dustDepletedUranium", itemDepletedUraniumOxide);
		net.minecraftforge.oredict.OreDictionary.registerOre("ingotDepletedUranium", itemDepletedUraniumIngot);
		CrusherRecipe.addRecipe(new ItemStack(itemFluoriteDust, FLUORITE_ORE_DUST), "oreFluorite", FLUORITE_CRUSH_ENERGY);
		CrusherRecipe.addRecipe(new ItemStack(itemFluoriteDust), "gemFluorite", FLUORITE_CRUSH_ENERGY / 2);
		GameRegistry.addSmelting(blockFluoriteOre, new ItemStack(itemFluorite), 0.2f);
		GameRegistry.registerWorldGenerator(new immersivenuclear.common.world.WorldGenFluorite(), 0);
		if(INConfig.fluoriteExcavatorVein)
			blusunrize.immersiveengineering.api.tool.ExcavatorHandler.addMineral("Fluorite", FLUORITE_EXCAVATOR_WEIGHT, FLUORITE_EXCAVATOR_FAIL,
					new String[]{"oreFluorite", "oreLead", "gemQuartz"}, new float[]{.7F, .2F, .1F});
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
	/** Gypsum in IE's Arc Furnace with coke dust: 2 gypsum -> 2 quicklime + 1 sulfur dust (provisional). */
	public static final int GYPSUM_PER_BURN = 2, GYPSUM_QUICKLIME = 2, GYPSUM_BURN_TICKS = 200, GYPSUM_BURN_ENERGY = 256;
	/** Gypsum in IE's concrete: it sets the cement, so the batch gives half as much again (provisional). */
	public static final int GYPSUM_CONCRETE_PER_MIX = 750;

	/** Quicklime: diorite (or limestone, when a mod adds it) burnt in IE's Coke Oven. */
	static void cokeOvenRecipes()
	{
		CokeOvenRecipe.addRecipe(new ItemStack(itemQuicklime), "stoneDiorite", QUICKLIME_TICKS, 0);
		// The raw agglomerate fired into its finished, concrete-like block, as cobblestone into stone
		GameRegistry.addSmelting(blockAgglomerate, new ItemStack(blockAgglomerateBlock), 0.1f);
		// Lead glass, as it is made: glass melted with lead (provisional time and energy, like IE's alloys)
		ArcFurnaceRecipe.addRecipe(new ItemStack(itemLeadedGlass), "blockGlass", ItemStack.EMPTY, 100, 512, "dustLead");
		// The Deconversion's depleted uranium oxide reduced to metal with coke, as IE's ores (provisional time and energy)
		ArcFurnaceRecipe.addRecipe(new ItemStack(itemDepletedUraniumIngot), "dustDepletedUranium", ItemStack.EMPTY, 100, 512, "dustCoke");
		// Gypsum back to sulfur and quicklime, as the Mueller-Kuehne process: burnt with coke, the sulfur leaves (here as
		// the slag slot's sulfur dust) and quicklime stays; the sulfur goes back to the Sulfuric Acid Unit
		java.util.List<ItemStack> sulfur = net.minecraftforge.oredict.OreDictionary.getOres("dustSulfur");
		if(!sulfur.isEmpty())
			ArcFurnaceRecipe.addRecipe(new ItemStack(itemQuicklime, GYPSUM_QUICKLIME), new blusunrize.immersiveengineering.api.crafting.IngredientStack("dustGypsum", GYPSUM_PER_BURN),
					sulfur.get(0).copy(), GYPSUM_BURN_TICKS, GYPSUM_BURN_ENERGY, "dustCoke");
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
		// The Conversion Unit's effluent: lime takes the fluoride out as calcium fluoride, the fluorite dust that goes back to the HF Kiln. IE's Bottling Machine, as the Mixer only makes fluids
		BottlingMachineRecipe.addRecipe(new ItemStack(itemFluoriteDust), new ItemStack(itemQuicklime), new FluidStack(fluidFluorinatedEffluent, FLUORINATED_EFFLUENT_PER_DUST));
		// IE's concrete with the neutralization cake in place of the clay (IE: 500 mB water, 2 sand, clay, gravel, 3200 IF)
		MixerRecipe.addRecipe(new FluidStack(blusunrize.immersiveengineering.common.IEContent.fluidConcrete, CONCRETE_PER_MIX), new FluidStack(FluidRegistry.WATER, CONCRETE_PER_MIX),
				new Object[]{"sand", "sand", new ItemStack(itemCake), "gravel"}, CONCRETE_MIX_ENERGY);
		// IE's concrete with gypsum in place of the clay: gypsum is what sets real cement, so a batch gives more concrete
		MixerRecipe.addRecipe(new FluidStack(blusunrize.immersiveengineering.common.IEContent.fluidConcrete, GYPSUM_CONCRETE_PER_MIX), new FluidStack(FluidRegistry.WATER, CONCRETE_PER_MIX),
				new Object[]{"sand", "sand", "dustGypsum", "gravel"}, CONCRETE_MIX_ENERGY);
	}

	/** Registry names used up to 0.1.0, mapped to the current ones so existing worlds keep their items. */
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
