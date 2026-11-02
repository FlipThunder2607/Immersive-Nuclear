package immersivenuclear.client.jei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileSulfuricAcidUnit;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

/**
 * One cycle of the Sulfuric Acid Unit, with the amounts the tile uses (TileSulfuricAcidUnit): any mod's sulfur (dust,
 * gem, crystal), or a block of sulfur for nine cycles' worth in one.
 */
public class SulfuricAcidUnitRecipe implements IRecipeWrapper
{
	final int size;

	public SulfuricAcidUnitRecipe(boolean block)
	{
		size = block ? TileSulfuricAcidUnit.BLOCK_SIZE : 1;
	}

	/** Every item the ore dictionary knows under the names this cycle takes. */
	List<ItemStack> sulfur()
	{
		List<ItemStack> out = new ArrayList<>();
		String[] names = size == 1 ? new String[]{"dustSulfur", "gemSulfur", "crystalSulfur", "itemSulfur"} : new String[]{TileSulfuricAcidUnit.SULFUR_BLOCK};
		for(String n : names)
			for(ItemStack s : OreDictionary.getOres(n))
				if(s.getMetadata() == OreDictionary.WILDCARD_VALUE)
				{
					ItemStack one = s.copy();
					one.setItemDamage(0);
					out.add(one);
				}
				else
					out.add(s.copy());
		return out;
	}

	public boolean hasSulfur()
	{
		return !sulfur().isEmpty();
	}

	@Override
	public void getIngredients(IIngredients ingredients)
	{
		ingredients.setInputLists(VanillaTypes.ITEM, Collections.singletonList(sulfur()));
		ingredients.setInput(VanillaTypes.FLUID, new FluidStack(FluidRegistry.WATER, TileSulfuricAcidUnit.WATER_PER_CYCLE * size));
		List<FluidStack> out = new ArrayList<>();
		out.add(new FluidStack(INContent.fluidSulfuricAcid, TileSulfuricAcidUnit.ACID_PER_CYCLE * size));
		out.add(new FluidStack(INContent.fluidSteam, TileSulfuricAcidUnit.STEAM_PER_CYCLE * size));
		ingredients.setOutputs(VanillaTypes.FLUID, out);
	}

	public int ticks()
	{
		return TileSulfuricAcidUnit.CYCLE_TICKS * size;
	}

	public int energy()
	{
		return TileSulfuricAcidUnit.ENERGY_PER_TICK * ticks();
	}

	@Override
	public void drawInfo(Minecraft mc, int w, int h, int mx, int my)
	{
		JeiGraphite.text(mc, energy() + " IF   " + ticks() / 20 + " s", 0, PageCategory.TEXT_Y, w);
	}
}
