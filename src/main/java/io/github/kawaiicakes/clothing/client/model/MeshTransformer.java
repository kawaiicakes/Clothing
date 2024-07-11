package io.github.kawaiicakes.clothing.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDefinition;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The <code>MeshTransformer</code> is an interface which provides methods that transform model data (as returned by
 * {@link #baseMesh()}) based on what entities are likely to have misaligned models. This information was
 * inferred by examining which {@link net.minecraft.client.model.geom.LayerDefinitions} for <code>INNER_ARMOR</code>
 * and <code>OUTER_ARMOR</code> layers for entities use a non-generic
 * {@link net.minecraft.client.model.geom.builders.LayerDefinition}.
 * <br><br>
 * Furthermore, this interface exists as a sensible entrypoint for anyone trying to add third-party support for a
 * custom entity type (via mixins). With respect to that task, you must also pay attention to
 * {@link ClothingModel#getEntityTypeKey()} and anything laid out in its Javadoc.
 */
@OnlyIn(Dist.CLIENT)
public interface MeshTransformer {
    /**
     * Implementing classes use this method to get the bulk of model information to create the most basic
     * {@link net.minecraft.client.model.geom.builders.LayerDefinition}; the one for the base {@link HumanoidModel}.
     * For most circumstances, you will probably not need to transform this mesh on a per-entity basis, but if you are
     * finding that cosmetics don't fit properly, override the appropriate mesh transformation method.
     * <br><br>
     * Mesh standards can be found on the wiki. In short, models should start by choosing a "body group", and then
     * creating children on that body group. If the body group is specific to a certain entity type, then override the
     * respective transformation method and create children on the body group there.
     * @return the {@link MeshDefinition} representing the most general data for a {@link ClothingModel}.
     */
    @NotNull
    MeshDefinition baseMesh();

    /**
     * Returns a mesh whose children are all empty.
     * @return a {@link MeshDefinition} as described.
     */
    static MeshDefinition emptyHumanoidMesh() {
        MeshDefinition toReturn = new MeshDefinition();
        PartDefinition partDefinition = toReturn.getRoot();

        partDefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        partDefinition.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        partDefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        partDefinition.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        partDefinition.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        partDefinition.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        partDefinition.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);

        return toReturn;
    }

    /**
     * Armor Stands "hat" and "head" groups have a {@link PartPose} y-offset of 1.0F, compared to the 0.0F of the
     * base humanoid mesh. In addition, "right_leg" and "left_leg" have a y-offset of 11.0F compared to 12.0F.
     * @param parentMesh the {@link MeshDefinition} to edit.
     */
    default void armorStandMeshTransformation(MeshDefinition parentMesh) {
        PartDefinition parentPart = parentMesh.getRoot();

        PartDefinition hatPart = parentPart.getChild("hat");
        PartDefinition headPart = parentPart.getChild("head");
        PartDefinition rightLegPart = parentPart.getChild("right_leg");
        PartDefinition leftLegPart = parentPart.getChild("left_leg");

        PartDefinition newHat = new PartDefinition(
                hatPart.cubes,
                PartPose.offsetAndRotation(
                        hatPart.partPose.x,
                    hatPart.partPose.y + 1.0F,
                        hatPart.partPose.z,
                        hatPart.partPose.xRot,
                        hatPart.partPose.yRot,
                        hatPart.partPose.zRot
                )
        );
        newHat.children.putAll(hatPart.children);
        parentPart.children.put("hat", newHat);

        PartDefinition newHead = new PartDefinition(
                headPart.cubes,
                PartPose.offsetAndRotation(
                        headPart.partPose.x,
                        headPart.partPose.y + 1.0F,
                        headPart.partPose.z,
                        headPart.partPose.xRot,
                        headPart.partPose.yRot,
                        headPart.partPose.zRot
                )
        );
        newHead.children.putAll(rightLegPart.children);
        parentPart.children.put("head", newHead);

        PartDefinition newRightLeg = new PartDefinition(
                rightLegPart.cubes,
                PartPose.offsetAndRotation(
                        rightLegPart.partPose.x,
                        rightLegPart.partPose.y - 1.0F,
                        rightLegPart.partPose.z,
                        rightLegPart.partPose.xRot,
                        rightLegPart.partPose.yRot,
                        rightLegPart.partPose.zRot
                )
        );
        newRightLeg.children.putAll(rightLegPart.children);
        parentPart.children.put("right_leg", newRightLeg);

        PartDefinition newLeftLeg = new PartDefinition(
                leftLegPart.cubes,
                PartPose.offsetAndRotation(
                        leftLegPart.partPose.x,
                        leftLegPart.partPose.y - 1.0F,
                        leftLegPart.partPose.z,
                        leftLegPart.partPose.xRot,
                        leftLegPart.partPose.yRot,
                        leftLegPart.partPose.zRot
                )
        );
        newLeftLeg.children.putAll(leftLegPart.children);
        parentPart.children.put("left_leg", newLeftLeg);
    }

    /**
     * The Drowned's <code>OUTER_ARMOR</code> layer has an inflated
     * {@link net.minecraft.client.model.geom.builders.CubeDeformation} by 0.25F.
     * @param parentMesh the {@link MeshDefinition} to edit.
     */
    default void drownedMeshTransformation(MeshDefinition parentMesh) {
    }

    /**
     * Appears to have an identical (but scaled) mesh to the HumanoidModel.
     * @param parentMesh the {@link MeshDefinition} to edit.
     */
    default void giantMeshTransformation(MeshDefinition parentMesh) {
    }

    /**
     * The Piglin <code>OUTER_ARMOR</code> layer has a {@link net.minecraft.client.model.geom.builders.CubeDeformation}
     * inflated by 0.02F.
     * @param parentMesh the {@link MeshDefinition} to edit.
     */
    default void piglinMeshTransformation(MeshDefinition parentMesh) {
    }

    /**
     * Appears to have an identical mesh to the HumanoidModel.
     * @param parentMesh the {@link MeshDefinition} to edit.
     */
    default void skeletonMeshTransformation(MeshDefinition parentMesh) {
    }

    /**
     * Appears to have an identical mesh to the HumanoidModel.
     * @param parentMesh the {@link MeshDefinition} to edit.
     */
    default void zombieMeshTransformation(MeshDefinition parentMesh) {
    }

    /**
     * "head" is translated an extra -2.0F units on the y. "body", "right_leg" and "left_leg" have extended the default
     * {@link net.minecraft.client.model.geom.builders.CubeDeformation} by 0.1F. "hat.hat_rim", unique to the
     * Zombie Villager model, has been made empty. Additionally, "right_leg" and "left_leg" have a pose
     * 0.1F units greater on the x-axis in their respective signs.
     * @param parentMesh the {@link MeshDefinition} to edit.
     */
    default void zombieVillagerMeshTransformation(MeshDefinition parentMesh) {
        PartDefinition parentPart = parentMesh.getRoot();

        PartDefinition headPart = parentPart.getChild("head");
        PartDefinition bodyPart = parentPart.getChild("body");
        PartDefinition rightLegPart = parentPart.getChild("right_leg");
        PartDefinition leftLegPart = parentPart.getChild("left_leg");
        PartDefinition hatPart = parentPart.getChild("hat");

        List<CubeDefinition> headCubes = new ArrayList<>();
        for (CubeDefinition cubeDefinition : headPart.cubes) {
            CubeDefinition newCube = new CubeDefinition(
                    cubeDefinition.comment,
                    cubeDefinition.texCoord.u(),
                    cubeDefinition.texCoord.v(),
                    cubeDefinition.origin.x(),
                    cubeDefinition.origin.y() - 2.0F,
                    cubeDefinition.origin.z(),
                    cubeDefinition.dimensions.x(),
                    cubeDefinition.dimensions.y(),
                    cubeDefinition.dimensions.z(),
                    cubeDefinition.grow,
                    cubeDefinition.mirror,
                    cubeDefinition.texScale.u(),
                    cubeDefinition.texScale.v()
            );

            headCubes.add(newCube);
        }
        PartDefinition newHead = new PartDefinition(
                headCubes,
                headPart.partPose
        );
        newHead.children.putAll(headPart.children);
        parentPart.children.put("head", newHead);

        List<CubeDefinition> bodyCubes = new ArrayList<>();
        for (CubeDefinition cubeDefinition : bodyPart.cubes) {
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
                    cubeDefinition.grow.extend(0.1F),
                    cubeDefinition.mirror,
                    cubeDefinition.texScale.u(),
                    cubeDefinition.texScale.v()
            );

            bodyCubes.add(newCube);
        }
        PartDefinition newBody = new PartDefinition(
                bodyCubes,
                bodyPart.partPose
        );
        newBody.children.putAll(bodyPart.children);
        parentPart.children.put("body", newBody);

        List<CubeDefinition> rightLegCubes = new ArrayList<>();
        for (CubeDefinition cubeDefinition : rightLegPart.cubes) {
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
                    cubeDefinition.grow.extend(0.1F),
                    cubeDefinition.mirror,
                    cubeDefinition.texScale.u(),
                    cubeDefinition.texScale.v()
            );

            rightLegCubes.add(newCube);
        }
        PartDefinition newRightLeg = new PartDefinition(
                rightLegCubes,
                PartPose.offsetAndRotation(
                        rightLegPart.partPose.x - 0.1F,
                        rightLegPart.partPose.y,
                        rightLegPart.partPose.z,
                        rightLegPart.partPose.xRot,
                        rightLegPart.partPose.yRot,
                        rightLegPart.partPose.zRot
                )
        );
        newRightLeg.children.putAll(bodyPart.children);
        parentPart.children.put("right_leg", newRightLeg);

        List<CubeDefinition> leftLegCubes = new ArrayList<>();
        for (CubeDefinition cubeDefinition : leftLegPart.cubes) {
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
                    cubeDefinition.grow.extend(0.1F),
                    cubeDefinition.mirror,
                    cubeDefinition.texScale.u(),
                    cubeDefinition.texScale.v()
            );

            leftLegCubes.add(newCube);
        }
        PartDefinition newLeftLeg = new PartDefinition(
                leftLegCubes,
                PartPose.offsetAndRotation(
                        leftLegPart.partPose.x + 0.1F,
                        leftLegPart.partPose.y,
                        leftLegPart.partPose.z,
                        leftLegPart.partPose.xRot,
                        leftLegPart.partPose.yRot,
                        leftLegPart.partPose.zRot
                )
        );
        newLeftLeg.children.putAll(bodyPart.children);
        parentPart.children.put("left_leg", newLeftLeg);

        hatPart.addOrReplaceChild("hat_rim", CubeListBuilder.create(), PartPose.ZERO);
    }

    /**
     * This turned out to be unnecessary as a default implementation, but it might come in handy later.
     * @param parentMesh the {@link MeshDefinition} to edit.
     */
    static void skeletonBodyModelTransformation(MeshDefinition parentMesh) {
        PartDefinition parentPart = parentMesh.getRoot();

        PartDefinition rightArmPart = parentPart.getChild("right_arm");
        PartDefinition leftArmPart = parentPart.getChild("left_arm");
        PartDefinition rightLegPart = parentPart.getChild("right_leg");
        PartDefinition leftLegPart = parentPart.getChild("left_leg");

        List<CubeDefinition> rightArmCubes = new ArrayList<>();
        for (CubeDefinition cubeDefinition : rightArmPart.cubes) {
            CubeDefinition newCube = new CubeDefinition(
                    cubeDefinition.comment,
                    cubeDefinition.texCoord.u(),
                    cubeDefinition.texCoord.v(),
                    cubeDefinition.origin.x() + 2.0F,
                    cubeDefinition.origin.y(),
                    cubeDefinition.origin.z() + 1.0F,
                    cubeDefinition.dimensions.x() - 2.0F,
                    cubeDefinition.dimensions.y(),
                    cubeDefinition.dimensions.z() - 2.0F,
                    cubeDefinition.grow,
                    cubeDefinition.mirror,
                    cubeDefinition.texScale.u(),
                    cubeDefinition.texScale.v()
            );

            rightArmCubes.add(newCube);
        }
        PartDefinition newRightArm = new PartDefinition(
                rightArmCubes,
                rightArmPart.partPose
        );
        newRightArm.children.putAll(rightArmPart.children);
        parentPart.children.put("right_arm", newRightArm);

        List<CubeDefinition> leftArmCubes = new ArrayList<>();
        for (CubeDefinition cubeDefinition : leftArmPart.cubes) {
            CubeDefinition newCube = new CubeDefinition(
                    cubeDefinition.comment,
                    cubeDefinition.texCoord.u(),
                    cubeDefinition.texCoord.v(),
                    cubeDefinition.origin.x(),
                    cubeDefinition.origin.y(),
                    cubeDefinition.origin.z() + 1.0F,
                    cubeDefinition.dimensions.x() - 2.0F,
                    cubeDefinition.dimensions.y(),
                    cubeDefinition.dimensions.z() - 2.0F,
                    cubeDefinition.grow,
                    cubeDefinition.mirror,
                    cubeDefinition.texScale.u(),
                    cubeDefinition.texScale.v()
            );

            leftArmCubes.add(newCube);
        }
        PartDefinition newLeftArm = new PartDefinition(
                leftArmCubes,
                leftArmPart.partPose
        );
        newLeftArm.children.putAll(leftArmPart.children);
        parentPart.children.put("left_arm", newLeftArm);

        List<CubeDefinition> rightLegCubes = new ArrayList<>();
        for (CubeDefinition cubeDefinition : rightLegPart.cubes) {
            CubeDefinition newCube = new CubeDefinition(
                    cubeDefinition.comment,
                    cubeDefinition.texCoord.u(),
                    cubeDefinition.texCoord.v(),
                    cubeDefinition.origin.x() + 1.0F,
                    cubeDefinition.origin.y(),
                    cubeDefinition.origin.z() + 1.0F,
                    cubeDefinition.dimensions.x() - 2.0F,
                    cubeDefinition.dimensions.y(),
                    cubeDefinition.dimensions.z() - 2.0F,
                    cubeDefinition.grow,
                    cubeDefinition.mirror,
                    cubeDefinition.texScale.u(),
                    cubeDefinition.texScale.v()
            );

            rightLegCubes.add(newCube);
        }
        PartDefinition newRightLeg = new PartDefinition(
                rightLegCubes,
                PartPose.offsetAndRotation(
                        rightLegPart.partPose.x - 0.1F,
                        rightLegPart.partPose.y,
                        rightLegPart.partPose.z,
                        rightLegPart.partPose.xRot,
                        rightLegPart.partPose.yRot,
                        rightLegPart.partPose.zRot
                )
        );
        newRightLeg.children.putAll(rightLegPart.children);
        parentPart.children.put("right_leg", newRightLeg);

        List<CubeDefinition> leftLegCubes = new ArrayList<>();
        for (CubeDefinition cubeDefinition : leftLegPart.cubes) {
            CubeDefinition newCube = new CubeDefinition(
                    cubeDefinition.comment,
                    cubeDefinition.texCoord.u(),
                    cubeDefinition.texCoord.v(),
                    cubeDefinition.origin.x() + 1.0F,
                    cubeDefinition.origin.y(),
                    cubeDefinition.origin.z() + 1.0F,
                    cubeDefinition.dimensions.x() - 2.0F,
                    cubeDefinition.dimensions.y(),
                    cubeDefinition.dimensions.z() - 2.0F,
                    cubeDefinition.grow,
                    cubeDefinition.mirror,
                    cubeDefinition.texScale.u(),
                    cubeDefinition.texScale.v()
            );

            leftLegCubes.add(newCube);
        }
        PartDefinition newLeftLeg = new PartDefinition(
                leftLegCubes,
                PartPose.offsetAndRotation(
                        leftLegPart.partPose.x - 0.1F,
                        leftLegPart.partPose.y,
                        leftLegPart.partPose.z,
                        leftLegPart.partPose.xRot,
                        leftLegPart.partPose.yRot,
                        leftLegPart.partPose.zRot
                )
        );
        newLeftLeg.children.putAll(leftLegPart.children);
        parentPart.children.put("left_leg", newLeftLeg);
    }
}
