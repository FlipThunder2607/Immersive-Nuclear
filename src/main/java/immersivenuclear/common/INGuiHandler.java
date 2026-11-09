package immersivenuclear.common;

import immersivenuclear.client.GuiChemicalProcessor;
import immersivenuclear.client.GuiChemicalSeparator;
import immersivenuclear.client.GuiConcentrateDrum;
import immersivenuclear.client.GuiFilterPress;
import immersivenuclear.client.GuiIndustrialDryer;
import immersivenuclear.client.GuiMetalExtractor;
import immersivenuclear.client.GuiPrecipitationUnit;
import immersivenuclear.client.GuiSulfuricAcidUnit;
import immersivenuclear.common.block.TileChemicalProcessor;
import immersivenuclear.common.block.TileChemicalSeparator;
import immersivenuclear.common.block.TileConcentrateDrum;
import immersivenuclear.common.block.TileFilterPress;
import immersivenuclear.common.block.TileIndustrialDryer;
import immersivenuclear.common.block.TileMetalExtractor;
import immersivenuclear.common.block.TilePrecipitationUnit;
import immersivenuclear.common.block.TileSulfuricAcidUnit;
import immersivenuclear.common.gui.ContainerChemicalProcessor;
import immersivenuclear.common.gui.ContainerChemicalSeparator;
import immersivenuclear.common.gui.ContainerConcentrateDrum;
import immersivenuclear.common.gui.ContainerFilterPress;
import immersivenuclear.common.gui.ContainerIndustrialDryer;
import immersivenuclear.common.gui.ContainerMetalExtractor;
import immersivenuclear.common.gui.ContainerPrecipitationUnit;
import immersivenuclear.common.gui.ContainerSulfuricAcidUnit;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

/** Opened with the master's position; the client class is only touched on the client. */
public class INGuiHandler implements IGuiHandler
{
	public static final int CHEMICAL_PROCESSOR = 0, FILTER_PRESS = 1, CHEMICAL_SEPARATOR = 2, METAL_EXTRACTOR = 3, PRECIPITATION_UNIT = 4, INDUSTRIAL_DRYER = 5, CONCENTRATE_DRUM = 6, SULFURIC_ACID_UNIT = 7, HF_KILN = 8, ELECTROLYZER = 9, CONVERSION_UNIT = 10, ENRICHMENT_UNIT = 11;

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

	static TileIndustrialDryer dryer(World world, int x, int y, int z)
	{
		TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
		return te instanceof TileIndustrialDryer && ((TileIndustrialDryer)te).isMaster() ? (TileIndustrialDryer)te : null;
	}

	static TileSulfuricAcidUnit acidUnit(World world, int x, int y, int z)
	{
		TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
		return te instanceof TileSulfuricAcidUnit && ((TileSulfuricAcidUnit)te).isMaster() ? (TileSulfuricAcidUnit)te : null;
	}

	static immersivenuclear.common.block.TileHFKiln kiln(World world, int x, int y, int z)
	{
		TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
		return te instanceof immersivenuclear.common.block.TileHFKiln && ((immersivenuclear.common.block.TileHFKiln)te).isMaster() ? (immersivenuclear.common.block.TileHFKiln)te : null;
	}

	static immersivenuclear.common.block.TileConversionUnit conversionUnit(World world, int x, int y, int z)
	{
		TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
		return te instanceof immersivenuclear.common.block.TileConversionUnit && ((immersivenuclear.common.block.TileConversionUnit)te).isMaster() ? (immersivenuclear.common.block.TileConversionUnit)te : null;
	}

	static immersivenuclear.common.block.TileEnrichmentBase enrichmentUnit(World world, int x, int y, int z)
	{
		TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
		return te instanceof immersivenuclear.common.block.TileEnrichmentBase && ((immersivenuclear.common.block.TileEnrichmentBase)te).isMaster() ? (immersivenuclear.common.block.TileEnrichmentBase)te : null;
	}

	static immersivenuclear.common.block.TileElectrolyzer electrolyzer(World world, int x, int y, int z)
	{
		TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
		return te instanceof immersivenuclear.common.block.TileElectrolyzer && ((immersivenuclear.common.block.TileElectrolyzer)te).isMaster() ? (immersivenuclear.common.block.TileElectrolyzer)te : null;
	}

	static TileConcentrateDrum drum(World world, int x, int y, int z)
	{
		TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
		return te instanceof TileConcentrateDrum ? (TileConcentrateDrum)te : null;
	}

	@Override
	public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z)
	{
		if(id == SULFURIC_ACID_UNIT)
		{
			TileSulfuricAcidUnit a = acidUnit(world, x, y, z);
			return a == null ? null : new ContainerSulfuricAcidUnit(player.inventory, a);
		}
		if(id == HF_KILN)
		{
			immersivenuclear.common.block.TileHFKiln k = kiln(world, x, y, z);
			return k == null ? null : new immersivenuclear.common.gui.ContainerHFKiln(player.inventory, k);
		}
		if(id == ELECTROLYZER)
		{
			immersivenuclear.common.block.TileElectrolyzer e = electrolyzer(world, x, y, z);
			return e == null ? null : new immersivenuclear.common.gui.ContainerElectrolyzer(player.inventory, e);
		}
		if(id == CONVERSION_UNIT)
		{
			immersivenuclear.common.block.TileConversionUnit c = conversionUnit(world, x, y, z);
			return c == null ? null : new immersivenuclear.common.gui.ContainerConversionUnit(player.inventory, c);
		}
		if(id == ENRICHMENT_UNIT)
		{
			immersivenuclear.common.block.TileEnrichmentBase e = enrichmentUnit(world, x, y, z);
			return e == null ? null : new immersivenuclear.common.gui.ContainerEnrichmentUnit(player.inventory, e);
		}
		if(id == CONCENTRATE_DRUM)
		{
			TileConcentrateDrum d = drum(world, x, y, z);
			return d == null ? null : new ContainerConcentrateDrum(player.inventory, d);
		}
		if(id == INDUSTRIAL_DRYER)
		{
			TileIndustrialDryer d = dryer(world, x, y, z);
			return d == null ? null : new ContainerIndustrialDryer(player.inventory, d);
		}
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
		if(id == SULFURIC_ACID_UNIT)
		{
			TileSulfuricAcidUnit a = acidUnit(world, x, y, z);
			return a == null ? null : new GuiSulfuricAcidUnit(new ContainerSulfuricAcidUnit(player.inventory, a));
		}
		if(id == HF_KILN)
		{
			immersivenuclear.common.block.TileHFKiln k = kiln(world, x, y, z);
			return k == null ? null : new immersivenuclear.client.GuiHFKiln(new immersivenuclear.common.gui.ContainerHFKiln(player.inventory, k));
		}
		if(id == ELECTROLYZER)
		{
			immersivenuclear.common.block.TileElectrolyzer e = electrolyzer(world, x, y, z);
			return e == null ? null : new immersivenuclear.client.GuiElectrolyzer(new immersivenuclear.common.gui.ContainerElectrolyzer(player.inventory, e));
		}
		if(id == CONVERSION_UNIT)
		{
			immersivenuclear.common.block.TileConversionUnit c = conversionUnit(world, x, y, z);
			return c == null ? null : new immersivenuclear.client.GuiConversionUnit(new immersivenuclear.common.gui.ContainerConversionUnit(player.inventory, c));
		}
		if(id == ENRICHMENT_UNIT)
		{
			immersivenuclear.common.block.TileEnrichmentBase e = enrichmentUnit(world, x, y, z);
			return e == null ? null : new immersivenuclear.client.GuiEnrichmentUnit(new immersivenuclear.common.gui.ContainerEnrichmentUnit(player.inventory, e));
		}
		if(id == CONCENTRATE_DRUM)
		{
			TileConcentrateDrum d = drum(world, x, y, z);
			return d == null ? null : new GuiConcentrateDrum(new ContainerConcentrateDrum(player.inventory, d));
		}
		if(id == INDUSTRIAL_DRYER)
		{
			TileIndustrialDryer d = dryer(world, x, y, z);
			return d == null ? null : new GuiIndustrialDryer(new ContainerIndustrialDryer(player.inventory, d));
		}
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
