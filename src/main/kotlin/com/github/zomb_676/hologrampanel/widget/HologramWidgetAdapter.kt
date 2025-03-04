package com.github.zomb_676.hologrampanel.widget

import com.github.zomb_676.hologrampanel.widget.component.HologramComponentWidgetBuilder
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import java.util.function.Function

@Suppress("FINAL_UPPER_BOUND")
interface HologramWidgetAdapter<S, T> : Function<S, T> where T : HologramWidget {
    val sourceType: Class<S>
    val targetType: Class<T>

    fun convert(source: S): T
    override fun apply(t: S): T = convert(t)

    companion object {
        inline fun <reified S, reified T : HologramWidget> create(crossinline converter: (S).() -> T): HologramWidgetAdapter<S, T> =
            object : HologramWidgetAdapter<S, T> {
                override val sourceType: Class<S>
                    get() = S::class.java
                override val targetType: Class<T>
                    get() = T::class.java

                override fun convert(source: S): T = converter.invoke(source)
            }

        val blockEntity: HologramWidgetAdapter<BlockEntity, HologramWidget> = create {
            HologramComponentWidgetBuilder.builder.invoke(this)
        }

        val entity: HologramWidgetAdapter<Entity, HologramWidget> = create {
            TODO()
        }

        val itemStack: HologramWidgetAdapter<ItemStack, HologramWidget> = create {
            TODO()
        }

        val block: HologramWidgetAdapter<BlockState, HologramWidget> = create {
            TODO()
        }

        val default: HologramWidgetAdapter<Any?, HologramWidget> = create {
            TODO()
        }

        val defaults: Map<Class<*>, HologramWidgetAdapter<*, *>> =
            listOf<HologramWidgetAdapter<*, *>>(blockEntity)
                .associateBy(HologramWidgetAdapter<*, *>::sourceType)
    }
}