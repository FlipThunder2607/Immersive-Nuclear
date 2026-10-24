package immersivenuclear.client;

import java.util.ArrayList;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.GuiIEContainerBase;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileChemicalProcessor;
import immersivenuclear.common.gui.ContainerChemicalProcessor;
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
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.client.config.GuiUtils;

/**
 * Interface of the Chemical Processing Unit, in the finish of machines 2 and 3 (graphite brushed steel, IE's own
 * slots, tanks and markers drawn at run time a little darker, the same right column), around its batch reactor:
 *   water buckets | water tank > REACTOR > slurry tank | slurry buckets
 * On the reactor's lid, the hopper with the uranium powder and reagent slots and the agitator's motor. In the
 * window: the batch's water rising while it fills (a stream from the inlet, the solids falling from the hopper), the
 * impeller turning while it reacts and the water turning into slurry, the level going down while it drains. Our sheet
 * (textures/gui/reactor.png, made by ferramentas/gui_processamento.py with the same constants) holds the background
 * and those parts; the button and the lights are machine 1's own sheet (chemical_processor.png).
 */
public class GuiChemicalProcessor extends GuiIEContainerBase
{
	static final String IE = "immersiveengineering:textures/gui/refinery.png";
	static final String OWN = ImmersiveNuclear.MODID + ":textures/gui/chemical_processor.png";
	static final String SHEET = ImmersiveNuclear.MODID + ":textures/gui/reactor.png";
	static final int SHEET_SIZE = 512;

	// Layout (same as gui_processamento.py)
	static final int ROW = 52, WATER_X = 32, SLURRY_X = 195;
	/** Recycled liquor tank and its pipe under the water tank into the water pipe (same as gui_processamento.py). */
	static final int RECYCLED_X = 10, RECYCLED_PIPE_Y = 108, RECYCLED_JOIN_X = 57;
	/** The reactor's well; also JEI's click area for the recipe (INJeiPlugin). */
	public static final int REACTOR_X = 62, REACTOR_Y = 42, REACTOR_W = 128, REACTOR_H = 70;
	static final int WIN_X = 4, WIN_Y = 8, WIN_W = 104, WIN_H = 56;
	static final int STRIP_X = 113, STRIP_Y = 8, STRIP_W = 5, STRIP_H = 56;
	static final int MOTOR_X = 48, MOTOR_UP = 14, MOTOR_W = 24, MOTOR_H = 18;
	static final int SHAFT_X = REACTOR_X + MOTOR_X + 11;
	static final int IMPELLER_W = 40, IMPELLER_H = 10, IMPELLER_STEP = 12;
	static final int HOPPER_X = 68, HOPPER_Y = 30, HOPPER_W = 40, HOPPER_H = 12;
	/** Water in and slurry out, both at the tanks' middle. */
	static final int PIPE_DY = 35;
	/** A full batch fills this much of the window. */
	static final float BATCH_LEVEL = 0.8F;
	static final int ENERGY_X = 248, ENERGY_Y = 16, ENERGY_W = 14, ENERGY_H = 64;
	static final int BUTTON_X = 247, BUTTON_Y = 86, BUTTON_SIZE = 16;
	static final int LIGHTS_X = 248, LIGHTS_W = 14, LIGHT_X = 251, LIGHT_Y = 112, LIGHT_STEP = 8;
	static final int PANEL_X = ContainerChemicalProcessor.INV_X - 8, PANEL_Y = ContainerChemicalProcessor.INV_Y - 8;

	// Our sprites in the sheet: {u, v, w, h}
	static final int[] REACTOR = {0, 260, REACTOR_W, REACTOR_H}, MOTOR = {130, 260, MOTOR_W, MOTOR_H}, IMPELLER = {160, 260, IMPELLER_W, IMPELLER_H},
			HOPPER = {0, 340, HOPPER_W, HOPPER_H};
	// IE's pieces in refinery.png
	static final int[] IE_SLOT = {35, 13, 20, 20}, IE_TANK = {11, 18, 20, 51}, IE_BLUE = {42, 10, 6, 3}, IE_ORANGE = {42, 49, 6, 3};

	static final int COL_EFFLUENT = 0xff706a58, COL_WATER = 0xff3f76e4, COL_SLURRY = 0xff968060, COL_SHAFT = 0xff96999e, COL_DUST = 0xff8a9a6a;

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
	void sprite(int[] s, int x, int y, int dv, int w, int h)
	{
		Gui.drawModalRectWithCustomSizedTexture(guiLeft + x, guiTop + y, s[0], s[1] + dv, w, h, SHEET_SIZE, SHEET_SIZE);
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
			case TileChemicalProcessor.STATE_RUNNING: return 0;
			case TileChemicalProcessor.STATE_NO_INPUT: return 1;
			case TileChemicalProcessor.STATE_NO_POWER:
			case TileChemicalProcessor.STATE_OUTPUT_FULL: return 2;
			case TileChemicalProcessor.STATE_DISABLED: return 3;
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
		int[][] xy = ContainerChemicalProcessor.SLOT_XY;
		for(int i = 0; i < xy.length; i++)
		{
			piece(IE_SLOT, xy[i][0] - 2, xy[i][1] - 2);
			if(i >= TileChemicalProcessor.SLOT_WATER_IN)
				piece(i == TileChemicalProcessor.SLOT_WATER_IN || i == TileChemicalProcessor.SLOT_SLURRY_IN ? IE_BLUE : IE_ORANGE, xy[i][0] + 5, xy[i][1] - 5);
		}
		piece(IE_TANK, RECYCLED_X, ROW);
		piece(IE_BLUE, RECYCLED_X + 7, ROW - 3);
		piece(IE_TANK, WATER_X, ROW);
		piece(IE_BLUE, WATER_X + 7, ROW - 3);
		piece(IE_TANK, SLURRY_X, ROW);
		piece(IE_ORANGE, SLURRY_X + 7, ROW - 3);
		GuiGraphite.endTint();

		// Fluid moving in the pipes
		if(fillingRecycled())
		{
			int rx = RECYCLED_X + 10;
			flow(rx, ROW + 51, rx, RECYCLED_PIPE_Y, COL_EFFLUENT);
			flow(rx, RECYCLED_PIPE_Y, RECYCLED_JOIN_X, RECYCLED_PIPE_Y, COL_EFFLUENT);
			flow(RECYCLED_JOIN_X, RECYCLED_PIPE_Y, RECYCLED_JOIN_X, REACTOR_Y + PIPE_DY, COL_EFFLUENT);
			flow(RECYCLED_JOIN_X, REACTOR_Y + PIPE_DY, REACTOR_X - 1, REACTOR_Y + PIPE_DY, COL_EFFLUENT);
		}
		else if(filling())
			flow(WATER_X + 20, REACTOR_Y + PIPE_DY, REACTOR_X - 1, REACTOR_Y + PIPE_DY, COL_WATER);
		if(draining())
			flow(REACTOR_X + REACTOR_W, REACTOR_Y + PIPE_DY, SLURRY_X - 1, REACTOR_Y + PIPE_DY, COL_SLURRY);
		GlStateManager.color(1, 1, 1, 1);

		ClientUtils.handleGuiTank(container.recycled, guiLeft + RECYCLED_X + 2, guiTop + ROW + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);
		ClientUtils.handleGuiTank(container.water, guiLeft + WATER_X + 2, guiTop + ROW + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);
		ClientUtils.handleGuiTank(container.slurry, guiLeft + SLURRY_X + 2, guiTop + ROW + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);

		drawReactor(partialTicks);

		// Energy (IE's red gradient), button, lights
		int eh = (int)((ENERGY_H - 4) * (container.energy / (float)TileChemicalProcessor.ENERGY_CAPACITY));
		if(eh > 0)
			ClientUtils.drawGradientRect(guiLeft + ENERGY_X + 2, guiTop + ENERGY_Y + ENERGY_H - 2 - eh, guiLeft + ENERGY_X + ENERGY_W - 2, guiTop + ENERGY_Y + ENERGY_H - 2, 0xffb51500, 0xff600b00);
		GlStateManager.color(1, 1, 1, 1);
		ClientUtils.bindTexture(OWN);
		drawTexturedModalRect(guiLeft + BUTTON_X, guiTop + BUTTON_Y, 0, 150, BUTTON_SIZE, BUTTON_SIZE);
		int lit = litLight();
		for(int i = 0; i < 4; i++)
			drawTexturedModalRect(guiLeft + LIGHT_X + 1, guiTop + LIGHT_Y + i * LIGHT_STEP, i * 8, i == lit ? 90 : 98, 6, 6);
	}

	/** The batch in the window, the stream and the solids while it fills, the agitator, the reaction strip. */
	void drawReactor(float partialTicks)
	{
		int wx = REACTOR_X + WIN_X, wy = REACTOR_Y + WIN_Y, floor = wy + WIN_H;
		int lh = Math.round(WIN_H * BATCH_LEVEL * Math.min(1, container.reactor / (float)TileChemicalProcessor.BATCH_WATER));
		int surface = floor - lh;
		float reacted = reacted();
		if(lh > 0)
		{
			// The water, the slurry over it as the reaction goes on, a light line at the surface
			if(reacted < 1)
				fluid(FluidRegistry.WATER, wx, surface, WIN_W, lh, COL_WATER);
			if(reacted > 0)
				fluid(INContent.fluidSlurry, wx, surface, WIN_W, lh, (int)(255 * reacted) << 24 | 0xffffff);
			rect(wx, surface, WIN_W, 1, 0x46ffffff);
			// The swirl round the impeller, moving with it
			if(agitating() && lh > 12)
				for(int k = 0; k < 9; k++)
				{
					int sx = wx + 10 + Math.floorMod(k * 37 + turn * 3 * (k % 2 == 0 ? 1 : -1), WIN_W - 24);
					int sy = surface + 4 + (k * 13) % Math.max(1, lh - 8);
					rect(sx, sy, 5, 1, 0x28ffffff);
				}
		}
		// While it fills: the water falls from the inlet, the solids from the hopper
		if(filling())
		{
			int inletY = REACTOR_Y + PIPE_DY;
			rect(wx + 1, inletY, 2, surface - inletY, fillingRecycled() ? 0xcc706a58 : 0xcc3f76e4);
			for(int k = 0; k < 4; k++)
			{
				int y = wy + Math.floorMod(ticks * 2 + k * 7, Math.max(1, surface - wy));
				rect(HOPPER_X + HOPPER_W / 2 - 4 + k * 2, y, 2, 2, COL_DUST);
			}
		}
		// Shaft and impeller near the floor
		rect(SHAFT_X, REACTOR_Y + 2, 2, WIN_Y + WIN_H - 10, COL_SHAFT);
		GlStateManager.color(1, 1, 1, 1);
		ClientUtils.bindTexture(SHEET);
		sprite(IMPELLER, SHAFT_X + 1 - IMPELLER_W / 2, floor - 16, Math.floorMod(turn, 4) * IMPELLER_STEP, IMPELLER_W, IMPELLER_H);
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		sprite(REACTOR, REACTOR_X, REACTOR_Y);
		sprite(MOTOR, REACTOR_X + MOTOR_X, REACTOR_Y - MOTOR_UP + 2);
		sprite(HOPPER, HOPPER_X, HOPPER_Y);
		// The reaction in the strip, IE's red
		int sh = (int)(STRIP_H * reacted);
		int sx = guiLeft + REACTOR_X + STRIP_X, sy = guiTop + REACTOR_Y + STRIP_Y;
		if(sh > 0)
			ClientUtils.drawGradientRect(sx, sy + STRIP_H - sh, sx + STRIP_W, sy + STRIP_H, 0xffb51500, 0xff600b00);
		GlStateManager.color(1, 1, 1, 1);
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

	static final String K = "gui.immersivenuclear.chemical_processor.";

	@Override
	public void drawScreen(int mx, int my, float partialTicks)
	{
		super.drawScreen(mx, my, partialTicks);
		ArrayList<String> tip = new ArrayList<>();
		ClientUtils.handleGuiTank(container.recycled, guiLeft + RECYCLED_X + 2, guiTop + ROW + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, tip);
		if(over(mx, my, RECYCLED_X, ROW, 20, 51))
		{
			tip.add(0, I18n.format(K + "recycled"));
			tip.add("§7" + I18n.format(K + "recycled.info", TileChemicalProcessor.MAX_RECYCLED, TileChemicalProcessor.REAGENT_SAVED));
		}
		ClientUtils.handleGuiTank(container.water, guiLeft + WATER_X + 2, guiTop + ROW + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, tip);
		ClientUtils.handleGuiTank(container.slurry, guiLeft + SLURRY_X + 2, guiTop + ROW + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, tip);
		if(over(mx, my, REACTOR_X, REACTOR_Y, REACTOR_W, REACTOR_H) || over(mx, my, REACTOR_X + MOTOR_X, REACTOR_Y - MOTOR_UP + 2, MOTOR_W, MOTOR_H))
		{
			tip.add(I18n.format(K + "reactor"));
			tip.add("§7" + I18n.format(K + "phase." + container.phase));
			if(container.phase == TileChemicalProcessor.PHASE_REACT)
				tip.add("§7" + I18n.format(K + "reaction", Math.round(100 * reacted())));
			if(container.reactor > 0)
				tip.add("§7" + container.reactor + " / " + TileChemicalProcessor.BATCH_WATER + " mB");
		}
		if(over(mx, my, HOPPER_X, HOPPER_Y, HOPPER_W, HOPPER_H))
			tip.add(I18n.format(K + "hopper", TileChemicalProcessor.BATCH_SOLIDS,
					TileChemicalProcessor.reagentFor(TileChemicalProcessor.recycledShare(container.recycled.getFluidAmount()))));
		if(over(mx, my, ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H))
			tip.add(container.energy + "/" + TileChemicalProcessor.ENERGY_CAPACITY + " IF");
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
				tip.add(on ? I18n.format("gui.immersivenuclear.state." + container.state) : "§7" + I18n.format("gui.immersivenuclear.light." + i));
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
			mc.playerController.sendEnchantPacket(inventorySlots.windowId, ContainerChemicalProcessor.BUTTON_REDSTONE);
		}
	}
}
