package immersivenuclear.client.jei;

import java.util.Collections;

import immersivenuclear.client.GuiChemicalProcessor;
import immersivenuclear.client.GuiChemicalSeparator;
import immersivenuclear.client.GuiFilterPress;
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
	}

	/** The liquids the Chemical Separation Unit uses or makes have recipe pages now, not the "coming later" note. */
	static boolean separatorFluid(Fluid f)
	{
		return f == INContent.fluidSolvent || f == INContent.fluidStrippingSolution || f == INContent.fluidPurifiedSolution || f == INContent.fluidEffluent;
	}

	@Override
	public void register(IModRegistry registry)
	{
		registry.addRecipes(Collections.singletonList(new ChemicalProcessorRecipe()), ChemicalProcessorCategory.UID);
		ItemStack machine = new ItemStack(INContent.itemChemicalProcessor);
		registry.addRecipeCatalyst(machine, ChemicalProcessorCategory.UID);
		// Clicking the arrow of the interface shows the recipes, like IE's machines
		registry.addRecipeClickArea(GuiChemicalProcessor.class, GuiChemicalProcessor.REACTOR_X, GuiChemicalProcessor.REACTOR_Y, GuiChemicalProcessor.REACTOR_W, GuiChemicalProcessor.REACTOR_H, ChemicalProcessorCategory.UID);
		registry.addIngredientInfo(machine, VanillaTypes.ITEM, "jei.immersivenuclear.chemical_processor.info");

		registry.addRecipes(Collections.singletonList(new FilterPressRecipe()), FilterPressCategory.UID);
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

		// What each of our materials is for
		registry.addIngredientInfo(new ItemStack(INContent.itemFilter), VanillaTypes.ITEM, "jei.immersivenuclear.filter.info");
		registry.addIngredientInfo(new ItemStack(INContent.itemReagent), VanillaTypes.ITEM, "jei.immersivenuclear.processing_reagent.info");
		registry.addIngredientInfo(new ItemStack(INContent.itemResidue), VanillaTypes.ITEM, "jei.immersivenuclear.wet_mineral_residue.info");
		registry.addIngredientInfo(new FluidStack(INContent.fluidSlurry, 1000), VanillaTypes.FLUID, "jei.immersivenuclear.uranium_slurry.info");
		registry.addIngredientInfo(new FluidStack(INContent.fluidCrudeSolution, 1000), VanillaTypes.FLUID, "jei.immersivenuclear.crude_uranium_solution.info");
		// Next stages of the line: registered already, no machine uses them yet
		for(Item item : new Item[]{INContent.itemQuicklime, INContent.itemCake, INContent.itemAgglomerate, INContent.itemPrecipitatingAgent,
				INContent.itemWetConcentrate, INContent.itemYellowcake})
			registry.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM, "jei." + item.getTranslationKey().substring(5) + ".info");
		for(Fluid fluid : INContent.fluids)
			if(fluid != INContent.fluidSlurry && fluid != INContent.fluidCrudeSolution)
				if(!separatorFluid(fluid))
					registry.addIngredientInfo(new FluidStack(fluid, 1000), VanillaTypes.FLUID, "jei.immersivenuclear.fluid_upcoming.info");
	}
}
