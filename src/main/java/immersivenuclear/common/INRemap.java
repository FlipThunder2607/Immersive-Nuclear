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

/** The registry names used up to 0.1.0, mapped to the current ones so existing worlds keep their items (split from INContent, 15/11/2026). */
@Mod.EventBusSubscriber(modid = ImmersiveNuclear.MODID)
public final class INRemap
{
	private INRemap()
	{
	}

	/** Registry names used up to 0.1.0, mapped to the current ones so existing worlds keep their items. */
	static final Map<String, String> RENAMED = new HashMap<>();

	static
	{
		RENAMED.put("reagente_processamento", "processing_reagent");
		RENAMED.put("rejeito_mineral_umido", "wet_mineral_residue");
		RENAMED.put("filtro", "filter");
		RENAMED.put("cal", "quicklime");
		RENAMED.put("torta_neutralizacao", "neutralization_cake");
		RENAMED.put("agente_precipitante", "precipitating_agent");
		RENAMED.put("concentrado_umido_uranio", "wet_uranium_concentrate");
		RENAMED.put("aglomerado_impuro", "impure_agglomerate");
	}

	@SubscribeEvent
	public static void remapItems(RegistryEvent.MissingMappings<Item> event)
	{
		for(RegistryEvent.MissingMappings.Mapping<Item> m : event.getAllMappings())
		{
			String name = m.key.getNamespace().equals(ImmersiveNuclear.MODID) ? RENAMED.get(m.key.getPath()) : null;
			if(name != null)
				m.remap(ForgeRegistries.ITEMS.getValue(new ResourceLocation(ImmersiveNuclear.MODID, name)));
		}
	}

	@SubscribeEvent
	public static void remapBlocks(RegistryEvent.MissingMappings<Block> event)
	{
		for(RegistryEvent.MissingMappings.Mapping<Block> m : event.getAllMappings())
		{
			String name = m.key.getNamespace().equals(ImmersiveNuclear.MODID) ? RENAMED.get(m.key.getPath()) : null;
			if(name != null)
				m.remap(ForgeRegistries.BLOCKS.getValue(new ResourceLocation(ImmersiveNuclear.MODID, name)));
		}
	}
}
