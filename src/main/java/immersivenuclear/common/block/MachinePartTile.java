package immersivenuclear.common.block;

import net.minecraft.util.EnumFacing;

/** A formed machine's part, as its hit boxes need it (see {@link MachineBounds}). */
public interface MachinePartTile
{
	/** The machine's model name: its hit boxes are assets/immersivenuclear/bounds/&lt;name&gt;.json. */
	String boundsName();

	/** This part's offset from the master, in the structure's reference orientation: {dx, dy, dz}. */
	int[] fromMaster();

	EnumFacing getFacing();

	boolean isMirrored();

	/** The part's port; 0 (no port) for plain structure. */
	int getPort();
}
