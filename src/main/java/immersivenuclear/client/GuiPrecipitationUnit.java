package immersivenuclear.client;

import java.util.ArrayList;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.GuiIEContainerBase;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TilePrecipitationUnit;
import immersivenuclear.common.gui.ContainerPrecipitationUnit;
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
import net.minecraftforge.fml.client.config.GuiUtils;

/**
 * Interface of the Precipitation Unit (prototype), in the mod's graphite finish drawn at run time ({@link GuiGraphite}:
 * plate, wells, arrow) with IE's own slots, tanks and markers a little darker, and the same right column as the other
 * machines (energy, redstone button, lights). Left to right, as the batch goes:
 *   solution buckets > AGITATED TANK (agent funnel on top) > SCREW (concentrate on top) > effluent tank | effluent buckets
 * The solution goes straight into the agitated tank (port or bucket); the tank's level is the batch.
 * In the tank: the batch rising while it fills, the agitator turning and the solid clouding the liquid while it
 * precipitates, the level going down while the screw takes it. In the screw: the flights turning, the solid climbing to
 * the discharge head and the liquid running back down to the effluent pipe.
 */
public class GuiPrecipitationUnit extends GuiIEContainerBase
{
	static final String IE = "immersiveengineering:textures/gui/refinery.png";
	static final String OWN = ImmersiveNuclear.MODID + ":textures/gui/chemical_processor.png";

	// Layout
	static final int EFFLUENT_X = 218, TANK_Y = 38;
	/** The agitated tank's well; its liquid starts this far below the top (the lid). */
	public static final int MIXER_X = 44, MIXER_Y = 34, MIXER_W = 80, MIXER_H = 76;
	static final int MIXER_LID = 8;
	/**
	 * The agitator as modelled (group "rotor"): three levels of four blades on the shaft, at these heights of the
	 * tank's inside from its floor (Blockbench Y 4.9, 5.9, 6.9 in 4.5..7.5); each blade reaches 1.875 of the inside's 5.6
	 * units of width.
	 */
	static final float[] BLADE_LEVELS = {0.133F, 0.467F, 0.8F};
	static final float BLADE_REACH = 1.875F / 5.6F;
	static final int BLADES = 4;
	/** The screw's well; the screw climbs from its lower left corner to its upper right one. */
	static final int SCREW_X = 154, SCREW_Y = 34, SCREW_W = 56, SCREW_H = 76;
	static final int ARROW_X = 130, ARROW_Y = 64;
	/** Pipes: solution from its bucket slot into the tank, slurry from the tank's bottom to the screw's foot, effluent out. */
	static final int PIPE_Y = ContainerPrecipitationUnit.SLOT_XY[TilePrecipitationUnit.SLOT_SOLUTION_IN][1] + 8, PIPE_X = ContainerPrecipitationUnit.SLOT_XY[TilePrecipitationUnit.SLOT_SOLUTION_IN][0] + 18,
			SLURRY_Y = MIXER_Y + MIXER_H - 8;
	static final int ENERGY_X = 248, ENERGY_Y = 16, ENERGY_W = 14, ENERGY_H = 64;
	static final int BUTTON_X = 247, BUTTON_Y = 86, BUTTON_SIZE = 16;
	static final int LIGHTS_X = 248, LIGHTS_W = 14, LIGHT_X = 251, LIGHT_Y = 112, LIGHT_STEP = 8;
	static final int PANEL_X = ContainerPrecipitationUnit.INV_X - 8, PANEL_Y = ContainerPrecipitationUnit.INV_Y - 8;

	// IE's pieces in refinery.png
	static final int[] IE_SLOT = {35, 13, 20, 20}, IE_TANK = {11, 18, 20, 51}, IE_BLUE = {42, 10, 6, 3}, IE_ORANGE = {42, 49, 6, 3};

	static final int COL_PIPE = 0xff2a2c30, COL_PIPE_EDGE = 0xff55585e, COL_SOLUTION = 0xffd8c860, COL_SLURRY = 0xffc8a830, COL_EFFLUENT = 0xff706a58,
			COL_SOLID = 0xffe0c040, COL_STEEL = 0xff96999e, COL_STEEL_DARK = 0xff5c5f64;

	final ContainerPrecipitationUnit container;
	/** Client counters for the liquid's specks and the screw. */
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
		if(precipitating() || dewatering())
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

	/** Part of a fluid's still sprite (tiled from the top left), tinted; binds the blocks atlas. */
	void fluid(Fluid f, int x, int y, int w, int h, int argb)
	{
		if(f == null || w <= 0 || h <= 0)
			return;
		mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		TextureAtlasSprite sp = mc.getTextureMapBlocks().getAtlasSprite(f.getStill().toString());
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		GlStateManager.color((argb >> 16 & 255) / 255F, (argb >> 8 & 255) / 255F, (argb & 255) / 255F, (argb >>> 24) / 255F);
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

	/** 0 green running, 1 yellow waiting for input, 2 red stopped (power, full output), 3 grey disabled; -1 idle. */
	int litLight()
	{
		switch(container.state)
		{
			case TilePrecipitationUnit.STATE_RUNNING: return 0;
			case TilePrecipitationUnit.STATE_NO_INPUT: return 1;
			case TilePrecipitationUnit.STATE_NO_POWER:
			case TilePrecipitationUnit.STATE_OUTPUT_FULL: return 2;
			case TilePrecipitationUnit.STATE_DISABLED: return 3;
			default: return -1;
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mx, int my)
	{
		// The graphite plate, the wells of the tank, the screw and the right column
		GuiGraphite.panel(guiLeft, guiTop, xSize, ySize);
		GuiGraphite.well(guiLeft + MIXER_X, guiTop + MIXER_Y, MIXER_W, MIXER_H);
		GuiGraphite.well(guiLeft + SCREW_X, guiTop + SCREW_Y, SCREW_W, SCREW_H);
		GuiGraphite.well(guiLeft + ENERGY_X, guiTop + ENERGY_Y, ENERGY_W, ENERGY_H);
		GuiGraphite.well(guiLeft + LIGHTS_X, guiTop + LIGHT_Y - 3, LIGHTS_W, 4 * LIGHT_STEP + 4);
		GuiGraphite.arrow(guiLeft + ARROW_X, guiTop + ARROW_Y, Math.round(GuiGraphite.ARROW_W * precipitated()));

		// Pipes: solution slot > agitated tank; tank bottom > screw foot; screw foot > effluent tank
		pipe(PIPE_X, PIPE_Y, MIXER_X - 1, PIPE_Y);
		pipe(MIXER_X + MIXER_W, SLURRY_Y, SCREW_X - 1, SLURRY_Y);
		int[][] drain = effluentPipe();
		for(int i = 1; i < drain.length; i++)
			pipe(drain[i - 1][0], drain[i - 1][1], drain[i][0], drain[i][1]);
		if(filling())
			flow(PIPE_X, PIPE_Y, MIXER_X - 1, PIPE_Y, COL_SOLUTION);
		if(dewatering())
		{
			flow(MIXER_X + MIXER_W, SLURRY_Y, SCREW_X - 1, SLURRY_Y, COL_SLURRY);
			for(int i = 1; i < drain.length; i++)
				flow(drain[i - 1][0], drain[i - 1][1], drain[i][0], drain[i][1], COL_EFFLUENT);
		}

		// IE's wooden inventory panel; IE's pieces, a little darker
		GlStateManager.color(1, 1, 1, 1);
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
		piece(IE_TANK, EFFLUENT_X, TANK_Y);
		piece(IE_ORANGE, EFFLUENT_X + 7, TANK_Y - 3);
		GuiGraphite.endTint();
		ClientUtils.handleGuiTank(container.effluent, guiLeft + EFFLUENT_X + 2, guiTop + TANK_Y + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);

		drawMixer(partialTicks);
		drawScrew();

		// Energy (IE's red gradient), button, lights
		int eh = (int)((ENERGY_H - 4) * (container.energy / (float)TilePrecipitationUnit.ENERGY_CAPACITY));
		if(eh > 0)
			ClientUtils.drawGradientRect(guiLeft + ENERGY_X + 2, guiTop + ENERGY_Y + ENERGY_H - 2 - eh, guiLeft + ENERGY_X + ENERGY_W - 2, guiTop + ENERGY_Y + ENERGY_H - 2, 0xffb51500, 0xff600b00);
		GlStateManager.color(1, 1, 1, 1);
		ClientUtils.bindTexture(OWN);
		drawTexturedModalRect(guiLeft + BUTTON_X, guiTop + BUTTON_Y, 0, 150, BUTTON_SIZE, BUTTON_SIZE);
		int lit = litLight();
		for(int i = 0; i < 4; i++)
			drawTexturedModalRect(guiLeft + LIGHT_X + 1, guiTop + LIGHT_Y + i * LIGHT_STEP, i * 8, i == lit ? 90 : 98, 6, 6);
	}

	/** The effluent's way: down from the screw's foot, under the screw, up beside it into the effluent tank's side. */
	static int[][] effluentPipe()
	{
		int bottom = SCREW_Y + SCREW_H + 6, side = SCREW_X + SCREW_W + 4, entry = TANK_Y + 40;
		return new int[][]{{SCREW_X + 6, SCREW_Y + SCREW_H}, {SCREW_X + 6, bottom}, {side, bottom}, {side, entry}, {EFFLUENT_X - 1, entry}};
	}

	/** The batch in the agitated tank: the solution, clouding yellow as the uranium precipitates; the agitator. */
	void drawMixer(float partialTicks)
	{
		int x = MIXER_X + 2, w = MIXER_W - 4, top = MIXER_Y + 2 + MIXER_LID, floor = MIXER_Y + MIXER_H - 2;
		int lh = Math.round((floor - top) * Math.min(1, container.mixer / (float)TilePrecipitationUnit.BATCH_SOLUTION));
		int surface = floor - lh;
		float p = precipitated();
		if(lh > 0)
		{
			fluid(INContent.fluidPurifiedSolution, x, surface, w, lh, 0xffffffff);
			rect(x, surface, w, lh, (int)(150 * p) << 24 | COL_SOLID & 0xffffff);
			rect(x, surface, w, 1, 0x46ffffff);
			// Solid specks swirling while it precipitates
			if(precipitating() && lh > 8)
				for(int k = 0; k < 12; k++)
				{
					int sx = x + 2 + Math.floorMod(k * 29 + turn * 2 * (k % 2 == 0 ? 1 : -1), w - 6);
					int sy = surface + 3 + (k * 11 + ticks / 3) % Math.max(1, lh - 6);
					rect(sx, sy, 2, 2, 0xc0f0d050);
				}
		}
		// The lid with the agent's funnel and the motor, the shaft down to the lowest blades
		rect(MIXER_X + 2, MIXER_Y + 2, MIXER_W - 4, MIXER_LID - 2, COL_STEEL_DARK);
		int cx = MIXER_X + MIXER_W / 2;
		int lowest = floor - Math.round(BLADE_LEVELS[0] * (floor - top));
		rect(cx - 1, MIXER_Y + 2, 2, lowest - MIXER_Y, COL_STEEL);
		drawBlades(cx, top, floor, w, rotorAngle + rotorSpeed * TilePrecipitationUnit.ROTOR_DEGREES_PER_TICK * partialTicks);
		// Funnel from the agent's slot down to the lid
		int[] agent = ContainerPrecipitationUnit.SLOT_XY[TilePrecipitationUnit.SLOT_AGENT];
		rect(agent[0] + 4, agent[1] + 18, 8, MIXER_Y - agent[1] - 18, COL_STEEL_DARK);
		if(container.phase == TilePrecipitationUnit.PHASE_PRECIPITATE && container.phaseTicks < 20 && running())
			for(int k = 0; k < 3; k++)
				rect(agent[0] + 6 + k, MIXER_Y + MIXER_LID + Math.floorMod(ticks * 3 + k * 9, Math.max(1, surface - MIXER_Y - MIXER_LID)), 2, 2, 0xffe8e0d0);
	}

	/**
	 * The four blades of each level seen from the side as they turn: each reaches out by cos of its angle, the ones
	 * turned towards the viewer light, the ones behind the shaft dark and drawn first.
	 */
	void drawBlades(int cx, int top, int floor, int w, float angle)
	{
		float reach = BLADE_REACH * w;
		for(float level : BLADE_LEVELS)
		{
			int y = floor - Math.round(level * (floor - top)) - 1;
			for(int pass = 0; pass < 2; pass++)
				for(int k = 0; k < BLADES; k++)
				{
					double a = Math.toRadians(angle + k * 360.0 / BLADES);
					boolean front = Math.sin(a) > 0;
					if(front != (pass == 1))
						continue;
					int tip = (int)Math.round(Math.cos(a) * reach);
					rect(Math.min(cx, cx + tip), y, Math.abs(tip) + 1, 3, front ? COL_STEEL : COL_STEEL_DARK);
				}
			rect(cx - 1, y - 1, 2, 5, COL_STEEL);
		}
	}

	/** The screw, a diagonal of flights from the foot (lower left) to the discharge head (upper right). */
	void drawScrew()
	{
		int x0 = SCREW_X + 6, y0 = SCREW_Y + SCREW_H - 8, x1 = SCREW_X + SCREW_W - 8, y1 = SCREW_Y + 8;
		int n = 14;
		float d = dewatered();
		for(int i = 0; i <= n; i++)
		{
			float t = i / (float)n;
			int px = Math.round(x0 + (x1 - x0) * t), py = Math.round(y0 + (y1 - y0) * t);
			rect(px - 3, py - 3, 7, 7, COL_STEEL_DARK);
			// The flights: every other one lit, stepping with the turn
			boolean lit = (i + (dewatering() ? turn / 2 : 0)) % 2 == 0;
			rect(px - 2, py - 2, 5, 5, lit ? COL_STEEL : COL_STEEL_DARK);
		}
		if(dewatering())
		{
			// Solid climbing to the head; liquid running down to the foot
			for(int k = 0; k < 5; k++)
			{
				float t = Math.floorMod(ticks + k * 9, 45) / 45F;
				rect(Math.round(x0 + (x1 - x0) * t) - 1, Math.round(y0 + (y1 - y0) * t) - 5, 3, 3, COL_SOLID);
			}
			for(int k = 0; k < 3; k++)
			{
				float t = 1 - Math.floorMod(ticks * 2 + k * 13, 40) / 40F;
				rect(Math.round(x0 + (x1 - x0) * t * 0.6F) + 2, Math.round(y0 + (y1 - y0) * t * 0.6F) + 2, 2, 2, COL_EFFLUENT);
			}
		}
		// Discharge head up to the concentrate slot
		int[] out = ContainerPrecipitationUnit.SLOT_XY[TilePrecipitationUnit.SLOT_CONCENTRATE];
		rect(out[0] + 4, out[1] + 18, 8, SCREW_Y - out[1] - 18, COL_STEEL_DARK);
		if(d > 0.95F)
			rect(out[0] + 6, out[1] + 18, 4, SCREW_Y - out[1] - 18, COL_SOLID);
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

	static final String K = "gui.immersivenuclear.precipitation_unit.";

	@Override
	public void drawScreen(int mx, int my, float partialTicks)
	{
		super.drawScreen(mx, my, partialTicks);
		ArrayList<String> tip = new ArrayList<>();
		ClientUtils.handleGuiTank(container.effluent, guiLeft + EFFLUENT_X + 2, guiTop + TANK_Y + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, tip);
		if(over(mx, my, EFFLUENT_X, TANK_Y, 20, 51))
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
		}
		if(over(mx, my, SCREW_X, SCREW_Y, SCREW_W, SCREW_H))
		{
			tip.add(I18n.format(K + "screw"));
			if(container.phase == TilePrecipitationUnit.PHASE_DEWATER)
				tip.add("§7" + I18n.format(K + "progress", Math.round(100 * dewatered())));
		}
		if(over(mx, my, ARROW_X, ARROW_Y, GuiGraphite.ARROW_W, GuiGraphite.ARROW_H))
			tip.add(I18n.format(K + "batch", TilePrecipitationUnit.BATCH_SOLUTION, TilePrecipitationUnit.AGENT_PER_BATCH,
					TilePrecipitationUnit.CONCENTRATE_PER_BATCH, TilePrecipitationUnit.EFFLUENT_PER_BATCH));
		int[] agent = ContainerPrecipitationUnit.SLOT_XY[TilePrecipitationUnit.SLOT_AGENT];
		if(over(mx, my, agent[0], agent[1], 16, 16) && container.getSlot(TilePrecipitationUnit.SLOT_AGENT).getStack().isEmpty())
			tip.add(I18n.format(K + "agent", TilePrecipitationUnit.AGENT_PER_BATCH));
		int[] out = ContainerPrecipitationUnit.SLOT_XY[TilePrecipitationUnit.SLOT_CONCENTRATE];
		if(over(mx, my, out[0], out[1], 16, 16) && container.getSlot(TilePrecipitationUnit.SLOT_CONCENTRATE).getStack().isEmpty())
			tip.add(I18n.format(K + "concentrate"));
		if(over(mx, my, ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H))
			tip.add(container.energy + "/" + TilePrecipitationUnit.ENERGY_CAPACITY + " IF");
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
			mc.playerController.sendEnchantPacket(inventorySlots.windowId, ContainerPrecipitationUnit.BUTTON_REDSTONE);
		}
	}
}
