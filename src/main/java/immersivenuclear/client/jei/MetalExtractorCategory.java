package immersivenuclear.client.jei;

import java.util.Collections;
import java.util.List;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileMetalExtractor;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

/**
 * JEI page of the Metal Extractor, a compact copy of its interface (GuiMetalExtractor) in the mod's finish: the vat,
 * which is the machine's input tank (JEI draws the effluent in it, the plates go over it), the beam between its guides,
 * the chute into the deposit slot and the depleted effluent tank. Positions are the interface's minus (OX, OY).
 */
public class MetalExtractorCategory implements IRecipeCategory<MetalExtractorRecipe>
{
	public static final String UID = ImmersiveNuclear.MODID + ".metal_extractor";
	static final int OX = 46, OY = 4, W = 176, H = 126;
	// Interface coordinates (GuiMetalExtractor)
	static final int VAT_X = 56, VAT_Y = 52, VAT_W = 114, VAT_H = 52, WALL = 3, GUIDE_L = 52, GUIDE_R = 170, GUIDE_W = 4, GUIDE_TOP = 8;
	static final int IX0 = VAT_X + WALL, IX1 = VAT_X + VAT_W - WALL, IY1 = VAT_Y + VAT_H - WALL, LEVEL_Y = VAT_Y + 4;
	static final int BEAM_Y = 50, BEAM_H = 4, PLATE_W = 4, PLATE_H = 38, PLATES = 6;
	static final int TANK_X = 197, TANK_Y = 53, DEPOSIT_X = 176, DEPOSIT_Y = 56, PIPE_Y = 78;
	/** The six plates (one slot showing the stack), under the vat's left end, and the text beside it (page coordinates). */
	static final int PLATES_X = 12, PLATES_Y = 106, TEXT_X = 34, TEXT_Y = 111;
	static final int COL_WALL = 0xff5c5d62, COL_WALL_LIGHT = 0xff74767a, COL_GUIDE = 0xff54565a, COL_GUIDE_LIGHT = 0xff787b80;
	static final int COL_BEAM = 0xff606268, COL_BEAM_LIGHT = 0xff8c8f94, COL_HAZARD = 0xffc8a028, COL_HAZARD_DARK = 0xff282828;
	static final int COL_PLATE = 0xff7c7f84, COL_PLATE_LIGHT = 0xffa0a3a8, COL_METAL = 0xff80684f, COL_CHUTE = 0xff6e7074;

	final JeiGraphite g;
	final IDrawable background, icon;
	/** Drawn over the effluent in the vat: the six plates, the metal on them (half way through the recovery). */
	final IDrawable plates = new IDrawable()
	{
		@Override
		public int getWidth()
		{
			return IX1 - IX0;
		}

		@Override
		public int getHeight()
		{
			return IY1 - LEVEL_Y;
		}

		@Override
		public void draw(Minecraft mc, int x, int y)
		{
			float gap = (IX1 - IX0 - PLATES * PLATE_W) / (float)(PLATES + 1);
			int top = y + BEAM_Y + BEAM_H + 1 - LEVEL_Y;
			for(int k = 0; k < PLATES; k++)
			{
				int px = x + Math.round(gap * (k + 1) + PLATE_W * k);
				JeiGraphite.rect(px - 1, top + 6, PLATE_W + 2, PLATE_H - 6, COL_METAL);
				JeiGraphite.rect(px, top, PLATE_W, PLATE_H, COL_PLATE);
				JeiGraphite.rect(px, top, 1, PLATE_H, COL_PLATE_LIGHT);
			}
			GlStateManager.color(1, 1, 1, 1);
		}
	};

	public MetalExtractorCategory(IGuiHelper helper)
	{
		g = new JeiGraphite(helper);
		background = helper.createBlankDrawable(W, H);
		icon = helper.createDrawableIngredient(new ItemStack(INContent.itemMetalExtractor));
	}

	@Override
	public String getUid()
	{
		return UID;
	}

	@Override
	public String getTitle()
	{
		return I18n.format("tile.immersivenuclear.metal_extractor.name");
	}

	@Override
	public String getModName()
	{
		return "Immersive Nuclear";
	}

	@Override
	public IDrawable getBackground()
	{
		return background;
	}

	@Override
	public IDrawable getIcon()
	{
		return icon;
	}

	@Override
	public void drawExtras(Minecraft mc)
	{
		g.plate(mc, W, H);
		// The vat: dark inside, walls; the guides of the beam; the pipe to the depleted effluent tank
		JeiGraphite.rect(IX0 - OX, VAT_Y - OY, IX1 - IX0, IY1 - VAT_Y, JeiGraphite.WELL);
		JeiGraphite.rect(VAT_X - OX, VAT_Y - OY, WALL, VAT_H, COL_WALL);
		JeiGraphite.rect(IX1 - OX, VAT_Y - OY, WALL, VAT_H, COL_WALL);
		JeiGraphite.rect(VAT_X - OX, IY1 - OY, VAT_W, WALL, COL_WALL);
		JeiGraphite.rect(VAT_X - OX, VAT_Y - OY, 1, VAT_H, COL_WALL_LIGHT);
		JeiGraphite.rect(IX1 - OX, VAT_Y - OY, 1, VAT_H, COL_WALL_LIGHT);
		for(int gx : new int[]{GUIDE_L, GUIDE_R})
		{
			JeiGraphite.rect(gx - OX, GUIDE_TOP - OY, GUIDE_W, VAT_Y + VAT_H - GUIDE_TOP, COL_GUIDE);
			JeiGraphite.rect(gx - OX, GUIDE_TOP - OY, 1, VAT_Y + VAT_H - GUIDE_TOP, COL_GUIDE_LIGHT);
		}
		JeiGraphite.pipe(GUIDE_R + GUIDE_W - OX, PIPE_Y - OY, TANK_X - 1 - OX, PIPE_Y - OY);
		// The beam, down (working), with its hazard ends
		int bx0 = GUIDE_L - OX, bx1 = GUIDE_R + GUIDE_W - OX, by = BEAM_Y - OY;
		JeiGraphite.rect(bx0, by, bx1 - bx0, BEAM_H, COL_BEAM);
		JeiGraphite.rect(bx0, by, bx1 - bx0, 1, COL_BEAM_LIGHT);
		for(int i = 0; i < 4; i++)
			for(int k = 0; k < BEAM_H; k++)
			{
				int c = (i + k) % 4 < 2 ? COL_HAZARD : COL_HAZARD_DARK;
				JeiGraphite.rect(bx0 + i, by + k, 1, 1, c);
				JeiGraphite.rect(bx1 - 1 - i, by + k, 1, 1, c);
			}
		// The tray's chute into the deposit slot
		for(int k = 0; k < 3; k++)
			JeiGraphite.rect(IX1 - 3 + k * 2 - OX, VAT_Y + k - OY, 6, 1, COL_CHUTE);
		GlStateManager.color(1, 1, 1, 1);
		g.tank(mc, TANK_X - OX, TANK_Y - OY, false);
		g.slot(mc, DEPOSIT_X - OX, DEPOSIT_Y - OY, false);
		g.slot(mc, PLATES_X, PLATES_Y, null);
	}

	@Override
	public void setRecipe(IRecipeLayout layout, MetalExtractorRecipe recipe, IIngredients ingredients)
	{
		IGuiItemStackGroup items = layout.getItemStacks();
		// JEI puts the item 1 px inside the slot position
		items.init(0, false, DEPOSIT_X - OX - 1, DEPOSIT_Y - OY - 1);
		items.init(1, true, PLATES_X - 1, PLATES_Y - 1);
		items.set(ingredients);
		items.addTooltipCallback((slot, input, stack, tooltip) -> {
			if(slot == 1)
				tooltip.add("§7" + I18n.format("jei.immersivenuclear.metal_extractor.electrodes"));
		});
		IGuiFluidStackGroup fluids = layout.getFluidStacks();
		// The vat holds exactly one batch
		fluids.init(0, true, IX0 - OX, LEVEL_Y - OY, IX1 - IX0, IY1 - LEVEL_Y, TileMetalExtractor.BATCH, false, plates);
		fluids.init(1, false, TANK_X + 2 - OX, TANK_Y + 2 - OY, 16, 47, TileMetalExtractor.DEPLETED_CAPACITY, false, g.gauge);
		fluids.set(ingredients);
	}

	@Override
	public List<String> getTooltipStrings(int mx, int my)
	{
		if(mx >= GUIDE_L - OX && mx < GUIDE_R + GUIDE_W - OX && my >= GUIDE_TOP - OY && my < LEVEL_Y - OY)
			return Collections.singletonList(I18n.format("jei.immersivenuclear.metal_extractor.plates", TileMetalExtractor.RECOVER_TICKS / 20,
					TileMetalExtractor.RECOVER_ENERGY));
		return Collections.emptyList();
	}
}
