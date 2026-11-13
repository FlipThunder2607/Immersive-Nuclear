package immersivenuclear.common.gui;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileDeconversionUnit;
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
 * Interface of the Deconversion Unit, in the interface standard (01/11), 330 x 239: the depleted hexafluoride and steam
 * columns at the left, the reactor on the feed hood, the rotary kiln through the electric furnace, the oxide's box
 * against the right panel, the candle filters and the condenser, the HF column at the right; IE's inventory panel
 * centred under it. The tanks, energy, the cycle's progress, state and redstone mode go to the client as window
 * properties (all below 32767). The hammer on the redstone port inverts the mode (no button).
 */
public class ContainerDeconversionUnit extends Container
{
	public static final int WIDTH = 330, HEIGHT = 239, INV_X = 77, INV_Y = 157;
	/** Slot positions (top-left of the 16x16 item), shared with the GUI; in TileDeconversionUnit's slot order. */
	public static final int[][] SLOT_XY = {
			{247, 119}, // oxide, in its box against the right panel
			{10, 39},   // depleted hexafluoride container, over its tank
			{10, 119},  // the emptied container, under it
			{40, 39},   // steam container, over its tank
			{40, 119},  // the emptied container, under it
			{282, 39},  // empty container for hydrofluoric acid, over its tank
			{282, 119}, // filled with hydrofluoric acid, under it
	};

	final TileDeconversionUnit tile;
	/** Client copies of the master's values (on the server they are read straight from the tile). */
	public final FluidTank uf6 = new FluidTank(TileDeconversionUnit.UF6_CAPACITY);
	public final FluidTank steam = new FluidTank(TileDeconversionUnit.STEAM_CAPACITY);
	public final FluidTank hf = new FluidTank(TileDeconversionUnit.HF_CAPACITY);
	public int energy, progress, state;
	public boolean redstoneInverted;
	final int[] sent = {-1, -1, -1, -1, -1, -1, -1};

	public ContainerDeconversionUnit(InventoryPlayer playerInv, TileDeconversionUnit tile)
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

	static FluidStack stack(Fluid fluid, int amount)
	{
		return amount > 0 && fluid != null ? new FluidStack(fluid, amount) : null;
	}

	@Override
	public void updateProgressBar(int id, int data)
	{
		switch(id)
		{
			case 0: uf6.setFluid(stack(INContent.fluidUF6Depleted, data)); break;
			case 1: steam.setFluid(stack(INContent.fluidSteam, data)); break;
			case 2: hf.setFluid(stack(INContent.fluidHydrofluoricAcid, data)); break;
			case 3: energy = data; break;
			case 4: progress = data; break;
			case 5: state = data; break;
			case 6: redstoneInverted = data != 0; break;
		}
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
