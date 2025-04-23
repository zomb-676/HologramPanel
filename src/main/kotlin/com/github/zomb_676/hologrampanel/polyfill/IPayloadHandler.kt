package com.github.zomb_676.hologrampanel.polyfill

fun interface IPayloadHandler<T> {
    fun handle(payload: T, context: IPayloadContext)
}