package immersivenuclear.common.gui;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileChemicalProcessor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Interface of the Chemical Processing Unit (layout: design/interface-processamento-quimico-v1.png). Slots sit where
 * IE's refinery puts its frames; tanks, energy, progress, state and redstone mode go to the client as window
 * properties (all below 32767, which is what those carry).
 */
public class ContainerChemicalProcessor extends Container
{
	/** Slot positions (top-left of the 16x16 item), shared with the GUI. */
	public static final int[][] SLOT_XY = {
			{58, 15},  // uranium powder
			{58, 54},  // reagent
			{11, 15},  // water container in
			{11, 54},  // empty water container out
			{106, 15}, // empty container in (polpa)
			{106, 54}, // polpa container out
	};
	public static final int BUTTON_REDSTONE = 0;

	final TileChemicalProcessor tile;
	/** Client copies of the master's values (on the server they are read straight from the tile). */
	public final FluidTank water = new FluidTank(TileChemicalProcessor.TANK_CAPACITY);
	public final FluidTank polpa = new FluidTank(TileChemicalProcessor.TANK_CAPACITY);
	public int energy, progress, state;
	public boolean redstoneInverted;
	final int[] sent = {-1, -1, -1, -1, -1, -1};

	public ContainerChemicalProcessor(InventoryPlayer playerInv, TileChemicalProcessor tile)
	{
		this.tile = tile;
		for(int i = 0; i < SLOT_XY.length; i++)
			addSlotToContainer(new SlotItemHandler(tile.getInventory(), i, SLOT_XY[i][0], SLOT_XY[i][1]));
		// Player inventory where IE's refinery has it
		for(int i = 0; i < 3; i++)
			for(int j = 0; j < 9; j++)
				addSlotToContainer(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 85 + i * 18));
		for(int i = 0; i < 9; i++)
			addSlotToContainer(new Slot(playerInv, i, 8 + i * 18, 143));
		read();
	}

	int[] values()
	{
		return new int[]{tile.getWater(), tile.getPolpa(), tile.getEnergy(), tile.getProgress(), tile.getState(), tile.isRedstoneInverted() ? 1 : 0};
	}

	/** Server side: mirror the tile into the client copies, so the same fields can be read on both sides. */
	void read()
	{
		int[] v = values();
		for(int i = 0; i < v.length; i++)
			updateProgressBar(i, v[i]);
	}

	@Override
	public void detectAndSendChanges()
	{
		super.detectAndSendChanges();
		int[] v = values();
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
			case 0: water.setFluid(data > 0 ? new FluidStack(FluidRegistry.WATER, data) : null); break;
			case 1: polpa.setFluid(data > 0 ? new FluidStack(INContent.fluidPolpa, data) : null); break;
			case 2: energy = data; break;
			case 3: progress = data; break;
			case 4: state = data; break;
			case 5: redstoneInverted = data != 0; break;
		}
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
