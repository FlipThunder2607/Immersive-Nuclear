package immersivenuclear.client.jei;

import java.util.Collections;

import immersivenuclear.client.GuiChemicalProcessor;
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

	@Override
	public void register(IModRegistry registry)
	{
		registry.addRecipes(Collections.singletonList(new ChemicalProcessorRecipe()), ChemicalProcessorCategory.UID);
		ItemStack machine = new ItemStack(INContent.itemChemicalProcessor);
		registry.addRecipeCatalyst(machine, ChemicalProcessorCategory.UID);
		// Clicking the arrow of the interface shows the recipes, like IE's machines
		registry.addRecipeClickArea(GuiChemicalProcessor.class, GuiChemicalProcessor.ARROW_X, GuiChemicalProcessor.ARROW_Y, 19, 13, ChemicalProcessorCategory.UID);
		registry.addIngredientInfo(machine, VanillaTypes.ITEM, "jei.immersivenuclear.chemical_processor.info");

		registry.addRecipes(Collections.singletonList(new FilterPressRecipe()), FilterPressCategory.UID);
		ItemStack press = new ItemStack(INContent.itemFilterPress);
		registry.addRecipeCatalyst(press, FilterPressCategory.UID);
		registry.addRecipeClickArea(GuiFilterPress.class, GuiFilterPress.PUMP_ARROW_X, GuiFilterPress.ARROW_Y, 19, 13, FilterPressCategory.UID);
		registry.addRecipeClickArea(GuiFilterPress.class, GuiFilterPress.FILTER_ARROW_X, GuiFilterPress.ARROW_Y, 19, 13, FilterPressCategory.UID);
		registry.addIngredientInfo(press, VanillaTypes.ITEM, "jei.immersivenuclear.filter_press.info");

		// What each of our materials is for
		registry.addIngredientInfo(new ItemStack(INContent.itemFilter), VanillaTypes.ITEM, "jei.immersivenuclear.filtro.info");
		registry.addIngredientInfo(new ItemStack(INContent.itemReagent), VanillaTypes.ITEM, "jei.immersivenuclear.reagente_processamento.info");
		registry.addIngredientInfo(new ItemStack(INContent.itemRejeito), VanillaTypes.ITEM, "jei.immersivenuclear.rejeito_mineral_umido.info");
		registry.addIngredientInfo(new FluidStack(INContent.fluidPolpa, 1000), VanillaTypes.FLUID, "jei.immersivenuclear.polpa_uranio.info");
		registry.addIngredientInfo(new FluidStack(INContent.fluidSolucaoBruta, 1000), VanillaTypes.FLUID, "jei.immersivenuclear.solucao_bruta_uranio.info");
		// Next stages of the line: registered already, no machine uses them yet
		for(Item item : new Item[]{INContent.itemCal, INContent.itemTorta, INContent.itemAglomerado, INContent.itemAgentePrecipitante,
				INContent.itemConcentradoUmido, INContent.itemYellowcake})
			registry.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM, "jei." + item.getTranslationKey().substring(5) + ".info");
		for(Fluid fluid : INContent.fluids)
			if(fluid != INContent.fluidPolpa && fluid != INContent.fluidSolucaoBruta)
				registry.addIngredientInfo(new FluidStack(fluid, 1000), VanillaTypes.FLUID, "jei.immersivenuclear.fluid_futuro.info");
	}
}
