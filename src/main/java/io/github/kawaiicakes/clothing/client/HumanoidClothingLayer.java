package io.github.kawaiicakes.clothing.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kawaiicakes.clothing.item.ClothingItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.model.renderable.BakedModelRenderable;
import org.jetbrains.annotations.NotNull;

/**
 * This extends {@link HumanoidArmorLayer} in case a third-party mod references instances of that class to render
 * stuff.
 * <br><br>
 * You could view this as the brain of this mod, where everything comes together to give it its primary functionality.
 * This class, like its parent, is responsible for handling the rendering of "stuff" onto an entity based on what
 * {@link ItemStack}s exist in the {@link EquipmentSlot}s of the entity. This class caches two {@link HumanoidModel}
 * instances just like its parent, as well as an additional two such that every "body group" has its own layer.
 * <br><br>
 * That said, this class works intimately with {@link ClothingItem}s to allow rendering {@link BakedModelRenderable}s;
 * permitting usage of OBJ and JSON models.
 * @version Forge 1.19.2
 * @author kawaiicakes
 */
// TODO: document this class fully
// TODO: expose more methods from super
@OnlyIn(Dist.CLIENT)
public class HumanoidClothingLayer<
        T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>>
        extends HumanoidArmorLayer<T,M,A>
{
    protected final A baseModel;
    protected final A overModel;

    /**
     * Added during {@link EntityRenderersEvent.AddLayers} to appropriate renderer.
     */
    public HumanoidClothingLayer(
            RenderLayerParent<T, M> pRenderer, A baseModel, A innerModel, A outerModel, A overModel
    ) {
        super(
                pRenderer,
                innerModel,
                outerModel
        );

        this.baseModel = baseModel;
        this.overModel = overModel;
    }

    /**
     * Renders stuff onto <code>pLivingEntity</code> according to what exists in its <code>EquipmentSlot</code>s.
     * @param pMatrixStack
     * @param pBuffer
     * @param pPackedLight
     * @param pLivingEntity
     * @param pLimbSwing
     * @param pLimbSwingAmount
     * @param pPartialTicks
     * @param pAgeInTicks
     * @param pNetHeadYaw
     * @param pHeadPitch
     */
    @Override
    public void render(
            @NotNull PoseStack pMatrixStack,
            @NotNull MultiBufferSource pBuffer, int pPackedLight,
            @NotNull T pLivingEntity, float pLimbSwing, float pLimbSwingAmount,
            float pPartialTicks, float pAgeInTicks,
            float pNetHeadYaw, float pHeadPitch
    ) {
        EquipmentSlot[] slots = {
                EquipmentSlot.FEET,
                EquipmentSlot.LEGS,
                EquipmentSlot.CHEST,
                EquipmentSlot.HEAD
        };

        for (EquipmentSlot slot : slots) {
            ItemStack stack = pLivingEntity.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ClothingItem clothing)) continue;
            if (!clothing.getSlot().equals(slot)) continue;

            clothing.render(
                    this,
                    stack,
                    pMatrixStack,
                    pBuffer, pPackedLight,
                    pLivingEntity,
                    pLimbSwing, pLimbSwingAmount,
                    pPartialTicks, pAgeInTicks,
                    pNetHeadYaw, pHeadPitch
            );
        }
    }

    /**
     * Returns the {@link HumanoidModel} instance associated with a slot; this method is used exclusively for rendering
     * models defined using {@link net.minecraft.client.model.geom.builders.MeshDefinition}s.
     * @param pSlot the {@link EquipmentSlot} a piece of clothing is primarily worn on.
     * @return the {@link HumanoidModel} associated with <code>pSlot</code>.
     */
    @Override
    public @NotNull A getArmorModel(@NotNull EquipmentSlot pSlot) {
        return switch (pSlot) {
            case LEGS -> this.baseModel;
            case CHEST -> this.outerModel;
            case HEAD -> this.overModel;
            default -> this.innerModel;
        };
    }

    /**
     * TODO
     * @param pModel
     * @param pSlot
     */
    @Override
    public void setPartVisibility(@NotNull A pModel, @NotNull EquipmentSlot pSlot) {
        super.setPartVisibility(pModel, pSlot);
    }
}