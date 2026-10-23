package immersivenuclear.client.jei;

import java.util.Arrays;
import java.util.Collections;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileChemicalProcessor;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

/** The unit's one cycle, with the amounts the tile uses (TileChemicalProcessor). */
public class ChemicalProcessorRecipe implements IRecipeWrapper
{
	@Override
	public void getIngredients(IIngredients ingredients)
	{
		ingredients.setInputLists(VanillaTypes.ITEM, Arrays.asList(
				OreDictionary.getOres("dustUranium"),
				Collections.singletonList(new ItemStack(INContent.itemReagent))));
		ingredients.setInput(VanillaTypes.FLUID, new FluidStack(FluidRegistry.WATER, TileChemicalProcessor.WATER_PER_CYCLE));
		ingredients.setOutput(VanillaTypes.FLUID, new FluidStack(INContent.fluidPolpa, TileChemicalProcessor.POLPA_PER_CYCLE));
	}
}
