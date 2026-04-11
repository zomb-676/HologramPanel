package com.github.zomb_676.hologrampanel.widget.element.progress

import com.github.zomb_676.hologrampanel.render.HologramStyle
import com.github.zomb_676.hologrampanel.render.state.FloatBlitRenderState.Companion.innerBlit
import com.github.zomb_676.hologrampanel.util.ProgressData
import com.github.zomb_676.hologrampanel.widget.element.IRenderElement.Companion.shortDescription
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.network.chat.Component
import net.minecraft.world.level.material.Fluid

class FluidBarElement(progress: ProgressData, val fluid: Fluid) : ProgressBarElement(progress) {
    override fun requireOutlineDecorate(): Boolean = true

    override fun fillBar(style: HologramStyle, left: Float, right: Float, height: Float, percent: Float) {
        val state = fluid.defaultFluidState()
        val model = Minecraft.getInstance().modelManager.fluidStateModelSet.get(state)
        val sprite: TextureAtlasSprite = model.stillMaterial.sprite

        val maxU = ((sprite.u1 - sprite.u0) * percent) + sprite.u0
        val maxV = ((sprite.v1 - sprite.v0) * percent) + sprite.v0

        style.guiGraphics.innerBlit(RenderPipelines.GUI_TEXTURED, sprite.atlasLocation(),
            left, right,
            0f, height,
            sprite.u0, maxU,
            sprite.v0, maxV,
            model.fluidTintSource?.color(state) ?: -1
        )
    }

    override fun getDescription(percent: Float): Component {
        val current = if (progress.progressCurrent < 1000) {
            "${progress.progressCurrent}mB"
        } else {
            "${shortDescription(progress.progressCurrent.toFloat() / 1000)}B"
        }
        val f = if (progress.progressCurrent == progress.progressMax) {
            current
        } else {
            val max = if (progress.progressMax < 1000) {
                "${progress.progressMax}mB"
            } else {
                "${shortDescription(progress.progressMax.toFloat() / 1000)}B"
            }
            "$current/$max"
        }
        val fluidName = fluid.fluidType.description
        return Component.literal("").append(fluidName).append(" ").append(f)
    }

    override fun toString(): String {
        return "FluidBar(fluid:${fluid.fluidType.description},${super.toString()})"
    }
}