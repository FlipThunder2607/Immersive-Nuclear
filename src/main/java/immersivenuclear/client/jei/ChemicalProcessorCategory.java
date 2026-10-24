package immersivenuclear.client.jei;

import java.util.Collections;
import java.util.List;

import blusunrize.immersiveengineering.client.ClientUtils;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileChemicalProcessor;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * JEI page of the Chemical Processing Unit, the upper part of its interface (GuiChemicalProcessor) like IE's own
 * JEI pages: the same refinery pieces at the same places, minus the bucket slots and the redstone/status panel.
 * Coordinates are the interface's minus (6, 10), where the page cuts it.
 */
public class ChemicalProcessorCategory implements IRecipeCategory<ChemicalProcessorRecipe>
{
	public static final String UID = ImmersiveNuclear.MODID + ".chemical_processor";
	static final ResourceLocation IE = new ResourceLocation("immersiveengineering", "textures/gui/refinery.png");
	static final ResourceLocation OWN = new ResourceLocation(ImmersiveNuclear.MODID, "textures/gui/chemical_processor.png");
	static final int OX = 6, OY = 10;

	final IDrawable background, icon, panel, slot, tank, blue, orange, arrow, gauge;
	final IDrawableAnimated progress;

	public ChemicalProcessorCategory(IGuiHelper helper)
	{
		background = helper.createDrawable(IE, OX, OY, 164, 62);
		panel = helper.createDrawable(OWN, 0, OY - 8, 145, 62);
		slot = helper.createDrawable(IE, 35, 13, 20, 20);
		tank = helper.createDrawable(IE, 11, 18, 20, 51);
		blue = helper.createDrawable(IE, 42, 10, 6, 3);
		orange = helper.createDrawable(IE, 42, 49, 6, 3);
		arrow = helper.createDrawable(IE, 83, 36, 19, 13);
		// IE's tank gauge, 2 px around the 16x47 inside
		gauge = helper.createDrawable(IE, 177, 31, 20, 51, -2, 0, -2, 0);
		IDrawableStatic fill = helper.createDrawable(OWN, 0, 70, 19, 13);
		progress = helper.createAnimatedDrawable(fill, TileChemicalProcessor.BATCH_TICKS, IDrawableAnimated.StartDirection.LEFT, false);
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

	@Override
	public void drawExtras(Minecraft mc)
	{
		// Our plain steel over the refinery's layout, then the interface's pieces (interface coordinates - (6, 10))
		panel.draw(mc, 9 - OX, 0);
		slot.draw(mc, 56 - OX, 13 - OY);
		blue.draw(mc, 63 - OX, 10 - OY);
		slot.draw(mc, 56 - OX, 52 - OY);
		blue.draw(mc, 63 - OX, 49 - OY);
		tank.draw(mc, 32 - OX, 18 - OY);
		tank.draw(mc, 80 - OX, 18 - OY);
		orange.draw(mc, 87 - OX, 15 - OY);
		arrow.draw(mc, 56 - OX, 37 - OY);
		progress.draw(mc, 56 - OX, 37 - OY);
		// Energy bar full (the cycle's whole need), as IE's own red
		ClientUtils.drawGradientRect(157 - OX, 21 - OY, 164 - OX, 67 - OY, 0xffb51500, 0xff600b00);
		int pumping = TileChemicalProcessor.BATCH_TICKS - TileChemicalProcessor.REACT_TICKS;
		int energy = TileChemicalProcessor.REACT_ENERGY * TileChemicalProcessor.REACT_TICKS + TileChemicalProcessor.PUMP_ENERGY * pumping;
		mc.fontRenderer.drawString(energy + " IF", 102 - OX, 30 - OY, 0x3a3a3a);
		mc.fontRenderer.drawString(TileChemicalProcessor.BATCH_TICKS / 20 + " s", 102 - OX, 42 - OY, 0x3a3a3a);
	}

	@Override
	public void setRecipe(IRecipeLayout layout, ChemicalProcessorRecipe recipe, IIngredients ingredients)
	{
		IGuiItemStackGroup items = layout.getItemStacks();
		// JEI puts the item 1 px inside the slot position
		items.init(0, true, 58 - OX - 1, 15 - OY - 1);
		items.init(1, true, 58 - OX - 1, 54 - OY - 1);
		items.set(ingredients);
		IGuiFluidStackGroup fluids = layout.getFluidStacks();
		int cap = TileChemicalProcessor.BATCH_WATER;
		fluids.init(0, true, 34 - OX, 20 - OY, 16, 47, cap, false, gauge);
		fluids.init(1, false, 82 - OX, 20 - OY, 16, 47, cap, false, gauge);
		fluids.set(ingredients);
	}

	@Override
	public List<String> getTooltipStrings(int mx, int my)
	{
		if(mx >= 157 - OX && mx < 164 - OX && my >= 21 - OY && my < 67 - OY)
			return Collections.singletonList(TileChemicalProcessor.REACT_ENERGY + " IF/t");
		return Collections.emptyList();
	}
}
