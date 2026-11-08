package immersivenuclear.client.jei;

import java.util.Collections;
import java.util.Arrays;
import java.util.List;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileConversionUnit;
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
 * JEI page of the Conversion Unit, in the mod's finish: hydrogen and acid at the left, yellowcake on top, the machine in the middle, hexafluoride and
 * fluorinated effluent at the right, and the ash (one every few cycles) under it.
 */
public class ConversionUnitCategory implements IRecipeCategory<ConversionUnitCategory.Recipe>
{
	public static final String UID = ImmersiveNuclear.MODID + ".conversion_unit";
	static final int W = 176, H = 100;
	static final int H2_X = 4, HF_X = 28, UF6_X = 128, EFF_X = 152, TANK_Y = 26, TEXT_Y = 88;
	static final int CAKE_X = 80, CAKE_Y = 4, ASH_X = 80, ASH_Y = 70;
	static final int BODY_X = 58, BODY_Y = 28, BODY_W = 60, BODY_H = 36, ARROW_X = 79, ARROW_Y = 40;
	static final int COL_BODY = 0xff3a3d42, COL_COIL = 0xffe8783c;

	/** One cycle, with the amounts the tile uses. */
	public static class Recipe implements IRecipeWrapper
	{
		@Override
		public void getIngredients(IIngredients ingredients)
		{
			ingredients.setInput(VanillaTypes.ITEM, new ItemStack(INContent.itemYellowcake));
			ingredients.setInputs(VanillaTypes.FLUID, Arrays.asList(new FluidStack(INContent.fluidHydrogen, TileConversionUnit.H2_PER_CYCLE),
					new FluidStack(INContent.fluidHydrofluoricAcid, TileConversionUnit.HF_PER_CYCLE)));
			ingredients.setOutput(VanillaTypes.ITEM, new ItemStack(INContent.itemFluorinationAsh));
			ingredients.setOutputs(VanillaTypes.FLUID, Arrays.asList(new FluidStack(INContent.fluidUF6, TileConversionUnit.UF6_PER_CYCLE),
					new FluidStack(INContent.fluidFluorinatedEffluent, TileConversionUnit.EFFLUENT_PER_CYCLE)));
		}

		@Override
		public void drawInfo(Minecraft mc, int w, int h, int mx, int my)
		{
			JeiGraphite.text(mc, TileConversionUnit.ENERGY_PER_TICK * TileConversionUnit.CYCLE_TICKS + " IF   " + TileConversionUnit.CYCLE_TICKS / 20 + " s", 0, TEXT_Y, w);
		}
	}

	final JeiGraphite g;
	final IDrawable background, icon, arrow;
	final IDrawableAnimated arrowFill;

	public ConversionUnitCategory(IGuiHelper helper)
	{
		g = new JeiGraphite(helper);
		background = helper.createBlankDrawable(W, H);
		arrow = helper.createDrawable(JeiGraphite.GRAPHITE, 184, 0, 19, 13);
		arrowFill = helper.createAnimatedDrawable(helper.createDrawable(JeiGraphite.GRAPHITE, 184, 13, 19, 13), 60, IDrawableAnimated.StartDirection.LEFT, false);
		icon = helper.createDrawableIngredient(new ItemStack(INContent.itemConversionUnit));
	}

	@Override
	public String getUid()
	{
		return UID;
	}

	@Override
	public String getTitle()
	{
		return I18n.format("tile.immersivenuclear.conversion_unit.name");
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
		JeiGraphite.pipe(HF_X + 20, TANK_Y + 26, BODY_X, TANK_Y + 26);
		JeiGraphite.pipe(H2_X + 10, TANK_Y - 1, H2_X + 10, 12);
		JeiGraphite.pipe(H2_X + 10, 12, BODY_X + 8, 12);
		JeiGraphite.pipe(BODY_X + 8, 12, BODY_X + 8, BODY_Y);
		JeiGraphite.pipe(BODY_X + BODY_W, TANK_Y + 26, UF6_X, TANK_Y + 26);
		JeiGraphite.pipe(BODY_X + BODY_W - 8, BODY_Y, BODY_X + BODY_W - 8, 12);
		JeiGraphite.pipe(BODY_X + BODY_W - 8, 12, EFF_X + 10, 12);
		JeiGraphite.pipe(EFF_X + 10, 12, EFF_X + 10, TANK_Y - 1);
		JeiGraphite.rect(CAKE_X + 3, CAKE_Y + 18, 10, BODY_Y - CAKE_Y - 18, JeiGraphite.WELL);
		JeiGraphite.rect(ASH_X + 3, BODY_Y + BODY_H, 10, ASH_Y - 2 - BODY_Y - BODY_H, JeiGraphite.WELL);
		JeiGraphite.rect(BODY_X, BODY_Y, BODY_W, BODY_H, JeiGraphite.WELL);
		JeiGraphite.rect(BODY_X + 2, BODY_Y + 2, BODY_W - 4, BODY_H - 4, COL_BODY);
		for(int x = BODY_X + 5; x < BODY_X + BODY_W - 5; x += 3)
		{
			JeiGraphite.rect(x, BODY_Y + 4, 2, 2, COL_COIL);
			JeiGraphite.rect(x, BODY_Y + BODY_H - 6, 2, 2, COL_COIL);
		}
		GlStateManager.color(1, 1, 1, 1);
		mc.getTextureManager().bindTexture(JeiGraphite.GRAPHITE);
		arrow.draw(mc, ARROW_X, ARROW_Y);
		arrowFill.draw(mc, ARROW_X, ARROW_Y);
		g.slot(mc, CAKE_X, CAKE_Y, true);
		g.slot(mc, ASH_X, ASH_Y, false);
		g.tank(mc, H2_X, TANK_Y, true);
		g.tank(mc, HF_X, TANK_Y, true);
		g.tank(mc, UF6_X, TANK_Y, false);
		g.tank(mc, EFF_X, TANK_Y, false);
	}

	@Override
	public void setRecipe(IRecipeLayout layout, Recipe recipe, IIngredients ingredients)
	{
		IGuiItemStackGroup items = layout.getItemStacks();
		// JEI puts the item 1 px inside the slot position
		items.init(0, true, CAKE_X - 1, CAKE_Y - 1);
		items.init(1, false, ASH_X - 1, ASH_Y - 1);
		items.set(ingredients);
		items.addTooltipCallback((slot, input, stack, tip) -> {
			if(slot == 1)
				tip.add("§7" + I18n.format("jei.immersivenuclear.conversion_unit.ash", TileConversionUnit.CYCLES_PER_ASH));
		});
		IGuiFluidStackGroup fluids = layout.getFluidStacks();
		fluids.init(0, true, H2_X + 2, TANK_Y + 2, 16, 47, TileConversionUnit.H2_CAPACITY, false, g.gauge);
		fluids.init(1, true, HF_X + 2, TANK_Y + 2, 16, 47, TileConversionUnit.HF_CAPACITY, false, g.gauge);
		fluids.init(2, false, UF6_X + 2, TANK_Y + 2, 16, 47, TileConversionUnit.UF6_CAPACITY, false, g.gauge);
		fluids.init(3, false, EFF_X + 2, TANK_Y + 2, 16, 47, TileConversionUnit.EFFLUENT_CAPACITY, false, g.gauge);
		fluids.set(ingredients);
	}

	@Override
	public List<String> getTooltipStrings(int mx, int my)
	{
		return Collections.emptyList();
	}
}
