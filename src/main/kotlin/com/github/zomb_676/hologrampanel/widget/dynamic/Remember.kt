package com.github.zomb_676.hologrampanel.widget.dynamic

import com.github.zomb_676.hologrampanel.Config
import com.github.zomb_676.hologrampanel.api.ComponentProvider
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.HologramContext
import com.github.zomb_676.hologrampanel.util.DistType
import com.github.zomb_676.hologrampanel.util.unsafeCast
import com.google.common.collect.ImmutableMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import org.jetbrains.annotations.ApiStatus
import java.util.*
import kotlin.reflect.KProperty

/**
 * update value on server package arrival and mark dirty when data actual change
 *
 * it is intended to re-build the widget when data actual change and only rebuild the necessary part
 *
 * inspired by jetpack-compose
 */
class Remember<T : HologramContext> private constructor() {
    val uuid: UUID = UUID.randomUUID()
    private val map: MutableMap<ComponentProvider<T, *>, MutableList<Holder<T, *>>> = mutableMapOf()
    private val requireMimicTick: MutableList<Holder<T, *>> = mutableListOf()

    @PublishedApi
    @ApiStatus.Internal
    internal val dirtyMark: Object2BooleanOpenHashMap<ComponentProvider<T, *>> = Object2BooleanOpenHashMap()
    private var provider: ComponentProvider<T, *>? = null

    private val servers: Int2ObjectOpenHashMap<Holder<T, *>> = Int2ObjectOpenHashMap()
    private val clients: Int2ObjectOpenHashMap<Holder<T, *>> = Int2ObjectOpenHashMap()
    private val keeps: Int2ObjectOpenHashMap<Any> = Int2ObjectOpenHashMap()

    lateinit var context: T

    class Holder<T : HologramContext, V>(
        internal val provider: ComponentProvider<T, *>,
        private val remember: Remember<T>,
        private val updater: (CompoundTag) -> V,
        initial: V,
        val equal: (V, V) -> Boolean
    ) {
        private var cachedValue: V = initial
        private var mimicTickValue: V = cachedValue
        private var lastValueSynced = 0
        private var mimicTick: ((V) -> V)? = null

        operator fun getValue(owner: Any?, property: KProperty<*>): V = this.get()

        /**
         * call this value if you are on java or not use property delegate
         */
        fun get(): V = if (mimicTick != null) {
            mimicTickValue
        } else {
            cachedValue
        }

        /**
         * mark dirty if the value really changed
         */
        internal fun tryUpdate(tag: CompoundTag) {
            val newValue = updater.invoke(tag)
            this.lastValueSynced = 0
            if (!equal.invoke(newValue, this.cachedValue)) {
                this.cachedValue = newValue
                this.mimicTickValue = newValue
                this.remember.markDirty(this.provider)
            }
        }

        /**
         * in most server, server will not sync to clients every tick
         *
         * so clients can do mimic tick to simulate the change for smooth animation in widgets
         */
        fun clientMimicTick(tick: (V) -> V): Holder<T, V> {
            require(this.mimicTickValue == null)
            this.mimicTick = tick
            this.remember.requireMimicTick.add(this)
            return this
        }

        fun tickMimic() {
            if (this.lastValueSynced < Config.Server.updateInternal.get()) {
                val tickFunction = this.mimicTick!!
                this.mimicTickValue = tickFunction.invoke(this.mimicTickValue)
                this.lastValueSynced++
            }
        }
    }

    private fun markDirty(provider: ComponentProvider<T, *>) {
        this.dirtyMark.put(provider, true)
    }

    internal inline fun providerScope(provider: ComponentProvider<T, *>, code: () -> Unit) {
        this.provider = provider
        code.invoke()
        this.provider = null
    }

    /**
     * do nothing just to avoid meanness new
     *
     * @param identity used to locate identity during multi call
     */
    fun <V : Any> keep(identity: Int, data: () -> V): V {
        val key = calculateKey(identity, data)
        var res = keeps.get(key)
        if (res == null) {
            res = data.invoke()
            keeps.put(key, res)
        }
        return res.unsafeCast()
    }

    /**
     * the value source is stored on clients which do not need server sync
     *
     * @param identity used to locate identity during multi call
     * @param initial the initial value
     * @param equal the equal function, by default, [Objects.equals], support box primary type
     * @param code the function to update the value
     */
    fun <V> client(identity: Int, initial: V, equal: (V, V) -> Boolean = Objects::equals, code: () -> V): Holder<T, V> {
        val provider = this.provider ?: throw RuntimeException()
        val key = calculateKey(identity, provider)
        var res: Holder<T, *>? = clients.get(key)
        if (res == null) {
            res = Holder(provider, this, { code.invoke() }, initial, equal)
            this.addHolder(key, res, DistType.CLIENT)
        }
        return res.unsafeCast()
    }

    /**
     * @param equals use [ItemStack.matches], consider [net.minecraft.core.component.DataComponentType] data
     */
    fun serverItemStack(
        identity: Int,
        keyName: String,
        equals: (ItemStack, ItemStack) -> Boolean = ItemStack::matches
    ): Holder<T, ItemStack> {
        return server(identity, ItemStack.EMPTY, equals) { tag ->
            ItemStack.parseOptional(
                context.getLevel().registryAccess(), tag.getCompound(keyName)
            )
        }
    }

    /**
     * the value requires data synced from server
     *
     * @param identity used to locate identity during multi call
     * @param initial the initial value which works as a predicate to check is package has arrived or not
     * @param equal the equal function, by default, [Objects.equals], support box primary type
     * @param code decode actual data from server
     */
    fun <V> server(
        identity: Int,
        initial: V,
        equal: (V, V) -> Boolean = Objects::equals,
        code: (tag: CompoundTag) -> V
    ): Holder<T, V> {
        val provider = this.provider ?: throw RuntimeException()
        require(provider is ServerDataProvider<T, *>)
        val key = calculateKey(identity, provider)
        var res: Holder<T, *>? = servers.get(key)
        if (res == null) {
            res = Holder(provider, this, code, initial, equal)
            this.addHolder(key, res, DistType.SERVER)
        }
        return res.unsafeCast()
    }

    /**
     * the function to calculate the actual identity key
     */
    private fun calculateKey(identity: Int, provider: Any): Int {
        var result = identity
        result = 31 * result + provider.hashCode()
        return result
    }

    private fun addHolder(key: Int, holder: Holder<T, *>, side: DistType) {
        this.map.computeIfAbsent(this.provider!!) { mutableListOf() }.add(holder)
        this.dirtyMark.put(this.provider, true)
        when (side) {
            DistType.CLIENT -> clients
            DistType.SERVER -> servers
        }.put(key, holder)
    }

    fun onReceiveData(data: ImmutableMap<ComponentProvider<*, *>, CompoundTag>) {
        this.servers.values.forEach { holder ->
            holder.tryUpdate(data.getValue(holder.provider))
        }
    }

    companion object {
        /**
         * the object for [client] type holders
         */
        private val MIMIC_EMPTY_TAG = CompoundTag()
        fun <T : HologramContext> create(context: T): Remember<T> = Remember<T>().also {
            it.context = context
        }
    }

    /**
     * tick [client] type holders
     */
    fun tickClientValueUpdate() {
        this.clients.values.forEach { it.tryUpdate(MIMIC_EMPTY_TAG) }
    }

    /**
     * tick [Holder.mimicTick] holders
     */
    fun tickMimicClientUpdate() {
        this.requireMimicTick.forEach { it.tickMimic() }
    }

    /**
     * check if the provider is marked as dirty and need re-build, and clean dirty mark
     */
    fun consumerRebuild(provider: ComponentProvider<T, *>): Boolean {
        val value = this.dirtyMark.getBoolean(provider)
        this.dirtyMark.put(provider, false)
        return value
    }

    /**
     * return all providers that need server data
     */
    fun serverDataEntries() = this.servers
        .values.asSequence().map { it.provider }.distinct().toList().unsafeCast<List<ServerDataProvider<T, *>>>()

    /**
     * return all the providers that use the [Remember] object
     */
    fun providers() = this.dirtyMark.keys

    fun needUpdate() = this.dirtyMark.values.contains(true)
}