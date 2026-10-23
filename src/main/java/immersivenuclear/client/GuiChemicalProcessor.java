package immersivenuclear.client;

import java.util.ArrayList;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.GuiIEContainerBase;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.block.TileChemicalProcessor;
import immersivenuclear.common.gui.ContainerChemicalProcessor;
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
 * Interface of the Chemical Processing Unit, in IE's style and drawn from IE's own refinery GUI at run time
 * (frames, markers, arrow, tank gauge, energy bar, player inventory). Our sheet (textures/gui/processamento_quimico.png,
 * ferramentas/textura_gui.py) only covers the refinery's layout and holds the arrow fill and the status lights.
 * Left to right: water buckets, water tank, uranium powder / arrow / reagent, polpa tank, polpa buckets,
 * redstone mode button over the status lights, energy.
 */
public class GuiChemicalProcessor extends GuiIEContainerBase
{
	static final String IE = "immersiveengineering:textures/gui/refinery.png";
	static final String OWN = ImmersiveNuclear.MODID + ":textures/gui/processamento_quimico.png";

	// Pieces of refinery.png: {u, v, w, h}
	static final int[] SLOT = {35, 13, 20, 20}, TANK = {11, 18, 20, 51}, BLUE = {42, 10, 6, 3}, ORANGE = {42, 49, 6, 3}, ARROW = {83, 36, 19, 13};
	// Our layout: tanks (inside of the gauge), arrow, redstone button and status panel
	static final int WATER_X = 34, POLPA_X = 82, TANK_Y = 20;
	/** Also JEI's click area for the recipes (INJeiPlugin). */
	public static final int ARROW_X = 56, ARROW_Y = 37;
	// Redstone button (IE assembler style, 16x16) and the status panel share one 16 px column between the polpa
	// buckets and the energy bar: the button centred on the top slots, the panel down to the bottom slots' edge
	static final int BUTTON_X = 131, BUTTON_Y = 15, BUTTON_SIZE = 16;
	static final int PANEL_X = 131, PANEL_Y = 36, PANEL_W = 16, PANEL_H = 36;
	static final int LIGHT_X = 136, LIGHT_Y = 40, LIGHT_STEP = 8;

	final ContainerChemicalProcessor container;

	public GuiChemicalProcessor(ContainerChemicalProcessor container)
	{
		super(container);
		this.container = container;
		xSize = 176;
		ySize = 166;
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

	/** A frame of any size, cut from the refinery's slot frame (2 px light border). */
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

	/** Which status light is on: 0 green running, 1 yellow missing input, 2 red no power / output full, 3 grey disabled. */
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
		ClientUtils.bindTexture(IE);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
		ClientUtils.bindTexture(OWN);
		drawTexturedModalRect(guiLeft + 9, guiTop + 8, 0, 0, 145, 65);

		ClientUtils.bindTexture(IE);
		int[][] xy = ContainerChemicalProcessor.SLOT_XY;
		slotFrame(xy[TileChemicalProcessor.SLOT_WATER_IN][0], xy[TileChemicalProcessor.SLOT_WATER_IN][1], BLUE);
		slotFrame(xy[TileChemicalProcessor.SLOT_WATER_OUT][0], xy[TileChemicalProcessor.SLOT_WATER_OUT][1], ORANGE);
		slotFrame(xy[TileChemicalProcessor.SLOT_URANIUM][0], xy[TileChemicalProcessor.SLOT_URANIUM][1], BLUE);
		slotFrame(xy[TileChemicalProcessor.SLOT_REAGENT][0], xy[TileChemicalProcessor.SLOT_REAGENT][1], BLUE);
		slotFrame(xy[TileChemicalProcessor.SLOT_POLPA_IN][0], xy[TileChemicalProcessor.SLOT_POLPA_IN][1], BLUE);
		slotFrame(xy[TileChemicalProcessor.SLOT_POLPA_OUT][0], xy[TileChemicalProcessor.SLOT_POLPA_OUT][1], ORANGE);
		piece(TANK, WATER_X - 2, TANK_Y - 2);
		piece(BLUE, WATER_X + 5, TANK_Y - 5);
		piece(TANK, POLPA_X - 2, TANK_Y - 2);
		piece(ORANGE, POLPA_X + 5, TANK_Y - 5);
		piece(ARROW, ARROW_X, ARROW_Y);
		frame(PANEL_X, PANEL_Y, PANEL_W, PANEL_H);

		ClientUtils.bindTexture(OWN);
		drawTexturedModalRect(guiLeft + BUTTON_X, guiTop + BUTTON_Y, 0, 150, BUTTON_SIZE, BUTTON_SIZE);
		drawTexturedModalRect(guiLeft + PANEL_X + 2, guiTop + PANEL_Y + 2, 0, 110, PANEL_W - 4, PANEL_H - 4);
		int lit = litLight();
		for(int i = 0; i < 4; i++)
			drawTexturedModalRect(guiLeft + LIGHT_X, guiTop + LIGHT_Y + i * LIGHT_STEP, i * 8, i == lit ? 90 : 98, 6, 6);
		int done = 19 * container.progress / TileChemicalProcessor.CYCLE_TICKS;
		if(done > 0)
			drawTexturedModalRect(guiLeft + ARROW_X, guiTop + ARROW_Y, 0, 70, done, 13);

		// Energy and tanks exactly as IE's refinery draws them
		int stored = (int)(46 * (container.energy / (float)TileChemicalProcessor.ENERGY_CAPACITY));
		ClientUtils.drawGradientRect(guiLeft + 157, guiTop + 21 + (46 - stored), guiLeft + 164, guiTop + 67, 0xffb51500, 0xff600b00);
		ClientUtils.handleGuiTank(container.water, guiLeft + WATER_X, guiTop + TANK_Y, 16, 47, 177, 31, 20, 51, mx, my, IE, null);
		ClientUtils.handleGuiTank(container.polpa, guiLeft + POLPA_X, guiTop + TANK_Y, 16, 47, 177, 31, 20, 51, mx, my, IE, null);
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
		ClientUtils.handleGuiTank(container.water, guiLeft + WATER_X, guiTop + TANK_Y, 16, 47, 177, 31, 20, 51, mx, my, IE, tooltip);
		ClientUtils.handleGuiTank(container.polpa, guiLeft + POLPA_X, guiTop + TANK_Y, 16, 47, 177, 31, 20, 51, mx, my, IE, tooltip);
		if(over(mx, my, 157, 21, 7, 46))
			tooltip.add(container.energy + "/" + TileChemicalProcessor.ENERGY_CAPACITY + " IF");
		if(over(mx, my, BUTTON_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE))
		{
			tooltip.add(I18n.format("gui.immersivenuclear.redstone"));
			tooltip.add("§7" + I18n.format(container.redstoneInverted ? "gui.immersivenuclear.redstone.inverted" : "gui.immersivenuclear.redstone.normal"));
		}
		for(int i = 0; i < 4; i++)
			if(over(mx, my, PANEL_X, LIGHT_Y + i * LIGHT_STEP - 1, PANEL_W, LIGHT_STEP))
			{
				boolean on = i == litLight();
				// The lit light names the exact state (red covers two)
				tooltip.add(on ? I18n.format("gui.immersivenuclear.state." + container.state) : "§7" + I18n.format("gui.immersivenuclear.light." + i));
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
			mc.playerController.sendEnchantPacket(inventorySlots.windowId, ContainerChemicalProcessor.BUTTON_REDSTONE);
		}
	}
}
