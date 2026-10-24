package immersivenuclear.common.gui;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileChemicalSeparator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Interface of the Chemical Separation Unit. 272 x 232: the process plate on top (two rows, one per stage, with the
 * solvent loop between them, bucket slots at both ends), a column for energy, redstone and status on the right, IE's
 * 176 wide inventory panel centred at the bottom. Everything the tile shows goes to the client as window properties
 * (all below 32767).
 */
public class ContainerChemicalSeparator extends Container
{
	/** INV_X: left of IE's inventory panel; INV_Y: top of the first inventory row (the panel starts 8 above). */
	public static final int WIDTH = 272, HEIGHT = 242, INV_X = 48, INV_Y = 161;
	/** Bucket slots (top-left of the 16x16 item), in the tile's slot order. */
	public static final int[][] SLOT_XY = {
			{10, 18}, {10, 49},     // crude solution: full in, empty back
			{10, 98}, {10, 129},    // stripping solution: full in, empty back
			{221, 18}, {221, 49},   // effluent: empty in, full out
			{221, 98}, {221, 129},  // purified solution: empty in, full out
	};
	public static final int BUTTON_REDSTONE = 0;
	public static final int V_CRUDE = 0, V_STRIPPING = 1, V_SOLVENT = 2, V_EFFLUENT = 3, V_PURIFIED = 4, V_LOADED = 5, V_ENERGY = 6,
			V_STATE = 7, V_REDSTONE = 8, V_A = 9, V_B = 13, V_FLOW = 17, VALUES = 18;

	final TileChemicalSeparator tile;
	/** Client copies of the master's values (on the server they are read straight from the tile). */
	public final FluidTank crude = new FluidTank(TileChemicalSeparator.TANK_CAPACITY), stripping = new FluidTank(TileChemicalSeparator.TANK_CAPACITY),
			solvent = new FluidTank(TileChemicalSeparator.SOLVENT_CAPACITY), effluent = new FluidTank(TileChemicalSeparator.TANK_CAPACITY),
			purified = new FluidTank(TileChemicalSeparator.TANK_CAPACITY), loaded = new FluidTank(TileChemicalSeparator.TANK_CAPACITY);
	public final int[] values = new int[VALUES];
	final int[] sent = new int[VALUES];

	public ContainerChemicalSeparator(InventoryPlayer playerInv, TileChemicalSeparator tile)
	{
		this.tile = tile;
		java.util.Arrays.fill(sent, -1);
		for(int i = 0; i < SLOT_XY.length; i++)
			addSlotToContainer(new SlotItemHandler(tile.getInventory(), i, SLOT_XY[i][0], SLOT_XY[i][1]));
		for(int i = 0; i < 3; i++)
			for(int j = 0; j < 9; j++)
				addSlotToContainer(new Slot(playerInv, j + i * 9 + 9, INV_X + 8 + j * 18, INV_Y + i * 18));
		for(int i = 0; i < 9; i++)
			addSlotToContainer(new Slot(playerInv, i, INV_X + 8 + i * 18, INV_Y + 58));
		read();
	}

	/** Server side: mirror the tile into the client copies, so the same fields can be read on both sides. */
	void read()
	{
		int[] v = tile.guiValues();
		for(int i = 0; i < v.length; i++)
			updateProgressBar(i, v[i]);
	}

	@Override
	public void detectAndSendChanges()
	{
		super.detectAndSendChanges();
		int[] v = tile.guiValues();
		for(int i = 0; i < v.length; i++)
			if(v[i] != sent[i])
			{
				for(IContainerListener l : listeners)
					l.sendWindowProperty(this, i, v[i]);
				sent[i] = v[i];
			}
		read();
	}

	static void set(FluidTank tank, Fluid fluid, int amount)
	{
		tank.setFluid(amount > 0 ? new FluidStack(fluid, amount) : null);
	}

	@Override
	public void updateProgressBar(int id, int data)
	{
		if(id < 0 || id >= VALUES)
			return;
		values[id] = data;
		switch(id)
		{
			case V_CRUDE: set(crude, INContent.fluidCrudeSolution, data); break;
			case V_STRIPPING: set(stripping, INContent.fluidStrippingSolution, data); break;
			case V_SOLVENT: set(solvent, INContent.fluidSolvent, data); break;
			case V_EFFLUENT: set(effluent, INContent.fluidEffluent, data); break;
			case V_PURIFIED: set(purified, INContent.fluidPurifiedSolution, data); break;
			case V_LOADED: set(loaded, INContent.fluidSolvent, data); break;
		}
	}

	public int energy()
	{
		return values[V_ENERGY];
	}

	public int state()
	{
		return values[V_STATE];
	}

	public boolean redstoneInverted()
	{
		return values[V_REDSTONE] != 0;
	}

	/** Settler A (0) or B (1): {phase, ticks, top, bottom}. */
	public int[] settler(int which)
	{
		int o = which == 0 ? V_A : V_B;
		return new int[]{values[o], values[o + 1], values[o + 2], values[o + 3]};
	}

	/** Bits of what moved last tick: 1 mixer A, 2 mixer B, 4 loaded solvent line, 8 clean solvent line. */
	public boolean moving(int bit)
	{
		return (values[V_FLOW] & bit) != 0;
	}

	/** The redstone mode button comes back through the vanilla "enchant" button packet. */
	@Override
	public boolean enchantItem(EntityPlayer player, int id)
	{
		if(id != BUTTON_REDSTONE)
			return false;
		tile.toggleRedstone();
		return true;
	}

	@Override
	public boolean canInteractWith(EntityPlayer player)
	{
		return !tile.isInvalid() && tile.getWorld().getTileEntity(tile.getPos()) == tile
				&& player.getDistanceSq(tile.getPos().getX() + .5, tile.getPos().getY() + .5, tile.getPos().getZ() + .5) <= 100;
	}

	/** Shift click: machine to player; player to the first machine slot that takes the item. */
	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index)
	{
		Slot slot = inventorySlots.get(index);
		if(slot == null || !slot.getHasStack())
			return ItemStack.EMPTY;
		ItemStack stack = slot.getStack();
		ItemStack copy = stack.copy();
		int machine = TileChemicalSeparator.SLOTS;
		if(index < machine)
		{
			if(!mergeItemStack(stack, machine, inventorySlots.size(), true))
				return ItemStack.EMPTY;
		}
		else
		{
			// Slot by slot: mergeItemStack would also top up an output slot holding the same item (empty buckets)
			boolean moved = false;
			for(int i = 0; i < machine && !stack.isEmpty(); i++)
				if(inventorySlots.get(i).isItemValid(stack))
					moved |= mergeItemStack(stack, i, i + 1, false);
			if(!moved)
				return ItemStack.EMPTY;
		}
		if(stack.isEmpty())
			slot.putStack(ItemStack.EMPTY);
		else
			slot.onSlotChanged();
		if(stack.getCount() == copy.getCount())
			return ItemStack.EMPTY;
		slot.onTake(player, stack);
		return copy;
	}
}
