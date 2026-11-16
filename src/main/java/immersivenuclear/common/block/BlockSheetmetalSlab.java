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
 * A sheet metal slab, as IE's: the half block (bottom or top) and the double slab two of them make, which drops them
 * back. One per metal (INRawMaterials.Metal); the double finds its half through 'half', set once both exist.
 */
public abstract class BlockSheetmetalSlab extends BlockSlab
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
	/** The half slab this block drops (itself for the half). */
	public BlockSheetmetalSlab half;

	BlockSheetmetalSlab(String name, String translation)
	{
		super(Material.IRON);
		setHardness(3f);
		setResistance(10f);
		setSoundType(SoundType.METAL);
		setHarvestLevel("pickaxe", 1);
		setRegistryName(ImmersiveNuclear.MODID, name);
		setTranslationKey(ImmersiveNuclear.MODID + "." + translation);
		IBlockState state = blockState.getBaseState().withProperty(VARIANT, Variant.DEFAULT);
		if(!isDouble())
		{
			state = state.withProperty(HALF, EnumBlockHalf.BOTTOM);
			setCreativeTab(INContent.TAB);
			half = this;
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
		return Item.getItemFromBlock(half);
	}

	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player)
	{
		return new ItemStack(half);
	}

	public static class Half extends BlockSheetmetalSlab
	{
		public Half(String metal)
		{
			super(metal + "_sheetmetal_slab", metal + "_sheetmetal_slab");
		}

		@Override
		public boolean isDouble()
		{
			return false;
		}
	}

	public static class Double extends BlockSheetmetalSlab
	{
		public Double(String metal)
		{
			super(metal + "_sheetmetal_slab_double", metal + "_sheetmetal_slab");
		}

		@Override
		public boolean isDouble()
		{
			return true;
		}
	}
}
