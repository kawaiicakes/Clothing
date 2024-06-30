package io.github.kawaiicakes.clothing;

import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.client.HumanoidClothingLayer;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import static io.github.kawaiicakes.clothing.client.model.GenericClothingLayers.*;

@Mod(ClothingMod.MOD_ID)
public class ClothingMod
{
    public static final String MOD_ID = "clothing";

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Item> ARMOR_REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public ClothingMod()
    {
        ARMOR_REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onRegisterGenericLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(ARMOR_STAND_BASE, );
            event.registerLayerDefinition(ARMOR_STAND_OVER, );
            event.registerLayerDefinition(DROWNED_BASE, );
            event.registerLayerDefinition(DROWNED_OVER, );
            event.registerLayerDefinition(GIANT_BASE, );
            event.registerLayerDefinition(GIANT_OVER, );
            event.registerLayerDefinition(HUSK_BASE, );
            event.registerLayerDefinition(HUSK_OVER, );
            event.registerLayerDefinition(PLAYER_BASE, );
            event.registerLayerDefinition(PLAYER_OVER, );
            event.registerLayerDefinition(PLAYER_SLIM_BASE, );
            event.registerLayerDefinition(PLAYER_SLIM_OVER, );
            event.registerLayerDefinition(PIGLIN_BASE, );
            event.registerLayerDefinition(PIGLIN_OVER, );
            event.registerLayerDefinition(PIGLIN_BRUTE_BASE, );
            event.registerLayerDefinition(PIGLIN_BRUTE_OVER, );
            event.registerLayerDefinition(SKELETON_BASE, );
            event.registerLayerDefinition(SKELETON_OVER, );
            event.registerLayerDefinition(STRAY_BASE, );
            event.registerLayerDefinition(STRAY_OVER, );
            event.registerLayerDefinition(WITHER_SKELETON_BASE, );
            event.registerLayerDefinition(WITHER_SKELETON_OVER, );
            event.registerLayerDefinition(ZOMBIE_BASE, );
            event.registerLayerDefinition(ZOMBIE_OVER, );
            event.registerLayerDefinition(ZOMBIFIED_PIGLIN_BASE, );
            event.registerLayerDefinition(ZOMBIFIED_PIGLIN_OVER, );
            event.registerLayerDefinition(ZOMBIE_VILLAGER_BASE, );
            event.registerLayerDefinition(ZOMBIE_VILLAGER_OVER, );
        }

        @SubscribeEvent
        public static void onAddGenericLayers(EntityRenderersEvent.AddLayers event) {
            EntityModelSet entityModelSet = event.getEntityModels();
            // this is so damn scuffed lol, I tried automating this with reflection and iterating over the renderer
            // types but that didn't work
            try {
                LivingEntityRenderer<ArmorStand, ArmorStandModel> armorRenderer
                        = event.getRenderer(EntityType.ARMOR_STAND);
                if (armorRenderer != null) {
                    armorRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    armorRenderer,
                                    new ArmorStandArmorModel(entityModelSet.bakeLayer(ARMOR_STAND_BASE)),
                                    new ArmorStandArmorModel(entityModelSet.bakeLayer(ARMOR_STAND_OVER))
                            )
                    );
                }

                LivingEntityRenderer<Drowned, DrownedModel<Drowned>> drownedRenderer
                        = event.getRenderer(EntityType.DROWNED);
                if (drownedRenderer != null) {
                    drownedRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    drownedRenderer,
                                    new DrownedModel<>(entityModelSet.bakeLayer(DROWNED_BASE)),
                                    new DrownedModel<>(entityModelSet.bakeLayer(DROWNED_OVER))
                            )
                    );
                }

                LivingEntityRenderer<Giant, GiantZombieModel> giantRenderer
                        = event.getRenderer(EntityType.GIANT);
                if (giantRenderer != null) {
                    giantRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    giantRenderer,
                                    new GiantZombieModel(entityModelSet.bakeLayer(GIANT_BASE)),
                                    new GiantZombieModel(entityModelSet.bakeLayer(GIANT_OVER))
                            )
                    );
                }

                LivingEntityRenderer<Husk, ZombieModel<Husk>> huskRenderer
                        = event.getRenderer(EntityType.HUSK);
                if (huskRenderer != null) {
                    huskRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    huskRenderer,
                                    new ZombieModel<>(entityModelSet.bakeLayer(HUSK_BASE)),
                                    new ZombieModel<>(entityModelSet.bakeLayer(HUSK_OVER))
                            )
                    );
                }

                LivingEntityRenderer<Piglin, PiglinModel<Piglin>> piglinRenderer
                        = event.getRenderer(EntityType.PIGLIN);
                if (piglinRenderer != null) {
                    piglinRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    piglinRenderer,
                                    new PiglinModel<>(entityModelSet.bakeLayer(PIGLIN_BASE)),
                                    new PiglinModel<>(entityModelSet.bakeLayer(PIGLIN_OVER))
                            )
                    );
                }

                LivingEntityRenderer<PiglinBrute, PiglinModel<PiglinBrute>> piglinBruteRenderer
                        = event.getRenderer(EntityType.PIGLIN_BRUTE);
                if (piglinBruteRenderer != null) {
                    piglinBruteRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    piglinBruteRenderer,
                                    new PiglinModel<>(entityModelSet.bakeLayer(PIGLIN_BRUTE_BASE)),
                                    new PiglinModel<>(entityModelSet.bakeLayer(PIGLIN_BRUTE_OVER))
                            )
                    );
                }

                LivingEntityRenderer<Skeleton, SkeletonModel<Skeleton>> skeletonRenderer
                        = event.getRenderer(EntityType.SKELETON);
                if (skeletonRenderer != null) {
                    skeletonRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    skeletonRenderer,
                                    new SkeletonModel<>(entityModelSet.bakeLayer(SKELETON_BASE)),
                                    new SkeletonModel<>(entityModelSet.bakeLayer(SKELETON_OVER))
                            )
                    );
                }

                LivingEntityRenderer<Stray, SkeletonModel<Stray>> strayRenderer
                        = event.getRenderer(EntityType.STRAY);
                if (strayRenderer != null) {
                    strayRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    strayRenderer,
                                    new SkeletonModel<>(entityModelSet.bakeLayer(STRAY_BASE)),
                                    new SkeletonModel<>(entityModelSet.bakeLayer(STRAY_OVER))
                            )
                    );
                }

                LivingEntityRenderer<WitherSkeleton, SkeletonModel<WitherSkeleton>> witherSkeletonRenderer
                        = event.getRenderer(EntityType.WITHER_SKELETON);
                if (witherSkeletonRenderer != null) {
                    witherSkeletonRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    witherSkeletonRenderer,
                                    new SkeletonModel<>(entityModelSet.bakeLayer(WITHER_SKELETON_BASE)),
                                    new SkeletonModel<>(entityModelSet.bakeLayer(WITHER_SKELETON_OVER))
                            )
                    );
                }

                LivingEntityRenderer<Zombie, ZombieModel<Zombie>> zombieRenderer
                        = event.getRenderer(EntityType.ZOMBIE);
                if (zombieRenderer != null) {
                    zombieRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    zombieRenderer,
                                    new ZombieModel<>(entityModelSet.bakeLayer(ZOMBIE_BASE)),
                                    new ZombieModel<>(entityModelSet.bakeLayer(ZOMBIE_OVER))
                            )
                    );
                }

                LivingEntityRenderer<ZombieVillager, ZombieVillagerModel<ZombieVillager>> zombieVillagerRenderer
                        = event.getRenderer(EntityType.ZOMBIE_VILLAGER);
                if (zombieVillagerRenderer != null) {
                    zombieVillagerRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    zombieVillagerRenderer,
                                    new ZombieVillagerModel<>(entityModelSet.bakeLayer(ZOMBIE_VILLAGER_BASE)),
                                    new ZombieVillagerModel<>(entityModelSet.bakeLayer(ZOMBIE_VILLAGER_OVER))
                            )
                    );
                }

                LivingEntityRenderer<ZombifiedPiglin, PiglinModel<ZombifiedPiglin>> zombifiedPiglinRenderer
                        = event.getRenderer(EntityType.ZOMBIFIED_PIGLIN);
                if (zombifiedPiglinRenderer != null) {
                    zombifiedPiglinRenderer.addLayer(
                            new HumanoidClothingLayer<>(
                                    zombifiedPiglinRenderer,
                                    new PiglinModel<>(entityModelSet.bakeLayer(ZOMBIFIED_PIGLIN_BASE)),
                                    new PiglinModel<>(entityModelSet.bakeLayer(ZOMBIFIED_PIGLIN_OVER))
                            )
                    );
                }
            } catch (RuntimeException e) {
                LOGGER.error("Error adding layer to entity!", e);
            }

            for (String skinName : event.getSkins()) {
                if (!(event.getSkin(skinName) instanceof PlayerRenderer playerRenderer)) {
                    LOGGER.info("unable to get player renderer!");
                    return;
                }

                playerRenderer.addLayer(
                        new HumanoidClothingLayer<>(
                                playerRenderer,
                                new HumanoidModel<>(
                                        entityModelSet.bakeLayer(
                                                skinName.equals("default") ? PLAYER_BASE : PLAYER_SLIM_BASE
                                        )
                                ),
                                new HumanoidModel<>(
                                        entityModelSet.bakeLayer(
                                                skinName.equals("default") ? PLAYER_OVER : PLAYER_SLIM_OVER
                                        )
                                )
                        )
                );
            }
        }
    }
}
