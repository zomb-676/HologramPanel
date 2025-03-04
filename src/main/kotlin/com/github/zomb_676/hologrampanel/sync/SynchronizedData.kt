package com.github.zomb_676.hologrampanel.sync

open class SynchronizedData<T>(initialData: T) {
    @PublishedApi
    internal var data: T = initialData
    protected var dirty: Boolean = false

    open fun getValue(): T = data

    open fun setValue(value: T) {
        this.data = value
    }

    /**
     * used by [DataSynchronizer] to get value only
     */
    internal fun getValueByPass() = data

    /**
     * used by [DataSynchronizer] to set value only
     */
    internal fun setValueByPass(value: T) {
        this.data = value
    }

    open fun updateIfNecessary() = false

    open fun update() {}

    fun consumeDirty(): Boolean {
        val state = dirty
        dirty = false
        return state
    }

    fun markDirty() {
        this.dirty = true
    }
}