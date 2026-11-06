package immersivenuclear.common.block;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;

/** The data cable coil block: a building block of the computers' multiblocks, the data cable's counterpart of IE's wire coil blocks. */
public class BlockDataCoil extends Block
{
	public BlockDataCoil()
	{
		// Same as IE's metal decoration
		super(Material.IRON);
		setHardness(3f);
		setResistance(15f);
		setSoundType(SoundType.METAL);
		setHarvestLevel("pickaxe", 1);
		setRegistryName(ImmersiveNuclear.MODID, "data_coil");
		setTranslationKey(ImmersiveNuclear.MODID + ".data_coil");
		setCreativeTab(INContent.TAB);
	}
}
