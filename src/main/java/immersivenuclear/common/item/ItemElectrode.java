package immersivenuclear.common.item;

import java.util.List;

import javax.annotation.Nullable;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * An electrode plate of the Metal Extractor, one per plate slot, six in the machine. Three materials, as in real
 * electrowinning: plain steel; steel with a lead-silver anode (the alloy real cells use: it corrodes less and wastes
 * less current); and an electrum-coated one, standing in for the titanium anodes coated with noble metal oxides (the
 * most efficient), which IE has no metal for. A better plate gives more deposit per batch, needs less energy and lasts
 * longer. Every batch wears each plate by one; a plate worn through breaks and leaves its slot empty.
 * All numbers are provisional.
 */
public class ItemElectrode extends Item
{
	/** 1 steel, 2 lead-silver, 3 electrum. */
	public final int tier;
	/** Batches it lasts. */
	public final int life;
	/** Its share of the batch's deposit (six steel plates give the base six) and of the recovery's energy. */
	public final float yield, energy;
	/** Colour of the plate in the world and the interface, as a multiplier of the steel (r, g, b). */
	public final float[] colour;

	public ItemElectrode(String name, int tier, int life, float yield, float energy, float[] colour)
	{
		this.tier = tier;
		this.life = life;
		this.yield = yield;
		this.energy = energy;
		this.colour = colour;
		setRegistryName(ImmersiveNuclear.MODID, name);
		setTranslationKey(ImmersiveNuclear.MODID + "." + name);
		setCreativeTab(INContent.TAB);
		setMaxStackSize(1);
		setMaxDamage(life);
		setNoRepair();
	}

	/** The plate in a stack, or null. */
	@Nullable
	public static ItemElectrode of(ItemStack stack)
	{
		return !stack.isEmpty() && stack.getItem() instanceof ItemElectrode ? (ItemElectrode)stack.getItem() : null;
	}

	/** One batch of wear; true when the plate is worn through (the caller empties the slot). */
	public static boolean wear(ItemStack stack)
	{
		ItemElectrode e = of(stack);
		if(e == null)
			return false;
		stack.setItemDamage(stack.getItemDamage() + 1);
		return stack.getItemDamage() >= e.life;
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
		tooltip.add("§7" + I18n.format("item.immersivenuclear.electrode.life", life - stack.getItemDamage(), life));
		tooltip.add("§7" + I18n.format("item.immersivenuclear.electrode.effect", Math.round(yield * 100), Math.round(energy * 100)));
	}
}
