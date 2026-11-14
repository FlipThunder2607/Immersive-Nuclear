package immersivenuclear.client;

import java.util.HashMap;
import java.util.Map;

import immersivenuclear.common.block.TileSulfuricAcidUnit;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

/**
 * The Sulfuric Acid Unit's glowing parts (drawn by the master only), full bright so they shine in the dark: while it
 * runs, the blue flame in the burner's sight window and the firebox's glow behind it (their textures glow whole; the
 * static model draws the dark firebox, burner_glow_off, and no flame) and a faint heat over the sulfur melter's copper
 * steam jacket (as the Conversion Unit's coils); the manometers' faint backlight while it has power (only their glass,
 * the needle stays dark). No LEDs on this machine (the user, 14/11). Over the flame, the burner's dark wired sight glass:
 * translucent, so the static model (cutout) leaves it to GLASS, drawn after the glow and blended, in the world's light. Everything else is the static blockstate model.
 */
public class RenderSulfuricAcidUnit extends TileEntitySpecialRenderer<TileSulfuricAcidUnit>
{
	static final GlowLayer GLOW, FIRE;
	static final GlowLayer GLASS = new GlowLayer("sulfuric_acid_unit", new String[]{"glass"},
			com.google.common.collect.ImmutableMap.of("#burner_sight_glass", "immersivenuclear:blocks/burner_sight_glass")).ownPlane();

	static
	{
		Map<String, String> emissive = new HashMap<>();
		emissive.put("#sheetmetal_copper", "immersivenuclear:blocks/conversion_coil_heat");
		emissive.put("#metal_multiblock_mixer", "immersivenuclear:blocks/mixer_gauge_glass_emissive");
		GLOW = new GlowLayer("sulfuric_acid_unit", new String[]{"coil", "gauges"}, emissive);
		Map<String, String> fire = new HashMap<>();
		fire.put("#burner_flame", null);
		fire.put("#burner_firebox_glow", null);
		// the flame and the firebox's lit lining are planes of their own (bb2obj overlays, not in the static model, which
		// keeps the cold firebrick 0.01 behind them): no pull towards the eye (with it their light showed along the edges of
		// the parts in front; drawn on the static walls' own plane, they flickered: the user 14/11)
		FIRE = new GlowLayer("sulfuric_acid_unit", new String[]{"flame", "flameglow"}, fire).ownPlane();
	}

	@Override
	public void render(TileSulfuricAcidUnit te, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
	{
		if(!te.isMaster())
			return;
		boolean lit = te.burnerLit(), powered = te.powered();
		FIRE.render(te.getWorld().getBlockState(te.getPos()), te.getFacing(), te.isMirrored(), x, y, z, lit, lit);
		GLOW.render(te.getWorld().getBlockState(te.getPos()), te.getFacing(), te.isMirrored(), x, y, z, lit, powered || lit);
		GLASS.renderLitBy(te.getWorld().getCombinedLight(te.getPos(), 0), te.getWorld().getBlockState(te.getPos()), te.getFacing(), te.isMirrored(), x, y, z, true);
	}

	@Override
	public boolean isGlobalRenderer(TileSulfuricAcidUnit te)
	{
		return te.isMaster();
	}
}
