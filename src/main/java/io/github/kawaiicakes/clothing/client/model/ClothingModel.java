package io.github.kawaiicakes.clothing.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashSet;
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

    // TODO: javadocs
    public ClothingModel(ResourceLocation modelId, int textureWidth, int textureHeight) {
        this.modelId = modelId;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    // TODO: Not sure if I'm gonna keep this class abstract. I'll probably come up with some default impl for this
    public abstract <T extends LivingEntity> HumanoidModel<T> getModelForEntityType(EntityType<T> entityType);

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
        // TODO: actual MeshDefinitions
        return LayerDefinition.create(new MeshDefinition(), this.textureWidth, this.textureHeight);
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
     * Mainly intended for internal use. This is a good override target for anyone using mixins to add support for
     * more kinds of entities. Be sure to reflect these changes in the other necessary classes!
     * @see ClassesThatDefineWhatEntitiesSupportClothing
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
