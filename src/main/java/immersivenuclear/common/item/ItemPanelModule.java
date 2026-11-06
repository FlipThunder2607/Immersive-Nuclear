package immersivenuclear.common.item;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A module of the computers' panels (the operator's console and the wall panel): commands go on the console's sloped panel, indicators on its vertical panel and on the wall panels,
 * and the text and colour modules on any of them. Each takes a rectangle of cells of the panel's 16 x 16 grid. For now it is only an item (the user, 04/11/2026): no function yet.
 */
public class ItemPanelModule extends Item
{
	public static final int COMMAND = 0, INDICATOR = 1, UNIVERSAL = 2;
	private static final String[] KINDS = {"command", "indicator", "universal"};

	public final int kind, cellsWide, cellsHigh;

	public ItemPanelModule(String id, int kind, int cellsWide, int cellsHigh)
	{
		this.kind = kind;
		this.cellsWide = cellsWide;
		this.cellsHigh = cellsHigh;
		setRegistryName(ImmersiveNuclear.MODID, "panel_module_" + id);
		setTranslationKey(ImmersiveNuclear.MODID + ".panel_module_" + id);
		setCreativeTab(INContent.TAB);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag)
	{
		tooltip.add("§7" + I18n.format("tooltip.immersivenuclear.panel_module." + KINDS[kind]));
		tooltip.add("§8" + I18n.format("tooltip.immersivenuclear.panel_module.cells", cellsWide, cellsHigh));
		tooltip.add("§8" + I18n.format("tooltip.immersivenuclear.panel_module.todo"));
	}
}
