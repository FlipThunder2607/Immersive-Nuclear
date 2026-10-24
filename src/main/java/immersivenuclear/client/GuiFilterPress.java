package immersivenuclear.client;

import java.util.ArrayList;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.GuiIEContainerBase;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileFilterPress;
import immersivenuclear.common.gui.ContainerFilterPress;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.client.config.GuiUtils;

/**
 * Interface of the Filtration Unit, in the finish of the Chemical Separation Unit (graphite brushed steel, IE's own
 * slots, tanks and markers drawn at run time a little darker, the same right column), as a flow diagram:
 *   buckets | storage tank > piston pump > pressure vessel with its dial > filter press > solution tank | buckets
 *   below: the 28 filters | the chute from the press into the residue slot
 * The pump's flywheel turns and its piston strokes while it pressurises; the dial shows the pressure; the press
 * closes its plates, fills its chambers when the pressure is released (the solution leaves at once) and drops the
 * cake down the chute when it opens. Our sheet (textures/gui/filter_press.png, made by ferramentas/gui_filtro.py with
 * the same constants) holds the background and those parts; the button and the lights are machine 1's.
 */
public class GuiFilterPress extends GuiIEContainerBase
{
	static final String IE = "immersiveengineering:textures/gui/refinery.png";
	static final String OWN = ImmersiveNuclear.MODID + ":textures/gui/chemical_processor.png";
	static final String SHEET = ImmersiveNuclear.MODID + ":textures/gui/filter_press.png";
	static final int SHEET_SIZE = 512;

	// Layout (same as gui_filtro.py)
	static final int ROW = 16, STORAGE_X = 32, SOLUTION_X = 195;
	/** Also JEI's click areas for the recipes (INJeiPlugin). */
	public static final int PUMP_X = 56, PUMP_DY = 13, PUMP_W = 30, PUMP_H = 24;
	public static final int PRESS_X = 120, PRESS_DY = 6, PRESS_W = 70, PRESS_H = 38;
	static final int WHEEL_CX = 7, WHEEL_CY = 12, WHEEL_R = 6, CRANK_R = 4;
	/** The piston's stroke is twice the crank radius. */
	static final int PUMP_WIN_X = 17, PUMP_WIN_Y = 9, PUMP_WIN_W = 11, PUMP_WIN_H = 6, PISTON_W = 3;
	static final int VESSEL_X = 90, VESSEL_DY = 14, VESSEL_W = 24, VESSEL_H = 40;
	static final int GLASS_X = 10, GLASS_Y = 8, GLASS_W = 4, GLASS_H = 24;
	static final int DIAL_X = 93, DIAL_DY = -5, DIAL_S = 18;
	static final int CHAMBER_X = 3, CHAMBER_Y = 4, CHAMBER_H = 30;
	static final int CYL_W = 9, HEAD_W = 3, PLATE_W = 2, PLATES = 7;
	/** The head moves this far when the press closes, and the chambers go from OPEN_GAP to CLOSED_GAP wide. */
	static final int HEAD_STROKE = 7, OPEN_GAP = 4, CLOSED_GAP = 3;
	static final int STRIP_X = 63, STRIP_Y = 5, STRIP_W = 4, STRIP_H = 28;
	static final int CHUTE_X = 147, CHUTE_Y = ROW + PRESS_DY + PRESS_H, CHUTE_S = 16;
	/** Every pipe between the parts at the tanks' middle; the slurry comes in low, under the pump. */
	static final int PIPE_DY = 25, INLET_DY = 44, INLET_X = PUMP_X + 22;
	static final int ENERGY_X = 248, ENERGY_Y = 16, ENERGY_W = 14, ENERGY_H = 64;
	static final int BUTTON_X = 247, BUTTON_Y = 86, BUTTON_SIZE = 16;
	static final int LIGHTS_X = 248, LIGHTS_W = 14, LIGHT_X = 251, LIGHT_Y = 112, LIGHT_STEP = 8;
	static final int PANEL_X = ContainerFilterPress.INV_X - 8, PANEL_Y = ContainerFilterPress.INV_Y - 8;

	// Our sprites in the sheet: {u, v, w, h}
	static final int[] PUMP = {0, 260, PUMP_W, PUMP_H}, WHEEL = {32, 260, 2 * WHEEL_R + 1, 2 * WHEEL_R + 1}, PISTON = {100, 260, PISTON_W, PUMP_WIN_H},
			VESSEL = {110, 260, VESSEL_W, VESSEL_H}, PRESS = {0, 304, PRESS_W, PRESS_H}, DIAL = {0, 346, DIAL_S, DIAL_S},
			PLATE = {72, 304, PLATE_W, CHAMBER_H}, HEAD = {76, 304, HEAD_W, CHAMBER_H}, CHUTE = {82, 304, CHUTE_S, CHUTE_S};
	static final int WHEEL_STEP = 15;
	// IE's pieces in refinery.png
	static final int[] IE_SLOT = {35, 13, 20, 20}, IE_TANK = {11, 18, 20, 51}, IE_BLUE = {42, 10, 6, 3}, IE_ORANGE = {42, 49, 6, 3};

	static final int COL_SLURRY = 0xff968060, COL_SOLUTION = 0xffc86e32, COL_CAKE = 0xff6e5a3c, COL_NEEDLE = 0xffb01e14, COL_ROD = 0xffaaadb2;

	final ContainerFilterPress container;
	/** Shown pressure, 0..1: eases towards the real one, so the needle rises and falls smoothly. */
	float shownPressure;
	/** The pump's crank angle, in degrees, turning while it pressurises. */
	float crank, lastCrank;
	/** The press step's tick count kept on the client (smooth), set from the server's on a new step or a drift. */
	int shownPhase = -1, shownTicks;

	public GuiFilterPress(ContainerFilterPress container)
	{
		super(container);
		this.container = container;
		xSize = ContainerFilterPress.WIDTH;
		ySize = ContainerFilterPress.HEIGHT;
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

	/** The pump's pressure on what is left in the vessel: it falls as the release empties it. */
	float targetPressure()
	{
		int amount = container.pressure.getFluidAmount();
		return amount > 0 ? container.pump / (float)TileFilterPress.PRESSURIZE_TICKS * Math.min(1, amount / (float)TileFilterPress.BATCH) : 0;
	}

	float pressure()
	{
		return shownPressure;
	}

	@Override
	public void updateScreen()
	{
		super.updateScreen();
		shownPressure += (targetPressure() - shownPressure) * 0.25F;
		if(Math.abs(targetPressure() - shownPressure) < 0.002F)
			shownPressure = targetPressure();
		lastCrank = crank;
		if(pumping())
			crank += 30;
		if(container.phase != shownPhase || Math.abs(container.phaseTicks - shownTicks) > 4)
		{
			shownPhase = container.phase;
			shownTicks = container.phaseTicks;
		}
		else if(container.state == TileFilterPress.STATE_RUNNING)
			shownTicks++;
	}

	// ---------- drawing helpers ----------
	void sprite(int[] s, int x, int y, int du, int w, int h)
	{
		Gui.drawModalRectWithCustomSizedTexture(guiLeft + x, guiTop + y, s[0] + du, s[1], w, h, SHEET_SIZE, SHEET_SIZE);
	}

	void sprite(int[] s, int x, int y)
	{
		sprite(s, x, y, 0, s[2], s[3]);
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

	/** Moving fluid inside a pipe of the background (3 px core), one pixel past each end so the elbows join. */
	void flow(int x0, int y0, int x1, int y1, int argb)
	{
		if(y0 == y1)
			rect(Math.min(x0, x1) - 1, y0 - 1, Math.abs(x1 - x0) + 3, 3, argb);
		else
			rect(x0 - 1, Math.min(y0, y1) - 1, 3, Math.abs(y1 - y0) + 3, argb);
	}

	/** A one pixel line, point by point (the needle, the connecting rod). */
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

	/** 0 green running, 1 yellow waiting (slurry, pressure), 2 red stopped (power, full output, filters), 3 grey disabled. */
	int litLight()
	{
		if(container.state == TileFilterPress.STATE_RUNNING)
			return container.phase == TileFilterPress.PHASE_CLOSED ? 1 : 0;
		switch(container.state)
		{
			case TileFilterPress.STATE_NO_INPUT: return 1;
			case TileFilterPress.STATE_NO_POWER:
			case TileFilterPress.STATE_OUTPUT_FULL:
			case TileFilterPress.STATE_NO_FILTERS: return 2;
			case TileFilterPress.STATE_DISABLED: return 3;
			default: return -1;
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mx, int my)
	{
		GlStateManager.color(1, 1, 1, 1);
		ClientUtils.bindTexture(SHEET);
		Gui.drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, xSize, ySize, SHEET_SIZE, SHEET_SIZE);

		// IE's wooden inventory panel; IE's pieces, a little darker
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
		piece(IE_TANK, STORAGE_X, ROW);
		piece(IE_BLUE, STORAGE_X + 7, ROW - 3);
		piece(IE_TANK, SOLUTION_X, ROW);
		piece(IE_ORANGE, SOLUTION_X + 7, ROW - 3);
		GuiGraphite.endTint();

		// Fluid moving in the pipes
		if(pumping())
		{
			flow(STORAGE_X + 20, ROW + INLET_DY, INLET_X, ROW + INLET_DY, COL_SLURRY);
			flow(INLET_X, ROW + INLET_DY, INLET_X, ROW + PUMP_DY + PUMP_H - 2, COL_SLURRY);
			flow(PUMP_X + PUMP_W, ROW + PIPE_DY, VESSEL_X - 1, ROW + PIPE_DY, COL_SLURRY);
		}
		if(releasing())
			flow(VESSEL_X + VESSEL_W, ROW + PIPE_DY, PRESS_X - 1, ROW + PIPE_DY, COL_SLURRY);
		if(filtering())
			flow(PRESS_X + PRESS_W, ROW + PIPE_DY, SOLUTION_X - 1, ROW + PIPE_DY, COL_SOLUTION);
		GlStateManager.color(1, 1, 1, 1);

		// Tanks: IE's fluid drawing and gauge
		ClientUtils.handleGuiTank(container.storage, guiLeft + STORAGE_X + 2, guiTop + ROW + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);
		ClientUtils.handleGuiTank(container.solution, guiLeft + SOLUTION_X + 2, guiTop + ROW + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);

		drawPump(partialTicks);
		drawVessel();
		drawPress(partialTicks);

		// Energy (IE's red gradient), button, lights
		int eh = (int)((ENERGY_H - 4) * (container.energy / (float)TileFilterPress.ENERGY_CAPACITY));
		if(eh > 0)
			ClientUtils.drawGradientRect(guiLeft + ENERGY_X + 2, guiTop + ENERGY_Y + ENERGY_H - 2 - eh, guiLeft + ENERGY_X + ENERGY_W - 2, guiTop + ENERGY_Y + ENERGY_H - 2, 0xffb51500, 0xff600b00);
		GlStateManager.color(1, 1, 1, 1);
		ClientUtils.bindTexture(OWN);
		drawTexturedModalRect(guiLeft + BUTTON_X, guiTop + BUTTON_Y, 0, 150, BUTTON_SIZE, BUTTON_SIZE);
		int lit = litLight();
		for(int i = 0; i < 4; i++)
			drawTexturedModalRect(guiLeft + LIGHT_X + 1, guiTop + LIGHT_Y + i * LIGHT_STEP, i * 8, i == lit ? 90 : 98, 6, 6);
	}

	/** Flywheel, crank and connecting rod driving the piston; the slurry in front of the piston. */
	void drawPump(float partialTicks)
	{
		float deg = lastCrank + (crank - lastCrank) * partialTicks;
		double a = Math.toRadians(deg);
		int wx = PUMP_X + WHEEL_CX, wy = ROW + PUMP_DY + WHEEL_CY;
		// Slider-crank: the rod has a fixed length, so the piston follows the crank pin (pin right = piston forward)
		float pinX = wx + (float)Math.cos(a) * CRANK_R, pinY = wy + (float)Math.sin(a) * CRANK_R;
		int rodLength = PUMP_X + PUMP_WIN_X - (wx - CRANK_R);
		int pos = Math.round(pinX + rodLength) - (PUMP_X + PUMP_WIN_X);
		int px = PUMP_X + PUMP_WIN_X + pos, py = ROW + PUMP_DY + PUMP_WIN_Y;
		if(container.pressure.getFluidAmount() > 0 || container.storage.getFluidAmount() > 0)
			fluid(INContent.fluidSlurry, px + PISTON_W, py, PUMP_WIN_W - pos - PISTON_W, PUMP_WIN_H, 1);
		ClientUtils.bindTexture(SHEET);
		sprite(PUMP, PUMP_X, ROW + PUMP_DY);
		sprite(PISTON, px, py);
		int frame = Math.floorMod((int)Math.floor(deg / 22.5), 4);
		sprite(WHEEL, wx - WHEEL_R, wy - WHEEL_R, frame * WHEEL_STEP, WHEEL[2], WHEEL[3]);
		line(pinX, pinY, px, py + PUMP_WIN_H / 2, COL_ROD);
		GlStateManager.color(1, 1, 1, 1);
	}

	/** The vessel with the slurry level in its glass, the dial and its needle on top. */
	void drawVessel()
	{
		int amount = container.pressure.getFluidAmount();
		int lh = Math.round(GLASS_H * Math.min(1, amount / (float)TileFilterPress.BATCH));
		int vx = VESSEL_X, vy = ROW + VESSEL_DY;
		if(lh > 0)
			fluid(INContent.fluidSlurry, vx + GLASS_X, vy + GLASS_Y + GLASS_H - lh, GLASS_W, lh, 1);
		ClientUtils.bindTexture(SHEET);
		sprite(VESSEL, vx, vy);
		sprite(DIAL, DIAL_X, ROW + DIAL_DY);
		// Needle over 270 degrees, from the lower left
		float c = (DIAL_S - 1) / 2F;
		double a = Math.toRadians(135 + 270 * pressure());
		float cx = DIAL_X + c, cy = ROW + DIAL_DY + c;
		line(cx, cy, cx + (float)Math.cos(a) * (c - 3), cy + (float)Math.sin(a) * (c - 3), COL_NEEDLE);
		GlStateManager.color(1, 1, 1, 1);
	}

	/** Left edge of the head, then of each plate, for a closing of 0 (open) .. 1 (closed); in GUI pixels, not rounded. */
	float[] platePositions(float closing)
	{
		int base = PRESS_X + CHAMBER_X + CYL_W;
		float[] xs = new float[PLATES + 1];
		float head = HEAD_STROKE * closing, gap = OPEN_GAP + (CLOSED_GAP - OPEN_GAP) * closing;
		xs[0] = base + head;
		for(int k = 1; k <= PLATES; k++)
			xs[k] = base + head + HEAD_W + gap * k + PLATE_W * (k - 1);
		return xs;
	}

	/** Draws at a fractional GUI x (the screen has 2 to 4 pixels per GUI pixel, so a moving part glides). */
	void at(float x, Runnable draw)
	{
		GlStateManager.pushMatrix();
		GlStateManager.translate(x - (int)Math.floor(x), 0, 0);
		draw.run();
		GlStateManager.popMatrix();
	}

	/** The press: rod, head and plates; the slurry in the chambers while it filters; the cake falling when it opens. */
	void drawPress(float partialTicks)
	{
		int phase = container.phase;
		float t = (shownPhase == phase ? shownTicks : container.phaseTicks) + (container.state == TileFilterPress.STATE_RUNNING ? partialTicks : 0);
		float[] xs = platePositions(TileFilterPress.closing(phase, t));
		int top = ROW + PRESS_DY + CHAMBER_Y;
		// Slurry in the chambers: they fill during the release, the cake darkens them as the filtering goes on
		if(phase == TileFilterPress.PHASE_FILTERING)
		{
			float fill = Math.min(1, t / TileFilterPress.RELEASE_TICKS), done = Math.min(1, t / TileFilterPress.FILTER_TICKS);
			int fh = Math.round((CHAMBER_H - 4) * fill);
			for(int k = 1; k <= PLATES; k++)
			{
				int x0 = (int)Math.floor(k == 1 ? xs[0] + HEAD_W : xs[k - 1] + PLATE_W), w = Math.round(xs[k] - (k == 1 ? xs[0] + HEAD_W : xs[k - 1] + PLATE_W));
				fluid(INContent.fluidSlurry, x0, top + CHAMBER_H - 2 - fh, w, fh, 0.85F);
				rect(x0, top + CHAMBER_H - 2 - fh, w, fh, (int)(0xb0 * done) << 24 | COL_CAKE & 0xffffff);
			}
		}
		// Rod from the cylinder barrel to the head
		int rodFrom = PRESS_X + CHAMBER_X + CYL_W - 3;
		float headX = xs[0];
		at(headX, () -> rect(rodFrom, top + CHAMBER_H / 2 - 1, (int)Math.floor(headX) - rodFrom + 1, 3, COL_ROD));
		// Filtering progress in the strip, IE's red
		if(phase == TileFilterPress.PHASE_FILTERING)
		{
			int sh = (int)(STRIP_H * Math.min(1, t / TileFilterPress.FILTER_TICKS));
			int sx = guiLeft + PRESS_X + STRIP_X, sy = guiTop + ROW + PRESS_DY + STRIP_Y;
			if(sh > 0)
				ClientUtils.drawGradientRect(sx, sy + STRIP_H - sh, sx + STRIP_W, sy + STRIP_H, 0xffb51500, 0xff600b00);
		}
		// The cake falls down the chute while the press opens
		if(phase == TileFilterPress.PHASE_OPENING && t >= TileFilterPress.OPEN_MOVE_TICKS)
		{
			float fall = (t - TileFilterPress.OPEN_MOVE_TICKS) / 20F;
			for(int k = 0; k < 3; k++)
			{
				float f = fall - k * .3F;
				if(f > 0 && f < 1)
					rect(CHUTE_X + 5 + k * 2, CHUTE_Y + (int)(f * (CHUTE_S - 3)), 3, 3, COL_CAKE);
			}
		}
		GlStateManager.color(1, 1, 1, 1);
		ClientUtils.bindTexture(SHEET);
		for(int k = 0; k <= PLATES; k++)
		{
			float x = xs[k];
			int[] s = k == 0 ? HEAD : PLATE;
			at(x, () -> sprite(s, (int)Math.floor(x), top));
		}
		sprite(PRESS, PRESS_X, ROW + PRESS_DY);
		sprite(CHUTE, CHUTE_X, CHUTE_Y);
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

	static final String K = "gui.immersivenuclear.filter_press.";

	@Override
	public void drawScreen(int mx, int my, float partialTicks)
	{
		super.drawScreen(mx, my, partialTicks);
		ArrayList<String> tip = new ArrayList<>();
		ClientUtils.handleGuiTank(container.storage, guiLeft + STORAGE_X + 2, guiTop + ROW + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, tip);
		ClientUtils.handleGuiTank(container.solution, guiLeft + SOLUTION_X + 2, guiTop + ROW + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, tip);
		if(over(mx, my, STORAGE_X, ROW, 20, 51))
			tip.add(0, I18n.format(K + "storage"));
		if(over(mx, my, SOLUTION_X, ROW, 20, 51))
			tip.add(0, I18n.format(K + "solution"));
		if(over(mx, my, PUMP_X, ROW + PUMP_DY, PUMP_W, PUMP_H))
			tip.add(I18n.format(K + "pump"));
		if(over(mx, my, VESSEL_X, ROW + VESSEL_DY, VESSEL_W, VESSEL_H) || over(mx, my, DIAL_X, ROW + DIAL_DY, DIAL_S, DIAL_S))
		{
			FluidStack f = container.pressure.getFluid();
			tip.add(I18n.format(K + "pressure_tank"));
			if(f != null)
				tip.add("§7" + f.getLocalizedName());
			tip.add("§7" + container.pressure.getFluidAmount() + " / " + TileFilterPress.BATCH + " mB");
			tip.add("§7" + I18n.format(K + "pressure", Math.round(100 * pressure())));
		}
		if(over(mx, my, PRESS_X, ROW + PRESS_DY, PRESS_W, PRESS_H))
		{
			tip.add(I18n.format(K + "press"));
			tip.add("§7" + I18n.format(K + "phase." + container.phase));
			if(container.phase == TileFilterPress.PHASE_FILTERING)
				tip.add("§7" + I18n.format(K + "filtering", Math.round(100F * Math.min(1, container.phaseTicks / (float)TileFilterPress.FILTER_TICKS))));
		}
		if(over(mx, my, ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H))
			tip.add(container.energy + "/" + TileFilterPress.ENERGY_CAPACITY + " IF");
		if(over(mx, my, BUTTON_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE))
		{
			tip.add(I18n.format("gui.immersivenuclear.redstone"));
			tip.add("§7" + I18n.format(container.redstoneInverted ? "gui.immersivenuclear.redstone.inverted" : "gui.immersivenuclear.redstone.normal"));
		}
		int lit = litLight();
		for(int i = 0; i < 4; i++)
			if(over(mx, my, LIGHTS_X, LIGHT_Y + i * LIGHT_STEP - 1, LIGHTS_W, LIGHT_STEP))
			{
				if(i != lit)
					tip.add("§7" + I18n.format(K + "light." + i));
				else if(container.state == TileFilterPress.STATE_RUNNING)
					tip.add(I18n.format(K + "phase." + container.phase));
				else
					tip.add(I18n.format(K + "state." + container.state));
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
			mc.playerController.sendEnchantPacket(inventorySlots.windowId, ContainerFilterPress.BUTTON_REDSTONE);
		}
	}
}
