package com.github.zomb_676.hologrampanel.util

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import java.util.concurrent.TimeUnit
import java.util.function.BiPredicate

/**
 * search and cache a mapping from [Class] to [T] by the Inherit Tree
 */
class InheritSearcher<T> {
    /**
     * use a cache mapping actual type to all available providers, already considering Inherit Tree
     */
    private val providerCache: Cache<Class<*>, List<T>> = CacheBuilder.newBuilder()
        .expireAfterAccess(120, TimeUnit.SECONDS)
        .build()

    /**
     * mapping from class to the [T]
     */
    private val classProvider: MutableMap<Class<*>, MutableList<T>> = mutableMapOf()

    fun resetCache() = providerCache.invalidateAll()

    fun resetMapper() = classProvider.clear()
    fun getMutableMapper() = classProvider


    private fun searchByInheritTree(c: Class<*>, list: MutableList<T>) {
        val target = classProvider[c]
        if (target != null) {
            list.addAll(target)
        }
        c.interfaces.forEach {
            searchByInheritTree(it, list)
        }
        val sup = c.superclass
        if (sup != null) {
            searchByInheritTree(sup, list)
        }
    }

    fun <I : Any?> collectByInstance(targetInstance: I, code: BiPredicate<I, T>): List<T> {
        if (targetInstance == null) return listOf()

        val targetClass: Class<out I> = targetInstance::class.java
        val res = providerCache.getIfPresent(targetClass)
        if (res != null) return res

        val container = mutableListOf<T>()
        searchByInheritTree(targetClass, container)
        val finalRes = if (container.isEmpty()) {
            listOf()
        } else {
            container.removeIf { t -> !code.test(targetInstance, t) }
            container
        }
        providerCache.put(targetClass, finalRes)
        return finalRes
    }
}