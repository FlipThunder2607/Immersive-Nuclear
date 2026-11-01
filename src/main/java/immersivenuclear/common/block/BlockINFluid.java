package immersivenuclear.common.block;

import immersivenuclear.ImmersiveNuclear;
import net.minecraft.block.material.Material;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 * The placed form of one of our fluids, so its bucket can pour it: it flows like water (a gas, the steam, rises). No
 * effect on whoever stands in it yet; the hazards come with the contamination system.
 */
public class BlockINFluid extends BlockFluidClassic
{
	public BlockINFluid(Fluid fluid)
	{
		super(fluid, Material.WATER);
		setRegistryName(ImmersiveNuclear.MODID, fluid.getName());
		setTranslationKey(ImmersiveNuclear.MODID + "." + fluid.getName());
		fluid.setBlock(this);
	}

	@Override
	public String getLocalizedName()
	{
		return getFluid().getLocalizedName(new FluidStack(getFluid(), 1000));
	}
}
