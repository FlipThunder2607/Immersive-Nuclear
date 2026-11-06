package immersivenuclear.common.multiblock;

import java.util.LinkedHashMap;
import java.util.Map;

import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.api.MultiblockHandler;
import blusunrize.immersiveengineering.api.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.api.crafting.IngredientStack;
import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.blocks.BlockTypes_MetalsAll;
import blusunrize.immersiveengineering.common.blocks.BlockTypes_MetalsIE;
import blusunrize.immersiveengineering.common.blocks.metal.BlockTypes_MetalDecoration0;
import blusunrize.immersiveengineering.common.blocks.metal.BlockTypes_MetalDevice0;
import immersivenuclear.common.INContent;
import immersivenuclear.common.block.TileComputerMachine;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Hammer formation of a computer multiblock (see {@link ComputerStructure}). */
public class MultiblockComputer implements IMultiblock
{
	/** One per structure, in the same order. */
	public static final MultiblockComputer[] ALL = new MultiblockComputer[ComputerStructure.ALL.length];

	static
	{
		for(ComputerStructure s : ComputerStructure.ALL)
			ALL[s.index] = new MultiblockComputer(s);
	}

	public final ComputerStructure structure;
	private ItemStack[][][] manualStructure;
	private IngredientStack[] materials;

	private MultiblockComputer(ComputerStructure structure)
	{
		this.structure = structure;
	}

	/** The block (as an item) that a structure code stands for. Empty for air. */
	public static ItemStack stackFor(char c)
	{
		switch(c)
		{
			case 'S': return new ItemStack(IEContent.blockSheetmetal, 1, BlockTypes_MetalsAll.STEEL.getMeta());
			case 's': return new ItemStack(IEContent.blockSheetmetalSlabs, 1, BlockTypes_MetalsAll.STEEL.getMeta());
			case 'T': return new ItemStack(IEContent.blockStorage, 1, BlockTypes_MetalsIE.STEEL.getMeta());
			case 'C': return new ItemStack(INContent.blockEngineering[2]);
			case 'r': return new ItemStack(IEContent.blockMetalDecoration0, 1, BlockTypes_MetalDecoration0.RADIATOR.getMeta());
			case 'R': return new ItemStack(IEContent.blockMetalDecoration0, 1, BlockTypes_MetalDecoration0.RS_ENGINEERING.getMeta());
			case 'L': return new ItemStack(IEContent.blockMetalDecoration0, 1, BlockTypes_MetalDecoration0.LIGHT_ENGINEERING.getMeta());
			case 'D': return new ItemStack(INContent.blockDataCoil);
			case 'P': return new ItemStack(IEContent.blockMetalDevice0, 1, BlockTypes_MetalDevice0.CAPACITOR_HV.getMeta());
			default: return ItemStack.EMPTY;
		}
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

	/** World position of a template offset, for a structure whose offset 0,0,0 is at {@code origin}. */
	public static BlockPos toWorld(BlockPos origin, EnumFacing facing, int dx, int dy, int dz)
	{
		int[] w = ComputerStructure.rotate(dx, dz, turns(facing));
		return origin.add(w[0], dy, w[1]);
	}

	@Override
	public String getUniqueName()
	{
		return structure.uniqueName;
	}

	@Override
	public boolean isBlockTrigger(IBlockState state)
	{
		IBlockState trigger = stateFor(structure.trigger);
		return state.getBlock() == trigger.getBlock() && state.getBlock().getMetaFromState(state) == trigger.getBlock().getMetaFromState(trigger);
	}

	/** Any trigger block of the structure takes the hammer: every place and way round it could stand in is tried, starting with its front on the side hit (it decides for a build that looks the same from every side). */
	@Override
	public boolean createStructure(World world, BlockPos pos, EnumFacing side, EntityPlayer player)
	{
		EnumFacing front = side.getAxis().isHorizontal() ? side : player.getHorizontalFacing().getOpposite();
		int first = front.rotateY().getHorizontalIndex();    // the reference orientation (NORTH) has its front to the west
		for(int i = 0; i < 4; i++)
		{
			EnumFacing facing = EnumFacing.HORIZONTALS[(first + i) & 3];
			for(int dy = 0; dy <= structure.maxDy; dy++)
				for(int dz = 0; dz <= structure.maxDz; dz++)
					for(int dx = 0; dx <= structure.maxDx; dx++)
					{
						if(structure.get(dx, dy, dz) != structure.trigger)
							continue;
						int[] w = ComputerStructure.rotate(dx, dz, turns(facing));
						BlockPos origin = pos.add(-w[0], -dy, -w[1]);
						if(structureCheck(world, origin, facing))
							return form(world, origin, facing, pos, player);
					}
		}
		return false;
	}

	private boolean form(World world, BlockPos origin, EnumFacing facing, BlockPos hit, EntityPlayer player)
	{
		// The client only confirms the hammer use; the server places the parts and syncs them
		if(world.isRemote)
			return true;
		ItemStack hammer = player.getHeldItemMainhand().getItem().getToolClasses(player.getHeldItemMainhand()).contains(Lib.TOOL_HAMMER) ? player.getHeldItemMainhand() : player.getHeldItemOffhand();
		if(MultiblockHandler.fireMultiblockFormationEventPost(player, this, hit, hammer).isCanceled())
			return false;
		Block block = INContent.blockComputerMachines[structure.index];
		for(int dy = 0; dy <= structure.maxDy; dy++)
			for(int dz = 0; dz <= structure.maxDz; dz++)
				for(int dx = 0; dx <= structure.maxDx; dx++)
				{
					BlockPos partPos = toWorld(origin, facing, dx, dy, dz);
					// Keep the replaced block exactly, to put it back on disassembly
					IBlockState original = world.getBlockState(partPos);
					TileEntity originalTile = world.getTileEntity(partPos);
					NBTTagCompound originalNbt = originalTile != null ? originalTile.writeToNBT(new NBTTagCompound()) : null;
					world.removeTileEntity(partPos);
					IBlockState formed = block.getDefaultState();
					world.setBlockState(partPos, formed);
					TileEntity te = world.getTileEntity(partPos);
					if(te instanceof TileComputerMachine)
					{
						TileComputerMachine part = (TileComputerMachine)te;
						part.setPart(structure, dx, dy, dz, facing);
						part.setOriginal(original, originalNbt);
						world.notifyBlockUpdate(partPos, formed, formed, 3);
					}
				}
		return true;
	}

	boolean structureCheck(World world, BlockPos origin, EnumFacing facing)
	{
		for(int dy = 0; dy <= structure.maxDy; dy++)
			for(int dz = 0; dz <= structure.maxDz; dz++)
				for(int dx = 0; dx <= structure.maxDx; dx++)
				{
					IBlockState expected = stateFor(structure.get(dx, dy, dz));
					IBlockState actual = world.getBlockState(toWorld(origin, facing, dx, dy, dz));
					if(actual.getBlock() != expected.getBlock() || actual.getBlock().getMetaFromState(actual) != expected.getBlock().getMetaFromState(expected))
						return false;
				}
		return true;
	}

	/** Puts the original blocks back, except at {@code broken} (the part being removed). Only parts still formed are restored, so a partly broken structure does not duplicate blocks. */
	public void disassemble(World world, BlockPos origin, EnumFacing facing, BlockPos broken)
	{
		Block block = INContent.blockComputerMachines[structure.index];
		for(int dy = 0; dy <= structure.maxDy; dy++)
			for(int dz = 0; dz <= structure.maxDz; dz++)
				for(int dx = 0; dx <= structure.maxDx; dx++)
				{
					BlockPos partPos = toWorld(origin, facing, dx, dy, dz);
					if(partPos.equals(broken) || world.getBlockState(partPos).getBlock() != block)
						continue;
					TileEntity te = world.getTileEntity(partPos);
					IBlockState original = te instanceof TileComputerMachine ? ((TileComputerMachine)te).getOriginalState() : null;
					NBTTagCompound originalNbt = te instanceof TileComputerMachine ? ((TileComputerMachine)te).getOriginalNbt() : null;
					if(original == null)
						original = stateFor(structure.get(dx, dy, dz));
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

	/** structure[h][l][w]: h = layer from the bottom, l = row (dz), w = column (dx). */
	@Override
	public ItemStack[][][] getStructureManual()
	{
		if(manualStructure == null)
		{
			manualStructure = new ItemStack[structure.maxDy + 1][structure.maxDz + 1][structure.maxDx + 1];
			for(int dy = 0; dy <= structure.maxDy; dy++)
				for(int dz = 0; dz <= structure.maxDz; dz++)
					for(int dx = 0; dx <= structure.maxDx; dx++)
						manualStructure[dy][dz][structure.maxDx - dx] = stackFor(structure.get(dx, dy, dz));
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

	@Override
	public IngredientStack[] getTotalMaterials()
	{
		if(materials == null)
		{
			Map<Character, Integer> counts = new LinkedHashMap<>();
			for(char c : "STCDrRL".toCharArray())
				counts.put(c, structure.count(c));
			counts.values().removeIf(v -> v <= 0);
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
		return 12;
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
		immersivenuclear.client.FormedPreview.render(new ItemStack(INContent.itemComputerMachines[structure.index]), structure.masterDx, structure.masterDy, structure.masterDz, structure.maxDz + 1);
	}
}
