package immersivenuclear.common.block;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The IN Workbench (15/11/2026, the user: "igual a bancada do IE, um bloco que ocupa dois"): one block that takes two, like IE's
 * Engineer's Workbench. The block placed is the main half (it draws the whole bench, models/block/in_workbench.json, front north);
 * the other half (DUMMY, drawn empty) goes to its right as the player sees the front. Breaking either half takes the other with it and
 * drops one bench (and what it holds). The main half keeps the contents (TileINWorkbench); a click on either opens the interface.
 */
public class BlockINWorkbench extends Block
{
	public static final PropertyDirection FACING = BlockHorizontal.FACING;
	public static final PropertyBool DUMMY = PropertyBool.create("dummy");
	/** Drawn only: the sepia paper on the drafting board while a blueprint is in the bench (the user, 15/11/2026). */
	public static final PropertyBool BLUEPRINT = PropertyBool.create("blueprint");

	public BlockINWorkbench()
	{
		super(Material.IRON);
		setHardness(2.5f);
		setResistance(10f);
		setSoundType(SoundType.METAL);
		setHarvestLevel("axe", 0);
		setRegistryName(ImmersiveNuclear.MODID, "in_workbench");
		setTranslationKey(ImmersiveNuclear.MODID + ".in_workbench");
		setCreativeTab(INContent.TAB);
		setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(DUMMY, false).withProperty(BLUEPRINT, false));
	}

	/** Where the other half is, from this one. */
	public static BlockPos partner(IBlockState state, BlockPos pos)
	{
		// the other half to the RIGHT of the block placed, as the player sees the front (the user, 15/11/2026)
		EnumFacing side = state.getValue(FACING).rotateYCCW();
		return state.getValue(DUMMY) ? pos.offset(side.getOpposite()) : pos.offset(side);
	}

	@Override
	protected BlockStateContainer createBlockState()
	{
		return new BlockStateContainer(this, FACING, DUMMY, BLUEPRINT);
	}

	@Override
	public IBlockState getStateFromMeta(int meta)
	{
		return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)).withProperty(DUMMY, (meta & 4) != 0);
	}

	@Override
	public int getMetaFromState(IBlockState state)
	{
		return state.getValue(FACING).getHorizontalIndex() | (state.getValue(DUMMY) ? 4 : 0);
	}

	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
	{
		boolean paper = false;
		if(!state.getValue(DUMMY))
		{
			net.minecraft.tileentity.TileEntity te = world instanceof net.minecraft.world.ChunkCache
					? ((net.minecraft.world.ChunkCache)world).getTileEntity(pos, net.minecraft.world.chunk.Chunk.EnumCreateEntityType.CHECK) : world.getTileEntity(pos);
			paper = te instanceof TileINWorkbench && ((TileINWorkbench)te).hasBlueprint();
		}
		return state.withProperty(BLUEPRINT, paper);
	}

	@Override
	public IBlockState withRotation(IBlockState state, Rotation rot)
	{
		return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
	}

	@Override
	public IBlockState withMirror(IBlockState state, Mirror mirror)
	{
		return state.withRotation(mirror.toRotation(state.getValue(FACING)));
	}

	/** The front faces the player; the other half's place, to the right of the front, must be free too. */
	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand)
	{
		return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
	}

	@Override
	public boolean canPlaceBlockAt(World world, BlockPos pos)
	{
		if(!super.canPlaceBlockAt(world, pos))
			return false;
		for(EnumFacing f : EnumFacing.HORIZONTALS)
			if(world.getBlockState(pos.offset(f)).getBlock().isReplaceable(world, pos.offset(f)))
				return true;
		return false;
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
	{
		BlockPos other = partner(state, pos);
		if(world.getBlockState(other).getBlock().isReplaceable(world, other))
			world.setBlockState(other, state.withProperty(DUMMY, true), 3);
		else if(!world.isRemote)
		{
			// no room for the other half: the bench does not stay
			world.setBlockToAir(pos);
			if(placer instanceof net.minecraft.entity.player.EntityPlayer && !((net.minecraft.entity.player.EntityPlayer)placer).capabilities.isCreativeMode)
				spawnAsEntity(world, pos, new ItemStack(this));
		}
	}

	/** The main half's position, from either half. */
	public static BlockPos master(IBlockState state, BlockPos pos)
	{
		return state.getValue(DUMMY) ? partner(state, pos) : pos;
	}

	@Override
	public boolean hasTileEntity(IBlockState state)
	{
		return !state.getValue(DUMMY);
	}

	@Override
	public net.minecraft.tileentity.TileEntity createTileEntity(World world, IBlockState state)
	{
		return state.getValue(DUMMY) ? null : new TileINWorkbench();
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, net.minecraft.entity.player.EntityPlayer player, EnumHand hand, EnumFacing side,
			float hitX, float hitY, float hitZ)
	{
		if(player.isSneaking())
			return false;
		BlockPos m = master(state, pos);
		if(!world.isRemote && world.getTileEntity(m) instanceof TileINWorkbench)
			player.openGui(ImmersiveNuclear.instance, immersivenuclear.common.INGuiHandler.IN_WORKBENCH, world, m.getX(), m.getY(), m.getZ());
		return true;
	}

	/** Breaking one half takes the other (without its drop: the bench drops once); the contents drop. */
	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state)
	{
		net.minecraft.tileentity.TileEntity te = world.getTileEntity(pos);
		if(te instanceof TileINWorkbench)
			((TileINWorkbench)te).dropContents();
		BlockPos other = partner(state, pos);
		IBlockState o = world.getBlockState(other);
		if(o.getBlock() == this && o.getValue(DUMMY) != state.getValue(DUMMY))
			world.setBlockState(other, net.minecraft.init.Blocks.AIR.getDefaultState(), 3);
		super.breakBlock(world, pos, state);
	}

	@Override
	public net.minecraft.item.Item getItemDropped(IBlockState state, java.util.Random rand, int fortune)
	{
		return net.minecraft.item.Item.getItemFromBlock(this);
	}

	@Override
	public ItemStack getPickBlock(IBlockState state, net.minecraft.util.math.RayTraceResult target, World world, BlockPos pos, net.minecraft.entity.player.EntityPlayer player)
	{
		return new ItemStack(this);
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
		return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
	}
}
