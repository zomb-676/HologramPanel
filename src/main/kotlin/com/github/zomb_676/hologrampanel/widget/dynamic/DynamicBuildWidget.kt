package com.github.zomb_676.hologrampanel.widget.dynamic

import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.util.unsafeCast
import com.github.zomb_676.hologrampanel.widget.component.HologramComponentWidget

class DynamicBuildWidget<T : HologramContext>(target: T, val container: DynamicBuildComponentWidget.Group<T>) :
    HologramComponentWidget<T>(target) {
    override fun initialComponent(): DynamicBuildComponentWidget.Group<T> = container

    val s = container.children
        .groupBy(DynamicBuildComponentWidget<T>::getProvider).toMutableMap()

    override fun updateComponent() {
        //todo check
        this.resetSelectState()
        target.getRememberData().unsafeCast<Remember<T>>().consumerRebuild { provider ->
            
        }
    }
}