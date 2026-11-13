package immersivenuclear.client.jei;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileDeconversionUnit;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * JEI page of the Deconversion Unit, a compact copy of its interface (GuiDeconversionUnit) in the mod's finish: depleted
 * hexafluoride and steam into the REACTOR on the FEED HOOD > ROTARY KILN through the ELECTRIC FURNACE (the bed from
 * uranyl fluoride to oxide) > the oxide; the gas over the top into the hydrofluoric acid tank.
 */
public class DeconversionUnitCategory implements IRecipeCategory<DeconversionUnitCategory.Recipe>
{
	public static final String UID = ImmersiveNuclear.MODID + ".deconversion_unit";
	static final int W = 176, H = 100;
	static final int UF6_X = 4, STEAM_X = 28, HF_X = 152, TANK_Y = 26, TEXT_Y = 88;
	static final int REACTOR_X = 53, REACTOR_Y = 10, REACTOR_W = 14, REACTOR_H = 24;
	static final int FEED_X = 51, FEED_Y = 36, FEED_W = 18, FEED_H = 28;
	static final int DRUM_X = 70, DRUM_Y = 40, DRUM_W = 44, DRUM_H = 20;
	static final int FURN_X = 82, FURN_Y = 35, FURN_W = 20, FURN_H = 30;
	static final int DISCH_X = 115, DISCH_Y = 36, DISCH_W = 14, DISCH_H = 28;
	static final int OXIDE_X = 113, OXIDE_Y = 70, ARROW_X = 89, ARROW_Y = 70;
	static final int COL_SHELL = 0xff5c6066, COL_FURN = 0xff3a3d42, COL_GLOW = 0xffff8a3c, COL_FEED = 0xffe0cc5c, COL_OXIDE = 0xff2e2a26,
			COL_HEAD = 0xff3a3d42, COL_REACTOR = 0xff9a9ea4, COL_HEATER = 0xffb06448;

	/** One cycle, with the amounts the tile uses. */
	public static class Recipe implements IRecipeWrapper
	{
		@Override
		public void getIngredients(IIngredients ingredients)
		{
			ingredients.setInputs(VanillaTypes.FLUID, Arrays.asList(new FluidStack(INContent.fluidUF6Depleted, TileDeconversionUnit.UF6_PER_CYCLE),
					new FluidStack(INContent.fluidSteam, TileDeconversionUnit.STEAM_PER_CYCLE)));
			ingredients.setOutput(VanillaTypes.ITEM, new ItemStack(INContent.itemDepletedUraniumOxide, TileDeconversionUnit.OXIDE_PER_CYCLE));
			ingredients.setOutput(VanillaTypes.FLUID, new FluidStack(INContent.fluidHydrofluoricAcid, TileDeconversionUnit.HF_PER_CYCLE));
		}

		@Override
		public void drawInfo(Minecraft mc, int w, int h, int mx, int my)
		{
			JeiGraphite.text(mc, TileDeconversionUnit.ENERGY_PER_TICK * TileDeconversionUnit.CYCLE_TICKS + " IF   " + TileDeconversionUnit.CYCLE_TICKS / 20 + " s", 0, TEXT_Y, w);
		}
	}

	final JeiGraphite g;
	final IDrawable background, icon, arrow;
	final IDrawableAnimated arrowFill;

	public DeconversionUnitCategory(IGuiHelper helper)
	{
		g = new JeiGraphite(helper);
		background = helper.createBlankDrawable(W, H);
		arrow = helper.createDrawable(JeiGraphite.GRAPHITE, 184, 0, 19, 13);
		arrowFill = helper.createAnimatedDrawable(helper.createDrawable(JeiGraphite.GRAPHITE, 184, 13, 19, 13), 60, IDrawableAnimated.StartDirection.LEFT, false);
		icon = helper.createDrawableIngredient(new ItemStack(INContent.itemDeconversionUnit));
	}

	@Override
	public String getUid()
	{
		return UID;
	}

	@Override
	public String getTitle()
	{
		return I18n.format("tile.immersivenuclear.deconversion_unit.name");
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

	@Override
	public void drawExtras(Minecraft mc)
	{
		g.plate(mc, W, H);
		// Pipes first, the wells over their ends: hexafluoride over the top and steam across into the reactor; the gas off
		// the reactor's top over to the HF tank
		JeiGraphite.pipe(UF6_X + 10, TANK_Y - 1, UF6_X + 10, 16);
		JeiGraphite.pipe(UF6_X + 10, 16, REACTOR_X, 16);
		JeiGraphite.pipe(STEAM_X + 20, 28, REACTOR_X, 28);
		JeiGraphite.pipe(REACTOR_X + 7, REACTOR_Y, REACTOR_X + 7, 5);
		JeiGraphite.pipe(REACTOR_X + 7, 5, HF_X + 10, 5);
		JeiGraphite.pipe(HF_X + 10, 5, HF_X + 10, TANK_Y - 1);
		JeiGraphite.rect(DISCH_X + 2, DISCH_Y + DISCH_H, 10, OXIDE_Y - 2 - DISCH_Y - DISCH_H, JeiGraphite.WELL);
		for(int[] b : new int[][]{{FEED_X, FEED_Y, FEED_W, FEED_H}, {DISCH_X, DISCH_Y, DISCH_W, DISCH_H}})
		{
			JeiGraphite.rect(b[0], b[1], b[2], b[3], JeiGraphite.WELL);
			JeiGraphite.rect(b[0] + 2, b[1] + 2, b[2] - 4, b[3] - 4, COL_HEAD);
		}
		// The reactor: shell and two heater bands
		JeiGraphite.rect(REACTOR_X, REACTOR_Y, REACTOR_W, REACTOR_H, JeiGraphite.WELL);
		JeiGraphite.rect(REACTOR_X + 2, REACTOR_Y + 2, REACTOR_W - 4, REACTOR_H - 4, COL_REACTOR);
		for(int y : new int[]{REACTOR_Y + 5, REACTOR_Y + 15})
			JeiGraphite.rect(REACTOR_X + 1, y, REACTOR_W - 2, 3, COL_HEATER);
		// The kiln: shell, the bed from uranyl fluoride to oxide
		JeiGraphite.rect(DRUM_X, DRUM_Y, DRUM_W, DRUM_H, JeiGraphite.WELL);
		JeiGraphite.rect(DRUM_X + 2, DRUM_Y + 2, DRUM_W - 4, DRUM_H - 4, COL_SHELL);
		JeiGraphite.rect(DRUM_X + 4, DRUM_Y + 5, DRUM_W - 8, DRUM_H - 10, 0xff1c1818);
		for(int i = 0; i < DRUM_W - 8; i++)
			JeiGraphite.rect(DRUM_X + 4 + i, DRUM_Y + DRUM_H - 9, 1, 4, HFKilnCategory.mix(COL_FEED, COL_OXIDE, i / (float)(DRUM_W - 9)));
		// The electric furnace round its middle, its slots glowing
		JeiGraphite.rect(FURN_X, FURN_Y, FURN_W, FURN_H, JeiGraphite.WELL);
		JeiGraphite.rect(FURN_X + 2, FURN_Y + 2, FURN_W - 4, FURN_H - 4, COL_FURN);
		for(int k = 0; k < 3; k++)
			JeiGraphite.rect(FURN_X + 4, FURN_Y + 6 + k * 7, FURN_W - 8, 2, COL_GLOW);

		GlStateManager.color(1, 1, 1, 1);
		mc.getTextureManager().bindTexture(JeiGraphite.GRAPHITE);
		arrow.draw(mc, ARROW_X, ARROW_Y);
		arrowFill.draw(mc, ARROW_X, ARROW_Y);
		g.slot(mc, OXIDE_X, OXIDE_Y, false);
		g.tank(mc, UF6_X, TANK_Y, true);
		g.tank(mc, STEAM_X, TANK_Y, true);
		g.tank(mc, HF_X, TANK_Y, false);
	}

	@Override
	public void setRecipe(IRecipeLayout layout, Recipe recipe, IIngredients ingredients)
	{
		IGuiItemStackGroup items = layout.getItemStacks();
		// JEI puts the item 1 px inside the slot position
		items.init(0, false, OXIDE_X - 1, OXIDE_Y - 1);
		items.set(ingredients);
		IGuiFluidStackGroup fluids = layout.getFluidStacks();
		fluids.init(0, true, UF6_X + 2, TANK_Y + 2, 16, 47, TileDeconversionUnit.UF6_CAPACITY / 4, false, g.gauge);
		fluids.init(1, true, STEAM_X + 2, TANK_Y + 2, 16, 47, TileDeconversionUnit.STEAM_CAPACITY / 4, false, g.gauge);
		fluids.init(2, false, HF_X + 2, TANK_Y + 2, 16, 47, TileDeconversionUnit.HF_CAPACITY / 4, false, g.gauge);
		fluids.set(ingredients);
	}

	@Override
	public List<String> getTooltipStrings(int mx, int my)
	{
		String k = "gui.immersivenuclear.deconversion_unit.";
		if(mx >= REACTOR_X && mx < REACTOR_X + REACTOR_W && my >= REACTOR_Y && my < REACTOR_Y + REACTOR_H)
			return Arrays.asList(I18n.format(k + "reactor"), I18n.format(k + "reactor_info"));
		if(mx >= FURN_X && mx < FURN_X + FURN_W && my >= FURN_Y && my < FURN_Y + FURN_H)
			return Arrays.asList(I18n.format(k + "furnace"), I18n.format(k + "furnace_info"));
		if(mx >= DRUM_X && mx < DRUM_X + DRUM_W && my >= DRUM_Y && my < DRUM_Y + DRUM_H)
			return Arrays.asList(I18n.format(k + "kiln"), I18n.format(k + "kiln_info"));
		return Collections.emptyList();
	}
}
