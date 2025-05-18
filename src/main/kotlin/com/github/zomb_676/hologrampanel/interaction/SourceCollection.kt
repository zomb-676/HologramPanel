package com.github.zomb_676.hologrampanel.interaction

import com.github.zomb_676.hologrampanel.api.ComponentProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.util.unsafeCast
import com.github.zomb_676.hologrampanel.widget.dynamic.DynamicBuildWidget
import com.google.common.collect.BiMap
import org.joml.Vector3f
import org.joml.Vector3fc

class SourceCollection {
    var currentSource: HologramRenderState? = null
        private set

    private val collections: MutableSet<HologramRenderState> = mutableSetOf()

    private val accumulated: Vector3f = Vector3f()
    private val cachedSourcePosition: Vector3f = Vector3f()

    fun getGroupSourcePosition(): Vector3fc = cachedSourcePosition

    fun fullyUpdateCentralSourcePosition(partialTick: Float) {
        accumulated.set(0f, 0f, 0f)
        collections.forEach { state ->
            accumulated.add(state.getSourceWorldPosition())
        }
        accumulated.div(collections.size.toFloat(), cachedSourcePosition)
    }

    internal fun addAndInit(source: HologramRenderState): Any? {
        require(currentSource == null && collections.isEmpty())
        currentSource = source
        add(source)
        return queryWidgetExpose(source)
    }

    fun add(state: HologramRenderState): SourceCollection {
        require(state.context is BlockHologramContext)
        if (collections.add(state)) {
            accumulated.add(state.getSourceWorldPosition())
            accumulated.div(collections.size.toFloat(), cachedSourcePosition)
            if (state.isControlled()) {
                switchCurrent(state)
            }
        }
        return this
    }

    fun remove(state: HologramRenderState): Boolean {
        if (collections.remove(state)) {
            accumulated.sub(state.getSourceWorldPosition())
            accumulated.div(collections.size.toFloat(), cachedSourcePosition)
            if (visible(state)) {
                currentSource = collections.firstOrNull()
            }
            return true
        }
        return false
    }

    fun switchCurrent(source: HologramRenderState) {
        if (!collections.contains(source)) return
        currentSource = source
    }

    //TODO not visible here should not sync
    fun visible(state: HologramRenderState) = state === currentSource

    fun onRemove(state: HologramRenderState, exposeMap: BiMap<Any, SourceCollection>) {
        val removedCurrent = currentSource === state
        if (this.remove(state) && removedCurrent) {
            val newState = currentSource
            if (newState == null) {
                exposeMap.inverse().remove(this)
                return
            }

            val expose = queryWidgetExpose(newState)
            if (expose != null) {
                exposeMap.inverse()[this] = expose
            } else {
                exposeMap.inverse().remove(this)
            }
        }
    }

    private fun queryWidgetExpose(state: HologramRenderState): Any? {
        fun <T : HologramContext> ComponentProvider<T, *>.expose(applyState: HologramRenderState): Any? =
            this.exposeSharedTarget(applyState.context.unsafeCast())

        val newWidget = state.widget
        if (newWidget is DynamicBuildWidget<*>) {
            return newWidget.sharedComponents.firstNotNullOfOrNull { it.expose(state) }
        }
        return null
    }

    fun size() = collections.size
}