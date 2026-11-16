package immersivenuclear.common.block;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;

/**
 * An ore that drops itself, as IE's metal ores: the Crusher turns it into two dusts, the Arc Furnace into ingots.
 * The ground it sits in sets its material, sound and tool (stone and terracotta: pickaxe; sand: shovel).
 */
public class BlockINOre extends Block
{
	public enum Ground
	{
		STONE(Material.ROCK, SoundType.STONE, "pickaxe", 3f), CLAY(Material.ROCK, SoundType.STONE, "pickaxe", 2f), SAND(Material.SAND, SoundType.SAND, "shovel", 1f);

		final Material material;
		final SoundType sound;
		final String tool;
		final float hardness;

		Ground(Material material, SoundType sound, String tool, float hardness)
		{
			this.material = material;
			this.sound = sound;
			this.tool = tool;
			this.hardness = hardness;
		}
	}

	public BlockINOre(String name, Ground ground)
	{
		super(ground.material);
		setHardness(ground.hardness);
		setResistance(5f);
		setSoundType(ground.sound);
		setHarvestLevel(ground.tool, 1);
		setRegistryName(ImmersiveNuclear.MODID, name);
		setTranslationKey(ImmersiveNuclear.MODID + "." + name);
		setCreativeTab(INContent.TAB);
	}
}
