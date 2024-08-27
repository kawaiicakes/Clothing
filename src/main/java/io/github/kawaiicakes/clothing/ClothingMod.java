package io.github.kawaiicakes.clothing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.client.HumanoidClothingLayer;
import io.github.kawaiicakes.clothing.client.model.ClothingItemModel;
import io.github.kawaiicakes.clothing.client.model.ClothingMeshDefinitions;
import io.github.kawaiicakes.clothing.common.data.*;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import io.github.kawaiicakes.clothing.common.network.ClothingPackets;
import io.github.kawaiicakes.clothing.common.resources.ClothingEntryLoader;
import io.github.kawaiicakes.clothing.common.resources.OverlayDefinitionLoader;
import io.github.kawaiicakes.clothing.common.resources.recipe.ClothingIngredient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DripParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotTypePreset;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

import java.util.ArrayList;
import java.util.Collection;

import static io.github.kawaiicakes.clothing.ClothingRegistry.BLEACH_CAULDRON;
import static io.github.kawaiicakes.clothing.ClothingRegistry.DRIPPING_BLEACH;

// TODO: javadoc and misc cleanup
// TODO: add JEI compat for new loom recipes
@Mod(ClothingMod.MOD_ID)
public class ClothingMod
{
    public static final String MOD_ID = "clothing";

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean CURIOS_LOADED = false;

    public ClothingMod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;

        ClothingRegistry.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onInterModEnqueue);
        modEventBus.addListener(this::onGatherData);
        modEventBus.addListener(this::onRegisterEvent);

        forgeEventBus.addListener(this::onAddReloadListener);
        forgeEventBus.addListener(this::onDatapackSync);
    }

    @SubscribeEvent
    public void onRegisterEvent(RegisterEvent event) {
        event.register(
                ForgeRegistries.Keys.RECIPE_SERIALIZERS,
                helper -> CraftingHelper.register(
                        new ResourceLocation(MOD_ID, "clothing"),
                        ClothingIngredient.Serializer.INSTANCE
                )
        );
    }

    @SubscribeEvent
    public void commonSetup(FMLCommonSetupEvent event) {
        /*
            Ensures singleton instance is non-null prior to client connections to/from the server. This was mainly done
            for the client, as upon connecting to a (remote) server for the first time
            ClothingEntryLoader#getLoader(String) would always return null in ClothingPackets.
         */
        OverlayDefinitionLoader.getInstance();
        ClothingEntryLoader.getInstance();

        ClothingPackets.register();

        ClothingRegistry.registerBrewingRecipes();
        ClothingRegistry.registerCauldronInteractions();
    }

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(OverlayDefinitionLoader.getInstance());
        event.addListener(ClothingEntryLoader.getInstance());
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        ClothingPackets.sendToPlayer(
                new ClothingPackets.S2CClothingEntryPacket(
                        ClothingEntryLoader.getInstance()
                ),
                event.getPlayer()
        );

        ClothingPackets.sendToPlayer(
                new ClothingPackets.S2COverlayPacket(
                        OverlayDefinitionLoader.getInstance()
                ),
                event.getPlayer()
        );
    }

    @SubscribeEvent
    public void onInterModEnqueue(InterModEnqueueEvent event) {
        // this check is necessary as I'm unsure if this will cause issues on clients who do not have Curios installed.
        // Namely, the reference(s) to SlotTypePreset
        if (CURIOS_LOADED) {
            boolean messageSent = InterModComms.sendTo(
                    "curios",
                    "register_type",
                    SlotTypePreset.BODY.getMessageBuilder()::build
            );

            String msg = messageSent ? "[Clothing] Successfully registered Curios Clothing slots"
                    : "[Clothing] Curios is present, but Clothing slots were unable to be registered!";

            LOGGER.info(msg);
        }
    }

    @SubscribeEvent
    public void onGatherData(GatherDataEvent event) {
        DataGenerator dataGenerator = event.getGenerator();
        ExistingFileHelper fileHelper = event.getExistingFileHelper();

        ClothingOverlayGenerator clothingOverlayGenerator = new ClothingOverlayGenerator(
                dataGenerator, fileHelper, MOD_ID
        );
        ClothingEntryGenerator clothingEntryGenerator = new ClothingEntryGenerator(dataGenerator, MOD_ID);

        ClothingLangGenerator clothingLangGenerator
                = new ClothingLangGenerator(dataGenerator, MOD_ID, "en_us", clothingEntryGenerator);
        ClothingItemModelGenerator clothingItemModelGenerator
                = new ClothingItemModelGenerator(
                        dataGenerator, MOD_ID, fileHelper, clothingEntryGenerator, clothingOverlayGenerator
        );

        ClothingRecipeGenerator clothingRecipeGenerator
                = new ClothingRecipeGenerator(dataGenerator);

        dataGenerator.addProvider(
                event.includeClient(),
                clothingItemModelGenerator
        );

        dataGenerator.addProvider(
                event.includeClient(),
                clothingLangGenerator
        );

        dataGenerator.addProvider(
                event.includeServer() || event.includeClient(),
                clothingRecipeGenerator
        );

        dataGenerator.addProvider(
                event.includeServer() || event.includeClient(),
                clothingEntryGenerator
        );

        dataGenerator.addProvider(
                event.includeServer() || event.includeClient(),
                clothingOverlayGenerator
        );
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onClientPlayerNetworkEvent(ClientPlayerNetworkEvent event) {
            if (event instanceof ClientPlayerNetworkEvent.Clone) return;

            ClothingItemModel.Baked.flushModelCaches();
            HumanoidClothingLayer.flushModelCaches();
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
            event.register(
                    DRIPPING_BLEACH.get(),
                    (spriteSet) -> new DripParticle.WaterHangProvider(spriteSet) {
                        @Override
                        public Particle createParticle(
                                @NotNull SimpleParticleType pType, @NotNull ClientLevel pLevel,
                                double pX, double pY, double pZ,
                                double pXSpeed, double pYSpeed, double pZSpeed
                        ) {
                            Particle toReturn
                                    = super.createParticle(pType, pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);

                            assert toReturn != null;
                            toReturn.setColor(0.88F, 0.98F, 0.75F);

                            return toReturn;
                        }
                    }
            );
        }

        @SubscribeEvent
        public static void onRegisterItemColorHandlers(RegisterColorHandlersEvent.Item event) {
            ItemColor handler = (pStack, pTintIndex) -> pTintIndex > 0
                    ? ClothingItem.FALLBACK_COLOR
                    : ((DyeableLeatherItem) pStack.getItem()).getColor(pStack);

            event.register(handler, ClothingRegistry.getAllClothing());
            event.register(handler, ClothingRegistry.SPOOL.get());
        }

        @SubscribeEvent
        public static void onRegisterBlockColorHandlers(RegisterColorHandlersEvent.Block event) {
            event.register(
                    (pState, pLevel, pPos, pTintIndex) -> 0xE2FABE,
                    BLEACH_CAULDRON.get()
            );
        }

        @SubscribeEvent
        public static void onModelLoaderRegistration(ModelEvent.RegisterGeometryLoaders event) {
            event.register(ClothingItemModel.Loader.ID, ClothingItemModel.Loader.INSTANCE);
        }

        /**
         * Note: The models here are registered again in the event of a resource pack swap. No extra magic needed here.
         */
        @SubscribeEvent
        public static void onModelRegistration(ModelEvent.RegisterAdditional event) {
            Collection<Collection<ResourceLocation>> lists = new ArrayList<>();
            ResourceManager manager = Minecraft.getInstance().getResourceManager();

            String[] locations = {
                    "models/clothing",
                    "models/item/clothing",
                    "models/item/clothing/overlays"
            };

            for (String location : locations) {
                lists.add(manager.listResources(
                        location, (existing) -> existing.getPath().endsWith(".json")).keySet()
                );
            }

            for (Collection<ResourceLocation> list : lists) {
                for (ResourceLocation modelLocation : list) {
                    String pathWithoutDuplicate = modelLocation.getPath().replace("models/", "");
                    String pathWithoutSuffix = pathWithoutDuplicate.replace(".json", "");
                    ResourceLocation withoutSuffix = new ResourceLocation(
                            modelLocation.getNamespace(), pathWithoutSuffix
                    );
                    event.register(withoutSuffix);
                }
            }
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            CURIOS_LOADED = ModList.get().isLoaded("curios");
            LOGGER.info(
                    CURIOS_LOADED ? "[Clothing] Curios successfully detected during client setup."
                            : "[Clothing] Curios was not detected during client setup."
            );

            if (!CURIOS_LOADED) return;

            ICurioRenderer renderer = new ICurioRenderer() {
                @SuppressWarnings("unchecked")
                @Override
                public <T extends LivingEntity, M extends EntityModel<T>> void render(
                        ItemStack stack,
                        SlotContext slotContext,
                        PoseStack matrixStack,
                        RenderLayerParent<T, M> renderLayerParent,
                        MultiBufferSource renderTypeBuffer,
                        int light,
                        float limbSwing, float limbSwingAmount,
                        float partialTicks, float ageInTicks,
                        float netHeadYaw, float headPitch
                ) {
                    if (!(renderLayerParent instanceof LivingEntityRenderer<T,M> parent)) return;

                    try {
                        HumanoidClothingLayer<T, ?, ?> layer = (HumanoidClothingLayer<T, ?, ?>) parent.layers.stream()
                                .filter(l -> l instanceof HumanoidClothingLayer<?, ?, ?>)
                                .findAny()
                                .orElseThrow();

                        layer.renderClothingFromItemStack(
                                stack,
                                (T) slotContext.entity(),
                                matrixStack, renderTypeBuffer,
                                light,
                                limbSwing, limbSwingAmount,
                                partialTicks, ageInTicks,
                                netHeadYaw, headPitch
                        );
                    } catch (ClassCastException e) {
                        LOGGER.error("Unable to cast entity for HumanoidClothingLayer to appropriate type!", e);
                    }
                    catch (Exception e) {
                        LOGGER.error("Unable to get HumanoidClothingLayer!", e);
                    }
                }
            };

            for (Item clothing : ClothingRegistry.getAllClothing()) {
                CuriosRendererRegistry.register(clothing, () -> renderer);
            }
        }

        @SubscribeEvent
        public static void addMeshLayers(EntityRenderersEvent.AddLayers event) {
            for (String entityTypeKey : ClothingMeshDefinitions.getEntityTypeKey()) {
                if (entityTypeKey.equals("minecraft:player") || entityTypeKey.equals("minecraft:player_slim")) continue;
                ClothingMeshDefinitions.addLayerHelper(entityTypeKey, event);
            }

            try {
                for (String skinName : event.getSkins()) {
                    LivingEntityRenderer<Player, HumanoidModel<Player>> playerRenderer
                            = event.getSkin(skinName);
                    if (playerRenderer == null) continue;

                    final String p = "minecraft:player";
                    final String ps = "minecraft:player_slim";

                    playerRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    playerRenderer,
                                    skinName.equals("default")
                                            ? ClothingMeshDefinitions.getModelForEntityType(p, 0.30F, event)
                                            : ClothingMeshDefinitions.getModelForEntityType(ps, 0.30F, event),
                                    skinName.equals("default")
                                            ? ClothingMeshDefinitions.getModelForEntityType(p, 0.31F, event)
                                            : ClothingMeshDefinitions.getModelForEntityType(ps, 0.31F, event),
                                    skinName.equals("default")
                                            ? ClothingMeshDefinitions.getModelForEntityType(p, 0.32F, event)
                                            : ClothingMeshDefinitions.getModelForEntityType(ps, 0.32F, event),
                                    skinName.equals("default")
                                            ? ClothingMeshDefinitions.getModelForEntityType(p, 0.33F, event)
                                            : ClothingMeshDefinitions.getModelForEntityType(ps, 0.33F, event),
                                    skinName.equals("default")
                                            ? ClothingMeshDefinitions.getModelForEntityType(p, 0.80F, event)
                                            : ClothingMeshDefinitions.getModelForEntityType(ps, 0.80F, event),
                                    skinName.equals("default")
                                            ? ClothingMeshDefinitions.getModelForEntityType(p, 1.30F, event)
                                            : ClothingMeshDefinitions.getModelForEntityType(ps, 1.30F, event)
                            )
                    );
                }
            } catch (RuntimeException e) {
                LOGGER.error("Error adding layer to player!", e);
            }
        }
    }
}
