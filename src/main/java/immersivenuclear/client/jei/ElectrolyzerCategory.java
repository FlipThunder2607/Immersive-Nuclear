package immersivenuclear.client.jei;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileElectrolyzer;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * JEI page of the Electrolyzer, a compact copy of its interface in the mod's finish: water into the ELECTROLYTE TANK
 * over the CELL STACKS; the gas rises into the OXYGEN and HYDROGEN SEPARATORS, oxygen over the top to its tank,
 * hydrogen past the arrow to its own.
 */
public class ElectrolyzerCategory implements IRecipeCategory<ElectrolyzerCategory.Recipe>
{
	public static final String UID = ImmersiveNuclear.MODID + ".electrolyzer";
	static final int W = 176, H = 100;
	static final int WATER_X = 4, TANK_Y = 26, H2_X = 124, O2_X = 150, TEXT_Y = 88;
	static final int ELEC_X = 28, ELEC_Y = 22, ELEC_W = 40, ELEC_H = 14;
	static final int STACK_X = 28, STACK_Y = 40, STACK_W = 40, STACK_H = 36;
	static final int SEP_O2_X = 74, SEP_H2_X = 90, SEP_Y = 18, SEP_W = 12, SEP_H = 58;
	static final int ARROW_X = 104, ARROW_Y = 44;
	/** The oxygen's line over the top of the page, to its tank. */
	static final int O2_LINE_Y = 8;
	static final int COL_INSIDE = 0xff24262a, COL_ELECTROLYTE = 0xff3c6e9a, COL_PLATE = 0xff96999e, COL_COPPER = 0xffb8693a,
			COL_BUBBLE = 0xffdfe8ee, COL_O2 = 0xff8fb4d8, COL_H2 = 0xffe6e6e6;

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

	final JeiGraphite g;
	final IDrawable background, icon, arrow;
	final IDrawableAnimated arrowFill;

	public ElectrolyzerCategory(IGuiHelper helper)
	{
		g = new JeiGraphite(helper);
		background = helper.createBlankDrawable(W, H);
		arrow = helper.createDrawable(JeiGraphite.GRAPHITE, 184, 0, 19, 13);
		arrowFill = helper.createAnimatedDrawable(helper.createDrawable(JeiGraphite.GRAPHITE, 184, 13, 19, 13), 60, IDrawableAnimated.StartDirection.LEFT, false);
		icon = helper.createDrawableIngredient(new ItemStack(INContent.itemElectrolyzer));
	}

	@Override
	public String getUid()
	{
		return UID;
	}

	@Override
	public String getTitle()
	{
		return I18n.format("tile.immersivenuclear.electrolyzer.name");
	}

	@Override
	public String getModName()
	{
		return "Immersive Nuclear";
	}

	@Override
	public IDrawable getBackground()
	{
		return background;
	}

	@Override
	public IDrawable getIcon()
	{
		return icon;
	}

	/** A separator column: electrolyte at the foot, bubbles rising through the gas. */
	static void separator(int x, int gas)
	{
		JeiGraphite.rect(x, SEP_Y, SEP_W, SEP_H, JeiGraphite.WELL);
		JeiGraphite.rect(x + 2, SEP_Y + 2, SEP_W - 4, SEP_H - 4, COL_INSIDE);
		JeiGraphite.rect(x + 2, SEP_Y + 2, SEP_W - 4, 18, gas);
		JeiGraphite.rect(x + 2, SEP_Y + SEP_H - 18, SEP_W - 4, 16, COL_ELECTROLYTE);
		for(int y = SEP_Y + 24; y < SEP_Y + SEP_H - 20; y += 5)
			JeiGraphite.rect(x + 4 + (y / 5) % 3, y, 2, 2, COL_BUBBLE);
	}

	@Override
	public void drawExtras(Minecraft mc)
	{
		g.plate(mc, W, H);
		// Pipes first, the wells over their ends: water to the electrolyte tank, the stacks' gas up into the two
		// separators, oxygen over the top to its tank, hydrogen past the arrow to its own
		JeiGraphite.pipe(WATER_X + 10, TANK_Y - 1, WATER_X + 10, 14);
		JeiGraphite.pipe(WATER_X + 10, 14, ELEC_X + ELEC_W / 2, 14);
		JeiGraphite.pipe(ELEC_X + ELEC_W / 2, 14, ELEC_X + ELEC_W / 2, ELEC_Y);
		JeiGraphite.pipe(STACK_X + STACK_W, STACK_Y + 10, SEP_O2_X, STACK_Y + 10);
		JeiGraphite.pipe(SEP_O2_X + SEP_W, STACK_Y + 24, SEP_H2_X, STACK_Y + 24);
		JeiGraphite.pipe(STACK_X + STACK_W, STACK_Y + 24, SEP_O2_X, STACK_Y + 24);
		JeiGraphite.pipe(SEP_O2_X + SEP_W / 2, SEP_Y, SEP_O2_X + SEP_W / 2, O2_LINE_Y);
		JeiGraphite.pipe(SEP_O2_X + SEP_W / 2, O2_LINE_Y, O2_X + 10, O2_LINE_Y);
		JeiGraphite.pipe(O2_X + 10, O2_LINE_Y, O2_X + 10, TANK_Y - 1);
		JeiGraphite.pipe(SEP_H2_X + SEP_W, ARROW_Y + 6, ARROW_X, ARROW_Y + 6);
		JeiGraphite.pipe(ARROW_X + 19, ARROW_Y + 6, H2_X, ARROW_Y + 6);

		// The electrolyte tank over the stacks
		JeiGraphite.rect(ELEC_X, ELEC_Y, ELEC_W, ELEC_H, JeiGraphite.WELL);
		JeiGraphite.rect(ELEC_X + 2, ELEC_Y + 2, ELEC_W - 4, ELEC_H - 4, COL_INSIDE);
		JeiGraphite.rect(ELEC_X + 2, ELEC_Y + 5, ELEC_W - 4, ELEC_H - 7, COL_ELECTROLYTE);
		// The cell stacks: steel plates with copper bus bars between them
		JeiGraphite.rect(STACK_X, STACK_Y, STACK_W, STACK_H, JeiGraphite.WELL);
		JeiGraphite.rect(STACK_X + 2, STACK_Y + 2, STACK_W - 4, STACK_H - 4, COL_INSIDE);
		for(int x = STACK_X + 3; x < STACK_X + STACK_W - 3; x += 4)
		{
			JeiGraphite.rect(x, STACK_Y + 5, 2, STACK_H - 10, COL_PLATE);
			JeiGraphite.rect(x + 2, STACK_Y + 5, 1, STACK_H - 10, COL_ELECTROLYTE);
		}
		JeiGraphite.rect(STACK_X + 2, STACK_Y + 2, STACK_W - 4, 2, COL_COPPER);
		JeiGraphite.rect(STACK_X + 2, STACK_Y + STACK_H - 4, STACK_W - 4, 2, COL_COPPER);
		separator(SEP_O2_X, COL_O2);
		separator(SEP_H2_X, COL_H2);

		GlStateManager.color(1, 1, 1, 1);
		mc.getTextureManager().bindTexture(JeiGraphite.GRAPHITE);
		arrow.draw(mc, ARROW_X, ARROW_Y);
		arrowFill.draw(mc, ARROW_X, ARROW_Y);
		g.tank(mc, WATER_X, TANK_Y, true);
		g.tank(mc, H2_X, TANK_Y, false);
		g.tank(mc, O2_X, TANK_Y, false);
	}

	@Override
	public void setRecipe(IRecipeLayout layout, Recipe recipe, IIngredients ingredients)
	{
		IGuiFluidStackGroup fluids = layout.getFluidStacks();
		fluids.init(0, true, WATER_X + 2, TANK_Y + 2, 16, 47, TileElectrolyzer.WATER_CAPACITY, false, g.gauge);
		fluids.init(1, false, H2_X + 2, TANK_Y + 2, 16, 47, TileElectrolyzer.H2_CAPACITY, false, g.gauge);
		fluids.init(2, false, O2_X + 2, TANK_Y + 2, 16, 47, TileElectrolyzer.O2_CAPACITY, false, g.gauge);
		fluids.set(ingredients);
	}

	static boolean in(int mx, int my, int x, int y, int w, int h)
	{
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	@Override
	public List<String> getTooltipStrings(int mx, int my)
	{
		String k = "gui.immersivenuclear.electrolyzer.";
		if(in(mx, my, STACK_X, STACK_Y, STACK_W, STACK_H))
			return Collections.singletonList(I18n.format(k + "stack"));
		if(in(mx, my, ELEC_X, ELEC_Y, ELEC_W, ELEC_H))
			return Arrays.asList(I18n.format(k + "electrolyte"), I18n.format(k + "electrolyte_info"));
		if(in(mx, my, SEP_O2_X, SEP_Y, SEP_W, SEP_H))
			return Collections.singletonList(I18n.format(k + "separator_o2"));
		if(in(mx, my, SEP_H2_X, SEP_Y, SEP_W, SEP_H))
			return Collections.singletonList(I18n.format(k + "separator_h2"));
		return Collections.emptyList();
	}
}
