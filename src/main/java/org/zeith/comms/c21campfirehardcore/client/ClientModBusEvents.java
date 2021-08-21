package org.zeith.comms.c21campfirehardcore.client;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.zeith.comms.c21campfirehardcore.Campfire;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModBusEvents
{
	@SubscribeEvent
	public static void clientSetup(FMLClientSetupEvent e)
	{
		ItemBlockRenderTypes.setRenderLayer(Campfire.BLOCK, RenderType.cutout());
	}
}