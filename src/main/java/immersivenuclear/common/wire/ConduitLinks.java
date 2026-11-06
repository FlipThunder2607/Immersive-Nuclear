package immersivenuclear.common.wire;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileConduit;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

/** Every wire laid in the conduits of a dimension, from machine to machine. The conduits keep their own list too, only to draw the wires (TileConduit). */
public class ConduitLinks extends WorldSavedData
{
	private static final String NAME = "immersivenuclear_conduit_links";

	/** One wire: its two ends (the master of a multiblock, or a single-block device), all the conduits from one to the other (power wires of one cabinet share the first ones) and the items laid. */
	public static class Link
	{
		public final int id, kind, items;
		public final BlockPos a, b;
		public final List<BlockPos> path;

		public Link(int id, int kind, BlockPos a, BlockPos b, List<BlockPos> path, int items)
		{
			this.id = id;
			this.kind = kind;
			this.a = a;
			this.b = b;
			this.path = path;
			this.items = items;
		}

		public boolean joins(BlockPos end)
		{
			return a.equals(end) || b.equals(end);
		}
	}

	private final Map<Integer, Link> links = new LinkedHashMap<>();
	private int nextId = 1;

	public ConduitLinks(String name)
	{
		super(name);
	}

	public static ConduitLinks get(World world)
	{
		ConduitLinks data = (ConduitLinks)world.getPerWorldStorage().getOrLoadData(ConduitLinks.class, NAME);
		if(data == null)
		{
			data = new ConduitLinks(NAME);
			world.getPerWorldStorage().setData(NAME, data);
		}
		return data;
	}

	public int newId()
	{
		markDirty();
		return nextId++;
	}

	public void add(Link link)
	{
		links.put(link.id, link);
		markDirty();
	}

	public boolean has(int kind, BlockPos a, BlockPos b)
	{
		for(Link link : links.values())
			if(link.kind == kind && link.joins(a) && link.joins(b))
				return true;
		return false;
	}

	/** The power wires of a Power Cabinet, which is always their first end. */
	public List<Link> powerFrom(BlockPos cabinet)
	{
		List<Link> found = new ArrayList<>();
		for(Link link : links.values())
			if(link.kind == ConduitRules.POWER && link.a.equals(cabinet))
				found.add(link);
		return found;
	}

	/** Takes a wire out of its conduits and drops its items at {@code dropAt}. */
	public void remove(World world, int id, BlockPos dropAt)
	{
		Link link = links.remove(id);
		if(link == null)
			return;
		markDirty();
		for(BlockPos pos : link.path)
		{
			TileEntity tile = world.getTileEntity(pos);
			if(tile instanceof TileConduit)
				((TileConduit)tile).removeWire(id);
		}
		if(link.items > 0)
			Block.spawnAsEntity(world, dropAt, new ItemStack(INContent.itemComputerParts[link.kind == ConduitRules.POWER ? 7 : 6], link.items));
	}

	/** Takes out every wire running through the conduit at pos. */
	public void removeThrough(World world, BlockPos pos)
	{
		for(Link link : new ArrayList<>(links.values()))
			if(link.path.contains(pos))
				remove(world, link.id, pos);
	}

	/** Takes out every wire ending at that machine or device. */
	public void removeAt(World world, BlockPos end, BlockPos dropAt)
	{
		for(Link link : new ArrayList<>(links.values()))
			if(link.joins(end))
				remove(world, link.id, dropAt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		links.clear();
		nextId = Math.max(1, nbt.getInteger("nextId"));
		NBTTagList list = nbt.getTagList("links", 10);
		for(int i = 0; i < list.tagCount(); i++)
		{
			NBTTagCompound tag = list.getCompoundTagAt(i);
			int[] xyz = tag.getIntArray("path");
			List<BlockPos> path = new ArrayList<>();
			for(int j = 0; j + 2 < xyz.length; j += 3)
				path.add(new BlockPos(xyz[j], xyz[j + 1], xyz[j + 2]));
			Link link = new Link(tag.getInteger("id"), tag.getInteger("kind"), BlockPos.fromLong(tag.getLong("a")), BlockPos.fromLong(tag.getLong("b")), path, tag.getInteger("items"));
			links.put(link.id, link);
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt)
	{
		nbt.setInteger("nextId", nextId);
		NBTTagList list = new NBTTagList();
		for(Link link : links.values())
		{
			NBTTagCompound tag = new NBTTagCompound();
			tag.setInteger("id", link.id);
			tag.setInteger("kind", link.kind);
			tag.setInteger("items", link.items);
			tag.setLong("a", link.a.toLong());
			tag.setLong("b", link.b.toLong());
			int[] xyz = new int[link.path.size() * 3];
			for(int j = 0; j < link.path.size(); j++)
			{
				BlockPos pos = link.path.get(j);
				xyz[j * 3] = pos.getX();
				xyz[j * 3 + 1] = pos.getY();
				xyz[j * 3 + 2] = pos.getZ();
			}
			tag.setIntArray("path", xyz);
			list.appendTag(tag);
		}
		nbt.setTag("links", list);
		return nbt;
	}
}
