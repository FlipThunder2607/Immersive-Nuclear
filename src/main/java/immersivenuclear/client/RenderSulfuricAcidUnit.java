package immersivenuclear.client;

import java.util.HashMap;
import java.util.Map;

import immersivenuclear.common.block.TileSulfuricAcidUnit;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

/**
 * The Sulfuric Acid Unit's glowing parts while it runs (drawn by the master only), full bright so they shine in the
 * dark: the blue flame behind the burner's sight window and the firebox's glow (their textures glow whole), and the
 * instrument cabinet's green signal lamp. Everything else is the static blockstate model.
 */
public class RenderSulfuricAcidUnit extends TileEntitySpecialRenderer<TileSulfuricAcidUnit>
{
	static final GlowLayer GLOW;

	static
	{
		Map<String, String> emissive = new HashMap<>();
		emissive.put("#burner_flame", null);
		emissive.put("#burner_glow", null);
		emissive.put("#arc_furnace_active", "immersivenuclear:blocks/arc_furnace_lamp_emissive");
		GLOW = new GlowLayer("sulfuric_acid_unit", new String[]{"flame", "flameglow", "lamp"}, emissive);
	}

	@Override
	public void render(TileSulfuricAcidUnit te, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
	{
		if(!te.isMaster())
			return;
		boolean lit = te.burnerLit();
		GLOW.render(te.getWorld().getBlockState(te.getPos()), te.getFacing(), te.isMirrored(), x, y, z, lit, lit, lit);
	}

	@Override
	public boolean isGlobalRenderer(TileSulfuricAcidUnit te)
	{
		return te.isMaster();
	}
}
