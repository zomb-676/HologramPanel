package com.github.zomb_676.hologrampanel.widget.dynamic

import com.github.zomb_676.hologrampanel.api.ComponentProvider
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.payload.SyncClosePayload
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.component.DataQueryManager
import com.github.zomb_676.hologrampanel.widget.component.HologramComponentWidget

/**
 * widget that support re-build partial when some [ComponentProvider] data have changed
 */
class DynamicBuildWidget<T : HologramContext>(
    target: T,
    val container: DynamicBuildComponentWidget.Group<T>,
    val providers: List<ComponentProvider<T, *>>
) :
    HologramComponentWidget<T>(target, container) {

    private var maps: Map<ComponentProvider<T, *>, List<DynamicBuildComponentWidget<T>>> =
        target.getRememberDataUnsafe<T>().providers().associateWith { prov ->
            this.container.children.filter { it.getProvider() == prov }
        }

    fun updateComponent(displayType: DisplayType) {
        val remember = target.getRememberDataUnsafe<T>()
        val builder = HologramWidgetBuilder(target)
        maps = maps.mapValues { (provider, list) ->
            if (remember.consumerRebuild(provider)) {
                builder.rebuildScope(provider) {
                    remember.providerScope(provider) {
                        provider.appendComponent(builder, displayType)
                    }
                }
            } else {
                list
            }
        }
        val oldChildren = this.container.children
        val res = maps.flatMap { (_, v) -> v }
        this.container.children =
            if (res.isNotEmpty()) res else listOf(DynamicBuildComponentWidget.onNoActiveProvider(target))
    }

    override fun onRemove() {
        DataQueryManager.Client.closeForWidget(this)
        SyncClosePayload(target.getRememberData().uuid).sendToServer()
    }

    override fun hasNoneOrdinaryContent(): Boolean {
        val children = this.container.children
        return children.size > 2 || children.any { it !is DynamicBuildComponentWidget.OrdinarySingle }
    }
}