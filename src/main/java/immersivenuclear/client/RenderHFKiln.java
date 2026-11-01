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
import immersivenuclear.common.block.TileHFKiln;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
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
 * The turning parts of the Hydrofluoric Acid Kiln (drawn by the master only), while it runs, spinning up and down
 * smoothly: the drum (its shell, riding rings and girth gear) round its axis; the pinion and the motor's gear,
 * meshed (twice the drum's speed: 16 teeth on the girth gear, 8 on each); the four support rollers under the riding
 * rings (turned by the rings, the other way round). They come from models/block/obj/hf_kiln_anim (written by bb2obj.py,
 * modelled at rest); the rest is the static blockstate model. Axes are in the master's model space (reference
 * orientation, before the blockstate turn): Blockbench (X, Y, Z) -> blocks ((Z - 7) / 2, (Y - 2) / 2, (9 - X) / 2),
 * from the axes forno_hf_detalhes.py prints. Every axis runs along the kiln (north-south), level (the drum is level,
 * as in the user's model).
 */
public class RenderHFKiln extends TileEntitySpecialRenderer<TileHFKiln>
{
	static final String ANIM = "block/obj/hf_kiln_anim";
	static final String[] PARTS = {"drum", "pinion", "motorgear", "roller0", "roller1", "roller2", "roller3"};
	/** Per part: axis point x, y (blocks; z of the point is on the kiln's middle), direction's y per block along -z, speed (x the drum's). */
	static final float[][] AXES = {
			{-0.5F, 0.75F, 0, 1F},                          // drum
			{0.6042F, 0.1125F, 0, -2F},                     // pinion: 8 teeth on the girth gear's 16
			{1.3F, 0.45F, 0, 2F},                           // motor gear: meshed with the pinion
			{-1.3331F, 0.2435F, 0, -1.6F / 0.35F},          // rollers: the ring's radius over the roller's
			{0.3331F, 0.2435F, 0, -1.6F / 0.35F},
			{-1.3331F, 0.2435F, 0, -1.6F / 0.35F},
			{0.3331F, 0.2435F, 0, -1.6F / 0.35F},
	};
	/** The axes' common point along the kiln (z of the drum's middle, Blockbench X 8). */
	static final float AXIS_Z = 0.5F;
	static IBakedModel[] parts = new IBakedModel[PARTS.length], partsMirrored = new IBakedModel[PARTS.length];
	/** The heating jacket and the heaters (group "heater"): out, lit, and the emissive layer (only the glowing pixels). */
	static IBakedModel glowOff, glowOffMirrored, glowOn, glowOnMirrored, glowEmissive, glowEmissiveMirrored;
	static final ImmutableMap<String, String> EMISSIVE = ImmutableMap.of("#furnace_heater", "immersivenuclear:blocks/furnace_heater_active_emissive",
			"#metal_decoration0_coil_lv_side", "immersivenuclear:blocks/kiln_coil_glow_emissive");
	static final String TRANSPARENT = "immersivenuclear:blocks/transparent";
	static final ImmutableMap<String, String> LIT = ImmutableMap.of("#furnace_heater", "immersivenuclear:blocks/furnace_heater_active",
			"#metal_decoration0_coil_lv_side", "immersivenuclear:blocks/kiln_coil_glow");

	/** Called from ModelBakeEvent: bakes each turning part on its own. */
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
			ImmutableMap.Builder<String, String> textures = ImmutableMap.builder();
			for(Map.Entry<String, JsonElement> e : side.getAsJsonObject("textures").entrySet())
				textures.put(e.getKey(), e.getValue().getAsString());
			IModel normal = RenderFilterPress.load(ANIM + ".obj", textures.build()), mirrored = RenderFilterPress.load(ANIM + "_mirrored.obj", textures.build());
			for(int i = 0; i < PARTS.length; i++)
			{
				parts[i] = RenderFilterPress.bakeGroup(normal, PARTS[i]);
				partsMirrored[i] = RenderFilterPress.bakeGroup(mirrored, PARTS[i]);
			}
			glowOff = RenderFilterPress.bakeGroup(normal, "glow");
			glowOffMirrored = RenderFilterPress.bakeGroup(mirrored, "glow");
			java.util.Map<String, String> lit = new java.util.HashMap<>(textures.build());
			lit.putAll(LIT);
			glowOn = RenderFilterPress.bakeGroup(RenderFilterPress.load(ANIM + ".obj", ImmutableMap.copyOf(lit)), "glow");
			glowOnMirrored = RenderFilterPress.bakeGroup(RenderFilterPress.load(ANIM + "_mirrored.obj", ImmutableMap.copyOf(lit)), "glow");
			// the emissive layer: every other texture transparent, so only the glowing pixels are drawn
			java.util.Map<String, String> emissive = new java.util.HashMap<>();
			for(String key : textures.build().keySet())
				emissive.put(key, key.startsWith("#") ? TRANSPARENT : textures.build().get(key));
			emissive.putAll(EMISSIVE);
			glowEmissive = RenderFilterPress.bakeGroup(RenderFilterPress.load(ANIM + ".obj", ImmutableMap.copyOf(emissive)), "glow");
			glowEmissiveMirrored = RenderFilterPress.bakeGroup(RenderFilterPress.load(ANIM + "_mirrored.obj", ImmutableMap.copyOf(emissive)), "glow");
		}
		catch(Exception e)
		{
			ImmersiveNuclear.logger.error("Could not load the hydrofluoric acid kiln's turning parts", e);
			parts = new IBakedModel[PARTS.length];
			partsMirrored = new IBakedModel[PARTS.length];
		}
	}

	/** The panel's signal lamp (element "Clock"). */
	static final GlowLayer GLOW = new GlowLayer("hf_kiln", new String[]{"lamp"}, ImmutableMap.of("#arc_furnace_active", "immersivenuclear:blocks/arc_furnace_lamp_emissive"));

	@Override
	public void render(TileHFKiln te, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
	{
		if(!te.isMaster())
			return;
		boolean mirrored = te.isMirrored();
		GLOW.render(te.getWorld().getBlockState(te.getPos()), te.getFacing(), mirrored, x, y, z, te.heaterLit());
		float drum = te.drumAngle + te.drumSpeed * TileHFKiln.DRUM_DEGREES_PER_TICK * partialTicks;
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
			GlStateManager.rotate(drum * a[3], 0, a[2], dz);
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
		// The heating jacket and the heaters in the world's light (their lit textures while it runs); then, while it runs,
		// only their glowing pixels again, full bright, so they shine in the dark and the rest of the part does not
		boolean lit = te.heaterLit();
		IBakedModel glow = lit ? (mirrored ? glowOnMirrored : glowOn) : (mirrored ? glowOffMirrored : glowOff);
		GlStateManager.disableLighting();
		if(glow != null)
		{
			int light = te.getWorld().getCombinedLight(pos.up(), 0);
			net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit, light & 0xFFFF, light >> 16);
			Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer().renderModelBrightnessColor(state, glow, 1, 1, 1, 1);
		}
		IBakedModel shine = mirrored ? glowEmissiveMirrored : glowEmissive;
		if(lit && shine != null)
		{
			net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit, 240, 240);
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
	public boolean isGlobalRenderer(TileHFKiln te)
	{
		return te.isMaster();
	}
}
