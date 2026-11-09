package immersivenuclear.client;

import java.util.HashMap;
import java.util.Map;

import immersivenuclear.common.block.TileEnrichmentBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

/** What shines on the Enrichment Unit, drawn by the master: the converters' lamps and the centrifuges' panels, lit by its state (green running, blinking while it spins up; red stopped). */
public class RenderEnrichmentUnit extends TileEntitySpecialRenderer<TileEnrichmentBase>
{
	static final String[] MACHINES = {"enrichment_unit", "enrichment_unit_4", "enrichment_unit_6"};
	static final String[] LIGHTS = {"led_red", "led_green", "lamp_green", "lamp_red"};
	static final Map<String, String> EMISSIVE = new HashMap<>();
	static final GlowLayer[] GLOW = new GlowLayer[MACHINES.length];

	static
	{
		// The converters' lamps: only the 4 pixels of the lens shine
		EMISSIVE.put("#red_led", "immersivenuclear:blocks/red_led_emissive");
		EMISSIVE.put("#metal_multiblock_refinery", "immersivenuclear:blocks/enrichment_led_green_emissive");
		EMISSIVE.put("#conversion_led_red_off", "immersivenuclear:blocks/common_lamp_red");
		EMISSIVE.put("#conversion_led_green_off", "immersivenuclear:blocks/common_lamp_green");
		for(int i = 0; i < MACHINES.length; i++)
			GLOW[i] = new GlowLayer(MACHINES[i], LIGHTS, EMISSIVE);
	}

	/** Called from ModelBakeEvent. */
	static void bake()
	{
		for(GlowLayer g : GLOW)
			g.bake();
	}

	@Override
	public void render(TileEnrichmentBase te, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
	{
		if(!te.isMaster())
			return;
		int s = te.stateForClient();
		boolean on = s == TileEnrichmentBase.STATE_RUNNING;
		boolean stopped = !on && s != TileEnrichmentBase.STATE_IDLE;
		boolean green = on && (te.rotorSpeed > 0.95F || te.getWorld().getTotalWorldTime() % 10 < 5);
		if(!green && !stopped)
			return;
		IBlockState state = te.getWorld().getBlockState(te.getPos()).getActualState(te.getWorld(), te.getPos());
		GLOW[te.banks() / 2 - 1].render(state, te.getFacing(), te.isMirrored(), x, y, z, stopped, green, green, stopped);
	}

	/** Global: the master's chunk may be out of view while the machine's far end is on screen. Off screen, the render box culls it anyway. */
	@Override
	public boolean isGlobalRenderer(TileEnrichmentBase te)
	{
		return te.isMaster();
	}
}
