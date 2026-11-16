package immersivenuclear.client;

import blusunrize.immersiveengineering.api.crafting.IngredientStack;
import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.GuiIEContainerBase;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INWorkbenchRecipe;
import immersivenuclear.common.gui.ContainerINWorkbench;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * The IN Workbench's interface (15/11/2026, approved as sketch v4): the bench seen from the front and above, laid out like its
 * model. The fixed picture is our sheet (textures/gui/in_workbench_gui.png, by ferramentas/gui_bancada_in_v4_esboco.py --jogo,
 * the same numbers as ContainerINWorkbench); IE's slot frames and wooden inventory are drawn from IE's own sheet. The blueprint's
 * family shows on the work mat, 12 a page: a click chooses (again: clears), the tooltip lists the materials it takes; the two
 * buttons at the mat's end turn the pages. No text on the screen.
 */
public class GuiINWorkbench extends GuiIEContainerBase
{
	static final String IE = "immersiveengineering:textures/gui/refinery.png";
	static final String SHEET = ImmersiveNuclear.MODID + ":textures/gui/in_workbench_gui.png";
	static final int SHEET_SIZE = 512;
	static final int[] IE_SLOT = {35, 13, 20, 20};
	/** The paper on the drafting board: x, y, w, h on the screen; its sprite is at (0, 256) in our sheet. */
	static final int[] PAPER = {13, 31, 56, 56};
	/** The page buttons at the mat's right end: x, y (up), y (down), w, h. */
	static final int PAGE_X = 209, PAGE_UP_Y = 35, PAGE_DOWN_Y = 56, PAGE_W = 7, PAGE_H = 16;

	final ContainerINWorkbench container;
	int page;

	public GuiINWorkbench(ContainerINWorkbench container)
	{
		super(container);
		this.container = container;
		xSize = ContainerINWorkbench.WIDTH;
		ySize = ContainerINWorkbench.HEIGHT;
	}

	List<INWorkbenchRecipe> recipes()
	{
		return container.tile.recipes();
	}

	/** Whether the materials on the mat (as the interface shows them) make this recipe. */
	boolean canMake(INWorkbenchRecipe r)
	{
		return r.matches(container.tile.inventory, immersivenuclear.common.block.TileINWorkbench.SLOT_MATERIALS,
				immersivenuclear.common.block.TileINWorkbench.MATERIALS);
	}

	/** How many of an input the materials hold. */
	int have(IngredientStack in)
	{
		int n = 0;
		for(int s = 0; s < immersivenuclear.common.block.TileINWorkbench.MATERIALS; s++)
		{
			ItemStack st = container.tile.inventory.getStackInSlot(immersivenuclear.common.block.TileINWorkbench.SLOT_MATERIALS + s);
			if(!st.isEmpty() && in.matchesItemStackIgnoringSize(st))
				n += st.getCount();
		}
		return n;
	}

	int pages()
	{
		return Math.max(1, (recipes().size() + ContainerINWorkbench.PER_PAGE - 1) / ContainerINWorkbench.PER_PAGE);
	}

	/** The recipe index under the mouse on the mat, or -1. */
	int choiceAt(int mx, int my)
	{
		int x = mx - guiLeft - (ContainerINWorkbench.CHOICE_X - 2), y = my - guiTop - (ContainerINWorkbench.CHOICE_Y - 2);
		if(x < 0 || y < 0)
			return -1;
		int i = x / ContainerINWorkbench.CHOICE_STEP, j = y / ContainerINWorkbench.CHOICE_STEP;
		if(i >= ContainerINWorkbench.CHOICE_COLS || j >= ContainerINWorkbench.CHOICE_ROWS || x % ContainerINWorkbench.CHOICE_STEP >= 20
				|| y % ContainerINWorkbench.CHOICE_STEP >= 20)
			return -1;
		int index = page * ContainerINWorkbench.PER_PAGE + j * ContainerINWorkbench.CHOICE_COLS + i;
		return index < recipes().size() ? index : -1;
	}

	boolean in(int mx, int my, int x, int y, int w, int h)
	{
		return mx >= guiLeft + x && mx < guiLeft + x + w && my >= guiTop + y && my < guiTop + y + h;
	}

	@Override
	protected void mouseClicked(int mx, int my, int button) throws java.io.IOException
	{
		if(page >= pages())
			page = 0;
		int c = choiceAt(mx, my);
		if(c >= 0 && button == 0 && (canMake(recipes().get(c)) || container.selected == c))
		{
			mc.playerController.sendEnchantPacket(container.windowId, c);
			container.selected = container.selected == c ? -1 : c;      // at once on the screen; the server confirms it
			mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord.getMasterRecord(net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 1F));
			return;
		}
		if(in(mx, my, PAGE_X, PAGE_UP_Y, PAGE_W, PAGE_H) && page > 0 || in(mx, my, PAGE_X, PAGE_DOWN_Y, PAGE_W, PAGE_H) && page < pages() - 1)
		{
			page += in(mx, my, PAGE_X, PAGE_UP_Y, PAGE_W, PAGE_H) ? -1 : 1;
			mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord.getMasterRecord(net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 1F));
			return;
		}
		super.mouseClicked(mx, my, button);
	}

	void frame(int x, int y)
	{
		drawTexturedModalRect(guiLeft + x, guiTop + y, IE_SLOT[0], IE_SLOT[1], IE_SLOT[2], IE_SLOT[3]);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mx, int my)
	{
		if(page >= pages())
			page = 0;
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.enableBlend();
		ClientUtils.bindTexture(SHEET);
		Gui.drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, xSize, ySize, SHEET_SIZE, SHEET_SIZE);
		// the sepia paper on the drafting board only with a blueprint in the bench (the board is bare without one)
		if(!container.tile.inventory.getStackInSlot(immersivenuclear.common.block.TileINWorkbench.SLOT_BLUEPRINT).isEmpty())
			Gui.drawModalRectWithCustomSizedTexture(guiLeft + PAPER[0], guiTop + PAPER[1], 0, 256, PAPER[2], PAPER[3], SHEET_SIZE, SHEET_SIZE);
		ClientUtils.bindTexture(IE);
		drawTexturedModalRect(guiLeft + ContainerINWorkbench.INV_X - 8, guiTop + ContainerINWorkbench.INV_Y - 8, 0, 77, 176, 89);
		GuiGraphite.ieTint();
		for(int s = 0; s < ContainerINWorkbench.BENCH_SLOTS; s++)
		{
			Slot slot = container.inventorySlots.get(s);
			frame(slot.xPos - 2, slot.yPos - 2);
		}
		for(int j = 0; j < ContainerINWorkbench.CHOICE_ROWS; j++)
			for(int i = 0; i < ContainerINWorkbench.CHOICE_COLS; i++)
				frame(ContainerINWorkbench.CHOICE_X - 2 + i * ContainerINWorkbench.CHOICE_STEP, ContainerINWorkbench.CHOICE_Y - 2 + j * ContainerINWorkbench.CHOICE_STEP);
		GuiGraphite.endTint();
		GlStateManager.color(1, 1, 1, 1);
		// the family's items on the mat; the chosen one framed in white, the one under the mouse lit
		List<INWorkbenchRecipe> r = recipes();
		RenderHelper.enableGUIStandardItemLighting();
		for(int k = 0; k < ContainerINWorkbench.PER_PAGE; k++)
		{
			int index = page * ContainerINWorkbench.PER_PAGE + k;
			if(index >= r.size())
				break;
			int x = guiLeft + ContainerINWorkbench.CHOICE_X + (k % ContainerINWorkbench.CHOICE_COLS) * ContainerINWorkbench.CHOICE_STEP;
			int y = guiTop + ContainerINWorkbench.CHOICE_Y + (k / ContainerINWorkbench.CHOICE_COLS) * ContainerINWorkbench.CHOICE_STEP;
			itemRender.renderItemAndEffectIntoGUI(r.get(index).output, x, y);
			itemRender.renderItemOverlayIntoGUI(fontRenderer, r.get(index).output, x, y, null);
		}
		RenderHelper.disableStandardItemLighting();
		GlStateManager.disableLighting();
		GlStateManager.disableDepth();
		// what the materials cannot make yet: dimmed
		for(int k = 0; k < ContainerINWorkbench.PER_PAGE; k++)
		{
			int index = page * ContainerINWorkbench.PER_PAGE + k;
			if(index >= r.size())
				break;
			if(canMake(r.get(index)))
				continue;
			int x = guiLeft + ContainerINWorkbench.CHOICE_X + (k % ContainerINWorkbench.CHOICE_COLS) * ContainerINWorkbench.CHOICE_STEP;
			int y = guiTop + ContainerINWorkbench.CHOICE_Y + (k / ContainerINWorkbench.CHOICE_COLS) * ContainerINWorkbench.CHOICE_STEP;
			drawRect(x, y, x + 16, y + 16, 0xa0303236);
		}
		int sel = container.selected;
		if(sel >= page * ContainerINWorkbench.PER_PAGE && sel < (page + 1) * ContainerINWorkbench.PER_PAGE && sel < r.size())
		{
			int k = sel - page * ContainerINWorkbench.PER_PAGE;
			int x = guiLeft + ContainerINWorkbench.CHOICE_X - 3 + (k % ContainerINWorkbench.CHOICE_COLS) * ContainerINWorkbench.CHOICE_STEP;
			int y = guiTop + ContainerINWorkbench.CHOICE_Y - 3 + (k / ContainerINWorkbench.CHOICE_COLS) * ContainerINWorkbench.CHOICE_STEP;
			drawRect(x, y, x + 22, y + 1, 0xffffffff);
			drawRect(x, y + 21, x + 22, y + 22, 0xffffffff);
			drawRect(x, y, x + 1, y + 22, 0xffffffff);
			drawRect(x + 21, y, x + 22, y + 22, 0xffffffff);
		}
		int hover = choiceAt(mx, my);
		if(hover >= 0)
		{
			int k = hover - page * ContainerINWorkbench.PER_PAGE;
			int x = guiLeft + ContainerINWorkbench.CHOICE_X + (k % ContainerINWorkbench.CHOICE_COLS) * ContainerINWorkbench.CHOICE_STEP;
			int y = guiTop + ContainerINWorkbench.CHOICE_Y + (k / ContainerINWorkbench.CHOICE_COLS) * ContainerINWorkbench.CHOICE_STEP;
			drawRect(x, y, x + 16, y + 16, 0x80ffffff);
		}
		// the page buttons grey out when there is nowhere to go
		if(page == 0)
			drawRect(guiLeft + PAGE_X, guiTop + PAGE_UP_Y, guiLeft + PAGE_X + PAGE_W, guiTop + PAGE_UP_Y + PAGE_H, 0x90202020);
		if(page >= pages() - 1)
			drawRect(guiLeft + PAGE_X, guiTop + PAGE_DOWN_Y, guiLeft + PAGE_X + PAGE_W, guiTop + PAGE_DOWN_Y + PAGE_H, 0x90202020);
		GlStateManager.enableDepth();
	}

	@Override
	public void drawScreen(int mx, int my, float partialTicks)
	{
		super.drawScreen(mx, my, partialTicks);
		int c = choiceAt(mx, my);
		if(c >= 0)
		{
			INWorkbenchRecipe r = recipes().get(c);
			List<String> tip = new ArrayList<>();
			tip.add(r.output.getRarity().color + r.output.getDisplayName() + (r.output.getCount() > 1 ? " x" + r.output.getCount() : ""));
			boolean ok = canMake(r);
			tip.add("§7" + I18n.format(ok ? "gui.immersivenuclear.in_workbench.takes" : "gui.immersivenuclear.in_workbench.missing"));
			for(IngredientStack in : r.inputs)
			{
				ItemStack ex = in.getExampleStack();
				int got = have(in);
				String name = ex.isEmpty() ? (in.oreName != null ? in.oreName : "?") : ex.getDisplayName();
				tip.add((got >= in.inputSize ? "§a " : "§c ") + Math.min(got, in.inputSize) + "/" + in.inputSize + " " + name);
			}
			if(ok || container.selected == c)
				tip.add("§8" + I18n.format(container.selected == c ? "gui.immersivenuclear.in_workbench.clear" : "gui.immersivenuclear.in_workbench.choose"));
			drawHoveringText(tip, mx, my, fontRenderer);
		}
		else if(in(mx, my, PAGE_X, PAGE_UP_Y, PAGE_W, PAGE_DOWN_Y + PAGE_H - PAGE_UP_Y) && pages() > 1)
			drawHoveringText(java.util.Collections.singletonList(I18n.format("gui.immersivenuclear.in_workbench.page", page + 1, pages())), mx, my, fontRenderer);
	}
}
