package com.github.zomb_676.hologrampanel.util.packed.color

object HologramColor {
    const val CHANNEL_MAX_COLOR = 0Xff
    const val CHANNEL_MAX_VALUE_FLOAT = CHANNEL_MAX_COLOR.toFloat()

    fun rgba(color: Int): HologramColorRGBA {
        return HologramColorRGBA.create(color)
    }

    fun rgba(r: Int, g: Int, b: Int, a: Int): HologramColorRGBA {
        return HologramColorRGBA.create(r, g, b, a)
    }

    fun rgba(r: Float, g: Float, b: Float, a: Float): HologramColorRGBA {
        return HologramColorRGBA.create((r * 255f).toInt(), (g * 255f).toInt(), (b * 255f).toInt(), (a * 255f).toInt())
    }

    fun argb(color: Int): HologramColorARGB {
        return HologramColorARGB.create(color)
    }

    fun argb(a: Int, r: Int, g: Int, b: Int): HologramColorARGB {
        return HologramColorARGB.create(a, r, g, b)
    }

    fun argb(a: Float, r: Float, g: Float, b: Float): HologramColorARGB {
        return HologramColorARGB.create((a * 255f).toInt(), (r * 255f).toInt(), (g * 255f).toInt(), (b * 255f).toInt())
    }
}