package immersivenuclear.client.jei;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileIndustrialDryer;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/** One batch of the Industrial Dryer, with the amounts the tile uses (TileIndustrialDryer). */
public class IndustrialDryerRecipe implements IRecipeWrapper
{
	@Override
	public void getIngredients(IIngredients ingredients)
	{
		ingredients.setInput(VanillaTypes.ITEM, new ItemStack(INContent.itemWetConcentrate, TileIndustrialDryer.WET_PER_BATCH));
		ingredients.setOutput(VanillaTypes.ITEM, new ItemStack(INContent.itemYellowcake, TileIndustrialDryer.YELLOWCAKE_PER_BATCH));
		ingredients.setOutput(VanillaTypes.FLUID, new FluidStack(INContent.fluidCondensate, TileIndustrialDryer.CONDENSATE_PER_BATCH));
	}

	/** Energy of one batch, every step together. */
	public static int energy()
	{
		return TileIndustrialDryer.LOAD_ENERGY * TileIndustrialDryer.LOAD_TICKS + TileIndustrialDryer.HEAT_ENERGY * TileIndustrialDryer.HEAT_TICKS
				+ TileIndustrialDryer.DRY_ENERGY * TileIndustrialDryer.DRY_TICKS + TileIndustrialDryer.DISCHARGE_ENERGY * TileIndustrialDryer.DISCHARGE_TICKS;
	}

	/** Ticks of one batch, every step together. */
	public static int ticks()
	{
		int t = 0;
		for(int p = 0; p < 4; p++)
			t += TileIndustrialDryer.phaseLength(p);
		return t;
	}
}
