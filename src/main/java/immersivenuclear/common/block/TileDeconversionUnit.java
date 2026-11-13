package immersivenuclear.common.block;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IHammerInteraction;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.ISoundTile;
import immersivenuclear.common.INContent;
import immersivenuclear.common.multiblock.DeconversionUnitStructure;
import immersivenuclear.common.multiblock.MultiblockDeconversionUnit;
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
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

/**
 * One part of a formed Deconversion Unit: which template block it replaced and how the structure is turned (see
 * {@link DeconversionUnitStructure}). The master part also holds the machine; the port parts hand their capability, on
 * their own face only, to the master.
 *
 * Cycles of 10 s: a cycle starts with its depleted uranium hexafluoride and steam (and room for its oxide and
 * hydrofluoric acid). The hexafluoride is hydrolysed with the steam in the reactor on the feed hood; the powder goes down
 * the rotary kiln through the electric furnace, where it becomes depleted uranium oxide, out of the discharge hood; the
 * hydrofluoric acid gas leaves through the candle filters and the condenser into its tank. It draws power while it runs
 * (the furnace and the kiln's motor); without power the cycle waits; a redstone signal stops it (the hammer inverts that).
 */
public class TileDeconversionUnit extends TileEntity implements MachinePartTile, ITickable, IHammerInteraction, ISoundTile
{
	// Provisional values: one enrichment cycle's tails (400 mB) per cycle
	public static final int UF6_PER_CYCLE = 400, STEAM_PER_CYCLE = 400, HF_PER_CYCLE = 250, OXIDE_PER_CYCLE = 1, CYCLE_TICKS = 200, ENERGY_PER_TICK = 96;
	public static final int UF6_CAPACITY = 8000, STEAM_CAPACITY = 8000, HF_CAPACITY = 8000, ENERGY_CAPACITY = 16000, PUSH_PER_TICK = 100;
	public static final int STATE_IDLE = 0, STATE_RUNNING = 1, STATE_DISABLED = 2, STATE_NO_INPUT = 3, STATE_NO_POWER = 4, STATE_OUTPUT_FULL = 5;
	/** The oxide out; each tank has its container slots: in (full or empty container) over it, out under it. */
	public static final int SLOT_OXIDE = 0, SLOT_UF6_IN = 1, SLOT_UF6_OUT = 2, SLOT_STEAM_IN = 3, SLOT_STEAM_OUT = 4, SLOT_HF_IN = 5, SLOT_HF_OUT = 6, SLOTS = 7;

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
				case SLOT_UF6_IN:
					return holds(stack, INContent.fluidUF6Depleted);
				case SLOT_STEAM_IN:
					return holds(stack, INContent.fluidSteam);
				case SLOT_HF_IN:
					return emptyOr(stack, INContent.fluidHydrofluoricAcid);
				default:
					// the oxide and the emptied or filled containers only come from the machine
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
	final FluidTank uf6 = inTank(UF6_CAPACITY, () -> INContent.fluidUF6Depleted);
	final FluidTank steam = inTank(STEAM_CAPACITY, () -> INContent.fluidSteam);
	final FluidTank hf = new FluidTank(HF_CAPACITY)
	{
		@Override
		protected void onContentsChanged()
		{
			markDirty();
		}
	};
	final Energy energy = new Energy();
	/** Ticks into the current cycle; 0 = no cycle running. */
	int progress;
	int state = STATE_IDLE;
	boolean redstoneInverted;
	/** Where each sound plays (template offsets): the kiln (the master, in the furnace), the furnace's heating, the
	 * kiln's motor and gearbox (the base under them), the hydrolysis reactor, the candle filters. */
	static final int[] HEATER_PART = {-3, 0, 1}, DRIVE_PART = {-2, -1, -3}, REACTOR_PART = {-3, 3, -4}, FILTER_PART = {-1, 1, -2};
	/** The loops come in as their starts end; the drive keeps the HF Kiln's sound (motor and gears). */
	final LoopSound kilnSound = new LoopSound(() -> INContent.soundDeconvKilnStart, () -> INContent.soundDeconvKilnLoop, () -> INContent.soundDeconvKilnStop, 40);
	final LoopSound drive = new LoopSound(() -> INContent.soundKilnDriveStart, () -> INContent.soundKilnDriveLoop, () -> INContent.soundKilnDriveStop, 60);
	final LoopSound heater = new LoopSound(() -> INContent.soundDeconvFurnaceStart, () -> INContent.soundDeconvFurnaceLoop, () -> INContent.soundDeconvFurnaceStop, 24);
	final LoopSound reactor = new LoopSound(() -> INContent.soundDeconvReactorStart, () -> INContent.soundDeconvReactorLoop, () -> INContent.soundDeconvReactorStop, 30);
	final LoopSound filterSound = new LoopSound(() -> INContent.soundDeconvFiltersStart, () -> INContent.soundDeconvFiltersLoop, () -> INContent.soundDeconvFiltersStop, 24);

	/** Client, master: the kiln's turn (degrees) and speed (0..1, easing up and down); the pinion and rollers follow it. */
	public float drumAngle, drumSpeed;
	/** The kiln's degrees per tick at full speed: one turn in 9 s. */
	public static final float DRUM_DEGREES_PER_TICK = 2.0F;

	/** Running, as last sent to the clients (server), or as received (client). */
	boolean sentRunning, clientRunning, sentPowered, clientPowered;

	public TileDeconversionUnit()
	{
		hf.setCanFill(false);
	}

	FluidTank inTank(int capacity, java.util.function.Supplier<Fluid> accepted)
	{
		FluidTank t = new FluidTank(capacity)
		{
			@Override
			public boolean canFillFluidType(FluidStack f)
			{
				return f != null && f.getFluid().getName().equals(accepted.get().getName());
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

	/** A fluid container holding this fluid (compared by name: the fluid may be another mod's). */
	static boolean holds(ItemStack stack, Fluid fluid)
	{
		FluidStack held = FluidUtil.getFluidContained(stack);
		return held != null && held.getFluid().getName().equals(fluid.getName());
	}

	/** A fluid container that is empty or already holds this fluid. */
	static boolean emptyOr(ItemStack stack, Fluid fluid)
	{
		FluidStack held = FluidUtil.getFluidContained(stack);
		return FluidUtil.getFluidHandler(stack) != null && (held == null || held.getFluid().getName().equals(fluid.getName()));
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

	/** The oxide port gives the oxide out (nothing goes in). */
	final IItemHandler oxidePort = new IItemHandler()
	{
		@Override
		public int getSlots()
		{
			return 1;
		}

		@Override
		public ItemStack getStackInSlot(int i)
		{
			return inventory.getStackInSlot(SLOT_OXIDE);
		}

		@Override
		public ItemStack insertItem(int i, ItemStack stack, boolean simulate)
		{
			return stack;
		}

		@Override
		public ItemStack extractItem(int i, int amount, boolean simulate)
		{
			return inventory.extractItem(SLOT_OXIDE, amount, simulate);
		}

		@Override
		public int getSlotLimit(int i)
		{
			return 64;
		}
	};

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
		return DeconversionUnitStructure.get(dx, dy, dz);
	}

	@Override
	public int getPort()
	{
		return DeconversionUnitStructure.portAt(dx, dy, dz);
	}

	/** What a broken part drops: the block it replaced. */
	public ItemStack getOriginalStack()
	{
		return MultiblockDeconversionUnit.stackFor(getPartCode()).copy();
	}

	public boolean isMaster()
	{
		return dx == DeconversionUnitStructure.MASTER_DX && dy == DeconversionUnitStructure.MASTER_DY && dz == DeconversionUnitStructure.MASTER_DZ;
	}

	@Override
	public String boundsName()
	{
		return "deconversion_unit";
	}

	@Override
	public int[] fromMaster()
	{
		return new int[]{dx - DeconversionUnitStructure.MASTER_DX, dy - DeconversionUnitStructure.MASTER_DY, dz - DeconversionUnitStructure.MASTER_DZ};
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
		int[] w = DeconversionUnitStructure.rotate(dx, dz, MultiblockDeconversionUnit.turns(facing), mirrored);
		return pos.add(-w[0], -dy, -w[1]);
	}

	public BlockPos getMasterPos()
	{
		return MultiblockDeconversionUnit.toWorld(getTriggerPos(), facing, mirrored, DeconversionUnitStructure.MASTER_DX, DeconversionUnitStructure.MASTER_DY, DeconversionUnitStructure.MASTER_DZ);
	}

	public TileDeconversionUnit getMaster()
	{
		if(isMaster())
			return this;
		if(world == null)
			return null;
		BlockPos m = getMasterPos();
		if(!world.isBlockLoaded(m))
			return null;
		TileEntity te = world.getTileEntity(m);
		return te instanceof TileDeconversionUnit && ((TileDeconversionUnit)te).isMaster() ? (TileDeconversionUnit)te : null;
	}

	/** World face a port works on, or null when it works on every side. */
	EnumFacing portWorldFace()
	{
		int[] f = DeconversionUnitStructure.portFace(dx, dy, dz);
		if(f == null || f[0] == 0 && f[1] == 0)
			return null;
		int[] w = DeconversionUnitStructure.rotate(f[0], f[1], MultiblockDeconversionUnit.turns(facing), mirrored);
		return EnumFacing.getFacingFromVector(w[0], 0, w[1]);
	}

	boolean portServes(Capability<?> cap, EnumFacing side)
	{
		int port = getPort();
		boolean right;
		if(cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
			right = port == DeconversionUnitStructure.PORT_UF6 || port == DeconversionUnitStructure.PORT_STEAM || port == DeconversionUnitStructure.PORT_HF;
		else if(cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			right = port == DeconversionUnitStructure.PORT_OXIDE;
		else if(cap == CapabilityEnergy.ENERGY)
			right = port == DeconversionUnitStructure.PORT_ENERGY;
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
			TileDeconversionUnit m = getMaster();
			if(m != null)
			{
				int port = getPort();
				if(cap == CapabilityEnergy.ENERGY)
					return CapabilityEnergy.ENERGY.cast(m.energy);
				if(cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
					return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(m.oxidePort);
				FluidTank t = port == DeconversionUnitStructure.PORT_UF6 ? m.uf6 : port == DeconversionUnitStructure.PORT_STEAM ? m.steam : m.hf;
				return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(t);
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
			{
				drumSpeed += ((on ? 1 : 0) - drumSpeed) * 0.04F;
				drumAngle = (drumAngle + drumSpeed * DRUM_DEGREES_PER_TICK) % 360;
				kilnSound.update(this, on);
			}
			else if(isPart(HEATER_PART))
				heater.update(this, on);
			else if(isPart(DRIVE_PART))
				drive.update(this, on);
			else if(isPart(REACTOR_PART))
				reactor.update(this, on);
			else if(isPart(FILTER_PART))
				filterSound.update(this, on);
			return;
		}
		if(getPort() == DeconversionUnitStructure.PORT_HF)
			push();
		if(isMaster())
		{
			handleContainers();
			process();
			sync();
		}
	}

	// ---------- the process ----------
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
				hf.fillInternal(new FluidStack(INContent.fluidHydrofluoricAcid, HF_PER_CYCLE), true);
				output(SLOT_OXIDE, new ItemStack(INContent.itemDepletedUraniumOxide, OXIDE_PER_CYCLE));
				progress = 0;
			}
		}
		if(state != before)
			markDirty();
	}

	/** Tells the clients when the machine starts or stops (its sounds). */
	void sync()
	{
		boolean on = state == STATE_RUNNING, powered = energy.getEnergyStored() > 0;
		if(on != sentRunning || powered != sentPowered)
		{
			sentRunning = on;
			sentPowered = powered;
			IBlockState st = world.getBlockState(pos);
			world.notifyBlockUpdate(pos, st, st, 2);
		}
	}

	/** Whether the machine has power, as the client knows it (the red LED and the manometers' light). */
	public boolean powered()
	{
		TileDeconversionUnit m = getMaster();
		return m != null && m.clientPowered;
	}

	/** Whether the machine runs, as the client knows it. */
	public boolean running()
	{
		TileDeconversionUnit m = getMaster();
		return m != null && m.clientRunning;
	}

	/** A cycle starts with its hexafluoride and steam in and room for its acid and oxide; they are used up as it starts. */
	boolean start()
	{
		if(uf6.getFluidAmount() < UF6_PER_CYCLE || steam.getFluidAmount() < STEAM_PER_CYCLE)
		{
			state = STATE_NO_INPUT;
			return false;
		}
		if(hf.getCapacity() - hf.getFluidAmount() < HF_PER_CYCLE || !canOutput(SLOT_OXIDE, new ItemStack(INContent.itemDepletedUraniumOxide, OXIDE_PER_CYCLE)))
		{
			state = STATE_OUTPUT_FULL;
			return false;
		}
		if(energy.getEnergyStored() < ENERGY_PER_TICK)
		{
			state = STATE_NO_POWER;
			return false;
		}
		uf6.drainInternal(UF6_PER_CYCLE, true);
		steam.drainInternal(STEAM_PER_CYCLE, true);
		return true;
	}

	/** Full containers empty into the hexafluoride and steam tanks; empty containers fill with hydrofluoric acid. */
	void handleContainers()
	{
		empty(SLOT_UF6_IN, SLOT_UF6_OUT, uf6);
		empty(SLOT_STEAM_IN, SLOT_STEAM_OUT, steam);
		fill(SLOT_HF_IN, SLOT_HF_OUT, hf);
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

	/** The hydrofluoric acid port pushes into what stands in front of it. */
	void push()
	{
		TileDeconversionUnit m = getMaster();
		EnumFacing face = portWorldFace();
		if(m == null || face == null || m.hf.getFluidAmount() <= 0)
			return;
		TileEntity out = world.getTileEntity(pos.offset(face));
		if(out == null || !out.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite()))
			return;
		IFluidHandler target = out.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite());
		if(target == null)
			return;
		FluidStack offer = m.hf.drainInternal(PUSH_PER_TICK, false);
		int taken = offer == null ? 0 : target.fill(offer, true);
		if(taken > 0)
			m.hf.drainInternal(taken, true);
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
		if(getPort() != DeconversionUnitStructure.PORT_REDSTONE)
			return false;
		TileDeconversionUnit m = getMaster();
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

	/** Hexafluoride, steam, hydrofluoric acid, energy, progress, state, redstone mode (all below 32767). */
	public int[] guiValues()
	{
		return new int[]{uf6.getFluidAmount(), steam.getFluidAmount(), hf.getFluidAmount(), energy.getEnergyStored(), progress, state, redstoneInverted ? 1 : 0};
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
			uf6.readFromNBT(m.getCompoundTag("uf6"));
			steam.readFromNBT(m.getCompoundTag("steam"));
			hf.readFromNBT(m.getCompoundTag("hf"));
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
			m.setTag("uf6", uf6.writeToNBT(new NBTTagCompound()));
			m.setTag("steam", steam.writeToNBT(new NBTTagCompound()));
			m.setTag("hf", hf.writeToNBT(new NBTTagCompound()));
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
		{
			nbt.setBoolean("running", state == STATE_RUNNING);
			nbt.setBoolean("powered", energy.getEnergyStored() > 0);
		}
	}

	void readClient(NBTTagCompound nbt)
	{
		readPart(nbt);
		clientRunning = nbt.getBoolean("running");
		clientPowered = nbt.getBoolean("powered");
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
			return kilnSound.shouldPlay(this, sound);
		if(isPart(HEATER_PART))
			return heater.shouldPlay(this, sound);
		if(isPart(DRIVE_PART))
			return drive.shouldPlay(this, sound);
		if(isPart(FILTER_PART))
			return filterSound.shouldPlay(this, sound);
		return isPart(REACTOR_PART) && reactor.shouldPlay(this, sound);
	}

	void stopSound()
	{
		if(world == null || !world.isRemote)
			return;
		if(isMaster())
			kilnSound.stop(this);
		else if(isPart(HEATER_PART))
			heater.stop(this);
		else if(isPart(DRIVE_PART))
			drive.stop(this);
		else if(isPart(REACTOR_PART))
			reactor.stop(this);
		else if(isPart(FILTER_PART))
			filterSound.stop(this);
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

	/** The master's box covers the whole machine (any turn): 5 blocks each way, 1 down, 5 up (the model reaches above the build). */
	@Override
	public AxisAlignedBB getRenderBoundingBox()
	{
		return isMaster() ? new AxisAlignedBB(pos.add(-5, -1, -5), pos.add(6, 6, 6)) : super.getRenderBoundingBox();
	}
}
