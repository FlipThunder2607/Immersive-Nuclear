package immersivenuclear.client;

import java.util.HashMap;
import java.util.Map;

import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.INContent;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Hazard notes under the name of our items and of any container holding our fluids (bucket, IE canister...). Only
 * a warning: no game effect exists yet (radiation and acid come with a later stage, see the catalogue).
 */
@Mod.EventBusSubscriber(modid = ImmersiveNuclear.MODID, value = Side.CLIENT)
public class HazardTooltips
{
	static final String RAD = "radioactive", TRACE = "radioactive_traces", CORR = "corrosive", FIRE = "flammable",
			TOXIC = "toxic", OXID = "oxidizer", CAUSTIC = "caustic";
	static Map<Item, String[]> items;
	static Map<String, String[]> fluids;

	static void build()
	{
		items = new HashMap<>();
		items.put(INContent.itemReagent, new String[]{OXID});
		items.put(INContent.itemResidue, new String[]{RAD, CORR});
		items.put(INContent.itemNeutralizedResidue, new String[]{RAD});
		items.put(INContent.itemQuicklime, new String[]{CAUSTIC});
		items.put(INContent.itemCake, new String[]{TRACE});
		items.put(INContent.itemMixedDeposit, new String[]{TRACE});
		items.put(INContent.itemAgglomerate, new String[]{TRACE});
		items.put(INContent.itemWetConcentrate, new String[]{RAD, TOXIC});
		items.put(INContent.itemYellowcake, new String[]{RAD, TOXIC});
		fluids = new HashMap<>();
		fluids.put(INContent.fluidSlurry.getName(), new String[]{CORR, RAD});
		fluids.put(INContent.fluidCrudeSolution.getName(), new String[]{CORR, RAD});
		fluids.put(INContent.fluidPurifiedSolution.getName(), new String[]{CORR, RAD});
		fluids.put(INContent.fluidEffluent.getName(), new String[]{CORR, TRACE});
		fluids.put(INContent.fluidDepletedEffluent.getName(), new String[]{CORR});
		fluids.put(INContent.fluidSolvent.getName(), new String[]{FIRE});
		fluids.put(INContent.fluidSludge.getName(), new String[]{TRACE});
	}

	@SubscribeEvent
	public static void onTooltip(ItemTooltipEvent event)
	{
		ItemStack stack = event.getItemStack();
		if(stack.isEmpty())
			return;
		if(items == null)
			build();
		String[] hazards = items.get(stack.getItem());
		if(hazards == null)
		{
			FluidStack fluid = FluidUtil.getFluidContained(stack);
			if(fluid != null)
				hazards = fluids.get(fluid.getFluid().getName());
		}
		if(hazards != null)
			for(String h : hazards)
				event.getToolTip().add(I18n.format("desc.immersivenuclear.hazard." + h));
	}
}
