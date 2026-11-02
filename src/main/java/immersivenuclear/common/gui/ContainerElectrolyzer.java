package immersivenuclear.common.gui;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileElectrolyzer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Interface of the Electrolyzer, in the interface standard (01/11), 322 x 239: the water column at the left, the
 * machine drawn in the middle, the hydrogen and oxygen columns at the right; IE's inventory panel centred under it. The
 * tanks, energy, the cycle's progress, state, redstone mode and the oxygen valve's venting go to the client as window
 * properties (all below 32767). The hammer on the redstone port inverts the mode (no button).
 */
public class ContainerElectrolyzer extends Container
{
	public static final int WIDTH = 322, HEIGHT = 239, INV_X = 81, INV_Y = 157;
	/** Slot positions (top-left of the 16x16 item), shared with the GUI; in TileElectrolyzer's slot order. */
	public static final int[][] SLOT_XY = {
			{10, 39},   // water container, over the water tank
			{10, 119},  // the emptied container, under it
			{244, 39},  // empty container for hydrogen, over its tank
			{244, 119}, // filled with hydrogen, under it
			{274, 39},  // empty container for oxygen, over its tank
			{274, 119}, // filled with oxygen, under it
	};
	public static final int BUTTON_REDSTONE = 0;

	final TileElectrolyzer tile;
	/** Client copies of the master's values (on the server they are read straight from the tile). */
	public final FluidTank water = new FluidTank(TileElectrolyzer.WATER_CAPACITY);
	public final FluidTank h2 = new FluidTank(TileElectrolyzer.H2_CAPACITY);
	public final FluidTank o2 = new FluidTank(TileElectrolyzer.O2_CAPACITY);
	public int energy, progress, state, venting, waterAmount;
	public boolean redstoneInverted, steam;
	final int[] sent = {-1, -1, -1, -1, -1, -1, -1, -1, -1};

	public ContainerElectrolyzer(InventoryPlayer playerInv, TileElectrolyzer tile)
	{
		this.tile = tile;
		for(int i = 0; i < SLOT_XY.length; i++)
			addSlotToContainer(new SlotItemHandler(tile.getInventory(), i, SLOT_XY[i][0], SLOT_XY[i][1]));
		for(int i = 0; i < 3; i++)
			for(int j = 0; j < 9; j++)
				addSlotToContainer(new Slot(playerInv, j + i * 9 + 9, INV_X + j * 18, INV_Y + i * 18));
		for(int i = 0; i < 9; i++)
			addSlotToContainer(new Slot(playerInv, i, INV_X + i * 18, INV_Y + 58));
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

	@Override
	public void updateProgressBar(int id, int data)
	{
		switch(id)
		{
			case 0: waterAmount = data; break;
			case 1: steam = data != 0; break;
			case 2: h2.setFluid(data > 0 ? new FluidStack(INContent.fluidHydrogen, data) : null); break;
			case 3: o2.setFluid(data > 0 ? new FluidStack(INContent.fluidOxygen, data) : null); break;
			case 4: energy = data; break;
			case 5: progress = data; break;
			case 6: state = data; break;
			case 7: redstoneInverted = data != 0; break;
			case 8: venting = data; break;
		}
		net.minecraftforge.fluids.Fluid f = steam ? INContent.fluidSteam : net.minecraftforge.fluids.FluidRegistry.WATER;
		water.setFluid(waterAmount > 0 ? new FluidStack(f, waterAmount) : null);
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
		int machine = SLOT_XY.length;
		if(index < machine)
		{
			if(!mergeItemStack(stack, machine, inventorySlots.size(), true))
				return ItemStack.EMPTY;
		}
		else
		{
			// Slot by slot: mergeItemStack would also top up an output slot holding the same item (filled buckets)
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
