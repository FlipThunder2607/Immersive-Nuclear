package immersivenuclear.common.block;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IHammerInteraction;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.ISoundTile;
import immersivenuclear.common.INContent;
import immersivenuclear.common.multiblock.MultiblockSulfuricAcidUnit;
import immersivenuclear.common.multiblock.SulfuricAcidUnitStructure;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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
import net.minecraftforge.oredict.OreDictionary;

/**
 * One part of a formed Sulfuric Acid Unit: which template block it replaced and how the structure is turned. The
 * master part also holds the machine; the port parts hand their capability, on their own face only, to the master
 * (see {@link SulfuricAcidUnitStructure#PORTS}).
 *
 * The contact process, in cycles of 5 s: a cycle starts with one sulfur (any mod's dust, gem or crystal; a block of
 * sulfur makes one cycle 9 times as long, with 9 times the water and the output) and its water (and room for its acid);
 * while it runs it draws power (the blower and the pump): the sulfur burns, the boiler cools the gas and makes steam,
 * the converter turns it into sulfur trioxide and the absorption tower into sulfuric acid, which goes into its tank
 * at the end with the steam. With the steam tank full the steam is vented through the steam drum's relief valve
 * (a cloud and a hiss), so it never stops the machine. Without power the cycle waits; a redstone signal stops it (the
 * hammer inverts that). While it runs a faint plume leaves the absorption tower's stack.
 */
public class TileSulfuricAcidUnit extends TileEntity implements ITickable, IHammerInteraction, ISoundTile
{
	// Provisional values
	public static final int WATER_PER_CYCLE = 500, ACID_PER_CYCLE = 500, STEAM_PER_CYCLE = 250, CYCLE_TICKS = 100, ENERGY_PER_TICK = 32;
	public static final int WATER_CAPACITY = 8000, ACID_CAPACITY = 8000, STEAM_CAPACITY = 4000, ENERGY_CAPACITY = 16000, PUSH_PER_TICK = 100;
	public static final int STATE_IDLE = 0, STATE_RUNNING = 1, STATE_DISABLED = 2, STATE_NO_INPUT = 3, STATE_NO_POWER = 4, STATE_OUTPUT_FULL = 5;
	/** Each tank has its container slots under it: in (empty or full container) and out. */
	public static final int SLOT_SULFUR = 0, SLOT_ACID_IN = 1, SLOT_ACID_OUT = 2, SLOT_WATER_IN = 3, SLOT_WATER_OUT = 4, SLOT_STEAM_IN = 5,
			SLOT_STEAM_OUT = 6, SLOTS = 7;

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
				case SLOT_SULFUR:
					return isSulfur(stack);
				case SLOT_WATER_IN:
					FluidStack w = FluidUtil.getFluidContained(stack);
					return w != null && w.getFluid() == FluidRegistry.WATER;
				case SLOT_ACID_IN:
					return emptyOr(stack, INContent.fluidSulfuricAcid);
				case SLOT_STEAM_IN:
					return emptyOr(stack, INContent.fluidSteam);
				default:
					// the filled containers only come from the machine
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
	final FluidTank water = new FluidTank(WATER_CAPACITY)
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
	final FluidTank acid = new FluidTank(ACID_CAPACITY)
	{
		@Override
		protected void onContentsChanged()
		{
			markDirty();
		}
	};
	final FluidTank steam = new FluidTank(STEAM_CAPACITY)
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
	/** The current cycle's size: 1 for a sulfur dust, 9 for a block of sulfur. */
	int cycleSize = 1;
	int state = STATE_IDLE;
	boolean redstoneInverted;
	/**
	 * Where each sound plays (template offsets): the burner (with its blower), the pump tank under the tower, the steam
	 * drum (its relief valve).
	 */
	static final int[] BURNER_PART = {1, 0, 2}, PUMP_PART = {3, 0, 5}, DRUM_PART = {3, 3, 1};
	/**
	 * Where the effects come out, in template blocks from the trigger block's corner (as the model draws them): the
	 * steam out of the relief valve's vent on the steam drum (model x 11.65, y 11.05, z 9.15), the faint plume out of the
	 * absorption tower's stack, just over its rain cap (model x 4.9, y 16.05, z 6.0). Template x = (model z - 3) / 2,
	 * y = model y / 2 - 1, z = (15 - model x) / 2.
	 */
	static final double[] RELIEF_AT = {3.08, 4.55, 1.68}, STACK_AT = {1.5, 7.1, 5.05};
	/** The loops come in as their starts end (burner 2.5 s, pump 1 s). */
	final LoopSound burner = new LoopSound(() -> INContent.soundAcidBurnerStart, () -> INContent.soundAcidBurnerLoop, () -> INContent.soundAcidBurnerStop, 50);
	final LoopSound pump = new LoopSound(() -> INContent.soundAcidPumpStart, () -> INContent.soundAcidPumpLoop, () -> INContent.soundAcidPumpStop, 20);
	final LoopSound vent = new LoopSound(() -> INContent.soundAcidVentStart, () -> INContent.soundAcidVentLoop, () -> INContent.soundAcidVentStop, 10);

	/** Running, as last sent to the clients (server), or as received (client): lights the burner. */
	boolean sentRunning, clientRunning;
	/** The steam tank is full while it runs, so the relief valve vents: as last sent / as received. */
	boolean sentVenting, clientVenting;

	public TileSulfuricAcidUnit()
	{
		water.setCanDrain(false);
		acid.setCanFill(false);
		steam.setCanFill(false);
	}

	/** A fluid container that is empty or already holds this fluid (compared by name: the fluid may be another mod's). */
	static boolean emptyOr(ItemStack stack, net.minecraftforge.fluids.Fluid fluid)
	{
		FluidStack held = FluidUtil.getFluidContained(stack);
		return FluidUtil.getFluidHandler(stack) != null && (held == null || held.getFluid().getName().equals(fluid.getName()));
	}

	/** Sulfur in any mod's form: one cycle's worth, by ore dictionary name. */
	static final String[] SULFUR = {"dustSulfur", "gemSulfur", "crystalSulfur", "itemSulfur"};
	/** A block of sulfur: nine cycles' worth in one long cycle. */
	public static final String SULFUR_BLOCK = "blockSulfur";
	public static final int BLOCK_SIZE = 9;

	public static boolean isSulfur(ItemStack stack)
	{
		return sulfurSize(stack) > 0;
	}

	/** Cycles' worth of sulfur in one item: 1, {@link #BLOCK_SIZE} for a block, 0 if it is not sulfur. */
	public static int sulfurSize(ItemStack stack)
	{
		if(stack.isEmpty())
			return 0;
		for(int id : OreDictionary.getOreIDs(stack))
		{
			String name = OreDictionary.getOreName(id);
			if(SULFUR_BLOCK.equals(name))
				return BLOCK_SIZE;
			for(String n : SULFUR)
				if(n.equals(name))
					return 1;
		}
		return 0;
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

	/** The sulfur port: takes sulfur into its slot, never gives anything out. */
	final IItemHandler sulfurPort = new IItemHandler()
	{
		@Override
		public int getSlots()
		{
			return 1;
		}

		@Override
		public ItemStack getStackInSlot(int i)
		{
			return inventory.getStackInSlot(SLOT_SULFUR);
		}

		@Override
		public ItemStack insertItem(int i, ItemStack stack, boolean simulate)
		{
			return inventory.insertItem(SLOT_SULFUR, stack, simulate);
		}

		@Override
		public ItemStack extractItem(int i, int amount, boolean simulate)
		{
			return ItemStack.EMPTY;
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
		return SulfuricAcidUnitStructure.get(dx, dy, dz);
	}

	public int getPort()
	{
		return SulfuricAcidUnitStructure.portAt(dx, dy, dz);
	}

	/** What a broken part drops: the block it replaced. */
	public ItemStack getOriginalStack()
	{
		return MultiblockSulfuricAcidUnit.stackFor(getPartCode()).copy();
	}

	public boolean isMaster()
	{
		return dx == SulfuricAcidUnitStructure.MASTER_DX && dy == SulfuricAcidUnitStructure.MASTER_DY && dz == SulfuricAcidUnitStructure.MASTER_DZ;
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
		int[] w = SulfuricAcidUnitStructure.rotate(dx, dz, MultiblockSulfuricAcidUnit.turns(facing), mirrored);
		return pos.add(-w[0], -dy, -w[1]);
	}

	public BlockPos getMasterPos()
	{
		return MultiblockSulfuricAcidUnit.toWorld(getTriggerPos(), facing, mirrored,
				SulfuricAcidUnitStructure.MASTER_DX, SulfuricAcidUnitStructure.MASTER_DY, SulfuricAcidUnitStructure.MASTER_DZ);
	}

	public TileSulfuricAcidUnit getMaster()
	{
		if(isMaster())
			return this;
		if(world == null)
			return null;
		BlockPos m = getMasterPos();
		if(!world.isBlockLoaded(m))
			return null;
		TileEntity te = world.getTileEntity(m);
		return te instanceof TileSulfuricAcidUnit && ((TileSulfuricAcidUnit)te).isMaster() ? (TileSulfuricAcidUnit)te : null;
	}

	/** World face a port works on, or null when it works on every side. */
	EnumFacing portWorldFace()
	{
		int[] f = SulfuricAcidUnitStructure.portFace(dx, dy, dz);
		if(f == null || f[0] == 0 && f[1] == 0)
			return null;
		int[] w = SulfuricAcidUnitStructure.rotate(f[0], f[1], MultiblockSulfuricAcidUnit.turns(facing), mirrored);
		return EnumFacing.getFacingFromVector(w[0], 0, w[1]);
	}

	boolean portServes(Capability<?> cap, EnumFacing side)
	{
		int port = getPort();
		boolean right;
		if(cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
			right = port == SulfuricAcidUnitStructure.PORT_WATER || port == SulfuricAcidUnitStructure.PORT_ACID || port == SulfuricAcidUnitStructure.PORT_STEAM;
		else if(cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			right = port == SulfuricAcidUnitStructure.PORT_SULFUR;
		else if(cap == CapabilityEnergy.ENERGY)
			right = port == SulfuricAcidUnitStructure.PORT_ENERGY;
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
			TileSulfuricAcidUnit m = getMaster();
			if(m != null)
			{
				if(cap == CapabilityEnergy.ENERGY)
					return CapabilityEnergy.ENERGY.cast(m.energy);
				if(cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
					return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(m.sulfurPort);
				int port = getPort();
				return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(port == SulfuricAcidUnitStructure.PORT_WATER ? m.water
						: port == SulfuricAcidUnitStructure.PORT_ACID ? m.acid : m.steam);
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
				effects();
			if(isPart(BURNER_PART))
				burner.update(this, burnerLit());
			else if(isPart(PUMP_PART))
				pump.update(this, burnerLit());
			else if(isPart(DRUM_PART))
				vent.update(this, venting());
			return;
		}
		int port = getPort();
		if(port == SulfuricAcidUnitStructure.PORT_ACID || port == SulfuricAcidUnitStructure.PORT_STEAM)
			push(port);
		if(isMaster())
		{
			handleContainers();
			process();
			sync();
		}
	}

	// ---------- the contact process ----------
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
			if(++progress >= CYCLE_TICKS * cycleSize)
			{
				acid.fillInternal(new FluidStack(INContent.fluidSulfuricAcid, ACID_PER_CYCLE * cycleSize), true);
				// With its tank full the steam goes out of the vent
				steam.fillInternal(new FluidStack(INContent.fluidSteam, STEAM_PER_CYCLE * cycleSize), true);
				progress = 0;
			}
		}
		if(state != before)
			markDirty();
	}

	/** Tells the clients when the machine starts or stops, so the burner lights up or goes out. */
	void sync()
	{
		boolean running = state == STATE_RUNNING, venting = isVenting();
		if(running != sentRunning || venting != sentVenting)
		{
			sentRunning = running;
			sentVenting = venting;
			IBlockState st = world.getBlockState(pos);
			world.notifyBlockUpdate(pos, st, st, 2);
		}
	}

	/** Running with no room for a cycle's steam: the relief valve vents it. */
	boolean isVenting()
	{
		return state == STATE_RUNNING && steam.getFluidAmount() + STEAM_PER_CYCLE * cycleSize > STEAM_CAPACITY;
	}

	/** Whether the relief valve vents, as the client knows it. */
	public boolean venting()
	{
		TileSulfuricAcidUnit m = getMaster();
		return m != null && m.clientVenting;
	}

	/** Client, on the master: the plume out of the stack while it runs, the relief valve's cloud while it vents. */
	void effects()
	{
		java.util.Random r = world.rand;
		if(clientRunning && r.nextInt(8) == 0)
		{
			// the clean gas: a thin, slow plume straight up off the cap
			Vec3d p = templatePoint(STACK_AT[0] + (r.nextDouble() - .5) * .1, STACK_AT[1], STACK_AT[2] + (r.nextDouble() - .5) * .1);
			world.spawnParticle(EnumParticleTypes.CLOUD, p.x, p.y, p.z, 0, .02 + r.nextDouble() * .015, 0);
		}
		if(clientVenting)
			for(int k = 0; k < 2; k++)
			{
				Vec3d p = templatePoint(RELIEF_AT[0] + (r.nextDouble() - .5) * .1, RELIEF_AT[1], RELIEF_AT[2] + (r.nextDouble() - .5) * .1);
				world.spawnParticle(EnumParticleTypes.CLOUD, p.x, p.y, p.z, (r.nextDouble() - .5) * .06, .12 + r.nextDouble() * .08, (r.nextDouble() - .5) * .06);
			}
	}

	/** World point of a point given in template blocks from the trigger block's corner (any turn, mirrored or not). */
	Vec3d templatePoint(double x, double y, double z)
	{
		int turns = MultiblockSulfuricAcidUnit.turns(facing);
		int[] ex = SulfuricAcidUnitStructure.rotate(1, 0, turns, mirrored), ez = SulfuricAcidUnitStructure.rotate(0, 1, turns, mirrored);
		BlockPos t = getTriggerPos();
		double u = x - .5, v = z - .5;
		return new Vec3d(t.getX() + .5 + ex[0] * u + ez[0] * v, t.getY() + y, t.getZ() + .5 + ex[1] * u + ez[1] * v);
	}

	/** Whether the burner burns: the master's running state, as the client knows it. */
	public boolean burnerLit()
	{
		TileSulfuricAcidUnit m = getMaster();
		return m != null && m.clientRunning;
	}

	/** A cycle starts with its sulfur and water in and room for its acid; they are used up as it starts. */
	boolean start()
	{
		ItemStack sulfur = inventory.getStackInSlot(SLOT_SULFUR);
		int size = sulfurSize(sulfur);
		if(size == 0 || water.getFluidAmount() < WATER_PER_CYCLE * size)
		{
			state = STATE_NO_INPUT;
			return false;
		}
		if(acid.getCapacity() - acid.getFluidAmount() < ACID_PER_CYCLE * size)
		{
			state = STATE_OUTPUT_FULL;
			return false;
		}
		if(energy.getEnergyStored() < ENERGY_PER_TICK)
		{
			state = STATE_NO_POWER;
			return false;
		}
		sulfur.shrink(1);
		inventory.setStackInSlot(SLOT_SULFUR, sulfur);
		water.drainInternal(WATER_PER_CYCLE * size, true);
		cycleSize = size;
		return true;
	}

	/** Water containers empty into the water tank; empty containers fill with acid or steam. Each into its out slot. */
	void handleContainers()
	{
		empty(SLOT_WATER_IN, SLOT_WATER_OUT, water);
		fill(SLOT_ACID_IN, SLOT_ACID_OUT, acid);
		fill(SLOT_STEAM_IN, SLOT_STEAM_OUT, steam);
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

	/** An output port pushes its fluid into what stands in front of it. */
	void push(int port)
	{
		TileSulfuricAcidUnit m = getMaster();
		EnumFacing face = portWorldFace();
		if(m == null || face == null)
			return;
		FluidTank tank = port == SulfuricAcidUnitStructure.PORT_ACID ? m.acid : m.steam;
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

	/** Hammer on the redstone port: invert the redstone control, like IE's machines, and say so in the chat. */
	@Override
	public boolean hammerUseSide(EnumFacing side, EntityPlayer player, float hitX, float hitY, float hitZ)
	{
		if(getPort() != SulfuricAcidUnitStructure.PORT_REDSTONE)
			return false;
		TileSulfuricAcidUnit m = getMaster();
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

	/** Water, acid, steam, energy, progress, state, redstone mode, venting, cycle size (all below 32767). */
	public int[] guiValues()
	{
		return new int[]{water.getFluidAmount(), acid.getFluidAmount(), steam.getFluidAmount(), energy.getEnergyStored(), progress, state, redstoneInverted ? 1 : 0,
				isVenting() ? 1 : 0, cycleSize};
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
			water.readFromNBT(m.getCompoundTag("water"));
			acid.readFromNBT(m.getCompoundTag("acid"));
			steam.readFromNBT(m.getCompoundTag("steam"));
			energy.set(m.getInteger("energy"));
			progress = m.getInteger("progress");
			cycleSize = Math.max(1, m.getInteger("cycleSize"));
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
			m.setTag("acid", acid.writeToNBT(new NBTTagCompound()));
			m.setTag("steam", steam.writeToNBT(new NBTTagCompound()));
			m.setInteger("energy", energy.getEnergyStored());
			m.setInteger("progress", progress);
			m.setInteger("cycleSize", cycleSize);
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
			nbt.setBoolean("venting", isVenting());
		}
	}

	void readClient(NBTTagCompound nbt)
	{
		readPart(nbt);
		clientRunning = nbt.getBoolean("running");
		clientVenting = nbt.getBoolean("venting");
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt)
	{
		int ox = dx, oy = dy, oz = dz;
		EnumFacing of = facing;
		boolean om = mirrored, lit = clientRunning;
		readClient(pkt.getNbtCompound());
		// Redrawn when the part changes; the whole machine when the burner lights up or goes out (every part draws a slice)
		if(ox != dx || oy != dy || oz != dz || of != facing || om != mirrored)
			world.markBlockRangeForRenderUpdate(pos, pos);
		if(isMaster() && lit != clientRunning)
		{
			world.markBlockRangeForRenderUpdate(pos.add(-4, -2, -4), pos.add(4, 6, 4));
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
		if(isPart(BURNER_PART))
			return burner.shouldPlay(this, sound);
		if(isPart(DRUM_PART))
			return vent.shouldPlay(this, sound);
		return isPart(PUMP_PART) && pump.shouldPlay(this, sound);
	}

	void stopSound()
	{
		if(world == null || !world.isRemote)
			return;
		if(isPart(BURNER_PART))
			burner.stop(this);
		else if(isPart(PUMP_PART))
			pump.stop(this);
		else if(isPart(DRUM_PART))
			vent.stop(this);
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

	/** The master's box covers the whole machine (any turn): 3 blocks each way, 2 down, 7 up to the stack's top. */
	@Override
	public AxisAlignedBB getRenderBoundingBox()
	{
		return isMaster() ? new AxisAlignedBB(pos.add(-3, -2, -3), pos.add(4, 8, 4)) : super.getRenderBoundingBox();
	}
}
