package immersivenuclear.client.jei;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileIndustrialDryer;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

/**
 * JEI page of the Industrial Dryer, a compact copy of its interface (GuiIndustrialDryer) in the mod's finish:
 * wet concentrate through the rotary valve > DRYING CHAMBER (heaters along the floor, the bed going from wet to dry,
 * the paddles) > yellowcake; the vapour goes up through the SCRUBBER (its fines back into the chamber) and the
 * CONDENSER into the condensate tank.
 */
public class IndustrialDryerCategory implements IRecipeCategory<IndustrialDryerRecipe>
{
	public static final String UID = ImmersiveNuclear.MODID + ".industrial_dryer";
	static final int W = 176, H = 100;
	static final int CX = 8, CY = 30, CW = 96, CH = 46, HEATER_H = 4, BED_DEPTH = 14, PADDLES = 7, PADDLE_REACH = 9;
	static final int WET_X = CX + 10, WET_Y = 6;
	static final int ARROW_X = CX + CW + 4, ARROW_Y = CY + CH / 2 - 6;
	static final int CAKE_X = ARROW_X + 24, CAKE_Y = ARROW_Y - 2;
	static final int OUT_X = 152, TANK_Y = 26, TEXT_Y = 88;
	static final int DOME_X = CX + CW - 14, VAPOUR_Y = 12, COND_X = 116, COND_Y = 5, COND_W = 26, COND_H = 15;
	/** The scrubber on the vapour line, between the dome and the condenser. */
	static final int SCRUB_X = DOME_X + 5, SCRUB_Y = 5, SCRUB_W = 12, SCRUB_H = 14;
	static final int COL_STEEL = 0xff96999e, COL_STEEL_DARK = 0xff5c5f64, COL_INSIDE = 0xff24262a, COL_BAND = 0xff3a3d42;
	static final int[] WET = {0x8a, 0x78, 0x34}, DRY = {0xe8, 0xc8, 0x40};

	final JeiGraphite g;
	final IDrawable background, icon, arrow;
	final IDrawableAnimated arrowFill;

	public IndustrialDryerCategory(IGuiHelper helper)
	{
		g = new JeiGraphite(helper);
		background = helper.createBlankDrawable(W, H);
		arrow = helper.createDrawable(JeiGraphite.GRAPHITE, 184, 0, 19, 13);
		arrowFill = helper.createAnimatedDrawable(helper.createDrawable(JeiGraphite.GRAPHITE, 184, 13, 19, 13), 60, IDrawableAnimated.StartDirection.LEFT, false);
		icon = helper.createDrawableIngredient(new ItemStack(INContent.itemIndustrialDryer));
	}

	@Override
	public String getUid()
	{
		return UID;
	}

	@Override
	public String getTitle()
	{
		return I18n.format("tile.immersivenuclear.industrial_dryer.name");
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

	static int mix(float t)
	{
		t = Math.max(0, Math.min(1, t));
		return 0xff000000 | Math.round(WET[0] + (DRY[0] - WET[0]) * t) << 16 | Math.round(WET[1] + (DRY[1] - WET[1]) * t) << 8
				| Math.round(WET[2] + (DRY[2] - WET[2]) * t);
	}

	@Override
	public void drawExtras(Minecraft mc)
	{
		g.plate(mc, W, H);
		// The rotary valve's chute down into the chamber
		JeiGraphite.rect(WET_X + 3, WET_Y + 18, 10, CY - WET_Y - 18, COL_STEEL_DARK);
		JeiGraphite.rect(WET_X + 5, WET_Y + 20, 6, CY - WET_Y - 20, JeiGraphite.WELL);
		// Vapour: chamber dome > condenser > condensate tank
		JeiGraphite.pipe(DOME_X, CY - 1, DOME_X, VAPOUR_Y);
		JeiGraphite.pipe(DOME_X, VAPOUR_Y, COND_X - 1, VAPOUR_Y);
		JeiGraphite.pipe(COND_X + COND_W, VAPOUR_Y, OUT_X + 10, VAPOUR_Y);
		JeiGraphite.pipe(OUT_X + 10, VAPOUR_Y, OUT_X + 10, TANK_Y - 1);
		JeiGraphite.rect(COND_X, COND_Y, COND_W, COND_H, JeiGraphite.WELL);
		JeiGraphite.rect(SCRUB_X, SCRUB_Y, SCRUB_W, SCRUB_H, JeiGraphite.WELL);
		JeiGraphite.rect(SCRUB_X + 2, SCRUB_Y + 3, SCRUB_W - 4, 1, COL_STEEL);
		JeiGraphite.rect(SCRUB_X + 2, SCRUB_Y + SCRUB_H - 5, SCRUB_W - 4, 3, mix(1));
		for(int x = COND_X + 3; x < COND_X + COND_W - 2; x += 3)
			JeiGraphite.rect(x, COND_Y + 2, 1, COND_H - 4, COL_STEEL_DARK);

		// The chamber: the heaters glowing along the floor, the bed from wet (feed end) to dry (discharge end), the paddles
		JeiGraphite.rect(CX, CY, CW, CH, JeiGraphite.WELL);
		int ix = CX + 2, iy = CY + 2, iw = CW - 4, ih = CH - 4, floor = iy + ih - HEATER_H;
		JeiGraphite.rect(ix, iy, iw, ih, COL_INSIDE);
		for(int k = 1; k < 4; k++)
			JeiGraphite.rect(ix + k * iw / 4, iy, 1, ih, COL_BAND);
		for(int i = 0; i < iw; i += 3)
			JeiGraphite.rect(ix + i, floor, Math.min(3, iw - i), HEATER_H, i / 3 % 2 == 0 ? 0xffe65a0f : 0xffaa3c0f);
		for(int i = 0; i < iw; i++)
		{
			int top = floor - BED_DEPTH + Math.round((float)Math.sin(i / 6.0));
			JeiGraphite.rect(ix + i, top, 1, floor - top, mix(i / (float)(iw - 1)));
		}
		int shaftY = floor - BED_DEPTH / 2 - 1;
		JeiGraphite.rect(ix + 2, shaftY, iw - 4, 2, COL_STEEL_DARK);
		for(int k = 0; k < PADDLES; k++)
		{
			int px = ix + 6 + k * (iw - 12) / (PADDLES - 1);
			boolean up = k % 2 == 0;
			JeiGraphite.rect(px, up ? shaftY - PADDLE_REACH : shaftY + 2, 2, PADDLE_REACH, COL_STEEL);
			JeiGraphite.rect(px - 1, up ? shaftY - PADDLE_REACH : shaftY + PADDLE_REACH, 4, 2, COL_STEEL);
		}

		GlStateManager.color(1, 1, 1, 1);
		mc.getTextureManager().bindTexture(JeiGraphite.GRAPHITE);
		arrow.draw(mc, ARROW_X, ARROW_Y);
		arrowFill.draw(mc, ARROW_X, ARROW_Y);
		g.slot(mc, WET_X, WET_Y, true);
		g.slot(mc, CAKE_X, CAKE_Y, false);
		g.tank(mc, OUT_X, TANK_Y, false);
		JeiGraphite.text(mc, IndustrialDryerRecipe.energy() + " IF   " + IndustrialDryerRecipe.ticks() / 20 + " s   "
				+ TileIndustrialDryer.DRYING_TEMPERATURE + " °C", 0, TEXT_Y, W);
	}

	@Override
	public void setRecipe(IRecipeLayout layout, IndustrialDryerRecipe recipe, IIngredients ingredients)
	{
		IGuiItemStackGroup items = layout.getItemStacks();
		// JEI puts the item 1 px inside the slot position
		items.init(0, true, WET_X - 1, WET_Y - 1);
		items.init(1, false, CAKE_X - 1, CAKE_Y - 1);
		items.set(ingredients);
		IGuiFluidStackGroup fluids = layout.getFluidStacks();
		fluids.init(0, false, OUT_X + 2, TANK_Y + 2, 16, 47, TileIndustrialDryer.CONDENSATE_CAPACITY, false, g.gauge);
		fluids.set(ingredients);
	}

	@Override
	public List<String> getTooltipStrings(int mx, int my)
	{
		String k = "gui.immersivenuclear.industrial_dryer.";
		if(mx >= CX && mx < CX + CW && my >= CY && my < CY + CH)
			return Arrays.asList(I18n.format(k + "chamber"), I18n.format(k + "temperature_info", TileIndustrialDryer.DRYING_TEMPERATURE));
		if(mx >= SCRUB_X && mx < SCRUB_X + SCRUB_W && my >= SCRUB_Y && my < SCRUB_Y + SCRUB_H)
			return Arrays.asList(I18n.format(k + "scrubber"), I18n.format(k + "scrubber_info", TileIndustrialDryer.FINES_EVERY));
		if(mx >= COND_X && mx < COND_X + COND_W && my >= COND_Y && my < COND_Y + COND_H)
			return Arrays.asList(I18n.format(k + "condenser"), I18n.format(k + "condenser_info"));
		return Collections.emptyList();
	}
}
