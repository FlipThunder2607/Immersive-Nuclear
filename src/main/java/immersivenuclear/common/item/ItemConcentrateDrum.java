package immersivenuclear.common.item;

import immersivenuclear.common.INContent;
import java.util.List;

import javax.annotation.Nullable;

import immersivenuclear.common.block.BlockConcentrateDrum;
import immersivenuclear.common.block.TileConcentrateDrum;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemStackHandler;

/** The Concentrate Drum as an item: empty drums stack, a filled one carries its yellowcake (BlockEntityTag) and does not. */
public class ItemConcentrateDrum extends ItemBlock
{
	public ItemConcentrateDrum(BlockConcentrateDrum block)
	{
		super(block);
		setRegistryName(block.getRegistryName());
	}

	/** Yellowcake the drum carries. */
	public static int contents(ItemStack stack)
	{
		NBTTagCompound tag = stack.getTagCompound();
		if(tag == null || !tag.hasKey("BlockEntityTag"))
			return 0;
		ItemStackHandler handler = new ItemStackHandler(1);
		handler.deserializeNBT(tag.getCompoundTag("BlockEntityTag").getCompoundTag("inventory"));
		return handler.getStackInSlot(0).getCount();
	}

	/** A drum holding {@code count} yellowcake (an empty drum for 0). */
	public static ItemStack withContents(int count)
	{
		ItemStack drum = new ItemStack(INContent.itemConcentrateDrum);
		if(count <= 0)
			return drum;
		ItemStackHandler handler = new ItemStackHandler(1);
		handler.setStackInSlot(0, new ItemStack(INContent.itemYellowcake, count));
		NBTTagCompound contents = new NBTTagCompound(), tag = new NBTTagCompound();
		contents.setTag("inventory", handler.serializeNBT());
		tag.setTag("BlockEntityTag", contents);
		drum.setTagCompound(tag);
		return drum;
	}

	/** A Concentrate Drum with room for more yellowcake: empty or partly filled. */
	public static boolean canTakeMore(ItemStack stack)
	{
		return stack.getItem() == INContent.itemConcentrateDrum && contents(stack) < TileConcentrateDrum.CAPACITY;
	}

	@Override
	public int getItemStackLimit(ItemStack stack)
	{
		return contents(stack) > 0 ? 1 : 16;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag)
	{
		int n = contents(stack);
		tooltip.add(n > 0 ? "§e" + I18n.format("desc.immersivenuclear.concentrate_drum.contents", n, TileConcentrateDrum.CAPACITY)
				: "§7" + I18n.format("desc.immersivenuclear.concentrate_drum.empty"));
		tooltip.add("§7" + I18n.format("desc.immersivenuclear.concentrate_drum.info"));
	}
}
