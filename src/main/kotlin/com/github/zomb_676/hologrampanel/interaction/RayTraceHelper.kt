package com.github.zomb_676.hologrampanel.interaction

import com.github.zomb_676.hologrampanel.AllRegisters
import com.github.zomb_676.hologrampanel.BuildInPlugin
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.util.profilerStack
import com.github.zomb_676.hologrampanel.util.unsafeCast
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.HologramWidget
import com.github.zomb_676.hologrampanel.widget.component.ComponentProvider
import com.github.zomb_676.hologrampanel.widget.component.DataQueryManager
import com.github.zomb_676.hologrampanel.widget.component.ServerDataProvider
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.EntitySelector
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult

object RayTraceHelper {
    fun findTarget(radius: Int, partialTicks: Float): HologramContext? = profilerStack("hologram_find_target") {
        val distance = 20.0
        val player = Minecraft.getInstance().player!!
        val blockHit = player.pick(distance, partialTicks, true)
        val entityHit = run {
            val from = player.eyePosition
            val viewVector = player.getViewVector(partialTicks)
            val to = from.add(viewVector.x * distance, viewVector.y * distance, viewVector.z * distance)
            val aabb = player.boundingBox.expandTowards(viewVector.scale(distance)).inflate(1.0, 1.0, 10.0)
            ProjectileUtil.getEntityHitResult(
                player, from, to, aabb, EntitySelector.ENTITY_STILL_ALIVE, distance
            )
        }
        val result : HitResult = run {
            if (entityHit == null) {
                return@run blockHit
            }
            val blockDistance = blockHit.distanceTo(player)
            val entityDistance = entityHit.distanceTo(player)
            return@run if (blockDistance < entityDistance) blockHit else entityHit
        }

        if (result.type == HitResult.Type.MISS) return null
        return when (result) {
            is BlockHitResult -> {
                if (Minecraft.getInstance().level?.getBlockEntity(result.blockPos) == null) {
                    //todo add event to control this
                    return null
                }
                BlockHologramContext.of(result, player)
            }
            is EntityHitResult -> EntityHologramContext.of(result, player)
            else -> throw RuntimeException("unknown hit result:$result")
        }
    }

    fun <T : HologramContext> createHologramWidget(
        context: T, displayType: DisplayType = DisplayType.NORMAL
    ): HologramWidget = profilerStack("create_hologram") {

        val widget = when (context) {
            is EntityHologramContext -> {
                val builder = HologramWidgetBuilder(context)
                applyProvider(context.getEntity(), builder, displayType)
                builder.build(BuildInPlugin.Companion.DefaultEntityDescriptionProvider.unsafeCast(), displayType)
            }

            is BlockHologramContext -> {
                val builder = HologramWidgetBuilder(context)
                applyProvider(context.getBlockState().block, builder, displayType)
                applyProvider(context.getFluidState().type, builder, displayType)
                applyProvider(context.getBlockEntity(), builder, displayType)
                builder.build(BuildInPlugin.Companion.DefaultBlockDescriptionProvider.unsafeCast(), displayType)
            }
        }
        val providers: List<ServerDataProvider<T>> = context.getRememberDataUnsafe<T>().serverDataEntries().unsafeCast()
        if (providers.isNotEmpty()) {
            val tag = CompoundTag()
            providers.forEach { provider ->
                provider.additionInformationForServer(tag, context)
            }
            DataQueryManager.Client.query(widget, tag, providers, context)
        }
        return widget
    }

    private val map: MutableMap<Class<*>, List<ComponentProvider<*>>> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    private fun <T : HologramContext> applyProvider(
        target: Any?, builder: HologramWidgetBuilder<T>, displayType: DisplayType
    ) {
        if (target == null) return
        queryProvidersForClass(target::class.java).forEach { provider ->
            builder.context.getRememberDataUnsafe<T>().providerScope(provider as ComponentProvider<T>) {
                builder.currentProvider = provider
                provider.appendComponent(builder, displayType)
                builder.currentProvider = null
            }
        }
    }

    private fun queryProvidersForClass(target: Class<*>): List<ComponentProvider<*>> {
        val res = map[target]
        if (res != null) return res

        val maps = AllRegisters.ComponentHologramProviderRegistry.REGISTRY.associateBy { it.targetClass() }
        val list = mutableListOf<ComponentProvider<*>>()
        searchByInheritTree(target, maps, list)
        map[target] = list
        return list
    }

    private fun <V> searchByInheritTree(c: Class<*>, map: Map<Class<*>, V>, list: MutableList<V>) {
        val target = map[c]
        if (target != null) {
            list.add(target)
        }
        c.interfaces.forEach {
            searchByInheritTree(it, map, list)
        }
        val sup = c.superclass
        if (sup != null) {
            searchByInheritTree(sup, map, list)
        }
    }
}