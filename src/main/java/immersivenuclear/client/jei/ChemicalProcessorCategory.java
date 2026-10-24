package immersivenuclear.client.jei;

import java.util.Collections;
import java.util.List;

import blusunrize.immersiveengineering.client.ClientUtils;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileChemicalProcessor;
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
 * JEI page of the Chemical Processing Unit, a compact copy of its interface (GuiChemicalProcessor) in the mod's finish:
 * water tank > reactor (hopper with the batch's solids on top, agitator, the batch as slurry) > slurry tank. Pieces
 * come from the interface's own sheet (textures/gui/reactor.png) and IE's refinery.
 */
public class ChemicalProcessorCategory implements IRecipeCategory<ChemicalProcessorRecipe>
{
	public static final String UID = ImmersiveNuclear.MODID + ".chemical_processor";
	static final ResourceLocation SHEET = new ResourceLocation(ImmersiveNuclear.MODID, "textures/gui/reactor.png");
	static final int W = 176, H = 118;
	// The reactor on the page and, relative to it, as in the interface
	static final int RX = 24, RY = 32;
	static final int WIN_X = 4, WIN_Y = 8, WIN_W = 104, WIN_H = 56, STRIP_X = 113, STRIP_Y = 8, STRIP_W = 5, STRIP_H = 56;
	static final int MOTOR_X = 48, MOTOR_DY = -12, SHAFT_X = 59, HOPPER_X = 6, HOPPER_DY = -12, PIPE_DY = 35;
	static final int[][] SOLIDS = {{8, -30}, {28, -30}};
	static final int IN_X = 2, OUT_X = 154, TANK_Y = RY + PIPE_DY - 25, TEXT_Y = 106;
	static final int COL_SLURRY = 0xff968060, COL_SHAFT = 0xff96999e;

	final JeiGraphite g;
	final IDrawable background, icon, reactor, motor, impeller, hopper;

	public ChemicalProcessorCategory(IGuiHelper helper)
	{
		g = new JeiGraphite(helper);
		background = helper.createBlankDrawable(W, H);
		reactor = JeiGraphite.sprite(helper, SHEET, 0, 260, 128, 70);
		motor = JeiGraphite.sprite(helper, SHEET, 130, 260, 24, 18);
		impeller = JeiGraphite.sprite(helper, SHEET, 160, 260, 40, 10);
		hopper = JeiGraphite.sprite(helper, SHEET, 0, 340, 40, 12);
		icon = helper.createDrawableIngredient(new ItemStack(INContent.itemChemicalProcessor));
	}

	@Override
	public String getUid()
	{
		return UID;
	}

	@Override
	public String getTitle()
	{
		return I18n.format("tile.immersivenuclear.chemical_processor.name");
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

	static int energy()
	{
		int pumping = TileChemicalProcessor.BATCH_TICKS - TileChemicalProcessor.REACT_TICKS;
		return TileChemicalProcessor.REACT_ENERGY * TileChemicalProcessor.REACT_TICKS + TileChemicalProcessor.PUMP_ENERGY * pumping;
	}

	@Override
	public void drawExtras(Minecraft mc)
	{
		g.plate(mc, W, H);
		JeiGraphite.pipe(IN_X + 20, RY + PIPE_DY, RX - 1, RY + PIPE_DY);
		JeiGraphite.pipe(RX + 128, RY + PIPE_DY, OUT_X - 1, RY + PIPE_DY);
		// The batch, reacted, with the agitator in it
		int wx = RX + WIN_X, wy = RY + WIN_Y, lh = Math.round(WIN_H * 0.8F);
		JeiGraphite.rect(wx, wy, WIN_W, WIN_H, JeiGraphite.WELL);
		JeiGraphite.rect(wx, wy + WIN_H - lh, WIN_W, lh, COL_SLURRY);
		JeiGraphite.rect(wx, wy + WIN_H - lh, WIN_W, 1, 0x46ffffff);
		JeiGraphite.rect(RX + SHAFT_X, RY + 2, 2, WIN_Y + WIN_H - 10, COL_SHAFT);
		JeiGraphite.rect(RX + STRIP_X, RY + STRIP_Y, STRIP_W, STRIP_H, JeiGraphite.WELL);
		ClientUtils.drawGradientRect(RX + STRIP_X, RY + STRIP_Y, RX + STRIP_X + STRIP_W, RY + STRIP_Y + STRIP_H, 0xffb51500, 0xff600b00);
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.enableBlend();
		impeller.draw(mc, RX + SHAFT_X + 1 - 20, wy + WIN_H - 16);
		reactor.draw(mc, RX, RY);
		motor.draw(mc, RX + MOTOR_X, RY + MOTOR_DY);
		hopper.draw(mc, RX + HOPPER_X, RY + HOPPER_DY);
		for(int[] s : SOLIDS)
			g.slot(mc, RX + s[0], RY + s[1], null);
		g.tank(mc, IN_X, TANK_Y, true);
		g.tank(mc, OUT_X, TANK_Y, false);
		JeiGraphite.text(mc, energy() + " IF   " + TileChemicalProcessor.BATCH_TICKS / 20 + " s", 0, TEXT_Y, W);
	}

	@Override
	public void setRecipe(IRecipeLayout layout, ChemicalProcessorRecipe recipe, IIngredients ingredients)
	{
		IGuiItemStackGroup items = layout.getItemStacks();
		// JEI puts the item 1 px inside the slot position
		for(int i = 0; i < SOLIDS.length; i++)
			items.init(i, true, RX + SOLIDS[i][0] - 1, RY + SOLIDS[i][1] - 1);
		items.set(ingredients);
		IGuiFluidStackGroup fluids = layout.getFluidStacks();
		int cap = TileChemicalProcessor.TANK_CAPACITY;
		fluids.init(0, true, IN_X + 2, TANK_Y + 2, 16, 47, cap, false, g.gauge);
		fluids.init(1, false, OUT_X + 2, TANK_Y + 2, 16, 47, cap, false, g.gauge);
		fluids.set(ingredients);
	}

	@Override
	public List<String> getTooltipStrings(int mx, int my)
	{
		if(mx >= RX && mx < RX + 128 && my >= RY && my < RY + 70)
			return Collections.singletonList(I18n.format("gui.immersivenuclear.chemical_processor.reactor"));
		return Collections.emptyList();
	}
}
