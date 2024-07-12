package io.github.kawaiicakes.clothing.client.model;

import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.client.HumanoidClothingLayer;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This class takes care of registering {@link LayerDefinition}s, similarly to
 * {@link net.minecraft.client.model.geom.LayerDefinitions} but tries not to cache anything and does a lot of
 * the "heavy lifting" so that {@link io.github.kawaiicakes.clothing.ClothingMod} doesn't need 10,000,000,000 imports.
 * I doubt a modder would need to access the {@link ModelLayerLocation}s of the generic layers anyway, but their names
 * are generated in {@link #registerLayers(EntityRenderersEvent.RegisterLayerDefinitions)} and
 * {@link #generateModelLayerLocation(String, float)} if you are curious.
 */
@OnlyIn(Dist.CLIENT)
public class GenericDefinitions {
    protected static Logger LOGGER = LogUtils.getLogger();

    public static <T extends LivingEntity, M extends HumanoidModel<T>> void addLayerHelper(
            String entityTypeKey,
            EntityRenderersEvent.AddLayers event
    ) {
        try {
            EntityType<?> type = EntityType.byString(entityTypeKey).orElseThrow();
            @SuppressWarnings("unchecked")
            EntityType<T> entityType = (EntityType<T>) type;

            LivingEntityRenderer<T, M> renderer = event.getRenderer(entityType);
            if (renderer == null) {
                throw new IllegalArgumentException("Unable to obtain renderer for " + entityTypeKey + "!");
            }
            renderer.addLayer(
                    new HumanoidClothingLayer<>(
                            renderer,
                            getModelForEntityType(entityTypeKey, 0.30F, event),
                            getModelForEntityType(entityTypeKey, 0.31F, event),
                            getModelForEntityType(entityTypeKey, 0.32F, event),
                            getModelForEntityType(entityTypeKey, 0.33F, event),
                            getModelForEntityType(entityTypeKey, 0.80F, event),
                            getModelForEntityType(entityTypeKey, 1.30F, event)
                    )
            );
        } catch (RuntimeException e) {
            LOGGER.error("Exception while adding layer for {}!", entityTypeKey, e);
        }
    }

    @Nullable
    public static <T extends LivingEntity, M extends HumanoidModel<T>> M getModelForEntityType(
            String entityTypeKey, float layerDeformation, EntityRenderersEvent.AddLayers event
    ) {
        if (Arrays.stream(getEntityTypeKey()).noneMatch((e) -> e.equals(entityTypeKey)))
            throw new IllegalArgumentException("Invalid entity!");

        final EntityModelSet modelSet = event.getEntityModels();

        ModelPart modelPart = modelSet.bakeLayer(generateModelLayerLocation(entityTypeKey, layerDeformation));

        Constructor<?> objConstructor = getModelConstructorForEntityType(entityTypeKey);

        M toReturn = null;
        try {
            assert objConstructor != null;
            Class<?> constructorClazz = objConstructor.getDeclaringClass();
            if (!(HumanoidModel.class.isAssignableFrom(constructorClazz))) throw new ClassCastException();
            //noinspection unchecked
            Constructor<M> modelConstructor
                    = (Constructor<M>) objConstructor;
            toReturn = modelConstructor.newInstance(modelPart);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            LOGGER.error("Unable to instantiate model for render on entity type {}!", entityTypeKey, e);
        } catch (NullPointerException e) {
            LOGGER.error(
                    "Model constructor with ModelPart parameter does not exist for entity type {}!", entityTypeKey, e
            );
        } catch (ClassCastException e) {
            LOGGER.error(
                    "Unable to cast constructor for model for entity type {}!", entityTypeKey, e
            );
        }
        return toReturn;
    }

    public static ModelLayerLocation generateModelLayerLocation(String entityTypeKey, float layerDeformation) {
        int layerInt = (int) ((layerDeformation - 0.30F) * 100);
        return new ModelLayerLocation(new ResourceLocation(entityTypeKey), "generic_" + layerInt);
    }

    public static LayerDefinition generateLayerDefinition(String entityTypeKey, CubeDeformation cubeDeformation) {
        if (Arrays.stream(getEntityTypeKey()).noneMatch((e) -> e.equals(entityTypeKey)))
            throw new IllegalArgumentException("Invalid entity!");

        MeshDefinition meshDefinition = switch (entityTypeKey) {
            case "minecraft:armor_stand" -> armorStandGeneric(cubeDeformation);
            case "minecraft:drowned" -> drownedGeneric(cubeDeformation);
            case "minecraft:piglin", "minecraft:piglin_brute", "minecraft:zombified_piglin"
                    -> piglinGeneric(cubeDeformation);
            case "minecraft:zombie_villager" -> zombieVillagerGeneric(cubeDeformation);
            default -> genericMesh(cubeDeformation);
        };

        return LayerDefinition.create(meshDefinition, 512, 256);
    }

    public static MeshDefinition genericMesh(CubeDeformation cubeDeformation) {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();

        partDefinition.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(
                                -4.0F, -8.0F, -4.0F,
                                8.0F, 8.0F, 8.0F,
                                cubeDeformation,
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
                                cubeDeformation.extend(0.5F),
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
                                cubeDeformation,
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
                                cubeDeformation,
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
                                cubeDeformation,
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
                                cubeDeformation,
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
                                cubeDeformation,
                                0.125F, 0.125F
                        ),
                PartPose.offset(1.9F, 12.0F, 0.0F)
        );

        return meshDefinition;
    }

    public static MeshDefinition armorStandGeneric(CubeDeformation cubeDeformation) {
        MeshDefinition toReturn = genericMesh(cubeDeformation);
        PartDefinition parentPart = toReturn.getRoot();

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

        return toReturn;
    }

    /*
        FIXME: I foresee obvious issues with this mesh and the Piglin mesh due to the extending of the CubeDeformation.
        The adjacent layers are bigger by less than 0.25 or even 0.02, meaning the extended legs will clip over/into
        other layers. I need to come up with a decent fix for this.
     */
    public static MeshDefinition drownedGeneric(CubeDeformation cubeDeformation) {
        MeshDefinition toReturn = genericMesh(cubeDeformation);
        PartDefinition parentPart = toReturn.getRoot();
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
                        cubeDefinition.grow.extend(
                                cubeDeformation.equals(new CubeDeformation(0.30F))
                                        || cubeDeformation.equals(new CubeDeformation(0.80F))
                                        ? 0.0F : 0.25F
                        ),
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
        return toReturn;
    }

    public static MeshDefinition piglinGeneric(CubeDeformation cubeDeformation) {
        MeshDefinition toReturn = genericMesh(cubeDeformation);
        PartDefinition parentPart = toReturn.getRoot();
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
                        cubeDefinition.grow.extend(
                                cubeDeformation.equals(new CubeDeformation(0.30F))
                                        || cubeDeformation.equals(new CubeDeformation(0.80F))
                                        ? 0.0F : 0.02F
                        ),
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
        return toReturn;
    }

    public static MeshDefinition zombieVillagerGeneric(CubeDeformation cubeDeformation) {
        MeshDefinition toReturn = genericMesh(cubeDeformation);
        PartDefinition parentPart = toReturn.getRoot();

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

        return toReturn;
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (String entityType : getEntityTypeKey()) {
            for (float i = 0.30F; i < 0.34F; i += 0.01F) {
                final float finalI = i;

                event.registerLayerDefinition(
                        generateModelLayerLocation(entityType, finalI),
                        () -> generateLayerDefinition(entityType, new CubeDeformation(finalI))
                );
            }
        }
    }

    @Nullable
    public static Constructor<?> getModelConstructorForEntityType(
            String entityType) {
        try {
            return switch (entityType) {
                case "minecraft:armor_stand" -> ArmorStandArmorModel.class.getConstructor(ModelPart.class);
                case "minecraft:drowned" -> DrownedModel.class.getConstructor(ModelPart.class);
                case "minecraft:giant" -> GiantZombieModel.class.getConstructor(ModelPart.class);
                case "minecraft:husk", "minecraft:zombie" -> ZombieModel.class.getConstructor(ModelPart.class);
                case "minecraft:skeleton", "minecraft:stray", "minecraft:wither_skeleton"
                        -> SkeletonModel.class.getConstructor(ModelPart.class);
                case "minecraft:zombie_villager" -> ZombieVillagerModel.class.getConstructor(ModelPart.class);
                default -> HumanoidModel.class.getConstructor(ModelPart.class);
            };
        } catch (NoSuchMethodException e) {
            LOGGER.error("No such constructor for model of entity type {}!", entityType, e);
            return null;
        } catch (ClassCastException e) {
            LOGGER.error("Unable to cast constructor to appropriate type for entity type {}!", entityType, e);
            return null;
        }
    }

    /**
     * Mainly intended for internal use. This is a good override target for anyone using mixins to add support for
     * more kinds of entities. Be sure to reflect these changes in the other necessary places!
     * @see #getModelConstructorForEntityType(String)
     * @see #generateLayerDefinition(String, CubeDeformation)
     * @return Returns a <code>Set</code> of all {@link EntityType}s that this model is intended to render on.
     */
    public static String[] getEntityTypeKey() {
        return new String[] {
                "minecraft:armor_stand",
                "minecraft:drowned",
                "minecraft:giant",
                "minecraft:husk",
                "minecraft:player",
                "minecraft:player_slim",
                "minecraft:piglin",
                "minecraft:piglin_brute",
                "minecraft:skeleton",
                "minecraft:stray",
                "minecraft:wither_skeleton",
                "minecraft:zombie",
                "minecraft:zombified_piglin",
                "minecraft:zombie_villager",
        };
    }
}
