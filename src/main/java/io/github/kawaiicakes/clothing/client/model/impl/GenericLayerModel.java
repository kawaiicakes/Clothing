package io.github.kawaiicakes.clothing.client.model.impl;

import io.github.kawaiicakes.clothing.client.model.ClothingModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;

public class GenericLayerModel extends ClothingModel {
    public static final CubeDeformation BASE = new CubeDeformation(0.30F);
    public static final CubeDeformation INNER = new CubeDeformation(0.31F);
    public static final CubeDeformation OUTER = new CubeDeformation(0.32F);
    public static final CubeDeformation OVER = new CubeDeformation(0.33F);

    protected final CubeDeformation cubeDeformation;

    protected GenericLayerModel(String path, CubeDeformation cubeDeformation) {
        super(new ResourceLocation(MOD_ID, path), 512, 256);
        this.cubeDeformation = cubeDeformation;
    }

    @Override
    public @NotNull MeshDefinition baseMesh() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();

        partDefinition.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(
                                -4.0F, -8.0F, -4.0F,
                                8.0F, 8.0F, 8.0F,
                                this.cubeDeformation,
                                0.125F, 0.125F
                        ),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        partDefinition.addOrReplaceChild(
                "hat",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(
                                -4.0F, -8.0F, -4.0F,
                                8.0F, 8.0F, 8.0F,
                                this.cubeDeformation.extend(0.5F),
                                0.125F, 0.125F
                        ),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        partDefinition.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(16, 16)
                        .addBox(
                                -4.0F, 0.0F, -2.0F,
                                8.0F, 12.0F, 4.0F,
                                this.cubeDeformation,
                                0.125F, 0.125F
                        ),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        partDefinition.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create()
                        .texOffs(40, 16)
                        .addBox(
                                -3.0F, -2.0F, -2.0F,
                                4.0F, 12.0F, 4.0F,
                                this.cubeDeformation,
                                0.125F, 0.125F
                        ),
                PartPose.offset(-5.0F, 2.0F, 0.0F)
        );

        partDefinition.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create()
                        .texOffs(40, 16)
                        .mirror()
                        .addBox(
                                -1.0F, -2.0F, -2.0F,
                                4.0F, 12.0F, 4.0F,
                                this.cubeDeformation,
                                0.125F, 0.125F
                        ),
                PartPose.offset(5.0F, 2.0F, 0.0F)
        );

        partDefinition.addOrReplaceChild(
                "right_leg",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(
                                -2.0F, 0.0F, -2.0F,
                                4.0F, 12.0F, 4.0F,
                                this.cubeDeformation,
                                0.125F, 0.125F
                        ),
                PartPose.offset(-1.9F, 12.0F, 0.0F)
        );

        partDefinition.addOrReplaceChild(
                "left_leg",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .mirror()
                        .addBox(
                                -2.0F, 0.0F, -2.0F,
                                4.0F, 12.0F, 4.0F,
                                this.cubeDeformation,
                                0.125F, 0.125F
                        ),
                PartPose.offset(1.9F, 12.0F, 0.0F)
        );

        return meshDefinition;
    }

    protected float drownedNonLegsInflation() {
        return 0.25F;
    }

    protected float piglinNonLegsInflation() {
        return 0.02F;
    }

    @Override
    public void drownedMeshTransformation(MeshDefinition parentMesh) {
        PartDefinition parentPart = parentMesh.getRoot();
        // TODO: public API for changing existing mesh properties. for that matter, override methods in
        // other classes (TBD) and declare them as public
        for (Map.Entry<String, PartDefinition> entry : parentPart.children.entrySet()) {
            PartDefinition originalPart = entry.getValue();

            List<CubeDefinition> newPartCubes = new ArrayList<>();
            for (CubeDefinition cubeDefinition : originalPart.cubes) {
                CubeDefinition newCube = new CubeDefinition(
                        cubeDefinition.comment,
                        cubeDefinition.texCoord.u(),
                        cubeDefinition.texCoord.v(),
                        cubeDefinition.origin.x(),
                        cubeDefinition.origin.y(),
                        cubeDefinition.origin.z(),
                        cubeDefinition.dimensions.x(),
                        cubeDefinition.dimensions.y(),
                        cubeDefinition.dimensions.z(),
                        cubeDefinition.grow.extend(drownedNonLegsInflation()),
                        cubeDefinition.mirror,
                        cubeDefinition.texScale.u(),
                        cubeDefinition.texScale.v()
                );
                newPartCubes.add(newCube);
            }
            PartDefinition newPart = new PartDefinition(
                    newPartCubes,
                    originalPart.partPose
            );
            parentPart.children.put(entry.getKey(), newPart);
        }
    }

    @Override
    public void piglinMeshTransformation(MeshDefinition parentMesh) {
        PartDefinition parentPart = parentMesh.getRoot();
        for (Map.Entry<String, PartDefinition> entry : parentPart.children.entrySet()) {
            PartDefinition originalPart = entry.getValue();

            List<CubeDefinition> newPartCubes = new ArrayList<>();
            for (CubeDefinition cubeDefinition : originalPart.cubes) {
                CubeDefinition newCube = new CubeDefinition(
                        cubeDefinition.comment,
                        cubeDefinition.texCoord.u(),
                        cubeDefinition.texCoord.v(),
                        cubeDefinition.origin.x(),
                        cubeDefinition.origin.y(),
                        cubeDefinition.origin.z(),
                        cubeDefinition.dimensions.x(),
                        cubeDefinition.dimensions.y(),
                        cubeDefinition.dimensions.z(),
                        cubeDefinition.grow.extend(piglinNonLegsInflation()),
                        cubeDefinition.mirror,
                        cubeDefinition.texScale.u(),
                        cubeDefinition.texScale.v()
                );
                newPartCubes.add(newCube);
            }
            PartDefinition newPart = new PartDefinition(
                    newPartCubes,
                    originalPart.partPose
            );
            parentPart.children.put(entry.getKey(), newPart);
        }
    }

    /**
     * @return the {@link GenericLayerModel} for the legs
     * @see io.github.kawaiicakes.clothing.client.HumanoidClothingLayer#getArmorModel(EquipmentSlot)
     */
    public static GenericLayerModel baseModel() {
        return new GenericLayerModel("generic_base", BASE) {
            @Override
            protected float drownedNonLegsInflation() {
                return 0.0F;
            }

            @Override
            protected float piglinNonLegsInflation() {
                return 0.0F;
            }
        };
    }

    /**
     * @return the {@link GenericLayerModel} for the feet
     * @see io.github.kawaiicakes.clothing.client.HumanoidClothingLayer#getArmorModel(EquipmentSlot)
     */
    public static GenericLayerModel innerModel() {
        return new GenericLayerModel("generic_inner", INNER);
    }

    /**
     * @return the {@link GenericLayerModel} for the chest
     * @see io.github.kawaiicakes.clothing.client.HumanoidClothingLayer#getArmorModel(EquipmentSlot)
     */
    public static GenericLayerModel outerModel() {
        return new GenericLayerModel("generic_outer", OUTER);
    }

    /**
     * @return the {@link GenericLayerModel} for the head
     * @see io.github.kawaiicakes.clothing.client.HumanoidClothingLayer#getArmorModel(EquipmentSlot)
     */
    public static GenericLayerModel overModel() {
        return new GenericLayerModel("generic_over", OVER);
    }
}
