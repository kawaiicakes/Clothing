package io.github.kawaiicakes.clothing.client.model;

import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.client.ClothingModelRepository;
import io.github.kawaiicakes.clothing.client.HumanoidClothingLayer;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.ZombieVillagerRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
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
import java.util.*;

/**
 * Permits abstraction of more tedious entity model registration stuff. This class can be viewed as the data for an
 * entity model itself, and the changes necessary to that data for it to work with different subclasses of
 * {@link HumanoidModel}. There are several entity types that you have to keep track of when
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
     * {@link LayerDefinition}; the one for the base {@link HumanoidModel}.
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
     * This overload of {@link #getModelForEntityType(String)} takes an {@link EntityType}, allowing use of generics
     * and therefore a more "direct" type on return.
     * @see ZombieVillagerRenderer
     * @param entityType the {@link EntityType} for render
     * @return an instance of <code>HumanoidModel</code>.
     * @param <T> the specific entity of the <code>entityType</code>
     */
    @Nullable
    public <T extends LivingEntity> HumanoidModel<T> getModelForEntityType(EntityType<T> entityType) {
        String entityTypeKey = EntityType.getKey(entityType).toString();
        try {
            //noinspection unchecked
            return (HumanoidModel<T>) this.getModelForEntityType(entityTypeKey);
        } catch (ClassCastException e) {
            LOGGER.error("Uh oh, stinky!", e);
            throw e;
        }
    }

    /**
     * Method used to get the specific {@link HumanoidModel} instance associated with an entity type that is used for
     * rendering in {@link HumanoidClothingLayer}. You can see what models are
     * used for entities in the constructors of their respective renderers; look for the calls to <code>#addLayer</code>.
     * <br><br>
     * It is suggested that you cache the returned model somewhere. I would have cached the models in this class as a
     * field, but I do fear that this may cause weird rendering issues.
     * @see ZombieVillagerRenderer
     * @param entityType the {@link EntityType}'s key as a String for render
     * @return an instance of <code>HumanoidModel</code>.
     */
    @Nullable
    public HumanoidModel<? extends LivingEntity> getModelForEntityType(String entityType) {
        if (Arrays.stream(getEntityTypes()).noneMatch((e) -> e.equals(entityType)))
            throw new IllegalArgumentException("Invalid entity!");

        ModelPart modelPart = this.bakedModels.getOrDefault(
                entityType, null
        );
        if (modelPart == null) throw new IllegalArgumentException("This entity type does not have a model!");

        Constructor<?> objConstructor = this.getModelConstructorForEntityType(entityType);

        HumanoidModel<? extends LivingEntity> toReturn = null;
        try {
            assert objConstructor != null;
            Class<?> constructorClazz = objConstructor.getDeclaringClass();
            if (!(HumanoidModel.class.isAssignableFrom(constructorClazz))) throw new ClassCastException();
            //noinspection unchecked
            Constructor<? extends HumanoidModel<? extends LivingEntity>> modelConstructor
                    = (Constructor<? extends HumanoidModel<? extends LivingEntity>>) objConstructor;
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

    /**
     * Used to dynamically obtain the constructor for the {@link HumanoidModel} implementation used for the passed
     * {@link EntityType}'s {@link HumanoidArmorLayer}. Alternative to
     * manually defining these values, and allows overriding by those using this API. Implementations should ensure
     * that the returned constructor will work with {@link #getModelForEntityType(EntityType)}. Override it if
     * necessary.
     * <br><br>
     * Implementations should be aware that a check for entity type validity would be redundant here since this is a
     * job better suited to {@link #getModelForEntityType(EntityType)}.
     * @see #getEntityTypes()
     * @param entityType the {@link EntityType} key's <code>String</code> for which this model will be rendered.
     * @return the <code>Constructor</code> who takes a {@link ModelPart} as an argument.
     *          If no such constructor exists, override {@link #getModelForEntityType(EntityType)}
     */
    @Nullable
    public Constructor<?> getModelConstructorForEntityType(
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
     * Creates a {@link LayerDefinition} from a {@link MeshDefinition} that is
     * tweaked according to the passed <code>entityType</code>. This is to allow for slight differences between models
     * depending on what kind of entity is wearing it.
     * @param entityType the <code>String</code> of the entity's type key. See {@link #getEntityTypes()} for valid
     *                   types.
     * @return the {@link LayerDefinition} that will be registered.
     * @throws IllegalArgumentException if the passed entity type is not supported for clothing rendering.
     */
    public LayerDefinition generateLayerDefinition(String entityType) {
        if (Arrays.stream(getEntityTypes()).noneMatch((e) -> e.equals(entityType)))
            throw new IllegalArgumentException("Invalid entity!");

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

        switch (entityType) {
            case "minecraft:armor_stand" -> this.armorStandMeshTransformation(meshDefinition);
            case "minecraft:drowned" -> this.drownedMeshTransformation(meshDefinition);
            case "minecraft:giant" -> this.giantMeshTransformation(meshDefinition);
            case "minecraft:husk", "minecraft:zombie" -> this.zombieMeshTransformation(meshDefinition);
            case "minecraft:piglin", "minecraft:piglin_brute", "minecraft:zombified_piglin"
                    -> this.piglinMeshTransformation(meshDefinition);
            case "minecraft:skeleton", "minecraft:stray", "minecraft:wither_skeleton"
                    -> this.skeletonMeshTransformation(meshDefinition);
            case "minecraft:zombie_villager" -> this.zombieVillagerMeshTransformation(meshDefinition);
        }

        return LayerDefinition.create(meshDefinition, this.textureWidth, this.textureHeight);
    }

    /**
     * Creates a {@link ModelLayerLocation} that represents the layer for the passed entity. This exists to allow for
     * reference of a <code>ModelLayerLocation</code> without the need to cache it.
     * @param entityType the {@link EntityType} key as a <code>String</code> of the entity that owns this layer.
     *                   See {@link #getEntityTypes()} for valid types.
     * @return the {@link ModelLayerLocation} for the layer of the passed <code>entityType</code>.
     * @throws IllegalArgumentException if the passed entity type is not supported for clothing rendering.
     */
    public ModelLayerLocation generateModelLayerLocation(String entityType) {
        if (Arrays.stream(getEntityTypes()).noneMatch((e) -> e.equals(entityType)))
            throw new IllegalArgumentException("Invalid entity!");
        return new ModelLayerLocation(new ResourceLocation(entityType), this.modelId.toString());
    }

    /**
     * Used when the model has its generated {@link LayerDefinition}s registered in
     * {@link ClothingModelRepository}.
     * @param event the {@link EntityRenderersEvent.RegisterLayerDefinitions} event,
     *              when layer definitions are registered. Mind blown.
     */
    @ApiStatus.Internal
    public void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (String entityType : getEntityTypes()) {
            event.registerLayerDefinition(
                    this.generateModelLayerLocation(entityType),
                    () -> this.generateLayerDefinition(entityType)
            );
        }
    }

    /**
     * Bakes all the {@link ModelPart}s associated with this model.
     * @param event the {@link EntityRenderersEvent.AddLayers} event when layers are
     *              added to entity renderers. Baking takes place during this time.
     */
    @ApiStatus.Internal
    public void bakeParts(EntityRenderersEvent.AddLayers event) {
        EntityModelSet modelSet = event.getEntityModels();

        for (String entityType : getEntityTypes()) {
            this.bakedModels.put(
                    entityType,
                    modelSet.bakeLayer(this.generateModelLayerLocation(entityType))
            );
        }
    }

    /**
     * Mainly intended for internal use. This is a good override target for anyone using mixins to add support for
     * more kinds of entities. Be sure to reflect these changes in the other necessary places!
     * @see #getModelConstructorForEntityType(String)
     * @see #generateLayerDefinition(String)
     * @see MeshTransformer
     * @return Returns a <code>Set</code> of all {@link EntityType}s that this model is intended to render on.
     */
    @ApiStatus.Internal
    public static String[] getEntityTypes() {
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
