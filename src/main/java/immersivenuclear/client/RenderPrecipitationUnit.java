package immersivenuclear.client;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TilePrecipitationUnit;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.IModel;

/**
 * The living parts of the Precipitation Unit (drawn by the master only), seen through the agitated tank's window:
 * - the agitator (Blockbench group "rotor": the shaft and three levels of four blades) turns around its shaft while the
 *   batch precipitates, spinning up and down smoothly;
 * - the batch in the tank: the purified solution rising while it fills, clouding yellow as the uranium precipitates,
 *   going down while the screw takes it. A little see-through, so the agitator shows in it.
 * The agitator comes from models/block/obj/precipitation_unit_anim (written by bb2obj.py, modelled at rest); the rest is
 * the static blockstate model. Positions are in the master's model space (reference orientation, before the
 * blockstate turn), read from the generated OBJ.
 */
public class RenderPrecipitationUnit extends TileEntitySpecialRenderer<TilePrecipitationUnit>
{
	static final String ANIM = "block/obj/precipitation_unit_anim";
	static IBakedModel rotor, rotorMirrored;
	/** The shaft's axis (x, z); mirrored, z becomes 1 - z like the mirrored OBJ. */
	static final float AXIS_X = 0.3775F, AXIS_Z = 1.0075F;
	/** Inside of the tank, just inside its walls: x from the window's wall to the back one, z, floor and full level (under the lid). */
	static final float TANK_X0 = -0.89F, TANK_X1 = 1.89F, TANK_Z0 = -0.39F, TANK_Z1 = 2.39F, TANK_FLOOR = 0.25F, TANK_FULL = 1.70F;
	/** The solid's yellow, mixed into the solution's colour as it precipitates; how see-through the liquid is. */
	static final float[] SOLID = {0.88F, 0.75F, 0.25F};
	static final float ALPHA_CLEAR = 0.72F, ALPHA_CLOUDY = 0.9F;

	/** Called from ModelBakeEvent: bakes the agitator on its own. */
	static void bake()
	{
		rotor = rotorMirrored = null;
		try
		{
			ResourceLocation json = new ResourceLocation(ImmersiveNuclear.MODID, "models/" + ANIM + ".json");
			JsonObject side;
			try(Reader r = new InputStreamReader(Minecraft.getMinecraft().getResourceManager().getResource(json).getInputStream(), StandardCharsets.UTF_8))
			{
				side = new JsonParser().parse(r).getAsJsonObject();
			}
			ImmutableMap.Builder<String, String> textures = ImmutableMap.builder();
			for(Map.Entry<String, JsonElement> e : side.getAsJsonObject("textures").entrySet())
				textures.put(e.getKey(), e.getValue().getAsString());
			IModel normal = RenderFilterPress.load(ANIM + ".obj", textures.build()), mirrored = RenderFilterPress.load(ANIM + "_mirrored.obj", textures.build());
			rotor = RenderFilterPress.bakeGroup(normal, "rotor");
			rotorMirrored = RenderFilterPress.bakeGroup(mirrored, "rotor");
		}
		catch(Exception e)
		{
			ImmersiveNuclear.logger.error("Could not load the precipitation unit's agitator", e);
			rotor = null;
		}
	}

	@Override
	public void render(TilePrecipitationUnit te, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
	{
		if(!te.isMaster())
			return;
		boolean mirrored = te.isMirrored();
		float angle = te.rotorAngle + te.rotorSpeed * TilePrecipitationUnit.ROTOR_DEGREES_PER_TICK * partialTicks;
		// Ease towards the synced level so it rises and falls smoothly
		te.shownLevel += (te.clientMixer / (float)TilePrecipitationUnit.BATCH_SOLUTION - te.shownLevel) * 0.1F;
		float level = Math.min(1, te.shownLevel);

		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		// Same turn as the blockstate (y 0 / 90 / 180 / 270 for north / east / south / west); the mirrored OBJ is already mirrored
		GlStateManager.translate(0.5, 0, 0.5);
		GlStateManager.rotate(-RenderChemicalProcessor.turnDegrees(te.getFacing()), 0, 1, 0);
		GlStateManager.translate(-0.5, 0, -0.5);
		bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

		if(rotor != null)
		{
			BlockPos pos = te.getPos();
			IBlockState state = te.getWorld().getBlockState(pos).getActualState(te.getWorld(), pos);
			float az = mirrored ? 1 - AXIS_Z : AXIS_Z;
			GlStateManager.pushMatrix();
			GlStateManager.translate(AXIS_X, 0, az);
			GlStateManager.rotate(mirrored ? -angle : angle, 0, 1, 0);
			GlStateManager.translate(-AXIS_X, 0, -az);
			RenderHelper.disableStandardItemLighting();
			GlStateManager.shadeModel(Minecraft.isAmbientOcclusionEnabled() ? GL11.GL_SMOOTH : GL11.GL_FLAT);
			GlStateManager.enableCull();
			Tessellator tess = Tessellator.getInstance();
			BufferBuilder buffer = tess.getBuffer();
			buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
			buffer.setTranslation(-pos.getX(), -pos.getY(), -pos.getZ());
			Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer().renderModel(te.getWorld(), mirrored ? rotorMirrored : rotor, state, pos, buffer, false);
			buffer.setTranslation(0, 0, 0);
			tess.draw();
			RenderHelper.enableStandardItemLighting();
			GlStateManager.popMatrix();
		}

		if(level > 0.002F)
		{
			// The liquid is drawn in the reference orientation, then mirrored
			if(mirrored)
			{
				GlStateManager.translate(0, 0, 1);
				GlStateManager.scale(1, 1, -1);
			}
			float p = TilePrecipitationUnit.precipitated(te.clientPhase, te.clientPhaseTicks + (te.clientRunning ? partialTicks : 0));
			float r = 1 + (SOLID[0] - 1) * p, g = 1 + (SOLID[1] - 1) * p, b = 1 + (SOLID[2] - 1) * p;
			float a = ALPHA_CLEAR + (ALPHA_CLOUDY - ALPHA_CLEAR) * p;
			GlStateManager.disableCull();
			GlStateManager.disableLighting();
			GlStateManager.enableBlend();
			GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			GlStateManager.depthMask(false);
			int light = te.getWorld().getCombinedLight(te.getPos().up(2), 0);
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 0xFFFF, light >> 16);
			TextureAtlasSprite s = RenderChemicalProcessor.sprite(INContent.fluidPurifiedSolution.getStill().toString());
			float top = TANK_FLOOR + (TANK_FULL - TANK_FLOOR) * level;
			// The window's wall first (behind the glass), then the others and the surface
			wall(s, true, TANK_X0, TANK_Z0, TANK_Z1, top, r, g, b, a);
			wall(s, true, TANK_X1, TANK_Z0, TANK_Z1, top, r, g, b, a);
			wall(s, false, TANK_Z0, TANK_X0, TANK_X1, top, r, g, b, a);
			wall(s, false, TANK_Z1, TANK_X0, TANK_X1, top, r, g, b, a);
			surface(s, top, r, g, b, a);
			GlStateManager.depthMask(true);
			GlStateManager.disableBlend();
			GlStateManager.enableLighting();
			GlStateManager.enableCull();
		}
		GlStateManager.popMatrix();
	}

	/**
	 * The liquid against one wall of the tank, from the floor up to y: on the plane x = plane ({@code onX}) or
	 * z = plane, from a0 to a1 along it. Tiled once per block both ways: a sprite stretched wider than a block would
	 * read the neighbouring sprites of the atlas.
	 */
	static void wall(TextureAtlasSprite s, boolean onX, float plane, float a0, float a1, float y, float r, float g, float b, float a)
	{
		BufferBuilder buf = Tessellator.getInstance().getBuffer();
		buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
		for(float ha = TANK_FLOOR; ha < y - 1e-4F; ha = Math.min(y, ha + 1))
		{
			float hb = Math.min(y, ha + 1);
			for(float wa = a0; wa < a1 - 1e-4F; wa = Math.min(a1, wa + 1))
			{
				float wb = Math.min(a1, wa + 1);
				float u0 = s.getInterpolatedU(0), u1 = s.getInterpolatedU((wb - wa) * 16);
				float v0 = s.getInterpolatedV(16), v1 = s.getInterpolatedV(16 - (hb - ha) * 16);
				if(onX)
				{
					buf.pos(plane, ha, wa).tex(u0, v0).color(r, g, b, a).endVertex();
					buf.pos(plane, ha, wb).tex(u1, v0).color(r, g, b, a).endVertex();
					buf.pos(plane, hb, wb).tex(u1, v1).color(r, g, b, a).endVertex();
					buf.pos(plane, hb, wa).tex(u0, v1).color(r, g, b, a).endVertex();
				}
				else
				{
					buf.pos(wa, ha, plane).tex(u0, v0).color(r, g, b, a).endVertex();
					buf.pos(wb, ha, plane).tex(u1, v0).color(r, g, b, a).endVertex();
					buf.pos(wb, hb, plane).tex(u1, v1).color(r, g, b, a).endVertex();
					buf.pos(wa, hb, plane).tex(u0, v1).color(r, g, b, a).endVertex();
				}
			}
		}
		Tessellator.getInstance().draw();
	}

	/** The liquid's surface over the tank's inside at height y, the texture repeated once per block. */
	static void surface(TextureAtlasSprite s, float y, float r, float g, float b, float a)
	{
		BufferBuilder buf = Tessellator.getInstance().getBuffer();
		buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
		for(float xa = TANK_X0; xa < TANK_X1 - 1e-4F; xa = Math.min(TANK_X1, xa + 1))
		{
			float xb = Math.min(TANK_X1, xa + 1);
			for(float za = TANK_Z0; za < TANK_Z1 - 1e-4F; za = Math.min(TANK_Z1, za + 1))
			{
				float zb = Math.min(TANK_Z1, za + 1);
				float u0 = s.getInterpolatedU(0), u1 = s.getInterpolatedU((xb - xa) * 16), v0 = s.getInterpolatedV(0), v1 = s.getInterpolatedV((zb - za) * 16);
				buf.pos(xa, y, za).tex(u0, v0).color(r, g, b, a).endVertex();
				buf.pos(xa, y, zb).tex(u0, v1).color(r, g, b, a).endVertex();
				buf.pos(xb, y, zb).tex(u1, v1).color(r, g, b, a).endVertex();
				buf.pos(xb, y, za).tex(u1, v0).color(r, g, b, a).endVertex();
			}
		}
		Tessellator.getInstance().draw();
	}

	/** Drawn whenever the machine's render box is on screen, not only while the master's chunk section is. */
	@Override
	public boolean isGlobalRenderer(TilePrecipitationUnit te)
	{
		return te.isMaster();
	}
}
