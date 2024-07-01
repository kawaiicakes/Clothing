package io.github.kawaiicakes.clothing.client.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public class GenericClothingLayers {
    public static final ModelLayerLocation ARMOR_STAND_BASE = register("armor_stand", "base");
    public static final ModelLayerLocation ARMOR_STAND_OVER = register("armor_stand", "over");
    public static final ModelLayerLocation DROWNED_BASE = register("drowned", "base");
    public static final ModelLayerLocation DROWNED_OVER = register("drowned", "over");
    public static final ModelLayerLocation GIANT_BASE = register("giant", "base");
    public static final ModelLayerLocation GIANT_OVER = register("giant", "over");
    public static final ModelLayerLocation HUSK_BASE = register("husk", "base");
    public static final ModelLayerLocation HUSK_OVER = register("husk", "over");
    public static final ModelLayerLocation PLAYER_BASE = register("player", "base");
    public static final ModelLayerLocation PLAYER_OVER = register("player", "over");
    public static final ModelLayerLocation PLAYER_SLIM_BASE = register("player_slim", "base");
    public static final ModelLayerLocation PLAYER_SLIM_OVER = register("player_slim", "over");
    public static final ModelLayerLocation PIGLIN_BASE = register("piglin", "base");
    public static final ModelLayerLocation PIGLIN_OVER = register("piglin", "over");
    public static final ModelLayerLocation PIGLIN_BRUTE_BASE = register("piglin_brute", "base");
    public static final ModelLayerLocation PIGLIN_BRUTE_OVER = register("piglin_brute", "over");
    public static final ModelLayerLocation SKELETON_BASE = register("skeleton", "base");
    public static final ModelLayerLocation SKELETON_OVER = register("skeleton", "over");
    public static final ModelLayerLocation STRAY_BASE = register("stray", "base");
    public static final ModelLayerLocation STRAY_OVER = register("stray", "over");
    public static final ModelLayerLocation WITHER_SKELETON_BASE = register("wither_skeleton", "base");
    public static final ModelLayerLocation WITHER_SKELETON_OVER = register("wither_skeleton", "over");
    public static final ModelLayerLocation ZOMBIE_BASE = register("zombie", "base");
    public static final ModelLayerLocation ZOMBIE_OVER = register("zombie", "over");
    public static final ModelLayerLocation ZOMBIFIED_PIGLIN_BASE = register("zombified_piglin", "base");
    public static final ModelLayerLocation ZOMBIFIED_PIGLIN_OVER = register("zombified_piglin", "over");
    public static final ModelLayerLocation ZOMBIE_VILLAGER_BASE = register("zombie_villager", "base");
    public static final ModelLayerLocation ZOMBIE_VILLAGER_OVER = register("zombie_villager", "over");

    public static ModelLayerLocation register(String entity, String layerName) {
        return new ModelLayerLocation(new ResourceLocation(entity), "clothing_" + layerName);
    }
}