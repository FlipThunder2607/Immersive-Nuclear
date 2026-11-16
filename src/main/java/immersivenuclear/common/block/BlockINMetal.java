package immersivenuclear.common.block;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;

/** A metal block, as IE's storage and sheet metal blocks (provisional hardness, IE's values). */
public class BlockINMetal extends Block
{
	public BlockINMetal(String name)
	{
		super(Material.IRON);
		setHardness(5f);
		setResistance(10f);
		setSoundType(SoundType.METAL);
		setHarvestLevel("pickaxe", 1);
		setRegistryName(ImmersiveNuclear.MODID, name);
		setTranslationKey(ImmersiveNuclear.MODID + "." + name);
		setCreativeTab(INContent.TAB);
	}
}
