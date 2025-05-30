package com.github.zomb_676.hologrampanel.api

import com.github.zomb_676.hologrampanel.interaction.context.HologramContext

/**
 * attach ticket by this instance
 */
class TicketAdder<T : HologramContext> internal constructor(internal val list: MutableList<HologramTicket<T>>) {
    fun attach(ticket: HologramTicket<T>) {
        list.add(ticket)
    }

    operator fun plusAssign(ticket: HologramTicket<T>) = attach(ticket)

    fun isEmpty() = list.isEmpty()
    fun isNotEmpty() = list.isNotEmpty()
}