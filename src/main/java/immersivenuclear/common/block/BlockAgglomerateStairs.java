package immersivenuclear.common.block;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;

/** Agglomerate brick stairs: vanilla stairs of the agglomerate bricks. */
public class BlockAgglomerateStairs extends BlockStairs
{
	public BlockAgglomerateStairs(IBlockState bricks)
	{
		super(bricks);
		setRegistryName(ImmersiveNuclear.MODID, "agglomerate_brick_stairs");
		setTranslationKey(ImmersiveNuclear.MODID + ".agglomerate_brick_stairs");
		setCreativeTab(INContent.TAB);
		useNeighborBrightness = true;
	}
}
