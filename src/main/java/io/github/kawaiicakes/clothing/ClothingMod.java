package io.github.kawaiicakes.clothing;

import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.client.HumanoidClothingLayer;
import io.github.kawaiicakes.clothing.client.model.GenericDefinitions;
import io.github.kawaiicakes.clothing.common.data.ClothingEntryGenerator;
import io.github.kawaiicakes.clothing.common.data.ClothingItemModelGenerator;
import io.github.kawaiicakes.clothing.common.data.ClothingLangGenerator;
import io.github.kawaiicakes.clothing.common.data.ClothingOverlayGenerator;
import io.github.kawaiicakes.clothing.common.network.ClothingPackets;
import io.github.kawaiicakes.clothing.common.resources.BakedClothingEntryLoader;
import io.github.kawaiicakes.clothing.common.resources.GenericClothingEntryLoader;
import io.github.kawaiicakes.clothing.common.resources.OverlayDefinitionLoader;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import io.github.kawaiicakes.clothing.common.item.ClothingRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
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
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.SlotTypePreset;

import java.util.Collection;

import static io.github.kawaiicakes.clothing.common.item.ClothingItem.BASE_MODEL_DATA;
import static io.github.kawaiicakes.clothing.common.item.ClothingRegistry.CLOTHING_REGISTRY;
import static io.github.kawaiicakes.clothing.common.resources.recipe.ClothingRecipeRegistry.SERIALIZER_REGISTRY;

// TODO: custom item models for clothing so I don't have to rely on 9213912939123 item predicates
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

        CLOTHING_REGISTRY.register(modEventBus);
        SERIALIZER_REGISTRY.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onInterModEnqueue);
        modEventBus.addListener(this::onGatherData);

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
                clothingEntryGenerator
        );

        dataGenerator.addProvider(
                event.includeServer() || event.includeClient(),
                clothingOverlayGenerator
        );
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onRegisterItemColorHandlers(RegisterColorHandlersEvent.Item event) {
            event.register(
                        (pStack, pTintIndex) -> {
                            if (pTintIndex == 1) return 0xFFFFFF;
                            return pTintIndex > 0 ? pTintIndex : ((ClothingItem<?>) pStack.getItem()).getColor(pStack);
                        },
                    ClothingRegistry.getAll()
            );
        }

        @SubscribeEvent
        public static void onModelRegistration(ModelEvent.RegisterAdditional event) {
            final Collection<ResourceLocation> list = Minecraft.getInstance().getResourceManager().listResources(
                    "models/clothing", (location) -> location.getPath().endsWith(".json")
            ).keySet();


            for (ResourceLocation modelLocation : list) {
                String pathWithoutDuplicate = modelLocation.getPath().replace("models/", "");
                String pathWithoutSuffix = pathWithoutDuplicate.replace(".json", "");
                ResourceLocation withoutSuffix = new ResourceLocation(modelLocation.getNamespace(), pathWithoutSuffix);
                event.register(withoutSuffix);
            }
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            CURIOS_LOADED = ModList.get().isLoaded("curios");
            LOGGER.info(
                    CURIOS_LOADED ? "[Clothing] Curios successfully detected during client setup."
                            : "[Clothing] Curios was not detected during client setup."
            );
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
