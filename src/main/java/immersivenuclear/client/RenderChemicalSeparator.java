package immersivenuclear.client;

import org.lwjgl.opengl.GL11;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileChemicalSeparator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fluids.Fluid;

/**
 * The liquid inside the two settlers of the Chemical Separation Unit (drawn by the master only), filling each
 * settler's whole inside: a sheet just behind the front wall and the liquid's surface. It shows through the sight
 * glass, whose slit is see-through in the model, so it needs no window coordinates. Same layers as the interface
 * (see {@link SettlerLayers}): cloudy while it fills, clearing from the top and the bottom while it settles, two
 * layers while it drains.
 * Positions are the settlers' insides in the OBJ model (master's model space, before the blockstate turn).
 */
public class RenderChemicalSeparator extends TileEntitySpecialRenderer<TileChemicalSeparator>
{
	/** Inside of the settlers: just behind the front wall to the back wall (template x), floor to lid (y). */
	static final float FRONT_X = -1.69F, BACK_X = 0.95F, Y0 = -0.25F, Y1 = 2.25F;
	/** Settler A (left end, north) and settler B (right end, south), along the template z. */
	static final float[][] INSIDE_Z = {{-2.7F, -0.05F}, {1.05F, 3.7F}};
	/** Layers drawn over the base one sit a little further in front, so they never fight with it. */
	static final float STEP = 0.0008F;

	@Override
	public void render(TileChemicalSeparator te, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
	{
		if(!te.isMaster())
			return;
		SettlerLayers a = SettlerLayers.of(0, te.clientSettlerA, Y1 - Y0), b = SettlerLayers.of(1, te.clientSettlerB, Y1 - Y0);
		if(a == null && b == null)
			return;
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		// Same turn as the blockstate (y 0 / 90 / 180 / 270 for north / east / south / west), then the mirror
		GlStateManager.translate(0.5, 0, 0.5);
		GlStateManager.rotate(-RenderChemicalProcessor.turnDegrees(te.getFacing()), 0, 1, 0);
		GlStateManager.translate(-0.5, 0, -0.5);
		if(te.isMirrored())
		{
			GlStateManager.translate(0, 0, 1);
			GlStateManager.scale(1, 1, -1);
		}
		bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		GlStateManager.disableCull();
		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		int light = te.getWorld().getCombinedLight(te.getPos().west(2).up(), 0);
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 0xFFFF, light >> 16);
		if(a != null)
			fill(a, INSIDE_Z[0]);
		if(b != null)
			fill(b, INSIDE_Z[1]);
		GlStateManager.disableBlend();
		GlStateManager.enableLighting();
		GlStateManager.enableCull();
		GlStateManager.popMatrix();
	}

	/** The layers on the front sheet, and the surface of the top layer. */
	static void fill(SettlerLayers l, float[] zs)
	{
		float bottom = Y0 + l.clearBottom, top = Y0 + l.height;
		if(l.clearBottom > 0.001F)
			front(l.outBottom, FRONT_X, zs, Y0, bottom, 0xffffff, 1);
		if(l.mixed > 0.001F)
		{
			// Emulsion: the water phase with the solvent stirred through it
			front(l.inBottom, FRONT_X, zs, bottom, bottom + l.mixed, 0xffffff, 1);
			front(INContent.fluidSolvent, FRONT_X + STEP, zs, bottom, bottom + l.mixed, l.inTopTint & 0xffffff, 0.56F);
		}
		if(l.clearTop > 0.001F)
			front(INContent.fluidSolvent, FRONT_X, zs, top - l.clearTop, top, l.outTopTint & 0xffffff, 1);
		// Surface: what floats on top (the mixture while it is still cloudy)
		if(l.clearTop > 0.001F)
			surface(INContent.fluidSolvent, top, zs, l.outTopTint & 0xffffff);
		else if(l.mixed > 0.001F)
			surface(l.inBottom, top, zs, 0xffffff);
		else
			surface(l.outBottom, top, zs, 0xffffff);
	}

	/**
	 * A vertical sheet on the plane x, from zs[0] to zs[1] and y0 to y1, the texture repeated once per block both ways
	 * (the settlers are wider than a block: one quad stretched over them would read neighbouring atlas sprites).
	 */
	static void front(Fluid f, float x, float[] zs, float y0, float y1, int color, float alpha)
	{
		TextureAtlasSprite s = RenderChemicalProcessor.sprite(f.getStill().toString());
		float r = (color >> 16 & 255) / 255F, g = (color >> 8 & 255) / 255F, b = (color & 255) / 255F;
		BufferBuilder buf = Tessellator.getInstance().getBuffer();
		buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
		for(float za = zs[0]; za < zs[1] - 1e-4F; za = Math.min(zs[1], za + 1))
		{
			float zb = Math.min(zs[1], za + 1);
			float u0 = s.getInterpolatedU(0), u1 = s.getInterpolatedU((zb - za) * 16);
			for(float ya = y0; ya < y1 - 1e-4F; )
			{
				float base = (float)Math.floor(ya + 1e-4F);
				float yb = Math.min(y1, base + 1);
				float va = s.getInterpolatedV(16 - (ya - base) * 16), vb = s.getInterpolatedV(16 - (yb - base) * 16);
				buf.pos(x, ya, za).tex(u0, va).color(r, g, b, alpha).endVertex();
				buf.pos(x, ya, zb).tex(u1, va).color(r, g, b, alpha).endVertex();
				buf.pos(x, yb, zb).tex(u1, vb).color(r, g, b, alpha).endVertex();
				buf.pos(x, yb, za).tex(u0, vb).color(r, g, b, alpha).endVertex();
				ya = yb;
			}
		}
		Tessellator.getInstance().draw();
	}

	/** A horizontal sheet over the whole inside at height y, the texture repeated once per block. */
	static void surface(Fluid f, float y, float[] zs, int color)
	{
		TextureAtlasSprite s = RenderChemicalProcessor.sprite(f.getStill().toString());
		float r = (color >> 16 & 255) / 255F, g = (color >> 8 & 255) / 255F, b = (color & 255) / 255F;
		BufferBuilder buf = Tessellator.getInstance().getBuffer();
		buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
		for(float xa = FRONT_X; xa < BACK_X - 1e-4F; xa = Math.min(BACK_X, xa + 1))
		{
			float xb = Math.min(BACK_X, xa + 1);
			for(float za = zs[0]; za < zs[1] - 1e-4F; za = Math.min(zs[1], za + 1))
			{
				float zb = Math.min(zs[1], za + 1);
				float u0 = s.getInterpolatedU(0), u1 = s.getInterpolatedU((xb - xa) * 16), v0 = s.getInterpolatedV(0), v1 = s.getInterpolatedV((zb - za) * 16);
				buf.pos(xa, y, za).tex(u0, v0).color(r, g, b, 1F).endVertex();
				buf.pos(xa, y, zb).tex(u0, v1).color(r, g, b, 1F).endVertex();
				buf.pos(xb, y, zb).tex(u1, v1).color(r, g, b, 1F).endVertex();
				buf.pos(xb, y, za).tex(u1, v0).color(r, g, b, 1F).endVertex();
			}
		}
		Tessellator.getInstance().draw();
	}
}
