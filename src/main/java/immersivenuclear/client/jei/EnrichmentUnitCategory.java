package immersivenuclear.client.jei;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileEnrichmentBase;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/** JEI page of the Enrichment Unit, in the mod's finish, one recipe per assembly: natural hexafluoride at the left, the cascade with as many banks lit as the assembly has, product and tails at the right. */
public class EnrichmentUnitCategory implements IRecipeCategory<EnrichmentUnitCategory.Recipe>
{
	public static final String UID = ImmersiveNuclear.MODID + ".enrichment_unit";
	static final int W = 176, H = 100;
	static final int FEED_X = 4, PRODUCT_X = 128, TAILS_X = 152, TANK_Y = 26, TEXT_Y = 88;
	static final int BANKS_X = 40, BANKS_Y = 28, BANK_W = 11, BANK_PITCH = 13, BANK_H = 36, ARROW_X = 60, ARROW_Y = 68;
	static final int COL_ON = 0xffc4c8ca, COL_OFF = 0xff3a3d42, COL_BAND = 0xff2a2c30;

	/** One cycle of one assembly. */
	public static class Recipe implements IRecipeWrapper
	{
		final int banks;

		public Recipe(int banks)
		{
			this.banks = banks;
		}

		@Override
		public void getIngredients(IIngredients ingredients)
		{
			ingredients.setInput(VanillaTypes.FLUID, new FluidStack(INContent.fluidUF6, TileEnrichmentBase.feedFor(banks)));
			ingredients.setOutputs(VanillaTypes.FLUID, Arrays.asList(new FluidStack(TileEnrichmentBase.productFor(banks), TileEnrichmentBase.PRODUCT_PER_CYCLE),
					new FluidStack(INContent.fluidUF6Depleted, TileEnrichmentBase.feedFor(banks) - TileEnrichmentBase.PRODUCT_PER_CYCLE)));
		}

		@Override
		public void drawInfo(Minecraft mc, int w, int h, int mx, int my)
		{
			for(int b = 0; b < 6; b++)
			{
				int x = BANKS_X + b * BANK_PITCH;
				JeiGraphite.rect(x, BANKS_Y, BANK_W, BANK_H, JeiGraphite.WELL);
				JeiGraphite.rect(x + 1, BANKS_Y + 1, BANK_W - 2, BANK_H - 2, b < banks ? COL_ON : COL_OFF);
				for(int y = BANKS_Y + 9; y < BANKS_Y + BANK_H - 4; y += 9)
					JeiGraphite.rect(x + 1, y, BANK_W - 2, 1, COL_BAND);
			}
			JeiGraphite.text(mc, TileEnrichmentBase.energyFor(banks) * TileEnrichmentBase.CYCLE_TICKS + " IF   " + TileEnrichmentBase.CYCLE_TICKS / 20 + " s", 0, TEXT_Y, w);
			JeiGraphite.text(mc, I18n.format("jei.immersivenuclear.enrichment_unit.banks", banks), 0, 6, w);
		}
	}

	final JeiGraphite g;
	final IDrawable background, icon, arrow;
	final IDrawableAnimated arrowFill;

	public EnrichmentUnitCategory(IGuiHelper helper)
	{
		g = new JeiGraphite(helper);
		background = helper.createBlankDrawable(W, H);
		arrow = helper.createDrawable(JeiGraphite.GRAPHITE, 184, 0, 19, 13);
		arrowFill = helper.createAnimatedDrawable(helper.createDrawable(JeiGraphite.GRAPHITE, 184, 13, 19, 13), 60, IDrawableAnimated.StartDirection.LEFT, false);
		icon = helper.createDrawableIngredient(new ItemStack(INContent.itemEnrichmentUnit));
	}

	@Override
	public String getUid()
	{
		return UID;
	}

	@Override
	public String getTitle()
	{
		return I18n.format("tile.immersivenuclear.enrichment_unit.title");
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
		JeiGraphite.pipe(FEED_X + 20, TANK_Y + 26, BANKS_X, TANK_Y + 26);
		JeiGraphite.pipe(BANKS_X + 6 * BANK_PITCH - 2, TANK_Y + 26, PRODUCT_X, TANK_Y + 26);
		GlStateManager.color(1, 1, 1, 1);
		mc.getTextureManager().bindTexture(JeiGraphite.GRAPHITE);
		arrow.draw(mc, ARROW_X + 12, ARROW_Y);
		arrowFill.draw(mc, ARROW_X + 12, ARROW_Y);
		g.tank(mc, FEED_X, TANK_Y, true);
		g.tank(mc, PRODUCT_X, TANK_Y, false);
		g.tank(mc, TAILS_X, TANK_Y, false);
	}

	@Override
	public void setRecipe(IRecipeLayout layout, Recipe recipe, IIngredients ingredients)
	{
		IGuiFluidStackGroup fluids = layout.getFluidStacks();
		fluids.init(0, true, FEED_X + 2, TANK_Y + 2, 16, 47, TileEnrichmentBase.feedFor(6), false, g.gauge);
		fluids.init(1, false, PRODUCT_X + 2, TANK_Y + 2, 16, 47, TileEnrichmentBase.PRODUCT_PER_CYCLE * 2, false, g.gauge);
		fluids.init(2, false, TAILS_X + 2, TANK_Y + 2, 16, 47, TileEnrichmentBase.feedFor(6), false, g.gauge);
		fluids.set(ingredients);
	}

	@Override
	public List<String> getTooltipStrings(int mx, int my)
	{
		return Collections.emptyList();
	}
}
