package immersivenuclear.common.block;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IHammerInteraction;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.ISoundTile;
import immersivenuclear.common.INContent;
import immersivenuclear.common.multiblock.MultiblockPrecipitationUnit;
import immersivenuclear.common.multiblock.PrecipitationUnitStructure;
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
 * One part of a formed Precipitation Unit: which template block it replaced and how the structure is turned.
 * The master part also holds the machine; the port parts hand their capability, on their own face only, to the
 * master (see {@link PrecipitationUnitStructure#PORTS}).
 *
 * Batches, in three steps. 1 fill: the solution port (or a bucket) fills the agitated tank straight, one batch.
 * 2 precipitate: the precipitating agent is dosed (it waits in its slot until the tank is full) and the agitator mixes;
 * the uranium comes out of the solution as a solid. 3 dewater: the slurry drops from the conical bottom into the screw,
 * which carries the solid up to the discharge head (wet concentrate, item) while the liquid runs back down and leaves
 * as chemical effluent. Without power the batch waits where it is; a redstone signal stops it (the hammer inverts that).
 */
public class TilePrecipitationUnit extends TileEntity implements ITickable, IHammerInteraction, ISoundTile
{
	// Provisional values
	/** The agitated tank holds 12000 mB (the user's choice); a batch is the full tank. */
	public static final int BATCH_SOLUTION = 12000, AGENT_PER_BATCH = 6, CONCENTRATE_PER_BATCH = 24, EFFLUENT_PER_BATCH = 9000;
	/** Room for a whole batch's effluent and some more, like the Metal Extractor's depleted effluent tank. */
	public static final int EFFLUENT_CAPACITY = 16000, ENERGY_CAPACITY = 16000;
	public static final int PRECIPITATE_TICKS = 400, DEWATER_TICKS = 200, PUSH_PER_TICK = 100;
	public static final int PRECIPITATE_ENERGY = 48, DEWATER_ENERGY = 32;
	/** Pipes and buckets fill in spurts: the filling counts as running this long after the level last rose. */
	static final int FILL_GRACE = 20;
	public static final int PHASE_FILL = 0, PHASE_PRECIPITATE = 1, PHASE_DEWATER = 2;
	public static final int STATE_IDLE = 0, STATE_RUNNING = 1, STATE_DISABLED = 2, STATE_NO_INPUT = 3, STATE_NO_POWER = 4, STATE_OUTPUT_FULL = 5;
	public static final int SLOT_AGENT = 0, SLOT_CONCENTRATE = 1, SLOT_SOLUTION_IN = 2, SLOT_SOLUTION_OUT = 3, SLOT_EFFLUENT_IN = 4, SLOT_EFFLUENT_OUT = 5,
			SLOTS = 6;

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
				case SLOT_AGENT:
					return stack.getItem() == INContent.itemPrecipitatingAgent;
				case SLOT_SOLUTION_IN:
					FluidStack in = FluidUtil.getFluidContained(stack);
					return in != null && in.getFluid() == INContent.fluidPurifiedSolution;
				case SLOT_EFFLUENT_IN:
					FluidStack held = FluidUtil.getFluidContained(stack);
					return FluidUtil.getFluidHandler(stack) != null && (held == null || held.getFluid() == INContent.fluidEffluent);
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
	/**
	 * The agitated tank, the machine's only input tank: the solution port and the buckets fill it with one batch of
	 * purified solution, only while it fills; after that it holds the slurry until the screw empties it.
	 */
	final FluidTank mixer = new FluidTank(BATCH_SOLUTION)
	{
		@Override
		public boolean canFillFluidType(FluidStack f)
		{
			return f != null && f.getFluid() == INContent.fluidPurifiedSolution;
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
	final FluidTank effluent = new FluidTank(EFFLUENT_CAPACITY)
	{
		@Override
		protected void onContentsChanged()
		{
			markDirty();
		}
	};
	final Energy energy = new Energy();
	int phase = PHASE_FILL, phaseTicks;
	int state = STATE_IDLE;
	/** What happened this tick, for the state. */
	boolean progressed, noPower, outputFull, noInput;
	boolean redstoneInverted;
	/** Tank level last tick, to tell when it is filling; ticks since it last rose. */
	int lastMixer, sinceFilled = FILL_GRACE;
	int sentPhase = -1, sentMixer = -1;
	boolean sentRunning;

	// ---------- client ----------
	public int clientPhase = PHASE_FILL, clientPhaseTicks, clientMixer;
	public boolean clientRunning;
	/** The agitator: its turn in degrees and its speed (0 stopped .. 1 full), easing up and down; the level shown, easing. */
	public float rotorAngle, rotorSpeed, shownLevel;
	/** Full speed: 90 rpm. */
	public static final float ROTOR_DEGREES_PER_TICK = 27;
	/** The agitator turns from its motor on the tank's lid (the generator); the screw sounds from its ramp. */
	static final int AGITATOR_DX = 2, AGITATOR_DY = 3, AGITATOR_DZ = -5, SCREW_DX = 2, SCREW_DY = 1, SCREW_DZ = -2;
	/** The loops come in as the starts end: the CS mixer's start is 4.5 s, the screw's 1.6 s. */
	static final int AGITATOR_LOOP_DELAY = 90, SCREW_LOOP_DELAY = 30;
	final LoopSound agitator = new LoopSound(() -> INContent.soundCsMixerStart, () -> INContent.soundCsMixerLoop, () -> INContent.soundCsMixerStop,
			AGITATOR_LOOP_DELAY);
	final LoopSound screw = new LoopSound(() -> INContent.soundPuScrewStart, () -> INContent.soundPuScrewLoop, () -> INContent.soundPuScrewStop,
			SCREW_LOOP_DELAY);

	public TilePrecipitationUnit()
	{
		mixer.setCanDrain(false);
		effluent.setCanFill(false);
		effluent.setCanDrain(true);
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

	/** An item port: the agent port only takes the agent in, the concentrate port only gives the concentrate out. */
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

	final ItemPort agentPort = new ItemPort(SLOT_AGENT, true), concentratePort = new ItemPort(SLOT_CONCENTRATE, false);

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
		return PrecipitationUnitStructure.get(dx, dy, dz);
	}

	public int getPort()
	{
		return PrecipitationUnitStructure.portAt(dx, dy, dz);
	}

	/** What a broken part drops: the block it replaced (a double slab is two slabs). */
	public ItemStack getOriginalStack()
	{
		ItemStack s = MultiblockPrecipitationUnit.stackFor(getPartCode()).copy();
		if(getPartCode() == 'D')
			s.setCount(2);
		return s;
	}

	public boolean isMaster()
	{
		return dx == PrecipitationUnitStructure.MASTER_DX && dy == PrecipitationUnitStructure.MASTER_DY && dz == PrecipitationUnitStructure.MASTER_DZ;
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
		int[] w = PrecipitationUnitStructure.rotate(dx, dz, MultiblockPrecipitationUnit.turns(facing), mirrored);
		return pos.add(-w[0], -dy, -w[1]);
	}

	public BlockPos getMasterPos()
	{
		return MultiblockPrecipitationUnit.toWorld(getTriggerPos(), facing, mirrored,
				PrecipitationUnitStructure.MASTER_DX, PrecipitationUnitStructure.MASTER_DY, PrecipitationUnitStructure.MASTER_DZ);
	}

	public TilePrecipitationUnit getMaster()
	{
		if(isMaster())
			return this;
		if(world == null)
			return null;
		BlockPos m = getMasterPos();
		if(!world.isBlockLoaded(m))
			return null;
		TileEntity te = world.getTileEntity(m);
		return te instanceof TilePrecipitationUnit && ((TilePrecipitationUnit)te).isMaster() ? (TilePrecipitationUnit)te : null;
	}

	/** World face a port works on, or null when it works on every side. */
	EnumFacing portWorldFace()
	{
		int[] f = PrecipitationUnitStructure.portFace(dx, dy, dz);
		if(f == null || f[0] == 0 && f[1] == 0)
			return null;
		int[] w = PrecipitationUnitStructure.rotate(f[0], f[1], MultiblockPrecipitationUnit.turns(facing), mirrored);
		return EnumFacing.getFacingFromVector(w[0], 0, w[1]);
	}

	boolean portServes(Capability<?> cap, EnumFacing side)
	{
		int port = getPort();
		boolean right;
		if(cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
			right = port == PrecipitationUnitStructure.PORT_SOLUTION || port == PrecipitationUnitStructure.PORT_EFFLUENT;
		else if(cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			right = port == PrecipitationUnitStructure.PORT_AGENT || port == PrecipitationUnitStructure.PORT_CONCENTRATE;
		else if(cap == CapabilityEnergy.ENERGY)
			right = port == PrecipitationUnitStructure.PORT_ENERGY;
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
			TilePrecipitationUnit m = getMaster();
			if(m != null)
			{
				int port = getPort();
				if(cap == CapabilityEnergy.ENERGY)
					return CapabilityEnergy.ENERGY.cast(m.energy);
				if(cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
					return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(port == PrecipitationUnitStructure.PORT_AGENT ? m.agentPort : m.concentratePort);
				return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(port == PrecipitationUnitStructure.PORT_SOLUTION ? m.mixer : m.effluent);
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
			TilePrecipitationUnit m = isAgitatorPart() || isScrewPart() ? getMaster() : null;
			if(isAgitatorPart())
				agitator.update(this, m != null && m.clientRunning && m.clientPhase == PHASE_PRECIPITATE);
			else if(isScrewPart())
				screw.update(this, m != null && m.clientRunning && m.clientPhase == PHASE_DEWATER);
			return;
		}
		int port = getPort();
		if(port == PrecipitationUnitStructure.PORT_EFFLUENT)
			pushEffluent();
		else if(port == PrecipitationUnitStructure.PORT_CONCENTRATE && world.getTotalWorldTime() % 8 == 0)
			pushConcentrate();
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
		sinceFilled = mixer.getFluidAmount() > lastMixer ? 0 : Math.min(FILL_GRACE, sinceFilled + 1);
		lastMixer = mixer.getFluidAmount();
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
			case PHASE_FILL:
				fill();
				break;
			case PHASE_PRECIPITATE:
				if(step(PRECIPITATE_ENERGY, PRECIPITATE_TICKS))
					next(PHASE_DEWATER);
				break;
			case PHASE_DEWATER:
				dewater();
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

	/** The port fills the tank by its own pressure; once a batch is in, the agent is dosed and the precipitation starts. */
	void fill()
	{
		if(mixer.getFluidAmount() < BATCH_SOLUTION)
		{
			if(sinceFilled < FILL_GRACE)
				progressed = true;
			else
				noInput = true;
			return;
		}
		ItemStack agent = inventory.getStackInSlot(SLOT_AGENT);
		if(agent.getCount() < AGENT_PER_BATCH)
		{
			noInput = true;
			return;
		}
		inventory.extractItem(SLOT_AGENT, AGENT_PER_BATCH, false);
		next(PHASE_PRECIPITATE);
		progressed = true;
	}

	/**
	 * The screw empties the tank evenly over the step: the liquid share goes to the effluent tank as it drains, the
	 * concentrate comes out of the discharge head at the end. It only starts with room for the whole batch's output.
	 */
	void dewater()
	{
		if(phaseTicks == 0 && (effluent.getCapacity() - effluent.getFluidAmount() < EFFLUENT_PER_BATCH
				|| !canOutput(SLOT_CONCENTRATE, new ItemStack(INContent.itemWetConcentrate, CONCENTRATE_PER_BATCH))))
		{
			outputFull = true;
			return;
		}
		if(!step(DEWATER_ENERGY, DEWATER_TICKS))
		{
			if(progressed)
				moveDewatered(phaseTicks);
			return;
		}
		moveDewatered(DEWATER_TICKS);
		output(SLOT_CONCENTRATE, new ItemStack(INContent.itemWetConcentrate, CONCENTRATE_PER_BATCH));
		mixer.setFluid(null);
		next(PHASE_FILL);
	}

	/** Brings the tank and the effluent to where they should be after {@code ticks} of the screw. */
	void moveDewatered(int ticks)
	{
		int left = BATCH_SOLUTION - BATCH_SOLUTION * ticks / DEWATER_TICKS;
		int drained = mixer.getFluidAmount() - left;
		if(drained > 0)
			mixer.drainInternal(drained, true);
		int out = EFFLUENT_PER_BATCH * ticks / DEWATER_TICKS - EFFLUENT_PER_BATCH * (ticks - 1) / DEWATER_TICKS;
		if(out > 0)
			effluent.fillInternal(new FluidStack(INContent.fluidEffluent, out), true);
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
	/** Bucket slots, like IE's: solution containers are emptied into the tank (while it fills), empty ones take the effluent. */
	void handleContainers()
	{
		ItemStack in = inventory.getStackInSlot(SLOT_SOLUTION_IN);
		if(!in.isEmpty() && phase == PHASE_FILL)
		{
			FluidActionResult r = FluidUtil.tryEmptyContainer(ItemHandlerHelper.copyStackWithSize(in, 1), mixer, Integer.MAX_VALUE, null, false);
			if(r.isSuccess() && canOutput(SLOT_SOLUTION_OUT, r.getResult()))
			{
				r = FluidUtil.tryEmptyContainer(ItemHandlerHelper.copyStackWithSize(in, 1), mixer, Integer.MAX_VALUE, null, true);
				output(SLOT_SOLUTION_OUT, r.getResult());
				in.shrink(1);
				inventory.setStackInSlot(SLOT_SOLUTION_IN, in);
			}
		}
		in = inventory.getStackInSlot(SLOT_EFFLUENT_IN);
		if(!in.isEmpty() && effluent.getFluidAmount() > 0)
		{
			FluidActionResult r = FluidUtil.tryFillContainer(ItemHandlerHelper.copyStackWithSize(in, 1), effluent, Integer.MAX_VALUE, null, false);
			if(r.isSuccess() && canOutput(SLOT_EFFLUENT_OUT, r.getResult()))
			{
				r = FluidUtil.tryFillContainer(ItemHandlerHelper.copyStackWithSize(in, 1), effluent, Integer.MAX_VALUE, null, true);
				output(SLOT_EFFLUENT_OUT, r.getResult());
				in.shrink(1);
				inventory.setStackInSlot(SLOT_EFFLUENT_IN, in);
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

	/** The effluent port empties its tank into whatever accepts fluid on its face. */
	void pushEffluent()
	{
		TilePrecipitationUnit m = getMaster();
		EnumFacing face = portWorldFace();
		if(m == null || face == null || m.effluent.getFluidAmount() <= 0)
			return;
		TileEntity out = world.getTileEntity(pos.offset(face));
		if(out == null || !out.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite()))
			return;
		IFluidHandler target = out.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite());
		if(target == null)
			return;
		FluidStack offer = m.effluent.drainInternal(PUSH_PER_TICK, false);
		int taken = offer == null ? 0 : target.fill(offer, true);
		if(taken > 0)
			m.effluent.drainInternal(taken, true);
	}

	/** The concentrate port hands the concentrate to whatever takes items on its face (conveyor, hopper, chest). */
	void pushConcentrate()
	{
		TilePrecipitationUnit m = getMaster();
		EnumFacing face = portWorldFace();
		if(m == null || face == null || m.inventory.getStackInSlot(SLOT_CONCENTRATE).isEmpty())
			return;
		TileEntity out = world.getTileEntity(pos.offset(face));
		if(out == null || !out.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face.getOpposite()))
			return;
		IItemHandler target = out.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face.getOpposite());
		if(target == null)
			return;
		ItemStack one = m.inventory.extractItem(SLOT_CONCENTRATE, 1, true);
		if(ItemHandlerHelper.insertItem(target, one, true).isEmpty())
			ItemHandlerHelper.insertItem(target, m.inventory.extractItem(SLOT_CONCENTRATE, 1, false), false);
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
		if(getPort() != PrecipitationUnitStructure.PORT_REDSTONE)
			return false;
		TilePrecipitationUnit m = getMaster();
		if(m == null)
			return false;
		if(!world.isRemote)
		{
			m.toggleRedstone();
			player.sendStatusMessage(new TextComponentTranslation(m.redstoneInverted ? "chat.immersivenuclear.redstone.inverted" : "chat.immersivenuclear.redstone.normal"), true);
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

	/** agitated tank, effluent tank, energy, phase, phase ticks, state, redstone mode. */
	public int[] guiValues()
	{
		return new int[]{mixer.getFluidAmount(), effluent.getFluidAmount(), energy.getEnergyStored(), phase, phaseTicks, state,
				redstoneInverted ? 1 : 0};
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
			mixer.readFromNBT(m.getCompoundTag("mixer"));
			effluent.readFromNBT(m.getCompoundTag("effluent"));
			energy.set(m.getInteger("energy"));
			phase = m.getInteger("phase");
			phaseTicks = m.getInteger("phaseTicks");
			state = m.getInteger("state");
			redstoneInverted = m.getBoolean("redstoneInverted");
			lastMixer = mixer.getFluidAmount();
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
			m.setTag("mixer", mixer.writeToNBT(new NBTTagCompound()));
			m.setTag("effluent", effluent.writeToNBT(new NBTTagCompound()));
			m.setInteger("energy", energy.getEnergyStored());
			m.setInteger("phase", phase);
			m.setInteger("phaseTicks", phaseTicks);
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
			nbt.setBoolean("running", state == STATE_RUNNING);
			nbt.setInteger("mixer", mixer.getFluidAmount());
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
			clientMixer = nbt.getInteger("mixer");
		}
	}

	/**
	 * Sent when the step or the running change (the parts' sounds and the agitator follow them), the tank moves 100 mB
	 * or ends full or empty, and every 2 s during a timed step.
	 */
	void sync()
	{
		boolean running = state == STATE_RUNNING;
		int level = mixer.getFluidAmount();
		if(phase != sentPhase || running != sentRunning || Math.abs(level - sentMixer) >= 100
				|| level != sentMixer && (level == 0 || level == BATCH_SOLUTION)
				|| world.getTotalWorldTime() % 40 == 0 && phase != PHASE_FILL && running)
		{
			sentPhase = phase;
			sentRunning = running;
			sentMixer = level;
			IBlockState st = world.getBlockState(pos);
			world.notifyBlockUpdate(pos, st, st, 2);
		}
	}

	/** Length of each timed step, in ticks (the fill goes by the liquid). */
	public static int phaseLength(int phase)
	{
		return phase == PHASE_PRECIPITATE ? PRECIPITATE_TICKS : phase == PHASE_DEWATER ? DEWATER_TICKS : 0;
	}

	/** How far the uranium has come out of the solution, 0..1 (all of it once the screw takes the slurry). */
	public static float precipitated(int phase, float ticks)
	{
		if(phase == PHASE_DEWATER)
			return 1;
		return phase == PHASE_PRECIPITATE ? Math.min(1, ticks / PRECIPITATE_TICKS) : 0;
	}

	/** Master, client side, every tick: the step's clock and the agitator, which spins up and down in about 2 s. */
	void updateClient()
	{
		if(clientRunning && clientPhaseTicks < phaseLength(clientPhase))
			clientPhaseTicks++;
		float target = clientRunning && clientPhase == PHASE_PRECIPITATE ? 1 : 0;
		rotorSpeed += Math.signum(target - rotorSpeed) * Math.min(Math.abs(target - rotorSpeed), 0.025F);
		rotorAngle = (rotorAngle + rotorSpeed * ROTOR_DEGREES_PER_TICK) % 360;
	}

	// ---------- client: sound ----------
	boolean isAgitatorPart()
	{
		return dx == AGITATOR_DX && dy == AGITATOR_DY && dz == AGITATOR_DZ;
	}

	boolean isScrewPart()
	{
		return dx == SCREW_DX && dy == SCREW_DY && dz == SCREW_DZ;
	}

	/** IE's looping tile sound asks this every 2 s and ends the loop when it is false. */
	@Override
	public boolean shoudlPlaySound(String sound)
	{
		if(isAgitatorPart())
			return agitator.shouldPlay(this, sound);
		return isScrewPart() && screw.shouldPlay(this, sound);
	}

	void stopSound()
	{
		if(world == null || !world.isRemote)
			return;
		if(isAgitatorPart())
			agitator.stop(this);
		else if(isScrewPart())
			screw.stop(this);
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

	/** The master draws the agitator and the liquid inside the tank, around it (any turn of the structure). */
	@Override
	public AxisAlignedBB getRenderBoundingBox()
	{
		return isMaster() ? new AxisAlignedBB(pos.add(-3, -1, -3), pos.add(4, 3, 4)) : super.getRenderBoundingBox();
	}
}
