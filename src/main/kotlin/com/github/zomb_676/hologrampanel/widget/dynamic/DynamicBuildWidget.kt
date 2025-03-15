package com.github.zomb_676.hologrampanel.widget.dynamic

import com.github.zomb_676.hologrampanel.api.ComponentProvider
import com.github.zomb_676.hologrampanel.interaction.HologramRenderState
import com.github.zomb_676.hologrampanel.interaction.InteractionCommand.Exact.SelectComponent
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.payload.SyncClosePayload
import com.github.zomb_676.hologrampanel.util.SelectedPath
import com.github.zomb_676.hologrampanel.util.unsafeCast
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.component.DataQueryManager
import com.github.zomb_676.hologrampanel.widget.component.HologramComponentWidget
import com.github.zomb_676.hologrampanel.widget.component.HologramWidgetComponent

/**
 * widget that support re-build partial when some [ComponentProvider] data have changed
 */
class DynamicBuildWidget<T : HologramContext>(target: T, val container: DynamicBuildComponentWidget.Group<T>) :
    HologramComponentWidget<T>(target, container) {

    private val path: SelectedPath<HologramWidgetComponent<T>> = object : SelectedPath<HologramWidgetComponent<T>> {
        val stack: MutableList<DynamicBuildComponentWidget.Group<T>> = mutableListOf()

        init {
            stack.add(container)
        }

        var current: DynamicBuildComponentWidget<T> = container.children.first()
        var currentIndex = 0

        override fun atTerminus(component: HologramWidgetComponent<T>): Boolean = this.current == component


        override fun atUnTerminusPath(component: HologramWidgetComponent<T>): Boolean = this.stack.contains(component)

        override fun unTerminalPath(): Sequence<HologramWidgetComponent<T>> = this.stack.asSequence()

        override fun terminal(): HologramWidgetComponent<T> = this.current

        override fun atWholePath(component: HologramWidgetComponent<T>): Boolean = if (component.isGroup())
            atUnTerminusPath(component) else atUnTerminusPath(component)

        override fun resetToDefault() {
            this.stack.clear()
            this.stack.add(container)
            this.current = container.children.first()
            this.currentIndex = 0
        }

        override fun tryRecover(newTop: HologramWidgetComponent<T>, oldContents: List<HologramWidgetComponent<T>>) {
            val newTop = newTop.unsafeCast<DynamicBuildComponentWidget.Group<T>>()

            require(newTop.getIdentityName() == stack.first().getIdentityName())
            this.recoveryCollapseState(oldContents, newTop.children)
            stack[0] = newTop.unsafeCast()


            var index = 0
            var error: Boolean = false
            while (++index < stack.size) {
                val queryName = stack[index].getIdentityName()
                val next = stack[index - 1].children.firstOrNull { it.getIdentityName() == queryName }
                if (next is DynamicBuildComponentWidget.Group<T>) {
                    stack[index] = next
                } else {
                    repeat(stack.size - index) { stack.removeLast() }
                    error = true
                    break
                }
            }
            if (!error) {
                val candidateCurrent =
                    this.stack.last().children.firstOrNull { it.getIdentityName() == this.current.getIdentityName() }
                if (candidateCurrent != null) {
                    this.current = candidateCurrent
                    this.currentIndex = this.stack.last().children.indexOf(candidateCurrent)
                    require(this.currentIndex >= 0)
                } else {
                    error = true
                }
            }
            if (error) {
                val parent = this.stack.last()
                if (parent.children.isNotEmpty()) {
                    this.current = parent.children.first()
                    this.currentIndex = 0
                } else {
                    this.current = stack.removeLast()
                    this.currentIndex = stack.last().children.indexOf(parent)
                }
            }
        }

        private fun recoveryCollapseState(
            old: List<HologramWidgetComponent<T>>,
            new: List<HologramWidgetComponent<T>>
        ) {
            fun recovery(old: DynamicBuildComponentWidget.Group<T>, new: DynamicBuildComponentWidget.Group<T>) {
                new.collapse = old.collapse
                for (element in old.children) {
                    if (element is DynamicBuildComponentWidget.Group<T>) {
                        val rec = new.children.firstOrNull { it.getIdentityName() == element.getIdentityName() }
                        if (rec is DynamicBuildComponentWidget.Group<T>) {
                            recovery(element, rec)
                        }
                    }
                }
            }

            val old = old.unsafeCast<List<DynamicBuildComponentWidget<T>>>()
            val new = new.unsafeCast<List<DynamicBuildComponentWidget<T>>>()
            for (element in old) {
                if (element is DynamicBuildComponentWidget.Group<T>) {
                    val rec = new.firstOrNull { it.getIdentityName() == element.getIdentityName() }
                    if (rec is DynamicBuildComponentWidget.Group) {
                        recovery(element, rec)
                    }
                }
            }
        }

        override fun selectCommand(state: HologramRenderState, command: SelectComponent) {
            when (command) {
                SelectComponent.SELECT_NEXT -> {
                    val children = stack.lastOrNull()?.children ?: return
                    this.currentIndex = (this.currentIndex + 1) % children.size
                    this.current = children[this.currentIndex]
                }

                SelectComponent.SELECT_BEFORE -> {
                    val children = stack.lastOrNull()?.children ?: return
                    --this.currentIndex
                    this.currentIndex = if (this.currentIndex < 0) {
                        children.size - 1
                    } else {
                        this.currentIndex % children.size
                    }
                    this.current = children[this.currentIndex]
                }

                SelectComponent.SELECT_GROUP_FIRST_CHILD -> {
                    val current = current
                    if (current is DynamicBuildComponentWidget.Group<T>) {
                        current.collapse = false
                        this.stack.addLast(current)
                        this.current = current.children.first()
                        this.currentIndex = 0
                    }
                }

                SelectComponent.SELECT_PARENT -> {
                    if (this.stack.size > 1) {
                        this.current = this.stack.removeLast()
                        this.currentIndex = 0
                    }
                }
            }
        }
    }

    private var maps: Map<ComponentProvider<T>, List<DynamicBuildComponentWidget<T>>> =
        target.getRememberDataUnsafe<T>().providers().associateWith { prov ->
            this.container.children.filter { it.getProvider() == prov }
        }

    fun updateComponent() {
        //todo
        val displayType = DisplayType.NORMAL

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
            if (res.isNotEmpty()) res else listOf(DynamicBuildComponentWidget.onNoProvider(target))
        this.getSelectedPath().tryRecover(this.container, oldChildren)
    }

    override fun onRemove() {
        DataQueryManager.Client.closeForWidget(this)
        SyncClosePayload(target.getRememberData().uuid).sendToServer()
    }

    override fun getSelectedPath(): SelectedPath<HologramWidgetComponent<T>> = this.path
}