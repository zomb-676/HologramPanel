package com.github.zomb_676.hologrampanel.widget.element

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.packed.Size
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.data.AtlasIds

/**
 * render a [TextureAtlasSprite], original use its size as element size
 *
 * overwrite it by use [setRenderSize]
 */
class TextureAtlasSpriteRenderElement(val sprite: TextureAtlasSprite) : RenderElement() {
    companion object {
        @Suppress("DEPRECATION")
        val missing: TextureAtlasSprite =
            Minecraft.getInstance().atlasManager.getAtlasOrThrow(AtlasIds.BLOCKS)// FIXME: Is this right?
                .getSprite(MissingTextureAtlasSprite.getLocation())
    }

    private var width = sprite.contents().width()
    private var height = sprite.contents().height()

    override fun measureContentSize(style: HologramStyle): Size {
        return Size.Companion.of(width, height).scale()
    }

    override fun render(
        style: HologramStyle, partialTicks: Float
    ) {
        val size = this.contentSize
        style.guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, 0, 0, size.width, size.height)
    }

    fun setRenderSize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    override fun toString(): String {
        return "Sprite(sprite=$sprite, width=$width, height=$height)"
    }
}