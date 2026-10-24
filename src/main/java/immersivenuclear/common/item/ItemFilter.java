package immersivenuclear.common.item;

import java.util.List;

import javax.annotation.Nullable;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Filtro (filter cloth in a frame) of the Filtration Unit, one per slot, 28 in the machine. Every batch wears each
 * filter by one; a filter worn to the end is spent ("esgotado"): it stays in its slot, no longer filters, and has
 * to be replaced. The model shows new / used / spent from the "wear" property (models/item/filter.json).
 */
public class ItemFilter extends Item
{
	/** Batches a filter lasts (provisional balance). */
	public static final int LIFE = 64;

	public ItemFilter()
	{
		setRegistryName(ImmersiveNuclear.MODID, "filter");
		setTranslationKey(ImmersiveNuclear.MODID + ".filter");
		setCreativeTab(INContent.TAB);
		setMaxStackSize(1);
		setMaxDamage(LIFE);
		setNoRepair();
	}

	/** Client only (the property getter does not exist on a dedicated server): "wear" 0..1 for the item model. */
	@SideOnly(Side.CLIENT)
	public void addWearProperty()
	{
		addPropertyOverride(new ResourceLocation(ImmersiveNuclear.MODID, "wear"), (stack, world, entity) -> stack.getItemDamage() / (float)LIFE);
	}

	public static boolean isFilter(ItemStack stack)
	{
		return !stack.isEmpty() && stack.getItem() == INContent.itemFilter;
	}

	/** A filter that can still take a batch. */
	public static boolean isUsable(ItemStack stack)
	{
		return isFilter(stack) && stack.getItemDamage() < LIFE;
	}

	/** One batch of wear; the filter is kept even when it reaches the end (spent, not destroyed). */
	public static void wear(ItemStack stack)
	{
		stack.setItemDamage(Math.min(LIFE, stack.getItemDamage() + 1));
	}

	@Override
	public boolean isDamageable()
	{
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag)
	{
		int left = LIFE - stack.getItemDamage();
		tooltip.add(left > 0 ? I18n.format("item.immersivenuclear.filter.life", left, LIFE) : "§c" + I18n.format("item.immersivenuclear.filter.spent"));
	}
}
