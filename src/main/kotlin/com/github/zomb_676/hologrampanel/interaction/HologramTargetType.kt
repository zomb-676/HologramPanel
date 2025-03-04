package com.github.zomb_676.hologrampanel.interaction

import com.github.zomb_676.hologrampanel.interaction.HologramTargetType.Extractor
import com.github.zomb_676.hologrampanel.interaction.HologramTargetType.Tester
import com.google.common.base.Predicate
import net.minecraft.core.BlockPos
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.capabilities.Capabilities

@Suppress("EXPOSED_SUPER_INTERFACE", "EXPOSED_PROPERTY_TYPE")
interface HologramTargetType<T> : Extractor<T>, Tester<T> {
    val type: Class<T>

    fun extract(pos: BlockPos, accessor: LevelAccessor): T? {
        val candidate: T = this.getCandidate(pos, accessor) ?: return null
        return if (this.satisfy(candidate)) candidate else null
    }

    fun interface Extractor<T> {
        fun getCandidate(pos: BlockPos, accessor: LevelAccessor): T?
    }

    fun interface Tester<T> : Predicate<T> {
        fun satisfy(candidate: T): Boolean
        override fun apply(input: T): Boolean = satisfy(input)
    }

    companion object {

        inline fun <reified T> create(
            crossinline extractor: (pos: BlockPos, accessor: LevelAccessor) -> T?, crossinline tester: (T) -> Boolean
        ) = object : HologramTargetType<T> {
            override val type: Class<T> get() = T::class.java

            override fun getCandidate(pos: BlockPos, level: LevelAccessor): T? = extractor.invoke(pos, level)

            override fun satisfy(candidate: T): Boolean = tester.invoke(candidate)
        }

        inline fun <reified T> create(extractor: Extractor<T>, tester: Tester<T>) = object : HologramTargetType<T> {
            override val type: Class<T> get() = T::class.java
            override fun getCandidate(pos: BlockPos, accessor: LevelAccessor): T? =
                extractor.getCandidate(pos, accessor)

            override fun satisfy(candidate: T): Boolean = tester.satisfy(candidate)
        }

        object Extractors {
            val BLOCK: Extractor<BlockState> = Extractor { pos, accessor -> accessor.getBlockState(pos) }
            val BLOCK_ENTITY: Extractor<BlockEntity> = Extractor { pos, accessor -> accessor.getBlockEntity(pos) }
        }

        object Testers {
            val HAS_ITEM_HANDLE_BLOCK_ENTITY: Tester<BlockEntity> = Tester { be ->
                be.level?.getCapability(Capabilities.ItemHandler.BLOCK, be.blockPos, be.blockState, be, null) != null
            }
        }

        val BLOCK = create<BlockState>({ pos, accessor -> accessor.getBlockState(pos) }, { state -> !state.isAir })

        val BLOCK_ENTITY_WITH_ITEM_CAPABILITY =
            create<BlockEntity>(Extractors.BLOCK_ENTITY, Testers.HAS_ITEM_HANDLE_BLOCK_ENTITY)

        val DEFAULTS = mutableListOf<HologramTargetType<*>>(BLOCK_ENTITY_WITH_ITEM_CAPABILITY)
    }
}