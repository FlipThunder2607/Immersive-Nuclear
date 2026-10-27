package immersivenuclear.common.block;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Light for a formed machine's model. The whole machine is one model drawn by its master block, and Minecraft lights
 * each face from the block next to the master on that side: a block put on top of the master (or any solid neighbour)
 * has no light of its own, so every face turned that way went black. Here a spot takes the brightest light of itself and
 * its six neighbours, so a solid block beside the master no longer darkens the model.
 */
public final class MachineLight
{
	private MachineLight()
	{
	}

	/** Packed sky and block light (as getCombinedLight) for a spot of the machine's model. */
	public static int packed(IBlockAccess world, BlockPos pos)
	{
		int sky = 0, block = 0;
		for(int i = -1; i < 6; i++)
		{
			int c = world.getCombinedLight(i < 0 ? pos : pos.offset(EnumFacing.byIndex(i)), 0);
			sky = Math.max(sky, c >> 20 & 15);
			block = Math.max(block, c >> 4 & 15);
		}
		return sky << 20 | block << 4;
	}
}
