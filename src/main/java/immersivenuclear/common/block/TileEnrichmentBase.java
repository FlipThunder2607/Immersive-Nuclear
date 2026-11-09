package immersivenuclear.common.block;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IHammerInteraction;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.ISoundTile;
import immersivenuclear.common.INContent;
import immersivenuclear.common.multiblock.EnrichmentUnitStructure;
import immersivenuclear.common.multiblock.MultiblockEnrichmentUnit;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
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
 * A part of a formed Enrichment Unit; the three assemblies (2, 4 and 6 banks) differ only in their structure and in the numbers below. All quantities are provisional.
 * The centrifuges spin up before they produce: natural hexafluoride goes in, the grade the assembly reaches comes out with the depleted rest.
 */
public abstract class TileEnrichmentBase extends TileEntity implements MachinePartTile, ITickable, IHammerInteraction, ISoundTile
{
	public static final int FEED_CAPACITY = 16000, PRODUCT_CAPACITY = 8000, TAILS_CAPACITY = 16000, PUSH_PER_TICK = 200;
	public static final int PRODUCT_PER_CYCLE = 100, CYCLE_TICKS = 200, SPIN_UP_TICKS = 80;
	public static final int STATE_IDLE = 0, STATE_RUNNING = 1, STATE_DISABLED = 2, STATE_NO_INPUT = 3, STATE_NO_POWER = 4, STATE_OUTPUT_FULL = 5;
	/** Each tank's container slots: in (full or empty container) and out. */
	public static final int SLOT_FEED_IN = 0, SLOT_FEED_OUT = 1, SLOT_PRODUCT_IN = 2, SLOT_PRODUCT_OUT = 3, SLOT_TAILS_IN = 4, SLOT_TAILS_OUT = 5, SLOTS = 6;
	/** Where the converters' and the compressor's sounds play (template offsets); the same blocks in the three assemblies. */
	static final int[] CONVERTER_PART = {1, 1, 0}, COMPRESSOR_PART = {5, 1, 4};

	int dx, dy, dz;
	EnumFacing facing = EnumFacing.NORTH;
	boolean mirrored;
	IBlockState originalState;
	NBTTagCompound originalNbt;
	boolean collisionPlaced;

	// ---------- what each assembly says ----------
	public abstract int banks();

	abstract char code(int dx, int dy, int dz);

	abstract int portOf(int dx, int dy, int dz);

	abstract boolean isCollisionCell(int dx, int dy, int dz);

	abstract int[] faceOf(int dx, int dy, int dz);

	abstract ItemStack stackOf(char code);

	abstract void placeCollision(World world, BlockPos trigger, EnumFacing facing, boolean mirrored);

	/** Natural hexafluoride per cycle: 5, 9 or 30 parts per part of product, as the cascade's size allows. */
	public int feedPerCycle()
	{
		return feedFor(banks());
	}

	public static int feedFor(int banks)
	{
		return PRODUCT_PER_CYCLE * (banks == 2 ? 5 : banks == 4 ? 9 : 30);
	}

	public static int energyFor(int banks)
	{
		return banks == 2 ? 1024 : banks == 4 ? 2048 : 4096;
	}

	public static Fluid productFor(int banks)
	{
		return banks == 2 ? INContent.fluidUF6Low : banks == 4 ? INContent.fluidUF6Medium : INContent.fluidUF6High;
	}

	public int tailsPerCycle()
	{
		return feedPerCycle() - PRODUCT_PER_CYCLE;
	}

	public int energyPerTick()
	{
		return energyFor(banks());
	}

	public int energyCapacity()
	{
		return energyPerTick() * 40;
	}

	public Fluid productFluid()
	{
		return productFor(banks());
	}

	// ---------- master only ----------
	final ItemStackHandler inventory = new ItemStackHandler(SLOTS)
	{
		@Override
		public boolean isItemValid(int slot, ItemStack stack)
		{
			switch(slot)
			{
				case SLOT_FEED_IN: return TileHFKiln.emptyOr(stack, INContent.fluidUF6);
				case SLOT_PRODUCT_IN: return TileHFKiln.emptyOr(stack, productFluid());
				case SLOT_TAILS_IN: return TileHFKiln.emptyOr(stack, INContent.fluidUF6Depleted);
				default: return false;
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
	final FluidTank feed = new FluidTank(FEED_CAPACITY)
	{
		@Override
		public boolean canFillFluidType(FluidStack fluid)
		{
			return fluid != null && fluid.getFluid() == INContent.fluidUF6;
		}

		@Override
		protected void onContentsChanged()
		{
			markDirty();
		}
	};
	final FluidTank product = output(PRODUCT_CAPACITY), tails = output(TAILS_CAPACITY);
	final Energy energy = new Energy();
	int progress, spin;
	int state = STATE_IDLE;
	boolean redstoneInverted;
	int sentState = -1, clientState;
	/** Client, master: the centrifuges' speed from 0 to 1, spinning up and down with the machine (the lamps blink until it is up). */
	public float rotorSpeed;

	{
		feed.setCanDrain(false);
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
			super(energyCapacity(), energyCapacity(), 0);
		}

		boolean use(int amount)
		{
			if(energy < amount)
				return false;
			energy -= amount;
			return true;
		}

		void set(int value)
		{
			energy = Math.min(capacity, value);
		}
	}

	final LoopSound cascade = new LoopSound(() -> INContent.soundEnrichmentCascade[banks() / 2 - 1][0], () -> INContent.soundEnrichmentCascade[banks() / 2 - 1][1], () -> INContent.soundEnrichmentCascade[banks() / 2 - 1][2], 78);
	final LoopSound converters = new LoopSound(() -> INContent.soundEnrichmentConverter[0], () -> INContent.soundEnrichmentConverter[1], () -> INContent.soundEnrichmentConverter[2], 78);
	final LoopSound compressor = new LoopSound(() -> INContent.soundEnrichmentCompressor[0], () -> INContent.soundEnrichmentCompressor[1], () -> INContent.soundEnrichmentCompressor[2], 78);

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
		return code(dx, dy, dz);
	}

	@Override
	public int getPort()
	{
		return portOf(dx, dy, dz);
	}

	/** Server: worked out on the part's first tick. */
	private boolean partChecked, plainPart, staleChecked;

	/** A collision part from an older layout is looked for once per load, not every tick. */
	private boolean staleCandidate()
	{
		if(staleChecked)
			return false;
		staleChecked = true;
		return true;
	}

	public ItemStack getOriginalStack()
	{
		return stackOf(getPartCode()).copy();
	}

	public boolean isMaster()
	{
		return dx == EnrichmentUnitStructure.MASTER_DX && dy == EnrichmentUnitStructure.MASTER_DY && dz == EnrichmentUnitStructure.MASTER_DZ;
	}

	boolean isPart(int[] part)
	{
		return dx == part[0] && dy == part[1] && dz == part[2];
	}

	/** The centrifuges' sound plays from every bank, one block in the middle of each (the banks are 3 blocks apart). */
	boolean isCascadePart()
	{
		return dy == 1 && dz == 0 && dx >= 3 && (dx - 3) % 3 == 0 && (dx - 3) / 3 < banks();
	}

	@Override
	public int[] fromMaster()
	{
		return new int[]{dx - EnrichmentUnitStructure.MASTER_DX, dy - EnrichmentUnitStructure.MASTER_DY, dz - EnrichmentUnitStructure.MASTER_DZ};
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

	public BlockPos getTriggerPos()
	{
		int[] w = EnrichmentUnitStructure.rotate(dx, dz, MultiblockEnrichmentUnit.turns(facing), mirrored);
		return pos.add(-w[0], -dy, -w[1]);
	}

	public BlockPos getMasterPos()
	{
		return MultiblockEnrichmentUnit.toWorld(getTriggerPos(), facing, mirrored, EnrichmentUnitStructure.MASTER_DX, EnrichmentUnitStructure.MASTER_DY, EnrichmentUnitStructure.MASTER_DZ);
	}

	public TileEnrichmentBase getMaster()
	{
		if(isMaster())
			return this;
		if(world == null)
			return null;
		BlockPos m = getMasterPos();
		if(!world.isBlockLoaded(m))
			return null;
		TileEntity te = world.getTileEntity(m);
		return te instanceof TileEnrichmentBase && ((TileEnrichmentBase)te).isMaster() ? (TileEnrichmentBase)te : null;
	}

	/** The machine's state, as the client knows it. */
	public int stateForClient()
	{
		TileEnrichmentBase m = getMaster();
		return m == null ? STATE_IDLE : m.clientState;
	}

	/** The master's box covers the whole machine, whichever way it is turned. */
	@Override
	public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox()
	{
		return isMaster() ? new net.minecraft.util.math.AxisAlignedBB(pos.add(-20, -2, -20), pos.add(21, 8, 21)) : super.getRenderBoundingBox();
	}

	/** Whether it runs (spinning up included), as the client knows it. */
	public boolean running()
	{
		TileEnrichmentBase m = getMaster();
		return m != null && m.clientState == STATE_RUNNING;
	}

	// ---------- ports ----------
	EnumFacing portWorldFace()
	{
		int[] f = faceOf(dx, dy, dz);
		if(f == null || f[0] == 0 && f[1] == 0)
			return null;
		int[] w = EnrichmentUnitStructure.rotate(f[0], f[1], MultiblockEnrichmentUnit.turns(facing), mirrored);
		return EnumFacing.getFacingFromVector(w[0], 0, w[1]);
	}

	boolean portServes(Capability<?> cap, EnumFacing side)
	{
		int port = getPort();
		boolean right;
		if(cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
			right = port == EnrichmentUnitStructure.PORT_FEED || port == EnrichmentUnitStructure.PORT_PRODUCT || port == EnrichmentUnitStructure.PORT_TAILS;
		else if(cap == CapabilityEnergy.ENERGY)
			right = port == EnrichmentUnitStructure.PORT_ENERGY;
		else
			return false;
		if(!right)
			return false;
		EnumFacing face = portWorldFace();
		return side == null || face == null || side == face;
	}

	FluidTank portTank(int port)
	{
		return port == EnrichmentUnitStructure.PORT_FEED ? feed : port == EnrichmentUnitStructure.PORT_PRODUCT ? product : tails;
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
			TileEnrichmentBase m = getMaster();
			if(m != null)
			{
				if(cap == CapabilityEnergy.ENERGY)
					return CapabilityEnergy.ENERGY.cast(m.energy);
				return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(m.portTank(getPort()));
			}
		}
		return super.getCapability(cap, side);
	}

	// ---------- each tick ----------
	@Override
	public void update()
	{
		if(world.isRemote)
		{
			if(!isMaster() && !isCascadePart() && !isPart(CONVERTER_PART) && !isPart(COMPRESSOR_PART))
				return;                                  // the machine's thousand plain parts have nothing to do on the client
			boolean on = running();
			if(isMaster())
			{
				rotorSpeed = on ? Math.min(1F, rotorSpeed + 1F / SPIN_UP_TICKS) : Math.max(0F, rotorSpeed - 0.5F / SPIN_UP_TICKS);
			}
			if(isCascadePart())
				cascade.update(this, on);
			else if(isPart(CONVERTER_PART))
				converters.update(this, on);
			else if(isPart(COMPRESSOR_PART))
				compressor.update(this, on);
			return;
		}
		if(!partChecked)
		{
			partChecked = true;
			plainPart = !isMaster() && getPort() == EnrichmentUnitStructure.PORT_NONE;
		}
		if(plainPart && !staleCandidate())
			return;                                      // a big machine has a thousand of these: nothing to do on the server either
		if(getPartCode() == '.' && !isCollisionCell(dx, dy, dz))
		{
			// An invisible collision part from before its cell was freed (the tile goes first, so the machine is not taken apart)
			world.removeTileEntity(pos);
			world.setBlockToAir(pos);
			return;
		}
		if(getPort() == EnrichmentUnitStructure.PORT_PRODUCT || getPort() == EnrichmentUnitStructure.PORT_TAILS)
			push();
		if(isMaster())
		{
			if(!collisionPlaced)
			{
				collisionPlaced = true;
				placeCollision(world, getTriggerPos(), facing, mirrored);
				markDirty();
			}
			empty(SLOT_FEED_IN, SLOT_FEED_OUT, feed);
			fill(SLOT_PRODUCT_IN, SLOT_PRODUCT_OUT, product);
			fill(SLOT_TAILS_IN, SLOT_TAILS_OUT, tails);
			process();
			if(state != sentState)
			{
				sentState = state;
				IBlockState st = world.getBlockState(pos);
				world.notifyBlockUpdate(pos, st, st, 2);
			}
		}
	}

	void process()
	{
		int before = state;
		if(world.isBlockPowered(getTriggerPos()) != redstoneInverted)
			state = STATE_DISABLED;
		else if(progress == 0 && spin >= SPIN_UP_TICKS && !start())
		{
			// start() set the state
		}
		else if(progress == 0 && spin < SPIN_UP_TICKS && !canStart())
		{
			// canStart() set the state
		}
		else if(!energy.use(energyPerTick()))
			state = STATE_NO_POWER;
		else
		{
			state = STATE_RUNNING;
			if(spin < SPIN_UP_TICKS)
				spin++;
			else if(++progress >= CYCLE_TICKS)
			{
				product.fillInternal(new FluidStack(productFluid(), PRODUCT_PER_CYCLE), true);
				tails.fillInternal(new FluidStack(INContent.fluidUF6Depleted, tailsPerCycle()), true);
				progress = 0;
			}
		}
		if(state != STATE_RUNNING && spin > 0)
			spin = Math.max(0, spin - 2);
		if(state != before)
			markDirty();
	}

	boolean canStart()
	{
		if(feed.getFluidAmount() < feedPerCycle())
		{
			state = STATE_NO_INPUT;
			return false;
		}
		if(product.getCapacity() - product.getFluidAmount() < PRODUCT_PER_CYCLE || tails.getCapacity() - tails.getFluidAmount() < tailsPerCycle())
		{
			state = STATE_OUTPUT_FULL;
			return false;
		}
		if(energy.getEnergyStored() < energyPerTick())
		{
			state = STATE_NO_POWER;
			return false;
		}
		return true;
	}

	boolean start()
	{
		if(!canStart())
			return false;
		feed.drainInternal(feedPerCycle(), true);
		return true;
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

	/** The output ports push into what stands in front of them. */
	void push()
	{
		TileEnrichmentBase m = getMaster();
		EnumFacing face = portWorldFace();
		if(m == null || face == null)
			return;
		FluidTank tank = m.portTank(getPort());
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

	@Override
	public boolean hammerUseSide(EnumFacing side, EntityPlayer player, float hitX, float hitY, float hitZ)
	{
		if(getPort() != EnrichmentUnitStructure.PORT_REDSTONE)
			return false;
		TileEnrichmentBase m = getMaster();
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

	/** Feed, product, tails, energy (out of 32000), progress, state, redstone mode, spin (all below 32767). */
	public int[] guiValues()
	{
		return new int[]{feed.getFluidAmount(), product.getFluidAmount(), tails.getFluidAmount(), (int)(32000L * energy.getEnergyStored() / energyCapacity()), progress, state, redstoneInverted ? 1 : 0, spin};
	}

	// ---------- saving and syncing ----------
	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		readPart(nbt);
		originalState = nbt.hasKey("originalState") ? Block.getStateById(nbt.getInteger("originalState")) : null;
		originalNbt = nbt.hasKey("originalNbt") ? nbt.getCompoundTag("originalNbt") : null;
		collisionPlaced = nbt.getBoolean("collisionPlaced");
		if(nbt.hasKey("machine"))
		{
			NBTTagCompound m = nbt.getCompoundTag("machine");
			feed.readFromNBT(m.getCompoundTag("feed"));
			product.readFromNBT(m.getCompoundTag("product"));
			tails.readFromNBT(m.getCompoundTag("tails"));
			energy.set(m.getInteger("energy"));
			progress = m.getInteger("progress");
			spin = m.getInteger("spin");
			state = m.getInteger("state");
			redstoneInverted = m.getBoolean("redstoneInverted");
			NBTTagCompound inv = m.getCompoundTag("inventory");
			inv.setInteger("Size", SLOTS);
			inventory.deserializeNBT(inv);
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
		nbt.setBoolean("collisionPlaced", collisionPlaced);
		if(isMaster())
		{
			NBTTagCompound m = new NBTTagCompound();
			m.setTag("feed", feed.writeToNBT(new NBTTagCompound()));
			m.setTag("product", product.writeToNBT(new NBTTagCompound()));
			m.setTag("tails", tails.writeToNBT(new NBTTagCompound()));
			m.setInteger("energy", energy.getEnergyStored());
			m.setInteger("progress", progress);
			m.setInteger("spin", spin);
			m.setInteger("state", state);
			m.setBoolean("redstoneInverted", redstoneInverted);
			m.setTag("inventory", inventory.serializeNBT());
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

	void writeClient(NBTTagCompound nbt)
	{
		writePart(nbt);
		if(isMaster())
			nbt.setInteger("state", state);
	}

	void readClient(NBTTagCompound nbt)
	{
		readPart(nbt);
		clientState = nbt.getInteger("state");
	}

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

	// ---------- sounds ----------
	@Override
	public boolean shoudlPlaySound(String sound)
	{
		if(isCascadePart())
			return cascade.shouldPlay(this, sound);
		if(isPart(CONVERTER_PART))
			return converters.shouldPlay(this, sound);
		return isPart(COMPRESSOR_PART) && compressor.shouldPlay(this, sound);
	}

	void stopSound()
	{
		if(world == null || !world.isRemote)
			return;
		if(isCascadePart())
			cascade.stop(this);
		else if(isPart(CONVERTER_PART))
			converters.stop(this);
		else if(isPart(COMPRESSOR_PART))
			compressor.stop(this);
	}

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
}
