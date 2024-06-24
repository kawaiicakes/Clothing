package io.github.kawaiicakes.clothing.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kawaiicakes.clothing.client.HumanoidGenericClothingLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Pretty much a 1:1 copy of {@link ArmorItem} so that clothing does not attempt to be rendered in
 * {@link net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer#renderArmorPiece(PoseStack, MultiBufferSource, LivingEntity, EquipmentSlot, int, HumanoidModel)}.
 */
public class ClothingItem extends Item implements Wearable {
    // Not sure if using the same UUIDs as the actual armour modifiers is a good idea. Oh well.
    private static final UUID[] ARMOR_MODIFIER_UUID_PER_SLOT = new UUID[]{UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"), UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"), UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"), UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150")};
    public static final DispenseItemBehavior DISPENSE_ITEM_BEHAVIOR = new DefaultDispenseItemBehavior() {
        protected @NotNull ItemStack execute(@NotNull BlockSource pSource, @NotNull ItemStack pItemStack) {
            return ArmorItem.dispenseArmor(pSource, pItemStack) ? pItemStack : super.execute(pSource, pItemStack);
        }
    };
    protected final EquipmentSlot slot;
    private final int defense;
    private final float toughness;
    protected final float knockbackResistance;
    protected final ArmorMaterial material;
    private final Multimap<Attribute, AttributeModifier> defaultModifiers;
    protected final float alpha;
    protected final boolean hasOverlay;
    protected final HumanoidGenericClothingLayer.ClothingLayer defaultClothingLayer;
    
    public ClothingItem(
            ArmorMaterial pMaterial, EquipmentSlot pSlot,
            Properties pProperties,
            float pAlpha,
            HumanoidGenericClothingLayer.ClothingLayer pDefaultClothingLayer
    ) {
        super(pProperties.defaultDurability(pMaterial.getDurabilityForSlot(pSlot)));

        this.material = pMaterial;
        this.slot = pSlot;
        this.defense = pMaterial.getDefenseForSlot(pSlot);
        this.toughness = pMaterial.getToughness();
        this.knockbackResistance = pMaterial.getKnockbackResistance();
        this.alpha = pAlpha;
        this.hasOverlay = pDefaultClothingLayer.equals(HumanoidGenericClothingLayer.ClothingLayer.CUSTOM);
        this.defaultClothingLayer = pDefaultClothingLayer;

        DispenserBlock.registerBehavior(this, DISPENSE_ITEM_BEHAVIOR);

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        UUID uuid = ARMOR_MODIFIER_UUID_PER_SLOT[pSlot.getIndex()];

        builder.put(
                Attributes.ARMOR,
                new AttributeModifier(
                        uuid,
                        "Armor modifier",
                        this.defense,
                        AttributeModifier.Operation.ADDITION
                )
        );
        builder.put(
                Attributes.ARMOR_TOUGHNESS,
                new AttributeModifier(
                        uuid,
                        "Armor toughness",
                        this.toughness,
                        AttributeModifier.Operation.ADDITION
                )
        );

        if (this.knockbackResistance > 0) {
            builder.put(
                    Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(
                            uuid,
                            "Armor knockback resistance",
                            this.knockbackResistance,
                            AttributeModifier.Operation.ADDITION
                    )
            );
        }

        this.defaultModifiers = builder.build();
    }

    public EquipmentSlot getSlot() {
        return this.slot;
    }

    @SuppressWarnings("deprecation")
    public int getEnchantmentValue() {
        return this.material.getEnchantmentValue();
    }

    public ArmorMaterial getMaterial() {
        return this.material;
    }

    public boolean isValidRepairItem(@NotNull ItemStack pToRepair, @NotNull ItemStack pRepair) {
        return this.material.getRepairIngredient().test(pRepair) || super.isValidRepairItem(pToRepair, pRepair);
    }

    @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level pLevel, Player pPlayer, @NotNull InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        EquipmentSlot equipmentslot = Mob.getEquipmentSlotForItem(itemstack);
        ItemStack itemstack1 = pPlayer.getItemBySlot(equipmentslot);
        if (itemstack1.isEmpty()) {
            pPlayer.setItemSlot(equipmentslot, itemstack.copy());
            if (!pLevel.isClientSide()) {
                pPlayer.awardStat(Stats.ITEM_USED.get(this));
            }

            itemstack.setCount(0);
            return InteractionResultHolder.sidedSuccess(itemstack, pLevel.isClientSide());
        } else {
            return InteractionResultHolder.fail(itemstack);
        }
    }

    @SuppressWarnings("deprecation")
    public @NotNull Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(@NotNull EquipmentSlot pEquipmentSlot) {
        return pEquipmentSlot == this.slot ? this.defaultModifiers : super.getDefaultAttributeModifiers(pEquipmentSlot);
    }

    public int getDefense() {
        return this.defense;
    }

    public float getToughness() {
        return this.toughness;
    }

    public float getAlpha() {
        return this.alpha;
    }

    public boolean hasOverlay() {
        return this.hasOverlay;
    }

    public HumanoidGenericClothingLayer.ClothingLayer getDefaultClothingLayer() {
        return this.defaultClothingLayer;
    }

    @Nullable
    public SoundEvent getEquipSound() {
        return this.getMaterial().getEquipSound();
    }
}
