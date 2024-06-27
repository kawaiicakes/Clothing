package io.github.kawaiicakes.clothing;

import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.client.HumanoidClothingLayer;
import io.github.kawaiicakes.clothing.item.ClothingItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import static net.minecraft.client.model.geom.LayerDefinitions.OUTER_ARMOR_DEFORMATION;

@Mod(ClothingMod.MOD_ID)
public class ClothingMod
{
    public static final String MOD_ID = "clothing";

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Item> ARMOR_REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static HumanoidModel<?> TEMP_MODEL;
    public static final ModelLayerLocation PLAYER_ROD = new ModelLayerLocation(
            new ResourceLocation("player"),
            "rod"
    );

    public static final RegistryObject<ClothingItem> TEST_ARMOR = ARMOR_REGISTRY.register(
            "test",
            () -> new ClothingItem(
                    ArmorMaterials.NETHERITE,
                    EquipmentSlot.CHEST,
                    new Item.Properties(),
                    15417396
            ) {
                @Override
                public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
                    return MOD_ID + ":textures/models/armor/rod.png";
                }

                @Override
                public @NotNull HumanoidModel<?> getClothingModel(LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> originalModel) {
                    return TEMP_MODEL;
                }

                @Override
                public float getAlpha(LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot, int packedLight, float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
                    return 1.0F;
                }

                @Override
                public @Nullable ResourceLocation overlayResource(LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot, int packedLight, float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
                    return null;
                }
            }
    );

    public ClothingMod()
    {
        ARMOR_REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(
                    PLAYER_ROD,
                    ClientEvents::createRod
            );
        }

        @SubscribeEvent
        public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
            for (String skinName : event.getSkins()) {
                LivingEntityRenderer<Player, HumanoidModel<Player>> playerRenderer = event.getSkin(skinName);
                if (playerRenderer == null) {
                    LOGGER.info("unable to get player renderer!");
                    return;
                }
                TEMP_MODEL = new HumanoidModel<>(event.getEntityModels().bakeLayer(PLAYER_ROD));
                playerRenderer.addLayer(
                        new HumanoidClothingLayer<>(
                                playerRenderer,
                                EntityType.PLAYER
                        )
                );
            }
        }

        public static LayerDefinition createRod() {
            MeshDefinition meshdefinition = new MeshDefinition();
            PartDefinition partdefinition = meshdefinition.getRoot();
            partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, OUTER_ARMOR_DEFORMATION), PartPose.offset(0.0F, 0.0F, 0.0F));
            partdefinition.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, OUTER_ARMOR_DEFORMATION.extend(0.5F)), PartPose.offset(0.0F, 0.0F, 0.0F));
            partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-0.5F, 3.0F, -8.0F, 1.0F, 1.0F, 16.0F, OUTER_ARMOR_DEFORMATION), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.2F, 3.8F, 0.0F));
            partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, OUTER_ARMOR_DEFORMATION), PartPose.offset(-5.0F, 2.0F, 0.0F));
            partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, OUTER_ARMOR_DEFORMATION), PartPose.offset(5.0F, 2.0F, 0.0F));
            partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, OUTER_ARMOR_DEFORMATION), PartPose.offset(-1.9F, 12.0F, 0.0F));
            partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, OUTER_ARMOR_DEFORMATION), PartPose.offset(1.9F, 12.0F, 0.0F));
            return LayerDefinition.create(meshdefinition, 64, 32);
        }
    }
}
