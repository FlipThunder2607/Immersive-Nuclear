package immersivenuclear.client;

import java.util.List;
import org.lwjgl.opengl.GL11;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.block.TileConduit;
import immersivenuclear.common.wire.ConduitRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;

/**
 * The wires laid in a conduit. Each lies in its lane: a wire running east-west is offset along z, one running north-south along x, one running up or down along both, by the same amount, so all three meet at
 * one point. Lane 0 is south and east, and the wires keep their sides through every turn and crossing (the user, 06/11/2026). That makes them cross each other there: in a block where wires run both ways, the
 * east-west ones climb over the others across the tray and come back down to the bed at its edge. Data cables fill a lane, the power wire four, so a full conduit has no gaps.
 */
public class RenderConduit extends TileEntitySpecialRenderer<TileConduit>
{
	private static final float PX = 1 / 16f;
	/** Half the height of a wire, and half the width of a data cable and of the power wire. */
	private static final float HALF = 0.5f * PX, HALF_DATA = 0.48f * PX, HALF_POWER = 1.96f * PX;
	/** Middle of a wire lying on the tray's bed, and of one climbing over the others. */
	private static final float LOW = 6 * PX + HALF, HIGH = LOW + PX;
	/** The middle of lane 0, the middle of the power wire, and where the lanes begin and end across the tray. */
	private static final float FIRST_LANE = 11.5f * PX, POWER_MIDDLE = 10 * PX, TRAY_START = 4 * PX, TRAY_END = 12 * PX;

	@Override
	public void render(TileConduit te, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
	{
		List<TileConduit.Wire> wires = te.getWires();
		if(wires.isEmpty())
			return;
		bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		RenderHelper.disableStandardItemLighting();
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		GlStateManager.color(1, 1, 1, 1);
		int light = te.getWorld().getCombinedLight(te.getPos(), 0);
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 0xFFFF, light >> 16);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
		TextureMap atlas = Minecraft.getMinecraft().getTextureMapBlocks();
		TextureAtlasSprite power = atlas.getAtlasSprite(ImmersiveNuclear.MODID + ":blocks/computers/cabo_energia"), data = atlas.getAtlasSprite(ImmersiveNuclear.MODID + ":blocks/computers/cabo_dados");
		boolean eastWest = false, northSouth = false;
		int lanes = 0;    // Branches of the power wire are one wire
		for(TileConduit.Wire wire : wires)
		{
			lanes |= 1 << wire.lane;
			eastWest |= wire.sideA.getAxis() == EnumFacing.Axis.X || wire.sideB.getAxis() == EnumFacing.Axis.X;
			northSouth |= wire.sideA.getAxis() == EnumFacing.Axis.Z || wire.sideB.getAxis() == EnumFacing.Axis.Z;
		}
		boolean crossing = Integer.bitCount(lanes) > 1 && eastWest && northSouth;
		for(TileConduit.Wire wire : wires)
		{
			TextureAtlasSprite sprite = wire.kind == ConduitRules.POWER ? power : data;
			boolean isPower = wire.kind == ConduitRules.POWER;
			float across = isPower ? POWER_MIDDLE : FIRST_LANE - wire.lane * PX, half = isPower ? HALF_POWER : HALF_DATA;
			boolean a = wire.sideA.getAxis() == EnumFacing.Axis.X, b = wire.sideB.getAxis() == EnumFacing.Axis.X;
			arm(buffer, sprite, across, half, wire.sideA, crossing);
			arm(buffer, sprite, across, half, wire.sideB, crossing);
			// A wire turning off the east-west run comes down at the meeting point
			if(crossing && a != b)
				box(buffer, sprite, across - half, LOW - HALF, across - half, across + half, HIGH + HALF, across + half);
		}
		tessellator.draw();
		GlStateManager.popMatrix();
		RenderHelper.enableStandardItemLighting();
	}

	/** The wire from the meeting point out to the edge of the block on that side. */
	private static void arm(BufferBuilder b, TextureAtlasSprite s, float across, float half, EnumFacing side, boolean crossing)
	{
		float lo = across - half, hi = across + half;
		if(crossing && side.getAxis() == EnumFacing.Axis.X)
		{
			boolean west = side == EnumFacing.WEST;
			float edge = west ? TRAY_START : TRAY_END;
			// On the bed from the edge of the block to the tray, up, and over the wires to the meeting point
			box(b, s, west ? 0 : edge, LOW - HALF, lo, west ? edge : 1, LOW + HALF, hi);
			box(b, s, edge - HALF, LOW - HALF, lo, edge + HALF, HIGH + HALF, hi);
			box(b, s, west ? edge : lo, HIGH - HALF, lo, west ? hi : edge, HIGH + HALF, hi);
			return;
		}
		float x0 = lo, x1 = hi, y0 = LOW - HALF, y1 = LOW + HALF, z0 = lo, z1 = hi;
		switch(side)
		{
			case DOWN: y0 = 0; break;
			case UP: y1 = 1; break;
			case NORTH: z0 = 0; break;
			case SOUTH: z1 = 1; break;
			case WEST: x0 = 0; break;
			default: x1 = 1; break;
		}
		box(b, s, x0, y0, z0, x1, y1, z1);
	}

	private static void box(BufferBuilder b, TextureAtlasSprite s, float x0, float y0, float z0, float x1, float y1, float z1)
	{
		float u0 = s.getInterpolatedU(6), u1 = s.getInterpolatedU(10), v0 = s.getInterpolatedV(6), v1 = s.getInterpolatedV(10);
		quad(b, 1f, u0, u1, v0, v1, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
		quad(b, .5f, u0, u1, v0, v1, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
		quad(b, .8f, u0, u1, v0, v1, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0);
		quad(b, .8f, u0, u1, v0, v1, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1);
		quad(b, .6f, u0, u1, v0, v1, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1);
		quad(b, .6f, u0, u1, v0, v1, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0);
	}

	private static void quad(BufferBuilder b, float shade, float u0, float u1, float v0, float v1, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz)
	{
		b.pos(ax, ay, az).tex(u0, v0).color(shade, shade, shade, 1f).endVertex();
		b.pos(bx, by, bz).tex(u0, v1).color(shade, shade, shade, 1f).endVertex();
		b.pos(cx, cy, cz).tex(u1, v1).color(shade, shade, shade, 1f).endVertex();
		b.pos(dx, dy, dz).tex(u1, v0).color(shade, shade, shade, 1f).endVertex();
	}
}
