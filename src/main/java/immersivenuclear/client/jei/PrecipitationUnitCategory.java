package immersivenuclear.client.jei;

import java.util.Collections;
import java.util.List;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TilePrecipitationUnit;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
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
 * JEI page of the Precipitation Unit, a compact copy of its interface (GuiPrecipitationUnit) in the mod's finish:
 * agent on the funnel > AGITATED TANK (the batch of solution, three levels of four blades) > SCREW (concentrate at its
 * head) > effluent tank. The batch's solution is shown in the tank itself, as the machine has no other input tank.
 */
public class PrecipitationUnitCategory implements IRecipeCategory<PrecipitationUnitRecipe>
{
	public static final String UID = ImmersiveNuclear.MODID + ".precipitation_unit";
	static final int W = 176, H = 108;
	/** The tank's well and, inside it, the liquid (JEI's fluid slot) under the lid. */
	static final int MX = 8, MY = 24, MW = 64, MH = 66, LID = 6;
	static final int AGENT_X = MX + MW / 2 - 8, AGENT_Y = 4;
	static final int ARROW_X = MX + MW + 4, ARROW_Y = MY + MH / 2 - 6;
	static final int SX = ARROW_X + 24, SY = MY, SW = 40, SH = MH;
	static final int CONC_X = SX + SW - 18, CONC_Y = 4;
	static final int OUT_X = 150, TANK_Y = MY + 8, TEXT_Y = 96;
	static final int COL_SHAFT = 0xff96999e, COL_SHAFT_DARK = 0xff5c5f64, COL_EFFLUENT = 0xff706a58;

	final JeiGraphite g;
	final IDrawable background, icon, arrow;
	final IDrawableAnimated arrowFill;

	public PrecipitationUnitCategory(IGuiHelper helper)
	{
		g = new JeiGraphite(helper);
		background = helper.createBlankDrawable(W, H);
		arrow = helper.createDrawable(JeiGraphite.GRAPHITE, 184, 0, 19, 13);
		arrowFill = helper.createAnimatedDrawable(helper.createDrawable(JeiGraphite.GRAPHITE, 184, 13, 19, 13), 60, IDrawableAnimated.StartDirection.LEFT, false);
		icon = helper.createDrawableIngredient(new ItemStack(INContent.itemPrecipitationUnit));
	}

	@Override
	public String getUid()
	{
		return UID;
	}

	@Override
	public String getTitle()
	{
		return I18n.format("tile.immersivenuclear.precipitation_unit.name");
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
		// Funnel, tank, slurry pipe to the screw's foot, head up to the concentrate, effluent pipe to its tank
		JeiGraphite.rect(AGENT_X + 4, AGENT_Y + 18, 8, MY - AGENT_Y - 18, COL_SHAFT_DARK);
		JeiGraphite.rect(CONC_X + 4, CONC_Y + 18, 8, SY - CONC_Y - 18, COL_SHAFT_DARK);
		JeiGraphite.rect(MX, MY, MW, MH, JeiGraphite.WELL);
		JeiGraphite.rect(SX, SY, SW, SH, JeiGraphite.WELL);
		JeiGraphite.pipe(SX + 4, SY + SH, SX + 4, SY + SH + 4);
		JeiGraphite.pipe(SX + 4, SY + SH + 4, OUT_X + 10, SY + SH + 4);
		JeiGraphite.pipe(OUT_X + 10, SY + SH + 4, OUT_X + 10, TANK_Y + 51);
		JeiGraphite.rect(MX + 2, MY + 2, MW - 4, LID - 2, COL_SHAFT_DARK);
		GlStateManager.color(1, 1, 1, 1);
		mc.getTextureManager().bindTexture(JeiGraphite.GRAPHITE);
		arrow.draw(mc, ARROW_X, ARROW_Y);
		arrowFill.draw(mc, ARROW_X, ARROW_Y);
		// The screw's flights, foot (lower left) to head (upper right)
		int x0 = SX + 5, y0 = SY + SH - 6, x1 = SX + SW - 6, y1 = SY + 6;
		for(int i = 0; i <= 10; i++)
		{
			int px = Math.round(x0 + (x1 - x0) * i / 10F), py = Math.round(y0 + (y1 - y0) * i / 10F);
			JeiGraphite.rect(px - 3, py - 3, 7, 7, COL_SHAFT_DARK);
			JeiGraphite.rect(px - 2, py - 2, 5, 5, i % 2 == 0 ? COL_SHAFT : COL_SHAFT_DARK);
		}
		g.slot(mc, AGENT_X, AGENT_Y, true);
		g.slot(mc, CONC_X, CONC_Y, false);
		g.tank(mc, OUT_X, TANK_Y, false);
		JeiGraphite.text(mc, PrecipitationUnitRecipe.energy() + " IF   " + PrecipitationUnitRecipe.ticks() / 20 + " s", 0, TEXT_Y, W);
	}

	/**
	 * The agitator, drawn over the liquid as the fluid slot's overlay (JEI draws the ingredients after the page): the
	 * shaft and three levels of four blades, at rest, at the interface's heights (GuiPrecipitationUnit.BLADE_LEVELS).
	 */
	static class Agitator implements IDrawable
	{
		static final float[] LEVELS = {0.133F, 0.467F, 0.8F};
		static final float REACH = 1.875F / 5.6F;

		@Override
		public int getWidth()
		{
			return MW - 4;
		}

		@Override
		public int getHeight()
		{
			return MH - LID - 2;
		}

		@Override
		public void draw(Minecraft mc, int x, int y)
		{
			int w = getWidth(), h = getHeight(), cx = x + w / 2, floor = y + h;
			int lowest = floor - Math.round(LEVELS[0] * h);
			JeiGraphite.rect(cx - 1, y - LID + 2, 2, lowest - y + LID - 2, COL_SHAFT);
			int reach = Math.round(REACH * w);
			for(float level : LEVELS)
			{
				int by = floor - Math.round(level * h) - 1;
				// Turned 30 degrees: two blades towards the viewer (light), two behind the shaft (dark)
				JeiGraphite.rect(cx - Math.round(reach * 0.5F), by, Math.round(reach * 0.5F), 3, COL_SHAFT_DARK);
				JeiGraphite.rect(cx, by, Math.round(reach * 0.866F) + 1, 3, COL_SHAFT_DARK);
				JeiGraphite.rect(cx - Math.round(reach * 0.866F), by, Math.round(reach * 0.866F), 3, COL_SHAFT);
				JeiGraphite.rect(cx, by, Math.round(reach * 0.5F) + 1, 3, COL_SHAFT);
				JeiGraphite.rect(cx - 1, by - 1, 2, 5, COL_SHAFT);
			}
			GlStateManager.color(1, 1, 1, 1);
		}
	}

	final Agitator agitator = new Agitator();

	@Override
	public void setRecipe(IRecipeLayout layout, PrecipitationUnitRecipe recipe, IIngredients ingredients)
	{
		IGuiItemStackGroup items = layout.getItemStacks();
		// JEI puts the item 1 px inside the slot position
		items.init(0, true, AGENT_X - 1, AGENT_Y - 1);
		items.init(1, false, CONC_X - 1, CONC_Y - 1);
		items.set(ingredients);
		IGuiFluidStackGroup fluids = layout.getFluidStacks();
		// The batch fills the tank (under the lid); the effluent goes to its tank
		fluids.init(0, true, MX + 2, MY + LID, MW - 4, MH - LID - 2, TilePrecipitationUnit.BATCH_SOLUTION, false, agitator);
		fluids.init(1, false, OUT_X + 2, TANK_Y + 2, 16, 47, TilePrecipitationUnit.EFFLUENT_CAPACITY, false, g.gauge);
		fluids.set(ingredients);
	}

	@Override
	public List<String> getTooltipStrings(int mx, int my)
	{
		if(mx >= SX && mx < SX + SW && my >= SY && my < SY + SH)
			return Collections.singletonList(I18n.format("gui.immersivenuclear.precipitation_unit.screw"));
		return Collections.emptyList();
	}
}
