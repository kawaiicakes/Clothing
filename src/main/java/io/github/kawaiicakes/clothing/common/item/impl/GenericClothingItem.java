package io.github.kawaiicakes.clothing.common.item.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.kawaiicakes.clothing.client.ClientClothingRenderManager;
import io.github.kawaiicakes.clothing.client.HumanoidClothingLayer;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import io.github.kawaiicakes.clothing.common.resources.ClothingEntryLoader;
import io.github.kawaiicakes.clothing.common.resources.GenericClothingEntryLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;

/**
 * Implementation of {@link ClothingItem} for simple cosmetics like T-shirts or anything that looks like default armour.
 */
public class GenericClothingItem extends ClothingItem<GenericClothingItem> {
    public static final String MODEL_LAYER_NBT_KEY = "modelLayer";
    public static final String TEXTURE_LOCATION_NBT_KEY = "texture";
    public static final String OVERLAY_NBT_KEY = "overlays";
    public static final String PART_VISIBILITY_KEY = "partVisibility";
    public static final String DEFAULT_TEXTURE_NBT_KEY = "default";

    // TODO: consider allowing multiple layers when rendering generic clothing
    // TODO: server-sided texture/overlay picker; data from GenericClothingEntryLoader prevents choosing arbitrarily
    public GenericClothingItem(EquipmentSlot pSlot) {
        super(pSlot);
    }

    /**
     * Returns the default texture generic clothing. The returned {@link ItemStack} is guaranteed to return non-null
     * values from the {@link ItemStack}'s tag via {@link CompoundTag#get(String)} for the arguments
     * {@link #CLOTHING_PROPERTY_NBT_KEY}, {@link #MODEL_LAYER_NBT_KEY}, {@link #TEXTURE_LOCATION_NBT_KEY},
     * and {@link #OVERLAY_NBT_KEY}.
     * @return the default {@link ItemStack} for this item.
     */
    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack toReturn = super.getDefaultInstance();

        this.setGenericLayerForRender(toReturn, ModelStrata.forSlot(this.getSlot()));
        this.setTextureLocation(toReturn, DEFAULT_TEXTURE_NBT_KEY);
        this.setOverlays(toReturn, new String[]{});
        this.setPartsForVisibility(toReturn, this.defaultPartVisibility());

        return toReturn;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
        String[] overlayNames = this.getOverlays(pStack);

        if (overlayNames.length == 0) return;

        pTooltipComponents.add(Component.empty());
        pTooltipComponents.add(
                Component.translatable("item.modifiers.clothing.overlays")
                        .withStyle(ChatFormatting.UNDERLINE)
                        .withStyle(ChatFormatting.BOLD)
                        .withStyle(ChatFormatting.DARK_AQUA)
        );
        for (String overlayName : overlayNames) {
            pTooltipComponents.add(
                    Component.literal(overlayName)
                            .withStyle(ChatFormatting.ITALIC)
                            .withStyle(ChatFormatting.AQUA)
            );
        }
    }

    @Override
    public @NotNull ClothingEntryLoader<GenericClothingItem> loaderForType() {
        return GenericClothingEntryLoader.getInstance();
    }

    /**
     * @param itemStack the {@code itemStack} representing this.
     * @return the {@link ModelStrata} indicating which layer the passed stack renders to.
     * @see HumanoidClothingLayer#modelForLayer(ModelStrata)
     */
    public ModelStrata getGenericLayerForRender(ItemStack itemStack) {
        String strataString = this.getClothingPropertyTag(itemStack).getString(MODEL_LAYER_NBT_KEY);
        return ModelStrata.byName(strataString);
    }

    /**
     * @param itemStack the {@code itemStack} representing this.
     * @param modelStrata the {@link ModelStrata} indicating which layer the passed stack renders to.
     * @see HumanoidClothingLayer#modelForLayer(ModelStrata)
     */
    public void setGenericLayerForRender(ItemStack itemStack, ModelStrata modelStrata) {
        this.getClothingPropertyTag(itemStack).putString(MODEL_LAYER_NBT_KEY, modelStrata.getSerializedName());
    }

    /**
     * @param itemStack the {@code itemStack} representing this.
     * @return the {@link String} pointing to the location of the texture folder.
     */
    public String getTextureLocation(ItemStack itemStack) {
        return this.getClothingPropertyTag(itemStack).getString(TEXTURE_LOCATION_NBT_KEY);
    }

    /**
     * @param itemStack the {@code itemStack} representing this.
     * @param textureLocation the {@link String} pointing to the location of the texture folder.
     */
    public void setTextureLocation(ItemStack itemStack, String textureLocation) {
        this.getClothingPropertyTag(itemStack).putString(TEXTURE_LOCATION_NBT_KEY, textureLocation);

        int texHash = this.getDescriptionId(itemStack).hashCode();

        this.setBaseModelData(itemStack, texHash);
    }

    /**
     * @param itemStack the {@code itemStack} representing this.
     * @return the array of {@link String}s whose values point to the overlay textures.
     */
    public String[] getOverlays(ItemStack itemStack) {
        ListTag listTag = this.getClothingPropertyTag(itemStack).getList(OVERLAY_NBT_KEY, Tag.TAG_STRING);
        String[] toReturn = new String[listTag.size()];
        for (int i = 0; i < listTag.size(); i++) {
            if (!(listTag.get(i) instanceof StringTag stringTag)) throw new RuntimeException();
            toReturn[i] = stringTag.getAsString();
        }
        return toReturn;
    }

    /**
     * @param itemStack the {@code itemStack} representing this.
     * @param overlays the array of {@link String}s whose values point to the overlay textures.
     */
    public void setOverlays(ItemStack itemStack, String[] overlays) {
        ListTag overlayTag = new ListTag();

        for (String overlay : overlays) {
            overlayTag.add(StringTag.valueOf(overlay));
        }

        this.getClothingPropertyTag(itemStack).put(OVERLAY_NBT_KEY, overlayTag);
    }

    /**
     * @param itemStack the {@code itemStack} representing this.
     * @return an array of {@link ModelPartReference} whose elements
     * correspond to what body parts the clothing will visibly render on.
     */
    public ModelPartReference[] getPartsForVisibility(ItemStack itemStack) {
        ListTag partList = this.getClothingPropertyTag(itemStack).getList(PART_VISIBILITY_KEY, Tag.TAG_STRING);

        ModelPartReference[] toReturn = new ModelPartReference[partList.size()];
        for (int i = 0; i < partList.size(); i++) {
            toReturn[i] = ModelPartReference.byName(partList.getString(i));
        }

        return toReturn;
    }

    /**
     * @param itemStack the {@code itemStack} representing this.
     * @param slots an array of {@link ModelPartReference} whose
     *              elements correspond to what body parts the clothing will visibly render on.
     */
    public void setPartsForVisibility(ItemStack itemStack, ModelPartReference[] slots) {
        ListTag partList = new ListTag();

        for (ModelPartReference part : slots) {
            partList.add(StringTag.valueOf(part.getSerializedName()));
        }

        this.getClothingPropertyTag(itemStack).put(PART_VISIBILITY_KEY, partList);
    }

    /**
     * This method is used exclusively for setting the default {@link ModelPart} visibility on the generic model as
     * returned by {@link #getGenericLayerForRender(ItemStack)} and
     * {@link HumanoidClothingLayer#modelForLayer(ModelStrata)}.
     * @return the array of {@link ModelPartReference} this item will
     * appear to be worn on.
     * @see HumanoidArmorLayer#setPartVisibility(HumanoidModel, EquipmentSlot)
     */
    @NotNull
    public ModelPartReference[] defaultPartVisibility() {
        return switch (this.getSlot()) {
            case HEAD:
                yield new ModelPartReference[] {
                        ModelPartReference.HEAD,
                        ModelPartReference.HAT
                };
            case CHEST:
                yield new ModelPartReference[] {
                        ModelPartReference.BODY,
                        ModelPartReference.RIGHT_ARM,
                        ModelPartReference.LEFT_ARM
                };
            case LEGS:
                yield new ModelPartReference[] {
                        ModelPartReference.BODY,
                        ModelPartReference.RIGHT_LEG,
                        ModelPartReference.LEFT_LEG
                };
            case MAINHAND:
                yield new ModelPartReference[] {
                        ModelPartReference.RIGHT_ARM
                };
            case OFFHAND:
                yield new ModelPartReference[] {
                        ModelPartReference.LEFT_ARM
                };
            case FEET:
                yield new ModelPartReference[] {
                        ModelPartReference.RIGHT_LEG,
                        ModelPartReference.LEFT_LEG
                };
        };
    }

    @Override
    public void acceptClientClothingRenderManager(Consumer<ClientClothingRenderManager> clothingManager) {
        clothingManager.accept(
                new ClientClothingRenderManager() {
                    @Override
                    public <T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> void render(
                            @NotNull HumanoidClothingLayer<T, M, A> pClothingLayer,
                            @NotNull ItemStack pItemStack,
                            @NotNull PoseStack pMatrixStack,
                            @NotNull MultiBufferSource pBuffer, int pPackedLight,
                            @NotNull T pLivingEntity,
                            float pLimbSwing, float pLimbSwingAmount,
                            float pPartialTicks, float pAgeInTicks,
                            float pNetHeadYaw, float pHeadPitch
                    ) {
                        boolean hasGlint = pItemStack.hasFoil();

                        ModelStrata modelStrata = GenericClothingItem.this.getGenericLayerForRender(pItemStack);
                        A clothingModel = pClothingLayer.modelForLayer(modelStrata);

                        pClothingLayer.getParentModel().copyPropertiesTo(clothingModel);
                        pClothingLayer.setPartVisibility(
                                clothingModel, GenericClothingItem.this.getPartsForVisibility(pItemStack)
                        );

                        int i = GenericClothingItem.this.getColor(pItemStack);
                        float r = (float)(i >> 16 & 255) / 255.0F;
                        float g = (float)(i >> 8 & 255) / 255.0F;
                        float b = (float)(i & 255) / 255.0F;

                        this.renderModel(
                                pMatrixStack,
                                pBuffer, pPackedLight,
                                hasGlint,
                                clothingModel,
                                r, g, b, this.getAlpha(
                                        pLivingEntity,
                                        pItemStack, GenericClothingItem.this.getSlot(),
                                        pPackedLight,
                                        pLimbSwing, pLimbSwingAmount,
                                        pPartialTicks, pAgeInTicks,
                                        pNetHeadYaw, pHeadPitch
                                ),
                                pClothingLayer.getArmorResource(
                                        pLivingEntity, pItemStack, GenericClothingItem.this.getSlot(), null
                                )
                        );

                        String[] overlays = GenericClothingItem.this.getOverlays(pItemStack);
                        if (overlays.length < 1) return;

                        for (int j = overlays.length - 1; j >= 0; j--) {
                            this.renderModel(
                                    pMatrixStack,
                                    pBuffer, pPackedLight,
                                    hasGlint,
                                    clothingModel,
                                    1.0F, 1.0F, 1.0F, this.getAlpha(
                                            pLivingEntity,
                                            pItemStack, GenericClothingItem.this.getSlot(),
                                            pPackedLight,
                                            pLimbSwing, pLimbSwingAmount,
                                            pPartialTicks, pAgeInTicks,
                                            pNetHeadYaw, pHeadPitch
                                    ),
                                    pClothingLayer.getArmorResource(
                                            pLivingEntity, pItemStack,
                                            GenericClothingItem.this.getSlot(), overlays[j]
                                    )
                            );
                        }
                    }

                    public void renderModel(
                            PoseStack pPoseStack,
                            MultiBufferSource pBuffer, int pPackedLight, boolean pGlint,
                            Model pModel,
                            float pRed, float pGreen, float pBlue, float pAlpha,
                            ResourceLocation armorResource
                    ) {
                        VertexConsumer vertexconsumer =
                                ItemRenderer.getArmorFoilBuffer(
                                        pBuffer,
                                        RenderType.armorCutoutNoCull(armorResource),
                                        false,
                                        pGlint
                                );

                        pModel.renderToBuffer(
                                pPoseStack,
                                vertexconsumer,
                                pPackedLight,
                                OverlayTexture.NO_OVERLAY,
                                pRed,
                                pGreen,
                                pBlue,
                                pAlpha
                        );
                    }
                }
        );
    }

    /**
     * Override of super that returns the appropriate texture for this generic clothing item. The passed {@code type}
     * is the texture identifier.
     * @param stack  ItemStack for the equipped armor
     * @param entity The entity wearing the clothing
     * @param slot   The slot the clothing is in
     * @param type   The subtype, can be any valid String according to
     *              {@link ResourceLocation#isValidResourceLocation(String)}.
     * @return the {@link String} that points to the texture for render.
     */
    @Override
    public @NotNull String getArmorTexture(ItemStack stack, Entity entity, @Nullable EquipmentSlot slot, String type) {
        if (type != null) return String.format(
                Locale.ROOT,
                "%s:textures/models/clothing/generic/overlays/%s.png",
                MOD_ID,
                type
        );

        return String.format(
                Locale.ROOT,
                "%s:textures/models/clothing/generic/%s.png",
                MOD_ID,
                this.getTextureLocation(stack)
        );
    }

    @Override
    public boolean canBeDepleted() {
        return false;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    public enum ModelStrata implements StringRepresentable {
        BASE("base"),
        INNER("inner"),
        OUTER("outer"),
        OVER("over"),
        OVER_LEG_ARMOR("over_leg_armor"),
        OVER_ARMOR("over_armor");

        private final String nbtTagID;

        ModelStrata(String nbtTagID) {
            this.nbtTagID = nbtTagID;
        }

        public static ModelStrata byName(String pTargetName) {
            for(ModelStrata modelStrata : values()) {
                if (modelStrata.getSerializedName().equals(pTargetName)) {
                    return modelStrata;
                }
            }

            throw new IllegalArgumentException("Invalid model name '" + pTargetName + "'");
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.nbtTagID;
        }

        public static ModelStrata forSlot(EquipmentSlot equipmentSlot) {
            return switch (equipmentSlot) {
                case FEET -> INNER;
                case LEGS -> BASE;
                case HEAD -> OVER;
                default -> OUTER;
            };
        }
    }
}