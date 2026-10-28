package immersivenuclear.client;

import java.util.ArrayList;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.GuiIEContainerBase;
import immersivenuclear.common.block.TileConcentrateDrum;
import immersivenuclear.common.gui.ContainerConcentrateDrum;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.config.GuiUtils;

/**
 * The Concentrate Drum's interface, in the mod's graphite finish: its one slot in IE's slot frame, and beside it the drum
 * seen from the side, with its hoops, filling with yellowcake up to a stack.
 */
public class GuiConcentrateDrum extends GuiIEContainerBase
{
	static final String IE = "immersiveengineering:textures/gui/refinery.png";
	static final int[] IE_SLOT = {35, 13, 20, 20};
	/** The drum's side view: its well, and the hoops across it. */
	static final int DRUM_X = 92, DRUM_Y = 12, DRUM_W = 22, DRUM_H = 60;
	static final int COL_STEEL = 0xff5c5f64, COL_HOOP = 0xff96999e, COL_YELLOWCAKE = 0xfffcf678, COL_YELLOWCAKE_DARK = 0xffb8a830;

	final ContainerConcentrateDrum container;

	public GuiConcentrateDrum(ContainerConcentrateDrum container)
	{
		super(container);
		this.container = container;
		xSize = ContainerConcentrateDrum.WIDTH;
		ySize = ContainerConcentrateDrum.HEIGHT;
	}

	void rect(int x, int y, int w, int h, int argb)
	{
		if(w > 0 && h > 0)
			drawRect(guiLeft + x, guiTop + y, guiLeft + x + w, guiTop + y + h, argb);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mx, int my)
	{
		GuiGraphite.panel(guiLeft, guiTop, xSize, ySize);
		GuiGraphite.well(guiLeft + DRUM_X, guiTop + DRUM_Y, DRUM_W, DRUM_H);
		// The drum's inside, the yellowcake up to its level, the two hoops over it
		int ix = DRUM_X + 2, iy = DRUM_Y + 2, iw = DRUM_W - 4, ih = DRUM_H - 4;
		rect(ix, iy, iw, ih, COL_STEEL);
		int level = Math.round(ih * container.count() / (float)TileConcentrateDrum.CAPACITY);
		rect(ix, iy + ih - level, iw, level, COL_YELLOWCAKE);
		rect(ix, iy + ih - level, iw, Math.min(1, level), COL_YELLOWCAKE_DARK);
		for(int hoop : new int[]{ih / 3, ih * 2 / 3})
			rect(ix - 1, iy + hoop, iw + 2, 2, COL_HOOP);

		// IE's wooden inventory panel and slot frame, a little darker
		GlStateManager.color(1, 1, 1, 1);
		ClientUtils.bindTexture(IE);
		drawTexturedModalRect(guiLeft + ContainerConcentrateDrum.INV_X - 8, guiTop + ContainerConcentrateDrum.INV_Y - 8, 0, 77, 176, 89);
		GuiGraphite.ieTint();
		drawTexturedModalRect(guiLeft + ContainerConcentrateDrum.SLOT_X - 2, guiTop + ContainerConcentrateDrum.SLOT_Y - 2, IE_SLOT[0], IE_SLOT[1], IE_SLOT[2], IE_SLOT[3]);
		GuiGraphite.endTint();
	}

	@Override
	public void drawScreen(int mx, int my, float partialTicks)
	{
		super.drawScreen(mx, my, partialTicks);
		ArrayList<String> tip = new ArrayList<>();
		if(mx >= guiLeft + DRUM_X && mx < guiLeft + DRUM_X + DRUM_W && my >= guiTop + DRUM_Y && my < guiTop + DRUM_Y + DRUM_H)
		{
			tip.add(I18n.format("tile.immersivenuclear.concentrate_drum.name"));
			tip.add("§7" + I18n.format("desc.immersivenuclear.concentrate_drum.contents", container.count(), TileConcentrateDrum.CAPACITY));
		}
		boolean empty = container.getSlot(0).getStack().isEmpty();
		if(empty && mx >= guiLeft + ContainerConcentrateDrum.SLOT_X && mx < guiLeft + ContainerConcentrateDrum.SLOT_X + 16
				&& my >= guiTop + ContainerConcentrateDrum.SLOT_Y && my < guiTop + ContainerConcentrateDrum.SLOT_Y + 16)
			tip.add(I18n.format("gui.immersivenuclear.concentrate_drum.slot"));
		if(!tip.isEmpty())
		{
			GuiUtils.drawHoveringText(tip, mx, my, width, height, GuiGraphite.TOOLTIP_WIDTH, fontRenderer);
			RenderHelper.enableGUIStandardItemLighting();
		}
	}
}
