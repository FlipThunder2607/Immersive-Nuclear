package immersivenuclear.client;

import java.util.ArrayList;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.GuiIEContainerBase;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.block.TileHFKiln;
import immersivenuclear.common.gui.ContainerHFKiln;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.client.config.GuiUtils;

/**
 * Interface of the Hydrofluoric Acid Kiln, in the interface standard (01/11): separate riveted panels with the machine
 * drawn between them, the world showing through the gaps.
 *   [sulfuric acid: in / tank / out]  the fluorite's box over the dark FEED HEAD; the copper ROTARY DRUM (its bed in a
 *   window, purple fluorite to white gypsum; the copper heating coil over 1/4..2/4 of it, it glows; two riding rings;
 *   the girth gear in the middle, driven through the gearbox by the green motor); the dark DISCHARGE HEAD, its chute into
 *   the gypsum's box; the gas through the SCRUBBER and the CONDENSER into the HF tank  [HF | energy over the status lamp]
 * Our sheet (textures/gui/hf_kiln_gui.png, made by ferramentas/gui_forno_hf_v4_esboco.py --jogo with the same numbers)
 * is the fixed background; what moves is drawn here. The JEI page keeps its own sheet.
 */
public class GuiHFKiln extends GuiIEContainerBase
{
	static final String IE = "immersiveengineering:textures/gui/refinery.png";
	static final String SHEET = ImmersiveNuclear.MODID + ":textures/gui/hf_kiln_gui.png";
	static final int SHEET_SIZE = 512;

	// Layout (same as gui_forno_hf_v4_esboco.py)
	static final int ROW1 = 37, ROW2 = 62, ROW3 = 117, TANK_Y = ROW2;
	static final int ACID_X = 8, HF_X = 242, CONTROL_X = 268;
	static final int FEED_X = 42, FEED_Y = 72, FEED_W = 24, FEED_H = 46;
	/** The drum; also JEI's click area (INJeiPlugin). */
	public static final int DRUM_X = 66, DRUM_Y = 82, DRUM_W = 106, DRUM_H = 26;
	static final int WIN_X0 = 69, WIN_X1 = 169, WIN_Y0 = 90, WIN_Y1 = 100;
	static final int COIL_X0 = 90, COIL_X1 = 112, GEAR_X = 116;
	static final int[] RINGS = {72, 156};
	static final int MOTOR_X = 112, MOTOR_Y = 112, MOTOR_W = 46, MOTOR_H = 24;
	static final int DISCH_X = 172, DISCH_Y = 72, DISCH_W = 24, DISCH_H = 46;
	static final int SCRUB_X = 204, SCRUB_Y = 34, SCRUB_W = 18, SCRUB_H = 28;
	static final int COND_X = 204, COND_Y = 66, COND_W = 18, COND_H = 28;
	static final int CHUTE_X0 = 196, CHUTE_Y0 = 98, CHUTE_X1 = 213, CHUTE_Y1 = 104;
	static final int ENERGY_X = CONTROL_X + 2, ENERGY_Y = ROW1, ENERGY_W = 11, ENERGY_H = ROW2 + 51 - ROW1;
	static final int LAMP_X = CONTROL_X, LAMP_Y = ROW3 + 2, LAMP_SIZE = 16;
	static final int PANEL_X = ContainerHFKiln.INV_X - 8, PANEL_Y = ContainerHFKiln.INV_Y - 8;

	// Our sprites in the sheet: {u, v, w, h}
	static final int[] LENS = {0, 330, 10, 10};
	// IE's pieces in refinery.png
	static final int[] IE_SLOT = {35, 13, 20, 20}, IE_TANK = {11, 18, 20, 51}, IE_BLUE = {42, 10, 6, 3}, IE_ORANGE = {42, 49, 6, 3};

	static final int[] FLUORITE = {0xa8, 0x78, 0xd8}, GYPSUM = {0xec, 0xea, 0xe4};
	static final int COL_ACID = 0xffd6c896, COL_GAS = 0xc8b4e6dc, COL_HF = 0xffc8e8e4, COL_GLOW = 0xff9a50, COL_TOOTH = 0xff64686e, COL_TOOTH_DARK = 0xff2a2c30;

	final ContainerHFKiln container;
	int ticks, turn;

	public GuiHFKiln(ContainerHFKiln container)
	{
		super(container);
		this.container = container;
		xSize = ContainerHFKiln.WIDTH;
		ySize = ContainerHFKiln.HEIGHT;
	}

	boolean running()
	{
		return container.state == TileHFKiln.STATE_RUNNING;
	}

	float done()
	{
		return container.progress / (float)TileHFKiln.CYCLE_TICKS;
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
			case TileHFKiln.STATE_RUNNING: return 0;
			case TileHFKiln.STATE_NO_INPUT:
			case TileHFKiln.STATE_DISABLED: return 1;
			case TileHFKiln.STATE_NO_POWER:
			case TileHFKiln.STATE_OUTPUT_FULL: return 2;
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
		int[][] xy = ContainerHFKiln.SLOT_XY;
		for(int i = 0; i < xy.length; i++)
		{
			piece(IE_SLOT, xy[i][0] - 2, xy[i][1] - 2);
			boolean in = i == TileHFKiln.SLOT_FLUORITE || i == TileHFKiln.SLOT_ACID_IN || i == TileHFKiln.SLOT_HF_IN;
			piece(in ? IE_BLUE : IE_ORANGE, xy[i][0] + 5, xy[i][1] - 5);
		}
		piece(IE_TANK, ACID_X, TANK_Y);
		piece(IE_BLUE, ACID_X + 7, TANK_Y - 3);
		piece(IE_TANK, HF_X, TANK_Y);
		piece(IE_ORANGE, HF_X + 7, TANK_Y - 3);
		GuiGraphite.endTint();
		GlStateManager.color(1, 1, 1, 1);
		ClientUtils.handleGuiTank(container.acid, guiLeft + ACID_X + 2, guiTop + TANK_Y + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);
		ClientUtils.handleGuiTank(container.hf, guiLeft + HF_X + 2, guiTop + TANK_Y + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);

		drawFlows();
		drawDrum();

		// Energy (IE's red gradient, inside IE's 7 px) and the lamp's lens
		int eh = (int)((ENERGY_H - 4) * (container.energy / (float)TileHFKiln.ENERGY_CAPACITY));
		if(eh > 0)
			ClientUtils.drawGradientRect(guiLeft + ENERGY_X + 2, guiTop + ENERGY_Y + ENERGY_H - 2 - eh, guiLeft + ENERGY_X + ENERGY_W - 2, guiTop + ENERGY_Y + ENERGY_H - 2, 0xffb51500, 0xff600b00);
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.enableBlend();
		ClientUtils.bindTexture(SHEET);
		Gui.drawModalRectWithCustomSizedTexture(guiLeft + LAMP_X + LAMP_SIZE / 2 - 5, guiTop + LAMP_Y + LAMP_SIZE / 2 - 5, LENS[0] + lamp() * LENS[2], LENS[1], LENS[2], LENS[3], SHEET_SIZE, SHEET_SIZE);
	}

	/** What moves in the pipes' cores while a cycle runs: the acid in, the gas up and over, the HF out. */
	void drawFlows()
	{
		if(!running())
			return;
		rect(32, 94, 8, 2, COL_ACID);
		rect(183, 25, 2, DISCH_Y - 25, COL_GAS);
		rect(183, 25, 31, 2, COL_GAS);
		rect(212, 25, 2, SCRUB_Y - 25, COL_GAS);
		rect(212, SCRUB_Y + SCRUB_H, 2, COND_Y - SCRUB_Y - SCRUB_H, COL_GAS);
		rect(212, COND_Y + COND_H, 2, 2, COL_HF);
		rect(212, 96, 22, 2, COL_HF);
		// bubbles in the scrubber's sight glass
		for(int k = 0; k < 3; k++)
			rect(SCRUB_X + 6 + k * 2, SCRUB_Y + 9 + Math.floorMod(9 - ticks / 2 - k * 3, 9), 1, 1, 0xffd8f0ec);
	}

	/** Whether x is in the drum's window where nothing stands in front of it (the coil, the rings, the girth gear). */
	static boolean bedShows(int x)
	{
		if(x >= COIL_X0 - 1 && x <= COIL_X1)
			return false;
		if(x >= GEAR_X - 1 && x <= GEAR_X + 6)
			return false;
		for(int r : RINGS)
			if(x >= r && x < r + 4)
				return false;
		return true;
	}

	/** The bed tumbling along the drum, purple fluorite turning into white gypsum; the coil glowing; the gear turning;
	 * the gypsum going down the chute. */
	void drawDrum()
	{
		boolean on = running();
		if(on || container.progress > 0)
			for(int x = WIN_X0; x < WIN_X1; x++)
			{
				if(!bedShows(x))
					continue;
				float t = (x - DRUM_X) / (float)DRUM_W;
				int r = Math.round(FLUORITE[0] + (GYPSUM[0] - FLUORITE[0]) * t), g = Math.round(FLUORITE[1] + (GYPSUM[1] - FLUORITE[1]) * t),
						b = Math.round(FLUORITE[2] + (GYPSUM[2] - FLUORITE[2]) * t);
				int wave = (int)Math.round(Math.sin(x * 0.6 - turn * 0.25));
				int top = WIN_Y1 - 4 + wave;
				rect(x, top, 1, WIN_Y1 - top, 0xff000000 | r << 16 | g << 8 | b);
			}
		if(on)
		{
			// The heating coil glows (the part of the machine that glows), breathing a little
			int a = Math.round(150 + 60 * (float)Math.sin(ticks * 0.15));
			for(int x = COIL_X0 + 1; x < COIL_X1 - 1; x += 2)
				rect(x, DRUM_Y + 2, 1, DRUM_H - 4, a << 24 | COL_GLOW);
			// The girth gear's teeth moving round
			int phase = (turn / 2) % 2;
			for(int y = DRUM_Y - 4; y < DRUM_Y + DRUM_H + 4; y++)
			{
				int c = (y + phase) % 2 == 0 ? COL_TOOTH : COL_TOOTH_DARK;
				rect(GEAR_X - 1, y, 1, 1, c);
				rect(GEAR_X + 6, y, 1, 1, c);
			}
			// Gypsum flakes going down the chute into the box
			for(int k = 0; k < 3; k++)
			{
				float f = Math.floorMod(ticks + k * 9, 27) / 27F;
				int x = Math.round(CHUTE_X0 + (CHUTE_X1 - CHUTE_X0) * f), y = Math.round(CHUTE_Y0 + 1 + (CHUTE_Y1 - CHUTE_Y0) * f);
				rect(x, y, 2, 2, 0xffeceae4);
			}
		}
	}

	boolean over(int mx, int my, int x, int y, int w, int h)
	{
		return mx >= guiLeft + x && mx < guiLeft + x + w && my >= guiTop + y && my < guiTop + y + h;
	}

	static final String K = "gui.immersivenuclear.hf_kiln.";

	@Override
	public void drawScreen(int mx, int my, float partialTicks)
	{
		super.drawScreen(mx, my, partialTicks);
		ArrayList<String> tip = new ArrayList<>();
		String[] tanks = {"acid", "hf"};
		int[] tx = {ACID_X, HF_X};
		FluidTank[] tt = {container.acid, container.hf};
		for(int i = 0; i < 2; i++)
		{
			ClientUtils.handleGuiTank(tt[i], guiLeft + tx[i] + 2, guiTop + TANK_Y + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, tip);
			if(over(mx, my, tx[i], TANK_Y, 20, 51))
				tip.add(0, I18n.format(K + tanks[i]));
		}
		String[] parts = {"feed", "drum", "motor", "discharge", "scrubber", "condenser"};
		int[][] boxes = {{FEED_X, FEED_Y, FEED_W, FEED_H}, {DRUM_X, DRUM_Y, DRUM_W, DRUM_H}, {MOTOR_X, MOTOR_Y, MOTOR_W, MOTOR_H},
				{DISCH_X, DISCH_Y, DISCH_W, DISCH_H}, {SCRUB_X, SCRUB_Y, SCRUB_W, SCRUB_H}, {COND_X, COND_Y, COND_W, COND_H}};
		for(int i = 0; i < boxes.length; i++)
			if(over(mx, my, boxes[i][0], boxes[i][1], boxes[i][2], boxes[i][3]))
			{
				tip.add(I18n.format(K + parts[i]));
				tip.add("§7" + I18n.format(K + parts[i] + "_info"));
				if(i == 1)
				{
					tip.add("§7" + I18n.format(K + "cycle", TileHFKiln.ACID_PER_CYCLE, TileHFKiln.HF_PER_CYCLE, TileHFKiln.GYPSUM_PER_CYCLE, TileHFKiln.CYCLE_TICKS / 20));
					if(container.progress > 0)
						tip.add("§7" + I18n.format(K + "progress", Math.round(100 * done())));
				}
				break;
			}
		String[] slotTips = {"fluorite", "gypsum", "acid_in", "acid_out", "hf_in", "hf_out"};
		int[][] xy = ContainerHFKiln.SLOT_XY;
		for(int i = 0; i < xy.length; i++)
			if(over(mx, my, xy[i][0], xy[i][1], 16, 16) && container.getSlot(i).getStack().isEmpty())
				tip.add(I18n.format(K + slotTips[i]));
		if(over(mx, my, ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H))
		{
			tip.add(container.energy + "/" + TileHFKiln.ENERGY_CAPACITY + " IF");
			tip.add("§7" + I18n.format(K + "energy", TileHFKiln.ENERGY_PER_TICK));
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
