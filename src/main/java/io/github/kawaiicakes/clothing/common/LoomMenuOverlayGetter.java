package io.github.kawaiicakes.clothing.common;

import io.github.kawaiicakes.clothing.common.resources.OverlayDefinitionLoader;

import java.util.List;

public interface LoomMenuOverlayGetter {
    default List<OverlayDefinitionLoader.OverlayDefinition> getClothing$selectableOverlays() {
        return List.of();
    }
}
