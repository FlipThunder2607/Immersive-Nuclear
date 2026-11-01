package immersivenuclear.common.world;

import java.util.Random;

import immersivenuclear.common.INConfig;
import immersivenuclear.common.INContent;
import net.minecraft.block.state.pattern.BlockMatcher;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.fml.common.IWorldGenerator;

/** Fluorite ore veins in the Overworld's stone, sizes and heights from the config (INConfig.fluorite). */
public class WorldGenFluorite implements IWorldGenerator
{
	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator generator, IChunkProvider provider)
	{
		INConfig.Ore c = INConfig.fluorite;
		if(!c.generate || world.provider.getDimension() != 0 || c.veinsPerChunk <= 0 || c.maxY <= c.minY)
			return;
		WorldGenMinable vein = new WorldGenMinable(INContent.blockFluoriteOre.getDefaultState(), c.veinSize, BlockMatcher.forBlock(Blocks.STONE));
		for(int i = 0; i < c.veinsPerChunk; i++)
			vein.generate(world, random, new BlockPos(chunkX * 16 + 8 + random.nextInt(16), c.minY + random.nextInt(c.maxY - c.minY), chunkZ * 16 + 8 + random.nextInt(16)));
	}
}
