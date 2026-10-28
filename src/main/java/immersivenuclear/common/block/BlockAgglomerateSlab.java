package immersivenuclear.common.block;

import java.util.Random;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

/**
 * Agglomerate brick slab, as vanilla's: a half block (bottom or top) and the double slab two of them make, which drops
 * them back. One variant only, which vanilla's slab still asks for.
 */
public abstract class BlockAgglomerateSlab extends BlockSlab
{
	public enum Variant implements IStringSerializable
	{
		DEFAULT;

		@Override
		public String getName()
		{
			return "default";
		}
	}

	public static final PropertyEnum<Variant> VARIANT = PropertyEnum.create("variant", Variant.class);

	BlockAgglomerateSlab(String name)
	{
		super(Material.ROCK);
		setHardness(3f);
		setResistance(60f);
		setSoundType(SoundType.STONE);
		setHarvestLevel("pickaxe", 1);
		setRegistryName(ImmersiveNuclear.MODID, name);
		setTranslationKey(ImmersiveNuclear.MODID + ".agglomerate_brick_slab");
		IBlockState state = blockState.getBaseState().withProperty(VARIANT, Variant.DEFAULT);
		if(!isDouble())
		{
			state = state.withProperty(HALF, EnumBlockHalf.BOTTOM);
			setCreativeTab(INContent.TAB);
		}
		setDefaultState(state);
		useNeighborBrightness = !isDouble();
	}

	@Override
	public String getTranslationKey(int meta)
	{
		return getTranslationKey();
	}

	@Override
	public IProperty<?> getVariantProperty()
	{
		return VARIANT;
	}

	@Override
	public Comparable<?> getTypeForItem(ItemStack stack)
	{
		return Variant.DEFAULT;
	}

	@Override
	public IBlockState getStateFromMeta(int meta)
	{
		IBlockState state = getDefaultState();
		return isDouble() ? state : state.withProperty(HALF, (meta & 8) != 0 ? EnumBlockHalf.TOP : EnumBlockHalf.BOTTOM);
	}

	@Override
	public int getMetaFromState(IBlockState state)
	{
		return !isDouble() && state.getValue(HALF) == EnumBlockHalf.TOP ? 8 : 0;
	}

	@Override
	protected BlockStateContainer createBlockState()
	{
		return isDouble() ? new BlockStateContainer(this, VARIANT) : new BlockStateContainer(this, HALF, VARIANT);
	}

	@Override
	public Item getItemDropped(IBlockState state, Random rand, int fortune)
	{
		return Item.getItemFromBlock(INContent.blockAgglomerateSlab);
	}

	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player)
	{
		return new ItemStack(INContent.blockAgglomerateSlab);
	}

	public static class Half extends BlockAgglomerateSlab
	{
		public Half()
		{
			super("agglomerate_brick_slab");
		}

		@Override
		public boolean isDouble()
		{
			return false;
		}
	}

	public static class Double extends BlockAgglomerateSlab
	{
		public Double()
		{
			super("agglomerate_brick_slab_double");
		}

		@Override
		public boolean isDouble()
		{
			return true;
		}
	}
}
