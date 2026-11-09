package immersivenuclear.common.block;

import immersivenuclear.common.multiblock.EnrichmentUnit6Structure;
import immersivenuclear.common.multiblock.MultiblockEnrichmentUnit6;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** The Enrichment Unit with 6 banks of centrifuges: everything but its structure is in {@link TileEnrichmentBase}. */
public class TileEnrichmentUnit6 extends TileEnrichmentBase
{
	@Override
	public int banks()
	{
		return 6;
	}

	@Override
	char code(int dx, int dy, int dz)
	{
		return EnrichmentUnit6Structure.get(dx, dy, dz);
	}

	@Override
	int portOf(int dx, int dy, int dz)
	{
		return EnrichmentUnit6Structure.portAt(dx, dy, dz);
	}

	@Override
	boolean isCollisionCell(int dx, int dy, int dz)
	{
		return EnrichmentUnit6Structure.isCollisionOnly(dx, dy, dz);
	}

	@Override
	int[] faceOf(int dx, int dy, int dz)
	{
		return EnrichmentUnit6Structure.portFace(dx, dy, dz);
	}

	@Override
	ItemStack stackOf(char code)
	{
		return MultiblockEnrichmentUnit6.stackFor(code);
	}

	@Override
	void placeCollision(World world, BlockPos trigger, EnumFacing facing, boolean mirrored)
	{
		MultiblockEnrichmentUnit6.placeCollisionParts(world, trigger, facing, mirrored);
	}

	@Override
	public String boundsName()
	{
		return "enrichment_unit_6";
	}
}
