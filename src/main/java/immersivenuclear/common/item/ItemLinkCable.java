package immersivenuclear.common.item;

import blusunrize.immersiveengineering.api.ApiUtils;
import blusunrize.immersiveengineering.api.energy.wires.IWireCoil;
import blusunrize.immersiveengineering.api.energy.wires.WireType;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.wire.LinkWireType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Link cable coil: one coil per connection, like IE's wire coils (cutting or breaking gives it back). */
public class ItemLinkCable extends Item implements IWireCoil
{
	public ItemLinkCable()
	{
		setRegistryName(ImmersiveNuclear.MODID, "link_cable");
		setTranslationKey(ImmersiveNuclear.MODID + ".link_cable");
		setCreativeTab(INContent.TAB);
	}

	@Override
	public WireType getWireType(ItemStack stack)
	{
		return LinkWireType.INSTANCE;
	}

	@Override
	public int getMaxLength(ItemStack stack)
	{
		return 32;
	}

	@Override
	public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand)
	{
		// On a computer it is laid in the conduits; anywhere else it hangs like IE's wires
		EnumActionResult laid = immersivenuclear.common.wire.ConduitWiring.use(player, world, pos, hand, immersivenuclear.common.wire.ConduitRules.DATA);
		return laid != EnumActionResult.PASS ? laid : ApiUtils.doCoilUse(this, player, world, pos, hand, side, hitX, hitY, hitZ);
	}
}
