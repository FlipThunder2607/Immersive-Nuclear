package immersivenuclear.common.item;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INAssembly;
import immersivenuclear.common.INContent;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;

/**
 * A component tape (the user, 15/11/2026): a reel with its paper tape, holding up to 128 of ONE electronic component. It keeps
 * the component's index (INAssembly.COMPONENTS) and the count in NBT; an empty tape takes any component, a loaded one only its
 * own until it is empty again. The IN Workbench loads it; the Modular Assembly Station uses the components up and gives the
 * empty tape back. The icon shows the component (the "component" property), the bar how full it is.
 */
@SuppressWarnings("deprecation")
public class ItemComponentTape extends Item
{
	public static final int CAPACITY = 128;
	public static final String TAG_COMPONENT = "component", TAG_COUNT = "count";

	public ItemComponentTape()
	{
		setRegistryName(ImmersiveNuclear.MODID, "component_tape");
		setTranslationKey(ImmersiveNuclear.MODID + ".component_tape");
		setCreativeTab(INContent.TAB);
		setMaxStackSize(1);
		addPropertyOverride(new ResourceLocation(ImmersiveNuclear.MODID, "component"),
				(stack, world, entity) -> count(stack) > 0 ? (component(stack) + 1) / 16F : 0F);
	}

	/** The component's index into INAssembly.COMPONENTS, or -1 when the tape is empty. */
	public static int component(ItemStack stack)
	{
		NBTTagCompound t = stack.getTagCompound();
		if(t == null || !t.hasKey(TAG_COMPONENT) || t.getInteger(TAG_COUNT) <= 0)
			return -1;
		int c = t.getInteger(TAG_COMPONENT);
		return c >= 0 && c < INAssembly.COMPONENTS.length ? c : -1;
	}

	public static int count(ItemStack stack)
	{
		NBTTagCompound t = stack.getTagCompound();
		return t == null || component0(t) < 0 ? 0 : Math.max(0, Math.min(CAPACITY, t.getInteger(TAG_COUNT)));
	}

	private static int component0(NBTTagCompound t)
	{
		return t.hasKey(TAG_COMPONENT) ? t.getInteger(TAG_COMPONENT) : -1;
	}

	/** A tape with count of component (an empty tape when count is 0). */
	public static ItemStack of(Item tape, int component, int count)
	{
		ItemStack s = new ItemStack(tape);
		if(count > 0)
		{
			NBTTagCompound t = new NBTTagCompound();
			t.setInteger(TAG_COMPONENT, component);
			t.setInteger(TAG_COUNT, Math.min(CAPACITY, count));
			s.setTagCompound(t);
		}
		return s;
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack)
	{
		int c = component(stack);
		if(c < 0)
			return I18n.translateToLocal("item." + ImmersiveNuclear.MODID + ".component_tape.empty");
		String part = I18n.translateToLocal("item." + ImmersiveNuclear.MODID + "." + INAssembly.COMPONENTS[c] + ".name");
		return I18n.translateToLocalFormatted("item." + ImmersiveNuclear.MODID + ".component_tape.loaded", part, count(stack), CAPACITY);
	}

	@Override
	public boolean showDurabilityBar(ItemStack stack)
	{
		return component(stack) >= 0;
	}

	@Override
	public double getDurabilityForDisplay(ItemStack stack)
	{
		return 1D - count(stack) / (double)CAPACITY;
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items)
	{
		if(!isInCreativeTab(tab))
			return;
		items.add(new ItemStack(this));
		for(int c = 0; c < INAssembly.COMPONENTS.length; c++)
			items.add(of(this, c, CAPACITY));
	}
}
