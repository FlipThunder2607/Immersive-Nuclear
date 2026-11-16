package immersivenuclear.common;

import static immersivenuclear.common.INContent.*;

import immersivenuclear.common.block.BlockAgglomerateSlab;
import immersivenuclear.common.block.BlockAgglomerateStairs;
import immersivenuclear.common.block.BlockConcentrateDrum;
import immersivenuclear.common.block.BlockLeadedGlass;
import immersivenuclear.common.block.TileConcentrateDrum;
import immersivenuclear.common.item.ItemConcentrateDrum;
import java.util.HashMap;
import java.util.Map;

import blusunrize.immersiveengineering.api.MultiblockHandler;
import blusunrize.immersiveengineering.api.crafting.ArcFurnaceRecipe;
import blusunrize.immersiveengineering.api.crafting.CokeOvenRecipe;
import blusunrize.immersiveengineering.api.crafting.BottlingMachineRecipe;
import blusunrize.immersiveengineering.api.crafting.CrusherRecipe;
import blusunrize.immersiveengineering.api.crafting.MixerRecipe;
import blusunrize.immersiveengineering.api.energy.wires.WireApi;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.block.BlockChemicalProcessor;
import immersivenuclear.common.block.BlockChemicalSeparator;
import immersivenuclear.common.block.BlockEngineering;
import immersivenuclear.common.block.BlockLinkConnector;
import immersivenuclear.common.block.BlockFilterPress;
import immersivenuclear.common.block.BlockMetalExtractor;
import immersivenuclear.common.block.BlockIndustrialDryer;
import immersivenuclear.common.block.BlockSulfuricAcidUnit;
import immersivenuclear.common.block.BlockHFKiln;
import immersivenuclear.common.block.BlockDeconversionUnit;
import immersivenuclear.common.block.BlockPrecipitationUnit;
import immersivenuclear.common.block.TileChemicalProcessor;
import immersivenuclear.common.block.TileChemicalSeparator;
import immersivenuclear.common.block.TileFilterPress;
import immersivenuclear.common.block.TileLinkConnector;
import immersivenuclear.common.block.TileMetalExtractor;
import immersivenuclear.common.block.TileIndustrialDryer;
import immersivenuclear.common.block.TileSulfuricAcidUnit;
import immersivenuclear.common.block.TileHFKiln;
import immersivenuclear.common.block.TileDeconversionUnit;
import immersivenuclear.common.block.TilePrecipitationUnit;
import immersivenuclear.common.item.ItemElectrode;
import immersivenuclear.common.item.ItemFilter;
import immersivenuclear.common.item.ItemLinkCable;
import immersivenuclear.common.wire.LinkWireType;
import immersivenuclear.common.multiblock.MultiblockChemicalProcessor;
import immersivenuclear.common.multiblock.MultiblockChemicalSeparator;
import immersivenuclear.common.multiblock.MultiblockFilterPress;
import immersivenuclear.common.multiblock.MultiblockMetalExtractor;
import immersivenuclear.common.multiblock.MultiblockIndustrialDryer;
import immersivenuclear.common.multiblock.MultiblockSulfuricAcidUnit;
import immersivenuclear.common.multiblock.MultiblockHFKiln;
import immersivenuclear.common.multiblock.MultiblockDeconversionUnit;
import immersivenuclear.common.multiblock.MultiblockPrecipitationUnit;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemSlab;
import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;

/** The first lines' own recipes in other mods' machines (split from INContent, 15/11/2026): fluorite, IE's Crusher, Coke Oven and Mixer. */
public final class INRecipes
{
	private INRecipes()
	{
	}

	// Fluorite: the Crusher gets more out of the ore than breaking it does (1-2 crystals), as IE's ores
	public static final int FLUORITE_ORE_DUST = 3, FLUORITE_CRUSH_ENERGY = 4000;
	public static final int FLUORITE_EXCAVATOR_WEIGHT = 15;
	public static final float FLUORITE_EXCAVATOR_FAIL = .1F;

	/**
	 * Fluorite: ore dictionary names shared with other mods (Immersive Intelligence has fluorite too, so either mod's
	 * works in the other's machines), the Crusher, the furnace, the Overworld's veins and a vein of IE's Excavator:
	 * fluorite with the galena and quartz it is found with.
	 */
	public static void fluorite()
	{
		net.minecraftforge.oredict.OreDictionary.registerOre("oreFluorite", blockFluoriteOre);
		net.minecraftforge.oredict.OreDictionary.registerOre("gemFluorite", itemFluorite);
		net.minecraftforge.oredict.OreDictionary.registerOre("dustFluorite", itemFluoriteDust);
		net.minecraftforge.oredict.OreDictionary.registerOre("dustGypsum", itemGypsum);
		net.minecraftforge.oredict.OreDictionary.registerOre("dustDepletedUranium", itemDepletedUraniumOxide);
		net.minecraftforge.oredict.OreDictionary.registerOre("ingotDepletedUranium", itemDepletedUraniumIngot);
		CrusherRecipe.addRecipe(new ItemStack(itemFluoriteDust, FLUORITE_ORE_DUST), "oreFluorite", FLUORITE_CRUSH_ENERGY);
		CrusherRecipe.addRecipe(new ItemStack(itemFluoriteDust), "gemFluorite", FLUORITE_CRUSH_ENERGY / 2);
		GameRegistry.addSmelting(blockFluoriteOre, new ItemStack(itemFluorite), 0.2f);
		GameRegistry.registerWorldGenerator(new immersivenuclear.common.world.WorldGenFluorite(), 0);
		if(INConfig.fluoriteExcavatorVein)
			blusunrize.immersiveengineering.api.tool.ExcavatorHandler.addMineral("Fluorite", FLUORITE_EXCAVATOR_WEIGHT, FLUORITE_EXCAVATOR_FAIL,
					new String[]{"oreFluorite", "oreLead", "gemQuartz"}, new float[]{.7F, .2F, .1F});
	}

	// The Wet Mineral Residue, neutralized with quicklime, is crushed in IE's Crusher for what the leaching left in it
	public static final int RESIDUE_CRUSH_ENERGY = 3200;
	public static final float RESIDUE_IRON = .15F, RESIDUE_CLAY = .10F, RESIDUE_URANIUM = .03F;
	// The Mixed Metal Deposit: one per 2000 mB of effluent, so each chance is twice the old Recovery Unit's per 1000 mB
	public static final int DEPOSIT_CRUSH_ENERGY = 4800, SLAG_META = 7;
	public static final float DEPOSIT_IRON = .20F, DEPOSIT_ALUMINUM = .12F, DEPOSIT_COPPER = .08F, DEPOSIT_LEAD = .06F, DEPOSIT_NICKEL = .04F,
			DEPOSIT_URANIUM = .02F, DEPOSIT_CLAY = .30F, DEPOSIT_HAFNIUM = .02F;

	/**
	 * Neutralized residue: mostly gangue (sand), with some iron minerals, clay and the little uranium the leaching did
	 * not dissolve. The uranium stays a small bonus, so reworking residue never beats the line itself.
	 */
	public static void crusherRecipes()
	{
		// The Mixed Metal Deposit: slag (the plates' scale) and, by chance, the metals of the effluent
		CrusherRecipe.addRecipe(new ItemStack(blusunrize.immersiveengineering.common.IEContent.itemMaterial, 1, SLAG_META), new ItemStack(itemMixedDeposit), DEPOSIT_CRUSH_ENERGY)
				.addToSecondaryOutput(new ItemStack(net.minecraft.init.Items.IRON_NUGGET), DEPOSIT_IRON, "nuggetAluminum", DEPOSIT_ALUMINUM, "nuggetCopper", DEPOSIT_COPPER,
						"nuggetLead", DEPOSIT_LEAD, "nuggetNickel", DEPOSIT_NICKEL, "nuggetUranium", DEPOSIT_URANIUM, new ItemStack(net.minecraft.init.Items.CLAY_BALL), DEPOSIT_CLAY,
						// the hafnium the chemical effluent carries (15/11/2026): a nugget now and then, as the other metals, below the
						// Separation's own hafnium (cycle rule)
						"nuggetHafnium", DEPOSIT_HAFNIUM);
		CrusherRecipe.addRecipe(new ItemStack(net.minecraft.init.Blocks.SAND), new ItemStack(itemNeutralizedResidue), RESIDUE_CRUSH_ENERGY)
				.addToSecondaryOutput(new ItemStack(net.minecraft.init.Items.IRON_NUGGET), RESIDUE_IRON, new ItemStack(net.minecraft.init.Items.CLAY_BALL), RESIDUE_CLAY,
						"nuggetUranium", RESIDUE_URANIUM);
	}

	// Recipes in IE's machines (provisional amounts)
	public static final int SOLVENT_PER_MIX = 1000, STRIPPING_PER_MIX = 1000, SLUDGE_PER_MIX = 1000;
	public static final int SOLVENT_MIX_ENERGY = 3200, STRIPPING_MIX_ENERGY = 1600, SLUDGE_MIX_ENERGY = 1600;
	public static final int QUICKLIME_TICKS = 900;
	public static final int CONCRETE_PER_MIX = 500, CONCRETE_MIX_ENERGY = 3200;
	/** Gypsum in IE's Arc Furnace with coke dust: 2 gypsum -> 2 quicklime + 1 sulfur dust (provisional). */
	public static final int GYPSUM_PER_BURN = 2, GYPSUM_QUICKLIME = 2, GYPSUM_BURN_TICKS = 200, GYPSUM_BURN_ENERGY = 256;
	/** Gypsum in IE's concrete: it sets the cement, so the batch gives half as much again (provisional). */
	public static final int GYPSUM_CONCRETE_PER_MIX = 750;

	/** Quicklime: diorite (or limestone, when a mod adds it) burnt in IE's Coke Oven. */
	public static void cokeOvenRecipes()
	{
		CokeOvenRecipe.addRecipe(new ItemStack(itemQuicklime), "stoneDiorite", QUICKLIME_TICKS, 0);
		// The raw agglomerate fired into its finished, concrete-like block, as cobblestone into stone
		GameRegistry.addSmelting(blockAgglomerate, new ItemStack(blockAgglomerateBlock), 0.1f);
		// Lead glass, as it is made: glass melted with lead (provisional time and energy, like IE's alloys)
		ArcFurnaceRecipe.addRecipe(new ItemStack(itemLeadedGlass), "blockGlass", ItemStack.EMPTY, 100, 512, "dustLead");
		// The Deconversion's depleted uranium oxide reduced to metal with coke, as IE's ores (provisional time and energy)
		ArcFurnaceRecipe.addRecipe(new ItemStack(itemDepletedUraniumIngot), "dustDepletedUranium", ItemStack.EMPTY, 100, 512, "dustCoke");
		// Gypsum back to sulfur and quicklime, as the Mueller-Kuehne process: burnt with coke, the sulfur leaves (here as
		// the slag slot's sulfur dust) and quicklime stays; the sulfur goes back to the Sulfuric Acid Unit
		java.util.List<ItemStack> sulfur = net.minecraftforge.oredict.OreDictionary.getOres("dustSulfur");
		if(!sulfur.isEmpty())
			ArcFurnaceRecipe.addRecipe(new ItemStack(itemQuicklime, GYPSUM_QUICKLIME), new blusunrize.immersiveengineering.api.crafting.IngredientStack("dustGypsum", GYPSUM_PER_BURN),
					sulfur.get(0).copy(), GYPSUM_BURN_TICKS, GYPSUM_BURN_ENERGY, "dustCoke");
		if(!net.minecraftforge.oredict.OreDictionary.getOres("stoneLimestone").isEmpty())
			CokeOvenRecipe.addRecipe(new ItemStack(itemQuicklime, 2), "stoneLimestone", QUICKLIME_TICKS, 0);
	}

	/**
	 * IE's Mixer makes the two liquids the separation needs: extraction solvent from biodiesel and saltpeter, stripping
	 * solution from water and saltpeter; and neutralizes the effluent with quicklime into sludge. Biodiesel comes from
	 * IE's refinery; without it (no IE fluid) the solvent recipe is left out.
	 */
	public static void mixerRecipes()
	{
		Fluid biodiesel = FluidRegistry.getFluid("biodiesel");
		if(biodiesel != null)
			MixerRecipe.addRecipe(new FluidStack(fluidSolvent, SOLVENT_PER_MIX), new FluidStack(biodiesel, SOLVENT_PER_MIX), new Object[]{"dustSaltpeter"}, SOLVENT_MIX_ENERGY);
		else
			ImmersiveNuclear.logger.warn("No biodiesel fluid: the extraction solvent has no Mixer recipe");
		MixerRecipe.addRecipe(new FluidStack(fluidStrippingSolution, STRIPPING_PER_MIX), new FluidStack(FluidRegistry.WATER, STRIPPING_PER_MIX), new Object[]{"dustSaltpeter"}, STRIPPING_MIX_ENERGY);
		// Neutralizing the effluent (or what is left of it after the Metal Extractor) with quicklime
		for(Fluid effluent : new Fluid[]{fluidEffluent, fluidDepletedEffluent})
			MixerRecipe.addRecipe(new FluidStack(fluidSludge, SLUDGE_PER_MIX), new FluidStack(effluent, SLUDGE_PER_MIX), new Object[]{new ItemStack(itemQuicklime)}, SLUDGE_MIX_ENERGY);
		// The Conversion Unit's effluent: lime takes the fluoride out as calcium fluoride, the fluorite dust that goes back to the HF Kiln. IE's Bottling Machine, as the Mixer only makes fluids
		BottlingMachineRecipe.addRecipe(new ItemStack(itemFluoriteDust), new ItemStack(itemQuicklime), new FluidStack(fluidFluorinatedEffluent, FLUORINATED_EFFLUENT_PER_DUST));
		// IE's concrete with the neutralization cake in place of the clay (IE: 500 mB water, 2 sand, clay, gravel, 3200 IF)
		MixerRecipe.addRecipe(new FluidStack(blusunrize.immersiveengineering.common.IEContent.fluidConcrete, CONCRETE_PER_MIX), new FluidStack(FluidRegistry.WATER, CONCRETE_PER_MIX),
				new Object[]{"sand", "sand", new ItemStack(itemCake), "gravel"}, CONCRETE_MIX_ENERGY);
		// IE's concrete with gypsum in place of the clay: gypsum is what sets real cement, so a batch gives more concrete
		MixerRecipe.addRecipe(new FluidStack(blusunrize.immersiveengineering.common.IEContent.fluidConcrete, GYPSUM_CONCRETE_PER_MIX), new FluidStack(FluidRegistry.WATER, CONCRETE_PER_MIX),
				new Object[]{"sand", "sand", "dustGypsum", "gravel"}, CONCRETE_MIX_ENERGY);
	}
}
