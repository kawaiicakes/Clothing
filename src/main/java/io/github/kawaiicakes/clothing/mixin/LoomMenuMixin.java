package io.github.kawaiicakes.clothing.mixin;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.common.LoomMenuOverlayGetter;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import io.github.kawaiicakes.clothing.common.item.impl.GenericClothingItem;
import io.github.kawaiicakes.clothing.common.resources.OverlayDefinitionLoader;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.List;

@Mixin(LoomMenu.class)
public abstract class LoomMenuMixin extends AbstractContainerMenu implements LoomMenuOverlayGetter {
    @Unique
    private static final Logger clothing$LOGGER = LogUtils.getLogger();

    @Final
    @Shadow
    private Container inputContainer;

    @Final
    @Mutable
    @Shadow
    Slot bannerSlot;

    @Final
    @Mutable
    @Shadow
    Slot dyeSlot;

    @Final
    @Mutable
    @Shadow
    private Slot patternSlot;

    @Final
    @Mutable
    @Shadow
    private Slot resultSlot;

    @Final
    @Mutable
    @Shadow
    DataSlot selectedBannerPatternIndex;

    @Unique
    protected List<OverlayDefinitionLoader.OverlayDefinition> clothing$selectableOverlays = List.of();

    @Unique
    private boolean clothing$isValidOverlayIndex(int i) {
        return i >= 0 && i < this.clothing$selectableOverlays.size();
    }

    @Override
    public List<OverlayDefinitionLoader.OverlayDefinition> getClothing$selectableOverlays() {
        return ImmutableList.copyOf(this.clothing$selectableOverlays);
    }

    @Unique
    public List<OverlayDefinitionLoader.OverlayDefinition> getClothing$selectableOverlays(ItemStack stack) {
        try {
            if (!(stack.getItem() instanceof ClothingItem<?> clothingItem))
                throw new IllegalArgumentException("Passed stack '" + stack + "' is not a clothing item!");

            if (!(clothingItem instanceof GenericClothingItem)) return List.of();

            if (stack.isEmpty()) return List.of();

            return OverlayDefinitionLoader.getInstance()
                    .getOverlays()
                    .stream()
                    .filter(definition -> definition.isValidEntry(stack))
                    .toList();
        } catch (Exception e) {
            clothing$LOGGER.error("Exception while trying to obtain valid overlays for '{}'!", stack, e);
            return List.of();
        }
    }

    @Unique
    private void clothing$setupClothingResultSlot(OverlayDefinitionLoader.OverlayDefinition overlay) {
        ItemStack clothingStack = this.bannerSlot.getItem();
        ItemStack dyeStack = this.dyeSlot.getItem();
        ItemStack outputStack = ItemStack.EMPTY;

        if (
                !clothingStack.isEmpty()
                        && clothingStack.getItem() instanceof ClothingItem<?> clothingItem
        ) {
            outputStack = clothingStack.copy();
            outputStack.setCount(1);

            if (!dyeStack.isEmpty()) {
                DyeColor dyecolor = ((DyeItem) dyeStack.getItem()).getDyeColor();
                clothingItem.setColor(outputStack, dyecolor.getId());
            }

            if (clothingItem instanceof GenericClothingItem genericClothingItem) {
                String[] existingOverlays = genericClothingItem.getOverlays(outputStack);
                if (Arrays.asList(existingOverlays).contains(overlay.name())) {
                    genericClothingItem.setOverlays(outputStack, ArrayUtils.add(existingOverlays, overlay.name()));
                }
            }
        }

        if (!ItemStack.matches(outputStack, this.resultSlot.getItem())) {
            this.resultSlot.set(outputStack);
        }
    }

    @Inject(
            at = @At("TAIL"),
            method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V"
    )
    private void init(int pContainerId, Inventory pPlayerInventory, ContainerLevelAccess pAccess, CallbackInfo ci) {
        Slot bannerSlot = new Slot(this.inputContainer, 0, 13, 26) {
            public boolean mayPlace(@NotNull ItemStack stack) {
                Item item = stack.getItem();
                return item instanceof BannerItem || item instanceof ClothingItem<?>;
            }
        };
        this.bannerSlot = bannerSlot;
        this.slots.set(0, bannerSlot);
    }

    // Rather than overwrite the whole thing, I'm just gonna run the logic twice in case another mod modifies the target
    @Inject(
            method = "quickMoveStack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/NonNullList;get(I)Ljava/lang/Object;",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void quickMoveStack(Player player, int index, CallbackInfoReturnable<ItemStack> ci) {
        Slot slotFromMixin = this.slots.get(index);

        if (slotFromMixin.hasItem() && index != this.resultSlot.index) {
            ItemStack itemstack1B = slotFromMixin.getItem();

            if (index != this.dyeSlot.index && index != this.bannerSlot.index && index != this.patternSlot.index) {
                if (itemstack1B.getItem() instanceof ClothingItem<?>) {
                    if (
                            !this.moveItemStackTo(
                                    itemstack1B,
                                    this.bannerSlot.index,
                                    this.bannerSlot.index + 1,
                                    false
                            )
                    ) {
                        ci.setReturnValue(ItemStack.EMPTY);
                        ci.cancel();
                    }
                }
            }
        }
    }

    @Inject(
            method = "slotsChanged",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void slotsChanged(Container container, CallbackInfo ci) {
        if (
                !this.bannerSlot.getItem().isEmpty()
                        && this.bannerSlot.getItem().getItem() instanceof ClothingItem<?>
        ) {
            ItemStack clothingStack = this.bannerSlot.getItem();

            int i = this.selectedBannerPatternIndex.get();
            boolean validOverlayIndex = this.clothing$isValidOverlayIndex(i);

            List<OverlayDefinitionLoader.OverlayDefinition> oldList = this.clothing$selectableOverlays;
            this.clothing$selectableOverlays = this.getClothing$selectableOverlays(clothingStack);

            final OverlayDefinitionLoader.OverlayDefinition overlay;

            if (this.clothing$selectableOverlays.size() == 1) {
                this.selectedBannerPatternIndex.set(0);
                overlay = this.clothing$selectableOverlays.get(0);
            } else if (!validOverlayIndex) {
                this.selectedBannerPatternIndex.set(-1);
                overlay = null;
            } else {
                OverlayDefinitionLoader.OverlayDefinition overlay1 = oldList.get(i);
                int j = this.clothing$selectableOverlays.indexOf(overlay1);
                if (j != -1) {
                    overlay = overlay1;
                    this.selectedBannerPatternIndex.set(j);
                } else {
                    overlay = null;
                    this.selectedBannerPatternIndex.set(-1);
                }
            }

            if (overlay != null) {
                    this.clothing$setupClothingResultSlot(overlay);
            } else {
                this.resultSlot.set(ItemStack.EMPTY);
            }

            this.broadcastChanges();
            ci.cancel();
        }
    }

    @Inject(
            method = "clickMenuButton",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void clickMenuButton(Player pPlayer, int pId, CallbackInfoReturnable<Boolean> cir) {
        if (
                pId >= 0
                        && pId < this.clothing$selectableOverlays.size()
                        && !this.bannerSlot.getItem().isEmpty()
                        && this.bannerSlot.getItem().getItem() instanceof ClothingItem<?>
        ) {
            this.selectedBannerPatternIndex.set(pId);
            this.clothing$setupClothingResultSlot(this.clothing$selectableOverlays.get(pId));
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    private LoomMenuMixin(@Nullable MenuType<?> pMenuType, int pContainerId) {
        super(pMenuType, pContainerId);
    }
}
