package immersivenuclear.common.item;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.INFuel;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

/**
 * A fuel line item that carries its enrichment grade (3, 5, 20 or 90 %) in NBT, one item for every grade (the user,
 * 14/11/2026: "o mesmo item mas com graus diferentes"). The grade shows in the name; stacks of different grades do not
 * merge. The enriched powder's icon also changes a little with the grade (a "grade" model property, 0..1).
 */
public class ItemGraded extends Item
{
	public static final String TAG = "grade";

	public ItemGraded(String name, boolean iconByGrade)
	{
		setRegistryName(ImmersiveNuclear.MODID, name);
		setTranslationKey(ImmersiveNuclear.MODID + "." + name);
		setCreativeTab(INContent.TAB);
		if(iconByGrade)
			addPropertyOverride(new ResourceLocation(ImmersiveNuclear.MODID, "grade"), (stack, world, entity) -> grade(stack) / 3F);
	}

	/** The grade's index into INFuel.GRADES (0 = 3 %); a stack without one is the lowest grade. */
	public static int grade(ItemStack stack)
	{
		NBTTagCompound t = stack.getTagCompound();
		return t == null ? 0 : MathHelper.clamp(t.getInteger(TAG), 0, INFuel.GRADES.length - 1);
	}

	public static ItemStack withGrade(Item item, int grade, int count)
	{
		ItemStack s = new ItemStack(item, count);
		NBTTagCompound t = new NBTTagCompound();
		t.setInteger(TAG, grade);
		s.setTagCompound(t);
		return s;
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack)
	{
		return super.getItemStackDisplayName(stack) + " (" + INFuel.GRADES[grade(stack)] + ")";
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items)
	{
		if(isInCreativeTab(tab))
			for(int g = 0; g < INFuel.GRADES.length; g++)
				items.add(withGrade(this, g, 1));
	}
}
