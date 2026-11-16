package immersivenuclear.common;

import static immersivenuclear.common.INContent.*;

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
import immersivenuclear.common.block.BlockDeconversionUnit;
import immersivenuclear.common.block.BlockPrecipitationUnit;
import immersivenuclear.common.block.TileChemicalProcessor;
import immersivenuclear.common.block.TileChemicalSeparator;
import immersivenuclear.common.block.TileFilterPress;
import immersivenuclear.common.block.TileLinkConnector;
import immersivenuclear.common.block.TileMetalExtractor;
import immersivenuclear.common.block.TileIndustrialDryer;
import immersivenuclear.common.block.TileSulfuricAcidUnit;
import immersivenuclear.common.block.TileHFKiln;
import immersivenuclear.common.block.TileDeconversionUnit;
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
import immersivenuclear.common.multiblock.MultiblockDeconversionUnit;
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

/** The mod's sounds (split from INContent, 15/11/2026): the fields the machines play and their registration. */
@Mod.EventBusSubscriber(modid = ImmersiveNuclear.MODID)
public final class INSounds
{
	private INSounds()
	{
	}

	/** The Enrichment Unit's sounds, each {start, loop, stop}; the cascade's by size (2, 4, 6 banks). */
	public static net.minecraft.util.SoundEvent[][] soundEnrichmentCascade = new net.minecraft.util.SoundEvent[3][];
	public static net.minecraft.util.SoundEvent[] soundEnrichmentConverter, soundEnrichmentCompressor;

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
	/** The Deconversion Unit: the rotary kiln, the electric furnace, the hydrolysis reactor, the candle filters (the kiln's
	 * motor and gears use the HF Kiln's drive). */
	public static SoundEvent soundDeconvKilnStart, soundDeconvKilnLoop, soundDeconvKilnStop, soundDeconvFurnaceStart, soundDeconvFurnaceLoop,
			soundDeconvFurnaceStop, soundDeconvReactorStart, soundDeconvReactorLoop, soundDeconvReactorStop, soundDeconvFiltersStart,
			soundDeconvFiltersLoop, soundDeconvFiltersStop;
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
		soundDeconvKilnStart = sound(event, "deconversion_kiln_start");
		soundDeconvKilnLoop = sound(event, "deconversion_kiln_loop");
		soundDeconvKilnStop = sound(event, "deconversion_kiln_stop");
		soundDeconvFurnaceStart = sound(event, "deconversion_furnace_start");
		soundDeconvFurnaceLoop = sound(event, "deconversion_furnace_loop");
		soundDeconvFurnaceStop = sound(event, "deconversion_furnace_stop");
		soundDeconvReactorStart = sound(event, "deconversion_reactor_start");
		soundDeconvReactorLoop = sound(event, "deconversion_reactor_loop");
		soundDeconvReactorStop = sound(event, "deconversion_reactor_stop");
		soundDeconvFiltersStart = sound(event, "deconversion_filters_start");
		soundDeconvFiltersLoop = sound(event, "deconversion_filters_loop");
		soundDeconvFiltersStop = sound(event, "deconversion_filters_stop");
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
		for(int i = 0; i < 3; i++)
			soundEnrichmentCascade[i] = soundSet(event, "enrichment_cascade_" + (i * 2 + 2));
		soundEnrichmentConverter = soundSet(event, "enrichment_converter");
		soundEnrichmentCompressor = soundSet(event, "enrichment_compressor");
		soundConversionScrubberStart = sound(event, "conversion_scrubber_start");
		soundConversionScrubberLoop = sound(event, "conversion_scrubber_loop");
		soundConversionScrubberStop = sound(event, "conversion_scrubber_stop");
	}

	static net.minecraft.util.SoundEvent[] soundSet(RegistryEvent.Register<SoundEvent> event, String name)
	{
		return new net.minecraft.util.SoundEvent[]{sound(event, name + "_start"), sound(event, name + "_loop"), sound(event, name + "_stop")};
	}

	static SoundEvent sound(RegistryEvent.Register<SoundEvent> event, String name)
	{
		ResourceLocation id = new ResourceLocation(ImmersiveNuclear.MODID, name);
		SoundEvent e = new SoundEvent(id).setRegistryName(id);
		event.getRegistry().register(e);
		return e;
	}
}
