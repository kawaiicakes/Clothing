package io.github.kawaiicakes.clothing;

import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.client.HumanoidClothingLayer;
import io.github.kawaiicakes.clothing.client.model.GenericDefinitions;
import io.github.kawaiicakes.clothing.common.resources.GenericClothingResourceLoader;
import io.github.kawaiicakes.clothing.item.ClothingItem;
import io.github.kawaiicakes.clothing.item.ClothingRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.SlotTypePreset;

import static io.github.kawaiicakes.clothing.item.ClothingRegistry.CLOTHING_REGISTRY;

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

        modEventBus.addListener(this::onInterModEnqueue);

        forgeEventBus.addListener(this::onAddReloadListener);
    }

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(GenericClothingResourceLoader.getInstance());
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {

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

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onRegisterItemColorHandlers(RegisterColorHandlersEvent.Item event) {
            event.register(
                    (
                            (pStack, pTintIndex) ->
                                    pTintIndex > 0 ? -1 :  ((ClothingItem) pStack.getItem()).getColor(pStack)
                    ),
                    ClothingRegistry.getAll()
            );
        }

        @SubscribeEvent
        public static void onModelRegistration(ModelEvent.RegisterAdditional event) {
            event.register(new ResourceLocation(MOD_ID, "cuboid"));
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
    }
}
