package immersivenuclear.common;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** A machine as an icon only (JEI, Hwyla, the manual's model): it cannot be kept. One that reaches an inventory or the ground, as JEI's cheat mode would put it there, is gone on the next tick. */
public class ItemMachineIcon extends Item
{
	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected)
	{
		stack.setCount(0);
	}

	@Override
	public boolean onEntityItemUpdate(EntityItem entity)
	{
		entity.setDead();
		return true;
	}
}
