package immersivenuclear.client;

import com.google.common.collect.ImmutableMap;

import immersivenuclear.common.block.TileElectrolyzer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

/**
 * The Electrolyzer's glowing bits, drawn by the master only, while the cells run: the rectifier panel's signal lamps
 * (elements "lamp", on IE's arc furnace texture) and the cells between the copper plates (a soft blue: the emissive
 * layer of electrolyzer_cell_stack). Full bright, but they light nothing around them. The rest is the static blockstate model.
 */
public class RenderElectrolyzer extends TileEntitySpecialRenderer<TileElectrolyzer>
{
	static final GlowLayer GLOW = new GlowLayer("electrolyzer", new String[]{"lamp", "cells"}, ImmutableMap.of("#arc_furnace_active", "immersivenuclear:blocks/arc_furnace_lamp_emissive",
			"#electrolyzer_cell_stack", "immersivenuclear:blocks/electrolyzer_cell_stack_emissive"));

	@Override
	public void render(TileElectrolyzer te, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
	{
		if(!te.isMaster())
			return;
		boolean on = te.running();
		// one flag per glowing part (lamp, cells): with one only, the cells were never drawn lit
		GLOW.render(te.getWorld().getBlockState(te.getPos()), te.getFacing(), te.isMirrored(), x, y, z, on, on);
	}

	@Override
	public boolean isGlobalRenderer(TileElectrolyzer te)
	{
		return te.isMaster();
	}
}
