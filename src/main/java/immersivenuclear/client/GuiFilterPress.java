package immersivenuclear.client;

import java.util.ArrayList;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.GuiIEContainerBase;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.block.TileFilterPress;
import immersivenuclear.common.gui.ContainerFilterPress;
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
 * Interface of the Filtration Unit, in the interface standard (the user's drawing, 01/11): separate riveted panels
 * with the machine drawn between them, the world showing through the gaps.
 *   [slurry: in / tank / out]  the centrifugal feed pump (its impeller turning in the eye, the blue motor) and the
 *   tall pressure vessel with its dial and level glass, on the steel base; the filter press: the actuators close the
 *   two heads in on the twelve plates, the chambers fill and darken with the cake, the press opens and the cake drops
 *   on the belt, which carries it into the residue box under the right actuator  [solution: in / tank / out | energy
 *   over the status lamp]
 * Under it the 28 filters in their own panel beside IE's inventory panel. Our sheet (textures/gui/filter_press_gui.png,
 * made by ferramentas/gui_filtro_v4_esboco.py --jogo with the same numbers) holds the background and the sprites; IE's
 * slots, tanks and marks come from IE's own sheet. The JEI page keeps its own sheet (filter_press.png).
 */
public class GuiFilterPress extends GuiIEContainerBase
{
	static final String IE = "immersiveengineering:textures/gui/refinery.png";
	static final String SHEET = ImmersiveNuclear.MODID + ":textures/gui/filter_press_gui.png";
	static final int SHEET_SIZE = 512;

	// Layout (same as gui_filtro_v4_esboco.py)
	static final int ROW1 = 37, ROW2 = 62, ROW3 = 117, BOT = 144;
	static final int STORAGE_X = 8, SOLUTION_X = 272, CONTROL_X = 298;
	/** The pump with its motor, and the press: also JEI's click areas for the recipes (INJeiPlugin). */
	public static final int PUMP_X = 40, PUMP_Y = 112, PUMP_W = 46, PUMP_H = 24;
	public static final int PRESS_X = 134, PRESS_Y = 64, PRESS_W = 97, PRESS_H = 64;
	static final int EYE_X = 49, EYE_Y = 121;
	static final int VESSEL_X = 88, VESSEL_Y = 52, VESSEL_W = 20, VESSEL_H = 84;
	static final int GLASS_X = 91, GLASS_Y = 86, GLASS_W = 3, GLASS_H = 24;
	static final int DIAL_CX = 98, DIAL_CY = 67, DIAL_R = 4;
	/** The actuators push each head in by HEAD_STROKE: open, the plates hang apart; closed, they are pressed together. */
	static final int HEAD_W = 6, PLATE_W = 6, PLATES = 10, HEAD_STROKE = 8, ROD_Y = 86;
	static final int BELT_X0 = 144, BELT_X1 = 227, BELT_Y = 134, BIN_X = 230;
	static final int ENERGY_X = CONTROL_X + 2, ENERGY_Y = ROW1, ENERGY_W = 11, ENERGY_H = ROW2 + 51 - ROW1;
	static final int LAMP_X = CONTROL_X, LAMP_Y = ROW3 + 2, LAMP_SIZE = 16;
	static final int PANEL_X = ContainerFilterPress.INV_X - 8, PANEL_Y = ContainerFilterPress.INV_Y - 8;

	// Our sprites in the sheet: {u, v, w, h}
	static final int[] HEAD = {0, 260, HEAD_W, PRESS_H}, PLATE = {8, 260, PLATE_W, PRESS_H - 4}, VANE = {16, 260, 6, 6}, LUMP = {48, 260, 5, 3},
			LENS = {0, 330, 10, 10};
	// IE's pieces in refinery.png
	static final int[] IE_SLOT = {35, 13, 20, 20}, IE_TANK = {11, 18, 20, 51}, IE_BLUE = {42, 10, 6, 3}, IE_ORANGE = {42, 49, 6, 3};

	static final int COL_SLURRY = 0xff968060, COL_SOLUTION = 0xffc86e32, COL_CAKE = 0xff6e5a3c, COL_NEEDLE = 0xffb01e14, COL_ROD = 0xffb4b8bc,
			COL_CLEAT = 0xffa85016;

	final ContainerFilterPress container;
	/** Shown pressure, 0..1: eases towards the real one, so the needle rises and falls smoothly. */
	float shownPressure;
	/** The impeller's turn, in eighths, while it pumps; the belt's run, in pixels, while it carries the cake. */
	int vane, belt;
	/** The vessel's level as shown, in mB: a new batch flows in over REFILL_TICKS instead of appearing at once. */
	float shownLevel;
	static final int REFILL_TICKS = 40;
	/** The press step's tick count kept on the client (smooth), set from the server's on a new step or a drift. */
	int shownPhase = -1, shownTicks;

	public GuiFilterPress(ContainerFilterPress container)
	{
		super(container);
		this.container = container;
		xSize = ContainerFilterPress.WIDTH;
		ySize = ContainerFilterPress.HEIGHT;
	}

	/** The vessel taking its new batch (shown): the slurry runs from the pump into it. */
	boolean refilling()
	{
		return container.pressure.getFluidAmount() - shownLevel > 1;
	}

	boolean pumping()
	{
		return container.pressure.getFluidAmount() > 0 && container.pump < TileFilterPress.PRESSURIZE_TICKS
				&& container.state != TileFilterPress.STATE_DISABLED && container.state != TileFilterPress.STATE_NO_POWER;
	}

	/** The pressurised slurry going into the plates: the first ticks of the filtering. */
	boolean releasing()
	{
		return container.phase == TileFilterPress.PHASE_FILTERING && container.phaseTicks < TileFilterPress.RELEASE_TICKS;
	}

	boolean filtering()
	{
		return container.phase == TileFilterPress.PHASE_FILTERING && container.state == TileFilterPress.STATE_RUNNING;
	}

	/** The cake on its way: the press has opened and the belt runs. */
	boolean discharging()
	{
		return container.phase == TileFilterPress.PHASE_OPENING && container.phaseTicks >= TileFilterPress.OPEN_MOVE_TICKS
				&& container.state == TileFilterPress.STATE_RUNNING;
	}

	/** The pump's pressure on what is left in the vessel: it falls as the release empties it. */
	float targetPressure()
	{
		int amount = container.pressure.getFluidAmount();
		return amount > 0 ? container.pump / (float)TileFilterPress.PRESSURIZE_TICKS * Math.min(1, amount / (float)TileFilterPress.BATCH) : 0;
	}

	@Override
	public void updateScreen()
	{
		super.updateScreen();
		shownPressure += (targetPressure() - shownPressure) * 0.25F;
		if(Math.abs(targetPressure() - shownPressure) < 0.002F)
			shownPressure = targetPressure();
		float level = container.pressure.getFluidAmount();
		if(level > shownLevel)
			shownLevel = Math.min(level, shownLevel + TileFilterPress.BATCH / (float)REFILL_TICKS);
		else
			shownLevel += (level - shownLevel) * 0.25F;
		if(pumping() || refilling())
			vane++;
		if(discharging())
			belt++;
		if(container.phase != shownPhase || Math.abs(container.phaseTicks - shownTicks) > 4)
		{
			shownPhase = container.phase;
			shownTicks = container.phaseTicks;
		}
		else if(container.state == TileFilterPress.STATE_RUNNING)
			shownTicks++;
	}

	// ---------- drawing helpers ----------
	void sprite(int[] s, int du, int x, int y)
	{
		Gui.drawModalRectWithCustomSizedTexture(guiLeft + x, guiTop + y, s[0] + du, s[1], s[2], s[3], SHEET_SIZE, SHEET_SIZE);
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

	/** A pipe's moving fluid: its 2 px core, from (x0, y0) to (x1, y1) (one axis). */
	void flow(int x0, int y0, int x1, int y1, int argb)
	{
		if(y0 == y1)
			rect(Math.min(x0, x1), y0, Math.abs(x1 - x0) + 1, 2, argb);
		else
			rect(x0, Math.min(y0, y1), 2, Math.abs(y1 - y0) + 1, argb);
	}

	/** A one pixel line, point by point (the needle). */
	void line(float x0, float y0, float x1, float y1, int argb)
	{
		int n = (int)Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)));
		for(int k = 0; k <= n; k++)
		{
			float t = n == 0 ? 0 : k / (float)n;
			int x = Math.round(x0 + (x1 - x0) * t), y = Math.round(y0 + (y1 - y0) * t);
			drawRect(guiLeft + x, guiTop + y, guiLeft + x + 1, guiTop + y + 1, argb);
		}
	}

	/** Part of a fluid's still sprite (tiled from the top left), with an alpha; binds the blocks atlas. */
	void fluid(Fluid f, int x, int y, int w, int h, float alpha)
	{
		if(f == null || w <= 0 || h <= 0)
			return;
		mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		TextureAtlasSprite sp = mc.getTextureMapBlocks().getAtlasSprite(f.getStill().toString());
		int c = f.getColor();
		GlStateManager.enableBlend();
		GlStateManager.color((c >> 16 & 255) / 255F, (c >> 8 & 255) / 255F, (c & 255) / 255F, alpha);
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

	/** Draws at a fractional GUI x (the screen has 2 to 4 pixels per GUI pixel, so a moving part glides). */
	void at(float x, Runnable draw)
	{
		GlStateManager.pushMatrix();
		GlStateManager.translate(x - (int)Math.floor(x), 0, 0);
		draw.run();
		GlStateManager.popMatrix();
	}

	/** The lamp's colour: 0 green running, 1 amber waiting (no slurry, or stopped by redstone), 2 red stopped (no power,
	 * output full, no filters), 3 off (idle). */
	int lamp()
	{
		switch(container.state)
		{
			case TileFilterPress.STATE_RUNNING: return 0;
			case TileFilterPress.STATE_NO_INPUT:
			case TileFilterPress.STATE_DISABLED: return 1;
			case TileFilterPress.STATE_NO_POWER:
			case TileFilterPress.STATE_OUTPUT_FULL:
			case TileFilterPress.STATE_NO_FILTERS: return 2;
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

		// IE's wooden inventory panel; IE's slots, tanks and port marks, a little darker
		ClientUtils.bindTexture(IE);
		drawTexturedModalRect(guiLeft + PANEL_X, guiTop + PANEL_Y, 0, 77, 176, 89);
		GuiGraphite.ieTint();
		int[][] xy = ContainerFilterPress.SLOT_XY;
		for(int i = 0; i < xy.length; i++)
		{
			piece(IE_SLOT, xy[i][0] - 2, xy[i][1] - 2);
			piece(i == 1 || i == 3 ? IE_BLUE : IE_ORANGE, xy[i][0] + 5, xy[i][1] - 5);
		}
		for(int i = 0; i < TileFilterPress.FILTERS; i++)
			piece(IE_SLOT, ContainerFilterPress.FILTERS_X + i % ContainerFilterPress.FILTER_COLUMNS * 18 - 2, ContainerFilterPress.FILTERS_Y + i / ContainerFilterPress.FILTER_COLUMNS * 18 - 2);
		piece(IE_TANK, STORAGE_X, ROW2);
		piece(IE_BLUE, STORAGE_X + 7, ROW2 - 3);
		piece(IE_TANK, SOLUTION_X, ROW2);
		piece(IE_ORANGE, SOLUTION_X + 7, ROW2 - 3);
		GuiGraphite.endTint();

		// Fluid moving in the pipes (their cores)
		// the heads' place now, for the pipes that end in them
		float closing = closing(partialTicks), left = headLeft(closing), right = headRight(closing);
		if(pumping() || refilling())
		{
			flow(36, 86, 40, 86, COL_SLURRY);
			flow(39, 86, 39, 124, COL_SLURRY);
			flow(39, 123, 43, 123, COL_SLURRY);
			flow(51, 98, 51, 115, COL_SLURRY);
			flow(51, 97, 90, 97, COL_SLURRY);
		}
		// only where the pipes show: from the vessel to the follower head, from the fixed head to the solution panel's edge
		if(releasing())
			at(left, () -> flow(108, 69, (int)Math.floor(left), 69, COL_SLURRY));
		if(filtering())
			at(right, () -> flow((int)Math.floor(right) + HEAD_W, 69, SOLUTION_X - 9, 69, COL_SOLUTION));
		GlStateManager.color(1, 1, 1, 1);

		ClientUtils.handleGuiTank(container.storage, guiLeft + STORAGE_X + 2, guiTop + ROW2 + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);
		ClientUtils.handleGuiTank(container.solution, guiLeft + SOLUTION_X + 2, guiTop + ROW2 + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);

		drawPumpAndVessel();
		drawPress(partialTicks, closing, left, right);

		// Energy (IE's red gradient, inside IE's 7 px) and the lamp's lens
		int eh = (int)((ENERGY_H - 4) * (container.energy / (float)TileFilterPress.ENERGY_CAPACITY));
		if(eh > 0)
			ClientUtils.drawGradientRect(guiLeft + ENERGY_X + 2, guiTop + ENERGY_Y + ENERGY_H - 2 - eh, guiLeft + ENERGY_X + ENERGY_W - 2, guiTop + ENERGY_Y + ENERGY_H - 2, 0xffb51500, 0xff600b00);
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.enableBlend();
		ClientUtils.bindTexture(SHEET);
		sprite(LENS, lamp() * LENS[2], LAMP_X + LAMP_SIZE / 2 - 5, LAMP_Y + LAMP_SIZE / 2 - 5);
	}

	/** How closed the press is now: from the step's ticks, between ticks too (partialTicks), so it glides. */
	float closing(float partialTicks)
	{
		int phase = container.phase;
		float t = (shownPhase == phase ? shownTicks : container.phaseTicks) + (container.state == TileFilterPress.STATE_RUNNING ? partialTicks : 0);
		return TileFilterPress.closing(phase, t);
	}

	float headLeft(float closing)
	{
		return PRESS_X + HEAD_STROKE * closing;
	}

	float headRight(float closing)
	{
		return PRESS_X + PRESS_W - HEAD_W - HEAD_STROKE * closing;
	}

	/** The impeller in the pump's eye, the slurry level in the vessel's glass, the dial's needle. */
	void drawPumpAndVessel()
	{
		int lh = Math.round(GLASS_H * Math.min(1, shownLevel / TileFilterPress.BATCH));
		if(lh > 0)
			fluid(container.feed(2), GLASS_X, GLASS_Y + GLASS_H - lh, GLASS_W, lh, 1);
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.enableBlend();
		ClientUtils.bindTexture(SHEET);
		sprite(VANE, Math.floorMod(vane, 4) * 8, EYE_X, EYE_Y);
		// Needle over 270 degrees, from the lower left
		double a = Math.toRadians(135 + 270 * shownPressure);
		line(DIAL_CX, DIAL_CY, DIAL_CX + (float)Math.cos(a) * DIAL_R, DIAL_CY + (float)Math.sin(a) * DIAL_R, COL_NEEDLE);
		GlStateManager.color(1, 1, 1, 1);
	}

	/** The press: the heads pushed in by the actuators' rods, the plates between them, the chambers filling and darkening
	 * while it filters; when it opens the cake drops on the belt and rides it into the residue box. */
	void drawPress(float partialTicks, float closing, float left, float right)
	{
		int phase = container.phase;
		float t = (shownPhase == phase ? shownTicks : container.phaseTicks) + (container.state == TileFilterPress.STATE_RUNNING ? partialTicks : 0);
		float gap = (right - left - HEAD_W - PLATES * PLATE_W) / (PLATES + 1);
		// the rods out of the actuators
		at(left, () -> rect(PRESS_X - 1, ROD_Y, (int)Math.floor(left) - PRESS_X + 1, 3, COL_ROD));
		at(right, () -> rect((int)Math.floor(right) + HEAD_W, ROD_Y, PRESS_X + PRESS_W + 1 - (int)Math.floor(right) - HEAD_W, 3, COL_ROD));
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.enableBlend();
		ClientUtils.bindTexture(SHEET);
		float[] xs = new float[PLATES];
		for(int k = 0; k < PLATES; k++)
		{
			float x = left + HEAD_W + gap * (k + 1) + PLATE_W * k;
			xs[k] = x;
			at(x, () -> sprite(PLATE, 0, (int)Math.floor(x), PRESS_Y + 2));
		}
		at(left, () -> sprite(HEAD, 0, (int)Math.floor(left), PRESS_Y));
		at(right, () -> sprite(HEAD, 0, (int)Math.floor(right), PRESS_Y));
		// the chambers: the slurry rises in the cloth during the release, the cake darkens it as the filtering goes on
		if(phase == TileFilterPress.PHASE_FILTERING)
		{
			float fill = Math.min(1, t / TileFilterPress.RELEASE_TICKS), done = Math.min(1, t / TileFilterPress.FILTER_TICKS);
			int fh = Math.round((PRESS_H - 8) * fill), top = PRESS_Y + PRESS_H - 4 - fh;
			for(float x : xs)
			{
				int x0 = (int)Math.floor(x) + 1;
				at(x, () -> {
					fluid(container.feed(4), x0, top, PLATE_W - 2, fh, 0.75F);
					rect(x0, top, PLATE_W - 2, fh, (int)(0xb0 * done) << 24 | COL_CAKE & 0xffffff);
				});
			}
		}
		GlStateManager.color(1, 1, 1, 1);
		// the belt: its cleats run while it carries the cake; the cake drops from the plates and rides into the box
		float run = belt + (discharging() ? partialTicks : 0);
		at(run, () -> {
			for(int x = BELT_X0 + Math.floorMod((int)Math.floor(run), 6); x < BELT_X1; x += 6)
				rect(x, BELT_Y, 1, 1, COL_CLEAT);
		});
		if(phase == TileFilterPress.PHASE_OPENING && t >= TileFilterPress.OPEN_MOVE_TICKS)
		{
			float way = (t - TileFilterPress.OPEN_MOVE_TICKS) / (TileFilterPress.OPEN_TICKS - TileFilterPress.OPEN_MOVE_TICKS);
			GlStateManager.enableBlend();
			ClientUtils.bindTexture(SHEET);
			for(int k = 0; k < 4; k++)
			{
				float f = way * 1.6F - k * .2F;
				if(f <= 0 || f >= 1)
					continue;
				int x0 = Math.round(xs[1 + k * 2]);
				// the first third of its way it falls from the plates onto the belt, then it rides to the box
				int y = f < .3F ? Math.round(PRESS_Y + PRESS_H + (BELT_Y - 3 - PRESS_Y - PRESS_H) * f / .3F) : BELT_Y - 3;
				float x = f < .3F ? x0 : x0 + (BIN_X - LUMP[2] - x0) * (f - .3F) / .7F;   // it goes into the box and is gone
				at(x, () -> sprite(LUMP, 0, (int)Math.floor(x), y));
			}
		}
		GlStateManager.color(1, 1, 1, 1);
	}

	boolean over(int mx, int my, int x, int y, int w, int h)
	{
		return mx >= guiLeft + x && mx < guiLeft + x + w && my >= guiTop + y && my < guiTop + y + h;
	}

	static final String K = "gui.immersivenuclear.filter_press.";

	@Override
	public void drawScreen(int mx, int my, float partialTicks)
	{
		super.drawScreen(mx, my, partialTicks);
		ArrayList<String> tip = new ArrayList<>();
		ClientUtils.handleGuiTank(container.storage, guiLeft + STORAGE_X + 2, guiTop + ROW2 + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, tip);
		ClientUtils.handleGuiTank(container.solution, guiLeft + SOLUTION_X + 2, guiTop + ROW2 + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, tip);
		if(over(mx, my, STORAGE_X, ROW2, 20, 51))
			tip.add(0, I18n.format(K + "storage"));
		if(over(mx, my, SOLUTION_X, ROW2, 20, 51))
			tip.add(0, I18n.format(K + "solution"));
		if(over(mx, my, PUMP_X, PUMP_Y, PUMP_W, PUMP_H))
			tip.add(I18n.format(K + "pump"));
		if(over(mx, my, VESSEL_X, VESSEL_Y, VESSEL_W, VESSEL_H))
		{
			FluidStack f = container.pressure.getFluid();
			tip.add(I18n.format(K + "pressure_tank"));
			if(f != null)
				tip.add("§7" + f.getLocalizedName());
			tip.add("§7" + container.pressure.getFluidAmount() + " / " + TileFilterPress.BATCH + " mB");
			tip.add("§7" + I18n.format(K + "pressure", Math.round(100 * shownPressure)));
		}
		if(over(mx, my, PRESS_X - 22, PRESS_Y, PRESS_W + 44, PRESS_H))
		{
			tip.add(I18n.format(K + "press"));
			tip.add("§7" + I18n.format(K + "phase." + container.phase));
			if(container.phase == TileFilterPress.PHASE_FILTERING)
				tip.add("§7" + I18n.format(K + "filtering", Math.round(100F * Math.min(1, container.phaseTicks / (float)TileFilterPress.FILTER_TICKS))));
		}
		if(over(mx, my, ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H))
			tip.add(container.energy + "/" + TileFilterPress.ENERGY_CAPACITY + " IF");
		if(over(mx, my, LAMP_X, LAMP_Y, LAMP_SIZE, LAMP_SIZE))
		{
			tip.add(container.state == TileFilterPress.STATE_RUNNING ? I18n.format(K + "phase." + container.phase) : I18n.format(K + "state." + container.state));
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
