package immersivenuclear.client.jei;

import java.util.Arrays;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileFilterPress;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * One batch of the Filtration Unit, with the amounts the tile uses (TileFilterPress): uranium slurry (crude solution
 * and residue) or neutralized sludge (water and neutralization cake).
 */
public class FilterPressRecipe implements IRecipeWrapper
{
	final boolean sludge;

	public FilterPressRecipe(boolean sludge)
	{
		this.sludge = sludge;
	}

	@Override
	public void getIngredients(IIngredients ingredients)
	{
		// The filters are worn, not used up: listed so they show the machine among their uses
		ingredients.setInput(VanillaTypes.ITEM, new ItemStack(INContent.itemFilter));
		ingredients.setInput(VanillaTypes.FLUID, new FluidStack(sludge ? INContent.fluidSludge : INContent.fluidSlurry, TileFilterPress.BATCH));
		ingredients.setOutput(VanillaTypes.ITEM, new ItemStack(sludge ? INContent.itemCake : INContent.itemResidue, TileFilterPress.RESIDUE_PER_BATCH));
		ingredients.setOutputs(VanillaTypes.FLUID, Arrays.asList(new FluidStack(sludge ? net.minecraftforge.fluids.FluidRegistry.WATER : INContent.fluidCrudeSolution,
				TileFilterPress.SOLUTION_PER_BATCH)));
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
		return TileFilterPress.CLOSE_TICKS + TileFilterPress.FILTER_TICKS + TileFilterPress.OPEN_TICKS;
	}
}
