package immersivenuclear.client.jei;

import java.util.Collections;
import java.util.List;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileMetalExtractor;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.client.resources.I18n;

/** JEI page of the Metal Extractor (see {@link PageCategory}): the vat with its plates (the effluent in it) and the plate
 * strip | the depleted effluent and the metal deposit. The recipe draws the text line (TEXT_X, TEXT_Y). */
public class MetalExtractorCategory extends PageCategory<MetalExtractorRecipe>
{
	public static final String UID = "immersivenuclear.metal_extractor";
	static final int TEXT_X = 0;

	public MetalExtractorCategory(IGuiHelper helper)
	{
		super(helper, "metal_extractor", 174, 156, INContent.itemMetalExtractor);
	}

	@Override
	public void setRecipe(IRecipeLayout layout, MetalExtractorRecipe recipe, IIngredients ingredients)
	{
		IGuiItemStackGroup items = layout.getItemStacks();
		// JEI puts the item 1 px inside the slot position
		items.init(0, false, 151, 118);
		items.init(1, true, 21, 118);
		items.set(ingredients);
		items.addTooltipCallback((slot, input, stack, tooltip) -> {
			if(slot == 1)
				tooltip.add("§7" + I18n.format("jei.immersivenuclear.metal_extractor.electrodes"));
		});
		IGuiFluidStackGroup fluids = layout.getFluidStacks();
		// The vat holds exactly one batch
		fluids.init(0, true, 18, 70, 114, 32, TileMetalExtractor.BATCH, false, null);
		tank(fluids, 1, false, 152, 64, TileMetalExtractor.DEPLETED_CAPACITY);
		fluids.set(ingredients);
	}

	@Override
	public List<String> getTooltipStrings(int mx, int my)
	{
		if(in(mx, my, 8, 40, 134, 30))
			return Collections.singletonList(I18n.format("jei.immersivenuclear.metal_extractor.plates", TileMetalExtractor.RECOVER_TICKS / 20,
					TileMetalExtractor.RECOVER_ENERGY));
		return Collections.emptyList();
	}
}
