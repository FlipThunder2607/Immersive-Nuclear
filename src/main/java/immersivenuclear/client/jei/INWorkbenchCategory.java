package immersivenuclear.client.jei;

import blusunrize.immersiveengineering.api.crafting.IngredientStack;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INAssembly;
import immersivenuclear.common.INWorkbenchRecipe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

/**
 * JEI page of the IN Workbench (15/11/2026): on the graphite plate, the blueprint, the six materials in a row as on the bench's
 * mat, the arrow and the product. Only the slots the recipe uses are drawn (the user: nothing empty sticking out), the arrow and the
 * product right after them. One page per item of each blueprint's family.
 */
public class INWorkbenchCategory implements IRecipeCategory<INWorkbenchCategory.Wrapper>
{
	public static final String UID = ImmersiveNuclear.MODID + ".in_workbench";
	static final int H = 48;
	/** As wide as the longest recipe needs (the page never shows empty slots). */
	final int W;
	static final int BP_X = 6, ROW_Y = 16, MAT_X = 32, STEP = 20;

	static int arrowX(int inputs)
	{
		return MAT_X + inputs * STEP + 2;
	}

	static int outX(int inputs)
	{
		return arrowX(inputs) + 26;
	}

	public static class Wrapper implements IRecipeWrapper
	{
		final INWorkbenchRecipe recipe;
		static INWorkbenchCategory category;

		public Wrapper(INWorkbenchRecipe recipe)
		{
			this.recipe = recipe;
		}

		/** The slots this recipe uses, the arrow and the product (the plate is the category's). */
		@Override
		public void drawInfo(Minecraft mc, int w, int h, int mx, int my)
		{
			if(category == null)
				return;
			int n = recipe.inputs.length;
			category.g.slot(mc, BP_X, ROW_Y, null);
			for(int i = 0; i < n; i++)
				category.g.slot(mc, MAT_X + i * STEP, ROW_Y, null);
			GlStateManager.color(1, 1, 1, 1);
			category.arrow.draw(mc, arrowX(n), ROW_Y + 2);
			category.g.slot(mc, outX(n), ROW_Y, null);
		}

		@Override
		public void getIngredients(IIngredients ingredients)
		{
			List<List<ItemStack>> in = new ArrayList<>();
			in.add(Collections.singletonList(new ItemStack(recipe.blueprint)));
			for(IngredientStack s : recipe.inputs)
				in.add(s.getSizedStackList());
			ingredients.setInputLists(VanillaTypes.ITEM, in);
			ingredients.setOutput(VanillaTypes.ITEM, recipe.output.copy());
		}
	}

	public static List<Wrapper> recipes()
	{
		List<Wrapper> l = new ArrayList<>();
		for(List<INWorkbenchRecipe> family : INWorkbenchRecipe.BY_BLUEPRINT.values())
			for(INWorkbenchRecipe r : family)
				l.add(new Wrapper(r));
		return l;
	}

	final JeiGraphite g;
	final IDrawable background, icon, arrow;

	public INWorkbenchCategory(IGuiHelper helper)
	{
		g = new JeiGraphite(helper);
		int most = 1;
		for(List<INWorkbenchRecipe> family : INWorkbenchRecipe.BY_BLUEPRINT.values())
			for(INWorkbenchRecipe r : family)
				most = Math.max(most, r.inputs.length);
		W = outX(most) + 24;
		background = helper.createBlankDrawable(W, H);
		arrow = helper.createDrawable(JeiGraphite.GRAPHITE, 184, 0, 19, 13);
		icon = helper.createDrawableIngredient(new ItemStack(INAssembly.itemINWorkbench));
		Wrapper.category = this;
	}

	@Override
	public String getUid()
	{
		return UID;
	}

	@Override
	public String getTitle()
	{
		return I18n.format("tile.immersivenuclear.in_workbench.name");
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
	}

	@Override
	public void setRecipe(IRecipeLayout layout, Wrapper recipe, IIngredients ingredients)
	{
		IGuiItemStackGroup items = layout.getItemStacks();
		int n = recipe.recipe.inputs.length;
		items.init(0, true, BP_X - 1, ROW_Y - 1);
		for(int i = 0; i < n; i++)
			items.init(1 + i, true, MAT_X + i * STEP - 1, ROW_Y - 1);
		items.init(1 + n, false, outX(n) - 1, ROW_Y - 1);
		items.set(ingredients);
	}
}
