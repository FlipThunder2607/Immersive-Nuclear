package immersivenuclear.client.jei;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileHFKiln;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

/** JEI page of the Hydrofluoric Acid Kiln (see {@link PageCategory}): the fluorite over the feed head, the rotary drum,
 * the discharge head with the gypsum under it, the scrubber | sulfuric acid in and hydrofluoric acid out. */
public class HFKilnCategory extends PageCategory<HFKilnCategory.Recipe>
{
	public static final String UID = "immersivenuclear.hf_kiln";

	/** One cycle, with the amounts the tile uses. */
	public static class Recipe implements IRecipeWrapper
	{
		@Override
		public void getIngredients(IIngredients ingredients)
		{
			ingredients.setInputLists(VanillaTypes.ITEM, Collections.singletonList(OreDictionary.getOres(TileHFKiln.FLUORITE)));
			ingredients.setInput(VanillaTypes.FLUID, new FluidStack(INContent.fluidSulfuricAcid, TileHFKiln.ACID_PER_CYCLE));
			ingredients.setOutput(VanillaTypes.ITEM, new ItemStack(INContent.itemGypsum, TileHFKiln.GYPSUM_PER_CYCLE));
			ingredients.setOutput(VanillaTypes.FLUID, new FluidStack(INContent.fluidHydrofluoricAcid, TileHFKiln.HF_PER_CYCLE));
		}

		@Override
		public void drawInfo(Minecraft mc, int w, int h, int mx, int my)
		{
			JeiGraphite.text(mc, TileHFKiln.ENERGY_PER_TICK * TileHFKiln.CYCLE_TICKS + " IF   " + TileHFKiln.CYCLE_TICKS / 20 + " s", 0, TEXT_Y, w);
		}
	}

	public HFKilnCategory(IGuiHelper helper)
	{
		super(helper, "hf_kiln", 186, 156, INContent.itemHFKiln);
	}

	@Override
	public void setRecipe(IRecipeLayout layout, Recipe recipe, IIngredients ingredients)
	{
		IGuiItemStackGroup items = layout.getItemStacks();
		// JEI puts the item 1 px inside the slot position
		items.init(0, true, 7, 38);
		items.init(1, false, 137, 118);
		items.set(ingredients);
		IGuiFluidStackGroup fluids = layout.getFluidStacks();
		tank(fluids, 0, true, 164, 35, TileHFKiln.ACID_CAPACITY);
		tank(fluids, 1, false, 164, 92, TileHFKiln.HF_CAPACITY);
		fluids.set(ingredients);
	}

	@Override
	public List<String> getTooltipStrings(int mx, int my)
	{
		String k = "gui.immersivenuclear.hf_kiln.";
		if(in(mx, my, 28, 79, 106, 32))
			return Arrays.asList(I18n.format(k + "drum"), I18n.format(k + "drum_info"));
		return Collections.emptyList();
	}
}
