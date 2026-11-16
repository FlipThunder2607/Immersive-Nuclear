package immersivenuclear.common.world;

import java.util.Random;

import immersivenuclear.common.INConfig;
import immersivenuclear.common.INRawMaterials;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.IWorldGenerator;

/**
 * The line 3 raw materials in the Overworld, where they are found for real (the user, 14/11/2026):
 * - zircon in clusters inside the granite pockets (its main source, like pegmatites) and, rarely, in the sand of beaches;
 * - borax in the terracotta of mesas (old dry lake clays) and near the surface of deserts' sand (a dry lake's crust).
 * Sizes, heights and frequencies from the config (INConfig).
 */
public class WorldGenRawMaterials implements IWorldGenerator
{
	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator generator, IChunkProvider provider)
	{
		if(world.provider.getDimension() != 0)
			return;
		int x0 = chunkX * 16 + 8, z0 = chunkZ * 16 + 8;
		Biome biome = world.getBiome(new BlockPos(x0 + 8, 64, z0 + 8));
		INConfig.Ore granite = INConfig.zirconGranite;
		if(granite.generate && granite.maxY > granite.minY)
		{
			WorldGenMinable vein = new WorldGenMinable(INRawMaterials.zirconOreGranite.getDefaultState(), granite.veinSize,
					s -> s != null && s.getBlock() == Blocks.STONE && s.getValue(BlockStone.VARIANT) == BlockStone.EnumType.GRANITE);
			for(int i = 0; i < granite.veinsPerChunk; i++)
				vein.generate(world, random, new BlockPos(x0 + random.nextInt(16), granite.minY + random.nextInt(granite.maxY - granite.minY), z0 + random.nextInt(16)));
		}
		stone(world, random, x0, z0, INConfig.tin, INRawMaterials.tinOre);
		stone(world, random, x0, z0, INConfig.chromite, INRawMaterials.chromiteOre);
		INConfig.Ore mesa = INConfig.boraxMesa;
		if(mesa.generate && mesa.maxY > mesa.minY && BiomeDictionary.hasType(biome, BiomeDictionary.Type.MESA))
		{
			WorldGenMinable vein = new WorldGenMinable(INRawMaterials.boraxOreTerracotta.getDefaultState(), mesa.veinSize,
					s -> s != null && (s.getBlock() == Blocks.HARDENED_CLAY || s.getBlock() == Blocks.STAINED_HARDENED_CLAY));
			for(int i = 0; i < mesa.veinsPerChunk; i++)
				vein.generate(world, random, new BlockPos(x0 + random.nextInt(16), mesa.minY + random.nextInt(mesa.maxY - mesa.minY), z0 + random.nextInt(16)));
		}
		if(BiomeDictionary.hasType(biome, BiomeDictionary.Type.BEACH))
			surface(world, random, x0, z0, INConfig.zirconBeach, INRawMaterials.zirconOreSand);
		if(BiomeDictionary.hasType(biome, BiomeDictionary.Type.SANDY) && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.BEACH)
				&& !BiomeDictionary.hasType(biome, BiomeDictionary.Type.MESA))
			surface(world, random, x0, z0, INConfig.boraxDesert, INRawMaterials.boraxOreSand);
	}

	/** Ordinary veins in the stone (tin, chromite), as IE's metal ores. */
	static void stone(World world, Random random, int x0, int z0, INConfig.Ore c, Block ore)
	{
		if(!c.generate || c.maxY <= c.minY)
			return;
		WorldGenMinable vein = new WorldGenMinable(ore.getDefaultState(), c.veinSize);
		for(int i = 0; i < c.veinsPerChunk; i++)
			vein.generate(world, random, new BlockPos(x0 + random.nextInt(16), c.minY + random.nextInt(c.maxY - c.minY), z0 + random.nextInt(16)));
	}

	/** A small patch of ore in the sand just under the surface (1 to 3 blocks down), now and then. */
	static void surface(World world, Random random, int x0, int z0, INConfig.Surface c, Block ore)
	{
		if(!c.generate || random.nextInt(c.oneInChunks) != 0)
			return;
		int x = x0 + random.nextInt(16), z = z0 + random.nextInt(16);
		BlockPos top = world.getHeight(new BlockPos(x, 0, z));
		for(int n = 0; n < c.patchSize; n++)
		{
			BlockPos p = top.add(random.nextInt(5) - 2, -1 - random.nextInt(3), random.nextInt(5) - 2);
			IBlockState s = world.getBlockState(p);
			if(s.getBlock() == Blocks.SAND && s.getValue(net.minecraft.block.BlockSand.VARIANT) == net.minecraft.block.BlockSand.EnumType.SAND)
				world.setBlockState(p, ore.getDefaultState(), 2);
		}
	}
}
