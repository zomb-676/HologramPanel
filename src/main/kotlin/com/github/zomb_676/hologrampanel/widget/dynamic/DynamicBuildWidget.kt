package com.github.zomb_676.hologrampanel.widget.dynamic

import com.github.zomb_676.hologrampanel.api.ComponentProvider
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.payload.SyncClosePayload
import com.github.zomb_676.hologrampanel.util.unsafeCast
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.component.DataQueryManager
import com.github.zomb_676.hologrampanel.widget.component.HologramComponentWidget
import com.github.zomb_676.hologrampanel.widget.element.IRenderElement
import com.google.common.collect.ImmutableBiMap
import net.minecraft.nbt.CompoundTag

/**
 * widget that support re-build partial when some [ComponentProvider] data have changed
 */
class DynamicBuildWidget<T : HologramContext>(
    target: T, val container: DynamicBuildComponentWidget.Group<T>, val providers: List<ComponentProvider<T, *>>
) : HologramComponentWidget<T>(target, container) {

    val sharedComponents = providers.filter(ComponentProvider<T, *>::considerShare)
    
    fun considerShare() = sharedComponents.isNotEmpty()

    private var maps: Map<ComponentProvider<T, *>, List<DynamicBuildComponentWidget<T>>> =
        target.getRememberDataUnsafe<T>().providers().associateWith { prov ->
            this.container.children.filter { it.getProvider() == prov }
        }

    fun updateComponent(displayType: DisplayType, trigByForceDisplay: Boolean) {
        val remember = target.getRememberDataUnsafe<T>()
        val builder = HologramWidgetBuilder(target)
        var change = false
        maps = maps.mapValues { (provider, old) ->
            if (remember.consumerRebuild(provider) || (trigByForceDisplay && provider.requireRebuildOnForceDisplay(target))) {
                change = true
                builder.rebuildScope(provider) {
                    remember.providerScope(provider) {
                        provider.appendComponent(builder, displayType)
                    }
                }
            } else {
                old
            }
        }
        if (!change) return
        val oldChildren = this.container.children
        val res = maps.flatMap { (_, v) -> v }
        this.container.children = if (res.isNotEmpty()) res else listOf(DynamicBuildComponentWidget.onNoActiveProvider(target))
        recoveryCollapseAndNewStateForComponent(oldChildren, this.container.children)
    }

    override fun onRemove() {
        DataQueryManager.Client.closeForWidget(this)
        SyncClosePayload(target.getRememberData().uuid).sendToServer()
        markAlComponentInvalid(this.container)
    }

    override fun onAdd(state: HologramRenderState) {
        super.onAdd(state)
        val context = state.context.unsafeCast<T>()
        val syncProviders: List<ServerDataProvider<T, *>> = context.getRememberDataUnsafe<T>().serverDataEntries()
        if (syncProviders.isNotEmpty()) {
            val tag = CompoundTag()
            syncProviders.forEach { provider ->
                provider.additionInformationForServer(tag, context)
            }
            DataQueryManager.Client.query(this, tag, syncProviders, context)
        }
    }

    override fun hasNoneOrdinaryContent(): Boolean {
        val children = this.container.children
        return children.size > 2 || children.any { it !is DynamicBuildComponentWidget.OrdinarySingle }
    }

    companion object {
        private fun <T : HologramContext> recoveryCollapseAndNewStateForComponent(
            old: List<DynamicBuildComponentWidget<T>>,
            new: List<DynamicBuildComponentWidget<T>>
        ) {
            fun recovery(old: DynamicBuildComponentWidget.Group<T>, new: DynamicBuildComponentWidget.Group<T>) {
                new.collapse = old.collapse
                recoveryCollapseAndNewStateForComponent(old.children, new.children)
            }

            for (oldElement in old) {
                val newElement = new.firstOrNull { it.getIdentityName() == oldElement.getIdentityName() }
                if (oldElement is DynamicBuildComponentWidget.Single<T> && newElement is DynamicBuildComponentWidget.Single<T>) {
                    oldElement.setReplacedBy(newElement)
                    recoveryCollapseAndNewStateForElements(oldElement.elements, newElement.elements)
                } else if (oldElement is DynamicBuildComponentWidget.Group<T> && newElement is DynamicBuildComponentWidget.Group<T>) {
                    oldElement.setReplacedBy(newElement)
                    recovery(oldElement, newElement)
                } else {
                    oldElement.setNoNewReplace()
                }
            }
        }

        private fun <T : HologramContext> markAlComponentInvalid(component: DynamicBuildComponentWidget.Group<T>) {
            component.setNoNewReplace()
            for (element in component.children) {
                when (element) {
                    is DynamicBuildComponentWidget.Single<*> -> {
                        element.setNoNewReplace()
                        element.elements.keys.forEach(IRenderElement::setNoNewReplace)
                    }

                    is DynamicBuildComponentWidget.Group<*> -> markAlComponentInvalid(element)
                }
            }
        }

        internal fun recoveryCollapseAndNewStateForElements(
            oldElements: ImmutableBiMap<IRenderElement, String>,
            newElements: ImmutableBiMap<IRenderElement, String>
        ) {
            for (old in oldElements) {
                val new = newElements.inverse().get(old.value)
                if (new == null) {
                    old.key.setNoNewReplace()
                } else {
                    old.key.setReplacedBy(new)
                }
            }
        }
    }
}