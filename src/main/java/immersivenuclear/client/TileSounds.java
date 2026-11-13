package immersivenuclear.client;

import java.lang.reflect.Field;
import java.util.Map;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Brings back a machine's loop that the game dropped. Minecraft has a fixed number of sound channels; when they run out
 * (rain, many machines, explosions) it stops some sounds. IE keeps its looping tile sound in a map and never notices,
 * so the machine went quiet until it stopped and started again. Every 2 s this looks at the loop: if the sound system
 * no longer plays it, it comes out of IE's map, and IE's next handleTileSound starts it again.
 */
public class TileSounds
{
	static Field mapField;
	static boolean broken;

	/** Client, every tick, before handleTileSound, while the loop should be playing. */
	public static void revive(TileEntity te)
	{
		if(broken || (te.getWorld().getTotalWorldTime() + te.getPos().hashCode()) % 40 != 0)
			return;
		Minecraft mc = Minecraft.getMinecraft();
		if(mc.isGamePaused())
			return;
		try
		{
			if(mapField == null)
			{
				mapField = ImmersiveEngineering.proxy.getClass().getDeclaredField("tileSoundMap");
				mapField.setAccessible(true);
			}
			@SuppressWarnings("unchecked")
			Map<BlockPos, ? extends ISound> map = (Map<BlockPos, ? extends ISound>)mapField.get(ImmersiveEngineering.proxy);
			ISound sound = map.get(te.getPos());
			if(sound != null && !mc.getSoundHandler().isSoundPlaying(sound))
			{
				mc.getSoundHandler().stopSound(sound);
				map.remove(te.getPos());
			}
		}
		catch(ReflectiveOperationException | RuntimeException e)
		{
			broken = true;                             // another IE build: keep IE's own behaviour
		}
	}
}
