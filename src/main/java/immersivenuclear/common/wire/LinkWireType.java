package immersivenuclear.common.wire;

import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler.Connection;
import blusunrize.immersiveengineering.api.energy.wires.WireType;
import immersivenuclear.common.INContent;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Data cable: looks and hangs like IE's HV (steel) wire, painted space grey; carries no energy. */
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

	/** Space grey cable (tints IE's wire texture, which darkens it a little). */
	@Override
	public int getColour(Connection connection)
	{
		return 0x3A3C40;
	}

	@Override
	public double getSlack()
	{
		return WireType.STEEL.getSlack();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public TextureAtlasSprite getIcon(Connection connection)
	{
		return WireType.STEEL.getIcon(connection);
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
		return WireType.STEEL.getRenderDiameter();
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
