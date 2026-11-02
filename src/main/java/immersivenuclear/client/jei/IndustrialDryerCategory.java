package immersivenuclear.client.jei;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileIndustrialDryer;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.client.resources.I18n;

/** JEI page of the Industrial Dryer (see {@link PageCategory}): the wet concentrate over the drying chamber, the scrubber,
 * the yellowcake's box | condensate. */
public class IndustrialDryerCategory extends PageCategory<IndustrialDryerRecipe>
{
	public static final String UID = "immersivenuclear.industrial_dryer";

	public IndustrialDryerCategory(IGuiHelper helper)
	{
		super(helper, "industrial_dryer", 186, 156, INContent.itemIndustrialDryer);
	}

	@Override
	String text()
	{
		return IndustrialDryerRecipe.energy() + " IF   " + IndustrialDryerRecipe.ticks() / 20 + " s   " + TileIndustrialDryer.DRYING_TEMPERATURE + " °C";
	}

	@Override
	public void setRecipe(IRecipeLayout layout, IndustrialDryerRecipe recipe, IIngredients ingredients)
	{
		IGuiItemStackGroup items = layout.getItemStacks();
		// JEI puts the item 1 px inside the slot position
		items.init(0, true, 23, 7);
		items.init(1, false, 132, 72);
		items.set(ingredients);
		IGuiFluidStackGroup fluids = layout.getFluidStacks();
		tank(fluids, 0, false, 164, 64, TileIndustrialDryer.CONDENSATE_CAPACITY);
		fluids.set(ingredients);
	}

	@Override
	public List<String> getTooltipStrings(int mx, int my)
	{
		String k = "gui.immersivenuclear.industrial_dryer.";
		if(in(mx, my, 98, 12, 16, 16))
			return Arrays.asList(I18n.format(k + "scrubber"), I18n.format(k + "scrubber_info", TileIndustrialDryer.FINES_EVERY));
		if(in(mx, my, 4, 46, 114, 48))
			return Arrays.asList(I18n.format(k + "chamber"), I18n.format(k + "temperature_info", TileIndustrialDryer.DRYING_TEMPERATURE));
		return Collections.emptyList();
	}
}
