package immersivenuclear.client;

import org.lwjgl.opengl.GL11;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileChemicalProcessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

/**
 * The reactor's liquid seen through the two tank windows of the Chemical Processing Unit (drawn by the master only).
 * The batch's water rises while it fills (a stream falls from the top of the window to the surface), turns into
 * slurry while the agitator turns, and goes down as it drains. Positions are the windows of the OBJ model, in the
 * master's model space before the blockstate turn.
 */
public class RenderChemicalProcessor extends TileEntitySpecialRenderer<TileChemicalProcessor>
{
	// Window opening, height in blocks from the master's floor
	static final float Y0 = -0.875F, Y1 = 2.375F;
	// Front window (west face in the reference orientation): just behind the glass
	static final float FRONT_X = -0.515F, FRONT_Z0 = 0F, FRONT_Z1 = 0.375F;
	// Left window (north face): just behind the glass
	static final float SIDE_Z = -0.765F, SIDE_X0 = 0.75F, SIDE_X1 = 1.125F;
	// The stream, on the side of the front window nearest the water inlet
	static final float STREAM_Z0 = 0.28F, STREAM_Z1 = 0.34F, STREAM_X = -0.512F;
	static final int WATER_COLOR = 0x3F76E4;
	static final float WATER_ALPHA = 0.75F;

	@Override
	public void render(TileChemicalProcessor te, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
	{
		if(!te.isMaster())
			return;
		// Ease towards the synced level so it rises and falls smoothly
		te.shownLevel += (te.clientReactor / (float)TileChemicalProcessor.BATCH_WATER - te.shownLevel) * 0.1F;
		float level = Math.min(1, te.shownLevel);
		if(level < 0.002F && !te.clientFilling)
			return;
		float mix = te.clientMix / (float)TileChemicalProcessor.MIX_STEPS;
		float waterTop = Y0 + (Y1 - Y0) * level;

		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		// Same turn as the blockstate (y 0 / 90 / 180 / 270 for north / east / south / west), then the mirror
		GlStateManager.translate(0.5, 0, 0.5);
		GlStateManager.rotate(-turnDegrees(te.getFacing()), 0, 1, 0);
		GlStateManager.translate(-0.5, 0, -0.5);
		if(te.isMirrored())
		{
			// Left and right swapped (template z), like the mirrored OBJ
			GlStateManager.translate(0, 0, 1);
			GlStateManager.scale(1, 1, -1);
		}
		bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		GlStateManager.disableCull();
		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		int light = te.getWorld().getCombinedLight(te.getPos().up(4), 0);
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 0xFFFF, light >> 16);

		Fluid slurryFluid = INContent.fluidSlurry;
		if(level > 0.002F)
		{
			// The water, then the slurry over it as the reaction goes on
			if(mix < 1)
				windows(sprite(FluidRegistry.WATER.getStill().toString()), Y0, waterTop, WATER_COLOR, WATER_ALPHA);
			if(mix > 0)
				windows(sprite(slurryFluid.getStill().toString()), Y0, waterTop, 0xFFFFFF, mix);
		}
		if(te.clientFilling && waterTop < Y1)
		{
			TextureAtlasSprite s = sprite(FluidRegistry.WATER.getFlowing().toString());
			quadX(s, STREAM_X, STREAM_Z0, STREAM_Z1, waterTop, Y1, WATER_COLOR, 0.9F);
		}

		GlStateManager.disableBlend();
		GlStateManager.enableLighting();
		GlStateManager.enableCull();
		GlStateManager.popMatrix();
	}

	static float turnDegrees(EnumFacing facing)
	{
		switch(facing)
		{
			case EAST: return 90;
			case SOUTH: return 180;
			case WEST: return 270;
			default: return 0;
		}
	}

	static TextureAtlasSprite sprite(String name)
	{
		return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(name);
	}

	static void windows(TextureAtlasSprite s, float y0, float y1, int color, float alpha)
	{
		quadX(s, FRONT_X, FRONT_Z0, FRONT_Z1, y0, y1, color, alpha);
		quadZ(s, SIDE_Z, SIDE_X0, SIDE_X1, y0, y1, color, alpha);
	}

	/** A vertical sheet on the plane x = const, from z0 to z1 and y0 to y1; texture repeated once per block of height. */
	static void quadX(TextureAtlasSprite s, float x, float z0, float z1, float y0, float y1, int color, float alpha)
	{
		sheet(s, true, x, z0, z1, y0, y1, color, alpha);
	}

	static void quadZ(TextureAtlasSprite s, float z, float x0, float x1, float y0, float y1, int color, float alpha)
	{
		sheet(s, false, z, x0, x1, y0, y1, color, alpha);
	}

	static void sheet(TextureAtlasSprite s, boolean onX, float plane, float a0, float a1, float y0, float y1, int color, float alpha)
	{
		float r = (color >> 16 & 255) / 255F, g = (color >> 8 & 255) / 255F, b = (color & 255) / 255F;
		float u0 = s.getInterpolatedU(0), u1 = s.getInterpolatedU((a1 - a0) * 16);
		BufferBuilder buf = Tessellator.getInstance().getBuffer();
		buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
		for(float ya = y0; ya < y1 - 1e-4F; )
		{
			float yb = Math.min(y1, (float)Math.floor(ya + 1e-4F) + 1);
			float va = s.getInterpolatedV(16 - (ya - (float)Math.floor(ya + 1e-4F)) * 16);
			float vb = s.getInterpolatedV(16 - (yb - (float)Math.floor(ya + 1e-4F)) * 16);
			if(onX)
			{
				buf.pos(plane, ya, a0).tex(u0, va).color(r, g, b, alpha).endVertex();
				buf.pos(plane, ya, a1).tex(u1, va).color(r, g, b, alpha).endVertex();
				buf.pos(plane, yb, a1).tex(u1, vb).color(r, g, b, alpha).endVertex();
				buf.pos(plane, yb, a0).tex(u0, vb).color(r, g, b, alpha).endVertex();
			}
			else
			{
				buf.pos(a0, ya, plane).tex(u0, va).color(r, g, b, alpha).endVertex();
				buf.pos(a1, ya, plane).tex(u1, va).color(r, g, b, alpha).endVertex();
				buf.pos(a1, yb, plane).tex(u1, vb).color(r, g, b, alpha).endVertex();
				buf.pos(a0, yb, plane).tex(u0, vb).color(r, g, b, alpha).endVertex();
			}
			ya = yb;
		}
		Tessellator.getInstance().draw();
	}
}
