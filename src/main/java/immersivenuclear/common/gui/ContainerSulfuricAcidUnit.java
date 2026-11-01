package immersivenuclear.common.gui;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileSulfuricAcidUnit;
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
 * Interface of the Sulfuric Acid Unit. 272 x 238, as in the approved sketch (ferramentas/gui_acido_sulfurico.py): the
 * sulfur over the burner, the process drawn left to right (burner, boiler and steam drum, converter, absorption tower),
 * then the sulfuric acid, water and steam tanks, each with its container slots (in, out) under it; the player inventory at the bottom,
 * centred. The tanks, energy, the cycle's progress, state and redstone mode go to the client as window properties
 * (all below 32767).
 */
public class ContainerSulfuricAcidUnit extends Container
{
	public static final int WIDTH = 272, HEIGHT = 238, INV_X = 56, INV_Y = 157;
	/** Slot positions (top-left of the 16x16 item), shared with the GUI; in TileSulfuricAcidUnit's slot order. */
	public static final int[][] SLOT_XY = {
			{26, 18},  // sulfur, over the burner's hopper (centred on the burner)
			{164, 92}, // empty container for acid, under the acid tank
			{164, 118}, // filled with acid
			{190, 92}, // water container, under the water tank
			{190, 118}, // the emptied water container
			{216, 92}, // empty container for steam, under the steam tank
			{216, 118}, // filled with steam
	};
	public static final int BUTTON_REDSTONE = 0;

	final TileSulfuricAcidUnit tile;
	/** Client copies of the master's values (on the server they are read straight from the tile). */
	public final FluidTank water = new FluidTank(TileSulfuricAcidUnit.WATER_CAPACITY);
	public final FluidTank acid = new FluidTank(TileSulfuricAcidUnit.ACID_CAPACITY);
	public final FluidTank steam = new FluidTank(TileSulfuricAcidUnit.STEAM_CAPACITY);
	public int energy, progress, state;
	public boolean redstoneInverted, venting;
	public int cycleSize = 1;
	final int[] sent = {-1, -1, -1, -1, -1, -1, -1, -1, -1};

	public ContainerSulfuricAcidUnit(InventoryPlayer playerInv, TileSulfuricAcidUnit tile)
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
			case 0: water.setFluid(data > 0 ? new FluidStack(FluidRegistry.WATER, data) : null); break;
			case 1: acid.setFluid(data > 0 ? new FluidStack(INContent.fluidSulfuricAcid, data) : null); break;
			case 2: steam.setFluid(data > 0 ? new FluidStack(INContent.fluidSteam, data) : null); break;
			case 3: energy = data; break;
			case 4: progress = data; break;
			case 5: state = data; break;
			case 6: redstoneInverted = data != 0; break;
			case 7: venting = data != 0; break;
			case 8: cycleSize = Math.max(1, data); break;
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
