package immersivenuclear.common;

import blusunrize.immersiveengineering.api.MultiblockHandler;
import immersivenuclear.ImmersiveNuclear;
import immersivenuclear.common.block.BlockChemicalProcessor;
import immersivenuclear.common.block.BlockFilterPress;
import immersivenuclear.common.block.TileChemicalProcessor;
import immersivenuclear.common.block.TileFilterPress;
import immersivenuclear.common.item.ItemFilter;
import immersivenuclear.common.multiblock.MultiblockChemicalProcessor;
import immersivenuclear.common.multiblock.MultiblockFilterPress;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod.EventBusSubscriber(modid = ImmersiveNuclear.MODID)
public class INContent
{
	public static BlockChemicalProcessor blockChemicalProcessor;
	public static BlockFilterPress blockFilterPress;

	/** The mod's creative tab; JEI also lists what is in it. */
	public static final CreativeTabs TAB = new CreativeTabs(ImmersiveNuclear.MODID)
	{
		@Override
		public ItemStack createIcon()
		{
			return new ItemStack(itemChemicalProcessor);
		}
	};

	/** Shown by pick block, Hwyla/Jade and JEI (as the machine of its recipes) for every formed part; drawn as the
	 * 3D model (blockstate variant "inventory"); it places nothing. */
	public static Item itemChemicalProcessor;

	/** The same for the Filtration Unit. */
	public static Item itemFilterPress;

	/** Reagente de processamento: consumed one per cycle by the Chemical Processing Unit. */
	public static Item itemReagent;

	/** Polpa de urânio: the unit's product, input of the filtration unit. Olive brown, opaque. */
	public static Fluid fluidPolpa;

	/** Solução bruta de urânio: the filtration unit's product (uranium still dissolved with impurities), input of
	 * the chemical separation. Dark amber. */
	public static Fluid fluidSolucaoBruta;

	/** Rejeito mineral úmido: the filtration unit's solid residue (the cake left between the plates). */
	public static Item itemRejeito;

	/** Filtro: the filter cloths of the Filtration Unit (28 per machine, worn by every batch). */
	public static ItemFilter itemFilter;

	/** Items of the next steps of the line (docs/CATALOGO-ITENS-E-MAQUINAS.md); no machine uses them yet. */
	public static Item itemCal, itemTorta, itemAgentePrecipitante, itemConcentradoUmido, itemYellowcake;

	/** Aglomerado impuro: heavy building block made of neutralisation cake or filter residue. */
	public static Block blockAglomerado;
	public static Item itemAglomerado;

	/** Fluids of the chemical separation, the effluent treatment and the dryer (catalogue); only tanks and pipes
	 * use them for now. */
	public static Fluid fluidSolvente, fluidLavagem, fluidPurificada, fluidEfluente, fluidLama, fluidEfluentePobre, fluidCondensado;

	/** Every fluid of the mod, for texture stitching and the hazard notes. */
	public static Fluid[] fluids;

	/** The Filtration Unit's five steps (sounds/filtro-prensa, made by the user): pressurise, close, release, filter (loop), open. */
	public static SoundEvent soundFpPressurize, soundFpClose, soundFpRelease, soundFpFilterLoop, soundFpOpen;

	/** Running sound of the Chemical Processing Unit (ferramentas/sons.py, from the user's sounds/ sample). */
	public static SoundEvent soundStart, soundLoop, soundStop;

	@SubscribeEvent
	public static void registerSounds(RegistryEvent.Register<SoundEvent> event)
	{
		soundStart = sound(event, "chemical_processor_start");
		soundLoop = sound(event, "chemical_processor_loop");
		soundStop = sound(event, "chemical_processor_stop");
		soundFpPressurize = sound(event, "filter_press_pressurize");
		soundFpClose = sound(event, "filter_press_close");
		soundFpRelease = sound(event, "filter_press_release");
		soundFpFilterLoop = sound(event, "filter_press_filter_loop");
		soundFpOpen = sound(event, "filter_press_open");
	}

	static SoundEvent sound(RegistryEvent.Register<SoundEvent> event, String name)
	{
		ResourceLocation id = new ResourceLocation(ImmersiveNuclear.MODID, name);
		SoundEvent e = new SoundEvent(id).setRegistryName(id);
		event.getRegistry().register(e);
		return e;
	}

	/** Fluids have to exist before blocks and items are registered (buckets look them up). */
	public static void preInit()
	{
		fluidPolpa = fluid("polpa_uranio", 1400, 3000);
		fluidSolucaoBruta = fluid("solucao_bruta_uranio", 1100, 1200);
		fluidSolvente = fluid("solvente_extracao", 800, 1100);
		fluidLavagem = fluid("solucao_lavagem", 1050, 1000);
		fluidPurificada = fluid("solucao_purificada_uranio", 1080, 1100);
		fluidEfluente = fluid("efluente_quimico", 1100, 1200);
		fluidLama = fluid("lama_neutralizada", 1500, 3500);
		fluidEfluentePobre = fluid("efluente_pobre", 1050, 1100);
		fluidCondensado = fluid("condensado_processo", 1000, 1000);
		fluids = new Fluid[]{fluidPolpa, fluidSolucaoBruta, fluidSolvente, fluidLavagem, fluidPurificada, fluidEfluente, fluidLama, fluidEfluentePobre, fluidCondensado};
	}

	static Item item(RegistryEvent.Register<Item> event, String name)
	{
		Item i = new Item().setRegistryName(ImmersiveNuclear.MODID, name).setTranslationKey(ImmersiveNuclear.MODID + "." + name).setCreativeTab(TAB);
		event.getRegistry().register(i);
		return i;
	}

	/** A fluid with its textures in blocks/fluid/<name>_still/_flow and a universal bucket. */
	static Fluid fluid(String name, int density, int viscosity)
	{
		Fluid f = new Fluid(name, new ResourceLocation(ImmersiveNuclear.MODID, "blocks/fluid/" + name + "_still"),
				new ResourceLocation(ImmersiveNuclear.MODID, "blocks/fluid/" + name + "_flow")).setDensity(density).setViscosity(viscosity).setUnlocalizedName(ImmersiveNuclear.MODID + "." + name);
		if(!FluidRegistry.registerFluid(f))
			f = FluidRegistry.getFluid(name);
		FluidRegistry.addBucketForFluid(f);
		return f;
	}

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> event)
	{
		itemChemicalProcessor = new Item().setRegistryName(ImmersiveNuclear.MODID, "chemical_processor").setTranslationKey(ImmersiveNuclear.MODID + ".chemical_processor").setCreativeTab(TAB);
		event.getRegistry().register(itemChemicalProcessor);
		itemFilterPress = new Item().setRegistryName(ImmersiveNuclear.MODID, "filter_press").setTranslationKey(ImmersiveNuclear.MODID + ".filter_press").setCreativeTab(TAB);
		event.getRegistry().register(itemFilterPress);
		itemReagent = new Item().setRegistryName(ImmersiveNuclear.MODID, "reagente_processamento").setTranslationKey(ImmersiveNuclear.MODID + ".reagente_processamento").setCreativeTab(TAB);
		event.getRegistry().register(itemReagent);
		itemRejeito = new Item().setRegistryName(ImmersiveNuclear.MODID, "rejeito_mineral_umido").setTranslationKey(ImmersiveNuclear.MODID + ".rejeito_mineral_umido").setCreativeTab(TAB);
		event.getRegistry().register(itemRejeito);
		itemFilter = new ItemFilter();
		event.getRegistry().register(itemFilter);
		itemCal = item(event, "cal");
		itemTorta = item(event, "torta_neutralizacao");
		itemAgentePrecipitante = item(event, "agente_precipitante");
		itemConcentradoUmido = item(event, "concentrado_umido_uranio");
		itemYellowcake = item(event, "yellowcake");
		itemAglomerado = new ItemBlock(blockAglomerado).setRegistryName(blockAglomerado.getRegistryName());
		event.getRegistry().register(itemAglomerado);
	}

	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> event)
	{
		blockChemicalProcessor = new BlockChemicalProcessor();
		event.getRegistry().register(blockChemicalProcessor);
		GameRegistry.registerTileEntity(TileChemicalProcessor.class, new ResourceLocation(ImmersiveNuclear.MODID, "chemical_processor"));
		blockFilterPress = new BlockFilterPress();
		event.getRegistry().register(blockFilterPress);
		GameRegistry.registerTileEntity(TileFilterPress.class, new ResourceLocation(ImmersiveNuclear.MODID, "filter_press"));
		// Heavy like IE's concrete (provisional values)
		blockAglomerado = new Block(Material.ROCK).setHardness(3f).setResistance(60f).setRegistryName(ImmersiveNuclear.MODID, "aglomerado_impuro")
				.setTranslationKey(ImmersiveNuclear.MODID + ".aglomerado_impuro").setCreativeTab(TAB);
		blockAglomerado.setHarvestLevel("pickaxe", 1);
		event.getRegistry().register(blockAglomerado);
	}

	public static void init()
	{
		MultiblockHandler.registerMultiblock(MultiblockChemicalProcessor.instance);
		MultiblockHandler.registerMultiblock(MultiblockFilterPress.instance);
	}
}
