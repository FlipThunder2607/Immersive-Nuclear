package immersivenuclear.common.gui;

import immersivenuclear.common.block.TileINWorkbench;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

/**
 * The IN Workbench's interface (the layout of ferramentas/gui_bancada_in_v4_esboco.py): the blueprint pinned on the drafting
 * board, the component tape on its holder by the parts bins, the six materials along the work mat's front, the product under
 * the magnifier lamp. The choices on the mat are drawn by the GUI; a click sends its index back (enchantItem).
 */
public class ContainerINWorkbench extends Container
{
	public static final int WIDTH = 272, HEIGHT = 206;
	public static final int INV_X = 56, INV_Y = 122;
	public static final int BLUEPRINT_X = 33, BLUEPRINT_Y = 40, TAPE_X = 77, TAPE_Y = 4, MAT_X = 84, MAT_Y = 82, MAT_STEP = 21;
	public static final int PRODUCT_X = 234, PRODUCT_Y = 42;
	/** The choices on the mat: 6 x 2 per page, items at CHOICE_X + i * 21, CHOICE_Y + j * 21. */
	public static final int CHOICE_X = 84, CHOICE_Y = 35, CHOICE_STEP = 21, CHOICE_COLS = 6, CHOICE_ROWS = 2, PER_PAGE = CHOICE_COLS * CHOICE_ROWS;
	public static final int BENCH_SLOTS = TileINWorkbench.SLOTS + 1;     // the tile's eight and the product

	public final TileINWorkbench tile;
	final InventoryBasic result = new InventoryBasic("product", false, 1);
	/** The chosen recipe, mirrored on the client. */
	public int selected = -1;
	/**
	 * Every slot as the client last got it from here. Minecraft sends no slot while it handles a click (it trusts the client's
	 * guess), but a craft changes the materials, the product and the player's inventory on the server only: what changed then
	 * goes on the next tick (the user, 15/11/2026: the materials only updated when taken out).
	 */
	final java.util.List<ItemStack> sent = new java.util.ArrayList<>();

	public ContainerINWorkbench(InventoryPlayer inv, TileINWorkbench tile)
	{
		this.tile = tile;
		addSlotToContainer(new SlotItemHandler(tile.inventory, TileINWorkbench.SLOT_BLUEPRINT, BLUEPRINT_X, BLUEPRINT_Y));
		addSlotToContainer(new SlotItemHandler(tile.inventory, TileINWorkbench.SLOT_TAPE, TAPE_X, TAPE_Y));
		for(int i = 0; i < TileINWorkbench.MATERIALS; i++)
			addSlotToContainer(new SlotItemHandler(tile.inventory, TileINWorkbench.SLOT_MATERIALS + i, MAT_X + i * MAT_STEP, MAT_Y));
		addSlotToContainer(new Slot(result, 0, PRODUCT_X, PRODUCT_Y)
		{
			@Override
			public boolean isItemValid(ItemStack stack)
			{
				return false;
			}

			/** The product goes whole (half a batch would leave the rest to be made again). */
			@Override
			public ItemStack decrStackSize(int amount)
			{
				return super.decrStackSize(getStack().getCount());
			}

			@Override
			public ItemStack onTake(EntityPlayer player, ItemStack stack)
			{
				if(!player.world.isRemote)
				{
					tile.craft();
					refresh();
					if(sent.size() > TileINWorkbench.SLOTS)
						sent.set(TileINWorkbench.SLOTS, ItemStack.EMPTY);   // the client now shows it empty: the next one goes again
				}
				return super.onTake(player, stack);
			}
		});
		for(int r = 0; r < 3; r++)
			for(int c = 0; c < 9; c++)
				addSlotToContainer(new Slot(inv, c + r * 9 + 9, INV_X + c * 18, INV_Y + r * 18));
		for(int c = 0; c < 9; c++)
			addSlotToContainer(new Slot(inv, c, INV_X + c * 18, INV_Y + 58));
		refresh();
	}

	/** The product's slot shows what the materials can make now (server side; the slot syncs it). */
	void refresh()
	{
		if(!tile.getWorld().isRemote)
			result.setInventorySlotContents(0, tile.product());
	}

	@Override
	public void detectAndSendChanges()
	{
		refresh();
		super.detectAndSendChanges();
		boolean free = !listeners.isEmpty();
		for(IContainerListener l : listeners)
			if(l instanceof net.minecraft.entity.player.EntityPlayerMP && ((net.minecraft.entity.player.EntityPlayerMP)l).isChangingQuantityOnly)
				free = false;
		while(sent.size() < inventorySlots.size())
			sent.add(ItemStack.EMPTY);
		if(free)
			for(int i = 0; i < inventorySlots.size(); i++)
			{
				ItemStack now = inventorySlots.get(i).getStack();
				if(!ItemStack.areItemStacksEqual(now, sent.get(i)))
				{
					for(IContainerListener l : listeners)
						l.sendSlotContents(this, i, now);
					sent.set(i, now.copy());
				}
			}
		if(selected != tile.selected)
		{
			selected = tile.selected;
			for(IContainerListener l : listeners)
				l.sendWindowProperty(this, 0, selected + 1);
		}
	}

	@Override
	public void addListener(IContainerListener listener)
	{
		super.addListener(listener);
		listener.sendWindowProperty(this, 0, tile.selected + 1);
		// the whole contents went with the listener: start from them
		sent.clear();
		for(Slot slot : inventorySlots)
			sent.add(slot.getStack().copy());
	}

	@Override
	public void updateProgressBar(int id, int data)
	{
		if(id == 0)
			selected = data - 1;
	}

	/** A choice on the mat clicked (its index in the family); clicking the chosen one again clears it. */
	@Override
	public boolean enchantItem(EntityPlayer player, int id)
	{
		if(id < 0 || id >= tile.recipes().size())
			return false;
		tile.selected = tile.selected == id ? -1 : id;
		tile.markDirty();
		refresh();
		return true;
	}

	@Override
	public boolean canInteractWith(EntityPlayer player)
	{
		return !tile.isInvalid() && player.getDistanceSq(tile.getPos().add(.5, .5, .5)) <= 64;
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index)
	{
		Slot slot = inventorySlots.get(index);
		if(slot == null || !slot.getHasStack())
			return ItemStack.EMPTY;
		ItemStack stack = slot.getStack(), copy = stack.copy();
		int product = TileINWorkbench.SLOTS;
		if(index == product)
		{
			// shift-click on the product: make as many as fit
			if(player.world.isRemote)
				return ItemStack.EMPTY;
			int made = 0;
			while(made < 64)
			{
				ItemStack p = tile.product();
				if(p.isEmpty() || !player.inventory.addItemStackToInventory(p.copy()))
					break;
				tile.craft();
				made++;
			}
			refresh();
			return ItemStack.EMPTY;
		}
		if(index < BENCH_SLOTS)
		{
			if(!mergeItemStack(stack, BENCH_SLOTS, inventorySlots.size(), true))
				return ItemStack.EMPTY;
		}
		else
		{
			boolean moved = false;
			for(int s = 0; s < TileINWorkbench.SLOTS && !stack.isEmpty(); s++)
			{
				Slot target = inventorySlots.get(s);
				if(!target.isItemValid(stack))
					continue;
				if(s < TileINWorkbench.SLOT_MATERIALS && (TileINWorkbench.isBenchBlueprint(stack) || stack.getItem() == immersivenuclear.common.INAssembly.itemComponentTape))
					moved |= mergeItemStack(stack, s, s + 1, false);
				else if(s >= TileINWorkbench.SLOT_MATERIALS)
				{
					moved |= mergeItemStack(stack, s, TileINWorkbench.SLOTS, false);
					break;
				}
			}
			if(!moved)
				return ItemStack.EMPTY;
		}
		if(stack.isEmpty())
			slot.putStack(ItemStack.EMPTY);
		else
			slot.onSlotChanged();
		return copy;
	}
}
