package com.github.zomb_676.hologrampanel.interaction

import com.github.zomb_676.hologrampanel.AllRegisters
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.util.profiler
import com.github.zomb_676.hologrampanel.util.unsafeCast
import com.github.zomb_676.hologrampanel.widget.HologramWidget
import com.github.zomb_676.hologrampanel.widget.component.ComponentProvider
import com.github.zomb_676.hologrampanel.widget.component.DataQueryManager
import com.github.zomb_676.hologrampanel.widget.component.HologramWidgetBuilder
import com.github.zomb_676.hologrampanel.widget.component.ServerDataProvider
import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult

object RayTraceHelper {
    fun findTarget(radius: Int, partialTicks: Float): HologramContext? {
        val player = Minecraft.getInstance().player!!
        val result: HitResult = player.pick(20.0, partialTicks, true)
        if (result.type == HitResult.Type.MISS) return null
        return when (result) {
            is BlockHitResult -> BlockHologramContext.of(result, player)
            is EntityHitResult -> EntityHologramContext.of(result, player)
            else -> throw RuntimeException("unknown hit result:$result")
        }
    }

    fun <T : HologramContext> createHologramWidget(source: T): HologramWidget {
        profiler.push("create_hologram")
        val widget = when (source) {
            is EntityHologramContext -> {
                val builder = HologramWidgetBuilder(source)
                apply(source.entity, builder)
                builder.build { component(source.entity.name) }
            }

            is BlockHologramContext -> {
                val builder = HologramWidgetBuilder(source)
                apply(source.getBlockState().block, builder)
                apply(source.getFluidState().type, builder)
                apply(source.getBlockEntity(), builder)
                builder.build {
                    item(source.getBlockState().block.asItem()).setScale(0.75)
                    component(source.getBlockState().block.name).setScale(1.5)
                }
            }
        }
        val providers: MutableSet<ServerDataProvider<T>> = mutableSetOf()
        widget.component.traverseRecursively { component ->
            val provider = component.unsafeCast<HologramWidgetBuilder.ProviderRelated<T>>().provider
            if (provider is ServerDataProvider<*>) {
                providers.add(provider.unsafeCast())
            }
        }
        if (providers.isNotEmpty()) {
            val tag = CompoundTag()
            providers.forEach { provider ->
                provider.additionInformationForServer(tag, source)
            }
            DataQueryManager.Client.query(widget, tag, providers.toList().unsafeCast(), source)
        }
        profiler.pop()
        return widget
    }

    private val map: MutableMap<Class<*>, List<ComponentProvider<*>>> = mutableMapOf()

    private fun <T : HologramContext> apply(target: Any?, builder: HologramWidgetBuilder<T>) {
        if (target == null) return
        query(target::class.java).forEach { provider ->
            builder.currentProvider = provider.unsafeCast<ComponentProvider<T>>()
            provider.unsafeCast<ComponentProvider<T>>().appendComponent(builder)
            builder.currentProvider = null
        }
    }

    private fun query(target: Class<*>): List<ComponentProvider<*>> {
        val res = map[target]
        if (res != null) return res

        val maps = AllRegisters.ComponentHologramProviderRegistry.REGISTRY
            .associateBy { it.targetClass() }
        val list = mutableListOf<ComponentProvider<*>>()
        find(target, maps, list)
        map[target] = list
        return list
    }

    private fun <V> find(c: Class<*>, map: Map<Class<*>, V>, list: MutableList<V>) {
        val target = map[c]
        if (target != null) {
            list.add(target)
        }
        c.interfaces.forEach {
            find(it, map, list)
        }
        val sup = c.superclass
        if (sup != null) {
            find(sup, map, list)
        }
    }
}