package immersivenuclear.client;

import java.util.ArrayList;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.GuiIEContainerBase;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.block.TileEnrichmentBase;
import immersivenuclear.common.gui.ContainerEnrichmentUnit;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.client.config.GuiUtils;

/**
 * Interface of the Enrichment Unit, one for its three assemblies: the sheet (ferramentas/gui_enriquecimento_v2_esboco.py --jogo) has the six banks dark, and the banks the assembly has are lit over it.
 * While it runs the gases move in their headers, the converters' lamps follow the machine and a light runs round the rotors, faster as they spin up.
 */
public class GuiEnrichmentUnit extends GuiIEContainerBase
{
	static final String IE = "immersiveengineering:textures/gui/refinery.png";
	static final String SHEET = ImmersiveNuclear.MODID + ":textures/gui/enrichment_unit_gui.png";
	static final int SHEET_SIZE = 512;

	// Layout (same as gui_enriquecimento_v2_esboco.py)
	static final int ROW2 = 62, ROW3 = 117, ROW1 = 37, TANK_Y = ROW2;
	static final int FEED_X = 8, PRODUCT_X = 250, TAILS_X = 280, CONTROL_X = 306;
	static final int CASCADE_X = 112, COL_W = 14, PITCH = 17, COL_TOP = 44, COL_H = 92, FEED_Y = 41, PRODUCT_Y = 50, TAILS_Y = 59, PURGE_P = 215, PURGE_T = 228;
	static final int ENERGY_X = CONTROL_X + 2, ENERGY_Y = ROW1, ENERGY_W = 11, ENERGY_H = ROW2 + 51 - ROW1;
	static final int LAMP_X = CONTROL_X, LAMP_Y = ROW3 + 2, LAMP_SIZE = 16;
	static final int PANEL_X = ContainerEnrichmentUnit.INV_X - 8, PANEL_Y = ContainerEnrichmentUnit.INV_Y - 8;
	static final int[] LENS = {0, 330, 10, 10};
	/** The cascade: JEI's click area. */
	public static final int AREA_X = CASCADE_X, AREA_Y = 70, AREA_W = 6 * PITCH - 3, AREA_H = 66;
	static final int BANKS_V = 345;
	static final int[] IE_SLOT = {35, 13, 20, 20}, IE_TANK = {11, 18, 20, 51}, IE_BLUE = {42, 10, 6, 3}, IE_ORANGE = {42, 49, 6, 3};
	static final int COL_NATURAL = 0xc8a6a2ac, COL_PRODUCT = 0xc876817d, COL_TAILS = 0xc82a2332;
	static final int LED_RED = 0xffe04034, LED_GREEN = 0xff60d860;
	static final String K = "gui.immersivenuclear.enrichment_unit.";

	final ContainerEnrichmentUnit container;
	final int banks;
	int ticks;

	public GuiEnrichmentUnit(ContainerEnrichmentUnit container)
	{
		super(container);
		this.container = container;
		banks = container.tile.banks();
		xSize = ContainerEnrichmentUnit.WIDTH;
		ySize = ContainerEnrichmentUnit.HEIGHT;
	}

	boolean running()
	{
		return container.state == TileEnrichmentBase.STATE_RUNNING;
	}

	@Override
	public void updateScreen()
	{
		super.updateScreen();
		ticks++;
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

	int lamp()
	{
		switch(container.state)
		{
			case TileEnrichmentBase.STATE_RUNNING: return 0;
			case TileEnrichmentBase.STATE_NO_INPUT:
			case TileEnrichmentBase.STATE_DISABLED: return 1;
			case TileEnrichmentBase.STATE_NO_POWER:
			case TileEnrichmentBase.STATE_OUTPUT_FULL: return 2;
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
		for(int b = 0; b < banks; b++)
			Gui.drawModalRectWithCustomSizedTexture(guiLeft + CASCADE_X + b * PITCH, guiTop + COL_TOP, b * (COL_W + 1), BANKS_V, COL_W, COL_H, SHEET_SIZE, SHEET_SIZE);

		ClientUtils.bindTexture(IE);
		drawTexturedModalRect(guiLeft + PANEL_X, guiTop + PANEL_Y, 0, 77, 176, 89);
		GuiGraphite.ieTint();
		int[][] xy = ContainerEnrichmentUnit.SLOT_XY;
		for(int i = 0; i < xy.length; i++)
		{
			piece(IE_SLOT, xy[i][0] - 2, xy[i][1] - 2);
			piece(i % 2 == 0 ? IE_BLUE : IE_ORANGE, xy[i][0] + 5, xy[i][1] - 5);
		}
		for(int x : new int[]{FEED_X, PRODUCT_X, TAILS_X})
		{
			piece(IE_TANK, x, TANK_Y);
			piece(x == FEED_X ? IE_BLUE : IE_ORANGE, x + 7, TANK_Y - 3);
		}
		GuiGraphite.endTint();
		GlStateManager.color(1, 1, 1, 1);
		FluidTank[] tanks = {container.feed, container.product, container.tails};
		int[] tx = {FEED_X, PRODUCT_X, TAILS_X};
		for(int i = 0; i < 3; i++)
			ClientUtils.handleGuiTank(tanks[i], guiLeft + tx[i] + 2, guiTop + TANK_Y + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, null);

		drawMoving();

		int eh = (int)((ENERGY_H - 4) * Math.min(1, container.energy / 32000F));
		if(eh > 0)
			ClientUtils.drawGradientRect(guiLeft + ENERGY_X + 2, guiTop + ENERGY_Y + ENERGY_H - 2 - eh, guiLeft + ENERGY_X + ENERGY_W - 2, guiTop + ENERGY_Y + ENERGY_H - 2, 0xffb51500, 0xff600b00);
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.enableBlend();
		ClientUtils.bindTexture(SHEET);
		Gui.drawModalRectWithCustomSizedTexture(guiLeft + LAMP_X + LAMP_SIZE / 2 - 5, guiTop + LAMP_Y + LAMP_SIZE / 2 - 5, LENS[0] + lamp() * LENS[2], LENS[1], LENS[2], LENS[3], SHEET_SIZE, SHEET_SIZE);
	}

	void drawMoving()
	{
		boolean on = running();
		for(int k = 0; k < 4; k++)                           // the converters' lamps: red at the left, green at the right
			rect(on ? 74 + k * 10 : 71 + k * 10, 86, 1, 1, on ? LED_GREEN : LED_RED);
		if(!on)
			return;
		int product = COL_PRODUCT;
		rect(36, 105, 8, 2, COL_NATURAL);                    // the natural hexafluoride into the autoclave and up
		rect(55, 46, 2, 26, COL_NATURAL);
		rect(60, FEED_Y + 1, CASCADE_X - 3 - 60, 2, COL_NATURAL);
		for(int b = 0; b < 6; b++)
		{
			int x = CASCADE_X - 1 + b * PITCH;
			if(b < banks)
				rect(x, FEED_Y + 1, PITCH - 2, 2, COL_NATURAL);
			rect(x, PRODUCT_Y + 1, PITCH - 2, 2, product);
			rect(x, TAILS_Y + 1, PITCH - 2, 2, COL_TAILS);
		}
		rect(PURGE_P + 5, PRODUCT_Y + 5, 2, 28, product);    // down into the purgers, on to the tanks
		rect(PURGE_T + 5, TAILS_Y + 5, 2, 19, COL_TAILS);
		rect(238, 98, 4, 2, product);
		rect(239, 110, 3, 2, COL_TAILS);
		// a light runs round each rotor, faster as it spins up
		int step = container.spin >= TileEnrichmentBase.SPIN_UP_TICKS ? 1 : 3;
		for(int b = 0; b < banks; b++)
			rect(CASCADE_X + b * PITCH + 2 + Math.floorMod((ticks + b * 3) / step, 10), 74, 2, 1, 0xfff0f4f8);
	}

	boolean over(int mx, int my, int x, int y, int w, int h)
	{
		return mx >= guiLeft + x && mx < guiLeft + x + w && my >= guiTop + y && my < guiTop + y + h;
	}

	@Override
	public void drawScreen(int mx, int my, float partialTicks)
	{
		super.drawScreen(mx, my, partialTicks);
		ArrayList<String> tip = new ArrayList<>();
		TileEnrichmentBase t = container.tile;
		String[] names = {"feed", "product", "tails"};
		int[] tx = {FEED_X, PRODUCT_X, TAILS_X};
		FluidTank[] tt = {container.feed, container.product, container.tails};
		for(int i = 0; i < 3; i++)
		{
			ClientUtils.handleGuiTank(tt[i], guiLeft + tx[i] + 2, guiTop + TANK_Y + 2, 16, 47, 177, 31, 20, 51, mx, my, IE, tip);
			if(over(mx, my, tx[i], TANK_Y, 20, 51))
				tip.add(0, I18n.format(K + names[i]));
		}
		if(over(mx, my, 44, 66, 22, 70))
			tip.add(I18n.format(K + "autoclave"));
		if(over(mx, my, 68, 77, 41, 59))
			tip.add(I18n.format(K + "converters"));
		if(over(mx, my, CASCADE_X, 70, 6 * PITCH - 3, 66))
		{
			tip.add(I18n.format(K + "cascade", banks));
			tip.add("§7" + I18n.format(K + "cycle", t.feedPerCycle(), TileEnrichmentBase.PRODUCT_PER_CYCLE, t.tailsPerCycle(), TileEnrichmentBase.CYCLE_TICKS / 20));
			if(container.progress > 0)
				tip.add("§7" + I18n.format(K + "progress", Math.round(100F * container.progress / TileEnrichmentBase.CYCLE_TICKS)));
		}
		if(over(mx, my, PURGE_P, 84, PURGE_T + 11 - PURGE_P, 50))
			tip.add(I18n.format(K + "purgers"));
		if(over(mx, my, ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H))
		{
			tip.add(Math.round(t.energyCapacity() * (container.energy / 32000F)) + "/" + t.energyCapacity() + " IF");
			tip.add("§7" + I18n.format(K + "energy", t.energyPerTick()));
		}
		if(over(mx, my, LAMP_X, LAMP_Y, LAMP_SIZE, LAMP_SIZE))
		{
			tip.add(I18n.format("gui.immersivenuclear.conversion_unit.state." + container.state));
			tip.add("§7" + I18n.format(container.redstoneInverted ? "gui.immersivenuclear.redstone.inverted" : "gui.immersivenuclear.redstone.normal"));
		}
		if(!tip.isEmpty())
		{
			GuiUtils.drawHoveringText(tip, mx, my, width, height, GuiGraphite.TOOLTIP_WIDTH, fontRenderer);
			RenderHelper.enableGUIStandardItemLighting();
		}
	}
}
