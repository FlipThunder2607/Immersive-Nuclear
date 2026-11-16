package immersivenuclear.common;

import blusunrize.immersiveengineering.api.crafting.ArcFurnaceRecipe;
import blusunrize.immersiveengineering.api.crafting.BlueprintCraftingRecipe;
import blusunrize.immersiveengineering.api.crafting.CrusherRecipe;
import blusunrize.immersiveengineering.api.crafting.IngredientStack;
import blusunrize.immersiveengineering.api.crafting.MetalPressRecipe;
import immersivenuclear.ImmersiveNuclear;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * Checks, once everything is loaded, that every recipe of the mod can be made: each input must match at least one item
 * (15/11/2026: two recipes asked for ore names that do not exist, 'elecTube' and 'componentSteel', and nobody noticed).
 * It looks at our crafting table recipes, the IN Workbench's, and the ones we add to IE's Arc Furnace, Metal Press, Crusher
 * and Engineer's Workbench (by their output). Each recipe that cannot be made is written to the log as an error, with the
 * input that matches nothing; the last line says how many were checked.
 */
public final class RecipeCheck
{
	private RecipeCheck()
	{
	}

	static boolean ours(ItemStack s)
	{
		return s != null && !s.isEmpty() && s.getItem().getRegistryName() != null && ImmersiveNuclear.MODID.equals(s.getItem().getRegistryName().getNamespace());
	}

	static boolean empty(IngredientStack in)
	{
		if(in == null)
			return false;
		if(in.fluid != null)
			return false;
		List<ItemStack> l = in.getStackList();
		return l == null || l.isEmpty();
	}

	static String name(IngredientStack in)
	{
		return in.oreName != null ? "ore '" + in.oreName + "'" : String.valueOf(in.getExampleStack());
	}

	public static void run()
	{
		List<String> bad = new ArrayList<>();
		int checked = 0;
		// the crafting table
		for(IRecipe r : ForgeRegistries.RECIPES.getValuesCollection())
		{
			ResourceLocation id = r.getRegistryName();
			if(id == null || !ImmersiveNuclear.MODID.equals(id.getNamespace()))
				continue;
			checked++;
			int slot = 0;
			for(Ingredient in : r.getIngredients())
			{
				if(in != Ingredient.EMPTY && in.getMatchingStacks().length == 0)
					bad.add("crafting " + id + ": ingredient " + slot + " matches nothing");
				slot++;
			}
		}
		// the IN Workbench
		for(List<INWorkbenchRecipe> family : INWorkbenchRecipe.BY_BLUEPRINT.values())
			for(INWorkbenchRecipe r : family)
			{
				checked++;
				for(IngredientStack in : r.inputs)
					if(empty(in))
						bad.add("IN Workbench " + r.output.getItem().getRegistryName() + ": " + name(in) + " matches nothing");
			}
		// what we add to IE's machines
		for(ArcFurnaceRecipe r : ArcFurnaceRecipe.recipeList)
			if(ours(r.output))
			{
				checked++;
				if(empty(r.input))
					bad.add("Arc Furnace " + r.output.getItem().getRegistryName() + ": " + name(r.input) + " matches nothing");
				if(r.additives != null)
					for(IngredientStack in : r.additives)
						if(empty(in))
							bad.add("Arc Furnace " + r.output.getItem().getRegistryName() + ": additive " + name(in) + " matches nothing");
			}
		for(MetalPressRecipe r : MetalPressRecipe.recipeList.values())
			if(ours(r.output))
			{
				checked++;
				if(empty(r.input))
					bad.add("Metal Press " + r.output.getItem().getRegistryName() + ": " + name(r.input) + " matches nothing");
			}
		for(CrusherRecipe r : CrusherRecipe.recipeList)
			if(ours(r.output))
			{
				checked++;
				if(empty(r.input))
					bad.add("Crusher " + r.output.getItem().getRegistryName() + ": " + name(r.input) + " matches nothing");
			}
		for(BlueprintCraftingRecipe r : BlueprintCraftingRecipe.recipeList.values())
			if(ours(r.output))
			{
				checked++;
				for(IngredientStack in : r.inputs)
					if(empty(in))
						bad.add("Engineer's Workbench " + r.output.getItem().getRegistryName() + ": " + name(in) + " matches nothing");
			}
		for(String b : bad)
			ImmersiveNuclear.logger.error("Recipe check: " + b);
		ImmersiveNuclear.logger.info("Recipe check: {} recipes checked, {} cannot be made", checked, bad.size());
	}
}
