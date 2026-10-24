package immersivenuclear.common.multiblock;

import static immersivenuclear.common.multiblock.ChemicalSeparatorStructure.*;

import java.util.LinkedHashMap;
import java.util.Map;

import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.api.MultiblockHandler;
import blusunrize.immersiveengineering.api.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.api.crafting.IngredientStack;
import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.blocks.BlockTypes_MetalsAll;
import blusunrize.immersiveengineering.common.blocks.TileEntityIESlab;
import blusunrize.immersiveengineering.common.blocks.metal.BlockTypes_MetalDecoration0;
import blusunrize.immersiveengineering.common.blocks.metal.BlockTypes_MetalDecoration1;
import blusunrize.immersiveengineering.common.blocks.metal.BlockTypes_MetalDevice0;
import blusunrize.immersiveengineering.common.blocks.metal.BlockTypes_MetalDevice1;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileChemicalSeparator;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Hammer formation of the Chemical Separation Unit (see {@link ChemicalSeparatorStructure}). */
public class MultiblockChemicalSeparator implements IMultiblock
{
	public static final MultiblockChemicalSeparator instance = new MultiblockChemicalSeparator();

	/** The block (as an item) that a structure code stands for. Empty for air. */
	public static ItemStack stackFor(char c)
	{
		switch(c)
		{
			case 's': return new ItemStack(IEContent.blockMetalDecoration1, 1, BlockTypes_MetalDecoration1.STEEL_SCAFFOLDING_0.getMeta());
			case 'l': return new ItemStack(IEContent.blockMetalDecoration0, 1, BlockTypes_MetalDecoration0.LIGHT_ENGINEERING.getMeta());
			case 'H': return new ItemStack(IEContent.blockMetalDecoration0, 1, BlockTypes_MetalDecoration0.HEAVY_ENGINEERING.getMeta());
			case 'R': return new ItemStack(IEContent.blockMetalDecoration0, 1, BlockTypes_MetalDecoration0.RS_ENGINEERING.getMeta());
			case 'G': return new ItemStack(IEContent.blockMetalDecoration0, 1, BlockTypes_MetalDecoration0.GENERATOR.getMeta());
			case 'S': return new ItemStack(IEContent.blockSheetmetal, 1, BlockTypes_MetalsAll.STEEL.getMeta());
			case 'b': return new ItemStack(IEContent.blockSheetmetalSlabs, 1, BlockTypes_MetalsAll.STEEL.getMeta());
			case 'B': return new ItemStack(IEContent.blockMetalDevice0, 1, BlockTypes_MetalDevice0.BARREL.getMeta());
			case 'P': return new ItemStack(IEContent.blockMetalDevice0, 1, BlockTypes_MetalDevice0.FLUID_PUMP.getMeta());
			case 'p': return new ItemStack(IEContent.blockMetalDevice1, 1, BlockTypes_MetalDevice1.FLUID_PIPE.getMeta());
			default: return ItemStack.EMPTY;
		}
	}

	/** Required slab half: 0 = lower, -1 = not a slab. Matches IE's TileEntityIESlab.slabType. */
	static int slabTypeFor(char c)
	{
		return c == 'b' ? 0 : -1;
	}

	/** Each pump is one item placed as two blocks (the upper one is IE's dummy half). */
	static boolean isPumpTop(int dx, int dy, int dz)
	{
		return get(dx, dy, dz) == 'P' && get(dx, dy - 1, dz) == 'P';
	}

	/** The block state a structure code stands for, or null for air. */
	public static IBlockState stateFor(char c)
	{
		ItemStack stack = stackFor(c);
		if(stack.isEmpty())
			return null;
		return Block.getBlockFromItem(stack.getItem()).getStateFromMeta(stack.getMetadata());
	}

	/** Clockwise quarter turns from the reference orientation (facing NORTH). */
	public static int turns(EnumFacing facing)
	{
		return (facing.getHorizontalIndex() + 2) & 3;
	}

	/** World position of a template offset, for a structure triggered at {@code trigger}. */
	public static BlockPos toWorld(BlockPos trigger, EnumFacing facing, boolean mirror, int dx, int dy, int dz)
	{
		int[] w = rotate(dx, dz, turns(facing), mirror);
		return trigger.add(w[0], dy, w[1]);
	}

	@Override
	public String getUniqueName()
	{
		return "IN:ChemicalSeparator";
	}

	@Override
	public boolean isBlockTrigger(IBlockState state)
	{
		return state.getBlock() == IEContent.blockMetalDecoration0 && state.getBlock().getMetaFromState(state) == BlockTypes_MetalDecoration0.RS_ENGINEERING.getMeta();
	}

	@Override
	public boolean createStructure(World world, BlockPos pos, EnumFacing side, EntityPlayer player)
	{
		// The front is the trigger's WEST face in the reference orientation (facing NORTH)
		if(side.getAxis() == EnumFacing.Axis.Y)
			side = player.getHorizontalFacing().getOpposite();
		EnumFacing facing = side.rotateY();

		boolean mirror = false;
		boolean ok = structureCheck(world, pos, facing, false);
		if(!ok)
		{
			mirror = true;
			ok = structureCheck(world, pos, facing, true);
		}
		if(!ok)
			return false;
		// The client only confirms the hammer use; the server places the parts and syncs them
		if(world.isRemote)
			return true;

		ItemStack hammer = player.getHeldItemMainhand().getItem().getToolClasses(player.getHeldItemMainhand()).contains(Lib.TOOL_HAMMER) ? player.getHeldItemMainhand() : player.getHeldItemOffhand();
		if(MultiblockHandler.fireMultiblockFormationEventPost(player, this, pos, hammer).isCanceled())
			return false;

		for(int dy = MIN_DY; dy <= MAX_DY; dy++)
			for(int dz = MIN_DZ; dz <= MAX_DZ; dz++)
				for(int dx = MIN_DX; dx <= MAX_DX; dx++)
				{
					if(!isPart(dx, dy, dz))
						continue;
					BlockPos partPos = toWorld(pos, facing, mirror, dx, dy, dz);
					// Keep the replaced block exactly (slab half, pump half, barrel contents) to put it back on disassembly
					IBlockState original = world.getBlockState(partPos);
					TileEntity originalTile = world.getTileEntity(partPos);
					NBTTagCompound originalNbt = originalTile != null ? originalTile.writeToNBT(new NBTTagCompound()) : null;
					// Without this, the barrels would spill their contents and the pumps would break their other half
					world.removeTileEntity(partPos);
					IBlockState formed = INContent.blockChemicalSeparator.getDefaultState();
					world.setBlockState(partPos, formed);
					TileEntity te = world.getTileEntity(partPos);
					if(te instanceof TileChemicalSeparator)
					{
						TileChemicalSeparator part = (TileChemicalSeparator)te;
						part.setPart(dx, dy, dz, facing, mirror);
						part.setOriginal(original, originalNbt);
						world.notifyBlockUpdate(partPos, formed, formed, 3);
					}
				}
		return true;
	}

	boolean structureCheck(World world, BlockPos trigger, EnumFacing facing, boolean mirror)
	{
		for(int dy = MIN_DY; dy <= MAX_DY; dy++)
			for(int dz = MIN_DZ; dz <= MAX_DZ; dz++)
				for(int dx = MIN_DX; dx <= MAX_DX; dx++)
				{
					char c = get(dx, dy, dz);
					if(c == '.')
						continue;
					IBlockState expected = stateFor(c);
					BlockPos partPos = toWorld(trigger, facing, mirror, dx, dy, dz);
					IBlockState actual = world.getBlockState(partPos);
					if(actual.getBlock() != expected.getBlock() || actual.getBlock().getMetaFromState(actual) != expected.getBlock().getMetaFromState(expected))
						return false;
					int slab = slabTypeFor(c);
					if(slab >= 0)
					{
						TileEntity te = world.getTileEntity(partPos);
						if(!(te instanceof TileEntityIESlab) || ((TileEntityIESlab)te).slabType != slab)
							return false;
					}
				}
		return true;
	}

	/**
	 * Puts the original blocks back, except at {@code broken} (the part being removed).
	 * Only parts still formed are restored, so a partly broken structure does not duplicate blocks. When the broken
	 * part is half of a pump, the other half of that pump is cleared too, since the broken half already drops it.
	 */
	public static void disassemble(World world, BlockPos trigger, EnumFacing facing, boolean mirror, BlockPos broken)
	{
		BlockPos pumpOther = null;
		for(int dy = MIN_DY; dy <= MAX_DY; dy++)
			for(int dz = MIN_DZ; dz <= MAX_DZ; dz++)
				for(int dx = MIN_DX; dx <= MAX_DX; dx++)
					if(isPumpTop(dx, dy, dz))
					{
						BlockPos top = toWorld(trigger, facing, mirror, dx, dy, dz);
						if(broken.equals(top))
							pumpOther = top.down();
						else if(broken.equals(top.down()))
							pumpOther = top;
					}

		for(int dy = MIN_DY; dy <= MAX_DY; dy++)
			for(int dz = MIN_DZ; dz <= MAX_DZ; dz++)
				for(int dx = MIN_DX; dx <= MAX_DX; dx++)
				{
					if(!isPart(dx, dy, dz))
						continue;
					BlockPos partPos = toWorld(trigger, facing, mirror, dx, dy, dz);
					if(partPos.equals(broken) || world.getBlockState(partPos).getBlock() != INContent.blockChemicalSeparator)
						continue;
					if(partPos.equals(pumpOther))
					{
						world.setBlockState(partPos, Blocks.AIR.getDefaultState());
						continue;
					}
					TileEntity te = world.getTileEntity(partPos);
					IBlockState original = te instanceof TileChemicalSeparator ? ((TileChemicalSeparator)te).getOriginalState() : null;
					NBTTagCompound originalNbt = te instanceof TileChemicalSeparator ? ((TileChemicalSeparator)te).getOriginalNbt() : null;
					if(original == null)
						original = stateFor(get(dx, dy, dz));
					world.setBlockState(partPos, original);
					if(originalNbt != null)
					{
						TileEntity restored = world.getTileEntity(partPos);
						if(restored != null)
						{
							NBTTagCompound nbt = originalNbt.copy();
							nbt.setInteger("x", partPos.getX());
							nbt.setInteger("y", partPos.getY());
							nbt.setInteger("z", partPos.getZ());
							restored.readFromNBT(nbt);
							restored.markDirty();
							world.notifyBlockUpdate(partPos, original, original, 3);
						}
					}
				}
	}

	static ItemStack[][][] manualStructure;

	/** structure[h][l][w]: h = layer from the bottom, l = row (dz), w = column (dx). Each pump shows once. */
	@Override
	public ItemStack[][][] getStructureManual()
	{
		if(manualStructure == null)
		{
			manualStructure = new ItemStack[MAX_DY - MIN_DY + 1][MAX_DZ - MIN_DZ + 1][MAX_DX - MIN_DX + 1];
			for(int dy = MIN_DY; dy <= MAX_DY; dy++)
				for(int dz = MIN_DZ; dz <= MAX_DZ; dz++)
					for(int dx = MIN_DX; dx <= MAX_DX; dx++)
						manualStructure[dy - MIN_DY][dz - MIN_DZ][dx - MIN_DX] = isPumpTop(dx, dy, dz) ? ItemStack.EMPTY : stackFor(get(dx, dy, dz));
		}
		return manualStructure;
	}

	@Override
	public IBlockState getBlockstateFromStack(int index, ItemStack stack)
	{
		if(!stack.isEmpty() && stack.getItem() instanceof ItemBlock)
			return ((ItemBlock)stack.getItem()).getBlock().getStateFromMeta(stack.getItemDamage());
		return null;
	}

	static IngredientStack[] materials;

	@Override
	public IngredientStack[] getTotalMaterials()
	{
		if(materials == null)
		{
			Map<Character, Integer> counts = new LinkedHashMap<>();
			for(char c : "sBplbSHGPR".toCharArray())
				counts.put(c, c == 'P' ? count(c) / 2 : count(c));
			materials = new IngredientStack[counts.size()];
			int i = 0;
			for(Map.Entry<Character, Integer> e : counts.entrySet())
			{
				ItemStack s = stackFor(e.getKey()).copy();
				s.setCount(e.getValue());
				materials[i++] = new IngredientStack(s);
			}
		}
		return materials;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean overwriteBlockRender(ItemStack stack, int iterator)
	{
		return false;
	}

	@Override
	public float getManualScale()
	{
		return 10;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean canRenderFormedStructure()
	{
		return true;
	}

	/** The formed machine in IE's manual and the projector's preview, as our 3D item model. */
	@Override
	@SideOnly(Side.CLIENT)
	public void renderFormedStructure()
	{
		immersivenuclear.client.FormedPreview.render(new ItemStack(INContent.itemChemicalSeparator), MASTER_DX - MIN_DX, MASTER_DY - MIN_DY, MASTER_DZ - MIN_DZ, MAX_DX - MIN_DX + 1);
	}
}
