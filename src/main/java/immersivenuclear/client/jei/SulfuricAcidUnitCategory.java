package immersivenuclear.client.jei;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileSulfuricAcidUnit;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.client.resources.I18n;

/** JEI page of the Sulfuric Acid Unit (see {@link PageCategory}): water | the sulfur over the burner, the boiler and its
 * steam drum, the converter, the absorption tower | acid and steam. The recipe draws the text line (TEXT_Y). */
public class SulfuricAcidUnitCategory extends PageCategory<SulfuricAcidUnitRecipe>
{
	public static final String UID = "immersivenuclear.sulfuric_acid_unit";

	public SulfuricAcidUnitCategory(IGuiHelper helper)
	{
		super(helper, "sulfuric_acid_unit", 186, 156, INContent.itemSulfuricAcidUnit);
	}

	@Override
	public void setRecipe(IRecipeLayout layout, SulfuricAcidUnitRecipe recipe, IIngredients ingredients)
	{
		IGuiItemStackGroup items = layout.getItemStacks();
		// JEI puts the item 1 px inside the slot position
		items.init(0, true, 30, 58);
		items.set(ingredients);
		IGuiFluidStackGroup fluids = layout.getFluidStacks();
		tank(fluids, 0, true, 4, 64, TileSulfuricAcidUnit.WATER_CAPACITY);
		tank(fluids, 1, false, 166, 35, TileSulfuricAcidUnit.ACID_CAPACITY);
		tank(fluids, 2, false, 166, 92, TileSulfuricAcidUnit.STEAM_CAPACITY);
		fluids.set(ingredients);
	}

	@Override
	public List<String> getTooltipStrings(int mx, int my)
	{
		String k = "gui.immersivenuclear.sulfuric_acid_unit.";
		if(in(mx, my, 18, 92, 44, 44))
			return Arrays.asList(I18n.format(k + "burner"), I18n.format(k + "burner_info"));
		if(in(mx, my, 102, 40, 26, 96))
			return Arrays.asList(I18n.format(k + "converter"), I18n.format(k + "converter_info"));
		if(in(mx, my, 138, 32, 24, 104))
			return Arrays.asList(I18n.format(k + "tower"), I18n.format(k + "tower_info"));
		return Collections.emptyList();
	}
}
