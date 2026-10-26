package immersivenuclear.common;

import immersivenuclear.client.GuiChemicalProcessor;
import immersivenuclear.client.GuiChemicalSeparator;
import immersivenuclear.client.GuiFilterPress;
import immersivenuclear.client.GuiMetalExtractor;
import immersivenuclear.client.GuiPrecipitationUnit;
import immersivenuclear.common.block.TileChemicalProcessor;
import immersivenuclear.common.block.TileChemicalSeparator;
import immersivenuclear.common.block.TileFilterPress;
import immersivenuclear.common.block.TileMetalExtractor;
import immersivenuclear.common.block.TilePrecipitationUnit;
import immersivenuclear.common.gui.ContainerChemicalProcessor;
import immersivenuclear.common.gui.ContainerChemicalSeparator;
import immersivenuclear.common.gui.ContainerFilterPress;
import immersivenuclear.common.gui.ContainerMetalExtractor;
import immersivenuclear.common.gui.ContainerPrecipitationUnit;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

/** Opened with the master's position; the client class is only touched on the client. */
public class INGuiHandler implements IGuiHandler
{
	public static final int CHEMICAL_PROCESSOR = 0, FILTER_PRESS = 1, CHEMICAL_SEPARATOR = 2, METAL_EXTRACTOR = 3, PRECIPITATION_UNIT = 4;

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

	static TileChemicalSeparator separator(World world, int x, int y, int z)
	{
		TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
		return te instanceof TileChemicalSeparator && ((TileChemicalSeparator)te).isMaster() ? (TileChemicalSeparator)te : null;
	}

	static TileMetalExtractor extractor(World world, int x, int y, int z)
	{
		TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
		return te instanceof TileMetalExtractor && ((TileMetalExtractor)te).isMaster() ? (TileMetalExtractor)te : null;
	}

	static TilePrecipitationUnit precipitation(World world, int x, int y, int z)
	{
		TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
		return te instanceof TilePrecipitationUnit && ((TilePrecipitationUnit)te).isMaster() ? (TilePrecipitationUnit)te : null;
	}

	@Override
	public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z)
	{
		if(id == PRECIPITATION_UNIT)
		{
			TilePrecipitationUnit p = precipitation(world, x, y, z);
			return p == null ? null : new ContainerPrecipitationUnit(player.inventory, p);
		}
		if(id == METAL_EXTRACTOR)
		{
			TileMetalExtractor e = extractor(world, x, y, z);
			return e == null ? null : new ContainerMetalExtractor(player.inventory, e);
		}
		if(id == CHEMICAL_SEPARATOR)
		{
			TileChemicalSeparator s = separator(world, x, y, z);
			return s == null ? null : new ContainerChemicalSeparator(player.inventory, s);
		}
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
		if(id == PRECIPITATION_UNIT)
		{
			TilePrecipitationUnit p = precipitation(world, x, y, z);
			return p == null ? null : new GuiPrecipitationUnit(new ContainerPrecipitationUnit(player.inventory, p));
		}
		if(id == METAL_EXTRACTOR)
		{
			TileMetalExtractor e = extractor(world, x, y, z);
			return e == null ? null : new GuiMetalExtractor(new ContainerMetalExtractor(player.inventory, e));
		}
		if(id == CHEMICAL_SEPARATOR)
		{
			TileChemicalSeparator s = separator(world, x, y, z);
			return s == null ? null : new GuiChemicalSeparator(new ContainerChemicalSeparator(player.inventory, s));
		}
		if(id == FILTER_PRESS)
		{
			TileFilterPress f = filterPress(world, x, y, z);
			return f == null ? null : new GuiFilterPress(new ContainerFilterPress(player.inventory, f));
		}
		TileChemicalProcessor m = id == CHEMICAL_PROCESSOR ? master(world, x, y, z) : null;
		return m == null ? null : new GuiChemicalProcessor(new ContainerChemicalProcessor(player.inventory, m));
	}
}
