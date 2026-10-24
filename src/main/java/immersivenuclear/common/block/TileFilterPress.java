package immersivenuclear.common.block;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IHammerInteraction;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.ISoundTile;
import immersivenuclear.common.INContent;
import immersivenuclear.common.item.ItemFilter;
import immersivenuclear.common.multiblock.FilterPressStructure;
import immersivenuclear.common.multiblock.MultiblockFilterPress;
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
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidRegistry;
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
 * One part of a formed Filtration Unit: which template block it replaced and how the structure is turned.
 * The master part also holds the machine; the port parts hand their capability, on their own face only, to the
 * master's storage (see {@link FilterPressStructure#PORTS}).
 *
 * The press works in batches, in four steps:
 * 1 the pump moves a batch of slurry from the storage tank into the pressure tank and pressurises it,
 * 2 at the same time the two actuators close the plates, 3 with the press closed and the pressure up, the
 * pressure is released and the slurry goes through the filters at once: the pressure tank empties into the plates
 * over the first RELEASE_TICKS while the crude solution already comes out little by little, 4 the actuators open
 * and the solids (wet mineral residue) fall on the conveyor.
 * The pump works on its own: as soon as a batch is released the pressure tank is free and the next batch is
 * pressurised while this one filters, so only the first batch waits for the pump.
 * The 28 filter slots must all hold a usable filter; every batch wears each filter by one.
 * Two feeds, one batch at a time: uranium slurry (crude solution + wet mineral residue) or neutralized sludge from
 * IE's Mixer (plain water + neutralization cake). The storage tank holds one of them at a time; a batch only starts
 * when the solution tank and the residue slot can take its products.
 */
public class TileFilterPress extends TileEntity implements ITickable, IHammerInteraction, ISoundTile
{
	// Gameplay values (provisional), the step lengths follow the length of each step's sound
	public static final int PRESSURIZE_TICKS = 100, CLOSE_TICKS = 60, RELEASE_TICKS = 40, FILTER_TICKS = 200, OPEN_TICKS = 120;
	/** During the opening: the plates are apart after this many ticks, the solids fall from then on. */
	public static final int OPEN_MOVE_TICKS = 60;
	public static final int BATCH = 1000, SOLUTION_PER_BATCH = 800, RESIDUE_PER_BATCH = 1;
	public static final int TANK_CAPACITY = 12000, ENERGY_CAPACITY = 32000;
	public static final int PUMP_ENERGY = 32, ACTUATOR_ENERGY = 16, FILTER_ENERGY = 8;
	public static final int FILTERS = 28;
	public static final int SOLUTION_PUSH_PER_TICK = 100;

	/**
	 * Press steps; the pump (step 1) runs on its own, see {@link #pumpTicks}. PHASE_RELEASE (3) is no longer a step of
	 * its own (the release is the start of the filtering): only presses saved during it still read it, as filtering.
	 */
	public static final int PHASE_OPEN = 0, PHASE_CLOSING = 1, PHASE_CLOSED = 2, PHASE_RELEASE = 3, PHASE_FILTERING = 4, PHASE_OPENING = 5;
	public static final int STATE_IDLE = 0, STATE_RUNNING = 1, STATE_DISABLED = 2, STATE_NO_INPUT = 3, STATE_NO_POWER = 4, STATE_OUTPUT_FULL = 5, STATE_NO_FILTERS = 6;

	/** Machine slots: 28 filters, the residue output, then the bucket slots (slurry in / out, solution in / out). */
	public static final int SLOT_RESIDUE = FILTERS, SLOT_SLURRY_IN = FILTERS + 1, SLOT_SLURRY_OUT = FILTERS + 2, SLOT_SOLUTION_IN = FILTERS + 3, SLOT_SOLUTION_OUT = FILTERS + 4, SLOTS = FILTERS + 5;

	int dx, dy, dz;
	EnumFacing facing = EnumFacing.NORTH;
	boolean mirrored;
	/** The block this part replaced (state + tile NBT), put back on disassembly. */
	IBlockState originalState;
	NBTTagCompound originalNbt;

	// ---------- master only ----------
	final ItemStackHandler inventory = new ItemStackHandler(SLOTS)
	{
		@Override
		public boolean isItemValid(int slot, ItemStack stack)
		{
			if(slot < FILTERS)
				return ItemFilter.isFilter(stack);
			switch(slot)
			{
				case SLOT_SLURRY_IN:
					FluidStack in = FluidUtil.getFluidContained(stack);
					return in != null && isFeed(in);
				case SLOT_SOLUTION_IN:
					FluidStack held = FluidUtil.getFluidContained(stack);
					return FluidUtil.getFluidHandler(stack) != null && (held == null || held.getFluid() == INContent.fluidCrudeSolution
							|| held.getFluid() == FluidRegistry.WATER);
				default: return false; // outputs only take what the machine puts there
			}
		}

		@Override
		public int getSlotLimit(int slot)
		{
			return slot < FILTERS ? 1 : 64;
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
	/** Storage tank: slurry coming in. */
	final FluidTank storage = new FluidTank(TANK_CAPACITY)
	{
		@Override
		public boolean canFillFluidType(FluidStack fluid)
		{
			return isFeed(fluid);
		}

		@Override
		protected void onContentsChanged()
		{
			markDirty();
		}
	};
	/** Pressure tank: one batch, filled and pressurised by the pump. */
	final FluidTank pressureTank = new FluidTank(BATCH);
	/** Crude solution coming out of the plates. */
	final FluidTank solution = new FluidTank(TANK_CAPACITY)
	{
		@Override
		protected void onContentsChanged()
		{
			markDirty();
		}
	};
	final Energy energy = new Energy();
	/** Pump progress on the batch in the pressure tank, 0..PRESSURIZE_TICKS (full = pressurised). */
	int pumpTicks;
	int phase = PHASE_OPEN, phaseTicks;
	/** Slurry between the plates (released, not yet filtered) and the solution already given for this batch. */
	int inPlates, solutionGiven;
	/** The batch in the plates is neutralized sludge (its products are water and cake), set as it is released. */
	boolean sludgeBatch;
	int state = STATE_IDLE;
	/** IE convention: a redstone signal stops the machine; hammering the redstone port inverts that. */
	boolean redstoneInverted;
	/** What the client last got (phase, pump); sent when it changes. */
	int sentPhase = -1, sentPump = -1, sentFilters = -1;
	int[] sentWear = new int[0];
	boolean sentPumping, sentRunning;

	// ---------- client only ----------
	public int clientPhase = PHASE_OPEN, clientPhaseTicks, clientPump;
	/** Which filter slots hold a filter (bit i = slot i): the model shows only those plates. */
	public int clientFilters;
	/** Wear of each filter, 0 new .. ItemFilter.LIFE spent: the cloth of its plate darkens with it. */
	public int[] clientWear = new int[FILTERS];
	public boolean clientPumping;
	/** False while the machine is stopped (redstone, power): the animation and the sounds hold where they are. */
	public boolean clientRunning;
	int playedPhase = -1;
	/** The mod's pump sound, played by the pump part while the slurry moves (pressurising, then into and through the plates). */
	final PumpSound pump = new PumpSound();
	static final int PUMP_DX = 1, PUMP_DY = 0, PUMP_DZ = -8;

	{
		storage.setCanDrain(false);
		solution.setCanFill(false);
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

	/** The filter input port: puts each usable filter in a free filter slot; gives nothing back. */
	class FilterPort implements IItemHandler
	{
		@Override
		public int getSlots()
		{
			return FILTERS;
		}

		@Override
		public ItemStack getStackInSlot(int i)
		{
			return inventory.getStackInSlot(i);
		}

		@Override
		public ItemStack insertItem(int i, ItemStack stack, boolean simulate)
		{
			if(!ItemFilter.isUsable(stack))
				return stack;
			for(int s = 0; s < FILTERS; s++)
				if(inventory.getStackInSlot(s).isEmpty())
					return inventory.insertItem(s, stack, simulate);
			return stack;
		}

		@Override
		public ItemStack extractItem(int i, int amount, boolean simulate)
		{
			return ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int i)
		{
			return 1;
		}

		@Override
		public boolean isItemValid(int i, ItemStack stack)
		{
			return ItemFilter.isUsable(stack);
		}
	}

	/** The spent filter output port: gives only spent filters out (filters with life left stay); takes nothing. */
	class SpentFilterPort implements IItemHandler
	{
		@Override
		public int getSlots()
		{
			return FILTERS;
		}

		@Override
		public ItemStack getStackInSlot(int i)
		{
			ItemStack s = inventory.getStackInSlot(i);
			return isSpent(s) ? s : ItemStack.EMPTY;
		}

		@Override
		public ItemStack insertItem(int i, ItemStack stack, boolean simulate)
		{
			return stack;
		}

		@Override
		public ItemStack extractItem(int i, int amount, boolean simulate)
		{
			return isSpent(inventory.getStackInSlot(i)) ? inventory.extractItem(i, amount, simulate) : ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int i)
		{
			return 1;
		}

		@Override
		public boolean isItemValid(int i, ItemStack stack)
		{
			return false;
		}
	}

	static boolean isSpent(ItemStack s)
	{
		return ItemFilter.isFilter(s) && !ItemFilter.isUsable(s);
	}

	/** The residue port: only gives the residue out. */
	class ResiduePort implements IItemHandler
	{
		@Override
		public int getSlots()
		{
			return 1;
		}

		@Override
		public ItemStack getStackInSlot(int i)
		{
			return inventory.getStackInSlot(SLOT_RESIDUE);
		}

		@Override
		public ItemStack insertItem(int i, ItemStack stack, boolean simulate)
		{
			return stack;
		}

		@Override
		public ItemStack extractItem(int i, int amount, boolean simulate)
		{
			return inventory.extractItem(SLOT_RESIDUE, amount, simulate);
		}

		@Override
		public int getSlotLimit(int i)
		{
			return 64;
		}

		@Override
		public boolean isItemValid(int i, ItemStack stack)
		{
			return false;
		}
	}

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
		return FilterPressStructure.get(dx, dy, dz);
	}

	public int getPort()
	{
		return FilterPressStructure.portAt(dx, dy, dz);
	}

	public ItemStack getOriginalStack()
	{
		return MultiblockFilterPress.stackFor(getPartCode());
	}

	public boolean isMaster()
	{
		return dx == FilterPressStructure.MASTER_DX && dy == FilterPressStructure.MASTER_DY && dz == FilterPressStructure.MASTER_DZ;
	}

	public EnumFacing getFacing()
	{
		return facing;
	}

	public boolean isMirrored()
	{
		return mirrored;
	}

	/** World position of the trigger block this part was formed from (also the redstone port). */
	public BlockPos getTriggerPos()
	{
		int[] w = FilterPressStructure.rotate(dx, dz, MultiblockFilterPress.turns(facing), mirrored);
		return pos.add(-w[0], -dy, -w[1]);
	}

	public BlockPos getMasterPos()
	{
		return MultiblockFilterPress.toWorld(getTriggerPos(), facing, mirrored,
				FilterPressStructure.MASTER_DX, FilterPressStructure.MASTER_DY, FilterPressStructure.MASTER_DZ);
	}

	public TileFilterPress getMaster()
	{
		if(isMaster())
			return this;
		if(world == null)
			return null;
		BlockPos m = getMasterPos();
		if(!world.isBlockLoaded(m))
			return null;
		TileEntity te = world.getTileEntity(m);
		return te instanceof TileFilterPress && ((TileFilterPress)te).isMaster() ? (TileFilterPress)te : null;
	}

	/** World face a port works on, or null when it works on every side. */
	EnumFacing portWorldFace()
	{
		int[] f = FilterPressStructure.portFace(dx, dy, dz);
		if(f == null || f[0] == 0 && f[1] == 0)
			return null;
		int[] w = FilterPressStructure.rotate(f[0], f[1], MultiblockFilterPress.turns(facing), mirrored);
		return EnumFacing.getFacingFromVector(w[0], 0, w[1]);
	}

	boolean portServes(Capability<?> cap, EnumFacing side)
	{
		int port = getPort();
		boolean right;
		if(cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			right = port == FilterPressStructure.PORT_FILTER || port == FilterPressStructure.PORT_FILTER_OUT || port == FilterPressStructure.PORT_RESIDUE;
		else if(cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
			right = port == FilterPressStructure.PORT_SLURRY || port == FilterPressStructure.PORT_SOLUTION;
		else if(cap == CapabilityEnergy.ENERGY)
			right = port == FilterPressStructure.PORT_ENERGY;
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
			TileFilterPress m = getMaster();
			if(m != null)
			{
				int port = getPort();
				if(port == FilterPressStructure.PORT_FILTER)
					return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(m.new FilterPort());
				if(port == FilterPressStructure.PORT_FILTER_OUT)
					return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(m.new SpentFilterPort());
				if(port == FilterPressStructure.PORT_RESIDUE)
					return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(m.new ResiduePort());
				if(port == FilterPressStructure.PORT_SLURRY)
					return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(m.storage);
				if(port == FilterPressStructure.PORT_SOLUTION)
					return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(m.solution);
				if(port == FilterPressStructure.PORT_ENERGY)
					return CapabilityEnergy.ENERGY.cast(m.energy);
			}
		}
		return super.getCapability(cap, side);
	}

	@Override
	public void update()
	{
		if(world.isRemote)
		{
			if(isMaster())
				updateClient();
			else if(isPumpPart())
			{
				TileFilterPress m = getMaster();
				pump.update(this, m != null && m.clientRunning && (m.clientPumping || m.clientPhase == PHASE_FILTERING));
			}
			return;
		}
		int port = getPort();
		if(port == FilterPressStructure.PORT_SOLUTION)
			pushSolution();
		else if(port == FilterPressStructure.PORT_RESIDUE && world.getTotalWorldTime() % 8 == 0)
			pushResidue();
		else if(port == FilterPressStructure.PORT_FILTER_OUT && world.getTotalWorldTime() % 8 == 4)
			pushSpentFilter();
		if(isMaster())
		{
			handleContainers();
			process();
			sync();
		}
	}

	// ---------- the batch ----------
	void process()
	{
		int before = state;
		boolean powered = world.isBlockPowered(getTriggerPos());
		if(powered != redstoneInverted)
		{
			state = STATE_DISABLED;
			if(state != before)
				markDirty();
			return;
		}
		boolean noPower = false;

		// 1 the pump: load a batch into the free pressure tank and pressurise it
		if(pressureTank.getFluidAmount() == 0 && storage.getFluidAmount() >= BATCH)
		{
			pressureTank.fillInternal(storage.drainInternal(BATCH, true), true);
			pumpTicks = 0;
		}
		if(pressureTank.getFluidAmount() > 0 && pumpTicks < PRESSURIZE_TICKS)
		{
			if(energy.use(PUMP_ENERGY))
				pumpTicks++;
			else
				noPower = true;
		}
		boolean batchReady = pressureTank.getFluidAmount() >= BATCH;

		// 2..5 the press
		switch(phase)
		{
			case PHASE_OPEN:
				if(!filtersReady())
					state = STATE_NO_FILTERS;
				else if(!batchReady)
					state = STATE_NO_INPUT;
				else if(!roomForBatch())
					state = STATE_OUTPUT_FULL;
				else
					next(PHASE_CLOSING);
				break;
			case PHASE_CLOSING:
				if(energy.use(ACTUATOR_ENERGY))
				{
					if(++phaseTicks >= CLOSE_TICKS)
						next(PHASE_CLOSED);
				}
				else
					noPower = true;
				break;
			case PHASE_CLOSED:
				// Waits for the pump only on the first batch; later the next batch is ready long before
				if(pumpTicks >= PRESSURIZE_TICKS)
				{
					inPlates = 0;
					solutionGiven = 0;
					sludgeBatch = isSludge(pressureTank.getFluid());
					next(PHASE_FILTERING);
				}
				break;
			case PHASE_RELEASE:
			case PHASE_FILTERING:
				phase = PHASE_FILTERING;
				// Released: the pressurised slurry flows into the plates over the first ticks (tank and gauge go down
				// together) and goes through the cloths from the first tick
				if(phaseTicks < RELEASE_TICKS && pressureTank.getFluidAmount() > 0)
				{
					FluidStack flow = pressureTank.drainInternal(phaseTicks + 1 >= RELEASE_TICKS ? BATCH : BATCH / RELEASE_TICKS, true);
					if(flow != null)
						inPlates += flow.amount;
					if(pressureTank.getFluidAmount() == 0)
						pumpTicks = 0;
				}
				if(energy.use(FILTER_ENERGY))
				{
					phaseTicks++;
					// The solution comes out little by little while the slurry passes through the cloths
					int due = SOLUTION_PER_BATCH * phaseTicks / FILTER_TICKS - solutionGiven;
					if(due > 0)
						solutionGiven += solution.fillInternal(new FluidStack(sludgeBatch ? FluidRegistry.WATER : INContent.fluidCrudeSolution, due), true);
					if(phaseTicks >= FILTER_TICKS)
					{
						inPlates = 0;
						next(PHASE_OPENING);
					}
				}
				else
					noPower = true;
				break;
			case PHASE_OPENING:
				if(phaseTicks < OPEN_MOVE_TICKS && !energy.use(ACTUATOR_ENERGY))
				{
					noPower = true;
					break;
				}
				if(++phaseTicks == OPEN_MOVE_TICKS)
					dropResidue();
				if(phaseTicks >= OPEN_TICKS)
					next(PHASE_OPEN);
				break;
		}
		// Open and waiting, the step above already named the reason
		if(noPower)
			state = STATE_NO_POWER;
		else if(phase != PHASE_OPEN)
			state = STATE_RUNNING;
		if(state != before)
			markDirty();
	}

	void next(int phase)
	{
		this.phase = phase;
		phaseTicks = 0;
		markDirty();
	}

	/** All 28 filter slots hold a filter that can take one more batch. */
	boolean filtersReady()
	{
		for(int i = 0; i < FILTERS; i++)
			if(!ItemFilter.isUsable(inventory.getStackInSlot(i)))
				return false;
		return true;
	}

	/**
	 * Room for the liquid and the solids of the batch in the pressure tank (crude solution and residue for slurry, water
	 * and cake for sludge), so a batch is never lost half way; the solution tank and the residue slot take one kind at a time.
	 */
	boolean roomForBatch()
	{
		boolean sludge = isSludge(pressureTank.getFluid());
		FluidStack held = solution.getFluid();
		boolean sameLiquid = held == null || held.getFluid() == (sludge ? FluidRegistry.WATER : INContent.fluidCrudeSolution);
		return sameLiquid && solution.getCapacity() - solution.getFluidAmount() >= SOLUTION_PER_BATCH
				&& canOutput(SLOT_RESIDUE, new ItemStack(sludge ? INContent.itemCake : INContent.itemResidue, RESIDUE_PER_BATCH));
	}

	/** What the press takes: uranium slurry or neutralized sludge. */
	public static boolean isFeed(FluidStack f)
	{
		return f != null && (f.getFluid() == INContent.fluidSlurry || isSludge(f));
	}

	public static boolean isSludge(FluidStack f)
	{
		return f != null && f.getFluid() == INContent.fluidSludge;
	}

	/** The plates are apart: the cake falls on the conveyor (into the residue slot) and every filter wears. */
	void dropResidue()
	{
		output(SLOT_RESIDUE, new ItemStack(sludgeBatch ? INContent.itemCake : INContent.itemResidue, RESIDUE_PER_BATCH));
		for(int i = 0; i < FILTERS; i++)
		{
			ItemStack f = inventory.getStackInSlot(i);
			if(ItemFilter.isFilter(f))
			{
				f = f.copy();
				ItemFilter.wear(f);
				inventory.setStackInSlot(i, f);
			}
		}
	}

	// ---------- containers and ports ----------
	/** Bucket slots, like IE's: slurry containers are emptied into the storage tank, empty ones are filled with solution. */
	void handleContainers()
	{
		ItemStack in = inventory.getStackInSlot(SLOT_SLURRY_IN);
		if(!in.isEmpty())
		{
			FluidActionResult r = FluidUtil.tryEmptyContainer(ItemHandlerHelper.copyStackWithSize(in, 1), storage, Integer.MAX_VALUE, null, false);
			if(r.isSuccess() && canOutput(SLOT_SLURRY_OUT, r.getResult()))
			{
				r = FluidUtil.tryEmptyContainer(ItemHandlerHelper.copyStackWithSize(in, 1), storage, Integer.MAX_VALUE, null, true);
				output(SLOT_SLURRY_OUT, r.getResult());
				in.shrink(1);
				inventory.setStackInSlot(SLOT_SLURRY_IN, in);
			}
		}
		in = inventory.getStackInSlot(SLOT_SOLUTION_IN);
		if(!in.isEmpty() && solution.getFluidAmount() > 0)
		{
			FluidActionResult r = FluidUtil.tryFillContainer(ItemHandlerHelper.copyStackWithSize(in, 1), solution, Integer.MAX_VALUE, null, false);
			if(r.isSuccess() && canOutput(SLOT_SOLUTION_OUT, r.getResult()))
			{
				r = FluidUtil.tryFillContainer(ItemHandlerHelper.copyStackWithSize(in, 1), solution, Integer.MAX_VALUE, null, true);
				output(SLOT_SOLUTION_OUT, r.getResult());
				in.shrink(1);
				inventory.setStackInSlot(SLOT_SOLUTION_IN, in);
			}
		}
	}

	boolean canOutput(int slot, ItemStack stack)
	{
		ItemStack o = inventory.getStackInSlot(slot);
		return stack.isEmpty() || o.isEmpty() || ItemHandlerHelper.canItemStacksStack(o, stack) && o.getCount() + stack.getCount() <= o.getMaxStackSize();
	}

	void output(int slot, ItemStack stack)
	{
		if(stack.isEmpty())
			return;
		ItemStack o = inventory.getStackInSlot(slot);
		if(o.isEmpty())
			inventory.setStackInSlot(slot, stack.copy());
		else
		{
			o = o.copy();
			o.grow(stack.getCount());
			inventory.setStackInSlot(slot, o);
		}
	}

	/** The solution port empties the solution tank into whatever accepts fluid on its face. */
	void pushSolution()
	{
		TileFilterPress m = getMaster();
		EnumFacing face = portWorldFace();
		if(m == null || face == null || m.solution.getFluidAmount() <= 0)
			return;
		TileEntity out = world.getTileEntity(pos.offset(face));
		if(out == null || !out.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite()))
			return;
		IFluidHandler target = out.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite());
		if(target == null)
			return;
		FluidStack offer = m.solution.drainInternal(SOLUTION_PUSH_PER_TICK, false);
		int taken = offer == null ? 0 : target.fill(offer, true);
		if(taken > 0)
			m.solution.drainInternal(taken, true);
	}

	/** The residue port hands the residue to whatever takes items on its face (conveyor, hopper, chest). */
	void pushResidue()
	{
		TileFilterPress m = getMaster();
		EnumFacing face = portWorldFace();
		if(m == null || face == null || m.inventory.getStackInSlot(SLOT_RESIDUE).isEmpty())
			return;
		TileEntity out = world.getTileEntity(pos.offset(face));
		if(out == null || !out.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face.getOpposite()))
			return;
		IItemHandler target = out.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face.getOpposite());
		if(target == null)
			return;
		ItemStack one = m.inventory.extractItem(SLOT_RESIDUE, 1, true);
		if(ItemHandlerHelper.insertItem(target, one, true).isEmpty())
			ItemHandlerHelper.insertItem(target, m.inventory.extractItem(SLOT_RESIDUE, 1, false), false);
	}

	/** The spent filter port hands one spent filter at a time to whatever takes items on its face. */
	void pushSpentFilter()
	{
		TileFilterPress m = getMaster();
		EnumFacing face = portWorldFace();
		if(m == null || face == null)
			return;
		TileEntity out = world.getTileEntity(pos.offset(face));
		if(out == null || !out.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face.getOpposite()))
			return;
		IItemHandler target = out.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face.getOpposite());
		if(target == null)
			return;
		for(int i = 0; i < FILTERS; i++)
			if(isSpent(m.inventory.getStackInSlot(i)))
			{
				if(ItemHandlerHelper.insertItem(target, m.inventory.getStackInSlot(i).copy(), true).isEmpty())
					ItemHandlerHelper.insertItem(target, m.inventory.extractItem(i, 1, false), false);
				return;
			}
	}

	/** Hammer on the redstone port: invert the redstone control, like IE's machines. */
	@Override
	public boolean hammerUseSide(EnumFacing side, EntityPlayer player, float hitX, float hitY, float hitZ)
	{
		if(getPort() != FilterPressStructure.PORT_REDSTONE)
			return false;
		TileFilterPress m = getMaster();
		if(m == null)
			return false;
		if(!world.isRemote)
		{
			m.toggleRedstone();
			player.sendStatusMessage(new TextComponentTranslation(m.redstoneInverted ? "chat.immersivenuclear.redstone.inverted" : "chat.immersivenuclear.redstone.normal"), true);
		}
		return true;
	}

	// ---------- client: animation and sound ----------
	/** Sent when the step or the pump changes; the client counts the ticks of a step itself. */
	void sync()
	{
		boolean pumping = pumpTicks > 0 && pumpTicks < PRESSURIZE_TICKS;
		int pump = pumpTicks * 20 / PRESSURIZE_TICKS;
		int filters = filterMask();
		int[] wear = filterWear();
		boolean running = state == STATE_RUNNING;
		if(phase != sentPhase || pumping != sentPumping || pump != sentPump || filters != sentFilters || running != sentRunning || !java.util.Arrays.equals(wear, sentWear)
				|| world.getTotalWorldTime() % 40 == 0 && phase != PHASE_OPEN)
		{
			sentFilters = filters;
			sentWear = wear;
			sentRunning = running;
			sentPhase = phase;
			sentPumping = pumping;
			sentPump = pump;
			IBlockState st = world.getBlockState(pos);
			world.notifyBlockUpdate(pos, st, st, 2);
		}
	}

	/** Bit i set when filter slot i holds a filter (new, used or spent). */
	int filterMask()
	{
		int mask = 0;
		for(int i = 0; i < FILTERS; i++)
			if(ItemFilter.isFilter(inventory.getStackInSlot(i)))
				mask |= 1 << i;
		return mask;
	}

	int[] filterWear()
	{
		int[] wear = new int[FILTERS];
		for(int i = 0; i < FILTERS; i++)
			wear[i] = inventory.getStackInSlot(i).getItemDamage();
		return wear;
	}

	/** How closed the plates are (0 open, as modelled; 1 pressed), from the client's step and its tick count. */
	public float closing(float partialTicks)
	{
		return closing(clientPhase, clientPhaseTicks + partialTicks);
	}

	/** How closed the plates are at tick t of a step (also for the interface). */
	public static float closing(int phase, float t)
	{
		switch(phase)
		{
			case PHASE_CLOSING: return smooth(Math.min(1, t / CLOSE_TICKS));
			case PHASE_CLOSED:
			case PHASE_FILTERING: return 1;
			case PHASE_OPENING: return 1 - smooth(Math.min(1, t / OPEN_MOVE_TICKS));
			default: return 0;
		}
	}

	static float smooth(float x)
	{
		return x * x * (3 - 2 * x);
	}

	/** Ticks since the solids started falling (during the opening), or -1. */
	public float fallTicks(float partialTicks)
	{
		return clientPhase == PHASE_OPENING && clientPhaseTicks >= OPEN_MOVE_TICKS ? clientPhaseTicks - OPEN_MOVE_TICKS + partialTicks : -1;
	}

	void updateClient()
	{
		int max = clientPhase == PHASE_CLOSING ? CLOSE_TICKS : clientPhase == PHASE_FILTERING ? FILTER_TICKS : clientPhase == PHASE_OPENING ? OPEN_TICKS : 0;
		if(clientRunning && clientPhaseTicks < max)
			clientPhaseTicks++;
		// One sound per step; the filtering one loops (IE's tile sound) and starts with the release. The pumping is
		// the mod's pump sound, on the pump part.
		if(clientPhase != playedPhase)
		{
			if(clientPhase == PHASE_CLOSING)
				playOnce(INContent.soundFpClose);
			else if(clientPhase == PHASE_FILTERING)
				playOnce(INContent.soundFpRelease);
			else if(clientPhase == PHASE_OPENING)
				playOnce(INContent.soundFpOpen);
			playedPhase = clientPhase;
		}
		ImmersiveEngineering.proxy.handleTileSound(INContent.soundFpFilterLoop, this, filterLoopOn(), 1, 1);
	}

	/** IE's looping tile sound asks this every 2 s and ends the loop when it is false. */
	@Override
	public boolean shoudlPlaySound(String sound)
	{
		if(isPumpPart())
			return pump.shouldPlay(this, sound);
		return isMaster() && filterLoopOn() && INContent.soundFpFilterLoop.getSoundName().toString().equals(sound);
	}

	boolean isPumpPart()
	{
		return dx == PUMP_DX && dy == PUMP_DY && dz == PUMP_DZ;
	}

	boolean filterLoopOn()
	{
		return clientPhase == PHASE_FILTERING && clientRunning && !isInvalid();
	}

	void playOnce(SoundEvent sound)
	{
		world.playSound(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, sound, SoundCategory.BLOCKS, 1, 1, false);
	}

	@Override
	public void invalidate()
	{
		super.invalidate();
		if(world != null && world.isRemote && isMaster())
			ImmersiveEngineering.proxy.handleTileSound(INContent.soundFpFilterLoop, this, false, 1, 1);
		if(world != null && world.isRemote && isPumpPart())
			pump.stop(this);
	}

	@Override
	public void onChunkUnload()
	{
		super.onChunkUnload();
		if(world != null && world.isRemote && isMaster())
			ImmersiveEngineering.proxy.handleTileSound(INContent.soundFpFilterLoop, this, false, 1, 1);
		if(world != null && world.isRemote && isPumpPart())
			pump.stop(this);
	}

	// ---------- for the interface (master only) ----------
	public IItemHandler getInventory()
	{
		return inventory;
	}

	/** Bits for the interface: storage holds sludge (1), pressure tank holds sludge (2), the plates' batch is sludge (4),
	 * the solution tank holds water (8). */
	public int getKinds()
	{
		FluidStack s = solution.getFluid();
		return (isSludge(storage.getFluid()) ? 1 : 0) | (isSludge(pressureTank.getFluid()) ? 2 : 0) | (sludgeBatch ? 4 : 0)
				| (s != null && s.getFluid() == FluidRegistry.WATER ? 8 : 0);
	}

	public int getStorage()
	{
		return storage.getFluidAmount();
	}

	public int getPressureTank()
	{
		return pressureTank.getFluidAmount();
	}

	public int getSolution()
	{
		return solution.getFluidAmount();
	}

	public int getEnergy()
	{
		return energy.getEnergyStored();
	}

	public int getPumpTicks()
	{
		return pumpTicks;
	}

	public int getPhase()
	{
		return phase;
	}

	public int getPhaseTicks()
	{
		return phaseTicks;
	}

	public int getState()
	{
		return state;
	}

	public boolean isRedstoneInverted()
	{
		return redstoneInverted;
	}

	public void toggleRedstone()
	{
		redstoneInverted = !redstoneInverted;
		markDirty();
	}

	/** Contents that drop when the structure comes apart (fluids and energy are lost, like IE's tanks). */
	public void dropContents()
	{
		for(int i = 0; i < inventory.getSlots(); i++)
		{
			ItemStack s = inventory.getStackInSlot(i);
			if(!s.isEmpty())
				Block.spawnAsEntity(world, pos, s.copy());
			inventory.setStackInSlot(i, ItemStack.EMPTY);
		}
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
			storage.readFromNBT(m.getCompoundTag("storage"));
			pressureTank.readFromNBT(m.getCompoundTag("pressureTank"));
			solution.readFromNBT(m.getCompoundTag("solution"));
			energy.set(m.getInteger("energy"));
			pumpTicks = m.getInteger("pumpTicks");
			phase = m.getInteger("phase");
			phaseTicks = m.getInteger("phaseTicks");
			if(phase == PHASE_RELEASE)
				phase = PHASE_FILTERING;
			inPlates = m.getInteger("inPlates");
			solutionGiven = m.getInteger("solutionGiven");
			sludgeBatch = m.getBoolean("sludgeBatch");
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
			m.setTag("storage", storage.writeToNBT(new NBTTagCompound()));
			m.setTag("pressureTank", pressureTank.writeToNBT(new NBTTagCompound()));
			m.setTag("solution", solution.writeToNBT(new NBTTagCompound()));
			m.setInteger("energy", energy.getEnergyStored());
			m.setInteger("pumpTicks", pumpTicks);
			m.setInteger("phase", phase);
			m.setInteger("phaseTicks", phaseTicks);
			m.setInteger("inPlates", inPlates);
			m.setInteger("solutionGiven", solutionGiven);
			m.setBoolean("sludgeBatch", sludgeBatch);
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

	/** The client only needs the part and, on the master, the step of the press and the pump. */
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

	void writeClient(NBTTagCompound nbt)
	{
		writePart(nbt);
		if(isMaster())
		{
			nbt.setInteger("phase", phase);
			nbt.setInteger("phaseTicks", phaseTicks);
			nbt.setInteger("pump", pumpTicks);
			nbt.setBoolean("pumping", pumpTicks > 0 && pumpTicks < PRESSURIZE_TICKS);
			nbt.setInteger("filters", filterMask());
			nbt.setIntArray("wear", filterWear());
			nbt.setBoolean("running", state == STATE_RUNNING);
		}
	}

	void readClient(NBTTagCompound nbt)
	{
		readPart(nbt);
		int p = nbt.getInteger("phase");
		int t = nbt.getInteger("phaseTicks");
		// Keep the client's own count within a step (smoother), take the server's on a new step or a big drift
		if(p != clientPhase || Math.abs(t - clientPhaseTicks) > 10 || !nbt.getBoolean("running"))
			clientPhaseTicks = t;
		clientPhase = p;
		clientPump = nbt.getInteger("pump");
		clientPumping = nbt.getBoolean("pumping");
		clientFilters = nbt.getInteger("filters");
		clientRunning = nbt.getBoolean("running");
		int[] wear = nbt.getIntArray("wear");
		if(wear.length == FILTERS)
			clientWear = wear;
	}

	/** The master draws the whole press, 9 blocks long. */
	@Override
	public AxisAlignedBB getRenderBoundingBox()
	{
		return isMaster() ? new AxisAlignedBB(pos.add(-5, -2, -5), pos.add(6, 3, 6)) : super.getRenderBoundingBox();
	}
}
