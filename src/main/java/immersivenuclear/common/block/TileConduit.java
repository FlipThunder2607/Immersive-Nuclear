package immersivenuclear.common.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import immersivenuclear.common.wire.ConduitRules;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

/** What a conduit keeps besides its support: the sides cut with the Engineer's Hammer, which do not connect (a new conduit has none cut), and the wires laid in it, to draw them and to count them. */
public class TileConduit extends TileEntity
{
	/** A wire through this block: its lane across the tray and the two sides it leaves by. The whole wire is in ConduitLinks, under the same id. */
	public static class Wire
	{
		public final int id, kind, lane;
		public final EnumFacing sideA, sideB;

		public Wire(int id, int kind, int lane, EnumFacing sideA, EnumFacing sideB)
		{
			this.id = id;
			this.kind = kind;
			this.lane = lane;
			this.sideA = sideA;
			this.sideB = sideB;
		}
	}

	/** One bit per side (EnumFacing.getIndex()), set when that side is cut. */
	private int cut;
	private final List<Wire> wires = new ArrayList<>();

	public List<Wire> getWires()
	{
		return Collections.unmodifiableList(wires);
	}

	/** Whether there is room for one more wire of that kind. */
	public boolean fits(int kind)
	{
		int power = 0, data = 0;
		for(Wire wire : wires)
			if(wire.kind == ConduitRules.POWER)
				power = 1;    // Branches of one network share the same wire
			else
				data++;
		return ConduitRules.fits(power, data, kind);
	}

	/** This block's part of a wire, or null. */
	public Wire wireOf(int id)
	{
		for(Wire wire : wires)
			if(wire.id == id)
				return wire;
		return null;
	}

	/** The lanes taken, one bit each. */
	public int usedLanes()
	{
		int used = 0;
		for(Wire wire : wires)
			used |= wire.kind == ConduitRules.POWER ? ConduitRules.POWER_LANES : 1 << wire.lane;
		return used;
	}

	public boolean hasWireOn(EnumFacing side)
	{
		for(Wire wire : wires)
			if(wire.sideA == side || wire.sideB == side)
				return true;
		return false;
	}

	public void addWire(Wire wire)
	{
		wires.add(wire);
		changed();
	}

	public void removeWire(int id)
	{
		if(wires.removeIf(wire -> wire.id == id))
			changed();
	}

	private void changed()
	{
		markDirty();
		IBlockState state = world.getBlockState(pos);
		world.notifyBlockUpdate(pos, state, state, 3);
	}

	public boolean isCut(EnumFacing side)
	{
		return (cut & 1 << side.getIndex()) != 0;
	}

	public void setCut(EnumFacing side, boolean value)
	{
		cut = value ? cut | 1 << side.getIndex() : cut & ~(1 << side.getIndex());
		changed();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		cut = nbt.getByte("cut");
		wires.clear();
		int[] saved = nbt.getIntArray("wires");
		for(int i = 0; i + 4 < saved.length; i += 5)
			wires.add(new Wire(saved[i], saved[i + 1], saved[i + 2], EnumFacing.byIndex(saved[i + 3]), EnumFacing.byIndex(saved[i + 4])));
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		nbt.setByte("cut", (byte)cut);
		int[] saved = new int[wires.size() * 5];
		for(int i = 0; i < wires.size(); i++)
		{
			Wire wire = wires.get(i);
			saved[i * 5] = wire.id;
			saved[i * 5 + 1] = wire.kind;
			saved[i * 5 + 2] = wire.lane;
			saved[i * 5 + 3] = wire.sideA.getIndex();
			saved[i * 5 + 4] = wire.sideB.getIndex();
		}
		nbt.setIntArray("wires", saved);
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
