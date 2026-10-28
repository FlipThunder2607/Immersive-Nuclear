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
import immersivenuclear.common.block.TileIndustrialDryer;
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
 * The drum loading station of the Industrial Dryer (drawn by the master only):
 * - the Concentrate Drum (Blockbench group "barrel", modelled under the discharge) rides the conveyor from the empty
 *   drums' port to the discharge, stops there while it fills, and goes on into the output once full;
 * - the conveyor (group "conveyorBelt") runs with IE's moving belt while a drum rides it.
 * Both come from models/block/obj/industrial_dryer_anim (written by bb2obj.py); the rest is the static blockstate model.
 */
public class RenderIndustrialDryer extends TileEntitySpecialRenderer<TileIndustrialDryer>
{
	static final String ANIM = "block/obj/industrial_dryer_anim";
	static final String DRUM = "drum", BELT = "belt", BELT_TEXTURE = "#conveyor_off", BELT_RUNNING = "immersiveengineering:blocks/conveyor";
	static IBakedModel drum, drumMirrored, belt, beltMirrored, beltRunning, beltRunningMirrored;

	/** Called from ModelBakeEvent: bakes the drum and the belt (standing and running) on their own. */
	static void bake()
	{
		drum = drumMirrored = belt = beltMirrored = beltRunning = beltRunningMirrored = null;
		try
		{
			ResourceLocation json = new ResourceLocation(ImmersiveNuclear.MODID, "models/" + ANIM + ".json");
			JsonObject side;
			try(Reader r = new InputStreamReader(Minecraft.getMinecraft().getResourceManager().getResource(json).getInputStream(), StandardCharsets.UTF_8))
			{
				side = new JsonParser().parse(r).getAsJsonObject();
			}
			ImmutableMap.Builder<String, String> textures = ImmutableMap.builder(), running = ImmutableMap.builder();
			for(Map.Entry<String, JsonElement> e : side.getAsJsonObject("textures").entrySet())
			{
				textures.put(e.getKey(), e.getValue().getAsString());
				running.put(e.getKey(), e.getKey().equals(BELT_TEXTURE) ? BELT_RUNNING : e.getValue().getAsString());
			}
			IModel normal = RenderFilterPress.load(ANIM + ".obj", textures.build()), mirrored = RenderFilterPress.load(ANIM + "_mirrored.obj", textures.build());
			drum = RenderFilterPress.bakeGroup(normal, DRUM);
			drumMirrored = RenderFilterPress.bakeGroup(mirrored, DRUM);
			belt = RenderFilterPress.bakeGroup(normal, BELT);
			beltMirrored = RenderFilterPress.bakeGroup(mirrored, BELT);
			beltRunning = RenderFilterPress.bakeGroup(RenderFilterPress.load(ANIM + ".obj", running.build()), BELT);
			beltRunningMirrored = RenderFilterPress.bakeGroup(RenderFilterPress.load(ANIM + "_mirrored.obj", running.build()), BELT);
		}
		catch(Exception e)
		{
			ImmersiveNuclear.logger.error("Could not load the industrial dryer's drum station", e);
			drum = null;
		}
	}

	@Override
	public void render(TileIndustrialDryer te, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
	{
		if(!te.isMaster() || drum == null)
			return;
		boolean mirrored = te.isMirrored();
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

		boolean moving = te.drumMoving();
		IBakedModel b = moving ? (mirrored ? beltRunningMirrored : beltRunning) : (mirrored ? beltMirrored : belt);
		draw(te, b, state, pos, 0, 0);
		float[] at = te.drumShift(partialTicks);
		// Mirrored, the model runs the other way along template z
		if(at != null)
			draw(te, mirrored ? drumMirrored : drum, state, pos, at[0], mirrored ? -at[1] : at[1]);
		RenderHelper.enableStandardItemLighting();
		GlStateManager.popMatrix();
	}

	/** One part, lit like a chunk at the master's position with the world's light, shifted by template (dx, dz). */
	static void draw(TileIndustrialDryer te, IBakedModel model, IBlockState state, BlockPos pos, float dx, float dz)
	{
		if(model == null)
			return;
		Tessellator tess = Tessellator.getInstance();
		BufferBuilder buffer = tess.getBuffer();
		GlStateManager.pushMatrix();
		GlStateManager.translate(dx, 0, dz);
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
		buffer.setTranslation(-pos.getX(), -pos.getY(), -pos.getZ());
		Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer().renderModel(te.getWorld(), model, state, pos, buffer, false);
		buffer.setTranslation(0, 0, 0);
		tess.draw();
		GlStateManager.popMatrix();
	}

	/** Drawn whenever the machine's render box is on screen, not only while the master's chunk section is. */
	@Override
	public boolean isGlobalRenderer(TileIndustrialDryer te)
	{
		return te.isMaster();
	}
}
