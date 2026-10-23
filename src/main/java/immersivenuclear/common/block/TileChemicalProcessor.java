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
 */
public class TileChemicalProcessor extends TileEntity implements ITickable, IHammerInteraction, ISoundTile
{
	// One continuous cycle: 1 uranium powder + 1 reagent + water + energy -> polpa. Gameplay values, not real chemistry.
	public static final int CYCLE_TICKS = 100;
	public static final int ENERGY_PER_TICK = 40;
	public static final int WATER_PER_CYCLE = 500;
	public static final int POLPA_PER_CYCLE = 500;
	public static final int TANK_CAPACITY = 8000;
	public static final int ENERGY_CAPACITY = 16000;
	/** Pushed per tick from the polpa port into whatever is on its face (pipe, tank). */
	public static final int POLPA_PUSH_PER_TICK = 100;

	public static final int STATE_IDLE = 0, STATE_RUNNING = 1, STATE_DISABLED = 2, STATE_NO_INPUT = 3, STATE_NO_POWER = 4, STATE_OUTPUT_FULL = 5;
	/** Machine slots, in the order the interface shows them (design/interface-processamento-quimico-v1.png). */
	public static final int SLOT_URANIUM = 0, SLOT_REAGENT = 1, SLOT_WATER_IN = 2, SLOT_WATER_OUT = 3, SLOT_POLPA_IN = 4, SLOT_POLPA_OUT = 5, SLOTS = 6;

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
					return in != null && in.getFluid() == FluidRegistry.WATER;
				case SLOT_POLPA_IN:
					// An empty container, or one that already holds polpa
					FluidStack held = FluidUtil.getFluidContained(stack);
					return FluidUtil.getFluidHandler(stack) != null && (held == null || held.getFluid() == INContent.fluidPolpa);
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
	final FluidTank polpa = new FluidTank(TANK_CAPACITY)
	{
		@Override
		protected void onContentsChanged()
		{
			markDirty();
		}
	};
	final Energy energy = new Energy();
	int progress;
	int state = STATE_IDLE;
	/** Ticks left of the "water coming in" animation; restarted whenever the water tank gains water. */
	int fillTicks;
	int lastWater;
	/** What the client last got for the tank window (see RenderChemicalProcessor); sent every SYNC_TICKS when it changes. */
	int sentWater = -1, sentPolpa = -1;
	boolean sentFilling;
	public static final int SYNC_TICKS = 5;

	// ---------- client only: tank window ----------
	public int clientWater, clientPolpa;
	public boolean clientFilling, clientRunning;
	boolean sentRunning;
	/** Smoothed levels the renderer draws, 0..1 of the tank. */
	public float shownWater, shownPolpa;
	/** IE convention: a redstone signal stops the machine; hammering the redstone port inverts that. */
	boolean redstoneInverted;

	{
		water.setCanDrain(false);
		polpa.setCanFill(false);
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

		void use(int amount)
		{
			energy -= amount;
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
			right = port == ChemicalProcessorStructure.PORT_WATER || port == ChemicalProcessorStructure.PORT_POLPA;
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
					return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(m.water);
				if(port == ChemicalProcessorStructure.PORT_POLPA)
					return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(m.polpa);
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

	@Override
	public void update()
	{
		if(world.isRemote)
		{
			if(isMaster())
				updateSound();
			return;
		}
		if(getPort() == ChemicalProcessorStructure.PORT_POLPA)
			pushPolpa();
		if(isMaster())
		{
			handleContainers();
			process();
			syncTankWindow();
		}
	}

	/** Water and polpa levels (and whether water is coming in) for the glass of the tank, sent to the client. */
	void syncTankWindow()
	{
		int w = water.getFluidAmount();
		if(w > lastWater)
			fillTicks = 20;
		else if(fillTicks > 0)
			fillTicks--;
		lastWater = w;
		if(world.getTotalWorldTime() % SYNC_TICKS != 0)
			return;
		boolean filling = fillTicks > 0;
		boolean running = state == STATE_RUNNING;
		if(w != sentWater || polpa.getFluidAmount() != sentPolpa || filling != sentFilling || running != sentRunning)
		{
			sentRunning = running;
			sentWater = w;
			sentPolpa = polpa.getFluidAmount();
			sentFilling = filling;
			IBlockState st = world.getBlockState(pos);
			world.notifyBlockUpdate(pos, st, st, 2);
		}
	}

	/** Bucket slots, like IE's: a full water container is emptied into the water tank, an empty one is filled with polpa. */
	void handleContainers()
	{
		ItemStack in = inventory.getStackInSlot(SLOT_WATER_IN);
		if(!in.isEmpty())
		{
			FluidActionResult r = FluidUtil.tryEmptyContainer(ItemHandlerHelper.copyStackWithSize(in, 1), water, Integer.MAX_VALUE, null, false);
			if(r.isSuccess() && canOutput(SLOT_WATER_OUT, r.getResult()))
			{
				r = FluidUtil.tryEmptyContainer(ItemHandlerHelper.copyStackWithSize(in, 1), water, Integer.MAX_VALUE, null, true);
				output(SLOT_WATER_OUT, r.getResult());
				in.shrink(1);
				inventory.setStackInSlot(SLOT_WATER_IN, in);
			}
		}
		in = inventory.getStackInSlot(SLOT_POLPA_IN);
		if(!in.isEmpty() && polpa.getFluidAmount() > 0)
		{
			FluidActionResult r = FluidUtil.tryFillContainer(ItemHandlerHelper.copyStackWithSize(in, 1), polpa, Integer.MAX_VALUE, null, false);
			if(r.isSuccess() && canOutput(SLOT_POLPA_OUT, r.getResult()))
			{
				r = FluidUtil.tryFillContainer(ItemHandlerHelper.copyStackWithSize(in, 1), polpa, Integer.MAX_VALUE, null, true);
				output(SLOT_POLPA_OUT, r.getResult());
				in.shrink(1);
				inventory.setStackInSlot(SLOT_POLPA_IN, in);
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
		boolean powered = world.isBlockPowered(getTriggerPos());
		int before = state;
		if(powered != redstoneInverted)
			state = STATE_DISABLED;
		else if(water.getFluidAmount() < WATER_PER_CYCLE || inventory.getStackInSlot(SLOT_URANIUM).isEmpty() || inventory.getStackInSlot(SLOT_REAGENT).isEmpty())
			state = STATE_NO_INPUT;
		else if(polpa.getCapacity() - polpa.getFluidAmount() < POLPA_PER_CYCLE)
			state = STATE_OUTPUT_FULL;
		else if(energy.getEnergyStored() < ENERGY_PER_TICK)
			state = STATE_NO_POWER;
		else
		{
			state = STATE_RUNNING;
			energy.use(ENERGY_PER_TICK);
			if(++progress >= CYCLE_TICKS)
			{
				progress = 0;
				inventory.extractItem(SLOT_URANIUM, 1, false);
				inventory.extractItem(SLOT_REAGENT, 1, false);
				water.drainInternal(WATER_PER_CYCLE, true);
				polpa.fillInternal(new FluidStack(INContent.fluidPolpa, POLPA_PER_CYCLE), true);
			}
			markDirty();
		}
		if(state != before)
			markDirty();
	}

	/** The polpa port empties the tank into whatever accepts fluid on its face. */
	void pushPolpa()
	{
		TileChemicalProcessor m = getMaster();
		EnumFacing face = portWorldFace();
		if(m == null || face == null || m.polpa.getFluidAmount() <= 0)
			return;
		TileEntity out = world.getTileEntity(pos.offset(face));
		if(out == null || !out.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite()))
			return;
		IFluidHandler target = out.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite());
		if(target == null)
			return;
		FluidStack offer = m.polpa.drainInternal(POLPA_PUSH_PER_TICK, false);
		int taken = offer == null ? 0 : target.fill(offer, true);
		if(taken > 0)
			m.polpa.drainInternal(taken, true);
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

	public int getPolpa()
	{
		return polpa.getFluidAmount();
	}

	public int getEnergy()
	{
		return energy.getEnergyStored();
	}

	public int getProgress()
	{
		return progress;
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
			polpa.readFromNBT(m.getCompoundTag("polpa"));
			energy.set(m.getInteger("energy"));
			progress = m.getInteger("progress");
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
			m.setTag("polpa", polpa.writeToNBT(new NBTTagCompound()));
			m.setInteger("energy", energy.getEnergyStored());
			m.setInteger("progress", progress);
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
			nbt.setInteger("water", water.getFluidAmount());
			nbt.setInteger("polpa", polpa.getFluidAmount());
			nbt.setBoolean("filling", fillTicks > 0);
			nbt.setBoolean("running", state == STATE_RUNNING);
		}
	}

	void readClient(NBTTagCompound nbt)
	{
		readPart(nbt);
		clientWater = nbt.getInteger("water");
		clientPolpa = nbt.getInteger("polpa");
		clientFilling = nbt.getBoolean("filling");
		clientRunning = nbt.getBoolean("running");
	}

	void updateSound()
	{
		offTicks = clientRunning ? 0 : offTicks + 1;
		boolean on = clientRunning || soundOn && offTicks < STOP_GRACE_TICKS;
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
	}

	@Override
	public void onChunkUnload()
	{
		super.onChunkUnload();
		if(world != null && world.isRemote && isMaster())
			ImmersiveEngineering.proxy.handleTileSound(INContent.soundLoop, this, false, SOUND_VOLUME, SOUND_PITCH);
	}

	/** The master draws the liquid in the tank windows, which reach well outside its own block. */
	@Override
	public AxisAlignedBB getRenderBoundingBox()
	{
		return isMaster() ? new AxisAlignedBB(pos.add(-3, -2, -3), pos.add(4, 5, 4)) : super.getRenderBoundingBox();
	}
}
