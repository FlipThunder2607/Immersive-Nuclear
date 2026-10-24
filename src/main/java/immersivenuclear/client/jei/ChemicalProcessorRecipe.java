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

/**
 * One batch of the unit, with the amounts the tile uses (TileChemicalProcessor): all water, or with the most recycled
 * liquor (chemical or depleted effluent) and the reagent it saves.
 */
public class ChemicalProcessorRecipe implements IRecipeWrapper
{
	/** Half the batch recycled liquor. */
	final boolean recycled;

	public ChemicalProcessorRecipe(boolean recycled)
	{
		this.recycled = recycled;
	}

	@Override
	public void getIngredients(IIngredients ingredients)
	{
		int share = recycled ? TileChemicalProcessor.MAX_RECYCLED : 0;
		java.util.List<ItemStack> uranium = new java.util.ArrayList<>();
		for(ItemStack s : OreDictionary.getOres("dustUranium"))
		{
			ItemStack c = s.copy();
			c.setCount(TileChemicalProcessor.BATCH_SOLIDS);
			uranium.add(c);
		}
		ingredients.setInputLists(VanillaTypes.ITEM, Arrays.asList(uranium,
				Collections.singletonList(new ItemStack(INContent.itemReagent, TileChemicalProcessor.reagentFor(share)))));
		if(recycled)
			ingredients.setInputLists(VanillaTypes.FLUID, Arrays.asList(Collections.singletonList(new FluidStack(FluidRegistry.WATER, TileChemicalProcessor.BATCH_WATER - share)),
					Arrays.asList(new FluidStack(INContent.fluidEffluent, share), new FluidStack(INContent.fluidDepletedEffluent, share))));
		else
			ingredients.setInput(VanillaTypes.FLUID, new FluidStack(FluidRegistry.WATER, TileChemicalProcessor.BATCH_WATER));
		ingredients.setOutput(VanillaTypes.FLUID, new FluidStack(INContent.fluidSlurry, TileChemicalProcessor.BATCH_WATER));
	}
}
