package immersivenuclear.client;

import java.util.ArrayList;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.GuiIEContainerBase;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.block.TileSulfuricAcidUnit;
import immersivenuclear.common.gui.ContainerSulfuricAcidUnit;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.lwjgl.opengl.GL11;

/**
 * Interface of the Sulfuric Acid Unit, in the interface standard (01/11): separate riveted panels with the machine drawn
 * between them, the world showing through the gaps. The contact process left to right, as in the machine:
 *   [water: in / tank / out]  the sulfur's slot over its hopper; the brick BURNER (its blue flame); the BOILER up to its
 *   STEAM DRUM (the level in its glass); the CONVERTER (four catalyst beds, they glow); the ABSORPTION TOWER (the acid
 *   trickling down its packing)  [acid | steam | energy over the status lamp]
 * Our sheet (textures/gui/sulfuric_acid_unit_gui.png, made by ferramentas/gui_acido_sulfurico_v9_esboco.py --jogo with
 * the same numbers) is the fixed background; what moves is drawn here. The JEI page keeps its own sheet.
 */
public class GuiSulfuricAcidUnit extends GuiIEContainerBase
{
	static final String IE = "immersiveengineering:textures/gui/refinery.png";
	static final String SHEET = ImmersiveNuclear.MODID + ":textures/gui/sulfuric_acid_unit_gui.png";
	static final int SHEET_SIZE = 512;

	// Layout (same as gui_acido_sulfurico_v9_esboco.py)
	static final int ROW1 = 37, ROW2 = 62, ROW3 = 117, TANK_Y = ROW2;
	static final int WATER_X = 8, ACID_X = 210, STEAM_X = 240, CONTROL_X = 266;
	static final int BURNER_X = 42, BURNER_Y = 92, BURNER_W = 44, BURNER_H = 44;
	static final int FLAME_X = 51, FLAME_Y = 101, FLAME_W = 26, FLAME_H = 26;
	static final int BOILER_X = 90, BOILER_Y = 80, BOILER_W = 28, BOILER_H = 56;
	static final int DRUM_X = 86, DRUM_Y = 62, DRUM_W = 36, DRUM_H = 16, GLASS_X = 94, GLASS_Y = 66, GLASS_W = 20, GLASS_H = 6;
	/** The converter; also JEI's click area (INJeiPlugin). */
	public static final int CONV_X = 126, CONV_Y = 40, CONV_W = 26, CONV_H = 96;
	static final int BED_Y0 = 46, BED_STEP = 22, BED_H = 14, BED_X = 131, BED_W = 16;
	static final int TOWER_X = 162, TOWER_Y = 32, TOWER_W = 30, TOWER_H = 104;
	static final int PACK_X = 168, PACK_Y = 40, PACK_W = 18, PACK_H = 80;
	static final int ENERGY_X = CONTROL_X + 2, ENERGY_Y = ROW1, ENERGY_W = 11, ENERGY_H = ROW2 + 51 - ROW1;
	static final int LAMP_X = CONTROL_X, LAMP_Y = ROW3 + 2, LAMP_SIZE = 16;
	static final int PANEL_X = ContainerSulfuricAcidUnit.INV_X - 8, PANEL_Y = ContainerSulfuricAcidUnit.INV_Y - 8;

	// Our sprites in the sheet: {u, v, w, h}
	static final int[] LENS = {0, 330, 10, 10};
	// IE's pieces in refinery.png
	static final int[] IE_SLOT = {35, 13, 20, 20}, IE_TANK = {11, 18, 20, 51}, IE_BLUE = {42, 10, 6, 3}, IE_ORANGE = {42, 49, 6, 3};

	static final int COL_WATER = 0xff3a6ac0, COL_ACID = 0xffd8d0a8, COL_ACID_DROP = 0xa0d8d0a8, COL_STEAM = 0xc8e0e4e8, COL_GAS = 0xb4d8c070;
	static final int[] FLAME = {0xff223ab0, 0xff4a8af0, 0xffd0e8ff};

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

	/** The lamp's colour: 0 green running, 1 amber waiting (input, or stopped by redstone), 2 red stopped (no power,
	 * output full), 3 off (idle). */
	int lamp()
	{
		switch(container.state)
		{
			case TileSulfuricAcidUnit.STATE_RUNNING: return 0;
			case TileSulfuricAcidUnit.STATE_NO_INPUT:
			case TileSulfuricAcidUnit.STATE_DISABLED: return 1;
			case TileSulfuricAcidUnit.STATE_NO_POWER:
			case TileSulfuricAcidUnit.STATE_OUTPUT_FULL: return 2;
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
		int[][] xy = ContainerSulfuricAcidUnit.SLOT_XY;
		for(int i = 0; i < xy.length; i++)
		{
			piece(IE_SLOT, xy[i][0] - 2, xy[i][1] - 2);
			boolean in = i == TileSulfuricAcidUnit.SLOT_SULFUR || i == TileSulfuricAcidUnit.SLOT_ACID_IN || i == TileSulfuricAcidUnit.SLOT_WATER_IN
					|| i == TileSulfuricAcidUnit.SLOT_STEAM_IN;
			piece(in ? IE_BLUE : IE_ORANGE, xy[i][0] + 5, xy[i][1] - 5);
		}
		for(int x : new int[]{WATER_X, ACID_X, STEAM_X})
		{
			piece(IE_TANK, x, TANK_Y);
			piece(x == WATER_X ? IE_BLUE : IE_ORANGE, x + 7, TANK_Y - 3);
		}
		GuiGraphite.endTint();
		GlStateManager.color(1, 1, 1, 1);
		ClientUtils.handleGuiTank(container.water, guiLeft + WATER_X + 2, guiTop + TANK_Y + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);
		ClientUtils.handleGuiTank(container.acid, guiLeft + ACID_X + 2, guiTop + TANK_Y + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);
		drawSteamTank();

		drawFlows();
		drawFlame();
		drawDrum();
		drawConverter();
		drawTower();

		// Energy (IE's red gradient, inside IE's 7 px) and the lamp's lens
		int eh = (int)((ENERGY_H - 4) * (container.energy / (float)TileSulfuricAcidUnit.ENERGY_CAPACITY));
		if(eh > 0)
			ClientUtils.drawGradientRect(guiLeft + ENERGY_X + 2, guiTop + ENERGY_Y + ENERGY_H - 2 - eh, guiLeft + ENERGY_X + ENERGY_W - 2, guiTop + ENERGY_Y + ENERGY_H - 2, 0xffb51500, 0xff600b00);
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.enableBlend();
		ClientUtils.bindTexture(SHEET);
		Gui.drawModalRectWithCustomSizedTexture(guiLeft + LAMP_X + LAMP_SIZE / 2 - 5, guiTop + LAMP_Y + LAMP_SIZE / 2 - 5, LENS[0] + lamp() * LENS[2], LENS[1], LENS[2], LENS[3], SHEET_SIZE, SHEET_SIZE);
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

	/** What moves in the visible stretches of the pipes while a cycle runs. */
	void drawFlows()
	{
		if(!running())
			return;
		// water: up out of its panel, over everything, down into the tower's top
		rect(17, 19, 2, 9, COL_WATER);
		rect(17, 19, 161, 2, COL_WATER);
		rect(176, 19, 2, 8, COL_WATER);
		// steam: up from the drum, across (where it shows between the vessels) to the steam tank
		rect(103, 47, 2, DRUM_Y - 47, COL_STEAM);
		rect(103, 47, CONV_X - 103, 2, COL_STEAM);
		rect(CONV_X + CONV_W, 47, TOWER_X - CONV_X - CONV_W, 2, COL_STEAM);
		rect(TOWER_X + TOWER_W, 47, 202 - TOWER_X - TOWER_W, 2, COL_STEAM);
		// the gas: boiler to the converter, converter to the tower's foot; the acid out of the tower's foot
		rect(BOILER_X + BOILER_W, 105, CONV_X - BOILER_X - BOILER_W, 2, COL_GAS);
		rect(CONV_X + CONV_W, 121, TOWER_X - CONV_X - CONV_W, 2, COL_GAS);
		rect(TOWER_X + TOWER_W, 127, 202 - TOWER_X - TOWER_W, 2, COL_ACID);
	}

	/** The blue flame, flickering while it burns (the part of the machine that glows). */
	void drawFlame()
	{
		if(!running())
			return;
		float f = 0.82F + 0.1F * (float)Math.sin(ticks * 0.9) + 0.06F * (float)Math.sin(ticks * 2.3 + 1);
		float cx = FLAME_W / 2F, cy = FLAME_H / 2F + 2;
		for(int j = 0; j < FLAME_H; j++)
		{
			// three bands: outer, middle, core; each row's half width from the band's limit
			for(int band = 0; band < 3; band++)
			{
				float limit = f * (band == 0 ? 1 : band == 1 ? 0.7F : 0.35F);
				// tall at the top: the flame licks upwards
				float dy = Math.abs(j + 0.5F - cy) / (FLAME_H / 2F) * (j < cy ? 0.5F : 0.8F);
				float half = (limit - dy) * (FLAME_W / 2F);
				if(half <= 0)
					continue;
				int x0 = Math.round(cx - half), x1 = Math.round(cx + half);
				rect(FLAME_X + Math.max(0, x0), FLAME_Y + j, Math.min(FLAME_W, x1) - Math.max(0, x0), 1, FLAME[band]);
			}
		}
	}

	/** The water's level in the steam drum's glass, boiling while it runs. */
	void drawDrum()
	{
		float lvl = container.water.getFluidAmount() / (float)TileSulfuricAcidUnit.WATER_CAPACITY;
		int h = container.water.getFluidAmount() > 0 ? Math.max(1, Math.round(GLASS_H * Math.min(1, 0.3F + 0.7F * lvl))) : 0;
		rect(GLASS_X, GLASS_Y + GLASS_H - h, GLASS_W, h, COL_WATER);
		if(running() && h > 1)
			for(int k = 0; k < 4; k++)
				rect(GLASS_X + 2 + Math.floorMod(k * 5 + ticks / 2, GLASS_W - 3), GLASS_Y + GLASS_H - h, 1, 1, 0xffd0e0f8);
	}

	/** Four catalyst beds, hottest at the top; they glow while it runs, each brightening as the gas passes it. */
	void drawConverter()
	{
		boolean on = running();
		int cycle = 4 * 20;
		for(int k = 0; k < 4; k++)
		{
			int y0 = BED_Y0 + k * BED_STEP + 5, h = BED_H - 5;
			float pass = on ? Math.max(0, (float)Math.cos(2 * Math.PI * (Math.floorMod(ticks - k * 20, cycle) / (float)cycle))) : 0;
			float hot = (1 - k / 4F) * (on ? 0.85F + 0.15F * pass : 0.35F);
			int r = Math.min(255, Math.round(100 + 130 * hot)), g = Math.min(255, Math.round(56 + 70 * hot)), b = 28;
			rect(BED_X, y0, BED_W, h, 0xff000000 | r << 16 | g << 8 | b);
			for(int i = BED_X + 1; i < BED_X + BED_W - 1; i += 3)
			{
				boolean spark = on && Math.floorMod(ticks / 3 + i * 7 + k * 13, 11) == 0;
				rect(i, y0 + (i % 2), 1, 1, spark ? 0xfffff0b0 : 0xff000000 | Math.min(255, r + 40) << 16 | Math.min(255, g + 40) << 8 | 50);
			}
			// the gas over the bed, drifting down
			if(on)
				for(int i = BED_X + 1 + Math.floorMod(ticks / 4 + k, 3); i < BED_X + BED_W - 1; i += 3)
					rect(i, y0 - 4 + Math.floorMod(ticks / 5 + i, 4), 1, 1, COL_GAS);
		}
	}

	/** The acid trickling down the packing while it runs, and the acid at the tower's foot. */
	void drawTower()
	{
		boolean on = running();
		if(on)
			for(int i = PACK_X + 2; i < PACK_X + PACK_W - 1; i += 4)
				for(int k = 0; k < 4; k++)
				{
					int y = PACK_Y + 3 + Math.floorMod(ticks * 2 + i * 5 + k * 19, PACK_H - 12);
					rect(i, y, 1, 3, COL_ACID_DROP);
				}
		int pool = on ? 6 : container.acid.getFluidAmount() > 0 ? 3 : 0;
		rect(PACK_X, PACK_Y + PACK_H - pool, PACK_W, pool, COL_ACID);
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
			ClientUtils.handleGuiTank(tt[i], guiLeft + tx[i] + 2, guiTop + TANK_Y + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, tip);
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
			tip.add("§7" + I18n.format(K + "cycle", TileSulfuricAcidUnit.WATER_PER_CYCLE, TileSulfuricAcidUnit.ACID_PER_CYCLE,
					TileSulfuricAcidUnit.STEAM_PER_CYCLE, TileSulfuricAcidUnit.CYCLE_TICKS / 20));
			if(container.progress > 0)
				tip.add("§7" + I18n.format(K + "progress", Math.round(100 * done())));
		}
		if(over(mx, my, TOWER_X, TOWER_Y, TOWER_W, TOWER_H))
		{
			tip.add(I18n.format(K + "tower"));
			tip.add("§7" + I18n.format(K + "tower_info"));
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
		if(over(mx, my, LAMP_X, LAMP_Y, LAMP_SIZE, LAMP_SIZE))
		{
			tip.add(I18n.format(K + "state." + container.state));
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
