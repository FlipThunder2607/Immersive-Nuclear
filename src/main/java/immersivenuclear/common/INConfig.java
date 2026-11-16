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

	@Config.Comment("Zircon ore in clusters inside the granite pockets underground (its main source, like the real pegmatites)")
	public static Ore zirconGranite = new Ore(true, 8, 5, 0, 80);

	@Config.Comment("Zircon ore in the sand of beaches, rare (the real heavy mineral sands)")
	public static Surface zirconBeach = new Surface(true, 6, 4);

	@Config.Comment("Borax ore in the terracotta of mesas (the clay beds of old dry lakes)")
	public static Ore boraxMesa = new Ore(true, 6, 7, 40, 128);

	@Config.Comment("Borax ore near the surface of deserts' sand (the crust of a dry lake)")
	public static Surface boraxDesert = new Surface(true, 3, 5);

	@Config.Comment("Tin ore (cassiterite) in the Overworld's stone (IE has no tin; turn it off when another mod brings its own)")
	public static Ore tin = new Ore(true, 8, 8, 20, 64);

	@Config.Comment("Chromite ore, deeper and rarer, in the Overworld's stone")
	public static Ore chromite = new Ore(true, 3, 6, 5, 32);

	@Config.Comment("Zircon, borax, cassiterite and chromite as mineral veins of IE's Excavator (applies after a restart)")
	public static boolean line3ExcavatorVeins = true;

	public static class Surface
	{
		@Config.Comment("Generate this ore")
		public boolean generate;
		@Config.Comment("One patch every this many chunks of the right biome, on average")
		@Config.RangeInt(min = 1, max = 256)
		public int oneInChunks;
		@Config.Comment("Blocks per patch")
		@Config.RangeInt(min = 1, max = 32)
		public int patchSize;

		Surface(boolean generate, int oneInChunks, int patchSize)
		{
			this.generate = generate;
			this.oneInChunks = oneInChunks;
			this.patchSize = patchSize;
		}
	}

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
