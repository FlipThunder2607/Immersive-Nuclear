package immersivenuclear.common.block;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;

/** Plaster brick stairs: vanilla stairs of the plaster bricks. */
public class BlockPlasterStairs extends BlockStairs
{
	public BlockPlasterStairs(IBlockState bricks)
	{
		super(bricks);
		setRegistryName(ImmersiveNuclear.MODID, "plaster_brick_stairs");
		setTranslationKey(ImmersiveNuclear.MODID + ".plaster_brick_stairs");
		setCreativeTab(INContent.TAB);
		useNeighborBrightness = true;
	}
}
