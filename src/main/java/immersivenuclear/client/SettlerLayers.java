package immersivenuclear.client;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileChemicalSeparator;
import net.minecraftforge.fluids.Fluid;

/**
 * What a settler shows behind its glass, for the interface and for the windows of the model alike.
 * Filling: one cloudy mixture. Settling: the clear top layer grows down from the surface and the clear bottom layer up
 * from the floor, until they meet at the interface. Draining: two clear layers.
 * Heights are in the unit of the window given (GUI pixels or blocks).
 */
public class SettlerLayers
{
	/** What goes in (the water phase of the mixture) and what comes out below once settled. */
	public final Fluid inBottom, outBottom;
	/** Tint of the solvent in the mixture and of the settled top layer (amber while it carries the uranium). */
	public final int inTopTint, outTopTint;
	public final float height, topHeight, bottomHeight, clearTop, clearBottom, mixed;

	SettlerLayers(int stage, int phase, int ticks, int top, int bottom, float windowH)
	{
		int total = top + bottom;
		inBottom = stage == 0 ? INContent.fluidCrudeSolution : INContent.fluidStrippingSolution;
		outBottom = stage == 0 ? INContent.fluidEffluent : INContent.fluidPurifiedSolution;
		int amber = 0xffffb04a, clear = 0xffffffff;
		inTopTint = stage == 0 ? clear : amber;
		outTopTint = stage == 0 ? amber : clear;
		height = windowH * Math.min(1, total / (float)TileChemicalSeparator.SETTLER_CAPACITY);
		topHeight = height * top / total;
		bottomHeight = height - topHeight;
		float p = phase == TileChemicalSeparator.PHASE_FILL ? 0 : phase == TileChemicalSeparator.PHASE_SETTLE ? Math.min(1, ticks / (float)TileChemicalSeparator.SETTLE_TICKS) : 1;
		clearTop = topHeight * p;
		clearBottom = bottomHeight * p;
		mixed = height - clearTop - clearBottom;
	}

	/** Settler 0 (A) or 1 (B) from {phase, ticks, top, bottom}; null when it is empty. */
	public static SettlerLayers of(int stage, int[] s, float windowH)
	{
		return s[2] + s[3] <= 0 ? null : new SettlerLayers(stage, s[0], s[1], s[2], s[3], windowH);
	}

	/** The layers have met: the interface line shows. */
	public boolean separated()
	{
		return mixed <= 0.001F && topHeight > 0 && bottomHeight > 0;
	}
}
