package immersivenuclear.client;

import java.util.ArrayList;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.GuiIEContainerBase;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.block.TileDeconversionUnit;
import immersivenuclear.common.gui.ContainerDeconversionUnit;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.client.config.GuiUtils;

/**
 * Interface of the Deconversion Unit, in the interface standard (01/11): separate riveted panels with the machine drawn
 * between them, the world showing through the gaps.
 *   [depleted UF6 | steam]  both into the HYDROLYSIS REACTOR on the FEED HOOD; the ROTARY KILN (its bed in a window, pale
 *   yellow uranyl fluoride to the dark oxide), two riding rings, the girth gear on the feed side driven through the
 *   gearbox by the orange motor, the ELECTRIC FURNACE (its slots glow); the DISCHARGE HOOD, its chute into the oxide's box; the gas off the
 *   reactor through the two CANDLE FILTERS and the CONDENSER into the HF tank  [HF | energy over the status lamp]
 * Our sheet (textures/gui/deconversion_unit_gui.png, made by ferramentas/gui_desconversao_v1_esboco.py --jogo with the
 * same numbers) is the fixed background; what moves is drawn here.
 */
public class GuiDeconversionUnit extends GuiIEContainerBase
{
	static final String IE = "immersiveengineering:textures/gui/refinery.png";
	static final String SHEET = ImmersiveNuclear.MODID + ":textures/gui/deconversion_unit_gui.png";
	static final int SHEET_SIZE = 512;

	// Layout (same as gui_desconversao_v1_esboco.py)
	static final int ROW1 = 37, ROW2 = 62, ROW3 = 117, TANK_Y = ROW2;
	static final int UF6_X = 8, STEAM_X = 38, HF_X = 280, CONTROL_X = 306;
	static final int FEED_X = 72, FEED_Y = 72, FEED_W = 24, FEED_H = 46;
	static final int REACTOR_X = 74, REACTOR_Y = 36, REACTOR_W = 20, REACTOR_H = 34;
	/** The kiln; also JEI's click area. */
	public static final int DRUM_X = 96, DRUM_Y = 82, DRUM_W = 114, DRUM_H = 26;
	static final int WIN_X0 = 99, WIN_X1 = 207, WIN_Y1 = 100;
	static final int FURN_X = 146, FURN_Y = 70, FURN_W = 48, FURN_H = 50;
	static final int GEAR_X = 130;
	static final int[] RINGS = {100, 200};
	static final int MOTOR_X = 108, MOTOR_Y = 112, MOTOR_W = 34, MOTOR_H = 24;
	static final int DISCH_X = 210, DISCH_Y = 72, DISCH_W = 24, DISCH_H = 46;
	static final int FILTERS_X = 154, FILTERS_Y = 36, FILTERS_W = 34, FILTERS_H = 34;
	static final int COND_X = 240, COND_Y = 34, COND_W = 20, COND_H = 34;
	static final int CHUTE_X0 = 234, CHUTE_Y0 = 98, CHUTE_X1 = 246, CHUTE_Y1 = 100;
	static final int ENERGY_X = CONTROL_X + 2, ENERGY_Y = ROW1, ENERGY_W = 11, ENERGY_H = ROW2 + 51 - ROW1;
	static final int LAMP_X = CONTROL_X, LAMP_Y = ROW3 + 2, LAMP_SIZE = 16;
	static final int PANEL_X = ContainerDeconversionUnit.INV_X - 8, PANEL_Y = ContainerDeconversionUnit.INV_Y - 8;

	// Our sprites in the sheet: {u, v, w, h}
	static final int[] LENS = {0, 330, 10, 10};
	// IE's pieces in refinery.png
	static final int[] IE_SLOT = {35, 13, 20, 20}, IE_TANK = {11, 18, 20, 51}, IE_BLUE = {42, 10, 6, 3}, IE_ORANGE = {42, 49, 6, 3};

	static final int[] FEED = {0xe0, 0xcc, 0x5c}, OXIDE = {0x2e, 0x2a, 0x26};
	static final int COL_UF6 = 0xff5a4c66, COL_STEAM = 0xffe4e6ea, COL_GAS = 0xc8b4e6dc, COL_HF = 0xffc8e8e4, COL_GLOW = 0xff8a3c,
			COL_TOOTH = 0xff64686e, COL_TOOTH_DARK = 0xff2a2c30;

	final ContainerDeconversionUnit container;
	int ticks, turn;

	public GuiDeconversionUnit(ContainerDeconversionUnit container)
	{
		super(container);
		this.container = container;
		xSize = ContainerDeconversionUnit.WIDTH;
		ySize = ContainerDeconversionUnit.HEIGHT;
	}

	boolean running()
	{
		return container.state == TileDeconversionUnit.STATE_RUNNING;
	}

	float done()
	{
		return container.progress / (float)TileDeconversionUnit.CYCLE_TICKS;
	}

	@Override
	public void updateScreen()
	{
		super.updateScreen();
		ticks++;
		if(running())
			turn++;
	}

	// ---------- drawing helpers ----------
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
			case TileDeconversionUnit.STATE_RUNNING: return 0;
			case TileDeconversionUnit.STATE_NO_INPUT:
			case TileDeconversionUnit.STATE_DISABLED: return 1;
			case TileDeconversionUnit.STATE_NO_POWER:
			case TileDeconversionUnit.STATE_OUTPUT_FULL: return 2;
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
		int[][] xy = ContainerDeconversionUnit.SLOT_XY;
		for(int i = 0; i < xy.length; i++)
		{
			piece(IE_SLOT, xy[i][0] - 2, xy[i][1] - 2);
			boolean in = i == TileDeconversionUnit.SLOT_UF6_IN || i == TileDeconversionUnit.SLOT_STEAM_IN || i == TileDeconversionUnit.SLOT_HF_IN;
			piece(in ? IE_BLUE : IE_ORANGE, xy[i][0] + 5, xy[i][1] - 5);
		}
		int[] tx = {UF6_X, STEAM_X, HF_X};
		for(int i = 0; i < 3; i++)
		{
			piece(IE_TANK, tx[i], TANK_Y);
			piece(i < 2 ? IE_BLUE : IE_ORANGE, tx[i] + 7, TANK_Y - 3);
		}
		GuiGraphite.endTint();
		GlStateManager.color(1, 1, 1, 1);
		FluidTank[] tanks = {container.uf6, container.steam, container.hf};
		for(int i = 0; i < 3; i++)
			ClientUtils.handleGuiTank(tanks[i], guiLeft + tx[i] + 2, guiTop + TANK_Y + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);

		drawFlows();
		drawKiln();

		// Energy (IE's red gradient, inside IE's 7 px) and the lamp's lens
		int eh = (int)((ENERGY_H - 4) * (container.energy / (float)TileDeconversionUnit.ENERGY_CAPACITY));
		if(eh > 0)
			ClientUtils.drawGradientRect(guiLeft + ENERGY_X + 2, guiTop + ENERGY_Y + ENERGY_H - 2 - eh, guiLeft + ENERGY_X + ENERGY_W - 2, guiTop + ENERGY_Y + ENERGY_H - 2, 0xffb51500, 0xff600b00);
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.enableBlend();
		ClientUtils.bindTexture(SHEET);
		Gui.drawModalRectWithCustomSizedTexture(guiLeft + LAMP_X + LAMP_SIZE / 2 - 5, guiTop + LAMP_Y + LAMP_SIZE / 2 - 5, LENS[0] + lamp() * LENS[2], LENS[1], LENS[2], LENS[3], SHEET_SIZE, SHEET_SIZE);
	}

	/** What moves in the pipes' cores while a cycle runs: hexafluoride and steam in, the gas up and over, the acid out. */
	void drawFlows()
	{
		if(!running())
			return;
		rect(62, 45, 14, 2, COL_UF6);
		rect(62, 59, 14, 2, COL_STEAM);
		rect(83, 29, 2, REACTOR_Y - 29, COL_GAS);                 // off the reactor's top,
		rect(82, 30, 81, 2, COL_GAS);                             // over to the first filter,
		rect(161, 29, 2, 9, COL_GAS);
		rect(166, 55, 10, 2, COL_GAS);                            // into the second,
		rect(179, 29, 2, 9, COL_GAS);
		rect(178, 30, 73, 2, COL_GAS);                            // over to the condenser,
		rect(249, 29, 2, COND_Y - 29, COL_GAS);
		rect(249, COND_Y + COND_H, 2, 31, COL_HF);                // and the acid down to its tank
		rect(248, 98, 27, 2, COL_HF);
		// bubbles in the reactor's sight glass
		for(int k = 0; k < 3; k++)
			rect(REACTOR_X + 8 + k * 2, REACTOR_Y + 14 + Math.floorMod(5 - ticks / 2 - k * 2, 5), 1, 1, 0xfff0e8b0);
	}

	/** Whether x is in the kiln's window where nothing stands in front of it (the furnace, the rings, the girth gear). */
	static boolean bedShows(int x)
	{
		if(x >= FURN_X - 2 && x < FURN_X + FURN_W + 2)
			return false;
		if(x >= GEAR_X - 1 && x <= GEAR_X + 6)
			return false;
		for(int r : RINGS)
			if(x >= r && x < r + 4)
				return false;
		return true;
	}

	/** The bed tumbling along the kiln, yellow uranyl fluoride turning into the dark oxide; the furnace's slots and the
	 * reactor's heaters glowing; the gear turning; the oxide going down the chute. */
	void drawKiln()
	{
		boolean on = running();
		if(on || container.progress > 0)
			for(int x = WIN_X0; x < WIN_X1; x++)
			{
				if(!bedShows(x))
					continue;
				float t = (x - DRUM_X) / (float)DRUM_W;
				int r = Math.round(FEED[0] + (OXIDE[0] - FEED[0]) * t), g = Math.round(FEED[1] + (OXIDE[1] - FEED[1]) * t),
						b = Math.round(FEED[2] + (OXIDE[2] - FEED[2]) * t);
				int wave = (int)Math.round(Math.sin(x * 0.6 - turn * 0.25));
				int top = WIN_Y1 - 4 + wave;
				rect(x, top, 1, WIN_Y1 - top, 0xff000000 | r << 16 | g << 8 | b);
			}
		if(!on)
			return;
		// The furnace's six heating slots and the reactor's two heater bands glow, breathing a little
		int a = Math.round(170 + 60 * (float)Math.sin(ticks * 0.15));
		for(int k = 0; k < 3; k++)
			for(int sx : new int[]{FURN_X + 4, FURN_X + 27})
			{
				rect(sx, FURN_Y + 10 + k * 12, 17, 3, a << 24 | COL_GLOW);
				rect(sx, FURN_Y + 11 + k * 12, 17, 1, 0xffffc070);
			}
		for(int by : new int[]{REACTOR_Y + 6, REACTOR_Y + 24})
			rect(REACTOR_X, by + 2, REACTOR_W, 1, a << 24 | 0xff9a50);
		// The girth gear's teeth moving round
		int phase = (turn / 2) % 2;
		for(int y = DRUM_Y - 4; y < DRUM_Y + DRUM_H + 4; y++)
		{
			int c = (y + phase) % 2 == 0 ? COL_TOOTH : COL_TOOTH_DARK;
			rect(GEAR_X - 1, y, 1, 1, c);
			rect(GEAR_X + 6, y, 1, 1, c);
		}
		// Oxide grains going down the chute into the box
		for(int k = 0; k < 3; k++)
		{
			float f = Math.floorMod(ticks + k * 7, 21) / 21F;
			int x = Math.round(CHUTE_X0 + (CHUTE_X1 - CHUTE_X0) * f), y = Math.round(CHUTE_Y0 + 1 + (CHUTE_Y1 - CHUTE_Y0) * f);
			rect(x, y, 2, 2, 0xff3a3530);
		}
	}

	boolean over(int mx, int my, int x, int y, int w, int h)
	{
		return mx >= guiLeft + x && mx < guiLeft + x + w && my >= guiTop + y && my < guiTop + y + h;
	}

	static final String K = "gui.immersivenuclear.deconversion_unit.";

	@Override
	public void drawScreen(int mx, int my, float partialTicks)
	{
		super.drawScreen(mx, my, partialTicks);
		ArrayList<String> tip = new ArrayList<>();
		String[] tanks = {"uf6", "steam", "hf"};
		int[] tx = {UF6_X, STEAM_X, HF_X};
		FluidTank[] tt = {container.uf6, container.steam, container.hf};
		for(int i = 0; i < 3; i++)
		{
			ClientUtils.handleGuiTank(tt[i], guiLeft + tx[i] + 2, guiTop + TANK_Y + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, tip);
			if(over(mx, my, tx[i], TANK_Y, 20, 51))
				tip.add(0, I18n.format(K + tanks[i]));
		}
		// the furnace before the kiln (it stands in front of it), the reactor before the feed hood
		String[] parts = {"reactor", "feed", "furnace", "kiln", "motor", "discharge", "filters", "condenser"};
		int[][] boxes = {{REACTOR_X, REACTOR_Y, REACTOR_W, REACTOR_H}, {FEED_X, FEED_Y, FEED_W, FEED_H}, {FURN_X, FURN_Y, FURN_W, FURN_H},
				{DRUM_X, DRUM_Y, DRUM_W, DRUM_H}, {MOTOR_X, MOTOR_Y, MOTOR_W, MOTOR_H}, {DISCH_X, DISCH_Y, DISCH_W, DISCH_H},
				{FILTERS_X, FILTERS_Y, FILTERS_W, FILTERS_H}, {COND_X, COND_Y, COND_W, COND_H}};
		for(int i = 0; i < boxes.length; i++)
			if(over(mx, my, boxes[i][0], boxes[i][1], boxes[i][2], boxes[i][3]))
			{
				tip.add(I18n.format(K + parts[i]));
				tip.add("§7" + I18n.format(K + parts[i] + "_info"));
				if(parts[i].equals("kiln") || parts[i].equals("furnace"))
				{
					tip.add("§7" + I18n.format(K + "cycle", TileDeconversionUnit.UF6_PER_CYCLE, TileDeconversionUnit.STEAM_PER_CYCLE,
							TileDeconversionUnit.OXIDE_PER_CYCLE, TileDeconversionUnit.HF_PER_CYCLE, TileDeconversionUnit.CYCLE_TICKS / 20));
					if(container.progress > 0)
						tip.add("§7" + I18n.format(K + "progress", Math.round(100 * done())));
				}
				break;
			}
		String[] slotTips = {"oxide", "uf6_in", "uf6_out", "steam_in", "steam_out", "hf_in", "hf_out"};
		int[][] xy = ContainerDeconversionUnit.SLOT_XY;
		for(int i = 0; i < xy.length; i++)
			if(over(mx, my, xy[i][0], xy[i][1], 16, 16) && container.getSlot(i).getStack().isEmpty())
				tip.add(I18n.format(K + slotTips[i]));
		if(over(mx, my, ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H))
		{
			tip.add(container.energy + "/" + TileDeconversionUnit.ENERGY_CAPACITY + " IF");
			tip.add("§7" + I18n.format(K + "energy", TileDeconversionUnit.ENERGY_PER_TICK));
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
