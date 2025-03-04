package com.github.zomb_676.hologrampanel.widget.interactive

import com.github.zomb_676.hologrampanel.interaction.InteractionCommand

abstract class InteractiveComponent() {
    abstract fun onInteractiveCommand(command: InteractionCommand.Raw)


}