package com.github.zomb_676.hologrampanel.api

import com.github.zomb_676.hologrampanel.interaction.context.HologramWorldContext


class TicketAdder<T : HologramWorldContext> internal constructor(internal val list: MutableList<HologramTicket<T>>) {
    fun attach(ticket: HologramTicket<T>) {
        list.add(ticket)
    }

    operator fun plusAssign(ticket: HologramTicket<T>) = attach(ticket)
}