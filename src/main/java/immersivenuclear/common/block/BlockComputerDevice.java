package immersivenuclear.common.block;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A single-block device of the mod's computers (card reader, card punch, paper-tape unit, CRT terminal, chart recorder, wall panel).
 * For now it is only a model: it can be placed on the ground (or anywhere), faces the player and has no function yet (the user, 04/11/2026).
 * The model is drawn from models/block/&lt;name&gt;.json (made from the design's .bbmodel by ferramentas/bbmodel_para_bloco_json.py); its front is north.
 */
public class BlockComputerDevice extends Block
{
	public static final PropertyDirection FACING = BlockHorizontal.FACING;

	/** The selection and collision box in blocks, for a device facing north. */
	private final AxisAlignedBB northBox;
	private final boolean translucent;
	private boolean conduitEntry = true;

	public BlockComputerDevice(String name, AxisAlignedBB northBox, boolean translucent)
	{
		super(Material.IRON);
		this.northBox = northBox;
		this.translucent = translucent;
		setHardness(3f);
		setResistance(15f);
		setSoundType(SoundType.METAL);
		setHarvestLevel("pickaxe", 0);
		setRegistryName(ImmersiveNuclear.MODID, name);
		setTranslationKey(ImmersiveNuclear.MODID + "." + name);
		setCreativeTab(INContent.TAB);
		setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
	}

	/** The wall panel takes its data by a click, like any device, and has no conduit entry: the conduit does not connect to it. */
	public BlockComputerDevice noConduitEntry()
	{
		conduitEntry = false;
		return this;
	}

	/** Whether the back of this device has a conduit entry (the conduit connects to it there). */
	public boolean hasConduitEntry()
	{
		return conduitEntry;
	}

	@Override
	protected BlockStateContainer createBlockState()
	{
		return new BlockStateContainer(this, FACING);
	}

	@Override
	public IBlockState getStateFromMeta(int meta)
	{
		return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
	}

	@Override
	public int getMetaFromState(IBlockState state)
	{
		return state.getValue(FACING).getHorizontalIndex();
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

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand)
	{
		return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
	}

	/** The box turned to the way the device faces (north is the model's front; the blockstate turns the model the same way). */
	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
	{
		AxisAlignedBB b = northBox;
		switch(state.getValue(FACING))
		{
			case EAST:
				return new AxisAlignedBB(1 - b.maxZ, b.minY, b.minX, 1 - b.minZ, b.maxY, b.maxX);
			case SOUTH:
				return new AxisAlignedBB(1 - b.maxX, b.minY, 1 - b.maxZ, 1 - b.minX, b.maxY, 1 - b.minZ);
			case WEST:
				return new AxisAlignedBB(b.minZ, b.minY, 1 - b.maxX, b.maxZ, b.maxY, 1 - b.minX);
			default:
				return b;
		}
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
		return translucent ? BlockRenderLayer.TRANSLUCENT : BlockRenderLayer.CUTOUT;
	}
}
