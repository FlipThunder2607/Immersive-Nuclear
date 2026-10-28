package immersivenuclear.common.block;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IHammerInteraction;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.ISoundTile;
import immersivenuclear.common.INContent;
import immersivenuclear.common.item.ItemConcentrateDrum;
import immersivenuclear.common.multiblock.IndustrialDryerStructure;
import immersivenuclear.common.multiblock.MultiblockIndustrialDryer;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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
 * One part of a formed Industrial Dryer: which template block it replaced and how the structure is turned.
 * The master part also holds the machine; the port parts hand their capability, on their own face only, to the
 * master (see {@link IndustrialDryerStructure#PORTS}).
 *
 * Batches, in four steps (90 s). 1 load: once a batch of wet concentrate waits in the feed (and there is room for the
 * batch's output), the rotary valve lets it into the chamber. 2 heat: the furnace heaters bring the chamber up to
 * temperature. 3 dry: the paddles turn the concentrate over while the water boils off; the vapour goes through the
 * cyclone (the fines fall back) to the condenser, and the condensate runs into its tank as it forms. 4 cool and
 * discharge: the dry yellowcake comes out of the discharge valve at the end. Without power the batch waits where it
 * is; a redstone signal stops it (the hammer inverts that).
 * Each step sounds from where it happens: the rotary valve at the feed hopper, the heaters under the chamber, the
 * paddles' motor (the generator) at the drive end, the discharge valve at the yellowcake port.
 */
public class TileIndustrialDryer extends TileEntity implements ITickable, IHammerInteraction, ISoundTile
{
	// Provisional values: one batch takes what the Precipitation Unit gives in one batch (24 wet concentrate, 90 s)
	public static final int WET_PER_BATCH = 24, YELLOWCAKE_PER_BATCH = 16, CONDENSATE_PER_BATCH = 2000;
	public static final int CONDENSATE_CAPACITY = 8000, ENERGY_CAPACITY = 16000, PUSH_PER_TICK = 100;
	public static final int LOAD_TICKS = 80, HEAT_TICKS = 200, DRY_TICKS = 1200, DISCHARGE_TICKS = 320;
	/** 76800 IF a batch: the heaters draw most while they heat up, then hold the temperature. */
	public static final int LOAD_ENERGY = 16, HEAT_ENERGY = 64, DRY_ENERGY = 48, DISCHARGE_ENERGY = 16;
	public static final int PHASE_LOAD = 0, PHASE_HEAT = 1, PHASE_DRY = 2, PHASE_DISCHARGE = 3;
	public static final int STATE_IDLE = 0, STATE_RUNNING = 1, STATE_DISABLED = 2, STATE_NO_INPUT = 3, STATE_NO_POWER = 4, STATE_OUTPUT_FULL = 5;
	public static final int SLOT_WET = 0, SLOT_YELLOWCAKE = 1, SLOT_CONDENSATE_IN = 2, SLOT_CONDENSATE_OUT = 3;
	/**
	 * The drum loading station: drums waiting on the conveyor (empty ones, up to a stack, or a partly filled one, which the
	 * station tops up), the drum under the discharge, the full drum out.
	 */
	public static final int SLOT_DRUM_EMPTY = 4, SLOT_DRUM_FILLING = 5, SLOT_DRUM_FULL = 6, SLOTS = 7;
	/** The drum at the station: none, riding the conveyor in, under the discharge, riding on into the output. */
	public static final int DRUM_NONE = 0, DRUM_ARRIVING = 1, DRUM_FILLING = 2, DRUM_LEAVING = 3, DRUM_POURING = 4;
	/** The rides in and out; while it pours, one yellowcake goes into the drum every DRUM_POUR_EVERY ticks. */
	public static final int DRUM_ARRIVE_TICKS = 80, DRUM_LEAVE_TICKS = 30, DRUM_POUR_EVERY = 8;
	/** The drum's ride in the model, in blocks: off the port across onto the conveyor, down the conveyor, into the output. */
	static final float DRUM_PORT_DX = 1.4F, DRUM_RUN_DZ = 3.5F, DRUM_OUT_DZ = 0.95F;
	/** Chamber temperature, °C: room temperature when loaded, drying temperature from the end of the heating. */
	public static final int ROOM_TEMPERATURE = 20, DRYING_TEMPERATURE = 140;
	/**
	 * The scrubber on the vapour line catches the fine yellowcake the vapour carries off while it dries; every
	 * FINES_EVERY batches' fines come back as one more yellowcake at the discharge (provisional).
	 */
	public static final int FINES_EVERY = 4;

	int dx, dy, dz;
	EnumFacing facing = EnumFacing.NORTH;
	boolean mirrored;
	/** The block this part replaced, put back on disassembly. */
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
				case SLOT_WET:
					return stack.getItem() == INContent.itemWetConcentrate;
				case SLOT_DRUM_EMPTY:
					return ItemConcentrateDrum.canTakeMore(stack);
				case SLOT_CONDENSATE_IN:
					FluidStack held = FluidUtil.getFluidContained(stack);
					return FluidUtil.getFluidHandler(stack) != null && (held == null || held.getFluid() == INContent.fluidCondensate);
				default:
					// outputs only take what the machine puts there
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
	final FluidTank condensate = new FluidTank(CONDENSATE_CAPACITY)
	{
		@Override
		protected void onContentsChanged()
		{
			markDirty();
		}
	};
	final Energy energy = new Energy();
	int phase = PHASE_LOAD, phaseTicks;
	int state = STATE_IDLE;
	/** What happened this tick, for the state. */
	boolean progressed, noPower, outputFull, noInput;
	boolean redstoneInverted;
	int sentPhase = -1;
	int drumState = DRUM_NONE, drumTicks, sentDrumState = -1;
	public int clientDrumState = DRUM_NONE, clientDrumTicks;
	boolean sentRunning;

	// ---------- client ----------
	public int clientPhase = PHASE_LOAD, clientPhaseTicks;
	public boolean clientRunning;
	/** Batches whose fines the scrubber holds, towards the next yellowcake it gives back (0..FINES_EVERY). */
	int fines;
	/** The step whose one-off sound (load, discharge) was last heard; -2 until the first update from the server. */
	int heardPhase = -2;
	/** Where each step sounds (template offsets). */
	static final int[] FEED_PART = {1, 1, 2}, HEATER_PART = {1, -1, 3}, MOTOR_PART = {1, 0, 0}, DISCHARGE_PART = {2, -1, 6};
	/** The loops come in as their starts end (heaters 1.5 s, paddles 3 s). */
	static final int HEATER_LOOP_DELAY = 28, PADDLES_LOOP_DELAY = 58, FILL_LOOP_DELAY = 10;
	final LoopSound heaters = new LoopSound(() -> INContent.soundDryerHeatStart, () -> INContent.soundDryerHeatLoop, () -> INContent.soundDryerHeatStop,
			HEATER_LOOP_DELAY);
	/**
	 * Where the effects come out, in template blocks from the trigger block's corner (as the model draws them): the
	 * steam from over the rain cap of the vent stack (model x 4.5..5, y 8.3..8.44, z 9..10), the yellowcake's dust out
	 * of the spout over the drum station's drum (model x 4.2..5.2, y 3.1, z 10.5..11.5).
	 */
	static final double[] STEAM_AT = {2.63, 3.25, 4.75}, DUST_AT = {3.0, 0.55, 5.35};
	/** The yellowcake pouring into the drum at the station, from the yellowcake port's part under it. */
	final LoopSound drumFill = new LoopSound(() -> INContent.soundDryerFillStart, () -> INContent.soundDryerFillLoop, () -> INContent.soundDryerFillStop,
			FILL_LOOP_DELAY);
	final LoopSound paddles = new LoopSound(() -> INContent.soundDryerDryStart, () -> INContent.soundDryerDryLoop, () -> INContent.soundDryerDryStop,
			PADDLES_LOOP_DELAY);

	public TileIndustrialDryer()
	{
		condensate.setCanFill(false);
		condensate.setCanDrain(true);
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

	/** An item port: the feed only takes the wet concentrate in, the discharge only gives the yellowcake out. */
	class ItemPort implements IItemHandler
	{
		final int slot;
		final boolean in;

		ItemPort(int slot, boolean in)
		{
			this.slot = slot;
			this.in = in;
		}

		@Override
		public int getSlots()
		{
			return 1;
		}

		@Override
		public ItemStack getStackInSlot(int i)
		{
			return inventory.getStackInSlot(slot);
		}

		@Override
		public ItemStack insertItem(int i, ItemStack stack, boolean simulate)
		{
			return in ? inventory.insertItem(slot, stack, simulate) : stack;
		}

		@Override
		public ItemStack extractItem(int i, int amount, boolean simulate)
		{
			return in ? ItemStack.EMPTY : inventory.extractItem(slot, amount, simulate);
		}

		@Override
		public int getSlotLimit(int i)
		{
			return 64;
		}

		@Override
		public boolean isItemValid(int i, ItemStack stack)
		{
			return in && inventory.isItemValid(slot, stack);
		}
	}

	final ItemPort wetPort = new ItemPort(SLOT_WET, true), drumPort = new ItemPort(SLOT_DRUM_EMPTY, true);

	/**
	 * The yellowcake port: the full drums first, then the loose yellowcake, but that only while no drum is waiting or
	 * being filled, as the station takes it.
	 */
	final IItemHandler yellowcakePort = new IItemHandler()
	{
		int slot(int i)
		{
			return i == 0 ? SLOT_DRUM_FULL : drumsInUse() ? -1 : SLOT_YELLOWCAKE;
		}

		@Override
		public int getSlots()
		{
			return 2;
		}

		@Override
		public ItemStack getStackInSlot(int i)
		{
			return slot(i) < 0 ? ItemStack.EMPTY : inventory.getStackInSlot(slot(i));
		}

		@Override
		public ItemStack insertItem(int i, ItemStack stack, boolean simulate)
		{
			return stack;
		}

		@Override
		public ItemStack extractItem(int i, int amount, boolean simulate)
		{
			return slot(i) < 0 ? ItemStack.EMPTY : inventory.extractItem(slot(i), amount, simulate);
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
	};

	/** Drums at the station: the yellowcake goes into them rather than out loose. */
	boolean drumsInUse()
	{
		return !inventory.getStackInSlot(SLOT_DRUM_EMPTY).isEmpty() || !inventory.getStackInSlot(SLOT_DRUM_FILLING).isEmpty();
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
		return IndustrialDryerStructure.get(dx, dy, dz);
	}

	public int getPort()
	{
		return IndustrialDryerStructure.portAt(dx, dy, dz);
	}

	/** What a broken part drops: the block it replaced. */
	public ItemStack getOriginalStack()
	{
		return MultiblockIndustrialDryer.stackFor(getPartCode()).copy();
	}

	public boolean isMaster()
	{
		return dx == IndustrialDryerStructure.MASTER_DX && dy == IndustrialDryerStructure.MASTER_DY && dz == IndustrialDryerStructure.MASTER_DZ;
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
		int[] w = IndustrialDryerStructure.rotate(dx, dz, MultiblockIndustrialDryer.turns(facing), mirrored);
		return pos.add(-w[0], -dy, -w[1]);
	}

	public BlockPos getMasterPos()
	{
		return MultiblockIndustrialDryer.toWorld(getTriggerPos(), facing, mirrored,
				IndustrialDryerStructure.MASTER_DX, IndustrialDryerStructure.MASTER_DY, IndustrialDryerStructure.MASTER_DZ);
	}

	public TileIndustrialDryer getMaster()
	{
		if(isMaster())
			return this;
		if(world == null)
			return null;
		BlockPos m = getMasterPos();
		if(!world.isBlockLoaded(m))
			return null;
		TileEntity te = world.getTileEntity(m);
		return te instanceof TileIndustrialDryer && ((TileIndustrialDryer)te).isMaster() ? (TileIndustrialDryer)te : null;
	}

	/** World face a port works on, or null when it works on every side. */
	EnumFacing portWorldFace()
	{
		int[] f = IndustrialDryerStructure.portFace(dx, dy, dz);
		if(f == null || f[0] == 0 && f[1] == 0)
			return null;
		int[] w = IndustrialDryerStructure.rotate(f[0], f[1], MultiblockIndustrialDryer.turns(facing), mirrored);
		return EnumFacing.getFacingFromVector(w[0], 0, w[1]);
	}

	boolean portServes(Capability<?> cap, EnumFacing side)
	{
		int port = getPort();
		boolean right;
		if(cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
			right = port == IndustrialDryerStructure.PORT_CONDENSATE;
		else if(cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			right = port == IndustrialDryerStructure.PORT_WET || port == IndustrialDryerStructure.PORT_YELLOWCAKE || port == IndustrialDryerStructure.PORT_DRUM;
		else if(cap == CapabilityEnergy.ENERGY)
			right = port == IndustrialDryerStructure.PORT_ENERGY;
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
			TileIndustrialDryer m = getMaster();
			if(m != null)
			{
				if(cap == CapabilityEnergy.ENERGY)
					return CapabilityEnergy.ENERGY.cast(m.energy);
				if(cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
					return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(getPort() == IndustrialDryerStructure.PORT_WET ? m.wetPort
							: getPort() == IndustrialDryerStructure.PORT_DRUM ? m.drumPort : m.yellowcakePort);
				return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(m.condensate);
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
			TileIndustrialDryer m = isPart(HEATER_PART) || isPart(MOTOR_PART) || isPart(DISCHARGE_PART) ? getMaster() : null;
			if(isPart(HEATER_PART))
				heaters.update(this, m != null && m.clientRunning && m.clientPhase == PHASE_HEAT);
			else if(isPart(MOTOR_PART))
				paddles.update(this, m != null && m.clientRunning && m.clientPhase == PHASE_DRY);
			else if(isPart(DISCHARGE_PART))
				drumFill.update(this, m != null && m.clientDrumState == DRUM_POURING);
			return;
		}
		int port = getPort();
		if(port == IndustrialDryerStructure.PORT_CONDENSATE)
			pushCondensate();
		else if(port == IndustrialDryerStructure.PORT_YELLOWCAKE && world.getTotalWorldTime() % 8 == 0)
			pushYellowcake();
		if(isMaster())
		{
			handleContainers();
			process();
			loadDrums();
			sync();
		}
	}

	// ---------- the batch ----------
	void process()
	{
		int before = state;
		progressed = noPower = outputFull = noInput = false;
		if(world.isBlockPowered(getTriggerPos()) != redstoneInverted)
		{
			state = STATE_DISABLED;
			if(state != before)
				markDirty();
			return;
		}
		switch(phase)
		{
			case PHASE_LOAD:
				load();
				break;
			case PHASE_HEAT:
				if(step(HEAT_ENERGY, HEAT_TICKS))
					next(PHASE_DRY);
				break;
			case PHASE_DRY:
				dry();
				break;
			case PHASE_DISCHARGE:
				if(step(DISCHARGE_ENERGY, DISCHARGE_TICKS))
				{
					// The scrubber's fines go out with the batch once it holds a yellowcake's worth
					int extra = fines >= FINES_EVERY ? 1 : 0;
					fines -= extra * FINES_EVERY;
					output(SLOT_YELLOWCAKE, new ItemStack(INContent.itemYellowcake, YELLOWCAKE_PER_BATCH + extra));
					next(PHASE_LOAD);
				}
				break;
		}
		if(progressed)
			state = STATE_RUNNING;
		else if(noPower)
			state = STATE_NO_POWER;
		else if(outputFull)
			state = STATE_OUTPUT_FULL;
		else if(noInput)
			state = STATE_NO_INPUT;
		else
			state = STATE_IDLE;
		if(state != before)
			markDirty();
	}

	/**
	 * A batch only goes in with room for its whole output (yellowcake and condensate), so it never stops halfway
	 * because of a full output. The rotary valve takes the batch at the start of the step.
	 */
	void load()
	{
		if(phaseTicks == 0)
		{
			if(inventory.getStackInSlot(SLOT_WET).getCount() < WET_PER_BATCH)
			{
				noInput = true;
				return;
			}
			int out = YELLOWCAKE_PER_BATCH + (fines + 1 >= FINES_EVERY ? 1 : 0);
			if(condensate.getCapacity() - condensate.getFluidAmount() < CONDENSATE_PER_BATCH
					|| !canOutput(SLOT_YELLOWCAKE, new ItemStack(INContent.itemYellowcake, out)))
			{
				outputFull = true;
				return;
			}
			if(!energy.use(LOAD_ENERGY))
			{
				noPower = true;
				return;
			}
			inventory.extractItem(SLOT_WET, WET_PER_BATCH, false);
			progressed = true;
			phaseTicks = 1;
			markDirty();
			return;
		}
		if(step(LOAD_ENERGY, LOAD_TICKS))
			next(PHASE_HEAT);
	}

	/** The water leaves the concentrate evenly over the step and reaches the condensate tank as it condenses. */
	void dry()
	{
		if(!step(DRY_ENERGY, DRY_TICKS))
		{
			if(progressed)
				condense(phaseTicks);
			return;
		}
		condense(DRY_TICKS);
		// The scrubber has caught this batch's fines
		fines++;
		next(PHASE_DISCHARGE);
	}

	void condense(int ticks)
	{
		int out = CONDENSATE_PER_BATCH * ticks / DRY_TICKS - CONDENSATE_PER_BATCH * (ticks - 1) / DRY_TICKS;
		if(out > 0)
			condensate.fillInternal(new FluidStack(INContent.fluidCondensate, out), true);
	}

	/** One tick of a timed step, if there is power for it; true when the step is done. */
	boolean step(int energyPerTick, int ticks)
	{
		if(!energy.use(energyPerTick))
		{
			noPower = true;
			return false;
		}
		progressed = true;
		phaseTicks++;
		markDirty();
		return phaseTicks >= ticks;
	}

	void next(int phase)
	{
		this.phase = phase;
		phaseTicks = 0;
		markDirty();
	}

	// ---------- containers and ports ----------
	/** Bucket slots, like IE's: empty containers take the condensate. */
	void handleContainers()
	{
		ItemStack in = inventory.getStackInSlot(SLOT_CONDENSATE_IN);
		if(!in.isEmpty() && condensate.getFluidAmount() > 0)
		{
			FluidActionResult r = FluidUtil.tryFillContainer(ItemHandlerHelper.copyStackWithSize(in, 1), condensate, Integer.MAX_VALUE, null, false);
			if(r.isSuccess() && canOutput(SLOT_CONDENSATE_OUT, r.getResult()))
			{
				r = FluidUtil.tryFillContainer(ItemHandlerHelper.copyStackWithSize(in, 1), condensate, Integer.MAX_VALUE, null, true);
				output(SLOT_CONDENSATE_OUT, r.getResult());
				in.shrink(1);
				inventory.setStackInSlot(SLOT_CONDENSATE_IN, in);
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

	/** The condensate port empties its tank into whatever accepts fluid on its face. */
	void pushCondensate()
	{
		TileIndustrialDryer m = getMaster();
		EnumFacing face = portWorldFace();
		if(m == null || face == null || m.condensate.getFluidAmount() <= 0)
			return;
		TileEntity out = world.getTileEntity(pos.offset(face));
		if(out == null || !out.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite()))
			return;
		IFluidHandler target = out.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite());
		if(target == null)
			return;
		FluidStack offer = m.condensate.drainInternal(PUSH_PER_TICK, false);
		int taken = offer == null ? 0 : target.fill(offer, true);
		if(taken > 0)
			m.condensate.drainInternal(taken, true);
	}

	/**
	 * The drum loading station: the conveyor brings the next empty drum from the port to under the discharge
	 * (DRUM_ARRIVE_TICKS), the drum takes the loose yellowcake up to its capacity, and once full goes on, sealed, into the
	 * output (DRUM_LEAVE_TICKS, the next drum waits for it).
	 */
	void loadDrums()
	{
		ItemStack drum = inventory.getStackInSlot(SLOT_DRUM_FILLING);
		switch(drumState)
		{
			case DRUM_ARRIVING:
				if(drum.isEmpty())
					drumState = DRUM_NONE;
				else if(++drumTicks >= DRUM_ARRIVE_TICKS)
					drumState = DRUM_FILLING;
				return;
			case DRUM_LEAVING:
				if(++drumTicks >= DRUM_LEAVE_TICKS)
					drumState = DRUM_NONE;
				return;
			case DRUM_FILLING:
			case DRUM_POURING:
				if(drum.isEmpty())
				{
					drumState = DRUM_NONE;
					return;
				}
				break;
			default:
				if(drum.isEmpty())
				{
					if(inventory.getStackInSlot(SLOT_DRUM_EMPTY).isEmpty())
						return;
					inventory.setStackInSlot(SLOT_DRUM_FILLING, inventory.extractItem(SLOT_DRUM_EMPTY, 1, false));
					drumState = DRUM_ARRIVING;
					drumTicks = 0;
					return;
				}
				drumState = DRUM_FILLING;
		}
		int n = ItemConcentrateDrum.contents(drum);
		if(n >= TileConcentrateDrum.CAPACITY)
		{
			if(inventory.getStackInSlot(SLOT_DRUM_FULL).isEmpty())
			{
				inventory.setStackInSlot(SLOT_DRUM_FULL, drum);
				inventory.setStackInSlot(SLOT_DRUM_FILLING, ItemStack.EMPTY);
				world.playSound(null, MultiblockIndustrialDryer.toWorld(getTriggerPos(), facing, mirrored, DISCHARGE_PART[0], DISCHARGE_PART[1], DISCHARGE_PART[2]), SoundEvents.BLOCK_METAL_PLACE, SoundCategory.BLOCKS, .7F, .6F);
				drumState = DRUM_LEAVING;
				drumTicks = 0;
			}
			return;
		}
		// The loose yellowcake pours into the drum a little at a time, from the spout over it
		if(inventory.getStackInSlot(SLOT_YELLOWCAKE).isEmpty())
		{
			drumState = DRUM_FILLING;
			return;
		}
		if(drumState != DRUM_POURING)
		{
			drumState = DRUM_POURING;
			drumTicks = 0;
		}
		if(drumTicks++ % DRUM_POUR_EVERY != 0)
			return;
		inventory.extractItem(SLOT_YELLOWCAKE, 1, false);
		inventory.setStackInSlot(SLOT_DRUM_FILLING, ItemConcentrateDrum.withContents(n + 1));
	}

	/** The conveyor runs while a drum rides it (client). */
	public boolean drumMoving()
	{
		return clientDrumState == DRUM_ARRIVING || clientDrumState == DRUM_LEAVING;
	}

	/**
	 * Where the drum shows, as a template {dx, dz} shift from under the discharge (where the model has it), or null when
	 * there is none (client). Arriving, it comes off the port onto the conveyor (DRUM_PORT_DX) and down it (DRUM_RUN_DZ);
	 * leaving, it goes on into the output (DRUM_OUT_DZ). Each move eases in and out.
	 */
	public float[] drumShift(float partialTicks)
	{
		switch(clientDrumState)
		{
			case DRUM_ARRIVING:
			{
				float t = ease((clientDrumTicks + partialTicks) / DRUM_ARRIVE_TICKS) * (DRUM_PORT_DX + DRUM_RUN_DZ);
				return t < DRUM_PORT_DX ? new float[]{DRUM_PORT_DX - t, -DRUM_RUN_DZ} : new float[]{0, t - DRUM_PORT_DX - DRUM_RUN_DZ};
			}
			case DRUM_FILLING:
			case DRUM_POURING:
				return new float[]{0, 0};
			case DRUM_LEAVING:
				return new float[]{0, ease((clientDrumTicks + partialTicks) / DRUM_LEAVE_TICKS) * DRUM_OUT_DZ};
			default:
				return null;
		}
	}

	static float ease(float t)
	{
		t = Math.max(0, Math.min(1, t));
		return t * t * (3 - 2 * t);
	}

	/**
	 * The yellowcake port hands the full drums, then the loose yellowcake (while no drums are in use), to whatever takes
	 * items on its face (conveyor, hopper, chest).
	 */
	void pushYellowcake()
	{
		TileIndustrialDryer m = getMaster();
		EnumFacing face = portWorldFace();
		if(m == null || face == null)
			return;
		TileEntity out = world.getTileEntity(pos.offset(face));
		if(out == null || !out.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face.getOpposite()))
			return;
		IItemHandler target = out.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face.getOpposite());
		if(target == null)
			return;
		for(int i = 0; i < 2; i++)
		{
			ItemStack one = m.yellowcakePort.extractItem(i, 1, true);
			if(!one.isEmpty() && ItemHandlerHelper.insertItem(target, one, true).isEmpty())
			{
				ItemHandlerHelper.insertItem(target, m.yellowcakePort.extractItem(i, 1, false), false);
				return;
			}
		}
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
		if(getPort() != IndustrialDryerStructure.PORT_REDSTONE)
			return false;
		TileIndustrialDryer m = getMaster();
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

	/** condensate tank, energy, phase, phase ticks, state, redstone mode, the scrubber's fines. */
	public int[] guiValues()
	{
		return new int[]{condensate.getFluidAmount(), energy.getEnergyStored(), phase, phaseTicks, state, redstoneInverted ? 1 : 0, fines};
	}

	/** Chamber temperature (°C) at a point of the batch: it rises while heating, holds while drying, falls while cooling. */
	public static int temperature(int phase, int ticks)
	{
		float f;
		switch(phase)
		{
			case PHASE_HEAT: f = Math.min(1, ticks / (float)HEAT_TICKS); break;
			case PHASE_DRY: f = 1; break;
			case PHASE_DISCHARGE: f = 1 - Math.min(1, ticks / (float)DISCHARGE_TICKS); break;
			default: f = 0;
		}
		return Math.round(ROOM_TEMPERATURE + (DRYING_TEMPERATURE - ROOM_TEMPERATURE) * f);
	}

	/** Length of each step, in ticks. */
	public static int phaseLength(int phase)
	{
		switch(phase)
		{
			case PHASE_LOAD: return LOAD_TICKS;
			case PHASE_HEAT: return HEAT_TICKS;
			case PHASE_DRY: return DRY_TICKS;
			default: return DISCHARGE_TICKS;
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
			condensate.readFromNBT(m.getCompoundTag("condensate"));
			energy.set(m.getInteger("energy"));
			phase = m.getInteger("phase");
			phaseTicks = m.getInteger("phaseTicks");
			state = m.getInteger("state");
			redstoneInverted = m.getBoolean("redstoneInverted");
			fines = m.getInteger("fines");
			drumState = m.getInteger("drumState");
			drumTicks = m.getInteger("drumTicks");
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
			m.setTag("condensate", condensate.writeToNBT(new NBTTagCompound()));
			m.setInteger("energy", energy.getEnergyStored());
			m.setInteger("phase", phase);
			m.setInteger("phaseTicks", phaseTicks);
			m.setInteger("state", state);
			m.setBoolean("redstoneInverted", redstoneInverted);
			m.setInteger("fines", fines);
			m.setInteger("drumState", drumState);
			m.setInteger("drumTicks", drumTicks);
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

	/** The client needs the part (which block of the structure, how it is turned) and, on the master, the batch's step. */
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
		boolean om = mirrored, heaters = heatersOn();
		readClient(pkt.getNbtCompound());
		// Redrawn when the part changes; the whole machine when the heaters' faces light up or go dark (every part draws a slice)
		if(ox != dx || oy != dy || oz != dz || of != facing || om != mirrored)
			world.markBlockRangeForRenderUpdate(pos, pos);
		if(isMaster() && heaters != heatersOn())
			world.markBlockRangeForRenderUpdate(pos.add(-3, -2, -3), pos.add(3, 2, 3));
	}

	void writeClient(NBTTagCompound nbt)
	{
		writePart(nbt);
		if(isMaster())
		{
			nbt.setInteger("phase", phase);
			nbt.setInteger("phaseTicks", phaseTicks);
			nbt.setBoolean("running", state == STATE_RUNNING);
			nbt.setInteger("drum", drumState);
			nbt.setInteger("drumTicks", drumTicks);
		}
	}

	void readClient(NBTTagCompound nbt)
	{
		readPart(nbt);
		if(nbt.hasKey("phase"))
		{
			int p = nbt.getInteger("phase");
			// The client counts the ticks of a step itself; the server's count only corrects it on a new step or a drift
			if(p != clientPhase || Math.abs(nbt.getInteger("phaseTicks") - clientPhaseTicks) > 4)
				clientPhaseTicks = nbt.getInteger("phaseTicks");
			clientPhase = p;
			clientRunning = nbt.getBoolean("running");
			int d = nbt.getInteger("drum");
			if(d != clientDrumState || Math.abs(nbt.getInteger("drumTicks") - clientDrumTicks) > 4)
				clientDrumTicks = nbt.getInteger("drumTicks");
			clientDrumState = d;
			// Joining in the middle of a step does not replay its sound
			if(heardPhase == -2)
				heardPhase = p;
		}
	}

	/** Sent when the step or the running change, and every 2 s while running. */
	void sync()
	{
		boolean running = state == STATE_RUNNING;
		if(phase != sentPhase || running != sentRunning || drumState != sentDrumState || world.getTotalWorldTime() % 40 == 0 && running)
		{
			sentDrumState = drumState;
			sentPhase = phase;
			sentRunning = running;
			IBlockState st = world.getBlockState(pos);
			world.notifyBlockUpdate(pos, st, st, 2);
		}
	}

	// ---------- client: sound ----------
	boolean isPart(int[] part)
	{
		return dx == part[0] && dy == part[1] && dz == part[2];
	}

	/**
	 * Master, client side, every tick: the step's clock, and the one-off sounds of the steps that start with a
	 * movement (the rotary valve lets the batch in, the discharge valve lets it out), once per batch.
	 */
	void updateClient()
	{
		if(clientRunning && clientPhaseTicks < phaseLength(clientPhase))
			clientPhaseTicks++;
		if(drumMoving() && clientDrumTicks < (clientDrumState == DRUM_ARRIVING ? DRUM_ARRIVE_TICKS : DRUM_LEAVE_TICKS))
			clientDrumTicks++;
		effects();
		if(!clientRunning || clientPhase == heardPhase || heardPhase == -2)
			return;
		heardPhase = clientPhase;
		if(clientPhaseTicks > 20)
			return;
		if(clientPhase == PHASE_LOAD)
			playAt(FEED_PART, INContent.soundDryerLoad);
		else if(clientPhase == PHASE_DISCHARGE)
			playAt(DISCHARGE_PART, INContent.soundDryerDischarge);
	}

	/** The heaters are on while they heat the chamber up and while they hold it hot for the drying (client, master). */
	public boolean heatersOn()
	{
		return clientRunning && (clientPhase == PHASE_HEAT || clientPhase == PHASE_DRY);
	}

	/**
	 * Steam out of the condenser while it dries (a wisp while the chamber heats up and while it cools), the yellowcake's
	 * dust at the discharge port while it pours.
	 */
	void effects()
	{
		java.util.Random r = world.rand;
		if(clientDrumState == DRUM_POURING)
			dust(r);
		if(!clientRunning)
			return;
		int every = clientPhase == PHASE_DRY ? 2 : clientPhase == PHASE_HEAT && clientPhaseTicks > HEAT_TICKS / 2 || clientPhase == PHASE_DISCHARGE ? 8 : 0;
		if(every > 0 && r.nextInt(every) == 0)
		{
			Vec3d p = templatePoint(STEAM_AT[0] + (r.nextDouble() - .5) * .4, STEAM_AT[1], STEAM_AT[2] + (r.nextDouble() - .5) * .4);
			world.spawnParticle(EnumParticleTypes.CLOUD, p.x, p.y, p.z, (r.nextDouble() - .5) * .02, .06 + r.nextDouble() * .04, (r.nextDouble() - .5) * .02);
		}
	}

	/** Yellowcake falling from the spout over the drum station. */
	void dust(java.util.Random r)
	{
		for(int k = 0; k < 6; k++)
		{
			// A thick stream from the spout down into the drum, spreading a little as it falls
			double fall = r.nextDouble(), spread = .1 + .14 * fall;
			Vec3d p = templatePoint(DUST_AT[0] + (r.nextDouble() - .5) * spread, DUST_AT[1] - fall * .55, DUST_AT[2] + (r.nextDouble() - .5) * spread);
			// The redstone particle takes its colour in the speed arguments
			world.spawnParticle(EnumParticleTypes.REDSTONE, p.x, p.y, p.z, .9, .78, .18);
		}
	}

	/** World point of a point given in template blocks from the trigger block's corner (any turn, mirrored or not). */
	Vec3d templatePoint(double x, double y, double z)
	{
		int turns = MultiblockIndustrialDryer.turns(facing);
		int[] ex = IndustrialDryerStructure.rotate(1, 0, turns, mirrored), ez = IndustrialDryerStructure.rotate(0, 1, turns, mirrored);
		BlockPos t = getTriggerPos();
		double u = x - .5, v = z - .5;
		return new Vec3d(t.getX() + .5 + ex[0] * u + ez[0] * v, t.getY() + y, t.getZ() + .5 + ex[1] * u + ez[1] * v);
	}

	void playAt(int[] part, SoundEvent sound)
	{
		BlockPos p = MultiblockIndustrialDryer.toWorld(getTriggerPos(), facing, mirrored, part[0], part[1], part[2]);
		world.playSound(p.getX() + .5, p.getY() + .5, p.getZ() + .5, sound, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
	}

	/** IE's looping tile sound asks this every 2 s and ends the loop when it is false. */
	@Override
	public boolean shoudlPlaySound(String sound)
	{
		if(isPart(HEATER_PART))
			return heaters.shouldPlay(this, sound);
		if(isPart(DISCHARGE_PART))
			return drumFill.shouldPlay(this, sound);
		return isPart(MOTOR_PART) && paddles.shouldPlay(this, sound);
	}

	void stopSound()
	{
		if(world == null || !world.isRemote)
			return;
		if(isPart(HEATER_PART))
			heaters.stop(this);
		else if(isPart(MOTOR_PART))
			paddles.stop(this);
		else if(isPart(DISCHARGE_PART))
			drumFill.stop(this);
	}

	/** Stop the loop when the structure comes apart or the chunk unloads. */
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

	/** The master draws the whole machine (any turn): 3 blocks each way, 2 down to the floor, 2 up to the condenser's top. */
	@Override
	public AxisAlignedBB getRenderBoundingBox()
	{
		return isMaster() ? new AxisAlignedBB(pos.add(-3, -2, -3), pos.add(4, 3, 4)) : super.getRenderBoundingBox();
	}
}
