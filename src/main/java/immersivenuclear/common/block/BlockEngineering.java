package immersivenuclear.common.block;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.common.Loader;

/**
 * Engineering block, like IE's light/heavy engineering: a metal casing for the mod's machines.
 * One block per kind, so CTM (which joins neighbours of the same block) never merges two kinds.
 */
public class BlockEngineering extends Block
{
	/** Registry names; textures in blocks/<name>.png and blocks/ctm/<name>_ctm.png */
	public static final String[] KINDS = {"precision_engineering", "nuclear_engineering", "control_engineering", "radioactive_handling"};

	public BlockEngineering(String name)
	{
		// Same as IE's metal decoration
		super(Material.IRON);
		setHardness(3f);
		setResistance(15f);
		setSoundType(SoundType.METAL);
		setHarvestLevel("pickaxe", 1);
		setRegistryName(ImmersiveNuclear.MODID, name);
		setTranslationKey(ImmersiveNuclear.MODID + "." + name);
		setCreativeTab(INContent.TAB);
	}

	/** With CTM the frame is a cutout overlay (models/block/ctm); without it, the plain cube only needs SOLID. */
	@Override
	public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer)
	{
		return layer == BlockRenderLayer.SOLID || (layer == BlockRenderLayer.CUTOUT && Loader.isModLoaded("ctm"));
	}
}
