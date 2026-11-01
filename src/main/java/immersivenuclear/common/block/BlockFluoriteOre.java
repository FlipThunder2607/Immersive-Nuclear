package immersivenuclear.common.block;

import java.util.Random;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;

/**
 * Fluorite ore: breaks into 1-2 fluorite crystals (more with Fortune, as vanilla's gem ores), some experience; Silk
 * Touch gives the ore itself, which IE's Crusher turns into more dust than the crystals would.
 */
public class BlockFluoriteOre extends Block
{
	public BlockFluoriteOre()
	{
		super(Material.ROCK);
		setHardness(3f);
		setResistance(5f);
		setHarvestLevel("pickaxe", 1);
		setRegistryName(ImmersiveNuclear.MODID, "fluorite_ore");
		setTranslationKey(ImmersiveNuclear.MODID + ".fluorite_ore");
		setCreativeTab(INContent.TAB);
	}

	@Override
	public Item getItemDropped(IBlockState state, Random rand, int fortune)
	{
		return INContent.itemFluorite;
	}

	@Override
	public int quantityDropped(Random random)
	{
		return 1 + random.nextInt(2);
	}

	@Override
	public int quantityDroppedWithBonus(int fortune, Random random)
	{
		// as vanilla's BlockOre: each level of Fortune may multiply the drop
		if(fortune > 0)
		{
			int bonus = random.nextInt(fortune + 2) - 1;
			return quantityDropped(random) * (Math.max(0, bonus) + 1);
		}
		return quantityDropped(random);
	}

	@Override
	public int getExpDrop(IBlockState state, IBlockAccess world, BlockPos pos, int fortune)
	{
		Random rand = world instanceof net.minecraft.world.World ? ((net.minecraft.world.World)world).rand : new Random();
		return MathHelper.getInt(rand, 1, 3);
	}
}
