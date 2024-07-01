package io.github.kawaiicakes.clothing.client.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
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

    public static final CubeDeformation BASE = new CubeDeformation(0.25F);
    public static final CubeDeformation OVER = new CubeDeformation(1.25F);

    public static ModelLayerLocation register(String entity, String layerName) {
        return new ModelLayerLocation(new ResourceLocation(entity), "clothing_" + layerName);
    }

    public static MeshDefinition createGenericMesh(CubeDeformation pCubeDeformation, float pYOffset) {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();

        partDefinition.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(
                                -4.0F, -8.0F, -4.0F,
                                8.0F, 8.0F, 8.0F,
                                pCubeDeformation,
                                0.25F, 0.25F
                        ),
                PartPose.offset(0.0F, 0.0F + pYOffset, 0.0F)
        );

        partDefinition.addOrReplaceChild(
                "hat",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(
                                -4.0F, -8.0F, -4.0F,
                                8.0F, 8.0F, 8.0F,
                                pCubeDeformation.extend(0.5F),
                                0.25F, 0.25F
                        ),
                PartPose.offset(0.0F, 0.0F + pYOffset, 0.0F));

        partDefinition.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(16, 16)
                        .addBox(
                                -4.0F, 0.0F, -2.0F,
                                8.0F, 12.0F, 4.0F,
                                pCubeDeformation,
                                0.25F, 0.25F
                        ),
                PartPose.offset(0.0F, 0.0F + pYOffset, 0.0F)
        );

        partDefinition.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create()
                        .texOffs(40, 16)
                        .addBox(
                                -3.0F, -2.0F, -2.0F,
                                4.0F, 12.0F, 4.0F,
                                pCubeDeformation,
                                0.25F, 0.25F
                        ),
                PartPose.offset(-5.0F, 2.0F + pYOffset, 0.0F)
        );

        partDefinition.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create()
                        .texOffs(40, 16)
                        .mirror()
                        .addBox(
                                -1.0F, -2.0F, -2.0F,
                                4.0F, 12.0F, 4.0F,
                                pCubeDeformation,
                                0.25F, 0.25F
                        ),
                PartPose.offset(5.0F, 2.0F + pYOffset, 0.0F)
        );

        partDefinition.addOrReplaceChild(
                "right_leg",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(
                                -2.0F, 0.0F, -2.0F,
                                4.0F, 12.0F, 4.0F,
                                pCubeDeformation,
                                0.25F, 0.25F
                        ),
                PartPose.offset(-1.9F, 12.0F + pYOffset, 0.0F)
        );

        partDefinition.addOrReplaceChild(
                "left_leg",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .mirror()
                        .addBox(
                                -2.0F, 0.0F, -2.0F,
                                4.0F, 12.0F, 4.0F,
                                pCubeDeformation,
                                0.25F, 0.25F
                        ),
                PartPose.offset(1.9F, 12.0F + pYOffset, 0.0F)
        );

        return meshDefinition;
    }
}