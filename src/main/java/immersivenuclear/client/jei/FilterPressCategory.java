package immersivenuclear.client.jei;

import java.util.Collections;
import java.util.List;

import blusunrize.immersiveengineering.client.ClientUtils;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileFilterPress;
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
import net.minecraftforge.fluids.FluidStack;

/**
 * JEI page of the Filtration Unit, the upper part of its interface (GuiFilterPress) like IE's own JEI pages: the same
 * refinery pieces at the same places. The slurry bucket column shows the filters (worn, not used up), the solution
 * bucket column the residue. Coordinates are the interface's minus (OX, OY), where the page cuts it.
 */
public class FilterPressCategory implements IRecipeCategory<FilterPressRecipe>
{
	public static final String UID = ImmersiveNuclear.MODID + ".filter_press";
	static final ResourceLocation IE = new ResourceLocation("immersiveengineering", "textures/gui/refinery.png");
	static final ResourceLocation OWN = new ResourceLocation(ImmersiveNuclear.MODID, "textures/gui/chemical_processor.png");
	static final int OX = 5, OY = 10, W = 166, H = 76;
	/** Line under the tanks for the energy and time of a batch. */
	static final int TEXT_Y = 65;
	// Interface coordinates (GuiFilterPress / ContainerFilterPress)
	static final int FILTER_X = 9, FILTER_Y = 17, RESIDUE_X = 151, RESIDUE_Y = 56;
	static final int STORAGE_X = 30, PRESSURE_X = 74, SOLUTION_X = 130, TANK_Y = 20, GAUGE_X = 94, GAUGE_Y = 18;
	static final int PUMP_ARROW_X = 51, FILTER_ARROW_X = 106, ARROW_Y = 37;

	final IDrawable background, icon, slot, tank, blue, orange, arrow, gauge;
	/** Our plain steel (145 x 65 on the sheet), cut to cover the page exactly: {main, right strip, bottom strip, corner}. */
	final IDrawable[] steel;
	final IDrawableAnimated pumping, filtering;

	public FilterPressCategory(IGuiHelper helper)
	{
		background = helper.createBlankDrawable(W, H);
		steel = new IDrawable[]{helper.createDrawable(OWN, 0, 0, 145, 65), helper.createDrawable(OWN, 0, 0, W - 145, 65),
				helper.createDrawable(OWN, 0, 0, 145, H - 65), helper.createDrawable(OWN, 0, 0, W - 145, H - 65)};
		slot = helper.createDrawable(IE, 35, 13, 20, 20);
		tank = helper.createDrawable(IE, 11, 18, 20, 51);
		blue = helper.createDrawable(IE, 42, 10, 6, 3);
		orange = helper.createDrawable(IE, 42, 49, 6, 3);
		arrow = helper.createDrawable(IE, 83, 36, 19, 13);
		// IE's tank gauge, 2 px around the 16x47 inside
		gauge = helper.createDrawable(IE, 177, 31, 20, 51, -2, 0, -2, 0);
		IDrawableStatic fill = helper.createDrawable(OWN, 0, 70, 19, 13);
		pumping = helper.createAnimatedDrawable(fill, TileFilterPress.PRESSURIZE_TICKS, IDrawableAnimated.StartDirection.LEFT, false);
		filtering = helper.createAnimatedDrawable(fill, TileFilterPress.FILTER_TICKS, IDrawableAnimated.StartDirection.LEFT, false);
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
		// Our plain steel, then the interface's pieces
		steel[0].draw(mc, 0, 0);
		steel[1].draw(mc, 145, 0);
		steel[2].draw(mc, 0, 65);
		steel[3].draw(mc, 145, 65);
		slot.draw(mc, FILTER_X - 2 - OX, FILTER_Y - 2 - OY);
		blue.draw(mc, FILTER_X + 5 - OX, FILTER_Y - 5 - OY);
		slot.draw(mc, RESIDUE_X - 2 - OX, RESIDUE_Y - 2 - OY);
		orange.draw(mc, RESIDUE_X + 5 - OX, RESIDUE_Y - 5 - OY);
		tank.draw(mc, STORAGE_X - 2 - OX, TANK_Y - 2 - OY);
		blue.draw(mc, STORAGE_X + 5 - OX, TANK_Y - 5 - OY);
		tank.draw(mc, PRESSURE_X - 2 - OX, TANK_Y - 2 - OY);
		tank.draw(mc, SOLUTION_X - 2 - OX, TANK_Y - 2 - OY);
		orange.draw(mc, SOLUTION_X + 5 - OX, TANK_Y - 5 - OY);
		arrow.draw(mc, PUMP_ARROW_X - OX, ARROW_Y - OY);
		pumping.draw(mc, PUMP_ARROW_X - OX, ARROW_Y - OY);
		arrow.draw(mc, FILTER_ARROW_X - OX, ARROW_Y - OY);
		filtering.draw(mc, FILTER_ARROW_X - OX, ARROW_Y - OY);
		// Pressure gauge, full (the batch is released at full pressure)
		ClientUtils.drawGradientRect(GAUGE_X - OX, GAUGE_Y - OY, GAUGE_X + 9 - OX, GAUGE_Y + 51 - OY, 0xff8b8b8b, 0xff8b8b8b);
		ClientUtils.drawGradientRect(GAUGE_X + 2 - OX, GAUGE_Y + 2 - OY, GAUGE_X + 7 - OX, GAUGE_Y + 49 - OY, 0xffffd640, 0xffb04000);
		String text = FilterPressRecipe.energy() + " IF   " + FilterPressRecipe.ticks() / 20 + " s";
		mc.fontRenderer.drawString(text, (W - mc.fontRenderer.getStringWidth(text)) / 2, TEXT_Y, 0x3a3a3a);
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
		int cap = TileFilterPress.BATCH;
		fluids.init(0, true, STORAGE_X - OX, TANK_Y - OY, 16, 47, cap, false, gauge);
		fluids.init(1, false, SOLUTION_X - OX, TANK_Y - OY, 16, 47, cap, false, gauge);
		// Only to show the step: the batch in the pressure tank (not an ingredient)
		fluids.init(2, true, PRESSURE_X - OX, TANK_Y - OY, 16, 47, cap, false, gauge);
		fluids.set(ingredients);
		fluids.set(2, new FluidStack(INContent.fluidSlurry, TileFilterPress.BATCH));
	}

	@Override
	public List<String> getTooltipStrings(int mx, int my)
	{
		if(mx >= GAUGE_X - OX && mx < GAUGE_X + 9 - OX && my >= GAUGE_Y - OY && my < GAUGE_Y + 51 - OY)
			return Collections.singletonList(I18n.format("gui.immersivenuclear.filter_press.pressure", 100));
		if(mx >= PUMP_ARROW_X - OX && mx < PUMP_ARROW_X + 19 - OX && my >= ARROW_Y - OY && my < ARROW_Y + 13 - OY)
			return Collections.singletonList(I18n.format("gui.immersivenuclear.filter_press.pump"));
		return Collections.emptyList();
	}
}
