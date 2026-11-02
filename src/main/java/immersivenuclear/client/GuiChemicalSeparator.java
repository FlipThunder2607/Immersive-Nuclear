package immersivenuclear.client;

import java.util.ArrayList;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.GuiIEContainerBase;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileChemicalSeparator;
import immersivenuclear.common.gui.ContainerChemicalSeparator;
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
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.client.config.GuiUtils;

/**
 * Interface of the Chemical Separation Unit, in the interface standard (01/11): separate riveted panels with the
 * machine drawn between them, the world showing through the gaps.
 *   [crude | stripping: in / tank / out]  the two mixer-settler stages side by side on the steel base: each mixer (its
 *   green motor on the lid) shows the feeds stirred together in its round window while it fills its settler; in the
 *   tall settler's window the mixture comes in cloudy and, while it settles, the clear layers grow from the top and
 *   the bottom until they meet; the solvent reservoir lies on both settlers' lids, its level in the glass
 *   [effluent | purified: in / tank / out | energy over the status lamp]
 * and IE's inventory panel centred under it. The pipes run behind the equipment and light up in the gaps while their
 * fluid moves. Our sheet (textures/gui/chemical_separator_gui.png, made by ferramentas/gui_separacao_v7_esboco.py
 * --jogo with the same numbers) holds the background and the sprites; IE's slots, tanks and marks come from IE's sheet.
 * The JEI page keeps its own sheet (chemical_separator.png).
 */
public class GuiChemicalSeparator extends GuiIEContainerBase
{
	static final String IE = "immersiveengineering:textures/gui/refinery.png";
	static final String SHEET = ImmersiveNuclear.MODID + ":textures/gui/chemical_separator_gui.png";
	static final int SHEET_SIZE = 512;
	/** IE's pieces are drawn this much darker, to sit on the graphite. */
	static final float IE_TINT = 0.8F;

	// Layout (same as gui_separacao_v7_esboco.py)
	static final int ROW1 = 37, ROW2 = 62, ROW3 = 117, TANK_W = 20, TANK_H = 51;
	static final int CRUDE_X = 8, STRIPPING_X = 38, EFFLUENT_X = 234, PURIFIED_X = 264, CONTROL_X = 290;
	/** Mixers {x, y, w, h}, stage A then B; their round windows' centre and radius. */
	static final int[][] MIXERS = {{76, 84, 20, 52}, {146, 84, 20, 52}};
	static final int MIX_WIN_DY = 28, MIX_WIN_R = 8;
	/** Settlers {x, y, w, h}, stage A then B: also JEI's click areas (INJeiPlugin). */
	public static final int[][] SETTLERS = {{100, 62, 42, 74}, {170, 62, 44, 74}};
	/** A settler's window inside it: from x + 6, y + 4, w - 12 wide, h - 9 high; its weir 10 from the far end. */
	static final int WIN_DX = 6, WIN_DY = 4, WIN_DW = 12, WIN_DH = 9, WEIR_DX = 10;
	static final int RES_X = 104, RES_Y = 42, RES_W = 106, RES_H = 14, RES_GLASS_X = 130, RES_GLASS_Y = 47, RES_GLASS_W = 24, RES_GLASS_H = 3;
	/** The pipes' cores (2 px): the upper line (crude in, effluent out), the lower one (stripping in, purified out), the
	 * solvent into the mixers' sides, the overflows mixer -> settler; and the solvent drops in the gaps. */
	static final int UPPER_Y = 97, LOWER_Y = 121, SOLVENT_Y = 89, OVERFLOW_Y = 127, DROP_TOP = 56;
	static final int ENERGY_X = CONTROL_X + 2, ENERGY_Y = ROW1, ENERGY_W = 11, ENERGY_H = ROW2 + 51 - ROW1;
	static final int LAMP_X = CONTROL_X, LAMP_Y = ROW3 + 2, LAMP_SIZE = 16;

	// Our sprites in the sheet: {u, v, w, h}
	static final int[] RING = {0, 260, 19, 19}, IMPELLER = {20, 260, 13, 4}, LENS = {0, 330, 10, 10};
	static final int IMPELLER_STEP = 14;
	// IE's pieces in refinery.png
	static final int[] IE_SLOT = {35, 13, 20, 20}, IE_TANK = {11, 18, 20, 51}, IE_GAUGE = {177, 31, 20, 51}, IE_BLUE = {42, 10, 6, 3}, IE_ORANGE = {42, 49, 6, 3};

	static final int COL_CRUDE = 0xffc86e32, COL_STRIPPING = 0xffaac8e6, COL_EFFLUENT = 0xff788c5a, COL_PURIFIED = 0xffe6d25a,
			COL_LOADED = 0xffffbe5a, COL_CLEAN = 0xffd2d2be, COL_MIX = 0xffb0a080, COL_WEIR = 0xff5a5d62, COL_SHAFT = 0xff96999e;

	final ContainerChemicalSeparator container;
	/** Client counters for the impellers. */
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

	/** A pipe's moving fluid, its 2 px core, only over [x0, x1] (horizontal) or [y0, y1] (vertical). */
	void hflow(int x0, int x1, int y, int argb)
	{
		rect(x0, y, x1 - x0, 2, argb);
	}

	void vflow(int x, int y0, int y1, int argb)
	{
		rect(x, y0, 2, y1 - y0, argb);
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

	/**
	 * The liquid in a settler's window. Filling: one cloudy mixture. Settling: the clear top layer grows down from the
	 * surface and the clear bottom layer up from the floor, until they meet. Draining: two clear layers.
	 */
	void settlerLiquid(int st)
	{
		int[] b = SETTLERS[st];
		int wx = b[0] + WIN_DX, wy = b[1] + WIN_DY, ww = b[2] - WIN_DW, wh = b[3] - WIN_DH;
		SettlerLayers l = SettlerLayers.of(st, container.settler(st), wh);
		if(l != null)
		{
			int floor = wy + wh;
			int h = Math.max(1, Math.round(l.height)), clearTop = Math.round(l.clearTop), clearBottom = Math.round(l.clearBottom);
			int mixed = h - clearTop - clearBottom;
			mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
			blend(true);
			if(mixed > 0)
			{
				// Emulsion: the water phase with the solvent stirred through it, a little murky
				int my = floor - clearBottom - mixed;
				fluid(l.inBottom, wx, my, ww, mixed, 0xffffffff);
				fluid(INContent.fluidSolvent, wx, my, ww, mixed, (l.inTopTint & 0x00ffffff) | 0x90000000);
				rect(wx, my, ww, mixed, 0x40706050);
				mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
				blend(true);
			}
			fluid(l.outBottom, wx, floor - clearBottom, ww, clearBottom, 0xffffffff);
			fluid(INContent.fluidSolvent, wx, floor - h, ww, clearTop, l.outTopTint);
			if(l.separated())
				rect(wx, floor - Math.round(l.bottomHeight) - 1, ww, 1, 0x90f0f0dc);
			blend(false);
		}
		rect(b[0] + b[2] - WEIR_DX, wy, 2, wh, COL_WEIR);                  // the weir, in front of the liquid
	}

	/** The mixer's round window: the two feeds stirred together while it fills its settler, the impeller turning. */
	void mixerWindow(int st)
	{
		int[] m = MIXERS[st], s = container.settler(st);
		int cx = m[0] + m[2] / 2, cy = m[1] + MIX_WIN_DY;
		if(s[0] == TileChemicalSeparator.PHASE_FILL && s[2] + s[3] > 0)
		{
			mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
			blend(true);
			fluid(st == 0 ? INContent.fluidCrudeSolution : INContent.fluidStrippingSolution, cx - MIX_WIN_R, cy - MIX_WIN_R, 2 * MIX_WIN_R + 1, 2 * MIX_WIN_R + 1, 0xffffffff);
			fluid(INContent.fluidSolvent, cx - MIX_WIN_R, cy - MIX_WIN_R, 2 * MIX_WIN_R + 1, 2 * MIX_WIN_R + 1, st == 0 ? 0x90ffffff : 0x90ffb04a);
			blend(false);
		}
		rect(cx, m[1] - 2, 1, cy + 2 - m[1] + 2, COL_SHAFT);              // the shaft down to the impeller
		GlStateManager.color(1, 1, 1, 1);
		blend(true);
		ClientUtils.bindTexture(SHEET);
		int turn = st == 0 ? turnA : turnB;
		sprite(IMPELLER, (turn / 2 % 4) * IMPELLER_STEP, cx - 6, cy + 1);
		sprite(RING, 0, cx - 9, cy - 9);                                  // the wall round the window, over the liquid
		blend(false);
	}

	/** 0 green running, 1 amber waiting (no input, or stopped by redstone), 2 red stopped (no power, output full), 3 off. */
	int lamp()
	{
		switch(container.state())
		{
			case TileChemicalSeparator.STATE_RUNNING: return 0;
			case TileChemicalSeparator.STATE_NO_INPUT:
			case TileChemicalSeparator.STATE_DISABLED: return 1;
			case TileChemicalSeparator.STATE_NO_POWER:
			case TileChemicalSeparator.STATE_OUTPUT_FULL: return 2;
			default: return 3;
		}
	}

	void tankLiquid(FluidTank tank, int x)
	{
		int h = Math.round(47 * tank.getFluidAmount() / (float)tank.getCapacity());
		if(h > 0 && tank.getFluid() != null)
		{
			mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
			fluid(tank.getFluid().getFluid(), x + 2, ROW2 + 2 + 47 - h, 16, h, 0xffffffff);
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mx, int my)
	{
		GlStateManager.color(1, 1, 1, 1);
		blend(true);
		ClientUtils.bindTexture(SHEET);
		Gui.drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, xSize, ySize, SHEET_SIZE, SHEET_SIZE);
		// IE's wooden inventory panel, then IE's slots, tanks and port marks, a little darker
		ClientUtils.bindTexture(IE);
		drawTexturedModalRect(guiLeft + ContainerChemicalSeparator.INV_X, guiTop + ContainerChemicalSeparator.INV_Y - 8, 0, 77, 176, 89);
		GlStateManager.color(IE_TINT, IE_TINT, IE_TINT, 1);
		int[][] xy = ContainerChemicalSeparator.SLOT_XY;
		for(int i = 0; i < xy.length; i++)
		{
			piece(IE_SLOT, xy[i][0] - 2, xy[i][1] - 2);
			piece(i % 2 == 0 ? IE_BLUE : IE_ORANGE, xy[i][0] + 5, xy[i][1] - 5);
		}
		for(int x : new int[]{CRUDE_X, STRIPPING_X, EFFLUENT_X, PURIFIED_X})
		{
			piece(IE_TANK, x, ROW2);
			piece(x < 100 ? IE_BLUE : IE_ORANGE, x + 7, ROW2 - 3);
		}
		GlStateManager.color(1, 1, 1, 1);

		drawFlows();
		GlStateManager.color(1, 1, 1, 1);

		// Liquids
		tankLiquid(container.crude, CRUDE_X);
		tankLiquid(container.stripping, STRIPPING_X);
		tankLiquid(container.effluent, EFFLUENT_X);
		tankLiquid(container.purified, PURIFIED_X);
		int rw = Math.round(RES_GLASS_W * container.solvent.getFluidAmount() / (float)TileChemicalSeparator.SOLVENT_CAPACITY);
		mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		fluid(INContent.fluidSolvent, RES_GLASS_X, RES_GLASS_Y, rw, RES_GLASS_H, 0xffffffff);
		GlStateManager.color(1, 1, 1, 1);
		blend(true);
		ClientUtils.bindTexture(IE);
		for(int x : new int[]{CRUDE_X, STRIPPING_X, EFFLUENT_X, PURIFIED_X})
			piece(IE_GAUGE, x, ROW2);
		for(int st = 0; st < 2; st++)
		{
			settlerLiquid(st);
			mixerWindow(st);
		}

		// Energy (IE's red gradient, inside IE's 7 px) and the lamp's lens
		int eh = (int)((ENERGY_H - 4) * (container.energy() / (float)TileChemicalSeparator.ENERGY_CAPACITY));
		if(eh > 0)
			ClientUtils.drawGradientRect(guiLeft + ENERGY_X + 2, guiTop + ENERGY_Y + ENERGY_H - 2 - eh, guiLeft + ENERGY_X + ENERGY_W - 2, guiTop + ENERGY_Y + ENERGY_H - 2, 0xffb51500, 0xff600b00);
		GlStateManager.color(1, 1, 1, 1);
		blend(true);
		ClientUtils.bindTexture(SHEET);
		sprite(LENS, lamp() * LENS[2], LAMP_X + LAMP_SIZE / 2 - 5, LAMP_Y + LAMP_SIZE / 2 - 5);
		blend(false);
	}

	/** Fluid moving through the pipes, lit only where the pipe shows (in the gaps between the equipment). */
	void drawFlows()
	{
		boolean mixA = container.moving(1), mixB = container.moving(2), loaded = container.moving(4);
		int[] ma = MIXERS[0], mb = MIXERS[1], sa = SETTLERS[0], sb = SETTLERS[1];
		int gapA = ma[0] + ma[2], gapMid = sa[0] + sa[2], gapB = mb[0] + mb[2];    // the 4 px gaps: mixer A | settler A | mixer B | settler B
		if(mixA)
		{
			hflow(66, ma[0], UPPER_Y, COL_CRUDE);                                 // crude into mixer A
			hflow(gapA, sa[0], OVERFLOW_Y, COL_MIX);                              // mixer A over into its settler
			vflow(gapA + 1, DROP_TOP, SOLVENT_Y, COL_CLEAN);                      // the clean solvent down from the reservoir
			hflow(gapA, sa[0], SOLVENT_Y, COL_CLEAN);
		}
		if(mixB)
		{
			hflow(66, ma[0], LOWER_Y, COL_STRIPPING);                             // stripping into mixer B (behind stage A)
			hflow(gapA, sa[0], LOWER_Y, COL_STRIPPING);
			hflow(gapMid, mb[0], LOWER_Y, COL_STRIPPING);
			hflow(gapB, sb[0], OVERFLOW_Y, COL_MIX);
		}
		if(loaded || mixB)
		{
			vflow(gapMid + 1, DROP_TOP, SOLVENT_Y, COL_LOADED);                   // the loaded solvent down into mixer B
			hflow(gapMid, mb[0], SOLVENT_Y, COL_LOADED);
		}
		if(container.moving(16))
		{
			hflow(gapMid, mb[0], UPPER_Y, COL_EFFLUENT);                          // settler A's raffinate (behind stage B)
			hflow(gapB, sb[0], UPPER_Y, COL_EFFLUENT);
			hflow(sb[0] + sb[2], 226, UPPER_Y, COL_EFFLUENT);
		}
		if(container.moving(32))
			hflow(sb[0] + sb[2], 226, LOWER_Y, COL_PURIFIED);                    // settler B's aqueous to the purified tank
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

	boolean over(int mx, int my, int x, int y, int w, int h)
	{
		return mx >= guiLeft + x && mx < guiLeft + x + w && my >= guiTop + y && my < guiTop + y + h;
	}

	boolean over(int mx, int my, int[] box)
	{
		return over(mx, my, box[0], box[1], box[2], box[3]);
	}

	static final String K = "gui.immersivenuclear.chemical_separator.";

	@Override
	public void drawScreen(int mx, int my, float partialTicks)
	{
		super.drawScreen(mx, my, partialTicks);
		ArrayList<String> tip = new ArrayList<>();
		tank(tip, container.crude, CRUDE_X, "crude", mx, my);
		tank(tip, container.stripping, STRIPPING_X, "stripping", mx, my);
		tank(tip, container.effluent, EFFLUENT_X, "effluent", mx, my);
		tank(tip, container.purified, PURIFIED_X, "purified", mx, my);
		if(over(mx, my, RES_X, RES_Y, RES_W, RES_H))
		{
			tip.add(I18n.format(K + "solvent"));
			tip.add("§7" + container.solvent.getFluidAmount() + " / " + TileChemicalSeparator.SOLVENT_CAPACITY + " mB");
			tip.add("§7" + I18n.format(K + "makeup", TileChemicalSeparator.MAKEUP_LEVEL));
		}
		for(int st = 0; st < 2; st++)
		{
			String stage = st == 0 ? "a" : "b";
			int[] m = MIXERS[st];
			if(over(mx, my, m[0], m[1] - 18, m[2], m[3] + 18))
			{
				tip.add(I18n.format(K + "mixer_" + stage));
				tip.add("§7" + I18n.format(container.moving(st == 0 ? 1 : 2) ? K + "mixing" : K + "mixer_waiting"));
				tip.add("§7" + I18n.format(K + "batch", Math.round(100 * mixingDone(st))));
			}
			if(over(mx, my, SETTLERS[st]))
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
		int gapMid = SETTLERS[0][0] + SETTLERS[0][2];
		if(over(mx, my, gapMid, DROP_TOP, 4, SOLVENT_Y + 2 - DROP_TOP))
		{
			tip.add(I18n.format(K + "loaded"));
			tip.add("§7" + container.loaded.getFluidAmount() + " / " + TileChemicalSeparator.TANK_CAPACITY + " mB");
		}
		else if(over(mx, my, MIXERS[0][0] + MIXERS[0][2], DROP_TOP, 4, SOLVENT_Y + 2 - DROP_TOP))
			tip.add(I18n.format(K + "clean"));
		if(over(mx, my, ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H))
			tip.add(container.energy() + "/" + TileChemicalSeparator.ENERGY_CAPACITY + " IF");
		if(over(mx, my, LAMP_X, LAMP_Y, LAMP_SIZE, LAMP_SIZE))
		{
			tip.add(I18n.format(K + "state." + container.state()));
			tip.add("§7" + I18n.format(container.redstoneInverted() ? "gui.immersivenuclear.redstone.inverted" : "gui.immersivenuclear.redstone.normal"));
		}
		if(!tip.isEmpty())
		{
			// Long lines wrap, and the tooltip stays on the screen
			GuiUtils.drawHoveringText(tip, mx, my, width, height, GuiGraphite.TOOLTIP_WIDTH, fontRenderer);
			RenderHelper.enableGUIStandardItemLighting();
		}
	}

	void tank(ArrayList<String> tip, FluidTank tank, int x, String name, int mx, int my)
	{
		if(!over(mx, my, x, ROW2, TANK_W, TANK_H))
			return;
		tip.add(I18n.format(K + name));
		if(tank.getFluid() != null)
			tip.add("§7" + tank.getFluid().getLocalizedName());
		tip.add("§7" + tank.getFluidAmount() + " / " + tank.getCapacity() + " mB");
	}
}
