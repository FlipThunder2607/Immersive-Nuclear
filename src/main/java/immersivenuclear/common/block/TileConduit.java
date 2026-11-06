package immersivenuclear.common.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

/** What a conduit keeps besides its support: the sides cut with the Engineer's Hammer, which do not connect. A new conduit has none cut. */
public class TileConduit extends TileEntity
{
	/** One bit per side (EnumFacing.getIndex()), set when that side is cut. */
	private int cut;

	public boolean isCut(EnumFacing side)
	{
		return (cut & 1 << side.getIndex()) != 0;
	}

	public void setCut(EnumFacing side, boolean value)
	{
		cut = value ? cut | 1 << side.getIndex() : cut & ~(1 << side.getIndex());
		markDirty();
		IBlockState state = world.getBlockState(pos);
		world.notifyBlockUpdate(pos, state, state, 3);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		cut = nbt.getByte("cut");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		nbt.setByte("cut", (byte)cut);
		return nbt;
	}

	@Override
	public NBTTagCompound getUpdateTag()
	{
		return writeToNBT(new NBTTagCompound());
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket()
	{
		return new SPacketUpdateTileEntity(pos, 3, getUpdateTag());
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt)
	{
		readFromNBT(pkt.getNbtCompound());
		world.markBlockRangeForRenderUpdate(pos, pos);
	}
}
