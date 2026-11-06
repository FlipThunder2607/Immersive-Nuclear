package immersivenuclear.common.block;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.multiblock.ComputerStructure;
import immersivenuclear.common.multiblock.MultiblockComputer;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Every part of a formed computer multiblock (models/block/obj/&lt;name&gt;*.obj, see {@link ComputerStructure}). For now only its model: no interface, no function. Each part draws its own slice of the model (see {@link MachinePartBlock}). */
public class BlockComputerMachine extends Block implements MachinePartBlock
{
	/** Set while parts are being put back, so restoring one part does not start another disassembly. */
	static boolean disassembling;

	public final ComputerStructure structure;

	public BlockComputerMachine(ComputerStructure structure)
	{
		super(Material.IRON);
		this.structure = structure;
		setRegistryName(ImmersiveNuclear.MODID, structure.name);
		setTranslationKey(ImmersiveNuclear.MODID + "." + structure.name);
		setHardness(3.0F);
		setResistance(15.0F);
		setSoundType(SoundType.METAL);
		setLightOpacity(0);
		setDefaultState(blockState.getBaseState().withProperty(BlockChemicalProcessor.FACING, EnumFacing.NORTH)
				.withProperty(BlockChemicalProcessor.MASTER, false).withProperty(BlockChemicalProcessor.MIRRORED, false));
	}

	@Override
	protected BlockStateContainer createBlockState()
	{
		return new ExtendedBlockState(this, new IProperty[]{BlockChemicalProcessor.FACING, BlockChemicalProcessor.MASTER, BlockChemicalProcessor.MIRRORED}, new IUnlistedProperty[]{OFFSET});
	}

	@Override
	public int getMetaFromState(IBlockState state)
	{
		return 0;
	}

	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
	{
		TileEntity te = world.getTileEntity(pos);
		if(te instanceof TileComputerMachine)
		{
			TileComputerMachine part = (TileComputerMachine)te;
			return state.withProperty(BlockChemicalProcessor.FACING, part.getFacing()).withProperty(BlockChemicalProcessor.MASTER, part.isMaster())
					.withProperty(BlockChemicalProcessor.MIRRORED, part.isMirrored());
		}
		return state;
	}

	@Override
	public boolean hasTileEntity(IBlockState state)
	{
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state)
	{
		return new TileComputerMachine();
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state)
	{
		TileEntity te = world.getTileEntity(pos);
		if(!disassembling && !world.isRemote && te instanceof TileComputerMachine)
		{
			TileComputerMachine part = (TileComputerMachine)te;
			disassembling = true;
			try
			{
				MultiblockComputer.ALL[structure.index].disassemble(world, part.getOrigin(), part.getFacing(), pos);
			}
			finally
			{
				disassembling = false;
			}
		}
		super.breakBlock(world, pos, state);
	}

	/** A broken part drops the block it replaced; nothing in creative, as harvestBlock is survival only. */
	@Override
	public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te, ItemStack stack)
	{
		player.addExhaustion(0.005F);
		if(te instanceof TileComputerMachine)
			spawnAsEntity(world, pos, ((TileComputerMachine)te).getOriginalStack());
	}

	/** The parts are placed by the structure; the hammer (or anything else) must not turn them. */
	@Override
	public boolean rotateBlock(World world, BlockPos pos, EnumFacing axis)
	{
		return false;
	}

	/** The block the part stands for, never the machine's model: the machine items are only icons. */
	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player)
	{
		TileEntity te = world.getTileEntity(pos);
		return te instanceof TileComputerMachine ? ((TileComputerMachine)te).getOriginalStack() : ItemStack.EMPTY;
	}

	/** Shaped like the model (MachineBounds): outline, collision and cursor. */
	@Override
	public net.minecraft.util.math.AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos)
	{
		return MachineBounds.outline(world, pos);
	}

	@Override
	public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, net.minecraft.util.math.AxisAlignedBB entityBox,
									  java.util.List<net.minecraft.util.math.AxisAlignedBB> boxes, @javax.annotation.Nullable net.minecraft.entity.Entity entity, boolean isActualState)
	{
		MachineBounds.addCollision(world, pos, entityBox, boxes, entity);
	}

	@Override
	public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, net.minecraft.util.math.Vec3d start, net.minecraft.util.math.Vec3d end)
	{
		return MachineBounds.rayTrace(world, pos, start, end);
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

	/** Which slice of the machine's model this part draws: its offset from the master. */
	@Override
	public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos)
	{
		TileEntity te = world.getTileEntity(pos);
		if(state instanceof IExtendedBlockState && te instanceof TileComputerMachine)
			return ((IExtendedBlockState)state).withProperty(OFFSET, pos.subtract(((TileComputerMachine)te).getMasterPos()));
		return state;
	}

	@Override
	public boolean isPartAt(EnumFacing facing, boolean mirrored, BlockPos offset)
	{
		int[] t = ComputerStructure.unrotate(offset.getX(), offset.getZ(), MultiblockComputer.turns(facing));
		return structure.isPart(structure.masterDx + t[0], structure.masterDy + offset.getY(), structure.masterDz + t[1]);
	}

	/** The model's light: the brightest of the spot and its neighbours, so a solid block beside the master does not darken it. */
	@Override
	@SideOnly(Side.CLIENT)
	public int getPackedLightmapCoords(IBlockState state, IBlockAccess world, BlockPos pos)
	{
		return MachineLight.packed(world, pos);
	}

	@Override
	public BlockRenderLayer getRenderLayer()
	{
		return BlockRenderLayer.CUTOUT;
	}
}
