package immersivenuclear.client;

import blusunrize.immersiveengineering.api.ManualHelper;
import blusunrize.immersiveengineering.api.ManualPageMultiblock;
import blusunrize.lib.manual.IManualPage;
import blusunrize.lib.manual.ManualInstance;
import blusunrize.lib.manual.ManualPages;
import immersivenuclear.common.INContent;
import immersivenuclear.common.multiblock.MultiblockChemicalProcessor;
import immersivenuclear.common.multiblock.MultiblockChemicalSeparator;
import immersivenuclear.common.multiblock.MultiblockFilterPress;
import immersivenuclear.common.multiblock.MultiblockMetalExtractor;
import immersivenuclear.common.multiblock.MultiblockPrecipitationUnit;
import net.minecraft.item.ItemStack;

/**
 * Our own category in IE's Engineer's Manual, laid out like IE's: one entry per machine (structure, then text, then
 * the recipe of the part it uses) and one per group of materials. IE builds the manual in its client postInit, so
 * this runs in ours. Texts: ie.manual.category.immersivenuclear.name and ie.manual.entry.&lt;entry&gt;[.name|.subtext|N]
 * in the lang files.
 */
public class ManualEntries
{
	public static final String CATEGORY = "immersivenuclear";

	static ManualInstance manual;

	public static void register()
	{
		manual = ManualHelper.getManual();
		ManualHelper.addEntry("in_chemical_processor", CATEGORY,
				new ManualPageMultiblock(manual, "in_chemical_processor0", MultiblockChemicalProcessor.instance),
				text("in_chemical_processor1"), text("in_chemical_processor2"),
				new ManualPages.Crafting(manual, "in_chemical_processor3", new ItemStack(INContent.itemReagent)));
		ManualHelper.addEntry("in_filter_press", CATEGORY,
				new ManualPageMultiblock(manual, "in_filter_press0", MultiblockFilterPress.instance),
				text("in_filter_press1"), text("in_filter_press2"),
				new ManualPages.Crafting(manual, "in_filter_press3", new ItemStack(INContent.itemFilter)));
		ManualHelper.addEntry("in_chemical_separator", CATEGORY,
				new ManualPageMultiblock(manual, "in_chemical_separator0", MultiblockChemicalSeparator.instance),
				text("in_chemical_separator1"), text("in_chemical_separator2"));
		ManualHelper.addEntry("in_metal_extractor", CATEGORY,
				new ManualPageMultiblock(manual, "in_metal_extractor0", MultiblockMetalExtractor.instance),
				text("in_metal_extractor1"), text("in_metal_extractor2"),
				new ManualPages.Crafting(manual, "in_metal_extractor3", new ItemStack(INContent.itemElectrodeSteel),
						new ItemStack(INContent.itemElectrodeLeadSilver), new ItemStack(INContent.itemElectrodeElectrum)));
		ManualHelper.addEntry("in_precipitation_unit", CATEGORY,
				new ManualPageMultiblock(manual, "in_precipitation_unit0", MultiblockPrecipitationUnit.instance),
				text("in_precipitation_unit1"), text("in_precipitation_unit2"),
				new ManualPages.Crafting(manual, "in_precipitation_unit3", new ItemStack(INContent.itemPrecipitatingAgent)));
		ManualHelper.addEntry("in_supplies", CATEGORY,
				text("in_supplies0"), text("in_supplies1"),
				new ManualPages.Crafting(manual, "in_supplies2", new ItemStack(INContent.itemPrecipitatingAgent)));
		ManualHelper.addEntry("in_byproducts", CATEGORY,
				text("in_byproducts0"), text("in_byproducts1"), text("in_byproducts2"),
				new ManualPages.Crafting(manual, "in_byproducts3", new ItemStack(INContent.itemNeutralizedResidue), new ItemStack(INContent.itemAgglomerate)));
		ManualHelper.addEntry("in_hazards", CATEGORY,
				text("in_hazards0"), text("in_hazards1"));
	}

	static IManualPage text(String key)
	{
		return new ManualPages.Text(manual, key);
	}
}
