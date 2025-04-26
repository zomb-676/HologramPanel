package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.payload.NetworkHandle
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.minecraftforge.fml.loading.FMLEnvironment
import net.minecraftforge.fml.loading.FMLLoader
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Mod(HologramPanel.MOD_ID)
class HologramPanel() {
    companion object {
        const val MOD_NAME = "Hologram Panel"
        const val MOD_ID = "hologram_panel"

        val LOGGER: Logger = LogManager.getLogger(MOD_ID)

        val underDevelopment = !FMLEnvironment.production
        val underDebug = underDevelopment

        fun rl(path: String): ResourceLocation = ResourceLocation(MOD_ID, path)

        var serverInstalled: Boolean = false
            internal set
    }


    init {
        val context = FMLJavaModLoadingContext.get()
        val dist: Dist = FMLLoader.getDist()
        val modBus: IEventBus = context.modEventBus

        PluginManager.init()
        Config.registerConfig(modBus)

        if (dist == Dist.DEDICATED_SERVER) {
            serverInstalled = true
        }

        EventHandler.initEvents(dist, modBus)
        AllRegisters.initEvents(dist, modBus)
        Config.initEvents(dist, modBus)
        NetworkHandle.registerPackets()
        BuildInHologramEventHandle.initEvents(dist, modBus)
    }
}
