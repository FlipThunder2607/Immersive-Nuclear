package immersivenuclear.common.block;

import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IHammerInteraction;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.multiblock.EnrichmentUnitStructure;
import immersivenuclear.common.multiblock.MultiblockEnrichmentUnit;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Every part of a formed EnrichmentUnit (models/block/obj/enrichment_unit*.obj). Same properties as
 * {@link BlockChemicalProcessor}, all from the tile entity, and {@link #ACTIVE}: the machine runs.
 * Every part draws its own slice of the model (see {@link MachinePartBlock}).
 */
public class BlockEnrichmentUnit extends Block implements MachinePartBlock
{
	public static final PropertyBool ACTIVE = PropertyBool.create("active");

	/** Set while parts are being put back, so restoring one part does not start another disassembly. */
	static boolean disassembling;

	public BlockEnrichmentUnit()
	{
		super(Material.IRON);
		setRegistryName(ImmersiveNuclear.MODID, "enrichment_unit");
		setTranslationKey(ImmersiveNuclear.MODID + ".enrichment_unit");
		setHardness(3.0F);
		setResistance(15.0F);
		setSoundType(SoundType.METAL);
		setLightOpacity(0);
		setDefaultState(blockState.getBaseState().withProperty(BlockChemicalProcessor.FACING, EnumFacing.NORTH)
				.withProperty(BlockChemicalProcessor.MASTER, false).withProperty(BlockChemicalProcessor.MIRRORED, false).withProperty(ACTIVE, false));
	}

	@Override
	protected BlockStateContainer createBlockState()
	{
		return new ExtendedBlockState(this, new IProperty[]{BlockChemicalProcessor.FACING, BlockChemicalProcessor.MASTER, BlockChemicalProcessor.MIRRORED, ACTIVE}, new IUnlistedProperty[]{OFFSET});
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
		if(te instanceof TileEnrichmentUnit)
		{
			TileEnrichmentUnit part = (TileEnrichmentUnit)te;
			return state.withProperty(BlockChemicalProcessor.FACING, part.getFacing()).withProperty(BlockChemicalProcessor.MASTER, part.isMaster())
					.withProperty(BlockChemicalProcessor.MIRRORED, part.isMirrored()).withProperty(ACTIVE, part.running());
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
		return new TileEnrichmentUnit();
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state)
	{
		TileEntity te = world.getTileEntity(pos);
		if(!disassembling && !world.isRemote && te instanceof TileEnrichmentUnit)
		{
			TileEnrichmentUnit part = (TileEnrichmentUnit)te;
			TileEnrichmentBase master = part.getMaster();
			if(master != null)
				master.dropContents();
			disassembling = true;
			try
			{
				MultiblockEnrichmentUnit.disassemble(world, part.getTriggerPos(), part.getFacing(), part.isMirrored(), pos);
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
		if(te instanceof TileEnrichmentUnit && ((TileEnrichmentUnit)te).getOriginalStack() != null)
			spawnAsEntity(world, pos, ((TileEnrichmentUnit)te).getOriginalStack());
	}

	/** Right click on any part opens the master's interface (not while sneaking or with the hammer). */
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
	{
		ItemStack held = player.getHeldItem(hand);
		if(!held.isEmpty() && held.getItem().getToolClasses(held).contains(Lib.TOOL_HAMMER))
		{
			// IE's own blocks hand the hammer to their tile (the hammer item does not): the redstone port inverts
			TileEntity tile = world.getTileEntity(pos);
			return tile instanceof IHammerInteraction && ((IHammerInteraction)tile).hammerUseSide(side, player, hitX, hitY, hitZ);
		}
		if(player.isSneaking())
			return false;
		TileEntity te = world.getTileEntity(pos);
		if(!(te instanceof TileEnrichmentBase) || ((TileEnrichmentBase)te).getMaster() == null)
			return false;
		if(!world.isRemote)
		{
			BlockPos m = ((TileEnrichmentBase)te).getMaster().getPos();
			player.openGui(ImmersiveNuclear.instance, immersivenuclear.common.INGuiHandler.ENRICHMENT_UNIT, world, m.getX(), m.getY(), m.getZ());
		}
		return true;
	}

	/** Only the redstone port takes a signal, so redstone dust only bends towards it. */
	@Override
	public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side)
	{
		TileEntity te = world.getTileEntity(pos);
		return te instanceof TileEnrichmentUnit && ((TileEnrichmentUnit)te).getPort() == EnrichmentUnitStructure.PORT_REDSTONE;
	}

	/** The parts are placed by the structure; the hammer (or anything else) must not turn them. */
	@Override
	public boolean rotateBlock(World world, BlockPos pos, EnumFacing axis)
	{
		return false;
	}

	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player)
	{
		// The block the part stands for (the template block it replaced), never the machine's model: the machine items are
		// only icons (JEI, the 3D model) and must not reach an inventory
		net.minecraft.tileentity.TileEntity te = world.getTileEntity(pos);
		if(te instanceof TileEnrichmentUnit)
		{
			ItemStack s = ((TileEnrichmentUnit)te).getOriginalStack();
			if(s != null && !s.isEmpty())
				return s;
		}
		return ItemStack.EMPTY;
	}

	/** Shaped like the model (MachineBounds): outline, collision and cursor; ports keep the whole block. */
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

	/** The ladder blocks of the structure are climbable (IE's ladder codes 'L' and 'M' in the template). */
	@Override
	public boolean isLadder(IBlockState state, IBlockAccess world, BlockPos pos, net.minecraft.entity.EntityLivingBase entity)
	{
		TileEntity te = world.getTileEntity(pos);
		return te instanceof TileEnrichmentUnit && (((TileEnrichmentUnit)te).getPartCode() == 'L' || ((TileEnrichmentUnit)te).getPartCode() == 'M');
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
		if(state instanceof IExtendedBlockState && te instanceof TileEnrichmentUnit)
			return ((IExtendedBlockState)state).withProperty(OFFSET, pos.subtract(((TileEnrichmentUnit)te).getMasterPos()));
		return state;
	}

	@Override
	public boolean isPartAt(EnumFacing facing, boolean mirrored, BlockPos offset)
	{
		int[] t = EnrichmentUnitStructure.unrotate(offset.getX(), offset.getZ(), MultiblockEnrichmentUnit.turns(facing), mirrored);
		return EnrichmentUnitStructure.isPart(EnrichmentUnitStructure.MASTER_DX + t[0], EnrichmentUnitStructure.MASTER_DY + offset.getY(), EnrichmentUnitStructure.MASTER_DZ + t[1]) || EnrichmentUnitStructure.isCollisionOnly(EnrichmentUnitStructure.MASTER_DX + t[0], EnrichmentUnitStructure.MASTER_DY + offset.getY(), EnrichmentUnitStructure.MASTER_DZ + t[1]);
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
