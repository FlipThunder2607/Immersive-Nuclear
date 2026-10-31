package immersivenuclear.client;

import java.util.ArrayList;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.GuiIEContainerBase;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.block.TileSulfuricAcidUnit;
import immersivenuclear.common.gui.ContainerSulfuricAcidUnit;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.lwjgl.opengl.GL11;

/**
 * Interface of the Sulfuric Acid Unit, in the mod's graphite finish drawn at run time ({@link GuiGraphite}) with IE's
 * own slots, tanks and markers a little darker, and the same right column as the other machines (energy, redstone
 * button, lights). The contact process left to right, as in the machine:
 *   sulfur > BURNER (blue flame) > BOILER under its STEAM DRUM > CONVERTER (four beds) > ABSORPTION TOWER > acid
 *   water tank > tower top; steam drum > steam tank
 * While a cycle runs the flame flickers, the gas moves through the pipes, the beds glow and the acid trickles down the
 * tower's packing.
 */
public class GuiSulfuricAcidUnit extends GuiIEContainerBase
{
	static final String IE = "immersiveengineering:textures/gui/refinery.png";
	static final String OWN = ImmersiveNuclear.MODID + ":textures/gui/chemical_processor.png";

	// Layout (as in the approved sketch, ferramentas/gui_acido_sulfurico.py)
	static final int BURNER_X = 12, BURNER_Y = 60, BURNER_W = 44, BURNER_H = 30;
	static final int DRUM_X = 60, DRUM_Y = 30, DRUM_W = 30, DRUM_H = 12;
	static final int BOILER_X = 64, BOILER_Y = 46, BOILER_W = 22, BOILER_H = 64;
	public static final int CONV_X = 96, CONV_Y = 26, CONV_W = 22, CONV_H = 86;
	static final int TOWER_X = 126, TOWER_Y = 26, TOWER_W = 20, TOWER_H = 86;
	/** The tanks, nearest the tower first: the acid comes in at its side; each has its container slots under it. */
	static final int ACID_X = 162, WATER_X = 188, STEAM_X = 214, TANK_Y = 30;
	public static final int ARROW_X = 133, ARROW_Y = 118;
	/** The acid: out of the tower's foot into the arrow, up the gap between the tower and the acid tank, into its side. */
	static final int FOOT_X = 130, ACID_PIPE_X = 154, ACID_IN_Y = TANK_Y + 36;
	/** The water: out of the top of its tank, over the acid tank, down into the top of the tower. */
	static final int WATER_OUT_X = WATER_X + 3, WATER_LINE_Y = 20, WATER_DROP_X = 141;
	/** The steam: over everything, then down into the top of its tank beside the port mark, like the water. */
	static final int STEAM_LINE_Y = 14, STEAM_IN_X = STEAM_X + 3;
	static final int ENERGY_X = 248, ENERGY_Y = 24, ENERGY_W = 14, ENERGY_H = 80;
	static final int BUTTON_X = 247, BUTTON_Y = 112, BUTTON_SIZE = 16;
	static final int LIGHTS_X = 248, LIGHTS_W = 14, LIGHT_X = 251, LIGHT_Y = 140, LIGHT_STEP = 8;
	static final int PANEL_X = ContainerSulfuricAcidUnit.INV_X - 8, PANEL_Y = ContainerSulfuricAcidUnit.INV_Y - 8;

	// IE's pieces in refinery.png
	static final int[] IE_SLOT = {35, 13, 20, 20}, IE_TANK = {11, 18, 20, 51}, IE_BLUE = {42, 10, 6, 3}, IE_ORANGE = {42, 49, 6, 3};

	static final int COL_PIPE = 0xff2a2c30, COL_PIPE_EDGE = 0xff55585e, COL_STEEL = 0xff96999e, COL_STEEL_DARK = 0xff5c5f64,
			COL_INSIDE = 0xff24262a, COL_BRICK = 0xff6e3428, COL_MORTAR = 0xff46221c, COL_FIREBOX = 0xff101222,
			COL_SULFUR = 0xffe8d040, COL_GAS = 0xc8c8d0a0, COL_STEAM = 0xc8b4b6ba, COL_WATER = 0xff3c6ab4, COL_ACID = 0xffd6c896,
			COL_PACKING = 0xffc8c4b8, COL_TUBE = 0xff8a5a40;

	final ContainerSulfuricAcidUnit container;
	int ticks;

	public GuiSulfuricAcidUnit(ContainerSulfuricAcidUnit container)
	{
		super(container);
		this.container = container;
		xSize = ContainerSulfuricAcidUnit.WIDTH;
		ySize = ContainerSulfuricAcidUnit.HEIGHT;
	}

	boolean running()
	{
		return container.state == TileSulfuricAcidUnit.STATE_RUNNING;
	}

	float done()
	{
		return container.progress / (float)(TileSulfuricAcidUnit.CYCLE_TICKS * container.cycleSize);
	}

	@Override
	public void updateScreen()
	{
		super.updateScreen();
		ticks++;
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

	/** Moving fluid inside a pipe (its 3 px core), only while the cycle runs. */
	void flow(int x0, int y0, int x1, int y1, int argb)
	{
		if(!running())
			return;
		if(y0 == y1)
			rect(Math.min(x0, x1) - 1, y0 - 1, Math.abs(x1 - x0) + 3, 3, argb);
		else
			rect(x0 - 1, Math.min(y0, y1) - 1, 3, Math.abs(y1 - y0) + 3, argb);
	}

	/** 0 green running, 1 yellow waiting for input, 2 red stopped (power, full output), 3 grey disabled; -1 idle. */
	int litLight()
	{
		switch(container.state)
		{
			case TileSulfuricAcidUnit.STATE_RUNNING: return 0;
			case TileSulfuricAcidUnit.STATE_NO_INPUT: return 1;
			case TileSulfuricAcidUnit.STATE_NO_POWER:
			case TileSulfuricAcidUnit.STATE_OUTPUT_FULL: return 2;
			case TileSulfuricAcidUnit.STATE_DISABLED: return 3;
			default: return -1;
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mx, int my)
	{
		GuiGraphite.panel(guiLeft, guiTop, xSize, ySize);

		// Pipes, with what moves through them while it runs
		int gasY = BURNER_Y + BURNER_H / 2;
		pipe(BURNER_X + BURNER_W, gasY, BOILER_X - 1, gasY);
		flow(BURNER_X + BURNER_W, gasY, BOILER_X - 1, gasY, COL_GAS);
		pipe(BOILER_X + BOILER_W / 2, BOILER_Y - 1, BOILER_X + BOILER_W / 2, DRUM_Y + DRUM_H);
		int steamX = DRUM_X + DRUM_W - 4;
		pipe(steamX, DRUM_Y - 1, steamX, STEAM_LINE_Y);
		pipe(steamX, STEAM_LINE_Y, STEAM_IN_X, STEAM_LINE_Y);
		pipe(STEAM_IN_X, STEAM_LINE_Y, STEAM_IN_X, TANK_Y - 1);
		flow(steamX, DRUM_Y - 1, steamX, STEAM_LINE_Y, COL_STEAM);
		flow(steamX, STEAM_LINE_Y, STEAM_IN_X, STEAM_LINE_Y, COL_STEAM);
		flow(STEAM_IN_X, STEAM_LINE_Y, STEAM_IN_X, TANK_Y - 1, COL_STEAM);
		int toConv = BOILER_Y + 10;
		pipe(BOILER_X + BOILER_W, toConv, CONV_X - 1, toConv);
		flow(BOILER_X + BOILER_W, toConv, CONV_X - 1, toConv, COL_GAS);
		int toTower = CONV_Y + CONV_H - 6;
		pipe(CONV_X + CONV_W, toTower, TOWER_X - 1, toTower);
		flow(CONV_X + CONV_W, toTower, TOWER_X - 1, toTower, COL_GAS);
		// Water: up out of its tank, over the acid tank, down into the tower's top
		pipe(WATER_OUT_X, TANK_Y - 1, WATER_OUT_X, WATER_LINE_Y);
		pipe(WATER_OUT_X, WATER_LINE_Y, WATER_DROP_X, WATER_LINE_Y);
		pipe(WATER_DROP_X, WATER_LINE_Y, WATER_DROP_X, TOWER_Y);
		flow(WATER_OUT_X, TANK_Y - 1, WATER_OUT_X, WATER_LINE_Y, COL_WATER);
		flow(WATER_OUT_X, WATER_LINE_Y, WATER_DROP_X, WATER_LINE_Y, COL_WATER);
		flow(WATER_DROP_X, WATER_LINE_Y, WATER_DROP_X, TOWER_Y, COL_WATER);
		// Acid: from the tower's foot into the arrow, up the gap beside the tower, into the acid tank's side
		int acidY = ARROW_Y + 6;
		pipe(FOOT_X, TOWER_Y + TOWER_H, FOOT_X, acidY);
		pipe(FOOT_X, acidY, ARROW_X - 1, acidY);
		pipe(ARROW_X + GuiGraphite.ARROW_W, acidY, ACID_PIPE_X, acidY);
		pipe(ACID_PIPE_X, acidY, ACID_PIPE_X, ACID_IN_Y);
		pipe(ACID_PIPE_X, ACID_IN_Y, ACID_X - 1, ACID_IN_Y);
		flow(FOOT_X, TOWER_Y + TOWER_H, FOOT_X, acidY, COL_ACID);
		flow(FOOT_X, acidY, ARROW_X - 1, acidY, COL_ACID);
		flow(ARROW_X + GuiGraphite.ARROW_W, acidY, ACID_PIPE_X, acidY, COL_ACID);
		flow(ACID_PIPE_X, acidY, ACID_PIPE_X, ACID_IN_Y, COL_ACID);
		flow(ACID_PIPE_X, ACID_IN_Y, ACID_X - 1, ACID_IN_Y, COL_ACID);

		// The wells over the pipes, so each frame covers the pipe's end where it meets it
		ClientUtils.bindTexture(GuiGraphite.SHEET);
		GlStateManager.color(1, 1, 1, 1);
		for(int[] w : new int[][]{{BURNER_X, BURNER_Y, BURNER_W, BURNER_H}, {DRUM_X, DRUM_Y, DRUM_W, DRUM_H}, {BOILER_X, BOILER_Y, BOILER_W, BOILER_H},
				{CONV_X, CONV_Y, CONV_W, CONV_H}, {TOWER_X, TOWER_Y, TOWER_W, TOWER_H}, {ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H},
				{LIGHTS_X, LIGHT_Y - 3, LIGHTS_W, 4 * LIGHT_STEP + 4}})
			GuiGraphite.well(guiLeft + w[0], guiTop + w[1], w[2], w[3]);
		GuiGraphite.arrow(guiLeft + ARROW_X, guiTop + ARROW_Y, Math.round(GuiGraphite.ARROW_W * done()));

		// IE's wooden inventory panel; IE's pieces, a little darker
		GlStateManager.color(1, 1, 1, 1);
		ClientUtils.bindTexture(IE);
		drawTexturedModalRect(guiLeft + PANEL_X, guiTop + PANEL_Y, 0, 77, 176, 89);
		GuiGraphite.ieTint();
		int[][] xy = ContainerSulfuricAcidUnit.SLOT_XY;
		for(int i = 0; i < xy.length; i++)
		{
			piece(IE_SLOT, xy[i][0] - 2, xy[i][1] - 2);
			boolean out = i == TileSulfuricAcidUnit.SLOT_ACID_OUT || i == TileSulfuricAcidUnit.SLOT_WATER_OUT || i == TileSulfuricAcidUnit.SLOT_STEAM_OUT;
			piece(out ? IE_ORANGE : IE_BLUE, xy[i][0] + 5, xy[i][1] - 5);
		}
		for(int x : new int[]{ACID_X, WATER_X, STEAM_X})
		{
			piece(IE_TANK, x, TANK_Y);
			piece(x == WATER_X ? IE_BLUE : IE_ORANGE, x + 7, TANK_Y - 3);
		}
		GuiGraphite.endTint();
		tank(container.acid, ACID_X, mx, my, null);
		tank(container.water, WATER_X, mx, my, null);
		drawSteamTank();

		drawFeed();
		drawBurner();
		drawBoiler();
		drawConverter();
		drawTower();

		// Energy (IE's red gradient), button, lights
		int eh = (int)((ENERGY_H - 4) * (container.energy / (float)TileSulfuricAcidUnit.ENERGY_CAPACITY));
		if(eh > 0)
			ClientUtils.drawGradientRect(guiLeft + ENERGY_X + 2, guiTop + ENERGY_Y + ENERGY_H - 2 - eh, guiLeft + ENERGY_X + ENERGY_W - 2, guiTop + ENERGY_Y + ENERGY_H - 2, 0xffb51500, 0xff600b00);
		GlStateManager.color(1, 1, 1, 1);
		ClientUtils.bindTexture(OWN);
		drawTexturedModalRect(guiLeft + BUTTON_X, guiTop + BUTTON_Y, 0, 150, BUTTON_SIZE, BUTTON_SIZE);
		int lit = litLight();
		for(int i = 0; i < 4; i++)
			drawTexturedModalRect(guiLeft + LIGHT_X + 1, guiTop + LIGHT_Y + i * LIGHT_STEP, i * 8, i == lit ? 90 : 98, 6, 6);
	}

	void tank(FluidTank tank, int x, int mx, int my, ArrayList<String> tip)
	{
		ClientUtils.handleGuiTank(tank, guiLeft + x + 2, guiTop + TANK_Y + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, tip);
	}

	/** Our steam, always with our own texture (in an older world another mod's steam may be the one by that name). */
	static final String STEAM_SPRITE = ImmersiveNuclear.MODID + ":blocks/fluid/steam_still";

	/** The steam tank, filled from the bottom with our steam's sprite, under IE's gauge. */
	void drawSteamTank()
	{
		int h = Math.round(47 * container.steam.getFluidAmount() / (float)TileSulfuricAcidUnit.STEAM_CAPACITY);
		if(h > 0)
		{
			TextureAtlasSprite sp = mc.getTextureMapBlocks().getAtlasSprite(STEAM_SPRITE);
			mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
			GlStateManager.color(1, 1, 1, 1);
			int x = guiLeft + STEAM_X + 2, bottom = guiTop + TANK_Y + 2 + 47;
			for(int done = 0; done < h; done += 16)
			{
				int part = Math.min(16, h - done);
				// the top piece shows the sprite's lower part, so the surface keeps its look
				float v0 = sp.getInterpolatedV(16 - part), v1 = sp.getMaxV();
				Tessellator t = Tessellator.getInstance();
				BufferBuilder b = t.getBuffer();
				b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
				int y0 = bottom - done - part, y1 = bottom - done;
				b.pos(x, y1, zLevel).tex(sp.getMinU(), v1).endVertex();
				b.pos(x + 16, y1, zLevel).tex(sp.getMaxU(), v1).endVertex();
				b.pos(x + 16, y0, zLevel).tex(sp.getMaxU(), v0).endVertex();
				b.pos(x, y0, zLevel).tex(sp.getMinU(), v0).endVertex();
				t.draw();
			}
		}
		ClientUtils.bindTexture(IE);
		GlStateManager.color(1, 1, 1, 1);
		drawTexturedModalRect(guiLeft + STEAM_X, guiTop + TANK_Y, 177, 31, 20, 51);
	}

	/** The chute from the sulfur's slot into the burner's hopper, sulfur falling while it runs. */
	void drawFeed()
	{
		int[] s = ContainerSulfuricAcidUnit.SLOT_XY[TileSulfuricAcidUnit.SLOT_SULFUR];
		int x = s[0] + 3, y = s[1] + 18, h = BURNER_Y - y;
		rect(x, y, 10, h, COL_STEEL_DARK);
		rect(x + 2, y + 2, 6, h - 2, COL_PIPE);
		if(running())
			for(int k = 0; k < 3; k++)
				rect(x + 3 + k % 2 * 2, y + 2 + Math.floorMod(ticks * 2 + k * 3, Math.max(1, h - 3)), 2, 2, COL_SULFUR);
	}

	/** Firebrick lining and the blue sulfur flame, flickering while it runs (embers only when it is out). */
	void drawBurner()
	{
		int ix = BURNER_X + 2, iy = BURNER_Y + 2, iw = BURNER_W - 4, ih = BURNER_H - 4;
		rect(ix, iy, iw, ih, COL_BRICK);
		for(int j = iy; j < iy + ih; j += 4)
		{
			rect(ix, j, iw, 1, COL_MORTAR);
			for(int i = ix + ((j - iy) / 4 % 2 == 0 ? 0 : 4); i < ix + iw; i += 8)
				rect(i, j, 1, 4, COL_MORTAR);
		}
		int fx = ix + 4, fy = iy + 4, fw = iw - 8, fh = ih - 8;
		rect(fx, fy, fw, fh, COL_FIREBOX);
		float strength = running() ? 1 : 0.25F;
		for(int i = 0; i < fw; i++)
		{
			// the flame's height wobbles along its length
			float wob = running() ? 0.15F * (float)Math.sin((ticks + i * 3) * 0.6) : 0;
			for(int j = 0; j < fh; j++)
			{
				float d = Math.abs(j - fh / 2F) / (fh / 2F);
				float k = Math.max(0, 1 - d * (1.3F - wob)) * Math.min(1, (i + 2) / (fw * 0.35F)) * Math.min(1, (fw - i) / (fw * 0.25F)) * strength;
				if(k > 0.05F)
				{
					int r = Math.round(40 + 150 * k * k), g = Math.round(70 + 160 * k * k), b = Math.round(160 + 95 * k);
					rect(fx + i, fy + j, 1, 1, 0xff000000 | r << 16 | g << 8 | b);
				}
			}
		}
		rect(BURNER_X - 4, BURNER_Y + BURNER_H / 2 - 4, 4, 8, COL_STEEL_DARK);
	}

	/** A colour from dark to light by t (0..1), opaque. */
	static int shade(int dark, int light, float t)
	{
		t = Math.max(0, Math.min(1, t));
		int c = 0xff000000;
		for(int sh = 0; sh <= 16; sh += 8)
			c |= Math.round((dark >> sh & 255) + ((light >> sh & 255) - (dark >> sh & 255)) * t) << sh;
		return c;
	}

	/** The boiler's fire tubes and the steam drum: water below, steam above, two safety valves. While it runs the tubes
	 * glow, hotter at the bottom where the gas comes in, with a wave of heat rising through them, and bubbles rise
	 * between them; the water in the drum ripples. */
	void drawBoiler()
	{
		boolean on = running();
		rect(BOILER_X + 2, BOILER_Y + 2, BOILER_W - 4, BOILER_H - 4, COL_INSIDE);
		int top = BOILER_Y + 6, bottom = BOILER_Y + BOILER_H - 4;
		for(int j = top; j < bottom; j += 4)
		{
			float heat = 0;
			if(on)
			{
				float depth = (j - top) / (float)(bottom - top);
				float wave = 0.5F + 0.5F * (float)Math.sin((ticks * 0.25) + depth * 6);
				heat = 0.25F + 0.45F * depth + 0.3F * wave * depth;
			}
			rect(BOILER_X + 4, j, BOILER_W - 8, 2, shade(COL_TUBE, 0xffe8783c, heat));
		}
		if(on)
			for(int k = 0; k < 3; k++)
			{
				int span = bottom - top - 4, y = bottom - 3 - Math.floorMod(ticks + k * span / 3, span);
				if((y - top) % 4 >= 2)
					rect(BOILER_X + 5 + k * 5, y, 1, 1, 0xffb4c8e0);
			}
		int dx = DRUM_X + 2, dy = DRUM_Y + 2, dw = DRUM_W - 4, dh = DRUM_H - 4;
		rect(dx, dy, dw, dh, COL_INSIDE);
		rect(dx, dy + dh / 2, dw, dh - dh / 2, COL_WATER);
		if(on)
			for(int i = 0; i < dw; i++)
				if(Math.sin(i * 0.9 + ticks * 0.3) > 0.4)
					rect(dx + i, dy + dh / 2 - 1, 1, 1, COL_WATER);
		if(running())
			for(int k = 0; k < 3; k++)
				rect(dx + 4 + k * 8 + (ticks / 3 + k) % 3, dy + 1, 3, 1, COL_STEAM);
		rect(DRUM_X + 4, DRUM_Y - 3, 3, 3, COL_STEEL_DARK);
		rect(DRUM_X + DRUM_W - 10, DRUM_Y - 3, 3, 3, COL_STEEL_DARK);
	}

	/** Four catalyst beds, hottest at the top; they glow while it runs. The gas goes down through them: each bed
	 * brightens as the gas passes, the catalyst sparkles, and the gas drifts down in the gaps between the beds. */
	void drawConverter()
	{
		boolean on = running();
		rect(CONV_X + 2, CONV_Y + 2, CONV_W - 4, CONV_H - 4, COL_INSIDE);
		int step = (CONV_H - 12) / 4, bedH = 10, cycle = 4 * 20;
		for(int k = 0; k < 4; k++)
		{
			int y0 = CONV_Y + 6 + k * step;
			// the pass of the gas: a pulse that reaches each bed a quarter of the cycle after the one above
			float pass = on ? Math.max(0, (float)Math.cos(2 * Math.PI * (Math.floorMod(ticks - k * 20, cycle) / (float)cycle))) : 0;
			float hot = (1 - k / 4F) * (on ? 0.85F + 0.15F * pass : 0.4F);
			int r = Math.min(255, Math.round(90 + 120 * hot)), g = Math.min(255, Math.round(60 + 40 * hot)), b = 30;
			rect(CONV_X + 3, y0, CONV_W - 6, bedH, 0xff000000 | r << 16 | g << 8 | b);
			for(int i = CONV_X + 4; i < CONV_X + CONV_W - 4; i += 3)
			{
				int sy = y0 + 2 + i % 5;
				boolean spark = on && Math.floorMod(ticks / 3 + i * 7 + k * 13, 11) == 0;
				rect(i, sy, 1, 1, spark ? 0xfffff0b0 : 0xff000000 | Math.min(255, r + 50) << 16 | Math.min(255, g + 40) << 8 | 50);
			}
			rect(CONV_X + 3, y0 + bedH, CONV_W - 6, 1, COL_STEEL);
			// the gas in the gap under the bed, drifting down
			int gap = step - bedH - 1;
			if(on && gap > 1)
				for(int i = CONV_X + 4 + Math.floorMod(ticks / 4 + k, 3); i < CONV_X + CONV_W - 4; i += 3)
					rect(i, y0 + bedH + 1 + Math.floorMod(ticks / 2 + i, gap), 1, 1, COL_GAS);
		}
	}

	/** The packing, the acid trickling down it while it runs, the acid at the foot and the stack on top. */
	void drawTower()
	{
		rect(TOWER_X + 2, TOWER_Y + 2, TOWER_W - 4, TOWER_H - 4, 0xff222428);
		for(int j = TOWER_Y + 14; j < TOWER_Y + TOWER_H - 10; j += 3)
			for(int i = TOWER_X + 3 + j % 2; i < TOWER_X + TOWER_W - 3; i += 3)
				rect(i, j, 2, 2, COL_PACKING);
		if(running())
			for(int i = TOWER_X + 4; i < TOWER_X + TOWER_W - 4; i += 4)
				rect(i, TOWER_Y + 6 + Math.floorMod(ticks * 2 + i * 5, TOWER_H - 18), 1, 3, COL_ACID);
		rect(TOWER_X + 2, TOWER_Y + TOWER_H - 10, TOWER_W - 4, 8, COL_ACID);
		rect(TOWER_X + TOWER_W / 2 - 1, TOWER_Y - 4, 3, 4, COL_STEEL_DARK);
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

	static final String K = "gui.immersivenuclear.sulfuric_acid_unit.";

	@Override
	public void drawScreen(int mx, int my, float partialTicks)
	{
		super.drawScreen(mx, my, partialTicks);
		ArrayList<String> tip = new ArrayList<>();
		String[] tanks = {"acid", "water"};
		int[] tx = {ACID_X, WATER_X};
		FluidTank[] tt = {container.acid, container.water};
		for(int i = 0; i < 2; i++)
		{
			tank(tt[i], tx[i], mx, my, tip);
			if(over(mx, my, tx[i], TANK_Y, 20, 51))
				tip.add(0, I18n.format(K + tanks[i]));
		}
		if(over(mx, my, STEAM_X, TANK_Y, 20, 51))
		{
			tip.add(I18n.format(K + "steam"));
			tip.add(I18n.format("fluid.immersivenuclear.steam"));
			tip.add("§7" + container.steam.getFluidAmount() + "/" + TileSulfuricAcidUnit.STEAM_CAPACITY + " mB");
			if(container.venting)
				tip.add("§e" + I18n.format(K + "venting"));
		}
		if(over(mx, my, BURNER_X, BURNER_Y, BURNER_W, BURNER_H))
		{
			tip.add(I18n.format(K + "burner"));
			tip.add("§7" + I18n.format(K + "burner_info"));
		}
		if(over(mx, my, BOILER_X, BOILER_Y, BOILER_W, BOILER_H) || over(mx, my, DRUM_X, DRUM_Y, DRUM_W, DRUM_H))
		{
			tip.add(I18n.format(K + "boiler"));
			tip.add("§7" + I18n.format(K + "boiler_info"));
		}
		if(over(mx, my, CONV_X, CONV_Y, CONV_W, CONV_H))
		{
			tip.add(I18n.format(K + "converter"));
			tip.add("§7" + I18n.format(K + "converter_info"));
		}
		if(over(mx, my, TOWER_X, TOWER_Y, TOWER_W, TOWER_H))
		{
			tip.add(I18n.format(K + "tower"));
			tip.add("§7" + I18n.format(K + "tower_info"));
		}
		if(over(mx, my, ARROW_X, ARROW_Y, GuiGraphite.ARROW_W, GuiGraphite.ARROW_H))
		{
			tip.add(I18n.format(K + "cycle", TileSulfuricAcidUnit.WATER_PER_CYCLE, TileSulfuricAcidUnit.ACID_PER_CYCLE,
					TileSulfuricAcidUnit.STEAM_PER_CYCLE, TileSulfuricAcidUnit.CYCLE_TICKS / 20));
			if(container.progress > 0)
				tip.add("§7" + I18n.format(K + "progress", Math.round(100 * done())));
		}
		String[] slotTips = {"sulfur", "acid_in", "acid_out", "water_in", "water_out", "steam_in", "steam_out"};
		int[][] xy = ContainerSulfuricAcidUnit.SLOT_XY;
		for(int i = 0; i < xy.length; i++)
			if(over(mx, my, xy[i][0], xy[i][1], 16, 16) && container.getSlot(i).getStack().isEmpty())
				tip.add(I18n.format(K + slotTips[i]));
		if(over(mx, my, ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H))
		{
			tip.add(container.energy + "/" + TileSulfuricAcidUnit.ENERGY_CAPACITY + " IF");
			tip.add("§7" + I18n.format(K + "energy", TileSulfuricAcidUnit.ENERGY_PER_TICK));
		}
		if(over(mx, my, BUTTON_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE))
		{
			tip.add(I18n.format("gui.immersivenuclear.redstone"));
			tip.add("§7" + I18n.format(container.redstoneInverted ? "gui.immersivenuclear.redstone.inverted" : "gui.immersivenuclear.redstone.normal"));
		}
		for(int i = 0; i < 4; i++)
			if(over(mx, my, LIGHTS_X, LIGHT_Y + i * LIGHT_STEP - 1, LIGHTS_W, LIGHT_STEP))
			{
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
			mc.playerController.sendEnchantPacket(inventorySlots.windowId, ContainerSulfuricAcidUnit.BUTTON_REDSTONE);
		}
	}
}
