package immersivenuclear.common.gui;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileEnrichmentBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.items.SlotItemHandler;

/** Interface of the Enrichment Unit, the same for its three assemblies: the natural hexafluoride at the left, the machine drawn in the middle, product and tails at the right, IE's inventory panel under it. */
public class ContainerEnrichmentUnit extends Container
{
	public static final int WIDTH = 330, HEIGHT = 239, INV_X = 77, INV_Y = 157;
	/** Slot positions (top-left of the 16x16 item), shared with the GUI; in the tile's slot order. */
	public static final int[][] SLOT_XY = {{10, 39}, {10, 119}, {252, 39}, {252, 119}, {282, 39}, {282, 119}};

	public final TileEnrichmentBase tile;
	public final FluidTank feed = new FluidTank(TileEnrichmentBase.FEED_CAPACITY);
	public final FluidTank product = new FluidTank(TileEnrichmentBase.PRODUCT_CAPACITY);
	public final FluidTank tails = new FluidTank(TileEnrichmentBase.TAILS_CAPACITY);
	public int energy, progress, state, spin;
	public boolean redstoneInverted;
	final int[] sent = {-1, -1, -1, -1, -1, -1, -1, -1};

	public ContainerEnrichmentUnit(InventoryPlayer playerInv, TileEnrichmentBase tile)
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
			case 0: feed.setFluid(data > 0 ? new FluidStack(INContent.fluidUF6, data) : null); break;
			case 1: product.setFluid(data > 0 ? new FluidStack(tile.productFluid(), data) : null); break;
			case 2: tails.setFluid(data > 0 ? new FluidStack(INContent.fluidUF6Depleted, data) : null); break;
			case 3: energy = data; break;
			case 4: progress = data; break;
			case 5: state = data; break;
			case 6: redstoneInverted = data != 0; break;
			case 7: spin = data; break;
		}
	}

	@Override
	public boolean canInteractWith(EntityPlayer player)
	{
		return !tile.isInvalid() && tile.getWorld().getTileEntity(tile.getPos()) == tile
				&& player.getDistanceSq(tile.getPos().getX() + .5, tile.getPos().getY() + .5, tile.getPos().getZ() + .5) <= 144;
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
