package immersivenuclear.client.jei;

import java.util.Arrays;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileChemicalSeparator;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraftforge.fluids.FluidStack;

/**
 * One extraction batch and one stripping batch of the unit, with the amounts the tile uses (TileChemicalSeparator):
 * crude solution and stripping solution in, the solvent lost by the batch made up; chemical effluent and purified
 * solution out (the solvent goes round inside).
 */
public class ChemicalSeparatorRecipe implements IRecipeWrapper
{
	@Override
	public void getIngredients(IIngredients ingredients)
	{
		ingredients.setInputs(VanillaTypes.FLUID, Arrays.asList(
				new FluidStack(INContent.fluidCrudeSolution, TileChemicalSeparator.A_AQUEOUS),
				new FluidStack(INContent.fluidStrippingSolution, TileChemicalSeparator.B_AQUEOUS),
				new FluidStack(INContent.fluidSolvent, TileChemicalSeparator.SOLVENT_LOSS)));
		ingredients.setOutputs(VanillaTypes.FLUID, Arrays.asList(
				new FluidStack(INContent.fluidEffluent, TileChemicalSeparator.A_AQUEOUS),
				new FluidStack(INContent.fluidPurifiedSolution, TileChemicalSeparator.B_AQUEOUS)));
	}

	/** Mixing both batches, then the pumps draining both settlers (each drains its two layers at once). */
	static int energy()
	{
		int mixA = TileChemicalSeparator.A_AQUEOUS / TileChemicalSeparator.A_AQUEOUS_RATE;
		int mixB = TileChemicalSeparator.B_SOLVENT / TileChemicalSeparator.B_SOLVENT_RATE;
		int drainA = Math.max(TileChemicalSeparator.A_AQUEOUS, TileChemicalSeparator.A_SOLVENT) / TileChemicalSeparator.DRAIN_PER_TICK;
		int drainB = Math.max(TileChemicalSeparator.B_AQUEOUS, TileChemicalSeparator.B_SOLVENT) / TileChemicalSeparator.DRAIN_PER_TICK;
		return (mixA + mixB) * TileChemicalSeparator.MIXER_ENERGY + (drainA + drainB) * TileChemicalSeparator.PUMP_ENERGY;
	}

	/** One settler's round: filling, settling, draining (the two stages run side by side). */
	static int ticks()
	{
		return TileChemicalSeparator.A_AQUEOUS / TileChemicalSeparator.A_AQUEOUS_RATE + TileChemicalSeparator.SETTLE_TICKS
				+ Math.max(TileChemicalSeparator.A_AQUEOUS, TileChemicalSeparator.A_SOLVENT) / TileChemicalSeparator.DRAIN_PER_TICK;
	}
}
