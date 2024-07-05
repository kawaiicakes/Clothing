package io.github.kawaiicakes.clothing.client.model;

import com.mojang.logging.LogUtils;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Permits abstraction of more tedious entity model registration stuff. This class can be viewed as the data for an
 * entity model itself, and the changes necessary to that data for it to work with different subclasses of
 * {@link net.minecraft.client.model.HumanoidModel}. There are several entity types that you have to keep track of when
 * making models, so this class should hopefully prove itself convenient.
 */
@OnlyIn(Dist.CLIENT)
public abstract class ClothingModel implements MeshTransformer {
    protected static final Logger LOGGER = LogUtils.getLogger();

    public final ResourceLocation modelId;
    protected int textureWidth;
    protected int textureHeight;
    protected final Map<String, ModelPart> bakedModels = new HashMap<>();

    /**
     * @param modelId the {@link ResourceLocation} used for internal identification of this model. It's recommended you
     *                choose something concise, descriptive, and unique.
     * @param textureWidth an <code>int</code> representing the width in pixels of the intended textures for use
     * @param textureHeight an <code>int</code> representing the height in pixels of the intended textures for use
     */
    public ClothingModel(ResourceLocation modelId, int textureWidth, int textureHeight) {
        this.modelId = modelId;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    /**
     * Implementing classes use this method to get the bulk of model information to create the most basic
     * {@link net.minecraft.client.model.geom.builders.LayerDefinition}; the one for the base {@link HumanoidModel}.
     * For most circumstances, you will probably not need to transform this mesh on a per-entity basis, but if you are
     * finding that cosmetics don't fit properly, override the appropriate mesh transformation method.
     * <br><br>
     * Mesh standards can be found on the wiki. In short, models should start by choosing a "body group", and then
     * creating children on that body group. If the body group is specific to a certain entity type, then override the
     * respective transformation method and create children on the body group there.
     * @return the {@link MeshDefinition} used for baking layers in the default {@link HumanoidModel}.
     * @see MeshTransformer
     */
    @Override
    public abstract @NotNull MeshDefinition baseMesh();

    /**
     * Method used to get the specific {@link HumanoidModel} instance associated with an entity type that is used for
     * rendering in {@link io.github.kawaiicakes.clothing.client.HumanoidClothingLayer}. You can see what models are
     * used for entities in the constructors of their respective renderers; look for the calls to <code>#addLayer</code>.
     * <br><br>
     * It is suggested that you cache the returned model somewhere. I would have cached the models in this class as a
     * field, but I do fear that this may cause weird rendering issues.
     * @see net.minecraft.client.renderer.entity.ZombieVillagerRenderer
     * @param entityType the {@link EntityType} for render
     * @return an instance of <code>U</code>.
     * @param <T> the specific entity of the <code>entityType</code>
     */
    @Nullable
    public <T extends LivingEntity> HumanoidModel<T> getModelForEntityType(EntityType<T> entityType) {
        if (!getEntityTypes().contains(entityType)) throw new IllegalArgumentException("Invalid entity!");

        ModelPart modelPart = this.bakedModels.getOrDefault(
                EntityType.getKey(entityType).toString(), null
        );
        if (modelPart == null) throw new IllegalArgumentException("This entity type does not have a model!");

        Constructor<?> objConstructor = getModelConstructorForEntityType(entityType);

        HumanoidModel<T> toReturn = null;
        try {
            assert objConstructor != null;
            Class<?> constructorClazz = objConstructor.getDeclaringClass();
            if (!(HumanoidModel.class.isAssignableFrom(constructorClazz))) throw new ClassCastException();
            //noinspection unchecked
            Constructor<? extends HumanoidModel<T>> modelConstructor = (Constructor<? extends HumanoidModel<T>>) objConstructor;
            toReturn = modelConstructor.newInstance(modelPart);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            LOGGER.error("Unable to instantiate model for render on entity type {}!", entityType, e);
        } catch (NullPointerException e) {
            LOGGER.error(
                    "Model constructor with ModelPart parameter does not exist for entity type {}!", entityType, e
            );
        } catch (ClassCastException e) {
            LOGGER.error(
                    "Unable to cast constructor for model for entity type {}!", entityType, e
            );
        }
        return toReturn;
    }

    // TODO: non-ad hoc implementation of slim player models
    // I think I should change a lot of these methods to take Strings, and have an overload that takes EntityTypes
    public ModelPart getModelPart(String entity) {
        return this.bakedModels.get(entity);
    }

    /**
     * Used to dynamically obtain the constructor for the {@link HumanoidModel} implementation used for the passed
     * {@link EntityType}'s {@link net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer}. Alternative to
     * manually defining these values, and allows overriding by those using this API. Implementations should ensure
     * that the returned constructor will work with {@link #getModelForEntityType(EntityType)}. Override it if
     * necessary.
     * <br><br>
     * Implementations should be aware that a check for entity type validity would be redundant here since this is a
     * job better suited to {@link #getModelForEntityType(EntityType)}.
     * @see #getEntityTypes()
     * @param entityType the {@link EntityType} for which this model will be rendered.
     * @return the <code>Constructor</code> of type <code>U</code> who takes a {@link ModelPart} as an argument.
     *          If no such constructor exists, override {@link #getModelForEntityType(EntityType)}
     */
    @Nullable
    public Constructor<?> getModelConstructorForEntityType(
            EntityType<? extends LivingEntity> entityType) {
        try {
            if (EntityType.ARMOR_STAND.equals(entityType)) {
                return ArmorStandArmorModel.class.getConstructor(ModelPart.class);
            } else if (EntityType.DROWNED.equals(entityType)) {
                return DrownedModel.class.getConstructor(ModelPart.class);
            } else if (EntityType.GIANT.equals(entityType)) {
                return GiantZombieModel.class.getConstructor(ModelPart.class);
            } else if (EntityType.HUSK.equals(entityType) || EntityType.ZOMBIE.equals(entityType)) {
                return ZombieModel.class.getConstructor(ModelPart.class);
            } else if (EntityType.SKELETON.equals(entityType)
                    || EntityType.STRAY.equals(entityType) || EntityType.WITHER_SKELETON.equals(entityType)) {
                return SkeletonModel.class.getConstructor(ModelPart.class);
            } else if (EntityType.ZOMBIE_VILLAGER.equals(entityType)) {
                return ZombieVillagerModel.class.getConstructor(ModelPart.class);
            } else {
                return HumanoidModel.class.getConstructor(ModelPart.class);
            }
        } catch (NoSuchMethodException e) {
            LOGGER.error("No such constructor for model of entity type {}!", entityType, e);
            return null;
        } catch (ClassCastException e) {
            LOGGER.error("Unable to cast constructor to appropriate type for entity type {}!", entityType, e);
            return null;
        }
    }

    /**
     * Creates a {@link LayerDefinition} from a {@link net.minecraft.client.model.geom.builders.MeshDefinition} that is
     * tweaked according to the passed <code>entityType</code>. This is to allow for slight differences between models
     * depending on what kind of entity is wearing it.
     * @param entityType the {@link EntityType} of an entity wearing the model. See {@link #getEntityTypes()} for valid
     *                   types.
     * @return the {@link LayerDefinition} that will be registered.
     * @param <T> an instance of {@link LivingEntity}
     * @throws IllegalArgumentException if the passed entity type is not supported for clothing rendering.
     */
    public <T extends LivingEntity> LayerDefinition generateLayerDefinition(EntityType<T> entityType) {
        if (!getEntityTypes().contains(entityType)) throw new IllegalArgumentException("Invalid entity!");

        final MeshDefinition meshDefinition = MeshTransformer.emptyHumanoidMesh();
        PartDefinition definitionPart = meshDefinition.getRoot();

        MeshDefinition baseMesh = this.baseMesh();
        PartDefinition basePart = baseMesh.getRoot();

        for (Map.Entry<String, PartDefinition> child : basePart.children.entrySet()) {
            String childName = child.getKey();
            if (!definitionPart.children.containsKey(childName)) {
                LOGGER.error("Unknown body group \"{}\" while trying to generate LayerDefinition!", childName);
                continue;
            }
            PartDefinition basePartChild = child.getValue();
            definitionPart.children.put(childName, basePartChild);
        }

        if (EntityType.ARMOR_STAND.equals(entityType)) {
            this.armorStandMeshTransformation(meshDefinition);
        } else if (EntityType.DROWNED.equals(entityType)) {
            this.drownedMeshTransformation(meshDefinition);
        } else if (EntityType.GIANT.equals(entityType)) {
            this.giantMeshTransformation(meshDefinition);
        } else if (EntityType.HUSK.equals(entityType) || EntityType.ZOMBIE.equals(entityType)) {
            this.zombieMeshTransformation(meshDefinition);
        } else if (EntityType.PIGLIN.equals(entityType)
                || EntityType.PIGLIN_BRUTE.equals(entityType) || EntityType.ZOMBIFIED_PIGLIN.equals(entityType)) {
            this.piglinMeshTransformation(meshDefinition);
        } else if (EntityType.SKELETON.equals(entityType)
                || EntityType.STRAY.equals(entityType) || EntityType.WITHER_SKELETON.equals(entityType)) {
            this.skeletonMeshTransformation(meshDefinition);
        } else if (EntityType.ZOMBIE_VILLAGER.equals(entityType)) {
            this.zombieVillagerMeshTransformation(meshDefinition);
        }

        return LayerDefinition.create(meshDefinition, this.textureWidth, this.textureHeight);
    }

    /**
     * Creates a {@link ModelLayerLocation} that represents the layer for the passed entity. This exists to allow for
     * reference of a <code>ModelLayerLocation</code> without the need to cache it.
     * @param entityType the {@link EntityType} of the entity that owns this layer. See {@link #getEntityTypes()} for
     *                   valid types.
     * @return the {@link ModelLayerLocation} for the layer of the passed <code>entityType</code>.
     * @param <T> an instance of {@link LivingEntity}
     * @throws IllegalArgumentException if the passed entity type is not supported for clothing rendering.
     */
    public <T extends LivingEntity> ModelLayerLocation generateModelLayerLocation(EntityType<T> entityType) {
        if (!getEntityTypes().contains(entityType)) throw new IllegalArgumentException("Invalid entity!");
        return new ModelLayerLocation(EntityType.getKey(entityType), this.modelId.toString());
    }

    public ModelLayerLocation generateSlimModelLayerLocation() {
        return new ModelLayerLocation(new ResourceLocation("player_slim"), this.modelId.toString());
    }

    /**
     * Used when the model has its generated {@link LayerDefinition}s registered in
     * {@link io.github.kawaiicakes.clothing.client.ClothingModelRepository}.
     * @param event the {@link net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions} event,
     *              when layer definitions are registered. Mind blown.
     */
    @ApiStatus.Internal
    public void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (EntityType<? extends LivingEntity> entityType : getEntityTypes()) {
            event.registerLayerDefinition(
                    generateModelLayerLocation(entityType),
                    () -> generateLayerDefinition(entityType)
            );

            if (!EntityType.PLAYER.equals(entityType)) continue;

            event.registerLayerDefinition(
                    new ModelLayerLocation(
                            new ResourceLocation("player_slim"),
                            this.modelId.toString()
                    ),
                    () -> generateLayerDefinition(entityType)
            );
        }
    }

    /**
     * Bakes all the {@link ModelPart}s associated with this model.
     * @param event the {@link net.minecraftforge.client.event.EntityRenderersEvent.AddLayers} event when layers are
     *              added to entity renderers. Baking takes place during this time.
     */
    @ApiStatus.Internal
    public void bakeParts(EntityRenderersEvent.AddLayers event) {
        EntityModelSet modelSet = event.getEntityModels();

        for (EntityType<? extends LivingEntity> entityType : getEntityTypes()) {
            this.bakedModels.put(
                    EntityType.getKey(entityType).toString(),
                    modelSet.bakeLayer(this.generateModelLayerLocation(entityType))
            );
        }

        this.bakedModels.put(
                String.valueOf(new ResourceLocation("player_slim")),
                modelSet.bakeLayer(this.generateSlimModelLayerLocation())
        );
    }

    /**
     * Mainly intended for internal use. This is a good override target for anyone using mixins to add support for
     * more kinds of entities. Be sure to reflect these changes in the other necessary places!
     * @see #getModelConstructorForEntityType(EntityType)
     * @see #generateLayerDefinition(EntityType)
     * @see MeshTransformer
     * @return Returns a <code>Set</code> of all {@link EntityType}s that this model is intended to render on.
     */
    @ApiStatus.Internal
    public static Set<EntityType<? extends LivingEntity>> getEntityTypes() {
        Set<EntityType<? extends LivingEntity>> toReturn = new HashSet<>();

        toReturn.add(EntityType.ARMOR_STAND);
        toReturn.add(EntityType.DROWNED);
        toReturn.add(EntityType.GIANT);
        toReturn.add(EntityType.HUSK);
        toReturn.add(EntityType.PLAYER);
        toReturn.add(EntityType.PIGLIN);
        toReturn.add(EntityType.PIGLIN_BRUTE);
        toReturn.add(EntityType.SKELETON);
        toReturn.add(EntityType.STRAY);
        toReturn.add(EntityType.WITHER_SKELETON);
        toReturn.add(EntityType.ZOMBIE);
        toReturn.add(EntityType.ZOMBIFIED_PIGLIN);
        toReturn.add(EntityType.ZOMBIE_VILLAGER);

        return toReturn;
    }
}
