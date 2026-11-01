package immersivenuclear.client;

import java.util.ArrayList;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.GuiIEContainerBase;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.block.TileHFKiln;
import immersivenuclear.common.gui.ContainerHFKiln;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.client.config.GuiUtils;

/**
 * Interface of the Hydrofluoric Acid Kiln, in the mod's graphite finish ({@link GuiGraphite}) with IE's own slots,
 * tanks and markers a little darker, and the same right column as the other machines (energy, redstone button,
 * lights). The kiln left to right, as in the machine:
 *   sulfuric acid tank + fluorite > FEED HEAD > ROTARY DRUM (heated zone, riding rings, girth gear, motor) > DISCHARGE
 *   HEAD > gypsum; the gas over the top > SCRUBBER > CONDENSER > hydrofluoric acid tank
 * While a cycle runs the heating coils glow and pulse, the drum's shell bands move (it turns), the bed goes from purple
 * fluorite to white gypsum along it, the girth gear's teeth turn, gypsum falls into its slot and the gas moves through
 * the lines.
 */
public class GuiHFKiln extends GuiIEContainerBase
{
	static final String IE = "immersiveengineering:textures/gui/refinery.png";
	static final String OWN = ImmersiveNuclear.MODID + ":textures/gui/chemical_processor.png";

	// Layout (as in the sketch, ferramentas/gui_forno_hf.py v2), on one grid: the tanks, the feed and discharge heads and the
	// scrubber over the condenser share the band y 36..87; the drum is centred on it, the motor under the drum; the slots
	// under the tanks, the gypsum and the arrow share a row, the redstone button the lower one
	static final int ACID_X = 8, HF_X = 222, TANK_Y = 36;
	static final int FEED_X = 32, FEED_Y = 36, FEED_W = 28, FEED_H = 51;
	public static final int DRUM_X = 64, DRUM_Y = 47, DRUM_W = 96, DRUM_H = 30;
	static final int MOTOR_X = 102, MOTOR_Y = 82, MOTOR_W = 20, MOTOR_H = 14;
	static final int DISCH_X = 164, DISCH_Y = 36, DISCH_W = 22, DISCH_H = 51;
	static final int SCRUB_X = 196, SCRUB_Y = 36, SCRUB_W = 18, SCRUB_H = 22;
	static final int COND_X = 196, COND_Y = 65, COND_W = 18, COND_H = 22;
	public static final int ARROW_X = 142, ARROW_Y = 100;
	/** The gas: up out of the feed head, over everything, down into the scrubber. */
	static final int GAS_Y = 22, GAS_UP_X = 56;
	static final int ENERGY_X = 248, ENERGY_Y = 36, ENERGY_W = 14, ENERGY_H = 80;
	static final int BUTTON_X = 247, BUTTON_Y = 122, BUTTON_SIZE = 16;
	static final int LIGHTS_X = 248, LIGHTS_W = 14, LIGHT_X = 251, LIGHT_Y = 148, LIGHT_STEP = 8;
	static final int PANEL_X = ContainerHFKiln.INV_X - 8, PANEL_Y = ContainerHFKiln.INV_Y - 8;

	// IE's pieces in refinery.png
	static final int[] IE_SLOT = {35, 13, 20, 20}, IE_TANK = {11, 18, 20, 51}, IE_BLUE = {42, 10, 6, 3}, IE_ORANGE = {42, 49, 6, 3};

	static final int COL_PIPE = 0xff2a2c30, COL_PIPE_EDGE = 0xff55585e, COL_STEEL = 0xff96999e, COL_STEEL_DARK = 0xff5c5f64,
			COL_INSIDE = 0xff24262a, COL_HEAD = 0xff3a3d42, COL_SHELL = 0xff6a3e2a, COL_SHELL_BAND = 0xff5a3424, COL_COIL = 0xffe8783c,
			COL_COIL_OFF = 0xff7a4a32, COL_FLUORITE = 0xffa06cd0, COL_GYPSUM = 0xffeeece2, COL_ACID = 0xffd6c896, COL_GAS = 0xc8b4e6dc,
			COL_HF = 0xff5ab4a4, COL_MOTOR = 0xff3c6a3c, COL_FINS = 0xff9cb8c4;

	final ContainerHFKiln container;
	int ticks;

	public GuiHFKiln(ContainerHFKiln container)
	{
		super(container);
		this.container = container;
		xSize = ContainerHFKiln.WIDTH;
		ySize = ContainerHFKiln.HEIGHT;
	}

	boolean running()
	{
		return container.state == TileHFKiln.STATE_RUNNING;
	}

	float done()
	{
		return container.progress / (float)TileHFKiln.CYCLE_TICKS;
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

	static int mix(int a, int b, float t)
	{
		t = Math.max(0, Math.min(1, t));
		int c = 0xff000000;
		for(int sh = 0; sh <= 16; sh += 8)
			c |= Math.round((a >> sh & 255) + ((b >> sh & 255) - (a >> sh & 255)) * t) << sh;
		return c;
	}

	/** 0 green running, 1 yellow waiting for input, 2 red stopped (power, full output), 3 grey disabled; -1 idle. */
	int litLight()
	{
		switch(container.state)
		{
			case TileHFKiln.STATE_RUNNING: return 0;
			case TileHFKiln.STATE_NO_INPUT: return 1;
			case TileHFKiln.STATE_NO_POWER:
			case TileHFKiln.STATE_OUTPUT_FULL: return 2;
			case TileHFKiln.STATE_DISABLED: return 3;
			default: return -1;
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mx, int my)
	{
		GuiGraphite.panel(guiLeft, guiTop, xSize, ySize);

		// Pipes first, with what moves through them while it runs; the wells over their ends
		int acidY = TANK_Y + 30;
		pipe(ACID_X + 20, acidY, FEED_X - 1, acidY);
		flow(ACID_X + 20, acidY, FEED_X - 1, acidY, COL_ACID);
		int scrubMid = SCRUB_X + SCRUB_W / 2;
		pipe(GAS_UP_X, FEED_Y - 1, GAS_UP_X, GAS_Y);
		pipe(GAS_UP_X, GAS_Y, scrubMid, GAS_Y);
		pipe(scrubMid, GAS_Y, scrubMid, SCRUB_Y - 1);
		pipe(scrubMid, SCRUB_Y + SCRUB_H, scrubMid, COND_Y - 1);
		flow(GAS_UP_X, FEED_Y - 1, GAS_UP_X, GAS_Y, COL_GAS);
		flow(GAS_UP_X, GAS_Y, scrubMid, GAS_Y, COL_GAS);
		flow(scrubMid, GAS_Y, scrubMid, SCRUB_Y - 1, COL_GAS);
		flow(scrubMid, SCRUB_Y + SCRUB_H, scrubMid, COND_Y - 1, COL_GAS);
		int hfY = COND_Y + COND_H - 8;
		pipe(COND_X + COND_W, hfY, HF_X - 1, hfY);
		flow(COND_X + COND_W, hfY, HF_X - 1, hfY, COL_HF);
		pipe(MOTOR_X + MOTOR_W / 2, MOTOR_Y - 1, MOTOR_X + MOTOR_W / 2, DRUM_Y + DRUM_H);

		ClientUtils.bindTexture(GuiGraphite.SHEET);
		GlStateManager.color(1, 1, 1, 1);
		for(int[] w : new int[][]{{FEED_X, FEED_Y, FEED_W, FEED_H}, {DRUM_X, DRUM_Y, DRUM_W, DRUM_H}, {MOTOR_X, MOTOR_Y, MOTOR_W, MOTOR_H},
				{DISCH_X, DISCH_Y, DISCH_W, DISCH_H}, {SCRUB_X, SCRUB_Y, SCRUB_W, SCRUB_H}, {COND_X, COND_Y, COND_W, COND_H},
				{ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H}, {LIGHTS_X, LIGHT_Y - 3, LIGHTS_W, 4 * LIGHT_STEP + 4}})
			GuiGraphite.well(guiLeft + w[0], guiTop + w[1], w[2], w[3]);
		GuiGraphite.arrow(guiLeft + ARROW_X, guiTop + ARROW_Y, Math.round(GuiGraphite.ARROW_W * done()));

		// IE's wooden inventory panel; IE's pieces, a little darker
		GlStateManager.color(1, 1, 1, 1);
		ClientUtils.bindTexture(IE);
		drawTexturedModalRect(guiLeft + PANEL_X, guiTop + PANEL_Y, 0, 77, 176, 89);
		GuiGraphite.ieTint();
		int[][] xy = ContainerHFKiln.SLOT_XY;
		for(int i = 0; i < xy.length; i++)
		{
			piece(IE_SLOT, xy[i][0] - 2, xy[i][1] - 2);
			boolean out = i == TileHFKiln.SLOT_GYPSUM || i == TileHFKiln.SLOT_ACID_OUT || i == TileHFKiln.SLOT_HF_OUT;
			piece(out ? IE_ORANGE : IE_BLUE, xy[i][0] + 5, xy[i][1] - 5);
		}
		for(int x : new int[]{ACID_X, HF_X})
		{
			piece(IE_TANK, x, TANK_Y);
			piece(x == ACID_X ? IE_BLUE : IE_ORANGE, x + 7, TANK_Y - 3);
		}
		GuiGraphite.endTint();
		tank(container.acid, ACID_X, mx, my, null);
		tank(container.hf, HF_X, mx, my, null);

		drawChutes();
		drawHeads();
		drawDrum();
		drawGasTrain();

		// Energy (IE's red gradient), button, lights
		int eh = (int)((ENERGY_H - 4) * (container.energy / (float)TileHFKiln.ENERGY_CAPACITY));
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

	/** The fluorite's chute into the feed head and the gypsum's out of the discharge head, falling while it runs. */
	void drawChutes()
	{
		int[] f = ContainerHFKiln.SLOT_XY[TileHFKiln.SLOT_FLUORITE];
		int x = f[0] + 3, y = f[1] + 18, h = FEED_Y - y;
		rect(x, y, 10, h, COL_STEEL_DARK);
		rect(x + 2, y + 2, 6, h - 2, COL_PIPE);
		int[] g = ContainerHFKiln.SLOT_XY[TileHFKiln.SLOT_GYPSUM];
		int gx = DISCH_X + 6, gy = DISCH_Y + DISCH_H, gh = g[1] - 2 - gy;
		rect(gx, gy, 10, gh, COL_STEEL_DARK);
		rect(gx + 2, gy, 6, gh - 2, COL_PIPE);
		if(running())
			for(int k = 0; k < 2; k++)
			{
				rect(x + 3 + k * 2, y + 2 + Math.floorMod(ticks + k * 5, Math.max(1, h - 3)), 2, 2, COL_FLUORITE);
				rect(gx + 3 + k * 2, gy + Math.floorMod(ticks + k * 4, Math.max(1, gh - 3)), 2, 2, COL_GYPSUM);
			}
	}

	/** The feed and discharge heads: steel boxes with the drum's seals; fluorite going in, gypsum coming out. */
	void drawHeads()
	{
		for(int[] h : new int[][]{{FEED_X, FEED_Y, FEED_W, FEED_H}, {DISCH_X, DISCH_Y, DISCH_W, DISCH_H}})
		{
			rect(h[0] + 2, h[1] + 2, h[2] - 4, h[3] - 4, COL_HEAD);
			rect(h[0] + 4, h[1] + h[3] / 2 - 8, h[2] - 8, 16, COL_INSIDE);
		}
		rect(FEED_X + 6, FEED_Y + FEED_H / 2 - 4, FEED_W - 12, 8, COL_FLUORITE);
		for(int i = 0; i < 4; i++)
			rect(DISCH_X + 5 + i * 3, DISCH_Y + DISCH_H / 2 + 2, 2, 2, COL_GYPSUM);
	}

	/** The drum, cut open: its shell (bands moving while it turns), the bed from fluorite to gypsum, the heating coils on
	 * the hot zone (glowing and pulsing while it runs), the riding rings, the girth gear (teeth moving) and the motor. */
	void drawDrum()
	{
		boolean on = running();
		int ix = DRUM_X + 2, iy = DRUM_Y + 2, iw = DRUM_W - 4, ih = DRUM_H - 4;
		rect(ix, iy, iw, ih, COL_SHELL);
		int turn = on ? ticks / 2 % 3 : 0;
		for(int j = iy + turn; j < iy + ih; j += 3)
			rect(ix, j, iw, 1, COL_SHELL_BAND);
		rect(ix + 4, iy + 5, iw - 8, ih - 10, 0xff1c1818);
		for(int i = 0; i < iw - 8; i++)
		{
			float t = i / (float)(iw - 9);
			// the bed tumbles a little while the drum turns
			int wave = on ? Math.round((float)Math.sin((i + ticks * 0.8) / 4.0)) : 0;
			int top = iy + ih - 11 - Math.round(2 * (1 - t)) + wave;
			rect(ix + 4 + i, top, 1, iy + ih - 5 - top, mix(COL_FLUORITE, COL_GYPSUM, t));
		}
		for(int i = ix + 4, k = 0; i < ix + 34; i += 3, k++)
		{
			float glow = on ? 0.7F + 0.3F * (float)Math.sin(ticks * 0.3 + k) : 0;
			int c = mix(COL_COIL_OFF, COL_COIL, glow);
			rect(i, iy, 2, 3, c);
			rect(i, iy + ih - 3, 2, 3, c);
		}
		for(int rx : new int[]{ix + 40, ix + iw - 12})
			rect(rx, DRUM_Y - 2, 4, DRUM_H + 4, COL_STEEL);
		int gx = ix + iw / 2 - 3, shift = on ? ticks / 3 % 3 : 0;
		rect(gx, DRUM_Y - 3, 6, DRUM_H + 6, 0xff4a4c50);
		for(int j = DRUM_Y - 3 + shift; j < DRUM_Y + DRUM_H + 3; j += 3)
			rect(gx - 1, j, 8, 1, COL_STEEL);
		rect(MOTOR_X + 3, MOTOR_Y + 3, MOTOR_W - 6, MOTOR_H - 6, COL_MOTOR);
		rect(MOTOR_X + 4, MOTOR_Y + 5, MOTOR_W - 8, 1, 0xff5a905a);
	}

	/** The scrubber's cartridges, the condenser's fins (drops of acid running down them while it runs). */
	void drawGasTrain()
	{
		rect(SCRUB_X + 2, SCRUB_Y + 2, SCRUB_W - 4, SCRUB_H - 4, COL_INSIDE);
		for(int j = SCRUB_Y + 6; j < SCRUB_Y + SCRUB_H - 4; j += 5)
			rect(SCRUB_X + 3, j, SCRUB_W - 6, 2, 0xff8c8e92);
		rect(COND_X + 2, COND_Y + 2, COND_W - 4, COND_H - 4, COL_INSIDE);
		for(int j = COND_Y + 4; j < COND_Y + COND_H - 3; j += 2)
			rect(COND_X + 3, j, COND_W - 6, 1, COL_FINS);
		if(running())
			for(int k = 0; k < 2; k++)
				rect(COND_X + 5 + k * 5, COND_Y + 4 + Math.floorMod(ticks + k * 7, COND_H - 8), 1, 2, COL_HF);
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

	static final String K = "gui.immersivenuclear.hf_kiln.";

	@Override
	public void drawScreen(int mx, int my, float partialTicks)
	{
		super.drawScreen(mx, my, partialTicks);
		ArrayList<String> tip = new ArrayList<>();
		String[] tanks = {"acid", "hf"};
		int[] tx = {ACID_X, HF_X};
		FluidTank[] tt = {container.acid, container.hf};
		for(int i = 0; i < 2; i++)
		{
			tank(tt[i], tx[i], mx, my, tip);
			if(over(mx, my, tx[i], TANK_Y, 20, 51))
				tip.add(0, I18n.format(K + tanks[i]));
		}
		String[][] parts = {{"feed"}, {"drum"}, {"motor"}, {"discharge"}, {"scrubber"}, {"condenser"}};
		int[][] boxes = {{FEED_X, FEED_Y, FEED_W, FEED_H}, {DRUM_X, DRUM_Y, DRUM_W, DRUM_H}, {MOTOR_X, MOTOR_Y, MOTOR_W, MOTOR_H},
				{DISCH_X, DISCH_Y, DISCH_W, DISCH_H}, {SCRUB_X, SCRUB_Y, SCRUB_W, SCRUB_H}, {COND_X, COND_Y, COND_W, COND_H}};
		for(int i = 0; i < boxes.length; i++)
			if(over(mx, my, boxes[i][0], boxes[i][1], boxes[i][2], boxes[i][3]))
			{
				tip.add(I18n.format(K + parts[i][0]));
				tip.add("§7" + I18n.format(K + parts[i][0] + "_info"));
			}
		if(over(mx, my, ARROW_X, ARROW_Y, GuiGraphite.ARROW_W, GuiGraphite.ARROW_H))
		{
			tip.add(I18n.format(K + "cycle", TileHFKiln.ACID_PER_CYCLE, TileHFKiln.HF_PER_CYCLE, TileHFKiln.GYPSUM_PER_CYCLE, TileHFKiln.CYCLE_TICKS / 20));
			if(container.progress > 0)
				tip.add("§7" + I18n.format(K + "progress", Math.round(100 * done())));
		}
		String[] slotTips = {"fluorite", "gypsum", "acid_in", "acid_out", "hf_in", "hf_out"};
		int[][] xy = ContainerHFKiln.SLOT_XY;
		for(int i = 0; i < xy.length; i++)
			if(over(mx, my, xy[i][0], xy[i][1], 16, 16) && container.getSlot(i).getStack().isEmpty())
				tip.add(I18n.format(K + slotTips[i]));
		if(over(mx, my, ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H))
		{
			tip.add(container.energy + "/" + TileHFKiln.ENERGY_CAPACITY + " IF");
			tip.add("§7" + I18n.format(K + "energy", TileHFKiln.ENERGY_PER_TICK));
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
			mc.playerController.sendEnchantPacket(inventorySlots.windowId, ContainerHFKiln.BUTTON_REDSTONE);
		}
	}
}
