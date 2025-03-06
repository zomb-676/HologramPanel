package com.github.zomb_676.hologrampanel.api

/**
 * indicate that the value returned will never change, you can save if safely
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.SOURCE)
annotation class EfficientConst {
    annotation class ConstAfterNotNull
    annotation class ConstDuringValidLifeCycle
}