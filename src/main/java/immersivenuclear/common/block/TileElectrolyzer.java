package immersivenuclear.common.block;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IHammerInteraction;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.ISoundTile;
import immersivenuclear.common.INContent;
import immersivenuclear.common.multiblock.ElectrolyzerStructure;
import immersivenuclear.common.multiblock.MultiblockElectrolyzer;
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
import net.minecraft.util.SoundCategory;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

/**
 * One part of a formed Electrolyzer: which template block it replaced and how the structure is turned. The master part
 * also holds the machine; the port parts hand their capability, on their own face only, to the master (see
 * {@link ElectrolyzerStructure#PORTS}).
 *
 * Water electrolysis in cycles of 5 s: a cycle starts with its water (or steam, which takes less power: part of the
 * energy to split it is already in it) and room for its hydrogen; while it runs it draws power (the rectifier feeding
 * the cell stacks): the water splits in the cells, the hydrogen and the oxygen rise to their separators and into their
 * tanks. The hydrogen's tank full stops the machine; the oxygen's tank full lifts its relief valve and the oxygen is
 * vented (a sound and a puff). Without power the cycle waits; a redstone signal stops it (the hammer inverts that).
 */
public class TileElectrolyzer extends TileEntity implements MachinePartTile, ITickable, IHammerInteraction, ISoundTile
{
	// Provisional values (the user's proposal, 31/10): 250 mB water -> 500 mB hydrogen + 250 mB oxygen in 5 s
	public static final int WATER_PER_CYCLE = 250, H2_PER_CYCLE = 500, O2_PER_CYCLE = 250, CYCLE_TICKS = 100;
	public static final int ENERGY_PER_TICK = 160, ENERGY_PER_TICK_STEAM = 110;
	public static final int WATER_CAPACITY = 8000, H2_CAPACITY = 8000, O2_CAPACITY = 8000, ENERGY_CAPACITY = 32000, PUSH_PER_TICK = 200;
	public static final int STATE_IDLE = 0, STATE_RUNNING = 1, STATE_DISABLED = 2, STATE_NO_INPUT = 3, STATE_NO_POWER = 4, STATE_OUTPUT_FULL = 5;
	/** Each tank has its container slots: in (full or empty container) and out. */
	public static final int SLOT_WATER_IN = 0, SLOT_WATER_OUT = 1, SLOT_H2_IN = 2, SLOT_H2_OUT = 3, SLOT_O2_IN = 4, SLOT_O2_OUT = 5, SLOTS = 6;
	/** How long (ticks) the oxygen's relief valve stays open after it vents: the interface shows it. */
	public static final int VENT_TICKS = 40;

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
				case SLOT_WATER_IN:
					FluidStack w = FluidUtil.getFluidContained(stack);
					return w != null && isFeed(w);
				case SLOT_H2_IN:
					return TileHFKiln.emptyOr(stack, INContent.fluidHydrogen);
				case SLOT_O2_IN:
					return TileHFKiln.emptyOr(stack, INContent.fluidOxygen);
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
	/** Water or steam (by name: either may be another mod's), one at a time. */
	final FluidTank water = new FluidTank(WATER_CAPACITY)
	{
		@Override
		public boolean canFillFluidType(FluidStack fluid)
		{
			return fluid != null && isFeed(fluid) && (getFluid() == null || getFluid().getFluid().getName().equals(fluid.getFluid().getName()));
		}

		@Override
		protected void onContentsChanged()
		{
			markDirty();
		}
	};
	final FluidTank h2 = tank(H2_CAPACITY), o2 = tank(O2_CAPACITY);
	final Energy energy = new Energy();
	/** Ticks into the current cycle; 0 = no cycle running. */
	int progress;
	int state = STATE_IDLE;
	boolean redstoneInverted;
	/** Ticks left of the oxygen relief valve's last opening. */
	int venting;
	/** Whether the cycle running now is on steam. */
	boolean onSteam;

	/** Where each sound plays (template offsets): the rectifier, the cell stacks (the master), the circulation pump, the
	 * water feed (the water port). */
	static final int[] RECTIFIER_PART = {1, 0, 5}, PUMP_PART = {4, 0, 3}, FEED_PART = {2, -1, 0};
	final LoopSound rectifier = new LoopSound(() -> INContent.soundElectrolyzerRectifierStart, () -> INContent.soundElectrolyzerRectifierLoop, () -> INContent.soundElectrolyzerRectifierStop, 20);
	final LoopSound cells = new LoopSound(() -> INContent.soundElectrolyzerCellsStart, () -> INContent.soundElectrolyzerCellsLoop, () -> INContent.soundElectrolyzerCellsStop, 20);
	final LoopSound pump = new LoopSound(() -> INContent.soundElectrolyzerPumpStart, () -> INContent.soundElectrolyzerPumpLoop, () -> INContent.soundElectrolyzerPumpStop, 24);
	final LoopSound feed = new LoopSound(() -> INContent.soundElectrolyzerFeedStart, () -> INContent.soundElectrolyzerFeedLoop, () -> INContent.soundElectrolyzerFeedStop, 16);

	/** Running, as last sent to the clients (server), or as received (client): lights the panel's lamps. */
	boolean sentRunning, clientRunning;

	public TileElectrolyzer()
	{
		water.setCanDrain(false);
		h2.setCanFill(false);
		o2.setCanFill(false);
	}

	FluidTank tank(int capacity)
	{
		return new FluidTank(capacity)
		{
			@Override
			protected void onContentsChanged()
			{
				markDirty();
			}
		};
	}

	public static boolean isFeed(FluidStack f)
	{
		String n = f.getFluid().getName();
		return "water".equals(n) || "steam".equals(n);
	}

	static boolean isSteam(FluidStack f)
	{
		return f != null && "steam".equals(f.getFluid().getName());
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
		return ElectrolyzerStructure.get(dx, dy, dz);
	}

	@Override
	public int getPort()
	{
		return ElectrolyzerStructure.portAt(dx, dy, dz);
	}

	/** What a broken part drops: the block it replaced. */
	public ItemStack getOriginalStack()
	{
		return MultiblockElectrolyzer.stackFor(getPartCode()).copy();
	}

	public boolean isMaster()
	{
		return dx == ElectrolyzerStructure.MASTER_DX && dy == ElectrolyzerStructure.MASTER_DY && dz == ElectrolyzerStructure.MASTER_DZ;
	}

	@Override
	public String boundsName()
	{
		return "electrolyzer";
	}

	@Override
	public int[] fromMaster()
	{
		return new int[]{dx - ElectrolyzerStructure.MASTER_DX, dy - ElectrolyzerStructure.MASTER_DY, dz - ElectrolyzerStructure.MASTER_DZ};
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
		int[] w = ElectrolyzerStructure.rotate(dx, dz, MultiblockElectrolyzer.turns(facing), mirrored);
		return pos.add(-w[0], -dy, -w[1]);
	}

	public BlockPos getMasterPos()
	{
		return MultiblockElectrolyzer.toWorld(getTriggerPos(), facing, mirrored, ElectrolyzerStructure.MASTER_DX, ElectrolyzerStructure.MASTER_DY, ElectrolyzerStructure.MASTER_DZ);
	}

	public TileElectrolyzer getMaster()
	{
		if(isMaster())
			return this;
		if(world == null)
			return null;
		BlockPos m = getMasterPos();
		if(!world.isBlockLoaded(m))
			return null;
		TileEntity te = world.getTileEntity(m);
		return te instanceof TileElectrolyzer && ((TileElectrolyzer)te).isMaster() ? (TileElectrolyzer)te : null;
	}

	/** World face a port works on, or null when it works on every side. */
	EnumFacing portWorldFace()
	{
		int[] f = ElectrolyzerStructure.portFace(dx, dy, dz);
		if(f == null || f[0] == 0 && f[1] == 0)
			return null;
		int[] w = ElectrolyzerStructure.rotate(f[0], f[1], MultiblockElectrolyzer.turns(facing), mirrored);
		return EnumFacing.getFacingFromVector(w[0], 0, w[1]);
	}

	boolean portServes(Capability<?> cap, EnumFacing side)
	{
		int port = getPort();
		boolean right;
		if(cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
			right = port == ElectrolyzerStructure.PORT_WATER || port == ElectrolyzerStructure.PORT_HYDROGEN || port == ElectrolyzerStructure.PORT_OXYGEN;
		else if(cap == CapabilityEnergy.ENERGY)
			right = port == ElectrolyzerStructure.PORT_ENERGY;
		else
			return false;
		if(!right)
			return false;
		EnumFacing face = portWorldFace();
		return side == null || face == null || side == face;
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
			TileElectrolyzer m = getMaster();
			if(m != null)
			{
				int port = getPort();
				if(cap == CapabilityEnergy.ENERGY)
					return CapabilityEnergy.ENERGY.cast(m.energy);
				return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(port == ElectrolyzerStructure.PORT_WATER ? m.water
						: port == ElectrolyzerStructure.PORT_HYDROGEN ? m.h2 : m.o2);
			}
		}
		return super.getCapability(cap, side);
	}

	@Override
	public void update()
	{
		if(world.isRemote)
		{
			boolean on = running();
			if(isMaster())
				cells.update(this, on);
			else if(isPart(RECTIFIER_PART))
				rectifier.update(this, on);
			else if(isPart(PUMP_PART))
				pump.update(this, on);
			else if(isPart(FEED_PART))
				feed.update(this, on);
			return;
		}
		if(getPort() == ElectrolyzerStructure.PORT_HYDROGEN || getPort() == ElectrolyzerStructure.PORT_OXYGEN)
			push();
		if(isMaster())
		{
			handleContainers();
			process();
			sync();
		}
	}

	// ---------- the electrolyzer ----------
	void process()
	{
		int before = state;
		if(venting > 0)
			venting--;
		if(world.isBlockPowered(getTriggerPos()) != redstoneInverted)
			state = STATE_DISABLED;
		else if(progress == 0 && !start())
		{
			// start() set the state
		}
		else if(!energy.use(onSteam ? ENERGY_PER_TICK_STEAM : ENERGY_PER_TICK))
			state = STATE_NO_POWER;
		else
		{
			state = STATE_RUNNING;
			if(++progress >= CYCLE_TICKS)
			{
				h2.fillInternal(new FluidStack(INContent.fluidHydrogen, H2_PER_CYCLE), true);
				int o2Room = o2.getCapacity() - o2.getFluidAmount();
				if(o2Room < O2_PER_CYCLE)
					vent();
				o2.fillInternal(new FluidStack(INContent.fluidOxygen, Math.min(O2_PER_CYCLE, o2Room)), true);
				progress = 0;
			}
		}
		if(state != before)
			markDirty();
	}

	/** The oxygen tank is full: its relief valve lifts and the oxygen goes out to the air. */
	void vent()
	{
		venting = VENT_TICKS;
		BlockPos at = MultiblockElectrolyzer.toWorld(getTriggerPos(), facing, mirrored, 4, 1, 0);
		world.playSound(null, at, INContent.soundElectrolyzerVent, SoundCategory.BLOCKS, 0.8F, 1F);
	}

	/** Tells the clients when it starts or stops, so the lamps light and the sounds play. */
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
		TileElectrolyzer m = getMaster();
		return m != null && m.clientRunning;
	}

	/** A cycle starts with its water (or steam) and room for its hydrogen; the water is used up as it starts. */
	boolean start()
	{
		if(water.getFluidAmount() < WATER_PER_CYCLE)
		{
			state = STATE_NO_INPUT;
			return false;
		}
		if(h2.getCapacity() - h2.getFluidAmount() < H2_PER_CYCLE)
		{
			state = STATE_OUTPUT_FULL;
			return false;
		}
		onSteam = isSteam(water.getFluid());
		if(energy.getEnergyStored() < (onSteam ? ENERGY_PER_TICK_STEAM : ENERGY_PER_TICK))
		{
			state = STATE_NO_POWER;
			return false;
		}
		water.drainInternal(WATER_PER_CYCLE, true);
		return true;
	}

	/** Water containers empty into the water tank; empty containers fill with hydrogen or oxygen. Each into its out slot. */
	void handleContainers()
	{
		empty(SLOT_WATER_IN, SLOT_WATER_OUT, water);
		fill(SLOT_H2_IN, SLOT_H2_OUT, h2);
		fill(SLOT_O2_IN, SLOT_O2_OUT, o2);
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

	/** The gas ports push into what stands in front of them. */
	void push()
	{
		TileElectrolyzer m = getMaster();
		EnumFacing face = portWorldFace();
		if(m == null || face == null)
			return;
		FluidTank tank = getPort() == ElectrolyzerStructure.PORT_HYDROGEN ? m.h2 : m.o2;
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
		if(getPort() != ElectrolyzerStructure.PORT_REDSTONE)
			return false;
		TileElectrolyzer m = getMaster();
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

	/** Water, water is steam, hydrogen, oxygen, energy, progress, state, redstone mode, venting (all below 32767). */
	public int[] guiValues()
	{
		return new int[]{water.getFluidAmount(), isSteam(water.getFluid()) ? 1 : 0, h2.getFluidAmount(), o2.getFluidAmount(),
				Math.min(32767, energy.getEnergyStored()), progress, state, redstoneInverted ? 1 : 0, venting};
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
			water.readFromNBT(m.getCompoundTag("water"));
			h2.readFromNBT(m.getCompoundTag("h2"));
			o2.readFromNBT(m.getCompoundTag("o2"));
			energy.set(m.getInteger("energy"));
			progress = m.getInteger("progress");
			state = m.getInteger("state");
			redstoneInverted = m.getBoolean("redstoneInverted");
			onSteam = m.getBoolean("onSteam");
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
			m.setTag("water", water.writeToNBT(new NBTTagCompound()));
			m.setTag("h2", h2.writeToNBT(new NBTTagCompound()));
			m.setTag("o2", o2.writeToNBT(new NBTTagCompound()));
			m.setInteger("energy", energy.getEnergyStored());
			m.setInteger("progress", progress);
			m.setInteger("state", state);
			m.setBoolean("redstoneInverted", redstoneInverted);
			m.setBoolean("onSteam", onSteam);
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

	boolean isPart(int[] part)
	{
		return dx == part[0] && dy == part[1] && dz == part[2];
	}

	/** IE's looping tile sound asks this every 2 s and ends the loop when it is false. */
	@Override
	public boolean shoudlPlaySound(String sound)
	{
		if(isMaster())
			return cells.shouldPlay(this, sound);
		if(isPart(RECTIFIER_PART))
			return rectifier.shouldPlay(this, sound);
		if(isPart(PUMP_PART))
			return pump.shouldPlay(this, sound);
		return isPart(FEED_PART) && feed.shouldPlay(this, sound);
	}

	void stopSound()
	{
		if(world == null || !world.isRemote)
			return;
		if(isMaster())
			cells.stop(this);
		else if(isPart(RECTIFIER_PART))
			rectifier.stop(this);
		else if(isPart(PUMP_PART))
			pump.stop(this);
		else if(isPart(FEED_PART))
			feed.stop(this);
	}

	/** Stop the loops when the structure comes apart or the chunk unloads. */
	@Override
	public void invalidate()
	{
		super.invalidate();
		stopSound();
	}

	@Override
	public void onChunkUnload()
	{
		super.onChunkUnload();
		stopSound();
	}

	/** The master's box covers the whole machine (any turn): 4 blocks each way, 1 down, 4 up. */
	@Override
	public AxisAlignedBB getRenderBoundingBox()
	{
		return isMaster() ? new AxisAlignedBB(pos.add(-4, -1, -4), pos.add(5, 5, 5)) : super.getRenderBoundingBox();
	}
}
