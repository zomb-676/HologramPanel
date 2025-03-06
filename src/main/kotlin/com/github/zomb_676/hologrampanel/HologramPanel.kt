package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.api.HologramPlugin
import com.github.zomb_676.hologrampanel.api.IHologramPlugin
import com.github.zomb_676.hologrampanel.util.getClassOf
import com.github.zomb_676.hologrampanel.widget.interactive.HologramInteractiveHelper
import com.github.zomb_676.hologrampanel.widget.interactive.HologramInteractiveTarget
import net.minecraft.resources.ResourceLocation
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModList
import net.neoforged.fml.common.Mod
import net.neoforged.fml.javafmlmod.FMLModContainer
import net.neoforged.fml.loading.FMLEnvironment
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.lang.annotation.ElementType
import kotlin.streams.asSequence

@Mod(HologramPanel.MOD_ID)
class HologramPanel(val container: FMLModContainer, val dist: Dist, val modBus: IEventBus) {
    companion object {
        const val MOD_NAME = "Hologram Panel"
        const val MOD_ID = "hologram_panel"

        val LOGGER: Logger = LogManager.getLogger(MOD_ID)

        val underDevelopment = !FMLEnvironment.production
        val underDebug = underDevelopment

        fun rl(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)

        var serverInstalled : Boolean = false
            internal set
    }

    init {
        if (dist == Dist.DEDICATED_SERVER) {
            serverInstalled = true
        }

        EventHandler.initEvents(dist, modBus)
        AllRegisters.initEvents(dist, modBus)

        ModList.get().allScanData.asSequence()
            .flatMap { it.getAnnotatedBy(HologramPlugin::class.java, ElementType.TYPE).asSequence() }
            .forEach {
                try {
                    val plugin = getClassOf<IHologramPlugin>(it.clazz().className)
                        .getDeclaredConstructor()
                        .apply { require(trySetAccessible()) }
                        .newInstance()
                    //todo process plugin
                } catch (e: Exception) {

                }
            }

        HologramInteractiveHelper.register(
            HologramInteractiveTarget.Companion.Furnace,
            HologramInteractiveTarget::FurnaceWidget
        )
    }
}
