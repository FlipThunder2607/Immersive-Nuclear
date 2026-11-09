package immersivenuclear.common.block;

import immersivenuclear.common.multiblock.EnrichmentUnitStructure;
import immersivenuclear.common.multiblock.MultiblockEnrichmentUnit;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** The Enrichment Unit with 2 banks of centrifuges: everything but its structure is in {@link TileEnrichmentBase}. */
public class TileEnrichmentUnit extends TileEnrichmentBase
{
	@Override
	public int banks()
	{
		return 2;
	}

	@Override
	char code(int dx, int dy, int dz)
	{
		return EnrichmentUnitStructure.get(dx, dy, dz);
	}

	@Override
	int portOf(int dx, int dy, int dz)
	{
		return EnrichmentUnitStructure.portAt(dx, dy, dz);
	}

	@Override
	boolean isCollisionCell(int dx, int dy, int dz)
	{
		return EnrichmentUnitStructure.isCollisionOnly(dx, dy, dz);
	}

	@Override
	int[] faceOf(int dx, int dy, int dz)
	{
		return EnrichmentUnitStructure.portFace(dx, dy, dz);
	}

	@Override
	ItemStack stackOf(char code)
	{
		return MultiblockEnrichmentUnit.stackFor(code);
	}

	@Override
	void placeCollision(World world, BlockPos trigger, EnumFacing facing, boolean mirrored)
	{
		MultiblockEnrichmentUnit.placeCollisionParts(world, trigger, facing, mirrored);
	}

	@Override
	public String boundsName()
	{
		return "enrichment_unit";
	}
}
