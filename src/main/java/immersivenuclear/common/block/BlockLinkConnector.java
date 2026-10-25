package immersivenuclear.common.block;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Link connector and link relay of the computers' data network: IE's LV connector model with our own texture
 * (blockstates/link_connector.json). For now only the block; the network comes with the computers.
 * Points away from the face it was placed on, like IE's connectors.
 */
public class BlockLinkConnector extends Block
{
	public static final PropertyDirection FACING = BlockDirectional.FACING;

	/** IE's connector_lv.obj: 6/16 wide, half a block tall, standing on the face it is attached to. */
	static final AxisAlignedBB[] BOXES = {
			new AxisAlignedBB(5 / 16D, 0.5, 5 / 16D, 11 / 16D, 1, 11 / 16D),   // down
			new AxisAlignedBB(5 / 16D, 0, 5 / 16D, 11 / 16D, 0.5, 11 / 16D),   // up
			new AxisAlignedBB(5 / 16D, 5 / 16D, 0.5, 11 / 16D, 11 / 16D, 1),   // north
			new AxisAlignedBB(5 / 16D, 5 / 16D, 0, 11 / 16D, 11 / 16D, 0.5),   // south
			new AxisAlignedBB(0.5, 5 / 16D, 5 / 16D, 1, 11 / 16D, 11 / 16D),   // west
			new AxisAlignedBB(0, 5 / 16D, 5 / 16D, 0.5, 11 / 16D, 11 / 16D)};  // east

	public BlockLinkConnector(String name)
	{
		super(Material.IRON);
		setHardness(3f);
		setResistance(15f);
		setSoundType(SoundType.METAL);
		setHarvestLevel("pickaxe", 0);
		setRegistryName(ImmersiveNuclear.MODID, name);
		setTranslationKey(ImmersiveNuclear.MODID + "." + name);
		setCreativeTab(INContent.TAB);
		setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.UP));
	}

	@Override
	protected BlockStateContainer createBlockState()
	{
		return new BlockStateContainer(this, FACING);
	}

	@Override
	public IBlockState getStateFromMeta(int meta)
	{
		return getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta & 7));
	}

	@Override
	public int getMetaFromState(IBlockState state)
	{
		return state.getValue(FACING).getIndex();
	}

	@Override
	public boolean hasTileEntity(IBlockState state)
	{
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state)
	{
		return new TileLinkConnector();
	}

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
	{
		return getDefaultState().withProperty(FACING, facing);
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos)
	{
		return BOXES[state.getValue(FACING).getIndex()];
	}

	@Override
	public boolean isOpaqueCube(IBlockState state)
	{
		return false;
	}

	@Override
	public boolean isFullCube(IBlockState state)
	{
		return false;
	}

	@Override
	public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face)
	{
		return BlockFaceShape.UNDEFINED;
	}
}
