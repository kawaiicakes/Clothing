package io.github.kawaiicakes.clothing.mixin;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import io.github.kawaiicakes.clothing.client.ClientClothingRenderManager;
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
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(LoomScreen.class)
public abstract class LoomScreenMixin extends AbstractContainerScreen<LoomMenu> {
    @Shadow @Final private static ResourceLocation BG_LOCATION;
    @Unique private static final Logger clothing$LOGGER = LogUtils.getLogger();
    @Shadow @Nullable private List<Pair<Holder<BannerPattern>, DyeColor>> resultBannerPatterns;
    @Shadow private int startRow;
    @Shadow private float scrollOffs;
    @Shadow private boolean scrolling;
    @Shadow private ItemStack bannerStack;
    @Shadow private ItemStack dyeStack;
    @Shadow private ItemStack patternStack;
    @Shadow private boolean displayPatterns;

    @Shadow protected abstract int totalRowCount();

    @Unique private boolean clothing$displayOverlays;
    @Unique private float clothing$xMouse;
    @Unique private float clothing$yMouse;
    @Unique
    private HumanoidClothingLayer<AbstractClientPlayer, HumanoidModel<AbstractClientPlayer>, HumanoidModel<AbstractClientPlayer>>
            clothing$humanoidClothingLayer;
    @Unique private ItemStack clothing$previewClothing;

    @Unique
    private int clothing$totalOverlayRowCount() {
        return Mth.positiveCeilDiv(((LoomMenuOverlayGetter) this.menu).getClothing$selectableOverlays().size(), 4);
    }

    @Unique
    private void clothing$renderGuiOverlay(
            PoseStack poseStack, OverlayDefinitionLoader.OverlayDefinition overlay, int x, int y
    ) {
        String overlayFullPath = "textures/item/clothing/overlays/" + overlay.name() + ".png";
        // TODO: OverlayDefinitionLoader L206
        ResourceLocation overlayLocation = new ResourceLocation("clothing", overlayFullPath);
        RenderSystem.setShaderTexture(0, overlayLocation);

        poseStack.pushPose();
        poseStack.translate((float) x + 0.5F, y, 0.0D);
        poseStack.scale(0.20F, 0.20F, 1.0F);
        blit(poseStack, 0, 0, 0, 0, 64, 64, 64, 64);
        poseStack.popPose();

        RenderSystem.setShaderTexture(0, BG_LOCATION);
    }

    @SuppressWarnings({"SameParameterValue"})
    @Unique
    private void clothing$renderClothingPreview(
            float partialTick, int pPosX, int pPosY, int pScale, float pMouseX, float pMouseY
    ) {
        if (!(this.clothing$previewClothing.getItem() instanceof ClothingItem<?> clothingItem)) return;

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

        if (clothingItem.getClientClothingRenderManager() instanceof ClientClothingRenderManager renderer) {
            MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

            renderer.render(
                    this.clothing$humanoidClothingLayer, this.clothing$previewClothing,
                    posestack1, buffer,
                    Minecraft.getInstance()
                            .getEntityRenderDispatcher()
                            .getPackedLightCoords(Minecraft.getInstance().player, partialTick),
                    Minecraft.getInstance().player,
                    0.0F, 0.0F,
                    partialTick, 0.0F,
                    0.0F, 0.0F
            );

            buffer.endBatch();
        }

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
            method = "mouseClicked",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;mouseClicked(DDI)Z",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true
    )
    private void mouseClicked(double pMouseX, double pMouseY, int pButton, CallbackInfoReturnable<Boolean> cir) {
        if (this.displayPatterns) return;
        if (!this.clothing$displayOverlays) return;

        int i = this.leftPos + 60;
        int j = this.topPos + 13;

        for(int k = 0; k < 4; ++k) {
            for(int l = 0; l < 4; ++l) {
                double d0 = pMouseX - (double)(i + l * 14);
                double d1 = pMouseY - (double)(j + k * 14);
                int i1 = k + this.startRow;
                int j1 = i1 * 4 + l;
                if (d0 >= 0.0D && d1 >= 0.0D && d0 < 14.0D && d1 < 14.0D) {
                    assert this.minecraft != null;
                    assert this.minecraft.player != null;
                    if (this.menu.clickMenuButton(this.minecraft.player, j1)) {
                        Minecraft.getInstance()
                                .getSoundManager()
                                .play(SimpleSoundInstance.forUI(SoundEvents.UI_LOOM_SELECT_PATTERN, 1.0F));

                        assert this.minecraft.gameMode != null;
                        this.minecraft.gameMode.handleInventoryButtonClick((this.menu).containerId, j1);

                        cir.setReturnValue(true);
                        cir.cancel();
                    }
                }
            }
        }

        i = this.leftPos + 119;
        j = this.topPos + 9;
        if (pMouseX >= (double)i && pMouseX < (double)(i + 12) && pMouseY >= (double)j && pMouseY < (double)(j + 56)) {
            this.scrolling = true;
        }
    }

    @Inject(
            method = "mouseDragged",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/LoomScreen;totalRowCount()I",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void mouseDragged(
            double pMouseX, double pMouseY,
            int pButton,
            double pDragX, double pDragY,
            CallbackInfoReturnable<Boolean> cir
    ) {
        int patternRows = this.totalRowCount() - 4;
        if (this.scrolling && this.displayPatterns && patternRows > 0) return;
        int overlayRowCount = this.clothing$totalOverlayRowCount() - 4;
        if (!this.scrolling || !this.clothing$displayOverlays || overlayRowCount <= 0) return;

        int j = this.topPos + 13;
        int k = j + 56;
        this.scrollOffs = ((float)pMouseY - (float)j - 7.5F) / ((float)(k - j) - 15.0F);
        this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
        this.startRow = Math.max((int)((double)(this.scrollOffs * (float) overlayRowCount) + 0.5D), 0);
        cir.setReturnValue(true);
        cir.cancel();
    }

    @Inject(
            method = "mouseScrolled",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/LoomScreen;totalRowCount()I",
                    shift = At.Shift.AFTER
            )
    )
    private void mouseScrolled(double pMouseX, double pMouseY, double pDelta, CallbackInfoReturnable<Boolean> cir) {
        int patternRows = this.totalRowCount() - 4;
        if (this.displayPatterns && patternRows > 0) return;
        int overlayRowCount = this.clothing$totalOverlayRowCount() - 4;
        if (!this.clothing$displayOverlays || overlayRowCount <= 0) return;

        float f = (float) pDelta / (float) overlayRowCount;
        this.scrollOffs = Mth.clamp(this.scrollOffs - f, 0.0F, 1.0F);
        this.startRow = Math.max((int)(this.scrollOffs * (float) overlayRowCount + 0.5F), 0);
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
        if (
                !this.clothing$displayOverlays
                        || this.clothing$previewClothing == null
                        || this.clothing$previewClothing.isEmpty()
        ) return;
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
     * Injects above a {@link com.mojang.blaze3d.platform.Lighting} call; the last line in the target method, to render
     * the overlay selection icons.
     */
    @Inject(
            method = "renderBg",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/Lighting;setupFor3DItems()V",
                    shift = At.Shift.BEFORE
            )
    )
    private void renderBgInject1(PoseStack pPoseStack, float pPartialTick, int pX, int pY, CallbackInfo ci) {
        if (this.displayPatterns) return;
        if (!this.clothing$displayOverlays) return;

        int k2 = this.leftPos + 60;
        int l2 = this.topPos + 13;
        List<OverlayDefinitionLoader.OverlayDefinition> overlayList
                = ((LoomMenuOverlayGetter) this.menu).getClothing$selectableOverlays();

        iterationEnd:
        for(int l = 0; l < 4; ++l) {
            for(int i1 = 0; i1 < 4; ++i1) {
                int j1 = l + this.startRow;
                int k1 = j1 * 4 + i1;
                if (k1 >= overlayList.size()) {
                    break iterationEnd;
                }

                RenderSystem.setShaderTexture(0, BG_LOCATION);
                int l1 = k2 + i1 * 14;
                int i2 = l2 + l * 14;
                boolean flag = pX >= l1 && pY >= i2 && pX < l1 + 14 && pY < i2 + 14;
                int j2;
                if (k1 == this.menu.getSelectedBannerPatternIndex()) {
                    j2 = this.imageHeight + 14;
                } else if (flag) {
                    j2 = this.imageHeight + 28;
                } else {
                    j2 = this.imageHeight;
                }

                this.blit(pPoseStack, l1, i2, 14, j2, 14, 14);
                this.clothing$renderGuiOverlay(pPoseStack, overlayList.get(k1), l1, i2);
            }
        }
    }

    @Inject(
            method = "containerChanged",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void containerChanged(CallbackInfo ci) {
        ItemStack clothingStack = this.menu.getBannerSlot().getItem();
        ItemStack dyeStack = this.menu.getDyeSlot().getItem();
        ItemStack patternStack = this.menu.getPatternSlot().getItem();

        final int totalOverlayRows = this.clothing$totalOverlayRowCount();

        if (
                !ItemStack.matches(clothingStack, this.bannerStack)
                        || !ItemStack.matches(dyeStack, this.dyeStack)
                        || !ItemStack.matches(patternStack, this.patternStack)
        ) {
            this.clothing$displayOverlays = !clothingStack.isEmpty()
                    && totalOverlayRows != 0;

            this.displayPatterns = !this.clothing$displayOverlays;
        }

        if (!this.displayPatterns) this.resultBannerPatterns = null;

        if (this.startRow >= totalOverlayRows) {
            this.startRow = 0;
            this.scrollOffs = 0.0F;
        }

        if (
                this.clothing$displayOverlays
                        && !this.menu.getResultSlot().getItem().isEmpty()
                        && this.menu.getResultSlot().getItem().getItem() instanceof ClothingItem<?>
        ) {
            this.clothing$previewClothing = this.menu.getResultSlot().getItem().copy();
        } else {
            this.clothing$previewClothing = null;
        }

        this.bannerStack = clothingStack.copy();
        this.dyeStack = dyeStack.copy();
        this.patternStack = patternStack.copy();

        ci.cancel();
    }

    private LoomScreenMixin(LoomMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }
}
