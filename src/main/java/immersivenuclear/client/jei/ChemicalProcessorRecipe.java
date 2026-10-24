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

/** One batch of the unit, with the amounts the tile uses (TileChemicalProcessor). */
public class ChemicalProcessorRecipe implements IRecipeWrapper
{
	@Override
	public void getIngredients(IIngredients ingredients)
	{
		java.util.List<ItemStack> uranium = new java.util.ArrayList<>();
		for(ItemStack s : OreDictionary.getOres("dustUranium"))
		{
			ItemStack c = s.copy();
			c.setCount(TileChemicalProcessor.BATCH_SOLIDS);
			uranium.add(c);
		}
		ingredients.setInputLists(VanillaTypes.ITEM, Arrays.asList(uranium,
				Collections.singletonList(new ItemStack(INContent.itemReagent, TileChemicalProcessor.BATCH_SOLIDS))));
		ingredients.setInput(VanillaTypes.FLUID, new FluidStack(FluidRegistry.WATER, TileChemicalProcessor.BATCH_WATER));
		ingredients.setOutput(VanillaTypes.FLUID, new FluidStack(INContent.fluidSlurry, TileChemicalProcessor.BATCH_WATER));
	}
}
