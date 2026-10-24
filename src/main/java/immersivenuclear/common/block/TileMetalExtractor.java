package immersivenuclear.common.block;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IHammerInteraction;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.ISoundTile;
import immersivenuclear.common.INContent;
import immersivenuclear.common.item.ItemElectrode;
import immersivenuclear.common.multiblock.MetalExtractorStructure;
import immersivenuclear.common.multiblock.MultiblockMetalExtractor;
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
 * One part of a formed Metal Extractor: which template block it replaced and how the structure is turned.
 * The master part also holds the machine; the port parts hand their capability, on their own face only, to the
 * master (see {@link MetalExtractorStructure#PORTS}).
 *
 * Electrolysis in batches; the vat is the machine's only input tank. 1 fill: the effluent port (or a bucket) fills the
 * vat with one batch of chemical effluent. 2 prepare: the liquid circulates and the current comes up. 3 recover: the
 * metals plate out on the six plates (high power). Then, at the same time:
 * 4 drain: what is left, depleted effluent, goes to its tank (and once the vat is empty, it fills again);
 * 5 collect: the platform lifts the plates out of the vat, the tray slides in under them, the cleaner goes down the
 * plates scraping the deposit into the tray and back up, and the tray slides back into the deposit slot.
 * The plates stay up until the vat is full again; then they go down (lower) and the next batch is prepared.
 * The six electrode plates are items (ItemElectrode, one per plate slot, mixed as the player likes): a batch starts
 * only with all six; their materials set the recovery's energy and the batch's deposit, and each batch wears them.
 * Without power the batch waits where it is; a redstone signal stops it (the hammer inverts that).
 */
public class TileMetalExtractor extends TileEntity implements ITickable, IHammerInteraction, ISoundTile
{
	public static final int BATCH = 12000, DEPLETED_CAPACITY = 16000, ENERGY_CAPACITY = 32000;
	// Provisional values
	public static final int PREPARE_TICKS = 100, RECOVER_TICKS = 1200, LOWER_TICKS = 40;
	public static final int PREPARE_ENERGY = 64, RECOVER_ENERGY = 256, PUMP_ENERGY = 16, COLLECT_ENERGY = 32;
	/** The pump drains 1000 mB a second; one deposit per 2000 mB of effluent with steel plates. */
	public static final int DRAIN_PER_TICK = 50, PUSH_PER_TICK = 100, DEPOSITS_PER_BATCH = BATCH / 2000;
	/**
	 * The collect, in ticks from its start (beside the drain): platform up (3 s), tray in (1 s), the cleaner down the
	 * plates scraping them (1.75 s) and back up (1.25 s), tray out (1 s); later, with the vat full again, the platform
	 * goes down (LOWER_TICKS, 2 s). The sounds (ferramentas/sons_extratora.py) last as long as these movements.
	 */
	public static final int RAISED = 60, TRAY_IN = 80, CLEANED = 115, SCRAPED = 140, TRAY_OUT = 160;
	/** The liquid's steps; LOWER (the plates going back into the full vat) shows as part of the preparation. */
	public static final int PHASE_FILL = 0, PHASE_PREPARE = 1, PHASE_RECOVER = 2, PHASE_DRAIN = 3, PHASE_LOWER = 4;
	/** The five steps the interface shows: fill, prepare, recover, drain, collect. */
	public static final int STEPS = 5, STEP_COLLECT = 4;
	public static final int STATE_IDLE = 0, STATE_RUNNING = 1, STATE_DISABLED = 2, STATE_NO_INPUT = 3, STATE_NO_POWER = 4, STATE_OUTPUT_FULL = 5,
			STATE_NO_PLATES = 6;
	public static final int SLOT_DEPOSIT = 0, SLOT_EFFLUENT_IN = 1, SLOT_EFFLUENT_OUT = 2, SLOT_DEPLETED_IN = 3, SLOT_DEPLETED_OUT = 4;
	/** The six electrode plates, left to right as seen from the front (the model's plate0 .. plate5). */
	public static final int SLOT_PLATE0 = 5, PLATES = 6, SLOTS = SLOT_PLATE0 + PLATES;

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
				case SLOT_EFFLUENT_IN:
					FluidStack in = FluidUtil.getFluidContained(stack);
					return in != null && in.getFluid() == INContent.fluidEffluent;
				case SLOT_DEPLETED_IN:
					FluidStack held = FluidUtil.getFluidContained(stack);
					return FluidUtil.getFluidHandler(stack) != null && (held == null || held.getFluid() == INContent.fluidDepletedEffluent);
				default:
					// the plate slots take electrode plates; outputs only take what the machine puts there
					return slot >= SLOT_PLATE0 && ItemElectrode.of(stack) != null;
			}
		}

		@Override
		public int getSlotLimit(int slot)
		{
			return slot >= SLOT_PLATE0 ? 1 : 64;
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
	/** The vat: takes chemical effluent only while it fills; after the recovery it holds the depleted effluent. */
	final FluidTank vat = new FluidTank(BATCH)
	{
		@Override
		public boolean canFillFluidType(FluidStack f)
		{
			return f != null && f.getFluid() == INContent.fluidEffluent;
		}

		@Override
		public int fill(FluidStack resource, boolean doFill)
		{
			return phase == PHASE_FILL ? super.fill(resource, doFill) : 0;
		}

		@Override
		protected void onContentsChanged()
		{
			markDirty();
		}
	};
	final FluidTank depleted = new FluidTank(DEPLETED_CAPACITY)
	{
		@Override
		protected void onContentsChanged()
		{
			markDirty();
		}
	};
	final Energy energy = new Energy();
	int phase = PHASE_FILL, phaseTicks;
	/** The collect's tick count: -1 with the platform down, up to TRAY_OUT (plates up and clean, waiting for the vat). */
	int collect = -1;
	/** Deposits the plates hold, set at the end of the recovery by their materials, given at the end of the collect. */
	int batchDeposits;
	int state = STATE_IDLE;
	/** What happened this tick, for the state: something moved, something lacked power, an output was full. */
	boolean progressed, noPower, outputFull, noPlates, collecting;
	boolean redstoneInverted;
	/** Vat level last tick, to tell when the port is filling it (the pump runs); ticks since it last rose. */
	int lastVat, sinceFilled = FILL_GRACE;
	/** Pipes and buckets fill in spurts: the filling counts as running this long after the level last rose. */
	static final int FILL_GRACE = 20;
	boolean pumping;
	int sentPhase = -1, sentVat = -1, sentCollect = -2, sentPlates = -1;
	boolean sentRunning, sentPumping, sentDepleted, sentCollecting;

	// ---------- client ----------
	public int clientPhase = PHASE_FILL, clientPhaseTicks, clientVat, clientCollect = -1;
	public boolean clientRunning, clientPumping, clientCollecting;
	/** The material of each plate slot (0 empty, else ItemElectrode.tier), for the renderer. */
	public int[] clientPlates = new int[PLATES];
	/** Whether the vat holds the depleted effluent (after the recovery) rather than the chemical effluent. */
	public boolean clientDepleted;
	final PumpSound pump = new PumpSound();
	/** The pump stands at the right end, by the depleted effluent port (lower half of the IE pump). */
	static final int PUMP_DX = 2, PUMP_DY = -1, PUMP_DZ = 0;
	/** The plates' hum comes from the power controller, at the back (middle light engineering block). */
	final LoopSound plates = new LoopSound(() -> INContent.soundMePlatesStart, () -> INContent.soundMePlatesLoop, () -> INContent.soundMePlatesStop, PLATES_LOOP_DELAY);
	static final int CABINET_DX = 3, CABINET_DY = 0, CABINET_DZ = -2;
	/** The loop comes in as the 2.2 s start ends, less the 60 ms crossfade of the recording. */
	static final int PLATES_LOOP_DELAY = 43;
	/** The collect's movements, in order: when each starts (ticks) and its one clip; the next one to play. */
	static final int[] MOVES = {0, RAISED, TRAY_IN, CLEANED, SCRAPED};
	int nextMove;
	/** The platform's way down (the LOWER step) plays once. */
	boolean playedLower;

	public TileMetalExtractor()
	{
		vat.setCanDrain(false);
		depleted.setCanFill(false);
		depleted.setCanDrain(true);
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

	/** The deposit port: only gives the deposit out. */
	class DepositPort implements IItemHandler
	{
		@Override
		public int getSlots()
		{
			return 1;
		}

		@Override
		public ItemStack getStackInSlot(int i)
		{
			return inventory.getStackInSlot(SLOT_DEPOSIT);
		}

		@Override
		public ItemStack insertItem(int i, ItemStack stack, boolean simulate)
		{
			return stack;
		}

		@Override
		public ItemStack extractItem(int i, int amount, boolean simulate)
		{
			return inventory.extractItem(SLOT_DEPOSIT, amount, simulate);
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

	final DepositPort depositPort = new DepositPort();

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
		return MetalExtractorStructure.get(dx, dy, dz);
	}

	public int getPort()
	{
		return MetalExtractorStructure.portAt(dx, dy, dz);
	}

	public ItemStack getOriginalStack()
	{
		return MultiblockMetalExtractor.stackFor(getPartCode());
	}

	public boolean isMaster()
	{
		return dx == MetalExtractorStructure.MASTER_DX && dy == MetalExtractorStructure.MASTER_DY && dz == MetalExtractorStructure.MASTER_DZ;
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
		int[] w = MetalExtractorStructure.rotate(dx, dz, MultiblockMetalExtractor.turns(facing), mirrored);
		return pos.add(-w[0], -dy, -w[1]);
	}

	public BlockPos getMasterPos()
	{
		return MultiblockMetalExtractor.toWorld(getTriggerPos(), facing, mirrored,
				MetalExtractorStructure.MASTER_DX, MetalExtractorStructure.MASTER_DY, MetalExtractorStructure.MASTER_DZ);
	}

	public TileMetalExtractor getMaster()
	{
		if(isMaster())
			return this;
		if(world == null)
			return null;
		BlockPos m = getMasterPos();
		if(!world.isBlockLoaded(m))
			return null;
		TileEntity te = world.getTileEntity(m);
		return te instanceof TileMetalExtractor && ((TileMetalExtractor)te).isMaster() ? (TileMetalExtractor)te : null;
	}

	/** World face a port works on, or null when it works on every side. */
	EnumFacing portWorldFace()
	{
		int[] f = MetalExtractorStructure.portFace(dx, dy, dz);
		if(f == null || f[0] == 0 && f[1] == 0)
			return null;
		int[] w = MetalExtractorStructure.rotate(f[0], f[1], MultiblockMetalExtractor.turns(facing), mirrored);
		return EnumFacing.getFacingFromVector(w[0], 0, w[1]);
	}

	boolean portServes(Capability<?> cap, EnumFacing side)
	{
		int port = getPort();
		boolean right;
		if(cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
			right = port == MetalExtractorStructure.PORT_EFFLUENT || port == MetalExtractorStructure.PORT_DEPLETED;
		else if(cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			right = port == MetalExtractorStructure.PORT_DEPOSIT;
		else if(cap == CapabilityEnergy.ENERGY)
			right = port == MetalExtractorStructure.PORT_ENERGY;
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
			TileMetalExtractor m = getMaster();
			if(m != null)
			{
				if(cap == CapabilityEnergy.ENERGY)
					return CapabilityEnergy.ENERGY.cast(m.energy);
				if(cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
					return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(m.depositPort);
				return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(getPort() == MetalExtractorStructure.PORT_EFFLUENT ? m.vat : m.depleted);
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
				TileMetalExtractor m = getMaster();
				pump.update(this, m != null && m.clientPumping);
			}
			else if(isCabinetPart())
			{
				TileMetalExtractor m = getMaster();
				plates.update(this, m != null && m.clientRunning && (m.clientPhase == PHASE_PREPARE || m.clientPhase == PHASE_RECOVER));
			}
			return;
		}
		int port = getPort();
		if(port == MetalExtractorStructure.PORT_DEPLETED)
			pushDepleted();
		else if(port == MetalExtractorStructure.PORT_DEPOSIT && world.getTotalWorldTime() % 8 == 0)
			pushDeposit();
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
		sinceFilled = vat.getFluidAmount() > lastVat ? 0 : Math.min(FILL_GRACE, sinceFilled + 1);
		boolean filling = sinceFilled < FILL_GRACE && phase == PHASE_FILL;
		lastVat = vat.getFluidAmount();
		// The pump only runs to take the liquid out of the vat (the effluent comes in by the port's own pressure)
		pumping = false;
		collecting = false;
		progressed = filling;
		noPower = outputFull = noPlates = false;
		if(world.isBlockPowered(getTriggerPos()) != redstoneInverted)
		{
			state = STATE_DISABLED;
			if(state != before)
				markDirty();
			return;
		}
		// The plates' cleaning runs beside the liquid's step
		if(collect >= 0 && collect < TRAY_OUT)
			collectTick();
		switch(phase)
		{
			case PHASE_FILL:
				// The plates go back down only into a full vat, once they are clean, and a batch needs all six
				if(vat.getFluidAmount() >= BATCH && (collect < 0 || collect >= TRAY_OUT))
				{
					if(!platesReady())
						noPlates = true;
					else
						next(collect < 0 ? PHASE_PREPARE : PHASE_LOWER);
				}
				break;
			case PHASE_LOWER:
				if(step(COLLECT_ENERGY, LOWER_TICKS))
				{
					collect = -1;
					next(PHASE_PREPARE);
				}
				break;
			case PHASE_PREPARE:
				if(step(PREPARE_ENERGY, PREPARE_TICKS))
					next(PHASE_RECOVER);
				break;
			case PHASE_RECOVER:
				if(step(Math.round(RECOVER_ENERGY * energyFactor()), RECOVER_TICKS))
				{
					finishPlates();
					// The metals are on the plates: what is left is depleted effluent; it drains while they are cleaned
					vat.setFluid(new FluidStack(INContent.fluidDepletedEffluent, vat.getFluidAmount()));
					next(PHASE_DRAIN);
					collect = 0;
				}
				break;
			case PHASE_DRAIN:
				drain();
				break;
		}
		if(progressed)
			state = STATE_RUNNING;
		else if(noPower)
			state = STATE_NO_POWER;
		else if(outputFull)
			state = STATE_OUTPUT_FULL;
		else if(noPlates)
			state = STATE_NO_PLATES;
		else
			state = phase == PHASE_FILL ? STATE_NO_INPUT : STATE_IDLE;
		if(state != before)
			markDirty();
	}

	// ---------- the electrode plates ----------
	boolean platesReady()
	{
		for(int i = 0; i < PLATES; i++)
			if(ItemElectrode.of(inventory.getStackInSlot(SLOT_PLATE0 + i)) == null)
				return false;
		return true;
	}

	/** The recovery's energy for the plates in place: the average of their materials' (1 = steel). */
	float energyFactor()
	{
		float sum = 0;
		int n = 0;
		for(int i = 0; i < PLATES; i++)
		{
			ItemElectrode e = ItemElectrode.of(inventory.getStackInSlot(SLOT_PLATE0 + i));
			if(e != null)
			{
				sum += e.energy;
				n++;
			}
		}
		return n == 0 ? 1 : sum / n;
	}

	/** End of the recovery: the deposit the plates hold (by their materials), and one batch of wear on each. */
	void finishPlates()
	{
		float yield = 0;
		for(int i = 0; i < PLATES; i++)
		{
			int slot = SLOT_PLATE0 + i;
			ItemStack plate = inventory.getStackInSlot(slot).copy();
			ItemElectrode e = ItemElectrode.of(plate);
			if(e == null)
				continue;
			yield += e.yield * DEPOSITS_PER_BATCH / PLATES;
			if(ItemElectrode.wear(plate))
			{
				inventory.setStackInSlot(slot, ItemStack.EMPTY);
				world.playSound(null, pos, net.minecraft.init.SoundEvents.ENTITY_ITEM_BREAK, net.minecraft.util.SoundCategory.BLOCKS, 0.8F, 0.8F);
			}
			else
				inventory.setStackInSlot(slot, plate);
		}
		batchDeposits = Math.round(yield);
	}

	/** 0 empty, else the plate's material tier, per plate slot. */
	int[] plateTiers()
	{
		int[] t = new int[PLATES];
		for(int i = 0; i < PLATES; i++)
		{
			ItemElectrode e = ItemElectrode.of(inventory.getStackInSlot(SLOT_PLATE0 + i));
			t[i] = e == null ? 0 : e.tier;
		}
		return t;
	}

	/** One tick of the cleaning; the deposit goes to its slot as the tray slides back (it waits there for room). */
	void collectTick()
	{
		ItemStack deposit = new ItemStack(INContent.itemMixedDeposit, Math.max(1, batchDeposits));
		if(collect == SCRAPED && batchDeposits > 0 && !canOutput(SLOT_DEPOSIT, deposit))
		{
			outputFull = true;
			return;
		}
		if(!energy.use(COLLECT_ENERGY))
		{
			noPower = true;
			return;
		}
		if(collect == SCRAPED && batchDeposits > 0)
		{
			output(SLOT_DEPOSIT, deposit);
			batchDeposits = 0;
		}
		collect++;
		collecting = progressed = true;
		markDirty();
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

	/** The pump empties the vat into the depleted effluent tank; once empty, the vat can fill again. */
	void drain()
	{
		if(vat.getFluidAmount() <= 0)
		{
			vat.setFluid(null);
			next(PHASE_FILL);
			return;
		}
		int move = Math.min(DRAIN_PER_TICK, Math.min(vat.getFluidAmount(), depleted.getCapacity() - depleted.getFluidAmount()));
		if(move <= 0)
		{
			outputFull = true;
			return;
		}
		if(!energy.use(PUMP_ENERGY))
		{
			noPower = true;
			return;
		}
		depleted.fillInternal(new FluidStack(INContent.fluidDepletedEffluent, move), true);
		vat.drainInternal(move, true);
		pumping = progressed = true;
	}

	void next(int phase)
	{
		this.phase = phase;
		phaseTicks = 0;
		markDirty();
	}

	// ---------- containers and ports ----------
	/** Bucket slots, like IE's: effluent containers are emptied into the vat (while it fills), empty ones take the depleted effluent. */
	void handleContainers()
	{
		ItemStack in = inventory.getStackInSlot(SLOT_EFFLUENT_IN);
		if(!in.isEmpty() && phase == PHASE_FILL)
		{
			FluidActionResult r = FluidUtil.tryEmptyContainer(ItemHandlerHelper.copyStackWithSize(in, 1), vat, Integer.MAX_VALUE, null, false);
			if(r.isSuccess() && canOutput(SLOT_EFFLUENT_OUT, r.getResult()))
			{
				r = FluidUtil.tryEmptyContainer(ItemHandlerHelper.copyStackWithSize(in, 1), vat, Integer.MAX_VALUE, null, true);
				output(SLOT_EFFLUENT_OUT, r.getResult());
				in.shrink(1);
				inventory.setStackInSlot(SLOT_EFFLUENT_IN, in);
			}
		}
		in = inventory.getStackInSlot(SLOT_DEPLETED_IN);
		if(!in.isEmpty() && depleted.getFluidAmount() > 0)
		{
			FluidActionResult r = FluidUtil.tryFillContainer(ItemHandlerHelper.copyStackWithSize(in, 1), depleted, Integer.MAX_VALUE, null, false);
			if(r.isSuccess() && canOutput(SLOT_DEPLETED_OUT, r.getResult()))
			{
				r = FluidUtil.tryFillContainer(ItemHandlerHelper.copyStackWithSize(in, 1), depleted, Integer.MAX_VALUE, null, true);
				output(SLOT_DEPLETED_OUT, r.getResult());
				in.shrink(1);
				inventory.setStackInSlot(SLOT_DEPLETED_IN, in);
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

	/** The depleted effluent port empties its tank into whatever accepts fluid on its face. */
	void pushDepleted()
	{
		TileMetalExtractor m = getMaster();
		EnumFacing face = portWorldFace();
		if(m == null || face == null || m.depleted.getFluidAmount() <= 0)
			return;
		TileEntity out = world.getTileEntity(pos.offset(face));
		if(out == null || !out.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite()))
			return;
		IFluidHandler target = out.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite());
		if(target == null)
			return;
		FluidStack offer = m.depleted.drainInternal(PUSH_PER_TICK, false);
		int taken = offer == null ? 0 : target.fill(offer, true);
		if(taken > 0)
			m.depleted.drainInternal(taken, true);
	}

	/** The deposit port hands the deposit to whatever takes items on its face (conveyor, hopper, chest). */
	void pushDeposit()
	{
		TileMetalExtractor m = getMaster();
		EnumFacing face = portWorldFace();
		if(m == null || face == null || m.inventory.getStackInSlot(SLOT_DEPOSIT).isEmpty())
			return;
		TileEntity out = world.getTileEntity(pos.offset(face));
		if(out == null || !out.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face.getOpposite()))
			return;
		IItemHandler target = out.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face.getOpposite());
		if(target == null)
			return;
		ItemStack one = m.inventory.extractItem(SLOT_DEPOSIT, 1, true);
		if(ItemHandlerHelper.insertItem(target, one, true).isEmpty())
			ItemHandlerHelper.insertItem(target, m.inventory.extractItem(SLOT_DEPOSIT, 1, false), false);
	}

	public void toggleRedstone()
	{
		redstoneInverted = !redstoneInverted;
		markDirty();
	}

	/** Hammer on the redstone port: invert the redstone control, like IE's machines. */
	@Override
	public boolean hammerUseSide(EnumFacing side, EntityPlayer player, float hitX, float hitY, float hitZ)
	{
		if(getPort() != MetalExtractorStructure.PORT_REDSTONE)
			return false;
		TileMetalExtractor m = getMaster();
		if(m == null)
			return false;
		if(!world.isRemote)
		{
			m.toggleRedstone();
			player.sendStatusMessage(new TextComponentTranslation(m.redstoneInverted ? "chat.immersivenuclear.redstone.inverted" : "chat.immersivenuclear.redstone.normal"), true);
		}
		return true;
	}

	/** The containers in the slots and the deposit drop; fluids and energy are lost. */
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

	// ---------- the moving parts, shared by the renderer and the interface ----------
	static float smooth(float x)
	{
		x = Math.max(0, Math.min(1, x));
		return x * x * (3 - 2 * x);
	}

	/**
	 * How far up the platform is: 0 down in the vat (as modelled), 1 raised. It rises as the collect starts, stays up
	 * (also while the vat fills again) and goes down in the LOWER step. {@code c} is the collect's tick count (-1 none).
	 */
	public static float lift(int phase, float t, float c)
	{
		if(phase == PHASE_LOWER)
			return 1 - smooth(t / LOWER_TICKS);
		if(c < 0)
			return 0;
		return c < RAISED ? smooth(c / RAISED) : 1;
	}

	/** How far the tray is in under the plates: 0 tucked under the controller (as modelled), 1 in. */
	public static float tray(float c)
	{
		if(c < RAISED || c >= TRAY_OUT)
			return 0;
		if(c < TRAY_IN)
			return smooth((c - RAISED) / (TRAY_IN - RAISED));
		if(c < SCRAPED)
			return 1;
		return 1 - smooth((c - SCRAPED) / (TRAY_OUT - SCRAPED));
	}

	/** How far the cleaner has gone down the plates and back: 0 at their top (as modelled), 1 at their bottom. */
	public static float cleaner(float c)
	{
		if(c < TRAY_IN || c >= SCRAPED)
			return 0;
		if(c < CLEANED)
			return smooth((c - TRAY_IN) / (CLEANED - TRAY_IN));
		return 1 - smooth((c - CLEANED) / (SCRAPED - CLEANED));
	}

	/** While the cleaner goes down scraping: where it is (0 top, 1 bottom); -1 otherwise. */
	public static float scraper(float c)
	{
		return c >= TRAY_IN && c < CLEANED ? cleaner(c) : -1;
	}

	/** How thick the metal on the plates is, 0..1: it grows while recovering and goes under the cleaner. */
	public static float coat(int phase, float t, float c)
	{
		if(phase == PHASE_RECOVER)
			return Math.min(1, t / RECOVER_TICKS);
		return c >= 0 && c < CLEANED ? 1 : 0;
	}

	/** Length of each timed step, in ticks (the fill and the drain go by the liquid). */
	public static int phaseLength(int phase)
	{
		switch(phase)
		{
			case PHASE_PREPARE: return PREPARE_TICKS;
			case PHASE_RECOVER: return RECOVER_TICKS;
			case PHASE_LOWER: return LOWER_TICKS;
			default: return 0;
		}
	}

	// ---------- for the interface ----------
	public IItemHandler getInventory()
	{
		return inventory;
	}

	/**
	 * vat, vat holds depleted effluent (0/1), depleted tank, energy, phase, phase ticks, state, redstone mode,
	 * collect ticks + 1 (0 = platform down), collecting this tick (0/1).
	 */
	public int[] guiValues()
	{
		FluidStack f = vat.getFluid();
		return new int[]{vat.getFluidAmount(), f != null && f.getFluid() == INContent.fluidDepletedEffluent ? 1 : 0, depleted.getFluidAmount(),
				energy.getEnergyStored(), phase, phaseTicks, state, redstoneInverted ? 1 : 0, collect + 1, collecting ? 1 : 0};
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
			vat.readFromNBT(m.getCompoundTag("vat"));
			depleted.readFromNBT(m.getCompoundTag("depleted"));
			energy.set(m.getInteger("energy"));
			phase = m.getInteger("phase");
			phaseTicks = m.getInteger("phaseTicks");
			collect = m.hasKey("collect") ? m.getInteger("collect") : -1;
			batchDeposits = m.getInteger("batchDeposits");
			// The plates only go down into a full vat
			if(phase == PHASE_LOWER && vat.getFluidAmount() < BATCH)
				phase = PHASE_FILL;
			state = m.getInteger("state");
			redstoneInverted = m.getBoolean("redstoneInverted");
			lastVat = vat.getFluidAmount();
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
			m.setTag("vat", vat.writeToNBT(new NBTTagCompound()));
			m.setTag("depleted", depleted.writeToNBT(new NBTTagCompound()));
			m.setInteger("energy", energy.getEnergyStored());
			m.setInteger("phase", phase);
			m.setInteger("phaseTicks", phaseTicks);
			m.setInteger("collect", collect);
			m.setInteger("batchDeposits", batchDeposits);
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

	/** The client needs the part (which block of the structure, how it is turned) and, on the master, the batch. */
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
			FluidStack f = vat.getFluid();
			nbt.setInteger("phase", phase);
			nbt.setInteger("phaseTicks", phaseTicks);
			nbt.setBoolean("running", state == STATE_RUNNING);
			nbt.setBoolean("pumping", pumping);
			nbt.setInteger("vat", vat.getFluidAmount());
			nbt.setBoolean("depleted", f != null && f.getFluid() == INContent.fluidDepletedEffluent);
			nbt.setInteger("collect", collect);
			nbt.setBoolean("collecting", collecting);
			nbt.setIntArray("plates", plateTiers());
		}
	}

	void readClient(NBTTagCompound nbt)
	{
		readPart(nbt);
		if(!nbt.hasKey("phase"))
			return;
		int p = nbt.getInteger("phase");
		// The client counts the ticks of a step itself; the server's count only corrects it on a new step or a drift
		if(p != clientPhase || Math.abs(nbt.getInteger("phaseTicks") - clientPhaseTicks) > 4)
			clientPhaseTicks = nbt.getInteger("phaseTicks");
		clientPhase = p;
		clientRunning = nbt.getBoolean("running");
		clientPumping = nbt.getBoolean("pumping");
		clientVat = nbt.getInteger("vat");
		clientDepleted = nbt.getBoolean("depleted");
		int c = nbt.getInteger("collect");
		if(c < 0 || clientCollect < 0 || Math.abs(c - clientCollect) > 4)
			clientCollect = c;
		clientCollecting = nbt.getBoolean("collecting");
		if(nbt.getIntArray("plates").length == PLATES)
			clientPlates = nbt.getIntArray("plates");
	}

	/**
	 * Sent when the step, the running, the pump or the cleaning change, the vat moves 250 mB, and every 2 s during a
	 * timed step or the cleaning.
	 */
	void sync()
	{
		boolean running = state == STATE_RUNNING;
		boolean dep = vat.getFluid() != null && vat.getFluid().getFluid() == INContent.fluidDepletedEffluent;
		boolean collectChanged = (collect < 0) != (sentCollect < 0) || collect >= TRAY_OUT && sentCollect < TRAY_OUT;
		int[] tiers = plateTiers();
		int plates = 0;
		for(int t : tiers)
			plates = plates * 4 + t;
		if(phase != sentPhase || running != sentRunning || pumping != sentPumping || dep != sentDepleted || Math.abs(vat.getFluidAmount() - sentVat) >= 250
				|| vat.getFluidAmount() != sentVat && (vat.getFluidAmount() == 0 || vat.getFluidAmount() == BATCH)
				|| collectChanged || collecting != sentCollecting || plates != sentPlates
				|| world.getTotalWorldTime() % 40 == 0 && (phaseLength(phase) > 0 || collecting))
		{
			sentCollect = collect;
			sentCollecting = collecting;
			sentPlates = plates;
			sentPhase = phase;
			sentRunning = running;
			sentPumping = pumping;
			sentDepleted = dep;
			sentVat = vat.getFluidAmount();
			IBlockState st = world.getBlockState(pos);
			world.notifyBlockUpdate(pos, st, st, 2);
		}
	}

	void updateClient()
	{
		if(clientRunning && clientPhaseTicks < phaseLength(clientPhase))
			clientPhaseTicks++;
		if(clientCollecting && clientCollect >= 0 && clientCollect < TRAY_OUT)
			clientCollect++;
		// Each movement of the collect plays its clip once as it starts (not the ones long past, when the chunk loads late)
		if(clientCollect < 0)
			nextMove = 0;
		else
			while(nextMove < MOVES.length && clientCollect > MOVES[nextMove])
			{
				if(clientCollect - MOVES[nextMove] < 10)
					LoopSound.play(this, moveSound(nextMove));
				nextMove++;
			}
		if(clientPhase == PHASE_LOWER && !playedLower && clientPhaseTicks < 10)
			LoopSound.play(this, INContent.soundMePlatformDown);
		playedLower = clientPhase == PHASE_LOWER;
		if(clientCollecting)
			cleaningEffects();
	}

	// ---------- client: the cleaning's effects ----------
	/** The plates in the master's model space (reference orientation), raised: x across, z along the vat, and y. */
	static final float PLATES_X0 = 0.175F, PLATES_X1 = 1.05F, PLATES_Z0 = -0.375F, PLATES_Z1 = 1.625F;
	/** The cleaner's comb, raised, at the plates' top; it goes down 1 block; the tray's floor under the plates. */
	static final float COMB_Y = 1.97F, COMB_TRAVEL = 1.0F, TRAY_FLOOR_Y = 0.55F;

	/**
	 * While the comb goes down the plates: flakes of the deposit breaking off and falling into the tray, sparks where
	 * it grinds and a little dust; as the tray slides back, the flakes tumbling into the chute.
	 */
	void cleaningEffects()
	{
		java.util.Random r = world.rand;
		float sc = scraper(clientCollect);
		int flake = net.minecraft.item.Item.getIdFromItem(INContent.itemMixedDeposit);
		if(sc >= 0)
		{
			float y = COMB_Y - COMB_TRAVEL * sc;
			for(int i = 0; i < 4; i++)
			{
				double[] p = toWorld(PLATES_X0 + r.nextFloat() * (PLATES_X1 - PLATES_X0), y - 0.05F, PLATES_Z0 + r.nextFloat() * (PLATES_Z1 - PLATES_Z0));
				world.spawnParticle(net.minecraft.util.EnumParticleTypes.ITEM_CRACK, p[0], p[1], p[2], (r.nextFloat() - 0.5) * 0.04, -0.05, (r.nextFloat() - 0.5) * 0.04, flake);
			}
			for(int i = 0; i < 2; i++)
			{
				double[] p = toWorld(PLATES_X0 + r.nextFloat() * (PLATES_X1 - PLATES_X0), y, PLATES_Z0 + r.nextFloat() * (PLATES_Z1 - PLATES_Z0));
				world.spawnParticle(net.minecraft.util.EnumParticleTypes.CRIT, p[0], p[1], p[2], (r.nextFloat() - 0.5) * 0.3, r.nextFloat() * 0.15, (r.nextFloat() - 0.5) * 0.3);
			}
			if(r.nextInt(3) == 0)
			{
				double[] p = toWorld(PLATES_X0 + r.nextFloat() * (PLATES_X1 - PLATES_X0), TRAY_FLOOR_Y + 0.1F, PLATES_Z0 + r.nextFloat() * (PLATES_Z1 - PLATES_Z0));
				world.spawnParticle(net.minecraft.util.EnumParticleTypes.SMOKE_NORMAL, p[0], p[1], p[2], 0, 0.02, 0);
			}
		}
		else if(clientCollect >= SCRAPED && clientCollect < TRAY_OUT && r.nextBoolean())
		{
			// The tray tips its load at the controller's end
			double[] p = toWorld(PLATES_X1 + 0.3F + r.nextFloat() * 0.4F, TRAY_FLOOR_Y, PLATES_Z0 + r.nextFloat() * (PLATES_Z1 - PLATES_Z0));
			world.spawnParticle(net.minecraft.util.EnumParticleTypes.ITEM_CRACK, p[0], p[1], p[2], 0.03, -0.08, 0, flake);
		}
	}

	/** World position of a point in the master's model space: mirrored, then turned like the blockstate. */
	double[] toWorld(float x, float y, float z)
	{
		double mz = mirrored ? 1 - z : z;
		double a = Math.toRadians(-turnDegrees(facing));
		double px = x - 0.5, pz = mz - 0.5;
		double rx = Math.cos(a) * px + Math.sin(a) * pz, rz = -Math.sin(a) * px + Math.cos(a) * pz;
		return new double[]{pos.getX() + rx + 0.5, pos.getY() + y, pos.getZ() + rz + 0.5};
	}

	/** The blockstate's turn: y 0 / 90 / 180 / 270 for north / east / south / west. */
	static float turnDegrees(EnumFacing facing)
	{
		switch(facing)
		{
			case EAST: return 90;
			case SOUTH: return 180;
			case WEST: return 270;
			default: return 0;
		}
	}

	static net.minecraft.util.SoundEvent moveSound(int move)
	{
		switch(move)
		{
			case 0: return INContent.soundMePlatformUp;
			case 1: return INContent.soundMeTrayIn;
			case 2: return INContent.soundMeCleanerDown;
			case 3: return INContent.soundMeCleanerUp;
			default: return INContent.soundMeTrayOut;
		}
	}

	// ---------- client: sound ----------
	boolean isPumpPart()
	{
		return dx == PUMP_DX && dy == PUMP_DY && dz == PUMP_DZ;
	}

	boolean isCabinetPart()
	{
		return dx == CABINET_DX && dy == CABINET_DY && dz == CABINET_DZ;
	}

	/** IE's looping tile sound asks this every 2 s and ends the loop when it is false. */
	@Override
	public boolean shoudlPlaySound(String sound)
	{
		if(isPumpPart())
			return pump.shouldPlay(this, sound);
		return isCabinetPart() && plates.shouldPlay(this, sound);
	}

	void stopSound()
	{
		if(world == null || !world.isRemote)
			return;
		if(isPumpPart())
			pump.stop(this);
		else if(isCabinetPart())
			plates.stop(this);
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

	/** The master draws the whole machine; the platform's connectors stand almost a block above the structure. */
	@Override
	public AxisAlignedBB getRenderBoundingBox()
	{
		return isMaster() ? new AxisAlignedBB(pos.add(-3, -1, -3), pos.add(4, 5, 4)) : super.getRenderBoundingBox();
	}
}
