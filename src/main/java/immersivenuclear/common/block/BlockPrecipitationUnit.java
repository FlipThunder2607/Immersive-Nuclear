package immersivenuclear.common.block;

import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IHammerInteraction;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INGuiHandler;
import immersivenuclear.common.INContent;
import immersivenuclear.common.multiblock.PrecipitationUnitStructure;
import immersivenuclear.common.multiblock.MultiblockPrecipitationUnit;
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
 * Every part of a formed Precipitation Unit. The master part draws the whole unit
 * (models/block/obj/precipitation_unit*.obj);
 * the other parts are invisible. Same properties as {@link BlockChemicalProcessor}, all from the tile entity.
 */
public class BlockPrecipitationUnit extends Block implements MachinePartBlock
{
	/** Set while parts are being put back, so restoring one part does not start another disassembly. */
	static boolean disassembling;

	public BlockPrecipitationUnit()
	{
		super(Material.IRON);
		setRegistryName(ImmersiveNuclear.MODID, "precipitation_unit");
		setTranslationKey(ImmersiveNuclear.MODID + ".precipitation_unit");
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
		if(te instanceof TilePrecipitationUnit)
		{
			TilePrecipitationUnit part = (TilePrecipitationUnit)te;
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
		return new TilePrecipitationUnit();
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state)
	{
		TileEntity te = world.getTileEntity(pos);
		if(!disassembling && !world.isRemote && te instanceof TilePrecipitationUnit)
		{
			TilePrecipitationUnit part = (TilePrecipitationUnit)te;
			TilePrecipitationUnit master = part.getMaster();
			if(master != null)
				master.dropContents();
			disassembling = true;
			try
			{
				MultiblockPrecipitationUnit.disassemble(world, part.getTriggerPos(), part.getFacing(), part.isMirrored(), pos);
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
		if(te instanceof TilePrecipitationUnit)
			spawnAsEntity(world, pos, ((TilePrecipitationUnit)te).getOriginalStack());
	}

	/** Right click on any part opens the interface of the master, like IE's multiblocks (not while sneaking; the hammer goes to the part, which inverts the redstone port). */
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
		if(!(te instanceof TilePrecipitationUnit))
			return false;
		TilePrecipitationUnit master = ((TilePrecipitationUnit)te).getMaster();
		if(master == null)
			return false;
		if(!world.isRemote)
		{
			BlockPos m = master.getPos();
			player.openGui(ImmersiveNuclear.instance, INGuiHandler.PRECIPITATION_UNIT, world, m.getX(), m.getY(), m.getZ());
		}
		return true;
	}

	/** Only the redstone port takes a signal, so redstone dust only bends towards it. */
	@Override
	public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side)
	{
		TileEntity te = world.getTileEntity(pos);
		return te instanceof TilePrecipitationUnit && ((TilePrecipitationUnit)te).getPort() == PrecipitationUnitStructure.PORT_REDSTONE;
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
		return new ItemStack(INContent.itemPrecipitationUnit);
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
		if(state instanceof IExtendedBlockState && te instanceof TilePrecipitationUnit)
			return ((IExtendedBlockState)state).withProperty(OFFSET, pos.subtract(((TilePrecipitationUnit)te).getMasterPos()));
		return state;
	}

	@Override
	public boolean isPartAt(EnumFacing facing, boolean mirrored, BlockPos offset)
	{
		int[] t = PrecipitationUnitStructure.unrotate(offset.getX(), offset.getZ(), MultiblockPrecipitationUnit.turns(facing), mirrored);
		return PrecipitationUnitStructure.isPart(PrecipitationUnitStructure.MASTER_DX + t[0], PrecipitationUnitStructure.MASTER_DY + offset.getY(), PrecipitationUnitStructure.MASTER_DZ + t[1]);
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
