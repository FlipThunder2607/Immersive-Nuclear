package immersivenuclear.common.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The machines' hit boxes, shaped like their models instead of whole blocks: the outline, what entities collide with and
 * what the cursor hits (through the gaps between the model's parts it reaches what is behind). The boxes come from the
 * model when it is converted (ferramentas/bb2obj.py writes assets/immersivenuclear/bounds/&lt;machine&gt;.json: per part,
 * up to 4 boxes on a 1/8 block grid, small details left out, in the structure's reference orientation); here they are
 * turned and mirrored with the machine. Ports (items, fluids, energy, redstone) keep the whole block, so pipes, cables
 * and the hammer always find them; a machine without the file keeps whole blocks everywhere. Used on both sides, as
 * collision runs on the server too.
 */
public final class MachineBounds
{
	static final List<AxisAlignedBB> FULL = Collections.singletonList(Block.FULL_BLOCK_AABB);
	/** A part the model leaves empty: a box of no size (Block.NULL_AABB is null in 1.12, and callers such as the rain's
	 * particles read the box of any block near the player). */
	static final AxisAlignedBB NOTHING = new AxisAlignedBB(0.5, 0, 0.5, 0.5, 0, 0.5);
	/** Model name -> grid size and boxes per part ({x0, y0, z0, x1, y1, z1} in grid units), keyed by {@link #key}. */
	static final Map<String, Model> MODELS = new ConcurrentHashMap<>();
	/** Turned boxes, per model, part, facing and mirroring. */
	static final Map<String, List<AxisAlignedBB>> TURNED = new ConcurrentHashMap<>();

	static final class Model
	{
		final int grid;
		final Map<Long, int[][]> cells;

		Model(int grid, Map<Long, int[][]> cells)
		{
			this.grid = grid;
			this.cells = cells;
		}
	}

	static final Model MISSING = new Model(1, Collections.emptyMap());

	private MachineBounds()
	{
	}

	/** The part's boxes, in its own block (0..1); whole block for ports, unknown blocks and machines without the file. */
	public static List<AxisAlignedBB> boxes(IBlockAccess world, BlockPos pos)
	{
		TileEntity te = world.getTileEntity(pos);
		if(!(te instanceof MachinePartTile))
			return FULL;
		MachinePartTile part = (MachinePartTile)te;
		if(part.getPort() != 0)
			return FULL;
		int[] r = part.fromMaster();
		String name = part.boundsName();
		EnumFacing facing = part.getFacing();
		boolean mirrored = part.isMirrored();
		return TURNED.computeIfAbsent(name + ' ' + r[0] + ',' + r[1] + ',' + r[2] + ' ' + facing + ' ' + mirrored,
				k -> turned(name, r, facing, mirrored));
	}

	/** The outline: round every box of the part (nothing for a part the model leaves empty). */
	public static AxisAlignedBB outline(IBlockAccess world, BlockPos pos)
	{
		List<AxisAlignedBB> boxes = boxes(world, pos);
		if(boxes.isEmpty())
			return NOTHING;
		AxisAlignedBB all = boxes.get(0);
		for(AxisAlignedBB b : boxes)
			all = all.union(b);
		return all;
	}

	public static void addCollision(IBlockAccess world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> out, @Nullable Entity entity)
	{
		for(AxisAlignedBB b : boxes(world, pos))
		{
			AxisAlignedBB w = b.offset(pos);
			if(entityBox.intersects(w))
				out.add(w);
		}
	}

	/** The nearest box the ray hits, or null: the cursor goes through the gaps to what is behind. */
	@Nullable
	public static RayTraceResult rayTrace(IBlockAccess world, BlockPos pos, Vec3d start, Vec3d end)
	{
		RayTraceResult best = null;
		double bestDistance = Double.MAX_VALUE;
		for(AxisAlignedBB b : boxes(world, pos))
		{
			RayTraceResult hit = b.offset(pos).calculateIntercept(start, end);
			if(hit != null)
			{
				double d = hit.hitVec.squareDistanceTo(start);
				if(d < bestDistance)
				{
					bestDistance = d;
					best = new RayTraceResult(hit.hitVec, hit.sideHit, pos);
				}
			}
		}
		return best;
	}

	static long key(int x, int y, int z)
	{
		return new BlockPos(x, y, z).toLong();
	}

	/**
	 * The template's boxes turned like the structure: mirrored, z across the cell (as the structure mirrors dz), then a
	 * quarter turn at a time, x' = 1 - z, z' = x (as the structure's rotate: x = -z, z = x, about the block centres).
	 */
	static List<AxisAlignedBB> turned(String name, int[] r, EnumFacing facing, boolean mirrored)
	{
		Model model = MODELS.computeIfAbsent(name, MachineBounds::load);
		if(model == MISSING)
			return FULL;
		int[][] boxes = model.cells.get(key(r[0], r[1], r[2]));
		if(boxes == null)
			return Collections.emptyList();
		int g = model.grid;
		int turns = (facing.getHorizontalIndex() + 2) & 3;
		List<AxisAlignedBB> out = new ArrayList<>(boxes.length);
		for(int[] b : boxes)
		{
			int x0 = b[0], y0 = b[1], z0 = b[2], x1 = b[3], y1 = b[4], z1 = b[5];
			if(mirrored)
			{
				int t = g - z1;
				z1 = g - z0;
				z0 = t;
			}
			for(int i = 0; i < turns; i++)
			{
				int nx0 = g - z1, nx1 = g - z0;
				z0 = x0;
				z1 = x1;
				x0 = nx0;
				x1 = nx1;
			}
			out.add(new AxisAlignedBB(x0 / (double)g, y0 / (double)g, z0 / (double)g, x1 / (double)g, y1 / (double)g, z1 / (double)g));
		}
		return Collections.unmodifiableList(out);
	}

	static Model load(String name)
	{
		try(InputStream in = MachineBounds.class.getResourceAsStream("/assets/immersivenuclear/bounds/" + name + ".json"))
		{
			if(in == null)
				return MISSING;
			JsonObject json = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			Map<Long, int[][]> cells = new HashMap<>();
			for(Map.Entry<String, JsonElement> cell : json.getAsJsonObject("cells").entrySet())
			{
				String[] k = cell.getKey().split(",");
				JsonArray list = cell.getValue().getAsJsonArray();
				int[][] boxes = new int[list.size()][6];
				for(int i = 0; i < list.size(); i++)
					for(int j = 0; j < 6; j++)
						boxes[i][j] = list.get(i).getAsJsonArray().get(j).getAsInt();
				cells.put(key(Integer.parseInt(k[0]), Integer.parseInt(k[1]), Integer.parseInt(k[2])), boxes);
			}
			return new Model(json.get("grid").getAsInt(), cells);
		}
		catch(Exception e)
		{
			return MISSING;
		}
	}
}
