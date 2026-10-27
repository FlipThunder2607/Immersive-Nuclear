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
 * Interface of the Chemical Processing Unit. 272 x 210 (as wide as the other machines'): the reactor in the middle, the
 * solids hopper on top of it, the player inventory at the bottom, centred. Tanks, energy, the batch (step, ticks,
 * reactor), what moves, state and redstone mode go to the client as window properties (all below 32767).
 */
public class ContainerChemicalProcessor extends Container
{
	public static final int WIDTH = 272, HEIGHT = 210, INV_X = 56, INV_Y = 129;
	/** Slot positions (top-left of the 16x16 item), shared with the GUI. */
	public static final int[][] SLOT_XY = {
			{70, 12},  // uranium powder, on the hopper
			{90, 12},  // reagent, on the hopper
			{10, 18},  // water or effluent container in (each fluid goes to its tank)
			{32, 18},  // empty container out
			{221, 54}, // empty container in (slurry)
			{221, 85}, // slurry container out
	};
	public static final int BUTTON_REDSTONE = 0;

	final TileChemicalProcessor tile;
	/** Client copies of the master's values (on the server they are read straight from the tile). */
	public final FluidTank water = new FluidTank(TileChemicalProcessor.TANK_CAPACITY);
	public final FluidTank slurry = new FluidTank(TileChemicalProcessor.TANK_CAPACITY);
	public final FluidTank recycled = new FluidTank(TileChemicalProcessor.TANK_CAPACITY);
	public int energy, phase, phaseTicks, reactor, moving, state, recycledType, batchRecycled;
	public boolean redstoneInverted;
	final int[] sent = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};

	public ContainerChemicalProcessor(InventoryPlayer playerInv, TileChemicalProcessor tile)
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

	int[] values()
	{
		return new int[]{tile.getWater(), tile.getSlurry(), tile.getEnergy(), tile.getPhase(), tile.getPhaseTicks(), tile.getReactor(),
				tile.getMoving(), tile.getState(), tile.isRedstoneInverted() ? 1 : 0, tile.getRecycledType(), tile.getRecycled(), tile.getBatchRecycled()};
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
			case 9: recycledType = data; break;
			case 10:
				recycled.setFluid(data > 0 && recycledType > 0 ? new FluidStack(recycledType == 2 ? INContent.fluidDepletedEffluent
						: recycledType == 3 ? INContent.fluidCondensate : INContent.fluidEffluent, data) : null);
				break;
			case 11: batchRecycled = data; break;
			case 0: water.setFluid(data > 0 ? new FluidStack(FluidRegistry.WATER, data) : null); break;
			case 1: slurry.setFluid(data > 0 ? new FluidStack(INContent.fluidSlurry, data) : null); break;
			case 2: energy = data; break;
			case 3: phase = data; break;
			case 4: phaseTicks = data; break;
			case 5: reactor = data; break;
			case 6: moving = data; break;
			case 7: state = data; break;
			case 8: redstoneInverted = data != 0; break;
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
