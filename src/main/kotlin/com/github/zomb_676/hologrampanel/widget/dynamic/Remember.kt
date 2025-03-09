package com.github.zomb_676.hologrampanel.widget.dynamic

import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.util.DistType
import com.github.zomb_676.hologrampanel.util.unsafeCast
import com.github.zomb_676.hologrampanel.widget.component.ComponentProvider
import com.github.zomb_676.hologrampanel.widget.component.ServerDataProvider
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import org.jetbrains.annotations.ApiStatus
import kotlin.reflect.KProperty

class Remember<T : HologramContext> private constructor() {
    private val map: MutableMap<ComponentProvider<T>, MutableList<Holder<T, *>>> = mutableMapOf()

    @PublishedApi
    @ApiStatus.Internal
    internal val dirtyMark: Object2BooleanOpenHashMap<ComponentProvider<T>> = Object2BooleanOpenHashMap()
    private var provider: ComponentProvider<T>? = null

    private val servers: Int2ObjectOpenHashMap<Holder<T, *>> = Int2ObjectOpenHashMap()
    private val clients: Int2ObjectOpenHashMap<Holder<T, *>> = Int2ObjectOpenHashMap()

    lateinit var context: T

    class Holder<T : HologramContext, V>(
        private val provider: ComponentProvider<T>,
        private val remember: Remember<T>,
        private val updater: (CompoundTag) -> V,
        initial: V
    ) {
        private var cachedValue: V = initial

        operator fun getValue(owner: Any?, property: KProperty<*>): V = this.get()

        /**
         * call this value if you are on java or not use property delegate
         */
        fun get(): V = this.cachedValue

        internal fun tryUpdate(tag: CompoundTag) {
            val newValue = updater.invoke(tag)
            if (newValue != this.cachedValue) {
                this.cachedValue = newValue
                this.remember.markDirty(this.provider)
            }
        }
    }

    private fun markDirty(provider: ComponentProvider<T>) {
        this.dirtyMark.put(provider, true)
    }

    internal inline fun providerScope(provider: ComponentProvider<T>, code: () -> Unit) {
        this.provider = provider
        code.invoke()
        this.provider = null
    }

    fun <V> client(identity: Int, initial: V, code: () -> V): Holder<T, V> {
        val provider = this.provider ?: throw RuntimeException()
        val key = calculateKey(identity, provider)
        var res: Holder<T, *>? = clients.get(key)
        if (res == null) {
            res = Holder(provider, this, { code.invoke() }, initial)
            this.addHolder(key, res, DistType.CLIENT)
        }
        return res.unsafeCast()
    }

    fun serverItemStack(identity: Int, keyName: String): Holder<T, ItemStack> {
        return server(identity, ItemStack.EMPTY) { tag ->
            ItemStack.parseOptional(
                context.getLevel().registryAccess(), tag.getCompound(keyName)
            )
        }
    }

    fun <V> server(identity: Int, initial: V, code: (tag: CompoundTag) -> V): Holder<T, V> {
        val provider = this.provider ?: throw RuntimeException()
        require(provider is ServerDataProvider<T>)
        val key = calculateKey(identity, provider)
        var res: Holder<T, *>? = servers.get(key)
        if (res == null) {
            res = Holder(provider, this, code, initial)
            this.addHolder(key, res, DistType.SERVER)
        }
        return res.unsafeCast()
    }

    private fun calculateKey(identity: Int, provider: ComponentProvider<T>): Int {
        var result = identity
        result = 31 * result + provider.hashCode()
        return result
    }

    fun addHolder(key: Int, holder: Holder<T, *>, side: DistType) {
        this.map.computeIfAbsent(this.provider!!) { mutableListOf() }.add(holder)
        this.dirtyMark.put(this.provider, true)
        when (side) {
            DistType.CLIENT -> clients
            DistType.SERVER -> servers
        }.put(key, holder)
    }

    fun onReceiveData(tag: CompoundTag) {
        this.servers.values.forEach { it.tryUpdate(tag) }
    }

    companion object {
        private val MIMIC_EMPTY_TAG = CompoundTag()
        fun <T : HologramContext> create(context: T): Remember<T> = Remember<T>().also {
            it.context = context
        }
    }

    fun tickClient() {
        this.clients.values.forEach { it.tryUpdate(MIMIC_EMPTY_TAG) }
    }

    inline fun consumerRebuild(code: (provider: ServerDataProvider<T>) -> Unit) {
        val iterator = this.dirtyMark.object2BooleanEntrySet().fastIterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (!next.booleanValue) {
                code.invoke(next.key.unsafeCast())
                next.setValue(false)
            }
        }
    }
}