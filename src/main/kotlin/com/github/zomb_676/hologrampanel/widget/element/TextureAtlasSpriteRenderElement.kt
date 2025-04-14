package com.github.zomb_676.hologrampanel.widget.element

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.util.packed.Size
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite

class TextureAtlasSpriteRenderElement(val sprite: TextureAtlasSprite) : RenderElement() {
    companion object {
        @Suppress("DEPRECATION")
        val missing: TextureAtlasSprite =
            Minecraft.getInstance().modelManager.getAtlas(TextureAtlas.LOCATION_BLOCKS)
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
        style.guiGraphics.blitSprite(RenderType::guiTextured, sprite, 0, 0, size.width, size.height)
    }

    fun setRenderSize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    override fun toString(): String {
        return "Sprite(sprite=$sprite, width=$width, height=$height)"
    }
}