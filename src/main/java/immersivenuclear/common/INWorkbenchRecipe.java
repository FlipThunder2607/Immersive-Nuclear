package immersivenuclear.common;

import blusunrize.immersiveengineering.api.crafting.IngredientStack;
import blusunrize.immersiveengineering.common.IEContent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * A recipe of the IN Workbench (15/11/2026): one item of a blueprint's family, made by hand from up to six materials. The
 * blueprint lists its family's recipes in order (the choices on the bench's work mat). The amounts are provisional; the bench
 * spends about 1.5 times what the Modular Assembly Station will (the station automates the same items, cheaper). The station's
 * machine modules use no electronic components of ours (only IE's and vanilla materials), so the player can start with them.
 */
public class INWorkbenchRecipe
{
	public static final int MAX_INPUTS = 6;
	/** Blueprint item -> its recipes, in the order shown. */
	public static final Map<Item, List<INWorkbenchRecipe>> BY_BLUEPRINT = new LinkedHashMap<>();

	public final Item blueprint;
	public final ItemStack output;
	public final IngredientStack[] inputs;

	public INWorkbenchRecipe(Item blueprint, ItemStack output, IngredientStack... inputs)
	{
		this.blueprint = blueprint;
		this.output = output;
		this.inputs = inputs;
	}

	public static List<INWorkbenchRecipe> of(Item blueprint)
	{
		List<INWorkbenchRecipe> l = BY_BLUEPRINT.get(blueprint);
		return l == null ? java.util.Collections.emptyList() : l;
	}

	static void add(Item blueprint, ItemStack output, Object... inputs)
	{
		IngredientStack[] in = new IngredientStack[inputs.length / 2];
		for(int i = 0; i < in.length; i++)
		{
			Object o = inputs[2 * i];
			int n = (Integer)inputs[2 * i + 1];
			in[i] = o instanceof String ? new IngredientStack((String)o, n) : new IngredientStack(copy((ItemStack)o, n));
		}
		BY_BLUEPRINT.computeIfAbsent(blueprint, k -> new ArrayList<>()).add(new INWorkbenchRecipe(blueprint, output, in));
	}

	static ItemStack copy(ItemStack s, int n)
	{
		ItemStack c = s.copy();
		c.setCount(n);
		return c;
	}

	/** Whether the materials hold every input (each counted over all the slots). */
	public boolean matches(IItemHandler materials, int first, int count)
	{
		for(IngredientStack in : inputs)
		{
			int have = 0;
			for(int s = first; s < first + count; s++)
			{
				ItemStack st = materials.getStackInSlot(s);
				if(!st.isEmpty() && in.matchesItemStackIgnoringSize(st))
					have += st.getCount();
			}
			if(have < in.inputSize)
				return false;
		}
		return true;
	}

	/** Takes one recipe's inputs out of the materials (call after matches). */
	public void consume(IItemHandlerModifiable materials, int first, int count)
	{
		for(IngredientStack in : inputs)
		{
			int left = in.inputSize;
			for(int s = first; s < first + count && left > 0; s++)
			{
				ItemStack st = materials.getStackInSlot(s);
				if(st.isEmpty() || !in.matchesItemStackIgnoringSize(st))
					continue;
				int take = Math.min(left, st.getCount());
				ItemStack rest = st.copy();
				rest.shrink(take);
				materials.setStackInSlot(s, rest.isEmpty() ? ItemStack.EMPTY : rest);
				left -= take;
			}
		}
	}

	static ItemStack item(Item i, int n)
	{
		return new ItemStack(i, n);
	}

	/** The bench's recipes (provisional amounts, playable now). */
	static void register()
	{
		BY_BLUEPRINT.clear();
		Item[] bp = INAssembly.itemBlueprints;
		Item[] c = INAssembly.itemComponents;
		// IE's steel and iron components are no ore names (only constants in IE's own recipe files): their items
		ItemStack board = new ItemStack(IEContent.itemMaterial, 1, 27), paper = new ItemStack(Items.PAPER);
		ItemStack steelPart = new ItemStack(IEContent.itemMaterial, 1, 9), ironPart = new ItemStack(IEContent.itemMaterial, 1, 8);
		// components: 0 resistor, 1 capacitor, 2 ferrite core, 3 inductor, 4 diode, 5 transistor, 6 integrated circuit,
		// 7 crystal oscillator, 8 relay, 9 Nixie tube
		Item components = bp[0];
		add(components, item(c[0], 4), "ingotHOPGraphite", 1, "wireCopper", 1);
		add(components, item(c[1], 4), "plateAluminum", 1, paper, 1, "wireCopper", 1);
		add(components, item(c[2], 8), "ingotFerrite", 1);
		add(components, item(c[3], 2), new ItemStack(c[2]), 1, "wireCopper", 2);
		add(components, item(c[4], 4), "plateSilicon", 1, "wireCopper", 1);
		add(components, item(c[5], 4), "plateSilicon", 1, "wireCopper", 1, "nuggetElectrum", 1);
		add(components, item(c[6], 2), "plateSilicon", 2, "nuggetGold", 1, "wireCopper", 1);
		add(components, item(c[7], 2), "gemQuartz", 1, "nuggetElectrum", 1, "wireCopper", 1);
		add(components, item(c[8], 2), "wireCopper", 2, "plateIron", 1, "dustRedstone", 1);
		add(components, item(c[9], 1), "blockGlass", 1, "nuggetNickel", 2, "wireCopper", 1);
		// component tapes: the empty reel, the empty tape (loading it is free, on the bench's tape holder)
		Item tapes = bp[1];
		add(tapes, item(INAssembly.itemEmptyReel, 2), "plateSteel", 2, "stickSteel", 1);
		add(tapes, item(INAssembly.itemComponentTape, 1), new ItemStack(INAssembly.itemEmptyReel), 1, paper, 2);
		// computer modules on the bench (the core memory only in the station)
		Item computer = bp[2];
		add(computer, item(INContent.itemComputerParts[0], 1), board, 1, "electronTube", 2, new ItemStack(c[0]), 4, new ItemStack(c[1]), 2);
		add(computer, item(INContent.itemComputerParts[1], 2), board, 1, new ItemStack(c[8]), 4, "wireCopper", 2);
		// the station's nine modules: IE's and vanilla materials only
		Item modules = bp[3];
		Item[] m = INAssembly.itemModules;
		add(modules, item(m[0], 1), "plateSteel", 6, steelPart, 3, "electronTube", 2, board, 1, "stickSteel", 3);
		add(modules, item(m[1], 1), "plateSteel", 6, steelPart, 3, "electronTube", 2, new ItemStack(IEContent.itemGraphiteElectrode), 1, "wireCopper", 6);
		add(modules, item(m[2], 1), "plateSteel", 9, steelPart, 3, "electronTube", 2, new ItemStack(Blocks.PISTON), 2);
		add(modules, item(m[3], 1), "plateSteel", 6, steelPart, 4, "electronTube", 2, "gemDiamond", 1);
		add(modules, item(m[4], 1), "plateSteel", 6, steelPart, 3, "electronTube", 2, new ItemStack(Blocks.HOPPER), 1);
		add(modules, item(m[5], 1), "plateSteel", 6, steelPart, 3, "electronTube", 3, board, 1, "stickAluminum", 3);
		add(modules, item(m[6], 1), "plateSteel", 6, steelPart, 3, "electronTube", 2, "ingotBrick", 6, "wireCopper", 6);
		add(modules, item(m[7], 1), "plateSteel", 6, steelPart, 3, "electronTube", 3, "paneGlass", 3, board, 1);
		add(modules, item(m[8], 1), "plateLead", 6, steelPart, 3, "electronTube", 3, board, 1, "ingotUranium", 1);
		// the four jigs
		Item jigs = bp[4];
		Item[] j = INAssembly.itemJigs;
		add(jigs, item(j[0], 1), "plateSteel", 4, steelPart, 2, new ItemStack(INFuel.itemZircaloyTube), 2);
		add(jigs, item(j[1], 1), "plateSteel", 4, steelPart, 2, "plateLead", 4);
		add(jigs, item(j[2], 1), "plateSteel", 4, steelPart, 2, board, 2);
		add(jigs, item(j[3], 1), "plateSteel", 4, steelPart, 2, "stickSteel", 4);
	}
}
