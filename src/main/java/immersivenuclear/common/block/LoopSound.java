package immersivenuclear.common.block;

import java.util.function.Supplier;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

/**
 * A machine part's running sound: start-up once, then IE's looping tile sound, shutdown once. IE keeps one looping
 * sound per block, so each sound of a machine plays from its own part (the block where that pump or mixer stands).
 * The tile must be an ISoundTile and answer {@link #shouldPlay} in shoudlPlaySound.
 */
public class LoopSound
{
	static final float VOLUME = 1.0F, PITCH = 1.0F;
	/** A pause shorter than this (between two batches) keeps the sound going. */
	static final int STOP_GRACE_TICKS = 20;

	/** The sounds are read when played: they are registered after the tiles' classes load. */
	final Supplier<SoundEvent> start, loop, stop;
	/** The loop comes in as the start-up ends. */
	final int loopDelayTicks;
	boolean on;
	int loopDelay, offTicks;

	public LoopSound(Supplier<SoundEvent> start, Supplier<SoundEvent> loop, Supplier<SoundEvent> stop, int loopDelayTicks)
	{
		this.start = start;
		this.loop = loop;
		this.stop = stop;
		this.loopDelayTicks = loopDelayTicks;
	}

	/** Client, every tick: whether the part is working now. */
	public void update(TileEntity te, boolean running)
	{
		offTicks = running ? 0 : offTicks + 1;
		boolean now = running || on && offTicks < STOP_GRACE_TICKS;
		if(now && !on)
		{
			play(te, start.get());
			loopDelay = loopDelayTicks;
		}
		else if(!now && on)
			play(te, stop.get());
		on = now;
		if(loopDelay > 0)
			loopDelay--;
		ImmersiveEngineering.proxy.handleTileSound(loop.get(), te, on && loopDelay == 0 && !te.isInvalid(), VOLUME, PITCH);
	}

	public boolean shouldPlay(TileEntity te, String sound)
	{
		return on && loopDelay == 0 && !te.isInvalid() && loop.get().getSoundName().toString().equals(sound);
	}

	/** When the structure comes apart or the chunk unloads. */
	public void stop(TileEntity te)
	{
		on = false;
		ImmersiveEngineering.proxy.handleTileSound(loop.get(), te, false, VOLUME, PITCH);
	}

	static void play(TileEntity te, SoundEvent sound)
	{
		te.getWorld().playSound(te.getPos().getX() + .5, te.getPos().getY() + .5, te.getPos().getZ() + .5, sound, SoundCategory.BLOCKS, VOLUME, PITCH, false);
	}
}
