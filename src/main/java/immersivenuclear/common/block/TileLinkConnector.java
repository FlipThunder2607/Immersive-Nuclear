package immersivenuclear.common.block;

import blusunrize.immersiveengineering.api.TargetingInfo;
import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler;
import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler.Connection;
import blusunrize.immersiveengineering.api.energy.wires.TileEntityImmersiveConnectable;
import blusunrize.immersiveengineering.api.energy.wires.WireType;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.ICacheData;
import immersivenuclear.common.wire.LinkWireType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.Set;

/**
 * Link connector / link relay: only link cables, no energy. The connector takes a single cable and talks to the
 * device it is attached to ({@link #getDevice()}); the relay takes any number of cables and only bridges them.
 */
public class TileLinkConnector extends TileEntityImmersiveConnectable implements ICacheData
{
	/**
	 * IE caches the connection model by the block's properties only, so a bare relay and a bare connector (same
	 * facing, no cables) shared one model and the relay showed the connector's texture. This keeps them apart.
	 */
	@Override
	public Object[] getCacheData()
	{
		return new Object[]{getBlockType()};
	}

	@Override
	protected boolean isRelay()
	{
		return world != null && world.getBlockState(pos).getBlock() instanceof BlockLinkConnector
				&& ((BlockLinkConnector)world.getBlockState(pos).getBlock()).relay;
	}

	@Override
	public boolean canConnectCable(WireType cableType, TargetingInfo targetingInfo, Vec3i offset)
	{
		if(cableType != LinkWireType.INSTANCE)
			return false;
		if(isRelay())
			return true;
		Set<Connection> conns = ImmersiveNetHandler.INSTANCE.getConnections(world, pos);
		return conns == null || conns.isEmpty();
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
	public boolean allowEnergyToPass(Connection connection)
	{
		return false;
	}

	EnumFacing getFacing()
	{
		IBlockState state = world.getBlockState(pos);
		return state.getBlock() instanceof BlockLinkConnector ? state.getValue(BlockLinkConnector.FACING) : EnumFacing.UP;
	}

	/** The block the connector is attached to (null for a relay, which only bridges). */
	public TileEntity getDevice()
	{
		return isRelay() ? null : world.getTileEntity(pos.offset(getFacing().getOpposite()));
	}

	/** The cable leaves from the tip of the model, at the middle of the block (like IE's LV connector). */
	@Override
	public Vec3d getConnectionOffset(Connection connection)
	{
		if(world == null || pos == null)
			return new Vec3d(0.5, 0.5, 0.5);
		Vec3i dir = getFacing().getDirectionVec();
		double r = connection.cableType.getRenderDiameter() / 2;
		return new Vec3d(0.5 - dir.getX() * r, 0.5 - dir.getY() * r, 0.5 - dir.getZ() * r);
	}
}
