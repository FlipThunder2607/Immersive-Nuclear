package immersivenuclear.client.jei;

import java.util.Collections;
import java.util.List;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TilePrecipitationUnit;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.client.resources.I18n;

/** JEI page of the Precipitation Unit (see {@link PageCategory}): the agent over the agitated tank (the batch in it), the
 * screw up to the concentrate's box | effluent. */
public class PrecipitationUnitCategory extends PageCategory<PrecipitationUnitRecipe>
{
	public static final String UID = "immersivenuclear.precipitation_unit";

	public PrecipitationUnitCategory(IGuiHelper helper)
	{
		super(helper, "precipitation_unit", 186, 156, INContent.itemPrecipitationUnit);
	}

	@Override
	String text()
	{
		return PrecipitationUnitRecipe.energy() + " IF   " + PrecipitationUnitRecipe.ticks() / 20 + " s";
	}

	@Override
	public void setRecipe(IRecipeLayout layout, PrecipitationUnitRecipe recipe, IIngredients ingredients)
	{
		IGuiItemStackGroup items = layout.getItemStacks();
		// JEI puts the item 1 px inside the slot position
		items.init(0, true, 13, 7);
		items.init(1, false, 132, 118);
		items.set(ingredients);
		IGuiFluidStackGroup fluids = layout.getFluidStacks();
		// The batch in the tank's window
		fluids.init(0, true, 12, 58, 84, 40, TilePrecipitationUnit.BATCH_SOLUTION, false, null);
		tank(fluids, 1, false, 164, 64, TilePrecipitationUnit.EFFLUENT_CAPACITY);
		fluids.set(ingredients);
	}

	@Override
	public List<String> getTooltipStrings(int mx, int my)
	{
		if(in(mx, my, 38, 93, 106, 38))
			return Collections.singletonList(I18n.format("gui.immersivenuclear.precipitation_unit.screw"));
		return Collections.emptyList();
	}
}
