package immersivenuclear.client.waila;

import immersivenuclear.common.INContent;
import immersivenuclear.common.block.*;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.IWailaRegistrar;
import mcp.mobius.waila.api.WailaPlugin;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Hwyla: a formed machine's part shows the machine (its name and its 3D model), while picking the block (the middle
 * button) gives the block the part replaced (the user, 01/11). Hwyla finds this class by its annotation; nothing else
 * refers to it, so the mod runs without Hwyla.
 */
@WailaPlugin
public class INWailaPlugin implements IWailaPlugin
{
	@Override
	public void register(IWailaRegistrar registrar)
	{
		machine(registrar, BlockChemicalProcessor.class, () -> INContent.itemChemicalProcessor);
		machine(registrar, BlockFilterPress.class, () -> INContent.itemFilterPress);
		machine(registrar, BlockChemicalSeparator.class, () -> INContent.itemChemicalSeparator);
		machine(registrar, BlockMetalExtractor.class, () -> INContent.itemMetalExtractor);
		machine(registrar, BlockPrecipitationUnit.class, () -> INContent.itemPrecipitationUnit);
		machine(registrar, BlockIndustrialDryer.class, () -> INContent.itemIndustrialDryer);
		machine(registrar, BlockSulfuricAcidUnit.class, () -> INContent.itemSulfuricAcidUnit);
		machine(registrar, BlockHFKiln.class, () -> INContent.itemHFKiln);
		machine(registrar, BlockElectrolyzer.class, () -> INContent.itemElectrolyzer);
	}

	static void machine(IWailaRegistrar registrar, Class<? extends Block> block, java.util.function.Supplier<Item> item)
	{
		registrar.registerStackProvider(new IWailaDataProvider()
		{
			@Override
			public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config)
			{
				return new ItemStack(item.get());
			}
		}, block);
	}
}
