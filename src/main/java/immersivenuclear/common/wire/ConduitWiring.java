package immersivenuclear.common.wire;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import immersivenuclear.common.block.BlockComputerDevice;
import immersivenuclear.common.block.BlockComputerMachine;
import immersivenuclear.common.block.BlockConduit;
import immersivenuclear.common.block.TileComputerMachine;
import immersivenuclear.common.block.TileConduit;
import immersivenuclear.common.multiblock.ComputerStructure;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * Laying a wire, the way IE's coils work: click one computer, then the other. They must be joined by conduits from port to port; the wire takes the shortest way with room for it and shows in the conduits.
 * Data cables go from machine to machine. The power wire is a network: it starts at a Power Cabinet and each new machine branches off the nearest conduit that has it (the user, 06/11/2026).
 */
public final class ConduitWiring
{
	private static final String START = "conduitStart", START_DIM = "conduitStartDim";
	/** How many conduits a search looks at. */
	private static final int MAX_SEARCH = 4096;

	private ConduitWiring()
	{
	}

	/** A conduit in front of a port, and the side of it the port is on. */
	private static class Port
	{
		final BlockPos conduit;
		final EnumFacing side;

		Port(BlockPos conduit, EnumFacing side)
		{
			this.conduit = conduit;
			this.side = side;
		}
	}

	/** Use of a wire item on a block. PASS when the block is not a computer with a port. */
	public static EnumActionResult use(EntityPlayer player, World world, BlockPos pos, EnumHand hand, int kind)
	{
		BlockPos end = endAt(world, pos);
		if(end == null)
			return EnumActionResult.PASS;
		if(world.isRemote)
			return EnumActionResult.SUCCESS;
		ItemStack stack = player.getHeldItem(hand);
		if(kind == ConduitRules.DATA && isPowerCabinet(world, end))
			return say(player, "wrong_wire");
		NBTTagCompound tag = stack.getTagCompound();
		boolean started = tag != null && tag.hasKey(START);
		BlockPos start = started ? BlockPos.fromLong(tag.getLong(START)) : null;
		boolean sameWorld = started && tag.getInteger(START_DIM) == world.provider.getDimension();
		if(started)
			clearStart(stack);
		if(!sameWorld || !start.equals(endAt(world, start)))
		{
			stack.setTagInfo(START, new NBTTagLong(end.toLong()));
			stack.setTagInfo(START_DIM, new NBTTagInt(world.provider.getDimension()));
			return say(player, "start");
		}
		if(start.equals(end))
			return say(player, "cancelled");
		// Power is a network from the Power Cabinet: each machine is reached from the nearest conduit that already has its power wire. Data goes from one machine to the other.
		BlockPos first = start, second = end;
		if(kind == ConduitRules.POWER)
		{
			boolean startsThere = isPowerCabinet(world, start);
			if(startsThere == isPowerCabinet(world, end))
				return say(player, "needs_cabinet");
			first = startsThere ? start : end;
			second = startsThere ? end : start;
		}
		ConduitLinks links = ConduitLinks.get(world);
		if(links.has(kind, first, second))
			return say(player, "duplicate");
		List<Port> from = ports(world, first), to = ports(world, second);
		Map<BlockPos, Seed> seeds = new LinkedHashMap<>();
		if(kind == ConduitRules.POWER)
			for(ConduitLinks.Link link : links.powerFrom(first))
				for(int i = 0; i < link.path.size(); i++)
				{
					TileConduit.Wire wire = wireOf(world, link, i);
					if(wire != null)
						seeds.putIfAbsent(link.path.get(i), new Seed(wire.sideA, link, i));
				}
		if(seeds.isEmpty() && !anyConduit(world, from) || !anyConduit(world, to))
			return say(player, "no_conduit");
		Route route = route(world, seeds, from, to, kind, false);
		if(route == null)
			return say(player, route(world, seeds, from, to, kind, true) == null ? "no_path" : "full");
		// The conduit a branch leaves from has the wire already: only the new ones count
		boolean branch = route.seed.link != null;
		int length = route.path.size();
		int[] used = new int[length + 1];
		used[0] = branch ? 0 : tile(world, route.path.get(0)).usedLanes(route.seed.side);
		for(int i = 1; i < length; i++)
		{
			EnumFacing d = towards(route.path.get(i - 1), route.path.get(i));
			used[i] = tile(world, route.path.get(i - 1)).usedLanes(d) | tile(world, route.path.get(i)).usedLanes(d.getOpposite());
		}
		used[length] = tile(world, route.path.get(length - 1)).usedLanes(route.last);
		int[] lanes = ConduitRules.assignLanes(used, kind);
		if(lanes == null)
			return say(player, "full");
		int items = 0;
		if(!player.capabilities.isCreativeMode)
		{
			items = ConduitRules.cost(route.path.size() - (branch ? 1 : 0));
			Item item = stack.getItem();
			if(player.inventory.clearMatchingItems(item, -1, 0, null) < items)
			{
				player.sendStatusMessage(new TextComponentTranslation("chat.immersivenuclear.conduit.not_enough", items), true);
				return EnumActionResult.SUCCESS;
			}
			player.inventory.clearMatchingItems(item, -1, items, null);
		}
		// A link always holds the whole way from its first end, so taking one out leaves the wire the others still run on
		int id = links.newId();
		List<BlockPos> whole = new ArrayList<>();
		if(branch)
			for(int i = 0; i < route.seed.index; i++)
			{
				BlockPos conduit = route.seed.link.path.get(i);
				TileConduit.Wire wire = wireOf(world, route.seed.link, i);
				if(wire != null)
					tile(world, conduit).addWire(new TileConduit.Wire(id, kind, wire.laneA, wire.laneB, wire.sideA, wire.sideB));
				whole.add(conduit);
			}
		for(int i = 0; i < route.path.size(); i++)
		{
			BlockPos conduit = route.path.get(i);
			EnumFacing a = i == 0 ? route.seed.side : towards(conduit, route.path.get(i - 1));
			EnumFacing b = i == route.path.size() - 1 ? route.last : towards(conduit, route.path.get(i + 1));
			tile(world, conduit).addWire(new TileConduit.Wire(id, kind, lanes[i], lanes[i + 1], a, b));
			whole.add(conduit);
		}
		links.add(new ConduitLinks.Link(id, kind, first, second, whole, items));
		return say(player, "done");
	}

	private static EnumActionResult say(EntityPlayer player, String key)
	{
		player.sendStatusMessage(new TextComponentTranslation("chat.immersivenuclear.conduit." + key), true);
		return EnumActionResult.SUCCESS;
	}

	private static void clearStart(ItemStack stack)
	{
		NBTTagCompound tag = stack.getTagCompound();
		tag.removeTag(START);
		tag.removeTag(START_DIM);
		if(tag.isEmpty())
			stack.setTagCompound(null);
	}

	/** The machine or device a block belongs to, by the position wires keep: the master of a multiblock, or the block itself. Null if it takes no wire. */
	private static BlockPos endAt(World world, BlockPos pos)
	{
		IBlockState state = world.getBlockState(pos);
		if(state.getBlock() instanceof BlockComputerMachine)
		{
			TileEntity te = world.getTileEntity(pos);
			return te instanceof TileComputerMachine ? ((TileComputerMachine)te).getMasterPos() : null;
		}
		if(state.getBlock() instanceof BlockComputerDevice && ((BlockComputerDevice)state.getBlock()).hasConduitEntry())
			return pos;
		return null;
	}

	/** The power cabinet carries power only: the data cables do not go through it. */
	private static boolean isPowerCabinet(World world, BlockPos end)
	{
		TileEntity te = world.getTileEntity(end);
		return te instanceof TileComputerMachine && ComputerStructure.POWER_CABINET.name.equals(((TileComputerMachine)te).boundsName());
	}

	private static List<Port> ports(World world, BlockPos end)
	{
		List<Port> ports = new ArrayList<>();
		IBlockState state = world.getBlockState(end);
		if(state.getBlock() instanceof BlockComputerDevice)
		{
			EnumFacing front = state.getValue(BlockComputerDevice.FACING);
			ports.add(new Port(end.offset(front.getOpposite()), front));
			return ports;
		}
		TileEntity te = world.getTileEntity(end);
		if(te instanceof TileComputerMachine)
		{
			TileComputerMachine machine = (TileComputerMachine)te;
			for(BlockPos entry : machine.conduitEntries())
				ports.add(new Port(entry.offset(machine.getBack()), machine.getBack().getOpposite()));
		}
		return ports;
	}

	private static boolean joined(World world, Port port)
	{
		return world.isBlockLoaded(port.conduit) && world.getBlockState(port.conduit).getBlock() instanceof BlockConduit && BlockConduit.connects(world, port.conduit, port.side);
	}

	private static boolean anyConduit(World world, List<Port> ports)
	{
		for(Port port : ports)
			if(joined(world, port))
				return true;
		return false;
	}

	private static TileConduit tile(World world, BlockPos pos)
	{
		return (TileConduit)world.getTileEntity(pos);
	}

	private static EnumFacing towards(BlockPos from, BlockPos to)
	{
		for(EnumFacing d : EnumFacing.VALUES)
			if(from.offset(d).equals(to))
				return d;
		throw new IllegalArgumentException("not neighbours: " + from + ", " + to);
	}

	/** Where a wire can begin: the side it comes from, and for a branch the wire it leaves and how far along it. */
	private static class Seed
	{
		final EnumFacing side;
		final ConduitLinks.Link link;
		final int index;

		Seed(EnumFacing side, ConduitLinks.Link link, int index)
		{
			this.side = side;
			this.link = link;
			this.index = index;
		}
	}

	private static class Route
	{
		final List<BlockPos> path;
		final Seed seed;
		final EnumFacing last;

		Route(List<BlockPos> path, Seed seed, EnumFacing last)
		{
			this.path = path;
			this.seed = seed;
			this.last = last;
		}
	}

	private static TileConduit.Wire wireOf(World world, ConduitLinks.Link link, int index)
	{
		TileEntity te = world.getTileEntity(link.path.get(index));
		return te instanceof TileConduit ? ((TileConduit)te).wireOf(link.id) : null;
	}

	private static boolean hasRoom(World world, BlockPos conduit, int kind, boolean anyRoom)
	{
		if(anyRoom)
			return true;
		TileEntity te = world.getTileEntity(conduit);
		return te instanceof TileConduit && ((TileConduit)te).fits(kind);
	}

	/**
	 * The shortest way along the conduits to a port of the second end, from the given seeds (the conduits of a power network) or a port of the first end. A conduit with no room for the wire counts as a wall,
	 * unless {@code anyRoom}.
	 */
	private static Route route(World world, Map<BlockPos, Seed> seeds, List<Port> from, List<Port> to, int kind, boolean anyRoom)
	{
		Map<BlockPos, Seed> starts = new LinkedHashMap<>(seeds);
		Map<BlockPos, EnumFacing> goals = new HashMap<>();
		for(Port port : to)
			if(joined(world, port))
				goals.putIfAbsent(port.conduit, port.side);
		for(Port port : from)
			if(joined(world, port) && hasRoom(world, port.conduit, kind, anyRoom))
				starts.putIfAbsent(port.conduit, new Seed(port.side, null, 0));
		Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		for(BlockPos start : starts.keySet())
		{
			cameFrom.put(start, start);
			queue.add(start);
		}
		while(!queue.isEmpty() && cameFrom.size() < MAX_SEARCH)
		{
			BlockPos pos = queue.poll();
			if(goals.containsKey(pos))
			{
				List<BlockPos> path = new ArrayList<>();
				BlockPos step = pos;
				while(!cameFrom.get(step).equals(step))
				{
					path.add(step);
					step = cameFrom.get(step);
				}
				path.add(step);
				Collections.reverse(path);
				return new Route(path, starts.get(step), goals.get(pos));
			}
			for(EnumFacing d : EnumFacing.VALUES)
			{
				BlockPos next = pos.offset(d);
				if(cameFrom.containsKey(next) || !world.isBlockLoaded(next) || !(world.getBlockState(next).getBlock() instanceof BlockConduit))
					continue;
				if(BlockConduit.connects(world, pos, d) && hasRoom(world, next, kind, anyRoom))
				{
					cameFrom.put(next, pos);
					queue.add(next);
				}
			}
		}
		return null;
	}
}
