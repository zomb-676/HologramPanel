package com.github.zomb_676.hologrampanel.util

import kotlin.reflect.KProperty

/**
 * indicate a designed immutable value's new value
 */
interface RebuildValue<T> {

    /**
     * the new immutable value that should be used
     *
     * @return if matches by reference, the instance is newest
     * if [T] is nullable and return null, means the object is not used any more or the holder is no longer valid
     */
    fun getCurrent(): T

    @Suppress("UNCHECKED_CAST")
    fun <V : T> getCurrentUnsafe() : V? = getCurrent() as V?

    fun stillValid() = getCurrent() != null

    fun isLatest() = getCurrent() == this

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = getCurrent()
}