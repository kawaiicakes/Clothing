package io.github.kawaiicakes.clothing.common;

import io.github.kawaiicakes.clothing.common.resources.OverlayDefinitionLoader;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface LoomMenuOverlayGetter {
    default List<OverlayDefinitionLoader.OverlayDefinition> getClothing$selectableOverlays() {
        return List.of();
    }

    default List<OverlayDefinitionLoader.OverlayDefinition> getClothing$selectableOverlays(ItemStack stack) {
        return List.of();
    }
}
