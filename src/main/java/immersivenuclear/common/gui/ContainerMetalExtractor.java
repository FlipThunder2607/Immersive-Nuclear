package immersivenuclear.common.gui;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileMetalExtractor;
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
 * Interface of the Metal Extractor. 272 x 241 (as wide as the other machines'): the vat and its parts on top, the
 * six electrode plate slots under it (one under each plate), the player inventory at the bottom, centred. Vat, depleted tank, energy, step, state and redstone mode go to the
 * client as window properties (all below 32767).
 */
public class ContainerMetalExtractor extends Container
{
	public static final int WIDTH = 272, HEIGHT = 241, INV_X = 56, INV_Y = 160;
	/** The plate slots: item x of the first, step, item y (under the vat's six plates). */
	public static final int PLATES_X = 60, PLATES_STEP = 18, PLATES_Y = 129;
	/** Slot positions (top-left of the 16x16 item), in the tile's slot order. */
	public static final int[][] SLOT_XY = {
			{176, 56}, // deposit out, at the end of the tray's chute
			{10, 18},  // effluent container in
			{10, 49},  // empty container out
			{221, 55}, // empty container in (depleted effluent)
			{221, 86}, // depleted effluent container out
			{PLATES_X, PLATES_Y}, {PLATES_X + PLATES_STEP, PLATES_Y}, {PLATES_X + 2 * PLATES_STEP, PLATES_Y}, // electrode plates
			{PLATES_X + 3 * PLATES_STEP, PLATES_Y}, {PLATES_X + 4 * PLATES_STEP, PLATES_Y}, {PLATES_X + 5 * PLATES_STEP, PLATES_Y},
	};
	public static final int BUTTON_REDSTONE = 0;

	final TileMetalExtractor tile;
	/** Client copies of the master's values (on the server they are read straight from the tile). */
	public final FluidTank vat = new FluidTank(TileMetalExtractor.BATCH);
	public final FluidTank depleted = new FluidTank(TileMetalExtractor.DEPLETED_CAPACITY);
	public int energy, phase, phaseTicks, state;
	/** The cleaning's tick count, -1 with the platform down; whether it moved this tick. */
	public int collect = -1;
	public boolean vatDepleted, redstoneInverted, collecting;
	final int[] sent = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1};

	public ContainerMetalExtractor(InventoryPlayer playerInv, TileMetalExtractor tile)
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
			case 0: vat.setFluid(data > 0 ? new FluidStack(vatDepleted ? INContent.fluidDepletedEffluent : INContent.fluidEffluent, data) : null); break;
			case 1:
				vatDepleted = data != 0;
				if(vat.getFluidAmount() > 0)
					vat.setFluid(new FluidStack(vatDepleted ? INContent.fluidDepletedEffluent : INContent.fluidEffluent, vat.getFluidAmount()));
				break;
			case 2: depleted.setFluid(data > 0 ? new FluidStack(INContent.fluidDepletedEffluent, data) : null); break;
			case 3: energy = data; break;
			case 4: phase = data; break;
			case 5: phaseTicks = data; break;
			case 6: state = data; break;
			case 7: redstoneInverted = data != 0; break;
			case 8: collect = data - 1; break;
			case 9: collecting = data != 0; break;
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
		int machine = TileMetalExtractor.SLOTS;
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
