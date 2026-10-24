package immersivenuclear.common.block;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IHammerInteraction;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.ISoundTile;
import immersivenuclear.common.INContent;
import immersivenuclear.common.multiblock.ChemicalSeparatorStructure;
import immersivenuclear.common.multiblock.MultiblockChemicalSeparator;
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
import net.minecraftforge.fluids.Fluid;
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
 * One part of a formed Chemical Separation Unit: which template block it replaced and how the structure is turned.
 * The master part also holds the machine; the port parts hand their capability, on their own face only, to the
 * master's tanks (see {@link ChemicalSeparatorStructure#PORTS}).
 *
 * Solvent extraction in two mixer-settler stages, with the solvent going round between them. The mixers work
 * continuously; each settler works in batches (fill, settle, drain) and the mixer in front of it waits while it settles.
 * 1 extraction: mixer A mixes crude solution with clean solvent from the reservoir into settler A. Settled, the
 * loaded solvent (with the uranium) floats and the chemical effluent sinks; the effluent goes to its tank and the
 * loaded solvent to the loaded solvent line.
 * 2 stripping: mixer B mixes the loaded solvent with stripping solution into settler B, which takes the uranium back.
 * Settled, the purified solution sinks and goes to its tank; the clean solvent floats and goes back to the reservoir,
 * less a small loss that the solvent port tops up.
 * Without stripping solution, stage 1 goes on until the loaded solvent line is full.
 *
 * Sounds: each mixer and each pump has its own, played from the part where it stands in the model (IE keeps one
 * looping sound per block): the two mixers while they mix, the pump of each settler while it drains, and the middle
 * pump while the loaded solvent goes from settler A to its line (see {@link #SOUND_PARTS}).
 */
public class TileChemicalSeparator extends TileEntity implements ITickable, IHammerInteraction, ISoundTile
{
	public static final int TANK_CAPACITY = 12000, SETTLER_CAPACITY = 16000, ENERGY_CAPACITY = 32000;
	/** One batch of each settler (provisional): A takes 10000 crude + 6000 solvent, B 6000 loaded solvent + 5000 stripping. */
	public static final int A_AQUEOUS = 10000, A_SOLVENT = 6000, B_SOLVENT = 6000, B_AQUEOUS = 5000;
	/**
	 * The solvent reservoir is a surge tank: the solvent port only tops it up to MAKEUP_LEVEL (one batch of mixer A), the
	 * rest is kept free for the clean solvent coming back from settler B. With the port allowed to fill it up, the
	 * returning solvent had nowhere to go, settler B stopped, the loaded line filled and settler A stopped too.
	 */
	public static final int SOLVENT_CAPACITY = TANK_CAPACITY + B_SOLVENT, MAKEUP_LEVEL = A_SOLVENT;
	/** Solvent lost per stripping batch (about 50 mB per 1000 mB of crude solution). */
	public static final int SOLVENT_LOSS = 500;
	/** Mixer feed per tick, in the batch's proportions (A fills in 400 ticks, B in 250). */
	public static final int A_AQUEOUS_RATE = 25, A_SOLVENT_RATE = 15, B_SOLVENT_RATE = 24, B_AQUEOUS_RATE = 20;
	public static final int SETTLE_TICKS = 1200, DRAIN_PER_TICK = 100, PUSH_PER_TICK = 100;
	public static final int MIXER_ENERGY = 32, PUMP_ENERGY = 16;
	public static final int STATE_IDLE = 0, STATE_RUNNING = 1, STATE_DISABLED = 2, STATE_NO_INPUT = 3, STATE_NO_POWER = 4, STATE_OUTPUT_FULL = 5;
	public static final int PHASE_FILL = 0, PHASE_SETTLE = 1, PHASE_DRAIN = 2;
	/** Bucket slots: a full container in and the empty one back for each input tank, an empty one in and the full one out for each output tank. */
	public static final int SLOT_CRUDE_IN = 0, SLOT_CRUDE_OUT = 1, SLOT_STRIPPING_IN = 2, SLOT_STRIPPING_OUT = 3,
			SLOT_EFFLUENT_IN = 4, SLOT_EFFLUENT_OUT = 5, SLOT_PURIFIED_IN = 6, SLOT_PURIFIED_OUT = 7, SLOTS = 8;

	int dx, dy, dz;
	EnumFacing facing = EnumFacing.NORTH;
	boolean mirrored;
	/** The block this part replaced (state + tile NBT), put back on disassembly. */
	IBlockState originalState;
	NBTTagCompound originalNbt;

	// ---------- master only ----------
	final FluidTank crude = input(TANK_CAPACITY, () -> INContent.fluidCrudeSolution);
	final FluidTank stripping = input(TANK_CAPACITY, () -> INContent.fluidStrippingSolution);
	/** Solvent reservoir: the initial charge, the top-ups (up to MAKEUP_LEVEL) and the clean solvent coming back from settler B. */
	final FluidTank solvent = new FluidTank(SOLVENT_CAPACITY)
	{
		@Override
		public boolean canFillFluidType(FluidStack f)
		{
			return f != null && f.getFluid() == INContent.fluidSolvent;
		}

		/** From outside (the solvent port) only up to the make-up level; the return from settler B uses fillInternal. */
		@Override
		public int fill(FluidStack resource, boolean doFill)
		{
			int room = MAKEUP_LEVEL - getFluidAmount();
			if(resource == null || room <= 0)
				return 0;
			FluidStack part = resource.copy();
			part.amount = Math.min(part.amount, room);
			return super.fill(part, doFill);
		}

		@Override
		protected void onContentsChanged()
		{
			markDirty();
		}
	};
	final FluidTank effluent = output(TANK_CAPACITY);
	final FluidTank purified = output(TANK_CAPACITY);
	/** Loaded solvent line between settler A and mixer B (no port). */
	final FluidTank loaded = output(TANK_CAPACITY);
	final Settler settlerA = new Settler(), settlerB = new Settler();

	{
		solvent.setCanDrain(false);
	}
	final Energy energy = new Energy();
	final ItemStackHandler inventory = new ItemStackHandler(SLOTS)
	{
		@Override
		public boolean isItemValid(int slot, ItemStack stack)
		{
			FluidStack held = FluidUtil.getFluidContained(stack);
			switch(slot)
			{
				case SLOT_CRUDE_IN: return held != null && held.getFluid() == INContent.fluidCrudeSolution;
				case SLOT_STRIPPING_IN: return held != null && held.getFluid() == INContent.fluidStrippingSolution;
				case SLOT_EFFLUENT_IN: return FluidUtil.getFluidHandler(stack) != null && (held == null || held.getFluid() == INContent.fluidEffluent);
				case SLOT_PURIFIED_IN: return FluidUtil.getFluidHandler(stack) != null && (held == null || held.getFluid() == INContent.fluidPurifiedSolution);
				default: return false; // outputs only take what the machine puts there
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
	int state = STATE_IDLE;
	/** IE convention: a redstone signal stops the machine; hammering the redstone port inverts that. */
	boolean redstoneInverted;
	/** What moved this tick, for the interface (impellers turning, flow lights) and the pump sound. */
	boolean mixingA, mixingB, flowLoaded, flowClean, flowEffluent, flowPurified;
	/** What the client last got for the windows of the model. */
	int sentWindows = Integer.MIN_VALUE;

	// ---------- client only: the settlers seen through the windows of the model, {phase, ticks, top, bottom} ----------
	public int[] clientSettlerA = new int[4], clientSettlerB = new int[4];
	/** What moved last tick, as the bits of {@link #moving()}. */
	public int clientMoving;

	/** What moves: the mixers, and the fluid in each line (also the interface's flow lights). */
	public static final int MOVE_MIXER_A = 1, MOVE_MIXER_B = 2, MOVE_LOADED = 4, MOVE_CLEAN = 8, MOVE_EFFLUENT = 16, MOVE_PURIFIED = 32;
	/**
	 * The parts that play a sound, where that mixer or pump stands in the model, and what runs it: {dx, dy, dz, bits of
	 * moving()}. Settler A is the left (north) one.
	 */
	static final int[][] SOUND_PARTS = {
			{3, 2, -1, MOVE_MIXER_A},                // mixer A, under its generator
			{3, 2, 1, MOVE_MIXER_B},                 // mixer B
			{2, 1, -2, MOVE_EFFLUENT},               // pump of settler A: the effluent out
			{2, 1, 2, MOVE_PURIFIED | MOVE_CLEAN},   // pump of settler B: the purified solution out, the clean solvent back
			{3, 0, 0, MOVE_LOADED},                  // middle pump: the loaded solvent from settler A to mixer B's line
	};
	/** The mixer's loop comes in as its 4.55 s start-up ends, less the 65 ms crossfade of the recording. */
	static final int MIXER_LOOP_DELAY = 90;
	/** This part's sound (client), made on its first tick when the part has one. */
	LoopSound sound;

	/** A settling basin: its step, the ticks in it, and how much of each layer it holds (solvent on top, water below). */
	static class Settler
	{
		int phase = PHASE_FILL, ticks, top, bottom;

		int total()
		{
			return top + bottom;
		}

		NBTTagCompound write()
		{
			NBTTagCompound n = new NBTTagCompound();
			n.setInteger("phase", phase);
			n.setInteger("ticks", ticks);
			n.setInteger("top", top);
			n.setInteger("bottom", bottom);
			return n;
		}

		void read(NBTTagCompound n)
		{
			phase = n.getInteger("phase");
			ticks = n.getInteger("ticks");
			top = n.getInteger("top");
			bottom = n.getInteger("bottom");
		}
	}

	FluidTank input(int capacity, java.util.function.Supplier<Fluid> accepted)
	{
		FluidTank t = new FluidTank(capacity)
		{
			@Override
			public boolean canFillFluidType(FluidStack f)
			{
				return f != null && f.getFluid() == accepted.get();
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

	FluidTank output(int capacity)
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
		return ChemicalSeparatorStructure.get(dx, dy, dz);
	}

	public int getPort()
	{
		return ChemicalSeparatorStructure.portAt(dx, dy, dz);
	}

	public ItemStack getOriginalStack()
	{
		return MultiblockChemicalSeparator.stackFor(getPartCode());
	}

	public boolean isMaster()
	{
		return dx == ChemicalSeparatorStructure.MASTER_DX && dy == ChemicalSeparatorStructure.MASTER_DY && dz == ChemicalSeparatorStructure.MASTER_DZ;
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
		int[] w = ChemicalSeparatorStructure.rotate(dx, dz, MultiblockChemicalSeparator.turns(facing), mirrored);
		return pos.add(-w[0], -dy, -w[1]);
	}

	public BlockPos getMasterPos()
	{
		return MultiblockChemicalSeparator.toWorld(getTriggerPos(), facing, mirrored,
				ChemicalSeparatorStructure.MASTER_DX, ChemicalSeparatorStructure.MASTER_DY, ChemicalSeparatorStructure.MASTER_DZ);
	}

	public TileChemicalSeparator getMaster()
	{
		if(isMaster())
			return this;
		if(world == null)
			return null;
		BlockPos m = getMasterPos();
		if(!world.isBlockLoaded(m))
			return null;
		TileEntity te = world.getTileEntity(m);
		return te instanceof TileChemicalSeparator && ((TileChemicalSeparator)te).isMaster() ? (TileChemicalSeparator)te : null;
	}

	/** World face a port works on, or null when it works on every side. */
	EnumFacing portWorldFace()
	{
		int[] f = ChemicalSeparatorStructure.portFace(dx, dy, dz);
		if(f == null || f[0] == 0 && f[1] == 0)
			return null;
		int[] w = ChemicalSeparatorStructure.rotate(f[0], f[1], MultiblockChemicalSeparator.turns(facing), mirrored);
		return EnumFacing.getFacingFromVector(w[0], 0, w[1]);
	}

	/** The master's tank behind a fluid port, or null. */
	FluidTank portTank(TileChemicalSeparator m)
	{
		switch(getPort())
		{
			case ChemicalSeparatorStructure.PORT_CRUDE: return m.crude;
			case ChemicalSeparatorStructure.PORT_STRIPPING: return m.stripping;
			case ChemicalSeparatorStructure.PORT_SOLVENT: return m.solvent;
			case ChemicalSeparatorStructure.PORT_EFFLUENT: return m.effluent;
			case ChemicalSeparatorStructure.PORT_PURIFIED: return m.purified;
			default: return null;
		}
	}

	boolean portServes(Capability<?> cap, EnumFacing side)
	{
		int port = getPort();
		boolean right;
		if(cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
			right = port >= ChemicalSeparatorStructure.PORT_CRUDE && port <= ChemicalSeparatorStructure.PORT_PURIFIED;
		else if(cap == CapabilityEnergy.ENERGY)
			right = port == ChemicalSeparatorStructure.PORT_ENERGY;
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
			TileChemicalSeparator m = getMaster();
			if(m != null)
			{
				if(cap == CapabilityEnergy.ENERGY)
					return CapabilityEnergy.ENERGY.cast(m.energy);
				return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(portTank(m));
			}
		}
		return super.getCapability(cap, side);
	}

	@Override
	public void update()
	{
		if(world.isRemote)
		{
			int bits = soundBits();
			if(bits != 0)
			{
				if(sound == null)
					sound = (bits & (MOVE_MIXER_A | MOVE_MIXER_B)) != 0 ? new LoopSound(() -> INContent.soundCsMixerStart,
							() -> INContent.soundCsMixerLoop, () -> INContent.soundCsMixerStop, MIXER_LOOP_DELAY) : new PumpSound();
				TileChemicalSeparator m = getMaster();
				sound.update(this, m != null && (m.clientMoving & bits) != 0);
			}
			return;
		}
		int port = getPort();
		if(port == ChemicalSeparatorStructure.PORT_EFFLUENT || port == ChemicalSeparatorStructure.PORT_PURIFIED)
			pushOutput();
		if(isMaster())
		{
			handleContainers();
			process();
		}
	}

	// ---------- the process ----------
	void process()
	{
		int before = state;
		mixingA = mixingB = flowLoaded = flowClean = flowEffluent = flowPurified = false;
		if(world.isBlockPowered(getTriggerPos()) != redstoneInverted)
		{
			state = STATE_DISABLED;
			if(state != before)
				markDirty();
			syncWindows();
			return;
		}
		boolean noPower = false, full = false;

		// Stage 1: mixer A feeds settler A with crude solution and clean solvent
		Settler a = settlerA;
		if(a.phase == PHASE_FILL)
		{
			int needAq = Math.min(A_AQUEOUS_RATE, A_AQUEOUS - a.bottom), needSol = Math.min(A_SOLVENT_RATE, A_SOLVENT - a.top);
			if(crude.getFluidAmount() >= needAq && solvent.getFluidAmount() >= needSol)
			{
				if(energy.use(MIXER_ENERGY))
				{
					crude.drainInternal(needAq, true);
					solvent.drainInternal(needSol, true);
					a.bottom += needAq;
					a.top += needSol;
					mixingA = true;
					if(a.bottom >= A_AQUEOUS && a.top >= A_SOLVENT)
						next(a, PHASE_SETTLE);
				}
				else
					noPower = true;
			}
		}
		else if(a.phase == PHASE_SETTLE)
		{
			if(++a.ticks >= SETTLE_TICKS)
				next(a, PHASE_DRAIN);
		}
		else
		{
			int[] moved = drain(a, effluent, INContent.fluidEffluent, loaded, INContent.fluidSolvent);
			if(moved == null)
				noPower = true;
			else
			{
				flowEffluent = moved[0] > 0;
				flowLoaded = moved[1] > 0;
				full |= moved[0] + moved[1] == 0;
			}
		}

		// Stage 2: mixer B feeds settler B with the loaded solvent and stripping solution
		Settler b = settlerB;
		if(b.phase == PHASE_FILL)
		{
			int needSol = Math.min(B_SOLVENT_RATE, B_SOLVENT - b.top), needAq = Math.min(B_AQUEOUS_RATE, B_AQUEOUS - b.bottom);
			if(loaded.getFluidAmount() >= needSol && stripping.getFluidAmount() >= needAq)
			{
				if(energy.use(MIXER_ENERGY))
				{
					loaded.drainInternal(needSol, true);
					stripping.drainInternal(needAq, true);
					b.top += needSol;
					b.bottom += needAq;
					mixingB = true;
					if(b.top >= B_SOLVENT && b.bottom >= B_AQUEOUS)
					{
						// The solvent lost by the batch (carried away in the water, evaporated)
						b.top -= SOLVENT_LOSS;
						next(b, PHASE_SETTLE);
					}
				}
				else
					noPower = true;
			}
		}
		else if(b.phase == PHASE_SETTLE)
		{
			if(++b.ticks >= SETTLE_TICKS)
				next(b, PHASE_DRAIN);
		}
		else
		{
			int[] moved = drain(b, purified, INContent.fluidPurifiedSolution, solvent, INContent.fluidSolvent);
			if(moved == null)
				noPower = true;
			else
			{
				flowPurified = moved[0] > 0;
				flowClean = moved[1] > 0;
				full |= moved[0] + moved[1] == 0;
			}
		}

		if(pumping() || a.phase == PHASE_SETTLE || b.phase == PHASE_SETTLE)
			state = STATE_RUNNING;
		else if(noPower)
			state = STATE_NO_POWER;
		else if(full)
			state = STATE_OUTPUT_FULL;
		else
			state = STATE_NO_INPUT;
		if(state != before || pumping())
			markDirty();
		syncWindows();
	}

	/** The windows show the settlers' layers: send them when they change visibly (levels in 1/64, settling in 1/32). */
	void syncWindows()
	{
		int key = moving();
		for(Settler s : new Settler[]{settlerA, settlerB})
			key = key * 31 + s.phase * 1000003 + s.top * 64 / SETTLER_CAPACITY * 1009 + s.bottom * 64 / SETTLER_CAPACITY * 101 + s.ticks * 32 / SETTLE_TICKS;
		if(key != sentWindows)
		{
			sentWindows = key;
			IBlockState st = world.getBlockState(pos);
			world.notifyBlockUpdate(pos, st, st, 2);
		}
	}

	/** Bits: MOVE_MIXER_A, MOVE_MIXER_B, MOVE_LOADED, MOVE_CLEAN, MOVE_EFFLUENT, MOVE_PURIFIED. */
	int moving()
	{
		return (mixingA ? MOVE_MIXER_A : 0) | (mixingB ? MOVE_MIXER_B : 0) | (flowLoaded ? MOVE_LOADED : 0) | (flowClean ? MOVE_CLEAN : 0)
				| (flowEffluent ? MOVE_EFFLUENT : 0) | (flowPurified ? MOVE_PURIFIED : 0);
	}

	/** Fluid really moving: a settler waiting with its outlets full is not pumping. */
	boolean pumping()
	{
		return mixingA || mixingB || flowLoaded || flowClean || flowEffluent || flowPurified;
	}

	static int[] settlerArray(Settler s)
	{
		return new int[]{s.phase, s.ticks, s.top, s.bottom};
	}

	void next(Settler s, int phase)
	{
		s.phase = phase;
		s.ticks = 0;
	}

	/**
	 * Draining a settled basin: the bottom layer to its tank, the top layer to its line, both at once as far as they
	 * have room. Returns {bottom moved, top moved}, or null without power. Empty, the basin fills again.
	 */
	int[] drain(Settler s, FluidTank bottomTo, Fluid bottomFluid, FluidTank topTo, Fluid topFluid)
	{
		if(!energy.use(PUMP_ENERGY))
			return null;
		int down = s.bottom > 0 ? bottomTo.fillInternal(new FluidStack(bottomFluid, Math.min(DRAIN_PER_TICK, s.bottom)), true) : 0;
		int up = s.top > 0 ? topTo.fillInternal(new FluidStack(topFluid, Math.min(DRAIN_PER_TICK, s.top)), true) : 0;
		s.bottom -= down;
		s.top -= up;
		if(s.total() == 0)
			next(s, PHASE_FILL);
		return new int[]{down, up};
	}

	// ---------- containers and ports ----------
	/** Bucket slots, like IE's: full containers are emptied into the input tanks, empty ones are filled from the output tanks. */
	void handleContainers()
	{
		emptyInto(SLOT_CRUDE_IN, SLOT_CRUDE_OUT, crude);
		emptyInto(SLOT_STRIPPING_IN, SLOT_STRIPPING_OUT, stripping);
		fillFrom(SLOT_EFFLUENT_IN, SLOT_EFFLUENT_OUT, effluent);
		fillFrom(SLOT_PURIFIED_IN, SLOT_PURIFIED_OUT, purified);
	}

	void emptyInto(int in, int out, FluidTank tank)
	{
		ItemStack s = inventory.getStackInSlot(in);
		if(s.isEmpty())
			return;
		FluidActionResult r = FluidUtil.tryEmptyContainer(ItemHandlerHelper.copyStackWithSize(s, 1), tank, Integer.MAX_VALUE, null, false);
		if(r.isSuccess() && canOutput(out, r.getResult()))
		{
			r = FluidUtil.tryEmptyContainer(ItemHandlerHelper.copyStackWithSize(s, 1), tank, Integer.MAX_VALUE, null, true);
			output(out, r.getResult());
			s.shrink(1);
			inventory.setStackInSlot(in, s);
		}
	}

	void fillFrom(int in, int out, FluidTank tank)
	{
		ItemStack s = inventory.getStackInSlot(in);
		if(s.isEmpty() || tank.getFluidAmount() <= 0)
			return;
		FluidActionResult r = FluidUtil.tryFillContainer(ItemHandlerHelper.copyStackWithSize(s, 1), tank, Integer.MAX_VALUE, null, false);
		if(r.isSuccess() && canOutput(out, r.getResult()))
		{
			r = FluidUtil.tryFillContainer(ItemHandlerHelper.copyStackWithSize(s, 1), tank, Integer.MAX_VALUE, null, true);
			output(out, r.getResult());
			s.shrink(1);
			inventory.setStackInSlot(in, s);
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

	/** An output port empties its tank into whatever accepts fluid on its face. */
	void pushOutput()
	{
		TileChemicalSeparator m = getMaster();
		EnumFacing face = portWorldFace();
		FluidTank tank = m == null ? null : portTank(m);
		if(tank == null || face == null || tank.getFluidAmount() <= 0)
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

	/** Hammer on the redstone port: invert the redstone control, like IE's machines. */
	@Override
	public boolean hammerUseSide(EnumFacing side, EntityPlayer player, float hitX, float hitY, float hitZ)
	{
		if(getPort() != ChemicalSeparatorStructure.PORT_REDSTONE)
			return false;
		TileChemicalSeparator m = getMaster();
		if(m == null)
			return false;
		if(!world.isRemote)
		{
			m.toggleRedstone();
			player.sendStatusMessage(new TextComponentTranslation(m.redstoneInverted ? "chat.immersivenuclear.redstone.inverted" : "chat.immersivenuclear.redstone.normal"), true);
		}
		return true;
	}

	/** The containers in the bucket slots drop; fluids and energy are lost. */
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

	/**
	 * Everything the interface shows, as small ints (window properties): the six tanks, energy, state, redstone mode,
	 * both settlers (phase, ticks, top, bottom), and what moved this tick (bits: mixer A, mixer B, loaded line, clean line,
	 * effluent outlet, purified outlet).
	 */
	public int[] guiValues()
	{
		return new int[]{crude.getFluidAmount(), stripping.getFluidAmount(), solvent.getFluidAmount(), effluent.getFluidAmount(),
				purified.getFluidAmount(), loaded.getFluidAmount(), energy.getEnergyStored(), state, redstoneInverted ? 1 : 0,
				settlerA.phase, settlerA.ticks, settlerA.top, settlerA.bottom, settlerB.phase, settlerB.ticks, settlerB.top, settlerB.bottom,
				moving()};
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
			crude.readFromNBT(m.getCompoundTag("crude"));
			stripping.readFromNBT(m.getCompoundTag("stripping"));
			solvent.readFromNBT(m.getCompoundTag("solvent"));
			effluent.readFromNBT(m.getCompoundTag("effluent"));
			purified.readFromNBT(m.getCompoundTag("purified"));
			loaded.readFromNBT(m.getCompoundTag("loaded"));
			settlerA.read(m.getCompoundTag("settlerA"));
			settlerB.read(m.getCompoundTag("settlerB"));
			energy.set(m.getInteger("energy"));
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
			m.setTag("crude", crude.writeToNBT(new NBTTagCompound()));
			m.setTag("stripping", stripping.writeToNBT(new NBTTagCompound()));
			m.setTag("solvent", solvent.writeToNBT(new NBTTagCompound()));
			m.setTag("effluent", effluent.writeToNBT(new NBTTagCompound()));
			m.setTag("purified", purified.writeToNBT(new NBTTagCompound()));
			m.setTag("loaded", loaded.writeToNBT(new NBTTagCompound()));
			m.setTag("settlerA", settlerA.write());
			m.setTag("settlerB", settlerB.write());
			m.setInteger("energy", energy.getEnergyStored());
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

	/** The client needs the part (which block of the structure, how it is turned) and, on the master, the settlers. */
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
			nbt.setIntArray("settlerA", settlerArray(settlerA));
			nbt.setIntArray("settlerB", settlerArray(settlerB));
			nbt.setInteger("moving", moving());
		}
	}

	void readClient(NBTTagCompound nbt)
	{
		readPart(nbt);
		if(nbt.getIntArray("settlerA").length == 4)
			clientSettlerA = nbt.getIntArray("settlerA");
		if(nbt.getIntArray("settlerB").length == 4)
			clientSettlerB = nbt.getIntArray("settlerB");
		clientMoving = nbt.getInteger("moving");
	}

	// ---------- client: sound ----------
	/** What runs this part's sound (bits of moving()), or 0 when it has none. */
	int soundBits()
	{
		for(int[] p : SOUND_PARTS)
			if(p[0] == dx && p[1] == dy && p[2] == dz)
				return p[3];
		return 0;
	}

	/** IE's looping tile sound asks this every 2 s and ends the loop when it is false. */
	@Override
	public boolean shoudlPlaySound(String name)
	{
		return sound != null && sound.shouldPlay(this, name);
	}

	void stopSound()
	{
		if(world != null && world.isRemote && sound != null)
			sound.stop(this);
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

	/** The master draws the whole unit, 7 blocks long. */
	@Override
	public AxisAlignedBB getRenderBoundingBox()
	{
		return isMaster() ? new AxisAlignedBB(pos.add(-4, -2, -4), pos.add(5, 4, 5)) : super.getRenderBoundingBox();
	}
}
