package immersivenuclear.common.block;

import immersivenuclear.ImmersiveNuclear;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Takes the multiblock parts that have nothing to do off their world's tick list (15/11/2026). Every block of a formed machine
 * is a ticking tile (one class for the whole machine), but only the master (processing), the output ports (pushing out) and,
 * on the client, the parts with an animation or a sound do anything: a 5 x 9 x 8 machine had some 170 tiles ticking for
 * nothing. A part tells it on its first tick ({@link #idle}); it is taken off at the END of the tick, never while the world
 * walks its list. Server and client parts wait apart, each taken off by its own thread. A part reloaded from disk is a new
 * tile and asks again; a broken machine's parts are gone anyway.
 */
@Mod.EventBusSubscriber(modid = ImmersiveNuclear.MODID)
public final class TickRetire
{
	private static final Set<TileEntity> SERVER = Collections.newSetFromMap(new ConcurrentHashMap<>()), CLIENT = Collections.newSetFromMap(new ConcurrentHashMap<>());

	private TickRetire()
	{
	}

	/**
	 * Called first in a part's update(): returns true (and has it taken off the tick list) when it has nothing to do on this
	 * side, false when it should go on.
	 */
	public static boolean idle(TileEntity te, boolean needed)
	{
		World w = te.getWorld();
		if(w == null)
			return false;
		if(w.isRemote)
		{
			// a freshly formed part may tick before the server's data says which part it is (it looks like a plain part
			// then): on the client a part goes only after CLIENT_WAIT ticks in a row with nothing to do
			if(needed)
			{
				CLIENT_SEEN.remove(te);
				return false;
			}
			long now = w.getTotalWorldTime();
			Long first = CLIENT_SEEN.get(te);
			if(first == null)
			{
				CLIENT_SEEN.put(te, now);
				return false;
			}
			if(now - first < CLIENT_WAIT)
				return false;
			CLIENT_SEEN.remove(te);
			CLIENT.add(te);
			return true;
		}
		if(needed)
			return false;
		SERVER.add(te);
		return true;
	}

	static final int CLIENT_WAIT = 40;
	/** Client thread only: when each part was first seen idle. */
	private static final java.util.Map<TileEntity, Long> CLIENT_SEEN = new java.util.WeakHashMap<>();

	static void drain(Set<TileEntity> set)
	{
		if(set.isEmpty())
			return;
		for(TileEntity te : set.toArray(new TileEntity[0]))
		{
			set.remove(te);
			World w = te.getWorld();
			if(w != null)
				w.tickableTileEntities.remove(te);
		}
	}

	@SubscribeEvent
	public static void onWorldTick(TickEvent.WorldTickEvent event)
	{
		if(event.phase == TickEvent.Phase.END && !event.world.isRemote)
			drain(SERVER);
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event)
	{
		if(event.phase == TickEvent.Phase.END)
			drain(CLIENT);
	}
}
