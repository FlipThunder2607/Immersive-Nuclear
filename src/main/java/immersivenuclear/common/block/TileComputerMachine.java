package immersivenuclear.common.block;

import immersivenuclear.common.multiblock.ComputerStructure;
import immersivenuclear.common.multiblock.MultiblockComputer;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/** A part of a formed computer multiblock. It only knows its place in the structure and the block it replaced: the machines have no function yet (the user, 05/11/2026). */
public class TileComputerMachine extends TileEntity implements MachinePartTile
{
	ComputerStructure structure = ComputerStructure.MAINFRAME;
	int dx, dy, dz;
	EnumFacing facing = EnumFacing.NORTH;
	IBlockState originalState;
	NBTTagCompound originalNbt;

	public void setPart(ComputerStructure structure, int dx, int dy, int dz, EnumFacing facing)
	{
		this.structure = structure;
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		this.facing = facing;
		markDirty();
	}

	public void setOriginal(IBlockState state, NBTTagCompound nbt)
	{
		originalState = state;
		originalNbt = nbt;
		markDirty();
	}

	public IBlockState getOriginalState()
	{
		return originalState;
	}

	public NBTTagCompound getOriginalNbt()
	{
		return originalNbt;
	}

	/** What a broken part drops: the block it replaced. */
	public ItemStack getOriginalStack()
	{
		return MultiblockComputer.stackFor(structure.get(dx, dy, dz)).copy();
	}

	public boolean isMaster()
	{
		return dx == structure.masterDx && dy == structure.masterDy && dz == structure.masterDz;
	}

	@Override
	public String boundsName()
	{
		return structure.name;
	}

	@Override
	public int[] fromMaster()
	{
		return new int[]{dx - structure.masterDx, dy - structure.masterDy, dz - structure.masterDz};
	}

	@Override
	public EnumFacing getFacing()
	{
		return facing;
	}

	@Override
	public boolean isMirrored()
	{
		return false;
	}

	@Override
	public int getPort()
	{
		return 0;
	}

	/** Whether this part has a conduit entry on the given side: the back of the machine, which faces east in the reference orientation (NORTH). */
	public boolean hasConduitEntry(EnumFacing side)
	{
		return side == facing.rotateY() && structure.hasConduitEntry(dx, dy, dz);
	}

	/** The side the machine's back looks to. */
	public EnumFacing getBack()
	{
		return facing.rotateY();
	}

	/** World positions of the parts with a conduit entry. */
	public java.util.List<BlockPos> conduitEntries()
	{
		java.util.List<BlockPos> entries = new java.util.ArrayList<>();
		for(int z = 0; z <= structure.maxDz; z++)
			if(structure.hasConduitEntry(structure.maxDx, 0, z))
				entries.add(MultiblockComputer.toWorld(getOrigin(), facing, structure.maxDx, 0, z));
		return entries;
	}

	/** World position of the structure's offset 0,0,0. */
	public BlockPos getOrigin()
	{
		int[] w = ComputerStructure.rotate(dx, dz, MultiblockComputer.turns(facing));
		return pos.add(-w[0], -dy, -w[1]);
	}

	public BlockPos getMasterPos()
	{
		return MultiblockComputer.toWorld(getOrigin(), facing, structure.masterDx, structure.masterDy, structure.masterDz);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		readPart(nbt);
		originalState = nbt.hasKey("originalState") ? Block.getStateById(nbt.getInteger("originalState")) : null;
		originalNbt = nbt.hasKey("originalNbt") ? nbt.getCompoundTag("originalNbt") : null;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		writePart(nbt);
		if(originalState != null)
			nbt.setInteger("originalState", Block.getStateId(originalState));
		if(originalNbt != null)
			nbt.setTag("originalNbt", originalNbt);
		return nbt;
	}

	void readPart(NBTTagCompound nbt)
	{
		structure = ComputerStructure.byName(nbt.getString("machine"));
		dx = nbt.getInteger("dx");
		dy = nbt.getInteger("dy");
		dz = nbt.getInteger("dz");
		facing = EnumFacing.byHorizontalIndex(nbt.getInteger("facing"));
	}

	void writePart(NBTTagCompound nbt)
	{
		nbt.setString("machine", structure.name);
		nbt.setInteger("dx", dx);
		nbt.setInteger("dy", dy);
		nbt.setInteger("dz", dz);
		nbt.setInteger("facing", facing.getHorizontalIndex());
	}

	/** The client needs which block of the structure this is and how it is turned. */
	@Override
	public NBTTagCompound getUpdateTag()
	{
		NBTTagCompound nbt = super.writeToNBT(new NBTTagCompound());
		writePart(nbt);
		return nbt;
	}

	@Override
	public void handleUpdateTag(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		readPart(nbt);
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket()
	{
		NBTTagCompound nbt = new NBTTagCompound();
		writePart(nbt);
		return new SPacketUpdateTileEntity(pos, 0, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt)
	{
		readPart(pkt.getNbtCompound());
		world.markBlockRangeForRenderUpdate(pos, pos);
	}
}
