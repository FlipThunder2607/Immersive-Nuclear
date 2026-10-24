package immersivenuclear.client.jei;

import java.util.Collections;
import java.util.List;

import blusunrize.immersiveengineering.client.ClientUtils;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileFilterPress;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * JEI page of the Filtration Unit, a compact copy of its interface (GuiFilterPress) in the mod's finish: storage tank >
 * piston pump > pressure vessel with its dial > filter press (closed, chambers full) > solution tank, the chute into
 * the residue slot, and the filters (worn, not used up) on the left. Positions are the interface's minus (OX, OY);
 * pieces come from its own sheet (textures/gui/filter_press.png) and IE's refinery.
 */
public class FilterPressCategory implements IRecipeCategory<FilterPressRecipe>
{
	public static final String UID = ImmersiveNuclear.MODID + ".filter_press";
	static final ResourceLocation SHEET = new ResourceLocation(ImmersiveNuclear.MODID, "textures/gui/filter_press.png");
	static final int OX = 30, OY = 8, W = 187, H = 106;
	// Interface coordinates (GuiFilterPress)
	static final int ROW = 16, STORAGE_X = 32, SOLUTION_X = 195;
	static final int PUMP_X = 56, PUMP_Y = ROW + 13, WHEEL_CX = 7, WHEEL_CY = 12, WHEEL_R = 6, PUMP_WIN_X = 17, PUMP_WIN_Y = 9, PUMP_WIN_W = 11, PUMP_WIN_H = 6;
	static final int VESSEL_X = 90, VESSEL_Y = ROW + 14, GLASS_X = 10, GLASS_Y = 8, GLASS_W = 4, GLASS_H = 24, DIAL_X = 93, DIAL_Y = ROW - 5, DIAL_S = 18;
	static final int PRESS_X = 120, PRESS_Y = ROW + 6, CHAMBER_X = 3, CHAMBER_Y = 4, CHAMBER_H = 30, CYL_W = 9, HEAD_W = 3, PLATE_W = 2, PLATES = 7;
	static final int HEAD_STROKE = 7, CLOSED_GAP = 3, STRIP_X = 63, STRIP_Y = 5, STRIP_W = 4, STRIP_H = 28;
	static final int CHUTE_X = 147, CHUTE_Y = ROW + 44, RESIDUE_X = 147, RESIDUE_Y = 84, FILTER_X = 34, FILTER_Y = 84;
	static final int PIPE_DY = 25, INLET_DY = 44, INLET_X = PUMP_X + 22, TEXT_Y = 98;
	static final int COL_SLURRY = 0xff968060, COL_CAKE = 0xff6e5a3c, COL_ROD = 0xffaaadb2, COL_NEEDLE = 0xffb01e14;

	final JeiGraphite g;
	final IDrawable background, icon, pump, wheel, piston, vessel, press, dial, plate, head, chute;

	public FilterPressCategory(IGuiHelper helper)
	{
		g = new JeiGraphite(helper);
		background = helper.createBlankDrawable(W, H);
		pump = JeiGraphite.sprite(helper, SHEET, 0, 260, 30, 24);
		wheel = JeiGraphite.sprite(helper, SHEET, 32, 260, 13, 13);
		piston = JeiGraphite.sprite(helper, SHEET, 100, 260, 3, 6);
		vessel = JeiGraphite.sprite(helper, SHEET, 110, 260, 24, 40);
		press = JeiGraphite.sprite(helper, SHEET, 0, 304, 70, 38);
		dial = JeiGraphite.sprite(helper, SHEET, 0, 346, 18, 18);
		plate = JeiGraphite.sprite(helper, SHEET, 72, 304, 2, 30);
		head = JeiGraphite.sprite(helper, SHEET, 76, 304, 3, 30);
		chute = JeiGraphite.sprite(helper, SHEET, 82, 304, 16, 16);
		icon = helper.createDrawableIngredient(new ItemStack(INContent.itemFilterPress));
	}

	@Override
	public String getUid()
	{
		return UID;
	}

	@Override
	public String getTitle()
	{
		return I18n.format("tile.immersivenuclear.filter_press.name");
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
		// Pipes: the slurry comes in under the pump; the others at the tanks' middle; the dial's stem
		JeiGraphite.pipe(STORAGE_X + 20 - OX, ROW + INLET_DY - OY, INLET_X - OX, ROW + INLET_DY - OY);
		JeiGraphite.pipe(INLET_X - OX, ROW + INLET_DY - OY, INLET_X - OX, PUMP_Y + 22 - OY);
		JeiGraphite.pipe(PUMP_X + 30 - OX, ROW + PIPE_DY - OY, VESSEL_X - OX, ROW + PIPE_DY - OY);
		JeiGraphite.pipe(VESSEL_X + 24 - OX, ROW + PIPE_DY - OY, PRESS_X - OX, ROW + PIPE_DY - OY);
		JeiGraphite.pipe(PRESS_X + 70 - OX, ROW + PIPE_DY - OY, SOLUTION_X - OX, ROW + PIPE_DY - OY);
		JeiGraphite.rect(DIAL_X + 8 - OX, DIAL_Y + DIAL_S - OY, 3, VESSEL_Y - DIAL_Y - DIAL_S + 1, 0xff787a7e);
		// Pump: the slurry in front of the piston, the crank at a quarter turn
		int wx = PUMP_X + WHEEL_CX - OX, wy = PUMP_Y + WHEEL_CY - OY, px = PUMP_X + PUMP_WIN_X + 4 - OX, py = PUMP_Y + PUMP_WIN_Y - OY;
		JeiGraphite.rect(px + 3, py, PUMP_WIN_W - 7, PUMP_WIN_H, COL_SLURRY);
		// Vessel: its glass full; press: closed, the chambers full of slurry turning into cake, the strip half way
		JeiGraphite.rect(VESSEL_X + GLASS_X - OX, VESSEL_Y + GLASS_Y - OY, GLASS_W, GLASS_H, COL_SLURRY);
		int base = PRESS_X + CHAMBER_X + CYL_W - OX, top = PRESS_Y + CHAMBER_Y - OY;
		JeiGraphite.rect(PRESS_X + CHAMBER_X - OX, top, 58, CHAMBER_H, JeiGraphite.WELL);
		int headX = base + HEAD_STROKE;
		for(int k = 1; k <= PLATES; k++)
		{
			int x0 = k == 1 ? headX + HEAD_W : headX + HEAD_W + CLOSED_GAP * (k - 1) + PLATE_W * (k - 1);
			JeiGraphite.rect(x0, top + 2, CLOSED_GAP, CHAMBER_H - 4, COL_SLURRY);
			JeiGraphite.rect(x0, top + 2, CLOSED_GAP, CHAMBER_H - 4, 0x58000000 | COL_CAKE & 0xffffff);
		}
		JeiGraphite.rect(base - 3, top + CHAMBER_H / 2 - 1, headX - base + 4, 3, COL_ROD);
		JeiGraphite.rect(PRESS_X + STRIP_X - OX, PRESS_Y + STRIP_Y - OY, STRIP_W, STRIP_H, JeiGraphite.WELL);
		int sx = PRESS_X + STRIP_X - OX, sy = PRESS_Y + STRIP_Y - OY;
		ClientUtils.drawGradientRect(sx, sy + STRIP_H / 2, sx + STRIP_W, sy + STRIP_H, 0xffb51500, 0xff600b00);
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.enableBlend();
		pump.draw(mc, PUMP_X - OX, PUMP_Y - OY);
		piston.draw(mc, px, py);
		wheel.draw(mc, wx - WHEEL_R, wy - WHEEL_R);
		JeiGraphite.line(wx, wy + 4, px, py + PUMP_WIN_H / 2, COL_ROD);
		GlStateManager.color(1, 1, 1, 1);
		vessel.draw(mc, VESSEL_X - OX, VESSEL_Y - OY);
		dial.draw(mc, DIAL_X - OX, DIAL_Y - OY);
		float c = (DIAL_S - 1) / 2F;
		double a = Math.toRadians(135 + 270 * 0.9);
		JeiGraphite.line(DIAL_X - OX + c, DIAL_Y - OY + c, DIAL_X - OX + c + (float)Math.cos(a) * (c - 3), DIAL_Y - OY + c + (float)Math.sin(a) * (c - 3), COL_NEEDLE);
		GlStateManager.color(1, 1, 1, 1);
		head.draw(mc, headX, top);
		for(int k = 1; k <= PLATES; k++)
			plate.draw(mc, headX + HEAD_W + CLOSED_GAP * k + PLATE_W * (k - 1), top);
		press.draw(mc, PRESS_X - OX, PRESS_Y - OY);
		chute.draw(mc, CHUTE_X - OX, CHUTE_Y - OY);
		g.tank(mc, STORAGE_X - OX, ROW - OY, true);
		g.tank(mc, SOLUTION_X - OX, ROW - OY, false);
		g.slot(mc, FILTER_X - OX, FILTER_Y - OY, true);
		g.slot(mc, RESIDUE_X - OX, RESIDUE_Y - OY, false);
		JeiGraphite.text(mc, FilterPressRecipe.energy() + " IF   " + FilterPressRecipe.ticks() / 20 + " s", CHUTE_X + 24 - OX, TEXT_Y - 8, W - (CHUTE_X + 24 - OX));
	}

	@Override
	public void setRecipe(IRecipeLayout layout, FilterPressRecipe recipe, IIngredients ingredients)
	{
		IGuiItemStackGroup items = layout.getItemStacks();
		// JEI puts the item 1 px inside the slot position
		items.init(0, true, FILTER_X - OX - 1, FILTER_Y - OY - 1);
		items.init(1, false, RESIDUE_X - OX - 1, RESIDUE_Y - OY - 1);
		items.set(ingredients);
		items.addTooltipCallback((slot, input, stack, tooltip) -> {
			if(slot == 0)
			{
				tooltip.add("§7" + I18n.format("jei.immersivenuclear.filter_press.filters", TileFilterPress.FILTERS));
				tooltip.add("§7" + I18n.format("jei.immersivenuclear.filter_press.wear"));
			}
		});
		IGuiFluidStackGroup fluids = layout.getFluidStacks();
		int cap = TileFilterPress.TANK_CAPACITY;
		fluids.init(0, true, STORAGE_X + 2 - OX, ROW + 2 - OY, 16, 47, cap, false, g.gauge);
		fluids.init(1, false, SOLUTION_X + 2 - OX, ROW + 2 - OY, 16, 47, cap, false, g.gauge);
		fluids.set(ingredients);
	}

	@Override
	public List<String> getTooltipStrings(int mx, int my)
	{
		if(mx >= PUMP_X - OX && mx < PUMP_X + 30 - OX && my >= PUMP_Y - OY && my < PUMP_Y + 24 - OY)
			return Collections.singletonList(I18n.format("gui.immersivenuclear.filter_press.pump"));
		if(mx >= VESSEL_X - OX && mx < VESSEL_X + 24 - OX && my >= DIAL_Y - OY && my < VESSEL_Y + 40 - OY)
			return Collections.singletonList(I18n.format("gui.immersivenuclear.filter_press.pressure_tank") + ": " + TileFilterPress.BATCH + " mB");
		if(mx >= PRESS_X - OX && mx < PRESS_X + 70 - OX && my >= PRESS_Y - OY && my < PRESS_Y + 38 - OY)
			return Collections.singletonList(I18n.format("gui.immersivenuclear.filter_press.press"));
		return Collections.emptyList();
	}
}
