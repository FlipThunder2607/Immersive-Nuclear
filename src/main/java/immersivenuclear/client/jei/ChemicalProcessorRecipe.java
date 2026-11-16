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
 * One batch of the unit, with the amounts the tile uses (TileChemicalProcessor): all water, with the most recycled
 * effluent (chemical or depleted) and the reagent it saves, or with the most of the dryer's process condensate (water
 * only: no reagent saved).
 */
public class ChemicalProcessorRecipe implements IRecipeWrapper
{
	/** Half the batch recycled liquor; that liquor is the process condensate. */
	final boolean recycled, condensate;

	public ChemicalProcessorRecipe(boolean recycled)
	{
		this(recycled, false);
	}

	public ChemicalProcessorRecipe(boolean recycled, boolean condensate)
	{
		this(recycled, condensate, TileChemicalProcessor.MAT_URANIUM);
	}

	/** TileChemicalProcessor.MAT_*: the uranium's pages, and one page (all water) for zircon and for borax. */
	final int material;

	public ChemicalProcessorRecipe(boolean recycled, boolean condensate, int material)
	{
		this.recycled = recycled || condensate;
		this.condensate = condensate;
		this.material = material;
	}

	@Override
	public void getIngredients(IIngredients ingredients)
	{
		int share = recycled ? TileChemicalProcessor.MAX_RECYCLED : 0;
		java.util.List<ItemStack> uranium = new java.util.ArrayList<>();
		String dust = material == TileChemicalProcessor.MAT_ZIRCONIUM ? "dustZircon" : material == TileChemicalProcessor.MAT_BORON ? "dustBorax"
				: material == TileChemicalProcessor.MAT_SILICA ? "dustQuartz" : "dustUranium";
		for(ItemStack s : OreDictionary.getOres(dust))
		{
			ItemStack c = s.copy();
			c.setCount(TileChemicalProcessor.BATCH_SOLIDS);
			uranium.add(c);
		}
		if(material == TileChemicalProcessor.MAT_URANIUM)
			uranium.add(new ItemStack(INContent.itemFluorinationAsh, TileChemicalProcessor.BATCH_SOLIDS));
		boolean acid = material == TileChemicalProcessor.MAT_SILICA;     // quartz in hydrofluoric acid: no reagent
		ingredients.setInputLists(VanillaTypes.ITEM, Arrays.asList(uranium, acid ? Collections.<ItemStack>emptyList()
				: Collections.singletonList(new ItemStack(INContent.itemReagent, TileChemicalProcessor.reagentFor(condensate ? 0 : share)))));
		if(recycled)
			ingredients.setInputLists(VanillaTypes.FLUID, Arrays.asList(Collections.singletonList(new FluidStack(FluidRegistry.WATER, TileChemicalProcessor.BATCH_WATER - share)),
					condensate ? Collections.singletonList(new FluidStack(INContent.fluidCondensate, share))
							: Arrays.asList(new FluidStack(INContent.fluidEffluent, share), new FluidStack(INContent.fluidDepletedEffluent, share))));
		else
			ingredients.setInput(VanillaTypes.FLUID, new FluidStack(TileChemicalProcessor.liquidOf(material), TileChemicalProcessor.BATCH_WATER));
		ingredients.setOutput(VanillaTypes.FLUID, new FluidStack(TileChemicalProcessor.outputOf(material), TileChemicalProcessor.BATCH_WATER));
	}
}
