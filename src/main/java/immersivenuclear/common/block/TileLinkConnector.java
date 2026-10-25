package immersivenuclear.common.block;

import blusunrize.immersiveengineering.api.TargetingInfo;
import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler.Connection;
import blusunrize.immersiveengineering.api.energy.wires.TileEntityImmersiveConnectable;
import blusunrize.immersiveengineering.api.energy.wires.WireType;
import immersivenuclear.common.wire.LinkWireType;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class TileLinkConnector extends TileEntityImmersiveConnectable
{
	@Override
	public boolean canConnectCable(WireType cableType, TargetingInfo targetingInfo, Vec3i offset)
	{
		return cableType == LinkWireType.INSTANCE;
	}

	@Override
	public WireType getCableLimiter(TargetingInfo targetingInfo)
	{
		return LinkWireType.INSTANCE;
	}

	@Override
	public boolean isEnergyOutput()
	{
		return false;
	}

	@Override
	public Vec3d getConnectionOffset(Connection connection)
	{
		Vec3d offset = new Vec3d(0.5, 0.25, 0.5);
		if(world != null && pos != null)
		{
			net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
			if(state.getBlock() instanceof BlockLinkConnector)
			{
				net.minecraft.util.EnumFacing facing = state.getValue(BlockLinkConnector.FACING);
				Vec3i direction = facing.getDirectionVec();
				offset = new Vec3d(
					0.5 - direction.getX() * 0.3125,
					0.5 - direction.getY() * 0.3125,
					0.5 - direction.getZ() * 0.3125);
			}
		}
		return offset;
	}

	@Override
	public boolean allowEnergyToPass(Connection connection)
	{
		return false;
	}
}
