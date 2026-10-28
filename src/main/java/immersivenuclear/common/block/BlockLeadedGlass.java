package immersivenuclear.common.block;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Lead Glass: a hot cell's window, made in IE's Arc Furnace (glass and lead dust). A reinforced pane of amber glass in a
 * steel frame; it goes into the Radioactive Handling block and can be built with. Tougher than glass, and it drops
 * itself. Panes side by side show no faces between them, like vanilla glass.
 */
public class BlockLeadedGlass extends Block
{
	public BlockLeadedGlass()
	{
		super(Material.GLASS);
		setHardness(1.5f);
		setResistance(30f);
		setSoundType(SoundType.GLASS);
		setHarvestLevel("pickaxe", 0);
		setRegistryName(ImmersiveNuclear.MODID, "leaded_glass");
		setTranslationKey(ImmersiveNuclear.MODID + ".leaded_glass");
		setCreativeTab(INContent.TAB);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public BlockRenderLayer getRenderLayer()
	{
		return BlockRenderLayer.TRANSLUCENT;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state)
	{
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side)
	{
		return world.getBlockState(pos.offset(side)).getBlock() != this && super.shouldSideBeRendered(state, world, pos, side);
	}
}
