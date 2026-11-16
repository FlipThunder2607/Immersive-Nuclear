package immersivenuclear.client;

import java.util.IdentityHashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.block.model.IBakedModel;

/**
 * The machines' moving and glowing parts, each compiled once into a display list and then only called, turned by the
 * renderer. Building them again every frame (vertex by vertex, with smooth lighting read from the world for every face)
 * cost most of the frame near a turning drum. Lit by the lightmap set before the call: the world's light at the machine,
 * or full bright.
 */
public final class ModelLists
{
	private static final Map<IBakedModel, Integer> LISTS = new IdentityHashMap<>();

	private ModelLists()
	{
	}

	/** Draws the model, compiling it on its first use. */
	public static void draw(IBlockState state, IBakedModel model)
	{
		if(model == null)
			return;
		Integer id = LISTS.get(model);
		if(id == null)
		{
			id = GLAllocation.generateDisplayLists(1);
			GlStateManager.glNewList(id, GL11.GL_COMPILE);
			Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer().renderModelBrightnessColor(state, model, 1, 1, 1, 1);
			GlStateManager.glEndList();
			LISTS.put(model, id);
		}
		GlStateManager.callList(id);
	}

	/** Draws the model in the world's light at the given packed light (World.getCombinedLight). */
	public static void drawLit(IBlockState state, IBakedModel model, int packedLight)
	{
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, packedLight & 0xFFFF, packedLight >> 16 & 0xFFFF);
		draw(state, model);
	}

	/** Called from ModelBakeEvent: the models are baked anew, the old lists go. */
	public static void clear()
	{
		for(int id : LISTS.values())
			GLAllocation.deleteDisplayLists(id);
		LISTS.clear();
	}
}
