package immersivenuclear.client;

import java.util.ArrayList;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.GuiIEContainerBase;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.block.TileFilterPress;
import immersivenuclear.common.gui.ContainerFilterPress;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Interface of the Filtration Unit (design/2-unidade-de-filtro-prensa/interface-filtracao-v1.png), in IE's style and
 * drawn from IE's refinery GUI at run time (border, steel, frames, markers, arrows, tank gauge, wooden inventory),
 * taller than IE's 166 to fit one slot per filter. Our sheet (textures/gui/processamento_quimico.png) gives the
 * arrow fill, the dark insets, the status lights and the blank button.
 * Top: polpa buckets | 1 storage tank > 2 pump > 3 pressure tank, 4 pressure gauge > 5 filtering > 6 solution tank |
 * solution buckets. Below: 28 filters | residue, redstone button, status lights, energy.
 */
public class GuiFilterPress extends GuiIEContainerBase
{
	static final String IE = "immersiveengineering:textures/gui/refinery.png";
	static final String OWN = ImmersiveNuclear.MODID + ":textures/gui/processamento_quimico.png";

	// Pieces of refinery.png: {u, v, w, h}
	static final int[] SLOT = {35, 13, 20, 20}, TANK = {11, 18, 20, 51}, BLUE = {42, 10, 6, 3}, ORANGE = {42, 49, 6, 3}, ARROW = {83, 36, 19, 13};
	static final int TANK_Y = 20, STORAGE_X = 30, PRESSURE_X = 74, SOLUTION_X = 130;
	static final int GAUGE_X = 94, GAUGE_Y = 18, GAUGE_W = 9, GAUGE_H = 51;
	/** Also JEI's click areas for the recipes (INJeiPlugin). */
	public static final int PUMP_ARROW_X = 51, FILTER_ARROW_X = 106, ARROW_Y = 37;
	static final int BUTTON_X = 141, BUTTON_Y = 105, BUTTON_SIZE = 16;
	static final int PANEL_X = 141, PANEL_Y = 122, PANEL_W = 16, PANEL_H = 36;
	static final int LIGHT_X = 146, LIGHT_Y = 126, LIGHT_STEP = 8;
	static final int ENERGY_X = 161, ENERGY_Y = 82, ENERGY_W = 9, ENERGY_H = 76;

	final ContainerFilterPress container;

	public GuiFilterPress(ContainerFilterPress container)
	{
		super(container);
		this.container = container;
		xSize = ContainerFilterPress.WIDTH;
		ySize = ContainerFilterPress.HEIGHT;
	}

	void piece(int[] p, int x, int y)
	{
		drawTexturedModalRect(guiLeft + x, guiTop + y, p[0], p[1], p[2], p[3]);
	}

	/** A slot frame, with the IE marker above it (blue = input, orange = output, null = none). */
	void slotFrame(int itemX, int itemY, int[] marker)
	{
		piece(SLOT, itemX - 2, itemY - 2);
		if(marker != null)
			piece(marker, itemX + 5, itemY - 5);
	}

	/** A frame of any size, cut from the refinery's slot frame (2 px light border). Refinery texture bound. */
	void frame(int x, int y, int w, int h)
	{
		int u = SLOT[0], v = SLOT[1];
		for(int i = 2; i < w - 2; i += 16)
		{
			int len = Math.min(16, w - 2 - i);
			drawTexturedModalRect(guiLeft + x + i, guiTop + y, u + 2, v, len, 2);
			drawTexturedModalRect(guiLeft + x + i, guiTop + y + h - 2, u + 2, v + 18, len, 2);
		}
		for(int j = 2; j < h - 2; j += 16)
		{
			int len = Math.min(16, h - 2 - j);
			drawTexturedModalRect(guiLeft + x, guiTop + y + j, u, v + 2, 2, len);
			drawTexturedModalRect(guiLeft + x + w - 2, guiTop + y + j, u + 18, v + 2, 2, len);
		}
		drawTexturedModalRect(guiLeft + x, guiTop + y, u, v, 2, 2);
		drawTexturedModalRect(guiLeft + x + w - 2, guiTop + y, u + 18, v, 2, 2);
		drawTexturedModalRect(guiLeft + x, guiTop + y + h - 2, u, v + 18, 2, 2);
		drawTexturedModalRect(guiLeft + x + w - 2, guiTop + y + h - 2, u + 18, v + 18, 2, 2);
	}

	/** The dark inside of a frame, tiled from our sheet (32 x 32 at 0, 110). Our texture bound. */
	void dark(int x, int y, int w, int h)
	{
		for(int j = 0; j < h; j += 32)
			for(int i = 0; i < w; i += 32)
				drawTexturedModalRect(guiLeft + x + i, guiTop + y + j, 0, 110, Math.min(32, w - i), Math.min(32, h - j));
	}

	/** 0 green running, 1 yellow waiting (polpa, pressure), 2 red stopped (power, full output, filters), 3 grey disabled. */
	int litLight()
	{
		if(container.state == TileFilterPress.STATE_RUNNING)
			return container.phase == TileFilterPress.PHASE_CLOSED ? 1 : 0;
		switch(container.state)
		{
			case TileFilterPress.STATE_NO_INPUT: return 1;
			case TileFilterPress.STATE_NO_POWER:
			case TileFilterPress.STATE_OUTPUT_FULL:
			case TileFilterPress.STATE_NO_FILTERS: return 2;
			case TileFilterPress.STATE_DISABLED: return 3;
			default: return -1;
		}
	}

	/** Shown pressure, 0..1: eases towards the real one, so the gauge rises and falls like a needle. */
	float shownPressure;

	/** The pump's pressure on what is left in the pressure tank: it falls as the release empties the tank. */
	float targetPressure()
	{
		int amount = container.pressure.getFluidAmount();
		return amount > 0 ? container.pump / (float)TileFilterPress.PRESSURIZE_TICKS * Math.min(1, amount / (float)TileFilterPress.BATCH) : 0;
	}

	float pressure()
	{
		return shownPressure;
	}

	@Override
	public void updateScreen()
	{
		super.updateScreen();
		shownPressure += (targetPressure() - shownPressure) * 0.25F;
		if(Math.abs(targetPressure() - shownPressure) < 0.002F)
			shownPressure = targetPressure();
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mx, int my)
	{
		GlStateManager.color(1, 1, 1, 1);
		ClientUtils.bindTexture(IE);
		// Refinery top (border and rivets), its clean row 76 stretched for the side borders, its wooden inventory
		// panel at the bottom; the inside is our steel, tiled (a stretched single row would show as streaks)
		int woodTop = ContainerFilterPress.INV_Y - 8;
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, 77);
		for(int y = 77; y < woodTop; y++)
			drawTexturedModalRect(guiLeft, guiTop + y, 0, 76, xSize, 1);
		drawTexturedModalRect(guiLeft, guiTop + woodTop, 0, 77, xSize, 89);
		drawTexturedModalRect(guiLeft, guiTop + ySize - 1, 0, 165, xSize, 1);
		ClientUtils.bindTexture(OWN);
		for(int y = 5; y < woodTop; y += 65)
			for(int x = 5; x < 171; x += 145)
				drawTexturedModalRect(guiLeft + x, guiTop + y, 0, 0, Math.min(145, 171 - x), Math.min(65, woodTop - y));
		ClientUtils.bindTexture(IE);

		int[][] xy = ContainerFilterPress.SLOT_XY;
		slotFrame(xy[1][0], xy[1][1], BLUE);
		slotFrame(xy[2][0], xy[2][1], ORANGE);
		slotFrame(xy[3][0], xy[3][1], BLUE);
		slotFrame(xy[4][0], xy[4][1], ORANGE);
		slotFrame(xy[0][0], xy[0][1], ORANGE);
		for(int i = 0; i < TileFilterPress.FILTERS; i++)
			slotFrame(ContainerFilterPress.FILTERS_X + i % ContainerFilterPress.FILTER_COLUMNS * 18, ContainerFilterPress.FILTERS_Y + i / ContainerFilterPress.FILTER_COLUMNS * 18, null);
		piece(TANK, STORAGE_X - 2, TANK_Y - 2);
		piece(BLUE, STORAGE_X + 5, TANK_Y - 5);
		piece(TANK, PRESSURE_X - 2, TANK_Y - 2);
		piece(TANK, SOLUTION_X - 2, TANK_Y - 2);
		piece(ORANGE, SOLUTION_X + 5, TANK_Y - 5);
		piece(ARROW, PUMP_ARROW_X, ARROW_Y);
		piece(ARROW, FILTER_ARROW_X, ARROW_Y);
		frame(GAUGE_X, GAUGE_Y, GAUGE_W, GAUGE_H);
		frame(PANEL_X, PANEL_Y, PANEL_W, PANEL_H);
		frame(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H);

		ClientUtils.bindTexture(OWN);
		dark(GAUGE_X + 2, GAUGE_Y + 2, GAUGE_W - 4, GAUGE_H - 4);
		dark(PANEL_X + 2, PANEL_Y + 2, PANEL_W - 4, PANEL_H - 4);
		dark(ENERGY_X + 2, ENERGY_Y + 2, ENERGY_W - 4, ENERGY_H - 4);
		drawTexturedModalRect(guiLeft + BUTTON_X, guiTop + BUTTON_Y, 0, 150, BUTTON_SIZE, BUTTON_SIZE);
		int lit = litLight();
		for(int i = 0; i < 4; i++)
			drawTexturedModalRect(guiLeft + LIGHT_X, guiTop + LIGHT_Y + i * LIGHT_STEP, i * 8, i == lit ? 90 : 98, 6, 6);
		// Pump arrow: pressurising; filtering arrow: the batch passing through the cloths
		int pump = (int)(19 * pressure());
		if(pump > 0)
			drawTexturedModalRect(guiLeft + PUMP_ARROW_X, guiTop + ARROW_Y, 0, 70, pump, 13);
		int filtering = filteringDone();
		if(filtering > 0)
			drawTexturedModalRect(guiLeft + FILTER_ARROW_X, guiTop + ARROW_Y, 0, 70, filtering, 13);

		// Pressure gauge, yellow to orange; energy with IE's red gradient
		int ph = (int)((GAUGE_H - 4) * pressure());
		if(ph > 0)
			ClientUtils.drawGradientRect(guiLeft + GAUGE_X + 2, guiTop + GAUGE_Y + GAUGE_H - 2 - ph, guiLeft + GAUGE_X + GAUGE_W - 2, guiTop + GAUGE_Y + GAUGE_H - 2, 0xffffd640, 0xffb04000);
		int eh = (int)((ENERGY_H - 4) * (container.energy / (float)TileFilterPress.ENERGY_CAPACITY));
		if(eh > 0)
			ClientUtils.drawGradientRect(guiLeft + ENERGY_X + 2, guiTop + ENERGY_Y + ENERGY_H - 2 - eh, guiLeft + ENERGY_X + ENERGY_W - 2, guiTop + ENERGY_Y + ENERGY_H - 2, 0xffb51500, 0xff600b00);
		ClientUtils.handleGuiTank(container.storage, guiLeft + STORAGE_X, guiTop + TANK_Y, 16, 47, 177, 31, 20, 51, mx, my, IE, null);
		ClientUtils.handleGuiTank(container.pressure, guiLeft + PRESSURE_X, guiTop + TANK_Y, 16, 47, 177, 31, 20, 51, mx, my, IE, null);
		ClientUtils.handleGuiTank(container.solution, guiLeft + SOLUTION_X, guiTop + TANK_Y, 16, 47, 177, 31, 20, 51, mx, my, IE, null);
	}

	int filteringDone()
	{
		switch(container.phase)
		{
			case TileFilterPress.PHASE_RELEASE: return 2;
			case TileFilterPress.PHASE_FILTERING: return 19 * container.phaseTicks / TileFilterPress.FILTER_TICKS;
			default: return 0;
		}
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

	@Override
	public void drawScreen(int mx, int my, float partialTicks)
	{
		super.drawScreen(mx, my, partialTicks);
		ArrayList<String> tooltip = new ArrayList<>();
		ClientUtils.handleGuiTank(container.storage, guiLeft + STORAGE_X, guiTop + TANK_Y, 16, 47, 177, 31, 20, 51, mx, my, IE, tooltip);
		ClientUtils.handleGuiTank(container.pressure, guiLeft + PRESSURE_X, guiTop + TANK_Y, 16, 47, 177, 31, 20, 51, mx, my, IE, tooltip);
		ClientUtils.handleGuiTank(container.solution, guiLeft + SOLUTION_X, guiTop + TANK_Y, 16, 47, 177, 31, 20, 51, mx, my, IE, tooltip);
		if(over(mx, my, STORAGE_X, TANK_Y, 16, 47))
			tooltip.add(0, I18n.format("gui.immersivenuclear.filter_press.storage"));
		if(over(mx, my, PRESSURE_X, TANK_Y, 16, 47))
			tooltip.add(0, I18n.format("gui.immersivenuclear.filter_press.pressure_tank"));
		if(over(mx, my, SOLUTION_X, TANK_Y, 16, 47))
			tooltip.add(0, I18n.format("gui.immersivenuclear.filter_press.solution"));
		if(over(mx, my, GAUGE_X, GAUGE_Y, GAUGE_W, GAUGE_H))
			tooltip.add(I18n.format("gui.immersivenuclear.filter_press.pressure", Math.round(100 * pressure())));
		if(over(mx, my, ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H))
			tooltip.add(container.energy + "/" + TileFilterPress.ENERGY_CAPACITY + " IF");
		if(over(mx, my, BUTTON_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE))
		{
			tooltip.add(I18n.format("gui.immersivenuclear.redstone"));
			tooltip.add("§7" + I18n.format(container.redstoneInverted ? "gui.immersivenuclear.redstone.inverted" : "gui.immersivenuclear.redstone.normal"));
		}
		int lit = litLight();
		for(int i = 0; i < 4; i++)
			if(over(mx, my, PANEL_X, LIGHT_Y + i * LIGHT_STEP - 1, PANEL_W, LIGHT_STEP))
			{
				if(i != lit)
					tooltip.add("§7" + I18n.format("gui.immersivenuclear.filter_press.light." + i));
				else if(container.state == TileFilterPress.STATE_RUNNING)
					tooltip.add(I18n.format("gui.immersivenuclear.filter_press.phase." + container.phase));
				else
					tooltip.add(I18n.format("gui.immersivenuclear.filter_press.state." + container.state));
			}
		if(!tooltip.isEmpty())
		{
			ClientUtils.drawHoveringText(tooltip, mx, my, fontRenderer, guiLeft + xSize, -1);
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
			mc.playerController.sendEnchantPacket(inventorySlots.windowId, ContainerFilterPress.BUTTON_REDSTONE);
		}
	}
}
