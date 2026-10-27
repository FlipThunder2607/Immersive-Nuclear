package immersivenuclear.client;

import java.util.ArrayList;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.GuiIEContainerBase;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.block.TileIndustrialDryer;
import immersivenuclear.common.gui.ContainerIndustrialDryer;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.client.config.GuiUtils;

/**
 * Interface of the Industrial Dryer, in the mod's graphite finish drawn at run time ({@link GuiGraphite}: plate, wells,
 * arrow) with IE's own slots, tank and markers a little darker, and the same right column as the other machines
 * (energy, redstone button, lights). As the batch goes:
 *   wet concentrate > feed valve > CHAMBER (thermometer beside it, the batch's steps under it) > discharge > yellowcake
 *                                     \ vapour > CONDENSER > condensate tank | condensate containers
 * In the chamber: the bed growing while the valve lets the batch in; the heaters glowing as the temperature rises; the
 * paddles turning the bed over while it dries (from the wet concentrate's colour to the yellowcake's) and the vapour
 * rising to the dome and through the condenser into the tank; the bed going out through the discharge valve.
 */
public class GuiIndustrialDryer extends GuiIEContainerBase
{
	static final String IE = "immersiveengineering:textures/gui/refinery.png";
	static final String OWN = ImmersiveNuclear.MODID + ":textures/gui/chemical_processor.png";

	// Layout (as in the approved sketch, ferramentas/gui_secador.py)
	public static final int CHAMBER_X = 40, CHAMBER_Y = 34, CHAMBER_W = 108, CHAMBER_H = 58;
	static final int TEMP_X = 24, TEMP_Y = 34, TEMP_W = 10, TEMP_H = 58;
	static final int STEPS_X = 40, STEPS_Y = 98, STEPS_W = 108, STEPS_H = 8;
	static final int COND_X = 176, COND_Y = 14, COND_W = 30, COND_H = 50;
	static final int TANK_X = 218, TANK_Y = 38;
	static final int ARROW_X = 152, ARROW_Y = 84;
	/** The scrubber on the vapour line (its pipe runs through it at VAPOUR_Y), and its return down into the chamber. */
	static final int SCRUB_X = 140, SCRUB_Y = 14, SCRUB_W = 14, SCRUB_H = 17;
	/** The bed's full depth, over the heaters' strip at the bottom of the chamber. */
	static final int BED_DEPTH = 18, HEATER_H = 4, PADDLES = 9, PADDLE_REACH = 12;
	/** Pipes: vapour from the dome over the chamber to the condenser's top; condensate from its foot to the tank. */
	static final int DOME_X = CHAMBER_X + CHAMBER_W - 28, VAPOUR_Y = 24, DRAIN_X = COND_X + COND_W - 6, DRAIN_Y = TANK_Y + 44;
	static final int ENERGY_X = 248, ENERGY_Y = 16, ENERGY_W = 14, ENERGY_H = 64;
	static final int BUTTON_X = 247, BUTTON_Y = 86, BUTTON_SIZE = 16;
	static final int LIGHTS_X = 248, LIGHTS_W = 14, LIGHT_X = 251, LIGHT_Y = 112, LIGHT_STEP = 8;
	static final int PANEL_X = ContainerIndustrialDryer.INV_X - 8, PANEL_Y = ContainerIndustrialDryer.INV_Y - 8;

	// IE's pieces in refinery.png
	static final int[] IE_SLOT = {35, 13, 20, 20}, IE_TANK = {11, 18, 20, 51}, IE_BLUE = {42, 10, 6, 3}, IE_ORANGE = {42, 49, 6, 3};

	static final int COL_PIPE = 0xff2a2c30, COL_PIPE_EDGE = 0xff55585e, COL_STEEL = 0xff96999e, COL_STEEL_DARK = 0xff5c5f64,
			COL_INSIDE = 0xff24262a, COL_BAND = 0xff3a3d42, COL_VAPOUR = 0xb4d2d6dc, COL_CONDENSATE = 0xffb4c4d0;
	/** The bed's colour: wet concentrate when it goes in, yellowcake when it is dry. */
	static final int[] WET = {0x8a, 0x78, 0x34}, DRY = {0xe8, 0xc8, 0x40};
	static final int COL_WET = 0xff8a7834, COL_DRY = 0xffe8c840;
	/** The steps' colours on the strip: load, heat, dry, cool and discharge. */
	static final int[] STEP_COLOURS = {0x5a8ac8, 0xe07020, 0xe0c040, 0x809ab0};

	final ContainerIndustrialDryer container;
	int ticks;
	/** The paddles' turn in degrees and their speed (0..1), easing up and down; the condenser fan's turn. */
	float paddleAngle, paddleSpeed, fanAngle;
	static final float PADDLE_DEGREES_PER_TICK = 6;

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
		return (temperature() - TileIndustrialDryer.ROOM_TEMPERATURE) / (float)(TileIndustrialDryer.DRYING_TEMPERATURE - TileIndustrialDryer.ROOM_TEMPERATURE);
	}

	@Override
	public void updateScreen()
	{
		super.updateScreen();
		ticks++;
		float target = in(TileIndustrialDryer.PHASE_DRY) ? 1 : 0;
		paddleSpeed += Math.signum(target - paddleSpeed) * Math.min(Math.abs(target - paddleSpeed), 0.05F);
		paddleAngle = (paddleAngle + paddleSpeed * PADDLE_DEGREES_PER_TICK) % 360;
		if(in(TileIndustrialDryer.PHASE_DRY))
			fanAngle = (fanAngle + 30) % 360;
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

	/** A pipe of the background: 5 px, dark core with a light edge. */
	void pipe(int x0, int y0, int x1, int y1)
	{
		if(y0 == y1)
		{
			rect(Math.min(x0, x1) - 2, y0 - 2, Math.abs(x1 - x0) + 5, 5, COL_PIPE_EDGE);
			rect(Math.min(x0, x1) - 1, y0 - 1, Math.abs(x1 - x0) + 3, 3, COL_PIPE);
		}
		else
		{
			rect(x0 - 2, Math.min(y0, y1) - 2, 5, Math.abs(y1 - y0) + 5, COL_PIPE_EDGE);
			rect(x0 - 1, Math.min(y0, y1) - 1, 3, Math.abs(y1 - y0) + 3, COL_PIPE);
		}
	}

	/** Moving fluid inside a pipe (its 3 px core). */
	void flow(int x0, int y0, int x1, int y1, int argb)
	{
		if(y0 == y1)
			rect(Math.min(x0, x1) - 1, y0 - 1, Math.abs(x1 - x0) + 3, 3, argb);
		else
			rect(x0 - 1, Math.min(y0, y1) - 1, 3, Math.abs(y1 - y0) + 3, argb);
	}

	static int mix(int[] a, int[] b, float t)
	{
		t = Math.max(0, Math.min(1, t));
		return 0xff000000 | Math.round(a[0] + (b[0] - a[0]) * t) << 16 | Math.round(a[1] + (b[1] - a[1]) * t) << 8 | Math.round(a[2] + (b[2] - a[2]) * t);
	}

	/** 0 green running, 1 yellow waiting for input, 2 red stopped (power, full output), 3 grey disabled; -1 idle. */
	int litLight()
	{
		switch(container.state)
		{
			case TileIndustrialDryer.STATE_RUNNING: return 0;
			case TileIndustrialDryer.STATE_NO_INPUT: return 1;
			case TileIndustrialDryer.STATE_NO_POWER:
			case TileIndustrialDryer.STATE_OUTPUT_FULL: return 2;
			case TileIndustrialDryer.STATE_DISABLED: return 3;
			default: return -1;
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mx, int my)
	{
		// The graphite plate and its wells
		GuiGraphite.panel(guiLeft, guiTop, xSize, ySize);
		GuiGraphite.well(guiLeft + CHAMBER_X, guiTop + CHAMBER_Y, CHAMBER_W, CHAMBER_H);
		GuiGraphite.well(guiLeft + TEMP_X, guiTop + TEMP_Y, TEMP_W, TEMP_H);
		GuiGraphite.well(guiLeft + STEPS_X, guiTop + STEPS_Y, STEPS_W, STEPS_H);
		GuiGraphite.well(guiLeft + COND_X, guiTop + COND_Y, COND_W, COND_H);
		GuiGraphite.well(guiLeft + ENERGY_X, guiTop + ENERGY_Y, ENERGY_W, ENERGY_H);
		GuiGraphite.well(guiLeft + LIGHTS_X, guiTop + LIGHT_Y - 3, LIGHTS_W, 4 * LIGHT_STEP + 4);
		GuiGraphite.arrow(guiLeft + ARROW_X, guiTop + ARROW_Y, Math.round(GuiGraphite.ARROW_W * batchDone()));

		// Pipes: dome > condenser's top; condenser's foot > tank
		pipe(DOME_X, CHAMBER_Y - 1, DOME_X, VAPOUR_Y);
		pipe(DOME_X, VAPOUR_Y, COND_X - 1, VAPOUR_Y);
		pipe(DRAIN_X, COND_Y + COND_H, DRAIN_X, DRAIN_Y);
		pipe(DRAIN_X, DRAIN_Y, TANK_X - 1, DRAIN_Y);
		if(in(TileIndustrialDryer.PHASE_DRY))
		{
			flow(DOME_X, CHAMBER_Y - 1, DOME_X, VAPOUR_Y, COL_VAPOUR);
			flow(DOME_X, VAPOUR_Y, COND_X - 1, VAPOUR_Y, COL_VAPOUR);
			flow(DRAIN_X, COND_Y + COND_H, DRAIN_X, DRAIN_Y, COL_CONDENSATE);
			flow(DRAIN_X, DRAIN_Y, TANK_X - 1, DRAIN_Y, COL_CONDENSATE);
		}

		// The scrubber on the vapour line, over its return to the chamber
		pipe(SCRUB_X + SCRUB_W / 2, SCRUB_Y + SCRUB_H, SCRUB_X + SCRUB_W / 2, CHAMBER_Y - 1);
		GuiGraphite.well(guiLeft + SCRUB_X, guiTop + SCRUB_Y, SCRUB_W, SCRUB_H);
		drawScrubber();

		// IE's wooden inventory panel; IE's pieces, a little darker
		GlStateManager.color(1, 1, 1, 1);
		ClientUtils.bindTexture(IE);
		drawTexturedModalRect(guiLeft + PANEL_X, guiTop + PANEL_Y, 0, 77, 176, 89);
		GuiGraphite.ieTint();
		int[][] xy = ContainerIndustrialDryer.SLOT_XY;
		for(int i = 0; i < xy.length; i++)
		{
			piece(IE_SLOT, xy[i][0] - 2, xy[i][1] - 2);
			boolean input = i == TileIndustrialDryer.SLOT_WET || i == TileIndustrialDryer.SLOT_CONDENSATE_IN;
			piece(input ? IE_BLUE : IE_ORANGE, xy[i][0] + 5, xy[i][1] - 5);
		}
		piece(IE_TANK, TANK_X, TANK_Y);
		piece(IE_ORANGE, TANK_X + 7, TANK_Y - 3);
		GuiGraphite.endTint();
		ClientUtils.handleGuiTank(container.condensate, guiLeft + TANK_X + 2, guiTop + TANK_Y + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);

		drawFeed();
		drawChamber(partialTicks);
		drawThermometer();
		drawSteps();
		drawCondenser();

		// Energy (IE's red gradient), button, lights
		int eh = (int)((ENERGY_H - 4) * (container.energy / (float)TileIndustrialDryer.ENERGY_CAPACITY));
		if(eh > 0)
			ClientUtils.drawGradientRect(guiLeft + ENERGY_X + 2, guiTop + ENERGY_Y + ENERGY_H - 2 - eh, guiLeft + ENERGY_X + ENERGY_W - 2, guiTop + ENERGY_Y + ENERGY_H - 2, 0xffb51500, 0xff600b00);
		GlStateManager.color(1, 1, 1, 1);
		ClientUtils.bindTexture(OWN);
		drawTexturedModalRect(guiLeft + BUTTON_X, guiTop + BUTTON_Y, 0, 150, BUTTON_SIZE, BUTTON_SIZE);
		int lit = litLight();
		for(int i = 0; i < 4; i++)
			drawTexturedModalRect(guiLeft + LIGHT_X + 1, guiTop + LIGHT_Y + i * LIGHT_STEP, i * 8, i == lit ? 90 : 98, 6, 6);
	}

	/** The rotary valve under the wet concentrate's slot, letting the lumps fall into the chamber while it loads. */
	void drawFeed()
	{
		int[] wet = ContainerIndustrialDryer.SLOT_XY[TileIndustrialDryer.SLOT_WET];
		int x = wet[0] + 3, y = wet[1] + 18, h = CHAMBER_Y - y;
		rect(x, y, 10, h, COL_STEEL_DARK);
		rect(x + 2, y + 2, 6, h - 2, COL_PIPE);
		// The valve's rotor: a vane that turns while it loads
		boolean loading = in(TileIndustrialDryer.PHASE_LOAD);
		int vane = loading ? ticks / 2 % 4 : 0;
		rect(x + (vane == 1 ? 2 : vane == 3 ? 6 : 4), y + 1, 2, 2, COL_STEEL);
		if(loading)
			for(int k = 0; k < 3; k++)
				rect(x + 3 + k % 2 * 2, y + 2 + Math.floorMod(ticks * 2 + k * 3, Math.max(1, h - 3)), 2, 2, COL_WET);
	}

	void drawChamber(float partialTicks)
	{
		int ix = CHAMBER_X + 2, iy = CHAMBER_Y + 2, iw = CHAMBER_W - 4, ih = CHAMBER_H - 4;
		int floor = iy + ih - HEATER_H;
		rect(ix, iy, iw, ih, COL_INSIDE);
		for(int k = 1; k < 4; k++)
			rect(ix + k * iw / 4, iy, 1, ih, COL_BAND);

		// The heaters along the bottom, glowing with the temperature
		float glow = heat();
		for(int i = 0; i < iw; i += 3)
		{
			boolean coil = i / 3 % 2 == 0;
			int r = Math.round(60 + (coil ? 170 : 110) * glow), g = Math.round(40 + (coil ? 50 : 20) * glow), b = 30 - Math.round(15 * glow);
			rect(ix + i, floor, Math.min(3, iw - i), HEATER_H, 0xff000000 | r << 16 | g << 8 | b);
		}

		// The bed: as deep as the share of the batch inside; its surface heaves while the paddles turn it
		float share = bed();
		int depth = Math.round(BED_DEPTH * share);
		boolean drying = in(TileIndustrialDryer.PHASE_DRY);
		float dry = dryness();
		if(depth > 0)
			for(int i = 0; i < iw; i++)
			{
				int wave = Math.round((drying ? 2 : 1) * (float)Math.sin(i / 6.0 + (drying ? paddleAngle / 30.0 : 0)));
				// While unloading, the bed runs down towards the discharge (right) end
				int d = container.phase == TileIndustrialDryer.PHASE_DISCHARGE ? Math.round(depth * (0.4F + 0.6F * i / (float)iw)) : depth;
				int top = Math.max(iy + 4, floor - d + (d > 2 ? wave : 0));
				// The drive end dries a little ahead of the discharge end
				rect(ix + i, top, 1, floor - top, mix(WET, DRY, dry + 0.15F * (0.5F - i / (float)iw)));
				if((i * 5) % 11 == 0 && floor - top > 6)
					rect(ix + i, top + 3 + i % 4, 1, 1, 0x50000000);
			}

		// The paddle shaft along the drum and its paddles, seen from the side as they turn
		int shaftY = floor - BED_DEPTH / 2 - 1;
		float angle = paddleAngle + paddleSpeed * PADDLE_DEGREES_PER_TICK * partialTicks;
		for(int pass = 0; pass < 2; pass++)
			for(int k = 0; k < PADDLES; k++)
			{
				double a = Math.toRadians(angle + k * 90);
				boolean front = Math.sin(a) > 0;
				if(front != (pass == 1))
					continue;
				int px = ix + 6 + k * (iw - 12) / (PADDLES - 1);
				int tip = (int)Math.round(Math.cos(a) * PADDLE_REACH);
				int col = front ? COL_STEEL : COL_STEEL_DARK;
				rect(px - 1, Math.min(shaftY, shaftY + tip), 3, Math.abs(tip) + 1, col);
				rect(px - 3, shaftY + tip - (tip < 0 ? 1 : 0), 7, 2, col);
			}
		rect(ix, shaftY - 1, iw, 3, COL_STEEL);

		// Vapour rising off the bed to the dome while it dries
		if(drying)
		{
			int rise = Math.max(1, floor - BED_DEPTH - iy - 2);
			for(int k = 0; k < 14; k++)
			{
				int vx = ix + 6 + (k * 37) % (iw - 12);
				int vy = floor - BED_DEPTH - Math.floorMod(ticks + k * 7, rise);
				// The vapour drifts towards the dome as it rises
				vx += Math.round((DOME_X - vx) * (floor - BED_DEPTH - vy) / (float)rise * 0.5F);
				rect(vx, vy, 2, 1, 0x6ed2d6dc);
				rect(vx + 1, vy - 1, 1, 1, 0x46d2d6dc);
			}
		}

		// The dome over the drum, the drive at the left end, the discharge valve at the right end
		rect(DOME_X - 5, CHAMBER_Y - 3, 11, 3, COL_STEEL_DARK);
		rect(DOME_X - 4, CHAMBER_Y - 2, 9, 1, COL_STEEL);
		rect(CHAMBER_X - 4, CHAMBER_Y + CHAMBER_H / 2 - 4, 4, 10, COL_STEEL_DARK);
		int vx = CHAMBER_X + CHAMBER_W - 12, vy = CHAMBER_Y + CHAMBER_H;
		rect(vx, vy, 10, 4, COL_STEEL_DARK);
		if(in(TileIndustrialDryer.PHASE_DISCHARGE))
		{
			// The dry yellowcake running out of the valve towards the arrow
			rect(vx + 3, vy, 4, 4, COL_DRY);
			for(int k = 0; k < 4; k++)
				rect(vx + 10 + Math.floorMod(ticks + k * 4, ARROW_X - vx - 10), vy + 1 + k % 2, 2, 2, COL_DRY);
		}
	}

	/**
	 * The scrubber: a spray washes the fines out of the vapour while it dries; they settle in its sump, which fills a
	 * quarter a batch, and go back down into the chamber with the batch that brings it to a yellowcake's worth.
	 */
	void drawScrubber()
	{
		int x = SCRUB_X + 2, y = SCRUB_Y + 2, w = SCRUB_W - 4, h = SCRUB_H - 4;
		rect(x, y, w, h, COL_INSIDE);
		rect(x, y + 1, w, 1, COL_STEEL);
		int held = Math.min(container.fines, TileIndustrialDryer.FINES_EVERY);
		int sump = Math.round((h - 4) * held / (float)TileIndustrialDryer.FINES_EVERY);
		rect(x, y + h - sump, w, sump, COL_DRY);
		if(in(TileIndustrialDryer.PHASE_DRY))
			for(int k = 0; k < 4; k++)
			{
				int dy = y + 3 + Math.floorMod(ticks + k * 5, Math.max(1, h - 4 - sump));
				rect(x + 1 + k * 3 % Math.max(1, w - 1), dy, 1, 2, k % 2 == 0 ? COL_CONDENSATE : COL_DRY);
			}
		// The fines going back with the batch that completes them
		if(in(TileIndustrialDryer.PHASE_DISCHARGE) && container.fines >= TileIndustrialDryer.FINES_EVERY)
			flow(SCRUB_X + SCRUB_W / 2, SCRUB_Y + SCRUB_H, SCRUB_X + SCRUB_W / 2, CHAMBER_Y - 1, COL_DRY);
	}

	/** The chamber's thermometer: up while heating, held while drying, down while cooling. */
	void drawThermometer()
	{
		int inner = TEMP_H - 4, hot = Math.round(inner * heat());
		for(int j = 0; j < hot; j++)
		{
			float f = j / (float)Math.max(1, hot - 1);
			int r = Math.round(0xf0 - 0x50 * f), g = Math.round(0xa0 - 0x80 * f);
			rect(TEMP_X + 3, TEMP_Y + TEMP_H - 2 - hot + j, TEMP_W - 6, 1, 0xff000000 | r << 16 | g << 8 | 0x10);
		}
		for(int k = 0; k < 5; k++)
			rect(TEMP_X + 2, TEMP_Y + 2 + k * inner / 4, 2, 1, COL_STEEL);
	}

	/** The batch's four steps, as wide as their time; the ones done full, the current one filling. */
	void drawSteps()
	{
		int inner = STEPS_W - 4, total = 0;
		for(int p = 0; p < 4; p++)
			total += TileIndustrialDryer.phaseLength(p);
		int x = STEPS_X + 2, used = 0;
		boolean batch = container.phase != TileIndustrialDryer.PHASE_LOAD || container.phaseTicks > 0;
		for(int p = 0; p < 4; p++)
		{
			int w = p == 3 ? inner - used : Math.round(inner * TileIndustrialDryer.phaseLength(p) / (float)total);
			used += w;
			int c = STEP_COLOURS[p];
			rect(x, STEPS_Y + 2, w, STEPS_H - 4, 0xff000000 | (c >> 18 & 63) << 16 | (c >> 10 & 63) << 8 | (c >> 2 & 63));
			float done = !batch ? 0 : p < container.phase ? 1 : p == container.phase ? stepDone() : 0;
			rect(x, STEPS_Y + 2, Math.round(w * done), STEPS_H - 4, 0xff000000 | c);
			rect(x + w - 1, STEPS_Y + 2, 1, STEPS_H - 4, COL_PIPE);
			x += w;
		}
	}

	/** The condenser: radiator fins, the fan on top turning while it dries, drops forming on the fins. */
	void drawCondenser()
	{
		int x = COND_X + 2, y = COND_Y + 2, w = COND_W - 4, h = COND_H - 4;
		rect(x, y, w, h, COL_INSIDE);
		for(int j = COND_Y + 8; j < COND_Y + COND_H - 4; j += 3)
			rect(COND_X + 4, j, COND_W - 8, 1, COL_STEEL);
		// The fan seen edge-on: its blades sweep from full width to a sliver as it turns
		int span = Math.round(Math.abs((float)Math.cos(Math.toRadians(fanAngle))) * (COND_W - 10));
		rect(COND_X + COND_W / 2 - span / 2, COND_Y + 4, Math.max(2, span), 2, COL_STEEL_DARK);
		rect(COND_X + COND_W / 2 - 1, COND_Y + 3, 2, 4, COL_STEEL);
		if(in(TileIndustrialDryer.PHASE_DRY))
			for(int k = 0; k < 5; k++)
			{
				int dx = COND_X + 5 + (k * 7) % (COND_W - 10);
				int dy = COND_Y + 9 + Math.floorMod(ticks + k * 9, COND_H - 14);
				rect(dx, dy, 1, 2, COL_CONDENSATE);
			}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mx, int my)
	{
		// The icon sits inside the button's bevel, a quarter smaller than an item
		RenderHelper.enableGUIStandardItemLighting();
		ItemStack icon = new ItemStack(container.redstoneInverted ? Item.getItemFromBlock(Blocks.REDSTONE_TORCH) : Items.REDSTONE);
		GlStateManager.pushMatrix();
		GlStateManager.translate(BUTTON_X + 2, BUTTON_Y + 2, 0);
		GlStateManager.scale(0.75F, 0.75F, 1);
		itemRender.renderItemIntoGUI(icon, 0, 0);
		GlStateManager.popMatrix();
		RenderHelper.disableStandardItemLighting();
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
		ClientUtils.handleGuiTank(container.condensate, guiLeft + TANK_X + 2, guiTop + TANK_Y + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, tip);
		if(over(mx, my, TANK_X, TANK_Y, 20, 51))
			tip.add(0, I18n.format(K + "condensate"));
		boolean batch = container.phase != TileIndustrialDryer.PHASE_LOAD || container.phaseTicks > 0;
		if(over(mx, my, CHAMBER_X, CHAMBER_Y, CHAMBER_W, CHAMBER_H))
		{
			tip.add(I18n.format(K + "chamber"));
			if(batch)
			{
				tip.add("§7" + I18n.format(K + "phase." + container.phase));
				tip.add("§7" + I18n.format(K + "progress", Math.round(100 * stepDone())));
			}
			else
				tip.add("§7" + I18n.format(K + "empty", TileIndustrialDryer.WET_PER_BATCH));
		}
		if(over(mx, my, TEMP_X, TEMP_Y, TEMP_W, TEMP_H))
		{
			tip.add(I18n.format(K + "temperature", temperature()));
			tip.add("§7" + I18n.format(K + "temperature_info", TileIndustrialDryer.DRYING_TEMPERATURE));
		}
		if(over(mx, my, STEPS_X, STEPS_Y, STEPS_W, STEPS_H))
			for(int p = 0; p < 4; p++)
			{
				String line = I18n.format(K + "step." + p, TileIndustrialDryer.phaseLength(p) / 20);
				tip.add(batch && p == container.phase ? "§e> " + line : "§7" + line);
			}
		if(over(mx, my, SCRUB_X, SCRUB_Y, SCRUB_W, SCRUB_H))
		{
			tip.add(I18n.format(K + "scrubber"));
			tip.add("§7" + I18n.format(K + "scrubber_info", TileIndustrialDryer.FINES_EVERY));
			tip.add("§7" + I18n.format(K + "scrubber_fines", Math.min(container.fines, TileIndustrialDryer.FINES_EVERY), TileIndustrialDryer.FINES_EVERY));
		}
		if(over(mx, my, COND_X, COND_Y, COND_W, COND_H))
		{
			tip.add(I18n.format(K + "condenser"));
			tip.add("§7" + I18n.format(K + "condenser_info"));
		}
		if(over(mx, my, ARROW_X, ARROW_Y, GuiGraphite.ARROW_W, GuiGraphite.ARROW_H))
		{
			int seconds = 0;
			for(int p = 0; p < 4; p++)
				seconds += TileIndustrialDryer.phaseLength(p) / 20;
			tip.add(I18n.format(K + "batch", TileIndustrialDryer.WET_PER_BATCH, TileIndustrialDryer.YELLOWCAKE_PER_BATCH,
					TileIndustrialDryer.CONDENSATE_PER_BATCH, seconds));
			tip.add("§7" + I18n.format(K + "batch_fines", TileIndustrialDryer.FINES_EVERY));
			if(batch)
				tip.add("§7" + I18n.format(K + "progress", Math.round(100 * batchDone())));
		}
		int[] wet = ContainerIndustrialDryer.SLOT_XY[TileIndustrialDryer.SLOT_WET];
		if(over(mx, my, wet[0], wet[1], 16, 16) && container.getSlot(TileIndustrialDryer.SLOT_WET).getStack().isEmpty())
			tip.add(I18n.format(K + "wet", TileIndustrialDryer.WET_PER_BATCH));
		int[] out = ContainerIndustrialDryer.SLOT_XY[TileIndustrialDryer.SLOT_YELLOWCAKE];
		if(over(mx, my, out[0], out[1], 16, 16) && container.getSlot(TileIndustrialDryer.SLOT_YELLOWCAKE).getStack().isEmpty())
			tip.add(I18n.format(K + "yellowcake"));
		if(over(mx, my, ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H))
			tip.add(container.energy + "/" + TileIndustrialDryer.ENERGY_CAPACITY + " IF");
		if(over(mx, my, BUTTON_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE))
		{
			tip.add(I18n.format("gui.immersivenuclear.redstone"));
			tip.add("§7" + I18n.format(container.redstoneInverted ? "gui.immersivenuclear.redstone.inverted" : "gui.immersivenuclear.redstone.normal"));
		}
		for(int i = 0; i < 4; i++)
			if(over(mx, my, LIGHTS_X, LIGHT_Y + i * LIGHT_STEP - 1, LIGHTS_W, LIGHT_STEP))
			{
				// The lit light names the exact state (red covers two)
				boolean on = i == litLight();
				tip.add(on ? I18n.format(K + "state." + container.state) : "§7" + I18n.format(K + "light." + i));
			}
		if(!tip.isEmpty())
		{
			// Long lines wrap, and the tooltip stays on the screen
			GuiUtils.drawHoveringText(tip, mx, my, width, height, GuiGraphite.TOOLTIP_WIDTH, fontRenderer);
			RenderHelper.enableGUIStandardItemLighting();
		}
	}

	@Override
	protected void mouseClicked(int mx, int my, int button) throws java.io.IOException
	{
		super.mouseClicked(mx, my, button);
		if(button == 0 && over(mx, my, BUTTON_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE))
		{
			mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			mc.playerController.sendEnchantPacket(inventorySlots.windowId, ContainerIndustrialDryer.BUTTON_REDSTONE);
		}
	}
}
