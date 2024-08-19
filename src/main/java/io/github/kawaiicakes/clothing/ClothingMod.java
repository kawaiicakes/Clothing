package io.github.kawaiicakes.clothing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.client.ClientClothingRenderManager;
import io.github.kawaiicakes.clothing.client.HumanoidClothingLayer;
import io.github.kawaiicakes.clothing.client.model.ClothingItemModel;
import io.github.kawaiicakes.clothing.client.model.GenericDefinitions;
import io.github.kawaiicakes.clothing.common.data.*;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import io.github.kawaiicakes.clothing.common.item.SpoolItem;
import io.github.kawaiicakes.clothing.common.network.ClothingPackets;
import io.github.kawaiicakes.clothing.common.resources.BakedClothingEntryLoader;
import io.github.kawaiicakes.clothing.common.resources.GenericClothingEntryLoader;
import io.github.kawaiicakes.clothing.common.resources.OverlayDefinitionLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DripParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
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
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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
import static io.github.kawaiicakes.clothing.common.item.ClothingItem.BASE_MODEL_DATA;

// TODO: add JEI compat for new loom recipes
// TODO: add new crafting type/"dye vial", allows crafting any dye colour from (automatically generated) inputs
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
        modEventBus.addListener(this::onLoadComplete);

        forgeEventBus.addListener(this::onAddReloadListener);
        forgeEventBus.addListener(this::onDatapackSync);
    }

    @SubscribeEvent
    public void commonSetup(FMLCommonSetupEvent event) {
        /*
            Ensures singleton instance is non-null prior to client connections to/from the server. This was mainly done
            for the client, as upon connecting to a (remote) server for the first time
            ClothingEntryLoader#getLoader(String) would always return null in ClothingPackets.
         */
        OverlayDefinitionLoader.getInstance();
        GenericClothingEntryLoader.getInstance();
        BakedClothingEntryLoader.getInstance();

        ClothingPackets.register();
    }

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(OverlayDefinitionLoader.getInstance());
        event.addListener(GenericClothingEntryLoader.getInstance());
        event.addListener(BakedClothingEntryLoader.getInstance());
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        ClothingPackets.sendToPlayer(
                new ClothingPackets.S2CClothingEntryPacket(
                        GenericClothingEntryLoader.getInstance()
                ),
                event.getPlayer()
        );

        ClothingPackets.sendToPlayer(
                new ClothingPackets.S2CClothingEntryPacket(
                        BakedClothingEntryLoader.getInstance()
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

    @SubscribeEvent
    public void onLoadComplete(FMLLoadCompleteEvent event) {
        ClothingRegistry.registerBrewingRecipes();
        ClothingRegistry.registerCauldronInteractions();
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onClientPlayerNetworkEvent(ClientPlayerNetworkEvent event) {
            if (event instanceof ClientPlayerNetworkEvent.Clone) return;

            ClothingItemModel.Baked.flushModelCaches();
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
            event.register(
                        (pStack, pTintIndex) -> {
                            if (pTintIndex == 1) return 0xFFFFFF;
                            return pTintIndex > 0 ? pTintIndex : ((ClothingItem<?>) pStack.getItem()).getColor(pStack);
                        },
                    ClothingRegistry.getAll()
            );

            event.register(
                    (pStack, pTintIndex) -> pTintIndex > 0 ? 0xFFFFFF : ((SpoolItem) pStack.getItem()).getColor(pStack),
                    ClothingRegistry.SPOOL.get()
            );
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
                    if (!(stack.getItem() instanceof ClothingItem<?> clothingItem)) return;
                    if (!(renderLayerParent instanceof LivingEntityRenderer<T,M> parent)) return;

                    HumanoidClothingLayer<?, ?, ?> layer = (HumanoidClothingLayer<?, ?, ?>) parent.layers.stream()
                            .filter(l -> l instanceof HumanoidClothingLayer<?,?,?>)
                            .findAny()
                            .orElseThrow();

                    Object obj = clothingItem.getClientClothingRenderManager();

                    if (!(obj instanceof ClientClothingRenderManager renderManager)) return;

                    // FIXME: param "pLivingEntity" should either be removed or should somehow actually reflect the entity this is being rendered on

                    renderManager.render(
                            layer,
                            stack,
                            matrixStack,
                            renderTypeBuffer,
                            light,
                            null,
                            limbSwing, limbSwingAmount,
                            partialTicks, ageInTicks,
                            netHeadYaw, headPitch
                    );
                }
            };

            for (Item clothing : ClothingRegistry.getAll()) {
                CuriosRendererRegistry.register(clothing, () -> renderer);
            }
        }

        @SubscribeEvent
        public static void addGenericLayers(EntityRenderersEvent.AddLayers event) {
            for (String entityTypeKey : GenericDefinitions.getEntityTypeKey()) {
                if (entityTypeKey.equals("minecraft:player") || entityTypeKey.equals("minecraft:player_slim")) continue;
                GenericDefinitions.addLayerHelper(entityTypeKey, event);
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
                                            ? GenericDefinitions.getModelForEntityType(p, 0.30F, event)
                                            : GenericDefinitions.getModelForEntityType(ps, 0.30F, event),
                                    skinName.equals("default")
                                            ? GenericDefinitions.getModelForEntityType(p, 0.31F, event)
                                            : GenericDefinitions.getModelForEntityType(ps, 0.31F, event),
                                    skinName.equals("default")
                                            ? GenericDefinitions.getModelForEntityType(p, 0.32F, event)
                                            : GenericDefinitions.getModelForEntityType(ps, 0.32F, event),
                                    skinName.equals("default")
                                            ? GenericDefinitions.getModelForEntityType(p, 0.33F, event)
                                            : GenericDefinitions.getModelForEntityType(ps, 0.33F, event),
                                    skinName.equals("default")
                                            ? GenericDefinitions.getModelForEntityType(p, 0.80F, event)
                                            : GenericDefinitions.getModelForEntityType(ps, 0.80F, event),
                                    skinName.equals("default")
                                            ? GenericDefinitions.getModelForEntityType(p, 1.30F, event)
                                            : GenericDefinitions.getModelForEntityType(ps, 1.30F, event)
                            )
                    );
                }
            } catch (RuntimeException e) {
                LOGGER.error("Error adding layer to player!", e);
            }
        }

        @SubscribeEvent
        public static void onBakingCompleted(ModelEvent.BakingCompleted event) {
            ClothingItem<?>[] clothingItems = ClothingRegistry.getAll();
            if (clothingItems == null) {
                LOGGER.error("Clothing has not been registered yet!");
                clothingItems = new ClothingItem<?>[0];
            }

            for (ClothingItem<?> clothingItem : clothingItems) {
                ItemProperties.register(
                        clothingItem,
                        BASE_MODEL_DATA,
                        (pStack, pLevel, pEntity, pSeed) -> {
                            if (!clothingItem.hasClothingPropertyTag(pStack)) return 0;
                            return clothingItem.getBaseModelData(pStack);
                        }
                );
            }
        }
    }
}
