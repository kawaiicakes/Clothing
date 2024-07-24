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
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
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

            String clothingName = this.usesDefaultModelStrata(stackForTab)
                    ? itemEntry.textureIdentifier()
                    : itemEntry.textureIdentifier() + "_" + itemEntry.modelLayer().getSerializedName();

            this.setClothingName(stackForTab, clothingName);
            this.setGenericLayerForRender(stackForTab, itemEntry.modelLayer());
            this.setTextureLocation(stackForTab, itemEntry.textureIdentifier());
            this.setOverlays(stackForTab, itemEntry.overlays());
            this.setSlotsForVisibility(stackForTab, itemEntry.slotsForVisibility());
            this.setColor(stackForTab, itemEntry.defaultColor());

            pItems.add(stackForTab);
        }
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
     * @see ItemStackInitializer
     */
    public String getTextureLocation(ItemStack itemStack) {
        return this.getClothingPropertyTag(itemStack).getString(TEXTURE_LOCATION_NBT_KEY);
    }

    /**
     * @param itemStack the {@code itemStack} representing this.
     * @param textureLocation the {@link String} pointing to the location of the texture folder.
     * @see ItemStackInitializer
     */
    public void setTextureLocation(ItemStack itemStack, String textureLocation) {
        this.getClothingPropertyTag(itemStack).putString(TEXTURE_LOCATION_NBT_KEY, textureLocation);

        String slotString = switch (this.getSlot()) {
            case FEET -> "_feet";
            case LEGS -> "_legs";
            case HEAD -> "_head";
            default -> "_chest";
        };

        int texHash = (textureLocation + slotString).hashCode();

        assert itemStack.getTag() != null;
        itemStack.getTag().putInt(
                "CustomModelData", texHash
        );
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
     * @return an array of {@link EquipmentSlot} whose elements correspond to what body parts the clothing
     *         will visibly render on.
     * @see HumanoidClothingLayer#setPartVisibility(HumanoidModel, EquipmentSlot[])
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
     * @param itemStack the {@code itemStack} representing this.
     * @param slots an array of {@link EquipmentSlot} whose elements correspond to what body parts the clothing
     *              will visibly render on.
     * @see HumanoidClothingLayer#setPartVisibility(HumanoidModel, EquipmentSlot[])
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
     * for setting {@link ModelPart} visibility on the generic model as returned by
     * {@link #getGenericLayerForRender(ItemStack)} and {@link HumanoidClothingLayer#modelForLayer(ModelStrata)}.
     * <br><br>
     * See {@link HumanoidClothingLayer#setPartVisibility(HumanoidModel, EquipmentSlot)} for further info.
     * @return the {@link EquipmentSlot} this item is worn in.
     */
    @Override
    public @NotNull EquipmentSlot slotForModel() {
        return super.slotForModel();
    }

    /**
     * Method determines whether the piece of clothing renders on a different {@link ModelStrata} than the default for
     * the slot it's worn on
     * @param pStack the {@link GenericClothingItem} as an {@link ItemStack} under scrutiny
     * @return {@code true} if matches default. {@code false} otherwise.
     */
    public boolean usesDefaultModelStrata(@NotNull ItemStack pStack) {
        return ModelStrata.forSlot(this.getSlot()).equals(this.getGenericLayerForRender(pStack));
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
                "%s:textures/models/clothing/generic/%s/%s.png",
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
     * The {@link ItemStackInitializer} is simply an immutable data carrier intended for serialization and
     * deserialization between client & server and from datapacks. Its fields are used in
     * {@link GenericClothingItem#fillItemCategory(CreativeModeTab, NonNullList)} to fill the appropriate creative
     * tab with data-driven clothing entries.
     * @param slot the {@link EquipmentSlot} the piece of clothing is worn in.
     * @param modelLayer the {@link ModelStrata} the piece of clothing will render on.
     *                   See {@link HumanoidClothingLayer#modelForLayer(ModelStrata)}.
     * @param textureIdentifier a simple {@link String} representing the folder name in which the clothing's textures
     *                          are stored.
     * @param overlays an array of {@link String}s whose elements represent the names of the overlays to add to the
     *                 clothing.
     * @param slotsForVisibility an array of {@link EquipmentSlot}s whose elements represent the parts of the body
     *                           will be made visible for render for this piece of clothing. See
     *                           {@link HumanoidClothingLayer#setPartVisibility(HumanoidModel, EquipmentSlot[])}.
     * @param defaultColor a hexadecimal colour as an {@code int} for which the clothing will be tinted.
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
         * @param stackData a {@link JsonObject} containing the item entry information.
         * @return a {@link Set} of {@link ItemStackInitializer}s that are used to load item entries in the creative
         * menu.
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

        public static ItemStackInitializer readFromNetwork(FriendlyByteBuf buf) {
            EquipmentSlot slot = buf.readEnum(EquipmentSlot.class);
            ModelStrata modelLayer = buf.readEnum(ModelStrata.class);
            String textureIdentifier = buf.readUtf();
            List<String> overlaysList = buf.readCollection(
                    ArrayList::new,
                    FriendlyByteBuf::readUtf
            );
            List<EquipmentSlot> slotsList = buf.readCollection(
                    ArrayList::new,
                    buf1 -> buf1.readEnum(EquipmentSlot.class)
            );
            int defaultColor = buf.readInt();

            return new ItemStackInitializer(
                    slot,
                    modelLayer,
                    textureIdentifier,
                    overlaysList.toArray(String[]::new),
                    slotsList.toArray(EquipmentSlot[]::new),
                    defaultColor
            );
        }

        public static void writeToNetwork(FriendlyByteBuf buf, ItemStackInitializer itemStackInitializer) {
            buf.writeEnum(itemStackInitializer.slot);
            buf.writeEnum(itemStackInitializer.modelLayer);
            buf.writeUtf(itemStackInitializer.textureIdentifier);
            buf.writeCollection(
                    Arrays.stream(itemStackInitializer.overlays).toList(),
                    FriendlyByteBuf::writeUtf
            );
            buf.writeCollection(
                    Arrays.stream(itemStackInitializer.slotsForVisibility).toList(),
                    FriendlyByteBuf::writeEnum
            );
            buf.writeInt(itemStackInitializer.defaultColor);
        }
    }
}