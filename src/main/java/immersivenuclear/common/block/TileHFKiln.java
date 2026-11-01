package immersivenuclear.common.block;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IHammerInteraction;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.ISoundTile;
import immersivenuclear.common.INContent;
import immersivenuclear.common.multiblock.HFKilnStructure;
import immersivenuclear.common.multiblock.MultiblockHFKiln;
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
import net.minecraftforge.oredict.OreDictionary;

/**
 * One part of a formed Hydrofluoric Acid Kiln: which template block it replaced and how the structure is turned. The
 * master part also holds the machine; the port parts hand their capability, on their own face only, to the master
 * (see {@link HFKilnStructure#PORTS}).
 *
 * A rotary kiln, in cycles of 10 s: a cycle starts with one fluorite dust and its sulfuric acid (and room for its
 * hydrofluoric acid and gypsum); while it runs it draws power (the heating and the motor turning the drum): the fluorite
 * reacts with the acid in the hot drum, the hydrofluoric acid gas goes up the gas line to the condenser and into its
 * tank, the gypsum comes out of the discharge head at the end. Without power the cycle waits; a redstone signal stops it
 * (the hammer inverts that).
 */
public class TileHFKiln extends TileEntity implements ITickable, IHammerInteraction, ISoundTile
{
	// Provisional values
	public static final int ACID_PER_CYCLE = 500, HF_PER_CYCLE = 500, GYPSUM_PER_CYCLE = 1, CYCLE_TICKS = 200, ENERGY_PER_TICK = 64;
	public static final int ACID_CAPACITY = 8000, HF_CAPACITY = 8000, ENERGY_CAPACITY = 16000, PUSH_PER_TICK = 100;
	public static final int STATE_IDLE = 0, STATE_RUNNING = 1, STATE_DISABLED = 2, STATE_NO_INPUT = 3, STATE_NO_POWER = 4, STATE_OUTPUT_FULL = 5;
	/** Fluorite in, gypsum out; each tank has its container slots under it: in (full or empty container) and out. */
	public static final int SLOT_FLUORITE = 0, SLOT_GYPSUM = 1, SLOT_ACID_IN = 2, SLOT_ACID_OUT = 3, SLOT_HF_IN = 4, SLOT_HF_OUT = 5, SLOTS = 6;
	/** Fluorite in any mod's ground form (Immersive Intelligence's too). */
	public static final String FLUORITE = "dustFluorite";

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
				case SLOT_FLUORITE:
					return isFluorite(stack);
				case SLOT_ACID_IN:
					FluidStack a = FluidUtil.getFluidContained(stack);
					return a != null && a.getFluid().getName().equals(INContent.fluidSulfuricAcid.getName());
				case SLOT_HF_IN:
					return emptyOr(stack, INContent.fluidHydrofluoricAcid);
				default:
					// the gypsum and the filled containers only come from the machine
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
	final FluidTank acid = new FluidTank(ACID_CAPACITY)
	{
		@Override
		public boolean canFillFluidType(FluidStack fluid)
		{
			return fluid != null && fluid.getFluid().getName().equals(INContent.fluidSulfuricAcid.getName());
		}

		@Override
		protected void onContentsChanged()
		{
			markDirty();
		}
	};
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
	/** Where each sound plays (template offsets): the motor and its gears, the heating coil, the drum (the master, in its middle). */
	static final int[] DRIVE_PART = {3, 0, 2}, HEATER_PART = {1, 0, 2}, DRUM_PART = {2, 0, 3};
	/** The loops come in as their starts end (drum and drive 3 s, heater 1 s). */
	final LoopSound drumSound = new LoopSound(() -> INContent.soundKilnDrumStart, () -> INContent.soundKilnDrumLoop, () -> INContent.soundKilnDrumStop, 60);
	final LoopSound drive = new LoopSound(() -> INContent.soundKilnDriveStart, () -> INContent.soundKilnDriveLoop, () -> INContent.soundKilnDriveStop, 60);
	final LoopSound heater = new LoopSound(() -> INContent.soundKilnHeaterStart, () -> INContent.soundKilnHeaterLoop, () -> INContent.soundKilnHeaterStop, 20);

	/** Running, as last sent to the clients (server), or as received (client): lights the heaters. */
	boolean sentRunning, clientRunning;
	/** Client, master: the drum's turn (degrees) and speed (0..1, easing up and down); the gears and rollers follow it. */
	public float drumAngle, drumSpeed;
	/** The drum's degrees per tick at full speed: one turn in 9 s. */
	public static final float DRUM_DEGREES_PER_TICK = 2.0F;

	public TileHFKiln()
	{
		acid.setCanDrain(false);
		hf.setCanFill(false);
	}

	/** A fluid container that is empty or already holds this fluid (compared by name: the fluid may be another mod's). */
	static boolean emptyOr(ItemStack stack, net.minecraftforge.fluids.Fluid fluid)
	{
		FluidStack held = FluidUtil.getFluidContained(stack);
		return FluidUtil.getFluidHandler(stack) != null && (held == null || held.getFluid().getName().equals(fluid.getName()));
	}

	public static boolean isFluorite(ItemStack stack)
	{
		if(stack.isEmpty())
			return false;
		for(int id : OreDictionary.getOreIDs(stack))
			if(FLUORITE.equals(OreDictionary.getOreName(id)))
				return true;
		return false;
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

	/** The fluorite port takes fluorite into its slot; the gypsum port gives the gypsum out. */
	final IItemHandler fluoritePort = port(SLOT_FLUORITE, true), gypsumPort = port(SLOT_GYPSUM, false);

	IItemHandler port(int slot, boolean in)
	{
		return new IItemHandler()
		{
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
		};
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
		return HFKilnStructure.get(dx, dy, dz);
	}

	public int getPort()
	{
		return HFKilnStructure.portAt(dx, dy, dz);
	}

	/** What a broken part drops: the block it replaced. */
	public ItemStack getOriginalStack()
	{
		return MultiblockHFKiln.stackFor(getPartCode()).copy();
	}

	public boolean isMaster()
	{
		return dx == HFKilnStructure.MASTER_DX && dy == HFKilnStructure.MASTER_DY && dz == HFKilnStructure.MASTER_DZ;
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
		int[] w = HFKilnStructure.rotate(dx, dz, MultiblockHFKiln.turns(facing), mirrored);
		return pos.add(-w[0], -dy, -w[1]);
	}

	public BlockPos getMasterPos()
	{
		return MultiblockHFKiln.toWorld(getTriggerPos(), facing, mirrored, HFKilnStructure.MASTER_DX, HFKilnStructure.MASTER_DY, HFKilnStructure.MASTER_DZ);
	}

	public TileHFKiln getMaster()
	{
		if(isMaster())
			return this;
		if(world == null)
			return null;
		BlockPos m = getMasterPos();
		if(!world.isBlockLoaded(m))
			return null;
		TileEntity te = world.getTileEntity(m);
		return te instanceof TileHFKiln && ((TileHFKiln)te).isMaster() ? (TileHFKiln)te : null;
	}

	/** World face a port works on, or null when it works on every side. */
	EnumFacing portWorldFace()
	{
		int[] f = HFKilnStructure.portFace(dx, dy, dz);
		if(f == null || f[0] == 0 && f[1] == 0)
			return null;
		int[] w = HFKilnStructure.rotate(f[0], f[1], MultiblockHFKiln.turns(facing), mirrored);
		return EnumFacing.getFacingFromVector(w[0], 0, w[1]);
	}

	boolean portServes(Capability<?> cap, EnumFacing side)
	{
		int port = getPort();
		boolean right;
		if(cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
			right = port == HFKilnStructure.PORT_ACID || port == HFKilnStructure.PORT_HF;
		else if(cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			right = port == HFKilnStructure.PORT_FLUORITE || port == HFKilnStructure.PORT_GYPSUM;
		else if(cap == CapabilityEnergy.ENERGY)
			right = port == HFKilnStructure.PORT_ENERGY;
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
			TileHFKiln m = getMaster();
			if(m != null)
			{
				int port = getPort();
				if(cap == CapabilityEnergy.ENERGY)
					return CapabilityEnergy.ENERGY.cast(m.energy);
				if(cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
					return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(port == HFKilnStructure.PORT_FLUORITE ? m.fluoritePort : m.gypsumPort);
				return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(port == HFKilnStructure.PORT_ACID ? m.acid : m.hf);
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
			{
				drumSpeed += ((clientRunning ? 1 : 0) - drumSpeed) * 0.04F;
				drumAngle = (drumAngle + drumSpeed * DRUM_DEGREES_PER_TICK) % 360;
			}
			if(isPart(DRIVE_PART))
				drive.update(this, heaterLit());
			else if(isPart(HEATER_PART))
				heater.update(this, heaterLit());
			else if(isPart(DRUM_PART))
				drumSound.update(this, heaterLit());
			return;
		}
		if(getPort() == HFKilnStructure.PORT_HF)
			push();
		if(isMaster())
		{
			handleContainers();
			process();
			sync();
		}
	}

	// ---------- the kiln ----------
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
				output(SLOT_GYPSUM, new ItemStack(INContent.itemGypsum, GYPSUM_PER_CYCLE));
				progress = 0;
			}
		}
		if(state != before)
			markDirty();
	}

	/** Tells the clients when the kiln starts or stops, so the heaters light up or go out. */
	void sync()
	{
		boolean running = state == STATE_RUNNING;
		if(running != sentRunning)
		{
			sentRunning = running;
			IBlockState st = world.getBlockState(pos);
			world.notifyBlockUpdate(pos, st, st, 2);
		}
	}

	/** Whether the kiln runs, as the client knows it: lights the heaters, turns the motor's sound on. */
	public boolean heaterLit()
	{
		TileHFKiln m = getMaster();
		return m != null && m.clientRunning;
	}

	/** A cycle starts with its fluorite and acid in and room for its hydrofluoric acid and gypsum; they are used up as it starts. */
	boolean start()
	{
		ItemStack fluorite = inventory.getStackInSlot(SLOT_FLUORITE);
		if(!isFluorite(fluorite) || acid.getFluidAmount() < ACID_PER_CYCLE)
		{
			state = STATE_NO_INPUT;
			return false;
		}
		if(hf.getCapacity() - hf.getFluidAmount() < HF_PER_CYCLE || !canOutput(SLOT_GYPSUM, new ItemStack(INContent.itemGypsum, GYPSUM_PER_CYCLE)))
		{
			state = STATE_OUTPUT_FULL;
			return false;
		}
		if(energy.getEnergyStored() < ENERGY_PER_TICK)
		{
			state = STATE_NO_POWER;
			return false;
		}
		fluorite.shrink(1);
		inventory.setStackInSlot(SLOT_FLUORITE, fluorite);
		acid.drainInternal(ACID_PER_CYCLE, true);
		return true;
	}

	/** Acid containers empty into the acid tank; empty containers fill with hydrofluoric acid. Each into its out slot. */
	void handleContainers()
	{
		empty(SLOT_ACID_IN, SLOT_ACID_OUT, acid);
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
		TileHFKiln m = getMaster();
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
		if(getPort() != HFKilnStructure.PORT_REDSTONE)
			return false;
		TileHFKiln m = getMaster();
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

	/** Acid, hydrofluoric acid, energy, progress, state, redstone mode (all below 32767). */
	public int[] guiValues()
	{
		return new int[]{acid.getFluidAmount(), hf.getFluidAmount(), energy.getEnergyStored(), progress, state, redstoneInverted ? 1 : 0};
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
			acid.readFromNBT(m.getCompoundTag("acid"));
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
			m.setTag("acid", acid.writeToNBT(new NBTTagCompound()));
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
		boolean om = mirrored, lit = clientRunning;
		readClient(pkt.getNbtCompound());
		// Redrawn when the part changes; the whole machine when the heaters light up or go out (every part draws a slice)
		if(ox != dx || oy != dy || oz != dz || of != facing || om != mirrored)
			world.markBlockRangeForRenderUpdate(pos, pos);
		if(isMaster() && lit != clientRunning)
		{
			world.markBlockRangeForRenderUpdate(pos.add(-4, -2, -4), pos.add(4, 2, 4));
		}
	}

	boolean isPart(int[] part)
	{
		return dx == part[0] && dy == part[1] && dz == part[2];
	}

	/** IE's looping tile sound asks this every 2 s and ends the loop when it is false. */
	@Override
	public boolean shoudlPlaySound(String sound)
	{
		if(isPart(HEATER_PART))
			return heater.shouldPlay(this, sound);
		if(isPart(DRUM_PART))
			return drumSound.shouldPlay(this, sound);
		return isPart(DRIVE_PART) && drive.shouldPlay(this, sound);
	}

	void stopSound()
	{
		if(world == null || !world.isRemote)
			return;
		if(isPart(DRIVE_PART))
			drive.stop(this);
		else if(isPart(HEATER_PART))
			heater.stop(this);
		else if(isPart(DRUM_PART))
			drumSound.stop(this);
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

	/** The master's box covers the whole machine (any turn): 4 blocks each way, 1 down, 2 up. */
	@Override
	public AxisAlignedBB getRenderBoundingBox()
	{
		return isMaster() ? new AxisAlignedBB(pos.add(-4, -1, -4), pos.add(5, 3, 5)) : super.getRenderBoundingBox();
	}
}
