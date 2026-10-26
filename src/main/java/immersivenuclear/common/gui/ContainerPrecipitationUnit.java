package immersivenuclear.common.gui;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TilePrecipitationUnit;
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
 * Interface of the Precipitation Unit. 272 x 210 (as wide as the other machines'): solution buckets, agitated tank with the
 * agent's funnel on top, screw dewaterer with the concentrate on top, effluent tank; the player inventory at the
 * bottom, centred. Tanks, energy, the batch (step, ticks), state and redstone mode go to the client as window
 * properties (all below 32767).
 */
public class ContainerPrecipitationUnit extends Container
{
	public static final int WIDTH = 272, HEIGHT = 210, INV_X = 56, INV_Y = 129;
	/** Slot positions (top-left of the 16x16 item), shared with the GUI; in TilePrecipitationUnit's slot order. */
	public static final int[][] SLOT_XY = {
			{76, 12},  // precipitating agent, on the funnel
			{192, 12}, // wet concentrate, at the discharge head
			{18, 38},  // solution container in, its pipe into the tank
			{18, 82},  // empty container out, under it
			{220, 12}, // empty container in, over the effluent tank
			{220, 96}, // effluent container out, under it
	};
	public static final int BUTTON_REDSTONE = 0;

	final TilePrecipitationUnit tile;
	/** Client copies of the master's values (on the server they are read straight from the tile). */
	public final FluidTank effluent = new FluidTank(TilePrecipitationUnit.EFFLUENT_CAPACITY);
	public int mixer, energy, phase, phaseTicks, state;
	public boolean redstoneInverted;
	final int[] sent = {-1, -1, -1, -1, -1, -1, -1};

	public ContainerPrecipitationUnit(InventoryPlayer playerInv, TilePrecipitationUnit tile)
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
			case 0: mixer = data; break;
			case 1: effluent.setFluid(data > 0 ? new FluidStack(INContent.fluidEffluent, data) : null); break;
			case 2: energy = data; break;
			case 3: phase = data; break;
			case 4: phaseTicks = data; break;
			case 5: state = data; break;
			case 6: redstoneInverted = data != 0; break;
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
