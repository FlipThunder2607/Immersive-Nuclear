package immersivenuclear.client;

import blusunrize.immersiveengineering.client.ClientUtils;
import immersivenuclear.ImmersiveNuclear;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

/**
 * The mod's GUI finish for machines 1 and 2: graphite brushed steel, finished the way IE finishes its GUIs (a row of
 * small rivets along the edge with a groove inside it, wells with a flat light frame and a dark inside), from
 * textures/gui/graphite.png (made by ferramentas/gui_grafite.py). IE's own slots, tanks and markers are drawn over it
 * a little darker ({@link #ieTint}). The Chemical Separation Unit has its own sheet in the same finish.
 */
public final class GuiGraphite
{
	public static final String SHEET = ImmersiveNuclear.MODID + ":textures/gui/graphite.png";
	/** IE's pieces are drawn this much darker, to sit on the graphite. */
	public static final float IE_TINT = 0.8F;
	public static final int ARROW_W = 19, ARROW_H = 13;
	/** Tooltips wrap their lines at this width, so they stay on the screen beside a wide interface. */
	public static final int TOOLTIP_WIDTH = 200;

	private GuiGraphite()
	{
	}

	static void blit(int x, int y, int u, int v, int w, int h)
	{
		Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, w, h, 256, 256);
	}

	/** A riveted graphite plate at screen x, y of w x h (binds the sheet). */
	public static void panel(int x, int y, int w, int h)
	{
		GlStateManager.color(1, 1, 1, 1);
		ClientUtils.bindTexture(SHEET);
		for(int j = 6; j < h - 6; j += 128)
			for(int i = 6; i < w - 6; i += 128)
				blit(x + i, y + j, 0, 0, Math.min(128, w - 6 - i), Math.min(128, h - 6 - j));
		for(int i = 6; i < w - 6; i += 8)
		{
			int n = Math.min(8, w - 6 - i);
			blit(x + i, y, 128, 0, n, 6);
			blit(x + i, y + h - 6, 128, 8, n, 6);
		}
		for(int j = 6; j < h - 6; j += 8)
		{
			int n = Math.min(8, h - 6 - j);
			blit(x, y + j, 136, 0, 6, n);
			blit(x + w - 6, y + j, 142, 0, 6, n);
		}
		blit(x, y, 148, 0, 6, 6);
		blit(x + w - 6, y, 154, 0, 6, 6);
		blit(x, y + h - 6, 148, 6, 6, 6);
		blit(x + w - 6, y + h - 6, 154, 6, 6, 6);
	}

	/** An IE well (2 px light frame, dark inside) of any size, 9-sliced; the sheet must be bound. */
	public static void well(int x, int y, int w, int h)
	{
		blit(x, y, 160, 0, 2, 2);
		blit(x + w - 2, y, 182, 0, 2, 2);
		blit(x, y + h - 2, 160, 22, 2, 2);
		blit(x + w - 2, y + h - 2, 182, 22, 2, 2);
		for(int i = 2; i < w - 2; i += 20)
		{
			int n = Math.min(20, w - 2 - i);
			blit(x + i, y, 162, 0, n, 2);
			blit(x + i, y + h - 2, 162, 22, n, 2);
		}
		for(int j = 2; j < h - 2; j += 20)
		{
			int n = Math.min(20, h - 2 - j);
			blit(x, y + j, 160, 2, 2, n);
			blit(x + w - 2, y + j, 182, 2, 2, n);
		}
		for(int j = 2; j < h - 2; j += 20)
			for(int i = 2; i < w - 2; i += 20)
				blit(x + i, y + j, 162, 2, Math.min(20, w - 2 - i), Math.min(20, h - 2 - j));
	}

	/** IE's process arrow in the graphite finish, filled from the left up to {@code done} px; the sheet must be bound. */
	public static void arrow(int x, int y, int done)
	{
		blit(x, y, 184, 0, ARROW_W, ARROW_H);
		if(done > 0)
			blit(x, y, 184, 13, Math.min(ARROW_W, done), ARROW_H);
	}

	/** Start drawing IE's pieces a little darker; {@link #endTint} goes back to normal. */
	public static void ieTint()
	{
		GlStateManager.color(IE_TINT, IE_TINT, IE_TINT, 1);
	}

	public static void endTint()
	{
		GlStateManager.color(1, 1, 1, 1);
	}
}
