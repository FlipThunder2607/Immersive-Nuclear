package immersivenuclear.common.item;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.wire.ConduitRules;
import immersivenuclear.common.wire.ConduitWiring;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** The computers' power wire: clicked on one computer and then on another, it is laid in the conduits between them. */
public class ItemPowerWire extends Item
{
	public ItemPowerWire()
	{
		setRegistryName(ImmersiveNuclear.MODID, "power_wire");
		setTranslationKey(ImmersiveNuclear.MODID + ".power_wire");
		setCreativeTab(INContent.TAB);
	}

	@Override
	public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand)
	{
		return ConduitWiring.use(player, world, pos, hand, ConduitRules.POWER);
	}
}
