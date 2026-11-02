package immersivenuclear.client;

import java.util.ArrayList;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.GuiIEContainerBase;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.block.TileMetalExtractor;
import immersivenuclear.common.gui.ContainerMetalExtractor;
import immersivenuclear.common.item.ItemElectrode;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.client.config.GuiUtils;

/**
 * Interface of the Metal Extractor, in the interface standard (01/11): separate riveted panels with the machine drawn
 * between them, the world showing through the gaps.
 *   [effluent buckets, at the vat's inlet]  THE VAT (the machine's only input tank) with its six plates hanging from the
 *   travelling beam between its guides; the HV cabinet (ammeter) with its wire to the beam; the tray's chute into the
 *   deposit box; under the vat the plate strip, one slot under each plate  [depleted: in / tank / out | energy, lamp]
 * The effluent rises and falls in the vat; the metal grows on the plates while the current runs (bubbles rising); to
 * collect, the beam lifts the plates out, the tray slides in under them, the scraper bar sweeps down them and the
 * flakes fall into the tray, which slides back and tips them down the chute. No step icons (the user's choice): the
 * vat's tooltip names the step. Our sheet (textures/gui/metal_extractor_gui.png, made by
 * ferramentas/gui_extratora_v3_esboco.py --jogo with the same numbers) is the fixed background; the rest is drawn here.
 * The JEI page keeps its own sheet (metal_extractor.png).
 */
public class GuiMetalExtractor extends GuiIEContainerBase
{
	static final String IE = "immersiveengineering:textures/gui/refinery.png";
	static final String SHEET = ImmersiveNuclear.MODID + ":textures/gui/metal_extractor_gui.png";
	static final int SHEET_SIZE = 512;

	// Layout (same as gui_extratora_v3_esboco.py)
	static final int ROW1 = 37, ROW2 = 62, ROW3 = 117;
	/** The vat; also JEI's click area (INJeiPlugin). */
	public static final int VAT_X = 52, VAT_Y = 50, VAT_W = 126, VAT_H = 56;
	/** The vat's window (the liquid and the plates), its inside. */
	static final int IX0 = VAT_X + 6, IX1 = VAT_X + VAT_W - 6, IY0 = VAT_Y + 6, IY1 = VAT_Y + VAT_H - 4;
	static final int GUIDE_L = VAT_X - 4, GUIDE_R = VAT_X + VAT_W, GUIDE_W = 4;
	/** The beam, the hangers from it to the plates; down, the plates hang in the liquid, up they clear the tray. */
	static final int BEAM_H = 4, HANGER = 14, PLATE_W = 4, PLATE_H = 30, PLATES = 6, BEAM_DOWN = 42, BEAM_UP = 1, TRAY_Y = 50;
	static final int PIPE_Y = 86, INLET_X0 = 36, INLET_X1 = VAT_X - 5, OUTLET_X0 = VAT_X + VAT_W + 6, OUTLET_X1 = 232;
	static final int TANK_X = 240, CONTROL_X = 266;
	static final int CABINET_X = 186, CABINET_Y = 34, CABINET_W = 40, CABINET_H = 40, DIAL_CX = 198, DIAL_CY = 48, DIAL_R = 5;
	/** The chute from the vat's spout down to the funnel on the deposit box's lid: its top end and its low end. */
	static final int CHUTE_X0 = VAT_X + VAT_W + 4, CHUTE_Y0 = VAT_Y + VAT_H - 9, CHUTE_X1 = 204, CHUTE_Y1 = 107;
	static final int ENERGY_X = CONTROL_X + 2, ENERGY_Y = ROW1, ENERGY_W = 11, ENERGY_H = ROW2 + 51 - ROW1;
	static final int LAMP_X = CONTROL_X, LAMP_Y = ROW3 + 2, LAMP_SIZE = 16;
	static final int PANEL_X = ContainerMetalExtractor.INV_X - 8, PANEL_Y = ContainerMetalExtractor.INV_Y - 8;
	/** Left x of each plate, each over its slot; the beam's five connectors. */
	static final int[] PLATE_XS = new int[PLATES], CONNECTORS = new int[5];

	static
	{
		for(int k = 0; k < PLATES; k++)
			PLATE_XS[k] = ContainerMetalExtractor.PLATES_X + 8 + k * ContainerMetalExtractor.PLATES_STEP - PLATE_W / 2;
		for(int k = 0; k < 5; k++)
			CONNECTORS[k] = Math.round(PLATE_XS[0] + 6 + k * (PLATE_XS[PLATES - 1] - PLATE_XS[0] - 8) / 4F);
	}

	// Our sprites in the sheet: {u, v, w, h}
	static final int[] LENS = {0, 330, 10, 10};
	// IE's pieces in refinery.png
	static final int[] IE_SLOT = {35, 13, 20, 20}, IE_TANK = {11, 18, 20, 51}, IE_BLUE = {42, 10, 6, 3}, IE_ORANGE = {42, 49, 6, 3};

	static final int COL_PLATE = 0xff7c7f84, COL_PLATE_LIGHT = 0xffa0a3a8, COL_PLATE_DARK = 0xff585a5e, COL_HANGER = 0xff96999e;
	static final int COL_METAL = 0xff80684f, COL_METAL_LIGHT = 0xff927a60, COL_METAL_DARK = 0xff6e5640;
	static final int COL_BEAM = 0xff606268, COL_BEAM_LIGHT = 0xff8c8f94, COL_BEAM_DARK = 0xff34363a, COL_HAZARD = 0xffc8a028, COL_HAZARD_DARK = 0xff282828;
	static final int COL_INSULATOR = 0xff96503c, COL_INSULATOR_LIGHT = 0xffbe6e50, COL_TIP = 0xffb0b2b6;
	static final int COL_TRAY = 0xff3c3e42, COL_TRAY_RIM = 0xff82858a, COL_SCRAPER = 0xffc8a028, COL_SCRAPER_DARK = 0xff5a4614;
	static final int COL_WIRE = 0xff464648, COL_WIRE_LIGHT = 0xff606062, COL_NEEDLE = 0xffb01e14, COL_BUBBLE = 0xccd6dad2;
	static final int COL_EFFLUENT = 0xff706a58, COL_DEPLETED = 0xff968c70;

	final ContainerMetalExtractor container;
	/** Shown current, 0..1: eases towards the real one, so the needle rises and falls smoothly. */
	float shownCurrent;
	/** The step's tick count kept on the client (smooth), set from the server's on a new step or a drift. */
	int shownPhase = -1, shownTicks;
	/** The cleaning's tick count kept on the client the same way (-1 with the platform down). */
	int shownCollect = -1;
	/** Ticks since the interface opened, for the bubbles. */
	int clock;

	public GuiMetalExtractor(ContainerMetalExtractor container)
	{
		super(container);
		this.container = container;
		xSize = ContainerMetalExtractor.WIDTH;
		ySize = ContainerMetalExtractor.HEIGHT;
	}

	boolean running()
	{
		return container.state == TileMetalExtractor.STATE_RUNNING;
	}

	float targetCurrent()
	{
		if(!running())
			return 0;
		if(container.phase == TileMetalExtractor.PHASE_PREPARE)
			return 0.85F * Math.min(1, container.phaseTicks / (float)TileMetalExtractor.PREPARE_TICKS);
		return container.phase == TileMetalExtractor.PHASE_RECOVER ? 0.85F : 0;
	}

	@Override
	public void updateScreen()
	{
		super.updateScreen();
		clock++;
		float target = targetCurrent();
		// A little flicker while the current runs, like a real ammeter
		if(target > 0.5F)
			target += (float)Math.sin(clock * 0.7F) * 0.01F;
		shownCurrent += (target - shownCurrent) * 0.15F;
		if(container.phase != shownPhase || Math.abs(container.phaseTicks - shownTicks) > 4)
		{
			shownPhase = container.phase;
			shownTicks = container.phaseTicks;
		}
		else if(running() && shownTicks < TileMetalExtractor.phaseLength(shownPhase))
			shownTicks++;
		if(container.collect < 0 || shownCollect < 0 || Math.abs(container.collect - shownCollect) > 4)
			shownCollect = container.collect;
		else if(container.collecting && shownCollect < TileMetalExtractor.TRAY_OUT)
			shownCollect++;
	}

	float ticks(float partialTicks)
	{
		return (shownPhase == container.phase ? shownTicks : container.phaseTicks) + (running() ? partialTicks : 0);
	}

	/** The cleaning's ticks, smooth; -1 with the platform down. */
	float collect(float partialTicks)
	{
		return shownCollect < 0 ? -1 : shownCollect + (container.collecting ? partialTicks : 0);
	}

	boolean cleaning()
	{
		return container.collect >= 0 && container.collect < TileMetalExtractor.TRAY_OUT;
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

	/** A one pixel line, point by point. */
	void line(float x0, float y0, float x1, float y1, int argb)
	{
		int n = (int)Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)));
		for(int k = 0; k <= n; k++)
		{
			float t = n == 0 ? 0 : k / (float)n;
			int x = Math.round(x0 + (x1 - x0) * t), y = Math.round(y0 + (y1 - y0) * t);
			rect(x, y, 1, 1, argb);
		}
	}

	/** Part of a fluid's still sprite (tiled from the top left); binds the blocks atlas. */
	void fluid(Fluid f, int x, int y, int w, int h)
	{
		if(f == null || w <= 0 || h <= 0)
			return;
		mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		TextureAtlasSprite sp = mc.getTextureMapBlocks().getAtlasSprite(f.getStill().toString());
		int c = f.getColor();
		GlStateManager.color((c >> 16 & 255) / 255F, (c >> 8 & 255) / 255F, (c & 255) / 255F, 1);
		Tessellator t = Tessellator.getInstance();
		BufferBuilder b = t.getBuffer();
		b.begin(7, DefaultVertexFormats.POSITION_TEX);
		for(int j = 0; j < h; j += 16)
			for(int i = 0; i < w; i += 16)
			{
				int tw = Math.min(16, w - i), th = Math.min(16, h - j);
				float u0 = sp.getMinU(), u1 = sp.getInterpolatedU(tw), v0 = sp.getMinV(), v1 = sp.getInterpolatedV(th);
				double px = guiLeft + x + i, py = guiTop + y + j;
				b.pos(px, py + th, zLevel).tex(u0, v1).endVertex();
				b.pos(px + tw, py + th, zLevel).tex(u1, v1).endVertex();
				b.pos(px + tw, py, zLevel).tex(u1, v0).endVertex();
				b.pos(px, py, zLevel).tex(u0, v0).endVertex();
			}
		t.draw();
		GlStateManager.color(1, 1, 1, 1);
	}

	/** The lamp's colour: 0 green running, 1 amber waiting (effluent, plates, or stopped by redstone), 2 red stopped (no
	 * power, output full), 3 off (idle). */
	int lamp()
	{
		switch(container.state)
		{
			case TileMetalExtractor.STATE_RUNNING: return 0;
			case TileMetalExtractor.STATE_NO_INPUT:
			case TileMetalExtractor.STATE_NO_PLATES:
			case TileMetalExtractor.STATE_DISABLED: return 1;
			case TileMetalExtractor.STATE_NO_POWER:
			case TileMetalExtractor.STATE_OUTPUT_FULL: return 2;
			default: return 3;
		}
	}

	/** An ARGB colour multiplied by an (r, g, b) tint. */
	static int tinted(int argb, float[] c)
	{
		int r = Math.min(255, Math.round((argb >> 16 & 255) * c[0])), g = Math.min(255, Math.round((argb >> 8 & 255) * c[1])), b = Math.min(255, Math.round((argb & 255) * c[2]));
		return argb & 0xff000000 | r << 16 | g << 8 | b;
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
		int[][] xy = ContainerMetalExtractor.SLOT_XY;
		for(int i = 0; i < xy.length; i++)
		{
			piece(IE_SLOT, xy[i][0] - 2, xy[i][1] - 2);
			// The plate slots hold the machine's own parts: no port mark
			if(i < TileMetalExtractor.SLOT_PLATE0)
				piece(i == 1 || i == 3 ? IE_BLUE : IE_ORANGE, xy[i][0] + 5, xy[i][1] - 5);
		}
		piece(IE_TANK, TANK_X, ROW2);
		piece(IE_ORANGE, TANK_X + 7, ROW2 - 3);
		GuiGraphite.endTint();

		// Liquid moving in the pipes: the effluent coming in while the vat fills, the depleted effluent while it drains
		if(running() && container.phase == TileMetalExtractor.PHASE_FILL)
			rect(INLET_X0, PIPE_Y, INLET_X1 - INLET_X0, 2, COL_EFFLUENT);
		if(running() && container.phase == TileMetalExtractor.PHASE_DRAIN)
			rect(OUTLET_X0, PIPE_Y, OUTLET_X1 - OUTLET_X0, 2, COL_DEPLETED);
		GlStateManager.color(1, 1, 1, 1);
		ClientUtils.handleGuiTank(container.depleted, guiLeft + TANK_X + 2, guiTop + ROW2 + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);

		drawVat(partialTicks);
		drawCabinet();

		// Energy (IE's red gradient, inside IE's 7 px) and the lamp's lens
		int eh = (int)((ENERGY_H - 4) * (container.energy / (float)TileMetalExtractor.ENERGY_CAPACITY));
		if(eh > 0)
			ClientUtils.drawGradientRect(guiLeft + ENERGY_X + 2, guiTop + ENERGY_Y + ENERGY_H - 2 - eh, guiLeft + ENERGY_X + ENERGY_W - 2, guiTop + ENERGY_Y + ENERGY_H - 2, 0xffb51500, 0xff600b00);
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.enableBlend();
		ClientUtils.bindTexture(SHEET);
		Gui.drawModalRectWithCustomSizedTexture(guiLeft + LAMP_X + LAMP_SIZE / 2 - 5, guiTop + LAMP_Y + LAMP_SIZE / 2 - 5, LENS[0] + lamp() * LENS[2], LENS[1], LENS[2], LENS[3], SHEET_SIZE, SHEET_SIZE);
	}

	int beamY(float partialTicks)
	{
		return Math.round(BEAM_DOWN + (BEAM_UP - BEAM_DOWN) * TileMetalExtractor.lift(container.phase, ticks(partialTicks), collect(partialTicks)));
	}

	/** The liquid, the plates with their metal, the bubbles, the beam and its hangers, the tray, the scraper, the flakes. */
	void drawVat(float partialTicks)
	{
		int phase = container.phase;
		float t = ticks(partialTicks), cl = collect(partialTicks);
		int amount = container.vat.getFluidAmount();
		int top = Math.round(IY1 - (IY1 - (IY0 + 2)) * Math.min(1, amount / (float)TileMetalExtractor.BATCH));
		if(amount > 0)
		{
			FluidStack f = container.vat.getFluid();
			fluid(f.getFluid(), IX0, top, IX1 - IX0, IY1 - top);
			rect(IX0, top, IX1 - IX0, 1, 0x5abebaa0);
		}
		int beamY = beamY(partialTicks), ptop = beamY + BEAM_H + HANGER;
		float coat = TileMetalExtractor.coat(phase, t, cl), scraper = TileMetalExtractor.scraper(cl);
		// The metal: on both faces, under the effluent (the top 6 px stay out of it), cleaned above the scraper
		int thick = coat < 0.02F ? 0 : coat < 0.5F ? 1 : 2;
		int start = ptop + 6;
		if(scraper >= 0)
			start = Math.max(start, ptop + Math.round(PLATE_H * scraper));
		for(int k = 0; k < PLATES; k++)
		{
			// Only the plates in their slots, in their material's colour; each on its hanger from the beam
			ItemElectrode e = ItemElectrode.of(container.getSlot(TileMetalExtractor.SLOT_PLATE0 + k).getStack());
			if(e == null)
				continue;
			int x0 = PLATE_XS[k];
			rect(x0 + 1, beamY + BEAM_H, 2, HANGER, COL_HANGER);
			rect(x0, ptop, PLATE_W, PLATE_H, tinted(COL_PLATE, e.colour));
			rect(x0, ptop, 1, PLATE_H, tinted(COL_PLATE_LIGHT, e.colour));
			rect(x0 + PLATE_W - 1, ptop, 1, PLATE_H, tinted(COL_PLATE_DARK, e.colour));
			for(int y = start; y < ptop + PLATE_H && thick > 0; y++)
			{
				int c = (y * 7 + x0 * 3) % 5 == 0 ? COL_METAL_LIGHT : (y * 5 + x0) % 7 == 0 ? COL_METAL_DARK : COL_METAL;
				rect(x0 - thick, y, thick, 1, c);
				rect(x0 + PLATE_W, y, thick, 1, c);
			}
		}
		// Bubbles rising between the plates while the current runs
		if(running() && (phase == TileMetalExtractor.PHASE_PREPARE || phase == TileMetalExtractor.PHASE_RECOVER) && amount > 0)
		{
			int room = IY1 - 3 - (top + 3);
			for(int k = 0; k < PLATES - 1 && room > 4; k++)
				for(int b = 0; b < 3; b++)
				{
					int seed = k * 37 + b * 13;
					int by = IY1 - 3 - Math.floorMod((int)((clock + partialTicks) * 0.6F) + seed * 5, room);
					int bx = PLATE_XS[k] + PLATE_W + 3 + Math.floorMod(seed + by / 6, 5);
					rect(bx, by, 1, 1, COL_BUBBLE);
				}
		}
		// The beam across the guides, hazard ends, five connectors
		int bx0 = GUIDE_L, bx1 = GUIDE_R + GUIDE_W;
		rect(bx0, beamY, bx1 - bx0, BEAM_H, COL_BEAM);
		rect(bx0, beamY, bx1 - bx0, 1, COL_BEAM_LIGHT);
		rect(bx0, beamY + BEAM_H - 1, bx1 - bx0, 1, COL_BEAM_DARK);
		for(int i = 0; i < 4; i++)
			for(int k = 0; k < BEAM_H; k++)
			{
				int c = (i + k) % 4 < 2 ? COL_HAZARD : COL_HAZARD_DARK;
				rect(bx0 + i, beamY + k, 1, 1, c);
				rect(bx1 - 1 - i, beamY + k, 1, 1, c);
			}
		for(int c : CONNECTORS)
		{
			rect(c - 1, beamY - 3, 3, 3, COL_INSULATOR);
			rect(c - 1, beamY - 3, 3, 1, COL_INSULATOR_LIGHT);
			rect(c, beamY - 4, 1, 1, COL_TIP);
		}
		// The tray slides in from the chute's side (right) under the raised plates
		float tray = TileMetalExtractor.tray(cl);
		if(tray > 0)
		{
			int w = Math.round((IX1 - IX0) * tray), x = IX1 - w;
			rect(x, TRAY_Y, w, 4, COL_TRAY);
			rect(x, TRAY_Y + 3, w, 1, COL_TRAY_RIM);
			rect(x, TRAY_Y, 1, 4, COL_TRAY_RIM);
			rect(IX1 - 1, TRAY_Y, 1, 4, COL_TRAY_RIM);
			// The flakes heap up as the scraper goes down
			float heap = scraper >= 0 ? scraper : cl >= TileMetalExtractor.CLEANED ? 1 : 0;
			for(int i = x + 2; i < IX1 - 2; i++)
			{
				if(((i * 31) % 97) / 97F > heap)
					continue;
				rect(i, TRAY_Y + 2, 1, 1, (i % 3 == 0) ? COL_METAL_LIGHT : COL_METAL);
				if(i % 2 == 0)
					rect(i, TRAY_Y + 1, 1, 1, (i % 5 == 0) ? COL_METAL_DARK : COL_METAL);
			}
		}
		// The cleaner goes down the plates scraping them, then back up
		if(cl >= TileMetalExtractor.TRAY_IN && cl < TileMetalExtractor.SCRAPED)
		{
			int cy = ptop + Math.round(PLATE_H * TileMetalExtractor.cleaner(cl));
			rect(GUIDE_L + GUIDE_W, cy - 1, GUIDE_R - GUIDE_L - GUIDE_W, 2, COL_SCRAPER);
			rect(GUIDE_L + GUIDE_W, cy + 1, GUIDE_R - GUIDE_L - GUIDE_W, 1, COL_SCRAPER_DARK);
		}
		if(scraper >= 0)
		{
			int sy = ptop + Math.round(PLATE_H * scraper);
			// Flakes falling from the bar into the tray
			for(int x0 : PLATE_XS)
				for(int k = 0; k < 2; k++)
				{
					int span = TRAY_Y - 2 - (sy + 3);
					if(span <= 0)
						continue;
					int fy = sy + 3 + Math.floorMod((int)((clock + partialTicks) * 1.5F) + x0 * 7 + k * 11, span);
					rect(x0 - 2 + Math.floorMod(x0 + k * 5, PLATE_W + 4), fy, 1, 1, COL_METAL);
				}
		}
		// As the tray slides back, the flakes go out of the spout and down the chute into the deposit box
		if(cl >= TileMetalExtractor.SCRAPED && cl < TileMetalExtractor.TRAY_OUT)
		{
			float f = (cl - TileMetalExtractor.SCRAPED) / (TileMetalExtractor.TRAY_OUT - TileMetalExtractor.SCRAPED);
			for(int k = 0; k < 3; k++)
			{
				float g = f * 2 - k * .3F;
				if(g > 0 && g < 1)
				{
					// down the chute's floor, then into the funnel
					int x = Math.round(CHUTE_X0 + (CHUTE_X1 - CHUTE_X0) * g), y = Math.round(CHUTE_Y0 + (CHUTE_Y1 - CHUTE_Y0) * g);
					rect(x, y, 2, 2, COL_METAL);
				}
			}
		}
	}

	/** The ammeter's needle and the HV wire from the cabinet's insulator to the beam's last connector. */
	void drawCabinet()
	{
		double a = Math.toRadians(135 + 270 * shownCurrent);
		line(DIAL_CX, DIAL_CY, DIAL_CX + (float)Math.cos(a) * DIAL_R, DIAL_CY + (float)Math.sin(a) * DIAL_R, COL_NEEDLE);
		rect(DIAL_CX, DIAL_CY, 1, 1, 0xff282828);
		float lift = TileMetalExtractor.lift(container.phase, ticks(0), collect(0));
		int ax = CABINET_X + 9, ay = CABINET_Y - 4, bx = CONNECTORS[4], by = beamY(0) - 4;
		float sag = 6 + 4 * lift;
		float px = ax, py = ay;
		for(int k = 1; k <= 24; k++)
		{
			float f = k / 24F;
			float x = ax + (bx - ax) * f, y = ay + (by - ay) * f + sag * 4 * f * (1 - f);
			line(px, py, x, y, COL_WIRE_LIGHT);
			line(px, py + 1, x, y + 1, COL_WIRE);
			px = x;
			py = y;
		}
	}

	boolean over(int mx, int my, int x, int y, int w, int h)
	{
		return mx >= guiLeft + x && mx < guiLeft + x + w && my >= guiTop + y && my < guiTop + y + h;
	}

	static final String K = "gui.immersivenuclear.metal_extractor.";

	@Override
	public void drawScreen(int mx, int my, float partialTicks)
	{
		super.drawScreen(mx, my, partialTicks);
		ArrayList<String> tip = new ArrayList<>();
		ClientUtils.handleGuiTank(container.depleted, guiLeft + TANK_X + 2, guiTop + ROW2 + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, tip);
		if(over(mx, my, TANK_X, ROW2, 20, 51))
			tip.add(0, I18n.format(K + "depleted"));
		if(over(mx, my, VAT_X, VAT_Y, VAT_W, VAT_H))
		{
			FluidStack f = container.vat.getFluid();
			tip.add(I18n.format(K + "vat"));
			tip.add("§7" + (f != null ? f.getLocalizedName() + ": " : "") + container.vat.getFluidAmount() + " / " + TileMetalExtractor.BATCH + " mB");
			tip.add("§7" + I18n.format(K + "phase." + container.phase));
			float coat = TileMetalExtractor.coat(container.phase, container.phaseTicks, container.collect);
			if(coat > 0)
				tip.add("§7" + I18n.format(K + "plates", Math.round(100 * coat)));
			if(cleaning())
				tip.add("§7" + I18n.format(K + "cleaning"));
			else if(container.collect >= TileMetalExtractor.TRAY_OUT && container.phase == TileMetalExtractor.PHASE_FILL)
				tip.add("§7" + I18n.format(K + "waiting_full"));
		}
		if(over(mx, my, CABINET_X, CABINET_Y - 4, CABINET_W, CABINET_H + 4))
		{
			tip.add(I18n.format(K + "cabinet"));
			tip.add("§7" + I18n.format(K + "current", Math.round(100 * shownCurrent / 0.85F)));
		}
		int px = ContainerMetalExtractor.PLATES_X - 1, py = ContainerMetalExtractor.PLATES_Y - 1;
		if(over(mx, my, px, py, ContainerMetalExtractor.PLATES_STEP * PLATES, 18) && getSlotUnderMouse() != null && !getSlotUnderMouse().getHasStack())
		{
			tip.add(I18n.format(K + "plate_slots"));
			tip.add("§7" + I18n.format("jei.immersivenuclear.metal_extractor.electrodes"));
		}
		if(over(mx, my, ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H))
			tip.add(container.energy + "/" + TileMetalExtractor.ENERGY_CAPACITY + " IF");
		if(over(mx, my, LAMP_X, LAMP_Y, LAMP_SIZE, LAMP_SIZE))
		{
			tip.add(container.state == TileMetalExtractor.STATE_RUNNING ? I18n.format(K + "phase." + container.phase) : I18n.format(K + "state." + container.state));
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
