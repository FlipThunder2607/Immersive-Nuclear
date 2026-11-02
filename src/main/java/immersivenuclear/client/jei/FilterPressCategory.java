package immersivenuclear.client.jei;

import java.util.Collections;
import java.util.List;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileFilterPress;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.client.resources.I18n;

/** JEI page of the Filter Press (see {@link PageCategory}): slurry and the filters | the closed press, its belt and the
 * residue's box | solution. */
public class FilterPressCategory extends PageCategory<FilterPressRecipe>
{
	public static final String UID = "immersivenuclear.filter_press";

	public FilterPressCategory(IGuiHelper helper)
	{
		super(helper, "filter_press", 186, 156, INContent.itemFilterPress);
	}

	@Override
	String text()
	{
		return FilterPressRecipe.energy() + " IF   " + FilterPressRecipe.ticks() / 20 + " s";
	}

	@Override
	public void setRecipe(IRecipeLayout layout, FilterPressRecipe recipe, IIngredients ingredients)
	{
		IGuiItemStackGroup items = layout.getItemStacks();
		// JEI puts the item 1 px inside the slot position
		items.init(0, true, 5, 118);
		items.init(1, false, 132, 118);
		items.set(ingredients);
		items.addTooltipCallback((slot, input, stack, tooltip) -> {
			if(slot == 0)
			{
				tooltip.add("§7" + I18n.format("jei.immersivenuclear.filter_press.filters", TileFilterPress.FILTERS));
				tooltip.add("§7" + I18n.format("jei.immersivenuclear.filter_press.wear"));
			}
		});
		IGuiFluidStackGroup fluids = layout.getFluidStacks();
		tank(fluids, 0, true, 6, 64, TileFilterPress.TANK_CAPACITY);
		tank(fluids, 1, false, 164, 64, TileFilterPress.TANK_CAPACITY);
		fluids.set(ingredients);
	}

	@Override
	public List<String> getTooltipStrings(int mx, int my)
	{
		if(in(mx, my, 28, 64, 97, 64))
			return Collections.singletonList(I18n.format("gui.immersivenuclear.filter_press.press"));
		return Collections.emptyList();
	}
}
