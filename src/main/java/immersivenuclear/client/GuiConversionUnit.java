package immersivenuclear.client;

import java.util.ArrayList;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.GuiIEContainerBase;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.block.TileConversionUnit;
import immersivenuclear.common.gui.ContainerConversionUnit;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.client.config.GuiUtils;

/**
 * Interface of the Conversion Unit, in the interface standard (01/11): separate riveted panels with the machine drawn
 * between them, the world showing through the gaps.
 *   [yellowcake in / ash out | hydrogen | acid]  the REDUCTION REACTOR (copper heating bands, the yellowcake hopper), the
 *   HYDROFLUORINATOR (a window on the green salt), the FLUORINE CELL CABINET, the SCRUBBER and the white AUTOCLAVE
 *   [hexafluoride | effluent | energy over the status lamp]
 * While it runs the gases move in their pipes. Our sheet (textures/gui/conversion_unit_gui.png, made by
 * ferramentas/gui_conversao_v4_esboco.py --jogo with the same numbers) is the fixed background; what moves is drawn here.
 */
public class GuiConversionUnit extends GuiIEContainerBase
{
	static final String IE = "immersiveengineering:textures/gui/refinery.png";
	static final String SHEET = ImmersiveNuclear.MODID + ":textures/gui/conversion_unit_gui.png";
	static final int SHEET_SIZE = 512;

	// Layout (same as gui_conversao_v4_esboco.py)
	static final int ROW1 = 37, ROW2 = 62, ROW3 = 117, TANK_Y = ROW2;
	static final int CAKE_X = 8, H2_X = 38, HF_X = 68, UF6_X = 250, EFF_X = 280, CONTROL_X = 306;
	/** The reduction reactor; also JEI's click area. */
	public static final int AREA_X = 106, AREA_Y = 50, AREA_W = 132, AREA_H = 86;
	static final int ENERGY_X = CONTROL_X + 2, ENERGY_Y = ROW1, ENERGY_W = 11, ENERGY_H = ROW2 + 51 - ROW1;
	static final int LAMP_X = CONTROL_X, LAMP_Y = ROW3 + 2, LAMP_SIZE = 16;
	static final int PANEL_X = ContainerConversionUnit.INV_X - 8, PANEL_Y = ContainerConversionUnit.INV_Y - 8;

	// Our sprites in the sheet: {u, v, w, h}
	static final int[] LENS = {0, 330, 10, 10};
	// IE's pieces in refinery.png
	static final int[] IE_SLOT = {35, 13, 20, 20}, IE_TANK = {11, 18, 20, 51}, IE_BLUE = {42, 10, 6, 3}, IE_ORANGE = {42, 49, 6, 3};

	/** The windows, {x, y, w, h}: dark in the background, their lit copies side by side in the sheet from WINDOWS_V. */
	static final int[][] WINDOWS = {{113, 82, 8, 8}, {138, 84, 6, 15}, {156, 76, 12, 14}, {180, 85, 8, 22}, {193, 85, 8, 22}, {206, 85, 8, 22}, {196, 39, 4, 12}, {196, 57, 4, 12}};
	static final int WINDOWS_V = 345;
	static final int COL_H2 = 0xc87c8796, COL_HF = 0xc8315651, COL_UF6 = 0xc8a6a2ac, COL_EFF = 0xc84a7a2c;
	static final int LED_RED = 0xffd03830, LED_ORANGE = 0xffe89830, LED_GREEN = 0xff50c050;

	final ContainerConversionUnit container;
	int ticks;

	public GuiConversionUnit(ContainerConversionUnit container)
	{
		super(container);
		this.container = container;
		xSize = ContainerConversionUnit.WIDTH;
		ySize = ContainerConversionUnit.HEIGHT;
	}

	boolean running()
	{
		return container.state == TileConversionUnit.STATE_RUNNING;
	}

	@Override
	public void updateScreen()
	{
		super.updateScreen();
		ticks++;
	}

	void piece(int[] p, int x, int y)
	{
		drawTexturedModalRect(guiLeft + x, guiTop + y, p[0], p[1], p[2], p[3]);
	}

	void rect(int x, int y, int w, int h, int argb)
	{
		if(w > 0 && h > 0)
			drawRect(guiLeft + x, guiTop + y, guiLeft + x + w, guiTop + y + h, argb);
	}

	/** The lamp's colour: 0 green running, 1 amber waiting (input, or stopped by redstone), 2 red stopped (no power,
	 * output full), 3 off (idle). */
	int lamp()
	{
		switch(container.state)
		{
			case TileConversionUnit.STATE_RUNNING: return 0;
			case TileConversionUnit.STATE_NO_INPUT:
			case TileConversionUnit.STATE_DISABLED: return 1;
			case TileConversionUnit.STATE_NO_POWER:
			case TileConversionUnit.STATE_OUTPUT_FULL: return 2;
			default: return 3;
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mx, int my)
	{
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.enableBlend();
		ClientUtils.bindTexture(SHEET);
		Gui.drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, xSize, ySize, SHEET_SIZE, SHEET_SIZE);

		// IE's wooden inventory panel; IE's pieces, a little darker
		ClientUtils.bindTexture(IE);
		drawTexturedModalRect(guiLeft + PANEL_X, guiTop + PANEL_Y, 0, 77, 176, 89);
		GuiGraphite.ieTint();
		int[][] xy = ContainerConversionUnit.SLOT_XY;
		for(int i = 0; i < xy.length; i++)
		{
			piece(IE_SLOT, xy[i][0] - 2, xy[i][1] - 2);
			// inputs blue (yellowcake, hydrogen, acid, and the empty containers' marks follow the standard), outputs orange
			boolean in = i == 0 || i == 2 || i == 4 || i == 6 || i == 8;
			piece(in ? IE_BLUE : IE_ORANGE, xy[i][0] + 5, xy[i][1] - 5);
		}
		for(int x : new int[]{H2_X, HF_X, UF6_X, EFF_X})
		{
			piece(IE_TANK, x, TANK_Y);
			piece(x == H2_X || x == HF_X ? IE_BLUE : IE_ORANGE, x + 7, TANK_Y - 3);
		}
		GuiGraphite.endTint();
		GlStateManager.color(1, 1, 1, 1);
		FluidTank[] tanks = {container.h2, container.hf, container.uf6, container.effluent};
		int[] tx = {H2_X, HF_X, UF6_X, EFF_X};
		for(int i = 0; i < 4; i++)
			ClientUtils.handleGuiTank(tanks[i], guiLeft + tx[i] + 2, guiTop + TANK_Y + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);

		drawFlows();

		// Energy (IE's red gradient, inside IE's 7 px) and the lamp's lens
		int eh = (int)((ENERGY_H - 4) * Math.min(1, container.energy / (float)TileConversionUnit.ENERGY_CAPACITY));
		if(eh > 0)
			ClientUtils.drawGradientRect(guiLeft + ENERGY_X + 2, guiTop + ENERGY_Y + ENERGY_H - 2 - eh, guiLeft + ENERGY_X + ENERGY_W - 2, guiTop + ENERGY_Y + ENERGY_H - 2, 0xffb51500, 0xff600b00);
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.enableBlend();
		ClientUtils.bindTexture(SHEET);
		Gui.drawModalRectWithCustomSizedTexture(guiLeft + LAMP_X + LAMP_SIZE / 2 - 5, guiTop + LAMP_Y + LAMP_SIZE / 2 - 5, LENS[0] + lamp() * LENS[2], LENS[1], LENS[2], LENS[3], SHEET_SIZE, SHEET_SIZE);
	}

	/** What changes with the machine's state: the LEDs always; while it runs, the coils' warmth, the lit windows, the bubbles, the flame and the flows in the pipes where they show. */
	void drawFlows()
	{
		boolean on = running();
		if(on)
		{
			for(int y = 75; y < 114; y += 4)                 // the reactor's coils, faintly warm
				rect(104, y, 26, 2, 0x38ff7828);
			GlStateManager.color(1, 1, 1, 1);
			GlStateManager.enableBlend();
			ClientUtils.bindTexture(SHEET);
			int u = 0;
			for(int i = 0; i < WINDOWS.length; i++)
			{
				int[] w = WINDOWS[i];
				Gui.drawModalRectWithCustomSizedTexture(guiLeft + w[0], guiTop + w[1], u, WINDOWS_V, w[2], w[3], SHEET_SIZE, SHEET_SIZE);
				u += w[2] + 1;
			}
			for(int i = 0; i < 4; i++)                       // the hydrofluorinator's bubbles
				rect(158 + i * 3, 89 - Math.floorMod(ticks / 2 + i * 5, 12), 1, 1, 0xffe0ffb0);
			for(int k = 0; k < 3; k++)                       // gas rising at the cells' electrodes
				for(int i = 0; i < 3; i++)
					rect(181 + k * 13 + i * 3, 105 - Math.floorMod((container.draining ? ticks / 2 : ticks) + i * 7 + k * 4, 20), 1, 1, 0xfff0ffff);
			if(ticks % 4 < 2)                                // the flame flickers
			{
				rect(196, 39, 4, 12, 0x30fff0a0);
				rect(196, 57, 4, 12, 0x30fff0a0);
			}
			rect(96, 93, 5, 2, COL_H2);                      // hydrogen, up to the reactor's frame
			rect(96, 47, 5, 2, COL_HF);                      // the acid over the top, around what stands in front of it
			rect(104, 47, 4, 2, COL_HF);
			rect(126, 47, 5, 2, COL_HF);
			rect(134, 47, 24, 2, COL_HF);
			rect(161, 52, 2, 2, COL_HF);                     // and down into the nozzle
			rect(238, 93, 4, 2, COL_EFF);                    // effluent out
		}
		// The cold traps: frost grows on the radiators with the batch; the hexafluoride only moves, and the drum only stirs, while they drain
		int frost = Math.min(36, 36 * container.trapped / TileConversionUnit.TRAP_CAPACITY);
		rect(219, 116 - frost, 10, frost, 0x70c8e8ff);
		if(container.draining)
		{
			rect(238, 127, 4, 2, COL_UF6);
			if(ticks % 8 < 4)
				rect(176, 118, 58, 17, 0x28a6a2ac);
		}
		int state = container.state;
		for(int k = 0; k < 3; k++)                           // the same LEDs as on the machine (RenderConversionUnit)
		{
			int x = 178 + k * 13;
			if(state == TileConversionUnit.STATE_NO_POWER || state == TileConversionUnit.STATE_OUTPUT_FULL)
				rect(x + 2, 81, 1, 1, LED_RED);
			if(state == TileConversionUnit.STATE_NO_INPUT || state == TileConversionUnit.STATE_DISABLED || container.draining && ticks % 10 < 5)
				rect(x + 4, 81, 1, 1, LED_ORANGE);
			if(state != TileConversionUnit.STATE_NO_POWER)
				rect(x + 6, 81, 1, 1, LED_GREEN);
			if(on && (container.draining ? (ticks + k * 3) % 12 < 6 : (ticks + k * 13) % 40 < 24))
				rect(x + 8, 81, 1, 1, LED_GREEN);
			rect(on ? x + 8 : x + 3, 112, 1, 1, on ? LED_GREEN : LED_RED);
		}
	}

	boolean over(int mx, int my, int x, int y, int w, int h)
	{
		return mx >= guiLeft + x && mx < guiLeft + x + w && my >= guiTop + y && my < guiTop + y + h;
	}

	static final String K = "gui.immersivenuclear.conversion_unit.";

	@Override
	public void drawScreen(int mx, int my, float partialTicks)
	{
		super.drawScreen(mx, my, partialTicks);
		ArrayList<String> tip = new ArrayList<>();
		String[] names = {"hydrogen", "acid", "hexafluoride", "effluent"};
		int[] tx = {H2_X, HF_X, UF6_X, EFF_X};
		FluidTank[] tt = {container.h2, container.hf, container.uf6, container.effluent};
		for(int i = 0; i < 4; i++)
		{
			ClientUtils.handleGuiTank(tt[i], guiLeft + tx[i] + 2, guiTop + TANK_Y + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, tip);
			if(over(mx, my, tx[i], TANK_Y, 20, 51))
				tip.add(0, I18n.format(K + names[i]));
		}
		if(over(mx, my, 101, 34, 33, 102))
		{
			tip.add(I18n.format(K + "reactor"));
			tip.add("§7" + I18n.format(K + "cycle", TileConversionUnit.H2_PER_CYCLE, TileConversionUnit.HF_PER_CYCLE, TileConversionUnit.UF6_PER_CYCLE,
					TileConversionUnit.EFFLUENT_PER_CYCLE, TileConversionUnit.CYCLE_TICKS / 20));
			if(container.progress > 0)
				tip.add("§7" + I18n.format(K + "progress", Math.round(100F * container.progress / TileConversionUnit.CYCLE_TICKS)));
		}
		if(over(mx, my, 147, 54, 30, 82))
			tip.add(I18n.format(K + "hydrofluorinator"));
		if(over(mx, my, 178, 78, 38, 40))
			tip.add(I18n.format(K + "cells"));
		if(over(mx, my, 218, 71, 11, 47))
		{
			tip.add(I18n.format(K + "cold_traps"));
			tip.add("§7" + container.trapped + " / " + TileConversionUnit.TRAP_CAPACITY + " mB");
		}
		if(over(mx, my, 229, 42, 8, 76))
			tip.add(I18n.format(K + "scrubber"));
		if(over(mx, my, 173, 118, 65, 17))
			tip.add(I18n.format(K + "autoclave"));
		String[] slotTips = {"cake_in", "ash_out", "hydrogen_in", "hydrogen_out", "acid_in", "acid_out", "hexafluoride_in", "hexafluoride_out", "effluent_in", "effluent_out"};
		int[][] xy = ContainerConversionUnit.SLOT_XY;
		for(int i = 0; i < xy.length; i++)
			if(over(mx, my, xy[i][0], xy[i][1], 16, 16) && container.getSlot(i).getStack().isEmpty())
				tip.add(I18n.format(K + slotTips[i]));
		if(over(mx, my, ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H))
		{
			tip.add(container.energy + "/" + TileConversionUnit.ENERGY_CAPACITY + " IF");
			tip.add("§7" + I18n.format(K + "energy", TileConversionUnit.ENERGY_PER_TICK));
		}
		if(over(mx, my, LAMP_X, LAMP_Y, LAMP_SIZE, LAMP_SIZE))
		{
			tip.add(I18n.format(K + "state." + container.state));
			tip.add("§7" + I18n.format(container.redstoneInverted ? "gui.immersivenuclear.redstone.inverted" : "gui.immersivenuclear.redstone.normal"));
		}
		if(!tip.isEmpty())
		{
			// Long lines wrap, and the tooltip stays on the screen
			GuiUtils.drawHoveringText(tip, mx, my, width, height, GuiGraphite.TOOLTIP_WIDTH, fontRenderer);
			RenderHelper.enableGUIStandardItemLighting();
		}
	}
}
