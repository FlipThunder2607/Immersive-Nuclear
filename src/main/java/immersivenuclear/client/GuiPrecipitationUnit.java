package immersivenuclear.client;

import java.util.ArrayList;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.GuiIEContainerBase;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TilePrecipitationUnit;
import immersivenuclear.common.gui.ContainerPrecipitationUnit;
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
import net.minecraftforge.fml.client.config.GuiUtils;

/**
 * Interface of the Precipitation Unit, in the interface standard (01/11): separate riveted panels with the machine drawn
 * between them, the world showing through the gaps.
 *   [solution buckets, at the tank's inlet]  THE AGITATED TANK on its frame (the batch and the agitator in its window;
 *   the green motor lying on the lid, the agent's slot over its small funnel); under the cone the SCREW DEWATERER runs flat
 *   and then rises to its head, the wet concentrate's slot under the head's chute
 *   [effluent: in / tank / out | energy over the status lamp]
 * Our sheet (textures/gui/precipitation_unit_gui.png, made by ferramentas/gui_precipitacao_v7_esboco.py --jogo with
 * the same numbers) is the fixed background; the batch, the agitator, the flights and the solid are drawn here. The JEI
 * page keeps its own sheet.
 */
public class GuiPrecipitationUnit extends GuiIEContainerBase
{
	static final String IE = "immersiveengineering:textures/gui/refinery.png";
	static final String SHEET = ImmersiveNuclear.MODID + ":textures/gui/precipitation_unit_gui.png";
	static final int SHEET_SIZE = 512;

	// Layout (same as gui_precipitacao_v7_esboco.py)
	static final int ROW1 = 37, ROW2 = 62, ROW3 = 117;
	/** The agitated tank's shell; also JEI's click area (INJeiPlugin). */
	public static final int MIXER_X = 52, MIXER_Y = 43, MIXER_W = 100, MIXER_H = 61;
	/** Its window: the batch and the agitator. */
	static final int WIN_X = 60, WIN_Y = 48, WIN_W = 84, WIN_H = 50;
	static final int SHAFT_X = 102;
	/** The agitator's two impellers: height above the window's floor and half span. */
	static final int[][] IMPELLERS = {{6, 18}, {20, 12}};
	static final int FUNNEL_X = 70;
	/** The screw: flat run from SX0 to SX1 (its trough's top at SCREW_Y), then rising at 45 degrees to the head. */
	static final int SCREW_Y = 121, SCREW_H = 10, SX0 = 86, SX1 = 154, RISE = 28, HEAD_X = 182, HEAD_W = 10;
	static final int CHUTE_X = 184, CHUTE_Y0 = 103, CHUTE_Y1 = 110;
	static final int PIPE_CORE = 73, INLET_X0 = 32, INLET_X1 = 50, OUTLET_X0 = 158, OUTLET_X1 = 208;
	static final int EFFLUENT_X = 216, CONTROL_X = 242;
	static final int ENERGY_X = CONTROL_X + 2, ENERGY_Y = ROW1, ENERGY_W = 11, ENERGY_H = ROW2 + 51 - ROW1;
	static final int LAMP_X = CONTROL_X, LAMP_Y = ROW3 + 2, LAMP_SIZE = 16;
	static final int PANEL_X = ContainerPrecipitationUnit.INV_X - 8, PANEL_Y = ContainerPrecipitationUnit.INV_Y - 8;

	// Our sprites in the sheet: {u, v, w, h}
	static final int[] LENS = {0, 330, 10, 10};
	// IE's pieces in refinery.png
	static final int[] IE_SLOT = {35, 13, 20, 20}, IE_TANK = {11, 18, 20, 51}, IE_BLUE = {42, 10, 6, 3}, IE_ORANGE = {42, 49, 6, 3};

	static final int COL_SOLUTION = 0xffd8c860, COL_EFFLUENT = 0xff8a806a, COL_SOLID = 0xffd8b030, COL_SOLID_DARK = 0xffa88420,
			COL_STEEL = 0xffa8acb0, COL_STEEL_DARK = 0xff5c5f64, COL_SHAFT = 0xff96999e, COL_SHAFT_LIGHT = 0xffc8ccd0;

	final ContainerPrecipitationUnit container;
	/** Client counters for the specks and the screw's flights. */
	int turn, ticks;
	/** The agitator's turn in degrees and its speed (0..1), easing up and down like the one in the world. */
	float rotorAngle, rotorSpeed;

	public GuiPrecipitationUnit(ContainerPrecipitationUnit container)
	{
		super(container);
		this.container = container;
		xSize = ContainerPrecipitationUnit.WIDTH;
		ySize = ContainerPrecipitationUnit.HEIGHT;
	}

	boolean running()
	{
		return container.state == TilePrecipitationUnit.STATE_RUNNING;
	}

	boolean filling()
	{
		return running() && container.phase == TilePrecipitationUnit.PHASE_FILL;
	}

	boolean precipitating()
	{
		return running() && container.phase == TilePrecipitationUnit.PHASE_PRECIPITATE;
	}

	boolean dewatering()
	{
		return running() && container.phase == TilePrecipitationUnit.PHASE_DEWATER;
	}

	/** How far the uranium has come out of the solution, 0..1 (all of it once the screw takes the slurry). */
	float precipitated()
	{
		if(container.phase == TilePrecipitationUnit.PHASE_DEWATER)
			return 1;
		return container.phase == TilePrecipitationUnit.PHASE_PRECIPITATE ? Math.min(1, container.phaseTicks / (float)TilePrecipitationUnit.PRECIPITATE_TICKS) : 0;
	}

	float dewatered()
	{
		return container.phase == TilePrecipitationUnit.PHASE_DEWATER ? Math.min(1, container.phaseTicks / (float)TilePrecipitationUnit.DEWATER_TICKS) : 0;
	}

	@Override
	public void updateScreen()
	{
		super.updateScreen();
		ticks++;
		if(dewatering())
			turn++;
		float target = precipitating() ? 1 : 0;
		rotorSpeed += Math.signum(target - rotorSpeed) * Math.min(Math.abs(target - rotorSpeed), 0.025F);
		rotorAngle = (rotorAngle + rotorSpeed * TilePrecipitationUnit.ROTOR_DEGREES_PER_TICK) % 360;
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

	/** Part of a fluid's still sprite (tiled from the top left), tinted; binds the blocks atlas. */
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

	/** The lamp's colour: 0 green running, 1 amber waiting (input, or stopped by redstone), 2 red stopped (no power,
	 * output full), 3 off (idle). */
	int lamp()
	{
		switch(container.state)
		{
			case TilePrecipitationUnit.STATE_RUNNING: return 0;
			case TilePrecipitationUnit.STATE_NO_INPUT:
			case TilePrecipitationUnit.STATE_DISABLED: return 1;
			case TilePrecipitationUnit.STATE_NO_POWER:
			case TilePrecipitationUnit.STATE_OUTPUT_FULL: return 2;
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
		int[][] xy = ContainerPrecipitationUnit.SLOT_XY;
		for(int i = 0; i < xy.length; i++)
		{
			piece(IE_SLOT, xy[i][0] - 2, xy[i][1] - 2);
			boolean in = i == TilePrecipitationUnit.SLOT_AGENT || i == TilePrecipitationUnit.SLOT_SOLUTION_IN || i == TilePrecipitationUnit.SLOT_EFFLUENT_IN;
			piece(in ? IE_BLUE : IE_ORANGE, xy[i][0] + 5, xy[i][1] - 5);
		}
		piece(IE_TANK, EFFLUENT_X, ROW2);
		piece(IE_ORANGE, EFFLUENT_X + 7, ROW2 - 3);
		GuiGraphite.endTint();

		// Liquid moving in the pipes: the solution coming in while the tank fills, the clear effluent while the screw runs
		if(filling())
			rect(INLET_X0, PIPE_CORE, INLET_X1 - INLET_X0, 2, COL_SOLUTION);
		if(dewatering())
			rect(OUTLET_X0, PIPE_CORE, OUTLET_X1 - OUTLET_X0, 2, COL_EFFLUENT);
		GlStateManager.color(1, 1, 1, 1);
		ClientUtils.handleGuiTank(container.effluent, guiLeft + EFFLUENT_X + 2, guiTop + ROW2 + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);

		drawMixer(partialTicks);
		drawScrew(partialTicks);

		// Energy (IE's red gradient, inside IE's 7 px) and the lamp's lens
		int eh = (int)((ENERGY_H - 4) * (container.energy / (float)TilePrecipitationUnit.ENERGY_CAPACITY));
		if(eh > 0)
			ClientUtils.drawGradientRect(guiLeft + ENERGY_X + 2, guiTop + ENERGY_Y + ENERGY_H - 2 - eh, guiLeft + ENERGY_X + ENERGY_W - 2, guiTop + ENERGY_Y + ENERGY_H - 2, 0xffb51500, 0xff600b00);
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.enableBlend();
		ClientUtils.bindTexture(SHEET);
		Gui.drawModalRectWithCustomSizedTexture(guiLeft + LAMP_X + LAMP_SIZE / 2 - 5, guiTop + LAMP_Y + LAMP_SIZE / 2 - 5, LENS[0] + lamp() * LENS[2], LENS[1], LENS[2], LENS[3], SHEET_SIZE, SHEET_SIZE);
	}

	/** The batch in the window, clouding yellow as the uranium precipitates; the agitator; the agent falling in. */
	void drawMixer(float partialTicks)
	{
		int floor = WIN_Y + WIN_H, top = WIN_Y + 3;
		int lh = Math.round((floor - top) * Math.min(1, container.mixer / (float)TilePrecipitationUnit.BATCH_SOLUTION));
		int surface = floor - lh;
		float p = precipitated();
		if(lh > 0)
		{
			fluid(INContent.fluidPurifiedSolution, WIN_X, surface, WIN_W, lh);
			rect(WIN_X, surface, WIN_W, lh, (int)(150 * p) << 24 | COL_SOLID & 0xffffff);
			rect(WIN_X, surface, WIN_W, 1, 0x46ffffff);
			// Solid specks swirling while it precipitates, settling low as it goes on
			if(p > 0 && lh > 8)
				for(int k = 0; k < 14; k++)
				{
					int sx = WIN_X + 2 + Math.floorMod(k * 29 + Math.round(rotorAngle / 8) * (k % 2 == 0 ? 1 : -1), WIN_W - 6);
					int span = Math.max(1, lh - 6);
					int sy = surface + 3 + Math.floorMod(k * 11 + (precipitating() ? ticks / 3 : 0), span);
					rect(sx, sy, 2, 2, 0xc0f0d050);
				}
		}
		// The shaft down the window and the two impellers turning
		int lowest = floor - IMPELLERS[0][0];
		rect(SHAFT_X - 1, WIN_Y, 3, lowest - WIN_Y + 2, COL_SHAFT);
		rect(SHAFT_X - 1, WIN_Y, 1, lowest - WIN_Y + 2, COL_SHAFT_LIGHT);
		float angle = rotorAngle + rotorSpeed * TilePrecipitationUnit.ROTOR_DEGREES_PER_TICK * partialTicks;
		for(int[] imp : IMPELLERS)
		{
			int y = floor - imp[0];
			for(int pass = 0; pass < 2; pass++)
				for(int k = 0; k < 4; k++)
				{
					double a = Math.toRadians(angle + k * 90);
					boolean front = Math.sin(a) > 0;
					if(front != (pass == 1))
						continue;
					int tip = (int)Math.round(Math.cos(a) * imp[1]);
					rect(Math.min(SHAFT_X, SHAFT_X + tip), y, Math.abs(tip) + 1, 2, front ? COL_STEEL : COL_STEEL_DARK);
					rect(Math.min(SHAFT_X, SHAFT_X + tip), y + 2, Math.abs(tip) + 1, 1, 0xff2a2c30);
				}
			rect(SHAFT_X - 2, y - 1, 5, 4, COL_SHAFT);
		}
		// The agent falling from the funnel at the start of the precipitation
		if(precipitating() && container.phaseTicks < 30)
			for(int k = 0; k < 4; k++)
				rect(FUNNEL_X - 2 + k, WIN_Y + Math.floorMod(ticks * 3 + k * 9, Math.max(1, surface - WIN_Y)), 1, 2, 0xffe8e0d0);
	}

	/** The trough's top at x (flat, then rising at 45 degrees). */
	static int troughTop(int x)
	{
		return SCREW_Y - Math.max(0, x - SX1);
	}

	/** The flights turning (moving towards the head) and the concentrate riding them, then down the chute. */
	void drawScrew(float partialTicks)
	{
		int shift = dewatering() ? turn / 2 % 4 : 0;
		// Flat run: upright flights
		for(int x = SX0 + 3 + shift; x < SX1; x += 4)
		{
			rect(x, SCREW_Y + 2, 1, SCREW_H - 4, COL_STEEL);
			rect(x + 1, SCREW_Y + SCREW_H / 2, 1, 1, COL_STEEL);
		}
		// Rising run: flights across the trough at -45 degrees
		for(int i = 3 + shift; i < RISE - 4; i += 4)
		{
			int x = SX1 + i, top = SCREW_Y - i;
			for(int j = 0; j < 4; j++)
				rect(x + j, top + 2 + j, 1, 1, COL_STEEL);
		}
		if(!dewatering())
			return;
		float d = dewatered();
		// The concentrate: lumps along the trough's floor from the cone's outlet to the head, then down the chute
		int pathFlat = SX1 - SHAFT_X, pathRise = RISE - 2, pathChute = CHUTE_Y1 - CHUTE_Y0 + 6;
		int path = pathFlat + pathRise + pathChute;
		for(int k = 0; k < 9; k++)
		{
			float s = (ticks + partialTicks) * 0.8F + k * path / 9F;
			int t = (int)(s % path);
			int x, y;
			if(t < pathFlat)
			{
				x = SHAFT_X + t;
				y = SCREW_Y + SCREW_H - 4;
			}
			else if(t < pathFlat + pathRise)
			{
				x = SX1 + (t - pathFlat);
				y = troughTop(x) + SCREW_H - 4;
			}
			else
			{
				x = CHUTE_X + 2 + k % 3 * 2;
				y = CHUTE_Y0 - 4 + (t - pathFlat - pathRise);
				if(y < CHUTE_Y0)
					continue;
			}
			rect(x, y, 2, 2, k % 3 == 0 ? COL_SOLID_DARK : COL_SOLID);
		}
		// The slurry in the cone's outlet going down into the screw
		if(d < 0.9F)
			rect(SHAFT_X - 1, SCREW_Y - 9, 3, 9, COL_SOLID_DARK);
	}

	boolean over(int mx, int my, int x, int y, int w, int h)
	{
		return mx >= guiLeft + x && mx < guiLeft + x + w && my >= guiTop + y && my < guiTop + y + h;
	}

	static final String K = "gui.immersivenuclear.precipitation_unit.";

	@Override
	public void drawScreen(int mx, int my, float partialTicks)
	{
		super.drawScreen(mx, my, partialTicks);
		ArrayList<String> tip = new ArrayList<>();
		ClientUtils.handleGuiTank(container.effluent, guiLeft + EFFLUENT_X + 2, guiTop + ROW2 + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, tip);
		if(over(mx, my, EFFLUENT_X, ROW2, 20, 51))
			tip.add(0, I18n.format(K + "effluent"));
		if(over(mx, my, MIXER_X, MIXER_Y, MIXER_W, MIXER_H))
		{
			tip.add(I18n.format(K + "mixer"));
			if(container.phase == TilePrecipitationUnit.PHASE_FILL)
				tip.add("§7" + I18n.format(K + "fill_info"));
			tip.add("§7" + I18n.format(K + "phase." + container.phase));
			if(container.phase == TilePrecipitationUnit.PHASE_PRECIPITATE)
				tip.add("§7" + I18n.format(K + "precipitated", Math.round(100 * precipitated())));
			tip.add("§7" + container.mixer + " / " + TilePrecipitationUnit.BATCH_SOLUTION + " mB");
			tip.add("§7" + I18n.format(K + "batch", TilePrecipitationUnit.BATCH_SOLUTION, TilePrecipitationUnit.AGENT_PER_BATCH,
					TilePrecipitationUnit.CONCENTRATE_PER_BATCH, TilePrecipitationUnit.EFFLUENT_PER_BATCH));
		}
		if(over(mx, my, SX0, SCREW_Y - RISE, HEAD_X + HEAD_W - SX0, RISE + SCREW_H) && !over(mx, my, MIXER_X, MIXER_Y, MIXER_W, MIXER_H))
		{
			tip.add(I18n.format(K + "screw"));
			if(container.phase == TilePrecipitationUnit.PHASE_DEWATER)
				tip.add("§7" + I18n.format(K + "progress", Math.round(100 * dewatered())));
		}
		int[] agent = ContainerPrecipitationUnit.SLOT_XY[TilePrecipitationUnit.SLOT_AGENT];
		if(over(mx, my, agent[0], agent[1], 16, 16) && container.getSlot(TilePrecipitationUnit.SLOT_AGENT).getStack().isEmpty())
			tip.add(I18n.format(K + "agent", TilePrecipitationUnit.AGENT_PER_BATCH));
		int[] out = ContainerPrecipitationUnit.SLOT_XY[TilePrecipitationUnit.SLOT_CONCENTRATE];
		if(over(mx, my, out[0], out[1], 16, 16) && container.getSlot(TilePrecipitationUnit.SLOT_CONCENTRATE).getStack().isEmpty())
			tip.add(I18n.format(K + "concentrate"));
		if(over(mx, my, ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H))
			tip.add(container.energy + "/" + TilePrecipitationUnit.ENERGY_CAPACITY + " IF");
		if(over(mx, my, LAMP_X, LAMP_Y, LAMP_SIZE, LAMP_SIZE))
		{
			tip.add(container.state == TilePrecipitationUnit.STATE_RUNNING ? I18n.format(K + "phase." + container.phase) : I18n.format(K + "state." + container.state));
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
