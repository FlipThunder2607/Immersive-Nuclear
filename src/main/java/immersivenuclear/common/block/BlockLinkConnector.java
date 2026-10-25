package immersivenuclear.common.block;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler;
import blusunrize.immersiveengineering.api.energy.wires.TileEntityImmersiveConnectable;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
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
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * Link connector and link relay of the computers' data network: IE's LV connector model with our own texture,
 * wrapped in IE's connection model (blockstates/link_connector.json) so the link cables show. The connector takes
 * one cable and talks to the device behind it; the relay takes many cables and only bridges them
 * (see {@link TileLinkConnector}). Points away from the face it was placed on, like IE's connectors.
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

	/** Relay: many cables, no device. Connector: one cable, talks to the device it is attached to. */
	public final boolean relay;

	public BlockLinkConnector(String name, boolean relay)
	{
		super(Material.IRON);
		this.relay = relay;
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
	public boolean hasTileEntity(IBlockState state)
	{
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state)
	{
		return new TileLinkConnector();
	}

	/** IE's connection model draws the cables from CONNECTIONS (like IE's BlockConnector); the tile tells relay from connector in its cache. */
	@Override
	protected BlockStateContainer createBlockState()
	{
		return new ExtendedBlockState(this, new IProperty[]{FACING}, new IUnlistedProperty[]{IEProperties.CONNECTIONS, IEProperties.TILEENTITY_PASSTHROUGH});
	}

	@Override
	public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos)
	{
		TileEntity te = world.getTileEntity(pos);
		if(state instanceof IExtendedBlockState && te instanceof TileEntityImmersiveConnectable)
			return ((IExtendedBlockState)state).withProperty(IEProperties.CONNECTIONS, ((TileEntityImmersiveConnectable)te).genConnBlockstate())
					.withProperty(IEProperties.TILEENTITY_PASSTHROUGH, te);
		return state;
	}

	/** IE sends a block event after linking so the client redraws the cables; a plain Block would drop it. */
	@Override
	public boolean eventReceived(IBlockState state, World world, BlockPos pos, int id, int param)
	{
		TileEntity te = world.getTileEntity(pos);
		return te != null && te.receiveClientEvent(id, param);
	}

	/** Breaking the block takes its cables down and drops them, like IE's connectors. */
	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state)
	{
		if(!world.isRemote && world.getTileEntity(pos) instanceof TileLinkConnector)
			ImmersiveNetHandler.INSTANCE.clearAllConnectionsFor(pos, world, world.getGameRules().getBoolean("doTileDrops"));
		super.breakBlock(world, pos, state);
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
