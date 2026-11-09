package immersivenuclear.common.block;

import immersivenuclear.common.multiblock.EnrichmentUnit4Structure;
import immersivenuclear.common.multiblock.MultiblockEnrichmentUnit4;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** The Enrichment Unit with 4 banks of centrifuges: everything but its structure is in {@link TileEnrichmentBase}. */
public class TileEnrichmentUnit4 extends TileEnrichmentBase
{
	@Override
	public int banks()
	{
		return 4;
	}

	@Override
	char code(int dx, int dy, int dz)
	{
		return EnrichmentUnit4Structure.get(dx, dy, dz);
	}

	@Override
	int portOf(int dx, int dy, int dz)
	{
		return EnrichmentUnit4Structure.portAt(dx, dy, dz);
	}

	@Override
	boolean isCollisionCell(int dx, int dy, int dz)
	{
		return EnrichmentUnit4Structure.isCollisionOnly(dx, dy, dz);
	}

	@Override
	int[] faceOf(int dx, int dy, int dz)
	{
		return EnrichmentUnit4Structure.portFace(dx, dy, dz);
	}

	@Override
	ItemStack stackOf(char code)
	{
		return MultiblockEnrichmentUnit4.stackFor(code);
	}

	@Override
	void placeCollision(World world, BlockPos trigger, EnumFacing facing, boolean mirrored)
	{
		MultiblockEnrichmentUnit4.placeCollisionParts(world, trigger, facing, mirrored);
	}

	@Override
	public String boundsName()
	{
		return "enrichment_unit_4";
	}
}
