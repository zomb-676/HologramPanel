package com.github.zomb_676.hologrampanel

import com.github.zomb_676.hologrampanel.api.HologramClientRegistration
import com.github.zomb_676.hologrampanel.api.HologramCommonRegistration
import com.github.zomb_676.hologrampanel.api.HologramPlugin
import com.github.zomb_676.hologrampanel.api.IHologramPlugin
import com.github.zomb_676.hologrampanel.interaction.context.BlockHologramContext
import com.github.zomb_676.hologrampanel.interaction.context.EntityHologramContext
import com.github.zomb_676.hologrampanel.widget.DisplayType
import com.github.zomb_676.hologrampanel.widget.component.ComponentProvider
import com.github.zomb_676.hologrampanel.widget.component.ServerDataProvider
import com.github.zomb_676.hologrampanel.widget.dynamic.HologramWidgetBuilder
import com.github.zomb_676.hologrampanel.widget.dynamic.IRenderElement
import io.netty.buffer.Unpooled
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntArrayTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.material.Fluids
import kotlin.jvm.optionals.getOrNull

@HologramPlugin
class BuildInPlugin : IHologramPlugin {
    override fun location(): ResourceLocation = HologramPanel.rl("build_in")

    override fun registerCommon(register: HologramCommonRegistration) {
        register.registerBlockComponent(object : ServerDataProvider<BlockHologramContext> {
            override fun appendComponent(
                builder: HologramWidgetBuilder<BlockHologramContext>,
                displayType: DisplayType
            ) {
                val remember = builder.context.getRememberData()
                val item0 by remember.serverItemStack(0, "furnace_slot_0")
                val item1 by remember.serverItemStack(1, "furnace_slot_1")
                val item2 by remember.serverItemStack(2, "furnace_slot_2")
                val litTimeRemaining by remember.server(3, 0) { tag -> tag.getIntArray("furnace_progress_data")[0] }
                val litTotalTime by remember.server(4, 0) { tag -> tag.getIntArray("furnace_progress_data")[1] }
                val cookingTimer by remember.server(5, 0) { tag -> tag.getIntArray("furnace_progress_data")[2] }
                val cookingTotalTime by remember.server(6, 0) { tag -> tag.getIntArray("furnace_progress_data")[3] }
                val progressBar = remember.keep(7, IRenderElement.ProgressData())

                progressBar.current(cookingTimer).max(cookingTotalTime)

                builder.single {
                    if (!item0.isEmpty) itemStack(item0)
                    if (!item1.isEmpty) itemStack(item1)
                    if (litTimeRemaining != 0) {
                        energyBar(progressBar)
                    }
                    if (!item2.isEmpty) itemStack(item2)
                }
                builder.single {
                    fluid(progressBar, Fluids.WATER.fluidType)
                }
                builder.single {
                    fluid(progressBar, Fluids.LAVA.fluidType)
                }
                builder.single {
                    workingProgress(progressBar)
                }
            }

            override fun targetClass(): Class<*> = AbstractFurnaceBlock::class.java

            override fun location(): ResourceLocation = HologramPanel.rl("abstract_furnace_block")

            override fun appendServerData(
                additionData: CompoundTag, targetData: CompoundTag, context: BlockHologramContext
            ): Boolean {
                val furnace = context.getBlockEntity<AbstractFurnaceBlockEntity>() ?: return true
                targetData.putIntArray(
                    "furnace_progress_data", intArrayOf(
                        furnace.litTimeRemaining, furnace.litTotalTime, furnace.cookingTimer, furnace.cookingTotalTime
                    )
                )
                val items = furnace.items
                val registryAccess = context.getLevel().registryAccess()
                targetData.put("furnace_slot_0", items[0].saveOptional(registryAccess))
                targetData.put("furnace_slot_1", items[1].saveOptional(registryAccess))
                targetData.put("furnace_slot_2", items[2].saveOptional(registryAccess))

                return true
            }
        })

        register.registerBlockComponent(object : ServerDataProvider<BlockHologramContext> {
            override fun appendServerData(
                additionData: CompoundTag,
                targetData: CompoundTag,
                context: BlockHologramContext
            ): Boolean {
                val brewStand = context.getBlockEntity<BrewingStandBlockEntity>() ?: return true
                targetData.putInt("brew_time", brewStand.brewTime)
                targetData.putInt("fuel", brewStand.fuel)
                val registryAccess = context.getLevel().registryAccess()
                val items = brewStand.items
                targetData.put("item0", items[0].saveOptional(registryAccess))
                targetData.put("item1", items[1].saveOptional(registryAccess))
                targetData.put("item2", items[2].saveOptional(registryAccess))
                targetData.put("item3", items[3].saveOptional(registryAccess))
                targetData.put("item4", items[4].saveOptional(registryAccess))
                return true
            }

            override fun appendComponent(
                builder: HologramWidgetBuilder<BlockHologramContext>,
                displayType: DisplayType
            ) {
                val context = builder.context
                val remember = context.getRememberData()
                val breeTime by remember.server(0, 0) { tag -> tag.getInt("brew_time") }
                val fuel by remember.server(1, -1) { tag -> tag.getInt("fuel") }
                val item0 by remember.serverItemStack(2, "item0")
                val item1 by remember.serverItemStack(3, "item1")
                val item2 by remember.serverItemStack(4, "item2")
                val item3 by remember.serverItemStack(5, "item3")
                val item4 by remember.serverItemStack(6, "item4")

                builder.single { text("fuel:$fuel") }
                builder.single { text("breeTime:$breeTime") }
                builder.single {
                    if (!item0.isEmpty) itemStack(item0)
                    if (!item1.isEmpty) itemStack(item1)
                    if (!item2.isEmpty) itemStack(item2)
                    if (!item3.isEmpty) itemStack(item3)
                    if (!item4.isEmpty) itemStack(item4)
                }
            }

            override fun targetClass(): Class<*> = BrewingStandBlock::class.java
            override fun location(): ResourceLocation = HologramPanel.rl("brewing_stand")
        })

        register.registerBlockComponent(object : ServerDataProvider<BlockHologramContext> {
            override fun appendServerData(
                additionData: CompoundTag,
                targetData: CompoundTag,
                context: BlockHologramContext
            ): Boolean {
                val campfire = context.getBlockEntity<CampfireBlockEntity>() ?: return true
                targetData.putIntArray("cooking_progress", campfire.cookingProgress)
                targetData.putIntArray("cooking_time", campfire.cookingTime)
                val items = campfire.items
                val registryAccess = context.getLevel().registryAccess()
                targetData.put("item0", items[0].saveOptional(registryAccess))
                targetData.put("item1", items[1].saveOptional(registryAccess))
                targetData.put("item2", items[2].saveOptional(registryAccess))
                targetData.put("item3", items[3].saveOptional(registryAccess))
                return true
            }

            override fun appendComponent(
                builder: HologramWidgetBuilder<BlockHologramContext>,
                displayType: DisplayType
            ) {
                val context = builder.context
                val remember = context.getRememberData()
                val cookingProgress by remember.server(
                    0,
                    intArrayOf(0, 0, 0, 0)
                ) { tag -> tag.getIntArray("cooking_progress") }
                val cookingTime by remember.server(
                    1,
                    intArrayOf(0, 0, 0, 0)
                ) { tag -> tag.getIntArray("cooking_time") }
                val item0 by remember.serverItemStack(2, "item0")
                val item1 by remember.serverItemStack(3, "item1")
                val item2 by remember.serverItemStack(4, "item2")
                val item3 by remember.serverItemStack(5, "item3")

                if (!item0.isEmpty) {
                    builder.single {
                        itemStack(item0)
                        text("progress:${cookingProgress[0]}/time:${cookingTime[0]}")
                    }
                }
                if (!item1.isEmpty) {
                    builder.single {
                        itemStack(item1)
                        text("progress:${cookingProgress[1]}/time:${cookingTime[1]}")
                    }
                }
                if (!item2.isEmpty) {
                    builder.single {
                        itemStack(item2)
                        text("progress:${cookingProgress[2]}/time:${cookingTime[2]}")
                    }
                }
                if (!item3.isEmpty) {
                    builder.single {
                        itemStack(item3)
                        text("progress:${cookingProgress[3]}/time:${cookingTime[3]}")
                    }
                }
            }

            override fun targetClass(): Class<*> = CampfireBlock::class.java

            override fun location(): ResourceLocation = HologramPanel.rl("camp_fire")
        })
        register.registerBlockComponent(object : ServerDataProvider<BlockHologramContext> {
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
                    builder.single {
                        itemStack(songItem).setScale(0.75)
                        component(song.description().copy())
                    }
                    builder.single {
                        text("$songStarted/${song.lengthInTicks()} Ticks").setScale(1.5)
                    }
                }
            }

            override fun targetClass(): Class<*> = JukeboxBlock::class.java

            override fun location(): ResourceLocation = HologramPanel.rl("juke_box")
        })

        register.registerBlockComponent(object : ServerDataProvider<BlockHologramContext> {
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
                    builder.single {
                        text("Bee Count:$beeCount")
                    }
                    repeat(beeCount) { index ->
                        val tick = tag.getInt("bee_${index}_ticks_in_hive")
                        val byteBuffer = Unpooled.wrappedBuffer(tag.getByteArray("bee_$index"))
                        val data = BeehiveBlockEntity.Occupant.STREAM_CODEC.decode(byteBuffer)
                        builder.group("Bee ${index + 1}") {
                            builder.single { text("inHive:$tick/${data.minTicksInHive()}") }
                            @Suppress("DEPRECATION") val pos = data.entityData().unsafe.get("flower_pos")
                            if (pos != null && pos.type == IntArrayTag.TYPE) {
                                val tag = (pos as IntArrayTag)
                                builder.single { text("flower pos:${tag[0]} ${tag[1]} ${tag[2]}") }
                            }
                        }
                    }
                }
            }

            override fun targetClass(): Class<*> = BeehiveBlock::class.java

            override fun location(): ResourceLocation = HologramPanel.rl("bee_hive")
        })

        register.registerEntityComponent(object : ServerDataProvider<EntityHologramContext> {
            override fun appendComponent(
                builder: HologramWidgetBuilder<EntityHologramContext>,
                displayType: DisplayType
            ) {
                val context = builder.context
                val entity = context.getEntity<LivingEntity>() ?: return
                val remember = context.getRememberData()
                val currentHealth by remember.server(0, -1.0f) { tag -> tag.getFloat("current_health") }
                builder.single {
                    heart()
                    text("health:${currentHealth}/${entity.maxHealth}")
                }
            }

            override fun targetClass(): Class<*> = LivingEntity::class.java

            override fun location(): ResourceLocation = HologramPanel.rl("health_display")

            override fun appendServerData(
                additionData: CompoundTag,
                targetData: CompoundTag,
                context: EntityHologramContext
            ): Boolean {
                val entity = context.getEntity<LivingEntity>() ?: return true
                targetData.putFloat("current_health", entity.health)
                return true
            }

        })

        register.registerEntityComponent(object : ServerDataProvider<EntityHologramContext> {
            override fun appendComponent(
                builder: HologramWidgetBuilder<EntityHologramContext>,
                displayType: DisplayType
            ) {
                val context = builder.context
                val remember = context.getRememberData()

                val lifeSpan by remember.server(0, 0) { tag -> tag.getInt("life_span") }
                val age by remember.server(1, -1) { tag -> tag.getInt("age") }

                if (age != -1) {
                    builder.single { text("remain:${lifeSpan - age} Ticks") }
                }
            }

            override fun targetClass(): Class<*> = ItemEntity::class.java

            override fun location(): ResourceLocation = HologramPanel.rl("item_entity")

            override fun appendServerData(
                additionData: CompoundTag,
                targetData: CompoundTag,
                context: EntityHologramContext
            ): Boolean {
                val itemEntity = context.getEntity<ItemEntity>() ?: return true
                targetData.putInt("life_span", itemEntity.lifespan)
                targetData.putInt("age", itemEntity.age)
                return true
            }

        })
    }

    override fun registerClient(register: HologramClientRegistration) {
    }

    companion object {
        object DefaultBlockDescriptionProvider : ComponentProvider<BlockHologramContext> {
            override fun appendComponent(
                builder: HologramWidgetBuilder<BlockHologramContext>,
                displayType: DisplayType
            ) {
                val context = builder.context
                builder.single {
                    item(context.getBlockState().block.asItem()).setScale(0.75)
                    component(context.getBlockState().block.name).setScale(1.5)
                }
            }

            override fun targetClass(): Class<*> = Block::class.java

            override fun location(): ResourceLocation = HologramPanel.rl("default_block_description_provider")
        }

        object DefaultEntityDescriptionProvider : ComponentProvider<EntityHologramContext> {
            override fun appendComponent(
                builder: HologramWidgetBuilder<EntityHologramContext>,
                displayType: DisplayType
            ) {
                val context = builder.context
                builder.single {
                    entity(context.getEntity())
                    component(context.getEntity().name)
                }
            }

            override fun targetClass(): Class<*> = Block::class.java

            override fun location(): ResourceLocation = HologramPanel.rl("default_block_description_provider")
        }
    }
}