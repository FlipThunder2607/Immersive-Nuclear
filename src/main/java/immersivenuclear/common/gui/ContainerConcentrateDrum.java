package immersivenuclear.common.gui;

import immersivenuclear.common.block.TileConcentrateDrum;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

/** The Concentrate Drum's one slot (yellowcake only) over the player's inventory. */
public class ContainerConcentrateDrum extends Container
{
	public static final int WIDTH = 176, HEIGHT = 166, SLOT_X = 62, SLOT_Y = 35, INV_X = 8, INV_Y = 84;

	final TileConcentrateDrum tile;

	public ContainerConcentrateDrum(InventoryPlayer playerInv, TileConcentrateDrum tile)
	{
		this.tile = tile;
		addSlotToContainer(new SlotItemHandler(tile.getInventory(), 0, SLOT_X, SLOT_Y));
		for(int i = 0; i < 3; i++)
			for(int j = 0; j < 9; j++)
				addSlotToContainer(new Slot(playerInv, j + i * 9 + 9, INV_X + j * 18, INV_Y + i * 18));
		for(int i = 0; i < 9; i++)
			addSlotToContainer(new Slot(playerInv, i, INV_X + i * 18, INV_Y + 58));
	}

	/** Yellowcake in the drum, as the client sees it. */
	public int count()
	{
		return inventorySlots.get(0).getStack().getCount();
	}

	@Override
	public boolean canInteractWith(EntityPlayer player)
	{
		return !tile.isInvalid() && player.getDistanceSq(tile.getPos().add(0.5, 0.5, 0.5)) <= 64;
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index)
	{
		Slot slot = inventorySlots.get(index);
		if(slot == null || !slot.getHasStack())
			return ItemStack.EMPTY;
		ItemStack stack = slot.getStack();
		ItemStack copy = stack.copy();
		if(index == 0)
		{
			if(!mergeItemStack(stack, 1, inventorySlots.size(), true))
				return ItemStack.EMPTY;
		}
		else if(!inventorySlots.get(0).isItemValid(stack) || !mergeItemStack(stack, 0, 1, false))
			return ItemStack.EMPTY;
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
