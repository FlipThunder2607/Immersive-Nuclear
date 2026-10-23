package immersivenuclear.client.jei;

import java.util.Arrays;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileFilterPress;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/** One batch of the Filtration Unit, with the amounts the tile uses (TileFilterPress). */
public class FilterPressRecipe implements IRecipeWrapper
{
	@Override
	public void getIngredients(IIngredients ingredients)
	{
		// The filters are worn, not used up: listed so they show the machine among their uses
		ingredients.setInput(VanillaTypes.ITEM, new ItemStack(INContent.itemFilter));
		ingredients.setInput(VanillaTypes.FLUID, new FluidStack(INContent.fluidPolpa, TileFilterPress.BATCH));
		ingredients.setOutput(VanillaTypes.ITEM, new ItemStack(INContent.itemRejeito, TileFilterPress.RESIDUE_PER_BATCH));
		ingredients.setOutputs(VanillaTypes.FLUID, Arrays.asList(new FluidStack(INContent.fluidSolucaoBruta, TileFilterPress.SOLUTION_PER_BATCH)));
	}

	/** Energy of one batch: pump, both actuator strokes, filtering. */
	public static int energy()
	{
		return TileFilterPress.PUMP_ENERGY * TileFilterPress.PRESSURIZE_TICKS + TileFilterPress.ACTUATOR_ENERGY * (TileFilterPress.CLOSE_TICKS + TileFilterPress.OPEN_MOVE_TICKS)
				+ TileFilterPress.FILTER_ENERGY * TileFilterPress.FILTER_TICKS;
	}

	/** Ticks of one batch once the machine is running (the pump works during the previous batch). */
	public static int ticks()
	{
		return TileFilterPress.CLOSE_TICKS + TileFilterPress.RELEASE_TICKS + TileFilterPress.FILTER_TICKS + TileFilterPress.OPEN_TICKS;
	}
}
