package immersivenuclear.client;

import java.util.ArrayList;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.GuiIEContainerBase;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.block.TileElectrolyzer;
import immersivenuclear.common.gui.ContainerElectrolyzer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.client.config.GuiUtils;

/**
 * Interface of the Electrolyzer, in the interface standard (01/11): separate riveted panels with the machine drawn
 * between them, the world showing through the gaps.
 *   [water: in / tank / out]  the RECTIFIER (fins, the green HV bushings, copper bus bars); the CELL STACK (blue cells
 *   between copper coil plates, its outlet header); the ELECTROLYTE TANK on its beam over the stack; the OXYGEN and
 *   HYDROGEN SEPARATORS (level gauges, pressure gauges, relief valves)  [hydrogen | oxygen | energy over the status lamp]
 * While it runs: bubbles rise in the cells, which glow softly between the plates (the part that glows); the water, the
 * gas from the header and each separator's gas move in their pipes; the electrolyte stands in the tank's window; the
 * oxygen's relief valve puffs when its tank is full and it vents.
 * Our sheet (textures/gui/electrolyzer_gui.png, made by ferramentas/gui_eletrolisador_v2_esboco.py --jogo with the
 * same numbers) is the fixed background; what moves is drawn here.
 */
public class GuiElectrolyzer extends GuiIEContainerBase
{
	static final String IE = "immersiveengineering:textures/gui/refinery.png";
	static final String SHEET = ImmersiveNuclear.MODID + ":textures/gui/electrolyzer_gui.png";
	static final int SHEET_SIZE = 512;

	// Layout (same as gui_eletrolisador_v2_esboco.py)
	static final int ROW1 = 37, ROW2 = 62, ROW3 = 117, TANK_Y = ROW2;
	static final int WATER_X = 8, H2_X = 242, O2_X = 272, CONTROL_X = 298;
	static final int RECT_X = 44, RECT_Y = 84, RECT_W = 40, RECT_H = 52;
	/** The cell stack; also JEI's click area (INJeiPlugin). */
	public static final int STACK_X = 98, STACK_Y = 104, STACK_W = 72, STACK_H = 32;
	static final int TANK_X = 104, TANK_Y0 = 54, TANK_W = 60, TANK_H = 28, LYE_X = 112, LYE_Y = 61, LYE_W = 44, LYE_H = 14;
	static final int O2_SEP_X = 180, O2_SEP_Y = 44, O2_SEP_W = 20, O2_SEP_H = 92;
	static final int H2_SEP_X = 206, H2_SEP_Y = 48, H2_SEP_W = 22, H2_SEP_H = 88;
	static final int ENERGY_X = CONTROL_X + 2, ENERGY_Y = ROW1, ENERGY_W = 11, ENERGY_H = ROW2 + 51 - ROW1;
	static final int LAMP_X = CONTROL_X, LAMP_Y = ROW3 + 2, LAMP_SIZE = 16;
	static final int PANEL_X = ContainerElectrolyzer.INV_X - 8, PANEL_Y = ContainerElectrolyzer.INV_Y - 8;

	// Our sprites in the sheet: {u, v, w, h}
	static final int[] LENS = {0, 330, 10, 10};
	// IE's pieces in refinery.png
	static final int[] IE_SLOT = {35, 13, 20, 20}, IE_TANK = {11, 18, 20, 51}, IE_BLUE = {42, 10, 6, 3}, IE_ORANGE = {42, 49, 6, 3};

	static final int COL_WATER = 0xff3a6ac0, COL_STEAM = 0xc8e0e4e8, COL_GAS_H2 = 0xc8eef2f8, COL_GAS_O2 = 0xc8a8d0f0, COL_LYE = 0xff9cc8e0,
			COL_LYE_TOP = 0xffd8eef6, COL_BUBBLE = 0xffe0f2fa, COL_GLOW = 0x78a0dcff;

	final ContainerElectrolyzer container;
	int ticks;

	public GuiElectrolyzer(ContainerElectrolyzer container)
	{
		super(container);
		this.container = container;
		xSize = ContainerElectrolyzer.WIDTH;
		ySize = ContainerElectrolyzer.HEIGHT;
	}

	boolean running()
	{
		return container.state == TileElectrolyzer.STATE_RUNNING;
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
			case TileElectrolyzer.STATE_RUNNING: return 0;
			case TileElectrolyzer.STATE_NO_INPUT:
			case TileElectrolyzer.STATE_DISABLED: return 1;
			case TileElectrolyzer.STATE_NO_POWER:
			case TileElectrolyzer.STATE_OUTPUT_FULL: return 2;
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
		int[][] xy = ContainerElectrolyzer.SLOT_XY;
		for(int i = 0; i < xy.length; i++)
		{
			piece(IE_SLOT, xy[i][0] - 2, xy[i][1] - 2);
			piece(i % 2 == 0 ? IE_BLUE : IE_ORANGE, xy[i][0] + 5, xy[i][1] - 5);
		}
		for(int x : new int[]{WATER_X, H2_X, O2_X})
		{
			piece(IE_TANK, x, TANK_Y);
			piece(x == WATER_X ? IE_BLUE : IE_ORANGE, x + 7, TANK_Y - 3);
		}
		GuiGraphite.endTint();
		GlStateManager.color(1, 1, 1, 1);
		ClientUtils.handleGuiTank(container.water, guiLeft + WATER_X + 2, guiTop + TANK_Y + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);
		ClientUtils.handleGuiTank(container.h2, guiLeft + H2_X + 2, guiTop + TANK_Y + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);
		ClientUtils.handleGuiTank(container.o2, guiLeft + O2_X + 2, guiTop + TANK_Y + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);

		drawFlows();
		drawMachine(partialTicks);

		// Energy (IE's red gradient, inside IE's 7 px) and the lamp's lens
		int eh = (int)((ENERGY_H - 4) * Math.min(1, container.energy / (float)TileElectrolyzer.ENERGY_CAPACITY));
		if(eh > 0)
			ClientUtils.drawGradientRect(guiLeft + ENERGY_X + 2, guiTop + ENERGY_Y + ENERGY_H - 2 - eh, guiLeft + ENERGY_X + ENERGY_W - 2, guiTop + ENERGY_Y + ENERGY_H - 2, 0xffb51500, 0xff600b00);
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.enableBlend();
		ClientUtils.bindTexture(SHEET);
		Gui.drawModalRectWithCustomSizedTexture(guiLeft + LAMP_X + LAMP_SIZE / 2 - 5, guiTop + LAMP_Y + LAMP_SIZE / 2 - 5, LENS[0] + lamp() * LENS[2], LENS[1], LENS[2], LENS[3], SHEET_SIZE, SHEET_SIZE);
	}

	/** What moves in the pipes' cores while it runs. */
	void drawFlows()
	{
		if(!running())
			return;
		int feed = container.steam ? COL_STEAM : COL_WATER;
		rect(17, 21, 2, 6, feed);                       // water up out of its panel,
		rect(17, 21, 98, 2, feed);                      // over,
		rect(113, 21, 2, TANK_Y0 - 21, feed);           // down into the electrolyte tank
		rect(133, TANK_Y0 + TANK_H, 2, STACK_Y - 6 - TANK_Y0 - TANK_H, COL_LYE);   // the tank's feed into the header
		rect(STACK_X + STACK_W, 100, O2_SEP_X - STACK_X - STACK_W, 2, COL_GAS_O2);  // header -> oxygen separator
		rect(O2_SEP_X + O2_SEP_W, 111, H2_SEP_X - O2_SEP_X - O2_SEP_W, 2, COL_GAS_H2); // -> hydrogen separator
		rect(189, 32, 2, O2_SEP_Y - 3 - 32, COL_GAS_O2);                            // oxygen up,
		rect(189, 32, 48, 2, COL_GAS_O2);                                           // into its tank
		rect(216, 38, 2, H2_SEP_Y - 3 - 38, COL_GAS_H2);                            // hydrogen up,
		rect(216, 38, 21, 2, COL_GAS_H2);                                           // into its tank
		rect(STACK_X + STACK_W, 129, H2_SEP_X - STACK_X - STACK_W, 2, COL_LYE);    // the lye's return
	}

	/** The electrolyte in its tank, the cells glowing and bubbling, the separators' levels, the oxygen valve's puff. */
	void drawMachine(float partialTicks)
	{
		boolean on = running();
		// the electrolyte's level in the tank's window: full while there is water to feed it
		float feed = Math.min(1, container.waterAmount / (float)(TileElectrolyzer.WATER_PER_CYCLE * 4));
		int lh = Math.round(LYE_H * (0.35F + 0.5F * feed));
		rect(LYE_X, LYE_Y + LYE_H - lh, LYE_W, lh, COL_LYE);
		rect(LYE_X, LYE_Y + LYE_H - lh, LYE_W, 1, COL_LYE_TOP);
		// the separators' level gauges: the electrolyte at half height, stirred by the gas while it runs
		for(int[] s : new int[][]{{O2_SEP_X, O2_SEP_Y, O2_SEP_W, O2_SEP_H}, {H2_SEP_X, H2_SEP_Y, H2_SEP_W, H2_SEP_H}})
		{
			int gx = s[0] + s[2], gy = s[1] + 10, gh = s[3] - 16;
			int level = gh / 2 + (on ? Math.round((float)Math.sin((ticks + partialTicks) * 0.3 + s[0]) * 1.5F) : 0);
			rect(gx, gy + gh - level, 1, level, COL_LYE);
		}
		if(on)
		{
			// the cells glow softly between the copper plates, breathing a little (the part that glows)
			int a = Math.round(90 + 40 * (float)Math.sin((ticks + partialTicks) * 0.12));
			for(int x = STACK_X; x < STACK_X + STACK_W; x++)
				if((x - STACK_X) % 4 != 0)
					rect(x, STACK_Y + 2, 1, STACK_H - 4, a << 24 | (COL_GLOW & 0xffffff));
			// bubbles rising in the cells
			for(int k = 0; k < 24; k++)
			{
				int x = STACK_X + 1 + (k * 11) % (STACK_W - 2);
				if((x - STACK_X) % 4 == 0)
					x++;
				int y = STACK_Y + STACK_H - 3 - Math.floorMod(ticks * 2 + k * 7, STACK_H - 6);
				rect(x, y, 1, 1, COL_BUBBLE);
			}
		}
		// the oxygen's relief valve venting: a puff over it
		if(container.venting > 0)
		{
			float f = 1 - container.venting / (float)TileElectrolyzer.VENT_TICKS;
			int cx = O2_SEP_X + 5, top = O2_SEP_Y - 13;
			for(int k = 0; k < 6; k++)
			{
				int x = cx - 3 + (k * 5) % 8 + Math.round(f * (k % 3 - 1) * 4);
				int y = top - 2 - Math.round(f * 10) - k % 3 * 2;
				rect(x, y, 2, 2, (Math.round(200 * (1 - f)) << 24) | 0xe8f0f4);
			}
		}
	}

	boolean over(int mx, int my, int x, int y, int w, int h)
	{
		return mx >= guiLeft + x && mx < guiLeft + x + w && my >= guiTop + y && my < guiTop + y + h;
	}

	static final String K = "gui.immersivenuclear.electrolyzer.";

	@Override
	public void drawScreen(int mx, int my, float partialTicks)
	{
		super.drawScreen(mx, my, partialTicks);
		ArrayList<String> tip = new ArrayList<>();
		String[] tanks = {"water", "hydrogen", "oxygen"};
		int[] tx = {WATER_X, H2_X, O2_X};
		FluidTank[] tt = {container.water, container.h2, container.o2};
		for(int i = 0; i < 3; i++)
		{
			ClientUtils.handleGuiTank(tt[i], guiLeft + tx[i] + 2, guiTop + TANK_Y + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, tip);
			if(over(mx, my, tx[i], TANK_Y, 20, 51))
				tip.add(0, I18n.format(K + tanks[i]));
		}
		if(over(mx, my, O2_X, TANK_Y, 20, 51) && container.venting > 0)
			tip.add("§e" + I18n.format(K + "venting"));
		if(over(mx, my, RECT_X, RECT_Y - 22, RECT_W, RECT_H + 22))
		{
			tip.add(I18n.format(K + "rectifier"));
			tip.add("§7" + I18n.format(K + "rectifier_info"));
		}
		if(over(mx, my, STACK_X - 5, STACK_Y - 7, STACK_W + 10, STACK_H + 7))
		{
			tip.add(I18n.format(K + "stack"));
			tip.add("§7" + I18n.format(K + "cycle", TileElectrolyzer.WATER_PER_CYCLE, TileElectrolyzer.H2_PER_CYCLE, TileElectrolyzer.O2_PER_CYCLE,
					TileElectrolyzer.CYCLE_TICKS / 20));
			if(container.progress > 0)
				tip.add("§7" + I18n.format(K + "progress", Math.round(100F * container.progress / TileElectrolyzer.CYCLE_TICKS)));
		}
		if(over(mx, my, TANK_X, TANK_Y0, TANK_W, TANK_H))
		{
			tip.add(I18n.format(K + "electrolyte"));
			tip.add("§7" + I18n.format(K + "electrolyte_info"));
		}
		if(over(mx, my, O2_SEP_X, O2_SEP_Y - 12, O2_SEP_W, O2_SEP_H + 12))
			tip.add(I18n.format(K + "separator_o2"));
		if(over(mx, my, H2_SEP_X, H2_SEP_Y - 12, H2_SEP_W, H2_SEP_H + 12))
			tip.add(I18n.format(K + "separator_h2"));
		String[] slotTips = {"water_in", "water_out", "hydrogen_in", "hydrogen_out", "oxygen_in", "oxygen_out"};
		int[][] xy = ContainerElectrolyzer.SLOT_XY;
		for(int i = 0; i < xy.length; i++)
			if(over(mx, my, xy[i][0], xy[i][1], 16, 16) && container.getSlot(i).getStack().isEmpty())
				tip.add(I18n.format(K + slotTips[i]));
		if(over(mx, my, ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H))
		{
			tip.add(container.energy + "/" + TileElectrolyzer.ENERGY_CAPACITY + " IF");
			tip.add("§7" + I18n.format(K + "energy", container.steam ? TileElectrolyzer.ENERGY_PER_TICK_STEAM : TileElectrolyzer.ENERGY_PER_TICK));
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
