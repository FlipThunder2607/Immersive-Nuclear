package immersivenuclear.client.jei;

import java.util.Collections;
import java.util.List;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileChemicalSeparator;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.client.resources.I18n;

/** JEI page of the Chemical Separator (see {@link PageCategory}): crude and stripping solutions | the two stages side by
 * side under the solvent's reservoir | effluent and purified solution. */
public class ChemicalSeparatorCategory extends PageCategory<ChemicalSeparatorRecipe>
{
	public static final String UID = "immersivenuclear.chemical_separator";

	public ChemicalSeparatorCategory(IGuiHelper helper)
	{
		super(helper, "chemical_separator", 186, 156, INContent.itemChemicalSeparator);
	}

	@Override
	String text()
	{
		return ChemicalSeparatorRecipe.energy() + " IF   " + ChemicalSeparatorRecipe.ticks() / 20 + " s";
	}

	@Override
	public void setRecipe(IRecipeLayout layout, ChemicalSeparatorRecipe recipe, IIngredients ingredients)
	{
		IGuiFluidStackGroup fluids = layout.getFluidStacks();
		int cap = TileChemicalSeparator.TANK_CAPACITY;
		tank(fluids, 0, true, 4, 35, cap);
		tank(fluids, 1, true, 4, 92, cap);
		// The solvent's make-up, in the reservoir's sight glass
		fluids.init(2, true, 75, 47, 24, 3, TileChemicalSeparator.SOLVENT_LOSS, false, null);
		tank(fluids, 3, false, 166, 35, cap);
		tank(fluids, 4, false, 166, 92, cap);
		fluids.set(ingredients);
		fluids.addTooltipCallback((slot, input, stack, tooltip) -> {
			if(slot == 2)
				tooltip.add("§7" + I18n.format("jei.immersivenuclear.chemical_separator.makeup"));
		});
	}

	@Override
	public List<String> getTooltipStrings(int mx, int my)
	{
		if(in(mx, my, 49, 62, 42, 74) || in(mx, my, 119, 62, 44, 74))
			return Collections.singletonList(I18n.format("jei.immersivenuclear.chemical_separator.settling", TileChemicalSeparator.SETTLE_TICKS / 20));
		return Collections.emptyList();
	}
}
