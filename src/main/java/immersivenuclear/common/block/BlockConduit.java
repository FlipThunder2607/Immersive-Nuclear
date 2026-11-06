package immersivenuclear.common.block;

import blusunrize.immersiveengineering.api.Lib;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The Conduit: the slatted cable tray that carries the computers' wires (1 power wire and 4 data cables, or 8 data cables; the wires are not in yet). It works like the tray of Industrial Renewal
 * (the user, 05/11/2026): one centre in the middle of the block, a connection to each of the six sides that has another conduit or a device's conduit entry, the other sides closed.
 * The Engineer's Hammer cuts and rejoins each connection, like IE's fluid pipes (kept in TileConduit).
 * Its only stored choice is the side of its support: the solid face the player pointed at when placing it (nothing when pointing at another conduit or at something not solid). The support shows
 * while that block is there and no conduit took that side. Everything else is computed for the model (blockstates/conduit.json).
 */
public class BlockConduit extends Block
{
	public static final PropertyBool NORTH = PropertyBool.create("north"), EAST = PropertyBool.create("east"), SOUTH = PropertyBool.create("south"),
			WEST = PropertyBool.create("west"), UP = PropertyBool.create("up"), DOWN = PropertyBool.create("down");
	public static final PropertyEnum<Base> BASE = PropertyEnum.create("base", Base.class);

	/** The side the support is on. The order is the metadata, and it matches the worlds made with the earlier tray (floor, ceiling, then the four walls). */
	public enum Base implements IStringSerializable
	{
		DOWN(EnumFacing.DOWN), UP(EnumFacing.UP), NORTH(EnumFacing.NORTH), EAST(EnumFacing.EAST), SOUTH(EnumFacing.SOUTH), WEST(EnumFacing.WEST), NONE(null);

		/** The side of the block that holds it, null for none. */
		public final EnumFacing side;

		Base(EnumFacing side)
		{
			this.side = side;
		}

		@Override
		public String getName()
		{
			return side == null ? "none" : side.getName();
		}

		public static Base of(EnumFacing side)
		{
			for(Base base : values())
				if(base.side == side)
					return base;
			return NONE;
		}
	}

	public BlockConduit()
	{
		super(Material.IRON);
		setHardness(1.5f);
		setResistance(10f);
		setSoundType(SoundType.METAL);
		setHarvestLevel("pickaxe", 0);
		setRegistryName(ImmersiveNuclear.MODID, "conduit");
		setTranslationKey(ImmersiveNuclear.MODID + ".conduit");
		setCreativeTab(INContent.TAB);
		setDefaultState(blockState.getBaseState().withProperty(NORTH, false).withProperty(EAST, false).withProperty(SOUTH, false).withProperty(WEST, false)
				.withProperty(UP, false).withProperty(DOWN, false).withProperty(BASE, Base.NONE));
	}

	@Override
	protected BlockStateContainer createBlockState()
	{
		return new BlockStateContainer(this, NORTH, EAST, SOUTH, WEST, UP, DOWN, BASE);
	}

	@Override
	public IBlockState getStateFromMeta(int meta)
	{
		Base[] all = Base.values();
		return getDefaultState().withProperty(BASE, all[meta >= 0 && meta < all.length ? meta : Base.NONE.ordinal()]);
	}

	@Override
	public int getMetaFromState(IBlockState state)
	{
		return state.getValue(BASE).ordinal();
	}

	/** The support goes on the solid face the player pointed at; pointing at another conduit, or at nothing solid, it has none. */
	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand)
	{
		EnumFacing side = facing.getOpposite();
		return getDefaultState().withProperty(BASE, holds(world, pos, side) ? Base.of(side) : Base.NONE);
	}

	/** Whether the block on the given side of pos can hold a support (a conduit never does). */
	private static boolean holds(IBlockAccess world, BlockPos pos, EnumFacing side)
	{
		BlockPos other = pos.offset(side);
		IBlockState state = world.getBlockState(other);
		return !(state.getBlock() instanceof BlockConduit) && state.isSideSolid(world, other, side.getOpposite());
	}

	@Override
	public boolean hasTileEntity(IBlockState state)
	{
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state)
	{
		return new TileConduit();
	}

	/** The hammer cuts or joins the connection it hits, like IE's fluid pipes: an arm or duct is hit anywhere along it, a closed side on its face. Both conduits of a link change together. */
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
	{
		ItemStack held = player.getHeldItem(hand);
		if(held.isEmpty() || !held.getItem().getToolClasses(held).contains(Lib.TOOL_HAMMER))
			return false;
		EnumFacing target = hitX < 0.19 ? EnumFacing.WEST : hitX > 0.81 ? EnumFacing.EAST : hitZ < 0.19 ? EnumFacing.NORTH : hitZ > 0.81 ? EnumFacing.SOUTH
				: hitY > 0.61 ? EnumFacing.UP : hitY < 0.32 ? EnumFacing.DOWN : side;
		TileEntity tile = world.getTileEntity(pos);
		if(world.isRemote || !(tile instanceof TileConduit) || !reaches(world, pos, target))
			return true;
		TileConduit conduit = (TileConduit)tile;
		boolean cut = !conduit.isCut(target);
		conduit.setCut(target, cut);
		TileEntity other = world.getTileEntity(pos.offset(target));
		if(other instanceof TileConduit)
			((TileConduit)other).setCut(target.getOpposite(), cut);
		return true;
	}

	/** Whether the side d of the conduit at pos was cut with the hammer. Called while rendering too, where a tile must not be created. */
	private static boolean isCut(IBlockAccess world, BlockPos pos, EnumFacing d)
	{
		TileEntity tile = world instanceof ChunkCache ? ((ChunkCache)world).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK) : world.getTileEntity(pos);
		return tile instanceof TileConduit && ((TileConduit)tile).isCut(d);
	}

	/** Whether the conduit at pos joins what is on its side d: something it reaches, unless one of the two ends was cut with the hammer. */
	public static boolean connects(IBlockAccess world, BlockPos pos, EnumFacing d)
	{
		return reaches(world, pos, d) && !isCut(world, pos, d) && !isCut(world, pos.offset(d), d.getOpposite());
	}

	/** Whether there is something to join on side d: another conduit, or a computer device whose back (the side of its conduit entry) faces it. */
	private static boolean reaches(IBlockAccess world, BlockPos pos, EnumFacing d)
	{
		IBlockState other = world.getBlockState(pos.offset(d));
		if(other.getBlock() instanceof BlockConduit)
			return true;
		if(d.getAxis().isHorizontal() && other.getBlock() instanceof BlockComputerDevice)
		{
			BlockComputerDevice device = (BlockComputerDevice)other.getBlock();
			return device.hasConduitEntry() && other.getValue(BlockComputerDevice.FACING) == d;
		}
		return false;
	}

	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
	{
		Base base = state.getValue(BASE);
		if(base != Base.NONE && (connects(world, pos, base.side) || !holds(world, pos, base.side)))
			base = Base.NONE;
		return state.withProperty(NORTH, connects(world, pos, EnumFacing.NORTH)).withProperty(EAST, connects(world, pos, EnumFacing.EAST)).withProperty(SOUTH, connects(world, pos, EnumFacing.SOUTH))
				.withProperty(WEST, connects(world, pos, EnumFacing.WEST)).withProperty(UP, connects(world, pos, EnumFacing.UP)).withProperty(DOWN, connects(world, pos, EnumFacing.DOWN))
				.withProperty(BASE, base);
	}

	/** The box of the centre, grown to the edge on every side with a connection or a support. */
	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos)
	{
		state = state.getActualState(world, pos);
		Base base = state.getValue(BASE);
		return new AxisAlignedBB(state.getValue(WEST) || base == Base.WEST ? 0 : 0.2, state.getValue(DOWN) || base == Base.DOWN ? 0 : 0.325, state.getValue(NORTH) || base == Base.NORTH ? 0 : 0.2,
				state.getValue(EAST) || base == Base.EAST ? 1 : 0.8, state.getValue(UP) || base == Base.UP ? 1 : 0.6, state.getValue(SOUTH) || base == Base.SOUTH ? 1 : 0.8);
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

	@Override
	@SideOnly(Side.CLIENT)
	public BlockRenderLayer getRenderLayer()
	{
		return BlockRenderLayer.CUTOUT;
	}
}
