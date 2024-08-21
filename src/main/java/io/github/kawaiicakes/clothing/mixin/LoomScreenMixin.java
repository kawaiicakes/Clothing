package io.github.kawaiicakes.clothing.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import io.github.kawaiicakes.clothing.client.HumanoidClothingLayer;
import io.github.kawaiicakes.clothing.common.LoomMenuOverlayGetter;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import io.github.kawaiicakes.clothing.common.resources.OverlayDefinitionLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.Holder;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;

import static net.minecraft.world.item.Items.WHITE_BANNER;

// TODO: rotatable clothing preview
// FIXME: seemingly at random, the preview is scaled up in size and is not aligned with the box
@Mixin(LoomScreen.class)
public abstract class LoomScreenMixin extends AbstractContainerScreen<LoomMenu> {
    @Shadow @Final private static ResourceLocation BG_LOCATION;
    @Unique private static final Logger clothing$LOGGER = LogUtils.getLogger();
    @Shadow @Nullable private List<Pair<Holder<BannerPattern>, DyeColor>> resultBannerPatterns;
    @Shadow private ItemStack bannerStack;
    @Shadow private boolean displayPatterns;

    @Unique private int clothing$selectedRowElement;

    @Shadow private boolean hasMaxPatterns;
    @Unique private boolean clothing$displayOverlays;
    @Unique private float clothing$xMouse;
    @Unique private float clothing$yMouse;
    @Unique
    private HumanoidClothingLayer<AbstractClientPlayer, HumanoidModel<AbstractClientPlayer>, HumanoidModel<AbstractClientPlayer>>
            clothing$humanoidClothingLayer;
    @Unique private ItemStack clothing$previewClothing;

    @Unique
    private void clothing$renderGuiOverlay(
            PoseStack poseStack, OverlayDefinitionLoader.OverlayDefinition overlay, int x, int y
    ) {
        String overlayFullPath = "textures/item/clothing/overlays/" + overlay.name().getPath() + ".png";
        ResourceLocation overlayLocation = new ResourceLocation(overlay.name().getNamespace(), overlayFullPath);
        RenderSystem.setShaderTexture(0, overlayLocation);

        poseStack.pushPose();
        poseStack.translate((x + 0.5F) + 0.15F, y + 0.60F, 0.0D);
        poseStack.scale(0.40F, 0.40F, 1.0F);
        blit(poseStack, 0, 0, 16, 16, 32, 32, 64, 64);
        poseStack.popPose();

        RenderSystem.setShaderTexture(0, BG_LOCATION);
    }

    @SuppressWarnings({"SameParameterValue"})
    @Unique
    private void clothing$renderClothingPreview(
            float partialTick, int pPosX, int pPosY, int pScale, float pMouseX, float pMouseY
    ) {
        if (!(this.clothing$previewClothing.getItem() instanceof ClothingItem)) return;

        PoseStack posestack = RenderSystem.getModelViewStack();
        posestack.pushPose();
        posestack.translate(pPosX, pPosY, 1050.0D);
        posestack.scale(1.0F, 1.0F, -1.0F);
        RenderSystem.applyModelViewMatrix();
        PoseStack posestack1 = new PoseStack();
        posestack1.translate(0.0D, 0.0D, 1000.0D);
        posestack1.scale((float)pScale, (float)pScale, (float)pScale);
        Quaternion quaternion = Vector3f.ZP.rotationDegrees(180.0F);
        posestack1.mulPose(quaternion);
        Lighting.setupForEntityInInventory();

        posestack1.scale(-1.0F, -1.0F, 1.0F);
        posestack1.translate(0.0D, -1.501F, 0.0D);

        assert Minecraft.getInstance().player != null;

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

        this.clothing$humanoidClothingLayer.renderClothingFromItemStack(this.clothing$previewClothing,
                posestack1, buffer,
                Minecraft.getInstance()
                        .getEntityRenderDispatcher()
                        .getPackedLightCoords(Minecraft.getInstance().player, partialTick),
                0.0F, 0.0F,
                partialTick, 0.0F,
                0.0F, 0.0F
        );

        buffer.endBatch();

        posestack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
    }

    @Inject(method = "init", at = @At(value = "TAIL"))
    private void init(CallbackInfo ci) {
        assert this.minecraft != null;
        assert this.minecraft.player != null;
        EntityRenderer<?> entityRenderer
                = this.minecraft.getEntityRenderDispatcher().getRenderer(this.minecraft.player);

        if (!(entityRenderer instanceof PlayerRenderer playerRenderer)) return;

        RenderLayer<?,?> renderLayer = playerRenderer.layers.stream()
                .filter(layer -> layer instanceof HumanoidClothingLayer<?,?,?>)
                .findFirst()
                .orElseThrow();

        //noinspection unchecked
        this.clothing$humanoidClothingLayer
                = (
                        HumanoidClothingLayer<AbstractClientPlayer,
                                HumanoidModel<AbstractClientPlayer>,
                                HumanoidModel<AbstractClientPlayer>>
                ) renderLayer;
    }

    @Inject(
            method = "render",
            at = @At(value = "TAIL")
    )
    private void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick, CallbackInfo ci) {
        this.clothing$xMouse = (float) pMouseX;
        this.clothing$yMouse = (float) pMouseY;
    }

    /**
     * Injects below a {@link com.mojang.blaze3d.platform.Lighting} call to render the overlays of the result stack
     * to the player model preview
     */
    @Inject(
            method = "renderBg",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/Lighting;setupForFlatItems()V",
                    shift = At.Shift.AFTER
            )
    )
    private void renderBgInject0(PoseStack pPoseStack, float pPartialTick, int pX, int pY, CallbackInfo ci) {
        if (this.clothing$previewClothing == null || this.clothing$previewClothing.isEmpty()) return;
        if (this.resultBannerPatterns != null) {
            clothing$LOGGER.error(
                    "Result banner patterns non-null but result overlays are also non-null! If you are seeing" +
                            " this, contact the mod author!"
            );
            return;
        }

        this.clothing$renderClothingPreview(
                pPartialTick,
                this.leftPos + 151,
                this.topPos + 56,
                50,
                this.leftPos - this.clothing$xMouse + 151,
                this.topPos - this.clothing$yMouse + 56 - 33
        );
    }

    /**
     * Makes the conditional check whether to display banner patterns OR overlays (as buttons)
     */
    @ModifyExpressionValue(
            method = "renderBg",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETFIELD,
                    target = "net/minecraft/client/gui/screens/inventory/LoomScreen.displayPatterns : Z"
            )
    )
    private boolean renderBgDisplayPatterns(boolean original) {
        return original || this.clothing$displayOverlays;
    }

    /**
     * Chooses the appropriate value to return when querying the number of selectable elements there are to click
     */
    @ModifyExpressionValue(
            method = "renderBg",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;size()I"
            )
    )
    private int renderBgListSize(int original) {
        if (this.clothing$displayOverlays)
            return ((LoomMenuOverlayGetter) this.menu).getClothing$selectableOverlays().size();

        return original;
    }

    /**
     * Caches an important local for later access since locals cannot be accessed from {@link WrapOperation}
     */
    @Inject(
            method = "renderBg",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/gui/screens/inventory/LoomScreen.renderPattern (Lnet/minecraft/core/Holder;II)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void renderBgCacheSelectedElement(
            PoseStack pPoseStack, float pPartialTick, int pX, int pY,
            CallbackInfo ci,
            @Local(ordinal = 10) int k1
    ) {
        this.clothing$selectedRowElement = k1;
    }

    /**
     * Helps decide whether to blit the vanilla banner buttons or the clothing buttons
     */
    @WrapOperation(
            method = "renderBg",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/LoomScreen;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V",
                    ordinal = 6
            )
    )
    private void renderBgBlitButtons(
            LoomScreen instance,
            PoseStack poseStack, int pX, int pY, int pUOffset, int pVOffset, int pUWidth, int pVHeight,
            Operation<Void> original
    ) {
        if (this.clothing$displayOverlays && this.displayPatterns) {
            clothing$LOGGER.error("uh oh, stinky!");
        }

        int offset = this.displayPatterns ? pUOffset : 14;

        original.call(instance, poseStack, pX, pY, offset, pVOffset, pUWidth, pVHeight);
    }

    /**
     * This is a bit of a hacky way to stop an {@link ArrayIndexOutOfBoundsException} from being thrown when
     * {@link #renderBg(PoseStack, float, int, int)} at L154 attempts to parse the arguments being passed to the
     * original {@code Operation}. This results in an exception since the banner patterns are expected to be empty when
     * a piece of clothing goes in. Returning null would ordinarily be problematic, but since the original operation
     * should not be called in the event that a null value is passed to it to begin with, this should be fine.
     * @see LoomMenuMixin#clickMenuButtonPreventArrayOutOfBoundsException(LoomMenu, Operation)
     */
    @WrapOperation(
            method = "renderBg",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;get(I)Ljava/lang/Object;"
            )
    )
    private <E> E renderBgPreventArrayOutOfBoundsException(List<E> instance, int index, Operation<E> original) {
        return instance.isEmpty() ? null : original.call(instance, index);
    }

    /**
     * Helps decide whether to blit the vanilla banner buttons or the clothing buttons
     */
    @WrapOperation(
            method = "renderBg",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/gui/screens/inventory/LoomScreen.renderPattern (Lnet/minecraft/core/Holder;II)V"
            )
    )
    private void renderBgDrawOnButtons(
            LoomScreen instance, Holder<BannerPattern> pPattern, int pX, int pY, Operation<Void> original,
            PoseStack pPoseStack, float pPartialTick, int pX1, int pY1
    ) {
        if (this.clothing$displayOverlays && this.displayPatterns) {
            clothing$LOGGER.error("uh oh, stinky!");
        }

        if (this.clothing$displayOverlays) {
            List<OverlayDefinitionLoader.OverlayDefinition> overlayList
                    = ((LoomMenuOverlayGetter) this.menu).getClothing$selectableOverlays();
            this.clothing$renderGuiOverlay(pPoseStack, overlayList.get(this.clothing$selectedRowElement), pX, pY);
            return;
        }

        original.call(instance, pPattern, pX, pY);
    }

    /**
     * Makes the conditional check whether to display banner patterns OR overlays (as buttons)
     */
    @ModifyExpressionValue(
            method = "mouseClicked",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETFIELD,
                    target = "net/minecraft/client/gui/screens/inventory/LoomScreen.displayPatterns : Z"
            )
    )
    private boolean mouseClickedDisplayPatterns(boolean original) {
        return original || this.clothing$displayOverlays;
    }

    /**
     * Makes the conditional check whether to display banner patterns OR overlays (as buttons)
     */
    @ModifyExpressionValue(
            method = "mouseDragged",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETFIELD,
                    target = "net/minecraft/client/gui/screens/inventory/LoomScreen.displayPatterns : Z"
            )
    )
    private boolean mouseDraggedDisplayPatterns(boolean original) {
        return original || this.clothing$displayOverlays;
    }

    /**
     * Makes the conditional check whether to display banner patterns OR overlays (as buttons)
     */
    @ModifyExpressionValue(
            method = "mouseScrolled",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETFIELD,
                    target = "net/minecraft/client/gui/screens/inventory/LoomScreen.displayPatterns : Z"
            )
    )
    private boolean mouseScrolledDisplayPatterns(boolean original) {
        return original || this.clothing$displayOverlays;
    }

    /**
     * Wraps the call to {@link net.minecraft.world.level.block.entity.BannerBlockEntity#createPatterns(DyeColor, ListTag)}
     * since a cast item is passed as the first argument. This will cause a ClassCastException if a clothing item is
     * in the banner slot.
     */
    @ModifyExpressionValue(
            method = "containerChanged",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;"
            )
    )
    private Item containerChangedCastWrapper(Item original) {
        if (this.menu.getResultSlot().getItem().getItem() instanceof ClothingItem)
            return WHITE_BANNER;
        return original;
    }

    /**
     * Assigns {@link #clothing$previewClothing} according to what {@link #resultBannerPatterns} is doing.
     */
    @Inject(
            method = "containerChanged",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.PUTFIELD,
                    target = "net/minecraft/client/gui/screens/inventory/LoomScreen.resultBannerPatterns : Ljava/util/List;",
                    shift = At.Shift.AFTER
            )
    )
    private void containerChangedPutFieldPreviewClothing(CallbackInfo ci) {
        if (this.resultBannerPatterns == null) {
            this.clothing$previewClothing = this.hasMaxPatterns ? null : this.menu.getResultSlot().getItem();
            return;
        }

        this.clothing$previewClothing = null;
    }

    /**
     * Modifies the result of {@link net.minecraft.world.level.block.entity.BannerBlockEntity#createPatterns(DyeColor, ListTag)}
     * to return null if a clothing item is present in the banner slot. Prevents the white banner returned to prevent
     * a ClassCastException from displaying.
     */
    @ModifyExpressionValue(
            method = "containerChanged",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/BannerBlockEntity;createPatterns(Lnet/minecraft/world/item/DyeColor;Lnet/minecraft/nbt/ListTag;)Ljava/util/List;"
            )
    )
    private List<Pair<Holder<BannerPattern>, DyeColor>> containerChangedModifyResultBannerPatterns(
            List<Pair<Holder<BannerPattern>, DyeColor>> original
    ) {
        if (
                !this.menu.getBannerSlot().getItem().isEmpty()
                        && this.menu.getBannerSlot().getItem().getItem() instanceof ClothingItem
        ) return null;

        return original;
    }

    /**
     * Assigns {@link #clothing$displayOverlays} in the same block as {@link #displayPatterns} is. Assignment now
     * considers clothing.
     */
    @WrapOperation(
            method = "containerChanged",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.PUTFIELD,
                    target = "net/minecraft/client/gui/screens/inventory/LoomScreen.displayPatterns : Z"
            )
    )
    private void containerChangedAssignDisplayClothingPatterns(
            LoomScreen instance, boolean value, Operation<Void> original
    ) {
        ItemStack clothingStack = this.menu.getBannerSlot().getItem();

        this.clothing$displayOverlays = !clothingStack.isEmpty()
                && clothingStack.getItem() instanceof ClothingItem;

        original.call(instance, value && !(clothingStack.getItem() instanceof ClothingItem));
    }

    /**
     * Wraps the original method to allow row count to work for both overlays and banner patterns.
     * @param original the original value
     * @return the row count
     */
    @WrapMethod(method = "totalRowCount")
    private int containerChangedRowCountCheck(Operation<Integer> original) {
        int modifiedReturn
                = Mth.positiveCeilDiv(((LoomMenuOverlayGetter) this.menu).getClothing$selectableOverlays().size(), 4);

        if (!this.bannerStack.isEmpty() && this.bannerStack.getItem() instanceof ClothingItem)
            return modifiedReturn;

        int toReturn = original.call();

        if (toReturn > 0 && modifiedReturn > 0)
            clothing$LOGGER.error("Overlay and pattern rows are both non-zero!");

        return toReturn;
    }

    private LoomScreenMixin(LoomMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }
}
