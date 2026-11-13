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
import immersivenuclear.common.block.TileDeconversionUnit;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.IModel;

/**
 * The moving and glowing parts of the Deconversion Unit (drawn by the master only), while it runs, spinning up and down
 * smoothly: the rotary kiln (its shell, riding rings and girth gear) round its axis; the pinion, meshed with the girth
 * gear (32 teeth on it, 8 on the pinion: four times the kiln's speed, the other way); the four support rollers under the
 * riding rings (turned by them, the other way). The heaters (the electric furnace's slots and the hydrolysis reactor's
 * bands) are cold and dark while it stands, hot while it runs, their glowing pixels full bright. The panel's LEDs: red
 * while it has power but does not run, green while it runs; the manometers glow faintly white while it has power.
 * They come from models/block/obj/deconversion_unit_anim (bb2obj.py, modelled at rest); the rest is the static model.
 * Axes are in the master's model space (reference orientation, before the blockstate turn): Blockbench (X, Y, Z) ->
 * blocks ((Z - 6) / 2, (Y - 2) / 2, (9 - X) / 2). Every axis runs along the kiln (Blockbench X), level.
 */
public class RenderDeconversionUnit extends TileEntitySpecialRenderer<TileDeconversionUnit>
{
	static final String ANIM = "block/obj/deconversion_unit_anim";
	static final String[] PARTS = {"drum", "pinion", "roller0", "roller1", "roller2", "roller3"};
	/** Per part: axis point x, y (blocks), speed (x the kiln's). */
	static final float[][] AXES = {
			{0.25F, 1.5F, 1F},                  // the kiln: Blockbench Y 5, Z 6.5
			{1.05F, 0.7975F, -4F},              // the pinion: Y 3.595, Z 8.1
			{-0.225F, 0.6315F, -1.77F / 0.38F}, // the rollers: Y 3.263, Z 5.55 / 7.45 (the rings' radius over the rollers')
			{0.725F, 0.6315F, -1.77F / 0.38F},
			{-0.225F, 0.6315F, -1.77F / 0.38F},
			{0.725F, 0.6315F, -1.77F / 0.38F},
	};
	static final float AXIS_Z = 0.5F;
	static IBakedModel[] parts = new IBakedModel[PARTS.length], partsMirrored = new IBakedModel[PARTS.length];
	/** The heaters (group "glow"): cold, hot, and the emissive layer (only the glowing pixels). */
	static IBakedModel heatOff, heatOffMirrored, heatOn, heatOnMirrored, heatShine, heatShineMirrored;
	static final ImmutableMap<String, String> COLD = ImmutableMap.of("#kiln_coil_glow", "immersivenuclear:blocks/kiln_coil_off");
	static final ImmutableMap<String, String> EMISSIVE = ImmutableMap.of("#kiln_coil_glow", "immersivenuclear:blocks/kiln_coil_glow_emissive");
	/** The panel's two LEDs (red: powered but not running, waiting or stopped; green: running) and the manometers' faint
	 * white backlight behind their glass (while it has power). */
	static final GlowLayer GLOW = new GlowLayer("deconversion_unit", new String[]{"led_red", "led_green", "gauges"},
			ImmutableMap.of("#red_led", "immersivenuclear:blocks/red_led_emissive", "#metal_multiblock_refinery", "immersivenuclear:blocks/enrichment_led_green_emissive",
					"#metal_multiblock_mixer", "immersivenuclear:blocks/mixer_gauge_glass_emissive"));

	/** Called from ModelBakeEvent: bakes each moving part on its own, and the heaters three ways. */
	static void bake()
	{
		try
		{
			ResourceLocation json = new ResourceLocation(ImmersiveNuclear.MODID, "models/" + ANIM + ".json");
			JsonObject side;
			try(Reader r = new InputStreamReader(Minecraft.getMinecraft().getResourceManager().getResource(json).getInputStream(), StandardCharsets.UTF_8))
			{
				side = new JsonParser().parse(r).getAsJsonObject();
			}
			ImmutableMap.Builder<String, String> b = ImmutableMap.builder();
			for(Map.Entry<String, JsonElement> e : side.getAsJsonObject("textures").entrySet())
				b.put(e.getKey(), e.getValue().getAsString());
			ImmutableMap<String, String> textures = b.build();
			IModel normal = RenderFilterPress.load(ANIM + ".obj", textures), mirrored = RenderFilterPress.load(ANIM + "_mirrored.obj", textures);
			for(int i = 0; i < PARTS.length; i++)
			{
				parts[i] = RenderFilterPress.bakeGroup(normal, PARTS[i]);
				partsMirrored[i] = RenderFilterPress.bakeGroup(mirrored, PARTS[i]);
			}
			heatOn = RenderFilterPress.bakeGroup(normal, "glow");
			heatOnMirrored = RenderFilterPress.bakeGroup(mirrored, "glow");
			java.util.Map<String, String> cold = new java.util.HashMap<>(textures);
			cold.putAll(COLD);
			heatOff = RenderFilterPress.bakeGroup(RenderFilterPress.load(ANIM + ".obj", ImmutableMap.copyOf(cold)), "glow");
			heatOffMirrored = RenderFilterPress.bakeGroup(RenderFilterPress.load(ANIM + "_mirrored.obj", ImmutableMap.copyOf(cold)), "glow");
			// the emissive layer: every other texture transparent, so only the glowing pixels are drawn
			java.util.Map<String, String> shine = new java.util.HashMap<>();
			for(String key : textures.keySet())
				shine.put(key, key.startsWith("#") ? GlowLayer.TRANSPARENT : textures.get(key));
			shine.putAll(EMISSIVE);
			heatShine = RenderFilterPress.bakeGroup(RenderFilterPress.load(ANIM + ".obj", ImmutableMap.copyOf(shine)), "glow");
			heatShineMirrored = RenderFilterPress.bakeGroup(RenderFilterPress.load(ANIM + "_mirrored.obj", ImmutableMap.copyOf(shine)), "glow");
		}
		catch(Exception e)
		{
			ImmersiveNuclear.logger.error("Could not load the deconversion unit's moving parts", e);
			parts = new IBakedModel[PARTS.length];
			partsMirrored = new IBakedModel[PARTS.length];
		}
	}

	@Override
	public void render(TileDeconversionUnit te, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
	{
		if(!te.isMaster())
			return;
		boolean mirrored = te.isMirrored(), lit = te.running();
		boolean powered = te.powered();
		GLOW.render(te.getWorld().getBlockState(te.getPos()), te.getFacing(), mirrored, x, y, z, powered && !lit, lit, powered || lit);
		float drum = te.drumAngle + te.drumSpeed * TileDeconversionUnit.DRUM_DEGREES_PER_TICK * partialTicks;
		BlockPos pos = te.getPos();
		IBlockState state = te.getWorld().getBlockState(pos).getActualState(te.getWorld(), pos);

		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		// Same turn as the blockstate (y 0 / 90 / 180 / 270 for north / east / south / west); the mirrored OBJ is already mirrored
		GlStateManager.translate(0.5, 0, 0.5);
		GlStateManager.rotate(-RenderChemicalProcessor.turnDegrees(te.getFacing()), 0, 1, 0);
		GlStateManager.translate(-0.5, 0, -0.5);
		bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		RenderHelper.disableStandardItemLighting();
		GlStateManager.shadeModel(Minecraft.isAmbientOcclusionEnabled() ? GL11.GL_SMOOTH : GL11.GL_FLAT);
		GlStateManager.enableCull();
		for(int i = 0; i < PARTS.length; i++)
		{
			IBakedModel m = mirrored ? partsMirrored[i] : parts[i];
			if(m == null)
				continue;
			float[] a = AXES[i];
			// mirrored, z becomes 1 - z like the mirrored OBJ: the axis runs the other way, so the turn is reversed
			float az = mirrored ? 1 - AXIS_Z : AXIS_Z, dz = mirrored ? 1 : -1;
			GlStateManager.pushMatrix();
			GlStateManager.translate(a[0], a[1], az);
			GlStateManager.rotate(drum * a[2], 0, 0, dz);
			GlStateManager.translate(-a[0], -a[1], -az);
			Tessellator tess = Tessellator.getInstance();
			BufferBuilder buffer = tess.getBuffer();
			buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
			buffer.setTranslation(-pos.getX(), -pos.getY(), -pos.getZ());
			Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer().renderModel(te.getWorld(), m, state, pos, buffer, false);
			buffer.setTranslation(0, 0, 0);
			tess.draw();
			GlStateManager.popMatrix();
		}
		// The heaters in the world's light (cold or hot); then, while it runs, only their glowing pixels again, full bright
		IBakedModel heat = lit ? (mirrored ? heatOnMirrored : heatOn) : (mirrored ? heatOffMirrored : heatOff);
		GlStateManager.disableLighting();
		if(heat != null)
		{
			int light = te.getWorld().getCombinedLight(pos.up(), 0);
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 0xFFFF, light >> 16);
			Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer().renderModelBrightnessColor(state, heat, 1, 1, 1, 1);
		}
		IBakedModel shine = mirrored ? heatShineMirrored : heatShine;
		if(lit && shine != null)
		{
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
			GlStateManager.enableBlend();
			GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			GlStateManager.enableAlpha();
			GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
			Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer().renderModelBrightnessColor(state, shine, 1, 1, 1, 1);
			GlStateManager.disableBlend();
		}
		RenderHelper.enableStandardItemLighting();
		GlStateManager.popMatrix();
	}

	@Override
	public boolean isGlobalRenderer(TileDeconversionUnit te)
	{
		return te.isMaster();
	}
}
