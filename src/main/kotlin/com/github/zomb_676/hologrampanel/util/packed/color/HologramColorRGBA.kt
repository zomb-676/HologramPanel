package com.github.zomb_676.hologrampanel.util.packed.color

/**
 * pack 4 8-bit usage int into a 32-bit int via bit operation
 *
 * create instance via [HologramColor]
 */
@JvmInline
value class HologramColorRGBA private constructor(val color: Int) {
    companion object {
        const val RED_SHIFT = 24
        const val GREEN_SHIFT = 16
        const val BLUE_SHIFT = 8
        const val ALPHA_SHIFT = 0

        internal fun create(color: Int): HologramColorRGBA = HologramColorRGBA(color)
        internal fun create(r: Int, g: Int, b: Int, a: Int): HologramColorRGBA =
            create((r shl RED_SHIFT) or (g shl GREEN_SHIFT) or (b shl BLUE_SHIFT) or (a shl ALPHA_SHIFT))
    }

    inline val red get() : Int = (color ushr RED_SHIFT) and 0xff
    inline val green get(): Int = (color ushr GREEN_SHIFT) and 0xff
    inline val blue get(): Int = (color ushr BLUE_SHIFT) and 0xff
    inline val alpha get(): Int = (color ushr ALPHA_SHIFT) and 0xff

    fun red(r: Int) = HologramColorRGBA(color and (0xff shl RED_SHIFT).inv() or (r shl RED_SHIFT))
    fun green(g: Int) = HologramColorRGBA(color and (0xff shl GREEN_SHIFT).inv() or (g shl GREEN_SHIFT))
    fun blue(b: Int) = HologramColorRGBA(color and (0xff shl BLUE_SHIFT).inv() or (b shl BLUE_SHIFT))
    fun alpha(a: Int) = HologramColorRGBA(color and (0xff shl ALPHA_SHIFT).inv() or (a shl ALPHA_SHIFT))

    inline val r get() = red / HologramColor.CHANNEL_MAX_VALUE_FLOAT
    inline val g get() = green / HologramColor.CHANNEL_MAX_VALUE_FLOAT
    inline val b get() = blue / HologramColor.CHANNEL_MAX_VALUE_FLOAT
    inline val a get() = alpha / HologramColor.CHANNEL_MAX_VALUE_FLOAT

    inline val argb get() = HologramColor.argb((color ushr 8) or (alpha shl HologramColorARGB.ALPHA_SHIFT))
}