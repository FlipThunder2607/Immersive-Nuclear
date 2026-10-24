package immersivenuclear.client.jei;

import java.util.Collections;
import java.util.List;

import blusunrize.immersiveengineering.client.ClientUtils;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileChemicalSeparator;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * JEI page of the Chemical Separation Unit, a compact copy of its interface (GuiChemicalSeparator) in the same finish:
 * the two stages as two rows (tank in > mixer > settler > tank out), the solvent make-up in the reservoir below, the
 * mixers' arrows filling over a batch. Pieces come from the interface's own sheet and IE's refinery.
 */
public class ChemicalSeparatorCategory implements IRecipeCategory<ChemicalSeparatorRecipe>
{
	public static final String UID = ImmersiveNuclear.MODID + ".chemical_separator";
	static final ResourceLocation IE = new ResourceLocation("immersiveengineering", "textures/gui/refinery.png");
	static final ResourceLocation SHEET = new ResourceLocation(ImmersiveNuclear.MODID, "textures/gui/chemical_separator.png");
	static final ResourceLocation GRAPHITE = new ResourceLocation(ImmersiveNuclear.MODID, "textures/gui/graphite.png");
	static final int W = 166, H = 130;
	static final int ROW_A = 2, ROW_B = 58, IN_X = 2, OUT_X = 144;
	static final int MIXER_X = 26, MIXER_DY = 12, FAN_D = 3, ARROW_DY = 40;
	static final int SETTLER_X = 60, SETTLER_DY = 6, WIN_X = 4, WIN_Y = 4, WIN_W = 52, WIN_H = 30, PIPE_DY = 25;
	static final int RES_X = 2, RES_Y = 113, RES_WIN_X = 3, RES_WIN_Y = 3, RES_WIN_W = 42, RES_WIN_H = 8;
	static final int TEXT_X = 56, TEXT_Y = 116;
	/** Colours of the settled layers in the windows (solvent on top, water below). */
	static final int AMBER = 0xffffb04a, EFFLUENT = 0xff788c5a, CLEAN = 0xffd2d2be, PURIFIED = 0xffe6d25a, PIPE = 0xff4e5054, WELL = 0xff1c1c1e;

	final IDrawable background, icon, tank, gauge, blue, orange, mixer, fan, settler, reservoir;
	final IDrawableStatic plate;
	final IDrawableAnimated mixingA, mixingB;

	public ChemicalSeparatorCategory(IGuiHelper helper)
	{
		background = helper.createBlankDrawable(W, H);
		plate = helper.createDrawable(GRAPHITE, 0, 0, 128, 128);
		tank = helper.createDrawable(IE, 11, 18, 20, 51);
		// IE's tank gauge, 2 px around the 16x47 inside
		gauge = helper.createDrawable(IE, 177, 31, 20, 51, -2, 0, -2, 0);
		blue = helper.createDrawable(IE, 42, 10, 6, 3);
		orange = helper.createDrawable(IE, 42, 49, 6, 3);
		mixer = helper.drawableBuilder(SHEET, 0, 260, 26, 26).setTextureSize(512, 512).build();
		fan = helper.drawableBuilder(SHEET, 28, 260, 20, 20).setTextureSize(512, 512).build();
		settler = helper.drawableBuilder(SHEET, 0, 304, 80, 38).setTextureSize(512, 512).build();
		reservoir = helper.drawableBuilder(SHEET, 0, 346, 48, 14).setTextureSize(512, 512).build();
		// The mixer's progress: the arrow fill of the interface, under each mixer
		mixingA = helper.drawableBuilder(SHEET, 120, 260, 19, 13).setTextureSize(512, 512)
				.buildAnimated(TileChemicalSeparator.A_AQUEOUS / TileChemicalSeparator.A_AQUEOUS_RATE, IDrawableAnimated.StartDirection.LEFT, false);
		mixingB = helper.drawableBuilder(SHEET, 120, 260, 19, 13).setTextureSize(512, 512)
				.buildAnimated(TileChemicalSeparator.B_SOLVENT / TileChemicalSeparator.B_SOLVENT_RATE, IDrawableAnimated.StartDirection.LEFT, false);
		icon = helper.createDrawableIngredient(new ItemStack(INContent.itemChemicalSeparator));
	}

	@Override
	public String getUid()
	{
		return UID;
	}

	@Override
	public String getTitle()
	{
		return I18n.format("tile.immersivenuclear.chemical_separator.name");
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

	static void rect(int x, int y, int w, int h, int argb)
	{
		ClientUtils.drawGradientRect(x, y, x + w, y + h, argb, argb);
	}

	@Override
	public void drawExtras(Minecraft mc)
	{
		// Graphite plate
		for(int y = 0; y < H; y += 128)
			for(int x = 0; x < W; x += 128)
				plate.draw(mc, x, y, 0, Math.max(0, y + 128 - H), 0, Math.max(0, x + 128 - W));
		for(int k = 0; k < 2; k++)
		{
			int row = k == 0 ? ROW_A : ROW_B;
			// Pipes between the parts, at the tanks' middle
			rect(IN_X + 20, row + PIPE_DY - 1, OUT_X - IN_X - 20, 3, PIPE);
			// Mixer: the dark well behind the fan, the lid, the fan; its progress arrow under it
			rect(MIXER_X + FAN_D, row + MIXER_DY + FAN_D, 20, 20, WELL);
			GlStateManager.color(1, 1, 1, 1);
			mixer.draw(mc, MIXER_X, row + MIXER_DY);
			fan.draw(mc, MIXER_X + FAN_D, row + MIXER_DY + FAN_D);
			(k == 0 ? mixingA : mixingB).draw(mc, MIXER_X + 4, row + ARROW_DY);
			// Settler, settled: solvent on top, water below
			int wx = SETTLER_X + WIN_X, wy = row + SETTLER_DY + WIN_Y;
			int top = k == 0 ? TileChemicalSeparator.A_SOLVENT : TileChemicalSeparator.B_SOLVENT - TileChemicalSeparator.SOLVENT_LOSS;
			int bottom = k == 0 ? TileChemicalSeparator.A_AQUEOUS : TileChemicalSeparator.B_AQUEOUS;
			int full = Math.round(WIN_H * (top + bottom) / (float)TileChemicalSeparator.SETTLER_CAPACITY);
			int bh = Math.round(full * bottom / (float)(top + bottom));
			rect(wx, wy, WIN_W, WIN_H, WELL);
			rect(wx, wy + WIN_H - bh, WIN_W, bh, k == 0 ? EFFLUENT : PURIFIED);
			rect(wx, wy + WIN_H - full, WIN_W, full - bh, k == 0 ? AMBER : CLEAN);
			GlStateManager.color(1, 1, 1, 1);
			settler.draw(mc, SETTLER_X, row + SETTLER_DY);
			// Tanks, IE's, with their port markers
			tank.draw(mc, IN_X, row);
			blue.draw(mc, IN_X + 7, row - 1);
			tank.draw(mc, OUT_X, row);
			orange.draw(mc, OUT_X + 7, row - 1);
		}
		// The solvent reservoir; JEI draws the make-up in its window
		rect(RES_X + RES_WIN_X, RES_Y + RES_WIN_Y, RES_WIN_W, RES_WIN_H, WELL);
		GlStateManager.color(1, 1, 1, 1);
		reservoir.draw(mc, RES_X, RES_Y);
		String text = ChemicalSeparatorRecipe.energy() + " IF   " + ChemicalSeparatorRecipe.ticks() / 20 + " s";
		mc.fontRenderer.drawString(text, TEXT_X + (W - TEXT_X - mc.fontRenderer.getStringWidth(text)) / 2, TEXT_Y, 0xd0d0d0);
	}

	@Override
	public void setRecipe(IRecipeLayout layout, ChemicalSeparatorRecipe recipe, IIngredients ingredients)
	{
		IGuiFluidStackGroup fluids = layout.getFluidStacks();
		int cap = TileChemicalSeparator.TANK_CAPACITY;
		fluids.init(0, true, IN_X + 2, ROW_A + 2, 16, 47, cap, false, gauge);
		fluids.init(1, true, IN_X + 2, ROW_B + 2, 16, 47, cap, false, gauge);
		fluids.init(2, true, RES_X + RES_WIN_X, RES_Y + RES_WIN_Y, RES_WIN_W, RES_WIN_H, TileChemicalSeparator.SOLVENT_LOSS, false, null);
		fluids.init(3, false, OUT_X + 2, ROW_A + 2, 16, 47, cap, false, gauge);
		fluids.init(4, false, OUT_X + 2, ROW_B + 2, 16, 47, cap, false, gauge);
		fluids.set(ingredients);
		fluids.addTooltipCallback((slot, input, stack, tooltip) -> {
			if(slot == 2)
				tooltip.add("§7" + I18n.format("jei.immersivenuclear.chemical_separator.makeup"));
		});
	}

	@Override
	public List<String> getTooltipStrings(int mx, int my)
	{
		for(int k = 0; k < 2; k++)
		{
			int row = k == 0 ? ROW_A : ROW_B;
			if(mx >= SETTLER_X && mx < SETTLER_X + 80 && my >= row + SETTLER_DY && my < row + SETTLER_DY + 38)
				return Collections.singletonList(I18n.format("jei.immersivenuclear.chemical_separator.settling", TileChemicalSeparator.SETTLE_TICKS / 20));
		}
		return Collections.emptyList();
	}
}
