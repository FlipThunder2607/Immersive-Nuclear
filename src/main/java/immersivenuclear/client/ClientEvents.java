package immersivenuclear.client;

import java.util.Map;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.BlockChemicalProcessor;
import immersivenuclear.common.block.MachinePartBlock;
import immersivenuclear.common.block.TileChemicalProcessor;
import immersivenuclear.common.block.TileChemicalSeparator;
import immersivenuclear.common.block.TileFilterPress;
import immersivenuclear.common.block.TileIndustrialDryer;
import immersivenuclear.common.block.TileMetalExtractor;
import immersivenuclear.common.block.TilePrecipitationUnit;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = ImmersiveNuclear.MODID, value = Side.CLIENT)
public class ClientEvents
{
	@SubscribeEvent
	public static void onModelRegistry(ModelRegistryEvent event)
	{
		// The formed unit is drawn from models/block/obj/*.obj
		OBJLoader.INSTANCE.addDomain(ImmersiveNuclear.MODID);
		ModelLoader.setCustomModelResourceLocation(INContent.itemChemicalProcessor, 0, new ModelResourceLocation(INContent.itemChemicalProcessor.getRegistryName(), "inventory"));
		ModelLoader.setCustomModelResourceLocation(INContent.itemFilterPress, 0, new ModelResourceLocation(INContent.itemFilterPress.getRegistryName(), "inventory"));
		ModelLoader.setCustomModelResourceLocation(INContent.itemChemicalSeparator, 0, new ModelResourceLocation(INContent.itemChemicalSeparator.getRegistryName(), "inventory"));
		ModelLoader.setCustomModelResourceLocation(INContent.itemMetalExtractor, 0, new ModelResourceLocation(INContent.itemMetalExtractor.getRegistryName(), "inventory"));
		ModelLoader.setCustomModelResourceLocation(INContent.itemPrecipitationUnit, 0, new ModelResourceLocation(INContent.itemPrecipitationUnit.getRegistryName(), "inventory"));
		ModelLoader.setCustomModelResourceLocation(INContent.itemIndustrialDryer, 0, new ModelResourceLocation(INContent.itemIndustrialDryer.getRegistryName(), "inventory"));
		ModelLoader.setCustomModelResourceLocation(INContent.itemReagent, 0, new ModelResourceLocation(INContent.itemReagent.getRegistryName(), "inventory"));
		for(Item item : new Item[]{INContent.itemResidue, INContent.itemNeutralizedResidue, INContent.itemMixedDeposit, INContent.itemElectrodeSteel,
				INContent.itemElectrodeLeadSilver, INContent.itemElectrodeElectrum, INContent.itemQuicklime, INContent.itemCake, INContent.itemPrecipitatingAgent,
				INContent.itemWetConcentrate, INContent.itemYellowcake, INContent.itemAgglomerate})
			ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
		for(Item item : INContent.itemEngineering)
			ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
		for(Item item : INContent.itemComputerParts)
			ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
		// The item shows the connector's 3D model, like IE's connectors
		for(Item item : new Item[]{INContent.itemLinkConnector, INContent.itemLinkRelay})
			ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
		// Like IE's engineering: with CTM the placed blocks join into one surface (blockstates/engineering_ctm.json)
		if(Loader.isModLoaded("ctm"))
			for(Block block : INContent.blockEngineering)
				ModelLoader.setCustomStateMapper(block, new StateMapperBase()
				{
					@Override
					protected ModelResourceLocation getModelResourceLocation(IBlockState state)
					{
						return new ModelResourceLocation(ImmersiveNuclear.MODID + ":engineering_ctm", "block=" + state.getBlock().getRegistryName().getPath());
					}
				});
		// New, used and spent looks come from the "wear" property (models/item/filter.json)
		INContent.itemFilter.addWearProperty();
		ModelLoader.setCustomModelResourceLocation(INContent.itemFilter, 0, new ModelResourceLocation(INContent.itemFilter.getRegistryName(), "inventory"));
		// Liquid behind the tank windows
		ClientRegistry.bindTileEntitySpecialRenderer(TileChemicalProcessor.class, new RenderChemicalProcessor());
		// Moving plates and actuators of the filter press
		ClientRegistry.bindTileEntitySpecialRenderer(TileFilterPress.class, new RenderFilterPress());
		// Liquid layers behind the settlers' sight glasses
		ClientRegistry.bindTileEntitySpecialRenderer(TileChemicalSeparator.class, new RenderChemicalSeparator());
		// HV wires from the controller to the plates' platform
		ClientRegistry.bindTileEntitySpecialRenderer(TileMetalExtractor.class, new RenderMetalExtractor());
		// The agitator turning and the batch in the agitated tank
		ClientRegistry.bindTileEntitySpecialRenderer(TilePrecipitationUnit.class, new RenderPrecipitationUnit());
	}

	/** Textures are stitched by now (the item variant of the blockstate names them all). */
	@SubscribeEvent
	public static void onModelBake(ModelBakeEvent event)
	{
		RenderFilterPress.bake();
		RenderMetalExtractor.bake();
		RenderPrecipitationUnit.bake();
		// Every part of a formed machine draws its own slice of the machine's model
		for(Block b : new Block[]{INContent.blockChemicalProcessor, INContent.blockFilterPress, INContent.blockChemicalSeparator,
				INContent.blockMetalExtractor, INContent.blockPrecipitationUnit, INContent.blockIndustrialDryer})
			for(Map.Entry<IBlockState, ModelResourceLocation> e : event.getModelManager().getBlockModelShapes().getBlockStateMapper().getVariants(b).entrySet())
			{
				IBakedModel whole = event.getModelRegistry().getObject(e.getValue());
				if(whole != null && !(whole instanceof SlicedMachineModel))
					event.getModelRegistry().putObject(e.getValue(), new SlicedMachineModel(whole, (MachinePartBlock)b,
							e.getKey().getValue(BlockChemicalProcessor.FACING), e.getKey().getValue(BlockChemicalProcessor.MIRRORED)));
			}
	}

	/** Our fluids have no world block, so nothing else asks for their textures (bucket, tanks, pipes draw them). */
	@SubscribeEvent
	public static void onTextureStitch(TextureStitchEvent.Pre event)
	{
		if(event.getMap() != Minecraft.getMinecraft().getTextureMapBlocks())
			return;
		for(Fluid f : INContent.fluids)
		{
			event.getMap().registerSprite(f.getStill());
			event.getMap().registerSprite(f.getFlowing());
		}
	}
}
