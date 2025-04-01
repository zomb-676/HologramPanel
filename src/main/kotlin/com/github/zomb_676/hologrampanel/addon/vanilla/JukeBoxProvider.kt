package com.github.zomb_676.hologrampanel.addon.vanilla

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.addon.universial.UniversalContainerBlockProvider
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
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
        targetData.put("song_item", jukebox.item.saveOptional(context.getLevel().registryAccess()))
        targetData.putLong("song_started", jukebox.songPlayer.ticksSinceSongStarted)
        return true
    }

    override fun appendComponent(
        builder: HologramWidgetBuilder<BlockHologramContext>,
        displayType: DisplayType
    ) {
        val context = builder.context
        val remember = context.getRememberData()

        val songItem by remember.serverItemStack(0, "song_item")
        val songStarted by remember.server(1, 0) { tag -> tag.getLong("song_started") }

        if (!songItem.isEmpty) {
            val playable = songItem.components.get(DataComponents.JUKEBOX_PLAYABLE) ?: return
            val song =
                playable.song().unwrap(context.getLevel().registryAccess()).getOrNull()?.value() ?: return
            builder.single("song_item") {
                itemStack(songItem)
                component(song.description().copy()).setPositionOffset(0, 3)
                text("${(song.lengthInTicks() - songStarted)/20}s").setPositionOffset(5,3)
            }
        }
    }

    override fun targetClass(): Class<JukeboxBlock> = JukeboxBlock::class.java

    override fun location(): ResourceLocation = HologramPanel.Companion.rl("juke_box")

    override fun replaceProvider(target: ResourceLocation): Boolean =
        target == UniversalContainerBlockProvider.location()
}