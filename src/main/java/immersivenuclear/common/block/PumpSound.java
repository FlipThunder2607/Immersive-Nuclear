package immersivenuclear.common.block;

import immersivenuclear.common.INContent;

/**
 * The pump sound every machine plays while fluid moves inside it (filling, draining, pressurising, solvent lines...),
 * from the part where that pump stands.
 */
public class PumpSound extends LoopSound
{
	/** The loop comes in as the 3.77 s start-up ends, less the 65 ms crossfade of the recording. */
	static final int LOOP_DELAY = 74;

	public PumpSound()
	{
		super(() -> INContent.soundPumpStart, () -> INContent.soundPumpLoop, () -> INContent.soundPumpStop, LOOP_DELAY);
	}
}
