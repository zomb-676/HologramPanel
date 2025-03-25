package com.github.zomb_676.hologrampanel.interaction

import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.PluginManager
import com.github.zomb_676.hologrampanel.addon.BuildInPlugin
import com.github.zomb_676.hologrampanel.api.ComponentProvider
import com.github.zomb_676.hologrampanel.api.HologramHolder
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.util.profilerStack
import com.github.zomb_676.hologrampanel.util.unsafeCast
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.HologramWidget
import com.github.zomb_676.hologrampanel.widget.component.DataQueryManager
import com.github.zomb_676.hologrampanel.widget.dynamic.DynamicBuildWidget
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.EntitySelector
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import org.apache.http.pool.ConnPool

object RayTraceHelper {
    /**
     * use ray-trace to create a satisfied [HologramContext]
     */
    fun findTarget(radius: Double, partialTicks: Float): HologramContext? = profilerStack("hologram_find_target") {
        val player = Minecraft.getInstance().player!!
        val blockHit = player.pick(radius, partialTicks, true)
        val entityHit = run {
            val from = player.eyePosition
            val viewVector = player.getViewVector(partialTicks)
            val to = from.add(viewVector.x * radius, viewVector.y * radius, viewVector.z * radius)
            val aabb = player.boundingBox.expandTowards(viewVector.scale(radius)).inflate(1.0, 1.0, 10.0)
            ProjectileUtil.getEntityHitResult(
                player, from, to, aabb, EntitySelector.ENTITY_STILL_ALIVE, radius
            )
        }
        val result: HitResult = run {
            if (entityHit == null) {
                return@run blockHit
            }
            val blockDistance = blockHit.distanceTo(player)
            val entityDistance = entityHit.distanceTo(player)
            return@run if (blockDistance < entityDistance) blockHit else entityHit
        }

        if (result.type == HitResult.Type.MISS) return@profilerStack null
        return@profilerStack when (result) {
            is BlockHitResult -> {
                val context = BlockHologramContext.of(result, player)
                if (PluginManager.getInstance().hideBlock(context.createTimeBlockState().block)) {
                    return@profilerStack null
                }
                context
            }

            is EntityHitResult -> {
                val context = EntityHologramContext.of(result, player)
                if (PluginManager.getInstance().hideEntity(context.getEntity())) {
                    return@profilerStack null
                }
                context
            }
            else -> throw RuntimeException("unknown hit result:$result")
        }
    }

    /**
     * create the widget by the context
     */
    fun <T : HologramContext> createHologramWidget(
        context: T, displayType: DisplayType = DisplayType.NORMAL
    ): HologramWidget? = profilerStack("create_hologram") {

        val widget: DynamicBuildWidget<T> = when (context) {
            is EntityHologramContext -> {
                val builder: HologramWidgetBuilder<EntityHologramContext> = HologramWidgetBuilder(context)
                val providers: List<ComponentProvider<EntityHologramContext, *>> = PluginManager.queryProviders(context)
                if (providers.isEmpty() && Config.Client.dropNonApplicableWidget.get())  return@profilerStack null
                applyProvider(providers, builder, displayType)
                val widget = builder.build(BuildInPlugin.Companion.DefaultEntityDescriptionProvider, displayType, providers)
                (context.getEntity() as HologramHolder).setWidget(widget)
                widget
            }

            is BlockHologramContext -> {
                val builder: HologramWidgetBuilder<BlockHologramContext> = HologramWidgetBuilder(context)
                val providers: List<ComponentProvider<BlockHologramContext, *>> = PluginManager.queryProviders(context)
                if (providers.isEmpty() && Config.Client.dropNonApplicableWidget.get())  return@profilerStack null
                applyProvider(providers, builder, displayType)
                builder.build(BuildInPlugin.Companion.DefaultBlockDescriptionProvider, displayType, providers)
            }
        }.unsafeCast()
        val syncProviders: List<ServerDataProvider<T, *>> =
            context.getRememberDataUnsafe<T>().serverDataEntries()
        if (syncProviders.isNotEmpty()) {
            val tag = CompoundTag()
            syncProviders.forEach { provider ->
                provider.additionInformationForServer(tag, context)
            }
            DataQueryManager.Client.query(widget, tag, syncProviders, context)
        }
        return@profilerStack widget
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : HologramContext> applyProvider(
        providers: List<ComponentProvider<T, *>>, builder: HologramWidgetBuilder<T>, displayType: DisplayType
    ) {
        val remember = builder.context.getRememberDataUnsafe<T>()
        providers.forEach { provider ->
            remember.providerScope(provider) {
                builder.currentProvider = provider
                provider.appendComponent(builder, displayType)
                builder.currentProvider = null
            }
        }
    }
}