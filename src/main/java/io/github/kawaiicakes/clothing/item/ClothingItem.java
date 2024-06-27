package io.github.kawaiicakes.clothing.item;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public abstract class ClothingItem extends ArmorItem implements DyeableLeatherItem {
    public ClothingItem(ArmorMaterial pMaterial, EquipmentSlot pSlot, Properties pProperties) {
        super(pMaterial, pSlot, pProperties);
    }

    /**
     * Implementations should return the desired model for this piece of clothing. Bear in mind that the models are
     * rendered as layers in {@link net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer}.
     * <br><br>
     * If you only plan to use the default armour model, just immediately return <code>originalModel</code>.
     * @param livingEntity the {@link LivingEntity} this model is made for.
     * @param stack the {@link ItemStack} representing this piece of clothing.
     * @param slot the {@link EquipmentSlot this piece of clothing goes in.}
     * @param originalModel the original {@link HumanoidModel} passed to this method in
     * {@link net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer}.
     * @return the desired {@link HumanoidModel} for this piece of clothing.
     */
    public abstract HumanoidModel<?> getClothingModel(
            LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> originalModel
    );

    /**
     * You don't need to override this unless you know what you're doing.
     */
    @Override
    public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer) {
        consumer.accept(
                // Forge docs say not to use a concrete implementation of IClientItemExtensions here
                new IClientItemExtensions() {
                    @Override
                    public @NotNull HumanoidModel<?> getHumanoidArmorModel(
                            LivingEntity livingEntity,
                            ItemStack itemStack, EquipmentSlot equipmentSlot,
                            HumanoidModel<?> original
                    ) {
                        return ClothingItem.this.getClothingModel(livingEntity, itemStack, equipmentSlot, original);
                    }
                }
        );
    }
}
