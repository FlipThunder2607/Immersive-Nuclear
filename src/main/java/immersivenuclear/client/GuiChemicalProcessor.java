package immersivenuclear.client;

import java.util.ArrayList;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.GuiIEContainerBase;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileChemicalProcessor;
import immersivenuclear.common.gui.ContainerChemicalProcessor;
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
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.client.config.GuiUtils;

/**
 * Interface of the Chemical Processing Unit, in the style of IE's Mixer (the user's layout, 01/11): separate riveted
 * panels with the machine itself drawn between them, the world showing through the gaps.
 *   [recycled liquor | water: container in / tank / container out]   the VAT (the hopper with the solids slots and the
 *   green agitator motor on its lid, the batch in its big window, standing on the machine's steel base)
 *   [slurry: in / tank / out | energy over the status lamp]
 * and IE's wooden inventory panel under it. In the window: the water (or the recycled liquor) running in from the
 * inlet and the solids falling from the hopper while it fills, the impellers turning while it reacts and the water
 * turning into slurry, the level going down while it drains; one lamp whose colour is the state. Our sheet
 * (textures/gui/chemical_processor_gui.png, made by ferramentas/gui_processamento_v10_esboco.py --jogo with the same
 * layout) holds the background and those sprites; IE's slots, tanks, marks and inventory come from IE's own sheet.
 * Redstone: a signal stops the machine, the hammer on the trigger inverts that (no button: the user's choice).
 */
public class GuiChemicalProcessor extends GuiIEContainerBase
{
	static final String IE = "immersiveengineering:textures/gui/refinery.png";
	static final String SHEET = ImmersiveNuclear.MODID + ":textures/gui/chemical_processor_gui.png";
	static final int SHEET_SIZE = 512;

	// Layout (same as gui_processamento_v10_esboco.py)
	static final int ROW1 = 37, ROW2 = 62, ROW3 = 117;
	static final int RECYCLED_X = 8, WATER_X = 38, SLURRY_X = 216, CONTROL_X = 242;
	static final int VX = 78, VY = 46, VW = 118, VH = 90;
	/** The vat's window; also JEI's click area for the recipe (INJeiPlugin). */
	public static final int REACTOR_X = 86, REACTOR_Y = 52, REACTOR_W = 102, REACTOR_H = 56;
	static final int SHAFT_X = VX + VW / 2;
	static final int HOPPER_X = 84, HOPPER_Y = 0, HOPPER_W = 50, HOPPER_H = 28, FUNNEL_X = HOPPER_X + HOPPER_W / 2;
	static final int MOTOR_X = 129, MOTOR_Y = 21, MOTOR_W = 56, MOTOR_H = 25;    // gearbox, coupling and the green motor on the lid
	/** The pipes' cores: water in at the tanks' middle, slurry out low. */
	static final int PIPE_W_Y = 86, PIPE_W_X0 = 62, PIPE_W_X1 = 80, PIPE_S_Y = 126, PIPE_S_X0 = 194, PIPE_S_X1 = 211;
	/** A full batch fills this much of the window. */
	static final float BATCH_LEVEL = 0.8F;
	static final int ENERGY_X = CONTROL_X + 2, ENERGY_Y = ROW1, ENERGY_W = 11, ENERGY_H = ROW2 + 51 - ROW1;
	static final int LAMP_X = CONTROL_X, LAMP_Y = ROW3 + 2, LAMP_SIZE = 16;
	static final int PANEL_X = ContainerChemicalProcessor.INV_X - 8, PANEL_Y = ContainerChemicalProcessor.INV_Y - 8;

	// Our sprites in the sheet: {u, v, w, h}
	static final int[] IMPELLER_LOW = {0, 260, 40, 4}, IMPELLER_HIGH = {0, 268, 24, 4}, LENS = {0, 280, 10, 10};
	// IE's pieces in refinery.png
	static final int[] IE_SLOT = {35, 13, 20, 20}, IE_TANK = {11, 18, 20, 51}, IE_BLUE = {42, 10, 6, 3}, IE_ORANGE = {42, 49, 6, 3};

	static final int COL_CONDENSATE = 0xffb4c4d0, COL_EFFLUENT = 0xff706a58, COL_WATER = 0xff3f76e4, COL_SLURRY = 0xff968060, COL_SHAFT = 0xff96999e,
			COL_SHAFT_LIGHT = 0xffc8ccd0, COL_DUST = 0xff8a9a6a;

	final ContainerChemicalProcessor container;
	/** Client counters: the agitator's turn, and the ticks for the swirl and the falling solids. */
	int turn, ticks;

	public GuiChemicalProcessor(ContainerChemicalProcessor container)
	{
		super(container);
		this.container = container;
		xSize = ContainerChemicalProcessor.WIDTH;
		ySize = ContainerChemicalProcessor.HEIGHT;
	}

	boolean running()
	{
		return container.state == TileChemicalProcessor.STATE_RUNNING;
	}

	boolean agitating()
	{
		return running() && container.phase == TileChemicalProcessor.PHASE_REACT;
	}

	boolean filling()
	{
		return (container.moving & 1) != 0;
	}

	/** Filling with the recycled liquor (the first part of a batch that takes it), not the water. */
	boolean fillingRecycled()
	{
		return filling() && container.reactor < container.batchRecycled;
	}

	boolean draining()
	{
		return (container.moving & 2) != 0;
	}

	/** How far the batch has turned from water into slurry, 0..1. */
	float reacted()
	{
		if(container.phase == TileChemicalProcessor.PHASE_DRAIN)
			return 1;
		return container.phase == TileChemicalProcessor.PHASE_REACT ? Math.min(1, container.phaseTicks / (float)TileChemicalProcessor.REACT_TICKS) : 0;
	}

	@Override
	public void updateScreen()
	{
		super.updateScreen();
		ticks++;
		if(agitating())
			turn++;
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

	/** The lamp's colour: 0 green running, 1 amber waiting (no input, or stopped by redstone), 2 red stopped (no power,
	 * output full), 3 off (idle). */
	int lamp()
	{
		switch(container.state)
		{
			case TileChemicalProcessor.STATE_RUNNING: return 0;
			case TileChemicalProcessor.STATE_NO_INPUT:
			case TileChemicalProcessor.STATE_DISABLED: return 1;
			case TileChemicalProcessor.STATE_NO_POWER:
			case TileChemicalProcessor.STATE_OUTPUT_FULL: return 2;
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
		int[][] xy = ContainerChemicalProcessor.SLOT_XY;
		for(int i = 0; i < xy.length; i++)
		{
			piece(IE_SLOT, xy[i][0] - 2, xy[i][1] - 2);
			boolean in = i == TileChemicalProcessor.SLOT_URANIUM || i == TileChemicalProcessor.SLOT_REAGENT || i == TileChemicalProcessor.SLOT_WATER_IN
					|| i == TileChemicalProcessor.SLOT_SLURRY_IN || i == TileChemicalProcessor.SLOT_RECYCLED_IN;
			piece(in ? IE_BLUE : IE_ORANGE, xy[i][0] + 5, xy[i][1] - 5);
		}
		for(int x : new int[]{RECYCLED_X, WATER_X, SLURRY_X})
		{
			piece(IE_TANK, x, ROW2);
			piece(x == SLURRY_X ? IE_ORANGE : IE_BLUE, x + 7, ROW2 - 3);
		}
		GuiGraphite.endTint();

		// Fluid moving in the pipes
		if(filling())
			rect(PIPE_W_X0, PIPE_W_Y, PIPE_W_X1 - PIPE_W_X0, 2, fillingRecycled() ? (container.recycledType == 3 ? COL_CONDENSATE : COL_EFFLUENT) : COL_WATER);
		if(draining())
			rect(PIPE_S_X0, PIPE_S_Y, PIPE_S_X1 - PIPE_S_X0, 2, COL_SLURRY);
		GlStateManager.color(1, 1, 1, 1);

		ClientUtils.handleGuiTank(container.recycled, guiLeft + RECYCLED_X + 2, guiTop + ROW2 + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);
		ClientUtils.handleGuiTank(container.water, guiLeft + WATER_X + 2, guiTop + ROW2 + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);
		ClientUtils.handleGuiTank(container.slurry, guiLeft + SLURRY_X + 2, guiTop + ROW2 + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);

		drawBatch();

		// Energy (IE's red gradient, inside IE's 7 px) and the lamp's lens
		int eh = (int)((ENERGY_H - 4) * (container.energy / (float)TileChemicalProcessor.ENERGY_CAPACITY));
		if(eh > 0)
			ClientUtils.drawGradientRect(guiLeft + ENERGY_X + 2, guiTop + ENERGY_Y + ENERGY_H - 2 - eh, guiLeft + ENERGY_X + ENERGY_W - 2, guiTop + ENERGY_Y + ENERGY_H - 2, 0xffb51500, 0xff600b00);
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.enableBlend();
		ClientUtils.bindTexture(SHEET);
		sprite(LENS, lamp() * LENS[2], LAMP_X + LAMP_SIZE / 2 - 5, LAMP_Y + LAMP_SIZE / 2 - 5);
	}

	/** The batch in the window, the stream and the solids while it fills, the shaft and the impellers. */
	void drawBatch()
	{
		int floor = REACTOR_Y + REACTOR_H;
		int lh = Math.round(REACTOR_H * BATCH_LEVEL * Math.min(1, container.reactor / (float)TileChemicalProcessor.BATCH_WATER));
		int surface = floor - lh;
		float reacted = reacted();
		if(lh > 0)
		{
			// The water, the slurry over it as the reaction goes on, a light line at the surface
			if(reacted < 1)
				fluid(FluidRegistry.WATER, REACTOR_X, surface, REACTOR_W, lh, COL_WATER);
			if(reacted > 0)
				fluid(INContent.fluidSlurry, REACTOR_X, surface, REACTOR_W, lh, (int)(255 * reacted) << 24 | 0xffffff);
			rect(REACTOR_X, surface, REACTOR_W, 1, 0x46ffffff);
			// The swirl round the impellers, moving with them
			if(agitating() && lh > 12)
				for(int k = 0; k < 9; k++)
				{
					int sx = REACTOR_X + 10 + Math.floorMod(k * 37 + turn * 3 * (k % 2 == 0 ? 1 : -1), REACTOR_W - 24);
					int sy = surface + 4 + (k * 13) % Math.max(1, lh - 8);
					rect(sx, sy, 5, 1, 0x28ffffff);
				}
		}
		// While it fills: the liquid runs in from the inlet, the solids fall from the hopper
		if(filling())
		{
			rect(REACTOR_X + 1, PIPE_W_Y, 2, surface - PIPE_W_Y, fillingRecycled() ? 0xcc706a58 : 0xcc3f76e4);
			for(int k = 0; k < 4; k++)
			{
				int y = REACTOR_Y + Math.floorMod(ticks * 2 + k * 7, Math.max(1, surface - REACTOR_Y));
				rect(FUNNEL_X - 4 + k * 2, y, 2, 2, COL_DUST);
			}
		}
		// The shaft down the window, the impellers turning (4 frames of a quarter turn)
		rect(SHAFT_X - 1, REACTOR_Y, 3, REACTOR_H - 4, COL_SHAFT);
		rect(SHAFT_X - 1, REACTOR_Y, 1, REACTOR_H - 4, COL_SHAFT_LIGHT);
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.enableBlend();
		ClientUtils.bindTexture(SHEET);
		int frame = Math.floorMod(turn, 4);
		sprite(IMPELLER_LOW, frame * IMPELLER_LOW[2], SHAFT_X - 20, floor - 10);
		sprite(IMPELLER_HIGH, frame * IMPELLER_HIGH[2], SHAFT_X - 13, floor - 23);
	}

	boolean over(int mx, int my, int x, int y, int w, int h)
	{
		return mx >= guiLeft + x && mx < guiLeft + x + w && my >= guiTop + y && my < guiTop + y + h;
	}

	static final String K = "gui.immersivenuclear.chemical_processor.";

	@Override
	public void drawScreen(int mx, int my, float partialTicks)
	{
		super.drawScreen(mx, my, partialTicks);
		ArrayList<String> tip = new ArrayList<>();
		ClientUtils.handleGuiTank(container.recycled, guiLeft + RECYCLED_X + 2, guiTop + ROW2 + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, tip);
		if(over(mx, my, RECYCLED_X, ROW2, 20, 51))
		{
			tip.add(0, I18n.format(K + "recycled"));
			tip.add("§7" + I18n.format(K + "recycled.info", TileChemicalProcessor.MAX_RECYCLED, TileChemicalProcessor.REAGENT_SAVED));
		}
		ClientUtils.handleGuiTank(container.water, guiLeft + WATER_X + 2, guiTop + ROW2 + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, tip);
		ClientUtils.handleGuiTank(container.slurry, guiLeft + SLURRY_X + 2, guiTop + ROW2 + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, tip);
		if(over(mx, my, VX, VY, VW, VH) || over(mx, my, MOTOR_X, MOTOR_Y, MOTOR_W, MOTOR_H))
		{
			tip.add(I18n.format(K + "reactor"));
			tip.add("§7" + I18n.format(K + "phase." + container.phase));
			if(container.phase == TileChemicalProcessor.PHASE_REACT)
				tip.add("§7" + I18n.format(K + "reaction", Math.round(100 * reacted())));
			if(container.reactor > 0)
				tip.add("§7" + container.reactor + " / " + TileChemicalProcessor.BATCH_WATER + " mB");
		}
		if(over(mx, my, HOPPER_X, HOPPER_Y, HOPPER_W, VY - HOPPER_Y) && !over(mx, my, MOTOR_X, MOTOR_Y, MOTOR_W, MOTOR_H))
			tip.add(I18n.format(K + "hopper", TileChemicalProcessor.BATCH_SOLIDS,
					TileChemicalProcessor.reagentFor(TileChemicalProcessor.recycledShare(container.recycled.getFluidAmount()), container.recycled.getFluid())));
		if(over(mx, my, ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H))
			tip.add(container.energy + "/" + TileChemicalProcessor.ENERGY_CAPACITY + " IF");
		if(over(mx, my, LAMP_X, LAMP_Y, LAMP_SIZE, LAMP_SIZE))
		{
			tip.add(I18n.format("gui.immersivenuclear.state." + container.state));
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
