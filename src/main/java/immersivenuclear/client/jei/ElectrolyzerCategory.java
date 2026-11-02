package immersivenuclear.client.jei;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileElectrolyzer;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/** JEI page of the Electrolyzer (see {@link PageCategory}): water | the electrolyte tank over the cell stacks, the
 * oxygen and hydrogen separators | hydrogen and oxygen out. The rectifier is left out of the crop (the page is 186 wide). */
public class ElectrolyzerCategory extends PageCategory<ElectrolyzerCategory.Recipe>
{
	public static final String UID = "immersivenuclear.electrolyzer";

	/** One cycle on water, or on steam (less power: part of the energy is already in it). */
	public static class Recipe implements IRecipeWrapper
	{
		final Fluid feed;
		final boolean steam;

		public Recipe(Fluid feed, boolean steam)
		{
			this.feed = feed;
			this.steam = steam;
		}

		@Override
		public void getIngredients(IIngredients ingredients)
		{
			ingredients.setInput(VanillaTypes.FLUID, new FluidStack(feed, TileElectrolyzer.WATER_PER_CYCLE));
			ingredients.setOutputs(VanillaTypes.FLUID, Arrays.asList(new FluidStack(INContent.fluidHydrogen, TileElectrolyzer.H2_PER_CYCLE),
					new FluidStack(INContent.fluidOxygen, TileElectrolyzer.O2_PER_CYCLE)));
		}

		@Override
		public void drawInfo(Minecraft mc, int w, int h, int mx, int my)
		{
			int perTick = steam ? TileElectrolyzer.ENERGY_PER_TICK_STEAM : TileElectrolyzer.ENERGY_PER_TICK;
			JeiGraphite.text(mc, perTick * TileElectrolyzer.CYCLE_TICKS + " IF   " + TileElectrolyzer.CYCLE_TICKS / 20 + " s", 0, TEXT_Y, w);
		}
	}

	/** The water recipe, and the steam one when some mod registers steam. */
	public static List<Recipe> recipes()
	{
		Fluid steam = FluidRegistry.getFluid("steam");
		return steam == null ? Collections.singletonList(new Recipe(FluidRegistry.WATER, false))
				: Arrays.asList(new Recipe(FluidRegistry.WATER, false), new Recipe(steam, true));
	}

	public ElectrolyzerCategory(IGuiHelper helper)
	{
		super(helper, "electrolyzer", 186, 156, INContent.itemElectrolyzer);
	}

	@Override
	public void setRecipe(IRecipeLayout layout, Recipe recipe, IIngredients ingredients)
	{
		IGuiFluidStackGroup fluids = layout.getFluidStacks();
		tank(fluids, 0, true, 4, 64, TileElectrolyzer.WATER_CAPACITY);
		tank(fluids, 1, false, 166, 35, TileElectrolyzer.H2_CAPACITY);
		tank(fluids, 2, false, 166, 92, TileElectrolyzer.O2_CAPACITY);
		fluids.set(ingredients);
	}

	@Override
	public List<String> getTooltipStrings(int mx, int my)
	{
		String k = "gui.immersivenuclear.electrolyzer.";
		// the page shows the sheet from x 92, after a 24 px side panel
		int dx = 24 - 92;
		if(in(mx, my, 98 + dx - 5, 104 - 7, 72 + 10, 32 + 7))
			return Collections.singletonList(I18n.format(k + "stack"));
		if(in(mx, my, 104 + dx, 54, 60, 28))
			return Arrays.asList(I18n.format(k + "electrolyte"), I18n.format(k + "electrolyte_info"));
		if(in(mx, my, 180 + dx, 44 - 12, 20, 92 + 12))
			return Collections.singletonList(I18n.format(k + "separator_o2"));
		if(in(mx, my, 206 + dx, 48 - 12, 22, 88 + 12))
			return Collections.singletonList(I18n.format(k + "separator_h2"));
		return Collections.emptyList();
	}
}
