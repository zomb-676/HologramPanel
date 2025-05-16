package com.github.zomb_676.hologrampanel

import net.minecraft.resources.ResourceLocation
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.fml.javafmlmod.FMLModContainer
import net.neoforged.fml.loading.FMLEnvironment
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*

@Mod(HologramPanel.MOD_ID)
class HologramPanel(val container: FMLModContainer, val dist: Dist, val modBus: IEventBus) {
    companion object {
        const val MOD_NAME = "Hologram Panel"
        const val MOD_ID = "hologram_panel"

        val LOGGER: Logger = LogManager.getLogger(MOD_ID)

        val underDevelopment = !FMLEnvironment.production
        val underDebug = underDevelopment

        fun rl(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)

        var serverInstalled: Boolean = false
            internal set

        val NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
            isGroupingUsed = false
            roundingMode = RoundingMode.HALF_UP
        }
    }


    init {
        PluginManager.init()
        Config.registerConfig(container, modBus)

        if (dist == Dist.DEDICATED_SERVER) {
            serverInstalled = true
        }

        EventHandler.initEvents(dist, modBus)
        AllRegisters.initEvents(dist, modBus)
        Config.initEvents(dist, modBus)
        BuildInHologramEventHandle.initEvents(dist, modBus)
    }
}
