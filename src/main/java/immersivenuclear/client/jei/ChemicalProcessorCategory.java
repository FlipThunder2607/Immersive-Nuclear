package immersivenuclear.client.jei;

import java.util.Collections;
import java.util.List;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileChemicalProcessor;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.client.resources.I18n;

/** JEI page of the Chemical Processing Unit (see {@link PageCategory}): water | the reactor under its hopper | slurry. */
public class ChemicalProcessorCategory extends PageCategory<ChemicalProcessorRecipe>
{
	public static final String UID = "immersivenuclear.chemical_processor";

	public ChemicalProcessorCategory(IGuiHelper helper)
	{
		super(helper, "chemical_processor", 186, 156, INContent.itemChemicalProcessor);
	}

	static int energy()
	{
		int pumping = TileChemicalProcessor.BATCH_TICKS - TileChemicalProcessor.REACT_TICKS;
		return TileChemicalProcessor.REACT_ENERGY * TileChemicalProcessor.REACT_TICKS + TileChemicalProcessor.PUMP_ENERGY * pumping;
	}

	@Override
	String text()
	{
		return energy() + " IF   " + TileChemicalProcessor.BATCH_TICKS / 20 + " s";
	}

	@Override
	public void setRecipe(IRecipeLayout layout, ChemicalProcessorRecipe recipe, IIngredients ingredients)
	{
		IGuiItemStackGroup items = layout.getItemStacks();
		// JEI puts the item 1 px inside the slot position
		items.init(0, true, 45, 5);
		items.init(1, true, 67, 5);
		items.set(ingredients);
		IGuiFluidStackGroup fluids = layout.getFluidStacks();
		int cap = TileChemicalProcessor.TANK_CAPACITY;
		if(recipe.recycled)
		{
			// The input tank split: water on top, the recycled liquor (effluent or depleted effluent) under it
			fluids.init(0, true, 6, 64, 16, 23, TileChemicalProcessor.BATCH_WATER / 2, false, null);
			fluids.init(2, true, 6, 88, 16, 23, TileChemicalProcessor.BATCH_WATER / 2, false, null);
		}
		else
			tank(fluids, 0, true, 6, 64, cap);
		tank(fluids, 1, false, 164, 64, cap);
		fluids.set(ingredients);
	}

	@Override
	public List<String> getTooltipStrings(int mx, int my)
	{
		if(in(mx, my, 34, 40, 118, 64))
			return Collections.singletonList(I18n.format("gui.immersivenuclear.chemical_processor.reactor"));
		return Collections.emptyList();
	}
}
