package com.github.zomb_676.hologrampanel.addon.vanilla

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import io.netty.buffer.Unpooled
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntArrayTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.BeehiveBlock
import net.minecraft.world.level.block.entity.BeehiveBlockEntity

data object  BeeHiveProvider : ServerDataProvider<BlockHologramContext, BeehiveBlock> {
    override fun appendServerData(
        additionData: CompoundTag,
        targetData: CompoundTag,
        context: BlockHologramContext
    ): Boolean {
        val beehive = context.getBlockEntity<BeehiveBlockEntity>() ?: return true
        val bees: List<BeehiveBlockEntity.BeeData> = beehive.stored
        val count = bees.size
        targetData.putInt("bee_count", count)
        repeat(count) { index ->
            val data = bees[index]
            val buffer = Unpooled.buffer()
            targetData.putInt("bee_${index}_ticks_in_hive", data.ticksInHive)
            BeehiveBlockEntity.Occupant.STREAM_CODEC.encode(buffer, data.occupant)
            targetData.putByteArray("bee_$index", buffer.array())
        }
        return true
    }

    override fun appendComponent(
        builder: HologramWidgetBuilder<BlockHologramContext>,
        displayType: DisplayType
    ) {
        val context = builder.context
        val remember = context.getRememberData()
        val data by remember.server(1, listOf()) { tag ->
            val count = tag.getInt("bee_count")
            List(count) { index ->
                val tick = tag.getInt("bee_${index}_ticks_in_hive")
                val byteBuffer = Unpooled.wrappedBuffer(tag.getByteArray("bee_$index"))
                val data = BeehiveBlockEntity.Occupant.STREAM_CODEC.decode(byteBuffer)
                BeehiveBlockEntity.BeeData(data)
            }
        }
        val beeCount = data.size
        if (beeCount != 0) {
            val tag = context.attachedServerData()!!
            builder.single("bee_count") {
                text("Bee Count:$beeCount")
            }
            repeat(beeCount) { index ->
                val tick = tag.getInt("bee_${index}_ticks_in_hive")
                val byteBuffer = Unpooled.wrappedBuffer(tag.getByteArray("bee_$index"))
                val data = BeehiveBlockEntity.Occupant.STREAM_CODEC.decode(byteBuffer)
                builder.lazyGroup("Bee$index", "Bee ${index + 1}") {
                    builder.single("in_hive") { text("inHive:$tick/${data.minTicksInHive()}") }
                    @Suppress("DEPRECATION") val pos = data.entityData().unsafe.get("flower_pos")
                    if (pos != null && pos.type == IntArrayTag.TYPE) {
                        val tag = (pos as IntArrayTag)
                        builder.single("flower_pos") { text("flower pos:${tag[0]} ${tag[1]} ${tag[2]}") }
                    }
                }
            }
        }
    }

    override fun targetClass(): Class<BeehiveBlock> = BeehiveBlock::class.java

    override fun location(): ResourceLocation = HologramPanel.Companion.rl("bee_hive")
}