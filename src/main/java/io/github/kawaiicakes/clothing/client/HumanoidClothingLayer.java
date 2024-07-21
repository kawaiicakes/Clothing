package io.github.kawaiicakes.clothing.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kawaiicakes.clothing.item.ClothingItem;
import io.github.kawaiicakes.clothing.item.impl.BakedModelClothingItem;
import io.github.kawaiicakes.clothing.item.impl.GenericClothingItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
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
 * instances just like its parent, an additional two such that every "body group" has its own layer, and two more
 * that allow for rendering over the vanilla armour.
 * <br><br>
 * That said, this class works intimately with {@link ClothingItem}s to allow rendering {@link BakedModelRenderable}s;
 * permitting usage of OBJ and JSON models.
 * @version Forge 1.19.2
 * @author kawaiicakes
 */
// TODO: expose more methods from super
@OnlyIn(Dist.CLIENT)
public class HumanoidClothingLayer<
        T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>>
        extends HumanoidArmorLayer<T,M,A>
{
    protected final A baseModel;
    protected final A overModel;
    protected final A overLegsArmorModel;
    protected final A overMainArmorModel;

    /**
     * Added during {@link EntityRenderersEvent.AddLayers} to appropriate renderer. Creates a
     * {@link RenderLayer} that behaves vaguely like its parent,
     * {@link HumanoidArmorLayer}, but is made to work specifically with {@link ClothingItem} instances.
     * <br><br>
     * Each parameter of type {@link A} represents a model for rendering as in {@link HumanoidArmorLayer}, each of whom
     * is layered on top of the previously provided one.
     */
    public HumanoidClothingLayer(
            RenderLayerParent<T, M> pRenderer,
            A baseModel, A innerModel, A outerModel, A overModel,
            A overLegsArmorModel, A overMainArmorModel
    ) {
        super(
                pRenderer,
                innerModel,
                outerModel
        );

        this.baseModel = baseModel;
        this.overModel = overModel;
        this.overLegsArmorModel = overLegsArmorModel;
        this.overMainArmorModel = overMainArmorModel;
    }

    /**
     * Renders stuff onto <code>pLivingEntity</code> according to what exists in its <code>EquipmentSlot</code>s. This
     * method renders clothing to the buffer according to the instructions specified by the {@link ClothingItem}
     * in a slot, as provided by {@link ClientClothingRenderManager#of(ClothingItem)}
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

            ClientClothingRenderManager renderManager = ClientClothingRenderManager.of(clothing);

            renderManager.render(
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
     * Identical to super; but access declaration made public
     */
    @Override
    public void setPartVisibility(@NotNull A pModel, @NotNull EquipmentSlot pSlot) {
        super.setPartVisibility(pModel, pSlot);
    }

    /**
     * Overload of {@link #setPartVisibility(HumanoidModel, EquipmentSlot)} unique to this class.
     * Allows for greater control of setting part visibility.
     * @param pModel the {@link A} to set part visibility on.
     * @param pSlots the array of {@link EquipmentSlot}s to toggle visibility for. See
     * {@link #setPartVisibility(HumanoidModel, EquipmentSlot)}
     */
    public void setPartVisibility(@NotNull A pModel, @NotNull EquipmentSlot[] pSlots) {
        pModel.setAllVisible(false);

        for (EquipmentSlot slot : pSlots) {
            switch (slot) {
                case HEAD:
                    pModel.head.visible = true;
                    pModel.hat.visible = true;
                    break;
                case CHEST:
                    pModel.body.visible = true;
                    pModel.rightArm.visible = true;
                    pModel.leftArm.visible = true;
                    break;
                case LEGS:
                    pModel.body.visible = true;
                    pModel.rightLeg.visible = true;
                    pModel.leftLeg.visible = true;
                    break;
                case FEET:
                    pModel.rightLeg.visible = true;
                    pModel.leftLeg.visible = true;
            }
        }
    }

    /**
     * Simply returns the appropriate generic model from the corresponding
     * {@link GenericClothingItem.ModelStrata}.
     * @param modelStrata the {@link GenericClothingItem.ModelStrata} whose
     *                    value corresponds to one of the model fields.
     * @return the appropriate {@link A} to render to.
     */
    public A modelForLayer(GenericClothingItem.ModelStrata modelStrata) {
        return switch (modelStrata) {
            case BASE -> this.baseModel;
            case INNER -> this.innerModel;
            case OUTER -> this.outerModel;
            case OVER -> this.overModel;
            case OVER_LEG_ARMOR -> this.overLegsArmorModel;
            case OVER_ARMOR -> this.overMainArmorModel;
        };
    }

    /**
     * Simply returns the appropriate model part from the corresponding
     * {@link io.github.kawaiicakes.clothing.item.impl.BakedModelClothingItem.ModelPartReference}. Exists to avoid
     * directly referencing client-only classes in common classes.
     * @see BakedModelClothingItem#getModelPartForParent(ItemStack)
     * @param reference the {@link io.github.kawaiicakes.clothing.item.impl.BakedModelClothingItem.ModelPartReference}
     *                  corresponding to the desired {@link ModelPart}
     * @return the desired {@link ModelPart}
     */
    public @NotNull ModelPart modelPartByReference(BakedModelClothingItem.ModelPartReference reference) {
        M parent = this.getParentModel();
        return switch (reference) {
            case HEAD -> parent.head;
            case HAT -> parent.hat;
            case BODY -> parent.body;
            case RIGHT_ARM -> parent.rightArm;
            case LEFT_ARM -> parent.leftArm;
            case RIGHT_LEG -> parent.rightLeg;
            case LEFT_LEG -> parent.leftLeg;
        };
    }
}