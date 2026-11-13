package immersivenuclear.client.jei;

import java.util.Collections;

import immersivenuclear.client.GuiChemicalProcessor;
import immersivenuclear.client.GuiChemicalSeparator;
import immersivenuclear.client.GuiFilterPress;
import immersivenuclear.client.GuiIndustrialDryer;
import immersivenuclear.client.GuiSulfuricAcidUnit;
import immersivenuclear.client.GuiMetalExtractor;
import immersivenuclear.client.GuiPrecipitationUnit;
import immersivenuclear.common.INContent;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

/** Found by JEI only (nothing else loads this class): recipe pages, machines, arrows of the interfaces, item notes. */
@JEIPlugin
public class INJeiPlugin implements IModPlugin
{
	@Override
	public void registerCategories(IRecipeCategoryRegistration registry)
	{
		registry.addRecipeCategories(new ChemicalProcessorCategory(registry.getJeiHelpers().getGuiHelper()));
		registry.addRecipeCategories(new FilterPressCategory(registry.getJeiHelpers().getGuiHelper()));
		registry.addRecipeCategories(new ChemicalSeparatorCategory(registry.getJeiHelpers().getGuiHelper()));
		registry.addRecipeCategories(new MetalExtractorCategory(registry.getJeiHelpers().getGuiHelper()));
		registry.addRecipeCategories(new PrecipitationUnitCategory(registry.getJeiHelpers().getGuiHelper()));
		registry.addRecipeCategories(new IndustrialDryerCategory(registry.getJeiHelpers().getGuiHelper()));
		registry.addRecipeCategories(new SulfuricAcidUnitCategory(registry.getJeiHelpers().getGuiHelper()));
		registry.addRecipeCategories(new HFKilnCategory(registry.getJeiHelpers().getGuiHelper()));
		registry.addRecipeCategories(new DeconversionUnitCategory(registry.getJeiHelpers().getGuiHelper()));
		registry.addRecipeCategories(new ConversionUnitCategory(registry.getJeiHelpers().getGuiHelper()));
		registry.addRecipeCategories(new EnrichmentUnitCategory(registry.getJeiHelpers().getGuiHelper()));
		registry.addRecipeCategories(new ElectrolyzerCategory(registry.getJeiHelpers().getGuiHelper()));
	}

	/** Liquids with recipe pages now (our machines, IE's Mixer), not the "coming later" note. */
	static boolean hasRecipes(Fluid f)
	{
		return f == INContent.fluidSolvent || f == INContent.fluidStrippingSolution || f == INContent.fluidPurifiedSolution || f == INContent.fluidEffluent
				|| f == INContent.fluidSludge || f == INContent.fluidDepletedEffluent || f == INContent.fluidCondensate
				|| f == INContent.fluidSulfuricAcid || f == INContent.fluidSteam || f == INContent.fluidHydrofluoricAcid
				|| f == INContent.fluidHydrogen || f == INContent.fluidOxygen || f == INContent.fluidUF6 || f == INContent.fluidFluorinatedEffluent
				|| f == INContent.fluidUF6Low || f == INContent.fluidUF6Medium || f == INContent.fluidUF6High || f == INContent.fluidUF6Depleted;
	}

	@Override
	public void register(IModRegistry registry)
	{
		registry.addRecipes(java.util.Arrays.asList(new ChemicalProcessorRecipe(false), new ChemicalProcessorRecipe(true),
				new ChemicalProcessorRecipe(false, true)), ChemicalProcessorCategory.UID);
		ItemStack machine = new ItemStack(INContent.itemChemicalProcessor);
		registry.addRecipeCatalyst(machine, ChemicalProcessorCategory.UID);
		// Clicking the arrow of the interface shows the recipes, like IE's machines
		registry.addRecipeClickArea(GuiChemicalProcessor.class, GuiChemicalProcessor.REACTOR_X, GuiChemicalProcessor.REACTOR_Y, GuiChemicalProcessor.REACTOR_W, GuiChemicalProcessor.REACTOR_H, ChemicalProcessorCategory.UID);
		registry.addIngredientInfo(machine, VanillaTypes.ITEM, "jei.immersivenuclear.chemical_processor.info");

		registry.addRecipes(java.util.Arrays.asList(new FilterPressRecipe(false), new FilterPressRecipe(true)), FilterPressCategory.UID);
		ItemStack press = new ItemStack(INContent.itemFilterPress);
		registry.addRecipeCatalyst(press, FilterPressCategory.UID);
		registry.addRecipeClickArea(GuiFilterPress.class, GuiFilterPress.PUMP_X, GuiFilterPress.PUMP_Y, GuiFilterPress.PUMP_W, GuiFilterPress.PUMP_H, FilterPressCategory.UID);
		registry.addRecipeClickArea(GuiFilterPress.class, GuiFilterPress.PRESS_X, GuiFilterPress.PRESS_Y, GuiFilterPress.PRESS_W, GuiFilterPress.PRESS_H, FilterPressCategory.UID);
		registry.addIngredientInfo(press, VanillaTypes.ITEM, "jei.immersivenuclear.filter_press.info");
		registry.addRecipes(Collections.singletonList(new ChemicalSeparatorRecipe()), ChemicalSeparatorCategory.UID);
		ItemStack separator = new ItemStack(INContent.itemChemicalSeparator);
		registry.addRecipeCatalyst(separator, ChemicalSeparatorCategory.UID);
		for(int[] s : GuiChemicalSeparator.SETTLERS)
			registry.addRecipeClickArea(GuiChemicalSeparator.class, s[0], s[1], s[2], s[3], ChemicalSeparatorCategory.UID);
		registry.addIngredientInfo(separator, VanillaTypes.ITEM, "jei.immersivenuclear.chemical_separator.info");
		registry.addRecipes(java.util.Arrays.asList(new MetalExtractorRecipe(INContent.itemElectrodeSteel), new MetalExtractorRecipe(INContent.itemElectrodeLeadSilver),
				new MetalExtractorRecipe(INContent.itemElectrodeElectrum)), MetalExtractorCategory.UID);
		ItemStack extractor = new ItemStack(INContent.itemMetalExtractor);
		registry.addRecipeCatalyst(extractor, MetalExtractorCategory.UID);
		// Clicking the vat of the interface shows the recipe
		registry.addRecipeClickArea(GuiMetalExtractor.class, GuiMetalExtractor.VAT_X, GuiMetalExtractor.VAT_Y, GuiMetalExtractor.VAT_W, GuiMetalExtractor.VAT_H, MetalExtractorCategory.UID);
		registry.addIngredientInfo(extractor, VanillaTypes.ITEM, "jei.immersivenuclear.metal_extractor.info");
		registry.addRecipes(Collections.singletonList(new PrecipitationUnitRecipe()), PrecipitationUnitCategory.UID);
		ItemStack precipitation = new ItemStack(INContent.itemPrecipitationUnit);
		registry.addRecipeCatalyst(precipitation, PrecipitationUnitCategory.UID);
		// Clicking the agitated tank of the interface shows the recipe
		registry.addRecipeClickArea(GuiPrecipitationUnit.class, GuiPrecipitationUnit.MIXER_X, GuiPrecipitationUnit.MIXER_Y, GuiPrecipitationUnit.MIXER_W,
				GuiPrecipitationUnit.MIXER_H, PrecipitationUnitCategory.UID);
		registry.addIngredientInfo(precipitation, VanillaTypes.ITEM, "jei.immersivenuclear.precipitation_unit.info");
		registry.addRecipes(Collections.singletonList(new IndustrialDryerRecipe()), IndustrialDryerCategory.UID);
		ItemStack dryer = new ItemStack(INContent.itemIndustrialDryer);
		registry.addRecipeCatalyst(dryer, IndustrialDryerCategory.UID);
		// Clicking the drying chamber of the interface shows the recipe
		registry.addRecipeClickArea(GuiIndustrialDryer.class, GuiIndustrialDryer.CHAMBER_X, GuiIndustrialDryer.CHAMBER_Y, GuiIndustrialDryer.CHAMBER_W,
				GuiIndustrialDryer.CHAMBER_H, IndustrialDryerCategory.UID);
		registry.addIngredientInfo(dryer, VanillaTypes.ITEM, "jei.immersivenuclear.industrial_dryer.info");
		// Sulfur in any form: one cycle per dust, nine in one long cycle per block (only the forms some mod has)
		java.util.List<SulfuricAcidUnitRecipe> acid = new java.util.ArrayList<>();
		for(boolean block : new boolean[]{false, true})
			if(new SulfuricAcidUnitRecipe(block).hasSulfur())
				acid.add(new SulfuricAcidUnitRecipe(block));
		registry.addRecipes(acid, SulfuricAcidUnitCategory.UID);
		ItemStack acidUnit = new ItemStack(INContent.itemSulfuricAcidUnit);
		registry.addRecipeCatalyst(acidUnit, SulfuricAcidUnitCategory.UID);
		// Clicking the converter of the interface shows the recipes
		registry.addRecipeClickArea(GuiSulfuricAcidUnit.class, GuiSulfuricAcidUnit.CONV_X, GuiSulfuricAcidUnit.CONV_Y, GuiSulfuricAcidUnit.CONV_W,
				GuiSulfuricAcidUnit.CONV_H, SulfuricAcidUnitCategory.UID);
		registry.addIngredientInfo(acidUnit, VanillaTypes.ITEM, "jei.immersivenuclear.sulfuric_acid_unit.info");
		registry.addIngredientInfo(new FluidStack(INContent.fluidSteam, 1000), VanillaTypes.FLUID, "jei.immersivenuclear.steam.info");
		registry.addRecipes(java.util.Arrays.asList(new EnrichmentUnitCategory.Recipe(2), new EnrichmentUnitCategory.Recipe(4), new EnrichmentUnitCategory.Recipe(6)), EnrichmentUnitCategory.UID);
		for(Item unit : new Item[]{INContent.itemEnrichmentUnit, INContent.itemEnrichmentUnit4, INContent.itemEnrichmentUnit6})
		{
			registry.addRecipeCatalyst(new ItemStack(unit), EnrichmentUnitCategory.UID);
			registry.addIngredientInfo(new ItemStack(unit), VanillaTypes.ITEM, "jei.immersivenuclear.enrichment_unit.info");
		}
		registry.addRecipeClickArea(immersivenuclear.client.GuiEnrichmentUnit.class, immersivenuclear.client.GuiEnrichmentUnit.AREA_X, immersivenuclear.client.GuiEnrichmentUnit.AREA_Y,
				immersivenuclear.client.GuiEnrichmentUnit.AREA_W, immersivenuclear.client.GuiEnrichmentUnit.AREA_H, EnrichmentUnitCategory.UID);
		registry.addRecipes(Collections.singletonList(new ConversionUnitCategory.Recipe()), ConversionUnitCategory.UID);
		registry.addRecipeCatalyst(new ItemStack(INContent.itemConversionUnit), ConversionUnitCategory.UID);
		registry.addRecipeClickArea(immersivenuclear.client.GuiConversionUnit.class, immersivenuclear.client.GuiConversionUnit.AREA_X, immersivenuclear.client.GuiConversionUnit.AREA_Y,
				immersivenuclear.client.GuiConversionUnit.AREA_W, immersivenuclear.client.GuiConversionUnit.AREA_H, ConversionUnitCategory.UID);
		registry.addIngredientInfo(new FluidStack(INContent.fluidFluorinatedEffluent, 1000), VanillaTypes.FLUID, "jei.immersivenuclear.fluorinated_effluent.info");
		registry.addIngredientInfo(new ItemStack(INContent.itemConversionUnit), VanillaTypes.ITEM, "jei.immersivenuclear.conversion_unit.info");
		registry.addRecipes(Collections.singletonList(new HFKilnCategory.Recipe()), HFKilnCategory.UID);
		ItemStack kiln = new ItemStack(INContent.itemHFKiln);
		registry.addRecipeCatalyst(kiln, HFKilnCategory.UID);
		// Clicking the drum of the interface shows the recipe
		registry.addRecipeClickArea(immersivenuclear.client.GuiHFKiln.class, immersivenuclear.client.GuiHFKiln.DRUM_X, immersivenuclear.client.GuiHFKiln.DRUM_Y, immersivenuclear.client.GuiHFKiln.DRUM_W, immersivenuclear.client.GuiHFKiln.DRUM_H, HFKilnCategory.UID);
		registry.addIngredientInfo(kiln, VanillaTypes.ITEM, "jei.immersivenuclear.hf_kiln.info");
		registry.addRecipes(Collections.singletonList(new DeconversionUnitCategory.Recipe()), DeconversionUnitCategory.UID);
		ItemStack deconversion = new ItemStack(INContent.itemDeconversionUnit);
		registry.addRecipeCatalyst(deconversion, DeconversionUnitCategory.UID);
		// Clicking the kiln of the interface shows the recipe
		registry.addRecipeClickArea(immersivenuclear.client.GuiDeconversionUnit.class, immersivenuclear.client.GuiDeconversionUnit.DRUM_X, immersivenuclear.client.GuiDeconversionUnit.DRUM_Y,
				immersivenuclear.client.GuiDeconversionUnit.DRUM_W, immersivenuclear.client.GuiDeconversionUnit.DRUM_H, DeconversionUnitCategory.UID);
		registry.addIngredientInfo(deconversion, VanillaTypes.ITEM, "jei.immersivenuclear.deconversion_unit.info");
		registry.addIngredientInfo(new FluidStack(INContent.fluidHydrofluoricAcid, 1000), VanillaTypes.FLUID, "jei.immersivenuclear.hydrofluoric_acid.info");
		registry.addRecipes(ElectrolyzerCategory.recipes(), ElectrolyzerCategory.UID);
		ItemStack electrolyzer = new ItemStack(INContent.itemElectrolyzer);
		registry.addRecipeCatalyst(electrolyzer, ElectrolyzerCategory.UID);
		// Clicking the cell stacks of the interface shows the recipes
		registry.addRecipeClickArea(immersivenuclear.client.GuiElectrolyzer.class, immersivenuclear.client.GuiElectrolyzer.STACK_X, immersivenuclear.client.GuiElectrolyzer.STACK_Y,
				immersivenuclear.client.GuiElectrolyzer.STACK_W, immersivenuclear.client.GuiElectrolyzer.STACK_H, ElectrolyzerCategory.UID);
		for(Item item : new Item[]{INContent.itemFluoriteOre, INContent.itemFluorite, INContent.itemFluoriteDust, INContent.itemGypsum, INContent.itemPlaster,
				INContent.itemFluorinationAsh, INContent.itemDepletedUraniumOxide, INContent.itemDepletedUraniumIngot})
			registry.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM, "jei." + item.getTranslationKey().substring(5) + ".info");
		registry.addIngredientInfo(new ItemStack(INContent.itemConcentrateDrum), VanillaTypes.ITEM, "jei.immersivenuclear.concentrate_drum.info");
		registry.addIngredientInfo(new ItemStack(INContent.itemAgglomerateBlock), VanillaTypes.ITEM, "jei.immersivenuclear.agglomerate_block.info");
		registry.addIngredientInfo(new ItemStack(INContent.itemAgglomerateTile), VanillaTypes.ITEM, "jei.immersivenuclear.agglomerate_tile.info");
		registry.addIngredientInfo(new ItemStack(INContent.itemAgglomerateBricks), VanillaTypes.ITEM, "jei.immersivenuclear.agglomerate_bricks.info");

		// What each of our materials is for
		registry.addIngredientInfo(new ItemStack(INContent.itemFilter), VanillaTypes.ITEM, "jei.immersivenuclear.filter.info");
		registry.addIngredientInfo(new ItemStack(INContent.itemReagent), VanillaTypes.ITEM, "jei.immersivenuclear.processing_reagent.info");
		registry.addIngredientInfo(new ItemStack(INContent.itemResidue), VanillaTypes.ITEM, "jei.immersivenuclear.wet_mineral_residue.info");
		registry.addIngredientInfo(new ItemStack(INContent.itemNeutralizedResidue), VanillaTypes.ITEM, "jei.immersivenuclear.neutralized_residue.info");
		registry.addIngredientInfo(new ItemStack(INContent.itemMixedDeposit), VanillaTypes.ITEM, "jei.immersivenuclear.mixed_metal_deposit.info");
		for(Item plate : new Item[]{INContent.itemElectrodeSteel, INContent.itemElectrodeLeadSilver, INContent.itemElectrodeElectrum})
			registry.addIngredientInfo(new ItemStack(plate), VanillaTypes.ITEM, "jei.immersivenuclear.electrode.info");
		registry.addIngredientInfo(new FluidStack(INContent.fluidDepletedEffluent, 1000), VanillaTypes.FLUID, "jei.immersivenuclear.depleted_effluent.info");
		registry.addIngredientInfo(new FluidStack(INContent.fluidSlurry, 1000), VanillaTypes.FLUID, "jei.immersivenuclear.uranium_slurry.info");
		registry.addIngredientInfo(new FluidStack(INContent.fluidCrudeSolution, 1000), VanillaTypes.FLUID, "jei.immersivenuclear.crude_uranium_solution.info");
		// Next stages of the line: registered already, no machine uses them yet
		for(Item item : new Item[]{INContent.itemQuicklime, INContent.itemCake, INContent.itemAgglomerate, INContent.itemPrecipitatingAgent,
				INContent.itemWetConcentrate, INContent.itemYellowcake})
			registry.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM, "jei." + item.getTranslationKey().substring(5) + ".info");
		// The electronic and relay modules have a recipe and a use now (the Control Engineering block); the rest wait for the computers
		for(Item item : INContent.itemComputerParts)
			registry.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM, item == INContent.itemComputerParts[0] || item == INContent.itemComputerParts[1]
					? "jei." + item.getTranslationKey().substring(5) + ".info" : "jei.immersivenuclear.computer_part.info");
		for(Item item : new Item[]{INContent.itemLeadedGlass, INContent.itemRemoteManipulator, INContent.itemPrecisionBearing})
			registry.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM, "jei." + item.getTranslationKey().substring(5) + ".info");
		for(Item item : new Item[]{INContent.itemLinkConnector, INContent.itemLinkRelay})
			registry.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM, "jei.immersivenuclear.computer_part.info");
		for(Item item : INContent.itemEngineering)
			registry.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM, "jei." + item.getTranslationKey().substring(5) + ".info");
		registry.addIngredientInfo(new FluidStack(INContent.fluidSludge, 1000), VanillaTypes.FLUID, "jei.immersivenuclear.neutralized_sludge.info");
		registry.addIngredientInfo(new FluidStack(INContent.fluidCondensate, 1000), VanillaTypes.FLUID, "jei.immersivenuclear.process_condensate.info");
		for(Fluid fluid : INContent.fluids)
			if(fluid != INContent.fluidSlurry && fluid != INContent.fluidCrudeSolution)
				if(!hasRecipes(fluid))
					registry.addIngredientInfo(new FluidStack(fluid, 1000), VanillaTypes.FLUID, "jei.immersivenuclear.fluid_upcoming.info");
	}
}
