package immersivenuclear.client.jei;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TilePrecipitationUnit;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/** One batch of the Precipitation Unit, with the amounts the tile uses (TilePrecipitationUnit). */
public class PrecipitationUnitRecipe implements IRecipeWrapper
{
	@Override
	public void getIngredients(IIngredients ingredients)
	{
		ingredients.setInput(VanillaTypes.ITEM, new ItemStack(INContent.itemPrecipitatingAgent, TilePrecipitationUnit.AGENT_PER_BATCH));
		ingredients.setInput(VanillaTypes.FLUID, new FluidStack(INContent.fluidPurifiedSolution, TilePrecipitationUnit.BATCH_SOLUTION));
		ingredients.setOutput(VanillaTypes.ITEM, new ItemStack(INContent.itemWetConcentrate, TilePrecipitationUnit.CONCENTRATE_PER_BATCH));
		ingredients.setOutput(VanillaTypes.FLUID, new FluidStack(INContent.fluidEffluent, TilePrecipitationUnit.EFFLUENT_PER_BATCH));
	}

	/** Energy of one batch: the agitator while it precipitates, the screw while it dewaters (the fill takes none). */
	public static int energy()
	{
		return TilePrecipitationUnit.PRECIPITATE_ENERGY * TilePrecipitationUnit.PRECIPITATE_TICKS
				+ TilePrecipitationUnit.DEWATER_ENERGY * TilePrecipitationUnit.DEWATER_TICKS;
	}

	/** Ticks of one batch, the fill aside. */
	public static int ticks()
	{
		return TilePrecipitationUnit.PRECIPITATE_TICKS + TilePrecipitationUnit.DEWATER_TICKS;
	}
}
