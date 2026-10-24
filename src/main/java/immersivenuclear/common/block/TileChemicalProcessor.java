package immersivenuclear.common.block;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IHammerInteraction;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.ISoundTile;
import immersivenuclear.common.INContent;
import immersivenuclear.common.multiblock.ChemicalProcessorStructure;
import immersivenuclear.common.multiblock.MultiblockChemicalProcessor;
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
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.oredict.OreDictionary;

/**
 * One part of a formed Chemical Processing Unit: which template block it replaced and how the structure is turned.
 * The master part also holds the machine (inventory, tanks, energy, progress); the port parts hand their
 * capability, on their own face only, to the master's storage (see {@link ChemicalProcessorStructure#PORTS}).
 * Right click opens the interface (ContainerChemicalProcessor / GuiChemicalProcessor) on the master.
 *
 * The reactor works in batches, like a settler: 1 fill (the hopper doses the uranium powder and the reagent, the pump
 * brings the water in), 2 react (the agitator turns, the water turns into slurry), 3 drain (the pump empties the
 * reactor into the slurry tank). A batch only starts with its solids at hand and room for all its slurry.
 * Recycled liquor: the chemical effluent (machine 3) or the depleted effluent (Metal Extractor) can make up to half of
 * a batch's liquid, from its own tank (the water port and bucket slot send each fluid to its tank). The acid left in
 * it saves reagent: a batch with the full half recycled takes a quarter less (the saving scales with the share).
 * Sounds: the machine's own sound while the agitator turns; the mod's pump sound ({@link PumpSound}), on the pump
 * part, while the reactor fills or drains.
 */
public class TileChemicalProcessor extends TileEntity implements ITickable, IHammerInteraction, ISoundTile
{
	// One batch (provisional): 8000 mB of water, 16 uranium powder and 16 reagent make 8000 mB of slurry.
	// Gameplay values, not real chemistry.
	public static final int BATCH_WATER = 8000, BATCH_SOLIDS = 16;
	/** Most of a batch that can be recycled liquor, and the reagent that saves with all of it (provisional). */
	public static final int MAX_RECYCLED = BATCH_WATER / 2, REAGENT_SAVED = BATCH_SOLIDS / 4;
	public static final int FILL_PER_TICK = 100, DRAIN_PER_TICK = 100, REACT_TICKS = 640;
	public static final int REACT_ENERGY = 40, PUMP_ENERGY = 16;
	/** Fill, react and drain, for JEI. */
	public static final int BATCH_TICKS = BATCH_WATER / FILL_PER_TICK + REACT_TICKS + BATCH_WATER / DRAIN_PER_TICK;
	public static final int PHASE_IDLE = 0, PHASE_FILL = 1, PHASE_REACT = 2, PHASE_DRAIN = 3;
	public static final int TANK_CAPACITY = 12000;
	public static final int ENERGY_CAPACITY = 16000;
	/** Pushed per tick from the slurry port into whatever is on its face (pipe, tank). */
	public static final int SLURRY_PUSH_PER_TICK = 100;

	public static final int STATE_IDLE = 0, STATE_RUNNING = 1, STATE_DISABLED = 2, STATE_NO_INPUT = 3, STATE_NO_POWER = 4, STATE_OUTPUT_FULL = 5;
	/** Machine slots, in the order the interface shows them. */
	public static final int SLOT_URANIUM = 0, SLOT_REAGENT = 1, SLOT_WATER_IN = 2, SLOT_WATER_OUT = 3, SLOT_SLURRY_IN = 4, SLOT_SLURRY_OUT = 5, SLOTS = 6;

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
				case SLOT_URANIUM: return isUraniumPowder(stack);
				case SLOT_REAGENT: return stack.getItem() == INContent.itemReagent;
				case SLOT_WATER_IN:
					FluidStack in = FluidUtil.getFluidContained(stack);
					return in != null && (in.getFluid() == FluidRegistry.WATER || isRecycled(in));
				case SLOT_SLURRY_IN:
					// An empty container, or one that already holds slurry
					FluidStack held = FluidUtil.getFluidContained(stack);
					return FluidUtil.getFluidHandler(stack) != null && (held == null || held.getFluid() == INContent.fluidSlurry);
				default: return false; // the two outputs only take what the machine puts there
			}
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
		{
			// ItemStackHandler does not check isItemValid itself in every Forge build
			return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
		}

		@Override
		protected void onContentsChanged(int slot)
		{
			markDirty();
		}
	};
	final FluidTank water = new FluidTank(TANK_CAPACITY)
	{
		@Override
		public boolean canFillFluidType(FluidStack fluid)
		{
			return fluid != null && fluid.getFluid() == FluidRegistry.WATER;
		}

		@Override
		protected void onContentsChanged()
		{
			markDirty();
		}
	};
	/** Recycled liquor: chemical effluent or depleted effluent (one at a time). */
	final FluidTank recycled = new FluidTank(TANK_CAPACITY)
	{
		@Override
		public boolean canFillFluidType(FluidStack fluid)
		{
			return isRecycled(fluid);
		}

		@Override
		protected void onContentsChanged()
		{
			markDirty();
		}
	};
	/** The water port and the bucket slot: water to its tank, effluent to the recycled liquor's. */
	final IFluidHandler inlet = new net.minecraftforge.fluids.capability.templates.FluidHandlerConcatenate(water, recycled);
	final FluidTank slurry = new FluidTank(TANK_CAPACITY)
	{
		@Override
		protected void onContentsChanged()
		{
			markDirty();
		}
	};
	final Energy energy = new Energy();
	int state = STATE_IDLE;
	/** The batch: its step, the ticks in it, the liquid in the reactor (mB), the recycled liquor it takes (mB). */
	int phase = PHASE_IDLE, phaseTicks, reactor, batchRecycled;
	/** What moved this tick: water into the reactor, slurry out of it. */
	boolean filling, draining;
	/** What the client last got for the reactor window (see RenderChemicalProcessor); sent every SYNC_TICKS when it changes. */
	int sentWindow = Integer.MIN_VALUE;
	public static final int SYNC_TICKS = 5;
	/** The reaction as the window shows it, in steps of 1/MIX_STEPS. */
	public static final int MIX_STEPS = 32;

	// ---------- client only: the reactor seen through the windows of the model, and the sounds ----------
	public int clientReactor, clientPhase, clientMix;
	public boolean clientFilling, clientDraining, clientRunning;
	/** Smoothed level the renderer draws, 0..1 of the reactor. */
	public float shownLevel;
	/** IE convention: a redstone signal stops the machine; hammering the redstone port inverts that. */
	boolean redstoneInverted;

	{
		water.setCanDrain(false);
		recycled.setCanDrain(false);
		slurry.setCanFill(false);
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

	/** An item port: inserts into one slot of the master, never gives items back. */
	static class SlotPort implements IItemHandler
	{
		final IItemHandler inv;
		final int slot;

		SlotPort(IItemHandler inv, int slot)
		{
			this.inv = inv;
			this.slot = slot;
		}

		@Override
		public int getSlots()
		{
			return 1;
		}

		@Override
		public ItemStack getStackInSlot(int i)
		{
			return inv.getStackInSlot(slot);
		}

		@Override
		public ItemStack insertItem(int i, ItemStack stack, boolean simulate)
		{
			return inv.insertItem(slot, stack, simulate);
		}

		@Override
		public ItemStack extractItem(int i, int amount, boolean simulate)
		{
			return ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int i)
		{
			return inv.getSlotLimit(slot);
		}

		public boolean isItemValid(int i, ItemStack stack)
		{
			return inv.isItemValid(slot, stack);
		}
	}

	/** Chemical effluent or depleted effluent: what the recycled liquor tank takes. */
	public static boolean isRecycled(FluidStack fluid)
	{
		return fluid != null && (fluid.getFluid() == INContent.fluidEffluent || fluid.getFluid() == INContent.fluidDepletedEffluent);
	}

	/** Recycled liquor a batch would take now: what the tank holds, up to half the batch. */
	public static int recycledShare(int inTank)
	{
		return Math.min(MAX_RECYCLED, inTank / FILL_PER_TICK * FILL_PER_TICK);
	}

	/** Reagent for a batch with this much recycled liquor: the full half saves REAGENT_SAVED, less saves less. */
	public static int reagentFor(int recycled)
	{
		return BATCH_SOLIDS - Math.round(REAGENT_SAVED * recycled / (float)MAX_RECYCLED);
	}

	public static boolean isUraniumPowder(ItemStack stack)
	{
		if(stack.isEmpty())
			return false;
		int id = OreDictionary.getOreID("dustUranium");
		for(int i : OreDictionary.getOreIDs(stack))
			if(i == id)
				return true;
		return false;
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
		return ChemicalProcessorStructure.get(dx, dy, dz);
	}

	public int getPort()
	{
		return ChemicalProcessorStructure.portAt(dx, dy, dz);
	}

	public ItemStack getOriginalStack()
	{
		return MultiblockChemicalProcessor.stackFor(getPartCode());
	}

	public boolean isMaster()
	{
		return dx == ChemicalProcessorStructure.MASTER_DX && dy == ChemicalProcessorStructure.MASTER_DY && dz == ChemicalProcessorStructure.MASTER_DZ;
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
		int[] w = ChemicalProcessorStructure.rotate(dx, dz, MultiblockChemicalProcessor.turns(facing), mirrored);
		return pos.add(-w[0], -dy, -w[1]);
	}

	public BlockPos getMasterPos()
	{
		return MultiblockChemicalProcessor.toWorld(getTriggerPos(), facing, mirrored,
				ChemicalProcessorStructure.MASTER_DX, ChemicalProcessorStructure.MASTER_DY, ChemicalProcessorStructure.MASTER_DZ);
	}

	public TileChemicalProcessor getMaster()
	{
		if(isMaster())
			return this;
		if(world == null)
			return null;
		BlockPos m = getMasterPos();
		if(!world.isBlockLoaded(m))
			return null;
		TileEntity te = world.getTileEntity(m);
		return te instanceof TileChemicalProcessor && ((TileChemicalProcessor)te).isMaster() ? (TileChemicalProcessor)te : null;
	}

	/** World face a port works on, or null when it works on every side. */
	EnumFacing portWorldFace()
	{
		int[] f = ChemicalProcessorStructure.portFace(dx, dy, dz);
		if(f == null || f[0] == 0 && f[1] == 0)
			return null;
		int[] w = ChemicalProcessorStructure.rotate(f[0], f[1], MultiblockChemicalProcessor.turns(facing), mirrored);
		return EnumFacing.getFacingFromVector(w[0], 0, w[1]);
	}

	boolean portServes(Capability<?> cap, EnumFacing side)
	{
		int port = getPort();
		boolean right;
		if(cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			right = port == ChemicalProcessorStructure.PORT_URANIUM || port == ChemicalProcessorStructure.PORT_REAGENT;
		else if(cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
			right = port == ChemicalProcessorStructure.PORT_WATER || port == ChemicalProcessorStructure.PORT_SLURRY;
		else if(cap == CapabilityEnergy.ENERGY)
			right = port == ChemicalProcessorStructure.PORT_ENERGY;
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
			TileChemicalProcessor m = getMaster();
			if(m != null)
			{
				int port = getPort();
				if(port == ChemicalProcessorStructure.PORT_URANIUM)
					return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(new SlotPort(m.inventory, SLOT_URANIUM));
				if(port == ChemicalProcessorStructure.PORT_REAGENT)
					return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(new SlotPort(m.inventory, SLOT_REAGENT));
				if(port == ChemicalProcessorStructure.PORT_WATER)
					return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(m.inlet);
				if(port == ChemicalProcessorStructure.PORT_SLURRY)
					return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(m.slurry);
				if(port == ChemicalProcessorStructure.PORT_ENERGY)
					return CapabilityEnergy.ENERGY.cast(m.energy);
			}
		}
		return super.getCapability(cap, side);
	}

	/** Running sound: start-up once, then the loop (IE's tile sound, like its machines), shutdown once. */
	static final float SOUND_VOLUME = 1.0F, SOUND_PITCH = 1.0F;
	/** The loop comes in just before the 3.2 s start-up ends (its tail fades out under the loop). */
	static final int LOOP_DELAY_TICKS = 58;
	/** A machine that stops for less than this (between two cycles) keeps its sound going. */
	static final int STOP_GRACE_TICKS = 20;
	// client only
	boolean soundOn;
	int loopDelay, offTicks;
	final PumpSound pump = new PumpSound();
	/** The part that plays the pump: the fluid pump of the structure (lower half). */
	static final int PUMP_DX = 1, PUMP_DY = 0, PUMP_DZ = 0;

	boolean isPumpPart()
	{
		return dx == PUMP_DX && dy == PUMP_DY && dz == PUMP_DZ;
	}

	@Override
	public void update()
	{
		if(world.isRemote)
		{
			if(isMaster())
				updateSound();
			else if(isPumpPart())
			{
				TileChemicalProcessor m = getMaster();
				pump.update(this, m != null && m.clientRunning && (m.clientFilling || m.clientDraining));
			}
			return;
		}
		if(getPort() == ChemicalProcessorStructure.PORT_SLURRY)
			pushSlurry();
		if(isMaster())
		{
			handleContainers();
			process();
			syncTankWindow();
		}
	}

	/** The reactor for the windows of the model and the sounds: level, step, reaction, what moves; sent when it changes. */
	void syncTankWindow()
	{
		if(world.getTotalWorldTime() % SYNC_TICKS != 0)
			return;
		int key = (((reactor * 64 / BATCH_WATER * 4 + phase) * (MIX_STEPS + 1) + mix()) * 2 + (filling ? 1 : 0)) * 4
				+ (draining ? 2 : 0) + (state == STATE_RUNNING ? 1 : 0);
		if(key != sentWindow)
		{
			sentWindow = key;
			IBlockState st = world.getBlockState(pos);
			world.notifyBlockUpdate(pos, st, st, 2);
		}
	}

	/** How far the reactor's liquid has turned from water into slurry, 0..MIX_STEPS. */
	int mix()
	{
		return phase == PHASE_DRAIN ? MIX_STEPS : phase == PHASE_REACT ? phaseTicks * MIX_STEPS / REACT_TICKS : 0;
	}

	/** Bucket slots, like IE's: a full water container is emptied into the water tank, an empty one is filled with slurry. */
	void handleContainers()
	{
		ItemStack in = inventory.getStackInSlot(SLOT_WATER_IN);
		if(!in.isEmpty())
		{
			FluidActionResult r = FluidUtil.tryEmptyContainer(ItemHandlerHelper.copyStackWithSize(in, 1), inlet, Integer.MAX_VALUE, null, false);
			if(r.isSuccess() && canOutput(SLOT_WATER_OUT, r.getResult()))
			{
				r = FluidUtil.tryEmptyContainer(ItemHandlerHelper.copyStackWithSize(in, 1), inlet, Integer.MAX_VALUE, null, true);
				output(SLOT_WATER_OUT, r.getResult());
				in.shrink(1);
				inventory.setStackInSlot(SLOT_WATER_IN, in);
			}
		}
		in = inventory.getStackInSlot(SLOT_SLURRY_IN);
		if(!in.isEmpty() && slurry.getFluidAmount() > 0)
		{
			FluidActionResult r = FluidUtil.tryFillContainer(ItemHandlerHelper.copyStackWithSize(in, 1), slurry, Integer.MAX_VALUE, null, false);
			if(r.isSuccess() && canOutput(SLOT_SLURRY_OUT, r.getResult()))
			{
				r = FluidUtil.tryFillContainer(ItemHandlerHelper.copyStackWithSize(in, 1), slurry, Integer.MAX_VALUE, null, true);
				output(SLOT_SLURRY_OUT, r.getResult());
				in.shrink(1);
				inventory.setStackInSlot(SLOT_SLURRY_IN, in);
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
			o.grow(stack.getCount());
			inventory.setStackInSlot(slot, o);
		}
	}

	void process()
	{
		int before = state;
		filling = draining = false;
		if(world.isBlockPowered(getTriggerPos()) != redstoneInverted)
		{
			state = STATE_DISABLED;
			if(state != before)
				markDirty();
			return;
		}
		switch(phase)
		{
			case PHASE_IDLE:
				// A new batch: the hopper doses the solids (less reagent with recycled liquor), then the reactor fills
				int share = recycledShare(recycled.getFluidAmount());
				if(inventory.getStackInSlot(SLOT_URANIUM).getCount() < BATCH_SOLIDS || inventory.getStackInSlot(SLOT_REAGENT).getCount() < reagentFor(share)
						|| water.getFluidAmount() + share < FILL_PER_TICK)
					state = STATE_NO_INPUT;
				else if(slurry.getCapacity() - slurry.getFluidAmount() < BATCH_WATER)
					state = STATE_OUTPUT_FULL;
				else
				{
					inventory.extractItem(SLOT_URANIUM, BATCH_SOLIDS, false);
					inventory.extractItem(SLOT_REAGENT, reagentFor(share), false);
					batchRecycled = share;
					next(PHASE_FILL);
					state = STATE_RUNNING;
				}
				break;
			case PHASE_FILL:
				// The recycled liquor first, then the water
				int in = Math.min(FILL_PER_TICK, BATCH_WATER - reactor);
				FluidTank from = reactor < batchRecycled ? recycled : water;
				if(from == recycled)
					in = Math.min(in, batchRecycled - reactor);
				if(from.getFluidAmount() < in)
					state = STATE_NO_INPUT;
				else if(!energy.use(PUMP_ENERGY))
					state = STATE_NO_POWER;
				else
				{
					from.drainInternal(in, true);
					reactor += in;
					filling = true;
					phaseTicks++;
					state = STATE_RUNNING;
					if(reactor >= BATCH_WATER)
						next(PHASE_REACT);
				}
				break;
			case PHASE_REACT:
				if(!energy.use(REACT_ENERGY))
					state = STATE_NO_POWER;
				else
				{
					state = STATE_RUNNING;
					if(++phaseTicks >= REACT_TICKS)
						next(PHASE_DRAIN);
				}
				break;
			default:
				if(!energy.use(PUMP_ENERGY))
					state = STATE_NO_POWER;
				else
				{
					int out = slurry.fillInternal(new FluidStack(INContent.fluidSlurry, Math.min(DRAIN_PER_TICK, reactor)), true);
					reactor -= out;
					draining = out > 0;
					phaseTicks++;
					state = out > 0 ? STATE_RUNNING : STATE_OUTPUT_FULL;
					if(reactor <= 0)
					{
						reactor = 0;
						next(PHASE_IDLE);
					}
				}
		}
		if(state != before || state == STATE_RUNNING)
			markDirty();
	}

	void next(int phase)
	{
		this.phase = phase;
		phaseTicks = 0;
	}

	/** The slurry port empties the tank into whatever accepts fluid on its face. */
	void pushSlurry()
	{
		TileChemicalProcessor m = getMaster();
		EnumFacing face = portWorldFace();
		if(m == null || face == null || m.slurry.getFluidAmount() <= 0)
			return;
		TileEntity out = world.getTileEntity(pos.offset(face));
		if(out == null || !out.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite()))
			return;
		IFluidHandler target = out.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite());
		if(target == null)
			return;
		FluidStack offer = m.slurry.drainInternal(SLURRY_PUSH_PER_TICK, false);
		int taken = offer == null ? 0 : target.fill(offer, true);
		if(taken > 0)
			m.slurry.drainInternal(taken, true);
	}

	/** Hammer on the redstone port: invert the redstone control, like IE's machines. */
	@Override
	public boolean hammerUseSide(EnumFacing side, EntityPlayer player, float hitX, float hitY, float hitZ)
	{
		if(getPort() != ChemicalProcessorStructure.PORT_REDSTONE)
			return false;
		TileChemicalProcessor m = getMaster();
		if(m == null)
			return false;
		if(!world.isRemote)
		{
			m.toggleRedstone();
			player.sendStatusMessage(new TextComponentTranslation(m.redstoneInverted ? "chat.immersivenuclear.redstone.inverted" : "chat.immersivenuclear.redstone.normal"), true);
		}
		return true;
	}

	// ---------- for the interface (master only) ----------
	public IItemHandler getInventory()
	{
		return inventory;
	}

	public int getWater()
	{
		return water.getFluidAmount();
	}

	public int getRecycled()
	{
		return recycled.getFluidAmount();
	}

	/** 0 none, 1 chemical effluent, 2 depleted effluent. */
	public int getRecycledType()
	{
		FluidStack f = recycled.getFluid();
		return f == null ? 0 : f.getFluid() == INContent.fluidDepletedEffluent ? 2 : 1;
	}

	public int getBatchRecycled()
	{
		return batchRecycled;
	}

	public int getSlurry()
	{
		return slurry.getFluidAmount();
	}

	public int getEnergy()
	{
		return energy.getEnergyStored();
	}

	public int getPhase()
	{
		return phase;
	}

	public int getPhaseTicks()
	{
		return phaseTicks;
	}

	public int getReactor()
	{
		return reactor;
	}

	/** Bits: water coming in, slurry going out. */
	public int getMoving()
	{
		return (filling ? 1 : 0) | (draining ? 2 : 0);
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
			inv.setInteger("Size", SLOTS); // 0.6.0 saved 2 slots; keep the handler at full size
			inventory.deserializeNBT(inv);
			water.readFromNBT(m.getCompoundTag("water"));
			recycled.readFromNBT(m.getCompoundTag("recycled"));
			batchRecycled = m.getInteger("batchRecycled");
			slurry.readFromNBT(m.getCompoundTag("slurry"));
			energy.set(m.getInteger("energy"));
			phase = m.getInteger("phase");
			phaseTicks = m.getInteger("phaseTicks");
			reactor = m.getInteger("reactor");
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
			m.setTag("water", water.writeToNBT(new NBTTagCompound()));
			m.setTag("recycled", recycled.writeToNBT(new NBTTagCompound()));
			m.setInteger("batchRecycled", batchRecycled);
			m.setTag("slurry", slurry.writeToNBT(new NBTTagCompound()));
			m.setInteger("energy", energy.getEnergyStored());
			m.setInteger("phase", phase);
			m.setInteger("phaseTicks", phaseTicks);
			m.setInteger("reactor", reactor);
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

	/** The client only needs the part (and, on the master, the tank window), not the replaced block. */
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
		// The model depends on the part, so redraw only when that changed (level updates come every few ticks)
		if(ox != dx || oy != dy || oz != dz || of != facing || om != mirrored)
			world.markBlockRangeForRenderUpdate(pos, pos);
	}

	void writeClient(NBTTagCompound nbt)
	{
		writePart(nbt);
		if(isMaster())
		{
			nbt.setInteger("reactor", reactor);
			nbt.setInteger("phase", phase);
			nbt.setInteger("mix", mix());
			nbt.setBoolean("filling", filling);
			nbt.setBoolean("draining", draining);
			nbt.setBoolean("running", state == STATE_RUNNING);
		}
	}

	void readClient(NBTTagCompound nbt)
	{
		readPart(nbt);
		clientReactor = nbt.getInteger("reactor");
		clientPhase = nbt.getInteger("phase");
		clientMix = nbt.getInteger("mix");
		clientFilling = nbt.getBoolean("filling");
		clientDraining = nbt.getBoolean("draining");
		clientRunning = nbt.getBoolean("running");
	}

	/** The machine's own sound runs while the agitator turns. */
	void updateSound()
	{
		boolean running = clientRunning && clientPhase == PHASE_REACT;
		offTicks = running ? 0 : offTicks + 1;
		boolean on = running || soundOn && offTicks < STOP_GRACE_TICKS;
		if(on && !soundOn)
		{
			playOnce(INContent.soundStart);
			loopDelay = LOOP_DELAY_TICKS;
		}
		else if(!on && soundOn)
			playOnce(INContent.soundStop);
		soundOn = on;
		if(loopDelay > 0)
			loopDelay--;
		ImmersiveEngineering.proxy.handleTileSound(INContent.soundLoop, this, on && loopDelay == 0 && !isInvalid(), SOUND_VOLUME, SOUND_PITCH);
	}

	/** IE's looping tile sound asks this every 2 s and ends the loop when it is false (or the tile is not an ISoundTile). */
	@Override
	public boolean shoudlPlaySound(String sound)
	{
		if(isPumpPart())
			return pump.shouldPlay(this, sound);
		return isMaster() && !isInvalid() && soundOn && loopDelay == 0 && INContent.soundLoop.getSoundName().toString().equals(sound);
	}

	void playOnce(SoundEvent sound)
	{
		world.playSound(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, sound, SoundCategory.BLOCKS, SOUND_VOLUME, SOUND_PITCH, false);
	}

	/** Stop the loop when the structure comes apart or the chunk unloads. */
	@Override
	public void invalidate()
	{
		super.invalidate();
		if(world != null && world.isRemote && isMaster())
			ImmersiveEngineering.proxy.handleTileSound(INContent.soundLoop, this, false, SOUND_VOLUME, SOUND_PITCH);
		if(world != null && world.isRemote && isPumpPart())
			pump.stop(this);
	}

	@Override
	public void onChunkUnload()
	{
		super.onChunkUnload();
		if(world != null && world.isRemote && isMaster())
			ImmersiveEngineering.proxy.handleTileSound(INContent.soundLoop, this, false, SOUND_VOLUME, SOUND_PITCH);
		if(world != null && world.isRemote && isPumpPart())
			pump.stop(this);
	}

	/** The master draws the liquid in the tank windows, which reach well outside its own block. */
	@Override
	public AxisAlignedBB getRenderBoundingBox()
	{
		return isMaster() ? new AxisAlignedBB(pos.add(-3, -2, -3), pos.add(4, 5, 4)) : super.getRenderBoundingBox();
	}
}
