package immersivenuclear.common.block;

import javax.annotation.Nullable;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.INGuiHandler;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * The Concentrate Drum (see TileConcentrateDrum): a standing steel drum, a little narrower than a block (its model is
 * the user's, design/itens-e-fluidos/tambor/concentrate_drum-v2.bbmodel, through ferramentas/bb2json.py). Right click
 * opens its slot; broken, it drops itself with its yellowcake inside, like a shulker box, so a filled drum can be
 * carried, stored or sent down a conveyor as one item. A comparator reads how full it is.
 */
public class BlockConcentrateDrum extends Block
{
	/** The drum's rims (the model's widest parts, 1.5..14.5 px); its full height, the bungs aside. */
	static final AxisAlignedBB BOX = new AxisAlignedBB(1.5 / 16D, 0, 1.5 / 16D, 14.5 / 16D, 1, 14.5 / 16D);

	public BlockConcentrateDrum()
	{
		super(Material.IRON);
		setHardness(2.5f);
		setResistance(15f);
		setSoundType(SoundType.METAL);
		setHarvestLevel("pickaxe", 1);
		setRegistryName(ImmersiveNuclear.MODID, "concentrate_drum");
		setTranslationKey(ImmersiveNuclear.MODID + ".concentrate_drum");
		setCreativeTab(INContent.TAB);
	}

	@Override
	public boolean hasTileEntity(IBlockState state)
	{
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state)
	{
		return new TileConcentrateDrum();
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
	{
		if(player.isSneaking())
			return false;
		if(!world.isRemote)
			player.openGui(ImmersiveNuclear.instance, INGuiHandler.CONCENTRATE_DRUM, world, pos.getX(), pos.getY(), pos.getZ());
		return true;
	}

	// ---------- keeping the contents when broken, like a shulker box ----------

	/** The drum as an item, its yellowcake inside (none when empty, so empty drums stack). */
	public ItemStack drumStack(@Nullable TileEntity te)
	{
		ItemStack stack = new ItemStack(this);
		NBTTagCompound contents = te instanceof TileConcentrateDrum ? ((TileConcentrateDrum)te).contentsTag() : null;
		if(contents != null)
		{
			NBTTagCompound tag = new NBTTagCompound();
			tag.setTag("BlockEntityTag", contents);
			stack.setTagCompound(tag);
		}
		return stack;
	}

	/** The block stays until it has dropped (harvestBlock), so getDrops still finds its tile. */
	@Override
	public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest)
	{
		if(willHarvest)
			return true;
		// Broken in creative: a drum with yellowcake still drops, so nothing is lost by accident
		TileEntity te = world.getTileEntity(pos);
		if(!world.isRemote && te instanceof TileConcentrateDrum && ((TileConcentrateDrum)te).getCount() > 0)
			spawnAsEntity(world, pos, drumStack(te));
		return super.removedByPlayer(state, world, pos, player, false);
	}

	@Override
	public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack tool)
	{
		super.harvestBlock(world, player, pos, state, te, tool);
		world.setBlockToAir(pos);
	}

	@Override
	public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune)
	{
		drops.add(drumStack(world.getTileEntity(pos)));
	}

	// ---------- comparator ----------

	@Override
	public boolean hasComparatorInputOverride(IBlockState state)
	{
		return true;
	}

	@Override
	public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos)
	{
		TileEntity te = world.getTileEntity(pos);
		return te instanceof TileConcentrateDrum ? ItemHandlerHelper.calcRedstoneFromInventory(((TileConcentrateDrum)te).getInventory()) : 0;
	}

	// ---------- shape ----------

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos)
	{
		return BOX;
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
		return face == EnumFacing.DOWN ? BlockFaceShape.CENTER_BIG : BlockFaceShape.UNDEFINED;
	}
}
