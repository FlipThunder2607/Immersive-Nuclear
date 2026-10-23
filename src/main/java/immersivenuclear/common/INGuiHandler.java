package immersivenuclear.common;

import immersivenuclear.client.GuiChemicalProcessor;
import immersivenuclear.client.GuiFilterPress;
import immersivenuclear.common.block.TileChemicalProcessor;
import immersivenuclear.common.block.TileFilterPress;
import immersivenuclear.common.gui.ContainerChemicalProcessor;
import immersivenuclear.common.gui.ContainerFilterPress;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

/** Opened with the master's position; the client class is only touched on the client. */
public class INGuiHandler implements IGuiHandler
{
	public static final int CHEMICAL_PROCESSOR = 0, FILTER_PRESS = 1;

	static TileChemicalProcessor master(World world, int x, int y, int z)
	{
		TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
		return te instanceof TileChemicalProcessor && ((TileChemicalProcessor)te).isMaster() ? (TileChemicalProcessor)te : null;
	}

	static TileFilterPress filterPress(World world, int x, int y, int z)
	{
		TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
		return te instanceof TileFilterPress && ((TileFilterPress)te).isMaster() ? (TileFilterPress)te : null;
	}

	@Override
	public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z)
	{
		if(id == FILTER_PRESS)
		{
			TileFilterPress f = filterPress(world, x, y, z);
			return f == null ? null : new ContainerFilterPress(player.inventory, f);
		}
		TileChemicalProcessor m = id == CHEMICAL_PROCESSOR ? master(world, x, y, z) : null;
		return m == null ? null : new ContainerChemicalProcessor(player.inventory, m);
	}

	@Override
	public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z)
	{
		if(id == FILTER_PRESS)
		{
			TileFilterPress f = filterPress(world, x, y, z);
			return f == null ? null : new GuiFilterPress(new ContainerFilterPress(player.inventory, f));
		}
		TileChemicalProcessor m = id == CHEMICAL_PROCESSOR ? master(world, x, y, z) : null;
		return m == null ? null : new GuiChemicalProcessor(new ContainerChemicalProcessor(player.inventory, m));
	}
}
