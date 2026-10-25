package immersivenuclear.common.wire;

import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler.Connection;
import blusunrize.immersiveengineering.api.energy.wires.WireType;
import immersivenuclear.common.INContent;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
public class LinkWireType extends WireType
{
	public static final LinkWireType INSTANCE = new LinkWireType();

	@Override
	public String getUniqueName()
	{
		return "immersivenuclear:link";
	}

	@Override
	public double getLossRatio()
	{
		return 0;
	}

	@Override
	public int getTransferRate()
	{
		return 0;
	}

	@Override
	public int getColour(Connection connection)
	{
		return 0x7CC4D8;
	}

	@Override
	public double getSlack()
	{
		return 1;
	}

	@Override
	public TextureAtlasSprite getIcon(Connection connection)
	{
		return WireType.REDSTONE.getIcon(connection);
	}

	@Override
	public int getMaxLength()
	{
		return 32;
	}

	@Override
	public ItemStack getWireCoil()
	{
		return new ItemStack(INContent.itemComputerParts[6]);
	}

	@Override
	public double getRenderDiameter()
	{
		return 0.03125;
	}

	@Override
	public boolean isEnergyWire()
	{
		return false;
	}

	@Override
	public String getCategory()
	{
		return "DATA";
	}

	@Override
	public boolean canCauseDamage()
	{
		return false;
	}
}
