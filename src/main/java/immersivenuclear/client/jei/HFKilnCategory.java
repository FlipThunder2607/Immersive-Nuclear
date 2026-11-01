package immersivenuclear.client.jei;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileHFKiln;
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
import net.minecraftforge.oredict.OreDictionary;

/**
 * JEI page of the Hydrofluoric Acid Kiln, a compact copy of its interface (GuiHFKiln) in the mod's finish: sulfuric
 * acid and fluorite into the FEED HEAD > ROTARY DRUM (heated zone, the bed from fluorite to gypsum) > gypsum; the gas
 * over the top into the hydrofluoric acid tank.
 */
public class HFKilnCategory implements IRecipeCategory<HFKilnCategory.Recipe>
{
	public static final String UID = ImmersiveNuclear.MODID + ".hf_kiln";
	static final int W = 176, H = 100;
	static final int ACID_X = 4, HF_X = 152, TANK_Y = 26, TEXT_Y = 88;
	static final int FLUORITE_X = 32, FLUORITE_Y = 4;
	static final int FEED_X = 30, FEED_Y = 34, FEED_W = 18, FEED_H = 30;
	static final int DRUM_X = 50, DRUM_Y = 38, DRUM_W = 62, DRUM_H = 22;
	static final int DISCH_X = 114, DISCH_Y = 34, DISCH_W = 14, DISCH_H = 30;
	static final int GYPSUM_X = 112, GYPSUM_Y = 70, ARROW_X = 88, ARROW_Y = 70;
	static final int COL_SHELL = 0xff6a3e2a, COL_COIL = 0xffe8783c, COL_FLUORITE = 0xffa06cd0, COL_GYPSUM = 0xffeeece2, COL_HEAD = 0xff3a3d42;

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

	final JeiGraphite g;
	final IDrawable background, icon, arrow;
	final IDrawableAnimated arrowFill;

	public HFKilnCategory(IGuiHelper helper)
	{
		g = new JeiGraphite(helper);
		background = helper.createBlankDrawable(W, H);
		arrow = helper.createDrawable(JeiGraphite.GRAPHITE, 184, 0, 19, 13);
		arrowFill = helper.createAnimatedDrawable(helper.createDrawable(JeiGraphite.GRAPHITE, 184, 13, 19, 13), 60, IDrawableAnimated.StartDirection.LEFT, false);
		icon = helper.createDrawableIngredient(new ItemStack(INContent.itemHFKiln));
	}

	@Override
	public String getUid()
	{
		return UID;
	}

	@Override
	public String getTitle()
	{
		return I18n.format("tile.immersivenuclear.hf_kiln.name");
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

	static int mix(int a, int b, float t)
	{
		int c = 0xff000000;
		for(int sh = 0; sh <= 16; sh += 8)
			c |= Math.round((a >> sh & 255) + ((b >> sh & 255) - (a >> sh & 255)) * t) << sh;
		return c;
	}

	@Override
	public void drawExtras(Minecraft mc)
	{
		g.plate(mc, W, H);
		// Pipes first, the wells over their ends: acid into the feed head, the gas over the top to the HF tank
		JeiGraphite.pipe(ACID_X + 20, TANK_Y + 26, FEED_X, TANK_Y + 26);
		JeiGraphite.pipe(FEED_X + 14, FEED_Y, FEED_X + 14, 12);
		JeiGraphite.pipe(FEED_X + 14, 12, HF_X + 10, 12);
		JeiGraphite.pipe(HF_X + 10, 12, HF_X + 10, TANK_Y - 1);
		JeiGraphite.rect(FLUORITE_X + 3, FLUORITE_Y + 18, 10, FEED_Y - FLUORITE_Y - 18, JeiGraphite.WELL);
		JeiGraphite.rect(DISCH_X + 2, DISCH_Y + DISCH_H, 10, GYPSUM_Y - 2 - DISCH_Y - DISCH_H, JeiGraphite.WELL);
		for(int[] b : new int[][]{{FEED_X, FEED_Y, FEED_W, FEED_H}, {DISCH_X, DISCH_Y, DISCH_W, DISCH_H}})
		{
			JeiGraphite.rect(b[0], b[1], b[2], b[3], JeiGraphite.WELL);
			JeiGraphite.rect(b[0] + 2, b[1] + 2, b[2] - 4, b[3] - 4, COL_HEAD);
		}
		// The drum: shell, the bed from fluorite to gypsum, the coils on the hot zone
		JeiGraphite.rect(DRUM_X, DRUM_Y, DRUM_W, DRUM_H, JeiGraphite.WELL);
		JeiGraphite.rect(DRUM_X + 2, DRUM_Y + 2, DRUM_W - 4, DRUM_H - 4, COL_SHELL);
		JeiGraphite.rect(DRUM_X + 4, DRUM_Y + 5, DRUM_W - 8, DRUM_H - 10, 0xff1c1818);
		for(int i = 0; i < DRUM_W - 8; i++)
			JeiGraphite.rect(DRUM_X + 4 + i, DRUM_Y + DRUM_H - 10, 1, 5, mix(COL_FLUORITE, COL_GYPSUM, i / (float)(DRUM_W - 9)));
		for(int x = DRUM_X + 4; x < DRUM_X + 24; x += 3)
		{
			JeiGraphite.rect(x, DRUM_Y + 2, 2, 2, COL_COIL);
			JeiGraphite.rect(x, DRUM_Y + DRUM_H - 4, 2, 2, COL_COIL);
		}

		GlStateManager.color(1, 1, 1, 1);
		mc.getTextureManager().bindTexture(JeiGraphite.GRAPHITE);
		arrow.draw(mc, ARROW_X, ARROW_Y);
		arrowFill.draw(mc, ARROW_X, ARROW_Y);
		g.slot(mc, FLUORITE_X, FLUORITE_Y, true);
		g.slot(mc, GYPSUM_X, GYPSUM_Y, false);
		g.tank(mc, ACID_X, TANK_Y, true);
		g.tank(mc, HF_X, TANK_Y, false);
	}

	@Override
	public void setRecipe(IRecipeLayout layout, Recipe recipe, IIngredients ingredients)
	{
		IGuiItemStackGroup items = layout.getItemStacks();
		// JEI puts the item 1 px inside the slot position
		items.init(0, true, FLUORITE_X - 1, FLUORITE_Y - 1);
		items.init(1, false, GYPSUM_X - 1, GYPSUM_Y - 1);
		items.set(ingredients);
		IGuiFluidStackGroup fluids = layout.getFluidStacks();
		fluids.init(0, true, ACID_X + 2, TANK_Y + 2, 16, 47, TileHFKiln.ACID_CAPACITY, false, g.gauge);
		fluids.init(1, false, HF_X + 2, TANK_Y + 2, 16, 47, TileHFKiln.HF_CAPACITY, false, g.gauge);
		fluids.set(ingredients);
	}

	@Override
	public List<String> getTooltipStrings(int mx, int my)
	{
		String k = "gui.immersivenuclear.hf_kiln.";
		if(mx >= DRUM_X && mx < DRUM_X + DRUM_W && my >= DRUM_Y && my < DRUM_Y + DRUM_H)
			return Arrays.asList(I18n.format(k + "drum"), I18n.format(k + "drum_info"));
		return Collections.emptyList();
	}
}
