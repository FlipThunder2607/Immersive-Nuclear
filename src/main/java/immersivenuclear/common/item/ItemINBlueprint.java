package immersivenuclear.common.item;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import immersivenuclear.common.INWorkbenchRecipe;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A sepia blueprint (15/11/2026): the family of items one can make. Its tooltip as IE's blueprint's (the user): where it works,
 * then "Created Items: Hold Shift", and with shift the items it makes.
 */
public class ItemINBlueprint extends Item
{
	public final String family;
	public final boolean bench;

	public ItemINBlueprint(String family, boolean bench)
	{
		this.family = family;
		this.bench = bench;
		setRegistryName(ImmersiveNuclear.MODID, "blueprint_" + family);
		setTranslationKey(ImmersiveNuclear.MODID + ".blueprint_" + family);
		setCreativeTab(INContent.TAB);
		setMaxStackSize(1);
	}

	/** What only the Modular Assembly Station makes with this blueprint (its recipes come with the station). */
	List<ItemStack> stationItems()
	{
		List<ItemStack> l = new java.util.ArrayList<>();
		Item[] parts = INContent.itemComputerParts;
		switch(family)
		{
			case "computer_modules":
				l.add(new ItemStack(parts[2]));                     // the core memory
				break;
			case "media":
				l.add(new ItemStack(parts[5]));                     // disk pack
				l.add(new ItemStack(parts[8]));                     // magnetic tape
				break;
			case "panel_commands":
			case "panel_indicators":
			case "panel_text":
				// the console's modules, by kind: commands, indicators, text and colour (ItemPanelModule.kind)
				int kind = family.equals("panel_commands") ? 0 : family.equals("panel_indicators") ? 1 : 2;
				for(Item p : INContent.itemPanelModules)
					if(((immersivenuclear.common.item.ItemPanelModule)p).kind == kind)
						l.add(new ItemStack(p));
				break;
			case "fuel_assembly":
				l.add(new ItemStack(immersivenuclear.common.INFuel.itemFuelRod));
				l.add(new ItemStack(immersivenuclear.common.INFuel.itemFuelElement));
				break;
			default:
				break;
		}
		return l;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag)
	{
		tooltip.add("§f" + I18n.format(bench ? "desc.immersivenuclear.blueprint.bench" : "desc.immersivenuclear.blueprint.station"));
		List<INWorkbenchRecipe> recipes = INWorkbenchRecipe.of(this);
		List<ItemStack> station = stationItems();
		if(recipes.isEmpty() && station.isEmpty())
			return;
		if(!GuiScreen.isShiftKeyDown())
		{
			tooltip.add("§f" + I18n.format("desc.immersivenuclear.blueprint.creates", "§6" + I18n.format("desc.immersivenuclear.blueprint.shift")));
			return;
		}
		tooltip.add("§f" + I18n.format("desc.immersivenuclear.blueprint.creates", ""));
		for(INWorkbenchRecipe r : recipes)
			tooltip.add("§7 " + (r.output.getCount() > 1 ? r.output.getCount() + "x " : "") + r.output.getDisplayName());
		for(ItemStack s : station)
			tooltip.add("§7 " + s.getDisplayName() + (bench ? " §7(" + I18n.format("desc.immersivenuclear.blueprint.station_short") + ")" : ""));
		if(recipes.isEmpty())
			tooltip.add("§e" + I18n.format("desc.immersivenuclear.blueprint.later"));
	}
}
