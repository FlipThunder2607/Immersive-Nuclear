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
import immersivenuclear.common.block.TileMetalExtractor;
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
import net.minecraftforge.fluids.Fluid;

/**
 * The living parts of the Metal Extractor (drawn by the master only), following the batch:
 * - the plates' platform (Blockbench group "Train") rises out of the vat to be scraped and goes back down, its six
 *   plates (group "Plates", one part each: plate0 .. plate5, left to right from the front) shown only when their slot
 *   holds an electrode plate, in its material's colour, taking the colour of the metal as it deposits and losing it
 *   under the scraper;
 * - the cleaner (group "Creaner": a comb between the plates, on a carriage on the left guide) rides up with the
 *   platform, goes down the plates scraping them and comes back up;
 * - the collector tray (group "Collector") slides in under the raised plates and back;
 * - the five HV wires, each from a connector on the power controller to the matching one on the platform, drawn like
 *   IE's own wires (IE's wire texture in the HV colour, a sixteenth of a block thick, hanging as a catenary with
 *   IE's formula). Each wire has a fixed length, a little longer than the gap with the platform raised, so it hangs
 *   looser while the platform is down (working);
 * - the effluent's surface in the vat, at its level.
 * The moving parts come from models/block/obj/metal_extractor_anim (one OBJ group per part, written by bb2obj.py),
 * modelled down (working) and tucked in; the rest is the static blockstate model. Positions are in the master's model
 * space (reference orientation, before the blockstate turn); the wire ends are printed by bb2obj.py.
 */
public class RenderMetalExtractor extends TileEntitySpecialRenderer<TileMetalExtractor>
{
	static final String ANIM = "block/obj/metal_extractor_anim";
	static IBakedModel train, trainMirrored, cleaner, cleanerMirrored, collector, collectorMirrored;
	static final IBakedModel[] plates = new IBakedModel[TileMetalExtractor.PLATES], platesMirrored = new IBakedModel[TileMetalExtractor.PLATES];

	/** The material colour of a plate tier (ItemElectrode.colour), or null for an empty slot. */
	static float[] tierColour(int tier)
	{
		switch(tier)
		{
			case 1: return INContent.itemElectrodeSteel.colour;
			case 2: return INContent.itemElectrodeLeadSilver.colour;
			case 3: return INContent.itemElectrodeElectrum.colour;
			default: return null;
		}
	}

	/** Controller tip {x, y, z}, then platform tip {x, y, z}, with the platform down (as the model is saved). */
	static final float[][] WIRES = {
			{2.4925F, 2.1650F, -0.2625F, 0.6275F, 1.4200F, -0.1275F},
			{2.4925F, 2.1650F, 0.1125F, 0.6275F, 1.4200F, 0.2475F},
			{2.4925F, 2.1650F, 0.4925F, 0.6275F, 1.4200F, 0.6275F},
			{2.4925F, 2.1650F, 0.8625F, 0.6275F, 1.4200F, 0.9975F},
			{2.4925F, 2.1650F, 1.2425F, 0.6275F, 1.4200F, 1.3775F},
	};
	/** How far the platform rises to have its plates scraped, and how far the tray slides in (towards the front, -x), in blocks. */
	static final float PLATFORM_LIFT = 1.375F, TRAY_TRAVEL = 1.5F;
	/** The cleaner's travel from the plates' top (as modelled) to their bottom: 2 Blockbench units. */
	static final float CLEANER_TRAVEL = 1.0F;
	/** Wire length over the gap with the platform raised. */
	static final float SLACK = 1.04F;
	/** IE's HV wire: colour (Config wireColouration) and render diameter. */
	static final int HV_COLOUR = 0x6f6f6f;
	static final float RADIUS = 0.0625F / 2;
	static final int SEGMENTS = 16;
	/** Inside of the vat: x and z from the front-left inner corner to the back-right one; floor and full level. */
	static final float VAT_X0 = -0.625F, VAT_X1 = 1.875F, VAT_Z0 = -0.625F, VAT_Z1 = 1.875F, VAT_FLOOR = -0.625F, VAT_FULL = 0.125F;
	/** The plates' colour (multiplier of the steel) with a full metal layer: a dull copper brown. */
	static final float[] METAL = {0.74F, 0.6F, 0.48F};

	/** Called from ModelBakeEvent: bakes each moving part on its own. */
	static void bake()
	{
		train = trainMirrored = cleaner = cleanerMirrored = collector = collectorMirrored = null;
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
			train = RenderFilterPress.bakeGroup(normal, "train");
			trainMirrored = RenderFilterPress.bakeGroup(mirrored, "train");
			for(int k = 0; k < TileMetalExtractor.PLATES; k++)
			{
				plates[k] = RenderFilterPress.bakeGroup(normal, "plate" + k);
				platesMirrored[k] = RenderFilterPress.bakeGroup(mirrored, "plate" + k);
			}
			cleaner = RenderFilterPress.bakeGroup(normal, "cleaner");
			cleanerMirrored = RenderFilterPress.bakeGroup(mirrored, "cleaner");
			collector = RenderFilterPress.bakeGroup(normal, "collector");
			collectorMirrored = RenderFilterPress.bakeGroup(mirrored, "collector");
		}
		catch(Exception e)
		{
			ImmersiveNuclear.logger.error("Could not load the metal extractor moving parts", e);
			train = null;
		}
	}

	@Override
	public void render(TileMetalExtractor te, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
	{
		if(!te.isMaster())
			return;
		float t = te.clientPhaseTicks + (te.clientRunning ? partialTicks : 0);
		float c = te.clientCollect < 0 ? -1 : te.clientCollect + (te.clientCollecting ? partialTicks : 0);
		float lift = TileMetalExtractor.lift(te.clientPhase, t, c), tray = TileMetalExtractor.tray(c);
		float coat = TileMetalExtractor.coat(te.clientPhase, t, c);
		// While the cleaner goes down the plates come clean from the top down: take the share still coated
		float scraper = TileMetalExtractor.scraper(c);
		if(scraper >= 0)
			coat *= 1 - scraper;
		boolean mirrored = te.isMirrored();

		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		// Same turn as the blockstate (y 0 / 90 / 180 / 270 for north / east / south / west); the mirrored OBJ is already mirrored
		GlStateManager.translate(0.5, 0, 0.5);
		GlStateManager.rotate(-RenderChemicalProcessor.turnDegrees(te.getFacing()), 0, 1, 0);
		GlStateManager.translate(-0.5, 0, -0.5);
		bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

		if(train != null)
		{
			BlockPos pos = te.getPos();
			IBlockState state = te.getWorld().getBlockState(pos).getActualState(te.getWorld(), pos);
			RenderHelper.disableStandardItemLighting();
			GlStateManager.shadeModel(Minecraft.isAmbientOcclusionEnabled() ? GL11.GL_SMOOTH : GL11.GL_FLAT);
			GlStateManager.enableCull();
			draw(te, mirrored ? trainMirrored : train, state, pos, 0, lift * PLATFORM_LIFT, null);
			for(int k = 0; k < TileMetalExtractor.PLATES; k++)
			{
				float[] m = tierColour(te.clientPlates[k]);
				if(m == null)
					continue;
				float[] tint = {m[0] * (1 + (METAL[0] - 1) * coat), m[1] * (1 + (METAL[1] - 1) * coat), m[2] * (1 + (METAL[2] - 1) * coat)};
				draw(te, mirrored ? platesMirrored[k] : plates[k], state, pos, 0, lift * PLATFORM_LIFT, tint);
			}
			draw(te, mirrored ? cleanerMirrored : cleaner, state, pos, 0, lift * PLATFORM_LIFT - TileMetalExtractor.cleaner(c) * CLEANER_TRAVEL, null);
			draw(te, mirrored ? collectorMirrored : collector, state, pos, -tray * TRAY_TRAVEL, 0, null);
			RenderHelper.enableStandardItemLighting();
		}

		// Wires and the liquid are drawn in the reference orientation, then mirrored
		if(mirrored)
		{
			GlStateManager.translate(0, 0, 1);
			GlStateManager.scale(1, 1, -1);
		}
		GlStateManager.disableCull();
		GlStateManager.disableLighting();
		int light = te.getWorld().getCombinedLight(te.getPos().up(2), 0);
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 0xFFFF, light >> 16);
		TextureAtlasSprite wireSprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("immersiveengineering:blocks/wire");
		BufferBuilder buf = Tessellator.getInstance().getBuffer();
		buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
		for(float[] w : WIRES)
		{
			float length = (float)Math.sqrt(sq(w[3] - w[0]) + sq(w[4] + PLATFORM_LIFT - w[1]) + sq(w[5] - w[2])) * SLACK;
			wire(buf, wireSprite, catenary(w[0], w[1], w[2], w[3], w[4] + lift * PLATFORM_LIFT, w[5], length));
		}
		Tessellator.getInstance().draw();
		if(te.clientVat > 0)
			surface(te.clientDepleted ? INContent.fluidDepletedEffluent : INContent.fluidEffluent,
					VAT_FLOOR + (VAT_FULL - VAT_FLOOR) * Math.min(1, te.clientVat / (float)TileMetalExtractor.BATCH));
		GlStateManager.enableLighting();
		GlStateManager.enableCull();
		GlStateManager.popMatrix();
	}

	/** One part, lit like a chunk at the master's position with the world's light, shifted; {@code tint} multiplies its colour. */
	void draw(TileMetalExtractor te, IBakedModel model, IBlockState state, BlockPos pos, float dx, float dy, float[] tint)
	{
		if(model == null)
			return;
		Tessellator tess = Tessellator.getInstance();
		BufferBuilder buffer = tess.getBuffer();
		GlStateManager.pushMatrix();
		GlStateManager.translate(dx, dy, 0);
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
		buffer.setTranslation(-pos.getX(), -pos.getY(), -pos.getZ());
		Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer().renderModel(te.getWorld(), model, state, pos, buffer, false);
		if(tint != null)
			for(int v = 1; v <= buffer.getVertexCount(); v++)
				buffer.putColorMultiplier(tint[0], tint[1], tint[2], v);
		buffer.setTranslation(0, 0, 0);
		tess.draw();
		GlStateManager.popMatrix();
	}

	/** The liquid's surface over the vat's inside at height y, the texture repeated once per block. */
	static void surface(Fluid f, float y)
	{
		TextureAtlasSprite s = RenderChemicalProcessor.sprite(f.getStill().toString());
		BufferBuilder buf = Tessellator.getInstance().getBuffer();
		buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
		for(float xa = VAT_X0; xa < VAT_X1 - 1e-4F; xa = Math.min(VAT_X1, xa + 1))
		{
			float xb = Math.min(VAT_X1, xa + 1);
			for(float za = VAT_Z0; za < VAT_Z1 - 1e-4F; za = Math.min(VAT_Z1, za + 1))
			{
				float zb = Math.min(VAT_Z1, za + 1);
				float u0 = s.getInterpolatedU(0), u1 = s.getInterpolatedU((xb - xa) * 16), v0 = s.getInterpolatedV(0), v1 = s.getInterpolatedV((zb - za) * 16);
				buf.pos(xa, y, za).tex(u0, v0).color(1F, 1F, 1F, 1F).endVertex();
				buf.pos(xa, y, zb).tex(u0, v1).color(1F, 1F, 1F, 1F).endVertex();
				buf.pos(xb, y, zb).tex(u1, v1).color(1F, 1F, 1F, 1F).endVertex();
				buf.pos(xb, y, za).tex(u1, v0).color(1F, 1F, 1F, 1F).endVertex();
			}
		}
		Tessellator.getInstance().draw();
	}

	static float sq(float v)
	{
		return v * v;
	}

	/**
	 * SEGMENTS + 1 points of a wire of the given length hanging from a to b, IE's way (ApiUtils.getConnectionCatenary);
	 * straight when it is not longer than the gap.
	 */
	static float[][] catenary(float ax, float ay, float az, float bx, float by, float bz, float length)
	{
		float dx = bx - ax, dy = by - ay, dz = bz - az;
		double dw = Math.sqrt(dx * dx + dz * dz);
		float[][] p = new float[SEGMENTS + 1][];
		boolean straight = dw < 1e-4 || length <= Math.sqrt(dw * dw + dy * dy) + 1e-4;
		double l = 0, a = 0, cp = 0, cq = 0;
		if(!straight)
		{
			double k = length;
			for(int i = 0; i < 1000; i++)
			{
				l += 0.01;
				if(Math.sinh(l) / l >= Math.sqrt(k * k - dy * dy) / dw)
					break;
			}
			a = dw / 2 / l;
			cp = (dw - a * Math.log((k + dy) / (k - dy))) * 0.5;
			cq = (dy - k * Math.cosh(l) / Math.sinh(l)) * 0.5;
		}
		for(int i = 0; i <= SEGMENTS; i++)
		{
			float f = i / (float)SEGMENTS;
			float y = straight ? dy * f : (float)(a * Math.cosh((dw * f - cp) / a) + cq);
			p[i] = new float[]{ax + dx * f, ay + y, az + dz * f};
		}
		return p;
	}

	/** Two crossed ribbons along the points (one flat, one upright), the texture's stripes across the wire. */
	static void wire(BufferBuilder buf, TextureAtlasSprite s, float[][] p)
	{
		float r = (HV_COLOUR >> 16 & 255) / 255F, g = (HV_COLOUR >> 8 & 255) / 255F, b = (HV_COLOUR & 255) / 255F;
		float v0 = s.getMinV(), v1 = s.getMaxV();
		for(int i = 0; i < SEGMENTS; i++)
		{
			float[] p0 = p[i], p1 = p[i + 1];
			float hx = p1[2] - p0[2], hz = -(p1[0] - p0[0]);
			float hl = (float)Math.sqrt(hx * hx + hz * hz);
			if(hl < 1e-6F)
				hl = 1;
			hx = hx / hl * RADIUS;
			hz = hz / hl * RADIUS;
			// About one stripe per pixel of wire, like IE's
			float seg = (float)Math.sqrt(sq(p1[0] - p0[0]) + sq(p1[1] - p0[1]) + sq(p1[2] - p0[2]));
			float u0 = s.getMinU(), u1 = s.getInterpolatedU(Math.min(16, seg * 16));
			for(float[] o : new float[][]{{hx, 0, hz}, {0, RADIUS, 0}})
			{
				buf.pos(p0[0] - o[0], p0[1] - o[1], p0[2] - o[2]).tex(u0, v0).color(r, g, b, 1F).endVertex();
				buf.pos(p1[0] - o[0], p1[1] - o[1], p1[2] - o[2]).tex(u1, v0).color(r, g, b, 1F).endVertex();
				buf.pos(p1[0] + o[0], p1[1] + o[1], p1[2] + o[2]).tex(u1, v1).color(r, g, b, 1F).endVertex();
				buf.pos(p0[0] + o[0], p0[1] + o[1], p0[2] + o[2]).tex(u0, v1).color(r, g, b, 1F).endVertex();
			}
		}
	}

	/** Drawn whenever the machine's render box is on screen, not only while the master's chunk section is. */
	@Override
	public boolean isGlobalRenderer(TileMetalExtractor te)
	{
		return te.isMaster();
	}
}
