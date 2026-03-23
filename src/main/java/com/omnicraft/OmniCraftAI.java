package com.omnicraft;

import com.omnicraft.config.ConfigManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = OmniCraftAI.MOD_ID, dist = Dist.CLIENT)
public class OmniCraftAI {
    public static final String MOD_ID = "omnicraft";

    public OmniCraftAI(IEventBus modBus) {
        modBus.addListener(this::onClientSetup);
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        ConfigManager.init();
    }
}