package io.github.kawaiicakes.clothing.client.model;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import org.jetbrains.annotations.ApiStatus;

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
public abstract class ClothingModel {
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

    // TODO: more mesh methods with default implementations? javadoc?
    /**
     * Implementing classes use this method to get the bulk of model information to create the most basic
     * <code>LayerDefinition</code>; the one for the player/HumanoidModel. [Tweaks to the mesh are made in
     * the base implementation of {@link #generateLayerDefinition(EntityType)}, but you'll need to override
     * it if you wish to tweak models further for different entities.] (this part up for rectification)
     * @return the {@link MeshDefinition} used for baking layers in the default {@link HumanoidModel}.
     */
    public abstract MeshDefinition baseMesh();

    /**
     * Method used to get the specific {@link HumanoidModel} instance associated with an entity type that is used for
     * rendering in {@link io.github.kawaiicakes.clothing.client.HumanoidClothingLayer}. You can see what models are
     * used for entities in the constructors of their respective renderers; look for the calls to <code>#addLayer</code>.
     * @see net.minecraft.client.renderer.entity.ZombieVillagerRenderer
     * @param entityType the {@link EntityType} for render
     * @return an instance of <code>U</code>.
     * @param <T> the specific entity of the <code>entityType</code>
     * @param <U> instance of {@link HumanoidModel} used for rendering of the
     * {@link net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer}.
     */
    public <T extends LivingEntity, U extends HumanoidModel<T>> U getModelForEntityType(EntityType<T> entityType) {
        if (!getEntityTypes().contains(entityType)) throw new IllegalArgumentException("Invalid entity!");
        // TODO
        ModelPart modelPart = this.bakedModels.getOrDefault(
                EntityType.getKey(entityType).toString(), new ModelPart(ImmutableList.of(), new HashMap<>())
        );

        //noinspection unchecked
        return (U) new HumanoidModel<>(modelPart);
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

        MeshDefinition meshDefinition;

        // TODO
        meshDefinition = this.baseMesh();

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
    }

    /**
     * Mainly intended for internal use. This is a good override target for anyone using mixins to add support for
     * more kinds of entities. Be sure to reflect these changes in the other necessary places!
     * @see ClassesThatDefineWhatEntitiesSupportClothing
     * @see #getModelForEntityType(EntityType)
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
