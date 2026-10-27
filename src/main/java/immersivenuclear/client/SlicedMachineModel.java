package immersivenuclear.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;

import immersivenuclear.common.block.MachinePartBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.property.IExtendedBlockState;

/**
 * A formed machine's whole model (baked in the master block's space, already turned by the blockstate), cut into one
 * slice per block: a part gets the quads whose middle lies inside its own block, moved into its own space. Quads in a
 * block that is not part of the structure (a chimney above the roof) go to the master. A quad on a block's face counts
 * for the block behind it. Slices are cut once and kept.
 */
public class SlicedMachineModel implements IBakedModel
{
	final IBakedModel whole;
	final MachinePartBlock block;
	final EnumFacing facing;
	final boolean mirrored;
	final Map<Long, List<BakedQuad>> slices = new ConcurrentHashMap<>();

	public SlicedMachineModel(IBakedModel whole, MachinePartBlock block, EnumFacing facing, boolean mirrored)
	{
		this.whole = whole;
		this.block = block;
		this.facing = facing;
		this.mirrored = mirrored;
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand)
	{
		if(!(state instanceof IExtendedBlockState))
			return Collections.emptyList();
		BlockPos offset = ((IExtendedBlockState)state).getValue(MachinePartBlock.OFFSET);
		if(offset == null)
			return Collections.emptyList();
		long key = offset.toLong() * 7 + (side == null ? 6 : side.ordinal());
		return slices.computeIfAbsent(key, k -> slice(offset, side));
	}

	List<BakedQuad> slice(BlockPos offset, @Nullable EnumFacing side)
	{
		boolean master = offset.equals(BlockPos.ORIGIN);
		List<BakedQuad> out = new ArrayList<>();
		for(BakedQuad q : whole.getQuads(null, side, 0))
		{
			BlockPos cell = cell(q);
			if(cell.equals(offset) || master && !block.isPartAt(facing, mirrored, cell))
				out.add(master ? q : moved(q, -offset.getX(), -offset.getY(), -offset.getZ()));
		}
		return out;
	}

	static int positionOffset(VertexFormat format)
	{
		for(int i = 0; i < format.getElementCount(); i++)
			if(format.getElement(i).getUsage() == VertexFormatElement.EnumUsage.POSITION)
				return format.getOffset(i) / 4;
		return 0;
	}

	/** The block holding the quad's middle, nudged a hair behind the quad (a face on a block's side is that block's). */
	static BlockPos cell(BakedQuad q)
	{
		int[] data = q.getVertexData();
		VertexFormat format = q.getFormat();
		int stride = format.getIntegerSize(), pos = positionOffset(format);
		float x = 0, y = 0, z = 0;
		for(int v = 0; v < 4; v++)
		{
			x += Float.intBitsToFloat(data[v * stride + pos]);
			y += Float.intBitsToFloat(data[v * stride + pos + 1]);
			z += Float.intBitsToFloat(data[v * stride + pos + 2]);
		}
		EnumFacing f = q.getFace();
		float n = 0.01F;
		return new BlockPos(MathHelper.floor(x / 4 - f.getXOffset() * n), MathHelper.floor(y / 4 - f.getYOffset() * n), MathHelper.floor(z / 4 - f.getZOffset() * n));
	}

	static BakedQuad moved(BakedQuad q, int dx, int dy, int dz)
	{
		int[] data = q.getVertexData().clone();
		VertexFormat format = q.getFormat();
		int stride = format.getIntegerSize(), pos = positionOffset(format);
		for(int v = 0; v < 4; v++)
		{
			int i = v * stride + pos;
			data[i] = Float.floatToRawIntBits(Float.intBitsToFloat(data[i]) + dx);
			data[i + 1] = Float.floatToRawIntBits(Float.intBitsToFloat(data[i + 1]) + dy);
			data[i + 2] = Float.floatToRawIntBits(Float.intBitsToFloat(data[i + 2]) + dz);
		}
		return new BakedQuad(data, q.getTintIndex(), q.getFace(), q.getSprite(), q.shouldApplyDiffuseLighting(), format);
	}

	@Override
	public boolean isAmbientOcclusion()
	{
		return whole.isAmbientOcclusion();
	}

	@Override
	public boolean isGui3d()
	{
		return whole.isGui3d();
	}

	@Override
	public boolean isBuiltInRenderer()
	{
		return false;
	}

	@Override
	public TextureAtlasSprite getParticleTexture()
	{
		return whole.getParticleTexture();
	}

	@Override
	@SuppressWarnings("deprecation")
	public ItemCameraTransforms getItemCameraTransforms()
	{
		return whole.getItemCameraTransforms();
	}

	@Override
	public ItemOverrideList getOverrides()
	{
		return whole.getOverrides();
	}

	@Override
	public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType type)
	{
		return whole.handlePerspective(type);
	}
}
