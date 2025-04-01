package com.github.zomb_676.hologrampanel.addon.vanilla

import com.github.zomb_676.hologrampanel.HologramPanel
import com.github.zomb_676.hologrampanel.addon.universial.UniversalContainerBlockProvider
import com.github.zomb_676.hologrampanel.api.ServerDataProvider
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.util.ProgressData
import com.github.zomb_676.hologrampanel.util.extractArray
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import com.github.zomb_676.hologrampanel.widget.dynamic.IRenderElement
import io.netty.buffer.ByteBuf
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.CampfireBlock
import net.minecraft.world.level.block.entity.CampfireBlockEntity
import org.apache.http.entity.ByteArrayEntity
import java.util.Arrays

data object CampfireProvider : ServerDataProvider<BlockHologramContext, CampfireBlock> {
    override fun appendServerData(
        additionData: CompoundTag,
        targetData: CompoundTag,
        context: BlockHologramContext
    ): Boolean {
        val campfire = context.getBlockEntity<CampfireBlockEntity>() ?: return true
        val buffer = context.createRegistryFriendlyByteBuf()
        for (progress in campfire.cookingProgress) {
            buffer.writeVarInt(progress)
        }
        for (progress in campfire.cookingTime) {
            buffer.writeVarInt(progress)
        }
        for (item in campfire.items) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, item)
        }
        targetData.putByteArray("campfire", buffer.extractArray())
        return true
    }

    override fun appendComponent(
        builder: HologramWidgetBuilder<BlockHologramContext>,
        displayType: DisplayType
    ) {
        val context = builder.context
        val remember = context.getRememberData()
        val progresses = remember.keep(0) { List(4) { ProgressData() } }
        val data by remember.server(1, ByteArray(0), Arrays::equals) { tag -> tag.getByteArray("campfire") }
        if (data.isEmpty()) return
        val buffer = context.warpRegistryFriendlyByteBuf(data)
        val cookingProgresses = IntArray(4) { buffer.readVarInt() }
        val cookingTimes = IntArray(4) { buffer.readVarInt() }
        val items = List(4) { ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer) }

        if (items.all(ItemStack::isEmpty)) return
        builder.single("cooking") {
            repeat(4) { index ->
                val itemStack = items[index]
                if (itemStack.isEmpty) return@repeat
                val cookingProgress = cookingProgresses[index]
                val progress = progresses[index].current(cookingProgress).max(cookingTimes[index])
                itemStack(itemStack).setPositionOffset(2, 2).noCalculateSize()
                workingTorusProgress(progress)
            }
        }
    }

    override fun targetClass(): Class<CampfireBlock> = CampfireBlock::class.java

    override fun location(): ResourceLocation = HologramPanel.Companion.rl("camp_fire")

    override fun replaceProvider(target: ResourceLocation): Boolean =
        target == UniversalContainerBlockProvider.location()
}