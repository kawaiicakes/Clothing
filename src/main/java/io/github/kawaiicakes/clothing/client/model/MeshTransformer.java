package io.github.kawaiicakes.clothing.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * The <code>MeshTransformer</code> is an interface which provides methods that transform model data (as returned by
 * {@link #baseMesh()}) based on what entities are likely to have misaligned models. This information was
 * gathered inferred by examining which {@link HumanoidModel} instances were used in instantiations of
 * {@link net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer}, which is in turn found being passed to
 * {@link net.minecraft.client.renderer.entity.LivingEntityRenderer#addLayer}.
 * <br><br>
 * Furthermore, this interface exists as a sensible entrypoint for anyone trying to add third-party support for a
 * custom entity type (via mixins). With respect to that task, you must also pay attention to
 * {@link ClothingModel#getEntityTypes()} and anything laid out in its Javadoc.
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
    static MeshDefinition nullHumanoidMesh() {
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

    // TODO: make default transformations.
    default MeshDefinition armorStandMeshTransformation() {
        return new MeshDefinition();
    }

    default MeshDefinition drownedMeshTransformation() {
        return new MeshDefinition();
    }

    default MeshDefinition giantMeshTransformation() {
        return new MeshDefinition();
    }

    default MeshDefinition skeletonMeshTransformation() {
        return new MeshDefinition();
    }

    default MeshDefinition zombieMeshTransformation() {
        return new MeshDefinition();
    }

    default MeshDefinition zombieVillagerMeshTransformation() {
        return new MeshDefinition();
    }

    /**
     * Utility method that simply overwrites the children in <code>parent</code> with the
     * children from <code>transformation</code>
     * @param parent {@link MeshDefinition} that will be overwritten
     * @param transformation {@link MeshDefinition} whose children will replace/be added to the <code>parent</code>
     */
    static void transformMesh(MeshDefinition parent, MeshDefinition transformation) {
        final PartDefinition basePart = parent.getRoot();
        final PartDefinition transformationPart = transformation.getRoot();

        for (Map.Entry<String, PartDefinition> child : transformationPart.children.entrySet()) {
            String childName = child.getKey();
            PartDefinition transformationPartChild = child.getValue();
            basePart.children.put(childName, transformationPartChild);
        }
    }
}
