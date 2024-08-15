package io.github.kawaiicakes.clothing.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import io.github.kawaiicakes.clothing.common.item.impl.GenericClothingItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;

/**
 * There is no need to add this to a reload listener since I don't need the models the super bakes when reload occurs
 */
@OnlyIn(Dist.CLIENT)
public class ClothingItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static ClothingItemRenderer INSTANCE;

    private ClothingItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        INSTANCE = this;
    }

    public static ClothingItemRenderer getInstance() {
        return INSTANCE != null ? INSTANCE : new ClothingItemRenderer();
    }

    @Override
    @ParametersAreNonnullByDefault
    public void renderByItem(
            ItemStack pStack,
            ItemTransforms.TransformType pTransformType,
            PoseStack pPoseStack,
            MultiBufferSource pBuffer,
            int pPackedLight,
            int pPackedOverlay
    ) {
        if (!(pStack.getItem() instanceof ClothingItem<?> clothingItem)) return;

        ResourceLocation baseModelLocation = baseModelLocation(
                clothingItem instanceof GenericClothingItem, clothingItem.getSlot()
        );
        BakedModel baseModel = Minecraft.getInstance().getModelManager().getModel(baseModelLocation);

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        // FIXME: overlays in item model should not be dyed with the base model
        for (BakedModel modelForPass : baseModel.getRenderPasses(pStack, false)) {
            for (RenderType renderType : modelForPass.getRenderTypes(pStack, false)) {
                VertexConsumer vertexConsumer = ItemRenderer.getFoilBuffer(
                        pBuffer, renderType, true, pStack.hasFoil()
                );

                /*
                    it seems upon preliminary examination that the tint index corresponds to the layer of a texture
                    in an item model. the vanilla colour handlers in ItemColors are used to indicate what colour to
                    return given an item and a tint index; for any value not the base layer (indicated as i > 0),
                    white will be returned, thus permitting things like potions to have a coloured base (layer0)
                    and a non-coloured glass bottle (layer1)
                 */
                itemRenderer.renderModelLists(
                        modelForPass, pStack, pPackedLight, pPackedOverlay, pPoseStack, vertexConsumer
                );
            }
        }
    }

    public static ResourceLocation baseModelLocation(boolean isGeneric, EquipmentSlot slot) {
        String prefix = isGeneric ? "generic" : "baked";
        String suffix = switch (slot) {
            case HEAD -> "hat";
            case LEGS -> "pants";
            case FEET -> "shoes";
            default -> "shirt";
        };

        return new ModelResourceLocation(new ResourceLocation(MOD_ID, prefix + "_" + suffix), "inventory");
    }

    public static ResourceLocation baseModelLocation(ResourceLocation entryLocation) {
        return new ResourceLocation(entryLocation.getNamespace(), "item/clothing/" + entryLocation.getPath());
    }

    public static ResourceLocation overlayModelLocation(ResourceLocation overlayLocation) {
        return new ResourceLocation(
                overlayLocation.getNamespace(), "item/clothing/overlays/" + overlayLocation.getPath()
        );
    }
}
