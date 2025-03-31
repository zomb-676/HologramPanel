package com.github.zomb_676.hologrampanel.api

import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.FluidType

/**
 * register and setup setting on both physical dist
 */
class HologramCommonRegistration(val plugin: IHologramPlugin) {

    internal val blockProviders: MutableList<ComponentProvider<BlockHologramContext, *>> = mutableListOf()
    internal val entityProviders: MutableList<ComponentProvider<EntityHologramContext, *>> = mutableListOf()

    /**
     * register [BlockHologramContext] provider
     */
    fun registerBlockComponent(provider: ComponentProvider<BlockHologramContext, *>) {
        blockProviders.add(provider)
        require(!checkType(provider.targetClass(), Entity::class.java)) {
            "block provider located in ${provider.location()} provided by ${plugin.location()} should not target Entity class"
        }
    }

    /**
     * register [EntityHologramContext] provider
     */
    fun registerEntityComponent(provider: ComponentProvider<EntityHologramContext, *>) {
        entityProviders.add(provider)
        require(!checkType(provider.targetClass(), Block::class.java)) {
            "entity provider located in ${provider.location()} provided by ${plugin.location()} should not target Block class"
        }
        require(!checkType(provider.targetClass(), BlockEntity::class.java)) {
            "entity provider located in ${provider.location()} provided by ${plugin.location()} should not target BlockEntity class"
        }
        require(!checkType(provider.targetClass(), FluidType::class.java)) {
            "entity provider located in ${provider.location()} provided by ${plugin.location()} should not target FluidType class"
        }
    }

    companion object {
        /**
         * check if [child] can be up cast to [parent]
         */
        private fun checkType(child: Class<*>, parent: Class<*>): Boolean {
            if (child == parent) return true
            val superClass = child.superclass
            if (superClass != null && checkType(superClass, parent)) {
                return true
            }
            return child.interfaces.any { i -> checkType(i, parent) }
        }
    }
}