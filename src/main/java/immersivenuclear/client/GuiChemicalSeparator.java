package immersivenuclear.client;

import java.util.ArrayList;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.GuiIEContainerBase;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileChemicalSeparator;
import immersivenuclear.common.gui.ContainerChemicalSeparator;
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
import net.minecraftforge.fluids.FluidTank;

/**
 * Interface of the Chemical Separation Unit: graphite brushed steel finished the way IE finishes its GUIs (riveted
 * border, wells with a flat light frame and a dark inside), with IE's own slots, tanks, gauges and port markers drawn
 * at run time from refinery.png, darkened a little to sit on the graphite; the button and the status lights are the
 * mod's (as on machines 1 and 2). Our sheet (textures/gui/chemical_separator.png, made by ferramentas/gui_separacao.py
 * with the same constants) holds the background and what IE has no piece for: settlers, mixers, reservoir.
 * No texts on the screen; the tooltips explain each part. A flow diagram, one row per stage:
 *   buckets | crude tank > mixer A > mixing > settler A > effluent tank | buckets
 *   solvent reservoir; return line B to A and loaded solvent line A to B
 *   buckets | stripping tank > mixer B > mixing > settler B > purified tank | buckets
 * In a settler the mixture comes in cloudy; while it settles the clear layers grow from the top and the bottom
 * until they meet at the interface.
 */
public class GuiChemicalSeparator extends GuiIEContainerBase
{
	static final String IE = "immersiveengineering:textures/gui/refinery.png";
	static final String STEEL = ImmersiveNuclear.MODID + ":textures/gui/chemical_processor.png";
	static final String SHEET = ImmersiveNuclear.MODID + ":textures/gui/chemical_separator.png";
	static final int SHEET_SIZE = 512;
	/** IE's pieces are drawn this much darker, to sit on the graphite. */
	static final float IE_TINT = 0.8F;

	// Layout (same as gui_separacao.py)
	/** Also JEI's click areas (the settlers, INJeiPlugin). */
	public static final int ROW_A = 16, ROW_B = 96;
	static final int TANK_IN_X = 32, TANK_OUT_X = 195, TANK_W = 20, TANK_H = 51;
	static final int MIXER_X = 57, MIXER_DY = 12, MIXER_S = 26, FAN_D = 3, FAN_S = 20;
	static final int ARROW_X = 87, ARROW_DY = 19;
	public static final int SETTLER_X = 110, SETTLER_DY = 6, SETTLER_W = 80, SETTLER_H = 38;
	static final int WIN_X = 4, WIN_Y = 4, WIN_W = 52, WIN_H = 30, STRIP_X = 66, STRIP_Y = 5, STRIP_W = 5, STRIP_H = 28;
	static final int PIPE_DY = 25, OUTLET_DY = 35;
	/** The reservoir sits halfway between the two rows; its front takes the feed to mixer A and the return from settler B. */
	static final int RES_X = 8, RES_Y = 75, RES_W = 48, RES_H = 13, RES_WIN_X = 3, RES_WIN_Y = 3, RES_WIN_W = 42, RES_WIN_H = 8;
	/** Between the rows, each pipe on its own: the loaded solvent line above the reservoir, feed and return on its front. */
	static final int LANE_LOADED_Y = 70, FEED_Y = 78, LANE_RETURN_Y = 85;
	static final int RETURN_X = 164, LOADED_X = 170, MIX_IN_X = 66, LOADED_DOWN_X = 76;
	static final int ENERGY_X = 248, ENERGY_Y = 16, ENERGY_W = 14, ENERGY_H = 64;
	static final int BUTTON_X = 247, BUTTON_Y = 86, BUTTON_SIZE = 16;
	static final int LIGHTS_X = 248, LIGHTS_Y = 108, LIGHTS_W = 14, LIGHT_X = 252, LIGHT_Y = 112, LIGHT_STEP = 8;

	// Our sprites in the sheet: {u, v, w, h}
	static final int[] MIXER = {0, 260, 26, 26}, FAN = {28, 260, 20, 20}, SETTLER = {0, 304, 80, 38}, RESERVOIR = {0, 346, 48, 13}, ARROW_FILL = {120, 260, 19, 13};
	static final int FAN_STEP = 22;
	// IE's pieces in refinery.png
	static final int[] IE_SLOT = {35, 13, 20, 20}, IE_TANK = {11, 18, 20, 51}, IE_GAUGE = {177, 31, 20, 51}, IE_BLUE = {42, 10, 6, 3}, IE_ORANGE = {42, 49, 6, 3};

	static final int COL_CRUDE = 0xffc86e32, COL_STRIPPING = 0xffaac8e6, COL_EFFLUENT = 0xff788c5a, COL_PURIFIED = 0xffe6d25a,
			COL_LOADED = 0xffffbe5a, COL_CLEAN = 0xffd2d2be, COL_MIX = 0xffb0a080;
	/** Tint of the solvent while it carries the uranium. */
	static final int AMBER = 0xffffb04a;

	final ContainerChemicalSeparator container;
	/** Client counters for the fans. */
	int turnA, turnB;

	public GuiChemicalSeparator(ContainerChemicalSeparator container)
	{
		super(container);
		this.container = container;
		xSize = ContainerChemicalSeparator.WIDTH;
		ySize = ContainerChemicalSeparator.HEIGHT;
	}

	@Override
	public void updateScreen()
	{
		super.updateScreen();
		if(container.moving(1))
			turnA++;
		if(container.moving(2))
			turnB++;
	}

	/** A sprite of our sheet (bound) at GUI x, y; du/dv shift inside it, w/h crop it. */
	void sprite(int[] s, int x, int y, int du, int dv, int w, int h)
	{
		Gui.drawModalRectWithCustomSizedTexture(guiLeft + x, guiTop + y, s[0] + du, s[1] + dv, w, h, SHEET_SIZE, SHEET_SIZE);
	}

	void sprite(int[] s, int x, int y)
	{
		sprite(s, x, y, 0, 0, s[2], s[3]);
	}

	/** A piece of the bound 256x256 texture (IE's, or the mod's sheet of machine 1). */
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

	/** Part of a fluid's still sprite (tiled from the top left), tinted; the blocks atlas must be bound. */
	void fluid(Fluid f, int x, int y, int w, int h, int argb)
	{
		if(f == null || w <= 0 || h <= 0)
			return;
		TextureAtlasSprite sp = mc.getTextureMapBlocks().getAtlasSprite(f.getStill().toString());
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

	void blend(boolean on)
	{
		if(on)
		{
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		}
		else
			GlStateManager.disableBlend();
	}

	int stageY(int st)
	{
		return st == 0 ? ROW_A : ROW_B;
	}

	/**
	 * The liquid in a settler window. Filling: one cloudy mixture. Settling: the clear top layer grows down from the
	 * surface and the clear bottom layer up from the floor, until they meet. Draining: two clear layers.
	 */
	void settlerLiquid(int st, int sx, int sy)
	{
		SettlerLayers l = SettlerLayers.of(st, container.settler(st), WIN_H);
		if(l == null)
			return;
		int floor = sy + WIN_Y + WIN_H, x = sx + WIN_X;
		int h = Math.max(1, Math.round(l.height)), clearTop = Math.round(l.clearTop), clearBottom = Math.round(l.clearBottom);
		int mixed = h - clearTop - clearBottom;
		mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		blend(true);
		if(mixed > 0)
		{
			// Emulsion: the water phase with the solvent stirred through it, a little murky
			int my = floor - clearBottom - mixed;
			fluid(l.inBottom, x, my, WIN_W, mixed, 0xffffffff);
			fluid(INContent.fluidSolvent, x, my, WIN_W, mixed, (l.inTopTint & 0x00ffffff) | 0x90000000);
			rect(x, my, WIN_W, mixed, 0x40706050);
			mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
			blend(true);
		}
		fluid(l.outBottom, x, floor - clearBottom, WIN_W, clearBottom, 0xffffffff);
		fluid(INContent.fluidSolvent, x, floor - h, WIN_W, clearTop, l.outTopTint);
		if(l.separated())
			rect(x, floor - Math.round(l.bottomHeight) - 1, WIN_W, 1, 0x90f0f0dc);
		blend(false);
	}

	/** The mixer's well seen from above: the two feeds stirred together while it fills its settler. */
	void mixerLiquid(int st)
	{
		int[] s = container.settler(st);
		if(s[0] != TileChemicalSeparator.PHASE_FILL || s[2] + s[3] <= 0)
			return;
		int x = MIXER_X + FAN_D, y = stageY(st) + MIXER_DY + FAN_D;
		mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		blend(true);
		fluid(st == 0 ? INContent.fluidCrudeSolution : INContent.fluidStrippingSolution, x, y, FAN_S, FAN_S, 0xffffffff);
		fluid(INContent.fluidSolvent, x, y, FAN_S, FAN_S, st == 0 ? 0x90ffffff : 0x90ffb04a);
		blend(false);
	}

	void tankLiquid(FluidTank tank, int x, int y)
	{
		int h = Math.round(47 * tank.getFluidAmount() / (float)tank.getCapacity());
		if(h > 0 && tank.getFluid() != null)
		{
			mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
			fluid(tank.getFluid().getFluid(), x + 2, y + 2 + 47 - h, 16, h, 0xffffffff);
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mx, int my)
	{
		GlStateManager.color(1, 1, 1, 1);
		ClientUtils.bindTexture(SHEET);
		sprite(new int[]{0, 0, xSize, ySize}, 0, 0);
		// IE's wooden inventory panel, then IE's slots, tanks and port markers, a little darker
		ClientUtils.bindTexture(IE);
		drawTexturedModalRect(guiLeft + ContainerChemicalSeparator.INV_X, guiTop + ContainerChemicalSeparator.INV_Y - 8, 0, 77, 176, 89);
		GlStateManager.color(IE_TINT, IE_TINT, IE_TINT, 1);
		int[][] xy = ContainerChemicalSeparator.SLOT_XY;
		for(int i = 0; i < xy.length; i++)
		{
			piece(IE_SLOT, xy[i][0] - 2, xy[i][1] - 2);
			piece(i % 2 == 0 ? IE_BLUE : IE_ORANGE, xy[i][0] + 5, xy[i][1] - 5);
		}
		for(int st = 0; st < 2; st++)
		{
			int y = stageY(st);
			piece(IE_TANK, TANK_IN_X, y);
			piece(IE_BLUE, TANK_IN_X + 7, y - 3);
			piece(IE_TANK, TANK_OUT_X, y);
			piece(IE_ORANGE, TANK_OUT_X + 7, y - 3);
		}
		GlStateManager.color(1, 1, 1, 1);

		drawFlows();
		GlStateManager.color(1, 1, 1, 1);

		// Liquids
		tankLiquid(container.crude, TANK_IN_X, ROW_A);
		tankLiquid(container.effluent, TANK_OUT_X, ROW_A);
		tankLiquid(container.stripping, TANK_IN_X, ROW_B);
		tankLiquid(container.purified, TANK_OUT_X, ROW_B);
		for(int st = 0; st < 2; st++)
		{
			settlerLiquid(st, SETTLER_X, stageY(st) + SETTLER_DY);
			mixerLiquid(st);
		}
		int rh = Math.round(RES_WIN_H * container.solvent.getFluidAmount() / (float)TileChemicalSeparator.TANK_CAPACITY);
		mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		fluid(INContent.fluidSolvent, RES_X + RES_WIN_X, RES_Y + RES_WIN_Y + RES_WIN_H - rh, RES_WIN_W, rh, 0xffffffff);

		// IE's tank gauges, our settlers, mixers and reservoir over the liquids
		GlStateManager.color(1, 1, 1, 1);
		blend(true);
		ClientUtils.bindTexture(IE);
		for(int x : new int[]{TANK_IN_X, TANK_OUT_X})
			for(int y : new int[]{ROW_A, ROW_B})
				piece(IE_GAUGE, x, y);
		ClientUtils.bindTexture(SHEET);
		for(int st = 0; st < 2; st++)
		{
			int y = stageY(st);
			sprite(SETTLER, SETTLER_X, y + SETTLER_DY);
			sprite(MIXER, MIXER_X, y + MIXER_DY);
			int turn = st == 0 ? turnA : turnB;
			sprite(FAN, MIXER_X + FAN_D, y + MIXER_DY + FAN_D, (turn / 2 % 4) * FAN_STEP, 0, FAN_S, FAN_S);
		}
		sprite(RESERVOIR, RES_X, RES_Y);
		// Mixing arrows: the fill has the arrow's own shape
		for(int st = 0; st < 2; st++)
		{
			int done = (int)(ARROW_FILL[2] * mixingDone(st));
			if(done > 0)
				sprite(ARROW_FILL, ARROW_X, stageY(st) + ARROW_DY, 0, 0, done, ARROW_FILL[3]);
		}
		// Button and status lights (the mod's sheet of machine 1)
		ClientUtils.bindTexture(STEEL);
		drawTexturedModalRect(guiLeft + BUTTON_X, guiTop + BUTTON_Y, 0, 150, BUTTON_SIZE, BUTTON_SIZE);
		int lit = litLight();
		for(int i = 0; i < 4; i++)
			drawTexturedModalRect(guiLeft + LIGHT_X, guiTop + LIGHT_Y + i * LIGHT_STEP, i * 8, i == lit ? 90 : 98, 6, 6);
		blend(false);
		// Settling bars and energy: IE's red gradient
		for(int st = 0; st < 2; st++)
		{
			int sh = (int)(STRIP_H * settled(st));
			int sx = SETTLER_X + STRIP_X, sy = stageY(st) + SETTLER_DY + STRIP_Y;
			if(sh > 0)
				ClientUtils.drawGradientRect(guiLeft + sx, guiTop + sy + STRIP_H - sh, guiLeft + sx + STRIP_W, guiTop + sy + STRIP_H, 0xffb51500, 0xff600b00);
		}
		int eh = (int)((ENERGY_H - 4) * (container.energy() / (float)TileChemicalSeparator.ENERGY_CAPACITY));
		if(eh > 0)
			ClientUtils.drawGradientRect(guiLeft + ENERGY_X + 2, guiTop + ENERGY_Y + ENERGY_H - 2 - eh, guiLeft + ENERGY_X + ENERGY_W - 2, guiTop + ENERGY_Y + ENERGY_H - 2, 0xffb51500, 0xff600b00);
	}

	/** Fluid moving through the pipes: coloured while it flows, the background's idle pipe otherwise. */
	void drawFlows()
	{
		boolean mixA = container.moving(1), mixB = container.moving(2), loaded = container.moving(4), clean = container.moving(8);
		for(int st = 0; st < 2; st++)
		{
			int y = stageY(st);
			boolean mixing = st == 0 ? mixA : mixB;
			if(mixing)
			{
				flow(TANK_IN_X + TANK_W, y + PIPE_DY, MIXER_X - 1, y + PIPE_DY, st == 0 ? COL_CRUDE : COL_STRIPPING);
				flow(MIXER_X + MIXER_S, y + PIPE_DY, ARROW_X - 1, y + PIPE_DY, COL_MIX);
				flow(ARROW_X + 19, y + PIPE_DY, SETTLER_X - 1, y + PIPE_DY, COL_MIX);
			}
			if(container.moving(st == 0 ? 16 : 32))
				flow(SETTLER_X + SETTLER_W, y + OUTLET_DY, TANK_OUT_X - 1, y + OUTLET_DY, st == 0 ? COL_EFFLUENT : COL_PURIFIED);
		}
		// A line lights up whole while its fluid moves (never a piece of it)
		if(clean)
		{
			flow(RETURN_X, ROW_B + SETTLER_DY, RETURN_X, LANE_RETURN_Y, COL_CLEAN);
			flow(RETURN_X, LANE_RETURN_Y, RES_X + RES_W, LANE_RETURN_Y, COL_CLEAN);
		}
		if(mixA)
		{
			flow(RES_X + RES_W, FEED_Y, MIX_IN_X, FEED_Y, COL_CLEAN);
			flow(MIX_IN_X, FEED_Y, MIX_IN_X, ROW_A + MIXER_DY + MIXER_S, COL_CLEAN);
		}
		if(loaded || mixB)
		{
			flow(LOADED_X, ROW_A + SETTLER_DY + SETTLER_H, LOADED_X, LANE_LOADED_Y, COL_LOADED);
			flow(LOADED_X, LANE_LOADED_Y, LOADED_DOWN_X, LANE_LOADED_Y, COL_LOADED);
			flow(LOADED_DOWN_X, LANE_LOADED_Y, LOADED_DOWN_X, ROW_B + MIXER_DY, COL_LOADED);
		}
	}

	/** How much of the batch the mixer has put into its settler (0 once it settles). */
	float mixingDone(int st)
	{
		int[] s = container.settler(st);
		if(s[0] != TileChemicalSeparator.PHASE_FILL)
			return 0;
		int batch = st == 0 ? TileChemicalSeparator.A_AQUEOUS + TileChemicalSeparator.A_SOLVENT : TileChemicalSeparator.B_SOLVENT + TileChemicalSeparator.B_AQUEOUS;
		return Math.min(1, (s[2] + s[3]) / (float)batch);
	}

	/** Settling progress, 0..1 (full while it drains). */
	float settled(int st)
	{
		int[] s = container.settler(st);
		if(s[0] == TileChemicalSeparator.PHASE_SETTLE)
			return s[1] / (float)TileChemicalSeparator.SETTLE_TICKS;
		return s[0] == TileChemicalSeparator.PHASE_DRAIN ? 1 : 0;
	}

	/** 0 green running, 1 yellow waiting for input, 2 red stopped (power, full output), 3 grey disabled; -1 idle. */
	int litLight()
	{
		switch(container.state())
		{
			case TileChemicalSeparator.STATE_RUNNING: return 0;
			case TileChemicalSeparator.STATE_NO_INPUT: return 1;
			case TileChemicalSeparator.STATE_NO_POWER:
			case TileChemicalSeparator.STATE_OUTPUT_FULL: return 2;
			case TileChemicalSeparator.STATE_DISABLED: return 3;
			default: return -1;
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mx, int my)
	{
		// The icon sits inside the button's bevel, a quarter smaller than an item
		RenderHelper.enableGUIStandardItemLighting();
		ItemStack icon = new ItemStack(container.redstoneInverted() ? Item.getItemFromBlock(Blocks.REDSTONE_TORCH) : Items.REDSTONE);
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

	static final String K = "gui.immersivenuclear.chemical_separator.";

	@Override
	public void drawScreen(int mx, int my, float partialTicks)
	{
		super.drawScreen(mx, my, partialTicks);
		ArrayList<String> tip = new ArrayList<>();
		tank(tip, container.crude, TANK_IN_X, ROW_A, "crude", mx, my);
		tank(tip, container.effluent, TANK_OUT_X, ROW_A, "effluent", mx, my);
		tank(tip, container.stripping, TANK_IN_X, ROW_B, "stripping", mx, my);
		tank(tip, container.purified, TANK_OUT_X, ROW_B, "purified", mx, my);
		if(over(mx, my, RES_X, RES_Y, RES_W, RES_H))
		{
			tip.add(I18n.format(K + "solvent"));
			tip.add("§7" + container.solvent.getFluidAmount() + " / " + TileChemicalSeparator.TANK_CAPACITY + " mB");
		}
		for(int st = 0; st < 2; st++)
		{
			int y = stageY(st);
			String stage = st == 0 ? "a" : "b";
			if(over(mx, my, MIXER_X, y + MIXER_DY, MIXER_S, MIXER_S) || over(mx, my, ARROW_X, y + ARROW_DY, 19, 13))
			{
				tip.add(I18n.format(K + "mixer_" + stage));
				tip.add("§7" + I18n.format(container.moving(st == 0 ? 1 : 2) ? K + "mixing" : K + "mixer_waiting"));
				tip.add("§7" + I18n.format(K + "batch", Math.round(100 * mixingDone(st))));
			}
			if(over(mx, my, SETTLER_X, y + SETTLER_DY, SETTLER_W, SETTLER_H))
			{
				int[] s = container.settler(st);
				tip.add(I18n.format(K + "settler_" + stage));
				if(s[0] == TileChemicalSeparator.PHASE_SETTLE)
					tip.add("§7" + I18n.format(K + "phase.1", Math.round(100 * settled(st))));
				else
					tip.add("§7" + I18n.format(K + "phase." + s[0]));
				if(s[2] + s[3] > 0)
				{
					tip.add("§7" + I18n.format(K + "layer.top." + stage, s[2]));
					tip.add("§7" + I18n.format(K + "layer.bottom." + stage + (s[0] == TileChemicalSeparator.PHASE_FILL ? ".in" : ""), s[3]));
				}
			}
		}
		if(over(mx, my, LOADED_DOWN_X - 2, LANE_LOADED_Y - 2, LOADED_X - LOADED_DOWN_X + 4, 5) || over(mx, my, LOADED_X - 2, ROW_A + SETTLER_DY + SETTLER_H, 5, LANE_LOADED_Y - ROW_A - SETTLER_DY - SETTLER_H))
		{
			tip.add(I18n.format(K + "loaded"));
			tip.add("§7" + container.loaded.getFluidAmount() + " / " + TileChemicalSeparator.TANK_CAPACITY + " mB");
		}
		else if(over(mx, my, RES_X + RES_W, LANE_RETURN_Y - 2, RETURN_X - RES_X - RES_W, 5) || over(mx, my, RETURN_X - 2, LANE_RETURN_Y, 5, ROW_B - LANE_RETURN_Y))
			tip.add(I18n.format(K + "clean"));
		if(over(mx, my, ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H))
			tip.add(container.energy() + "/" + TileChemicalSeparator.ENERGY_CAPACITY + " IF");
		if(over(mx, my, BUTTON_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE))
		{
			tip.add(I18n.format("gui.immersivenuclear.redstone"));
			tip.add("§7" + I18n.format(container.redstoneInverted() ? "gui.immersivenuclear.redstone.inverted" : "gui.immersivenuclear.redstone.normal"));
		}
		int lit = litLight();
		for(int i = 0; i < 4; i++)
			if(over(mx, my, LIGHTS_X, LIGHT_Y + i * LIGHT_STEP - 1, LIGHTS_W, LIGHT_STEP))
				tip.add(i == lit ? I18n.format(K + "state." + container.state()) : "§7" + I18n.format(K + "light." + i));
		if(!tip.isEmpty())
		{
			ClientUtils.drawHoveringText(tip, mx, my, fontRenderer, guiLeft + xSize, -1);
			RenderHelper.enableGUIStandardItemLighting();
		}
	}

	void tank(ArrayList<String> tip, FluidTank tank, int x, int y, String name, int mx, int my)
	{
		if(!over(mx, my, x, y, TANK_W, TANK_H))
			return;
		tip.add(I18n.format(K + name));
		if(tank.getFluid() != null)
			tip.add("§7" + tank.getFluid().getLocalizedName());
		tip.add("§7" + tank.getFluidAmount() + " / " + tank.getCapacity() + " mB");
	}

	@Override
	protected void mouseClicked(int mx, int my, int button) throws java.io.IOException
	{
		super.mouseClicked(mx, my, button);
		if(button == 0 && over(mx, my, BUTTON_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE))
		{
			mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			mc.playerController.sendEnchantPacket(inventorySlots.windowId, ContainerChemicalSeparator.BUTTON_REDSTONE);
		}
	}
}
