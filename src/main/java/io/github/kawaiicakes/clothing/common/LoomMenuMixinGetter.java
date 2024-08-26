package io.github.kawaiicakes.clothing.common;

import io.github.kawaiicakes.clothing.common.resources.OverlayDefinitionLoader;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface LoomMenuMixinGetter {
    default List<OverlayDefinitionLoader.OverlayDefinition> getClothing$selectableOverlays() {
        return List.of();
    }

    default List<OverlayDefinitionLoader.OverlayDefinition> getClothing$selectableOverlays(
            ItemStack clothing, ItemStack spool, ItemStack pattern
    ) {
        return List.of();
    }

    default int getClothing$stratumOrdinal() {
        return -1;
    }

    default void setClothing$stratumOrdinal(int ordinal) {}
}
