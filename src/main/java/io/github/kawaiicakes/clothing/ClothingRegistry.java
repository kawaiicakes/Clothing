package io.github.kawaiicakes.clothing;

import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.common.fluid.BleachFluid;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import io.github.kawaiicakes.clothing.common.item.ClothingTabs;
import io.github.kawaiicakes.clothing.common.item.SpoolItem;
import io.github.kawaiicakes.clothing.common.item.impl.BakedModelClothingItem;
import io.github.kawaiicakes.clothing.common.item.impl.GenericClothingItem;
import io.github.kawaiicakes.clothing.common.resources.recipe.ClothingRecipe;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;
import static io.github.kawaiicakes.clothing.common.item.ClothingItem.NEW_DYED_ITEM;
import static net.minecraft.core.cauldron.CauldronInteraction.*;
import static net.minecraft.world.item.Items.BUCKET;
import static net.minecraft.world.level.block.Blocks.CAULDRON;

public class ClothingRegistry {
    protected static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZER_REGISTRY
            = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MOD_ID);

    public static final DeferredRegister<Item> CLOTHING_REGISTRY
            = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final DeferredRegister<Item> ITEM_REGISTRY
            = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final DeferredRegister<Block> BLOCK_REGISTRY
            = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);

    public static final DeferredRegister<FluidType> FLUID_TYPE_REGISTRY
            = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, MOD_ID);

    public static final DeferredRegister<Fluid> FLUID_REGISTRY
            = DeferredRegister.create(ForgeRegistries.FLUIDS, MOD_ID);

    public static final DeferredRegister<ParticleType<?>> PARTICLE_REGISTRY
            = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MOD_ID);

    public static final RegistryObject<SimpleParticleType> DRIPPING_BLEACH
            = PARTICLE_REGISTRY.register("dripping_bleach", () -> new SimpleParticleType(false));

    // TODO: bleach crafting
    public static final Map<Item, CauldronInteraction> BLEACH = CauldronInteraction.newInteractionMap();

    public static final RegistryObject<FluidType> BLEACH_FLUID_TYPE = FLUID_TYPE_REGISTRY.register(
            "bleach",
            () -> BleachFluid.TYPE
    );

    public static final RegistryObject<FlowingFluid> BLEACH_FLUID = FLUID_REGISTRY.register(
            "bleach",
            BleachFluid.Source::new
    );

    public static final RegistryObject<FlowingFluid> BLEACH_FLUID_FLOWING = FLUID_REGISTRY.register(
            "flowing_bleach",
            BleachFluid.Flowing::new
    );

    public static final Material BLEACH_MATERIAL
            = (new Material.Builder(MaterialColor.NONE)).noCollider().nonSolid().replaceable().liquid().build();

    public static final RegistryObject<LiquidBlock> LEGACY_BLEACH_BLOCK = BLOCK_REGISTRY.register(
            "bleach",
            () -> new LiquidBlock(
                    BLEACH_FLUID,
                    BlockBehaviour.Properties.of(BLEACH_MATERIAL).noCollission().strength(100.0F).noLootTable()
            )
    );

    public static final RegistryObject<ClothingRecipe.Serializer> CLOTHING_SERIALIZER
            = SERIALIZER_REGISTRY.register("clothing_recipe", ClothingRecipe.Serializer::new);

    public static final RegistryObject<Item> BLEACH_BUCKET = ITEM_REGISTRY.register(
            "bleach_bucket",
            () -> new BucketItem(
                    BLEACH_FLUID,
                    (new Item.Properties())
                            .craftRemainder(BUCKET)
                            .stacksTo(1)
                            .tab(ClothingTabs.CLOTHING_TAB_MISC)
            )
    );

    public static final RegistryObject<LayeredCauldronBlock> BLEACH_CAULDRON = BLOCK_REGISTRY.register(
            "bleach_cauldron",
            () -> new LayeredCauldronBlock(
                    BlockBehaviour.Properties.copy(CAULDRON), precipitation -> false, BLEACH
            )
    );

    public static final RegistryObject<SpoolItem> SPOOL = ITEM_REGISTRY.register(
            "spool", () -> new SpoolItem(new Item.Properties().tab(ClothingTabs.CLOTHING_TAB_MISC))
    );

    public static final RegistryObject<GenericClothingItem> GENERIC_HAT
            = CLOTHING_REGISTRY.register("generic_hat", () -> new GenericClothingItem(EquipmentSlot.HEAD));
    public static final RegistryObject<GenericClothingItem> GENERIC_SHIRT
            = CLOTHING_REGISTRY.register("generic_shirt", () -> new GenericClothingItem(EquipmentSlot.CHEST));
    public static final RegistryObject<GenericClothingItem> GENERIC_PANTS
            = CLOTHING_REGISTRY.register("generic_pants", () -> new GenericClothingItem(EquipmentSlot.LEGS));
    public static final RegistryObject<GenericClothingItem> GENERIC_SHOES
            = CLOTHING_REGISTRY.register("generic_shoes", () -> new GenericClothingItem(EquipmentSlot.FEET));
    public static final RegistryObject<BakedModelClothingItem> BAKED_HAT
            = CLOTHING_REGISTRY.register("baked_hat", () -> new BakedModelClothingItem(EquipmentSlot.HEAD));
    public static final RegistryObject<BakedModelClothingItem> BAKED_SHIRT
            = CLOTHING_REGISTRY.register("baked_shirt", () -> new BakedModelClothingItem(EquipmentSlot.CHEST));
    public static final RegistryObject<BakedModelClothingItem> BAKED_PANTS
            = CLOTHING_REGISTRY.register("baked_pants", () -> new BakedModelClothingItem(EquipmentSlot.LEGS));
    public static final RegistryObject<BakedModelClothingItem> BAKED_SHOES
            = CLOTHING_REGISTRY.register("baked_shoes", () -> new BakedModelClothingItem(EquipmentSlot.FEET));
    public static final CauldronInteraction FILL_BLEACH = (pBlockState, pLevel, pBlockPos, pPlayer, pHand, pStack) ->
            CauldronInteraction.emptyBucket(
                    pLevel,
                    pBlockPos,
                    pPlayer,
                    pHand,
                    pStack,
                    BLEACH_CAULDRON.get().defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3),
                    SoundEvents.BUCKET_EMPTY
            );

    @Nullable
    public static List<RegistryObject<Item>> getGeneric() {
        try {
            List<RegistryObject<Item>> genericClothingItems = new ArrayList<>();

            for (RegistryObject<Item> registryObject : CLOTHING_REGISTRY.getEntries()) {
                if (!registryObject.isPresent() || !(registryObject.get() instanceof GenericClothingItem))
                    continue;

                genericClothingItems.add(registryObject);
            }

            if (genericClothingItems.isEmpty()) throw new RuntimeException("No generic clothing items exist!");

            return genericClothingItems;
        } catch (RuntimeException e) {
            LOGGER.error("Error returning Clothing from registry!", e);
            return null;
        }
    }

    @Nullable
    public static ClothingItem<?>[] getAll() {
        try {
            ClothingItem<?>[] toReturn = new ClothingItem<?>[CLOTHING_REGISTRY.getEntries().size()];

            int arbitraryIndexNumber = 0;
            for (RegistryObject<Item> registryObject : CLOTHING_REGISTRY.getEntries()) {
                toReturn[arbitraryIndexNumber++] = (ClothingItem<?>) registryObject.get();
            }

            return toReturn;
        } catch (RuntimeException e) {
            LOGGER.error("Error returning Clothing from registry!", e);
            return null;
        }
    }

    @Nullable
    public static Item get(String itemName) {
        try {
            RegistryObject<Item> itemRegistryObject = CLOTHING_REGISTRY.getEntries().stream().filter(
                    (i) -> i.getId().getPath().equals(itemName)
            ).findFirst().orElseThrow();

            return itemRegistryObject.get();
        } catch (NoSuchElementException | NullPointerException e) {
            LOGGER.error("No such Clothing item as {}!", itemName, e);
            return null;
        }
    }

    @ApiStatus.Internal
    public static void register(IEventBus bus) {
        CLOTHING_REGISTRY.register(bus);
        SERIALIZER_REGISTRY.register(bus);
        ITEM_REGISTRY.register(bus);
        BLOCK_REGISTRY.register(bus);
        FLUID_TYPE_REGISTRY.register(bus);
        FLUID_REGISTRY.register(bus);
        PARTICLE_REGISTRY.register(bus);
    }

    @ApiStatus.Internal
    public static void registerCauldronInteractions() {
        List<Map<Item, CauldronInteraction>> vanillaPlus = new ArrayList<>();
        vanillaPlus.add(EMPTY);
        vanillaPlus.add(WATER);
        vanillaPlus.add(LAVA);
        vanillaPlus.add(POWDER_SNOW);
        vanillaPlus.add(BLEACH);

        for (Map<Item, CauldronInteraction> pInteractionsMap : vanillaPlus) {
            pInteractionsMap.put(BLEACH_BUCKET.get(), FILL_BLEACH);

            if (!pInteractionsMap.equals(BLEACH)) continue;

            pInteractionsMap.put(Items.LAVA_BUCKET, FILL_LAVA);
            pInteractionsMap.put(Items.WATER_BUCKET, FILL_WATER);
            pInteractionsMap.put(Items.POWDER_SNOW_BUCKET, FILL_POWDER_SNOW);
        }

        for (Item item : getAll()) {
            WATER.put(item, NEW_DYED_ITEM);

            if (!(item instanceof GenericClothingItem)) continue;

            ClothingRegistry.BLEACH.put(item, GenericClothingItem.GENERIC_CLOTHING);
        }
    }
}
