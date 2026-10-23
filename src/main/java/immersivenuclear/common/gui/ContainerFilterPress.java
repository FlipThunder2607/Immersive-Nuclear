package immersivenuclear.common.gui;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileFilterPress;
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
 * Interface of the Filtration Unit (layout: design/2-unidade-de-filtro-prensa/interface-filtracao-v1.png, approved by
 * the user). 176 x 248: fluids on top, the 28 filter slots below, the player inventory at the bottom. Tanks, energy,
 * pump, step, state and redstone mode go to the client as window properties (all below 32767).
 */
public class ContainerFilterPress extends Container
{
	public static final int WIDTH = 176, HEIGHT = 248, INV_Y = 166;
	public static final int FILTERS_X = 9, FILTERS_Y = 84, FILTER_COLUMNS = 7;
	/** Slot positions (top-left of the 16x16 item) after the filters, in the tile's slot order. */
	public static final int[][] SLOT_XY = {
			{142, 84}, // residue out
			{9, 17},   // polpa container in
			{9, 56},   // empty container out
			{151, 17}, // empty container in (solution)
			{151, 56}, // solution container out
	};
	public static final int BUTTON_REDSTONE = 0;

	final TileFilterPress tile;
	/** Client copies of the master's values (on the server they are read straight from the tile). */
	public final FluidTank storage = new FluidTank(TileFilterPress.TANK_CAPACITY);
	public final FluidTank pressure = new FluidTank(TileFilterPress.BATCH);
	public final FluidTank solution = new FluidTank(TileFilterPress.TANK_CAPACITY);
	public int energy, pump, phase, phaseTicks, state;
	public boolean redstoneInverted;
	final int[] sent = {-1, -1, -1, -1, -1, -1, -1, -1, -1};

	public ContainerFilterPress(InventoryPlayer playerInv, TileFilterPress tile)
	{
		this.tile = tile;
		for(int i = 0; i < TileFilterPress.FILTERS; i++)
			addSlotToContainer(new SlotItemHandler(tile.getInventory(), i, FILTERS_X + i % FILTER_COLUMNS * 18, FILTERS_Y + i / FILTER_COLUMNS * 18));
		for(int i = 0; i < SLOT_XY.length; i++)
			addSlotToContainer(new SlotItemHandler(tile.getInventory(), TileFilterPress.FILTERS + i, SLOT_XY[i][0], SLOT_XY[i][1]));
		for(int i = 0; i < 3; i++)
			for(int j = 0; j < 9; j++)
				addSlotToContainer(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, INV_Y + i * 18));
		for(int i = 0; i < 9; i++)
			addSlotToContainer(new Slot(playerInv, i, 8 + i * 18, INV_Y + 58));
		read();
	}

	int[] values()
	{
		return new int[]{tile.getStorage(), tile.getPressureTank(), tile.getSolution(), tile.getEnergy(), tile.getPumpTicks(),
				tile.getPhase(), tile.getPhaseTicks(), tile.getState(), tile.isRedstoneInverted() ? 1 : 0};
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
			case 0: storage.setFluid(data > 0 ? new FluidStack(INContent.fluidPolpa, data) : null); break;
			case 1: pressure.setFluid(data > 0 ? new FluidStack(INContent.fluidPolpa, data) : null); break;
			case 2: solution.setFluid(data > 0 ? new FluidStack(INContent.fluidSolucaoBruta, data) : null); break;
			case 3: energy = data; break;
			case 4: pump = data; break;
			case 5: phase = data; break;
			case 6: phaseTicks = data; break;
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

	/** Shift click: machine to player; player to the first machine slot that takes the item (filters one per slot). */
	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index)
	{
		Slot slot = inventorySlots.get(index);
		if(slot == null || !slot.getHasStack())
			return ItemStack.EMPTY;
		ItemStack stack = slot.getStack();
		ItemStack copy = stack.copy();
		int machine = TileFilterPress.SLOTS;
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
				if(inventorySlots.get(i).isItemValid(stack) && !inventorySlots.get(i).getHasStack() || i >= TileFilterPress.FILTERS && inventorySlots.get(i).isItemValid(stack))
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
