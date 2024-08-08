package io.github.kawaiicakes.clothing.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.common.LoomMenuOverlayGetter;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import io.github.kawaiicakes.clothing.common.resources.OverlayDefinitionLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
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

import javax.annotation.Nullable;
import java.util.List;

@Mixin(LoomScreen.class)
public abstract class LoomScreenMixin extends AbstractContainerScreen<LoomMenu> {
    /*
        TODO

        Fix banners no longer working if a clothing item has previously been inside

        #mouseClicked
        #mouseDragged
        #mouseScrolled

        More changes as the need for them arises
     */

    @Shadow @Final private static ResourceLocation BG_LOCATION;
    @Unique private static final Logger clothing$LOGGER = LogUtils.getLogger();
    @Shadow @Nullable private List<Pair<Holder<BannerPattern>, DyeColor>> resultBannerPatterns;
    @Shadow private int startRow;
    @Shadow private float scrollOffs;
    @Shadow private ItemStack bannerStack;
    @Shadow private ItemStack dyeStack;
    @Shadow private ItemStack patternStack;
    @Shadow private boolean displayPatterns;
    @Unique private boolean clothing$displayOverlays;
    @Unique private float clothing$xMouse;
    @Unique private float clothing$yMouse;

    @Unique
    private int clothing$totalOverlayRowCount() {
        return Mth.positiveCeilDiv(((LoomMenuOverlayGetter) this.menu).getClothing$selectableOverlays().size(), 4);
    }

    @Unique
    private void clothing$renderOverlay(
            PoseStack poseStack, OverlayDefinitionLoader.OverlayDefinition overlay, int x, int y
    ) {
        String overlayFullPath = "textures/item/clothing/overlays/" + overlay.name() + ".png";
        // TODO: OverlayDefinitionLoader L206
        ResourceLocation overlayLocation = new ResourceLocation("clothing", overlayFullPath);
        RenderSystem.setShaderTexture(0, overlayLocation);

        poseStack.pushPose();
        poseStack.scale(0.25F, 0.25F, 1.0F);
        blit(poseStack, x, y, 0, 0, 64, 64, 64, 64);
        poseStack.popPose();

        RenderSystem.setShaderTexture(0, BG_LOCATION);
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
    private void renderBgInject0(CallbackInfo ci) {
        if (!this.clothing$displayOverlays) return;
        if (this.resultBannerPatterns != null) {
            clothing$LOGGER.error(
                    "Result banner patterns non-null but result overlays are also non-null! If you are seeing" +
                            " this, contact the mod author!"
            );
            return;
        }

        Player previewPlayer = Minecraft.getInstance().player;
        assert previewPlayer != null;

        ItemStack resultStack = this.menu.getResultSlot().getItem();

        if (!resultStack.isEmpty() && resultStack.getItem() instanceof ClothingItem<?>) {
            // FIXME: this isn't ideal
            NonNullList<ItemStack> ogArmor = previewPlayer.getInventory().armor;

            int slotIndexForPreview = ((ClothingItem<?>) resultStack.getItem())
                    .getSlot()
                    .getIndex();
            previewPlayer.getInventory().armor.set(slotIndexForPreview, resultStack);

            InventoryScreen.renderEntityInInventory(
                    this.leftPos, this.topPos,
                    20,
                    this.leftPos - this.clothing$xMouse, this.topPos - this.clothing$yMouse,
                    previewPlayer
            );

            previewPlayer.getInventory().armor.set(slotIndexForPreview, ogArmor.get(slotIndexForPreview));
        }

        InventoryScreen.renderEntityInInventory(
                this.leftPos, this.topPos,
                10,
                this.leftPos - this.clothing$xMouse, this.topPos - this.clothing$yMouse,
                previewPlayer
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
        if (!this.clothing$displayOverlays) return;
        if (this.displayPatterns) {
            clothing$LOGGER.error(
                    "Overlays are to be displayed but so are the banner patterns! If you are seeing" +
                            " this, contact the mod author!"
            );
            return;
        }

        int k2 = ((int) (41.0F * this.scrollOffs)) + 60;
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
                this.clothing$renderOverlay(pPoseStack, overlayList.get(k1), l1, i2);
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

        this.bannerStack = clothingStack.copy();
        this.dyeStack = dyeStack.copy();
        this.patternStack = patternStack.copy();

        ci.cancel();
    }

    private LoomScreenMixin(LoomMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }
}
