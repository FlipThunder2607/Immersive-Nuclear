package immersivenuclear.common.block;

import immersivenuclear.common.INAssembly;
import immersivenuclear.common.INWorkbenchRecipe;
import immersivenuclear.common.item.ItemComponentTape;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraftforge.items.ItemStackHandler;

/**
 * The IN Workbench's contents, on its main half: the blueprint, the component tape on its holder, the six materials, and the
 * chosen item of the blueprint's family (-1: none). With no item chosen, the components among the materials load into the tape
 * (free; one kind per tape, up to 128).
 */
public class TileINWorkbench extends TileEntity implements ITickable
{
	public static final int SLOT_BLUEPRINT = 0, SLOT_TAPE = 1, SLOT_MATERIALS = 2, MATERIALS = INWorkbenchRecipe.MAX_INPUTS, SLOTS = 2 + MATERIALS;
	public static final int LOAD_TICKS = 5;

	public final ItemStackHandler inventory = new ItemStackHandler(SLOTS)
	{
		@Override
		public int getSlotLimit(int slot)
		{
			return slot == SLOT_MATERIALS - 1 || slot == SLOT_BLUEPRINT ? 1 : 64;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack)
		{
			if(slot == SLOT_BLUEPRINT)
				return isBenchBlueprint(stack);
			if(slot == SLOT_TAPE)
				return stack.getItem() == INAssembly.itemComponentTape;
			return true;
		}

		@Override
		protected void onContentsChanged(int slot)
		{
			if(slot == SLOT_BLUEPRINT)
			{
				if(selected >= recipes().size())
					selected = -1;
				// the paper on the board shows or goes: the clients redraw the bench
				if(world != null && !world.isRemote)
				{
					net.minecraft.block.state.IBlockState st = world.getBlockState(pos);
					world.notifyBlockUpdate(pos, st, st, 3);
				}
			}
			markDirty();
		}
	};
	/** The chosen recipe of the blueprint's family, or -1. */
	public int selected = -1;
	int ticks;

	public static boolean isBenchBlueprint(ItemStack stack)
	{
		return !stack.isEmpty() && !INWorkbenchRecipe.of(stack.getItem()).isEmpty();
	}

	public List<INWorkbenchRecipe> recipes()
	{
		ItemStack b = inventory.getStackInSlot(SLOT_BLUEPRINT);
		return b.isEmpty() ? java.util.Collections.emptyList() : INWorkbenchRecipe.of(b.getItem());
	}

	/** The chosen recipe, if any. */
	public INWorkbenchRecipe recipe()
	{
		List<INWorkbenchRecipe> r = recipes();
		return selected >= 0 && selected < r.size() ? r.get(selected) : null;
	}

	/** What the product's slot shows: the chosen item when the materials hold its inputs. */
	public ItemStack product()
	{
		INWorkbenchRecipe r = recipe();
		return r != null && r.matches(inventory, SLOT_MATERIALS, MATERIALS) ? r.output.copy() : ItemStack.EMPTY;
	}

	/** Makes one: takes the inputs out and returns the product (empty when it cannot). */
	public ItemStack craft()
	{
		INWorkbenchRecipe r = recipe();
		if(r == null || !r.matches(inventory, SLOT_MATERIALS, MATERIALS))
			return ItemStack.EMPTY;
		r.consume(inventory, SLOT_MATERIALS, MATERIALS);
		markDirty();
		return r.output.copy();
	}

	static int componentIndex(Item item)
	{
		for(int i = 0; i < INAssembly.itemComponents.length; i++)
			if(INAssembly.itemComponents[i] == item)
				return i;
		return -1;
	}

	@Override
	public void update()
	{
		if(world.isRemote || ++ticks % LOAD_TICKS != 0 || selected >= 0)
			return;
		ItemStack tape = inventory.getStackInSlot(SLOT_TAPE);
		if(tape.isEmpty())
			return;
		int kind = ItemComponentTape.component(tape), count = ItemComponentTape.count(tape);
		if(count >= ItemComponentTape.CAPACITY)
			return;
		for(int s = SLOT_MATERIALS; s < SLOT_MATERIALS + MATERIALS; s++)
		{
			ItemStack st = inventory.getStackInSlot(s);
			int c = st.isEmpty() ? -1 : componentIndex(st.getItem());
			if(c < 0 || kind >= 0 && c != kind)
				continue;
			int move = Math.min(st.getCount(), ItemComponentTape.CAPACITY - count);
			ItemStack rest = st.copy();
			rest.shrink(move);
			inventory.setStackInSlot(s, rest.isEmpty() ? ItemStack.EMPTY : rest);
			inventory.setStackInSlot(SLOT_TAPE, ItemComponentTape.of(INAssembly.itemComponentTape, c, count + move));
			return;
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		inventory.deserializeNBT(nbt.getCompoundTag("inventory"));
		selected = nbt.hasKey("selected") ? nbt.getInteger("selected") : -1;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		nbt.setTag("inventory", inventory.serializeNBT());
		nbt.setInteger("selected", selected);
		return nbt;
	}

	/** On the clients: whether a blueprint lies on the board (the paper drawn on the block). The inventory itself only goes
	 * through the open interface, so a block update never overwrites what the interface shows. */
	public boolean clientBlueprint;

	public boolean hasBlueprint()
	{
		return world != null && world.isRemote ? clientBlueprint : !inventory.getStackInSlot(SLOT_BLUEPRINT).isEmpty();
	}

	@Override
	public NBTTagCompound getUpdateTag()
	{
		NBTTagCompound t = super.getUpdateTag();
		t.setBoolean("blueprint", !inventory.getStackInSlot(SLOT_BLUEPRINT).isEmpty());
		return t;
	}

	@Override
	public void handleUpdateTag(NBTTagCompound tag)
	{
		clientBlueprint = tag.getBoolean("blueprint");
	}

	@Override
	public net.minecraft.network.play.server.SPacketUpdateTileEntity getUpdatePacket()
	{
		return new net.minecraft.network.play.server.SPacketUpdateTileEntity(pos, 0, getUpdateTag());
	}

	@Override
	public void onDataPacket(net.minecraft.network.NetworkManager net, net.minecraft.network.play.server.SPacketUpdateTileEntity pkt)
	{
		clientBlueprint = pkt.getNbtCompound().getBoolean("blueprint");
		world.markBlockRangeForRenderUpdate(pos, pos);
	}

	/** Everything it holds, dropped when the bench is broken. */
	public void dropContents()
	{
		for(int s = 0; s < SLOTS; s++)
		{
			ItemStack st = inventory.getStackInSlot(s);
			if(!st.isEmpty())
				net.minecraft.inventory.InventoryHelper.spawnItemStack(world, pos.getX() + .5, pos.getY() + .8, pos.getZ() + .5, st);
			inventory.setStackInSlot(s, ItemStack.EMPTY);
		}
	}
}
