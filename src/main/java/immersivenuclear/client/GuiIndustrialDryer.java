package immersivenuclear.client;

import java.util.ArrayList;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.GuiIEContainerBase;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.block.TileConcentrateDrum;
import immersivenuclear.common.block.TileIndustrialDryer;
import immersivenuclear.common.gui.ContainerIndustrialDryer;
import immersivenuclear.common.item.ItemConcentrateDrum;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.config.GuiUtils;

/**
 * Interface of the Industrial Dryer, in the interface standard (01/11): separate riveted panels with the machine drawn
 * between them, the world showing through the gaps.
 *   the wet concentrate's slot over its funnel and rotary valve; the DRYING CHAMBER on its saddles (the bed, the paddle
 *   shaft and the glowing heaters in its window, the thermometer at its left end, the green drive motor); the dome's
 *   vapour duct through the scrubber to the finned CONDENSER, its condensate into the tank; the discharge valve into
 *   the yellowcake's box, its chute down into the drum being filled; the drum conveyor along the bottom
 *   [condensate: in / tank / out | energy over the status lamp]
 * Our sheet (textures/gui/industrial_dryer_gui.png, made by ferramentas/gui_secador_v2_esboco.py --jogo with the same
 * numbers) is the fixed background; what moves is drawn here. The JEI page keeps its own sheet.
 */
public class GuiIndustrialDryer extends GuiIEContainerBase
{
	static final String IE = "immersiveengineering:textures/gui/refinery.png";
	static final String SHEET = ImmersiveNuclear.MODID + ":textures/gui/industrial_dryer_gui.png";
	static final int SHEET_SIZE = 512;

	// Layout (same as gui_secador_v2_esboco.py)
	static final int ROW1 = 37, ROW2 = 62, ROW3 = 117;
	/** The drying chamber's shell; also JEI's click area (INJeiPlugin). */
	public static final int CHAMBER_X = 26, CHAMBER_Y = 46, CHAMBER_W = 114, CHAMBER_H = 48;
	/** Its window: the bed, the paddles, the heaters along its floor. */
	static final int WIN_X = 42, WIN_Y = 52, WIN_W = 88, WIN_H = 36, HEAT_Y = 85, SHAFT_Y = 70, BED_DEPTH = 12;
	static final int PADDLE_X0 = 48, PADDLE_STEP = 12, PADDLES = 7, PADDLE_REACH = 9;
	static final int TEMP_X = 32, TEMP_Y = 50, TEMP_W = 6, TEMP_H = 40;
	static final int FEED_X = 54;
	/** The vapour's way: up from the dome, across (through the scrubber), down into the condenser. */
	static final int DOME_CORE_X = 95, DUCT_CORE_Y = 19, COND_CORE_X = 195;
	static final int SCRUB_X = 120, SCRUB_Y = 12, SCRUB_W = 16, SCRUB_H = 16;
	static final int COND_X = 188, COND_Y = 30, COND_W = 16, COND_H = 72, DRAIN_CORE_Y = 107;
	static final int YC_BOX_X = 146, YC_BOX_Y = 64, YC_BOX_W = 34, YC_BOX_H = 34, DISCHARGE_Y = 79, CHUTE_X = 159;
	static final int BELT_X0 = 22, BELT_X1 = 214, BELT_Y = 139;
	static final int TANK_X = 220, CONTROL_X = 246;
	static final int ENERGY_X = CONTROL_X + 2, ENERGY_Y = ROW1, ENERGY_W = 11, ENERGY_H = ROW2 + 51 - ROW1;
	static final int LAMP_X = CONTROL_X, LAMP_Y = ROW3 + 2, LAMP_SIZE = 16;
	static final int PANEL_X = ContainerIndustrialDryer.INV_X - 8, PANEL_Y = ContainerIndustrialDryer.INV_Y - 8;

	// Our sprites in the sheet: {u, v, w, h}
	static final int[] LENS = {0, 330, 10, 10};
	// IE's pieces in refinery.png
	static final int[] IE_SLOT = {35, 13, 20, 20}, IE_TANK = {11, 18, 20, 51}, IE_BLUE = {42, 10, 6, 3}, IE_ORANGE = {42, 49, 6, 3};

	static final int COL_STEEL = 0xffa8acb0, COL_STEEL_DARK = 0xff5c5f64, COL_SHAFT = 0xff96999e, COL_SHAFT_LIGHT = 0xffc8ccd0,
			COL_VAPOUR = 0xb4d2d6dc, COL_CONDENSATE = 0xffb4c4d0, COL_HEAT = 0xff8a2a, COL_THERMO = 0xffd04a1e, COL_SLAT = 0xff2a2c30;
	/** The bed's colour: wet concentrate when it goes in, yellowcake when it is dry. */
	static final int[] WET = {0x8a, 0x78, 0x34}, DRY = {0xe8, 0xc8, 0x40};
	static final int COL_DRY = 0xffe8c840, COL_WET = 0xff8a7834;
	static final float PADDLE_DEGREES_PER_TICK = 6;

	final ContainerIndustrialDryer container;
	int ticks, belt;
	/** The paddles' turn in degrees and their speed (0..1), easing up and down. */
	float paddleAngle, paddleSpeed;

	public GuiIndustrialDryer(ContainerIndustrialDryer container)
	{
		super(container);
		this.container = container;
		xSize = ContainerIndustrialDryer.WIDTH;
		ySize = ContainerIndustrialDryer.HEIGHT;
	}

	boolean running()
	{
		return container.state == TileIndustrialDryer.STATE_RUNNING;
	}

	boolean in(int phase)
	{
		return running() && container.phase == phase;
	}

	/** How far the current step has gone, 0..1. */
	float stepDone()
	{
		return Math.min(1, container.phaseTicks / (float)TileIndustrialDryer.phaseLength(container.phase));
	}

	/** How far the whole batch has gone, 0..1 (0 while no batch is in). */
	float batchDone()
	{
		int total = 0, before = 0;
		for(int p = 0; p < 4; p++)
		{
			total += TileIndustrialDryer.phaseLength(p);
			if(p < container.phase)
				before += TileIndustrialDryer.phaseLength(p);
		}
		return (before + container.phaseTicks) / (float)total;
	}

	/** How much of the batch is in the chamber, 0..1. */
	float bed()
	{
		switch(container.phase)
		{
			case TileIndustrialDryer.PHASE_LOAD: return stepDone();
			case TileIndustrialDryer.PHASE_DISCHARGE: return 1 - stepDone();
			default: return 1;
		}
	}

	/** How dry the bed is, 0..1. */
	float dryness()
	{
		switch(container.phase)
		{
			case TileIndustrialDryer.PHASE_DRY: return stepDone();
			case TileIndustrialDryer.PHASE_DISCHARGE: return 1;
			default: return 0;
		}
	}

	int temperature()
	{
		return TileIndustrialDryer.temperature(container.phase, container.phaseTicks);
	}

	/** 0 room temperature .. 1 drying temperature. */
	float heat()
	{
		return Math.max(0, Math.min(1, (temperature() - TileIndustrialDryer.ROOM_TEMPERATURE) / (float)(TileIndustrialDryer.DRYING_TEMPERATURE - TileIndustrialDryer.ROOM_TEMPERATURE)));
	}

	boolean has(int slot)
	{
		return !container.getSlot(slot).getStack().isEmpty();
	}

	/** Yellowcake in the drum being filled. */
	int drumFill()
	{
		return ItemConcentrateDrum.contents(container.getSlot(TileIndustrialDryer.SLOT_DRUM_FILLING).getStack());
	}

	@Override
	public void updateScreen()
	{
		super.updateScreen();
		ticks++;
		float target = in(TileIndustrialDryer.PHASE_DRY) ? 1 : 0;
		paddleSpeed += Math.signum(target - paddleSpeed) * Math.min(Math.abs(target - paddleSpeed), 0.05F);
		paddleAngle = (paddleAngle + paddleSpeed * PADDLE_DEGREES_PER_TICK) % 360;
		// The conveyor creeps while a drum waits to come under the chute
		if(has(TileIndustrialDryer.SLOT_DRUM_EMPTY) && !has(TileIndustrialDryer.SLOT_DRUM_FILLING) || filling())
			belt++;
	}

	/** Yellowcake going down the chute into the drum. */
	boolean filling()
	{
		return has(TileIndustrialDryer.SLOT_DRUM_FILLING) && has(TileIndustrialDryer.SLOT_YELLOWCAKE) && drumFill() < TileConcentrateDrum.CAPACITY;
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

	static int mix(int[] a, int[] b, float t, int alpha)
	{
		int r = Math.round(a[0] + (b[0] - a[0]) * t), g = Math.round(a[1] + (b[1] - a[1]) * t), bl = Math.round(a[2] + (b[2] - a[2]) * t);
		return alpha << 24 | r << 16 | g << 8 | bl;
	}

	/** The lamp's colour: 0 green running, 1 amber waiting (input, or stopped by redstone), 2 red stopped (no power,
	 * output full), 3 off (idle). */
	int lamp()
	{
		switch(container.state)
		{
			case TileIndustrialDryer.STATE_RUNNING: return 0;
			case TileIndustrialDryer.STATE_NO_INPUT:
			case TileIndustrialDryer.STATE_DISABLED: return 1;
			case TileIndustrialDryer.STATE_NO_POWER:
			case TileIndustrialDryer.STATE_OUTPUT_FULL: return 2;
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
		int[][] xy = ContainerIndustrialDryer.SLOT_XY;
		for(int i = 0; i < xy.length; i++)
		{
			piece(IE_SLOT, xy[i][0] - 2, xy[i][1] - 2);
			if(i == TileIndustrialDryer.SLOT_DRUM_FILLING)
				continue;
			boolean input = i == TileIndustrialDryer.SLOT_WET || i == TileIndustrialDryer.SLOT_CONDENSATE_IN || i == TileIndustrialDryer.SLOT_DRUM_EMPTY;
			piece(input ? IE_BLUE : IE_ORANGE, xy[i][0] + 5, xy[i][1] - 5);
		}
		piece(IE_TANK, TANK_X, ROW2);
		piece(IE_ORANGE, TANK_X + 7, ROW2 - 3);
		GuiGraphite.endTint();
		GlStateManager.color(1, 1, 1, 1);
		ClientUtils.handleGuiTank(container.condensate, guiLeft + TANK_X + 2, guiTop + ROW2 + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);

		drawFlows();
		drawChamber(partialTicks);
		drawDrums();

		// Energy (IE's red gradient, inside IE's 7 px) and the lamp's lens
		int eh = (int)((ENERGY_H - 4) * (container.energy / (float)TileIndustrialDryer.ENERGY_CAPACITY));
		if(eh > 0)
			ClientUtils.drawGradientRect(guiLeft + ENERGY_X + 2, guiTop + ENERGY_Y + ENERGY_H - 2 - eh, guiLeft + ENERGY_X + ENERGY_W - 2, guiTop + ENERGY_Y + ENERGY_H - 2, 0xffb51500, 0xff600b00);
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.enableBlend();
		ClientUtils.bindTexture(SHEET);
		Gui.drawModalRectWithCustomSizedTexture(guiLeft + LAMP_X + LAMP_SIZE / 2 - 5, guiTop + LAMP_Y + LAMP_SIZE / 2 - 5, LENS[0] + lamp() * LENS[2], LENS[1], LENS[2], LENS[3], SHEET_SIZE, SHEET_SIZE);
	}

	/** Vapour in the duct, condensate to the tank, fines in the scrubber's glass, yellowcake through the valve and chute. */
	void drawFlows()
	{
		if(in(TileIndustrialDryer.PHASE_DRY))
		{
			rect(DOME_CORE_X, DUCT_CORE_Y, 2, CHAMBER_Y - 6 - DUCT_CORE_Y, COL_VAPOUR);
			rect(DOME_CORE_X, DUCT_CORE_Y, COND_CORE_X + 2 - DOME_CORE_X, 2, COL_VAPOUR);
			rect(COND_CORE_X, DUCT_CORE_Y, 2, COND_Y - DUCT_CORE_Y, COL_VAPOUR);
			rect(COND_CORE_X, COND_Y + COND_H, 2, DRAIN_CORE_Y - COND_Y - COND_H, COL_CONDENSATE);
			rect(COND_CORE_X, DRAIN_CORE_Y, TANK_X - 4 - COND_CORE_X, 2, COL_CONDENSATE);
		}
		// The fines caught in the scrubber's sight glass
		int fines = Math.round(5 * Math.min(1, container.fines / (float)TileIndustrialDryer.FINES_EVERY));
		if(fines > 0)
			rect(SCRUB_X + 4, SCRUB_Y + 10 - fines, SCRUB_W - 8, fines, COL_DRY);
		if(in(TileIndustrialDryer.PHASE_DISCHARGE))
			rect(CHAMBER_X + CHAMBER_W, DISCHARGE_Y, YC_BOX_X - CHAMBER_X - CHAMBER_W, 2, COL_DRY);
		if(filling())
			for(int k = 0; k < 3; k++)
				rect(CHUTE_X + 2 + k * 2, YC_BOX_Y + YC_BOX_H + Math.floorMod(ticks * 2 + k * 5, ROW3 - 2 - YC_BOX_Y - YC_BOX_H), 2, 2, COL_DRY);
	}

	/** The bed, the paddles turning it over, the heaters glowing, the vapour rising, the thermometer, the feed. */
	void drawChamber(float partialTicks)
	{
		// The heaters glow with the temperature (the part of the machine that glows)
		float h = heat();
		if(h > 0)
		{
			int a = Math.round(255 * h) << 24;
			for(int x = WIN_X; x < WIN_X + WIN_W; x++)
				if(x % 4 == 1 || x % 4 == 2)
					rect(x, HEAT_Y + 1, 1, 1, a | COL_HEAT);
			rect(WIN_X, HEAT_Y - 1, WIN_W, 1, Math.round(70 * h) << 24 | COL_HEAT);
		}
		// The bed: its depth as the batch goes in and out, its colour from wet to dry, its top stirred by the paddles
		float angle = paddleAngle + paddleSpeed * PADDLE_DEGREES_PER_TICK * partialTicks;
		int depth = Math.round(BED_DEPTH * bed());
		if(depth > 0)
		{
			int colour = mix(WET, DRY, dryness(), 255);
			for(int x = WIN_X; x < WIN_X + WIN_W; x++)
			{
				int top = HEAT_Y - depth + (depth > 3 ? Math.round(1.5F * (float)Math.sin(x * 0.5 + Math.toRadians(angle) * 2)) : 0);
				rect(x, top, 1, HEAT_Y - top, colour);
			}
		}
		// The shaft and its paddles turning (seen from the side: each arm swings up and down)
		rect(WIN_X, SHAFT_Y - 1, WIN_W, 3, COL_SHAFT);
		rect(WIN_X, SHAFT_Y - 1, WIN_W, 1, COL_SHAFT_LIGHT);
		for(int k = 0; k < PADDLES; k++)
		{
			double a = Math.toRadians(angle + k * 51.4);
			int tip = (int)Math.round(Math.sin(a) * PADDLE_REACH);
			boolean front = Math.cos(a) > 0;
			int x = PADDLE_X0 + k * PADDLE_STEP;
			int colour = front ? COL_STEEL : COL_STEEL_DARK;
			rect(x, Math.min(SHAFT_Y, SHAFT_Y + tip), 2, Math.abs(tip) + 1, colour);
			rect(x - 2, SHAFT_Y + tip + (tip < 0 ? -1 : 0), 6, 2, colour);
		}
		// The vapour rising off the bed while it dries
		if(in(TileIndustrialDryer.PHASE_DRY))
			for(int k = 0; k < 8; k++)
			{
				int span = Math.max(1, HEAT_Y - depth - WIN_Y - 2);
				int y = HEAT_Y - depth - 2 - Math.floorMod(ticks / 2 + k * 7, span);
				int x = WIN_X + 5 + Math.floorMod(k * 23 + (ticks / 6) * (k % 2 == 0 ? 1 : -1), WIN_W - 10);
				rect(x, y, 1, 1, 0x8ce8f0f4);
			}
		// The wet concentrate falling in from the feed valve while it loads
		if(in(TileIndustrialDryer.PHASE_LOAD))
			for(int k = 0; k < 3; k++)
				rect(FEED_X - 1 + k, WIN_Y + Math.floorMod(ticks * 2 + k * 7, Math.max(1, HEAT_Y - depth - WIN_Y)), 1, 2, COL_WET);
		// The thermometer's column
		int th = Math.round((TEMP_H - 6) * (0.08F + 0.92F * h));
		rect(TEMP_X + 2, TEMP_Y + TEMP_H - 2 - th, 2, th, COL_THERMO);
	}

	/** The conveyor's slats moving and the drum's level under the chute. */
	void drawDrums()
	{
		for(int x = BELT_X0 + 6 + belt % 5; x < BELT_X1 - 4; x += 5)
			rect(x, BELT_Y - 1, 1, 1, COL_SLAT);
		if(has(TileIndustrialDryer.SLOT_DRUM_FILLING))
		{
			int[] st = ContainerIndustrialDryer.SLOT_XY[TileIndustrialDryer.SLOT_DRUM_FILLING];
			rect(st[0], st[1] + 17, Math.round(16 * drumFill() / (float)TileConcentrateDrum.CAPACITY), 1, COL_DRY);
		}
	}

	boolean over(int mx, int my, int x, int y, int w, int h)
	{
		return mx >= guiLeft + x && mx < guiLeft + x + w && my >= guiTop + y && my < guiTop + y + h;
	}

	static final String K = "gui.immersivenuclear.industrial_dryer.";

	@Override
	public void drawScreen(int mx, int my, float partialTicks)
	{
		super.drawScreen(mx, my, partialTicks);
		ArrayList<String> tip = new ArrayList<>();
		ClientUtils.handleGuiTank(container.condensate, guiLeft + TANK_X + 2, guiTop + ROW2 + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, tip);
		if(over(mx, my, TANK_X, ROW2, 20, 51))
			tip.add(0, I18n.format(K + "condensate"));
		boolean batch = container.phase != TileIndustrialDryer.PHASE_LOAD || container.phaseTicks > 0;
		if(over(mx, my, TEMP_X, TEMP_Y, TEMP_W, TEMP_H))
		{
			tip.add(I18n.format(K + "temperature", temperature()));
			tip.add("§7" + I18n.format(K + "temperature_info", TileIndustrialDryer.DRYING_TEMPERATURE));
		}
		else if(over(mx, my, CHAMBER_X, CHAMBER_Y, CHAMBER_W, CHAMBER_H))
		{
			tip.add(I18n.format(K + "chamber"));
			if(batch)
			{
				tip.add("§7" + I18n.format(K + "phase." + container.phase) + " - " + I18n.format(K + "progress", Math.round(100 * stepDone())));
				for(int p = 0; p < 4; p++)
				{
					String line = I18n.format(K + "step." + p, TileIndustrialDryer.phaseLength(p) / 20);
					tip.add(p == container.phase ? "§e> " + line : "§7" + line);
				}
			}
			else
				tip.add("§7" + I18n.format(K + "empty", TileIndustrialDryer.WET_PER_BATCH));
			int seconds = 0;
			for(int p = 0; p < 4; p++)
				seconds += TileIndustrialDryer.phaseLength(p) / 20;
			tip.add("§7" + I18n.format(K + "batch", TileIndustrialDryer.WET_PER_BATCH, TileIndustrialDryer.YELLOWCAKE_PER_BATCH,
					TileIndustrialDryer.CONDENSATE_PER_BATCH, seconds));
			if(batch)
				tip.add("§7" + I18n.format(K + "progress", Math.round(100 * batchDone())));
		}
		if(over(mx, my, SCRUB_X, SCRUB_Y, SCRUB_W, SCRUB_H))
		{
			tip.add(I18n.format(K + "scrubber"));
			tip.add("§7" + I18n.format(K + "scrubber_info", TileIndustrialDryer.FINES_EVERY));
			tip.add("§7" + I18n.format(K + "scrubber_fines", Math.min(container.fines, TileIndustrialDryer.FINES_EVERY), TileIndustrialDryer.FINES_EVERY));
		}
		if(over(mx, my, COND_X - 2, COND_Y, COND_W + 4, COND_H))
		{
			tip.add(I18n.format(K + "condenser"));
			tip.add("§7" + I18n.format(K + "condenser_info"));
		}
		int[] wet = ContainerIndustrialDryer.SLOT_XY[TileIndustrialDryer.SLOT_WET];
		if(over(mx, my, wet[0], wet[1], 16, 16) && !has(TileIndustrialDryer.SLOT_WET))
			tip.add(I18n.format(K + "wet", TileIndustrialDryer.WET_PER_BATCH));
		int[] out = ContainerIndustrialDryer.SLOT_XY[TileIndustrialDryer.SLOT_YELLOWCAKE];
		if(over(mx, my, out[0], out[1], 16, 16) && !has(TileIndustrialDryer.SLOT_YELLOWCAKE))
			tip.add(I18n.format(K + "yellowcake"));
		int[] empty = ContainerIndustrialDryer.SLOT_XY[TileIndustrialDryer.SLOT_DRUM_EMPTY];
		if(over(mx, my, empty[0], empty[1], 16, 16) && !has(TileIndustrialDryer.SLOT_DRUM_EMPTY))
			tip.add(I18n.format(K + "drum_empty"));
		int[] st = ContainerIndustrialDryer.SLOT_XY[TileIndustrialDryer.SLOT_DRUM_FILLING];
		if(over(mx, my, BELT_X0, BELT_Y - 1, BELT_X1 - BELT_X0, 6) || over(mx, my, st[0], st[1], 16, 16) && !has(TileIndustrialDryer.SLOT_DRUM_FILLING))
		{
			tip.add(I18n.format(K + "drum_station"));
			tip.add("§7" + I18n.format(K + "drum_station_info", TileConcentrateDrum.CAPACITY));
			if(has(TileIndustrialDryer.SLOT_DRUM_FILLING))
				tip.add("§e" + I18n.format(K + "drum_fill", drumFill(), TileConcentrateDrum.CAPACITY));
		}
		int[] full = ContainerIndustrialDryer.SLOT_XY[TileIndustrialDryer.SLOT_DRUM_FULL];
		if(over(mx, my, full[0], full[1], 16, 16) && !has(TileIndustrialDryer.SLOT_DRUM_FULL))
			tip.add(I18n.format(K + "drum_full"));
		if(over(mx, my, ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H))
			tip.add(container.energy + "/" + TileIndustrialDryer.ENERGY_CAPACITY + " IF");
		if(over(mx, my, LAMP_X, LAMP_Y, LAMP_SIZE, LAMP_SIZE))
		{
			tip.add(container.state == TileIndustrialDryer.STATE_RUNNING ? I18n.format(K + "phase." + container.phase) : I18n.format(K + "state." + container.state));
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
