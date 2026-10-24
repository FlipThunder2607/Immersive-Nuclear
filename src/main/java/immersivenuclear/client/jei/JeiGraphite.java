package immersivenuclear.client.jei;

import blusunrize.immersiveengineering.client.ClientUtils;
import immersivenuclear.ImmersiveNuclear;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableStatic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/**
 * What the JEI pages share with the interfaces: the graphite plate, IE's pieces, pipes, wells and plain lines, so each
 * page is a compact copy of its machine's interface in the mod's finish.
 */
final class JeiGraphite
{
	static final ResourceLocation IE = new ResourceLocation("immersiveengineering", "textures/gui/refinery.png");
	static final ResourceLocation GRAPHITE = new ResourceLocation(ImmersiveNuclear.MODID, "textures/gui/graphite.png");
	static final int WELL = 0xff1c1c1e, PIPE_EDGE = 0xff1e1f21, PIPE_LIGHT = 0xff606266, PIPE = 0xff4e5054, TEXT = 0xd0d0d0;

	final IDrawableStatic plate;
	final IDrawable slot, tank, gauge, blue, orange;

	JeiGraphite(IGuiHelper helper)
	{
		plate = helper.createDrawable(GRAPHITE, 0, 0, 128, 128);
		slot = helper.createDrawable(IE, 35, 13, 20, 20);
		tank = helper.createDrawable(IE, 11, 18, 20, 51);
		// IE's tank gauge, 2 px around the 16x47 inside
		gauge = helper.createDrawable(IE, 177, 31, 20, 51, -2, 0, -2, 0);
		blue = helper.createDrawable(IE, 42, 10, 6, 3);
		orange = helper.createDrawable(IE, 42, 49, 6, 3);
	}

	/** A sprite of one of our 512 x 512 interface sheets. */
	static IDrawableStatic sprite(IGuiHelper helper, ResourceLocation sheet, int u, int v, int w, int h)
	{
		return helper.drawableBuilder(sheet, u, v, w, h).setTextureSize(512, 512).build();
	}

	/** The graphite plate over the whole page. */
	void plate(Minecraft mc, int w, int h)
	{
		GlStateManager.color(1, 1, 1, 1);
		for(int y = 0; y < h; y += 128)
			for(int x = 0; x < w; x += 128)
				plate.draw(mc, x, y, 0, Math.max(0, y + 128 - h), 0, Math.max(0, x + 128 - w));
	}

	/** An IE tank frame with its port marker (input blue, output orange); JEI draws the fluid and the gauge. */
	void tank(Minecraft mc, int x, int y, boolean input)
	{
		GlStateManager.color(1, 1, 1, 1);
		tank.draw(mc, x, y);
		(input ? blue : orange).draw(mc, x + 7, y - 1);
	}

	/** An IE slot frame around the 16 x 16 item at x, y, with a marker when given. */
	void slot(Minecraft mc, int x, int y, Boolean input)
	{
		GlStateManager.color(1, 1, 1, 1);
		slot.draw(mc, x - 2, y - 2);
		if(input != null)
			(input ? blue : orange).draw(mc, x + 5, y - 5);
	}

	static void rect(int x, int y, int w, int h, int argb)
	{
		if(w > 0 && h > 0)
			ClientUtils.drawGradientRect(x, y, x + w, y + h, argb, argb);
	}

	/** A pipe like the interfaces' (dark walls, a lighter body), horizontal or vertical, centred on its line. */
	static void pipe(int x0, int y0, int x1, int y1)
	{
		if(y0 == y1)
		{
			int x = Math.min(x0, x1), w = Math.abs(x1 - x0) + 1;
			rect(x, y0 - 2, w, 5, PIPE_EDGE);
			rect(x, y0 - 1, w, 1, PIPE_LIGHT);
			rect(x, y0, w, 2, PIPE);
		}
		else
		{
			int y = Math.min(y0, y1), h = Math.abs(y1 - y0) + 1;
			rect(x0 - 2, y, 5, h, PIPE_EDGE);
			rect(x0 - 1, y, 1, h, PIPE_LIGHT);
			rect(x0, y, 2, h, PIPE);
		}
	}

	/** A one pixel line, point by point (a dial's needle, a connecting rod). */
	static void line(float x0, float y0, float x1, float y1, int argb)
	{
		int n = (int)Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)));
		for(int k = 0; k <= n; k++)
		{
			float t = n == 0 ? 0 : k / (float)n;
			rect(Math.round(x0 + (x1 - x0) * t), Math.round(y0 + (y1 - y0) * t), 1, 1, argb);
		}
	}

	static void text(Minecraft mc, String text, int x, int y, int w)
	{
		mc.fontRenderer.drawString(text, x + (w - mc.fontRenderer.getStringWidth(text)) / 2, y, TEXT);
	}
}
