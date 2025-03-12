package com.github.zomb_676.hologrampanel.api

/**
 * the annotated class will be considered as a HologramPanel Plugin
 *
 * it must have a constructor with no parameter
 *
 * @property enable change this if you want to disable your plugin for debug usage
 */
annotation class HologramPlugin(val enable: Boolean = true)