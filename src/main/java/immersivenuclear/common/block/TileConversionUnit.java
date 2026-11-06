package immersivenuclear.common.block;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IHammerInteraction;
import immersivenuclear.common.INContent;
import immersivenuclear.common.multiblock.ConversionUnitStructure;
import immersivenuclear.common.multiblock.MultiblockConversionUnit;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

/**
 * One part of a formed Conversion Unit: which template block it replaced and how the structure is turned. The master
 * part also holds the machine; the port parts hand their capability, on their own face only, to the master (see
 * {@link ConversionUnitStructure#PORTS}).
 *
 * Cycles of 10 s: a cycle starts with its yellowcake, hydrogen and hydrofluoric acid in the machine and room for its
 * hexafluoride and effluent; the yellowcake is reduced with the hydrogen, fluorinated with the acid (part of it split
 * into fluorine in the cells) and the hexafluoride gas is condensed in the cold traps into the autoclave. While it
 * runs it draws power (cells, reactor heating and cold traps). Now and then a little fluorination ash settles in the
 * reactor. Without power the cycle waits; a redstone signal stops it (the hammer inverts that).
 */
public class TileConversionUnit extends TileEntity implements MachinePartTile, ITickable, IHammerInteraction
{
	// Provisional values (the user's proposal, 31/10, and Arc Furnace level energy, 02/11): 1 yellowcake + 250 mB hydrogen
	// + 1500 mB hydrofluoric acid (about 6 parts) -> 1000 mB hexafluoride + 1000 mB effluent in 10 s, and 1 ash in 4 cycles
	public static final int H2_PER_CYCLE = 250, HF_PER_CYCLE = 1500, UF6_PER_CYCLE = 1000, EFFLUENT_PER_CYCLE = 1000, CYCLE_TICKS = 200, CYCLES_PER_ASH = 4;
	/** Cells 320 + reactor heating 128 + cold traps 64 (the flame tower burns the cells' hydrogen: no power). */
	public static final int ENERGY_PER_TICK = 512;
	public static final int H2_CAPACITY = 8000, HF_CAPACITY = 16000, UF6_CAPACITY = 8000, EFFLUENT_CAPACITY = 8000, ENERGY_CAPACITY = 32000, PUSH_PER_TICK = 200;
	public static final int STATE_IDLE = 0, STATE_RUNNING = 1, STATE_DISABLED = 2, STATE_NO_INPUT = 3, STATE_NO_POWER = 4, STATE_OUTPUT_FULL = 5;
	/** Yellowcake in, fluorination ash out, then each tank's container slots: in (full or empty container) and out. */
	public static final int SLOT_CAKE = 0, SLOT_ASH = 1, SLOT_H2_IN = 2, SLOT_H2_OUT = 3, SLOT_HF_IN = 4, SLOT_HF_OUT = 5, SLOT_UF6_IN = 6,
			SLOT_UF6_OUT = 7, SLOT_EFF_IN = 8, SLOT_EFF_OUT = 9, SLOTS = 10;

	int dx, dy, dz;
	EnumFacing facing = EnumFacing.NORTH;
	boolean mirrored;
	IBlockState originalState;
	NBTTagCompound originalNbt;

	// ---------- master only ----------
	final ItemStackHandler inventory = new ItemStackHandler(SLOTS)
	{
		@Override
		public boolean isItemValid(int slot, ItemStack stack)
		{
			switch(slot)
			{
				case SLOT_CAKE:
					return stack.getItem() == INContent.itemYellowcake;
				case SLOT_H2_IN:
					return TileHFKiln.emptyOr(stack, INContent.fluidHydrogen);
				case SLOT_HF_IN:
					return TileHFKiln.emptyOr(stack, INContent.fluidHydrofluoricAcid);
				case SLOT_UF6_IN:
					return TileHFKiln.emptyOr(stack, INContent.fluidUF6);
				case SLOT_EFF_IN:
					return TileHFKiln.emptyOr(stack, INContent.fluidFluorinatedEffluent);
				default:
					return false;
			}
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
		{
			return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
		}

		@Override
		protected void onContentsChanged(int slot)
		{
			markDirty();
		}
	};
	final FluidTank h2 = inputTank(H2_CAPACITY, INContent.fluidHydrogen), hf = inputTank(HF_CAPACITY, INContent.fluidHydrofluoricAcid);
	final FluidTank uf6 = outputTank(UF6_CAPACITY), effluent = outputTank(EFFLUENT_CAPACITY);
	final Energy energy = new Energy();
	/** Ticks into the current cycle; 0 = no cycle running. */
	int progress;
	/** Cycles finished since the last ash. */
	int ashCount;
	int state = STATE_IDLE;
	boolean redstoneInverted;

	/** Running, as last sent to the clients (server), or as received (client). */
	boolean sentRunning, clientRunning;

	/** What the item port shows: slot 0 takes yellowcake (insert only), slot 1 gives the ash (extract only). */
	final IItemHandler itemPort = new IItemHandler()
	{
		@Override
		public int getSlots()
		{
			return 2;
		}

		@Override
		public ItemStack getStackInSlot(int slot)
		{
			return inventory.getStackInSlot(slot);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
		{
			return slot == 0 ? inventory.insertItem(SLOT_CAKE, stack, simulate) : stack;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate)
		{
			return slot == 1 ? inventory.extractItem(SLOT_ASH, amount, simulate) : ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot)
		{
			return inventory.getSlotLimit(slot);
		}
	};

	FluidTank inputTank(int capacity, net.minecraftforge.fluids.Fluid accepted)
	{
		FluidTank t = new FluidTank(capacity)
		{
			@Override
			public boolean canFillFluidType(FluidStack f)
			{
				return f != null && f.getFluid().getName().equals(accepted.getName());
			}

			@Override
			protected void onContentsChanged()
			{
				markDirty();
			}
		};
		t.setCanDrain(false);
		return t;
	}

	FluidTank outputTank(int capacity)
	{
		FluidTank t = new FluidTank(capacity)
		{
			@Override
			protected void onContentsChanged()
			{
				markDirty();
			}
		};
		t.setCanFill(false);
		return t;
	}

	class Energy extends EnergyStorage
	{
		Energy()
		{
			super(ENERGY_CAPACITY, ENERGY_CAPACITY, 0);
		}

		@Override
		public int receiveEnergy(int maxReceive, boolean simulate)
		{
			int got = super.receiveEnergy(maxReceive, simulate);
			if(got > 0 && !simulate)
				markDirty();
			return got;
		}

		void set(int value)
		{
			energy = Math.max(0, Math.min(capacity, value));
		}

		boolean use(int amount)
		{
			if(energy < amount)
				return false;
			energy -= amount;
			return true;
		}
	}

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
		return ConversionUnitStructure.get(dx, dy, dz);
	}

	@Override
	public int getPort()
	{
		return ConversionUnitStructure.portAt(dx, dy, dz);
	}

	/** What a broken part drops: the block it replaced. */
	public ItemStack getOriginalStack()
	{
		return MultiblockConversionUnit.stackFor(getPartCode()).copy();
	}

	public boolean isMaster()
	{
		return dx == ConversionUnitStructure.MASTER_DX && dy == ConversionUnitStructure.MASTER_DY && dz == ConversionUnitStructure.MASTER_DZ;
	}

	@Override
	public String boundsName()
	{
		return "conversion_unit";
	}

	@Override
	public int[] fromMaster()
	{
		return new int[]{dx - ConversionUnitStructure.MASTER_DX, dy - ConversionUnitStructure.MASTER_DY, dz - ConversionUnitStructure.MASTER_DZ};
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
		int[] w = ConversionUnitStructure.rotate(dx, dz, MultiblockConversionUnit.turns(facing), mirrored);
		return pos.add(-w[0], -dy, -w[1]);
	}

	public BlockPos getMasterPos()
	{
		return MultiblockConversionUnit.toWorld(getTriggerPos(), facing, mirrored, ConversionUnitStructure.MASTER_DX, ConversionUnitStructure.MASTER_DY, ConversionUnitStructure.MASTER_DZ);
	}

	public TileConversionUnit getMaster()
	{
		if(isMaster())
			return this;
		if(world == null)
			return null;
		BlockPos m = getMasterPos();
		if(!world.isBlockLoaded(m))
			return null;
		TileEntity te = world.getTileEntity(m);
		return te instanceof TileConversionUnit && ((TileConversionUnit)te).isMaster() ? (TileConversionUnit)te : null;
	}

	/** World face a port works on, or null when it works on every side. */
	EnumFacing portWorldFace()
	{
		int[] f = ConversionUnitStructure.portFace(dx, dy, dz);
		if(f == null || f[0] == 0 && f[1] == 0)
			return null;
		int[] w = ConversionUnitStructure.rotate(f[0], f[1], MultiblockConversionUnit.turns(facing), mirrored);
		return EnumFacing.getFacingFromVector(w[0], 0, w[1]);
	}

	boolean portServes(Capability<?> cap, EnumFacing side)
	{
		int port = getPort();
		boolean right;
		if(cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
			right = port == ConversionUnitStructure.PORT_HYDROGEN || port == ConversionUnitStructure.PORT_HF
					|| port == ConversionUnitStructure.PORT_UF6 || port == ConversionUnitStructure.PORT_EFFLUENT;
		else if(cap == CapabilityEnergy.ENERGY)
			right = port == ConversionUnitStructure.PORT_ENERGY;
		else if(cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			right = port == ConversionUnitStructure.PORT_ITEM;
		else
			return false;
		if(!right)
			return false;
		EnumFacing face = portWorldFace();
		return side == null || face == null || side == face;
	}

	FluidTank portTank(int port)
	{
		switch(port)
		{
			case ConversionUnitStructure.PORT_HYDROGEN: return h2;
			case ConversionUnitStructure.PORT_HF: return hf;
			case ConversionUnitStructure.PORT_UF6: return uf6;
			default: return effluent;
		}
	}

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing side)
	{
		if(portServes(cap, side) && getMaster() != null)
			return true;
		return super.hasCapability(cap, side);
	}

	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing side)
	{
		if(portServes(cap, side))
		{
			TileConversionUnit m = getMaster();
			if(m != null)
			{
				if(cap == CapabilityEnergy.ENERGY)
					return CapabilityEnergy.ENERGY.cast(m.energy);
				if(cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
					return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(m.itemPort);
				return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(m.portTank(getPort()));
			}
		}
		return super.getCapability(cap, side);
	}

	@Override
	public void update()
	{
		if(world.isRemote)
			return;
		if(getPort() == ConversionUnitStructure.PORT_UF6 || getPort() == ConversionUnitStructure.PORT_EFFLUENT)
			push();
		if(isMaster())
		{
			handleContainers();
			process();
			sync();
		}
	}

	// ---------- the conversion ----------
	void process()
	{
		int before = state;
		if(world.isBlockPowered(getTriggerPos()) != redstoneInverted)
			state = STATE_DISABLED;
		else if(progress == 0 && !start())
		{
			// start() set the state
		}
		else if(!energy.use(ENERGY_PER_TICK))
			state = STATE_NO_POWER;
		else
		{
			state = STATE_RUNNING;
			if(++progress >= CYCLE_TICKS)
			{
				uf6.fillInternal(new FluidStack(INContent.fluidUF6, UF6_PER_CYCLE), true);
				effluent.fillInternal(new FluidStack(INContent.fluidFluorinatedEffluent, EFFLUENT_PER_CYCLE), true);
				if(++ashCount >= CYCLES_PER_ASH)
				{
					ashCount = 0;
					output(SLOT_ASH, new ItemStack(INContent.itemFluorinationAsh));
				}
				progress = 0;
			}
		}
		if(state != before)
			markDirty();
	}

	/** A cycle starts with its yellowcake, hydrogen and acid, and room for the products; the feed is used up as it starts. */
	boolean start()
	{
		if(inventory.getStackInSlot(SLOT_CAKE).isEmpty() || h2.getFluidAmount() < H2_PER_CYCLE || hf.getFluidAmount() < HF_PER_CYCLE)
		{
			state = STATE_NO_INPUT;
			return false;
		}
		if(uf6.getCapacity() - uf6.getFluidAmount() < UF6_PER_CYCLE || effluent.getCapacity() - effluent.getFluidAmount() < EFFLUENT_PER_CYCLE
				|| ashCount == CYCLES_PER_ASH - 1 && !canOutput(SLOT_ASH, new ItemStack(INContent.itemFluorinationAsh)))
		{
			state = STATE_OUTPUT_FULL;
			return false;
		}
		if(energy.getEnergyStored() < ENERGY_PER_TICK)
		{
			state = STATE_NO_POWER;
			return false;
		}
		inventory.extractItem(SLOT_CAKE, 1, false);
		h2.drainInternal(H2_PER_CYCLE, true);
		hf.drainInternal(HF_PER_CYCLE, true);
		return true;
	}

	/** Tells the clients when it starts or stops. */
	void sync()
	{
		boolean on = state == STATE_RUNNING;
		if(on != sentRunning)
		{
			sentRunning = on;
			IBlockState st = world.getBlockState(pos);
			world.notifyBlockUpdate(pos, st, st, 2);
		}
	}

	/** Whether it runs, as the client knows it. */
	public boolean running()
	{
		TileConversionUnit m = getMaster();
		return m != null && m.clientRunning;
	}

	/** Full containers empty into the input tanks; empty containers fill with the products. Each into its out slot. */
	void handleContainers()
	{
		empty(SLOT_H2_IN, SLOT_H2_OUT, h2);
		empty(SLOT_HF_IN, SLOT_HF_OUT, hf);
		fill(SLOT_UF6_IN, SLOT_UF6_OUT, uf6);
		fill(SLOT_EFF_IN, SLOT_EFF_OUT, effluent);
	}

	void empty(int inSlot, int outSlot, FluidTank tank)
	{
		ItemStack in = inventory.getStackInSlot(inSlot);
		if(in.isEmpty())
			return;
		FluidActionResult r = FluidUtil.tryEmptyContainer(ItemHandlerHelper.copyStackWithSize(in, 1), tank, Integer.MAX_VALUE, null, false);
		if(r.isSuccess() && canOutput(outSlot, r.getResult()))
		{
			r = FluidUtil.tryEmptyContainer(ItemHandlerHelper.copyStackWithSize(in, 1), tank, Integer.MAX_VALUE, null, true);
			output(outSlot, r.getResult());
			in.shrink(1);
			inventory.setStackInSlot(inSlot, in);
		}
	}

	void fill(int inSlot, int outSlot, FluidTank tank)
	{
		ItemStack in = inventory.getStackInSlot(inSlot);
		if(in.isEmpty() || tank.getFluidAmount() <= 0)
			return;
		FluidActionResult r = FluidUtil.tryFillContainer(ItemHandlerHelper.copyStackWithSize(in, 1), tank, Integer.MAX_VALUE, null, false);
		if(r.isSuccess() && canOutput(outSlot, r.getResult()))
		{
			r = FluidUtil.tryFillContainer(ItemHandlerHelper.copyStackWithSize(in, 1), tank, Integer.MAX_VALUE, null, true);
			output(outSlot, r.getResult());
			in.shrink(1);
			inventory.setStackInSlot(inSlot, in);
		}
	}

	void output(int slot, ItemStack stack)
	{
		if(stack.isEmpty())
			return;
		ItemStack out = inventory.getStackInSlot(slot);
		if(out.isEmpty())
			inventory.setStackInSlot(slot, stack.copy());
		else
		{
			out.grow(stack.getCount());
			inventory.setStackInSlot(slot, out);
		}
	}

	boolean canOutput(int slot, ItemStack stack)
	{
		ItemStack out = inventory.getStackInSlot(slot);
		return stack.isEmpty() || out.isEmpty() || ItemHandlerHelper.canItemStacksStack(out, stack) && out.getCount() + stack.getCount() <= out.getMaxStackSize();
	}

	/** The output ports push into what stands in front of them. */
	void push()
	{
		TileConversionUnit m = getMaster();
		EnumFacing face = portWorldFace();
		if(m == null || face == null)
			return;
		FluidTank tank = m.portTank(getPort());
		if(tank.getFluidAmount() <= 0)
			return;
		TileEntity out = world.getTileEntity(pos.offset(face));
		if(out == null || !out.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite()))
			return;
		IFluidHandler target = out.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite());
		if(target == null)
			return;
		FluidStack offer = tank.drainInternal(PUSH_PER_TICK, false);
		int taken = offer == null ? 0 : target.fill(offer, true);
		if(taken > 0)
			tank.drainInternal(taken, true);
	}

	public void toggleRedstone()
	{
		redstoneInverted = !redstoneInverted;
		markDirty();
	}

	/** Hammer on the redstone port: invert the redstone control, like IE's machines, and say so in the chat. */
	@Override
	public boolean hammerUseSide(EnumFacing side, EntityPlayer player, float hitX, float hitY, float hitZ)
	{
		if(getPort() != ConversionUnitStructure.PORT_REDSTONE)
			return false;
		TileConversionUnit m = getMaster();
		if(m == null)
			return false;
		if(!world.isRemote)
		{
			m.toggleRedstone();
			player.sendStatusMessage(new TextComponentTranslation(m.redstoneInverted ? "chat.immersivenuclear.redstone.inverted" : "chat.immersivenuclear.redstone.normal"), false);
		}
		return true;
	}

	/** The items in the slots drop; fluids and energy are lost. */
	public void dropContents()
	{
		for(int i = 0; i < SLOTS; i++)
		{
			ItemStack s = inventory.getStackInSlot(i);
			if(!s.isEmpty())
				Block.spawnAsEntity(world, pos, s.copy());
			inventory.setStackInSlot(i, ItemStack.EMPTY);
		}
	}

	// ---------- for the interface ----------
	public IItemHandler getInventory()
	{
		return inventory;
	}

	/** Hydrogen, acid, hexafluoride, effluent, energy, progress, state, redstone mode (all below 32767). */
	public int[] guiValues()
	{
		return new int[]{h2.getFluidAmount(), hf.getFluidAmount(), uf6.getFluidAmount(), effluent.getFluidAmount(),
				Math.min(32767, energy.getEnergyStored()), progress, state, redstoneInverted ? 1 : 0};
	}

	// ---------- saving and syncing ----------
	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		readPart(nbt);
		originalState = nbt.hasKey("originalState") ? Block.getStateById(nbt.getInteger("originalState")) : null;
		originalNbt = nbt.hasKey("originalNbt") ? nbt.getCompoundTag("originalNbt") : null;
		if(nbt.hasKey("machine"))
		{
			NBTTagCompound m = nbt.getCompoundTag("machine");
			NBTTagCompound inv = m.getCompoundTag("inventory");
			inv.setInteger("Size", SLOTS);
			inventory.deserializeNBT(inv);
			h2.readFromNBT(m.getCompoundTag("h2"));
			hf.readFromNBT(m.getCompoundTag("hf"));
			uf6.readFromNBT(m.getCompoundTag("uf6"));
			effluent.readFromNBT(m.getCompoundTag("effluent"));
			energy.set(m.getInteger("energy"));
			progress = m.getInteger("progress");
			ashCount = m.getInteger("ashCount");
			state = m.getInteger("state");
			redstoneInverted = m.getBoolean("redstoneInverted");
		}
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
		if(isMaster())
		{
			NBTTagCompound m = new NBTTagCompound();
			m.setTag("inventory", inventory.serializeNBT());
			m.setTag("h2", h2.writeToNBT(new NBTTagCompound()));
			m.setTag("hf", hf.writeToNBT(new NBTTagCompound()));
			m.setTag("uf6", uf6.writeToNBT(new NBTTagCompound()));
			m.setTag("effluent", effluent.writeToNBT(new NBTTagCompound()));
			m.setInteger("energy", energy.getEnergyStored());
			m.setInteger("progress", progress);
			m.setInteger("ashCount", ashCount);
			m.setInteger("state", state);
			m.setBoolean("redstoneInverted", redstoneInverted);
			nbt.setTag("machine", m);
		}
		return nbt;
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

	/** The client needs the part (which block of the structure, how it is turned) and, on the master, whether it runs. */
	@Override
	public NBTTagCompound getUpdateTag()
	{
		NBTTagCompound nbt = super.writeToNBT(new NBTTagCompound());
		writeClient(nbt);
		return nbt;
	}

	@Override
	public void handleUpdateTag(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		readClient(nbt);
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket()
	{
		NBTTagCompound nbt = new NBTTagCompound();
		writeClient(nbt);
		return new SPacketUpdateTileEntity(pos, 0, nbt);
	}

	void writeClient(NBTTagCompound nbt)
	{
		writePart(nbt);
		if(isMaster())
			nbt.setBoolean("running", state == STATE_RUNNING);
	}

	void readClient(NBTTagCompound nbt)
	{
		readPart(nbt);
		clientRunning = nbt.getBoolean("running");
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt)
	{
		int ox = dx, oy = dy, oz = dz;
		EnumFacing of = facing;
		boolean om = mirrored;
		readClient(pkt.getNbtCompound());
		if(ox != dx || oy != dy || oz != dz || of != facing || om != mirrored)
			world.markBlockRangeForRenderUpdate(pos, pos);
	}

	/** The master's box covers the whole machine (any turn): 10 blocks each way, 2 down, 10 up. */
	@Override
	public AxisAlignedBB getRenderBoundingBox()
	{
		return isMaster() ? new AxisAlignedBB(pos.add(-10, -2, -10), pos.add(11, 10, 11)) : super.getRenderBoundingBox();
	}
}
