package immersivenuclear.common.block;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import immersivenuclear.common.INContent;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

/**
 * The pump sound every machine plays while fluid moves inside it (filling, draining, pressurising, solvent lines...):
 * start-up once, then IE's looping tile sound, shutdown once. IE keeps one looping sound per block, so a machine plays
 * it from one of its pump parts, not from the part that plays its own sound. The tile must be an ISoundTile and
 * answer {@link #shouldPlay} in shoudlPlaySound.
 */
public class PumpSound
{
	static final float VOLUME = 1.0F, PITCH = 1.0F;
	/** The loop comes in as the 3.77 s start-up ends, less the 65 ms crossfade of the recording. */
	static final int LOOP_DELAY = 74;
	/** A pause shorter than this (between two batches) keeps the sound going. */
	static final int STOP_GRACE_TICKS = 20;

	boolean on;
	int loopDelay, offTicks;

	/** Client, every tick: whether fluid is moving now. */
	public void update(TileEntity te, boolean moving)
	{
		offTicks = moving ? 0 : offTicks + 1;
		boolean now = moving || on && offTicks < STOP_GRACE_TICKS;
		if(now && !on)
		{
			play(te, INContent.soundPumpStart);
			loopDelay = LOOP_DELAY;
		}
		else if(!now && on)
			play(te, INContent.soundPumpStop);
		on = now;
		if(loopDelay > 0)
			loopDelay--;
		ImmersiveEngineering.proxy.handleTileSound(INContent.soundPumpLoop, te, on && loopDelay == 0 && !te.isInvalid(), VOLUME, PITCH);
	}

	public boolean shouldPlay(TileEntity te, String sound)
	{
		return on && loopDelay == 0 && !te.isInvalid() && INContent.soundPumpLoop.getSoundName().toString().equals(sound);
	}

	/** When the structure comes apart or the chunk unloads. */
	public void stop(TileEntity te)
	{
		on = false;
		ImmersiveEngineering.proxy.handleTileSound(INContent.soundPumpLoop, te, false, VOLUME, PITCH);
	}

	static void play(TileEntity te, SoundEvent sound)
	{
		te.getWorld().playSound(te.getPos().getX() + .5, te.getPos().getY() + .5, te.getPos().getZ() + .5, sound, SoundCategory.BLOCKS, VOLUME, PITCH, false);
	}
}
