package com.github.zomb_676.hologrampanel.widget.element

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.packed.Size
import net.minecraft.resources.ResourceLocation

class RawTextureAtlasSpriteRenderElement(
    val atlas: ResourceLocation,
    val uOffset: Int,
    val vOffset: Int,
    val uWidth: Int,
    val vWidth: Int,
) : RenderElement() {

    private var width = uWidth
    private var height = uWidth
    private var textureWidth = 256
    private var textureHeight = 256

    override fun measureContentSize(style: HologramStyle): Size {
        return Size.of(width, height).scale()
    }

    override fun render(
        style: HologramStyle, partialTicks: Float
    ) {
        val size = this.contentSize
        style.guiGraphics.blit(
            atlas, 0, size.width, 0, size.height, 0,
            uWidth, vWidth, uOffset.toFloat(), vOffset.toFloat(),
            textureWidth, textureHeight
        )
    }

    fun setRenderSize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    fun setTextureSize(width: Int, height: Int) {
        this.textureWidth = width
        this.textureHeight = height
    }

    override fun toString(): String {
        return "RawSprite(atlas=$atlas, vWidth=$vWidth, uWidth=$uWidth, vOffset=$vOffset, uOffset=$uOffset)"
    }
}