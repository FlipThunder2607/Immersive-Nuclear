package immersivenuclear.client;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import immersivenuclear.ImmersiveNuclear;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;

/**
 * The glowing bits of a machine, drawn again over its model, full bright, while they are lit: a burner's flame, the
 * heaters' hot elements, a panel's signal lamp. Each glowing part is a group of the machine's _anim OBJ (bb2obj.py's
 * "glow" rules: the faces stay in the static model too); it is baked with every texture made transparent but the
 * glowing ones (their emissive layer: only the pixels that glow), so the rest of the part keeps the world's light.
 */
public class GlowLayer
{
	public static final String TRANSPARENT = ImmersiveNuclear.MODID + ":blocks/transparent";
	final String anim;
	final String[] parts;
	/** Texture key -> its emissive layer; a key mapped to null keeps its own texture (it glows whole, as a flame). */
	final Map<String, String> emissive;
	IBakedModel[] normal, mirrored;

	public GlowLayer(String machine, String[] parts, Map<String, String> emissive)
	{
		this.anim = "block/obj/" + machine + "_anim";
		this.parts = parts;
		this.emissive = emissive;
	}

	/** Called from ModelBakeEvent. */
	void bake()
	{
		normal = new IBakedModel[parts.length];
		mirrored = new IBakedModel[parts.length];
		try
		{
			JsonObject side;
			try(Reader r = new InputStreamReader(Minecraft.getMinecraft().getResourceManager()
					.getResource(new ResourceLocation(ImmersiveNuclear.MODID, "models/" + anim + ".json")).getInputStream(), StandardCharsets.UTF_8))
			{
				side = new JsonParser().parse(r).getAsJsonObject();
			}
			Map<String, String> textures = new HashMap<>();
			for(Map.Entry<String, JsonElement> e : side.getAsJsonObject("textures").entrySet())
			{
				String key = e.getKey();
				if(!key.startsWith("#"))
					textures.put(key, e.getValue().getAsString());
				else if(emissive.containsKey(key))
					textures.put(key, emissive.get(key) != null ? emissive.get(key) : e.getValue().getAsString());
				else
					textures.put(key, TRANSPARENT);
			}
			IModel n = RenderFilterPress.load(anim + ".obj", ImmutableMap.copyOf(textures)), m = RenderFilterPress.load(anim + "_mirrored.obj", ImmutableMap.copyOf(textures));
			for(int i = 0; i < parts.length; i++)
			{
				normal[i] = RenderFilterPress.bakeGroup(n, parts[i]);
				mirrored[i] = RenderFilterPress.bakeGroup(m, parts[i]);
			}
		}
		catch(Exception e)
		{
			ImmersiveNuclear.logger.error("Could not load the glowing parts of " + anim, e);
		}
	}

	/**
	 * Draws the lit parts over the machine, at the master's render position (x, y, z), turned like the blockstate.
	 * lit[i] for parts[i].
	 */
	public void render(IBlockState state, EnumFacing facing, boolean isMirrored, double x, double y, double z, boolean... lit)
	{
		if(normal == null)
			return;
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		GlStateManager.translate(0.5, 0, 0.5);
		GlStateManager.rotate(-RenderChemicalProcessor.turnDegrees(facing), 0, 1, 0);
		GlStateManager.translate(-0.5, 0, -0.5);
		Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		RenderHelper.disableStandardItemLighting();
		GlStateManager.disableLighting();
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		GlStateManager.enableAlpha();
		GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
		// drawn on the static model's own faces: pulled a hair towards the eye so it wins the depth test
		GlStateManager.enablePolygonOffset();
		GlStateManager.doPolygonOffset(-1, -10);
		for(int i = 0; i < parts.length; i++)
		{
			IBakedModel model = isMirrored ? mirrored[i] : normal[i];
			if(model != null && i < lit.length && lit[i])
				Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer().renderModelBrightnessColor(state, model, 1, 1, 1, 1);
		}
		GlStateManager.disablePolygonOffset();
		GlStateManager.disableBlend();
		RenderHelper.enableStandardItemLighting();
		GlStateManager.popMatrix();
	}
}
