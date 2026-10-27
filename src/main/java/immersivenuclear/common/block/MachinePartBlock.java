package immersivenuclear.common.block;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * A formed machine's block whose model is cut into one slice per block: every part draws, with the chunk it stands in,
 * the piece of the machine's model inside its own block (see client/SlicedMachineModel). So the machine does not vanish
 * with the master's chunk section, each piece takes the light where it stands, and nothing is drawn every frame.
 */
public interface MachinePartBlock
{
	/** The part's offset from the master, in world blocks (extended state, set from the tile entity). */
	IUnlistedProperty<BlockPos> OFFSET = new IUnlistedProperty<BlockPos>()
	{
		@Override
		public String getName()
		{
			return "offset";
		}

		@Override
		public boolean isValid(BlockPos value)
		{
			return true;
		}

		@Override
		public Class<BlockPos> getType()
		{
			return BlockPos.class;
		}

		@Override
		public String valueToString(BlockPos value)
		{
			return value.toString();
		}
	};

	/** Whether a world offset from the master is one of the structure's blocks, for a structure turned and mirrored so. */
	boolean isPartAt(EnumFacing facing, boolean mirrored, BlockPos offset);
}
