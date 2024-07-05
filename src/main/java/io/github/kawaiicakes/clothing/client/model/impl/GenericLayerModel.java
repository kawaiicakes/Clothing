package io.github.kawaiicakes.clothing.client.model.impl;

import io.github.kawaiicakes.clothing.client.model.ClothingModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDefinition;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;

public class GenericLayerModel extends ClothingModel {
    public static final CubeDeformation BASE = new CubeDeformation(0.3F);
    public static final CubeDeformation OVER = new CubeDeformation(1.125F);

    protected final CubeDeformation cubeDeformation;

    protected GenericLayerModel(String path, CubeDeformation cubeDeformation) {
        super(new ResourceLocation(MOD_ID, path), 512, 256);
        this.cubeDeformation = cubeDeformation;
    }

    @Override
    public @NotNull MeshDefinition baseMesh() {
        return HumanoidModel.createMesh(this.cubeDeformation, 0.0F);
    }

    public static GenericLayerModel baseModel() {
        return new GenericLayerModel("base", BASE);
    }

    public static GenericLayerModel overModel() {
        return new GenericLayerModel("over", OVER) {
            @Override
            public void drownedMeshTransformation(MeshDefinition parentMesh) {
                PartDefinition parentPart = parentMesh.getRoot();
                // TODO: public API for changing existing mesh properties.
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
                                cubeDefinition.grow.extend(0.25F),
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
                                cubeDefinition.grow.extend(0.02F),
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
        };
    }
}
