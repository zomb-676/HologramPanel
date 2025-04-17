package com.github.zomb_676.hologrampanel.mixin;

import com.github.zomb_676.hologrampanel.api.HologramHolder;
import com.github.zomb_676.hologrampanel.widget.HologramWidget;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nullable;

/**
 * implementation {@link HologramHolder} for {@link Entity}
 */
@Mixin(Entity.class)
public class EntityMixin implements HologramHolder {
    @Unique
    @Nullable
    HologramWidget hologramPanel$widget = null;

    @Override
    @Unique
    public @Nullable HologramWidget setWidget(@Nullable HologramWidget widget) {
        var old = this.hologramPanel$widget;
        this.hologramPanel$widget = widget;
        return old;
    }

    @Override
    @Unique
    public @Nullable HologramWidget getWidget() {
        return this.hologramPanel$widget;
    }
}
