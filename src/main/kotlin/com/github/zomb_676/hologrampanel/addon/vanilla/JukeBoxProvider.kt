package com.github.zomb_676.hologrampanel.addon.vanilla

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.addon.universial.UniversalContainerBlockProvider
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.RecordItem
import net.minecraft.world.level.block.JukeboxBlock
import net.minecraft.world.level.block.entity.JukeboxBlockEntity
import kotlin.jvm.optionals.getOrNull

data object JukeBoxProvider : ServerDataProvider<BlockHologramContext, JukeboxBlock> {
    override fun appendServerData(
        additionData: CompoundTag,
        targetData: CompoundTag,
        context: BlockHologramContext
    ): Boolean {
        val jukebox = context.getBlockEntity<JukeboxBlockEntity>() ?: return true
        targetData.put("song_item", jukebox.items[0].save(CompoundTag()))
        targetData.putLong("song_started", jukebox.tickCount - jukebox.recordStartedTick)
        return true
    }

    override fun appendComponent(
        builder: HologramWidgetBuilder<BlockHologramContext>,
        displayType: DisplayType
    ) {
        val context = builder.context
        val remember = context.getRememberData()

        val songItem by remember.server(0, ItemStack.EMPTY) { tag -> ItemStack.of(tag.getCompound("song_item")) }
        val songStarted by remember.server(1, 0) { tag -> tag.getLong("song_started") }

        if (!songItem.isEmpty) {
            val item = songItem.item as? RecordItem? ?: return
            val lengthInTicks = item.lengthInTicks
            builder.single("song_item") {
                itemStack("song_item_logo", songItem)
                val remain = lengthInTicks - songStarted
                if (remain > 0) {
                    component("song_item_description", item.displayName).setPositionOffset(0, 3)
                    text("song_length", "${(lengthInTicks - songStarted) / 20}s").setPositionOffset(5, 3)
                }
            }
        }
    }

    override fun targetClass(): Class<JukeboxBlock> = JukeboxBlock::class.java

    override fun location(): ResourceLocation = HologramPanel.Companion.rl("juke_box")

    override fun replaceProvider(target: ResourceLocation): Boolean =
        target == UniversalContainerBlockProvider.location()
}