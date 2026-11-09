package immersivenuclear.client;

import com.google.common.collect.ImmutableMap;

import immersivenuclear.common.block.TileConversionUnit;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

/**
 * What is seen through the Conversion Unit's windows while it runs, drawn by the master only, full bright over the dark glass of the static model: the hot powder of the reduction reactor, the blue glow of
 * the hot cell, the green liquid of the hydrofluorinator, the electrolyte of the fluorine cells and the fire of the flame tower. Animated textures (ferramentas/janelas_conversao.py); they light nothing around them.
 */
public class RenderConversionUnit extends TileEntitySpecialRenderer<TileConversionUnit>
{
	static final GlowLayer GLOW = new GlowLayer("conversion_unit", new String[]{"window_reactor", "window_hotcell", "window_hidro", "window_cell", "window_fire"}, ImmutableMap.<String, String>builder()
			.put("#conversion_window_reactor", "immersivenuclear:blocks/conversion_window_reactor")
			.put("#conversion_window_hotcell", "immersivenuclear:blocks/conversion_window_hotcell")
			.put("#conversion_window_hidro", "immersivenuclear:blocks/conversion_window_hidro")
			.put("#conversion_window_cell", "immersivenuclear:blocks/conversion_window_cell")
			.put("#conversion_window_fire", "immersivenuclear:blocks/conversion_window_fire").build()).ownPlane();
	/** The glass only reflects the light around it: full bright, it washed the windows out at night. */
	static final GlowLayer GLASS = new GlowLayer("conversion_unit", new String[]{"glass"}, ImmutableMap.of("#conversion_glass", "immersivenuclear:blocks/conversion_glass")).ownPlane();

	/** The fluorine cells' LEDs, lit by the machine's state: fault, waiting, power, one working lamp per cell, and the local panel's on and off. */
	static final GlowLayer LEDS = new GlowLayer("conversion_unit", new String[]{"led_fault", "led_wait", "led_power", "led_run0", "led_run1", "led_run2", "led_on", "led_off"}, ImmutableMap.of(
			"#conversion_led_red_off", "immersivenuclear:blocks/common_lamp_red", "#conversion_led_orange_off", "immersivenuclear:blocks/common_lamp_orange", "#conversion_led_green_off", "immersivenuclear:blocks/common_lamp_green"));

	/** The reduction reactor's electric heating coils: a faint glow over the copper while it runs. */
	static final GlowLayer COILS = new GlowLayer("conversion_unit", new String[]{"reactor_coil"}, ImmutableMap.of("#sheetmetal_copper", "immersivenuclear:blocks/conversion_coil_heat"));

	@Override
	public void render(TileConversionUnit te, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
	{
		if(!te.isMaster())
			return;
		boolean on = te.running();
		GLOW.render(te.getWorld().getBlockState(te.getPos()), te.getFacing(), te.isMirrored(), x, y, z, on, on, on, on, on);
		COILS.render(te.getWorld().getBlockState(te.getPos()), te.getFacing(), te.isMirrored(), x, y, z, on);
		int state = te.stateForClient();
		long time = te.getWorld().getTotalWorldTime();
		boolean[] cell = new boolean[3];
		for(int i = 0; i < 3; i++)
			cell[i] = on && (te.draining() ? (time + i * 3) % 12 < 6 : (time + i * 13) % 40 < 24);
		LEDS.render(te.getWorld().getBlockState(te.getPos()), te.getFacing(), te.isMirrored(), x, y, z,
				state == TileConversionUnit.STATE_NO_POWER || state == TileConversionUnit.STATE_OUTPUT_FULL,
				state == TileConversionUnit.STATE_NO_INPUT || state == TileConversionUnit.STATE_DISABLED || te.draining() && time % 10 < 5,
				state != TileConversionUnit.STATE_NO_POWER, cell[0], cell[1], cell[2], on, !on);
		GLASS.renderLitBy(te.getWorld().getCombinedLight(te.getPos(), 0), te.getWorld().getBlockState(te.getPos()), te.getFacing(), te.isMirrored(), x, y, z, true);
	}

	@Override
	public boolean isGlobalRenderer(TileConversionUnit te)
	{
		return te.isMaster();                           // the master's chunk may be out of view; off screen, the render box culls it anyway
	}
}
