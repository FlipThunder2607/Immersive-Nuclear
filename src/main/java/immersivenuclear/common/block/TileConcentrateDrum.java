package immersivenuclear.common.block;

import javax.annotation.Nullable;

import immersivenuclear.common.INContent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * The Concentrate Drum: a sealed steel drum with one slot, for yellowcake only (a stack). Hoppers, conveyors and pipes
 * fill and empty it from every side. Its contents travel with it when it is broken (BlockConcentrateDrum), as
 * "BlockEntityTag" on the item, so vanilla puts them back when it is placed again.
 */
public class TileConcentrateDrum extends TileEntity
{
	public static final int CAPACITY = 64;

	final ItemStackHandler inventory = new ItemStackHandler(1)
	{
		@Override
		public boolean isItemValid(int slot, ItemStack stack)
		{
			return stack.getItem() == INContent.itemYellowcake;
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
		{
			return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
		}

		@Override
		protected void onContentsChanged(int slot)
		{
			markDirty();
			if(world != null && !world.isRemote)
				world.updateComparatorOutputLevel(pos, getBlockType());
		}
	};

	public IItemHandler getInventory()
	{
		return inventory;
	}

	/** Yellowcake inside. */
	public int getCount()
	{
		return inventory.getStackInSlot(0).getCount();
	}

	/** The contents as the item carries them (its "BlockEntityTag"); null when empty. */
	@Nullable
	public NBTTagCompound contentsTag()
	{
		if(getCount() == 0)
			return null;
		NBTTagCompound tag = new NBTTagCompound();
		tag.setTag("inventory", inventory.serializeNBT());
		return tag;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		if(nbt.hasKey("inventory"))
			inventory.deserializeNBT(nbt.getCompoundTag("inventory"));
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		nbt.setTag("inventory", inventory.serializeNBT());
		return nbt;
	}

	@Override
	public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing)
	{
		return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing)
	{
		if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
		return super.getCapability(capability, facing);
	}
}
