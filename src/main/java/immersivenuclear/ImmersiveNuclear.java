package immersivenuclear;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import immersivenuclear.common.INContent;
import immersivenuclear.common.INGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = ImmersiveNuclear.MODID, name = "Immersive Nuclear", version = ImmersiveNuclear.VERSION, dependencies = "required-after:immersiveengineering@[0.12-98,);")
public class ImmersiveNuclear
{
	public static final String MODID = "immersivenuclear";
	public static final String VERSION = "@VERSION@";
	public static final Logger logger = LogManager.getLogger(MODID);

	@Mod.Instance(MODID)
	public static ImmersiveNuclear instance;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		INContent.preInit();
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event)
	{
		INContent.init();
		NetworkRegistry.INSTANCE.registerGuiHandler(this, new INGuiHandler());
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
		// IE builds its Engineer's Manual in its own client postInit, which runs before ours
		if(event.getSide().isClient())
			immersivenuclear.client.ManualEntries.register();
	}
}
