package immersivenuclear.client.jei;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileMetalExtractor;
import immersivenuclear.common.item.ItemElectrode;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * One batch of the Metal Extractor with six plates of one material, with the amounts the tile uses
 * (TileMetalExtractor, ItemElectrode): one page per material.
 */
public class MetalExtractorRecipe implements IRecipeWrapper
{
	final ItemElectrode plate;

	public MetalExtractorRecipe(ItemElectrode plate)
	{
		this.plate = plate;
	}

	/** Deposits of a batch with six of these plates (TileMetalExtractor.finishPlates). */
	public int deposits()
	{
		return Math.round(plate.yield * TileMetalExtractor.DEPOSITS_PER_BATCH);
	}

	@Override
	public void getIngredients(IIngredients ingredients)
	{
		// The plates are worn, not used up: listed so they show the machine among their uses
		ingredients.setInput(VanillaTypes.ITEM, new ItemStack(plate, TileMetalExtractor.PLATES));
		ingredients.setInput(VanillaTypes.FLUID, new FluidStack(INContent.fluidEffluent, TileMetalExtractor.BATCH));
		ingredients.setOutput(VanillaTypes.FLUID, new FluidStack(INContent.fluidDepletedEffluent, TileMetalExtractor.BATCH));
		ingredients.setOutput(VanillaTypes.ITEM, new ItemStack(INContent.itemMixedDeposit, deposits()));
	}

	/** Energy and time of the batch, for its plates, beside the plates' slot (MetalExtractorCategory). */
	@Override
	public void drawInfo(net.minecraft.client.Minecraft mc, int width, int height, int mouseX, int mouseY)
	{
		JeiGraphite.text(mc, energy() + " IF   " + ticks() / 20 + " s", MetalExtractorCategory.TEXT_X, MetalExtractorCategory.TEXT_Y, width - MetalExtractorCategory.TEXT_X);
	}

	/** Ticks to drain one batch. */
	static int drainTicks()
	{
		return TileMetalExtractor.BATCH / TileMetalExtractor.DRAIN_PER_TICK;
	}

	/** Energy of one batch: prepare, recover, the pump draining, the cleaning, the plates going back down. */
	public int energy()
	{
		return TileMetalExtractor.PREPARE_ENERGY * TileMetalExtractor.PREPARE_TICKS + Math.round(TileMetalExtractor.RECOVER_ENERGY * plate.energy) * TileMetalExtractor.RECOVER_TICKS
				+ TileMetalExtractor.PUMP_ENERGY * drainTicks() + TileMetalExtractor.COLLECT_ENERGY * (TileMetalExtractor.TRAY_OUT + TileMetalExtractor.LOWER_TICKS);
	}

	/** Ticks of one batch, the fill aside: prepare, recover, drain and cleaning together, the plates going down. */
	public static int ticks()
	{
		return TileMetalExtractor.PREPARE_TICKS + TileMetalExtractor.RECOVER_TICKS + Math.max(drainTicks(), TileMetalExtractor.TRAY_OUT)
				+ TileMetalExtractor.LOWER_TICKS;
	}
}
