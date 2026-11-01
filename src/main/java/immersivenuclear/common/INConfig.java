package immersivenuclear.common;

import immersivenuclear.ImmersiveNuclear;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** config/immersivenuclear.cfg: the ores the mod generates (a pack that already has fluorite can turn ours off). */
@Config(modid = ImmersiveNuclear.MODID)
@Mod.EventBusSubscriber(modid = ImmersiveNuclear.MODID)
public class INConfig
{
	@Config.Comment("Fluorite ore in the Overworld's stone")
	public static Ore fluorite = new Ore(true, 5, 8, 10, 60);

	@Config.Comment("Fluorite as a mineral vein of IE's Excavator (applies after a restart)")
	public static boolean fluoriteExcavatorVein = true;

	public static class Ore
	{
		@Config.Comment("Generate this ore")
		public boolean generate;
		@Config.RangeInt(min = 0, max = 64)
		public int veinsPerChunk;
		@Config.Comment("Blocks per vein")
		@Config.RangeInt(min = 1, max = 64)
		public int veinSize;
		@Config.RangeInt(min = 0, max = 255)
		public int minY;
		@Config.RangeInt(min = 1, max = 256)
		public int maxY;

		Ore(boolean generate, int veinsPerChunk, int veinSize, int minY, int maxY)
		{
			this.generate = generate;
			this.veinsPerChunk = veinsPerChunk;
			this.veinSize = veinSize;
			this.minY = minY;
			this.maxY = maxY;
		}
	}

	@SubscribeEvent
	public static void onChanged(ConfigChangedEvent.OnConfigChangedEvent event)
	{
		if(ImmersiveNuclear.MODID.equals(event.getModID()))
			ConfigManager.sync(ImmersiveNuclear.MODID, Config.Type.INSTANCE);
	}
}
