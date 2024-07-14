package io.github.kawaiicakes.clothing.item.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.client.ClientClothingRenderManager;
import io.github.kawaiicakes.clothing.client.HumanoidClothingLayer;
import io.github.kawaiicakes.clothing.common.resources.GenericClothingResourceLoader;
import io.github.kawaiicakes.clothing.item.ClothingItem;
import io.github.kawaiicakes.clothing.item.ClothingMaterials;
import io.github.kawaiicakes.clothing.item.ClothingTab;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.NonNullList;
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
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Consumer;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;

/**
 * Implementation of {@link ClothingItem} for simple cosmetics like T-shirts or anything that looks like default armour.
 */
public class GenericClothingItem extends ClothingItem {
    public static final String MODEL_LAYER_NBT_KEY = "modelLayer";
    public static final String TEXTURE_LOCATION_NBT_KEY = "texture";
    public static final String OVERLAY_NBT_KEY = "overlays";
    public static final String SLOT_VISIBILITY_KEY = "slotVisibility";
    public static final String DEFAULT_TEXTURE_NBT_KEY = "default";

    // TODO: final assets, etc.
    // TODO: item icon changes with texture
    // TODO: server-sided texture/overlay picker; data from GenericClothingResourceLoader prevents choosing arbitrarily
    public GenericClothingItem(EquipmentSlot pSlot) {
        super(
                ClothingMaterials.CLOTH,
                pSlot,
                new Properties()
                        .tab(ClothingTab.CLOTHING_TAB)
                        .stacksTo(1)
        );
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
        this.setSlotsForVisibility(toReturn, new EquipmentSlot[]{this.slotForModel()});
        this.setColor(toReturn, 0xFFFFFF);

        return toReturn;
    }

    @Override
    public void fillItemCategory(@NotNull CreativeModeTab pCategory, @NotNull NonNullList<ItemStack> pItems) {
        if (!this.allowedIn(pCategory)) return;

        for (ItemStackInitializer itemEntry : GenericClothingResourceLoader.getInstance().genericClothingEntries()) {
            if (!this.getSlot().equals(itemEntry.slot())) continue;

            ItemStack stackForTab = this.getDefaultInstance();

            this.setGenericLayerForRender(stackForTab, itemEntry.modelLayer());
            this.setTextureLocation(stackForTab, itemEntry.textureIdentifier());
            this.setOverlays(stackForTab, itemEntry.overlays());
            this.setSlotsForVisibility(stackForTab, itemEntry.slotsForVisibility());
            this.setColor(stackForTab, itemEntry.defaultColor());

            pItems.add(stackForTab);
        }
    }

    /**
     * TODO
     * @param itemStack
     * @return
     */
    public ModelStrata getGenericLayerForRender(ItemStack itemStack) {
        String strataString = this.getClothingPropertyTag(itemStack).getString(MODEL_LAYER_NBT_KEY);
        return ModelStrata.byName(strataString);
    }

    /**
     * TODO
     * @param itemStack
     * @param modelStrata
     */
    public void setGenericLayerForRender(ItemStack itemStack, ModelStrata modelStrata) {
        this.getClothingPropertyTag(itemStack).putString(MODEL_LAYER_NBT_KEY, modelStrata.getSerializedName());
    }

    /**
     * TODO
     * @param itemStack
     * @return
     */
    public String getTextureLocation(ItemStack itemStack) {
        return this.getClothingPropertyTag(itemStack).getString(TEXTURE_LOCATION_NBT_KEY);
    }

    /**
     * TODO
     * @param itemStack
     * @param textureLocation
     */
    public void setTextureLocation(ItemStack itemStack, String textureLocation) {
        this.getClothingPropertyTag(itemStack).putString(TEXTURE_LOCATION_NBT_KEY, textureLocation);
    }

    /**
     * TODO
     * @param itemStack
     * @return
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
     * TODO
     * @param itemStack
     * @param overlays
     */
    public void setOverlays(ItemStack itemStack, String[] overlays) {
        ListTag overlayTag = new ListTag();

        for (String overlay : overlays) {
            overlayTag.add(StringTag.valueOf(overlay));
        }

        this.getClothingPropertyTag(itemStack).put(OVERLAY_NBT_KEY, overlayTag);
    }

    // TODO: custom name from data entries/lang key generated from clothing
    @Override
    public String getDescriptionId(ItemStack pStack) {
        return super.getDescriptionId(pStack);
    }

    // TODO: cool tooltip stuff lol
    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }

    /**
     * TODO
     * @param itemStack
     * @return
     */
    public EquipmentSlot[] getSlotsForVisibility(ItemStack itemStack) {
        ListTag slotList = this.getClothingPropertyTag(itemStack).getList(SLOT_VISIBILITY_KEY, Tag.TAG_STRING);

        EquipmentSlot[] toReturn = new EquipmentSlot[slotList.size()];
        for (int i = 0; i < slotList.size(); i++) {
            toReturn[i] = EquipmentSlot.byName(slotList.getString(i));
        }

        return toReturn;
    }

    /**
     * TODO
     * @param itemStack
     * @param slots
     */
    public void setSlotsForVisibility(ItemStack itemStack, EquipmentSlot[] slots) {
        ListTag slotList = new ListTag();

        for (EquipmentSlot slot : slots) {
            slotList.add(StringTag.valueOf(slot.getName()));
        }

        this.getClothingPropertyTag(itemStack).put(SLOT_VISIBILITY_KEY, slotList);
    }

    /**
     * Identical to super, but this is here for documentation purposes. In this class, this method is used exclusively
     * for setting {@link net.minecraft.client.model.geom.ModelPart} visibility on the generic model as returned by
     * {@link #getGenericLayerForRender(ItemStack)} and {@link HumanoidClothingLayer#modelForLayer(ModelStrata)}.
     * <br><br>
     * See {@link HumanoidClothingLayer#setPartVisibility(HumanoidModel, EquipmentSlot)} for further info.
     * @return the {@link EquipmentSlot} this item is worn in.
     */
    @Override
    public @NotNull EquipmentSlot slotForModel() {
        return super.slotForModel();
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
                                clothingModel, GenericClothingItem.this.getSlotsForVisibility(pItemStack)
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

                        for (String overlay : overlays) {
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
                                            GenericClothingItem.this.getSlot(), overlay
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
     * TODO
     * @param stack  ItemStack for the equipped armor
     * @param entity The entity wearing the clothing
     * @param slot   The slot the clothing is in
     * @param type   The subtype, can be any valid String according to
     *              {@link net.minecraft.resources.ResourceLocation#isValidResourceLocation(String)}.
     * @return
     */
    @Override
    public @NotNull String getArmorTexture(ItemStack stack, Entity entity, @Nullable EquipmentSlot slot, String type) {
        if (type != null) return String.format(
                Locale.ROOT,
                "%s:textures/models/armor/generic/overlays/%s.png",
                MOD_ID,
                type
        );

        return String.format(
                Locale.ROOT,
                "%s:textures/models/armor/generic/%s/%s.png",
                MOD_ID,
                this.getTextureLocation(stack),
                this.getGenericLayerForRender(stack).getSerializedName()
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

    /**
     * TODO
     * @param slot
     * @param modelLayer
     * @param textureIdentifier
     * @param overlays
     * @param slotsForVisibility
     * @param defaultColor
     */
    @ApiStatus.Internal
    @ParametersAreNonnullByDefault
    public record ItemStackInitializer(
            EquipmentSlot slot,
            ModelStrata modelLayer,
            String textureIdentifier,
            String[] overlays,
            EquipmentSlot[] slotsForVisibility,
            int defaultColor
    ) {
        private static final Logger LOGGER = LogUtils.getLogger();

        /**
         * Returns a shallow copy. Not sure if this is even necessary, but I want to take care not to allow mutation
         * of values from the {@link GenericClothingResourceLoader}
         * @param original the instance of this to copy
         * @return shallow copy of passed instance
         */
        public static ItemStackInitializer copyOf(ItemStackInitializer original) {
            String[] overlays = new String[original.overlays.length];
            System.arraycopy(original.overlays, 0, overlays, 0, original.overlays.length);

            EquipmentSlot[] slotsForVisibility = new EquipmentSlot[original.slotsForVisibility.length];
            System.arraycopy(
                    original.slotsForVisibility, 0,
                    slotsForVisibility, 0, original.slotsForVisibility.length
            );

            return new ItemStackInitializer(
                    original.slot,
                    original.modelLayer,
                    original.textureIdentifier,
                    overlays,
                    slotsForVisibility,
                    original.defaultColor
            );
        }

        /**
         * TODO
         * @param stackData
         * @return
         */
        @Nullable
        public static Set<ItemStackInitializer> fromJson(ResourceLocation entryId, JsonObject stackData) {
            Set<ItemStackInitializer> toReturn = new HashSet<>();

            for (String slotKey : stackData.keySet()) {
                final EquipmentSlot slot;

                try {
                    slot = EquipmentSlot.byName(slotKey);
                    if (slot.getType().equals(EquipmentSlot.Type.HAND)) throw new IllegalArgumentException();
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Data entry has invalid slot \"{}\" for clothing!", slotKey, e);
                    return null;
                } catch (IllegalStateException e) {
                    LOGGER.error("Slot \"{}\" is not a JSON array!", slotKey, e);
                    return null;
                }

                try {
                    JsonArray itemsForSlot = stackData.get(slotKey).getAsJsonArray();

                    for (int i = 0; i < itemsForSlot.size(); i++) {
                        JsonElement itemElement = itemsForSlot.get(i);
                        JsonObject itemObject = itemElement.getAsJsonObject();
                        final ItemStackInitializer item = fromArrayEntry(entryId, slot, itemObject);
                        if (item == null) throw new RuntimeException();
                        toReturn.add(item);
                    }
                } catch (IllegalStateException e) {
                    LOGGER.error("Clothing entry is of invalid JSON type!", e);
                    return null;
                } catch (RuntimeException e) {
                    LOGGER.error("Error while deserializing clothing data!", e);
                    return null;
                }
            }

            return toReturn;
        }

        @Nullable
        @ParametersAreNonnullByDefault
        public static ItemStackInitializer fromArrayEntry(
                ResourceLocation fileLocation, EquipmentSlot slot, JsonObject element
        ) {
            try {
                final ModelStrata modelLayer =
                        element.has("layer")
                                ? ModelStrata.byName(element.get("layer").getAsJsonPrimitive().getAsString())
                                : ModelStrata.forSlot(slot);

                final String[] overlays =
                        element.has("overlays")
                                ? collapseJsonArrayToStringArray(element.get("overlays").getAsJsonArray())
                                : new String[0];

                if (overlays == null) throw new RuntimeException("String array for overlays returned null!");

                final EquipmentSlot[] slotsForVisibility =
                        element.has("slot_visibility")
                                ? Arrays.stream(
                                        collapseJsonArrayToStringArray(element.get("slot_visibility").getAsJsonArray())
                                )
                                        .map(EquipmentSlot::byName)
                                        .toArray(EquipmentSlot[]::new)
                                : new EquipmentSlot[]{slot};

                final int defaultColor =
                        element.has("color")
                                ? element.get("color").getAsJsonPrimitive().getAsInt()
                                : 0xFFFFFF;

                return new ItemStackInitializer(
                        slot,
                        modelLayer,
                        fileLocation.getPath(),
                        overlays,
                        slotsForVisibility,
                        defaultColor
                );
            } catch (RuntimeException e) {
                LOGGER.error("Error deserializing from JSON array entry!", e);
                return null;
            }
        }

        @Nullable
        public static String[] collapseJsonArrayToStringArray(JsonArray jsonArray) {
            String[] toReturn = new String[jsonArray.size()];
            for (int i = 0; i < jsonArray.size(); i++) {
                if (!(jsonArray.get(i) instanceof JsonPrimitive primitive)) return null;
                toReturn[i] = primitive.getAsString();
            }
            return toReturn;
        }
    }
}