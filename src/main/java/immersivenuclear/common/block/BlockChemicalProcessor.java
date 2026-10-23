package immersivenuclear.common.block;

import blusunrize.immersiveengineering.api.Lib;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INGuiHandler;
import immersivenuclear.common.INContent;
import immersivenuclear.common.multiblock.ChemicalProcessorStructure;
import immersivenuclear.common.multiblock.MultiblockChemicalProcessor;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
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

/**
 * Every part of a formed Chemical Processing Unit. The master part draws the whole unit
 * (models/block/obj/chemical_processor*.obj, generated from design/modelo-processamento-quimico);
 * the other parts are invisible. The properties only come from the tile entity, nothing is stored in meta.
 */
public class BlockChemicalProcessor extends Block
{
	public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
	public static final PropertyBool MASTER = PropertyBool.create("master");
	public static final PropertyBool MIRRORED = PropertyBool.create("mirrored");

	/** Set while parts are being put back, so restoring one part does not start another disassembly. */
	static boolean disassembling;

	public BlockChemicalProcessor()
	{
		super(Material.IRON);
		setRegistryName(ImmersiveNuclear.MODID, "chemical_processor");
		setTranslationKey(ImmersiveNuclear.MODID + ".chemical_processor");
		setHardness(3.0F);
		setResistance(15.0F);
		setSoundType(SoundType.METAL);
		setLightOpacity(0);
		setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(MASTER, false).withProperty(MIRRORED, false));
	}

	@Override
	protected BlockStateContainer createBlockState()
	{
		return new BlockStateContainer(this, FACING, MASTER, MIRRORED);
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
		if(te instanceof TileChemicalProcessor)
		{
			TileChemicalProcessor part = (TileChemicalProcessor)te;
			return state.withProperty(FACING, part.getFacing()).withProperty(MASTER, part.isMaster()).withProperty(MIRRORED, part.isMirrored());
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
		return new TileChemicalProcessor();
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state)
	{
		TileEntity te = world.getTileEntity(pos);
		if(!disassembling && !world.isRemote && te instanceof TileChemicalProcessor)
		{
			TileChemicalProcessor part = (TileChemicalProcessor)te;
			TileChemicalProcessor master = part.getMaster();
			if(master != null)
				master.dropContents();
			disassembling = true;
			try
			{
				MultiblockChemicalProcessor.disassemble(world, part.getTriggerPos(), part.getFacing(), part.isMirrored(), pos);
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
		if(te instanceof TileChemicalProcessor)
			spawnAsEntity(world, pos, ((TileChemicalProcessor)te).getOriginalStack());
	}

	/** Right click on any part opens the interface of the master, like IE's multiblocks (not while sneaking or with the hammer). */
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
	{
		ItemStack held = player.getHeldItem(hand);
		if(player.isSneaking() || !held.isEmpty() && held.getItem().getToolClasses(held).contains(Lib.TOOL_HAMMER))
			return false;
		TileEntity te = world.getTileEntity(pos);
		if(!(te instanceof TileChemicalProcessor))
			return false;
		TileChemicalProcessor master = ((TileChemicalProcessor)te).getMaster();
		if(master == null)
			return false;
		if(!world.isRemote)
		{
			BlockPos m = master.getPos();
			player.openGui(ImmersiveNuclear.instance, INGuiHandler.CHEMICAL_PROCESSOR, world, m.getX(), m.getY(), m.getZ());
		}
		return true;
	}

	/** Only the redstone port takes a signal, so redstone dust only bends towards it. */
	@Override
	public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side)
	{
		TileEntity te = world.getTileEntity(pos);
		return te instanceof TileChemicalProcessor && ((TileChemicalProcessor)te).getPort() == ChemicalProcessorStructure.PORT_REDSTONE;
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
		return new ItemStack(INContent.itemChemicalProcessor);
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
	public BlockRenderLayer getRenderLayer()
	{
		return BlockRenderLayer.CUTOUT;
	}
}
