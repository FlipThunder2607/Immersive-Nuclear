package immersivenuclear.common.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * What every part of a formed machine shares (15/11/2026, the code review: the machines' tiles each had the same 120 lines):
 * where the part sits in the structure (its offset from the trigger, how the structure is turned and mirrored), the block it
 * replaced (given back when the machine comes apart), the master's position and tile, the world face of its port, and the
 * part's NBT. Each machine gives its structure through {@link Layout}, its Structure and Multiblock classes' static helpers;
 * a machine overrides whatever it does differently.
 */
public abstract class TileMachinePart extends TileEntity implements MachinePartTile
{
	/** A machine's structure, as the static helpers of its Structure and Multiblock classes give it. */
	public interface Layout
	{
		char get(int dx, int dy, int dz);

		int portAt(int dx, int dy, int dz);

		int[] portFace(int dx, int dy, int dz);

		int[] rotate(int dx, int dz, int turns, boolean mirror);

		int turns(EnumFacing facing);

		BlockPos toWorld(BlockPos trigger, EnumFacing facing, boolean mirror, int dx, int dy, int dz);

		ItemStack stackFor(char c);

		/** The master's offset in the structure: {dx, dy, dz}. */
		int[] master();
	}

	int dx, dy, dz;
	EnumFacing facing = EnumFacing.NORTH;
	boolean mirrored;
	IBlockState originalState;
	NBTTagCompound originalNbt;

	protected abstract Layout layout();

	// ---------- the part ----------
	public void setPart(int dx, int dy, int dz, EnumFacing facing, boolean mirrored)
	{
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		this.facing = facing;
		this.mirrored = mirrored;
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

	public char getPartCode()
	{
		return layout().get(dx, dy, dz);
	}

	@Override
	public int getPort()
	{
		return layout().portAt(dx, dy, dz);
	}

	/** What a broken part drops: the block it replaced. */
	public ItemStack getOriginalStack()
	{
		return layout().stackFor(getPartCode()).copy();
	}

	public boolean isMaster()
	{
		int[] m = layout().master();
		return dx == m[0] && dy == m[1] && dz == m[2];
	}

	@Override
	public int[] fromMaster()
	{
		int[] m = layout().master();
		return new int[]{dx - m[0], dy - m[1], dz - m[2]};
	}

	@Override
	public EnumFacing getFacing()
	{
		return facing;
	}

	@Override
	public boolean isMirrored()
	{
		return mirrored;
	}

	/** World position of the trigger block this part was formed from (also the redstone port). */
	public BlockPos getTriggerPos()
	{
		int[] w = layout().rotate(dx, dz, layout().turns(facing), mirrored);
		return pos.add(-w[0], -dy, -w[1]);
	}

	public BlockPos getMasterPos()
	{
		int[] m = layout().master();
		return layout().toWorld(getTriggerPos(), facing, mirrored, m[0], m[1], m[2]);
	}

	/** The master's tile (this one on the master), or null when it is not loaded or not a master of this machine. */
	protected TileMachinePart master()
	{
		if(isMaster())
			return this;
		if(world == null)
			return null;
		BlockPos m = getMasterPos();
		if(!world.isBlockLoaded(m))
			return null;
		TileEntity te = world.getTileEntity(m);
		return getClass().isInstance(te) && ((TileMachinePart)te).isMaster() ? (TileMachinePart)te : null;
	}

	/** World face a port works on, or null when it works on every side. */
	EnumFacing portWorldFace()
	{
		int[] f = layout().portFace(dx, dy, dz);
		if(f == null || f[0] == 0 && f[1] == 0)
			return null;
		int[] w = layout().rotate(f[0], f[1], layout().turns(facing), mirrored);
		return EnumFacing.getFacingFromVector(w[0], 0, w[1]);
	}

	boolean isPart(int[] part)
	{
		return dx == part[0] && dy == part[1] && dz == part[2];
	}

	void readPart(NBTTagCompound nbt)
	{
		dx = nbt.getInteger("dx");
		dy = nbt.getInteger("dy");
		dz = nbt.getInteger("dz");
		facing = EnumFacing.byHorizontalIndex(nbt.getInteger("facing"));
		mirrored = nbt.getBoolean("mirrored");
	}

	void writePart(NBTTagCompound nbt)
	{
		nbt.setInteger("dx", dx);
		nbt.setInteger("dy", dy);
		nbt.setInteger("dz", dz);
		nbt.setInteger("facing", facing.getHorizontalIndex());
		nbt.setBoolean("mirrored", mirrored);
	}
}
