package io.github.kawaiicakes.clothing;

import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.client.ClothingModelRepository;
import io.github.kawaiicakes.clothing.client.HumanoidClothingLayer;
import io.github.kawaiicakes.clothing.client.model.impl.GenericLayerModel;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
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
        // un/comment as needed
        CLOTHING_REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onInterModEnqueue);
        FMLJavaModLoadingContext.get().getModEventBus().register(ClothingModelRepository.class);
    }

    @SubscribeEvent
    public void onInterModEnqueue(InterModEnqueueEvent event) {
        // this check is necessary as I'm unsure if this will cause issues on clients who do not have Curios installed.
        // Namely, the reference(s) to classes which only exist in the Curios API
        if (CURIOS_LOADED) {
            boolean messageSent = InterModComms.sendTo(
                    "curios",
                    "register_type",
                    SlotTypePreset.BODY.getMessageBuilder()::build
            );

            String msg = messageSent ? "[Clothing] Successfully registered Curios Clothing slots"
                    : "[Clothing] Curios is present, but was slots were unable to be registered!";

            LOGGER.info(msg);
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientEvents {
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
            GenericLayerModel baseModel =
                    (GenericLayerModel) ClothingModelRepository.getModel(new ResourceLocation(MOD_ID, "base"));
            GenericLayerModel overModel =
                    (GenericLayerModel) ClothingModelRepository.getModel(new ResourceLocation(MOD_ID, "over"));
            // this is so damn scuffed lol, I tried automating this with reflection and iterating over the renderer
            // types but that didn't work
            try {
                LivingEntityRenderer<ArmorStand, ArmorStandModel> armorRenderer
                        = event.getRenderer(EntityType.ARMOR_STAND);
                if (armorRenderer != null) {
                    armorRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    armorRenderer,
                                    baseModel.getModelForEntityType(EntityType.ARMOR_STAND),
                                    overModel.getModelForEntityType(EntityType.ARMOR_STAND)
                            )
                    );
                }

                LivingEntityRenderer<Drowned, DrownedModel<Drowned>> drownedRenderer
                        = event.getRenderer(EntityType.DROWNED);
                if (drownedRenderer != null) {
                    drownedRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    drownedRenderer,
                                    baseModel.getModelForEntityType(EntityType.DROWNED),
                                    overModel.getModelForEntityType(EntityType.DROWNED)
                            )
                    );
                }

                LivingEntityRenderer<Giant, GiantZombieModel> giantRenderer
                        = event.getRenderer(EntityType.GIANT);
                if (giantRenderer != null) {
                    giantRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    giantRenderer,
                                    baseModel.getModelForEntityType(EntityType.GIANT),
                                    overModel.getModelForEntityType(EntityType.GIANT)
                            )
                    );
                }

                LivingEntityRenderer<Husk, ZombieModel<Husk>> huskRenderer
                        = event.getRenderer(EntityType.HUSK);
                if (huskRenderer != null) {
                    huskRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    huskRenderer,
                                    baseModel.getModelForEntityType(EntityType.HUSK),
                                    overModel.getModelForEntityType(EntityType.HUSK)
                            )
                    );
                }

                LivingEntityRenderer<Piglin, PiglinModel<Piglin>> piglinRenderer
                        = event.getRenderer(EntityType.PIGLIN);
                if (piglinRenderer != null) {
                    piglinRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    piglinRenderer,
                                    baseModel.getModelForEntityType(EntityType.PIGLIN),
                                    overModel.getModelForEntityType(EntityType.PIGLIN)
                            )
                    );
                }

                LivingEntityRenderer<PiglinBrute, PiglinModel<PiglinBrute>> piglinBruteRenderer
                        = event.getRenderer(EntityType.PIGLIN_BRUTE);
                if (piglinBruteRenderer != null) {
                    piglinBruteRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    piglinBruteRenderer,
                                    baseModel.getModelForEntityType(EntityType.PIGLIN_BRUTE),
                                    overModel.getModelForEntityType(EntityType.PIGLIN_BRUTE)
                            )
                    );
                }

                LivingEntityRenderer<Skeleton, SkeletonModel<Skeleton>> skeletonRenderer
                        = event.getRenderer(EntityType.SKELETON);
                if (skeletonRenderer != null) {
                    skeletonRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    skeletonRenderer,
                                    baseModel.getModelForEntityType(EntityType.SKELETON),
                                    overModel.getModelForEntityType(EntityType.SKELETON)
                            )
                    );
                }

                LivingEntityRenderer<Stray, SkeletonModel<Stray>> strayRenderer
                        = event.getRenderer(EntityType.STRAY);
                if (strayRenderer != null) {
                    strayRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    strayRenderer,
                                    baseModel.getModelForEntityType(EntityType.STRAY),
                                    overModel.getModelForEntityType(EntityType.STRAY)
                            )
                    );
                }

                LivingEntityRenderer<WitherSkeleton, SkeletonModel<WitherSkeleton>> witherSkeletonRenderer
                        = event.getRenderer(EntityType.WITHER_SKELETON);
                if (witherSkeletonRenderer != null) {
                    witherSkeletonRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    witherSkeletonRenderer,
                                    baseModel.getModelForEntityType(EntityType.WITHER_SKELETON),
                                    overModel.getModelForEntityType(EntityType.WITHER_SKELETON)
                            )
                    );
                }

                LivingEntityRenderer<Zombie, ZombieModel<Zombie>> zombieRenderer
                        = event.getRenderer(EntityType.ZOMBIE);
                if (zombieRenderer != null) {
                    zombieRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    zombieRenderer,
                                    baseModel.getModelForEntityType(EntityType.ZOMBIE),
                                    overModel.getModelForEntityType(EntityType.ZOMBIE)
                            )
                    );
                }

                LivingEntityRenderer<ZombieVillager, ZombieVillagerModel<ZombieVillager>> zombieVillagerRenderer
                        = event.getRenderer(EntityType.ZOMBIE_VILLAGER);
                if (zombieVillagerRenderer != null) {
                    zombieVillagerRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    zombieVillagerRenderer,
                                    baseModel.getModelForEntityType(EntityType.ZOMBIE_VILLAGER),
                                    overModel.getModelForEntityType(EntityType.ZOMBIE_VILLAGER)
                            )
                    );
                }

                LivingEntityRenderer<ZombifiedPiglin, PiglinModel<ZombifiedPiglin>> zombifiedPiglinRenderer
                        = event.getRenderer(EntityType.ZOMBIFIED_PIGLIN);
                if (zombifiedPiglinRenderer != null) {
                    zombifiedPiglinRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    zombifiedPiglinRenderer,
                                    baseModel.getModelForEntityType(EntityType.ZOMBIFIED_PIGLIN),
                                    overModel.getModelForEntityType(EntityType.ZOMBIFIED_PIGLIN)
                            )
                    );
                }
            } catch (RuntimeException e) {
                LOGGER.error("Error adding layer to entity!", e);
            }

            try {
                for (String skinName : event.getSkins()) {
                    LivingEntityRenderer<Player, HumanoidModel<Player>> playerRenderer
                            = event.getSkin(skinName);
                    if (playerRenderer == null) continue;

                    //noinspection unchecked
                    playerRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    playerRenderer,
                                    skinName.equals("default")
                                            ? baseModel.getModelForEntityType(EntityType.PLAYER)
                                            : (HumanoidModel<Player>) baseModel
                                                    .getModelForEntityType("minecraft:player_slim"),
                                    skinName.equals("default")
                                            ? overModel.getModelForEntityType(EntityType.PLAYER)
                                            : (HumanoidModel<Player>) overModel
                                                    .getModelForEntityType("minecraft:player_slim")
                            )
                    );
                }
            } catch (RuntimeException e) {
                LOGGER.error("Error adding layer to player!", e);
            }
        }

        static {
            ClothingModelRepository.registerModel(GenericLayerModel::baseModel);
            ClothingModelRepository.registerModel(GenericLayerModel::overModel);
        }
    }
}
