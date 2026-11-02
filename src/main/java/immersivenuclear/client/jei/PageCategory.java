package immersivenuclear.client.jei;

import java.util.Collections;
import java.util.List;

import immersivenuclear.ImmersiveNuclear;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * A JEI page in the interface standard (01/11): a simplified copy of the machine's interface, made by
 * ferramentas/gui_jei.py (textures/gui/jei/&lt;machine&gt;.png): the machine cropped from its game sheet with its moving parts
 * drawn still, side panels with only the recipe's tanks, IE's slot and tank frames baked in. JEI's window is 198 px
 * wide, so a page is at most 186. JEI draws the items, the fluids and the tank gauges over it; the line under it holds
 * the energy and time.
 */
abstract class PageCategory<T extends IRecipeWrapper> implements IRecipeCategory<T>
{
	/** The text line's y on every page. */
	static final int TEXT_Y = 146;

	final JeiGraphite g;
	final IDrawable background, icon;
	final IDrawableStatic page;
	final int w, h;
	final String uid, title;

	PageCategory(IGuiHelper helper, String machine, int w, int h, Item item)
	{
		this.w = w;
		this.h = h;
		g = new JeiGraphite(helper);
		background = helper.createBlankDrawable(w, h);
		page = helper.drawableBuilder(new ResourceLocation(ImmersiveNuclear.MODID, "textures/gui/jei/" + machine + ".png"), 0, 0, w, h).setTextureSize(w, h).build();
		icon = helper.createDrawableIngredient(new ItemStack(item));
		uid = ImmersiveNuclear.MODID + "." + machine;
		title = "tile.immersivenuclear." + machine + ".name";
	}

	@Override
	public String getUid()
	{
		return uid;
	}

	@Override
	public String getTitle()
	{
		return I18n.format(title);
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

	/** The energy and time line, or null when the recipe draws its own. */
	String text()
	{
		return null;
	}

	@Override
	public void drawExtras(Minecraft mc)
	{
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.enableBlend();
		page.draw(mc, 0, 0);
		String t = text();
		if(t != null)
			JeiGraphite.text(mc, t, 0, TEXT_Y, w);
	}

	/** A tank on the page (x, y the fluid's top left, inside IE's frame), with IE's gauge over it. */
	void tank(IGuiFluidStackGroup fluids, int index, boolean input, int x, int y, int capacity)
	{
		fluids.init(index, input, x, y, 16, 47, capacity, false, g.gauge);
	}

	@Override
	public List<String> getTooltipStrings(int mx, int my)
	{
		return Collections.emptyList();
	}

	static boolean in(int mx, int my, int x, int y, int w, int h)
	{
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}
}
