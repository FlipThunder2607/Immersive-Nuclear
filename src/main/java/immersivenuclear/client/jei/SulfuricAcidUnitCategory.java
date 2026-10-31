package immersivenuclear.client.jei;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileSulfuricAcidUnit;
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
 * JEI page of the Sulfuric Acid Unit, a compact copy of its interface (GuiSulfuricAcidUnit) in the mod's finish:
 * sulfur into the BURNER (blue flame) > CONVERTER (four catalyst beds) > ABSORPTION TOWER > sulfuric acid; the water
 * goes in at the tower's top, the steam comes off the boiler. Energy and time come from the recipe (drawInfo).
 */
public class SulfuricAcidUnitCategory implements IRecipeCategory<SulfuricAcidUnitRecipe>
{
	public static final String UID = ImmersiveNuclear.MODID + ".sulfuric_acid_unit";
	static final int W = 176, H = 100;
	static final int SULFUR_X = 34, SULFUR_Y = 4;
	static final int WATER_X = 4, TANK_Y = 26;
	static final int BURNER_X = 30, BURNER_Y = 40, BURNER_W = 26, BURNER_H = 16;
	static final int CONV_X = 62, CONV_Y = 18, CONV_W = 12, CONV_H = 58;
	static final int TOWER_X = 80, TOWER_Y = 18, TOWER_W = 12, TOWER_H = 58;
	static final int ARROW_X = 98, ARROW_Y = 44;
	static final int ACID_X = 124, STEAM_X = 150, TEXT_Y = 88;
	/** The steam's line, from under the burner (the boiler) along the bottom to its tank. */
	static final int STEAM_Y = 81;
	static final int COL_BRICK = 0xff6e3428, COL_FIREBOX = 0xff101222, COL_INSIDE = 0xff24262a, COL_STEEL = 0xff96999e,
			COL_PACKING = 0xffc8c4b8, COL_ACID = 0xffd6c896;

	final JeiGraphite g;
	final IDrawable background, icon, arrow;
	final IDrawableAnimated arrowFill;

	public SulfuricAcidUnitCategory(IGuiHelper helper)
	{
		g = new JeiGraphite(helper);
		background = helper.createBlankDrawable(W, H);
		arrow = helper.createDrawable(JeiGraphite.GRAPHITE, 184, 0, 19, 13);
		arrowFill = helper.createAnimatedDrawable(helper.createDrawable(JeiGraphite.GRAPHITE, 184, 13, 19, 13), 60, IDrawableAnimated.StartDirection.LEFT, false);
		icon = helper.createDrawableIngredient(new ItemStack(INContent.itemSulfuricAcidUnit));
	}

	@Override
	public String getUid()
	{
		return UID;
	}

	@Override
	public String getTitle()
	{
		return I18n.format("tile.immersivenuclear.sulfuric_acid_unit.name");
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
		// Pipes first, the wells over their ends: water to the tower's top, burner > converter > tower > arrow > acid,
		// steam from over the burner (the boiler) to its tank
		JeiGraphite.pipe(WATER_X + 10, TANK_Y - 1, WATER_X + 10, 10);
		JeiGraphite.pipe(WATER_X + 10, 10, TOWER_X + 6, 10);
		JeiGraphite.pipe(TOWER_X + 6, 10, TOWER_X + 6, TOWER_Y);
		JeiGraphite.pipe(BURNER_X + BURNER_W, BURNER_Y + 8, CONV_X, BURNER_Y + 8);
		JeiGraphite.pipe(CONV_X + CONV_W, CONV_Y + CONV_H - 6, TOWER_X, CONV_Y + CONV_H - 6);
		JeiGraphite.pipe(TOWER_X + TOWER_W, ARROW_Y + 6, ARROW_X, ARROW_Y + 6);
		JeiGraphite.pipe(ARROW_X + 19, ARROW_Y + 6, ACID_X, ARROW_Y + 6);
		JeiGraphite.pipe(BURNER_X + BURNER_W / 2, BURNER_Y + BURNER_H, BURNER_X + BURNER_W / 2, STEAM_Y);
		JeiGraphite.pipe(BURNER_X + BURNER_W / 2, STEAM_Y, STEAM_X + 10, STEAM_Y);
		JeiGraphite.pipe(STEAM_X + 10, STEAM_Y, STEAM_X + 10, TANK_Y + 51);
		JeiGraphite.rect(SULFUR_X + 3, SULFUR_Y + 18, 10, BURNER_Y - SULFUR_Y - 18, JeiGraphite.WELL);

		// The burner: firebrick round the blue flame
		JeiGraphite.rect(BURNER_X, BURNER_Y, BURNER_W, BURNER_H, JeiGraphite.WELL);
		JeiGraphite.rect(BURNER_X + 2, BURNER_Y + 2, BURNER_W - 4, BURNER_H - 4, COL_BRICK);
		JeiGraphite.rect(BURNER_X + 4, BURNER_Y + 4, BURNER_W - 8, BURNER_H - 8, COL_FIREBOX);
		for(int i = 0; i < BURNER_W - 8; i++)
		{
			float k = Math.min(1, (i + 2) / 6F) * Math.min(1, (BURNER_W - 8 - i) / 4F);
			JeiGraphite.rect(BURNER_X + 4 + i, BURNER_Y + 6, 1, BURNER_H - 12, 0xff000000 | Math.round(40 + 150 * k) << 16 | Math.round(70 + 160 * k) << 8 | Math.round(160 + 95 * k));
		}
		// The converter: four beds, hottest at the top
		JeiGraphite.rect(CONV_X, CONV_Y, CONV_W, CONV_H, JeiGraphite.WELL);
		JeiGraphite.rect(CONV_X + 2, CONV_Y + 2, CONV_W - 4, CONV_H - 4, COL_INSIDE);
		for(int k = 0; k < 4; k++)
		{
			float hot = 1 - k / 4F;
			int y0 = CONV_Y + 4 + k * (CONV_H - 8) / 4;
			JeiGraphite.rect(CONV_X + 2, y0, CONV_W - 4, 8, 0xff000000 | Math.round(90 + 120 * hot) << 16 | Math.round(60 + 40 * hot) << 8 | 30);
			JeiGraphite.rect(CONV_X + 2, y0 + 8, CONV_W - 4, 1, COL_STEEL);
		}
		// The absorption tower: packing, acid at the foot
		JeiGraphite.rect(TOWER_X, TOWER_Y, TOWER_W, TOWER_H, JeiGraphite.WELL);
		JeiGraphite.rect(TOWER_X + 2, TOWER_Y + 2, TOWER_W - 4, TOWER_H - 4, COL_INSIDE);
		for(int y = TOWER_Y + 6; y < TOWER_Y + TOWER_H - 8; y += 3)
			for(int x = TOWER_X + 3 + y % 2; x < TOWER_X + TOWER_W - 3; x += 3)
				JeiGraphite.rect(x, y, 2, 2, COL_PACKING);
		JeiGraphite.rect(TOWER_X + 2, TOWER_Y + TOWER_H - 8, TOWER_W - 4, 6, COL_ACID);

		GlStateManager.color(1, 1, 1, 1);
		mc.getTextureManager().bindTexture(JeiGraphite.GRAPHITE);
		arrow.draw(mc, ARROW_X, ARROW_Y);
		arrowFill.draw(mc, ARROW_X, ARROW_Y);
		g.slot(mc, SULFUR_X, SULFUR_Y, true);
		g.tank(mc, WATER_X, TANK_Y, true);
		g.tank(mc, ACID_X, TANK_Y, false);
		g.tank(mc, STEAM_X, TANK_Y, false);
	}

	@Override
	public void setRecipe(IRecipeLayout layout, SulfuricAcidUnitRecipe recipe, IIngredients ingredients)
	{
		IGuiItemStackGroup items = layout.getItemStacks();
		// JEI puts the item 1 px inside the slot position
		items.init(0, true, SULFUR_X - 1, SULFUR_Y - 1);
		items.set(ingredients);
		IGuiFluidStackGroup fluids = layout.getFluidStacks();
		fluids.init(0, true, WATER_X + 2, TANK_Y + 2, 16, 47, TileSulfuricAcidUnit.WATER_CAPACITY, false, g.gauge);
		fluids.init(1, false, ACID_X + 2, TANK_Y + 2, 16, 47, TileSulfuricAcidUnit.ACID_CAPACITY, false, g.gauge);
		fluids.init(2, false, STEAM_X + 2, TANK_Y + 2, 16, 47, TileSulfuricAcidUnit.STEAM_CAPACITY, false, g.gauge);
		fluids.set(ingredients);
	}

	@Override
	public List<String> getTooltipStrings(int mx, int my)
	{
		String k = "gui.immersivenuclear.sulfuric_acid_unit.";
		if(mx >= BURNER_X && mx < BURNER_X + BURNER_W && my >= BURNER_Y && my < BURNER_Y + BURNER_H)
			return Arrays.asList(I18n.format(k + "burner"), I18n.format(k + "burner_info"));
		if(mx >= CONV_X && mx < CONV_X + CONV_W && my >= CONV_Y && my < CONV_Y + CONV_H)
			return Arrays.asList(I18n.format(k + "converter"), I18n.format(k + "converter_info"));
		if(mx >= TOWER_X && mx < TOWER_X + TOWER_W && my >= TOWER_Y && my < TOWER_Y + TOWER_H)
			return Arrays.asList(I18n.format(k + "tower"), I18n.format(k + "tower_info"));
		return Collections.emptyList();
	}
}
