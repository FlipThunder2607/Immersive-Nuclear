package immersivenuclear.client;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileFilterPress;
import immersivenuclear.common.item.ItemFilter;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.obj.OBJModel;
import net.minecraftforge.common.model.TRSRTransformation;

/**
 * The living parts of the Filtration Unit (drawn by the master only), following the machine's step:
 * the filter plates close towards the middle like an accordion, pushed by the two actuators (with their telescopic
 * pipes), and open again; when they are apart the solids fall on the conveyor, which runs with IE's moving belt and
 * carries them into the opening at the end. The parts come from models/block/obj/filter_press_anim (one OBJ group
 * per part, written by bb2obj.py with each part's closed displacement in filter_press_anim.json), modelled open;
 * the rest of the press is the static blockstate model.
 */
public class RenderFilterPress extends TileEntitySpecialRenderer<TileFilterPress>
{
	static final String ANIM = "block/obj/filter_press_anim";
	static final String PLATE = "plate_", CLOTH = "cloth_", BELT = "belt", BELT_TEXTURE = "#conveyor_off", BELT_RUNNING = "immersiveengineering:blocks/conveyor";

	/** Per moving part: baked model (normal, mirrored) and the displacement along template z when closed, in blocks. */
	static final List<IBakedModel> parts = new ArrayList<>(), partsMirrored = new ArrayList<>();
	static final List<Float> offsets = new ArrayList<>();
	/** Per part: the filter slot whose filter it is (plates, from the pump end on), or -1 (actuators). */
	static final List<Integer> slotOf = new ArrayList<>();
	/** Per part: true for the cloth of a plate, which takes the colour of its filter's wear. */
	static final List<Boolean> isCloth = new ArrayList<>();
	// Cloth colour (multiplier of IE's texture): new, used (the brown of the residue soaking in), spent
	static final float[] NEW = {1, 1, 1}, USED = {0.72F, 0.55F, 0.36F}, SPENT = {0.36F, 0.32F, 0.28F};
	/** The belt, standing and running (IE's animated conveyor texture), normal and mirrored. */
	static IBakedModel belt, beltMirrored, beltRunning, beltRunningMirrored;

	// The falling solids, in the master's model space (reference orientation): the belt runs along +z into the
	// opening of the light engineering block at the end of the conveyor
	static final float BELT_Y = -0.925F, PLATES_BOTTOM_Y = -0.5F, CHUTE_Z = 3.0F, BELT_SPEED = 0.08F, FALL_TICKS = 8;
	static final float[][] PIECES = {{0.42F, -1.35F}, {0.58F, -0.7F}, {0.47F, -0.05F}, {0.55F, 0.6F}, {0.44F, 1.3F}, {0.56F, 2.0F}};
	static final float PIECE_SCALE = 0.35F;
	static ItemStack residue;

	/** Called from ModelBakeEvent: bakes every group of the animated OBJ on its own. */
	static void bake()
	{
		parts.clear();
		partsMirrored.clear();
		offsets.clear();
		slotOf.clear();
		isCloth.clear();
		belt = beltMirrored = beltRunning = beltRunningMirrored = null;
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
			IModel normal = load(ANIM + ".obj", textures.build()), mirrored = load(ANIM + "_mirrored.obj", textures.build());
			// Plates are named plate_<Blockbench x>; the pump end has the highest x and takes filter slot 0
			List<Float> plateX = new ArrayList<>();
			for(Map.Entry<String, JsonElement> e : side.getAsJsonObject("parts").entrySet())
				if(e.getKey().startsWith(PLATE))
					plateX.add(Float.parseFloat(e.getKey().substring(PLATE.length())));
			plateX.sort(java.util.Collections.reverseOrder());
			for(Map.Entry<String, JsonElement> e : side.getAsJsonObject("parts").entrySet())
			{
				if(e.getKey().equals(BELT))
				{
					belt = bakeGroup(normal, BELT);
					beltMirrored = bakeGroup(mirrored, BELT);
					beltRunning = bakeGroup(load(ANIM + ".obj", running.build()), BELT);
					beltRunningMirrored = bakeGroup(load(ANIM + "_mirrored.obj", running.build()), BELT);
					continue;
				}
				parts.add(bakeGroup(normal, e.getKey()));
				partsMirrored.add(bakeGroup(mirrored, e.getKey()));
				offsets.add(e.getValue().getAsFloat());
				String k = e.getKey();
				String plate = k.startsWith(PLATE) ? k.substring(PLATE.length()) : k.startsWith(CLOTH) ? k.substring(CLOTH.length()) : null;
				slotOf.add(plate == null ? -1 : plateX.indexOf(Float.parseFloat(plate)));
				isCloth.add(k.startsWith(CLOTH));
			}
		}
		catch(Exception e)
		{
			ImmersiveNuclear.logger.error("Could not load the filter press moving parts", e);
			parts.clear();
			partsMirrored.clear();
			offsets.clear();
			slotOf.clear();
			isCloth.clear();
		}
	}

	/** A moving-part OBJ, flat lit like the machine's own model: it is drawn every frame, and smooth lighting would ask
	 * the light of several neighbours for every face of every part. */
	static IModel load(String obj, ImmutableMap<String, String> textures) throws Exception
	{
		return ModelLoaderRegistry.getModel(new ResourceLocation(ImmersiveNuclear.MODID, obj))
				.process(ImmutableMap.of("flip-v", "true", "ambient", "false")).retexture(textures);
	}

	static IBakedModel bakeGroup(IModel model, String group)
	{
		return model.bake(new OBJModel.OBJState(ImmutableList.of(group), true, TRSRTransformation.identity()), DefaultVertexFormats.BLOCK, ModelLoader.defaultTextureGetter());
	}

	@Override
	public void render(TileFilterPress te, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
	{
		if(!te.isMaster() || parts.isEmpty())
			return;
		float closed = te.closing(partialTicks);
		float fall = te.fallTicks(partialTicks);

		BlockPos pos = te.getPos();
		IBlockState state = te.getWorld().getBlockState(pos).getActualState(te.getWorld(), pos);
		boolean mirrored = te.isMirrored();
		List<IBakedModel> models = mirrored ? partsMirrored : parts;

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

		for(int i = 0; i < models.size(); i++)
		{
			// A plate only shows when its slot holds a filter: the press fills up one plate at a time
			int slot = slotOf.get(i);
			if(slot >= 0 && (te.clientFilters & 1 << slot) == 0)
				continue;
			// Mirrored, left and right swap: the parts move the other way along template z
			draw(te, models.get(i), state, pos, (mirrored ? -1 : 1) * offsets.get(i) * closed, isCloth.get(i) ? clothColour(te.clientWear[slot]) : null);
		}
		// The belt runs while the solids are on it
		IBakedModel b = fall >= 0 ? (mirrored ? beltRunningMirrored : beltRunning) : (mirrored ? beltMirrored : belt);
		if(b != null)
			draw(te, b, state, pos, 0, null);
		RenderHelper.enableStandardItemLighting();

		if(fall >= 0)
			drawSolids(fall, mirrored);
		GlStateManager.popMatrix();
	}

	/** Clean cloth while new, browner as it wears, dark once spent (like the item's three looks). */
	static float[] clothColour(int wear)
	{
		if(wear >= ItemFilter.LIFE)
			return SPENT;
		float t = Math.min(1, wear / (float)(ItemFilter.LIFE - 1));
		return new float[]{NEW[0] + (USED[0] - NEW[0]) * t, NEW[1] + (USED[1] - NEW[1]) * t, NEW[2] + (USED[2] - NEW[2]) * t};
	}

	/** One part, lit like a chunk at the master's position with the world's light, shifted along template z;
	 * {@code tint} multiplies its colour (null = as textured). */
	void draw(TileFilterPress te, IBakedModel model, IBlockState state, BlockPos pos, float dz, float[] tint)
	{
		Tessellator tess = Tessellator.getInstance();
		BufferBuilder buffer = tess.getBuffer();
		GlStateManager.pushMatrix();
		GlStateManager.translate(0, 0, dz);
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

	/** Lumps of wet residue: they drop from under the plates onto the belt and ride it into the opening. */
	void drawSolids(float t, boolean mirrored)
	{
		if(residue == null)
			residue = new ItemStack(INContent.itemResidue);
		for(float[] p : PIECES)
		{
			float py, pz = p[1];
			if(t < FALL_TICKS)
			{
				float f = t / FALL_TICKS;
				py = PLATES_BOTTOM_Y + (BELT_Y + 0.1F - PLATES_BOTTOM_Y) * f * f;
			}
			else
			{
				py = BELT_Y + 0.1F;
				pz += (t - FALL_TICKS) * BELT_SPEED;
			}
			if(pz > CHUTE_Z - 0.1F)
				continue;
			GlStateManager.pushMatrix();
			GlStateManager.translate(p[0], py, mirrored ? 1 - pz : pz);
			GlStateManager.rotate((p[1] * 97) % 360, 0, 1, 0);
			GlStateManager.scale(PIECE_SCALE, PIECE_SCALE, PIECE_SCALE);
			Minecraft.getMinecraft().getRenderItem().renderItem(residue, ItemCameraTransforms.TransformType.FIXED);
			GlStateManager.popMatrix();
		}
	}

	/** Drawn whenever the machine's render box is on screen, not only while the master's chunk section is. */
	@Override
	public boolean isGlobalRenderer(TileFilterPress te)
	{
		return te.isMaster();
	}
}
