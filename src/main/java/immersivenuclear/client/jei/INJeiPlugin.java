package immersivenuclear.client.jei;

import java.util.Collections;

import immersivenuclear.client.GuiChemicalProcessor;
import immersivenuclear.client.GuiChemicalSeparator;
import immersivenuclear.client.GuiFilterPress;
import immersivenuclear.client.GuiMetalExtractor;
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
	}

	/** Liquids with recipe pages now (machine 3, IE's Mixer), not the "coming later" note. */
	static boolean hasRecipes(Fluid f)
	{
		return f == INContent.fluidSolvent || f == INContent.fluidStrippingSolution || f == INContent.fluidPurifiedSolution || f == INContent.fluidEffluent
				|| f == INContent.fluidSludge || f == INContent.fluidDepletedEffluent;
	}

	@Override
	public void register(IModRegistry registry)
	{
		registry.addRecipes(java.util.Arrays.asList(new ChemicalProcessorRecipe(false), new ChemicalProcessorRecipe(true)), ChemicalProcessorCategory.UID);
		ItemStack machine = new ItemStack(INContent.itemChemicalProcessor);
		registry.addRecipeCatalyst(machine, ChemicalProcessorCategory.UID);
		// Clicking the arrow of the interface shows the recipes, like IE's machines
		registry.addRecipeClickArea(GuiChemicalProcessor.class, GuiChemicalProcessor.REACTOR_X, GuiChemicalProcessor.REACTOR_Y, GuiChemicalProcessor.REACTOR_W, GuiChemicalProcessor.REACTOR_H, ChemicalProcessorCategory.UID);
		registry.addIngredientInfo(machine, VanillaTypes.ITEM, "jei.immersivenuclear.chemical_processor.info");

		registry.addRecipes(java.util.Arrays.asList(new FilterPressRecipe(false), new FilterPressRecipe(true)), FilterPressCategory.UID);
		ItemStack press = new ItemStack(INContent.itemFilterPress);
		registry.addRecipeCatalyst(press, FilterPressCategory.UID);
		registry.addRecipeClickArea(GuiFilterPress.class, GuiFilterPress.PUMP_X, 16 + GuiFilterPress.PUMP_DY, GuiFilterPress.PUMP_W, GuiFilterPress.PUMP_H, FilterPressCategory.UID);
		registry.addRecipeClickArea(GuiFilterPress.class, GuiFilterPress.PRESS_X, 16 + GuiFilterPress.PRESS_DY, GuiFilterPress.PRESS_W, GuiFilterPress.PRESS_H, FilterPressCategory.UID);
		registry.addIngredientInfo(press, VanillaTypes.ITEM, "jei.immersivenuclear.filter_press.info");
		registry.addRecipes(Collections.singletonList(new ChemicalSeparatorRecipe()), ChemicalSeparatorCategory.UID);
		ItemStack separator = new ItemStack(INContent.itemChemicalSeparator);
		registry.addRecipeCatalyst(separator, ChemicalSeparatorCategory.UID);
		for(int row : new int[]{GuiChemicalSeparator.ROW_A, GuiChemicalSeparator.ROW_B})
			registry.addRecipeClickArea(GuiChemicalSeparator.class, GuiChemicalSeparator.SETTLER_X, row + GuiChemicalSeparator.SETTLER_DY,
					GuiChemicalSeparator.SETTLER_W, GuiChemicalSeparator.SETTLER_H, ChemicalSeparatorCategory.UID);
		registry.addIngredientInfo(separator, VanillaTypes.ITEM, "jei.immersivenuclear.chemical_separator.info");
		registry.addRecipes(java.util.Arrays.asList(new MetalExtractorRecipe(INContent.itemElectrodeSteel), new MetalExtractorRecipe(INContent.itemElectrodeLeadSilver),
				new MetalExtractorRecipe(INContent.itemElectrodeElectrum)), MetalExtractorCategory.UID);
		ItemStack extractor = new ItemStack(INContent.itemMetalExtractor);
		registry.addRecipeCatalyst(extractor, MetalExtractorCategory.UID);
		// Clicking the vat of the interface shows the recipe
		registry.addRecipeClickArea(GuiMetalExtractor.class, GuiMetalExtractor.VAT_X, GuiMetalExtractor.VAT_Y, GuiMetalExtractor.VAT_W, GuiMetalExtractor.VAT_H, MetalExtractorCategory.UID);
		registry.addIngredientInfo(extractor, VanillaTypes.ITEM, "jei.immersivenuclear.metal_extractor.info");

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
		for(Item item : INContent.itemComputerParts)
			registry.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM, "jei.immersivenuclear.computer_part.info");
		for(Item item : new Item[]{INContent.itemLinkConnector, INContent.itemLinkRelay})
			registry.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM, "jei.immersivenuclear.computer_part.info");
		for(Item item : INContent.itemEngineering)
			registry.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM, "jei." + item.getTranslationKey().substring(5) + ".info");
		registry.addIngredientInfo(new FluidStack(INContent.fluidSludge, 1000), VanillaTypes.FLUID, "jei.immersivenuclear.neutralized_sludge.info");
		for(Fluid fluid : INContent.fluids)
			if(fluid != INContent.fluidSlurry && fluid != INContent.fluidCrudeSolution)
				if(!hasRecipes(fluid))
					registry.addIngredientInfo(new FluidStack(fluid, 1000), VanillaTypes.FLUID, "jei.immersivenuclear.fluid_upcoming.info");
	}
}
