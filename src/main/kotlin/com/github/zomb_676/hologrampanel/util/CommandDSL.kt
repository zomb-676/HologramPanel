package com.github.zomb_676.hologrampanel.util

import com.github.zomb_676.hologrampanel.HologramPanel.Companion.LOGGER
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.RedirectModifier
import com.mojang.brigadier.SingleRedirectModifier
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.CommandNode
import java.util.function.Predicate

@JvmInline
@Suppress("unused")
value class CommandDSL<S>(
    val dispatcher: CommandDispatcher<S>,
) {
    inline operator fun String.invoke(block: WithNode<S, LiteralArgumentBuilder<S>>.() -> Unit) {
        val n = LiteralArgumentBuilder.literal<S>(this)
        block(WithNode(n))
        dispatcher.register(n)
    }

    @JvmInline
    value class WithNode<S, T : ArgumentBuilder<S, T>>(
        val node: ArgumentBuilder<S, T>
    ) {
        inline operator fun String.invoke(block: WithNode<S, LiteralArgumentBuilder<S>>.() -> Unit) {
            val n = LiteralArgumentBuilder.literal<S>(this)
            block(WithNode(n))
            node.then(n)
        }

        inline operator fun <Arg> String.invoke(
            pType: ArgumentType<Arg>,
            block: WithNode<S, RequiredArgumentBuilder<S, Arg>>.() -> Unit
        ) {
            val n = RequiredArgumentBuilder.argument<S, Arg>(this, pType)
            block(WithNode(n))
            node.then(n)
        }

        inline fun execute(crossinline block: CommandContext<S>.() -> Unit) {
            node.executes { context ->
                try {
                    block(context)
                    return@executes Command.SINGLE_SUCCESS
                    //so no need to return Command.SINGLE_SUCCESS manually
                    //can just write code and just throw when error happened
                } catch (e: Exception) {
                    LOGGER.catching(e)
                    return@executes 2
                }
            }
        }

        inline fun require(predicate: Predicate<S>, block: WithNode<S, T>.() -> Unit) {
            val n = node.requires(predicate)
            block(WithNode(n))
        }

        inline fun redirect(target: CommandNode<S>, block: WithNode<S, T>.() -> Unit) {
            val n = node.redirect(target)
            block(WithNode(n))
        }

        inline fun redirect(
            target: CommandNode<S>,
            modifier: SingleRedirectModifier<S>,
            block: WithNode<S, T>.() -> Unit
        ) {
            val n = node.redirect(target, modifier)
            block(WithNode(n))
        }

        inline fun fork(target: CommandNode<S>, modifier: RedirectModifier<S>, block: WithNode<S, T>.() -> Unit) {
            val n = node.fork(target, modifier)
            block(WithNode((n)))
        }

        inline fun forward(
            target: CommandNode<S>,
            modifier: RedirectModifier<S>,
            fork: Boolean,
            block: WithNode<S, T>.() -> Unit
        ) {
            val n = node.forward(target, modifier, fork)
            block(WithNode(n))
        }

        inline val arguments get() = node.arguments
        inline val command get() = node.command
        inline val requirement get() = node.requirement
        inline val redirect get() = node.redirect
        inline val redirectModifier get() = node.redirectModifier
        inline val isFork get() = node.isFork
    }
}